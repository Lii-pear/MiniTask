plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.example.minitask.baselineprofile"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR,LOW-BATTERY"
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        create("release") {
            initWith(getByName("debug"))
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents {
    beforeVariants(selector().all()) { variant ->
        variant.enable = variant.buildType == "release"
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
}
