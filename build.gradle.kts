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
val srcApk = file(".build-outputs/app-debug.apk")
val downloadDir = file("APK_DOWNLOAD")
downloadDir.mkdirs()
val destApk = file("APK_DOWNLOAD/app-debug.apk")
if (srcApk.exists()) {
    srcApk.copyTo(destApk, overwrite = true)
    logger.lifecycle("SUCCESS_COPY_APK: Copied APK to APK_DOWNLOAD/app-debug.apk. Size: ${destApk.length()} bytes.")
} else {
    logger.lifecycle("ERROR_COPY_APK: Source APK does not exist in .build-outputs/app-debug.apk")
}

tasks.register("copyApk") {
    doLast {
        if (srcApk.exists()) {
            srcApk.copyTo(destApk, overwrite = true)
            println("SUCCESS_COPY_APK: Copied APK to APK_DOWNLOAD/app-debug.apk. Size: ${destApk.length()} bytes.")
        } else {
            println("ERROR_COPY_APK: Source APK does not exist in .build-outputs/app-debug.apk")
        }
    }
}


