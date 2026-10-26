plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "de.galaktogon.gaphomewidget"
    compileSdk = 35
    defaultConfig {
        applicationId = "de.galaktogon.gaphomewidget"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin {
    jvmToolchain(17)
}
