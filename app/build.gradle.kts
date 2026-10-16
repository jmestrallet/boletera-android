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
    require(it in setOf("public", "beta", "alpha", "lab")) { "boleteraChannel must be public, beta, alpha or lab" }
}
val isAlpha = distributionChannel == "alpha"
val isBeta = distributionChannel == "beta"
// Lab is a separate app installed next to Boletera, so a broken experiment never replaces it.
val isLab = distributionChannel == "lab"
val legacyUpdateBridge = providers.gradleProperty("boleteraLegacyUpdateBridge").orElse("false").get().let {
    it.toBooleanStrictOrNull() ?: error("boleteraLegacyUpdateBridge must be true or false")
}.also { enabled ->
    require(!enabled || (!isBeta && !isAlpha && !isLab)) { "The legacy update bridge must use the public channel" }
}
android {
    namespace = "uy.boletera.prueba"
    compileSdk = 35
    defaultConfig {
        applicationId = if (isLab) "uy.boletera.lab" else "uy.boletera.prueba"
        minSdk = 26
        targetSdk = 35
        versionCode = if (legacyUpdateBridge) 49 else if (isLab) 76 else 61
        versionName = when {
            legacyUpdateBridge -> "0.2.31-prueba"
            isLab -> "0.2.40-lab.16"
            isAlpha -> "0.2.40-alpha.5"
            isBeta -> "0.2.40-beta.2"
            else -> "0.2.40"
        }
        buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"$distributionChannel\"")
        // Key that can only open issues in the private jmestrallet/boletera-reportes repo. It lives in .tools (never
        // committed) and is read at build time; without it «Enviar reporte» falls back to the share sheet.
        val reportToken = rootProject.file(".tools/reports-token.txt").takeIf { it.exists() }?.readText()?.trim().orEmpty()
            .filter { it.isLetterOrDigit() || it == '_' }
        buildConfigField("String", "REPORT_TOKEN", "\"$reportToken\"")
        buildConfigField("String", "DISPLAY_VERSION", when {
            legacyUpdateBridge -> "\"0.2.31\""
            isLab -> "\"0.2.40 Lab 16\""
            isAlpha -> "\"0.2.40 Alpha 5\""
            isBeta -> "\"0.2.40 Beta 2\""
            else -> "\"0.2.40\""
        })
        resValue("string", "app_name", if (isLab) "Boletera Lab" else if (isAlpha) "Boletera Alpha" else if (isBeta) "Boletera Beta" else "Boletera")
        // Lab gets a violet launcher tile so it is never mistaken for the everyday app.
        resValue("color", "app_icon_background", if (isLab) "#7C4DFF" else "#1A73D9")
        resValue("color", "app_icon_background_end", if (isLab) "#4527A0" else "#0B4A94")
        manifestPlaceholders["appTheme"] = if (isLab) "@style/AppTheme.Lab" else "@style/AppTheme"
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
    // Backdrop blur for the Lab glass panels; falls back to a tinted surface before Android 12.
    implementation("dev.chrisbanes.haze:haze:1.6.10")
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
