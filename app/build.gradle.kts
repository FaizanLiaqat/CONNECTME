plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}
val cameraxVersion = "1.2.1"
android {
    namespace = "com.example.connectmeapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.connectmeapp"
        minSdk = 24
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Add Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Your existing dependencies
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Firebase dependencies (use BOM versions)
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation(libs.androidx.espresso.idling.resource)

    // REMOVE THESE TWO LINES - they're causing the conflict
    // implementation(libs.firebase.auth)
    // implementation(libs.firebase.database)

    // Rest of your dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.viewpager2)
    implementation("com.google.android.material:material:1.10.0")

    implementation("com.google.guava:guava:31.1-android")
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:1.0.0-alpha31")

    implementation("io.agora.rtc:full-sdk:4.5.0")

    androidTestImplementation ("androidx.test.espresso:espresso-core:3.5.1")

    // Espresso for UI interactions
    androidTestImplementation ("androidx.test.espresso:espresso-contrib:3.5.1")

    // JUnit for testing
    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.5")

    // Espresso for Firebase UI (if needed)
    androidTestImplementation ("androidx.test.espresso:espresso-intents:3.5.1")
}