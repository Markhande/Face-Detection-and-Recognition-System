plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.localbase"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 36
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
    buildFeatures {viewBinding = true}
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src\\main\\assets", "src\\main\\assets")
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    val room_version = "2.7.2"

    implementation("androidx.room:room-runtime:$room_version")

    // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
    // See Add the KSP plugin to your project
    ksp("androidx.room:room-compiler:$room_version")

    // If this project only uses Java source, use the Java annotationProcessor
    // No additional plugins are necessary
    annotationProcessor("androidx.room:room-compiler:$room_version")

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$room_version")

    implementation("com.airbnb.android:lottie:6.3.0")

    implementation("com.google.dagger:hilt-android:2.56.2")
    ksp("com.google.dagger:hilt-compiler:2.56.2")

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.9")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")  // Latest version
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")

    implementation("com.google.mlkit:face-detection:16.1.7")
    // Tensor Flow GPU
    implementation("org.tensorflow:tensorflow-lite-gpu:2.12.0")

    // Use this dependency to use the dynamically downloaded model in Google Play Services
    //implementation("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")

    // glide image
    implementation("com.github.bumptech.glide:glide:5.0.5")

    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.12.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    implementation("io.coil-kt:coil:2.4.0")

    implementation("org.tensorflow:tensorflow-lite:2.12.0")
    //  implementation("org.opencv:opencv-android:4.11.0")

//    implementation("com.google.mediapipe:mediapipe-framework:0.10.20")
//    implementation("com.google.mediapipe:mediapipe-facedetection:0.10.20")

    //implementation("com.google.mediapipe:solution-core:0.10.20")
    // implementation("com.google.mediapipe:facedetection:0.10.20")
    // For face detection
    //implementation("com.google.mediapipe:facemesh:0.10.20")
    // For face mesh
    //implementation("com.google.mediapipe:hands:0.10.20")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
    // Mediapipe Face Detection
    implementation("com.google.mediapipe:tasks-vision:0.10.20")

    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.0")

    implementation("com.airbnb.android:lottie:6.4.0")
    //implementation("com.google.mediapipe:mediapipe-framework:0.8.22")
}