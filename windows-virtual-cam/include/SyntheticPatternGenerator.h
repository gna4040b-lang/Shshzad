#pragma once
#include "Common.h"

class SyntheticPatternGenerator {
public:
    SyntheticPatternGenerator();
    ~SyntheticPatternGenerator();

    HRESULT FillFrame(
        BYTE* pBuffer,
        DWORD bufferSize,
        UINT32 width,
        UINT32 height,
        GUID format,
        bool isCameraOn,
        UINT64 frameIndex,
        LONGLONG timestamp100ns
    );

private:
    void DrawColorBarsRGB32(BYTE* pBuffer, UINT32 width, UINT32 height, UINT64 frameIndex, LONGLONG timestamp100ns);
    void DrawStandbyCardRGB32(BYTE* pBuffer, UINT32 width, UINT32 height, LONGLONG timestamp100ns);
};
