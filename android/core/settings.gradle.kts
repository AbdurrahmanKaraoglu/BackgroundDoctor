// Standalone build of the core library, used for its unit tests and for publishing.
// The Flutter plugin compiles these sources directly (see ../build.gradle.kts).
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "background-doctor-core"
