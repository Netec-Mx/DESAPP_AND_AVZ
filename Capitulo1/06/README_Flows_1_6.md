# Tema 1.6 · Flow, StateFlow, SharedFlow y collect

Seis PoC de Kotlin para consola, basadas en las hojas compartidas. Cada archivo se compila y ejecuta por separado. Los comentarios del código se limitan a las decisiones que ayudan a comprender el comportamiento.

<br/><br/>

## 1. Temas que se practican

| Orden | Tema | Lo que podremos observar |
|---|---|---|
| 1 | Emitir, transformar y recolectar | `flow`, `emit`, `map` y `collect`. |
| 2 | Flujo frío | Crear el flujo no ejecuta su bloque; cada recolección ejecuta su propia producción. |
| 3 | StateFlow | Valor inicial, `value`, igualdad, estado compartido y conflación. |
| 4 | SharedFlow | Varios suscriptores, valores repetidos y `replay`. |
| 5 | Búfer | `extraBufferCapacity` y políticas cuando hay un suscriptor lento. |
| 6 | Convertir y compartir | `stateIn`, `shareIn`, `scope` y `WhileSubscribed`. |
| 7 | Estado y eventos | Encapsular modificaciones y observar ambos flujos por separado. |
| 8 | Conectar y desconectar la observación | Simulación en consola de la idea que después se aplica al ciclo de vida. |

**Distinción inicial:** `Flow` es la interfaz general. Aquí llamamos flujo frío al que construimos con `flow { ... }`. `StateFlow` y `SharedFlow` también son flujos, pero son calientes.

<br/><br/>


## 2. Preparación y ejecución en Windows

Necesitas un JDK, el compilador Kotlin disponible como `kotlinc` y el archivo `kotlinx-coroutines-core-jvm.jar` compatible con tu instalación. Usamos la biblioteca de corrutinas del entorno del curso; no hace falta Android Studio.

Abre CMD en la carpeta donde guardaste los archivos. Configura `COR` con la ruta real del JAR. Si ya la tienes configurada, conserva tu valor.

```bat
set "COR=C:\ruta\kotlinc\lib\kotlinx-coroutines-core-jvm.jar"
```

Para la primera demo:

```bat
kotlinc Demo01_FlowFrio.kt -cp "%COR%" -include-runtime -d demo01.jar
java -cp "demo01.jar;%COR%" Demo01_FlowFrioKt
```

Para las demás, cambia el archivo, el JAR de salida y la clase principal según esta tabla:

| Archivo de entrada | JAR de salida | Clase principal |
|---|---|---|
| `Demo01_FlowFrio.kt` | `demo01.jar` | `Demo01_FlowFrioKt` |
| `Demo02_StateFlow.kt` | `demo02.jar` | `Demo02_StateFlowKt` |
| `Demo03_SharedFlowReplay.kt` | `demo03.jar` | `Demo03_SharedFlowReplayKt` |
| `Demo04_SharedFlowBuffer.kt` | `demo04.jar` | `Demo04_SharedFlowBufferKt` |
| `Demo05_StateInShareIn.kt` | `demo05.jar` | `Demo05_StateInShareInKt` |
| `Demo06_EstadoEventos.kt` | `demo06.jar` | `Demo06_EstadoEventosKt` |

Por ejemplo, para la segunda demo:

```bat
kotlinc Demo02_StateFlow.kt -cp "%COR%" -include-runtime -d demo02.jar
java -cp "demo02.jar;%COR%" Demo02_StateFlowKt
```

En PowerShell, la primera demo se ejecuta así:

```powershell
$env:COR = 'C:\ruta\kotlinc\lib\kotlinx-coroutines-core-jvm.jar'
kotlinc Demo01_FlowFrio.kt -cp "$env:COR" -include-runtime -d demo01.jar
java -cp "demo01.jar;$env:COR" Demo01_FlowFrioKt
```

`-include-runtime` incorpora la biblioteca estándar de Kotlin al JAR, pero la biblioteca de corrutinas sigue entrando por el classpath. Por eso también indicamos `COR` al ejecutar.

**Dos apoyos de las demos:** `runBlocking` permite ejecutar corrutinas desde esta aplicación de consola y bloquea el hilo que lo invoca hasta terminar. `yield()` da oportunidad de avanzar a otras corrutinas listas del mismo ámbito; no representa un cambio de estado ni de hilo.

<br/><br/>


## 3. PoC 1: emitir, transformar y recolectar un flujo frío

**Archivo:** `Demo01_FlowFrio.kt`.

**Objetivo:** seguir una temperatura desde su emisión hasta su presentación y comprobar que un segundo recolector inicia otra ejecución.

Antes de ejecutar, predice cuántas veces aparecerá `PRODUCTOR: comienza una lectura`.

```kotlin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val temperaturas = flow {
        println("PRODUCTOR: comienza una lectura")
        for (celsius in listOf(20, 25, 30)) {
            delay(200)
            println("PRODUCTOR: emite $celsius C")
            emit(celsius)
        }
    }

    // Crear el flujo no ejecuta su bloque: hace falta recolectarlo.
    println("Flujo creado; todavia no hay lecturas")

    val mensajes = temperaturas.map { celsius ->
        "Temperatura: $celsius C = ${celsius * 9 / 5 + 32} F"
    }

    println("--- Recolector A ---")
    mensajes.collect { mensaje -> println("A recibe: $mensaje") }

    // El segundo collect vuelve a ejecutar el productor completo.
    println("--- Recolector B ---")
    mensajes.collect { mensaje -> println("B recibe: $mensaje") }
}
```

**Salida de referencia:**

```text
Flujo creado; todavia no hay lecturas
--- Recolector A ---
PRODUCTOR: comienza una lectura
PRODUCTOR: emite 20 C
A recibe: Temperatura: 20 C = 68 F
PRODUCTOR: emite 25 C
A recibe: Temperatura: 25 C = 77 F
PRODUCTOR: emite 30 C
A recibe: Temperatura: 30 C = 86 F
--- Recolector B ---
PRODUCTOR: comienza una lectura
PRODUCTOR: emite 20 C
B recibe: Temperatura: 20 C = 68 F
PRODUCTOR: emite 25 C
B recibe: Temperatura: 25 C = 77 F
PRODUCTOR: emite 30 C
B recibe: Temperatura: 30 C = 86 F
```

**Lectura del resultado:**

1. El primer mensaje aparece sin que el productor haya emitido nada.
2. `flow` define la producción; `emit` entrega cada temperatura.
3. `map` crea el mensaje con la conversión a Fahrenheit.
4. `collect` inicia la recolección y presenta los mensajes.
5. A y B recolectan uno después del otro para que sea fácil ver que el productor se ejecuta dos veces. También serían ejecuciones independientes si recolectaran concurrentemente.

El flujo termina normalmente cuando concluye su bloque. No necesitamos detener estos recolectores.

**Pregunta de comprobación:** si quitamos los dos `collect`, ¿se toman lecturas? **Respuesta:** no; solo se construye el flujo.

<br/><br/>


## 4. PoC 2: StateFlow, estado actual e igualdad

**Archivo:** `Demo02_StateFlow.kt`.

**Objetivo:** diferenciar el estado actual de una lista histórica de cambios.

```kotlin
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

class Contador {
    private val _estado = MutableStateFlow(0)
    val estado: StateFlow<Int> = _estado.asStateFlow()

    fun cambiarA(valor: Int) {
        _estado.value = valor
    }
}

fun main() = runBlocking {
    val contador = Contador()
    println("Valor disponible sin recolectores: ${contador.estado.value}")

    val observadorA = launch {
        contador.estado.collect { println("A recibe: $it") }
    }
    yield()

    contador.cambiarA(1)
    yield()
    println("Asignamos otra vez 1")
    contador.cambiarA(1) // Un valor igual al actual no genera otra emision.
    yield()

    contador.cambiarA(2)
    yield()
    val observadorB = launch {
        contador.estado.collect { println("B recibe: $it") }
    }
    yield() // B recibe el estado actual, aunque no vio los cambios anteriores.

    observadorA.cancelAndJoin()
    observadorB.cancelAndJoin()

    println("--- Recolector lento ---")
    val lento = launch {
        contador.estado.collect {
            println("LENTO recibe: $it")
            delay(250)
        }
    }
    yield()
    contador.cambiarA(3)
    contador.cambiarA(4)
    contador.cambiarA(5)
    delay(400) // El recolector lento recibe 5 y omite los estados intermedios.
    lento.cancelAndJoin()
}
```

**Salida de referencia:**

```text
Valor disponible sin recolectores: 0
A recibe: 0
A recibe: 1
Asignamos otra vez 1
A recibe: 2
B recibe: 2
--- Recolector lento ---
LENTO recibe: 2
LENTO recibe: 5
```

**Qué demuestra cada parte:**

- Antes de tener recolectores, `value` ya permite consultar el valor inicial `0`.
- A recibe el estado inicial y los cambios a `1` y `2`.
- Asignar otra vez `1` no genera otra emisión porque es igual al valor actual.
- B se conecta después y recibe `2`; no vuelve a recorrer `0` y `1`.
- El recolector lento recibe `2`, tarda en procesarlo y después recibe `5`. No recibe necesariamente todos los estados intermedios.
- `_estado` permite modificar el estado desde `Contador`; `estado` expone su observación sin un setter público.

**Igualdad y conflación:** la comparación por igualdad evita volver a emitir un estado igual al actual. Además, un recolector lento puede saltarse cambios intermedios y recibir el más reciente. No debe usarse StateFlow como historial de todos los cambios.

**Pregunta de comprobación:** ¿un suscriptor tardío recibe todos los estados? **Respuesta:** recibe el actual y los cambios posteriores que alcance a observar.

<br/><br/>


## 5. PoC 3: SharedFlow y replay

**Archivo:** `Demo03_SharedFlowReplay.kt`.

**Objetivo:** comprobar qué sucede con eventos previos y con suscriptores que llegan en momentos distintos.

```kotlin
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

suspend fun demostrarReplay(replay: Int) = coroutineScope {
    println("--- SharedFlow con replay = $replay ---")
    val avisos = MutableSharedFlow<String>(replay = replay)

    println("Emitimos PREVIO sin suscriptores")
    avisos.emit("PREVIO")

    val a = launch {
        avisos.collect { println("A recibe: $it") }
    }
    // Esperamos una suscripcion real antes de emitir el siguiente aviso.
    avisos.subscriptionCount.first { it == 1 }
    yield()
    avisos.emit("ACTUAL")
    yield()

    val b = launch {
        avisos.collect { println("B recibe: $it") }
    }
    avisos.subscriptionCount.first { it == 2 }
    yield()

    avisos.emit("NUEVO")
    avisos.emit("NUEVO") // SharedFlow tambien entrega valores consecutivos iguales.
    yield()

    a.cancelAndJoin()
    b.cancelAndJoin()
}

fun main() = runBlocking {
    demostrarReplay(0)
    demostrarReplay(1)
}
```

**Salida de referencia:**

```text
--- SharedFlow con replay = 0 ---
Emitimos PREVIO sin suscriptores
A recibe: ACTUAL
A recibe: NUEVO
B recibe: NUEVO
A recibe: NUEVO
B recibe: NUEVO
--- SharedFlow con replay = 1 ---
Emitimos PREVIO sin suscriptores
A recibe: PREVIO
A recibe: ACTUAL
B recibe: ACTUAL
A recibe: NUEVO
B recibe: NUEVO
B recibe: NUEVO
A recibe: NUEVO
```

El orden de las líneas de A y B puede variar; lo importante es qué valores recibe cada uno.

| Configuración | A, que llega primero | B, que llega después de ACTUAL |
|---|---|---|
| `replay = 0` | ACTUAL y las dos emisiones NUEVO. | Las dos emisiones NUEVO. |
| `replay = 1` | PREVIO, ACTUAL y las dos emisiones NUEVO. | ACTUAL y las dos emisiones NUEVO. |

**Observaciones:**

- Con `replay = 0`, PREVIO se emite sin suscriptores y no se conserva.
- Con `replay = 1`, cada nuevo suscriptor recibe el último valor conservado. El primero no lo consume ni lo quita para los demás.
- Cuando B se conecta, ACTUAL ya reemplazó a PREVIO como último valor conservado.
- SharedFlow entrega también las emisiones consecutivas iguales de NUEVO.
- Un evento se difunde a los suscriptores: no se reparte para que solo uno lo reciba.
- `replay` se configura al crear el MutableSharedFlow; no hay un setter para cambiar su capacidad después.

**Pregunta de comprobación:** ¿`replay = 1` significa que solo un suscriptor recibe el evento? **Respuesta:** no; indica cuántos valores anteriores se ofrecen a cada nuevo suscriptor.

<br/><br/>


## 6. PoC 4: un suscriptor lento y un búfer de dos valores

**Archivo:** `Demo04_SharedFlowBuffer.kt`.

**Objetivo:** observar las tres políticas de desbordamiento sin mezclar este caso con replay.

```kotlin
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

suspend fun demostrarBuffer(politica: BufferOverflow) = coroutineScope {
    println("--- $politica ---")
    val avisos = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 2,
        onBufferOverflow = politica
    )
    val recibidos = mutableListOf<Int>()
    val lento = launch {
        avisos.collect { valor ->
            recibidos.add(valor)
            println("RECEPTOR: procesa $valor")
            delay(150)
        }
    }
    avisos.subscriptionCount.first { it == 1 }

    avisos.emit(1)
    yield() // El receptor empieza a procesar 1 antes de la rafaga.
    for (valor in 2..5) {
        println("EMISOR: intenta emitir $valor")
        avisos.emit(valor)
        println("EMISOR: termino emit($valor)")
    }

    delay(900)
    lento.cancelAndJoin()
    println("Valores recibidos: $recibidos")
}

fun main() = runBlocking {
    demostrarBuffer(BufferOverflow.SUSPEND)
    demostrarBuffer(BufferOverflow.DROP_OLDEST)
    demostrarBuffer(BufferOverflow.DROP_LATEST)
}
```

La demo hace que el receptor empiece a procesar `1` y, mientras espera, el productor intenta emitir una ráfaga de `2` a `5`. Hay dos posiciones adicionales de búfer.

| Política | Valores recibidos en esta demo | Lo que ocurre al llenarse el búfer |
|---|---|---|
| `SUSPEND` | `[1, 2, 3, 4, 5]` | `emit` espera espacio; los mensajes de emisor y receptor se intercalan. |
| `DROP_OLDEST` | `[1, 4, 5]` | Los pendientes más antiguos dejan espacio a los nuevos. |
| `DROP_LATEST` | `[1, 2, 3]` | Se descartan las nuevas emisiones que no caben. |

**Diferencia que conviene recordar:** `replay` conserva valores para nuevos suscriptores. `extraBufferCapacity` da margen ante suscriptores actuales lentos. Sin suscriptores, solo se conservan los valores de replay; la capacidad adicional no guarda eventos para alguien que llegue después.

Los tiempos de espera se usan para hacer visible un receptor lento. Las cifras de la tabla corresponden a la ráfaga controlada de este ejemplo.

**Pregunta de comprobación:** si usamos `replay = 0` y `extraBufferCapacity = 16`, ¿se guardan 16 eventos emitidos sin suscriptores? **Respuesta:** no.

<br/><br/>

## 7. PoC 5: stateIn, shareIn y WhileSubscribed

**Archivo:** `Demo05_StateInShareIn.kt`.

**Objetivo:** convertir un origen frío en una producción compartida y observar cuándo se mantiene, se detiene y se reinicia.

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

fun lecturas(): Flow<Int> = flow {
    println("PRODUCTOR: inicia una ejecucion")
    try {
        var numero = 1
        while (true) {
            delay(100)
            emit(numero++)
        }
    } finally {
        println("PRODUCTOR: termina esta ejecucion")
    }
}

suspend fun demostrarConversion(usarStateIn: Boolean) = coroutineScope {
    println("--- ${if (usarStateIn) "stateIn" else "shareIn"} ---")
    // Un Job hijo permite terminar la produccion compartida al cerrar la demo.
    val trabajo = Job(coroutineContext[Job])
    val ambito = CoroutineScope(coroutineContext + trabajo)
    val estrategia = SharingStarted.WhileSubscribed(stopTimeoutMillis = 500)
    val origen = lecturas()

    val compartido: Flow<Int> = if (usarStateIn) {
        val estado = origen.stateIn(ambito, estrategia, initialValue = 0)
        println("Valor inicial de StateFlow: ${estado.value}")
        estado
    } else {
        origen.shareIn(ambito, estrategia, replay = 1)
    }

    fun observar(nombre: String) = launch {
        compartido.collect { println("$nombre recibe: $it") }
    }

    try {
        println("Sin recolectores: todavia no comienza el productor")
        yield()
        val a = observar("A")
        delay(250)
        val b = observar("B")
        delay(200)
        a.cancelAndJoin()
        b.cancelAndJoin()

        println("Sin suscriptores durante 200 ms: volvemos antes del limite")
        delay(200)
        val c = observar("C")
        delay(200)
        c.cancelAndJoin()

        println("Ahora esperamos mas de 500 ms sin suscriptores")
        delay(700)
        println("D se conecta: recibe lo conservado y reinicia el origen")
        val d = observar("D")
        delay(250)
        d.cancelAndJoin()
    } finally {
        trabajo.cancelAndJoin()
    }
}

fun main() = runBlocking {
    demostrarConversion(usarStateIn = true)
    demostrarConversion(usarStateIn = false)
}
```

Se ejecuta el mismo recorrido dos veces:

| Primera ejecución | Segunda ejecución |
|---|---|
| `stateIn(..., initialValue = 0)` crea un StateFlow. | `shareIn(..., replay = 1)` crea un SharedFlow. |
| Hay un valor inicial consultable. | No necesita un valor inicial; repite la última emisión disponible. |

**Sigue estos pasos en la salida:**

1. Sin suscriptores, `WhileSubscribed` no arranca el origen.
2. Se conecta A y aparece `PRODUCTOR: inicia una ejecucion`.
3. Se conecta B y observa la misma producción: no aparece otro inicio por su suscripción.
4. Se retiran A y B. C llega 200 ms después, antes del límite de 500 ms, y se mantiene la misma ejecución.
5. Se retira C y pasan más de 500 ms: aparece `PRODUCTOR: termina esta ejecucion`.
6. Se conecta D y comienza una nueva ejecución del origen, cuyo contador vuelve a empezar en `1`.
7. Al cerrar la demo se cancela el ámbito compartido para que el programa termine.

**¿Por qué D puede recibir primero un número alto y después 1?** La configuración conserva por defecto el último valor aunque se detenga el origen. Al regresar, D recibe ese valor conservado y luego los de la nueva ejecución. Detener la producción y borrar el valor conservado son comportamientos distintos.

La PPT usa `WhileSubscribed(5000)`, es decir, cinco segundos. Aquí usamos **500 ms** para hacer la demostración breve; el mecanismo es el mismo. El número exacto de lecturas puede variar ligeramente con la planificación.

**Sobre scope:** el ámbito mantiene la corrutina que comparte el origen. En esta demo creamos un ámbito hijo y lo cerramos explícitamente. No crea por sí mismo un hilo nuevo. Los recolectores comparten la producción mientras esa ejecución permanece activa.

**Pregunta de comprobación:** ¿cancelar un recolector detiene inmediatamente al productor? **Respuesta:** depende de si quedan suscriptores y de la política; aquí, al irse el último, se esperan 500 ms.

<br/><br/>


## 8. PoC 6: separar estado y eventos

**Archivo:** `Demo06_EstadoEventos.kt`.

**Objetivo:** trasladar la idea del catálogo de productos de la PPT a consola, sin ViewModel ni Compose.

```kotlin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

sealed interface EstadoProductos {
    object Cargando : EstadoProductos
    data class Contenido(val productos: List<String>) : EstadoProductos
    data class Error(val mensaje: String) : EstadoProductos
}

class Catalogo {
    private val _estado = MutableStateFlow<EstadoProductos>(EstadoProductos.Cargando)
    val estado: StateFlow<EstadoProductos> = _estado.asStateFlow()

    private val _avisos = MutableSharedFlow<String>(replay = 0)
    val avisos: SharedFlow<String> = _avisos.asSharedFlow()

    suspend fun esperarObservadorDeAvisos() {
        _avisos.subscriptionCount.first { it > 0 }
    }

    suspend fun cargar(simularError: Boolean = false) {
        _estado.value = EstadoProductos.Cargando
        try {
            delay(300)
            if (simularError) throw IllegalStateException("Conexion simulada no disponible")
            _estado.value = EstadoProductos.Contenido(listOf("Teclado", "Monitor"))
            _avisos.emit("Productos actualizados")
        } catch (e: CancellationException) {
            throw e // Una cancelacion debe conservar su significado.
        } catch (e: IllegalStateException) {
            _estado.value = EstadoProductos.Error(e.message ?: "Error de carga")
            _avisos.emit("No fue posible actualizar los productos")
        }
    }
}

fun describir(estado: EstadoProductos): String = when (estado) {
    EstadoProductos.Cargando -> "Cargando..."
    is EstadoProductos.Contenido -> "Productos: ${estado.productos}"
    is EstadoProductos.Error -> "Error: ${estado.mensaje}"
}

fun main() = runBlocking {
    val catalogo = Catalogo()

    fun conectarPantalla() = launch {
        launch { catalogo.estado.collect { println("PANTALLA: ${describir(it)}") } }
        launch { catalogo.avisos.collect { println("AVISO: $it") } }
    }

    println("--- Pantalla conectada ---")
    var pantalla = conectarPantalla()
    catalogo.esperarObservadorDeAvisos()
    catalogo.cargar()
    yield()

    println("--- Pantalla desconectada ---")
    pantalla.cancelAndJoin()
    catalogo.cargar(simularError = true)
    println("La carga termino sin observadores")

    println("--- Pantalla conectada otra vez ---")
    pantalla = conectarPantalla()
    catalogo.esperarObservadorDeAvisos()
    yield() // Regresa el estado Error, pero no se repite el aviso pasado.
    pantalla.cancelAndJoin()
}
```

**Salida de referencia:**

```text
--- Pantalla conectada ---
PANTALLA: Cargando...
PANTALLA: Productos: [Teclado, Monitor]
AVISO: Productos actualizados
--- Pantalla desconectada ---
La carga termino sin observadores
--- Pantalla conectada otra vez ---
PANTALLA: Error: Conexion simulada no disponible
```

**Recorrido:**

1. La pantalla simulada observa dos flujos: estado y avisos. Cada `collect` tiene su propia corrutina porque ambos flujos permanecen abiertos.
2. La primera carga termina correctamente: se conserva el contenido y se entrega un aviso a quien está conectado.
3. Desconectamos la pantalla cancelando sus dos recolectores.
4. Una segunda carga simula un error. El estado queda en Error; el aviso se emite sin suscriptores y `replay = 0` no lo conserva.
5. Reconectamos la pantalla. Recibe el estado Error actual, pero no vuelve a recibir el aviso anterior.

`StateFlow` y `SharedFlow` no terminan normalmente al entregar un valor; detenemos sus recolectores para cerrar estas aplicaciones de consola.

**Estado:** responde «¿cómo está el catálogo ahora?». **Evento:** informa «¿qué aviso acaba de ocurrir?». En esta configuración, el evento no tiene entrega garantizada cuando no hay observadores.

**Pregunta de comprobación:** al reconectar, ¿por qué reaparece Error pero no el aviso? **Respuesta:** StateFlow conserva el estado actual; SharedFlow con replay cero no conserva el aviso anterior.

<br/><br/>


## 9. Relación con las hojas de Android

La sexta PoC es una simulación manual de conectar y desconectar observadores. No implementa el ciclo de vida de Android ni sustituye sus API.

| Idea practicada en consola | Correspondencia en las hojas |
|---|---|
| `Catalogo` expone estado y avisos sin permitir modificaciones externas. | El ViewModel expone StateFlow y SharedFlow. |
| Conectar y cancelar el recolector de estado. | `collectAsStateWithLifecycle()` adapta el flujo a State de Compose y gestiona su recolección según el ciclo de vida; por defecto, desde STARTED. |
| Conectar y cancelar el recolector de avisos. | `repeatOnLifecycle(STARTED)` con `collect` permite observar eventos mientras el ciclo está activo. |
| Cancelar y volver a crear los recolectores. | La recolección se cancela al bajar del estado mínimo y vuelve a iniciarse al regresar. |
| Configurar `WhileSubscribed`. | Controlar la producción compartida cuando cambian las suscripciones; es una decisión separada de la recolección de la pantalla. |

Las API de Android se aplicarán cuando trabajemos con la interfaz. Estas PoC permiten comprender antes el comportamiento de los datos.

<br/><br/>


## 10. Glosario

| Término | Significado en estas demos |
|---|---|
| Flow<T> | Interfaz que representa un flujo de valores de tipo T. |
| Productor | Código que proporciona los valores. |
| `flow { }` | Constructor cuyo bloque se ejecuta por cada recolección. |
| `emit(valor)` | Entrega un valor al flujo; puede suspender al emisor. |
| Operador | Operación aplicada al flujo para transformar o seleccionar valores. |
| `map` | Transforma cada valor recibido. |
| `collect` | Operación terminal que recolecta y procesa valores. |
| Recolector o suscriptor | Consumidor conectado al flujo. |
| Flujo frío | En estos ejemplos, origen cuya ejecución comienza al recolectarlo y se repite independientemente por cada recolección. |
| Flujo caliente | Flujo compartido existente al que se conectan los consumidores. |
| StateFlow | Flujo caliente que mantiene un estado actual y exige un valor inicial. |
| MutableStateFlow | Variante que permite cambiar el estado. |
| `value` | Propiedad para consultar el estado actual y, en la variante mutable, modificarlo. |
| Igualdad | Comparación mediante `equals`; StateFlow no emite de nuevo un valor igual al actual. |
| Conflación | Un recolector lento puede saltar estados intermedios y observar el más reciente. |
| SharedFlow | Flujo caliente que difunde valores a varios suscriptores. |
| MutableSharedFlow | Variante que permite emitir y configurar replay y búfer. |
| `replay` | Cantidad de valores anteriores conservados para nuevos suscriptores. |
| Búfer | Espacio para valores pendientes cuando hay suscriptores lentos. |
| `extraBufferCapacity` | Capacidad adicional que permite al emisor adelantarse a los suscriptores. |
| `SUSPEND` | Espera espacio cuando el búfer está lleno. |
| `DROP_OLDEST` | Descarta el pendiente más antiguo para admitir una emisión nueva. |
| `DROP_LATEST` | Descarta una nueva emisión que no cabe. |
| `asStateFlow` / `asSharedFlow` | Exponen vistas de observación sin los métodos públicos de modificación. |
| `stateIn` | Comparte un origen como StateFlow. |
| `shareIn` | Comparte un origen como SharedFlow. |
| Scope o ámbito | Define la vida y el contexto de las corrutinas que contiene. |
| `SharingStarted` | Política que controla cuándo se recolecta el origen compartido. |
| `WhileSubscribed` | Mantiene la producción según la presencia de suscriptores y el tiempo de espera configurado. |
| Estado | Información que describe la situación actual. |
| Evento | Valor que representa algo que ocurrió. |
| `subscriptionCount` | Número de suscriptores activos; aquí ayuda a emitir después de que se conecten. |
| `first { ... }` | Espera el primer valor que cumple una condición y termina esa recolección. |
| `yield()` | Permite avanzar a otras corrutinas listas; aquí facilita observar los pasos. |
| `cancelAndJoin()` | Cancela una corrutina y espera a que termine. |
| `collectAsStateWithLifecycle` | API de AndroidX que adapta emisiones a State de Compose respetando el ciclo de vida. |


<br/><br/>


## 11. Temas aprendidos

Al terminar las seis demos podemos:

1. Identificar quién emite, quién transforma y quién recolecta.
2. Crear un flujo frío y demostrar que cada recolección ejecuta su bloque.
3. Elegir StateFlow para representar un estado actual observable.
4. Explicar por qué no se repiten estados iguales y por qué pueden omitirse estados intermedios.
5. Compartir eventos con SharedFlow y distinguir replay de capacidad adicional.
6. Predecir qué recibe un suscriptor tardío con replay cero o uno.
7. Comparar SUSPEND, DROP_OLDEST y DROP_LATEST ante un consumidor lento.
8. Compartir un origen con stateIn o shareIn y controlar su ejecución con WhileSubscribed.
9. Encapsular las modificaciones y exponer flujos de observación.
10. Separar el estado conservado de los avisos que ocurren en un momento determinado.
11. Explicar qué sucede al desconectar y reconectar los recolectores y relacionarlo con las hojas de ciclo de vida.

## Referencias oficiales de apoyo

- [Flow](https://kotlinlang.org/docs/flow.html): creación, transformación y recolección.
- [StateFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/): estado actual, igualdad y conflación.
- [SharedFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-shared-flow/): replay y comportamiento del búfer con y sin suscriptores.
- [stateIn](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/state-in.html) y [shareIn](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/share-in.html): producción compartida.
- [WhileSubscribed](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-sharing-started/-companion/-while-subscribed.html): espera antes de detener el origen y conservación del valor.
- [Corrutinas y ciclo de vida en Android](https://developer.android.com/topic/libraries/architecture/coroutines): relación posterior con la interfaz.

