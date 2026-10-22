import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val geoapifyApiKey: String = localProperties.getProperty("GEOAPIFY_API_KEY") ?: ""

android {
    namespace = "com.vihaan.myjourney"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vihaan.myjourney"
        minSdk = 26
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

val generateKotlinBuildConfig = tasks.register("generateKotlinBuildConfig") {
    val localPropertiesFile = rootProject.file("local.properties")
    inputs.file(localPropertiesFile).optional(true)
    val outputDir = layout.buildDirectory.dir("generated/source/buildConfig/kotlin")
    outputs.dir(outputDir)
    doLast {
        val props = Properties()
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { props.load(it) }
        }
        val key = props.getProperty("GEOAPIFY_API_KEY") ?: ""
        val file = outputDir.get().file("com/vihaan/myjourney/BuildConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package com.vihaan.myjourney

            object BuildConfig {
                const val GEOAPIFY_API_KEY: String = "$key"
            }
            """.trimIndent()
        )
    }
}

android.sourceSets.getByName("main").kotlin.srcDir(generateKotlinBuildConfig.map { it.outputs.files.singleFile })

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateKotlinBuildConfig)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.play.services.location)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
