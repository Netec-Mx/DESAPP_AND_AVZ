# 1.7 Manejo de hilos, dispatchers y cancelación

<br/><br/>

## Temas que se abordan

1. Hilo de ejecución, corrutina y protección del hilo principal.
2. Diferencia entre suspensión y bloqueo.
3. Herencia del contexto al iniciar una corrutina.
4. Elección de dispatcher: Main, IO, Default y Unconfined.
5. Ejecución temporal de un bloque con `withContext` y regreso al contexto del llamador.
6. Solicitud de cancelación mediante Job, cancelación de hijas y espera de finalización.
7. Cooperación mediante `delay`, `isActive`, `ensureActive` y `yield`.
8. Propagación de `CancellationException`.
9. Limpieza con `finally` y protección de la parte suspendible con `NonCancellable`.
10. Aplicación conjunta al procesamiento de una imagen representada por intensidades de gris.


<br/><br/>

## Recorrido de las PoC

| Orden | Archivo | Pregunta que responde |
|---|---|---|
| 1 | `Demo01_SuspensionBloqueo.kt` | ¿Por qué otra corrutina puede avanzar durante delay, pero espera durante Thread.sleep en el mismo hilo? |
| 2 | `Demo02_Dispatchers.kt` | ¿En qué hilo se ejecutan las etapas y qué ocurre al salir de withContext? |
| 3 | `Demo03_CancelarYEsperar.kt` | ¿Qué diferencia hay entre solicitar la cancelación y esperar a que termine una tarea con hijas? |
| 4 | `Demo04_CancelacionCPU.kt` | ¿Cómo coopera un bucle que no usa funciones de suspensión? |
| 5 | `Demo05_Limpieza.kt` | ¿Qué sucede con una limpieza que necesita suspenderse después de la cancelación? |
| 6 | `Demo06_ProcesadorImagen.kt` | ¿Cómo se integran IO, Default, cancelación y limpieza en un proceso pequeño? |

<br/><br/>

## Preparación y ejecución en Windows

Necesitas el JDK, el compilador `kotlinc` y un JAR compatible de `kotlinx-coroutines-core-jvm`. Conserva las herramientas del curso. En la validación se utilizó Kotlin/JVM 2.2.10, JDK 17 y el JAR de corrutinas incluido en esa instalación del compilador.

Los archivos se compilan individualmente porque cada uno es un programa completo. Guarda los seis `.kt` en una carpeta y abre CMD en ella.

Si ya tienes la variable `COR` del curso, reutilízala. Si necesitas definirla, sustituye esta ruta por la ruta real del JAR:

```bat
set "COR=C:\ruta\kotlinc\lib\kotlinx-coroutines-core-jvm.jar"
```

Los siguientes comandos son para **CMD**. En PowerShell, el equivalente para la primera demo es:

```powershell
$env:COR = 'C:\ruta\kotlinc\lib\kotlinx-coroutines-core-jvm.jar'
kotlinc Demo01_SuspensionBloqueo.kt -cp "$env:COR" -include-runtime -d demo01.jar
java -cp "demo01.jar;$env:COR" Demo01_SuspensionBloqueoKt
```

`-include-runtime` incorpora la biblioteca estándar de Kotlin; la biblioteca de corrutinas sigue siendo una dependencia indicada con `-cp` tanto al compilar como al ejecutar. No se necesita Gradle, Android ni Android Studio.


<br/><br/>


## Ideas de apoyo antes de comenzar

| Concepto | Significado práctico |
|---|---|
| Hilo | Recurso del sistema operativo sobre el que se ejecutan instrucciones. |
| Corrutina | Tarea que puede suspenderse y reanudarse; su código se ejecuta sobre hilos. |
| Dispatcher | Elemento del contexto que dirige dónde puede ejecutarse la corrutina. |
| Contexto | Conjunto de elementos asociados a la corrutina; aquí nos interesan el dispatcher y el Job. |
| Scope | Ámbito que organiza las corrutinas y sus relaciones de vida. |
| Suspensión | Se pausa el avance de una corrutina; durante esa pausa, el hilo puede atender otro trabajo listo. |
| Bloqueo | El hilo queda ocupado por la operación, incluso mientras esta espera. |
| Concurrencia | Varias tareas avanzan durante un periodo común; pueden alternarse en un mismo hilo. |

`runBlocking` sirve como puente desde `main` en estas demos: crea una corrutina y bloquea el hilo que lo invoca hasta que ella y sus hijas terminan. Su bucle de eventos puede ejecutar corrutinas que heredan ese contexto. No es un dispatcher ni es el mecanismo para mantener libre una interfaz Android.

`coroutineScope` es una función suspendible que crea un ámbito para el bloque y espera a sus hijas. No crea por sí misma un hilo nuevo. Las PoC 1, 4 y 5 la usan para agrupar cada comparación.

<br/><br/>

## PoC 1. Suspender una corrutina o bloquear el hilo

**Objetivo:** ver dos corrutinas avanzar en el mismo hilo y comparar `delay` con `Thread.sleep`.

**Conceptos de la PPT:** corrutinas sobre hilos, herencia del contexto, operaciones bloqueantes y cuidado del hilo principal.

Antes de ejecutar, predice si B aparecerá antes o después de que A termine su espera en cada caso.

### Código completo

```kotlin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

suspend fun comparar(bloquear: Boolean) = coroutineScope {
    println("--- ${if (bloquear) "Thread.sleep" else "delay"} ---")
    val tarea = launch {
        println("A: empieza en ${Thread.currentThread().name}")
        if (bloquear) {
            Thread.sleep(200) // Ocupa el hilo; ninguna otra tarea puede usarlo aqui.
        } else {
            delay(200) // Suspende A; el hilo puede ejecutar otra corrutina lista.
        }
        println("A: termina la espera")
    }
    yield() // Dejamos que A comience antes del siguiente mensaje.
    println("B: puede avanzar en ${Thread.currentThread().name}")
    tarea.join()
}

fun main() = runBlocking {
    comparar(bloquear = false)
    comparar(bloquear = true)
}
```

<br/><br/>

### Compilar y ejecutar en CMD

```bat
kotlinc Demo01_SuspensionBloqueo.kt -cp "%COR%" -include-runtime -d demo01.jar
java -cp "demo01.jar;%COR%" Demo01_SuspensionBloqueoKt
```

<br/><br/>

### Salida observada de referencia

```text
--- delay ---
A: empieza en main
B: puede avanzar en main
A: termina la espera
--- Thread.sleep ---
A: empieza en main
A: termina la espera
B: puede avanzar en main
```

<br/><br/>

### Explicación paso a paso

1. `launch` hereda aquí el contexto de `runBlocking`; A y la corrutina coordinadora usan el hilo llamado `main`.
2. `yield` permite que A empiece antes de imprimir el mensaje B.
3. En la primera comparación, A llama a `delay` y se suspende. La coordinadora puede imprimir B durante esa espera.
4. En la segunda, A llama a `Thread.sleep`. El hilo queda bloqueado durante 200 ms; la coordinadora imprime B después de que A termina.
5. `join` espera a que A termine antes de pasar a la siguiente comparación.

**Observa:** hay concurrencia sin necesitar dos hilos para estas tareas. El nombre `main` identifica el hilo de esta consola; no implica que estemos usando `Dispatchers.Main` de Android.

La suspensión de A permite que el hilo atienda a la coordinadora; no garantiza que siempre haya otro trabajo disponible. El bloqueo de Thread.sleep es intencional para esta comparación.

<br/><br/>

## PoC 2. Elegir el dispatcher y regresar al contexto de coordinación

**Objetivo:** identificar el hilo de cada etapa y observar una secuencia IO → coordinación → Default → coordinación.

**Conceptos de la PPT:** herencia del contexto, IO, Default, Unconfined y `withContext`.

<br/><br/>

### Código completo

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun mostrarHilo(mensaje: String) {
    println("$mensaje | hilo=${Thread.currentThread().name}")
}

fun main() = runBlocking {
    mostrarHilo("Inicio en consola")
    launch {
        mostrarHilo("launch hereda el contexto de runBlocking")
    }.join()

    val numeros = withContext(Dispatchers.IO) {
        mostrarHilo("IO: espera bloqueante simulada")
        Thread.sleep(100) // Simula una API bloqueante; sigue ocupando un hilo de IO.
        listOf(1, 2, 3, 4)
    }
    mostrarHilo("Regreso a la coordinacion")

    val suma = withContext(Dispatchers.Default) {
        mostrarHilo("Default: calculo")
        numeros.sumOf { it * it }
    }
    mostrarHilo("Resultado: $suma")

    launch(Dispatchers.Unconfined) {
        mostrarHilo("Unconfined: antes de delay")
        delay(100)
        mostrarHilo("Unconfined: despues de delay")
    }.join()

    mostrarHilo("Fin en consola")
    // Dispatchers.Main requiere una integracion de UI; no se utiliza en esta consola.
}
```
<br/><br/>

### Compilar y ejecutar en CMD

```bat
kotlinc Demo02_Dispatchers.kt -cp "%COR%" -include-runtime -d demo02.jar
java -cp "demo02.jar;%COR%" Demo02_DispatchersKt
```

<br/><br/>

### Salida observada de referencia

```text
Inicio en consola | hilo=main
launch hereda el contexto de runBlocking | hilo=main
IO: espera bloqueante simulada | hilo=DefaultDispatcher-worker-1
Regreso a la coordinacion | hilo=main
Default: calculo | hilo=DefaultDispatcher-worker-1
Resultado: 30 | hilo=main
Unconfined: antes de delay | hilo=main
Unconfined: despues de delay | hilo=kotlinx.coroutines.DefaultExecutor
Fin en consola | hilo=main
```

Los nombres y números de los hilos de trabajo pueden cambiar entre ejecuciones. No se deben usar como identificadores de una corrutina ni como una prueba de rendimiento.

<br/><br/>


### Explicación paso a paso

1. El primer `launch` hereda el contexto y se ejecuta en el hilo de coordinación de esta consola.
2. `withContext(IO)` ejecuta una espera bloqueante simulada en un hilo apropiado para ese tipo de trabajo. Thread.sleep continúa bloqueando ese hilo de trabajo; cambiar el dispatcher no transforma el bloqueo en suspensión.
3. Al terminar el bloque, devuelve la lista y el código continúa en el contexto de coordinación.
4. `withContext(Default)` calcula la suma de cuadrados: 1 + 4 + 9 + 16 = 30. El cálculo es deliberadamente pequeño para facilitar su lectura.
5. El resultado se presenta al regresar al contexto del llamador.
6. Unconfined empieza en el hilo que lo invoca. Después de la suspensión, su continuación depende del mecanismo que la reanude. En esta ejecución, delay la reanuda en otro hilo.

**Observa:** `withContext` espera la terminación de su bloque y devuelve su resultado; el flujo del llamador continúa de forma secuencial. Cambia los elementos de contexto indicados durante ese bloque. Evitamos describirlo como «nunca crea una corrutina»: lo relevante aquí es que no se lanza una tarea independiente que el llamador deje ejecutándose sin esperarla.

<br/><br/>

### Elección según las diapositivas

| Dispatcher | Uso | Precisión que ayuda a interpretar la demo |
|---|---|---|
| Main | Interfaz de usuario y coordinación ligera en Android. | Requiere el módulo de integración correspondiente; no está disponible con solo coroutines-core en esta consola. |
| IO | Entrada/salida mediante API bloqueantes. | No vuelve automáticamente cancelable o suspendible a una API bloqueante. |
| Default | Trabajo de CPU. | El hilo concreto puede variar al reanudar una corrutina. |
| Unconfined | Casos donde no se requiere confinamiento a un hilo. | No garantiza regresar al hilo de la interfaz después de suspenderse. |

IO y Default comparten hilos de la implementación: pueden aparecer nombres iguales y cambiar entre ellos no garantiza un cambio físico de hilo. La elección se realiza según el trabajo que se ejecuta.

<br/><br/>

## PoC 3. Solicitar cancelación y esperar a que termine

**Objetivo:** ver la solicitud de cancelación, su detección, el cierre en finally y la propagación a una corrutina hija.

**Conceptos de la PPT:** Job, `cancel`, cancelación cooperativa, CancellationException, finally y jerarquía de tareas.

<br/><br/>

### Código completo

```kotlin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

fun main() = runBlocking {
    val tarea = launch {
        launch {
            try {
                println("HIJA: comienza")
                delay(5000)
                println("HIJA: completo su trabajo")
            } finally {
                println("HIJA: ejecuta finally")
            }
        }
        try {
            println("TAREA: comienza")
            repeat(10) { paso ->
                delay(100)
                println("TAREA: paso ${paso + 1}")
            }
        } catch (e: CancellationException) {
            println("TAREA: detecto la cancelacion")
            throw e // Registrar la cancelacion no debe convertirla en un exito.
        } finally {
            println("TAREA: ejecuta finally")
        }
    }

    yield()
    delay(250)
    println("CONTROL: solicita cancelar")
    tarea.cancel() // Solicita cancelar la tarea y sus hijas; no espera su cierre.
    println("CONTROL: isCancelled=${tarea.isCancelled}, isCompleted=${tarea.isCompleted}")
    tarea.join()
    println("CONTROL: finalizaron la tarea y su hija; isCompleted=${tarea.isCompleted}")
}
```

<br/><br/>

### Compilar y ejecutar en CMD

```bat
kotlinc Demo03_CancelarYEsperar.kt -cp "%COR%" -include-runtime -d demo03.jar
java -cp "demo03.jar;%COR%" Demo03_CancelarYEsperarKt
```

### Salida observada de referencia

```text
TAREA: comienza
HIJA: comienza
TAREA: paso 1
TAREA: paso 2
CONTROL: solicita cancelar
CONTROL: isCancelled=true, isCompleted=false
HIJA: ejecuta finally
TAREA: detecto la cancelacion
TAREA: ejecuta finally
CONTROL: finalizaron la tarea y su hija; isCompleted=true
```

La cantidad de pasos impresos antes de la solicitud puede variar según la planificación. La salida de referencia no es una garantía de tiempos exactos.

<br/><br/>

### Explicación paso a paso

1. La tarea principal de la demo crea una hija que también espera con delay.
2. La coordinadora espera brevemente y llama a `tarea.cancel()`.
3. La solicitud afecta a esa tarea y a su hija. No cancela a la coordinadora, que está fuera de esa rama.
4. `isCancelled` ya puede ser true mientras todavía queda limpieza pendiente y `isCompleted` es false.
5. Las esperas cancelables detectan la solicitud. La tarea registra la CancellationException y la relanza.
6. Se ejecutan los bloques finally. Su orden relativo no es la regla que se pretende enseñar.
7. `join` termina cuando la tarea y sus hijas han finalizado, incluida su limpieza.

<br/><br/>

**Observa:** `cancel` solicita; `join` espera. `join` por sí solo no solicita cancelar. `cancelAndJoin`, utilizado en las PoC siguientes, reúne ambas operaciones.

La captura de CancellationException sirve aquí para observarla. Si no se captura, su propagación ocurre automáticamente. No hace falta capturarla solo para volver a lanzarla.

<br/><br/>

## PoC 4. Cooperación en un bucle de CPU

**Objetivo:** comparar el mismo bucle con ninguna comprobación, con isActive, con ensureActive y con yield.

**Conceptos de la PPT:** cancelación cooperativa, consulta del estado, lanzamiento de CancellationException y cesión de ejecución.

<br/><br/>

### Código completo

```kotlin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.system.measureTimeMillis

enum class Cooperacion { NINGUNA, IS_ACTIVE, ENSURE_ACTIVE, YIELD }

suspend fun comparar(modo: Cooperacion) = coroutineScope {
    println("--- $modo ---")
    val iniciada = CompletableDeferred<Unit>()
    val tarea = launch(Dispatchers.Default) {
        val limite = System.nanoTime() + 600_000_000L
        var resultado = 0L
        iniciada.complete(Unit) // Solo sincroniza el inicio para hacer visible la comparacion.
        try {
            while (System.nanoTime() < limite) {
                when (modo) {
                    Cooperacion.NINGUNA -> Unit
                    Cooperacion.IS_ACTIVE -> if (!isActive) {
                        println("CPU: isActive es false; salimos del bucle")
                        break
                    }
                    Cooperacion.ENSURE_ACTIVE -> ensureActive()
                    Cooperacion.YIELD -> yield()
                }
                resultado = (resultado + 17) % 1_000_003
            }
            println("CPU: llego despues del bucle; resultado=$resultado")
        } catch (e: CancellationException) {
            println("CPU: detecto CancellationException")
            throw e
        } finally {
            println("CPU: ejecuta finally")
        }
    }
    iniciada.await()
    delay(50)
    println("CONTROL: solicita cancelar")
    tarea.cancel()
    val espera = measureTimeMillis { tarea.join() }
    println("CONTROL: espero aproximadamente $espera ms; isCancelled=${tarea.isCancelled}")
}

fun main() = runBlocking {
    Cooperacion.values().forEach { comparar(it) }
}
```

<br/><br/>

### Compilar y ejecutar en CMD

```bat
kotlinc Demo04_CancelacionCPU.kt -cp "%COR%" -include-runtime -d demo04.jar
java -cp "demo04.jar;%COR%" Demo04_CancelacionCPUKt
```

<br/><br/>

### Salida observada de referencia

```text
--- NINGUNA ---
CONTROL: solicita cancelar
CPU: llego despues del bucle; resultado=740586
CPU: ejecuta finally
CONTROL: espero aproximadamente 545 ms; isCancelled=true
--- IS_ACTIVE ---
CONTROL: solicita cancelar
CPU: isActive es false; salimos del bucle
CPU: llego despues del bucle; resultado=433477
CPU: ejecuta finally
CONTROL: espero aproximadamente 1 ms; isCancelled=true
--- ENSURE_ACTIVE ---
CONTROL: solicita cancelar
CPU: detecto CancellationException
CPU: ejecuta finally
CONTROL: espero aproximadamente 0 ms; isCancelled=true
--- YIELD ---
CONTROL: solicita cancelar
CPU: detecto CancellationException
CPU: ejecuta finally
CONTROL: espero aproximadamente 0 ms; isCancelled=true
```

Los tiempos, el resultado numérico y la intercalación exacta de algunos mensajes pueden variar. El bucle tiene un límite de 600 ms para que incluso el caso sin cooperación termine.

<br/><br/>

### Explicación paso a paso

1. Cada caso ejecuta trabajo de CPU en Default.
2. `CompletableDeferred<Unit>` funciona únicamente como una señal auxiliar: la coordinadora espera a que el bucle comience antes de solicitar la cancelación. `complete(Unit)` emite esa señal y `await()` la espera. No necesitamos desarrollar otro tema para utilizar este apoyo.
3. Tras una espera breve, la coordinadora solicita cancelar y mide cuánto tarda join en regresar.
4. Sin comprobaciones, el código sigue calculando hasta su límite y puede ejecutar instrucciones posteriores a la solicitud.
5. Con isActive, el código observa false y decide salir del bucle mediante break.
6. Con ensureActive, la comprobación lanza CancellationException al detectar la cancelación; no suspende por sí misma.
7. Con yield, se comprueba la cancelación y se da oportunidad a otras corrutinas listas. Si detecta la solicitud, también se abandona el trabajo mediante CancellationException.

<br/><br/>

| Caso | Cómo responde | Qué buscar |
|---|---|---|
| NINGUNA | Sigue hasta el límite temporal del ejemplo. | Join suele esperar bastante más; aparece el mensaje posterior al bucle. |
| IS_ACTIVE | La condición provoca un break. | Aparece «isActive es false» y después el mensaje que está fuera del bucle. |
| ENSURE_ACTIVE | Lanza CancellationException. | Se omite el mensaje posterior al bucle; se ejecuta finally. |
| YIELD | Comprueba cancelación y permite alternar trabajo cuando procede. | Se omite el mensaje posterior al bucle al detectar la solicitud; se ejecuta finally. |

<br/><br/>

**Observa:** salir con break no abandona toda la función. El código que sigue al bucle puede ejecutarse aunque el Job esté cancelado. Por eso no interpretamos isActive como una excepción automática.

No hay una velocidad fija de cancelación: depende de cuándo se solicita, cuándo se comprueba y cuánto trabajo hay entre comprobaciones. La frecuencia usada en el ejemplo facilita observar el mecanismo; no pretende medir cuál herramienta es más rápida.

<br/><br/>

## PoC 5. finally y limpieza que necesita suspenderse

**Objetivo:** comprobar que finally se ejecuta al cancelar y distinguir la limpieza directa de la que necesita una operación suspendible.

**Conceptos de la PPT:** liberación de recursos, propagación de cancelación y `withContext(NonCancellable)`.

El recurso y su confirmación de cierre se simulan mediante mensajes; no hay una conexión de red real.

<br/><br/>

### Código completo

```kotlin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

suspend fun confirmarCierre() {
    println("LIMPIEZA: inicia confirmacion suspendible")
    delay(100)
    println("LIMPIEZA: confirmacion terminada")
}

suspend fun demostrar(proteger: Boolean) = coroutineScope {
    println("--- ${if (proteger) "Con NonCancellable" else "Sin NonCancellable"} ---")
    val tarea = launch {
        try {
            println("TAREA: utiliza el recurso simulado")
            delay(5000)
        } finally {
            println("FINALLY: libera el recurso sin suspender")
            if (proteger) {
                // Protegemos solo el tramo de limpieza que necesita suspenderse.
                withContext(NonCancellable) { confirmarCierre() }
            } else {
                try {
                    confirmarCierre()
                } catch (e: CancellationException) {
                    println("LIMPIEZA: delay detecto que la tarea ya estaba cancelada")
                    throw e
                }
            }
        }
    }
    yield()
    println("CONTROL: cancela y espera")
    tarea.cancelAndJoin()
    println("CONTROL: tarea terminada; isCancelled=${tarea.isCancelled}")
}

fun main() = runBlocking {
    demostrar(proteger = false)
    demostrar(proteger = true)
}
```
<br/><br/>

### Compilar y ejecutar en CMD

```bat
kotlinc Demo05_Limpieza.kt -cp "%COR%" -include-runtime -d demo05.jar
java -cp "demo05.jar;%COR%" Demo05_LimpiezaKt
```

<br/><br/>

### Salida observada de referencia

```text
--- Sin NonCancellable ---
TAREA: utiliza el recurso simulado
CONTROL: cancela y espera
FINALLY: libera el recurso sin suspender
LIMPIEZA: inicia confirmacion suspendible
LIMPIEZA: delay detecto que la tarea ya estaba cancelada
CONTROL: tarea terminada; isCancelled=true
--- Con NonCancellable ---
TAREA: utiliza el recurso simulado
CONTROL: cancela y espera
FINALLY: libera el recurso sin suspender
LIMPIEZA: inicia confirmacion suspendible
LIMPIEZA: confirmacion terminada
CONTROL: tarea terminada; isCancelled=true
```

<br/><br/>

### Explicación paso a paso

1. La tarea comienza y queda suspendida en una espera larga.
2. La coordinadora solicita cancelar y espera su cierre.
3. En ambos casos se ejecuta finally y aparece el mensaje de limpieza que no suspende.
4. Sin protección, la llamada a delay dentro de confirmarCierre detecta que la tarea ya está cancelada. No aparece la confirmación terminada.
5. Con NonCancellable, el pequeño bloque de confirmación puede suspenderse y terminar pese a la cancelación.
6. Al regresar, el Job original sigue cancelado. NonCancellable no reinicia el trabajo principal ni lo convierte en una tarea completada con éxito.
7. CancelAndJoin espera también el tramo de limpieza protegido.

**Observa:** usamos NonCancellable solo para el tramo suspendible necesario. Las instrucciones directas del finally no necesitan esa protección para ejecutarse durante el desenrollado normal por cancelación.

NonCancellable es un Job especial, no otro dispatcher. En este bloque permite realizar la limpieza suspendible; no selecciona por sí mismo un hilo diferente.

No toda función declarada suspend falla necesariamente por el hecho de ser suspend: en este caso concreto, delay comprueba la cancelación. Esta precisión evita confundir la palabra clave con el comportamiento de cada API.

<br/><br/>

## PoC 6. Procesador de imagen representada por intensidades de gris

**Objetivo:** recorrer lectura en IO, filtro en Default, guardado en IO y presentación en el contexto de coordinación; ejecutar los caminos de éxito, cancelación y error de lectura.

**Conceptos de la PPT:** caso del procesador de imágenes, withContext, comprobación de cancelación durante CPU, IOException, CancellationException y limpieza final.

### Qué es real y qué se simula

- **Real:** lectura y escritura de archivos temporales, cambio de dispatcher, operaciones de CPU, solicitud de cancelación y eliminación de los archivos al terminar.
- **Representación didáctica:** seis intensidades entre 0 y 255 almacenadas como texto. No se decodifica un JPEG ni un PNG. El filtro aumenta el brillo en 20 unidades, con máximo 255.
- **Simulación:** una confirmación de cierre mediante delay y un error IOException provocado de forma controlada.
- **Coordinación:** la consola usa su contexto original de runBlocking. En Android, el papel de coordinación y presentación de UI corresponde normalmente a Main; esta demo no instala ni imita Dispatchers.Main.

<br/><br/>

### Código completo

```kotlin
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun registrar(mensaje: String) {
    println("$mensaje | hilo=${Thread.currentThread().name}")
}

class ProcesadorImagen(private val entrada: Path, private val salida: Path) {
    suspend fun procesar(
        simularError: Boolean = false,
        calculoIniciado: CompletableDeferred<Unit>? = null
    ) {
        try {
            registrar("COORDINACION: inicia")
            val pixeles = withContext(Dispatchers.IO) {
                registrar("IO: lee intensidades del archivo")
                if (simularError) throw IOException("Fallo de lectura simulado")
                Files.readString(entrada).trim().split(",").map { it.toInt() }
            }
            val procesados = withContext(Dispatchers.Default) {
                registrar("CPU: aplica brillo")
                val resultado = IntArray(pixeles.size)
                val limite = System.nanoTime() + 300_000_000L
                calculoIniciado?.complete(Unit)
                // Repetimos el filtro brevemente para poder observar su cancelacion.
                do {
                    ensureActive()
                    for (i in pixeles.indices) {
                        resultado[i] = (pixeles[i] + 20).coerceAtMost(255)
                    }
                } while (System.nanoTime() < limite)
                resultado
            }
            withContext(Dispatchers.IO) {
                registrar("IO: guarda el resultado")
                Files.writeString(salida, procesados.joinToString(","))
            }
            registrar("COORDINACION: resultado ${procesados.toList()}")
        } catch (e: CancellationException) {
            registrar("COORDINACION: procesamiento cancelado")
            throw e
        } catch (e: IOException) {
            registrar("COORDINACION: error de entrada/salida: ${e.message}")
        } finally {
            registrar("FINALLY: libera referencias temporales simuladas")
            withContext(NonCancellable) {
                delay(30) // Simula una confirmacion de cierre que necesita suspenderse.
                registrar("FINALLY: confirmacion de cierre terminada")
            }
        }
    }
}

fun main() = runBlocking {
    val carpeta = withContext(Dispatchers.IO) { Files.createTempDirectory("poc-imagen-") }
    val entrada = carpeta.resolve("pixeles.txt")
    val salida = carpeta.resolve("resultado.txt")
    try {
        withContext(Dispatchers.IO) {
            Files.writeString(entrada, "0,40,100,180,240,255")
        }
        val procesador = ProcesadorImagen(entrada, salida)

        println("--- Caso 1: finalizacion normal ---")
        procesador.procesar()
        withContext(Dispatchers.IO) {
            println("ARCHIVO: ${Files.readString(salida)}")
            Files.delete(salida)
        }

        println("--- Caso 2: cancelacion durante el calculo ---")
        val calculoIniciado = CompletableDeferred<Unit>()
        val tarea = launch { procesador.procesar(calculoIniciado = calculoIniciado) }
        calculoIniciado.await()
        tarea.cancelAndJoin()
        val guardado = withContext(Dispatchers.IO) { Files.exists(salida) }
        println("CONTROL: existe resultado del caso cancelado=$guardado")

        println("--- Caso 3: error de entrada/salida ---")
        procesador.procesar(simularError = true)
    } finally {
        withContext(Dispatchers.IO) {
            Files.deleteIfExists(entrada)
            Files.deleteIfExists(salida)
            Files.deleteIfExists(carpeta)
        }
        println("CONTROL: archivos temporales eliminados")
    }
}
```

<br/><br/>

### Compilar y ejecutar en CMD

```bat
kotlinc Demo06_ProcesadorImagen.kt -cp "%COR%" -include-runtime -d demo06.jar
java -cp "demo06.jar;%COR%" Demo06_ProcesadorImagenKt
```

<br/><br/>


### Salida observada de referencia

```text
--- Caso 1: finalizacion normal ---
COORDINACION: inicia | hilo=main
IO: lee intensidades del archivo | hilo=DefaultDispatcher-worker-1
CPU: aplica brillo | hilo=DefaultDispatcher-worker-1
IO: guarda el resultado | hilo=DefaultDispatcher-worker-1
COORDINACION: resultado [20, 60, 120, 200, 255, 255] | hilo=main
FINALLY: libera referencias temporales simuladas | hilo=main
FINALLY: confirmacion de cierre terminada | hilo=main
ARCHIVO: 20,60,120,200,255,255
--- Caso 2: cancelacion durante el calculo ---
COORDINACION: inicia | hilo=main
IO: lee intensidades del archivo | hilo=DefaultDispatcher-worker-1
CPU: aplica brillo | hilo=DefaultDispatcher-worker-1
COORDINACION: procesamiento cancelado | hilo=main
FINALLY: libera referencias temporales simuladas | hilo=main
FINALLY: confirmacion de cierre terminada | hilo=main
CONTROL: existe resultado del caso cancelado=false
--- Caso 3: error de entrada/salida ---
COORDINACION: inicia | hilo=main
IO: lee intensidades del archivo | hilo=DefaultDispatcher-worker-1
COORDINACION: error de entrada/salida: Fallo de lectura simulado | hilo=main
FINALLY: libera referencias temporales simuladas | hilo=main
FINALLY: confirmacion de cierre terminada | hilo=main
CONTROL: archivos temporales eliminados
```

Los nombres y números de los hilos de trabajo pueden variar. La salida debe conservar las relaciones entre etapas que se explican a continuación.

<br/><br/>

### Explicación paso a paso

**Caso 1: finalización normal**

1. La entrada contiene `0,40,100,180,240,255`.
2. IO lee esas intensidades del archivo.
3. Default calcula el filtro. Se repite brevemente para hacer observable la fase de CPU; un procesador real no necesita repetir el mismo filtro para alargarlo.
4. El resultado es `[20, 60, 120, 200, 255, 255]`.
5. IO escribe el resultado y, al volver, la coordinadora lo presenta.
6. Se ejecuta finally y termina la confirmación de cierre.
7. El archivo de resultado se elimina antes del caso siguiente para poder comprobar si la tarea cancelada llega a crear uno nuevo.

<br/><br/>

**Caso 2: cancelación durante el cálculo**

1. La señal auxiliar informa que comenzó la fase de CPU.
2. La coordinadora cancela la tarea.
3. EnsureActive comprueba la cancelación dentro del bucle y evita continuar con el trabajo pendiente.
4. La CancellationException se registra y se relanza.
5. Se ejecuta la limpieza, incluida la confirmación suspendible protegida.
6. Se comprueba que no se guardó un resultado de este caso. CancelAndJoin ya esperó la terminación.

<br/><br/>


**Caso 3: error de lectura**

1. IO provoca una IOException controlada.
2. El error de entrada/salida se maneja mediante su catch específico.
3. No se ejecutan el filtro ni el guardado.
4. Finally también se ejecuta en este camino.

Al terminar, el finally de main elimina la entrada, cualquier salida pendiente y la carpeta temporal.

**Observa:** la cancelación se trata por separado de IOException. No se publica un resultado exitoso después de detectar la cancelación. La lectura bloqueante de archivos no se vuelve interrumpible por seleccionar IO; el punto de cooperación explícito del cálculo está en ensureActive.

<br/><br/>

## Relación con las hojas de Android

El hilo llamado main en estas aplicaciones de consola no es una instalación de Dispatchers.Main. En Android, Main permite atender la interfaz; aquí los mensajes permiten observar la coordinación sin incorporar Android ni Compose.

Un trabajo largo ejecutado directamente en el hilo de UI puede retrasar la atención a entradas y el dibujo. Como referencia de las diapositivas, a 60 Hz un fotograma dispone de unos 16.7 ms; el umbral de ANR depende del tipo de espera. Cinco segundos es una referencia habitual para la falta de respuesta a eventos de entrada, no una regla universal para toda operación Android. No se reproducen ANR en estas PoC de consola.

Para explicar las diapositivas con precisión, basta recordar:

- Elegir el dispatcher por el trabajo: UI, entrada/salida bloqueante o CPU.
- WithContext ejecuta un bloque con el contexto indicado y espera su resultado.
- Cancelar solicita el cierre; el código coopera al comprobar el estado o utilizar operaciones cancelables.
- Finally contiene el cierre que corresponde tanto al éxito como al error o la cancelación.
- NonCancellable protege el tramo de limpieza que necesita suspenderse, sin reactivar el trabajo cancelado.

<br/><br/>

## Referencias oficiales utilizadas

- [Contexto y dispatchers](https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html): herencia, hilos y Unconfined.
- [Dispatchers.IO](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-i-o.html): trabajo bloqueante y relación con Default.
- [Dispatchers.Main](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-main.html): integración con plataformas de UI.
- [withContext](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-context.html): ejecución del bloque y retorno de su resultado.
- [Cancelación de corrutinas](https://kotlinlang.org/docs/coroutines-cancellation.html): Job y cooperación.
- [ensureActive](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html): comprobación sin suspensión.
- [ANR en Android](https://developer.android.com/games/optimize/vitals/anr): contexto de la falta de respuesta a entradas.

<br/><br/>

## Glosario

| Término | Definición breve |
|---|---|
| Hilo | Unidad del sistema operativo que ejecuta instrucciones. |
| Hilo principal o UI thread | Hilo que atiende la interfaz en Android. En consola, el nombre main no instala la infraestructura de Android. |
| Corrutina | Tarea que puede suspenderse y reanudarse sobre hilos. |
| Suspensión | Pausa del avance de la corrutina sin retener un hilo exclusivamente para esa espera. |
| Bloqueo | Operación que ocupa el hilo y le impide atender otras tareas durante ese intervalo. |
| Concurrencia | Avance de varias tareas durante un periodo común, incluso alternándose en un hilo. |
| Paralelismo | Ejecución simultánea de trabajo en varios recursos de procesamiento. |
| Dispatcher | Elemento del contexto que dirige dónde puede ejecutarse una corrutina. |
| Dispatchers.Main | Dispatcher integrado con el hilo de UI de la plataforma. |
| Dispatchers.IO | Dispatcher para operaciones bloqueantes de entrada/salida. |
| Dispatchers.Default | Dispatcher para trabajo de CPU. |
| Dispatchers.Unconfined | Dispatcher que empieza en el hilo invocador y no fija un hilo para las continuaciones posteriores. |
| Contexto | Conjunto de elementos asociados a una corrutina, como Job y dispatcher. |
| Scope o ámbito | Organización de corrutinas con un contexto y relaciones de vida. |
| Job | Objeto que representa el estado, la finalización y las relaciones de una tarea. |
| Corrutina hija | Tarea creada dentro de la jerarquía de otra; la cancelación del padre se propaga a ella. |
| runBlocking | Puente bloqueante para ejecutar corrutinas desde código como main en estas demos. |
| coroutineScope | Función suspendible que crea un ámbito para el bloque y espera sus hijas. |
| withContext | Ejecuta un bloque con el contexto indicado, espera su cierre y devuelve su resultado. |
| delay | Espera suspendible que coopera con la cancelación. |
| Thread.sleep | Espera que bloquea el hilo que la ejecuta. |
| Cancelación cooperativa | Forma de detener trabajo en la que el código responde a la solicitud de cancelación. |
| cancel | Solicita cancelar un Job y sus hijas. |
| join | Suspende al llamador hasta que el Job termina. |
| cancelAndJoin | Solicita cancelar y después espera la finalización. |
| isActive | Consulta si la tarea sigue activa; por sí sola no lanza una excepción. |
| isCancelled | Indica que el Job fue cancelado; puede quedar cierre pendiente. |
| isCompleted | Indica que el Job y sus hijas terminaron. |
| ensureActive | Lanza CancellationException cuando el contexto ya no está activo; no suspende. |
| yield | Comprueba la cancelación y permite que otras corrutinas listas tengan oportunidad de ejecutarse. |
| CancellationException | Señal de cancelación que debe conservar su significado al capturarla. |
| IOException | Excepción de entrada/salida; se trata de forma separada de la cancelación. |
| finally | Bloque de cierre ejecutado al salir del try, incluido el desenrollado por cancelación. |
| NonCancellable | Contexto utilizado aquí para permitir la parte suspendible del cierre pese a la cancelación. |
| CompletableDeferred<Unit> | Señal auxiliar de finalización usada para sincronizar el inicio de una fase de la demo. |
| CPU | Procesador; en estas PoC realiza cálculos de números e intensidades. |
| I/O | Entrada/salida; en el caso integrado, leer y escribir archivos. |
| Píxel | Elemento de una imagen; aquí se representa mediante una intensidad de gris entre 0 y 255. |
| ANR | Application Not Responding: condición de Android asociada a una falta de respuesta dentro del tiempo esperado por el sistema. |

<br/><br/>

## Temas aprendidos

Al terminar este material, el participante podrá:

1. Distinguir una corrutina del hilo sobre el que ejecuta su código.
2. Explicar la diferencia observable entre delay y Thread.sleep.
3. Identificar cuándo una corrutina hereda el contexto y por qué launch no garantiza trabajar fuera del hilo principal.
4. Elegir Main, IO o Default según el trabajo y explicar la particularidad de Unconfined.
5. Utilizar withContext para ejecutar una etapa con otro contexto y esperar su resultado.
6. Solicitar cancelación y esperar a que terminen la tarea, sus hijas y su limpieza.
7. Reconocer por qué un bucle sin cooperación puede continuar después de cancel().
8. Utilizar isActive, ensureActive y yield y describir sus diferencias.
9. Permitir que CancellationException se propague y relanzarla cuando se captura para registrar la cancelación.
10. Ejecutar limpieza en finally y proteger únicamente su parte suspendible cuando sea necesario.
11. Seguir los caminos de éxito, cancelación y error de entrada/salida en un proceso pequeño.
12. Relacionar estas decisiones con la protección del hilo de UI, manteniendo las demos independientes de Android.
