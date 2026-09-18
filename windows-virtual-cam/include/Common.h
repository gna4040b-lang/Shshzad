#pragma once

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif

#include <windows.h>
#include <mfapi.h>
#include <mfidl.h>
#include <mfreadwrite.h>
#include <mferror.h>
#include <mfvirtualcamera.h>
#include <shlwapi.h>
#include <strsafe.h>

#include <iostream>
#include <string>
#include <vector>
#include <memory>
#include <chrono>
#include <atomic>
#include <mutex>
#include <thread>

template <class T>
void SafeRelease(T** ppT) {
    if (ppT && *ppT) {
        (*ppT)->Release();
        *ppT = nullptr;
    }
}

#define CHECK_HR(hr, msg) \
    if (FAILED(hr)) { \
        std::wcerr << L"[ERROR] " << msg << L" (HRESULT: 0x" << std::hex << hr << std::dec << L")" << std::endl; \
        return hr; \
    }

#define CHECK_HR_VOID(hr, msg) \
    if (FAILED(hr)) { \
        std::wcerr << L"[ERROR] " << msg << L" (HRESULT: 0x" << std::hex << hr << std::dec << L")" << std::endl; \
        return; \
    }

struct VideoConfig {
    UINT32 width = 1280;
    UINT32 height = 720;
    UINT32 fpsNumerator = 30;
    UINT32 fpsDenominator = 1;
    GUID videoFormat = MFVideoFormat_RGB32; // Default to standard 32-bit ARGB
    bool isLooping = true;
    bool isVirtualCamOn = true; // true = active video, false = standby card
};
