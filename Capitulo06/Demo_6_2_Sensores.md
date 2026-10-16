# Demo 6.2 — Sensores básicos del dispositivo con Jetpack Compose

<br/>

## Descripción

Crear una aplicación pequeña con dos vistas: **Lista** y **Monitor**.

La primera consulta los sensores que Android expone en el dispositivo y muestra su nombre, fabricante, tipo y consumo declarado. La segunda reproduce los conceptos de `SensorMonitorScreen` mediante una versión sencilla que permite seleccionar acelerómetro, giroscopio, proximidad o luz y observar sus lecturas.

Se utilizan directamente `SensorManager`, `SensorEventListener` y estados de Compose. El proyecto contiene una Activity y dos archivos con composables. No incorpora capas de dominio/datos, repositorios, casos de uso, ViewModel, Hilt, Navigation Component ni pruebas unitarias. La validación se hace mediante una demostración manual.

**Comportamiento esperado:** solo se escucha el sensor seleccionado mientras el monitor está visible, la app está en estado `RESUMED` y el usuario no ha pulsado **Pausar**. Al cambiar de sensor, ir a Lista o pausar la Activity, se libera la escucha anterior.

<br/>
<br/>

## Objetivos

Al finalizar, se podrá:

- Consultar el inventario de sensores mediante `getSensorList(Sensor.TYPE_ALL)` y presentarlo en una `LazyColumn`.
- Obtener un sensor predeterminado con `getDefaultSensor()` y manejar su ausencia.
- Leer eventos del acelerómetro, giroscopio, proximidad y luz, actualizar estados de Compose y mostrar las unidades correspondientes.
- Registrar y liberar un `SensorEventListener` con `DisposableEffect` y el ciclo de vida de la Activity.

<br/>

## Qué se mostrará

| Vista | Contenido | Interacción |
| --- | --- | --- |
| Lista | Cantidad de sensores y tarjetas con sus metadatos | Desplazar la lista |
| Monitor | Selector de cuatro sensores, disponibilidad, estado y valores | Cambiar sensor; pausar y reanudar |


<br/>

| Sensor | Constante | Lecturas | Unidad |
| --- | --- | --- | --- |
| Acelerómetro | `TYPE_ACCELEROMETER` | X, Y, Z; incluye gravedad | m/s² |
| Giroscopio | `TYPE_GYROSCOPE` | Velocidad angular alrededor de X, Y, Z | rad/s |
| Proximidad | `TYPE_PROXIMITY` | Valor reportado y estado cercano/lejano | cm; puede ser binario |
| Luz | `TYPE_LIGHT` | Iluminación ambiental | lx |

La lista incluye sensores físicos y sensores derivados por software. Puede haber varias implementaciones del mismo tipo; la cantidad de entradas no equivale al número de componentes físicos. Su contenido depende del dispositivo o AVD. [1]

<br/>
<br/>


## Instrucciones

### Paso 1. Crear un proyecto independiente

En Android Studio, selecciona **New Project → Phone and Tablet → Empty Activity**, la plantilla que utiliza Jetpack Compose. No selecciones **Empty Views Activity**.

Completa los datos:

| Campo | Valor |
| --- | --- |
| Name | `SensorDemo` |
| Package name | `com.netec.demos.sensordemo` |
| Save location | `C:\Users\Netec\AndroidStudioProjects\SensorDemo` o tu carpeta de proyectos |
| Language | Kotlin, si la plantilla permite elegirlo |
| Minimum SDK | API 31 Android 12|
| Build configuration language | Kotlin DSL |

Pulsa **Finish** y espera a que termine la sincronización. Conserva las versiones de AGP, Kotlin y Compose que genere tu plantilla. Usa el JDK compatible que Android Studio configure para Gradle.

Este proyecto es independiente de `TelemetryTracker`.

<br/>
<br/>

### Paso 2. Confirmar la configuración del módulo

Abre `app/build.gradle.kts` y verifica:

- `namespace` y `applicationId`: `com.example.sensordemo`.
- `minSdk`: `30`.
- Compose habilitado por la plantilla.
- `compileSdk` y `targetSdk`: conserva los valores funcionales generados; para el entorno del curso puedes usar API 37 si ya tienes ese SDK instalado.

Conserva la forma del bloque `compileSdk` que haya generado tu versión de AGP; no sustituyas todo el archivo Gradle.

<br/>
<br/>

### Paso 3. Agregar la dependencia de ciclo de vida para Compose

En el bloque `dependencies` de `app/build.gradle.kts`, agrega una sola vez:  

```kotlin
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
```

Lifecycle 2.11.0 es una versión estable publicada. Si la plantilla ya contiene `lifecycle-runtime-compose`, conserva esa declaración. Mantén la misma versión que el resto de tus dependencias Lifecycle; si tu plantilla usa otra versión funcional, usa esa versión también en esta línea. No agregues dos declaraciones para la misma dependencia.

Esta dependencia proporciona `androidx.lifecycle.compose.LocalLifecycleOwner`. No se requieren bibliotecas adicionales para acceder a sensores: las clases `android.hardware` pertenecen al SDK de Android.

Pulsa **Sync Now**.

<br/>
<br/>

### Paso 4. Conservar el tema y el manifiesto de la plantilla

Conserva los archivos de `ui/theme` y el `AndroidManifest.xml` generado. El código llama a `SensorDemoTheme`; si Android Studio generó otro nombre para el tema, cambia solamente el import y esa llamada en `MainActivity.kt`.

Para estos cuatro sensores, con la frecuencia `SENSOR_DELAY_UI`, no se agregan permisos de ubicación, `BODY_SENSORS`, `ACTIVITY_RECOGNITION` ni permisos de alta frecuencia. No se solicita acceso a sensores de salud ni se cuentan pasos. El permiso de alta frecuencia se relaciona con adquisición de movimiento superior a 200 Hz.

No declares sensores como características obligatorias del dispositivo: la demo debe poder abrirse aunque alguno falte.

<br/>
<br/>

### Paso 5. Crear los archivos Kotlin

En la vista **Project**, abre:

`app/src/main/java/com/example/sensordemo/`

Reemplaza `MainActivity.kt` y crea dos archivos Kotlin:

| Archivo | Responsabilidad |
| --- | --- |
| `MainActivity.kt` | Obtener SensorManager y elegir Lista o Monitor |
| `SensorListScreen.kt` | Consultar y dibujar el inventario |
| `SensorMonitorScreen.kt` | Seleccionar sensor, escuchar eventos y mostrar lecturas |

<br/>

La carpeta `ui/theme` permanece como la generó Android Studio.

**Ruta completa de ejemplo para MainActivity:**

`C:\Users\Netec\AndroidStudioProjects\SensorDemo\app\src\main\java\com\example\sensordemo\MainActivity.kt`

Los otros dos archivos se crean en la misma carpeta. Si tu plantilla usa `src/main/kotlin`, conserva ese directorio y la misma estructura de paquetes.

<br/>

## 5. Código Kotlin completo

### 5.1 MainActivity.kt

La Activity configura Compose. `SensorDemoApp` obtiene el servicio una vez con `remember` y conserva la vista seleccionada con `rememberSaveable`. El condicional agrega una sola vista a la composición. Así, al volver a Lista, el monitor sale de la composición y ejecuta su limpieza.

```kotlin
package com.netec.demos.sensordemo

import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.sensordemo.ui.theme.SensorDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SensorDemoTheme {
                SensorDemoApp()
            }
        }
    }
}

@Composable
private fun SensorDemoApp() {
    val context = LocalContext.current
    val sensorManager = remember(context) {
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE)
            as SensorManager
    }
    var mostrarMonitor by rememberSaveable { mutableStateOf(false) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Demo de sensores", style = MaterialTheme.typography.headlineSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { mostrarMonitor = false },
                    enabled = mostrarMonitor
                ) { Text("Lista") }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { mostrarMonitor = true },
                    enabled = !mostrarMonitor
                ) { Text("Monitor") }
            }

            if (mostrarMonitor) {
                SensorMonitorScreen(
                    sensorManager = sensorManager,
                    modifier = Modifier.weight(1f)
                )
            } else {
                SensorListScreen(
                    sensorManager = sensorManager,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
```

### 5.2 SensorListScreen.kt

`getSensorList(TYPE_ALL)` consulta el inventario sin activar la escucha. Las tarjetas presentan metadatos que el sistema reporta; `power` es consumo declarado en mA, no una medición en tiempo real de la batería.

```kotlin
package com.netec.demos.sensordemo

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SensorListScreen(
    sensorManager: SensorManager,
    modifier: Modifier = Modifier
) {
    // Consulta el inventario; no registra listeners.
    val sensores = remember(sensorManager) {
        sensorManager.getSensorList(Sensor.TYPE_ALL)
            .sortedWith(compareBy<Sensor> { it.type }.thenBy { it.name })
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Sensores disponibles: ${sensores.size}",
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (sensores.isEmpty()) {
            item { Text("Android no reportó sensores en este dispositivo.") }
        }
        items(sensores) { sensor ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(sensor.name, style = MaterialTheme.typography.titleSmall)
                    Text("Fabricante: ${sensor.vendor}")
                    Text("Tipo: ${sensor.stringType} (${sensor.type})")
                    Text("Consumo declarado: ${sensor.power} mA")
                }
            }
        }
    }
}
```

<br/>
<br/>

### 5.3 SensorMonitorScreen.kt

El archivo se divide mediante comentarios en las tres partes usadas para explicar el tema:

1. **Selección, disponibilidad y estados:** se obtiene el sensor y se preparan las lecturas.
2. **Listener y limpieza:** se registra cuando la Activity está activa; se libera en pausa o al salir del composable.
3. **Presentación:** se dibuja el selector, el estado, las lecturas y el botón de control.

```kotlin
package com.netec.demos.sensordemo

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

@Composable
fun SensorMonitorScreen(
    sensorManager: SensorManager,
    modifier: Modifier = Modifier
) {
    // Parte 1: selección, disponibilidad y estados de Compose.
    val opciones = remember {
        listOf(
            Sensor.TYPE_ACCELEROMETER to "Acelerómetro",
            Sensor.TYPE_GYROSCOPE to "Giroscopio",
            Sensor.TYPE_PROXIMITY to "Proximidad",
            Sensor.TYPE_LIGHT to "Luz"
        )
    }
    var tipoSeleccionado by rememberSaveable {
        mutableStateOf(Sensor.TYPE_ACCELEROMETER)
    }
    var habilitado by rememberSaveable { mutableStateOf(true) }
    val sensor = remember(sensorManager, tipoSeleccionado) {
        sensorManager.getDefaultSensor(tipoSeleccionado)
    }
    var valores by remember(tipoSeleccionado) {
        mutableStateOf<List<Float>>(emptyList())
    }
    var activo by remember(tipoSeleccionado) { mutableStateOf(false) }
    var falloRegistro by remember(tipoSeleccionado) { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Parte 2: listener y limpieza coordinados con el ciclo de vida.
    DisposableEffect(sensorManager, sensor, tipoSeleccionado, lifecycleOwner, habilitado) {
        var registrado = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!registrado || event.sensor.type != tipoSeleccionado) return
                // Android reutiliza el arreglo del evento: copiamos su contenido.
                valores = event.values.toList()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Esta POC no presenta cambios de precisión.
            }
        }

        fun iniciar() {
            if (!habilitado || sensor == null || registrado) return
            registrado = sensorManager.registerListener(
                listener,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
            activo = registrado
            falloRegistro = !registrado
            Log.d("SensorDemo", "Registro: ${sensor.name}; activo=$registrado")
        }

        fun detener() {
            if (registrado) {
                registrado = false
                sensorManager.unregisterListener(listener)
                Log.d("SensorDemo", "Listener liberado: ${sensor?.name}")
            }
            activo = false
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> iniciar()
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> detener()
                else -> Unit
            }
        }

        falloRegistro = false
        lifecycleOwner.lifecycle.addObserver(observer)
        // Cubre el caso en el que entramos al monitor con la Activity ya activa.
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            iniciar()
        }

        onDispose {
            detener()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Parte 3: controles y presentación de las lecturas.
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Monitor de sensores", style = MaterialTheme.typography.titleMedium)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(opciones, key = { it.first }) { (tipo, nombre) ->
                    FilterChip(
                        selected = tipoSeleccionado == tipo,
                        onClick = { tipoSeleccionado = tipo },
                        label = { Text(nombre) }
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        opciones.first { it.first == tipoSeleccionado }.second,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (sensor == null) {
                        Text("Sensor no disponible en este dispositivo.")
                    } else {
                        Text("Sensor: ${sensor.name}")
                        Text(
                            when {
                                !habilitado -> "Lectura pausada"
                                falloRegistro -> "No se pudo activar el sensor"
                                activo -> "Escuchando eventos"
                                else -> "Esperando que la app esté en primer plano"
                            }
                        )
                        if (valores.isEmpty()) {
                            Text("Sin lecturas recibidas.")
                        } else {
                            Text(
                                if (activo) "Última lectura recibida:"
                                else "Última lectura conservada:"
                            )
                            LecturaSensor(tipoSeleccionado, valores, sensor)
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = { habilitado = !habilitado },
                enabled = sensor != null
            ) {
                Text(if (habilitado) "Pausar" else "Reanudar")
            }
        }
        item {
            Text(
                when (tipoSeleccionado) {
                    Sensor.TYPE_ACCELEROMETER ->
                        "Inclina el teléfono. El acelerómetro incluye la gravedad."
                    Sensor.TYPE_GYROSCOPE ->
                        "Gira el teléfono. Mide velocidad angular, no ángulos."
                    Sensor.TYPE_PROXIMITY ->
                        "Acerca la mano a la parte superior de la pantalla."
                    else -> "Cubre y descubre el sensor de luz del teléfono."
                }
            )
            Text("Desliza el selector horizontalmente para ver los cuatro sensores.")
        }
    }
}

@Composable
private fun LecturaSensor(tipo: Int, valores: List<Float>, sensor: Sensor) {
    when (tipo) {
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GYROSCOPE -> {
            val unidad = if (tipo == Sensor.TYPE_ACCELEROMETER) "m/s²" else "rad/s"
            listOf("X", "Y", "Z").forEachIndexed { indice, eje ->
                Text("$eje: ${formato(valores.getOrElse(indice) { 0f })} $unidad")
            }
        }
        Sensor.TYPE_LIGHT -> Text("Iluminación: ${formato(valores[0])} lx")
        Sensor.TYPE_PROXIMITY -> {
            val distancia = valores[0]
            Text("Valor reportado: ${formato(distancia)} cm")
            Text(
                if (distancia < sensor.maximumRange) "Estado: cercano"
                else "Estado: lejano"
            )
            Text("Algunos sensores reportan solo dos valores: cercano y lejano.")
        }
    }
}

private fun formato(valor: Float): String = String.format(Locale.US, "%.2f", valor)
```

<br/><br/>

## 6. Resumen de funciones creadas

| Elemento | Explicación breve |
| --- | --- |
| `getSystemService(SENSOR_SERVICE)` | Obtiene el servicio de sensores de Android |
| `getSensorList(TYPE_ALL)` | Consulta los sensores que el sistema expone |
| `getDefaultSensor(tipo)` | Obtiene la implementación predeterminada o devuelve `null` |
| `remember` | Conserva objetos y estados durante las recomposiciones |
| `rememberSaveable` | Conserva la selección y la pausa ante recreaciones de la Activity |
| `mutableStateOf` | Hace que Compose observe cambios y actualice la interfaz |
| `onSensorChanged` | Recibe los valores del sensor seleccionado |
| `event.values.toList()` | Copia los valores porque Android reutiliza los objetos de evento |
| `SENSOR_DELAY_UI` | Solicita un ritmo adecuado para una interfaz; la frecuencia real depende del sistema |
| `registerListener` | Activa la recepción; su resultado indica si el registro fue exitoso |
| `ON_RESUME` / `ON_PAUSE` | Coordinan la escucha con el primer plano de la Activity |
| `DisposableEffect` | Agrupa recursos que deben liberarse cuando cambian sus claves o se sale de la composición |
| `onDispose` | Libera el listener y elimina el observador del ciclo de vida |

<br/>

`DisposableEffect` no equivale por sí solo a detectar que la app pasó al fondo. Una Activity puede pausarse y sus composables seguir en la composición. Por eso el ejemplo también observa el ciclo de vida. [5]

Las claves incluyen el sensor seleccionado, la Activity y `habilitado`. Cambiar de sensor o pulsar **Pausar** descarta el efecto anterior y libera su listener. `registrado` evita duplicar el registro y descarta eventos pendientes después de detenerlo.

No se agregan los valores de lectura como claves del efecto: recibir una muestra debe actualizar la UI, sin volver a registrar el sensor.

**Qué se conserva:** la vista, el sensor seleccionado y el botón de pausa sobreviven a la rotación. Las lecturas usan `remember`, por lo que se reinician al recrear la Activity o salir del Monitor. Dentro del Monitor, pausar conserva la última lectura y la etiqueta como conservada.

<br/>
<br/>

## 7. Ejecutar la demo

### Desde Android Studio

1. Selecciona un teléfono con depuración USB o un AVD con API 30 o superior.
2. Selecciona la configuración **app**.
3. Pulsa **Run**.
4. Al abrir, se muestra la vista **Lista**.

<br/>

### Compilar desde PowerShell/CMD, sin ejecutar pruebas

Desde la carpeta raíz del proyecto:

```powershell
cd C:\Users\Netec\AndroidStudioProjects\SensorDemo
.\gradlew.bat :app:assembleDebug
```

Este comando compila el APK de depuración y no ejecuta pruebas unitarias. Para instalarlo en un único dispositivo conectado:

```powershell
.\gradlew.bat :app:installDebug
```

Después abre **SensorDemo** desde el dispositivo. Si hay varios dispositivos conectados, selecciona uno en Android Studio y usa **Run**.

<br/>

## 8. Guion de demostración manual

### 8.1 Inventario de sensores

1. Abre **Lista** y señala la cantidad de entradas.

2. Desplázate y muestra nombre, fabricante y tipo.

3. Si dispones de teléfono y AVD, compara sus inventarios.

<br/>

**Nota:** Primero preguntamos a Android qué sensores expone. La aplicación consulta las capacidades del dispositivo en el que se está ejecutando”.

<br/><br/>

### 8.2 Acelerómetro

1. Pulsa **Monitor**; inicia seleccionado el acelerómetro.
2. Comprueba el texto **Escuchando eventos** y la llegada de lecturas X, Y y Z.
3. Coloca el teléfono sobre una mesa, con la pantalla hacia arriba: espera aproximadamente X ≈ 0, Y ≈ 0 y Z ≈ 9.8 m/s².
4. Inclina el teléfono y observa cómo cambian los ejes.

**Nota:** La gravedad forma parte de esta medición. Por eso el acelerómetro no muestra tres ceros cuando el teléfono está quieto sobre una mesa. 

Los ejes pertenecen al sistema de coordenadas del sensor, referido a la orientación natural del dispositivo. Girar la pantalla no redefine automáticamente esos ejes. Esta POC presenta valores crudos, sin remapeo ni filtros. [1]


<br/><br/>

### 8.3 Giroscopio

1. Desliza el selector y elige **Giroscopio**.

2. Mantén el teléfono inmóvil y observa valores próximos a cero.

3. Gíralo suavemente y observa los cambios durante el giro.

4. Detén el movimiento: las lecturas deben volver cerca de cero.


**Nota:** Aquí medimos la velocidad del giro en radianes por segundo. No estamos mostrando un ángulo acumulado.

<br/><br/>

### 8.4 Proximidad

1. Elige **Proximidad**.

2. Acerca y retira la mano del sensor, que suele estar en la parte superior del teléfono.

3. Observa el valor reportado y la etiqueta cercano/lejano.

**Nota:** Algunos dispositivos entregan una distancia y otros solo dos valores. Para esta demostración, un valor menor que el rango máximo se interpreta como cercano; no estamos construyendo un medidor exacto de distancia”. 

<br/><br/>

### 8.5 Luz

1. Elige **Luz**.

2. Cubre el sensor y después expón el teléfono a una fuente de luz.

3. Observa la variación de la iluminación en lux.

**Nota:** El evento cambia el estado de Compose y la pantalla muestra el nivel de iluminación. No modificamos el brillo de la pantalla.

<br/><br/>

### 8.6 Pausa y liberación

1. Vuelve al acelerómetro y mueve el teléfono para tener cambios visibles.

2. Pulsa **Pausar**: se conserva la última lectura y deja de actualizarse.

3. Pulsa **Reanudar**: vuelven las lecturas.

4. Pulsa Inicio en Android y después vuelve a la app.

5. Cambia de **Monitor** a **Lista**.

6. En **Logcat**, filtra por `SensorDemo` y muestra los mensajes de registro y liberación.

<br/>

**Nota:** Recibir eventos requiere registrar un listener. Cuando dejamos de usarlos, debemos liberarlo. Aquí lo hacemos al pausar, cambiar de sensor o salir del monitor.

Si el usuario pausó manualmente, volver del fondo mantiene la pausa hasta que pulse **Reanudar**.

<br/><br/>

## 9. Ejecutar en el emulador

Abre el menú **… → Extended Controls → Virtual Sensors** del emulador. Si tu versión integrada en Android Studio no muestra el panel, abre el emulador en una ventana independiente desde la configuración de **Tools → Emulator**.

| Sensor | Control para demostrarlo |
| --- | --- |
| Acelerómetro | `Device Pose`: modificar la orientación o posición del dispositivo virtual |
| Giroscopio | Cambiar la orientación en `Device Pose`, si el AVD expone este sensor |
| Proximidad | `Additional sensors`: cambiar el valor de proximidad |
| Luz | `Additional sensors`: cambiar la iluminación |

Los controles disponibles dependen de la versión del emulador y del AVD. La rotación virtual permite demostrar el acelerómetro; luz y proximidad se ajustan en los controles de sensores adicionales. [7] Para el giroscopio, comprueba que el AVD lo reporte y produzca eventos; un teléfono permite mostrar el giro físico con mayor claridad.

Si aparece **Sensor no disponible**, continúa con uno presente en la lista. La aplicación no debe cerrarse.

<br/><br/>

## Conclusiones

La demostración queda completa cuando se observa:

- La lista de sensores del dispositivo, con su cantidad y metadatos.

- Lecturas de los sensores disponibles y unidades correctas.

- Mensaje claro para cualquier sensor ausente.

- Pausa y reanudación desde el botón.

- Liberación de la escucha al cambiar sensor, salir del Monitor o pausar la Activity.

- Recuperación de la escucha al volver al primer plano si el usuario la dejó habilitada.


**Nota:** No se crean clases de pruebas unitarias ni instrumentadas. Si la plantilla generó `ExampleUnitTest.kt` y `ExampleInstrumentedTest.kt`, puedes eliminarlas junto con las dependencias de ejemplo `testImplementation` y `androidTestImplementation`; no intervienen en esta demo. No se modifica código ajeno al nuevo proyecto.


## Bibliografía oficial - 2026

[1] https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview

[2] https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion

[3] https://developer.android.com/develop/sensors-and-location/sensors/sensors_position

[4] https://developer.android.com/develop/sensors-and-location/sensors/sensors_environment

[5] https://developer.android.com/develop/ui/compose/side-effects

[6] https://developer.android.com/jetpack/androidx/releases/lifecycle

[7] https://developer.android.com/studio/run/emulator-extended-controls

<br/>

