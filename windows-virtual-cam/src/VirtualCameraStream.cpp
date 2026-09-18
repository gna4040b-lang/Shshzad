#include "VirtualCameraStream.h"
#include "VirtualCameraMediaSource.h"

VirtualCameraStream::VirtualCameraStream(
    VirtualCameraMediaSource* pSource,
    IMFStreamDescriptor* pSD,
    const VideoConfig& config
) : m_pSource(pSource), m_pStreamDescriptor(pSD), m_config(config) {
    if (m_pSource) m_pSource->AddRef();
    if (m_pStreamDescriptor) m_pStreamDescriptor->AddRef();

    MFCreateEventQueue(&m_pEventQueue);

    m_videoReader = std::make_unique<VideoFileReader>();
    m_patternGenerator = std::make_unique<SyntheticPatternGenerator>();
    m_networkReceiver = std::make_unique<NetworkStreamReceiver>();
}

VirtualCameraStream::~VirtualCameraStream() {
    Shutdown();
    SafeRelease(&m_pEventQueue);
    SafeRelease(&m_pStreamDescriptor);
    SafeRelease(&m_pSource);
}

// IUnknown
STDMETHODIMP VirtualCameraStream::QueryInterface(REFIID riid, void** ppv) {
    if (!ppv) return E_POINTER;
    if (riid == IID_IUnknown || riid == IID_IMFMediaEventGenerator || riid == IID_IMFMediaStream) {
        *ppv = static_cast<IMFMediaStream*>(this);
        AddRef();
        return S_OK;
    }
    *ppv = nullptr;
    return E_NOINTERFACE;
}

STDMETHODIMP_(ULONG) VirtualCameraStream::AddRef() {
    return InterlockedIncrement(&m_refCount);
}

STDMETHODIMP_(ULONG) VirtualCameraStream::Release() {
    ULONG count = InterlockedDecrement(&m_refCount);
    if (count == 0) {
        delete this;
    }
    return count;
}

// IMFMediaEventGenerator
STDMETHODIMP VirtualCameraStream::GetEvent(DWORD dwFlags, IMFMediaEvent** ppEvent) {
    if (!m_pEventQueue) return MF_E_SHUTDOWN;
    return m_pEventQueue->GetEvent(dwFlags, ppEvent);
}

STDMETHODIMP VirtualCameraStream::BeginGetEvent(IMFAsyncCallback* pCallback, IUnknown* punkState) {
    if (!m_pEventQueue) return MF_E_SHUTDOWN;
    return m_pEventQueue->BeginGetEvent(pCallback, punkState);
}

STDMETHODIMP VirtualCameraStream::EndGetEvent(IMFAsyncResult* pResult, IMFMediaEvent** ppEvent) {
    if (!m_pEventQueue) return MF_E_SHUTDOWN;
    return m_pEventQueue->EndGetEvent(pResult, ppEvent);
}

STDMETHODIMP VirtualCameraStream::QueueEvent(MediaEventType met, REFGUID guidExtendedType, HRESULT hrStatus, const PROPVARIANT* pvValue) {
    if (!m_pEventQueue) return MF_E_SHUTDOWN;
    return m_pEventQueue->QueueEventParamVar(met, guidExtendedType, hrStatus, pvValue);
}

// IMFMediaStream
STDMETHODIMP VirtualCameraStream::GetMediaSource(IMFMediaSource** ppMediaSource) {
    if (!ppMediaSource) return E_POINTER;
    if (!m_pSource) return MF_E_SHUTDOWN;
    *ppMediaSource = m_pSource;
    (*ppMediaSource)->AddRef();
    return S_OK;
}

STDMETHODIMP VirtualCameraStream::GetStreamDescriptor(IMFStreamDescriptor** ppStreamDescriptor) {
    if (!ppStreamDescriptor) return E_POINTER;
    if (!m_pStreamDescriptor) return MF_E_SHUTDOWN;
    *ppStreamDescriptor = m_pStreamDescriptor;
    (*ppStreamDescriptor)->AddRef();
    return S_OK;
}

STDMETHODIMP VirtualCameraStream::RequestSample(IUnknown* pToken) {
    std::lock_guard<std::mutex> lock(m_stateMutex);
    if (m_isShutdown) return MF_E_SHUTDOWN;
    if (!m_isActive || m_isPaused) return S_OK;

    return DeliverSample(pToken);
}

HRESULT VirtualCameraStream::Start(LONGLONG startPosition100ns) {
    std::lock_guard<std::mutex> lock(m_stateMutex);
    m_isActive = true;
    m_isPaused = false;
    m_currentTime100ns = (startPosition100ns >= 0) ? startPosition100ns : 0;

    PROPVARIANT var;
    PropVariantInit(&var);
    var.vt = VT_I8;
    var.hVal.QuadPart = m_currentTime100ns;
    QueueEvent(MEStreamStarted, GUID_NULL, S_OK, &var);
    PropVariantClear(&var);

    return S_OK;
}

HRESULT VirtualCameraStream::Pause() {
    std::lock_guard<std::mutex> lock(m_stateMutex);
    m_isPaused = true;
    return QueueEvent(MEStreamPaused, GUID_NULL, S_OK, nullptr);
}

HRESULT VirtualCameraStream::Stop() {
    std::lock_guard<std::mutex> lock(m_stateMutex);
    m_isActive = false;
    m_isPaused = false;
    return QueueEvent(MEStreamStopped, GUID_NULL, S_OK, nullptr);
}

HRESULT VirtualCameraStream::Shutdown() {
    std::lock_guard<std::mutex> lock(m_stateMutex);
    m_isShutdown = true;
    m_isActive = false;
    if (m_videoReader) m_videoReader->Close();
    if (m_networkReceiver) m_networkReceiver->Stop();
    if (m_pEventQueue) m_pEventQueue->Shutdown();
    return S_OK;
}

void VirtualCameraStream::SetVideoFile(const std::wstring& filePath) {
    std::lock_guard<std::mutex> lock(m_stateMutex);
    if (m_networkReceiver) m_networkReceiver->Stop();
    if (m_videoReader) {
        m_videoReader->OpenVideo(filePath, m_config.width, m_config.height, m_config.videoFormat);
    }
}

void VirtualCameraStream::SetNetworkStream(const std::wstring& url) {
    std::lock_guard<std::mutex> lock(m_stateMutex);
    if (m_videoReader) m_videoReader->Close();
    if (m_networkReceiver) {
        m_networkReceiver->Start(url, m_config.width, m_config.height);
    }
}

void VirtualCameraStream::ClearSource() {
    std::lock_guard<std::mutex> lock(m_stateMutex);
    if (m_videoReader) m_videoReader->Close();
    if (m_networkReceiver) m_networkReceiver->Stop();
}

void VirtualCameraStream::SetConfig(const VideoConfig& config) {
    std::lock_guard<std::mutex> lock(m_stateMutex);
    m_config = config;
}

VideoConfig VirtualCameraStream::GetConfig() const {
    return m_config;
}

HRESULT VirtualCameraStream::DeliverSample(IUnknown* pToken) {
    LONGLONG frameDuration100ns = (10000000LL * m_config.fpsDenominator) / m_config.fpsNumerator;

    IMFSample* pSample = nullptr;
    HRESULT hr = CreateSampleFromFrame(&pSample, m_currentTime100ns, frameDuration100ns);
    if (SUCCEEDED(hr) && pSample) {
        if (pToken) {
            pSample->SetUnknown(MFSampleExtension_Token, pToken);
        }

        hr = QueueEvent(MEMediaSample, GUID_NULL, S_OK, nullptr);
        SafeRelease(&pSample);
    }

    m_currentTime100ns += frameDuration100ns;
    m_frameIndex++;

    return hr;
}

HRESULT VirtualCameraStream::CreateSampleFromFrame(
    IMFSample** ppSample,
    LONGLONG timestamp100ns,
    LONGLONG duration100ns
) {
    if (!ppSample) return E_POINTER;
    *ppSample = nullptr;

    DWORD bufferSize = m_config.width * m_config.height * 4;
    IMFMediaBuffer* pMediaBuffer = nullptr;
    HRESULT hr = MFCreateMemoryBuffer(bufferSize, &pMediaBuffer);
    if (FAILED(hr)) return hr;

    BYTE* pData = nullptr;
    hr = pMediaBuffer->Lock(&pData, nullptr, nullptr);
    if (SUCCEEDED(hr)) {
        bool frameRendered = false;

        // 1. If camera power is OFF, render Standby test card
        if (!m_config.isVirtualCamOn) {
            m_patternGenerator->FillFrame(
                pData, bufferSize, m_config.width, m_config.height,
                m_config.videoFormat, false, m_frameIndex, timestamp100ns
            );
            frameRendered = true;
        }

        // 2. If Network Stream receiver is active and connected, fetch network frame
        if (!frameRendered && m_networkReceiver && m_networkReceiver->IsConnected()) {
            if (m_networkReceiver->CopyLatestFrame(pData, bufferSize)) {
                frameRendered = true;
            }
        }

        // 3. If Video file reader is open, read frame from video
        if (!frameRendered && m_videoReader && m_videoReader->IsOpen()) {
            bool isEndOfStream = false;
            hr = m_videoReader->ReadNextFrame(pData, bufferSize, m_config.isLooping, isEndOfStream);
            if (SUCCEEDED(hr)) {
                frameRendered = true;
            }
        }

        // 4. Default: Render real-time SMPTE calibration bar pattern
        if (!frameRendered) {
            m_patternGenerator->FillFrame(
                pData, bufferSize, m_config.width, m_config.height,
                m_config.videoFormat, true, m_frameIndex, timestamp100ns
            );
        }

        pMediaBuffer->Unlock();
        pMediaBuffer->SetCurrentLength(bufferSize);
    }

    IMFSample* pSample = nullptr;
    hr = MFCreateSample(&pSample);
    if (SUCCEEDED(hr)) {
        pSample->AddBuffer(pMediaBuffer);
        pSample->SetSampleTime(timestamp100ns);
        pSample->SetSampleDuration(duration100ns);
        *ppSample = pSample;
    }

    SafeRelease(&pMediaBuffer);
    return hr;
}
