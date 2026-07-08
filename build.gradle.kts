// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

// Ensure the APK copy is done during Gradle configuration phase
val srcApkBuild = file("app/build/outputs/apk/debug/app-debug.apk")
val srcApkPlatform = file(".build-outputs/app-debug.apk")
val downloadDir = file("APK_DOWNLOAD")
downloadDir.mkdirs()

fun copyApkToDownload(src: java.io.File) {
    if (src.exists()) {
        val destApk = file("APK_DOWNLOAD/app-debug.apk")
        val destApkBin = file("APK_DOWNLOAD/app-debug.apk.bin")
        val destBin = file("APK_DOWNLOAD/app-debug.bin")
        
        src.copyTo(destApk, overwrite = true)
        src.copyTo(destApkBin, overwrite = true)
        src.copyTo(destBin, overwrite = true)
        
        logger.lifecycle("SUCCESS_COPY_APK: Copied APK from ${src.path} to APK_DOWNLOAD/ with size: ${destApk.length()} bytes.")
    }
}

if (srcApkBuild.exists()) {
    copyApkToDownload(srcApkBuild)
} else if (srcApkPlatform.exists()) {
    copyApkToDownload(srcApkPlatform)
} else {
    logger.lifecycle("WARNING_COPY_APK: Source APK not found yet.")
}

tasks.register("copyApk") {
    doLast {
        if (srcApkBuild.exists()) {
            copyApkToDownload(srcApkBuild)
        } else if (srcApkPlatform.exists()) {
            copyApkToDownload(srcApkPlatform)
        } else {
            println("ERROR_COPY_APK: No source APK found in build outputs or .build-outputs")
        }
    }
}


