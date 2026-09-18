#include "SyntheticPatternGenerator.h"
#include <cmath>

SyntheticPatternGenerator::SyntheticPatternGenerator() {}
SyntheticPatternGenerator::~SyntheticPatternGenerator() {}

HRESULT SyntheticPatternGenerator::FillFrame(
    BYTE* pBuffer,
    DWORD bufferSize,
    UINT32 width,
    UINT32 height,
    GUID format,
    bool isCameraOn,
    UINT64 frameIndex,
    LONGLONG timestamp100ns
) {
    if (!pBuffer) return E_POINTER;
    DWORD requiredSize = width * height * 4;
    if (bufferSize < requiredSize) return E_INVALIDARG;

    if (format == MFVideoFormat_RGB32) {
        if (isCameraOn) {
            DrawColorBarsRGB32(pBuffer, width, height, frameIndex, timestamp100ns);
        } else {
            DrawStandbyCardRGB32(pBuffer, width, height, timestamp100ns);
        }
        return S_OK;
    }

    // Default fallback: black frame
    memset(pBuffer, 0, bufferSize);
    return S_OK;
}

void SyntheticPatternGenerator::DrawColorBarsRGB32(
    BYTE* pBuffer,
    UINT32 width,
    UINT32 height,
    UINT64 frameIndex,
    LONGLONG timestamp100ns
) {
    // 7 standard 75% SMPTE color bars: White/Grey, Yellow, Cyan, Green, Magenta, Red, Blue
    const UINT32 colors[7] = {
        0x00C0C0C0, // Grey/White
        0x00C0C000, // Yellow
        0x0000C0C0, // Cyan
        0x0000C000, // Green
        0x00C000C0, // Magenta
        0x00C00000, // Red
        0x000000C0  // Blue
    };

    UINT32* pPixels = reinterpret_cast<UINT32*>(pBuffer);
    UINT32 barHeight = (height * 7) / 10;
    UINT32 barWidth = width / 7;

    for (UINT32 y = 0; y < height; ++y) {
        for (UINT32 x = 0; x < width; ++x) {
            UINT32 index = y * width + x;
            if (y < barHeight) {
                UINT32 barIndex = x / barWidth;
                if (barIndex >= 7) barIndex = 6;
                pPixels[index] = colors[barIndex];
            } else {
                // Bottom control section with dynamic animated indicator
                double cycle = (frameIndex % 60) / 60.0 * 2.0 * 3.14159265;
                UINT32 scanX = static_cast<UINT32>(((std::sin(cycle) + 1.0) / 2.0) * width);

                if (std::abs(static_cast<int>(x) - static_cast<int>(scanX)) < 4) {
                    pPixels[index] = 0x0022D3EE; // Electric Cyan scanline
                } else if ((x / 32 + y / 32) % 2 == 0) {
                    pPixels[index] = 0x001E293B; // Slate dark checkerboard
                } else {
                    pPixels[index] = 0x000F172A;
                }
            }
        }
    }
}

void SyntheticPatternGenerator::DrawStandbyCardRGB32(
    BYTE* pBuffer,
    UINT32 width,
    UINT32 height,
    LONGLONG timestamp100ns
) {
    UINT32* pPixels = reinterpret_cast<UINT32*>(pBuffer);

    for (UINT32 y = 0; y < height; ++y) {
        for (UINT32 x = 0; x < width; ++x) {
            UINT32 index = y * width + x;
            // Draw standby amber/dark slate border card
            if (x < 12 || x >= width - 12 || y < 12 || y >= height - 12) {
                pPixels[index] = 0x00EF4444; // Warning red border
            } else if (x > width / 4 && x < (width * 3) / 4 && y > height / 3 && y < (height * 2) / 3) {
                pPixels[index] = 0x00334155; // Inner banner
            } else {
                pPixels[index] = 0x000F172A; // Dark background
            }
        }
    }
}
