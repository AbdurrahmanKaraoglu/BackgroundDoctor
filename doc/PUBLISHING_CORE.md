# Publishing the Kotlin core

`android/core` is a standalone Android library (`com.abdurrahmankaraoglu:background-doctor-core`).
The Flutter plugin compiles its sources directly, so publishing it is optional and only
needed for native Android apps.

**Nothing has been published yet.** Both routes below need accounts or keys that only the
maintainer has.

## Build and test locally

```bash
cd android/core
./gradlew testDebugUnitTest        # 34 JVM + Robolectric tests
./gradlew publishToMavenLocal      # ~/.m2/repository/com/abdurrahmankaraoglu/background-doctor-core/0.1.1/
```

The Gradle wrapper and `local.properties` (with `sdk.dir`) are not committed. Copy the wrapper
from `example/android` or use a local Gradle 9.x installation.

## Option A: JitPack (no account, no signing)

1. Add a `jitpack.yml` at the repository root:
   ```yaml
   jdk:
     - openjdk17
   install:
     - cd android/core && gradle publishToMavenLocal
   ```
2. Push a tag (`v0.1.0` already exists for the Flutter package).
3. Consumers add `maven { url = uri("https://jitpack.io") }` and
   `implementation("com.github.AbdurrahmanKaraoglu:BackgroundDoctor:v0.1.0")`.

JitPack builds on demand. The first request for a tag can take a few minutes.

## Option B: Maven Central (Central Portal)

1. Create an account at https://central.sonatype.com and verify the `com.abdurrahmankaraoglu`
   namespace through a DNS TXT record on abdurrahmankaraoglu.com.
2. Create a GPG key and publish it to a key server (`gpg --keyserver keys.openpgp.org --send-keys <id>`).
3. Add the `signing` plugin, plus a Central Portal upload plugin such as
   `com.vanniktech.maven.publish`, to `android/core/build.gradle.kts`. Keep these credentials
   in `~/.gradle/gradle.properties`, never in the repo: `signing.keyId`, `signing.password`,
   `signing.secretKeyRingFile`, `mavenCentralUsername`, `mavenCentralPassword`.
4. Run `./gradlew publishAndReleaseToMavenCentral` (or upload and release from the portal UI).

The POM (name, description, MIT license, SCM, developer) is already configured in
`android/core/build.gradle.kts`.

## Keeping versions in sync

Bump `version` in `android/core/build.gradle.kts` together with `pubspec.yaml`, and bump
`KnownOemIntents.VERSION` whenever the intent table changes.
