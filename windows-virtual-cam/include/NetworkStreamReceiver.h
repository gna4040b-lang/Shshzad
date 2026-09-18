#pragma once
#include "Common.h"
#include <wincodec.h>

class NetworkStreamReceiver {
public:
    NetworkStreamReceiver();
    ~NetworkStreamReceiver();

    bool Start(const std::wstring& url, UINT32 targetWidth, UINT32 targetHeight);
    void Stop();
    bool CopyLatestFrame(BYTE* pDestBuffer, DWORD destBufferSize);
    bool IsConnected() const { return m_isConnected; }

private:
    void WorkerLoop(const std::wstring& url);

    std::atomic<bool> m_isRunning{false};
    std::atomic<bool> m_isConnected{false};
    std::thread m_workerThread;

    UINT32 m_width = 1280;
    UINT32 m_height = 720;
    std::vector<BYTE> m_frameBuffer;
    std::mutex m_frameMutex;
    IWICImagingFactory* m_pWicFactory = nullptr;
};
