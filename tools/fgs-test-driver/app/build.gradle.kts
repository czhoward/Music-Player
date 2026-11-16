plugins {
    id("com.android.application")
}

android {
    namespace = "org.fossify.musicplayer.fgstest"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.fossify.musicplayer.fgstest"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("androidx.media3:media3-session:1.10.1")
}
