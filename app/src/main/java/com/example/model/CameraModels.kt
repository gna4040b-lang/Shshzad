package com.example.model

enum class ResolutionOption(val label: String, val width: Int, val height: Int) {
    RES_480P("480p (SD)", 640, 480),
    RES_720P("720p (HD)", 1280, 720),
    RES_1080P("1080p (FHD)", 1920, 1080);

    val aspectRatio: Float
        get() = width.toFloat() / height.toFloat()
}

data class PhysicalCameraDetail(
    val id: String,
    val facing: String,
    val hardwareLevel: String,
    val maxResolution: String,
    val isLogicalMultiCamera: Boolean
)

data class SystemAuditResult(
    val selinuxEnforcing: Boolean,
    val physicalCameras: List<PhysicalCameraDetail>,
    val systemWideInjectionAllowed: Boolean = false,
    val limitationSummary: String,
    val technicalDetails: List<String>,
    val legitimateAlternatives: List<String>
)
