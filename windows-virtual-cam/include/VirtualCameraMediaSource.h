#pragma once
#include "Common.h"
#include "VirtualCameraStream.h"

// CLSID for our Media Foundation Virtual Camera Source: {C342898B-933B-49E8-9DC2-E7A47B2E58C1}
static const GUID CLSID_VirtualCameraMediaSource = 
{ 0xc342898b, 0x933b, 0x49e8, { 0x9d, 0xc2, 0xe7, 0xa4, 0x7b, 0x2e, 0x58, 0xc1 } };

class VirtualCameraMediaSource : public IMFMediaSource, public IMFGetService {
public:
    static HRESULT CreateInstance(const VideoConfig& config, VirtualCameraMediaSource** ppSource);

    // IUnknown
    STDMETHODIMP QueryInterface(REFIID riid, void** ppv) override;
    STDMETHODIMP_(ULONG) AddRef() override;
    STDMETHODIMP_(ULONG) Release() override;

    // IMFMediaEventGenerator
    STDMETHODIMP GetEvent(DWORD dwFlags, IMFMediaEvent** ppEvent) override;
    STDMETHODIMP BeginGetEvent(IMFAsyncCallback* pCallback, IUnknown* punkState) override;
    STDMETHODIMP EndGetEvent(IMFAsyncResult* pResult, IMFMediaEvent** ppEvent) override;
    STDMETHODIMP QueueEvent(MediaEventType met, REFGUID guidExtendedType, HRESULT hrStatus, const PROPVARIANT* pvValue) override;

    // IMFMediaSource
    STDMETHODIMP GetCharacteristics(DWORD* pdwCharacteristics) override;
    STDMETHODIMP CreatePresentationDescriptor(IMFPresentationDescriptor** ppPresentationDescriptor) override;
    STDMETHODIMP Start(
        IMFPresentationDescriptor* pPresentationDescriptor,
        const GUID* pguidTimeFormat,
        const PROPVARIANT* pvarStartPosition
    ) override;
    STDMETHODIMP Stop() override;
    STDMETHODIMP Pause() override;
    STDMETHODIMP Shutdown() override;

    // IMFGetService
    STDMETHODIMP GetService(REFGUID guidService, REFIID riid, LPVOID* ppvObject) override;

    // Stream access
    VirtualCameraStream* GetStream() const { return m_pStream; }

protected:
    VirtualCameraMediaSource();
    virtual ~VirtualCameraMediaSource();
    HRESULT Initialize(const VideoConfig& config);

private:
    long m_refCount = 1;
    IMFMediaEventQueue* m_pEventQueue = nullptr;
    IMFPresentationDescriptor* m_pPresentationDescriptor = nullptr;
    VirtualCameraStream* m_pStream = nullptr;

    std::mutex m_mutex;
    bool m_isShutdown = false;
};
