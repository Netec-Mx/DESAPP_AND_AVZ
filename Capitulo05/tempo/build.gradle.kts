// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Cap02
    alias(libs.plugins.kotlin.serialization) apply false

    // Cap03

    // Plugin de Hilt para la inyección de dependencias
    id("com.google.dagger.hilt.android") version "2.60.1" apply false

    // Herramienta KSP para procesar anotaciones con rendimiento optimizado
    id("com.google.devtools.ksp") version "2.3.6" apply false

    // false indica que no se activan en el proyecto principal

    // Cap05
    alias(libs.plugins.room) apply false
}