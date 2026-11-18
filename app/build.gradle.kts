plugins {
    id("com.android.application")
}

val releaseStoreFile = providers.environmentVariable("CAREEROPS_RELEASE_STORE_FILE")
val releaseStorePassword = providers.environmentVariable("CAREEROPS_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("CAREEROPS_RELEASE_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("CAREEROPS_RELEASE_KEY_PASSWORD")

val releaseSigningInputs = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
)
val releaseSigningConfigured = releaseSigningInputs.all { provider ->
    provider.isPresent && provider.get().isNotBlank()
}
val anyReleaseSigningInput = releaseSigningInputs.any { provider ->
    provider.isPresent && provider.get().isNotBlank()
}
val localciUnsigned = providers.gradleProperty("localciProtectedUnsignedBundle").orNull == "true"
val localciVersionCode = providers.gradleProperty("localciVersionCode").orNull?.toIntOrNull()
val localciVersionName = providers.gradleProperty("localciVersionName").orNull

check(!localciUnsigned || !anyReleaseSigningInput) {
    "Protected unsigned bundle builds must not receive any release signing credentials, including a partial configuration."
}
check(!localciUnsigned || (localciVersionCode != null && localciVersionCode > 0 && !localciVersionName.isNullOrBlank())) {
    "Protected unsigned bundle builds require an allocated versionCode and versionName."
}

android {
    namespace = "com.careerops.share"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.careerops.share"
        minSdk = 26
        targetSdk = 36
        // Preserve literal defaults for existing hosted release-workflow version readers.
        versionCode = 5
        versionName = "0.3.0"
        // Protected LocalCI-only unsigned bundle versions are allocated by the publisher.
        if (localciUnsigned) {
            versionCode = requireNotNull(localciVersionCode)
            versionName = requireNotNull(localciVersionName)
        }
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
    if (name == "assembleRelease" || (name == "bundleRelease" && !localciUnsigned)) {
        dependsOn(verifyReleaseSigningConfigured)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
