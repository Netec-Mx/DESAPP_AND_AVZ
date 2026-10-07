# Laboratorio Final

## Descripción General

En esta práctica final integradora, consolidarás todos los conocimientos técnicos adquiridos a lo largo del curso. Diseñarás y construirás un sistema completo, asíncrono y tolerante a fallos que recolecta coordenadas geográficas en tiempo real desde el servicio de geolocalización del dispositivo, las almacena de forma persistente y local mediante una base de datos Room 2.8.4, y enriquece cada registro de ubicación mediante consultas REST asíncronas a una API meteorológica externa utilizando Retrofit 3.0.0-alpha y OkHttp 5.5.0-alpha. La interfaz de usuario estará completamente implementada en Jetpack Compose, estructurada bajo el patrón arquitectónico Clean Architecture / MVVM, y optimizada para la estabilidad de renderizado. Asimismo, aprenderás a interactuar de forma guiada con JetBrains AI Assistant para acelerar las tareas de codificación repetitiva (como TypeConverters y esquemas DAO) y la generación exhaustiva de pruebas unitarias basadas en flujos.

[VISUAL: 07-01-0002 - Diagrama de arquitectura de datos unificado: El servicio de ubicación y Retrofit alimentan la base de datos Room (Single Source of Truth), la cual expone flujos de datos asíncronos (Flows) al ViewModel para renderizar la UI reactiva en Jetpack Compose.]

## Objetivos de Aprendizaje

Al finalizar este laboratorio, serás capaz de:
- [ ] **Diseñar e implementar** una arquitectura Clean Architecture / MVVM desacoplada utilizando el patrón Repository y fuentes de datos locales/remotas unificadas.
- [ ] **Configurar y optimizar** una base de datos local utilizando Room 2.8.4 con procesamiento de anotaciones mediante KSP y conversores de tipos personalizados.
- [ ] **Consumir y parsear** APIs REST asíncronas de manera concurrente con Retrofit 3.0.0-alpha y OkHttp 5.5.0-alpha, administrando timeouts, intercepciones y reintentos.
- [ ] **Gestionar estados complejos** de UI en un flujo unidireccional utilizando `StateFlow` y `SharedFlow` para la transmisión de eventos de un solo uso.
- [ ] **Utilizar de manera guiada** JetBrains AI Assistant para optimizar estructuras de bases de datos, generar pruebas unitarias de integración y diagnosticar cuellos de botella en la renderización.

## Prerrequisitos

Para completar este laboratorio con éxito, requieres:
1. **Conocimientos teóricos previos**:
   - Arquitectura de software MVVM en Android.
   - Manejo de asincronía avanzada mediante Kotlin Coroutines (Scopes, Dispatchers y combinadores de flujos).
   - Estructuración de layouts declarativos con Jetpack Compose y optimización del ciclo de vida del estado.
2. **Acceso a plataformas y licencias**:
   - Licencia activa o de evaluación de **JetBrains AI Assistant** configurada y autenticada en el IDE.
   - Conexión ilimitada a Internet para descargar el catálogo de dependencias de Maven y realizar consultas HTTP directas a la API pública de climatología.

## Entorno de Laboratorio

### Requisitos de Hardware

| Componente | Requisito Mínimo | Requisito Recomendado |
| :--- | :--- | :--- |
| **Procesador** | Intel Core i7 / AMD Ryzen 7 (11ª Gen) o Apple Silicon (M1/M2/M3) | Apple Silicon M3 Pro/Max o AMD Ryzen 9 |
| **Memoria RAM** | 16 GB | 32 GB |
| **Almacenamiento** | SSD con 40 GB libres de espacio dedicado | SSD NVMe PCIe 4.0 con 80 GB libres |

### Requisitos de Software

| Herramienta / SDK | Versión Exacta | Origen / Enlace Oficial | Licencia |
| :--- | :--- | :--- | :--- |
| **Android Studio** | Ladybug (2024.2.1 Patch 3) | [Android Developer](https://developer.android.com/studio) | Free / Apache 2.0 |
| **JDK** | Eclipse Temurin JDK 17.0.10+7 | [Adoptium](https://adoptium.net) | GPLv2+CE |
| **Kotlin Compiler** | 2.3.10 | [Kotlin Github](https://github.com/JetBrains/kotlin) | Apache 2.0 |
| **Room Database** | 2.8.4 | [Android Jetpack](https://developer.android.com/jetpack) | Apache 2.0 |
| **Retrofit** | 3.0.0-alpha | [Square Github](https://github.com/square/retrofit) | Apache 2.0 |
| **OkHttp** | 5.5.0-alpha | [Square Github](https://github.com/square/okhttp) | Apache 2.0 |
| **JetBrains AI Assistant** | 242.23339 | [JetBrains](https://www.jetbrains.com/ai/) | Licencia Comercial / Edu |

### Configuración de Variables del Entorno

Asegúrate de que tu archivo `gradle.properties` global o de proyecto tenga configurados los siguientes parámetros para optimizar la compilación:

```properties
org.gradle.jvmargs=-Xmx4096m -XX:+UseParallelGC
kotlin.code.style=official
android.useAndroidX=true
android.nonTransitiveRClass=true
```

## Instrucciones Paso a Paso

### Paso 1: Sincronizar Dependencias en Gradle con Gradle Version Catalog

**Objetivo:** Configurar de forma unificada las dependencias del proyecto integrador utilizando las versiones exactas solicitadas, garantizando la compatibilidad entre el compilador de Kotlin 2.3.10 y el compilador de KSP (Kotlin Symbol Processing).

**Instrucciones:**

1. Abre tu archivo `gradle/libs.versions.toml` y actualiza/añade las versiones de las librerías estratégicas:

```toml
[versions]
agp = "8.4.0"
kotlin = "2.3.10"
ksp = "2.3.10-1.0.20" # Asegura compatibilidad 1:1 con Kotlin 2.3.10
composeBom = "2026.02.01"
room = "2.8.4"
retrofit = "3.0.0-alpha"
okhttp = "5.5.0-alpha"
playServicesLocation = "21.4.0"
mapsCompose = "4.3.3"
hilt = "2.51.1"

[libraries]
## Core Android & Compose
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", value = "1.12.0" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", value = "2.7.0" }

## Room (Local Storage)
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

## Retrofit & Network
retrofit-core = { group = "com.squareup.retrofit3", name = "retrofit", version.ref = "retrofit" }
retrofit-converter-gson = { group = "com.squareup.retrofit3", name = "converter-gson", version.ref = "retrofit" }
okhttp-core = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }

## Play Services & Maps
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }
maps-compose = { group = "com.google.maps.android", name = "maps-compose", version.ref = "mapsCompose" }

## Hilt (Dependency Injection)
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

2. Aplica los plugins y las dependencias en el archivo `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.advancedtracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.advancedtracker"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11" // Compatible con Kotlin 2.x/2.3
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit & OkHttp
    implementation(libs.retrofit-core)
    implementation(libs.retrofit-converter-gson)
    implementation(platform(libs.okhttp.core))
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    // Maps & Location
    implementation(libs.play.services-location)
    implementation(libs.maps-compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Local Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.mockk:mockk:1.13.10")
}
```

3. Sincroniza Gradle (`Sync Project with Gradle Files`) en Android Studio.

**Output esperado:** La sincronización de Gradle debe terminar con éxito, sin errores de compatibilidad entre KSP y Kotlin 2.3.10.

**Verificación:** Ejecuta en la terminal de Android Studio el comando `./gradlew app:dependencies --configuration compileClasspath` para asegurar que todas las librerías se resuelven correctamente sin dependencias circulares u obsoletas.

---

### Paso 2: Persistencia de Datos con Room 2.8.4 y JetBrains AI Assistant

**Objetivo:** Crear el esquema relacional local `telemetry_db` y su DAO para almacenar coordenadas y variables climatológicas locales. Se utilizará JetBrains AI Assistant de forma interactiva para generar los convertidores de tipo complejos requeridos para guardar estructuras anidadas.

**Instrucciones:**

1. Crea el paquete `com.example.advancedtracker.data.local.entity` e implementa la entidad de persistencia `LocationTelemetryEntity.kt`:

```kotlin
package com.example.advancedtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telemetry_records")
data class LocationTelemetryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val temperature: Double?,
    val weatherCondition: String?,
    val windSpeed: Double?
)
```

2. Abre la ventana de chat de **JetBrains AI Assistant** (`View -> Tool Windows -> AI Assistant`).
3. Envía el siguiente prompt de diseño asistido para generar un convertidor de tipos seguro (`TypeConverter`) en caso de necesitar persistir listas o estructuras de datos complejas en futuros desarrollos:

> **Prompt para JetBrains AI Assistant:**
> *"Actúa como un Ingeniero Principal de Android y genera un Room TypeConverter robusto en Kotlin que serialice y deserialice un objeto personalizado llamado `WeatherDetails` (que contiene `humidity: Int`, `pressure: Double` y `uvIndex: Double`) hacia y desde un formato de texto plano JSON utilizando la biblioteca Gson compatible con Room 2.8.4. Asegura la prevención de Memory Leaks y manejo estructurado de excepciones NullPointerException."*

4. Revisa y adapta el código generado por la IA en un nuevo archivo `WeatherTypeConverters.kt` dentro de `com.example.advancedtracker.data.local.converters`:

```kotlin
package com.example.advancedtracker.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class WeatherDetails(
    val humidity: Int,
    val pressure: Double,
    val uvIndex: Double
)

class WeatherTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromWeatherDetails(details: WeatherDetails?): String? {
        if (details == null) return null
        return try {
            gson.toJson(details)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun toWeatherDetails(json: String?): WeatherDetails? {
        if (json.isNullOrEmpty()) return null
        return try {
            val type = object : TypeToken<WeatherDetails>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }
}
```

5. Crea la interfaz `LocationDao.kt` en `com.example.advancedtracker.data.local.dao`:

```kotlin
package com.example.advancedtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.advancedtracker.data.local.entity.LocationTelemetryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationTelemetryEntity): Long

    @Query("SELECT * FROM telemetry_records ORDER BY timestamp DESC")
    fun getAllLocationsFlow(): Flow<List<LocationTelemetryEntity>>

    @Query("DELETE FROM telemetry_records")
    suspend fun clearTelemetryData()
}
```

6. Configura el archivo base de la base de datos `TelemetryDatabase.kt` en `com.example.advancedtracker.data.local`:

```kotlin
package com.example.advancedtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.advancedtracker.data.local.dao.LocationDao
import com.example.advancedtracker.data.local.converters.WeatherTypeConverters
import com.example.advancedtracker.data.local.entity.LocationTelemetryEntity

@Database(
    entities = [LocationTelemetryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(WeatherTypeConverters::class)
abstract class TelemetryDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}
```

**Output esperado:** La base de datos compile de forma adecuada y el procesador de KSP genere el archivo `TelemetryDatabase_Impl.java` automáticamente en el directorio build de compilación del proyecto.

**Verificación:** Compila el proyecto con `Build -> Make Project`. No debe haber errores de conversión de tipos ni problemas de definición en la estructura del DAO.

---

### Paso 3: Construir el Cliente de Red con Retrofit 3.0.0 y OkHttp 5.5.0

**Objetivo:** Implementar la capa de comunicación HTTP para consumir el servicio meteorológico externo de forma asíncrona, segura y resiliente aplicando políticas de Timeout.

**Instrucciones:**

1. Define los modelos de transferencia de datos (DTO) en `com.example.advancedtracker.data.remote.model`:

```kotlin
package com.example.advancedtracker.data.remote.model

import com.google.gson.annotations.SerializedName

data class WeatherResponseDto(
    @SerializedName("current_weather") val currentWeather: CurrentWeatherDto
)

data class CurrentWeatherDto(
    @SerializedName("temperature") val temperature: Double,
    @SerializedName("windspeed") val windSpeed: Double,
    @SerializedName("weathercode") val weatherCode: Int
)
```

2. Crea la interfaz del servicio API REST `WeatherApiService.kt` en `com.example.advancedtracker.data.remote`:

```kotlin
package com.example.advancedtracker.data.remote

import com.example.advancedtracker.data.remote.model.WeatherResponseDto
import retrofit3.http.GET
import retrofit3.http.Query

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true
    ): WeatherResponseDto
}
```

*Nota técnica:* Las clases de anotación en Retrofit 3.0.0 pertenecen al paquete `retrofit3.http` en lugar del clásico `retrofit2.http`.

3. Implementa el módulo de inyección de dependencias `NetworkModule.kt` en `com.example.advancedtracker.di` utilizando un cliente HTTP robusto de OkHttp 5.5.0 configurado para mitigar problemas de red inestable:

```kotlin
package com.example.advancedtracker.di

import android.content.Context
import androidx.room.Room
import com.example.advancedtracker.data.local.TelemetryDatabase
import com.example.advancedtracker.data.remote.WeatherApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit3.Retrofit
import retrofit3.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherApiService(okHttpClient: OkHttpClient): WeatherApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTelemetryDatabase(@ApplicationContext context: Context): TelemetryDatabase {
        return Room.databaseBuilder(
            context,
            TelemetryDatabase::class.java,
            "telemetry_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideLocationDao(database: TelemetryDatabase) = database.locationDao()
}
```

**Output esperado:** Todas las dependencias quedan expuestas de manera unificada mediante Dagger Hilt utilizando las clases de inicialización actualizadas de Retrofit 3 y OkHttp 5.

**Verificación:** Compila el módulo para comprobar que no existan discrepancias entre las firmas de métodos de Retrofit y el inyector de dependencias.

---

### Paso 4: Implementar el Repositorio y la Lógica Offline-First

**Objetivo:** Desarrollar el componente de integración empresarial (Repository) que coordine de forma síncrona la escritura local en la base de datos y la recolección remota de los cambios meteorológicos ambientales.

**Instrucciones:**

1. Modela el dominio de negocio limpio `LocationTelemetry.kt` en `com.example.advancedtracker.domain.model`:

```kotlin
package com.example.advancedtracker.domain.model

data class LocationTelemetry(
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val temperature: Double,
    val weatherCondition: String,
    val windSpeed: Double
)
```

2. Crea la interfaz `LocationWeatherRepository.kt` en `com.example.advancedtracker.domain.repository`:

```kotlin
package com.example.advancedtracker.domain.repository

import com.example.advancedtracker.domain.model.LocationTelemetry
import kotlinx.coroutines.flow.Flow

interface LocationWeatherRepository {
    fun getTelemetryStream(): Flow<List<LocationTelemetry>>
    suspend fun recordTelemetry(latitude: Double, longitude: Double)
    suspend fun clearHistory()
}
```

3. Desarrolla la implementación del repositorio en la capa de datos `LocationWeatherRepositoryImpl.kt` en `com.example.advancedtracker.data.repository`:

```kotlin
package com.example.advancedtracker.data.repository

import com.example.advancedtracker.data.local.dao.LocationDao
import com.example.advancedtracker.data.local.entity.LocationTelemetryEntity
import com.example.advancedtracker.data.remote.WeatherApiService
import com.example.advancedtracker.domain.model.LocationTelemetry
import com.example.advancedtracker.domain.repository.LocationWeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationWeatherRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao,
    private val weatherApiService: WeatherApiService
) : LocationWeatherRepository {

    override fun getTelemetryStream(): Flow<List<LocationTelemetry>> {
        return locationDao.getAllLocationsFlow().map { list ->
            list.map { entity ->
                LocationTelemetry(
                    id = entity.id,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    timestamp = entity.timestamp,
                    temperature = entity.temperature ?: 0.0,
                    weatherCondition = entity.weatherCondition ?: "Desconocido",
                    windSpeed = entity.windSpeed ?: 0.0
                )
            }
        }
    }

    override suspend fun recordTelemetry(latitude: Double, longitude: Double) {
        val timestamp = System.currentTimeMillis()
        var temp: Double? = null
        var condition: String? = null
        var wind: Double? = null

        try {
            // Intenta obtener los datos del servidor remoto
            val response = weatherApiService.getWeatherForecast(latitude, longitude)
            temp = response.currentWeather.temperature
            wind = response.currentWeather.windSpeed
            condition = mapWeatherCode(response.currentWeather.weatherCode)
        } catch (e: Exception) {
            // Manejo de error estructurado offline-first: persistimos las coordenadas con clima por defecto
            temp = 0.0
            wind = 0.0
            condition = "Fuera de línea (Error de red)"
        } finally {
            val entity = LocationTelemetryEntity(
                latitude = latitude,
                longitude = longitude,
                timestamp = timestamp,
                temperature = temp,
                weatherCondition = condition,
                windSpeed = wind
            )
            locationDao.insertLocation(entity)
        }
    }

    override suspend fun clearHistory() {
        locationDao.clearTelemetryData()
    }

    private fun mapWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Cielo despejado"
            1, 2, 3 -> "Parcialmente nublado"
            45, 48 -> "Niebla húmeda"
            51, 53, 55 -> "Llovizna leve"
            61, 63, 65 -> "Lluvia persistente"
            71, 73, 75 -> "Nevada ligera"
            80, 81, 82 -> "Chubascos"
            95, 96, 99 -> "Tormenta eléctrica"
            else -> "Código de Clima: $code"
        }
    }
}
```

4. Vincula la interfaz con su implementación concreta en un módulo Hilt `RepositoryModule.kt` en `com.example.advancedtracker.di`:

```kotlin
package com.example.advancedtracker.di

import com.example.advancedtracker.data.repository.LocationWeatherRepositoryImpl
import com.example.advancedtracker.domain.repository.LocationWeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocationWeatherRepository(
        impl: LocationWeatherRepositoryImpl
    ): LocationWeatherRepository
}
```

**Output esperado:** Un flujo unidireccional estructurado en el que cada evento de red es controlado mediante bloques `try-catch-finally`, asegurando la inmutabilidad y persistencia de las coordenadas del usuario incluso si la red del operador celular decae de golpe.

**Verificación:** Compila el módulo general de la aplicación móvil. El motor de compilación Hilt resolverá de forma exitosa las implementaciones enlazadas mediante `@Binds`.

---

### Paso 5: Construir el DashboardViewModel y la UI Reactiva con Jetpack Compose

**Objetivo:** Desarrollar la lógica de negocio reactiva del ViewModel que expone el estado de UI encapsulado mediante `StateFlow` y diseñar un Dashboard interactivo usando Jetpack Compose y Material Design 3.

**Instrucciones:**

1. Diseña el estado completo de la UI en `com.example.advancedtracker.ui.dashboard.state` mediante una clase sellada (`sealed interface`):

```kotlin
package com.example.advancedtracker.ui.dashboard.state

import com.example.advancedtracker.domain.model.LocationTelemetry

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(val history: List<LocationTelemetry>) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}
```

2. Implementa el componente central de negocio `DashboardViewModel.kt` en `com.example.advancedtracker.ui.dashboard`:

```kotlin
package com.example.advancedtracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedtracker.domain.repository.LocationWeatherRepository
import com.example.advancedtracker.ui.dashboard.state.DashboardUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: LocationWeatherRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = repository.getTelemetryStream()
        .map { history ->
            if (history.isEmpty()) {
                DashboardUiState.Success(emptyList())
            } else {
                DashboardUiState.Success(history)
            }
        }
        .catch { e -> emit(DashboardUiState.Error(e.localizedMessage ?: "Error de base de datos")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )

    fun recordNewLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            repository.recordTelemetry(latitude, longitude)
        }
    }

    fun clearTelemetryHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
```

3. Crea la pantalla principal declarativa `DashboardScreen.kt` en `com.example.advancedtracker.ui.dashboard.views`. Esta pantalla integra un `Scaffold`, visualiza la lista de telemetría y ofrece botones de control rápido con Material Design 3:

```kotlin
package com.example.advancedtracker.ui.dashboard.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.advancedtracker.domain.model.LocationTelemetry
import com.example.advancedtracker.ui.dashboard.DashboardViewModel
import com.example.advancedtracker.ui.dashboard.state.DashboardUiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapeo y Telemetría Offline-First", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val currentState = state) {
                is DashboardUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is DashboardUiState.Error -> {
                    Text(
                        text = "Error de sincronización: ${currentState.message}",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                is DashboardUiState.Success -> {
                    DashboardContent(
                        history = currentState.history,
                        onSimulateRecord = {
                            // Simulador de cambio de posicionamiento aleatorio (Madrid como base)
                            val latRandom = 40.416775 + (Math.random() - 0.5) * 0.1
                            val lonRandom = -3.703790 + (Math.random() - 0.5) * 0.1
                            viewModel.recordNewLocation(latRandom, lonRandom)
                        },
                        onClearAll = { viewModel.clearTelemetryHistory() }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    history: List<LocationTelemetry>,
    onSimulateRecord: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tarjetas de control rápido
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onSimulateRecord,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Grabar")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Capturar GPS")
            }

            Button(
                onClick = onClearAll,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Borrar Historial")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Historial de Eventos Geolocalizados (${history.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = history,
                key = { telemetry -> telemetry.id } // Clave única que evita recomposiciones innecesarias
            ) { telemetry ->
                TelemetryCard(item = telemetry)
            }
        }
    }
}

@Composable
fun TelemetryCard(item: LocationTelemetry, modifier: Modifier = Modifier) {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val formattedDate = formatter.format(Date(item.timestamp))

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Pin",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.latitude.toString().take(8)}, ${item.longitude.toString().take(8)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Clima", style = MaterialTheme.typography.labelSmall)
                    Text(text = item.weatherCondition, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                Column {
                    Text(text = "Temperatura", style = MaterialTheme.typography.labelSmall)
                    Text(text = "${item.temperature} °C", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                Column {
                    Text(text = "Viento", style = MaterialTheme.typography.labelSmall)
                    Text(text = "${item.windSpeed} km/h", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
```

4. Define tu clase `MainActivity.kt` en `com.example.advancedtracker` para que sirva de base a Dagger Hilt y levante la interfaz unificada:

```kotlin
package com.example.advancedtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.advancedtracker.ui.dashboard.DashboardViewModel
import com.example.advancedtracker.ui.dashboard.views.DashboardScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}
```

5. Adiciona el punto de entrada de la aplicación global `@HiltAndroidApp` creando `TrackerApplication.kt` en `com.example.advancedtracker`:

```kotlin
package com.example.advancedtracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TrackerApplication : Application()
```

6. Asegura la declaración correcta de los componentes en tu archivo `AndroidManifest.xml` (incluyendo conectividad a Internet y nombres del Application):

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://tools.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <application
        android:name=".TrackerApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="Advanced Location Tracker"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Material3.DayNight.NoActionBar"
        tools:targetApi="35">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Material3.DayNight.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

**Output esperado:** Al presionar el botón "Capturar GPS", el sistema simulará una nueva ubicación con latitud y longitud, guardará las variables locales en Room, resolverá en segundo plano los datos climatológicos en la nube de forma asíncrona y actualizará fluidamente la interfaz reactiva del usuario en la pantalla del dispositivo móvil.

**Verificación:** Despliega la aplicación en tu emulador o dispositivo físico. Presiona repetidamente "Capturar GPS". Deben crearse tarjetas con datos de climatología reales obtenidos de manera dinámica.

---

### Paso 6: Generar Pruebas Unitarias del ViewModel con JetBrains AI Assistant

**Objetivo:** Desarrollar el archivo de pruebas asíncronas para el `DashboardViewModel` interactuando de manera iterativa con la Inteligencia Artificial de JetBrains para asegurar la cobertura lógica de emisiones de flujos.

**Instrucciones:**

1. Abre el archivo `DashboardViewModel.kt` en el IDE de Android Studio.
2. Selecciona la definición completa del ViewModel. Abre la barra lateral de **JetBrains AI Assistant** y ejecuta el siguiente flujo interactivo mediante la ventana de chat:

> **Prompt para JetBrains AI Assistant:**
> *"Genera la suite de pruebas unitarias en Kotlin con JUnit 4, Coroutines TestDispatcher de kotlinx-coroutines-test y MockK para el `DashboardViewModel` de mi proyecto Android. Debe evaluar tres flujos claves:
> 1) La inicialización del ViewModel emite el estado inicial `DashboardUiState.Loading` y pasa con éxito a `DashboardUiState.Success` al cargarse la telemetría del repositorio.
> 2) `recordNewLocation` llama correctamente al método `recordTelemetry` del repositorio.
> 3) El flujo de datos captura errores y emite correctamente `DashboardUiState.Error`.
> Por favor, escribe código limpio que use la anotación `@OptIn(ExperimentalCoroutinesApi::class)`."*

3. Adapta y crea el archivo de pruebas unitarias generado en `src/test/java/com/example/advancedtracker/ui/dashboard/DashboardViewModelTest.kt`:

```kotlin
package com.example.advancedtracker.ui.dashboard

import com.example.advancedtracker.domain.model.LocationTelemetry
import com.example.advancedtracker.domain.repository.LocationWeatherRepository
import com.example.advancedtracker.ui.dashboard.state.DashboardUiState
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val repository: LocationWeatherRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when init model, if repository flow has data, state transitions to Success`() = runTest {
        // Arrange
        val telemetryList = listOf(
            LocationTelemetry(1, 40.41, -3.70, 1000L, 15.0, "Cielo Despejado", 12.0)
        )
        coEvery { repository.getTelemetryStream() } returns flowOf(telemetryList)

        // Act
        val viewModel = DashboardViewModel(repository)
        
        // Assert: Estado inicial esperado es Loading
        assertEquals(DashboardUiState.Loading, viewModel.uiState.value)

        // Ejecutar los coroutines pendientes
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert final: Estado exitoso con lista
        val finalState = viewModel.uiState.value
        assert(finalState is DashboardUiState.Success)
        assertEquals(telemetryList, (finalState as DashboardUiState.Success).history)
    }

    @Test
    fun `when recordNewLocation is called, repository triggers recordTelemetry`() = runTest {
        // Arrange
        val viewModel = DashboardViewModel(repository)
        val lat = 40.0
        val lon = -3.0

        // Act
        viewModel.recordNewLocation(lat, lon)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { repository.recordTelemetry(lat, lon) }
    }

    @Test
    fun `when repository throws exception, flow emits Error state`() = runTest {
        // Arrange
        coEvery { repository.getTelemetryStream() } returns flow {
            throw RuntimeException("Database corruption")
        }

        // Act
        val viewModel = DashboardViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val finalState = viewModel.uiState.value
        assert(finalState is DashboardUiState.Error)
        assertEquals("Database corruption", (finalState as DashboardUiState.Error).message)
    }
}
```

**Output esperado:** La suite de pruebas unitarias simula correctamente los eventos concurrentes y pasa los tres casos lógicos planteados.

**Verificación:** Haz clic derecho sobre el archivo `DashboardViewModelTest.kt` y selecciona `Run 'DashboardViewModelTest'`. Todos los tests deben figurar en verde.

---

## Validación y Pruebas

Para garantizar que tu aplicación de telemetría geolocalizada con capacidad de cacheo meteorológico y offline-first opera con el nivel de calidad que exigen los entornos de producción modernos, ejecuta las siguientes validaciones críticas de arquitectura:

### Caso de Prueba 1: Sincronización Exitosa de Telemetría Climatológica (Online)
1. Asegura que tu dispositivo o emulador Android tenga habilitado el acceso a redes móviles o Wi-Fi.
2. Inicia la aplicación. Presiona el botón **Capturar GPS**.
3. Verifica en el terminal mediante Logcat que la llamada REST no arroje excepciones HTTP 403 o 404:
   ```bash
   adb logcat | grep -i "okhttp"
   ```
4. Comprueba que la tarjeta agregada renderiza un estado meteorológico real (ej. "Cielo despejado", "Llovizna leve") y una temperatura distinta a los valores por defecto (0.0).

### Caso de Prueba Adverso: Resiliencia Ante Ausencia de Red (Offline)
1. Coloca el dispositivo móvil o el emulador en **Modo Avión** (desconexión completa de red celular, satelital y Wi-Fi).
2. Presiona de nuevo el botón **Capturar GPS**.
3. **Validación Esperada:** El sistema no debe crashear, congelar la interfaz táctil ni mostrar diálogos de fallo críticos. Debe procesar la excepción en la capa del Repositorio de manera limpia.
4. **Verificación Visual:** La UI insertará de inmediato una tarjeta con los datos de posición del usuario, mostrando en la sección Clima: *"Fuera de línea (Error de red)"*, conservando los datos de latitud y longitud intactos.

---

## Solución de Problemas

A continuación, se documentan las dos incidencias técnicas más comunes que pueden ocurrir durante el desarrollo o compilación de esta práctica y cómo solucionarlas:

### Incidencia 1: Error de compilación por inconsistencias de KSP con Kotlin Compiler 2.3.10
* **Surgimiento:** Ocurre al sincronizar las tareas Gradle con el comando compile, reportando un error similar a `ksp-2.3.10-1.x is incompatible with kotlin-2.3.10`.
* **Causa:** El compilador KSP requiere de una versión específica mapeada minuciosamente a la versión de lenguaje de Kotlin (v2.3.10). El uso de placeholders como `latest` o `+` rompe la coherencia del motor de procesamiento de anotaciones de Room.
* **Solución:** Fuerza la definición estricta en tu archivo de Catálogo de Versiones `gradle/libs.versions.toml`:
  ```toml
  kotlin = "2.3.10"
  ksp = "2.3.10-1.0.20" # Comprueba el mapeo del release en GitHub de KSP
  ```
  Limpia la caché de compilación previa utilizando la terminal de comandos:
  ```bash
  ./gradlew clean
  ```

### Incidencia 2: Inconsistencias de inyección de dependencias en tiempo de ejecución (`IllegalStateException: Hilt can't inject components`)
* **Surgimiento:** La aplicación crashea de inmediato durante su arranque inicial (`onCreate`) arrojando un error de inicialización en el inyector.
* **Causa:** Falta declarar el nombre personalizado de la aplicación (`android:name=".TrackerApplication"`) en el nodo raíz de configuración dentro del archivo de manifiesto `AndroidManifest.xml` o se omitió la anotación `@HiltAndroidApp` en dicha clase.
* **Solución:** 
  1. Verifica que la clase `TrackerApplication` herede directamente de `android.app.Application` y posea la etiqueta `@HiltAndroidApp`.
  2. Abre el archivo `AndroidManifest.xml` y cerciórate de que el tag `<application>` contenga la propiedad de puntero de clase correspondiente:
     ```xml
     <application
         android:name=".TrackerApplication"
         ... >
     ```

---

## Limpieza

Si requieres resetear por completo tu espacio de almacenamiento local o inicializar tu base de datos de desarrollo a un estado inicial puro sin cambiar el ID del emulador, realiza los siguientes pasos de saneamiento:

1. Limpia los datos de compilación generados por Gradle en el proyecto:
   ```bash
   ./gradlew clean
   ```
2. Desinstala la aplicación móvil del emulador Android para erradicar por completo la base de datos `telemetry_db` y evitar problemas por migraciones faltantes:
   ```bash
   adb uninstall com.example.advancedtracker
   ```
3. Realiza un borrado de caché frío en tu emulador desde el Administrador de Dispositivos de Android Studio seleccionando el dispositivo actual y ejecutando la opción `Wipe Data`.

---

## Resumen

En esta práctica final integradora has alcanzado los siguientes hitos de ingeniería Android de nivel avanzado:

- **Estructuración MVVM Unidireccional:** Creaste un ciclo de vida de flujo en el que la interfaz de usuario se mantiene libre de acoplamientos, delegando la recopilación en tiempo real, el mapeo de datos y el control de llamadas REST a un ViewModel y un Repositorio desacoplados.
- **Persistencia Local con Room 2.8.4:** Diseñaste esquemas relacionales, habilitaste el procesamiento de anotaciones asistido por KSP y aprendiste a usar TypeConverters personalizados para transformar estructuras complejas en JSON con la ayuda guiada de JetBrains AI Assistant.
- **Redes Seguras con Retrofit 3.0.0 y OkHttp 5.5.0:** Implementaste llamadas a servicios externos preparadas para operar de forma fluida ante variaciones en el estado de red de los usuarios (offline-first).
- **Pruebas Unitarias de Flujos:** Implementaste suites de pruebas asíncronas para evaluar las transiciones de estados de UI utilizando herramientas de testeo modernas como Coroutines Test Dispatcher y MockK.
