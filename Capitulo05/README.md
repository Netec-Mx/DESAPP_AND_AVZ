# Laboratorio 5.1. Persistencia avanzada con Room y DataStore

<br/>
<br/>

## Descripción general

Continúa sobre el proyecto **TelemetryTracker** que completó el laboratorio 4.1. Agrega una funcionalidad de consulta persistente que reutiliza su servicio Retrofit y conserva los siete campos de telemetría ya utilizados. Room será la fuente de lectura de esta funcionalidad; la API actualizará la base de datos. DataStore Preferences conservará el dispositivo seleccionado y la opción de consultar únicamente el almacenamiento local.

La práctica es incremental: agrega clases y configuraciones, sin sustituir los DTO, contratos, repositorios, ViewModel, navegación, autenticación o pantallas anteriores. Una actividad adicional de la variante `debug` permite verificar la persistencia sin modificar `MainActivity`.

**Alcance:** lectura offline-first, actualización manual desde la API, preferencias, migración manual y automática, y pruebas. La creación de registros por POST sigue funcionando en la pantalla anterior. En esta práctica no se implementan escrituras offline, una cola de sincronización ni sincronización con WorkManager.


<br/><br/>

## Objetivos

Al finalizar, podrás:

- Definir entidades, índices, un DAO reactivo y una base Room sobre el dominio de telemetría existente.
- Persistir preferencias mediante DataStore con operaciones suspendidas y lectura por `Flow`.
- Aplicar arquitectura limpia: dominio independiente de Room, Retrofit, Android y Hilt; infraestructura que implementa sus contratos; presentación que invoca casos de uso.
- Consultar datos locales antes de depender de la red y conservarlos ante errores HTTP o de conectividad.
- Ejecutar y comprobar una migración manual 1 → 2 y una automática 2 → 3 sin perder registros.
- Visualizar los datos de Room en Database Inspector y comprobar las preferencias tanto en pantalla como después de reiniciar el proceso.
- Revisar el código y las pruebas con asistencia de IA, de forma opcional y verificando sus sugerencias.

<br/><br/>

## Prerrequisitos

- Proyecto del laboratorio 4.1 compilando, pruebas anteriores pasando y consulta GET funcionando contra el servidor usado en esa práctica.
- Android Studio, emulador o dispositivo con API 31 o superior y acceso a las dependencias Maven del proyecto.
- Hilt y Compose ya configurados; `TrackerApplication` conserva `@HiltAndroidApp` y su registro en el manifiesto.
- Conocimientos de MVVM, corrutinas, `Flow`, `StateFlow`, SQL básico y separación de capas.
- Terminal PowerShell en Windows, situada en la raíz del proyecto. Los comandos Gradle de este documento usan `gradlew.bat`.


<br/><br/>

## Instrucciones

### Paso 1. Verificar y respaldar el proyecto funcional de 4.1

Antes de editar, guarda una copia del proyecto o un commit con el estado funcional de 4.1. Evita copiar carpetas de compilación si realizas un respaldo manual.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

**Resultado esperado:** compilación y pruebas existentes exitosas. Ejecuta también la aplicación anterior y comprueba su GET y POST. Si falla esta línea base, resuelve ese problema antes de agregar persistencia.

<br/><br/>

### Paso 2. Establecer qué elementos del proyecto se conservan

| Elemento existente de 4.1 | Uso en 5.1 |
| --- | --- |
| `domain.model.tracking.TelemetryData` | Se reutiliza sin modificar sus seis propiedades. |
| `data.remote.dto.RemoteTelemetryDto` y sus conversiones | Se conserva el contrato de red y el mapeo al dominio. |
| `data.remote.network.RemoteTelemetryApi` | Se reutiliza `fetchHistory(limit = 50)`; `sendRecord` permanece disponible. |
| `RemoteNetworkModule`, `RemoteNetworkFactory` y `RemoteAuthInterceptor` | Se conservan Retrofit, OkHttp, URL y mecanismo de autenticación existentes. |
| `RemoteRepositoryModule`, `RepositoryModule` y repositorios anteriores | Se conservan sus implementaciones y bindings Hilt. |
| ViewModel, pantallas, `MainActivity` y navegación anteriores | Permanecen operativos. |
| Modelos de los laboratorios 1–3 | Permanecen en sus paquetes actuales. |
| AGP, Kotlin, SDK, Java, Hilt, Compose y dependencias de red | Se conservan los valores del proyecto que ya compiló. |

Las preferencias nuevas de 5.1 utilizan el archivo independiente `telemetry_lab51_settings.preferences_pb`.

<br/>

#### Correspondencia exacta de datos

Se reutiliza el modelo existente `domain.model.tracking.TelemetryData`. El nombre `TelemetryRecord` utilizado en esta práctica será un `typealias` de ese modelo; no introduce otra clase de dominio.

| API y `RemoteTelemetryDto` | Dominio `TelemetryData` | Columna Room | Tipo SQLite |
| --- | --- | --- | --- |
| `id: String` | `id: String` | `id` | TEXT |
| `latitude: Double` | `latitude: Double` | `latitude` | REAL |
| `longitude: Double` | `longitude: Double` | `longitude` | REAL |
| `speedKmh: Double` | `speedKmh: Double` | `speed_kmh` | REAL |
| `timestamp: Long` | `timestamp: Date` | `timestamp` | INTEGER |
| `isMocked: Boolean` | `isMocked: Boolean` | `is_mocked` | INTEGER: 0 o 1 |

Se conserva la conversión existente: `Date(timestamp)` al pasar de la API al dominio y `timestamp.time` al regresar al DTO. El valor representa **milisegundos Unix**. Room almacena ese valor como `Long`; el mapper reconstruye el `Date` al leerlo.

El campo técnico `cachedAt`, agregado en el paso 14, registra cuándo se guarda o actualiza una lectura en la caché local. También utiliza milisegundos Unix, pero no sustituye a `timestamp` ni modifica el dominio o el DTO.

El modelo actual no contiene `deviceId` ni `status`. El filtro de esta práctica utiliza `isMocked`, conservando el valor recibido de la API.

<br/>

#### Servidor y configuración de red

Desde la raíz de `TelemetryTracker`, inicia el servidor compatible con estos seis campos:

```cmd
py -3 tools\telemetry_server_merged.py
```

Mantén esa terminal abierta. Si otro servidor ocupa el puerto 8080, detén ese proceso antes de ejecutar el comando.

Reutiliza la URL base `http://10.0.2.2:8080/v1/` y la autenticación configuradas en `RemoteNetworkModule`. Con `@GET("telemetry")`, la petición continúa siendo:

```text
GET /v1/telemetry?limit=50
```

<br/><br/>

### Paso 3. Agregar las dependencias necesarias para persistencia

1. Catálogo de versiones

En `gradle/libs.versions.toml`, incorpora las entradas que falten **dentro de las secciones existentes**. No copies una segunda sección `[versions]`, `[libraries]` o `[plugins]`.

```toml
# Dentro de [versions]
room = "2.8.4"
# Solo si no existe ya una versión/alias DataStore:
datastore = "1.1.2"

# Dentro de [libraries]
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
# Reutiliza tu alias DataStore si ya existe; no dupliques la dependencia.
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Dentro de [plugins]
room = { id = "androidx.room", version.ref = "room" }
```

Room 2.8.4 es la versión fijada para esta práctica, no una indicación de actualizar todas las bibliotecas a sus versiones más recientes. Si el proyecto ya tiene Room y DataStore funcionales, conserva sus versiones y reutiliza sus aliases; los artefactos de Room deben compartir una misma versión.

<br/><br/>

2. KSP y plugin Room

Si KSP ya está aplicado, úsalo con su versión existente. No cambies el procesador Hilt que ya funciona. Room usará exclusivamente `ksp(libs.room.compiler)`; no agregues también `kapt` para ese mismo compilador.

Solo si el proyecto todavía no tiene KSP, agrega estas entradas al catálogo:

```toml
# Dentro de [versions]
ksp = "2.3.6"

# Dentro de [plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

En el bloque `plugins` del `build.gradle.kts` raíz, agrega Room; agrega KSP solamente si falta. Si KSP ya se declara mediante `id(...)`, no declares además su alias.

```kotlin
// Añadir al bloque plugins existente de la raíz:
alias(libs.plugins.room) apply false
// Solo cuando KSP no estaba declarado:
alias(libs.plugins.ksp) apply false
```

En el bloque `plugins` de `app/build.gradle.kts`, agrega Room y, únicamente si falta, KSP:

```kotlin
// Añadir al bloque plugins existente del módulo:
alias(libs.plugins.room)
// Solo cuando KSP no estaba aplicado:  
alias(libs.plugins.ksp)
```

------------------

En `app/build.gradle.kts`, conserva el plugin Room dentro del bloque `plugins` existente:

```kotlin
alias(libs.plugins.room)
```

Fuera de `android`, a nivel del script del módulo, agrega este bloque **solo si todavía no existe**:

```kotlin
room {
    schemaDirectory("$projectDir/schemas")
}
```


Dentro de `android`, conserva el runner en el bloque `defaultConfig` existente:

```kotlin
android {
    // Conserva tus demás configuraciones.

    defaultConfig {
        // Conserva tus demás propiedades.
        // Añade esta línea solo si falta.
        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }
}
```


**Integra estas configuraciones en los bloques existentes, sin duplicarlos.**


Dentro de `dependencies`, agrega solo las dependencias faltantes:

```kotlin
implementation(libs.room.runtime)
implementation(libs.room.ktx)
ksp(libs.room.compiler)
implementation(libs.datastore.preferences)
androidTestImplementation(libs.room.testing)

// Reutiliza tus aliases y versiones actuales si estos artefactos ya existen.
androidTestImplementation("androidx.test:core-ktx:1.7.0")
androidTestImplementation("androidx.test:runner:1.7.0")
androidTestImplementation("androidx.test.ext:junit:1.3.0")
```

Las pruebas locales requieren JUnit 4 y `kotlinx-coroutines-test`, ya utilizados en capítulos anteriores. Reutiliza esas dependencias y conserva alineadas las versiones de `kotlinx-coroutines-core` y `kotlinx-coroutines-test`. La pantalla usa Material 3 y `ComponentActivity`, que ya existen en el proyecto.

No sustituyas `compileSdk`, `targetSdk`, `minSdk`, `compileOptions`, plugins Compose o serialización, Hilt, Retrofit u OkHttp. El JDK con que se ejecuta Gradle y el nivel de bytecode de la aplicación son configuraciones distintas: conserva ambos como en la compilación funcional.

<br/>

3. Sincronizar

Ejecuta **Sync Project with Gradle Files**. Todavía no esperes JSON de esquemas: se generarán al compilar después de crear la base de datos.

**Verificación:** no hay aliases duplicados ni compiladores de Room registrados a la vez en KAPT y KSP.


<br/><br/>

### Paso 4. Crear el dominio, sus contratos y los casos de uso

Todos los archivos principales nuevos de los pasos 4–11 viven bajo `app/src/main/java/`. La ruta indicada es relativa a ese directorio. Cada bloque contiene el archivo completo.

### 4.1 Modelo de telemetría y preferencias

Archivo: `com/example/advancedtracker/domain/persistence/TelemetryRecord.kt`.

```kotlin
package com.example.advancedtracker.domain.persistence

import com.example.advancedtracker.domain.model.tracking.TelemetryData

// Compatibilidad de nombres: este alias ES el modelo existente, no otro modelo.
typealias TelemetryRecord = TelemetryData
```

<br/>

Archivo: `com/example/advancedtracker/domain/persistence/TelemetrySettings.kt`.

```kotlin
package com.example.advancedtracker.domain.persistence

data class TelemetrySettings(
    val onlyMocked: Boolean = false,
    val onlyLocal: Boolean = false
)
```

<br/>

2. Resultado de actualización

Archivo: `com/example/advancedtracker/domain/persistence/RefreshResult.kt`.

```kotlin
package com.example.advancedtracker.domain.persistence

sealed interface RefreshResult {
    data class Updated(val receivedCount: Int) : RefreshResult
    data class Failed(val kind: FailureKind, val message: String) : RefreshResult
    data object Skipped : RefreshResult
}

enum class FailureKind { NETWORK, LOCAL_STORAGE }
```

<br/>

**Nota:** Una falla de actualización no lleva una lista `Any?`: los datos se siguen observando desde Room y el resultado informa únicamente qué ocurrió al actualizar.

<br/>

### 4.3 Interfaces

Archivo: `com/example/advancedtracker/domain/persistence/StoredTelemetryRepository.kt`.

```kotlin
package com.example.advancedtracker.domain.persistence

import kotlinx.coroutines.flow.Flow

interface StoredTelemetryRepository {
    fun observeRecords(): Flow<List<TelemetryRecord>>
    suspend fun refresh(): RefreshResult
}
```

<br/>

Archivo: `com/example/advancedtracker/domain/persistence/TelemetrySettingsRepository.kt`.

```kotlin
package com.example.advancedtracker.domain.persistence

import kotlinx.coroutines.flow.Flow

interface TelemetrySettingsRepository {
    val settings: Flow<TelemetrySettings>
    suspend fun saveOnlyMocked(enabled: Boolean)
    suspend fun saveOnlyLocal(enabled: Boolean)
}
```

<br/>

4. Casos de uso

Archivo: `com/example/advancedtracker/domain/persistence/PersistenceUseCases.kt`.

```kotlin
package com.example.advancedtracker.domain.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class ObserveStoredTelemetryUseCase(
    private val records: StoredTelemetryRepository,
    private val settings: TelemetrySettingsRepository
) {
    operator fun invoke(): Flow<List<TelemetryRecord>> = combine(
        records.observeRecords(), settings.settings
    ) { all, preferences ->
        if (preferences.onlyMocked) all.filter { it.isMocked } else all
    }
}

class ObserveTelemetrySettingsUseCase(
    private val repository: TelemetrySettingsRepository
) {
    operator fun invoke(): Flow<TelemetrySettings> = repository.settings
}

class RefreshStoredTelemetryUseCase(
    private val records: StoredTelemetryRepository,
    private val settings: TelemetrySettingsRepository
) {
    suspend operator fun invoke(): RefreshResult {
        if (settings.settings.first().onlyLocal) return RefreshResult.Skipped
        return records.refresh()
    }
}

class SaveTelemetrySettingsUseCase(
    private val repository: TelemetrySettingsRepository
) {
    suspend fun onlyMocked(enabled: Boolean) = repository.saveOnlyMocked(enabled)
    suspend fun onlyLocal(enabled: Boolean) = repository.saveOnlyLocal(enabled)
}
```

<br/>

**Resultado esperado:** el nuevo dominio solo depende de Kotlin y corrutinas. No importa `data.*`, clases Android, Retrofit, Room ni Hilt. Los casos de uso se construyen por inyección en un módulo externo.

<br/><br/>

### Paso 5. Crear la entidad Room conservando los campos de telemetría

La versión 1 **se crea en este laboratorio**. No se presupone una base Room anterior. Si tienes ya una base llamada `telemetry_db` con otro esquema, consérvala y usa el nombre `telemetry_lab51_db` en el módulo de este laboratorio; utiliza ese mismo nombre en todas las comprobaciones.

Archivo: `com/example/advancedtracker/data/local/persistence/TelemetryEntity.kt`.

```kotlin
package com.example.advancedtracker.data.local.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "telemetry_table",
    indices = [Index(value = ["timestamp"])]
)
data class TelemetryEntity(
    @PrimaryKey val id: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "speed_kmh") val speedKmh: Double,
    val timestamp: Long,
    @ColumnInfo(name = "is_mocked") val isMocked: Boolean
)
```

<br/>

**Nota:** La llave primaria garantiza unicidad de `id`. Los índices de dispositivo y tiempo facilitan búsquedas; no son índices únicos. No se agrega una relación con una tabla de dispositivos que la API existente no proporciona.

<br/><br/>

### Paso 6. Implementar el DAO y la base de datos en versión 1


1. Archivo: `com/example/advancedtracker/data/local/persistence/TelemetryDao.kt`.

```kotlin
package com.example.advancedtracker.data.local.persistence

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {
    @Query("SELECT * FROM telemetry_table ORDER BY timestamp DESC, id ASC")
    fun observeAll(): Flow<List<TelemetryEntity>>

    @Query("SELECT * FROM telemetry_table WHERE is_mocked = :isMocked ORDER BY timestamp DESC, id ASC")
    fun observeByMocked(isMocked: Boolean): Flow<List<TelemetryEntity>>

    @Query("SELECT * FROM telemetry_table ORDER BY timestamp DESC, id ASC")
    suspend fun readAll(): List<TelemetryEntity>

    @Upsert
    suspend fun upsertAll(records: List<TelemetryEntity>)

    @Transaction
    suspend fun mergeRemote(records: List<TelemetryEntity>) {
        upsertAll(records)
    }
}
```

<br/>

**Nota:**  `mergeRemote` inserta o actualiza por identificador dentro de una transacción. No borra registros que no aparecen en la última respuesta: `limit=50` es una ventana de consulta, no una lista completa que autorice eliminar el historial. Una respuesta vacía conserva lo almacenado. El laboratorio acumula historial local; su purgado y la propagación de borrados remotos requieren una política posterior.


2. **Base de datos inicial**

Archivo: `com/example/advancedtracker/data/local/persistence/TelemetryDatabase.kt`.

```kotlin
 package com.example.advancedtracker.data.local.persistence

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TelemetryEntity::class], version = 1, exportSchema = true)
abstract class TelemetryDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao
}
```


<br/><br/>

### Paso 7. Agregar los mapeos entre DTO, entidad y dominio

Archivo: `com/example/advancedtracker/data/local/persistence/TelemetryMappers.kt`.

```kotlin
package com.example.advancedtracker.data.local.persistence

import com.example.advancedtracker.data.remote.dto.RemoteTelemetryDto
import com.example.advancedtracker.domain.persistence.TelemetryRecord
import java.util.Date

fun RemoteTelemetryDto.toStoredEntity(): TelemetryEntity = TelemetryEntity(
    id = id,
    latitude = latitude,
    longitude = longitude,
    speedKmh = speedKmh,
    timestamp = timestamp,
    isMocked = isMocked
)

fun TelemetryEntity.toRecord(): TelemetryRecord = TelemetryRecord(
    id = id,
    latitude = latitude,
    longitude = longitude,
    speedKmh = speedKmh,
    timestamp = Date(timestamp),
    isMocked = isMocked
)
```

<br/>

**Nota:** No se agrega un segundo DTO de red ni se requieren conversiones de `Double` a `Float`.


<br/><br/>

### Paso 8. Implementar las preferencias DataStore sin interferir con la autenticación

Archivo: `com/example/advancedtracker/data/local/persistence/TelemetrySettingsDataStore.kt`.

```kotlin
package com.example.advancedtracker.data.local.persistence

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.advancedtracker.domain.persistence.TelemetrySettings
import com.example.advancedtracker.domain.persistence.TelemetrySettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.telemetryLabSettings by preferencesDataStore(
    name = "telemetry_lab51_settings"
)

@Singleton
class TelemetrySettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : TelemetrySettingsRepository {
    private val onlyMocked = booleanPreferencesKey("only_mocked")
    private val onlyLocal = booleanPreferencesKey("only_local")

    override val settings: Flow<TelemetrySettings> =
        context.telemetryLabSettings.data.map { preferences ->
            TelemetrySettings(
                onlyMocked = preferences[onlyMocked] ?: false,
                onlyLocal = preferences[onlyLocal] ?: false
            )
        }

    override suspend fun saveOnlyMocked(enabled: Boolean) {
        context.telemetryLabSettings.edit { preferences -> preferences[onlyMocked] = enabled }
    }

    override suspend fun saveOnlyLocal(enabled: Boolean) {
        context.telemetryLabSettings.edit { preferences -> preferences[onlyLocal] = enabled }
    }
}
```

**Nota:** El archivo es `files/datastore/telemetry_lab51_settings.preferences_pb`. Es independiente del almacenamiento de autenticación de 4.1. Preferences DataStore no necesita una clase `Serializer`; esa se utiliza en DataStore tipado.

Un error de lectura se deja llegar a la presentación para mostrarlo; no se sustituuye silenciosamente por valores predeterminados que podrían reactivar consultas de red. Un error de escritura también se informa. La opción `onlyLocal` impide iniciar una nueva actualización, pero no cancela una petición que ya estaba en curso.

<br/><br/>

### Paso 9. Construir el repositorio híbrido con Room y la API

Archivo: `com/example/advancedtracker/data/repository/persistence/RoomTelemetryRepository.kt`.

```kotlin
package com.example.advancedtracker.data.repository.persistence

import com.example.advancedtracker.data.local.persistence.TelemetryDao
import com.example.advancedtracker.data.local.persistence.toRecord
import com.example.advancedtracker.data.local.persistence.toStoredEntity
import com.example.advancedtracker.data.remote.network.RemoteTelemetryApi
import com.example.advancedtracker.domain.persistence.FailureKind
import com.example.advancedtracker.domain.persistence.RefreshResult
import com.example.advancedtracker.domain.persistence.StoredTelemetryRepository
import com.example.advancedtracker.domain.persistence.TelemetryRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTelemetryRepository @Inject constructor(
    private val api: RemoteTelemetryApi,
    private val dao: TelemetryDao
) : StoredTelemetryRepository {

    override fun observeRecords(): Flow<List<TelemetryRecord>> =
        dao.observeAll().map { entities -> entities.map { it.toRecord() } }

    override suspend fun refresh(): RefreshResult {
        val remote = try {
            api.fetchHistory(limit = 50)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (http: HttpException) {
            return RefreshResult.Failed(
                FailureKind.NETWORK, "La API respondió HTTP ${http.code()}."
            )
        } catch (io: IOException) {
            return RefreshResult.Failed(
                FailureKind.NETWORK, "No fue posible conectar con la API."
            )
        } catch (error: Exception) {
            return RefreshResult.Failed(
                FailureKind.NETWORK, "La respuesta de la API no pudo procesarse."
            )
        }

        return try {
            dao.mergeRemote(remote.map { it.toStoredEntity() })
            RefreshResult.Updated(remote.size)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            RefreshResult.Failed(
                FailureKind.LOCAL_STORAGE,
                "La API respondió, pero no fue posible guardar los datos en Room."
            )
        }
    }
}
```

<br/>

**Observaciones:**

- La observación inicia con Room y no espera la petición HTTP. La actualización es una operación separada: consulta la API y escribe en Room; **nunca entrega directamente a la pantalla la lista recibida por Retrofit**.

- Las consultas suspendidas y el `Flow` de Room, junto con las llamadas suspendidas de Retrofit y DataStore, ya ofrecen ejecución asíncrona. No se agrega `runBlocking` ni `allowMainThreadQueries()`. Se vuelve a lanzar `CancellationException` para respetar la cancelación del ViewModel.

- Una falla de lectura de Room se propaga por `observeRecords()` y la pantalla la maneja. Esto es distinto de una falla de actualización: no se afirma que una base inaccesible pueda seguir proporcionando datos. Los errores HTTP y de red no borran la caché.

<br/><br/>

### Paso 10. Agregar los módulos de inyección de dependencias con Hilt

1. Base y casos de uso

Archivo: `com/example/advancedtracker/di/persistence/PersistenceModule.kt`.

```kotlin
package com.example.advancedtracker.di.persistence

import android.content.Context
import androidx.room.Room
import com.example.advancedtracker.data.local.persistence.TelemetryDao
import com.example.advancedtracker.data.local.persistence.TelemetryDatabase
import com.example.advancedtracker.domain.persistence.ObserveStoredTelemetryUseCase
import com.example.advancedtracker.domain.persistence.ObserveTelemetrySettingsUseCase
import com.example.advancedtracker.domain.persistence.RefreshStoredTelemetryUseCase
import com.example.advancedtracker.domain.persistence.SaveTelemetrySettingsUseCase
import com.example.advancedtracker.domain.persistence.StoredTelemetryRepository
import com.example.advancedtracker.domain.persistence.TelemetrySettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): TelemetryDatabase =
        Room.databaseBuilder(
            context,
           TelemetryDatabase::class.java,
            "telemetry_lab51_tracking_db"
        ).build()

    @Provides
    fun provideDao(
        database: TelemetryDatabase
    ): TelemetryDao = database.telemetryDao()

    @Provides
    fun provideObserveRecords(
        records: StoredTelemetryRepository,
        settings: TelemetrySettingsRepository
    ): ObserveStoredTelemetryUseCase =
        ObserveStoredTelemetryUseCase(records, settings)

    @Provides
    fun provideObserveSettings(
        repository: TelemetrySettingsRepository
    ): ObserveTelemetrySettingsUseCase =
        ObserveTelemetrySettingsUseCase(repository)

    @Provides
    fun provideRefresh(
        records: StoredTelemetryRepository,
        settings: TelemetrySettingsRepository
    ): RefreshStoredTelemetryUseCase =
        RefreshStoredTelemetryUseCase(records, settings)

    @Provides
    fun provideSaveSettings(
        repository: TelemetrySettingsRepository
    ): SaveTelemetrySettingsUseCase =
        SaveTelemetrySettingsUseCase(repository)
}
```

<br/>

### 10.2 Bindings adicionales

Archivo: `com/example/advancedtracker/di/persistence/PersistenceBindings.kt`.

```kotlin
package com.example.advancedtracker.di.persistence

import com.example.advancedtracker.data.local.persistence.TelemetrySettingsDataStore
import com.example.advancedtracker.data.repository.persistence.RoomTelemetryRepository
import com.example.advancedtracker.domain.persistence.StoredTelemetryRepository
import com.example.advancedtracker.domain.persistence.TelemetrySettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PersistenceBindings {
    @Binds
    abstract fun bindStoredRecords(impl: RoomTelemetryRepository): StoredTelemetryRepository

    @Binds
    abstract fun bindSettings(impl: TelemetrySettingsDataStore): TelemetrySettingsRepository
}
```

<br/>

El laboratorio 4.1 continúa proporcionando `TelemetryApiService`. No agregues otro `@Provides` para esa misma API ni otro binding para el repositorio previo. Hilt administra una instancia singleton de la base; no hace falta otro singleton manual con `INSTANCE`.

<br/>

**Verificación:** compila para comprobar el grafo y exportar versión 1.

```powershell
.\gradlew.bat :app:assembleDebug
```

Debe existir `app/schemas/com.example.advancedtracker.data.local.persistence.TelemetryDatabase/1.json`. No edites a mano este JSON.


<br/><br/>

### Paso 11. Crear el estado, ViewModel y pantalla de verificación

1. Estado y ViewModel

Archivo: `com/example/advancedtracker/ui/persistence/PersistenceViewModel.kt`.

```kotlin
package com.example.advancedtracker.ui.persistence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedtracker.domain.persistence.ObserveStoredTelemetryUseCase
import com.example.advancedtracker.domain.persistence.ObserveTelemetrySettingsUseCase
import com.example.advancedtracker.domain.persistence.RefreshResult
import com.example.advancedtracker.domain.persistence.RefreshStoredTelemetryUseCase
import com.example.advancedtracker.domain.persistence.SaveTelemetrySettingsUseCase
import com.example.advancedtracker.domain.persistence.TelemetryRecord
import com.example.advancedtracker.domain.persistence.TelemetrySettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersistenceUiState(
    val records: List<TelemetryRecord> = emptyList(),
    val reading: Boolean = true,
    val refreshing: Boolean = false,
    val message: String = "",
    val storageError: String? = null,
    val settings: TelemetrySettings = TelemetrySettings(),
    val settingsReady: Boolean = false,
    val settingsError: String? = null
)

@HiltViewModel
class PersistenceViewModel @Inject constructor(
    private val observeRecords: ObserveStoredTelemetryUseCase,
    private val observeSettings: ObserveTelemetrySettingsUseCase,
    private val refreshRecords: RefreshStoredTelemetryUseCase,
    private val saveSettings: SaveTelemetrySettingsUseCase
) : ViewModel() {
    private val mutableState = MutableStateFlow(PersistenceUiState())
    val state = mutableState.asStateFlow()
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            observeRecords()
                .catch { error ->
                    if (error is CancellationException) throw error
                    mutableState.update {
                        it.copy(reading = false, storageError = "No fue posible leer Room o aplicar las preferencias.")
                    }
                }
                .collect { records ->
                    mutableState.update {
                        it.copy(records = records, reading = false, storageError = null)
                    }
                }
        }
        viewModelScope.launch {
            observeSettings()
                .catch { error ->
                    if (error is CancellationException) throw error
                    mutableState.update {
                        it.copy(settingsReady = false, settingsError = "No fue posible leer DataStore.")
                    }
                }
                .collect { settings ->
                    mutableState.update {
                        it.copy(settings = settings, settingsReady = true, settingsError = null)
                    }
                }
        }
        viewModelScope.launch {
            try {
                observeSettings().first()
                refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(message = "La actualización requiere preferencias accesibles.") }
            }
        }
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            mutableState.update { it.copy(refreshing = true, message = "Actualizando desde la API…") }
            try {
                val message = when (val result = refreshRecords()) {
                    is RefreshResult.Updated -> "Guardados ${result.receivedCount} registros recibidos de la API."
                    is RefreshResult.Failed -> result.message + " Se conserva el historial local disponible."
                    RefreshResult.Skipped -> "Solo almacenamiento local: no se inició una petición HTTP."
                }
                mutableState.update { it.copy(message = message) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(message = "No fue posible consultar las preferencias para actualizar.") }
            } finally {
                mutableState.update { it.copy(refreshing = false) }
            }
        }
    }

    fun setOnlyMocked(enabled: Boolean) = persistSetting {
        saveSettings.onlyMocked(enabled)
    }

    fun setOnlyLocal(enabled: Boolean) = persistSetting {
        saveSettings.onlyLocal(enabled)
    }

    private fun persistSetting(operation: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                operation()
                mutableState.update { it.copy(settingsError = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(settingsError = "No fue posible guardar la preferencia.") }
            }
        }
    }
}
```

<br/>

**Nota:** El mensaje de actualización y los datos locales son partes distintas del estado. Un error de red mantiene visibles los registros; una lista vacía y un error de almacenamiento no se presentan como el mismo resultado.

<br/>

2. Pantalla Compose

Archivo: `com/example/advancedtracker/ui/persistence/PersistenceScreen.kt`.

```kotlin
package com.example.advancedtracker.ui.persistence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PersistenceScreen(
    state: PersistenceUiState,
    onRefresh: () -> Unit,
    onOnlyMocked: (Boolean) -> Unit,
    onOnlyLocal: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Laboratorio 5.1 · Persistencia", style = MaterialTheme.typography.titleLarge)
        Text("Datos mostrados desde Room: ${state.records.size}")
        Text("Filtro guardado — solo simulados: ${state.settings.onlyMocked}")
        Text("Solo local guardado: ${state.settings.onlyLocal}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mostrar solo lecturas simuladas", modifier = Modifier.weight(1f))
            Switch(
                checked = state.settings.onlyMocked,
                onCheckedChange = onOnlyMocked,
                enabled = state.settingsReady
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Consultar solo almacenamiento local", modifier = Modifier.weight(1f))
            Switch(
                checked = state.settings.onlyLocal,
                onCheckedChange = onOnlyLocal,
                enabled = state.settingsReady && !state.refreshing
            )
        }
        Button(onClick = onRefresh, enabled = state.settingsReady && !state.refreshing) {
            Text("Actualizar")
        }
        if (state.reading || state.refreshing) CircularProgressIndicator()
        if (state.message.isNotBlank()) Text(state.message)
        state.storageError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.settingsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!state.reading && state.records.isEmpty() && state.storageError == null) {
            Text("Sin registros para el filtro actual. Room puede estar vacío o no contener lecturas simuladas.")
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.records, key = { it.id }) { record ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(record.id)
                    Text("Latitud: ${record.latitude} · Longitud: ${record.longitude}")
                    Text("Velocidad: ${record.speedKmh} km/h · Simulado: ${record.isMocked}")
                    Text("Fecha: ${record.timestamp}")
                    Text("Timestamp en milisegundos: ${record.timestamp.time}")
                }
                HorizontalDivider()
            }
        }
    }
}
```

<br/><br/>

### Paso 12. Incorporar una actividad exclusiva de la variante debug

Este archivo vive en **`app/src/debug/java/`**, no en `main`: 
`com/example/advancedtracker/ui/persistence/PersistenceLabActivity.kt`.

```kotlin
package com.example.advancedtracker.ui.persistence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PersistenceLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this)[PersistenceViewModel::class.java]
        setContent {
            val state by viewModel.state.collectAsState()
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                    PersistenceScreen(
                        state = state,
                        onRefresh = viewModel::refresh,
                        onOnlyMocked = { viewModel.setOnlyMocked(it) },
                        onOnlyLocal = { viewModel.setOnlyLocal(it) }
                    )
                }
            }
        }
    }
}
```

<br/>

**Nota:** `

- ViewModelProvider` evita agregar otra biblioteca de integración Hilt–Compose. `collectAsState()` basta para esta pantalla didáctica; los datos se conservan en Room y las preferencias en DataStore.


<br/>

Agrega el registro al archivo **`app/src/debug/AndroidManifest.xml`**. Si ya existe, integra solo `<activity>` dentro de su `<application>`; conserva lo demás. Si no existe, créalo completo:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application android:usesCleartextTraffic="true">
        <activity
            android:name="com.example.advancedtracker.ui.remote.RemoteTelemetryActivity"
            android:exported="true"
            android:label="Telemetría REST" />

        <!-- Cap 05 -->
        <activity
            android:name="com.example.advancedtracker.ui.persistence.PersistenceLabActivity"
            android:exported="true" />
    </application>

</manifest>
```

<br/>

**Nota:** La actividad se exporta en la variante debug para abrirla con `adb`; no declara otro `MAIN/LAUNCHER` y no cambia el acceso anterior. No agregues este registro al manifiesto `main`. La variante release no incluye esta actividad.

<br/><br/>

### Paso 13. Ejecutar e inspeccionar Room y DataStore

1. Instalar sin borrar datos previos

Conecta un dispositivo y ejecuta:

```cmd

.\gradlew.bat :app:assembleDebug

.\gradlew.bat :app:installDebug

adb shell am start -n com.example.advancedtracker/com.example.advancedtracker.ui.persistence.PersistenceLabActivity

```

<br/>

Inicia la API del laboratorio 4.1 con la siguiente instrucción:

```cmd
py -3 tools/telemetry_server_merged.py
```

<br/>


2. Database Inspector: observar la base interna

    1. Ejecuta la variante debug en un emulador o dispositivo. Para esta práctica se recomienda API 31 o superior; Database Inspector requiere API 26 o superior y SQLite del sistema.

    2. Mantén la actividad abierta y pulsa **Actualizar** con el servidor disponible.

    3. En Android Studio abre **View > Tool Windows > App Inspection**.

    4. Selecciona el dispositivo y el proceso de la aplicación.

    5. En **Database Inspector**, expande `telemetry_lab51_tracking_db` y abre `telemetry_table`.

    6. Compara un registro completo con la pantalla: sus seis campos deben coincidir.

    7. Abre una pestaña de consulta y ejecuta:

    ```sql
    SELECT id, latitude, longitude, speed_kmh, timestamp, is_mocked
    FROM telemetry_table
    ORDER BY timestamp DESC, id ASC;

    SELECT COUNT(*) AS total FROM telemetry_table;

    SELECT is_mocked, COUNT(*) AS registros
    FROM telemetry_table
    GROUP BY is_mocked;

    PRAGMA user_version;
    PRAGMA table_info('telemetry_table');
    ```

<br/>

**Resultado esperado:** `user_version` devuelve `1`, existen las seis columnas y los registros mostrados coinciden con Room. Guarda una captura del inspector y otra de la pantalla con el mismo `id`. Si se exporta la tabla a CSV desde el inspector, guarda también ese archivo como evidencia.

Una base vacía no demuestra persistencia de registros. Si la API no devuelve datos, crea uno usando el POST funcional del laboratorio 4.1 y después vuelve a actualizar aquí.



<br/>

3. Ver DataStore y demostrar persistencia

    1. Activa **Mostrar solo lecturas simuladas**. Comprueba que **Filtro guardado — solo simulados** muestra `true` y que todos los registros visibles tienen `isMocked=true`.

    2. Si no hay registros simulados, es correcto ver una lista vacía: el filtro no borra ninguna fila de Room. Para ver todo el historial, desactiva ese switch.

    3. Activa **Consultar solo almacenamiento local** y espera que **Solo local guardado** muestre `true`.

    4. Detén el proceso y vuelve a abrir la actividad:

    ```cmd
    adb shell am force-stop com.example.advancedtracker

    adb shell am start -n com.example.advancedtracker/com.example.advancedtracker.ui.persistence.PersistenceLabActivity
    ```

    5. Comprueba que ambos valores se recuperan desde DataStore. La opción solo local impide iniciar un nuevo GET.

    6. En **View > Tool Windows > Device Explorer**, verifica el archivo `/data/data/com.example.advancedtracker/files/datastore/telemetry_lab51_settings.preferences_pb` (puede aparecer bajo `/data/user/0/...`). Como alternativa para la aplicación debug:

    ```cmd
    adb shell run-as com.example.advancedtracker ls files/datastore
    ```

El archivo `.preferences_pb` es binario. Database Inspector muestra SQLite/Room, no los valores de DataStore. La pantalla muestra los valores observados desde DataStore y recuperarlos tras `force-stop` demuestra la persistencia.


<br/><br/>

### Paso 14. Implementar y verificar la migración manual de versión 1 a 2

Realiza este paso **después** de crear versión 1, insertar al menos un registro y guardar sus evidencias. No desinstales la aplicación ni borres sus datos.

> **Nota:** Si Database Inspector muestra `[DETACHED]`, está desconectado y presenta una copia anterior de la base de datos. Mantén la aplicación abierta, selecciona su proceso activo en **App Inspection** y vuelve a ejecutar la consulta con **Run**. Para observar cambios en la tabla automáticamente, activa **Live updates**.

<br/>

1. Agregar metadato local

En la nueva entidad de esta práctica, en `app/src/main/java/com/example/advancedtracker/data/local/persistence/TelemetryEntity.kt` agrega una coma después de `isMocked` y la propiedad siguiente:

```kotlin
@ColumnInfo(name = "cached_at", defaultValue = "0")
val cachedAt: Long = 0L
```

<br/>

En `TelemetryMappers.kt`, cambia únicamente el mapeo DTO → entidad: agrega una coma después de `isMocked = isMocked` e incorpora:

```kotlin
cachedAt = System.currentTimeMillis()
```

El mapeo entidad → dominio continúa igual: `cachedAt` es un detalle de caché, no un nuevo campo del contrato API.


<br/>

2 Migración

Archivo: `com/example/advancedtracker/data/local/persistence/TelemetryMigrations.kt`.

```kotlin
package com.example.advancedtracker.data.local.persistence

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE telemetry_table ADD COLUMN cached_at INTEGER NOT NULL DEFAULT 0"
        )
    }
}
```

<br/>

En `TelemetryDatabase.kt`, cambia `version = 1` por `version = 2`; no cambies el nombre de la clase ni de la tabla.

<br/>

En `PersistenceModule.kt`, importa:

```kotlin
import com.example.advancedtracker.data.local.persistence.MIGRATION_1_2
```

En `provideDatabase`, agrega `.addMigrations(MIGRATION_1_2)` antes de `.build()`.

<br/>

3. Instalar la actualización

```cmd
.\gradlew.bat :app:installDebug

adb shell am start -n com.example.advancedtracker/com.example.advancedtracker.ui.persistence.PersistenceLabActivity
```

Mantén `onlyLocal=true` durante la comparación para evitar que un nuevo GET modifique los registros. Conserva `1.json` y verifica que ahora existe `2.json` en el mismo directorio de esquemas.

Consulta en el inspector:

```sql
PRAGMA user_version;
SELECT id, latitude, longitude, speed_kmh, timestamp, is_mocked, cached_at
FROM telemetry_table
ORDER BY timestamp DESC, id ASC;
```

**Resultado esperado:** versión `2`, mismos identificadores y datos de negocio; registros anteriores con `cached_at=0`. Al desactivar `onlyLocal` y actualizar, los registros recibidos tendrán un `cached_at` de la hora actual, en milisegundos. Los registros antiguos no incluidos en la ventana GET conservan su valor.


<br/><br/>

### Paso 15. Implementar y verificar la migración automática.

#### 15.1 Agregar origen técnico de caché

Agrega a la entidad, después de `cachedAt`, la propiedad:

```kotlin
@ColumnInfo(name = "cache_source", defaultValue = "'remote'")
val cacheSource: String = "remote"
```

<br/>

**Nota:** Es un metadato técnico. No cambia los seis campos del dominio ni requiere modificar el DTO.
Agregar `cacheSource` únicamente en `TelemetryEntity.kt`, la entidad almacenada en Room. Este campo indica el origen del registro; el valor `"remote"` significa que proviene de la API. Es información adicional para administrar la caché local. Los seis campos de telemetría permanecen iguales, por lo que no necesitas modificar `TelemetryData`, `RemoteTelemetryDto` ni el servidor Python.

<br/>

2. Base final versión 3

Sustituye **solo el archivo nuevo** `TelemetryDatabase.kt` de esta práctica por:

```kotlin
package com.example.advancedtracker.data.local.persistence

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TelemetryEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 2, to = 3)]
)
abstract class TrackingTelemetryDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao
}
```

<br/>

**Nota:** Room compara `2.json` con el esquema actual y genera la migración. No necesitas `AutoMigrationSpec` para agregar esta columna con un valor predeterminado; cambios ambiguos como renombrar o eliminar requieren instrucciones adicionales.

Conserva `.addMigrations(MIGRATION_1_2)` en el builder: la ruta desde versión 1 hasta 3 incluye el primer paso manual y el segundo automático.

<br/>

3. Entidad y mapeo finales de referencia

Después de los dos pasos de migración, `TelemetryEntity.kt` debe quedar así:

```kotlin
package com.example.advancedtracker.data.local.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "telemetry_table",
    indices = [Index(value = ["timestamp"])]
)
data class TelemetryEntity(
    @PrimaryKey 
    val id: String,

    val latitude: Double,

    val longitude: Double,

    @ColumnInfo(name = "speed_kmh") 
    val speedKmh: Double,

    val timestamp: Long,
    @ColumnInfo(name = "is_mocked") 

    val isMocked: Boolean,
    @ColumnInfo(name = "cached_at", defaultValue = "0") 

    val cachedAt: Long = 0L,

    @ColumnInfo(name = "cache_source", defaultValue = "'remote'") 
    val cacheSource: String = "remote"
)
```

El archivo final `TelemetryMappers.kt` debe quedar así:

```kotlin
package com.example.advancedtracker.data.local.persistence

import com.example.advancedtracker.data.remote.dto.RemoteTelemetryDto
import com.example.advancedtracker.domain.persistence.TelemetryRecord
import java.util.Date

fun RemoteTelemetryDto.toStoredEntity(): TelemetryEntity = TelemetryEntity(
    id = id,
    latitude = latitude,
    longitude = longitude,
    speedKmh = speedKmh,
    timestamp = timestamp,
    isMocked = isMocked,
    cachedAt = System.currentTimeMillis()
)

fun TelemetryEntity.toRecord(): TelemetryRecord = TelemetryRecord(
    id = id,
    latitude = latitude,
    longitude = longitude,
    speedKmh = speedKmh,
    timestamp = Date(timestamp),
    isMocked = isMocked
)
```

<br/>

El método final de construcción en `PersistenceModule` es:

```kotlin
@Provides
@Singleton
fun provideDatabase(@ApplicationContext context: Context): TrackingTelemetryDatabase =
    Room.databaseBuilder(context, TrackingTelemetryDatabase::class.java, "telemetry_lab51_tracking_db")
        .addMigrations(MIGRATION_1_2)
        .build()
```


<br/>

4. Comprobar la actualización

```cmd

.\gradlew.bat :app:installDebug

adb shell am start -n com.example.advancedtracker/com.example.advancedtracker.ui.persistence.PersistenceLabActivity

```

<br/>

**Resultado esperado:** Debe generarse `3.json` y conservarse `1.json` y `2.json`. Repite la consulta con `onlyLocal=true`:


Desde el `Database Inspector` realiza lo siguiente

```sql
PRAGMA user_version;
SELECT id, latitude, longitude, speed_kmh, timestamp, is_mocked,
       cached_at, cache_source
FROM telemetry_table
ORDER BY timestamp DESC, id ASC;
```

**Resultado esperado:** versión `3`, registros preservados y `cache_source='remote'`. Una instalación nueva crea directamente versión 3; para demostrar la migración se necesita la secuencia ejecutada o las pruebas del paso siguiente.


<br/><br/>

### Paso 16. Agregar y ejecutar las pruebas unitarias e instrumentadas

Implementa estas pruebas cuando el código ya esté en versión 3. Las locales verifican lógica y propagación de fallas con dobles de prueba; las instrumentadas verifican Room real y las migraciones.

<br/>

1. Pruebas locales del repositorio

Archivo bajo `app/src/test/java/`: `com/example/advancedtracker/data/repository/persistence/RoomTelemetryRepositoryTest.kt`.

```kotlin
package com.example.advancedtracker.data.repository.persistence

import com.example.advancedtracker.data.local.persistence.TelemetryDao
import com.example.advancedtracker.data.local.persistence.TelemetryEntity
import com.example.advancedtracker.data.remote.dto.RemoteTelemetryDto
import com.example.advancedtracker.data.remote.network.RemoteTelemetryApi
import com.example.advancedtracker.domain.persistence.FailureKind
import com.example.advancedtracker.domain.persistence.RefreshResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class RoomTelemetryRepositoryTest {
    private fun dto(id: String = "tel_01", speedKmh: Double = 62.5) =
        RemoteTelemetryDto(id, 19.4326, -99.1332, speedKmh, 1700000000000L, false)

    private class FakeApi : RemoteTelemetryApi {
        var response = emptyList<RemoteTelemetryDto>()
        var failure: Exception? = null
        var calls = 0

        override suspend fun fetchHistory(limit: Int): List<RemoteTelemetryDto> {
            calls++
            failure?.let { throw it }
            return response
        }

        override suspend fun sendRecord(data: RemoteTelemetryDto): RemoteTelemetryDto = data
    }

    private class FakeDao : TelemetryDao {
        val rows = MutableStateFlow<List<TelemetryEntity>>(emptyList())
        var writeFailure: Exception? = null
        var readFailure: Exception? = null

        override fun observeAll(): Flow<List<TelemetryEntity>> =
            if (readFailure == null) rows else flow { throw readFailure!! }

        override fun observeByMocked(isMocked: Boolean): Flow<List<TelemetryEntity>> = flow {
            emit(rows.value.filter { it.isMocked == isMocked })
        }

        override suspend fun readAll(): List<TelemetryEntity> = rows.value

        override suspend fun upsertAll(records: List<TelemetryEntity>) {
            writeFailure?.let { throw it }
            rows.value = (rows.value.associateBy { it.id } + records.associateBy { it.id })
                .values.sortedWith(compareByDescending<TelemetryEntity> { it.timestamp }.thenBy { it.id })
        }

        override suspend fun mergeRemote(records: List<TelemetryEntity>) = upsertAll(records)
    }

    @Test
    fun refreshStoresAllSixFields() = runTest {
        val api = FakeApi().apply { response = listOf(dto()) }
        val repository = RoomTelemetryRepository(api, FakeDao())
        assertEquals(RefreshResult.Updated(1), repository.refresh())
        val record = repository.observeRecords().first().single()
        assertEquals("tel_01", record.id)
        assertEquals(19.4326, record.latitude, 0.0)
        assertEquals(-99.1332, record.longitude, 0.0)
        assertEquals(62.5, record.speedKmh, 0.0)
        assertEquals(1700000000000L, record.timestamp.time)
        assertFalse(record.isMocked)
    }

    @Test
    fun observingCacheDoesNotCallApi() = runTest {
        val api = FakeApi()
        val repository = RoomTelemetryRepository(api, FakeDao())
        assertTrue(repository.observeRecords().first().isEmpty())
        assertEquals(0, api.calls)
    }

    @Test
    fun networkFailureKeepsCachedRows() = runTest {
        val api = FakeApi().apply { response = listOf(dto()) }
        val repository = RoomTelemetryRepository(api, FakeDao())
        repository.refresh()
        val before = repository.observeRecords().first()
        api.failure = IOException("Sin conexión")
        val result = repository.refresh() as RefreshResult.Failed
        assertEquals(FailureKind.NETWORK, result.kind)
        assertEquals(before, repository.observeRecords().first())
    }

    @Test
    fun partialOrEmptyResponseDoesNotDeleteHistory() = runTest {
        val api = FakeApi().apply { response = listOf(dto("old"), dto("new")) }
        val repository = RoomTelemetryRepository(api, FakeDao())
        repository.refresh()
        api.response = listOf(dto("new", speedKmh = 80.0))
        repository.refresh()
        assertEquals(setOf("old", "new"), repository.observeRecords().first().map { it.id }.toSet())
        assertEquals(80.0, repository.observeRecords().first().first { it.id == "new" }.speedKmh, 0.0)
        api.response = emptyList()
        repository.refresh()
        assertEquals(2, repository.observeRecords().first().size)
    }

    @Test
    fun writeFailureReturnsLocalStorageError() = runTest {
        val dao = FakeDao().apply { writeFailure = IllegalStateException("Disco no disponible") }
        val api = FakeApi().apply { response = listOf(dto()) }
        val result = RoomTelemetryRepository(api, dao).refresh() as RefreshResult.Failed
        assertEquals(FailureKind.LOCAL_STORAGE, result.kind)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun readFailureIsNotInventedAsSuccessfulData() = runTest {
        val dao = FakeDao().apply { readFailure = IllegalStateException("Lectura imposible") }
        val repository = RoomTelemetryRepository(FakeApi(), dao)
        try {
            repository.observeRecords().first()
            fail("La lectura debía fallar")
        } catch (expected: IllegalStateException) {
            assertEquals("Lectura imposible", expected.message)
        }
    }

    @Test
    fun cancellationIsPropagated() = runTest {
        val api = FakeApi().apply { failure = CancellationException("Cancelada") }
        try {
            RoomTelemetryRepository(api, FakeDao()).refresh()
            fail("La cancelación debía propagarse")
        } catch (expected: CancellationException) {
            assertEquals("Cancelada", expected.message)
        }
    }
}
```

Estos dobles no prueban SQLite ni simulan físicamente un disco lleno. Verifican la reacción del repositorio a una falla de su colaborador, sin Mockito ni clases Android dentro de pruebas JVM.

<br/><br/>

2. Caso de uso: solo local evita actualizar

Archivo bajo `app/src/test/java/`: `com/example/advancedtracker/domain/persistence/PersistenceUseCasesTest.kt`.

```kotlin
package com.example.advancedtracker.domain.persistence

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class PersistenceUseCasesTest {
    private class Settings : TelemetrySettingsRepository {
        override val settings = MutableStateFlow(TelemetrySettings())
        override suspend fun saveOnlyMocked(enabled: Boolean) {
            settings.value = settings.value.copy(onlyMocked = enabled)
        }
        override suspend fun saveOnlyLocal(enabled: Boolean) {
            settings.value = settings.value.copy(onlyLocal = enabled)
        }
    }

    private class Records : StoredTelemetryRepository {
        val rows = MutableStateFlow<List<TelemetryRecord>>(emptyList())
        var refreshCalls = 0
        override fun observeRecords() = rows
        override suspend fun refresh(): RefreshResult {
            refreshCalls++
            return RefreshResult.Updated(0)
        }
    }

    @Test
    fun localOnlyDoesNotInvokeRefresh() = runTest {
        val records = Records()
        val settings = Settings().apply { saveOnlyLocal(true) }
        assertEquals(RefreshResult.Skipped, RefreshStoredTelemetryUseCase(records, settings)())
        assertEquals(0, records.refreshCalls)
    }

    @Test
    fun filteringUsesSavedMockedPreference() = runTest {
        val records = Records().apply {
            rows.value = listOf(
                TelemetryRecord("a", 19.0, -99.0, 60.0, Date(1L), false),
                TelemetryRecord("b", 20.0, -98.0, 30.0, Date(2L), true)
            )
        }
        val settings = Settings()
        SaveTelemetrySettingsUseCase(settings).onlyMocked(true)
        val result = ObserveStoredTelemetryUseCase(records, settings)().first()
        assertEquals(listOf("b"), result.map { it.id })
    }
}
```

<br/>


**Nota:** Las pruebas unitarias locales. No requieren el servidor Python ni un emulador.

3. **Confirma que agregaste los archivos de los puntos 16.1 y 16.2:**

   | Archivo | Ruta |
   |---|---|
   | `RoomTelemetryRepositoryTest.kt` | `app/src/test/java/com/example/advancedtracker/data/repository/persistence/` |
   | `PersistenceUseCasesTest.kt` | `app/src/test/java/com/example/advancedtracker/domain/persistence/` |

<br/>

4. **Abre CMD en la raíz del proyecto:**

   ```cmd
   cd C:\Users\Netec\AndroidStudioProjects\TelemetryTracker
   ```

<br/>

5. **Ejecuta todas las pruebas unitarias**, incluidas las de laboratorios anteriores:

   ```cmd
   .\gradlew.bat :app:testDebugUnitTest
   ```

<br/>

6. **Comprueba que termine con `BUILD SUCCESSFUL`.** Si falla, revisa el primer error antes de continuar.

<br/>

7. **Abre el reporte HTML:**

   ```cmd
   start "" "app\build\reports\tests\testDebugUnitTest\index.html"
   ```

<br/>

8. **Verifica en el reporte:**
   - Aparecen `RoomTelemetryRepositoryTest` y `PersistenceUseCasesTest`.
   - Las pruebas de ambas clases se ejecutaron.
   - El total de fallos es **0**.


<br/>
<br/>

9. Prueba instrumentada de Room real

Las **pruebas instrumentadas en Android** son pruebas automatizadas que se ejecutan en **un dispositivo físico o un emulador**, con acceso a las funciones del sistema Android. Utilizan un entorno de instrumentación y un ejecutor, como `AndroidJUnitRunner`, para comprobar el comportamiento de la aplicación. 

En el laboratorio permiten verificar:

- Que el DAO inserta, consulta y actualiza registros usando **Room y SQLite reales**.
- Que una transacción conserva la integridad de los datos.
- Que las migraciones actualizan la estructura de la base **sin perder registros**.

Los  archivos se colocan en:

```text
app/src/androidTest/java/
```

La diferencia principal con las pruebas unitarias locales es **dónde se ejecutan**: las locales corren en la JVM de tu computadora; las instrumentadas corren dentro de Android. Una prueba instrumentada puede comprobar la base de datos sin interactuar con la pantalla.


Archivo bajo `app/src/androidTest/java/`: `com/example/advancedtracker/data/local/persistence/TelemetryDaoTest.kt`.

```kotlin
package com.example.advancedtracker.data.local.persistence

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TelemetryDaoTest {
    private lateinit var database: TrackingTelemetryDatabase
    private lateinit var dao: TelemetryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TrackingTelemetryDatabase::class.java).build()
        dao = database.telemetryDao()
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun mergesWithoutDuplicatesAndKeepsHistory() = runBlocking {
        val old = TelemetryEntity("old", 19.4, -99.1, 60.0, 1L, false)
        val recent = TelemetryEntity("new", 20.0, -98.0, 30.0, 2L, true)
        dao.mergeRemote(listOf(old, recent))
        dao.mergeRemote(listOf(recent.copy(speedKmh = 80.0)))
        dao.mergeRemote(emptyList())
        val all = dao.observeAll().first()
        assertEquals(listOf("new", "old"), all.map { it.id })
        assertEquals(80.0, all.first().speedKmh, 0.0)
        assertEquals("new", dao.observeByMocked(true).first().single().id)
    }

    @Test
    fun failedBatchRollsBackAllItsChanges() = runBlocking {
        val original = TelemetryEntity("existing", 19.4, -99.1, 60.0, 1L, false)
        dao.mergeRemote(listOf(original))
        database.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER reject_test_row
            BEFORE INSERT ON telemetry_table
            WHEN NEW.id = 'rejected'
            BEGIN
                SELECT RAISE(ABORT, 'simulated storage failure');
            END
        """.trimIndent())
        try {
            dao.mergeRemote(listOf(
                original.copy(id = "accepted"),
                original.copy(id = "rejected")
            ))
            fail("El trigger debía impedir completar la transacción")
        } catch (expected: SQLiteException) {
            assertEquals(listOf(original), dao.readAll())
        }
    }
}
```

<br/>

La base en memoria es exclusiva de pruebas; la aplicación sigue usando `Room.databaseBuilder` y un archivo persistente. El trigger de la segunda prueba provoca una falla controlada en SQLite y comprueba que una actualización por lote no deja inserciones parciales; no simula físicamente agotamiento de disco.

<br/>
<br/>

10. Prueba de migraciones y conservación de datos

Archivo bajo `app/src/androidTest/java/`: `com/example/advancedtracker/data/local/persistence/TelemetryMigrationTest.kt`.

```kotlin
package com.example.advancedtracker.data.local.persistence

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TelemetryMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TrackingTelemetryDatabase::class.java
    )

    @Test
    fun manualOneToTwoPreservesRecord() {
        val name = "lab51_migration_1_2"
        helper.createDatabase(name, 1).apply {
            execSQL("""
                INSERT INTO telemetry_table
                (id, latitude, longitude, speed_kmh, timestamp, is_mocked)
                VALUES ('tel_01', 19.4326, -99.1332, 62.5, 1700000000000, 0)
            """.trimIndent())
            close()
        }
        helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2).use { database ->
            database.query("SELECT id, speed_kmh, cached_at FROM telemetry_table").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("tel_01", cursor.getString(0))
                assertEquals(62.5, cursor.getDouble(1), 0.0)
                assertEquals(0L, cursor.getLong(2))
                assertEquals(1, cursor.count)
            }
        }
    }

    @Test
    fun automaticTwoToThreePreservesRecord() {
        val name = "lab51_migration_2_3"
        helper.createDatabase(name, 2).apply {
            execSQL("""
                INSERT INTO telemetry_table
                (id, latitude, longitude, speed_kmh, timestamp, is_mocked, cached_at)
                VALUES ('tel_02', 19.0, -99.0, 70.0, 2, 1, 1234)
            """.trimIndent())
            close()
        }
        // El helper obtiene la migración automática de TrackingTelemetryDatabase.
        helper.runMigrationsAndValidate(name, 3, true).use { database ->
            database.query("SELECT id, cached_at, cache_source FROM telemetry_table").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("tel_02", cursor.getString(0))
                assertEquals(1234L, cursor.getLong(1))
                assertEquals("remote", cursor.getString(2))
            }
        }
    }

    @Test
    fun completeOneToThreeRouteIsValid() {
        val name = "lab51_migration_1_3"
        helper.createDatabase(name, 1).apply {
            execSQL("""
                INSERT INTO telemetry_table
                (id, latitude, longitude, speed_kmh, timestamp, is_mocked)
                VALUES ('chain', 19.0, -99.0, 50.0, 3, 0)
            """.trimIndent())
            close()
        }
        helper.runMigrationsAndValidate(name, 3, true, MIGRATION_1_2).use { database ->
            database.query("SELECT id, cached_at, cache_source FROM telemetry_table").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("chain", cursor.getString(0))
                assertEquals(0L, cursor.getLong(1))
                assertEquals("remote", cursor.getString(2))
            }
        }
    }
}
```

<br/>

Estas pruebas crean bases separadas; no modifican `telemetry_lab51_tracking_db`. Requieren los tres JSON exportados y configurados como assets. Validan el esquema y la conservación de datos; no se conforman con comprobar que se lanzó o capturó una excepción.

<br/>
<br/>

11. Ejecutar y localizar los reportes

Primero las pruebas JVM, que incluyen las anteriores del laboratorio 4.1:

```cmd
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

<br/>

Después, con un emulador o dispositivo conectado:

```cmd
.\gradlew.bat :app:connectedDebugAndroidTest
```

<br/>

Reportes habituales:

- Pruebas JVM: `app/build/reports/tests/testDebugUnitTest/index.html`.
- Instrumentadas: `app/build/reports/androidTests/connected/`, abriendo el `index.html` que genere tu versión de AGP.

Estos son reportes de resultados de pruebas, no porcentajes de cobertura JaCoCo. Si el proyecto ya tiene cobertura configurada, conserva sus tareas existentes.




<br/><br/>

### Paso 17. Validar el comportamiento offline y reunir las evidencias

1. Validaciones manuales finales

Antes de cada escenario, recuerda que el filtro puede ocultar registros existentes. Desactiva **Mostrar solo lecturas simuladas** para ver todo el historial.

| Escenario | Acciones | Resultado verificable |
| --- | --- | --- |
| API disponible | Desactiva solo local y pulsa Actualizar. | La respuesta se guarda; la lista sale de Room; campos coinciden con el inspector. |
| Actualización repetida | Actualiza dos veces con los mismos IDs. | No aparecen duplicados de llave primaria. |
| Caché con fallo de red | Guarda registros, detén el servidor y pulsa Actualizar. | Se informa el fallo; el historial sigue visible y Room conserva las filas. |
| Reinicio sin red | Con registros guardados y solo local desactivado, detén el servidor, ejecuta force-stop y abre la actividad. | Se lee el historial local sin esperar a que termine el timeout HTTP; luego se informa la falla de actualización. |
| Solo local | Activa la opción y pulsa Actualizar; revisa logs del servidor o del cliente. | Se informa que no se inició HTTP; los datos locales siguen visibles. |
| Filtro persistente | Activa solo simulados y reinicia el proceso. | Se recupera el mismo valor desde DataStore. |
| Filtro sin resultados | Activa solo simulados cuando todos los registros tienen isMocked=false. | Se muestra estado vacío para el filtro; no se borran filas de Room. |
| Instalación nueva sin servidor | Usa otro emulador sin datos previos; detén el servidor. | Lista vacía y mensaje de falla; no se inventan datos de caché. |
| Migraciones | Actualiza 1 → 2 → 3 sin desinstalar y con solo local activo. | Misma información de negocio y columnas técnicas nuevas; pruebas validan ambas rutas. |
| Regresión de 4.1 | Abre la actividad original y repite GET y POST. | La funcionalidad anterior sigue operativa. Tras POST, Actualizar en 5.1 incorpora el nuevo registro cuando lo devuelve GET. |

<br/>

Para el servidor local usado en 4.1, **detener el servidor** es una manera controlada de provocar una falla. No supongas que cambiar únicamente la conexión celular del emulador elimina Wi-Fi o el acceso al host por `10.0.2.2`. Comprueba la falla con el mensaje y los logs.

Una lectura Room fallida finaliza ese flujo y muestra error. La práctica no implementa recuperación automática de un archivo de base dañado: después de corregir la causa, vuelve a abrir la actividad para iniciar la observación. No uses una prueba unitaria que agregue artificialmente `Error` fuera del repositorio y luego lo declare como resultado controlado.

<br/>
<br/>

2. Evidencias que debe entregar el alumno

| Evidencia | Qué debe mostrar |
| --- | --- |
| E1. Línea base | Compilación/pruebas de 4.1 y registro del entorno que funcionaba. |
| E2. Consulta persistente | Pantalla e inspector con el mismo registro y sus seis campos. |
| E3. Datos internos | Consulta SQL de conteo, columnas y `PRAGMA user_version` final igual a 3. |
| E4. Offline tras reinicio | Historial visible con servidor detenido después de force-stop; mensaje de fallo de actualización. |
| E5. DataStore | Filtro y solo local recuperados después de reiniciar; archivo `.preferences_pb` en Device Explorer o listado con run-as. |
| E6. Migración manual | Evidencia versión 1 y 2, con el mismo registro y `cached_at` agregado. |
| E7. Migración automática | Evidencia versión 3 con `cache_source`; archivos `1.json`, `2.json`, `3.json`. |
| E8. Pruebas | Reportes JVM e instrumentados con las pruebas anteriores y nuevas pasando. |
| E9. Continuidad | GET y POST anteriores funcionando y nueva consulta local incorporando el registro creado. |

Conserva el código y los esquemas en el respaldo del proyecto. Una captura con datos solo en memoria no sustituye las evidencias después de cerrar el proceso.

<br/>
<br/>

3. Revisión opcional con IA

Puedes utilizar el asistente de IA disponible; no es obligatorio JetBrains AI Assistant. Comparte únicamente código del laboratorio, sin tokens ni datos sensibles.

Prompt sugerido:

> Revisa estas clases agregadas a TelemetryTracker para un laboratorio de Room 2.8.4, DataStore y arquitectura limpia. El DTO RemoteTelemetryDto, el servicio RemoteTelemetryApi y el dominio TelemetryData deben conservarse. El dominio existente contiene id, latitude, longitude, speedKmh, timestamp de tipo Date e isMocked. La lectura viene exclusivamente de Room; la actualización GET agrega o actualiza por id, sin borrar registros ausentes de una respuesta limitada. Identifica dependencias de infraestructura dentro del dominio, pérdida de campos, captura incorrecta de CancellationException, fallas de lectura/escritura no informadas, duplicación de DataStore y migraciones sin esquemas previos. No modifiques versiones, clases anteriores, endpoint, token ni navegación. Para cada observación indica archivo, causa y una comprobación concreta.

Valida cada sugerencia con documentación y pruebas. No aceptes como requisito agregar `Dispatchers.IO` alrededor de todas las APIs suspendidas de Room, DataStore o Retrofit, ni borrar datos para que las migraciones dejen de fallar.


### Paso 18. Resolver problemas y cerrar la práctica

### 18.1 Solución de problemas


#### KSP o plugin declarado dos veces

Revisa si el proyecto ya aplica KSP mediante `id("com.google.devtools.ksp")` o por un alias. Conserva una declaración. No muevas Hilt de KAPT a KSP como parte de esta práctica si su configuración anterior funcionaba.


#### Room no encuentra el esquema para AutoMigration

Confirma que compilaste versión 1 y luego versión 2 antes de declarar versión 3. Verifica el directorio `app/schemas/...TrackingTelemetryDatabase/` y la configuración `room.schemaDirectory`. No escribas JSON ficticios ni renombres `3.json` a `2.json`.


#### Error de integridad o migración al abrir la base

Estos errores normalmente aparecen al abrir la base en ejecución, no como una validación de los datos instalados por KSP. Confirma nombre de la base, número de versión, columnas y valores predeterminados. `defaultValue = "0"` y `DEFAULT 0` deben coincidir; también `'remote'` en versión 3. Conserva `.addMigrations(MIGRATION_1_2)`.

No uses `fallbackToDestructiveMigration()` ni desinstales la aplicación para superar la prueba: eso eliminaría los datos que debes preservar. Esta revisión utiliza `telemetry_lab51_tracking_db` para mantener separada la base previa de siete campos; confirma que el módulo, el inspector y las comprobaciones usan ese mismo nombre.


#### Múltiples DataStore activos para un archivo

Debe existir un único delegado para `telemetry_lab51_settings`. No copies el delegado a otro archivo ni crees otra instancia para esa misma ruta. Mantén separados los archivos nuevos y los de autenticación anteriores.


#### 401, conexión rechazada o URL incorrecta

Valida primero la consulta anterior de 4.1 con su token y servidor. Esta funcionalidad reutiliza `RemoteTelemetryApi` y el cliente existente. No agregues otra API, otro interceptor o una URL de ejemplo. Un 401 se informa y conserva la caché disponible; no prueba una desconexión física.


#### Database Inspector no muestra la base

Selecciona el proceso debug correcto y abre la pantalla de persistencia para que Room acceda a la base. Verifica API del dispositivo y que no se haya detenido el proceso. Esta práctica utiliza SQLite del sistema, sin un driver SQLite empaquetado.


#### No se muestran registros, pero el inspector sí tiene datos

Desactiva Mostrar solo lecturas simuladas. Comprueba que seleccionaste la misma aplicación y base. La lista muestra todos los registros históricos del filtro, por lo que puede tener más filas que la última respuesta limitada de la API.


#### No se puede iniciar la actividad

Selecciona variante debug, instala el APK y comprueba `app/src/debug/AndroidManifest.xml`. Usa el `applicationId` real en el componente `adb` si hay un sufijo. La actividad de laboratorio no existe en release.

<br/><br/>

## Limpieza 

Conserva la aplicación instalada, los datos, preferencias y esquemas para el siguiente laboratorio. Detén el servidor si ya terminaste la validación y guarda tu respaldo o commit.

Si deseas eliminar únicamente archivos de compilación, puedes ejecutar:

```powershell
.\gradlew.bat clean
```

Esto no borra Room ni DataStore del dispositivo. No ejecutes `adb uninstall`, `pm clear`, eliminación de esquemas o limpieza de Local History como cierre rutinario de esta práctica.


<br/><br/>


## Resumen

Se agregaron contratos y casos de uso de dominio, persistencia Room, preferencias DataStore y una consulta adicional que observa el almacenamiento local. La API, el DTO y las funciones anteriores se conservan. El historial sigue disponible ante fallas de red, los errores de almacenamiento se muestran y las migraciones se demuestran sobre estados reales, con pruebas y evidencias de los datos internos.

<br/><br/>

## Recursos oficiales

- Room: https://developer.android.com/training/data-storage/room
- Versiones Room: https://developer.android.com/jetpack/androidx/releases/room
- DAO y consultas: https://developer.android.com/training/data-storage/room/accessing-data
- Migraciones: https://developer.android.com/training/data-storage/room/migrating-db-versions
- MigrationTestHelper: https://developer.android.com/reference/androidx/room/testing/MigrationTestHelper
- Database Inspector: https://developer.android.com/studio/inspect/database
- Pruebas de bases de datos: https://developer.android.com/training/data-storage/room/testing-db
- DataStore: https://developer.android.com/topic/libraries/architecture/datastore
- Versiones DataStore: https://developer.android.com/jetpack/androidx/releases/datastore
- Offline-first: https://developer.android.com/topic/architecture/data-layer/offline-first
- KSP: https://github.com/google/ksp
- KSP 2.3.6: https://github.com/google/ksp/releases/tag/2.3.6

