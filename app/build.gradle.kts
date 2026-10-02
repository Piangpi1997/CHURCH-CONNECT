plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Applying this plugin is optional until the owner supplies the church's Firebase config.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val configuredChurchId = providers.gradleProperty("CHURCH_ID").orElse("cmf-setapak").get()
val useFirebaseEmulators = providers.gradleProperty("USE_FIREBASE_EMULATORS").map(String::toBoolean).getOrElse(false)
val releaseStoreFile = providers.environmentVariable("CHURCH_RELEASE_STORE_FILE").orNull?.takeIf(String::isNotBlank)
val releaseStorePassword = providers.environmentVariable("CHURCH_RELEASE_STORE_PASSWORD").orNull?.takeIf(String::isNotBlank)
val releaseKeyAlias = providers.environmentVariable("CHURCH_RELEASE_KEY_ALIAS").orNull?.takeIf(String::isNotBlank)
val releaseKeyPassword = providers.environmentVariable("CHURCH_RELEASE_KEY_PASSWORD").orNull?.takeIf(String::isNotBlank)
val releaseSigningValues = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
if (releaseSigningValues.any { it != null } && releaseSigningValues.any { it == null }) {
    throw GradleException("Release signing requires CHURCH_RELEASE_STORE_FILE, CHURCH_RELEASE_STORE_PASSWORD, CHURCH_RELEASE_KEY_ALIAS, and CHURCH_RELEASE_KEY_PASSWORD.")
}
if (releaseStoreFile != null && !file(releaseStoreFile).isFile) {
    throw GradleException("CHURCH_RELEASE_STORE_FILE must point to an existing keystore outside the source repository.")
}

android {
    namespace = "org.cmf.churchconnect"
    compileSdk = 35
    defaultConfig {
        applicationId = "org.cmf.churchconnect"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "CHURCH_ID", "\"${configuredChurchId.replace("\"", "\\\"")}\"")
        buildConfigField("boolean", "USE_FIREBASE_EMULATORS", useFirebaseEmulators.toString())
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.create("churchRelease") {
                    storeFile = file(releaseStoreFile)
                    storePassword = releaseStorePassword
                    keyAlias = releaseKeyAlias
                    keyPassword = releaseKeyPassword
                }
            }
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.zxing:core:3.5.3")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
