plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
}

android {
    namespace = "com.sapanbase.sdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

// JitPack builds from the git tag and re-writes group/version to match the
// requested coordinate; these values are the local/default identity.
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.sapanhq"
            artifactId = "sapanbase-android"
            version = "0.2.0"
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
