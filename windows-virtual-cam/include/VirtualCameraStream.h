#pragma once
#include "Common.h"
#include "VideoFileReader.h"
#include "SyntheticPatternGenerator.h"
#include "NetworkStreamReceiver.h"

class VirtualCameraMediaSource;

class VirtualCameraStream : public IMFMediaStream {
public:
    VirtualCameraStream(VirtualCameraMediaSource* pSource, IMFStreamDescriptor* pSD, const VideoConfig& config);
    virtual ~VirtualCameraStream();

    // IUnknown
    STDMETHODIMP QueryInterface(REFIID riid, void** ppv) override;
    STDMETHODIMP_(ULONG) AddRef() override;
    STDMETHODIMP_(ULONG) Release() override;

    // IMFMediaEventGenerator
    STDMETHODIMP GetEvent(DWORD dwFlags, IMFMediaEvent** ppEvent) override;
    STDMETHODIMP BeginGetEvent(IMFAsyncCallback* pCallback, IUnknown* punkState) override;
    STDMETHODIMP EndGetEvent(IMFAsyncResult* pResult, IMFMediaEvent** ppEvent) override;
    STDMETHODIMP QueueEvent(MediaEventType met, REFGUID guidExtendedType, HRESULT hrStatus, const PROPVARIANT* pvValue) override;

    // IMFMediaStream
    STDMETHODIMP GetMediaSource(IMFMediaSource** ppMediaSource) override;
    STDMETHODIMP GetStreamDescriptor(IMFStreamDescriptor** ppStreamDescriptor) override;
    STDMETHODIMP RequestSample(IUnknown* pToken) override;

    // Internal controls
    HRESULT Start(LONGLONG startPosition100ns);
    HRESULT Pause();
    HRESULT Stop();
    HRESULT Shutdown();

    void SetVideoFile(const std::wstring& filePath);
    void SetNetworkStream(const std::wstring& url);
    void ClearSource();
    void SetConfig(const VideoConfig& config);
    VideoConfig GetConfig() const;

private:
    HRESULT DeliverSample(IUnknown* pToken);
    HRESULT CreateSampleFromFrame(IMFSample** ppSample, LONGLONG timestamp100ns, LONGLONG duration100ns);

    long m_refCount = 1;
    VirtualCameraMediaSource* m_pSource = nullptr;
    IMFMediaEventQueue* m_pEventQueue = nullptr;
    IMFStreamDescriptor* m_pStreamDescriptor = nullptr;

    VideoConfig m_config;
    std::mutex m_stateMutex;
    bool m_isActive = false;
    bool m_isPaused = false;
    bool m_isShutdown = false;

    UINT64 m_frameIndex = 0;
    LONGLONG m_currentTime100ns = 0;

    std::unique_ptr<VideoFileReader> m_videoReader;
    std::unique_ptr<SyntheticPatternGenerator> m_patternGenerator;
    std::unique_ptr<NetworkStreamReceiver> m_networkReceiver;
};
