plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // cap02
    alias(libs.plugins.kotlin.serialization)

    // cap03
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")

    // cap05
    alias(libs.plugins.room)
}

android {
    namespace = "com.example.advancedtracker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.advancedtracker"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


    }

    buildTypes {
        // Cap 04
        getByName("debug") {
            enableUnitTestCoverage = true
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // Cap 04
        buildConfig = true
   }
}

// Cap 05
room {
    schemaDirectory("$projectDir/schemas")
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
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Cap01 necesario para pruebas unitarias, cap01.
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    // Cap02
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)

    // Cap03
    // Cap03: ifecycle components y recolección segura en Compose
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Cap03: Dagger Hilt para Inyección de Dependencias
    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-compiler:2.60.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Cap03: Coroutines de Kotlin para concurrencia asíncrona
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // Cap03: Componentes de prueba
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

    // Cap04
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.client)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.client)
    implementation(libs.retrofit.serialization)
    implementation(libs.coroutines.android)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.serialization.json)

    testImplementation(platform(libs.okhttp.bom))
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)

    // Cap05
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    androidTestImplementation(libs.room.testing)

    // Reutiliza tus aliases y versiones actuales si estos artefactos ya existen.
    androidTestImplementation("androidx.test:core-ktx:1.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")

    // Cap06
    implementation(libs.play.services.location)
    implementation(libs.maps.compose)
}