plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    // Снимки экранов без телефона: Robolectric рисует экраны на сервере сборки (workflow Screenshots)
    id("io.github.takahirom.roborazzi")
}

android {
    namespace = "com.zerospin.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zerospin.app"
        minSdk = 26
        targetSdk = 35
        // Номер сборки GitHub растёт с каждым запуском, поэтому новая сборка ставится поверх старой
        val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = runNumber
        versionName = "0.1.$runNumber"
    }

    // Один и тот же ключ для всех сборок: Android ставит обновление только при совпадении подписи
    signingConfigs {
        create("zerospin") {
            storeFile = file("zerospin-release.jks")
            storePassword = "zerospin-2026"
            keyAlias = "zerospin"
            keyPassword = "zerospin-2026"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("zerospin")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("zerospin")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.26.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.26.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.26.0")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
