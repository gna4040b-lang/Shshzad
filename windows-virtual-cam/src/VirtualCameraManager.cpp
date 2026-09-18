#include "VirtualCameraManager.h"

// Function signature for dynamic resolution of MFCreateVirtualCamera from mfplat.dll
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

VirtualCameraManager::VirtualCameraManager() {}

VirtualCameraManager::~VirtualCameraManager() {
    Shutdown();
}

HRESULT VirtualCameraManager::Initialize(const VideoConfig& initialConfig) {
    std::lock_guard<std::mutex> lock(m_managerMutex);
    m_config = initialConfig;

    HRESULT hr = MFStartup(MF_VERSION);
    if (FAILED(hr)) {
        std::wcerr << L"[VirtualCameraManager] MFStartup failed: 0x" << std::hex << hr << std::dec << std::endl;
        return hr;
    }

    // Create the media source instance
    hr = VirtualCameraMediaSource::CreateInstance(m_config, &m_pMediaSource);
    if (FAILED(hr)) {
        std::wcerr << L"[VirtualCameraManager] Failed to create media source instance: 0x" << std::hex << hr << std::dec << std::endl;
        return hr;
    }

    return S_OK;
}

HRESULT VirtualCameraManager::StartVirtualCamera(const std::wstring& friendlyName) {
    std::lock_guard<std::mutex> lock(m_managerMutex);
    if (m_isRunning) return S_OK;
    if (!m_pMediaSource) return E_FAIL;

    // Load MFCreateVirtualCamera from mfplat.dll
    HMODULE hMfPlat = GetModuleHandleW(L"mfplat.dll");
    if (!hMfPlat) {
        hMfPlat = LoadLibraryW(L"mfplat.dll");
    }

    if (hMfPlat) {
        auto pfnCreateVC = reinterpret_cast<PFN_MFCreateVirtualCamera>(
            GetProcAddress(hMfPlat, "MFCreateVirtualCamera")
        );

        if (pfnCreateVC) {
            m_isMfVirtualCameraSupported = true;
            const GUID categories[] = { KSCATEGORY_VIDEO_CAMERA };

            HRESULT hr = pfnCreateVC(
                MFVirtualCameraType_SoftwareCameraSource,
                MFVirtualCameraLifetime_Session,
                MFVirtualCameraAccess_CurrentUser,
                friendlyName.c_str(),
                L"{C342898B-933B-49E8-9DC2-E7A47B2E58C1}",
                categories,
                1,
                &m_pVirtualCamera
            );

            if (SUCCEEDED(hr) && m_pVirtualCamera) {
                // Register and start the virtual camera device
                hr = m_pVirtualCamera->Start(nullptr);
                if (SUCCEEDED(hr)) {
                    std::wcout << L"[VirtualCameraManager] Media Foundation Virtual Camera successfully started: \""
                               << friendlyName << L"\"" << std::endl;
                    std::wcout << L"                      Registered with Windows Camera Subsystem." << std::endl;
                } else {
                    std::wcerr << L"[VirtualCameraManager] Virtual camera Start returned: 0x"
                               << std::hex << hr << std::dec << std::endl;
                }
            } else {
                std::wcout << L"[VirtualCameraManager] MFCreateVirtualCamera returned: 0x"
                           << std::hex << hr << std::dec
                           << L" (Operating in Local Direct COM / Media Foundation Source mode)." << std::endl;
            }
        }
    }

    // Start playback on media source
    PROPVARIANT var;
    PropVariantInit(&var);
    var.vt = VT_I8;
    var.hVal.QuadPart = 0;
    m_pMediaSource->Start(nullptr, nullptr, &var);
    PropVariantClear(&var);

    m_isRunning = true;
    return S_OK;
}

HRESULT VirtualCameraManager::StopVirtualCamera() {
    std::lock_guard<std::mutex> lock(m_managerMutex);
    if (!m_isRunning) return S_OK;

    if (m_pVirtualCamera) {
        m_pVirtualCamera->Stop();
        SafeRelease(&m_pVirtualCamera);
    }

    if (m_pMediaSource) {
        m_pMediaSource->Stop();
    }

    m_isRunning = false;
    std::wcout << L"[VirtualCameraManager] Virtual Camera stopped." << std::endl;
    return S_OK;
}

void VirtualCameraManager::Shutdown() {
    StopVirtualCamera();
    std::lock_guard<std::mutex> lock(m_managerMutex);
    if (m_pMediaSource) {
        m_pMediaSource->Shutdown();
        SafeRelease(&m_pMediaSource);
    }
    MFShutdown();
}

HRESULT VirtualCameraManager::SetVideoFile(const std::wstring& filePath) {
    std::lock_guard<std::mutex> lock(m_managerMutex);
    if (!m_pMediaSource || !m_pMediaSource->GetStream()) return E_FAIL;
    m_pMediaSource->GetStream()->SetVideoFile(filePath);
    return S_OK;
}

HRESULT VirtualCameraManager::SetNetworkStream(const std::wstring& url) {
    std::lock_guard<std::mutex> lock(m_managerMutex);
    if (!m_pMediaSource || !m_pMediaSource->GetStream()) return E_FAIL;
    m_pMediaSource->GetStream()->SetNetworkStream(url);
    return S_OK;
}

void VirtualCameraManager::SetCameraPower(bool isOn) {
    std::lock_guard<std::mutex> lock(m_managerMutex);
    m_config.isVirtualCamOn = isOn;
    if (m_pMediaSource && m_pMediaSource->GetStream()) {
        m_pMediaSource->GetStream()->SetConfig(m_config);
    }
}

void VirtualCameraManager::SetLooping(bool isLooping) {
    std::lock_guard<std::mutex> lock(m_managerMutex);
    m_config.isLooping = isLooping;
    if (m_pMediaSource && m_pMediaSource->GetStream()) {
        m_pMediaSource->GetStream()->SetConfig(m_config);
    }
}

void VirtualCameraManager::SetPlayPause(bool isPlaying) {
    std::lock_guard<std::mutex> lock(m_managerMutex);
    if (!m_pMediaSource || !m_pMediaSource->GetStream()) return;
    if (isPlaying) {
        m_pMediaSource->GetStream()->Start(-1);
    } else {
        m_pMediaSource->GetStream()->Pause();
    }
}

void VirtualCameraManager::UpdateConfig(const VideoConfig& config) {
    std::lock_guard<std::mutex> lock(m_managerMutex);
    m_config = config;
    if (m_pMediaSource && m_pMediaSource->GetStream()) {
        m_pMediaSource->GetStream()->SetConfig(m_config);
    }
}

VideoConfig VirtualCameraManager::GetCurrentConfig() const {
    return m_config;
}
