plugins { id("com.android.application"); id("org.jetbrains.kotlin.plugin.compose") }
android {
    namespace = "com.cbdog.ptvbox"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.cbdog.ptvbox"
        minSdk = 26
        targetSdk = 35
        versionCode = 24
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    packaging.resources.merges += setOf("META-INF/LICENSE.md", "META-INF/NOTICE.md")
    sourceSets.getByName("main").jniLibs.setSrcDirs(if(providers.gradleProperty("ptvboxArmHost").orNull=="true")listOf("build/armhost-libs")else emptyList<String>())
    if(providers.gradleProperty("ptvboxArmHost").orNull=="true")packaging.jniLibs.excludes += setOf("**/x86/*.so","**/x86_64/*.so")
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    signingConfigs {
        create("releaseLocal") {
            val filePath = providers.environmentVariable("PTVBOX_KEYSTORE").orNull
            if (filePath != null) {
                storeFile = file(filePath)
                storePassword = providers.environmentVariable("PTVBOX_STORE_PASSWORD").orNull
                keyAlias = "ptvbox"
                keyPassword = providers.environmentVariable("PTVBOX_KEY_PASSWORD").orNull
            }
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("releaseLocal") }
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.tv:tv-material:1.1.0")
    implementation("androidx.tvprovider:tvprovider:1.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.11.1")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:5.5.0")
    implementation("io.github.biezhi:TinyPinyin:2.0.3.RELEASE")
    implementation("com.google.zxing:core:3.5.3")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.87")
    implementation("com.google.code.gson:gson:2.15.0")
    implementation("org.jsoup:jsoup:1.24.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
