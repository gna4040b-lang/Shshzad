cmake_minimum_required(VERSION 3.16)
project(WindowsVirtualCamera LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(MSVC)
    add_compile_options(/W4 /utf-8 /EHsc)
endif()

include_directories(include)

set(COMMON_SOURCES
    src/SyntheticPatternGenerator.cpp
    src/VideoFileReader.cpp
    src/NetworkStreamReceiver.cpp
    src/VirtualCameraStream.cpp
    src/VirtualCameraMediaSource.cpp
    src/VirtualCameraManager.cpp
)

# 1. Main Windows Desktop Controller Executable
add_executable(VirtualCameraController
    src/VirtualCameraController.cpp
    ${COMMON_SOURCES}
)

target_link_libraries(VirtualCameraController PRIVATE
    mf
    mfplat
    mfuuid
    mfreadwrite
    wininet
    shlwapi
    windowscodecs
    ole32
    oleaut32
    user32
    gdi32
    comdlg32
)

# 2. Standalone Virtual Camera Driver Registration Utility
add_executable(VirtualCameraRegister
    src/register_driver.cpp
)

target_link_libraries(VirtualCameraRegister PRIVATE
    mf
    mfplat
    mfuuid
    ole32
)

# 3. DirectShow COM In-Process Server (DLL) for Legacy 32/64-bit DirectShow Applications
add_library(VirtualCameraDirectShowFilter SHARED
    src/VirtualCameraDirectShowFilter.cpp
)

target_link_libraries(VirtualCameraDirectShowFilter PRIVATE
    ole32
    oleaut32
    user32
    advapi32
)
