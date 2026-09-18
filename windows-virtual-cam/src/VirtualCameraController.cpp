#include "Common.h"
#include "VirtualCameraManager.h"
#include <commdlg.h>
#include <conio.h>
#include <iomanip>

void PrintBanner() {
    std::wcout << L"================================================================" << std::endl;
    std::wcout << L"       WINDOWS MEDIA FOUNDATION VIRTUAL CAMERA STUDIO          " << std::endl;
    std::wcout << L"================================================================" << std::endl;
}

std::wstring OpenFileDialog() {
    wchar_t filename[MAX_PATH] = { 0 };
    OPENFILENAMEW ofn = { 0 };
    ofn.lStructSize = sizeof(ofn);
    ofn.lpstrFilter = L"Video Files (*.mp4;*.mkv;*.avi;*.wmv;*.mov)\0*.mp4;*.mkv;*.avi;*.wmv;*.mov\0All Files (*.*)\0*.*\0";
    ofn.lpstrFile = filename;
    ofn.nMaxFile = MAX_PATH;
    ofn.Flags = OFN_FILEMUSTEXIST | OFN_PATHMUSTEXIST;
    ofn.lpstrTitle = L"Select Prerecorded Video for Virtual Camera";

    if (GetOpenFileNameW(&ofn)) {
        return std::wstring(filename);
    }
    return L"";
}

void PrintStatus(VirtualCameraManager& manager, const std::wstring& currentSource, bool isPlaying) {
    VideoConfig cfg = manager.GetCurrentConfig();
    std::wcout << L"\n---------------------- CAMERA STATUS --------------------------" << std::endl;
    std::wcout << L" Virtual Driver : " << (manager.IsRunning() ? L"[ACTIVE BROADCASTING]" : L"[STOPPED / IDLE]") << std::endl;
    std::wcout << L" Device Name    : \"Virtual Camera MF Video Source\"" << std::endl;
    std::wcout << L" Input Source   : " << (currentSource.empty() ? L"Studio Calibration Pattern (Default)" : currentSource) << std::endl;
    std::wcout << L" Power Switch   : " << (cfg.isVirtualCamOn ? L"ON (Video Feed)" : L"OFF (Standby Pattern)") << std::endl;
    std::wcout << L" Playback State : " << (isPlaying ? L"PLAYING" : L"PAUSED") << std::endl;
    std::wcout << L" Loop Mode      : " << (cfg.isLooping ? L"LOOPING ENABLED" : L"SINGLE PLAY") << std::endl;
    std::wcout << L" Format Output  : " << cfg.width << L"x" << cfg.height << L" @ " << cfg.fpsNumerator << L" FPS" << std::endl;
    std::wcout << L"---------------------------------------------------------------" << std::endl;
    std::wcout << L" [1] Select Video File (MP4/MKV/AVI)" << std::endl;
    std::wcout << L" [2] Connect to Android Phone Stream (HTTP MJPEG)" << std::endl;
    std::wcout << L" [3] Start Virtual Camera Output" << std::endl;
    std::wcout << L" [4] Stop Virtual Camera Output" << std::endl;
    std::wcout << L" [5] Toggle Play / Pause" << std::endl;
    std::wcout << L" [6] Toggle Loop (ON/OFF)" << std::endl;
    std::wcout << L" [7] Toggle Power (ON / Standby)" << std::endl;
    std::wcout << L" [8] Change Resolution (480p / 720p / 1080p)" << std::endl;
    std::wcout << L" [9] Change Framerate (15 / 24 / 30 / 60 FPS)" << std::endl;
    std::wcout << L" [0] Exit Application" << std::endl;
    std::wcout << L"Select option (0-9): ";
}

int wmain(int argc, wchar_t* argv[]) {
    CoInitializeEx(NULL, COINIT_MULTITHREADED);
    PrintBanner();

    VideoConfig config;
    config.width = 1280;
    config.height = 720;
    config.fpsNumerator = 30;
    config.fpsDenominator = 1;
    config.isLooping = true;
    config.isVirtualCamOn = true;

    VirtualCameraManager manager;
    if (FAILED(manager.Initialize(config))) {
        std::wcerr << L"Failed to initialize Media Foundation Virtual Camera subsystem." << std::endl;
        CoUninitialize();
        return 1;
    }

    std::wstring currentSource = L"";
    bool isPlaying = true;

    // Check command-line arguments
    for (int i = 1; i < argc; ++i) {
        std::wstring arg = argv[i];
        if ((arg == L"--video" || arg == L"-v") && i + 1 < argc) {
            currentSource = argv[++i];
            manager.SetVideoFile(currentSource);
        } else if ((arg == L"--network" || arg == L"-n") && i + 1 < argc) {
            currentSource = argv[++i];
            manager.SetNetworkStream(currentSource);
        } else if (arg == L"--start") {
            manager.StartVirtualCamera();
        }
    }

    bool running = true;
    while (running) {
        PrintStatus(manager, currentSource, isPlaying);
        wchar_t choice = _getwch();
        std::wcout << choice << std::endl;

        switch (choice) {
            case L'1': {
                std::wstring selected = OpenFileDialog();
                if (!selected.empty()) {
                    currentSource = selected;
                    manager.SetVideoFile(selected);
                    std::wcout << L"\n[OK] Loaded Video: " << selected << std::endl;
                }
                break;
            }
            case L'2': {
                std::wcout << L"\nEnter Android Phone Stream URL (e.g. http://192.168.1.50:8080/videostream or http://localhost:8080/videostream): ";
                std::wstring streamUrl;
                std::wcin >> streamUrl;
                if (!streamUrl.empty()) {
                    currentSource = streamUrl;
                    manager.SetNetworkStream(streamUrl);
                    std::wcout << L"\n[OK] Connected to Network Stream: " << streamUrl << std::endl;
                }
                break;
            }
            case L'3': {
                manager.StartVirtualCamera();
                std::wcout << L"\n[OK] Virtual Camera Started. Ready in Windows Camera, Teams, OBS, Zoom." << std::endl;
                break;
            }
            case L'4': {
                manager.StopVirtualCamera();
                std::wcout << L"\n[OK] Virtual Camera Stopped." << std::endl;
                break;
            }
            case L'5': {
                isPlaying = !isPlaying;
                manager.SetPlayPause(isPlaying);
                std::wcout << L"\n[OK] Playback state changed to: " << (isPlaying ? L"PLAYING" : L"PAUSED") << std::endl;
                break;
            }
            case L'6': {
                config.isLooping = !config.isLooping;
                manager.SetLooping(config.isLooping);
                std::wcout << L"\n[OK] Looping state: " << (config.isLooping ? L"ENABLED" : L"DISABLED") << std::endl;
                break;
            }
            case L'7': {
                config.isVirtualCamOn = !config.isVirtualCamOn;
                manager.SetCameraPower(config.isVirtualCamOn);
                std::wcout << L"\n[OK] Camera Power: " << (config.isVirtualCamOn ? L"ON (Video)" : L"OFF (Standby Card)") << std::endl;
                break;
            }
            case L'8': {
                std::wcout << L"\nChoose Resolution: [1] 480p (640x480)  [2] 720p (1280x720)  [3] 1080p (1920x1080): ";
                wchar_t rChoice = _getwch();
                std::wcout << rChoice << std::endl;
                if (rChoice == L'1') {
                    config.width = 640; config.height = 480;
                } else if (rChoice == L'2') {
                    config.width = 1280; config.height = 720;
                } else if (rChoice == L'3') {
                    config.width = 1920; config.height = 1080;
                }
                manager.UpdateConfig(config);
                if (!currentSource.empty() && currentSource.find(L"http") != 0) {
                    manager.SetVideoFile(currentSource);
                }
                std::wcout << L"[OK] Resolution set to " << config.width << L"x" << config.height << std::endl;
                break;
            }
            case L'9': {
                std::wcout << L"\nChoose Framerate: [1] 15 FPS  [2] 24 FPS  [3] 30 FPS  [4] 60 FPS: ";
                wchar_t fChoice = _getwch();
                std::wcout << fChoice << std::endl;
                if (fChoice == L'1') config.fpsNumerator = 15;
                else if (fChoice == L'2') config.fpsNumerator = 24;
                else if (fChoice == L'3') config.fpsNumerator = 30;
                else if (fChoice == L'4') config.fpsNumerator = 60;
                manager.UpdateConfig(config);
                std::wcout << L"[OK] Framerate set to " << config.fpsNumerator << L" FPS" << std::endl;
                break;
            }
            case L'0':
            case 27: // ESC
                running = false;
                break;
            default:
                break;
        }
    }

    manager.Shutdown();
    CoUninitialize();
    std::wcout << L"Virtual Camera Controller closed cleanly." << std::endl;
    return 0;
}
