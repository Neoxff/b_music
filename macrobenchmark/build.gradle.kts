plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android") version "2.3.21"
}

android {
    namespace = "com.bmusic.app.macrobenchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = false
    }
}

dependencies {
    implementation("androidx.benchmark:benchmark-macro:1.4.0")
    implementation("androidx.benchmark:benchmark-baseline-profile:1.4.0")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.test:runner:1.7.0")
    implementation("androidx.test:rules:1.7.0")
    implementation("androidx.test.ext:junit:1.3.0")
}
