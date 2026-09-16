import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
val localSigning = Properties().apply {
    val file = rootProject.file(".tools/signing.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val distributionChannel = providers.gradleProperty("boleteraChannel").orElse("public").get().also {
    require(it in setOf("public", "beta")) { "boleteraChannel must be public or beta" }
}
val isBeta = distributionChannel == "beta"
val legacyUpdateBridge = providers.gradleProperty("boleteraLegacyUpdateBridge").orElse("false").get().let {
    it.toBooleanStrictOrNull() ?: error("boleteraLegacyUpdateBridge must be true or false")
}.also { enabled ->
    require(!enabled || !isBeta) { "The legacy update bridge must use the public channel" }
}
android {
    namespace = "uy.boletera.prueba"
    compileSdk = 35
    defaultConfig {
        applicationId = "uy.boletera.prueba"
        minSdk = 26
        targetSdk = 35
        versionCode = if (legacyUpdateBridge) 49 else 50
        versionName = when {
            legacyUpdateBridge -> "0.2.31-prueba"
            isBeta -> "0.2.32-beta.1"
            else -> "0.2.32"
        }
        buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"$distributionChannel\"")
        buildConfigField("String", "DISPLAY_VERSION", when {
            legacyUpdateBridge -> "\"0.2.31\""
            isBeta -> "\"0.2.32 Beta 1\""
            else -> "\"0.2.32\""
        })
        resValue("string", "app_name", if (isBeta) "Boletera Beta" else "Boletera")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isIncludeAndroidResources = true }
    signingConfigs {
        create("localTest") {
            if (localSigning.isNotEmpty()) {
                storeFile = rootProject.file(".tools/boletera-test.jks")
                storePassword = localSigning.getProperty("password")
                keyAlias = "boletera-test"
                keyPassword = localSigning.getProperty("password")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            if (localSigning.isNotEmpty()) signingConfig = signingConfigs.getByName("localTest")
        }
    }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
