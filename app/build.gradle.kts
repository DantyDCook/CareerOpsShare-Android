plugins {
    id("com.android.application")
}

val releaseStoreFile = providers.environmentVariable("CAREEROPS_RELEASE_STORE_FILE")
val releaseStorePassword = providers.environmentVariable("CAREEROPS_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("CAREEROPS_RELEASE_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("CAREEROPS_RELEASE_KEY_PASSWORD")

val releaseSigningConfigured = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { provider -> provider.isPresent && provider.get().isNotBlank() }

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

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

val verifyReleaseSigningConfigured = tasks.register("verifyReleaseSigningConfigured") {
    group = "verification"
    description = "Fails release builds unless the stable CareerOps Share signing environment is configured."

    doLast {
        check(releaseSigningConfigured) {
            """
            Stable release signing is not configured.
            Set CAREEROPS_RELEASE_STORE_FILE, CAREEROPS_RELEASE_STORE_PASSWORD,
            CAREEROPS_RELEASE_KEY_ALIAS, and CAREEROPS_RELEASE_KEY_PASSWORD.
            See docs/SIGNING.md.
            """.trimIndent()
        }
    }
}

tasks.configureEach {
    if (name == "assembleRelease" || name == "bundleRelease") {
        dependsOn(verifyReleaseSigningConfigured)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
