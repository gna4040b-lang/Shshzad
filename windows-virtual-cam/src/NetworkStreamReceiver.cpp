#include "NetworkStreamReceiver.h"
#include <wininet.h>

NetworkStreamReceiver::NetworkStreamReceiver() {
    CoInitializeEx(NULL, COINIT_MULTITHREADED);
    CoCreateInstance(
        CLSID_WICImagingFactory,
        NULL,
        CLSCTX_INPROC_SERVER,
        IID_PPV_ARGS(&m_pWicFactory)
    );
}

NetworkStreamReceiver::~NetworkStreamReceiver() {
    Stop();
    SafeRelease(&m_pWicFactory);
}

bool NetworkStreamReceiver::Start(const std::wstring& url, UINT32 targetWidth, UINT32 targetHeight) {
    Stop();
    m_width = targetWidth;
    m_height = targetHeight;
    m_frameBuffer.resize(targetWidth * targetHeight * 4, 0);

    m_isRunning = true;
    m_workerThread = std::thread(&NetworkStreamReceiver::WorkerLoop, this, url);
    return true;
}

void NetworkStreamReceiver::Stop() {
    m_isRunning = false;
    m_isConnected = false;
    if (m_workerThread.joinable()) {
        m_workerThread.join();
    }
}

bool NetworkStreamReceiver::CopyLatestFrame(BYTE* pDestBuffer, DWORD destBufferSize) {
    if (!m_isConnected) return false;
    std::lock_guard<std::mutex> lock(m_frameMutex);
    DWORD copySize = (destBufferSize < m_frameBuffer.size()) ? destBufferSize : static_cast<DWORD>(m_frameBuffer.size());
    memcpy(pDestBuffer, m_frameBuffer.data(), copySize);
    return true;
}

void NetworkStreamReceiver::WorkerLoop(const std::wstring& url) {
    HINTERNET hInternet = InternetOpenW(L"VirtualCameraBridge/1.0", INTERNET_OPEN_TYPE_PRECONFIG, NULL, NULL, 0);
    if (!hInternet) return;

    HINTERNET hFile = InternetOpenUrlW(hInternet, url.c_str(), NULL, 0, INTERNET_FLAG_RELOAD | INTERNET_FLAG_NO_CACHE_WRITE, 0);
    if (!hFile) {
        InternetCloseHandle(hInternet);
        return;
    }

    m_isConnected = true;
    std::vector<BYTE> readChunk(64 * 1024);
    std::vector<BYTE> mjpegBuffer;

    while (m_isRunning) {
        DWORD bytesRead = 0;
        if (!InternetReadFile(hFile, readChunk.data(), static_cast<DWORD>(readChunk.size()), &bytesRead) || bytesRead == 0) {
            std::this_thread::sleep_for(std::chrono::milliseconds(20));
            continue;
        }

        mjpegBuffer.insert(mjpegBuffer.end(), readChunk.begin(), readChunk.begin() + bytesRead);

        // Scan for JPEG SOI (0xFF 0xD8) and EOI (0xFF 0xD9)
        size_t soi = std::string::npos;
        size_t eoi = std::string::npos;

        for (size_t i = 0; i + 1 < mjpegBuffer.size(); ++i) {
            if (mjpegBuffer[i] == 0xFF && mjpegBuffer[i + 1] == 0xD8 && soi == std::string::npos) {
                soi = i;
            }
            if (mjpegBuffer[i] == 0xFF && mjpegBuffer[i + 1] == 0xD9 && soi != std::string::npos) {
                eoi = i + 2;
                break;
            }
        }

        if (soi != std::string::npos && eoi != std::string::npos && eoi > soi) {
            // Found complete JPEG frame in stream
            if (m_pWicFactory) {
                IWICStream* pStream = nullptr;
                if (SUCCEEDED(m_pWicFactory->CreateStream(&pStream))) {
                    pStream->InitializeFromMemory(mjpegBuffer.data() + soi, static_cast<DWORD>(eoi - soi));

                    IWICBitmapDecoder* pDecoder = nullptr;
                    if (SUCCEEDED(m_pWicFactory->CreateDecoderFromStream(pStream, NULL, WICDecodeMetadataCacheOnDemand, &pDecoder))) {
                        IWICBitmapFrameDecode* pFrame = nullptr;
                        if (SUCCEEDED(pDecoder->GetFrame(0, &pFrame))) {
                            IWICFormatConverter* pConverter = nullptr;
                            if (SUCCEEDED(m_pWicFactory->CreateFormatConverter(&pConverter))) {
                                pConverter->Initialize(
                                    pFrame,
                                    GUID_WICPixelFormat32bppBGRA,
                                    WICBitmapDitherTypeNone,
                                    NULL,
                                    0.0,
                                    WICBitmapPaletteTypeCustom
                                );

                                std::lock_guard<std::mutex> lock(m_frameMutex);
                                pConverter->CopyPixels(
                                    NULL,
                                    m_width * 4,
                                    static_cast<UINT>(m_frameBuffer.size()),
                                    m_frameBuffer.data()
                                );
                                SafeRelease(&pConverter);
                            }
                            SafeRelease(&pFrame);
                        }
                        SafeRelease(&pDecoder);
                    }
                    SafeRelease(&pStream);
                }
            }
            // Trim processed buffer
            mjpegBuffer.erase(mjpegBuffer.begin(), mjpegBuffer.begin() + eoi);
        }

        if (mjpegBuffer.size() > 512 * 1024) {
            mjpegBuffer.clear(); // Safety cap against memory bloat
        }
    }

    m_isConnected = false;
    InternetCloseHandle(hFile);
    InternetCloseHandle(hInternet);
}
