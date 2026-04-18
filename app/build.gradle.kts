plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

android {
    namespace = "com.example.rimereader"
    compileSdk = 35 // Standardowy zapis

    defaultConfig {
        applicationId = "com.example.rimereader"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // --- NOWE: Włączenie Jetpack Compose ---
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    // Bibliotekę material na razie zostawiamy (potrzebujemy jej, jeśli starsze okna mają się wyświetlać poprawnie, zanim je całkowicie wyrzucimy)
    implementation(libs.material)
    implementation(libs.androidx.activity)

    // Baza danych Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    add("kapt", "androidx.room:room-compiler:$roomVersion")

    // Cykl życia aplikacji
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // Odtwarzacz Media3 (tylko silnik, bez zbędnego UI)
    implementation("androidx.media3:media3-exoplayer:1.2.0")

    // --- NOWE: Biblioteki Jetpack Compose ---
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    // Biblioteka pozwalająca użyć funkcji setContent w MainActivity
    implementation("androidx.activity:activity-compose:1.8.2")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}