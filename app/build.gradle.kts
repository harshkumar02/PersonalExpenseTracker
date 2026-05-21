plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.compose")   // version inherited from root
}

android {
    namespace = "com.hktech.personalexpensetracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hktech.personalexpensetracker"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures { compose = true }

    // ✅ Use Java 21 everywhere (matches your JBR 21 Gradle JDK)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Activity Compose (provides setContent / rememberLauncherForActivityResult)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

// Room
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Material Components + AppCompat (for XML M3 theme availability)
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Accompanist permissions (optional)
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Jetpack Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Google Sign-In for backup
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
}
