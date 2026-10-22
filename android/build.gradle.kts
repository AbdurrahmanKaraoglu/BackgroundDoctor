group = "com.abdurrahmankaraoglu.background_doctor"
version = "1.0-SNAPSHOT"

buildscript {
    val kotlinVersion = "2.4.0"
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:9.1.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
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

// AGP 9+ compiles Kotlin on its own; host apps still on AGP 8 need the Kotlin Gradle plugin.
val agpMajorVersion = com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION.substringBefore('.').toInt()
if (agpMajorVersion < 9) {
    apply(plugin = "org.jetbrains.kotlin.android")
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
            java.srcDirs("src/main/kotlin", "core/src/main/kotlin")
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

// Works with both the Kotlin Gradle plugin and AGP's built-in Kotlin support.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
