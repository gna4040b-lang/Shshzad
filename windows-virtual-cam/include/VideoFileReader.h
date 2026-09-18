#pragma once
#include "Common.h"

class VideoFileReader {
public:
    VideoFileReader();
    ~VideoFileReader();

    HRESULT OpenVideo(const std::wstring& filePath, UINT32 targetWidth, UINT32 targetHeight, GUID targetFormat);
    void Close();

    HRESULT ReadNextFrame(BYTE* pDestBuffer, DWORD destBufferSize, bool isLooping, bool& isEndOfStream);
    HRESULT SeekTo(LONGLONG time100ns);
    
    bool IsOpen() const { return m_pReader != nullptr; }
    LONGLONG GetDuration() const { return m_duration100ns; }
    UINT32 GetNativeWidth() const { return m_nativeWidth; }
    UINT32 GetNativeHeight() const { return m_nativeHeight; }
    std::wstring GetFilePath() const { return m_filePath; }

private:
    IMFSourceReader* m_pReader = nullptr;
    std::wstring m_filePath;
    LONGLONG m_duration100ns = 0;
    UINT32 m_nativeWidth = 0;
    UINT32 m_nativeHeight = 0;
    UINT32 m_targetWidth = 1280;
    UINT32 m_targetHeight = 720;
    GUID m_targetFormat = MFVideoFormat_RGB32;
    std::mutex m_readerMutex;
};
