import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.pomori"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.pomori"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // İmza bilgisi repoda tutulmaz: yerelde keystore.properties, CI'da ortam değişkenleri.
    // İkisi de yoksa release derlemesi imzasız çıkar (debug/test akışı bozulmaz).
    val keystoreProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    fun signingValue(propKey: String, envKey: String): String? =
        keystoreProps.getProperty(propKey) ?: System.getenv(envKey)

    val releaseStoreFile = signingValue("storeFile", "POMORI_KEYSTORE_FILE")
    val hasReleaseSigning = releaseStoreFile != null &&
        signingValue("storePassword", "POMORI_KEYSTORE_PASSWORD") != null &&
        signingValue("keyAlias", "POMORI_KEY_ALIAS") != null &&
        signingValue("keyPassword", "POMORI_KEY_PASSWORD") != null

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = signingValue("storePassword", "POMORI_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "POMORI_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "POMORI_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
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
    }
    // MigrationTestHelper, eski sürüm şemalarını (schemas/.../N.json) test varlıklarından okur.
    sourceSets {
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }
}

// Room şema dosyalarını repoya yazar (migration testleri için)
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore, WorkManager, Coroutines
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    // @HiltWorker: worker'lara Hilt bağımlılıklarını enjekte eder
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.kotlinx.coroutines.android)

    // Unit test
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // MigrationTestHelper (room-testing 2.8.5) kotlinx-serialization 1.8.x ile derlendi; AGP test sınıf yolunu
    // uygulamanın çalışma zamanı sürümüne kilitlediği için burada yükseltmezsek 1.7.3 kalır ve
    // AbstractMethodError ile düşer. Constraint yeni bağımlılık eklemez, yalnızca sürümü yükseltir.
    constraints {
        implementation(libs.kotlinx.serialization.core)
    }
}
