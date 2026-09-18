#pragma once
#include "Common.h"
#include "VirtualCameraMediaSource.h"

class VirtualCameraManager {
public:
    VirtualCameraManager();
    ~VirtualCameraManager();

    HRESULT Initialize(const VideoConfig& initialConfig);
    HRESULT StartVirtualCamera(const std::wstring& friendlyName = L"Virtual Camera MF Video Source");
    HRESULT StopVirtualCamera();
    void Shutdown();

    // Source manipulation
    HRESULT SetVideoFile(const std::wstring& filePath);
    HRESULT SetNetworkStream(const std::wstring& url);
    void SetCameraPower(bool isOn);
    void SetLooping(bool isLooping);
    void SetPlayPause(bool isPlaying);
    void UpdateConfig(const VideoConfig& config);

    bool IsRunning() const { return m_isRunning; }
    VideoConfig GetCurrentConfig() const;
    std::wstring GetCurrentVideoPath() const;

private:
    std::mutex m_managerMutex;
    bool m_isRunning = false;
    bool m_isMfVirtualCameraSupported = false;

    VideoConfig m_config;
    VirtualCameraMediaSource* m_pMediaSource = nullptr;
    IMFVirtualCamera* m_pVirtualCamera = nullptr;
};
