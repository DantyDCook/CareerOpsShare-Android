plugins {
    id("com.android.application")
}

android {
    namespace = "com.careerops.share"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.careerops.share"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
