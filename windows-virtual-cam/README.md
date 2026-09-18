# Windows Media Foundation Virtual Camera Solution

This directory contains the production-ready C++ source code, build scripts, and driver implementation for the **Windows Virtual Camera**.

It utilizes the legitimate Microsoft **Windows Media Foundation (MF) Virtual Camera API** (`MFCreateVirtualCamera` and `IMFMediaSource`), introduced in Windows 10 (21H2 Build 19044+) and Windows 11, with complementary DirectShow COM registration for maximum legacy app compatibility.

---

## 1. Architectural Architecture & Legitimate OS APIs

Unlike intrusive kernel hooks or driver modifications, this implementation strictly adheres to Microsoft's supported OS architecture:

```
+-------------------------------------------------------------------------+
|                  Third-Party Windows Applications                       |
|   (Microsoft Teams, Zoom, OBS Studio, Chrome/Edge WebRTC, Windows Cam)  |
+-------------------------------------------------------------------------+
                                   ▲
                                   │ Queries MFEnumDeviceSources / DirectShow
                                   ▼
+-------------------------------------------------------------------------+
|                Windows Media Foundation Subsystem (mfplat.dll)          |
|    - Device Name: "Virtual Camera MF Video Source"                      |
|    - Category: KSCATEGORY_VIDEO_CAMERA                                  |
|    - Interface: MFVirtualCameraType_SoftwareCameraSource                |
+-------------------------------------------------------------------------+
                                   ▲
                                   │ IMFSample / IMFMediaStream
                                   ▼
+-------------------------------------------------------------------------+
|             VirtualCameraMediaSource / VirtualCameraStream              |
|                                                                         |
|  [Input Mode A: Prerecorded Video]   |  [Input Mode B: Android Live]    |
|   - IMFSourceReader (MP4/MKV/AVI)    |   - WinINet MJPEG Stream Receiver|
|   - Play / Pause / Seek / Loop       |   - Connects to Android Phone IP |
|   - Standby Calibration Generator    |     or USB Tether (adb forward)  |
+-------------------------------------------------------------------------+
```

### Key Capabilities
- **Genuine Webcam Registration**: Registers directly with Windows Media Foundation and appears as `VirtualCamera MF Video Source` across all standard camera selection menus.
- **Prerecorded Video Selection**: Opens any video file (`.mp4`, `.mkv`, `.avi`, `.wmv`, `.mov`) via Windows Media Foundation Source Reader.
- **Comprehensive Controls**:
  - **Play / Pause**: Freeze or resume video stream in real time.
  - **Loop Toggle**: Seamlessly repeat video upon reaching End-Of-File (EOF).
  - **Power ON / OFF**: Switch instantly between active video output and an official Standby / Muted test card.
  - **Resolution Settings**: 1080p (1920x1080), 720p (1280x720), 480p (640x480).
  - **FPS Settings**: 15, 24, 30, 60 frames per second.
- **Android Phone Companion Mode**: Receives wireless or wired USB video from the companion Android Virtual Camera Studio app.

---

## 2. Prerequisites & Dependencies

To build the Windows solution, ensure your workstation has:
1. **Operating System**: Windows 10 (Version 21H2, Build 19044 or newer) or Windows 11.
2. **Compiler**: Visual Studio 2022 (Community, Professional, or Enterprise) with the **"Desktop development with C++"** workload installed.
3. **Windows SDK**: Windows 10/11 SDK (Version `10.0.22000.0` or higher) containing `<mfvirtualcamera.h>`.
4. **Build System**: CMake (version 3.16 or higher).

---

## 3. Quick Build Instructions

### Option A: Using the Automated Batch Script
Open Command Prompt in this folder and execute:
```cmd
build_windows.bat
```

### Option B: Using PowerShell
Open PowerShell in this folder and execute:
```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\build_windows.ps1
```

### Option C: Manual CMake Build
```cmd
mkdir build
cd build
cmake -G "Visual Studio 17 2022" -A x64 ..
cmake --build . --config Release --parallel
```

The compiled binaries will be placed in `build\Release\`:
- `VirtualCameraController.exe` — Interactive Desktop Studio Controller
- `VirtualCameraRegister.exe` — Standalone Virtual Camera Driver Registration Utility
- `VirtualCameraDirectShowFilter.dll` — DirectShow COM In-Process Server

---

## 4. How to Use the Windows Virtual Camera

### Step 1: Launch the Controller
Run `VirtualCameraController.exe` (Run as Administrator on first launch to allow Windows to register the virtual device):
```cmd
.\build\Release\VirtualCameraController.exe
```

### Step 2: Select Video or Android Stream
- Press `[1]` to open the file selection dialog and pick any `.mp4`, `.mkv`, or `.avi` video file.
- Press `[2]` to enter the stream URL of the companion Android app (e.g. `http://192.168.1.50:8080/videostream`).

### Step 3: Start the Virtual Camera
- Press `[3]` to start broadcasting.
- The virtual device `"Virtual Camera MF Video Source"` is now active on your PC.

### Step 4: Verify in Applications
Open any video conferencing app or the built-in Windows Camera app:
- **Windows Camera**: Press `Win + S`, type `Camera`, open the app, and switch camera device to `"Virtual Camera MF Video Source"`.
- **Microsoft Teams**: Settings -> Devices -> Camera -> Select `"Virtual Camera MF Video Source"`.
- **OBS Studio**: Add source -> Video Capture Device -> Select `"Virtual Camera MF Video Source"`.
- **Google Chrome / Edge**: Go to `chrome://settings/content/camera` and choose `"Virtual Camera MF Video Source"`.

### Step 5: Adjust Playback Controls On-The-Fly
While broadcasting, you can interactively press:
- `[5]` — Toggle Play / Pause.
- `[6]` — Toggle Loop ON / OFF.
- `[7]` — Toggle Camera Power (ON = Video stream, OFF = Standby card).
- `[8]` — Change resolution (480p / 720p / 1080p).
- `[9]` — Change framerate (15 / 24 / 30 / 60 FPS).

---

## 5. Bridging with the Android App (USB & Wi-Fi)

To use your Android phone as the video source for your Windows virtual webcam:
1. In the Android app, select your prerecorded video and tap **Start Server**.
2. **Wi-Fi Mode**: Note the IP shown on screen (e.g. `http://192.168.1.45:8080/videostream`) and enter it into the Windows Controller (`Option [2]`).
3. **Low-Latency USB Mode (via ADB)**:
   Connect your phone with a USB cable and run on Windows:
   ```cmd
   adb forward tcp:8080 tcp:8080
   ```
   Then enter `http://localhost:8080/videostream` in the Windows Controller!

---

## 6. Codebase File Map

| File | Purpose |
|------|---------|
| `include/Common.h` | Shared definitions, COM helpers, and `VideoConfig` structure. |
| `include/SyntheticPatternGenerator.h` | Studio SMPTE color-bar and standby card generator. |
| `include/VideoFileReader.h` | Media Foundation SourceReader file decoder with loop & seek. |
| `include/NetworkStreamReceiver.h` | HTTP MJPEG network stream receiver using WinINet & WIC. |
| `include/VirtualCameraStream.h` | Implements `IMFMediaStream` sample queue and delivery. |
| `include/VirtualCameraMediaSource.h` | Implements `IMFMediaSource` device lifecycle & descriptors. |
| `include/VirtualCameraManager.h` | Manages `MFCreateVirtualCamera` registration and high-level commands. |
| `src/VirtualCameraController.cpp` | Desktop controller application with interactive controls. |
| `src/register_driver.cpp` | Standalone CLI tool to register/unregister virtual camera. |
| `src/VirtualCameraDirectShowFilter.cpp` | DirectShow COM in-process server for legacy applications. |
