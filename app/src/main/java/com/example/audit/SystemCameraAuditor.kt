package com.example.audit

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.example.model.PhysicalCameraDetail
import com.example.model.SystemAuditResult
import java.io.File

object SystemCameraAuditor {

    fun performAudit(context: Context): SystemAuditResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val cameraList = mutableListOf<PhysicalCameraDetail>()

        if (cameraManager != null) {
            try {
                for (id in cameraManager.cameraIdList) {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                        CameraCharacteristics.LENS_FACING_BACK -> "Back (Rear)"
                        CameraCharacteristics.LENS_FACING_FRONT -> "Front (Selfie)"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "External (USB)"
                        else -> "Unknown"
                    }

                    val hwLevel = when (characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY (Emulated HAL1)"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED (Basic Camera2)"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL (Per-frame controls)"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3 (YUV reprocess & RAW)"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                        else -> "Unknown Level"
                    }

                    val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val sizes = map?.getOutputSizes(ImageFormat.JPEG)
                    val maxRes = if (sizes != null && sizes.isNotEmpty()) {
                        "${sizes[0].width}x${sizes[0].height}"
                    } else {
                        "N/A"
                    }

                    val isLogical = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        characteristics.physicalCameraIds.isNotEmpty()
                    } else {
                        false
                    }

                    cameraList.add(
                        PhysicalCameraDetail(
                            id = id,
                            facing = facing,
                            hardwareLevel = hwLevel,
                            maxResolution = maxRes,
                            isLogicalMultiCamera = isLogical
                        )
                    )
                }
            } catch (e: Exception) {
                // CameraManager access error or permission
            }
        }

        val isSelinuxEnforcing = checkSelinuxEnforcing()

        val limitationSummary = "Stock unrooted Android restricts system-wide virtual camera injection into 3rd-party apps. Unprivileged apps cannot register a system camera device at the HAL level."

        val technicalDetails = listOf(
            "Android Camera HAL Architecture: Hardware abstraction operates in the privileged 'cameraserver' daemon (UID 1047 / 'camera') interacting with kernel V4L2/UVC drivers.",
            "SELinux Sandbox Isolation: Third-party apps run in the 'untrusted_app' domain, strictly blocked from communicating with camera HAL AIDL/HIDL services as a provider.",
            "No Public Virtual Camera API: Android SDK only provides consumer APIs (Camera2, CameraX). The 'android.permission.SYSTEM_CAMERA' permission is signature-only for OEMs.",
            "CTS (Compatibility Test Suite) Enforcement: Android certification mandates that all reported camera IDs correspond to genuine physical sensors or certified OEM peripherals to prevent spoofing."
        )

        val legitimateAlternatives = listOf(
            "Local High-Performance IP/MJPEG Stream Server: Broadcasts the virtual feed over Wi-Fi/LAN/USB (via adb port forwarding) for consumption by PC, OBS Studio, and video apps.",
            "In-App Surface / OpenGL Pipeline: Renders custom prerecorded video into Compose / OpenGL / TextureView for apps requiring internal video test sources.",
            "Windows Media Foundation Driver Bridge: Streams video directly to the companion Windows Virtual Camera driver, making it appear as a genuine Windows webcam in PC apps."
        )

        return SystemAuditResult(
            selinuxEnforcing = isSelinuxEnforcing,
            physicalCameras = cameraList,
            systemWideInjectionAllowed = false,
            limitationSummary = limitationSummary,
            technicalDetails = technicalDetails,
            legitimateAlternatives = legitimateAlternatives
        )
    }

    private fun checkSelinuxEnforcing(): Boolean {
        return try {
            val file = File("/sys/fs/selinux/enforce")
            if (file.exists()) {
                file.readText().trim() == "1"
            } else {
                true // Standard production Android devices are enforcing by default
            }
        } catch (_: Exception) {
            true
        }
    }
}
