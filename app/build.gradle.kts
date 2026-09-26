import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// ============================================================
// dependenciesInfo: ALWAYS disabled (privacy / store compliance).
// ============================================================
// The default (includeInApk = true) embeds a Google-encrypted
// DEPENDENCY_INFO_BLOCK blob into the APK signing block.
// IzzyOnDroid / F-Droid flag this blob during APK scans and
// require it to be absent. Google Play does NOT need it either.
// Setting both to false removes the blob and satisfies both stores.
// See: https://izzyondroid.org/docs/general/AppInclusionPolicy/

android {
    namespace = "com.ahmedsamy.app"
    compileSdk = 36

    defaultConfig {
        applicationId = providers.gradleProperty("APP_ID").get()
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("VERSION_CODE").get().toInt()
        versionName = providers.gradleProperty("VERSION_NAME").get()

        resValue("string", "app_name", providers.gradleProperty("APP_NAME").get())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val props = Properties()
        val signingFile = File(rootDir, "signing.properties")
        if (signingFile.exists()) {
            signingFile.inputStream().use { props.load(it) }
            create("release") {
                storeFile = file(props.getProperty("release.store.file"))
                storePassword = props.getProperty("release.store.password")
                keyAlias = props.getProperty("release.key.alias")
                keyPassword = props.getProperty("release.key.password")
            }
        } else {
            println("NOTE: signing.properties not found. See signing.properties.example for setup. Building unsigned APK.")
        }
    }

    buildTypes {
        getByName("debug") {
            // Keep debug builds fast and open
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        getByName("release") {
            // Optimize APK size using R8
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false

            val signingFile = File(rootDir, "signing.properties")
            if (signingFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = null
            }

            // ALWAYS disabled: removes the Google-encrypted DEPENDENCY_INFO_BLOCK
            // blob from the APK signing block. Required by IzzyOnDroid, safe for Google Play.
            dependenciesInfo {
                includeInApk = false
                includeInBundle = false
            }
        }
    }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_21)
        targetCompatibility(JavaVersion.VERSION_21)
    }

    kotlin { jvmToolchain(21) }
    buildFeatures {
        compose = true
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Jetpack Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
