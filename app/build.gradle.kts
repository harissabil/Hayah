import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun requiredConfig(key: String): String {
    return localProperties.getProperty(key)
        ?: providers.gradleProperty(key).orNull
        ?: throw GradleException("Missing required config '$key'. Add it to local.properties or pass -P$key=...")
}

fun quoted(value: String): String {
    val escaped = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$escaped\""
}

android {
    namespace = "id.harissabil.hayah"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "id.harissabil.hayah"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AppAuth redirect scheme — matches the registered redirect URI
        manifestPlaceholders["appAuthRedirectScheme"] = "id.harissabil.hayah"

        buildConfigField("boolean", "USE_PRODUCTION", requiredConfig("HAYAH_USE_PRODUCTION"))
        buildConfigField("String", "OAUTH_CLIENT_ID_PROD", quoted(requiredConfig("HAYAH_CLIENT_ID_PROD")))
        buildConfigField("String", "OAUTH_CLIENT_ID_TEST", quoted(requiredConfig("HAYAH_CLIENT_ID_TEST")))
        buildConfigField("String", "OAUTH_AUTH_ENDPOINT_PROD", quoted(requiredConfig("HAYAH_AUTH_ENDPOINT_PROD")))
        buildConfigField("String", "OAUTH_AUTH_ENDPOINT_TEST", quoted(requiredConfig("HAYAH_AUTH_ENDPOINT_TEST")))
        buildConfigField("String", "OAUTH_TOKEN_PROXY_URL", quoted(requiredConfig("HAYAH_TOKEN_PROXY_URL")))
        buildConfigField("String", "OAUTH_REVOKE_PROXY_URL", quoted(requiredConfig("HAYAH_REVOKE_PROXY_URL")))
        buildConfigField("String", "OAUTH_API_BASE_PROD", quoted(requiredConfig("HAYAH_API_BASE_PROD")))
        buildConfigField("String", "OAUTH_API_BASE_TEST", quoted(requiredConfig("HAYAH_API_BASE_TEST")))
        buildConfigField("String", "OAUTH_REDIRECT_URI", quoted(requiredConfig("HAYAH_REDIRECT_URI")))
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.text.google.fonts)

    // Auth
    implementation(libs.appauth)

    // Networking
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Persistence
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    // DI
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // Firebase AI
    implementation(libs.firebase.ai)

    // Activity Recognition
    implementation(libs.play.services.location)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}