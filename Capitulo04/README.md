# Laboratorio 4.1. Integración de API REST

<br/>

## Descripción general

En este laboratorio 4.1 agregarás consumo real de una API REST al proyecto TelemetryTracker que viene de los laboratorios anteriores. Configurarás Retrofit, OkHttp y Kotlinx Serialization; consultarás y enviarás telemetría; y presentarás sus resultados en una pantalla nueva con Hilt, corrutinas y StateFlow.

La integración es adicional. Conserva el simulador GPS, los casos de uso y la pantalla existentes, así como el repositorio de motores. La nueva integración utiliza **el mismo `com.example.advancedtracker.domain.model.tracking.TelemetryData`**.  

Un DTO nuevo convierte la representación JSON a ese modelo sin modificarlo. La fecha sigue siendo `java.util.Date` en dominio y se transporta como milisegundos Unix en JSON. El **servidor Python** de esta guía utiliza ese contrato y se ejecuta localmente. La app realiza HTTP real hacia él; MockWebServer se reserva para pruebas JVM.

<br/>
<br/>

## Observiones 

Para este laboratorio utilizaremos una **API REST local**, porque permite controlar el entorno y obtener resultados reproducibles. La aplicación realizará peticiones HTTP reales con Retrofit y OkHttp, enviará y recibirá JSON y utilizará una cabecera Bearer. El servidor se ejecutará en la computadora del participante.

Esta decisión ofrece ventajas didácticas:

- **Conservar el modelo existente:** el contrato de la API corresponde a `TelemetryData`, evitando cambios ajenos al objetivo del laboratorio.

- **Controlar los escenarios:** podemos provocar respuestas exitosas, listas vacías, errores HTTP y datos incompatibles para comprobar cómo responde la aplicación.

- **Reducir dependencias externas:** la práctica no requiere cuentas, credenciales de terceros ni disponibilidad de un servicio público.

- **Facilitar el diagnóstico:** podemos revisar ambos extremos de la comunicación y distinguir errores del cliente, del servidor y de la configuración de red.

- **Practicar con datos ficticios:** cada participante puede crear registros y reiniciar el servidor para repetir el ejercicio.

El objetivo es aprender y validar la integración de red en un entorno controlado. Una vez que funcione, podremos conectar una API externa, ajustando su URL, autenticación y DTO al contrato correspondiente.

<br/>

**Resultado final:** la aplicación conserva sus funciones anteriores y ofrece una pantalla REST que consulta y envía registros del modelo existente, muestra Loading/Success/Error, permite reintentar y conserva su estado durante la rotación.

<br/>

**Alcance:** token Bearer ficticio en memoria, API local, GET y POST. Login, refresh token, DataStore, persistencia y sincronización offline se abordan en sus prácticas correspondientes. Los registros del servidor se reinician cuando se reinicia Python.

<br/>
<br/>

## Objetivos

Al finalizar podrás:

- Incorporar una fuente remota conservando los contratos y el modelo de los laboratorios anteriores.
- Configurar dependencias estables de Retrofit/OkHttp y serialización JSON.
- Convertir TelemetryData a un DTO HTTP y reconstruirlo sin cambiar sus campos ni tipos.
- Inyectar un Bearer ficticio con un interceptor y ocultarlo en logs.
- Manejar errores HTTP, conectividad y JSON preservando la cancelación de corrutinas.
- Integrar un repositorio y casos de uso remotos adicionales mediante Hilt.
- Mostrar estados de red en Compose y validar GET/POST con pruebas controladas.

<br/>
<br/>

## Prerrequisitos

- Proyecto del capítulo anterior con Compose, Material 3 y Hilt funcionales.

- Application Hilt registrada y Activity/pantallas anteriores operativas.

- Modelo existente `domain.model.tracking.TelemetryData` con los campos confirmados en el simulador: `id: String`, `latitude: Double`, `longitude: Double`, `speedKmh: Double`, `timestamp: Date`, `isMocked: Boolean`.

- Contrato original `domain.repository.TelemetryRepository` con `observeTelemetry`, `saveTelemetry` y `forceRefresh`.

- Python 3.9 o posterior, Android Studio, AVD API 31 o superior e internet para descargar dependencias.

- Conocimiento de MVVM, Hilt, Flow/StateFlow y corrutinas.

<br/>
<br/>

## Comandos de configuración inicial

### Preparar una copia y comprobar la base

1. Cierra procesos que estén compilando. Copia **la carpeta completa TelemetryTracker** a otra ubicación con un nombre como `TelemetryTracker_antes_API`. Verifica que la copia incluya `app`, `gradle`, `gradlew.bat` y los archivos de configuración. 

2. Abre la terminal de Android Studio en la raíz del proyecto y ejecuta:

```powershell
.\gradlew.bat :app:assembleDebug
```

3. Ejecuta la app anterior y confirma que sus pantallas siguen funcionando.

<br/><br/>

### Decisiones de integración

| Componente anterior | Acción en esta práctica |
| --- | --- |
| `domain.model.tracking.TelemetryData` | Conservar sin modificar |
| `domain.repository.TelemetryRepository` | Conservar sus métodos y tipos |
| `data.repository.TelemetryRepositoryImpl` | Conservar el simulador |
| `data.repository.TelemetryRepository` | Conservar el repositorio de motores; es otro tipo |
| `GetTelemetryStreamUseCase`, `SaveTelemetryUseCase` | Conservar |
| `RepositoryModule` | Conservar su binding anterior |
| ViewModel y UI anteriores | Conservar |
| MainActivity y Application | Conservar |
| Gradle y Manifest debug | Agregar únicamente las entradas descritas |

<br/>

<<OJO>> -- Extraer los nuevos codigos, solo si funciona.

La nueva rama usa `RemoteTelemetryRepository`, `RemoteTelemetryRepositoryImpl`, casos de uso Fetch/SendRemoteTelemetry, `RemoteTelemetryViewModel` y `RemoteTelemetryActivity`.

<br/><br/>

## Instrucciones

### Paso 1. Agregar dependencias sin reemplazar la configuración

**Objetivo:** disponer de red y serialización manteniendo las versiones base de Android, Compose y Hilt que ya funcionan.

**Instrucciones:**

1. Abre `gradle/libs.versions.toml`. Integra las entradas dentro de las secciones existentes. No dupliques `[versions]`, `[libraries]`, `[plugins]` ni claves.

2. Usa estas versiones para red si aún no las tienes. Si ya configuraste Retrofit 3.0.0 y OkHttp 4.12.0, reutilízalas. La versión JSON debe coincidir con la biblioteca que ya sincronizaste, si es compatible con Kotlin del proyecto. En el entorno del curso Kotlin 2.3.10 se propone JSON 1.8.1; no se afirma que sea la versión más reciente.

```toml
[versions]
retrofit = "3.0.0"
okhttp = "4.12.0"
serializacionJson = "1.8.1"

[libraries]
# Los aliases lab-* evitan confusión con los aliases de intentos anteriores.
lab-okhttp-bom = { module = "com.squareup.okhttp3:okhttp-bom", version.ref = "okhttp" }
lab-okhttp = { module = "com.squareup.okhttp3:okhttp" }
lab-logging = { module = "com.squareup.okhttp3:logging-interceptor" }
lab-mockwebserver = { module = "com.squareup.okhttp3:mockwebserver" }
lab-retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
lab-converter = { module = "com.squareup.retrofit2:converter-kotlinx-serialization", version.ref = "retrofit" }
lab-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serializacionJson" }
lab-coroutines = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
lab-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
lab-lifecycle-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
lab-lifecycle-viewmodel = { module = "androidx.lifecycle:lifecycle-viewmodel-ktx", version.ref = "lifecycle" }

[plugins]
# Agrega este alias solo si no tienes ya uno para el mismo plugin.
lab-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

`serializacionJson` está escrito igual en `[versions]` y `version.ref`. Si tu catálogo usa `serializationJson`, conserva ese nombre y ajusta la referencia exactamente. No definas las dos claves solo para ocultar un error de escritura.

<br/>

4. Integra las siguientes dependencias en app. Si la misma biblioteca ya está declarada por otro alias, reutiliza su declaración; no hace falta agregarla dos veces.

```kotlin
dependencies {
    implementation(platform(libs.lab.okhttp.bom))
    implementation(libs.lab.okhttp)
    implementation(libs.lab.logging)
    implementation(libs.lab.retrofit)
    implementation(libs.lab.converter)
    implementation(libs.lab.json)
    implementation(libs.lab.coroutines)
    implementation(libs.lab.lifecycle.compose)
    implementation(libs.lab.lifecycle.viewmodel)
    testImplementation(platform(libs.lab.okhttp.bom))
    testImplementation(libs.lab.mockwebserver)
    testImplementation(libs.lab.coroutines.test)
    testImplementation("junit:junit:4.13.2")
}
```


<OJO> No lo hice si funciona, agregare las que ya puse

<br/>

Conserva Material 3, activity-compose y activity-ktx del proyecto. `by viewModels()` requiere activity-ktx; si falta, utiliza la misma versión que activity-compose para incorporarlo. Mantén la versión de coroutines-test alineada con coroutines de producción. No sustituyas compileSdk, targetSdk, minSdk ni configuración Java.

<br/>

5. Dentro de `android.buildFeatures` habilita `buildConfig = true`, conservando las demás opciones. Se usará BuildConfig.DEBUG para los logs.

<br/>

6. En el Manifest principal añade, fuera de `<application>`, solo si falta:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

<br/>

7. En `app/src/debug/AndroidManifest.xml`, integra temporalmente el permiso HTTP local. Si no existe, crea:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:usesCleartextTraffic="true" />
</manifest>
```

Si ya tienes networkSecurityConfig, adapta sus reglas de debug para el host local; esa configuración puede prevalecer sobre el atributo. La excepción HTTP se limita al laboratorio debug; una API real utiliza HTTPS.

<<OJO>>

<br/>

8. Ejecuta Sync y después:

```powershell
.\gradlew.bat :app:assembleDebug
```

**Resultado esperado:** la base continúa compilando con las nuevas bibliotecas.

**Verificación:** BUILD SUCCESSFUL. Si el Sync falla con una versión inexistente, compara literalmente `version.ref` con `[versions]`. El plugin genera serializers; la dependencia JSON proporciona `Json`.

<br/>
<br/>

### Paso 2. Preparar una API compatible con TelemetryData

**Objetivo:** consultar y enviar los mismos datos del modelo existente mediante HTTP real.

**Instrucciones:**

1. Crea `tools/telemetry_server_merged.py` en la raíz del proyecto:

```python
import argparse
import json
import math
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

TOKEN = "lab-token"
LOCK = threading.Lock()
FIELDS = ("id", "latitude", "longitude", "speedKmh", "timestamp", "isMocked")
RECORDS = [{"id": "tel_01", "latitude": 19.4326, "longitude": -99.1332,
            "speedKmh": 62.5, "timestamp": 1700000000000, "isMocked": True}]


class Handler(BaseHTTPRequestHandler):
    def reply(self, code, value):
        raw = json.dumps(value, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        try:
            self.wfile.write(raw)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def ready(self):
        if urlparse(self.path).path != "/v1/telemetry":
            self.reply(404, {"message": "Ruta inexistente"})
            return False
        if self.headers.get("Authorization") != "Bearer " + TOKEN:
            self.reply(401, {"message": "Token incorrecto o ausente"})
            return False
        time.sleep(self.server.delay)
        if self.server.scenario == "error500":
            self.reply(500, {"message": "Error simulado"})
            return False
        return True

    def do_GET(self):
        if not self.ready():
            return
        if self.server.scenario == "invalid":
            self.reply(200, [{"id": "incompleto"}])
            return
        try:
            limit = int(parse_qs(urlparse(self.path).query).get("limit", ["50"])[0])
            if not 1 <= limit <= 100:
                raise ValueError()
        except ValueError:
            self.reply(400, {"message": "limit debe estar entre 1 y 100"})
            return
        with LOCK:
            records = [] if self.server.scenario == "empty" else list(RECORDS[:limit])
        self.reply(200, records)

    def do_POST(self):
        if not self.ready():
            return
        try:
            size = int(self.headers.get("Content-Length", "0"))
            if not 0 < size <= 65536:
                raise ValueError()
            data = json.loads(self.rfile.read(size))
            if not isinstance(data, dict) or any(key not in data for key in FIELDS):
                raise ValueError()
            if not isinstance(data["id"], str) or not data["id"].strip():
                raise ValueError()
            for key in ("latitude", "longitude", "speedKmh"):
                if type(data[key]) not in (int, float) or not math.isfinite(data[key]):
                    raise ValueError()
            if not -90 <= data["latitude"] <= 90 or not -180 <= data["longitude"] <= 180:
                raise ValueError()
            if data["speedKmh"] < 0 or type(data["timestamp"]) is not int or data["timestamp"] < 0:
                raise ValueError()
            if type(data["isMocked"]) is not bool:
                raise ValueError()
        except (ValueError, TypeError, UnicodeDecodeError):
            self.reply(400, {"message": "Payload incompatible"})
            return
        item = {key: data[key] for key in FIELDS}
        with LOCK:
            existing = next((record for record in RECORDS if record["id"] == item["id"]), None)
            if existing is not None:
                code, result = (200, existing) if existing == item else (409, {"message": "Id ya usado con otros datos"})
            else:
                RECORDS.insert(0, item)
                code, result = 201, item
        self.reply(code, result)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8080)
    parser.add_argument("--delay", type=float, default=1.0)
    parser.add_argument("--scenario", choices=["normal", "empty", "error500", "invalid"], default="normal")
    args = parser.parse_args()
    server = ThreadingHTTPServer(("127.0.0.1", args.port), Handler)
    server.delay = max(0.0, args.delay)
    server.scenario = args.scenario
    print(f"API TelemetryData: http://127.0.0.1:{args.port}/v1/telemetry ({args.scenario})")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
```

<br/>

3. Deja el servidor activo en una terminal:

```cmd
py -3 tools/telemetry_server_merged.py
```

**Contrato:**

| Operación | Respuesta |
| --- | --- |
| GET `/v1/telemetry?limit=50` | 200 y arreglo de registros |
| POST `/v1/telemetry` con registro completo, incluido id | 201 y el mismo registro almacenado |
| Repetir id y mismos datos | 200 y registro existente; no lo duplica |
| Repetir id con otros datos | 409 |
| Token ausente/incorrecto | 401 |
| Payload incompatible | 400 |
| Ruta inexistente | 404 |

El id se conserva desde el modelo. El servidor no asigna un id nuevo ni introduce campos adicionales en el modelo. El control de repetición por id pertenece a este servidor educativo en memoria; no constituye una garantía de idempotencia de cualquier backend real.

<br/>

### Verificación con Postman

1. GET `http://127.0.0.1:8080/v1/telemetry?limit=50`. En Authorization selecciona Bearer Token y escribe `lab-token`. Debes obtener 200.

2. POST `http://127.0.0.1:8080/v1/telemetry`. Mismo Bearer. Body → raw → JSON:

```json
{
  "id": "postman_01",
  "latitude": 19.4326,
  "longitude": -99.1332,
  "speedKmh": 0.0,
  "timestamp": 1700000000000,
  "isMocked": true
}
```

<br/>

3. Comprueba 201 en el primer envío y consulta GET. Repite exactamente el POST: 200 sin duplicado. Cambia speedKmh manteniendo el id: 409.

<br/>

4. Cambia el token: 401. Envía `{}` con token correcto: 400. Consulta `/v1/no-existe`: 404.

| Origen de la petición | Dirección |
| --- | --- |
| Postman/PowerShell en Windows | `127.0.0.1:8080` |
| App en AVD | `10.0.2.2:8080` |
| App en dispositivo USB con adb reverse | `127.0.0.1:8080` después de configurar reverse |

Desde el AVD, localhost representa al propio emulador; 10.0.2.2 permite acceder al loopback del host.

<br/>

**Resultado esperado:** API probada independientemente de Android.

**Verificación:** guarda evidencia de GET 200, POST 201 y errores controlados. No continúes a Android si el GET de Postman no funciona.

<br/>
<br/>

### Paso 3. Agregar un DTO y mapear al modelo existente

**Objetivo:** serializar datos de red sin cambiar TelemetryData ni agregar anotaciones a ese modelo.

**Instrucciones:**

1. Crea el paquete `com.example.advancedtracker.data.remote.dto`.

2. Crea `RemoteTelemetryDto.kt`:

```kotlin
package com.example.advancedtracker.data.remote.dto

import com.example.advancedtracker.domain.model.tracking.TelemetryData
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class RemoteTelemetryDto(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double,
    val timestamp: Long,
    val isMocked: Boolean
)

fun RemoteTelemetryDto.toTelemetryData(): TelemetryData = TelemetryData(
    id = id,
    latitude = latitude,
    longitude = longitude,
    speedKmh = speedKmh,
    timestamp = Date(timestamp),
    isMocked = isMocked
)

fun TelemetryData.toRemoteDto(): RemoteTelemetryDto = RemoteTelemetryDto(
    id = id,
    latitude = latitude,
    longitude = longitude,
    speedKmh = speedKmh,
    timestamp = timestamp.time,
    isMocked = isMocked
)
```

**Nota:** No crees una segunda clase TelemetryData. Este DTO solo es la representación de transporte. La conversión Date ↔ Long preserva milisegundos y los demás campos se copian sin renombrar ni inferir valores. `isMocked=true` indica datos de demostración; recibirlos por HTTP no los convierte en una lectura GPS real.

<br/>

**Resultado esperado:** la API y la pantalla utilizan el modelo anterior al salir de la capa de datos.

**Verificación:** compila. Revisa que el import sea exactamente `domain.model.tracking.TelemetryData` y que no se cambió su archivo original.

<br/>
<br/>

### Paso 4. Agregar contrato remoto y resultado de operaciones

**Objetivo:** expresar resultados de API sin sustituir el contrato que usan los casos de uso anteriores.

**Instrucciones:**

1. Crea `domain/model/remote/RemoteResult.kt`:

```kotlin
package com.example.advancedtracker.domain.model.remote

enum class RemoteErrorKind { HTTP, CONNECTION, SERIALIZATION, UNEXPECTED }

sealed interface RemoteResult<out T> {
    data class Success<out T>(val data: T) : RemoteResult<T>
    data class Error(
        val kind: RemoteErrorKind,
        val message: String,
        val httpCode: Int? = null
    ) : RemoteResult<Nothing>
}
```

<br/>

**Nota:** RemoteResult es un envoltorio de operaciones, no otro modelo de telemetría. Loading pertenecerá al estado UI. Solo errores HTTP tienen código HTTP; no se inventan códigos -1/-999 para conectividad.

<br/>

2. Agrega `domain/repository/RemoteTelemetryRepository.kt`:

```kotlin
package com.example.advancedtracker.domain.repository

import com.example.advancedtracker.domain.model.remote.RemoteResult
import com.example.advancedtracker.domain.model.tracking.TelemetryData

interface RemoteTelemetryRepository {
    suspend fun fetchHistory(): RemoteResult<List<TelemetryData>>
    suspend fun sendRecord(data: TelemetryData): RemoteResult<TelemetryData>
}
```

**Nota:** La interfaz anterior `TelemetryRepository` sigue con observeTelemetry/saveTelemetry/forceRefresh. No se agrega REST a esa interfaz ni se cambia el significado de observeTelemetry.

**Resultado esperado:** dos contratos distintos para responsabilidades distintas, ambos con TelemetryData donde corresponde.

**Verificación:** los casos de uso anteriores siguen importando `domain.repository.TelemetryRepository`. El código nuevo usa RemoteTelemetryRepository.


<br/>
<br/>

### Paso 5. Implementar servicio, interceptor y configuración de red

**Objetivo:** realizar GET/POST con autenticación ficticia y errores controlados.

**Instrucciones:**

1. Crea `data/remote/network/RemoteTelemetryApi.kt`:

```kotlin
package com.example.advancedtracker.data.remote.network

import com.example.advancedtracker.data.remote.dto.RemoteTelemetryDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RemoteTelemetryApi {
    @GET("telemetry")
    suspend fun fetchHistory(@Query("limit") limit: Int = 50): List<RemoteTelemetryDto>

    @POST("telemetry")
    suspend fun sendRecord(@Body data: RemoteTelemetryDto): RemoteTelemetryDto
}
```

<br/>

2. Crea `data/remote/network/SafeRemoteCall.kt`:

```kotlin
package com.example.advancedtracker.data.remote.network

import com.example.advancedtracker.domain.model.remote.RemoteErrorKind
import com.example.advancedtracker.domain.model.remote.RemoteResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeRemoteCall(call: suspend () -> T): RemoteResult<T> = try {
    RemoteResult.Success(call())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    val message = when (e.code()) {
        401 -> "La API rechazó el token de práctica."
        404 -> "El recurso no existe."
        409 -> "El id ya existe con otros datos."
        in 500..599 -> "El servidor no pudo completar la operación."
        else -> "La solicitud falló: HTTP ${e.code()}."
    }
    RemoteResult.Error(RemoteErrorKind.HTTP, message, e.code())
} catch (e: SerializationException) {
    RemoteResult.Error(RemoteErrorKind.SERIALIZATION, "JSON incompatible con la telemetría esperada.")
} catch (e: IOException) {
    RemoteResult.Error(RemoteErrorKind.CONNECTION, "No fue posible conectar con la API local.")
} catch (e: Exception) {
    RemoteResult.Error(RemoteErrorKind.UNEXPECTED, "Error inesperado en la operación remota.")
}
```

**Nota:** Con retorno directo de DTO, Retrofit lanza HttpException para respuestas HTTP no exitosas. Si se utilizara Response<T>, habría que inspeccionar explícitamente su estado y cuerpo. La cancelación se relanza antes de Exception. Los endpoints suspend de Retrofit utilizan peticiones asíncronas; no requieren envolver esta llamada de red en runBlocking ni añadir Dispatchers.IO para volverla asíncrona.

<br/>

3. Crea `data/remote/network/RemoteTokenStore.kt`:

```kotlin
package com.example.advancedtracker.data.remote.network

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteTokenStore @Inject constructor() {
    @Volatile
    var token: String? = "lab-token"
}
```

<br/>


4. Crea `data/remote/network/RemoteAuthInterceptor.kt`:

```kotlin
package com.example.advancedtracker.data.remote.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class RemoteAuthInterceptor @Inject constructor(
    private val tokens: RemoteTokenStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().header("Accept", "application/json")
        val token = tokens.token
        if (token.isNullOrBlank()) request.removeHeader("Authorization")
        else request.header("Authorization", "Bearer $token")
        return chain.proceed(request.build())
    }
}
```

**Nota:** El token se consulta en memoria, sin runBlocking. Este cliente solo se usa para la API del laboratorio. No implementa refresco ni reintentos automáticos de 401.

<br/>

5. Crea `data/remote/network/RemoteNetworkFactory.kt`:

```kotlin
package com.example.advancedtracker.data.remote.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object RemoteNetworkFactory {
    val json = Json { ignoreUnknownKeys = true }

    fun client(auth: RemoteAuthInterceptor, logs: Boolean): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            level = if (logs) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(auth)
            .addInterceptor(logging)
            .build()
    }

    fun api(baseUrl: String, client: OkHttpClient): RemoteTelemetryApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(RemoteTelemetryApi::class.java)
}
```

**Nota:** Los campos adicionales se ignoran, pero los campos obligatorios ausentes o incompatibles producen error. No se usa coerceInputValues para inventar coordenadas. BODY solo se usará en debug con datos ficticios; redactHeader oculta Authorization pero no los cuerpos.

<br/>

6. Agrega `di/RemoteNetworkModule.kt`:

```kotlin
package com.example.advancedtracker.di

import com.example.advancedtracker.BuildConfig
import com.example.advancedtracker.data.remote.network.RemoteAuthInterceptor
import com.example.advancedtracker.data.remote.network.RemoteNetworkFactory
import com.example.advancedtracker.data.remote.network.RemoteTelemetryApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RemoteHttpClient

@Module
@InstallIn(SingletonComponent::class)
object RemoteNetworkModule {
    private const val BASE_URL = "http://10.0.2.2:8080/v1/"

    @Provides
    @Singleton
    @RemoteHttpClient
    fun provideClient(auth: RemoteAuthInterceptor): OkHttpClient =
        RemoteNetworkFactory.client(auth, logs = BuildConfig.DEBUG)

    @Provides
    @Singleton
    fun provideRemoteApi(@RemoteHttpClient client: OkHttpClient): RemoteTelemetryApi =
        RemoteNetworkFactory.api(BASE_URL, client)
}
```

**Nota:** La URL termina en `/`. El qualifier distingue este cliente de cualquier OkHttpClient previo; no reemplaza otros proveedores. Si el namespace de tu proyecto es diferente, adapta el import de BuildConfig. No modifiques el NetworkModule previo para copiar este bloque: es un módulo nuevo.

**Resultado esperado:** red preparada sin modificar los bindings del simulador ni depender de UserPreferencesRepository/DataStore.

**Verificación:** compila. Si aparece DuplicateBindings, identifica el tipo exacto; este módulo solo proporciona RemoteTelemetryApi y un OkHttpClient calificado. Revisa si se copió el módulo dos veces.

<br/>
<br/>

### Paso 6. Agregar repositorio REST y casos de uso adicionales

**Objetivo:** integrar red con el mismo modelo, manteniendo los casos de uso existentes.

**Instrucciones:**

1. Crea `data/remote/repository/RemoteTelemetryRepositoryImpl.kt`:

```kotlin
package com.example.advancedtracker.data.remote.repository

import com.example.advancedtracker.data.remote.dto.toRemoteDto
import com.example.advancedtracker.data.remote.dto.toTelemetryData
import com.example.advancedtracker.data.remote.network.RemoteTelemetryApi
import com.example.advancedtracker.data.remote.network.safeRemoteCall
import com.example.advancedtracker.domain.model.remote.RemoteResult
import com.example.advancedtracker.domain.model.tracking.TelemetryData
import com.example.advancedtracker.domain.repository.RemoteTelemetryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteTelemetryRepositoryImpl @Inject constructor(
    private val api: RemoteTelemetryApi
) : RemoteTelemetryRepository {
    override suspend fun fetchHistory(): RemoteResult<List<TelemetryData>> =
        safeRemoteCall { api.fetchHistory().map { it.toTelemetryData() } }

    override suspend fun sendRecord(data: TelemetryData): RemoteResult<TelemetryData> =
        safeRemoteCall { api.sendRecord(data.toRemoteDto()).toTelemetryData() }
}
```

<br/>


2. Agrega `di/RemoteRepositoryModule.kt`:

```kotlin
package com.example.advancedtracker.di

import com.example.advancedtracker.data.remote.repository.RemoteTelemetryRepositoryImpl
import com.example.advancedtracker.domain.repository.RemoteTelemetryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteRepositoryModule {
    @Binds
    abstract fun bindRemoteRepository(implementation: RemoteTelemetryRepositoryImpl): RemoteTelemetryRepository
}
```

**Nota:** RepositoryModule continúa proporcionando el repositorio anterior; RemoteRepositoryModule proporciona otra interfaz y no genera un binding duplicado de la original.

<br/>

3. Crea `domain/usecase/remote/RemoteTelemetryUseCases.kt`:

```kotlin
package com.example.advancedtracker.domain.usecase.remote

import com.example.advancedtracker.domain.model.tracking.TelemetryData
import com.example.advancedtracker.domain.repository.RemoteTelemetryRepository
import javax.inject.Inject

class FetchRemoteTelemetryUseCase @Inject constructor(
    private val repository: RemoteTelemetryRepository
) {
    suspend operator fun invoke() = repository.fetchHistory()
}

class SendRemoteTelemetryUseCase @Inject constructor(
    private val repository: RemoteTelemetryRepository
) {
    suspend operator fun invoke(data: TelemetryData) = repository.sendRecord(data)
}
```

<br/>

4. No edites GetTelemetryStreamUseCase ni SaveTelemetryUseCase. Los nuevos casos de uso no cambian el funcionamiento del stream simulado ni su almacenamiento previo.

**Resultado esperado:** el grafo anterior sigue activo y el nuevo repositorio ofrece GET/POST con TelemetryData.

**Verificación detallada:**

1. Abre Terminal en la raíz del proyecto.

2. Ejecuta `.\gradlew.bat :app:assembleDebug`.

3. Espera BUILD SUCCESSFUL; no es necesario arrancar Python para compilar.


<br/>
<br/>

### Paso 7. Agregar ViewModel y estado REST independientes

**Objetivo:** mostrar estados y controlar POST/GET manteniendo el ViewModel previo.

**Instrucciones:**

1. Crea `ui/remote/RemoteTelemetryUiState.kt`:

```kotlin
package com.example.advancedtracker.ui.remote

import com.example.advancedtracker.domain.model.tracking.TelemetryData

sealed interface RemoteTelemetryUiState {
    data object Loading : RemoteTelemetryUiState
    data class Success(val records: List<TelemetryData>, val notice: String? = null) : RemoteTelemetryUiState
    data class Error(val message: String) : RemoteTelemetryUiState
}
```

<br/>

2. Crea `ui/remote/RemoteTelemetryViewModel.kt`:

```kotlin
package com.example.advancedtracker.ui.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedtracker.domain.model.remote.RemoteResult
import com.example.advancedtracker.domain.model.tracking.TelemetryData
import com.example.advancedtracker.domain.usecase.remote.FetchRemoteTelemetryUseCase
import com.example.advancedtracker.domain.usecase.remote.SendRemoteTelemetryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RemoteTelemetryViewModel @Inject constructor(
    private val fetch: FetchRemoteTelemetryUseCase,
    private val send: SendRemoteTelemetryUseCase
) : ViewModel() {
    private val _state = MutableStateFlow<RemoteTelemetryUiState>(RemoteTelemetryUiState.Loading)
    val state = _state.asStateFlow()
    private var operation: Job? = null

    init { refresh() }

    fun refresh() {
        if (operation?.isActive == true) return
        operation = viewModelScope.launch {
            _state.value = RemoteTelemetryUiState.Loading
            publish(fetch())
        }
    }

    fun sendDemo() {
        if (operation?.isActive == true) return
        operation = viewModelScope.launch {
            _state.value = RemoteTelemetryUiState.Loading
            val sample = TelemetryData(
                id = UUID.randomUUID().toString(),
                latitude = 19.4326,
                longitude = -99.1332,
                speedKmh = 0.0,
                timestamp = Date(),
                isMocked = true
            )
            when (val result = send(sample)) {
                is RemoteResult.Error -> {
                    _state.value = RemoteTelemetryUiState.Error("No se confirmó el envío. ${result.message}")
                }
                is RemoteResult.Success -> {
                    when (val history = fetch()) {
                        is RemoteResult.Success -> publish(history, "Enviado: ${result.data.id}")
                        is RemoteResult.Error -> _state.value = RemoteTelemetryUiState.Error(
                            "El registro se envió, pero no se pudo consultar la lista. ${history.message}"
                        )
                    }
                }
            }
        }
    }

    private fun publish(result: RemoteResult<List<TelemetryData>>, notice: String? = null) {
        _state.value = when (result) {
            is RemoteResult.Success -> RemoteTelemetryUiState.Success(result.data, notice)
            is RemoteResult.Error -> RemoteTelemetryUiState.Error(result.message)
        }
    }
}
```

<br/>
<br/>

### Paso 8. Agregar una pantalla y abrirla sin sustituir MainActivity

**Objetivo:** visualizar la integración REST como una función adicional del proyecto.

**Instrucciones:**

1. Crea `ui/remote/RemoteTelemetryScreen.kt`:

```kotlin
package com.example.advancedtracker.ui.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RemoteTelemetryScreen(
    state: RemoteTelemetryUiState,
    onRefresh: () -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Telemetría REST", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onBack) { Text("Volver") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRefresh, enabled = state !is RemoteTelemetryUiState.Loading) {
                Text("Consultar")
            }
            Button(onClick = onSend, enabled = state is RemoteTelemetryUiState.Success) {
                Text("Enviar demo")
            }
        }
        when (state) {
            RemoteTelemetryUiState.Loading -> {
                CircularProgressIndicator()
                Text("Operación de red en curso…")
            }
            is RemoteTelemetryUiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRefresh) { Text("Reintentar consulta") }
            }
            is RemoteTelemetryUiState.Success -> {
                state.notice?.let { Text(it) }
                Text("Registros: ${state.records.size}")
                if (state.records.isEmpty()) Text("No hay registros de telemetría.")
                else LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.records, key = { it.id }) { data ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Id: ${data.id}")
                                Text("Latitud: ${data.latitude}")
                                Text("Longitud: ${data.longitude}")
                                Text("Velocidad: ${data.speedKmh} km/h")
                                Text("Fecha: ${data.timestamp}")
                                Text("Demostración: ${data.isMocked}")
                            }
                        }
                    }
                }
            }
        }
    }
}
```

<br/>

2. Crea `ui/remote/RemoteTelemetryActivity.kt`:

```kotlin
package com.example.advancedtracker.ui.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RemoteTelemetryActivity : ComponentActivity() {
    private val viewModel: RemoteTelemetryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                Scaffold { padding ->
                    RemoteTelemetryScreen(
                        state = state,
                        onRefresh = viewModel::refresh,
                        onSend = viewModel::sendDemo,
                        onBack = { finish() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}
```

**Nota:** Puedes utilizar el tema Compose ya existente en vez del MaterialTheme básico. La consulta inicial está en init del ViewModel; no se invoca desde el cuerpo del composable.

<br/>

3. Para esta práctica, registra la Activity **en el Manifest debug**, junto con el atributo HTTP agregado antes:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:usesCleartextTraffic="true">
        <activity
            android:name="com.example.advancedtracker.ui.remote.RemoteTelemetryActivity"
            android:exported="true"
            android:label="Telemetría REST" />
    </application>
</manifest>
```

**Nota:** Integra el elemento en el archivo debug existente; no dupliques application. Se exporta solo en debug para abrirla desde adb o una configuración específica de Android Studio. No agregues MAIN/LAUNCHER: la pantalla inicial anterior permanece igual. Esta Activity es parte del ejercicio debug, no una configuración lista para publicar.

<br/>

4. Con el servidor normal activo y un AVD iniciado, instala y abre la pantalla adicional:

```powershell
.\gradlew.bat :app:installDebug
adb shell am start -n com.example.advancedtracker/com.example.advancedtracker.ui.remote.RemoteTelemetryActivity
```

El primer paquete antes de `/` es el applicationId instalado. Si tu variante tiene applicationIdSuffix, usa el id resultante, por ejemplo `com.example.advancedtracker.debug/com.example.advancedtracker.ui.remote.RemoteTelemetryActivity`. La clase después de `/` no cambia por un suffix. Si hay varios dispositivos conectados, selecciona el AVD con `adb -s IDENTIFICADOR ...`.

Si PowerShell no reconoce adb, usa su ruta:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.example.advancedtracker/com.example.advancedtracker.ui.remote.RemoteTelemetryActivity
```

<br/>

```cmd
adb -s emulator-5554 shell am start -W -n com.example.advancedtracker/com.example.advancedtracker.ui.remote.RemoteTelemetryActivity
```

<br/>

**Nota:** Usa tu ruta de SDK si difiere. También puedes crear una configuración Android App en Run → Edit Configurations y elegir Launch: Specified Activity, con RemoteTelemetryActivity, conservando la configuración anterior para la Activity inicial.

<br/>

5. **Acceso opcional desde la pantalla actual:** agrega `ui/remote/OpenRemoteTelemetryButton.kt`:

```kotlin
package com.example.advancedtracker.ui.remote

import android.content.Intent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.advancedtracker.BuildConfig

@Composable
fun OpenRemoteTelemetryButton() {
    if (BuildConfig.DEBUG) {
        val context = LocalContext.current
        Button(onClick = {
            context.startActivity(Intent(context, RemoteTelemetryActivity::class.java))
        }) {
            Text("Abrir telemetría REST")
        }
    }
}
```

En el Column o contenedor de botones de tu pantalla de inicio, añade la llamada:

```kotlin
OpenRemoteTelemetryButton()
```

Importa `com.example.advancedtracker.ui.remote.OpenRemoteTelemetryButton`. Esa es una adición de acceso, no una sustitución de la pantalla. Si aún no sabes dónde colocarla, usa primero el comando adb del punto 4: puedes completar y validar todo el laboratorio sin editar MainActivity.

**Resultado esperado:** una pantalla REST independiente; la pantalla anterior sigue accesible por su icono y configuración original.

**Verificación:** consulta lista, envía demo y pulsa Volver. Abre la app anterior desde su icono y verifica sus motores/simulador. Si la Activity REST se abrió sola desde adb, Volver puede regresar al launcher; eso no altera la Activity anterior.

<br/>
<br/>

### Paso 9. Validar comportamiento real y continuidad

**Objetivo:** demostrar consumo HTTP y funcionamiento previo conservado.

**Instrucciones:**

1. Ejecuta Python en modo normal y abre la Activity REST. Observa Loading → Success.

2. Enviar demo: POST debe confirmarse y el siguiente GET incluir la muestra, con el mismo id, fecha y valores. Mantén menos de 50 registros para observar el aumento del contador.

3. Rota el AVD en Success. El estado debe conservarse mientras se mantiene el mismo ViewModel y no debe aparecer un GET extra provocado solo por la recreación. Verifica las peticiones en la consola Python. Esto cubre cambio de configuración, no restauración tras muerte del proceso.

4. Detén Python y pulsa Consultar. Debe aparecer Error de conexión. Reinicia el servidor y pulsa Reintentar consulta.

5. Prueba los siguientes comandos de uno en uno, deteniendo el servidor anterior con Ctrl+C y luego pulsando Consultar en la pantalla:

```powershell
py -3 tools/telemetry_server_merged.py --scenario empty
```

```powershell
py -3 tools/telemetry_server_merged.py --scenario error500
```

```powershell
py -3 tools/telemetry_server_merged.py --scenario invalid
```

<br/>

6. Al terminar regresa a modo normal. Para observar Loading durante dos segundos usa `--delay 2`.

7. Abre la pantalla anterior y comprueba sus funciones de telemetría/motores. Ejecuta sus pruebas anteriores si están disponibles.

| Escenario | Resultado esperado |
| --- | --- |
| Normal | Lista de TelemetryData desde GET |
| Enviar demo | POST confirmado, seguido de GET; misma representación del modelo |
| Empty | Success con mensaje sin registros |
| Error500 | Error HTTP controlado |
| Invalid | Error de serialización por campos ausentes |
| Servidor apagado | Error de conexión y reintento |
| Rotación | Mismo estado sin consulta causada solo por recreación |
| Pantalla anterior | Comportamiento original conservado |

**Resultado esperado:** nueva funcionalidad REST junto con continuidad del proyecto.

**Verificación:** guarda capturas de estados, evidencia POST/GET en servidor y una captura de la pantalla anterior operativa.

<br/>
<br/>

### Paso 10. Crear pruebas de red, mapeo y ViewModel

**Objetivo:** validar la integración nueva sin reemplazar las pruebas previas.

**Instrucciones:**

1. Crea `app/src/test/java/com/example/advancedtracker/data/remote/RemoteTelemetryApiTest.kt`:

```kotlin
package com.example.advancedtracker.data.remote

import com.example.advancedtracker.data.remote.dto.toRemoteDto
import com.example.advancedtracker.data.remote.dto.toTelemetryData
import com.example.advancedtracker.data.remote.network.RemoteAuthInterceptor
import com.example.advancedtracker.data.remote.network.RemoteNetworkFactory
import com.example.advancedtracker.data.remote.network.RemoteTokenStore
import com.example.advancedtracker.data.remote.network.safeRemoteCall
import com.example.advancedtracker.data.remote.repository.RemoteTelemetryRepositoryImpl
import com.example.advancedtracker.domain.model.remote.RemoteErrorKind
import com.example.advancedtracker.domain.model.remote.RemoteResult
import com.example.advancedtracker.domain.model.tracking.TelemetryData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.Date
import java.util.concurrent.TimeUnit

class RemoteTelemetryApiTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var tokens: RemoteTokenStore
    private lateinit var repository: RemoteTelemetryRepositoryImpl
    private val itemJson = """
        {"id":"tel_01","latitude":19.4326,"longitude":-99.1332,
         "speedKmh":62.5,"timestamp":1700000000000,"isMocked":true}
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokens = RemoteTokenStore()
        client = RemoteNetworkFactory.client(RemoteAuthInterceptor(tokens), logs = false)
        repository = RemoteTelemetryRepositoryImpl(
            RemoteNetworkFactory.api(server.url("/v1/").toString(), client)
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }

    private fun respond(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code)
            .setHeader("Content-Type", "application/json").setBody(body))
    }

    @Test
    fun getMapsExistingModelAndSendsBearer() = runBlocking {
        respond(200, "[$itemJson]")
        val result = repository.fetchHistory()
        assertTrue(result is RemoteResult.Success)
        val list = (result as RemoteResult.Success).data
        assertEquals(1, list.size)
        assertEquals("tel_01", list[0].id)
        assertEquals(62.5, list[0].speedKmh, 0.0001)
        assertEquals(1700000000000L, list[0].timestamp.time)
        assertTrue(list[0].isMocked)
        val request = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(request)
        assertEquals("GET", request!!.method)
        assertEquals("/v1/telemetry?limit=50", request.path)
        assertEquals("Bearer lab-token", request.getHeader("Authorization"))
    }

    @Test
    fun postPreservesIdDateAndExistingFields() = runBlocking {
        respond(201, itemJson)
        val sample = TelemetryData(id = "tel_01", latitude = 19.4326, longitude = -99.1332,
            speedKmh = 62.5, timestamp = Date(1700000000000L), isMocked = true)
        val result = repository.sendRecord(sample)
        assertTrue(result is RemoteResult.Success)
        assertEquals(sample.id, (result as RemoteResult.Success).data.id)
        val request = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(request)
        assertEquals("POST", request!!.method)
        assertEquals("/v1/telemetry", request.path)
        val body = RemoteNetworkFactory.json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals(sample.id, body["id"]!!.jsonPrimitive.content)
        assertEquals(sample.timestamp.time.toString(), body["timestamp"]!!.jsonPrimitive.content)
        assertTrue(body.containsKey("speedKmh"))
        assertTrue(body.containsKey("isMocked"))
        assertFalse(body.containsKey("deviceId"))
        assertFalse(body.containsKey("status"))
    }

    @Test
    fun absentTokenIsOmittedAnd401IsReported() = runBlocking {
        tokens.token = null
        respond(401, "{}")
        val result = repository.fetchHistory()
        assertTrue(result is RemoteResult.Error)
        assertEquals(401, (result as RemoteResult.Error).httpCode)
        val request = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(request)
        assertNull(request!!.getHeader("Authorization"))
    }

    @Test
    fun http500IsPreserved() = runBlocking {
        respond(500, "{}")
        val result = repository.fetchHistory()
        assertTrue(result is RemoteResult.Error)
        val error = result as RemoteResult.Error
        assertEquals(RemoteErrorKind.HTTP, error.kind)
        assertEquals(500, error.httpCode)
    }

    @Test
    fun http404IsPreserved() = runBlocking {
        respond(404, "{}")
        val result = repository.fetchHistory()
        assertTrue(result is RemoteResult.Error)
        assertEquals(404, (result as RemoteResult.Error).httpCode)
    }

    @Test
    fun incompleteJsonIsSerializationError() = runBlocking {
        respond(200, "[{\"id\":\"incompleto\"}]")
        val result = repository.fetchHistory()
        assertTrue(result is RemoteResult.Error)
        assertEquals(RemoteErrorKind.SERIALIZATION, (result as RemoteResult.Error).kind)
    }

    @Test
    fun emptyArrayIsSuccessful() = runBlocking {
        respond(200, "[]")
        val result = repository.fetchHistory()
        assertTrue(result is RemoteResult.Success)
        assertTrue((result as RemoteResult.Success).data.isEmpty())
    }

    @Test
    fun dtoRoundTripPreservesOriginalModelValues() {
        val sample = TelemetryData(id = "roundtrip", latitude = -33.4489, longitude = -70.6693,
            speedKmh = 80.0, timestamp = Date(1700000000123L), isMocked = true)
        val reconstructed = sample.toRemoteDto().toTelemetryData()
        assertEquals(sample.id, reconstructed.id)
        assertEquals(sample.latitude, reconstructed.latitude, 0.0)
        assertEquals(sample.longitude, reconstructed.longitude, 0.0)
        assertEquals(sample.speedKmh, reconstructed.speedKmh, 0.0)
        assertEquals(sample.timestamp.time, reconstructed.timestamp.time)
        assertEquals(sample.isMocked, reconstructed.isMocked)
    }

    @Test
    fun ioExceptionIsConnectionError() = runBlocking {
        val result = safeRemoteCall<Unit> { throw IOException("simulado") }
        assertTrue(result is RemoteResult.Error)
        assertEquals(RemoteErrorKind.CONNECTION, (result as RemoteResult.Error).kind)
    }

    @Test
    fun cancellationIsRethrown() = runBlocking {
        val cancellation = CancellationException("prueba")
        try {
            safeRemoteCall<Unit> { throw cancellation }
            fail("La cancelación no debe transformarse en Error")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }
}
```

<br/>

2. Crea `app/src/test/java/com/example/advancedtracker/ui/remote/RemoteTelemetryViewModelTest.kt`:

```kotlin
package com.example.advancedtracker.ui.remote

import com.example.advancedtracker.domain.model.remote.RemoteErrorKind
import com.example.advancedtracker.domain.model.remote.RemoteResult
import com.example.advancedtracker.domain.model.tracking.TelemetryData
import com.example.advancedtracker.domain.repository.RemoteTelemetryRepository
import com.example.advancedtracker.domain.usecase.remote.FetchRemoteTelemetryUseCase
import com.example.advancedtracker.domain.usecase.remote.SendRemoteTelemetryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteTelemetryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val item = TelemetryData(id = "tel_01", latitude = 19.4326, longitude = -99.1332,
        speedKmh = 0.0, timestamp = Date(1700000000000L), isMocked = true)

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private class FakeRepository(item: TelemetryData) : RemoteTelemetryRepository {
        var getCalls = 0
        var postCalls = 0
        var getResult: RemoteResult<List<TelemetryData>> = RemoteResult.Success(listOf(item))
        var postResult: RemoteResult<TelemetryData> = RemoteResult.Success(item)

        override suspend fun fetchHistory(): RemoteResult<List<TelemetryData>> {
            getCalls++
            delay(1)
            return getResult
        }
        override suspend fun sendRecord(data: TelemetryData): RemoteResult<TelemetryData> {
            postCalls++
            delay(1)
            return postResult
        }
    }

    private fun viewModel(fake: FakeRepository) = RemoteTelemetryViewModel(
        FetchRemoteTelemetryUseCase(fake), SendRemoteTelemetryUseCase(fake)
    )

    @Test fun initialLoadReachesSuccess() = runTest {
        val fake = FakeRepository(item)
        val vm = viewModel(fake)
        assertEquals(RemoteTelemetryUiState.Loading, vm.state.value)
        advanceUntilIdle()
        val success = vm.state.value as RemoteTelemetryUiState.Success
        assertEquals(item.id, success.records.first().id)
        assertEquals(1, fake.getCalls)
    }

    @Test fun failedPostDoesNotTriggerGet() = runTest {
        val fake = FakeRepository(item)
        val vm = viewModel(fake)
        advanceUntilIdle()
        fake.postResult = RemoteResult.Error(RemoteErrorKind.HTTP, "Servidor falló", 500)
        vm.sendDemo()
        runCurrent()
        assertEquals(RemoteTelemetryUiState.Loading, vm.state.value)
        advanceUntilIdle()
        assertTrue(vm.state.value is RemoteTelemetryUiState.Error)
        assertEquals(1, fake.postCalls)
        assertEquals(1, fake.getCalls)
    }

    @Test fun successfulPostRefreshesHistory() = runTest {
        val fake = FakeRepository(item)
        val vm = viewModel(fake)
        advanceUntilIdle()
        vm.sendDemo()
        advanceUntilIdle()
        val success = vm.state.value as RemoteTelemetryUiState.Success
        assertTrue(success.notice!!.contains(item.id))
        assertEquals(1, fake.postCalls)
        assertEquals(2, fake.getCalls)
    }

    @Test fun failedRefreshAfterPostReportsConfirmedSend() = runTest {
        val fake = FakeRepository(item)
        val vm = viewModel(fake)
        advanceUntilIdle()
        fake.getResult = RemoteResult.Error(RemoteErrorKind.CONNECTION, "Sin red")
        vm.sendDemo()
        advanceUntilIdle()
        val error = vm.state.value as RemoteTelemetryUiState.Error
        assertTrue(error.message.contains("El registro se envió"))
        assertEquals(1, fake.postCalls)
        assertEquals(2, fake.getCalls)
    }

    @Test fun repeatedActionsWhileBusyDoNotStartAnotherOperation() = runTest {
        val fake = FakeRepository(item)
        val vm = viewModel(fake)
        vm.refresh()
        vm.sendDemo()
        advanceUntilIdle()
        assertEquals(1, fake.getCalls)
        assertEquals(0, fake.postCalls)
    }
}
```

<br/>

3. Ejecuta solo las pruebas nuevas:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.advancedtracker.data.remote.RemoteTelemetryApiTest"
```

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.advancedtracker.ui.remote.RemoteTelemetryViewModelTest"
```

<br/>

4. Ejecuta después las nuevas junto con todas las anteriores:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

**Resultado esperado:** 10 pruebas nuevas de red/mapeo y 5 nuevas de ViewModel aprobadas, además de las pruebas anteriores que ya funcionaban. No renombres ni sustituyas TelemetryViewModelTest previo.

**Verificación:** revisa `app/build/reports/tests/testDebugUnitTest/index.html`. La prueba IOException valida la clasificación del utilitario; no sustituye una prueba real de desconexión. Las pruebas JVM no sustituyen la ejecución visual ni prueban automáticamente que las pantallas anteriores se conservaron.

<br/>
<br/>

## Validación y pruebas finales

| Criterio | Evidencia |
| --- | --- |
| Base anterior funcional antes de agregar red | Compilación y pantalla previa |
| Modelo intacto | Mismos campos, tipos y paquete de TelemetryData |
| Contrato anterior intacto | observeTelemetry/saveTelemetry/forceRefresh presentes |
| Casos de uso previos intactos | Compilan sin cambios |
| GET real | Registros desde Python con timestamp convertido a Date |
| POST real | Id y campos conservados; GET posterior incluye muestra |
| Cabecera Bearer | Pruebas presente/ausente aprobadas |
| Estados UI | Loading, lista, lista vacía, Error y reintento |
| Fallo de POST | No se oculta con un GET |
| Rotación | Estado conservado durante cambio de configuración |
| Pantalla anterior | Motores/simulador operativos después de añadir red |
| Automatización | 15 pruebas nuevas y pruebas previas aprobadas |

<br/>
 
### Casos adversarios

- **401:** token ausente o incorrecto → Error, sin refresco/reintentos infinitos.
- **404/500:** código HTTP conservado, UI estable.
- **409:** mismo id con datos diferentes → Error del contrato de esta API.
- **JSON incompatible:** Error de serialización; no se rellenan coordenadas ficticias.
- **Conexión fallida:** Error con reintento de consulta.
- **POST confirmado y GET fallido:** mensaje distingue envío confirmado de actualización fallida.
- **Cancelación:** CancellationException se propaga.
- **Clicks repetidos:** se ejecuta una sola operación mientras la pantalla está ocupada.

<br/>
<br/>

## Solución de problemas

### La API anterior devuelve deviceId/status/speed

Está activo el servidor anterior en el mismo puerto. Detén ese proceso y ejecuta `telemetry_server_merged.py`. Este contrato utiliza id/latitude/longitude/speedKmh/timestamp/isMocked. No modifiques TelemetryData para adaptarlo al servidor equivocado.

### Gradle: version reference serializationJson doesn't exist

El nombre de la referencia debe coincidir con la clave real. Esta guía utiliza `serializacionJson` en ambas posiciones. Compara las letras; conserva una sola clave de versión. No cambies el import de Json para solucionar un alias del catálogo.

### Android Studio sugiere crear Json

No crees una clase Json. Verifica `implementation(libs.lab.json)`, el Sync exitoso y `import kotlinx.serialization.json.Json`. El plugin de serialización y la biblioteca JSON cumplen funciones diferentes.

### observeTelemetry/saveTelemetry no encontrados

El contrato anterior sigue sustituido por el del intento previo. Recupera ese contrato y su implementación simulada. Este laboratorio no utiliza esos nombres para red y no requiere cambiar GetTelemetryStreamUseCase ni SaveTelemetryUseCase.


### DuplicateBindings o MissingBinding

Lee el tipo exacto del error. RemoteRepositoryModule solo vincula RemoteTelemetryRepository; RepositoryModule conserva TelemetryRepository. El OkHttpClient nuevo lleva @RemoteHttpClient en el proveedor y en el parámetro del consumidor. Verifica @Inject, @Module y @InstallIn. No añadas otra Application ni otro procesamiento Hilt paralelo.

### Activity class does not exist / Permission Denial

Instala la variante debug actual y verifica la Activity en su Manifest combinado. En el comando adb usa el applicationId instalado y la clase completa. El atributo exported=true de debug permite el arranque desde adb. No copies el registro a release sin revisar el acceso deseado.

### Connection refused / timeout

Primero verifica GET con Postman. En AVD usa 10.0.2.2, no localhost. Revisa puerto y proceso Python. Para otro puerto, arranca Python con `--port 8081` y ajusta BASE_URL. No confíes en ping al host como prueba de que HTTP funciona.

### HTTP cleartext bloqueado

Verifica variante debug y Manifest combinado. Si existe networkSecurityConfig, ajusta su configuración debug para permitir la API local. La URL de esta práctica es HTTP y no debe utilizarse como configuración de publicación.

### TelemetryData no coincide con el mapeador

Confirma el import `domain.model.tracking.TelemetryData` y revisa su definición real. No uses el modelo de motores TelemetryEngine ni el Telemetry del intento anterior. Esta guía conserva las firmas confirmadas en el simulador que compartiste; si hay otros campos obligatorios en tu archivo real, ajusta únicamente el mapeo de transporte después de verificarlo, no la definición del modelo.

### Clases del intento anterior con errores

Que no se usen en la UI no evita que Kotlin las compile. Guarda una copia, identifica qué archivos pertenecen exclusivamente al intento anterior y recupéralos desde comentarios/Local History o sácalos del conjunto de fuentes solo después de verificar sus referencias. No borres modelos o repositorios por tener el mismo nombre simple en otro paquete.

<br/>
<br/>

## Limpieza

1. Detén Python con Ctrl+C.

2. Conserva el modelo, los contratos y las pruebas anteriores, junto con los componentes REST nuevos.

3. Guarda copia de proyecto y evidencias. La carpeta de respaldo debe estar fuera del proyecto activo.

4. Si configuraste `adb reverse tcp:8080 tcp:8080` para un teléfono USB, retíralo al terminar con `adb reverse --remove tcp:8080`.

5. No borres Local History ni invalidez cachés como rutina. No es obligatorio ejecutar clean; compilar de nuevo es suficiente cuando no hay un problema específico de compilados.

## Resumen

Agregaste una fuente REST al proyecto progresivo sin modificar TelemetryData ni el contrato utilizado por los casos de uso anteriores. La representación JSON introduce únicamente un DTO de transporte que convierte Date a milisegundos y de regreso.

El nuevo repositorio, sus casos de uso y su pantalla se integran con Hilt y permiten GET/POST reales. El simulador y el repositorio de motores permanecen activos. Las verificaciones combinan pruebas automatizadas nuevas, pruebas previas y comprobaciones visuales de continuidad.

<br/>
<br/>

## Recursos adicionales

- [Retrofit: documentación y código oficiales](https://github.com/square/retrofit).
- [Retrofit: cambios y versión 3.0.0](https://github.com/square/retrofit/blob/trunk/CHANGELOG.md).
- [OkHttp: logging y redacción de cabeceras](https://github.com/square/okhttp/tree/master/okhttp-logging-interceptor).
- [Kotlin: serialización](https://kotlinlang.org/docs/serialization.html).
- [Kotlin: manejo de excepciones y cancelación](https://kotlinlang.org/docs/exception-handling.html).
- [Android: inyección con Hilt](https://developer.android.com/training/dependency-injection/hilt-android).
- [Android: estado y recolección con ciclo de vida](https://developer.android.com/develop/ui/compose/state).
- [Android: IP y red del emulador](https://developer.android.com/studio/run/emulator-networking-address).
- [Android: configuración de seguridad de red](https://developer.android.com/privacy-and-security/security-config).

<br/>
<br/>

