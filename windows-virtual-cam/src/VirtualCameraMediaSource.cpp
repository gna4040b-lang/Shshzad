#include "VirtualCameraMediaSource.h"

VirtualCameraMediaSource::VirtualCameraMediaSource() {
    MFCreateEventQueue(&m_pEventQueue);
}

VirtualCameraMediaSource::~VirtualCameraMediaSource() {
    Shutdown();
    SafeRelease(&m_pEventQueue);
    SafeRelease(&m_pPresentationDescriptor);
    SafeRelease(&m_pStream);
}

HRESULT VirtualCameraMediaSource::CreateInstance(const VideoConfig& config, VirtualCameraMediaSource** ppSource) {
    if (!ppSource) return E_POINTER;
    *ppSource = nullptr;

    VirtualCameraMediaSource* pSource = new (std::nothrow) VirtualCameraMediaSource();
    if (!pSource) return E_OUTOFMEMORY;

    HRESULT hr = pSource->Initialize(config);
    if (FAILED(hr)) {
        pSource->Release();
        return hr;
    }

    *ppSource = pSource;
    return S_OK;
}

HRESULT VirtualCameraMediaSource::Initialize(const VideoConfig& config) {
    // 1. Create Media Types for the stream (RGB32 & NV12 at target resolution and framerate)
    IMFMediaType* pMediaType = nullptr;
    HRESULT hr = MFCreateMediaType(&pMediaType);
    if (FAILED(hr)) return hr;

    pMediaType->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Video);
    pMediaType->SetGUID(MF_MT_SUBTYPE, config.videoFormat);
    MFSetAttributeSize(pMediaType, MF_MT_FRAME_SIZE, config.width, config.height);
    MFSetAttributeRatio(pMediaType, MF_MT_FRAME_RATE, config.fpsNumerator, config.fpsDenominator);
    MFSetAttributeRatio(pMediaType, MF_MT_PIXEL_ASPECT_RATIO, 1, 1);
    pMediaType->SetUINT32(MF_MT_INTERLACE_MODE, MFVideoInterlace_Progressive);
    pMediaType->SetUINT32(MF_MT_ALL_SAMPLES_INDEPENDENT, TRUE);

    // 2. Create Stream Descriptor
    IMFMediaType* mediaTypes[1] = { pMediaType };
    IMFStreamDescriptor* pSD = nullptr;
    hr = MFCreateStreamDescriptor(0, 1, mediaTypes, &pSD);
    SafeRelease(&pMediaType);
    if (FAILED(hr)) return hr;

    // 3. Create Presentation Descriptor
    IMFStreamDescriptor* descriptors[1] = { pSD };
    hr = MFCreatePresentationDescriptor(1, descriptors, &m_pPresentationDescriptor);
    if (FAILED(hr)) {
        SafeRelease(&pSD);
        return hr;
    }

    // Select the video stream by default
    m_pPresentationDescriptor->SelectStream(0);

    // 4. Create Stream object
    m_pStream = new (std::nothrow) VirtualCameraStream(this, pSD, config);
    SafeRelease(&pSD);
    if (!m_pStream) return E_OUTOFMEMORY;

    return S_OK;
}

// IUnknown
STDMETHODIMP VirtualCameraMediaSource::QueryInterface(REFIID riid, void** ppv) {
    if (!ppv) return E_POINTER;
    if (riid == IID_IUnknown || riid == IID_IMFMediaEventGenerator || riid == IID_IMFMediaSource) {
        *ppv = static_cast<IMFMediaSource*>(this);
        AddRef();
        return S_OK;
    }
    if (riid == IID_IMFGetService) {
        *ppv = static_cast<IMFGetService*>(this);
        AddRef();
        return S_OK;
    }
    *ppv = nullptr;
    return E_NOINTERFACE;
}

STDMETHODIMP_(ULONG) VirtualCameraMediaSource::AddRef() {
    return InterlockedIncrement(&m_refCount);
}

STDMETHODIMP_(ULONG) VirtualCameraMediaSource::Release() {
    ULONG count = InterlockedDecrement(&m_refCount);
    if (count == 0) {
        delete this;
    }
    return count;
}

// IMFMediaEventGenerator
STDMETHODIMP VirtualCameraMediaSource::GetEvent(DWORD dwFlags, IMFMediaEvent** ppEvent) {
    if (!m_pEventQueue) return MF_E_SHUTDOWN;
    return m_pEventQueue->GetEvent(dwFlags, ppEvent);
}

STDMETHODIMP VirtualCameraMediaSource::BeginGetEvent(IMFAsyncCallback* pCallback, IUnknown* punkState) {
    if (!m_pEventQueue) return MF_E_SHUTDOWN;
    return m_pEventQueue->BeginGetEvent(pCallback, punkState);
}

STDMETHODIMP VirtualCameraMediaSource::EndGetEvent(IMFAsyncResult* pResult, IMFMediaEvent** ppEvent) {
    if (!m_pEventQueue) return MF_E_SHUTDOWN;
    return m_pEventQueue->EndGetEvent(pResult, ppEvent);
}

STDMETHODIMP VirtualCameraMediaSource::QueueEvent(MediaEventType met, REFGUID guidExtendedType, HRESULT hrStatus, const PROPVARIANT* pvValue) {
    if (!m_pEventQueue) return MF_E_SHUTDOWN;
    return m_pEventQueue->QueueEventParamVar(met, guidExtendedType, hrStatus, pvValue);
}

// IMFMediaSource
STDMETHODIMP VirtualCameraMediaSource::GetCharacteristics(DWORD* pdwCharacteristics) {
    if (!pdwCharacteristics) return E_POINTER;
    // Live camera source with no fixed seek duration
    *pdwCharacteristics = MFMEDIASOURCE_IS_LIVE;
    return S_OK;
}

STDMETHODIMP VirtualCameraMediaSource::CreatePresentationDescriptor(IMFPresentationDescriptor** ppPresentationDescriptor) {
    if (!ppPresentationDescriptor) return E_POINTER;
    std::lock_guard<std::mutex> lock(m_mutex);
    if (m_isShutdown) return MF_E_SHUTDOWN;
    if (!m_pPresentationDescriptor) return E_FAIL;

    return m_pPresentationDescriptor->Clone(ppPresentationDescriptor);
}

STDMETHODIMP VirtualCameraMediaSource::Start(
    IMFPresentationDescriptor* pPresentationDescriptor,
    const GUID* pguidTimeFormat,
    const PROPVARIANT* pvarStartPosition
) {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (m_isShutdown) return MF_E_SHUTDOWN;

    LONGLONG startPos = 0;
    if (pvarStartPosition && pvarStartPosition->vt == VT_I8) {
        startPos = pvarStartPosition->hVal.QuadPart;
    }

    if (m_pStream) {
        m_pStream->Start(startPos);
    }

    PROPVARIANT var;
    PropVariantInit(&var);
    var.vt = VT_I8;
    var.hVal.QuadPart = startPos;
    QueueEvent(MESourceStarted, GUID_NULL, S_OK, &var);
    PropVariantClear(&var);

    return S_OK;
}

STDMETHODIMP VirtualCameraMediaSource::Stop() {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (m_isShutdown) return MF_E_SHUTDOWN;

    if (m_pStream) {
        m_pStream->Stop();
    }
    return QueueEvent(MESourceStopped, GUID_NULL, S_OK, nullptr);
}

STDMETHODIMP VirtualCameraMediaSource::Pause() {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (m_isShutdown) return MF_E_SHUTDOWN;

    if (m_pStream) {
        m_pStream->Pause();
    }
    return QueueEvent(MESourcePaused, GUID_NULL, S_OK, nullptr);
}

STDMETHODIMP VirtualCameraMediaSource::Shutdown() {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (m_isShutdown) return S_OK;
    m_isShutdown = true;

    if (m_pStream) {
        m_pStream->Shutdown();
    }
    if (m_pEventQueue) {
        m_pEventQueue->Shutdown();
    }
    return S_OK;
}

STDMETHODIMP VirtualCameraMediaSource::GetService(REFGUID guidService, REFIID riid, LPVOID* ppvObject) {
    if (!ppvObject) return E_POINTER;
    *ppvObject = nullptr;
    return MF_E_UNSUPPORTED_SERVICE;
}
