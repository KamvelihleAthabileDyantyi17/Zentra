import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

/*
 * Release signing. The secret values are NEVER committed. They come from either:
 *  - keystore.properties in the project root (your computer; it's in .gitignore), or
 *  - environment variables (GitHub Actions, filled from repository secrets).
 * See docs/play-store/README.md for how to create the keystore.
 */
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun signingValue(property: String, envVar: String): String? =
    keystoreProps.getProperty(property) ?: System.getenv(envVar)
val releaseStoreFile = signingValue("storeFile", "SHANNOS_KEYSTORE_PATH")

android {
    namespace = "com.example.shannosfoodtruck"
    compileSdk = 35

    defaultConfig {
        // Must match the package in app/google-services.json
        applicationId = "com.example.shannosfoodtruck"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "2.3"

        // true  = live data from Firebase (Firestore + Auth)
        // false = offline demo data, handy for quick testing without internet
        buildConfigField("boolean", "USE_FIREBASE", "true")
    }

    signingConfigs {
        create("release") {
            if (releaseStoreFile != null) {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = signingValue("storePassword", "SHANNOS_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "SHANNOS_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "SHANNOS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed only when a keystore is available; otherwise the release build is unsigned
            if (releaseStoreFile != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        // Lets android.util.Log calls run harmlessly inside plain JVM unit tests
        unitTests.isReturnDefaultValues = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    debugImplementation(libs.compose.ui.tooling)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)

    // Google sign-in (SSO) via Android Credential Manager
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
