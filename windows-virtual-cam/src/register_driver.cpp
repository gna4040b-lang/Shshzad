#include "Common.h"
#include <iostream>

typedef HRESULT (STDAPICALLTYPE *PFN_MFCreateVirtualCamera)(
    MFVirtualCameraType type,
    MFVirtualCameraLifetime lifetime,
    MFVirtualCameraAccess access,
    LPCWSTR friendlyName,
    LPCWSTR sourceId,
    const GUID* categories,
    ULONG categoryCount,
    IMFVirtualCamera** ppVirtualCamera
);

int wmain(int argc, wchar_t* argv[]) {
    CoInitializeEx(NULL, COINIT_MULTITHREADED);
    MFStartup(MF_VERSION);

    std::wcout << L"Media Foundation Virtual Camera Registration Utility" << std::endl;

    bool unregister = false;
    if (argc > 1 && wcscmp(argv[1], L"--unregister") == 0) {
        unregister = true;
    }

    HMODULE hMfPlat = LoadLibraryW(L"mfplat.dll");
    if (!hMfPlat) {
        std::wcerr << L"Failed to load mfplat.dll" << std::endl;
        return 1;
    }

    auto pfnCreate = (PFN_MFCreateVirtualCamera)GetProcAddress(hMfPlat, "MFCreateVirtualCamera");
    if (!pfnCreate) {
        std::wcerr << L"MFCreateVirtualCamera is not available on this version of Windows (requires Windows 10 21H2+ or Windows 11)." << std::endl;
        return 1;
    }

    const GUID categories[] = { KSCATEGORY_VIDEO_CAMERA };
    IMFVirtualCamera* pCam = nullptr;

    HRESULT hr = pfnCreate(
        MFVirtualCameraType_SoftwareCameraSource,
        MFVirtualCameraLifetime_Session,
        MFVirtualCameraAccess_CurrentUser,
        L"Virtual Camera MF Video Source",
        L"{C342898B-933B-49E8-9DC2-E7A47B2E58C1}",
        categories,
        1,
        &pCam
    );

    if (SUCCEEDED(hr) && pCam) {
        if (unregister) {
            pCam->Stop();
            pCam->Remove();
            std::wcout << L"Virtual Camera unregistered successfully." << std::endl;
        } else {
            pCam->Start(nullptr);
            std::wcout << L"Virtual Camera registered and active!" << std::endl;
        }
        pCam->Release();
    } else {
        std::wcerr << L"Registration failed with HRESULT: 0x" << std::hex << hr << std::dec << std::endl;
    }

    MFShutdown();
    CoUninitialize();
    return 0;
}
