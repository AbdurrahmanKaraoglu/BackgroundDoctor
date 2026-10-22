plugins {
    id("com.android.library") version "9.1.0"
    `maven-publish`
}

group = "com.abdurrahmankaraoglu"
version = "0.1.0"

android {
    namespace = "com.abdurrahmankaraoglu.backgrounddoctor.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Robolectric/Conscrypt build native library names with toLowerCase(); a Turkish
        // default locale turns "windows" into "wındows". Pin the test JVM locale.
        unitTests.all { it.jvmArgs("-Duser.language=en", "-Duser.country=US") }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.test:core:1.7.0")
}

// `./gradlew publishToMavenLocal` works out of the box. Publishing to Maven Central needs
// signing keys and a Central Portal account; see doc/PUBLISHING_CORE.md.
publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "background-doctor-core"
            afterEvaluate { from(components["release"]) }
            pom {
                name.set("Background Doctor core")
                description.set("Diagnoses why an Android app doesn't run in the background and why its process exited.")
                url.set("https://github.com/AbdurrahmanKaraoglu/BackgroundDoctor")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("AbdurrahmanKaraoglu")
                        name.set("Abdurrahman Karaoğlu")
                    }
                }
                scm {
                    url.set("https://github.com/AbdurrahmanKaraoglu/BackgroundDoctor")
                    connection.set("scm:git:https://github.com/AbdurrahmanKaraoglu/BackgroundDoctor.git")
                }
            }
        }
    }
}
