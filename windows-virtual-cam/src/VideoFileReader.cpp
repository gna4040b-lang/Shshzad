#include "VideoFileReader.h"
#include <propvarutil.h>

VideoFileReader::VideoFileReader() {}

VideoFileReader::~VideoFileReader() {
    Close();
}

void VideoFileReader::Close() {
    std::lock_guard<std::mutex> lock(m_readerMutex);
    SafeRelease(&m_pReader);
    m_filePath.clear();
    m_duration100ns = 0;
    m_nativeWidth = 0;
    m_nativeHeight = 0;
}

HRESULT VideoFileReader::OpenVideo(
    const std::wstring& filePath,
    UINT32 targetWidth,
    UINT32 targetHeight,
    GUID targetFormat
) {
    std::lock_guard<std::mutex> lock(m_readerMutex);
    Close();

    m_targetWidth = targetWidth;
    m_targetHeight = targetHeight;
    m_targetFormat = targetFormat;
    m_filePath = filePath;

    IMFAttributes* pAttributes = nullptr;
    HRESULT hr = MFCreateAttributes(&pAttributes, 2);
    if (SUCCEEDED(hr)) {
        // Enable video processing and hardware transforms
        pAttributes->SetUINT32(MF_READWRITE_ENABLE_HARDWARE_TRANSFORMS, TRUE);
        pAttributes->SetUINT32(MF_SOURCE_READER_ENABLE_VIDEO_PROCESSING, TRUE);
    }

    hr = MFCreateSourceReaderFromURL(filePath.c_str(), pAttributes, &m_pReader);
    SafeRelease(&pAttributes);
    if (FAILED(hr)) {
        std::wcerr << L"[VideoFileReader] Failed to create source reader from URL: " << filePath << std::endl;
        return hr;
    }

    // Deselect all streams except the primary video stream
    hr = m_pReader->SetStreamSelection(MF_SOURCE_READER_ALL_STREAMS, FALSE);
    hr = m_pReader->SetStreamSelection(MF_SOURCE_READER_FIRST_VIDEO_STREAM, TRUE);

    // Query native video format
    IMFMediaType* pNativeType = nullptr;
    hr = m_pReader->GetNativeMediaType(MF_SOURCE_READER_FIRST_VIDEO_STREAM, 0, &pNativeType);
    if (SUCCEEDED(hr)) {
        MFGetAttributeSize(pNativeType, MF_MT_FRAME_SIZE, &m_nativeWidth, &m_nativeHeight);
        SafeRelease(&pNativeType);
    }

    // Query duration
    PROPVARIANT var;
    PropVariantInit(&var);
    hr = m_pReader->GetPresentationAttribute(MF_SOURCE_READER_MEDIASOURCE, MF_PD_DURATION, &var);
    if (SUCCEEDED(hr) && var.vt == VT_UI8) {
        m_duration100ns = var.uhVal.QuadPart;
    }
    PropVariantClear(&var);

    // Configure desired output type: RGB32 / NV12 at target resolution
    IMFMediaType* pDesiredType = nullptr;
    hr = MFCreateMediaType(&pDesiredType);
    if (SUCCEEDED(hr)) {
        pDesiredType->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Video);
        pDesiredType->SetGUID(MF_MT_SUBTYPE, targetFormat);
        MFSetAttributeSize(pDesiredType, MF_MT_FRAME_SIZE, targetWidth, targetHeight);

        hr = m_pReader->SetCurrentMediaType(MF_SOURCE_READER_FIRST_VIDEO_STREAM, NULL, pDesiredType);
        SafeRelease(&pDesiredType);
    }

    if (FAILED(hr)) {
        std::wcerr << L"[VideoFileReader] Failed to set output media type" << std::endl;
        Close();
        return hr;
    }

    std::wcout << L"[VideoFileReader] Successfully loaded video: " << filePath
               << L" (" << m_nativeWidth << L"x" << m_nativeHeight
               << L" -> Target: " << targetWidth << L"x" << targetHeight << L")" << std::endl;

    return S_OK;
}

HRESULT VideoFileReader::ReadNextFrame(
    BYTE* pDestBuffer,
    DWORD destBufferSize,
    bool isLooping,
    bool& isEndOfStream
) {
    std::lock_guard<std::mutex> lock(m_readerMutex);
    isEndOfStream = false;
    if (!m_pReader) return E_FAIL;

    DWORD streamIndex = 0;
    DWORD streamFlags = 0;
    LONGLONG timestamp = 0;
    IMFSample* pSample = nullptr;

    HRESULT hr = m_pReader->ReadSample(
        MF_SOURCE_READER_FIRST_VIDEO_STREAM,
        0,
        &streamIndex,
        &streamFlags,
        &timestamp,
        &pSample
    );

    if (FAILED(hr)) return hr;

    if (streamFlags & MF_SOURCE_READERF_ENDOFSTREAM) {
        isEndOfStream = true;
        SafeRelease(&pSample);
        if (isLooping) {
            SeekTo(0);
        }
        return S_FALSE;
    }

    if (!pSample) {
        return S_FALSE; // Stream tick or gap
    }

    // Lock buffer from sample and copy
    IMFMediaBuffer* pBuffer = nullptr;
    hr = pSample->ConvertToContiguousBuffer(&pBuffer);
    if (SUCCEEDED(hr)) {
        BYTE* pSrcData = nullptr;
        DWORD srcLength = 0;
        hr = pBuffer->Lock(&pSrcData, nullptr, &srcLength);
        if (SUCCEEDED(hr)) {
            DWORD copySize = (destBufferSize < srcLength) ? destBufferSize : srcLength;
            memcpy(pDestBuffer, pSrcData, copySize);
            pBuffer->Unlock();
        }
        SafeRelease(&pBuffer);
    }

    SafeRelease(&pSample);
    return hr;
}

HRESULT VideoFileReader::SeekTo(LONGLONG time100ns) {
    if (!m_pReader) return E_FAIL;
    PROPVARIANT var;
    PropVariantInit(&var);
    var.vt = VT_I8;
    var.hVal.QuadPart = time100ns;
    HRESULT hr = m_pReader->SetCurrentPosition(GUID_NULL, var);
    PropVariantClear(&var);
    return hr;
}
