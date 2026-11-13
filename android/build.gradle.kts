group = "com.abdurrahmankaraoglu.background_doctor"
version = "1.0-SNAPSHOT"

buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:9.3.1")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.library")
}

android {
    namespace = "com.abdurrahmankaraoglu.background_doctor"

    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            // The Flutter-independent core (core/) is compiled into the plugin directly, so the
            // host app's Gradle settings don't need to know about a second project.
            // With AGP's built-in Kotlin, Kotlin sources must be listed under `kotlin`.
            kotlin.srcDirs("src/main/kotlin", "core/src/main/kotlin")
            manifest.srcFile("core/src/main/AndroidManifest.xml")
        }
    }

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("core/consumer-rules.pro")
    }

    lint {
        abortOnError = false
    }
}

// Kotlin is compiled by AGP's built-in Kotlin support; the plugin doesn't apply KGP itself.
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
