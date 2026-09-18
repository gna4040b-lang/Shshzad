#include "Common.h"
#include <olectl.h>

// DirectShow COM Registration and Virtual Video Source Class
// CLSID: {B683F204-6FA4-46AE-9486-E0D5505C7A92}
static const GUID CLSID_VirtualCameraDirectShowFilter = 
{ 0xb683f204, 0x6fa4, 0x46ae, { 0x94, 0x86, 0xe0, 0xd5, 0x50, 0x5c, 0x7a, 0x92 } };

extern "C" BOOL WINAPI DllMain(HINSTANCE hInstance, DWORD dwReason, LPVOID lpReserved) {
    return TRUE;
}

// Exported COM registration entry point for regsvr32
STDAPI DllRegisterServer() {
    HKEY hKey = NULL;
    LONG lRes = RegCreateKeyExW(
        HKEY_CLASSES_ROOT,
        L"CLSID\\{B683F204-6FA4-46AE-9486-E0D5505C7A92}",
        0, NULL, REG_OPTION_NON_VOLATILE, KEY_WRITE, NULL, &hKey, NULL
    );

    if (lRes == ERROR_SUCCESS) {
        const wchar_t* name = L"Virtual Camera Video Source";
        RegSetValueExW(hKey, NULL, 0, REG_SZ, (const BYTE*)name, (DWORD)(wcslen(name) + 1) * sizeof(wchar_t));
        RegCloseKey(hKey);
    }

    // Register under DirectShow Video Input Device Category
    HKEY hCatKey = NULL;
    lRes = RegCreateKeyExW(
        HKEY_CLASSES_ROOT,
        L"CLSID\\{860BB310-5D01-11d0-BD3B-00A0C911CE86}\\Instance\\{B683F204-6FA4-46AE-9486-E0D5505C7A92}",
        0, NULL, REG_OPTION_NON_VOLATILE, KEY_WRITE, NULL, &hCatKey, NULL
    );

    if (lRes == ERROR_SUCCESS) {
        const wchar_t* friendly = L"Virtual Camera Video Source (DirectShow)";
        const wchar_t* clsidStr = L"{B683F204-6FA4-46AE-9486-E0D5505C7A92}";
        RegSetValueExW(hCatKey, L"FriendlyName", 0, REG_SZ, (const BYTE*)friendly, (DWORD)(wcslen(friendly) + 1) * sizeof(wchar_t));
        RegSetValueExW(hCatKey, L"CLSID", 0, REG_SZ, (const BYTE*)clsidStr, (DWORD)(wcslen(clsidStr) + 1) * sizeof(wchar_t));
        RegCloseKey(hCatKey);
    }

    return S_OK;
}

STDAPI DllUnregisterServer() {
    RegDeleteKeyW(HKEY_CLASSES_ROOT, L"CLSID\\{860BB310-5D01-11d0-BD3B-00A0C911CE86}\\Instance\\{B683F204-6FA4-46AE-9486-E0D5505C7A92}");
    RegDeleteKeyW(HKEY_CLASSES_ROOT, L"CLSID\\{B683F204-6FA4-46AE-9486-E0D5505C7A92}");
    return S_OK;
}
