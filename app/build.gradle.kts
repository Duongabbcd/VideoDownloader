plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kspp)
    alias(libs.plugins.serialization)
    alias(libs.plugins.parcelize)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.ezt.video.downloader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ezt.video.downloader"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
//            signingConfig = signingConfigs.getByName("release")
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
        viewBinding { enable = true }
        buildConfig = true
        compose = true
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
    }

}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.ui.android)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.gson)
    implementation(libs.constraintlayout)
    implementation(libs.work.runtime.ktx)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.media3.exoplayer)

    implementation("com.google.dagger:hilt-android:2.55")
    ksp("com.google.dagger:hilt-compiler:2.55")

    // Kotlin / Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${libs.versions.coroutines.get()}")


// Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")

    //UI dimen dp generator
    implementation("com.intuit.sdp:sdp-android:1.1.1")

    //important libs
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.0")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.0")
    implementation("io.github.junkfood02.youtubedl-android:aria2c:0.17.3")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.preference:preference-ktx:1.2.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    implementation("androidx.paging:paging-runtime-ktx:3.3.6")
    implementation("androidx.room:room-paging:2.5.2")

    implementation("com.neoutils.highlight:highlight-view:2.2.0")
    implementation("me.zhanghai.android.fastscroll:library:1.3.0")

    implementation("jp.wasabeef:picasso-transformations:2.4.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("com.anggrayudi:storage:1.5.5")

    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.10")

    implementation("io.noties.markwon:core:4.6.2")
    implementation("org.greenrobot:eventbus:3.3.1")
    implementation("com.github.teamnewpipe:newpipeextractor:0.24.8")

    implementation("androidx.navigation:navigation-fragment-ktx:2.9.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.5")
    implementation("com.devbrackets.android:exomedia:5.1.0")

    // For media playback using ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.6.0")
    // For DASH playback support with ExoPlayer
    implementation("androidx.media3:media3-exoplayer-dash:1.6.0")
    // For HLS playback support with ExoPlayer
    implementation("androidx.media3:media3-exoplayer-hls:1.6.0")
    implementation("androidx.media3:media3-ui:1.6.0")
    // For RTSP playback support with ExoPlayer
    implementation("androidx.media3:media3-exoplayer-rtsp:1.6.0")

    implementation("it.xabaras.android:recyclerview-swipedecorator:1.4")
    implementation("com.google.accompanist:accompanist-webview:0.30.1")

    implementation ("androidx.compose.material3:material3-android:1.3.1")
    implementation ("androidx.compose.runtime:runtime:1.9.3")
    implementation ("androidx.compose.ui:ui-android:1.7.8")

    //glide
    implementation("com.google.android.material:material:1.10.0")
    implementation("com.github.bumptech.glide:glide:4.15.1") // or your Glide version
    ksp("com.github.bumptech.glide:compiler:4.15.1")        // if using kapt
    implementation("androidx.browser:browser:1.6.0")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
