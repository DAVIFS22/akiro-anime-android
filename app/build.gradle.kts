plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // TODO: uncomment once app/google-services.json (Android app config) is added.
    // The web config in firebase-applet-config.json does NOT work here — see README.md.
    // id("com.google.gms.google-services")
}

android {
    namespace = "com.akiro.anime"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.akiro.anime"
        minSdk = 28
        targetSdk = 34
        versionCode = 6
        versionName = "1.3.0"

        // TMDB key injected at build time (set TMDB_API_KEY as a GitHub Actions secret).
        buildConfigField("String", "TMDB_API_KEY", "\"${project.findProperty("TMDB_API_KEY") ?: System.getenv("TMDB_API_KEY") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Networking (replaces axios calls from the web version)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Local storage (replaces localStorage / StorageService)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Image loading (posters/backdrops)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Video playback (replaces the web <video> player)
    implementation("androidx.media3:media3-exoplayer:1.4.0")
    implementation("androidx.media3:media3-ui:1.4.0")
    implementation("androidx.media3:media3-session:1.4.0")


    // BitTorrent engine used by the in-app Torrentio/magnet player.
    // libtorrent4j publishes separate native artifacts for each Android ABI.
    implementation("org.libtorrent4j:libtorrent4j:2.1.0-39")
    implementation("org.libtorrent4j:libtorrent4j-android-arm64:2.1.0-39")
    implementation("org.libtorrent4j:libtorrent4j-android-arm:2.1.0-39")
    implementation("org.libtorrent4j:libtorrent4j-android-x86:2.1.0-39")
    implementation("org.libtorrent4j:libtorrent4j-android-x86_64:2.1.0-39")

    // Firebase (mirrors src/api/firebase.ts usage)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
