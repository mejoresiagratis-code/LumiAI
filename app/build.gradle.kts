import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Load local.properties for signing & API keys (never commit this file)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace         = "com.lumiai.flashlight"
    compileSdk        = 35

    defaultConfig {
        applicationId         = "com.lumiai.flashlight"
        minSdk                = 23          // 97% coverage
        targetSdk             = 35
        versionCode           = 86
        versionName           = "2.5.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AdMob App ID
        manifestPlaceholders["admobAppId"] = localProps.getProperty("ADMOB_APP_ID", "ca-app-pub-7644513562367479~7453103317")
    }

    signingConfigs {
        create("release") {
            storeFile     = localProps.getProperty("KEYSTORE_PATH")?.let { file(it) }
            storePassword = localProps.getProperty("KEYSTORE_PASS", "")
            keyAlias      = localProps.getProperty("KEY_ALIAS", "")
            keyPassword   = localProps.getProperty("KEY_PASS", "")
        }
    }

    buildTypes {
        debug {
            // applicationIdSuffix = ".debug" // removed: would require separate Firebase app
            isDebuggable        = true
            buildConfigField("Boolean", "IS_DEBUG", "true")
            // Test AdMob IDs in debug
            buildConfigField("String", "ADMOB_BANNER_ID",        "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID",  "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "ADMOB_REWARDED_ID",      "\"ca-app-pub-3940256099942544/5224354917\"")
        }
        release {
            isMinifyEnabled     = true
            isShrinkResources   = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig       = signingConfigs.getByName("release")
            buildConfigField("Boolean", "IS_DEBUG", "false")
            // Production AdMob IDs — account ca-app-pub-7644513562367479
            buildConfigField("String", "ADMOB_BANNER_ID",        "\"ca-app-pub-7644513562367479/7748616787\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID",  "\"ca-app-pub-7644513562367479/4306165418\"")
            buildConfigField("String", "ADMOB_REWARDED_ID",      "\"ca-app-pub-7644513562367479/3486984320\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose     = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.coroutines.android)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material)
    implementation(libs.compose.animation)
    implementation(libs.navigation.compose)
    debugImplementation(libs.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room — removed: no Entity/Dao/Database exists in this project. DataStore covers all
    //         persistence needs. Re-add if relational data structures are required in future.

    // DataStore
    implementation(libs.datastore.preferences)

    // CameraX (flash control)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)

    // Ads
    implementation(libs.admob)
    implementation(libs.ump)

    // Billing
    implementation(libs.play.billing)

    // ML Kit — removed: AmbientSmart uses the hardware light sensor (TYPE_LIGHT) directly.
    //           Re-add if scene classification via camera frames is implemented in future.

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.performance)
    implementation(libs.firebase.config)
    implementation(libs.firebase.messaging)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso)
}
