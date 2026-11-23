import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun localProperty(propertyName: String, environmentName: String): String =
    System.getenv(environmentName)?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(propertyName, "").trim()

fun quoted(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val mapboxPublicToken = localProperty("mapbox.public.token", "MAPBOX_PUBLIC_TOKEN")
val mapProvider = localProperty("map.provider", "MAP_PROVIDER")
    .ifBlank { if (mapboxPublicToken.isBlank()) "ADDRESS_ONLY" else "MAPBOX" }
    .uppercase()

android {
    namespace = "com.sharemeal.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.sharemeal.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "USE_MOCK_DATA", "true")
            buildConfigField("String", "API_BASE_URL", "\"https://api.sharemeal.app/api/v1/\"")
            buildConfigField("String", "MAP_PROVIDER", quoted(mapProvider))
            buildConfigField("String", "MAPBOX_PUBLIC_TOKEN", quoted(mapboxPublicToken))
        }
        release {
            optimization {
                enable = false
            }
            buildConfigField("boolean", "USE_MOCK_DATA", "false")
            buildConfigField("String", "API_BASE_URL", "\"https://api.sharemeal.app/api/v1/\"")
            buildConfigField("String", "MAP_PROVIDER", quoted(mapProvider))
            buildConfigField("String", "MAPBOX_PUBLIC_TOKEN", quoted(mapboxPublicToken))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.play.services.location)
    implementation(libs.mapbox.maps)
    implementation(libs.mapbox.maps.compose)

    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.work.testing)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
