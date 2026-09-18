# Cross-Platform Virtual Camera Solution (Android & Windows PC)

A legitimate, production-grade virtual camera solution spanning **native Android** and **Windows 10/11 PC**, built strictly according to official OS security and media framework specifications.

---

## 1. Technical Feasibility & Architectural Summary

### Android OS Analysis & Verification:
- **System-Wide Limitation**: On standard unrooted Android, third-party user apps operate inside an unprivileged app sandbox (`untrusted_app` SELinux domain). The Android Camera HAL (Hardware Abstraction Layer, `android.hardware.camera.provider` / `ICameraProvider`) is controlled exclusively by the privileged system daemon `cameraserver`. Standard user applications are only *consumers* of camera streams via `Camera2` / `CameraX`; no public or unprivileged API exists to register a system-wide virtual camera device into other third-party apps (e.g. Zoom, WhatsApp, banking apps).
- **Legitimate Working Alternative**:
  1. **Built-in High-Performance Local MJPEG/IP Stream Server**: Broadcasts the virtual camera feed over Wi-Fi / LAN / USB (via ADB port forwarding) with zero-latency multipart streaming, directly ingestible by PC video capture drivers, OBS Studio, and web clients.
  2. **In-App Video & Calibration Studio**: Allows picking prerecorded videos or generating SMPTE color-bar calibration patterns with live timecode, frame scrubbing, Play/Pause, Loop toggle, Resolution selection (480p, 720p, 1080p), and Framerate selection (15, 24, 30, 60 FPS).
  3. **Real-Time Camera HAL Audit**: Performs live queries on `CameraManager`, reporting all hardware levels, lenses, and SELinux enforcement status.

### Windows OS Implementation:
- **Real Media Foundation Virtual Camera**: Utilizes Microsoft's official **Media Foundation Virtual Camera API** (`MFCreateVirtualCamera` and `IMFMediaSource`), introduced in Windows 10 21H2 and Windows 11.
- **System-Wide Webcam Registration**: Registers as `"Virtual Camera MF Video Source"` in `KSCATEGORY_VIDEO_CAMERA`, recognized natively by Windows Camera, Microsoft Teams, OBS Studio, Zoom, and WebRTC browsers without kernel hooks.
- **Controls & Video Playback**: Full playback engine using `IMFSourceReader` supporting Play/Pause, Loop, Camera Power ON/OFF (switches to official Standby test card), resolution, and FPS adjustment.
- **Cross-Platform Bridge**: Ingests local video files OR connects live to the Android device's network stream over Wi-Fi or USB.

---

## 2. Android App Overview & Build Instructions

### Features:
- **Studio & Output**:
  - Video selection via Android Photo Picker (`ActivityResultContracts.PickVisualMedia`).
  - Real-time video preview monitor in 16:9 ratio.
  - Video playback timeline scrubber with timestamps.
  - Master Camera Power switch (ON = live video stream, OFF = standby test card).
  - Play / Pause and Loop toggle buttons.
  - Resolution selector: 480p (SD), 720p (HD), 1080p (FHD).
  - Framerate selector: 15, 24, 30, 60 FPS.
  - Embedded HTTP MJPEG Stream Server with one-tap start/stop, live client counter, and stream URL clipboard copy.
- **Android OS Audit Tab**:
  - Live device HAL inspection.
  - Hardware characteristics (LEGACY, LIMITED, FULL, LEVEL_3).
  - Factual OS architectural explanation and security policies.
- **Windows PC Bridge Tab**:
  - Step-by-step connection guide for pairing with the Windows driver.

### Building & Running Android:
```bash
# Compile and build debug APK
gradle :app:assembleDebug

# Run unit and Robolectric tests
gradle :app:testDebugUnitTest
```

---

## 3. Windows Virtual Camera Build Instructions

All Windows source files and build configurations are located in `/windows-virtual-cam/`.

### Prerequisites:
- Windows 10 (21H2+ / Build 19044) or Windows 11.
- Visual Studio 2022 with **C++ Desktop Development**.
- Windows 10/11 SDK (10.0.22000.0+).
- CMake 3.16+.

### Build Steps:
1. Open Command Prompt or PowerShell in `windows-virtual-cam/`.
2. Run the automated build script:
   ```cmd
   build_windows.bat
   ```
   *or with PowerShell:*
   ```powershell
   .\build_windows.ps1
   ```
3. The binaries will be generated in `windows-virtual-cam\build\Release\`:
   - `VirtualCameraController.exe` — Interactive Studio Controller
   - `VirtualCameraRegister.exe` — CLI Registration Utility
   - `VirtualCameraDirectShowFilter.dll` — DirectShow COM Filter

### Running the Windows Controller:
```cmd
.\build\Release\VirtualCameraController.exe
```
- Press `[1]` to select any video file (`.mp4`, `.mkv`, `.avi`).
- Press `[2]` to connect to the Android phone's stream (`http://<phone_ip>:8080/videostream`).
- Press `[3]` to start broadcasting virtual camera output.
- Open Windows Camera, Teams, or OBS and select **"Virtual Camera MF Video Source"**!

---

## 4. Repository Structure

```
├── app/                                 # Native Android Application (Kotlin + Jetpack Compose)
│   ├── src/main/java/com/example/
│   │   ├── audit/SystemCameraAuditor.kt # Hardware HAL audit & security analyzer
│   │   ├── engine/VirtualCameraEngine.kt# Frame generator, video decoder, timeline
│   │   ├── model/CameraModels.kt        # Resolution and audit data models
│   │   ├── service/HttpMjpegStreamServer.kt # Local HTTP MJPEG video stream server
│   │   ├── ui/VirtualCameraScreen.kt    # Jetpack Compose Studio interface
│   │   ├── ui/VirtualCameraViewModel.kt # MVI/MVVM ViewModel coordinator
│   │   └── MainActivity.kt              # Entry Activity
│   └── src/test/java/com/example/       # Robolectric & Screenshot unit tests
├── windows-virtual-cam/                 # Native Windows C++ Media Foundation Virtual Camera
│   ├── include/
│   │   ├── Common.h                     # Types, COM helpers, VideoConfig
│   │   ├── SyntheticPatternGenerator.h  # SMPTE bars & standby card generator
│   │   ├── VideoFileReader.h            # Media Foundation SourceReader decoder
│   │   ├── NetworkStreamReceiver.h      # HTTP MJPEG client using WinINet & WIC
│   │   ├── VirtualCameraStream.h        # IMFMediaStream implementation
│   │   ├── VirtualCameraMediaSource.h   # IMFMediaSource implementation
│   │   └── VirtualCameraManager.h       # MFCreateVirtualCamera management
│   ├── src/
│   │   ├── VirtualCameraController.cpp  # Interactive Windows Console & Win32 GUI
│   │   ├── register_driver.cpp          # Standalone driver registration tool
│   │   └── VirtualCameraDirectShowFilter.cpp # DirectShow COM in-process server
│   ├── CMakeLists.txt                   # Windows CMake configuration
│   ├── build_windows.bat                # Automated batch build script
│   ├── build_windows.ps1                # Automated PowerShell build script
│   └── README.md                        # Windows-specific documentation
├── metadata.json                        # AI Studio Platform Metadata
└── README.md                            # Comprehensive project documentation
```
