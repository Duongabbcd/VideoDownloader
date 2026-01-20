plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kspp)
    alias(libs.plugins.serialization)
    alias(libs.plugins.parcelize)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("dagger.hilt.android.plugin")
    id ("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.ezt.priv.shortvideodownloader"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ezt.priv.shortvideodownloader"
        minSdk = 28
        targetSdk = 36
        versionCode = 6
        versionName = "1.0.6"


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/piano2.jks")
            storePassword = "12345678"
            keyAlias = "piano2025"
            keyPassword = "12345678"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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
        dataBinding = true
    }


    bundle {
        language {
            enableSplit = false
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
//            include("armeabi-v7a", "arm64-v8a", "arm64-v8a-16k")
            isUniversalApk  =  false
        }
    }

    base {
        archivesName.set("VideoDownloader_${defaultConfig.versionName}")
    }

    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    lint {
        disable.add("NullSafeMutableLiveData")
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
    implementation(libs.firebase.config)

    implementation(libs.gson)
    implementation(libs.constraintlayout)
    implementation(libs.work.runtime.ktx)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.media3.exoplayer)

    implementation("com.google.dagger:hilt-android:2.55")
    kapt ("com.google.dagger:hilt-compiler:2.55")

    // Kotlin / Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${libs.versions.coroutines.get()}")


// Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")

    //UI dimen dp generator
    implementation("com.intuit.sdp:sdp-android:1.1.1")

    //important libs
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.0")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:aria2c:0.18.0")

    implementation("com.googlecode.mp4parser:isoparser:1.1.22")

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

    implementation("androidx.navigation:navigation-fragment-ktx:2.9.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.6")
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

    implementation ("androidx.compose.runtime:runtime:1.9.4")
    implementation ("androidx.compose.ui:ui-android:1.7.8")

    //glide
    implementation("com.google.android.material:material:1.10.0")
    implementation("com.github.bumptech.glide:glide:4.16.0") // or your Glide version
    ksp("com.github.bumptech.glide:compiler:4.15.1")        // if using kapt
    implementation("androidx.browser:browser:1.6.0")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    //advertisement
    implementation("com.facebook.android:facebook-android-sdk:18.0.2")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-messaging")

    implementation(libs.adjust.android)
    implementation("com.android.installreferrer:installreferrer:2.2")

    implementation("com.google.ads.mediation:pangle:7.2.0.6.0")
    implementation("com.google.ads.mediation:applovin:13.5.1.0")
    implementation("com.google.ads.mediation:facebook:6.21.0.0")
    implementation("com.google.ads.mediation:vungle:7.6.0.0")
    implementation("com.google.ads.mediation:mintegral:17.0.21.0")

    implementation("com.google.android.gms:play-services-ads:24.7.0") // or latest
    implementation ("com.airbnb.android:lottie:6.6.6")
    //check update
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    implementation ("io.reactivex.rxjava3:rxandroid:3.0.2")
    // Because RxAndroid releases are few and far between, it is recommended you also
    // explicitly depend on RxJava's latest version for bug fixes and new features.
    // (see https://github.com/ReactiveX/RxJava/releases for latest 3.x.x version)
    implementation ("io.reactivex.rxjava3:rxjava:3.1.10")
    // optional - RxJava3 support for Room
    implementation("androidx.room:room-rxjava3:2.6.1")
}
