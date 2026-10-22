import java.util.Properties

plugins {
    id("com.android.application")
}

// The release key stays out of the repo; without it, release builds fall back to the debug key.
val key = rootProject.file("firma/clave.properties").takeIf { it.exists() }
    ?.let { f -> Properties().apply { f.inputStream().use { load(it) } } }

android {
    namespace = "ai.askpancho.desconexion"
    compileSdk = 37

    signingConfigs {
        if (key != null) create("release") {
            storeFile = rootProject.file("firma/desconexion.jks")
            storePassword = key.getProperty("storePassword")
            keyAlias = key.getProperty("keyAlias")
            keyPassword = key.getProperty("storePassword")
        }
    }

    defaultConfig {
        applicationId = "ai.askpancho.desconexion"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName(if (key != null) "release" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
