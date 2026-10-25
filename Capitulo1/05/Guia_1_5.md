# 1.5 — Corrutinas: suspend, launch, async y await

<br/><br/>

## Objetivo y recorrido

Observar cómo se suspende y continúa una corrutina, cómo iniciar una tarea y cómo recuperar un resultado. Los ejemplos son de consola y siguen los conceptos y el dashboard de las hojas compartidas. La PPT se conserva.

| Bloque | Contenido de las hojas que demuestra |
| --- | --- |
| 1 | Función `suspend`: llamada, suspensión y retorno |
| 2 | `launch`, `Job` y espera mediante `join()` |
| 3 | Bloquear el hilo con `Thread.sleep` y suspender la corrutina con `delay` |
| 4 | `async`, `Deferred<T>` y obtención del resultado con `await()` |
| 5 | Dashboard secuencial y concurrente, `coroutineScope` y medición de tiempos |

El archivo está completo y comentado. Cada bloque se puede ejecutar por separado.

<br/><br/>

## Compilar y ejecutar desde CMD

Descarga `Demo_1_5.kt` y abre CMD en la carpeta donde lo guardaste. Utiliza el JAR de corrutinas que ya tienes. La siguiente ruta corresponde a la instalación de Kotlin que habías mostrado; ajústala si tu carpeta es otra.

```bat
set "COR13=C:\work\AndroidBasico\software\kotlin-compiler-2.4.20\kotlinc\lib\kotlinx-coroutines-core-jvm.jar"
dir "%COR13%"
```

`dir` debe mostrar ese archivo. Después compila:

```bat
kotlinc Demo_1_5.kt -cp "%COR13%" -include-runtime -d demo15.jar
```

Si la compilación terminó sin errores, ejecuta los cinco bloques:

```bat
java -cp "demo15.jar;%COR13%" tema15.Demo_1_5Kt
```

Para ejecutar solo un bloque, agrega su número. Por ejemplo, el bloque 2:

```bat
java -cp "demo15.jar;%COR13%" tema15.Demo_1_5Kt 2
```

Usa la misma ventana de CMD. `-include-runtime` incluye Kotlin; el classpath también debe incluir el JAR de corrutinas al ejecutar. `tema15.Demo_1_5Kt` es la clase de entrada generada para este archivo y su paquete.

Si copias desde este documento, utiliza únicamente el listado de **Código completo**. Los fragmentos de las explicaciones muestran partes del programa.

<br/><br/>

## Cómo leer los mensajes

Cada mensaje comienza con el nombre del hilo, por ejemplo `[main]`. Es el hilo de este programa de consola.

Dentro de `main`, `runBlocking` crea la corrutina desde la que llamamos a los ejemplos. No devuelve el control a su llamador hasta que termina el trabajo de su bloque. Durante esa espera, su mecanismo interno permite que ese hilo ejecute las corrutinas del bloque.

`coroutineScope` agrupa las tareas hijas de cada ejemplo y espera a que terminen su bloque y sus hijos antes de retornar. No crea un hilo por cada tarea.

**Dos referencias que conviene distinguir:** el nombre `[main]` identifica al hilo; los mensajes «hija», «precio», «envío» o «perfil» identifican el trabajo que está ejecutándose sobre él.

<br/><br/>

## 1. suspend: poder suspenderse no significa crear otra corrutina

La primera función, `mensajeInmediato`, está declarada con `suspend`, pero devuelve un texto inmediatamente. Muestra que el modificador no obliga a suspender en cada llamada.

Después se llama a `obtenerMensaje`:

```kotlin
val mensaje = obtenerMensaje()
```

El recorrido es:

1. La corrutina imprime el mensaje anterior a la llamada.
2. Entra en `obtenerMensaje` y llega a `delay(250)`.
3. La corrutina se suspende durante la espera y puede continuar después.
4. La función devuelve el texto.
5. La misma secuencia continúa con la impresión del resultado.

```text
=== DEMO 1: Llamar a una funcion suspend ===
[main] Mensaje inmediato: Disponible sin espera
[main] 1. Antes de llamar a obtenerMensaje
[main] 2. Dentro de obtenerMensaje, antes de delay
[main] 3. Dentro de obtenerMensaje, despues de delay
[main] 4. Resultado: Datos disponibles
```

**Lo esencial:** esta llamada no crea una corrutina adicional. La función se ejecuta dentro de la corrutina que la llamó. `suspend` permite llamar a operaciones de suspensión; aquí el punto de suspensión es `delay`.

En este primer bloque no hemos creado otra tarea para aprovechar la espera. Los bloques siguientes muestran cómo otra corrutina puede usar ese hilo durante una suspensión.

<br/><br/>

## 2. launch: iniciar una tarea y conservar su Job

El código conserva el objeto devuelto por `launch`:

```kotlin
val tarea: Job = launch {
    // Trabajo de la corrutina hija.
}
tarea.join()
```

En esta configuración de consola se observa lo siguiente:

1. La corrutina del ejemplo imprime «Antes de launch».
2. `launch` crea la tarea hija y devuelve su `Job`.
3. La corrutina del ejemplo imprime el mensaje 2 y llama a `join()`.
4. Como la hija no ha terminado, quien espera se suspende. El mismo hilo ejecuta a la hija.
5. La hija alcanza su `delay`, espera, continúa y termina.
6. Quien estaba en `join()` puede continuar e imprimir los mensajes finales.

```text
=== DEMO 2: launch devuelve Job; join espera el final ===
[main] 1. Antes de launch
[main] 2. launch devolvio el Job; ahora llamamos a join
[main] 3. La corrutina hija comienza
[main] 4. La corrutina hija termina
[main] 5. join termino; el hijo ya finalizo
[main] 6. Job completado: true
```

**Lo esencial:** `join()` espera la finalización; no recupera un dato calculado por la tarea. `isCompleted` permite comprobar que el `Job` ha finalizado.

El orden observado corresponde al scope heredado de nuestro `runBlocking`. No es una regla universal que cualquier `launch` empiece a ejecutar su cuerpo después de la línea siguiente.

<br/><br/>

## 3. Bloquear el hilo o suspender la corrutina

El bloque compara las dos esperas que aparecen en la PPT. En cada caso se crea un hijo que solo necesita imprimir un mensaje.

| Caso | Espera del padre | Lo que sucede con el hijo en esta configuración |
| --- | --- | --- |
| A | `Thread.sleep(300)` | El hilo permanece ocupado esperando y no puede ejecutar al hijo. El hijo imprime después. |
| B | `delay(300)` | El padre se suspende y el mismo hilo puede ejecutar al hijo durante esa espera. |

```text
=== DEMO 3: Thread.sleep bloquea; delay permite compartir el hilo ===
[main] A1. Crear un hijo y bloquear con Thread.sleep
[main] A2. Termino Thread.sleep; el hijo aun no ejecuto su mensaje
[main] A3. El hijo consigue ejecutar su mensaje
[main] B1. Crear otro hijo y esperar con delay
[main] B2. El hijo ejecuta su mensaje durante el delay del padre
[main] B3. Termino delay
```

**Lo esencial:** en A, el mensaje del hijo aparece después de terminar el bloqueo. En B, aparece antes del mensaje que anuncia el final de `delay`.

El nombre del hilo se mantiene. El ejemplo muestra concurrencia mediante alternancia de ejecución sobre un hilo, no ejecución simultánea de ambas tareas en hilos diferentes.

<br/><br/>

## 4. async y await: iniciar una tarea para obtener un resultado

En este bloque dos operaciones independientes producen números: precio `80` y envío `15`. Las esperas son de 300 y 150 milisegundos.

```kotlin
val precio: Deferred<Int> = async { /* produce 80 */ }
val envio: Deferred<Int> = async { /* produce 15 */ }

val precioObtenido = precio.await()
val envioObtenido = envio.await()
```

Los cuerpos completos están en el archivo. El orden importante es crear **los dos `async` antes del primer `await`**.

`async` devuelve un `Deferred<Int>`, que representa una tarea con un resultado entero. `await()` espera si hace falta y devuelve ese entero. Mientras esperamos el precio, la tarea de envío también puede avanzar.

```text
=== DEMO 4: async produce un resultado; await lo obtiene ===
[main] 1. Crear las dos tareas antes de esperar sus resultados
[main] 2. Ambos Deferred creados; llamar a precio.await()
[main] Precio: inicio
[main] Envio: inicio
[main] Envio: listo
[main] Precio: listo
[main] 3. Valores obtenidos: precio=80, envio=15
[main] 4. Total: 95
```

**Lo esencial:** el envío puede terminar mientras se espera el precio. Cuando después llamamos a `envio.await()`, puede estar ya disponible su resultado. En ese caso no necesita suspender para devolverlo.

Las tareas ya fueron creadas: escribir los dos `await()` uno después del otro no convierte automáticamente el trabajo en secuencial.

<br/><br/>

## Elegir entre launch y async

| Necesidad | Constructor | Objeto recibido | Espera |
| --- | --- | --- | --- |
| Realizar una acción | `launch` | `Job` | `join()` espera su finalización |
| Producir un valor que vamos a utilizar | `async` | `Deferred<T>` | `await()` espera y obtiene el valor |

Si el trabajo no terminó, `join()` y `await()` pueden suspender a la corrutina que espera, dejando disponible el hilo para otra tarea.

<br/><br/>

## 5. Dashboard: el mismo trabajo, dos órdenes de consulta

El caso mantiene los modelos y operaciones de las hojas: `UserProfile`, `UserPost`, `UserDashboard`, `UserApiClient`, `LocalDatabase` y `UserRepository`.

| Operación | Espera simulada |
| --- | --- |
| Consultar el perfil | 1500 ms |
| Consultar las publicaciones | 1000 ms |
| Registrar la consulta | 500 ms |

Las tres usan `delay`; los mensajes permiten observar cuándo empiezan y terminan. Son operaciones simuladas.

<br/><br/>

### Versión secuencial

Se inicia el registro mediante `launch`. Después se consulta el perfil y, cuando ese perfil está disponible, comienza la consulta de publicaciones:

```kotlin
launch { database.saveQueryLog(userId) }
val profile = apiClient.fetchUserProfile(userId)
val posts = apiClient.fetchUserPosts(userId)
UserDashboard(profile, posts)
```

«Secuencial» describe aquí las dos consultas. El registro es otra tarea y puede avanzar durante las suspensiones. Por eso sus 500 ms no se suman a las dos consultas: estas tardan aproximadamente **1500 + 1000 = 2500 ms**.

<br/><br/>

### Versión concurrente

También se inicia el registro. Las dos consultas se preparan con `async` antes de esperar:

```kotlin
val profileDeferred = async { apiClient.fetchUserProfile(userId) }
val postsDeferred = async { apiClient.fetchUserPosts(userId) }

val profile = profileDeferred.await()
val posts = postsDeferred.await()
UserDashboard(profile, posts)
```

Las tres esperas se solapan. El tiempo aproximado queda determinado por la más larga: **1500 ms**, más el trabajo de ejecución del programa.

<br/><br/>

### Salida observada

```text
=== DEMO 5: Dashboard: consultas secuenciales y concurrentes ===
[main] VERSION SECUENCIAL: perfil y despues publicaciones
[main] Perfil: inicio de consulta
[main] Registro: inicio
[main] Registro: completado para usr_99
[main] Perfil: datos disponibles
[main] Publicaciones: inicio de consulta
[main] Publicaciones: datos disponibles
[main] Dashboard secuencial: Sofia, 1 publicacion
[main] VERSION CONCURRENTE: iniciar ambas consultas antes de await
[main] Registro: inicio
[main] Perfil: inicio de consulta
[main] Publicaciones: inicio de consulta
[main] Registro: completado para usr_99
[main] Publicaciones: datos disponibles
[main] Perfil: datos disponibles
[main] Dashboard concurrente: Sofia, 1 publicacion
Tiempo secuencial observado: 2536 ms
Tiempo concurrente observado: 1509 ms
```

Los milisegundos son mediciones de una ejecución, no cifras exactas garantizadas. También puede variar el intercalado de mensajes de tareas independientes.

<br/><br/>

**Lo esencial para explicar el ejemplo:**

- En la versión secuencial, `Publicaciones: inicio` aparece después de `Perfil: datos disponibles`.
- En la concurrente, ambas consultas pueden haber empezado antes de que se complete alguna de ellas.
- Ambas versiones realizan el registro y entregan perfil y publicaciones.
- `coroutineScope` retorna cuando terminaron su bloque y sus hijos, incluido el registro.
- El beneficio de esta simulación procede de solapar las esperas; no de crear un hilo por consulta.

<br/><br/>

## Recomendaciones de las últimas hojas

Las hojas cierran con estas pautas. Aquí se conservan como recordatorio, sin añadir ejemplos sobre otros mecanismos:

- Cada tarea debe tener un responsable y una duración controlada. `coroutineScope` agrupa las subtareas de este programa.
- Las tareas dependientes requieren respetar su orden; las independientes pueden iniciarse antes de esperar sus resultados.
- La cancelación debe respetarse. Si se captura `CancellationException`, debe volver a lanzarse.
- El dispatcher se elige según la operación; declarar `suspend` no realiza esa elección ni cambia automáticamente de hilo.
- Debe decidirse cómo afectan los fallos a las tareas relacionadas. La PPT menciona la supervisión cuando se necesita independencia entre hijos.

<br/><br/>


## Referencias de apoyo

- [Corrutinas: conceptos básicos — Kotlin](https://kotlinlang.org/docs/coroutines-basics.html).
- [Composición de funciones suspendidas — Kotlin](https://kotlinlang.org/docs/composing-suspending-functions.html).


<br/><br/>

## Glosario del submódulo 1.5

| Término | Significado en estas hojas y ejemplos |
| --- | --- |
| Corrutina | Tarea cuyo código puede suspenderse y continuar después. No equivale a un hilo. |
| Hilo | Recurso de ejecución que realiza instrucciones. Varias corrutinas pueden alternar su trabajo sobre él. |
| Suspensión | Pausa de la ejecución de una corrutina que permite usar el hilo para otro trabajo. |
| Reanudación | Continuación de una corrutina después de una suspensión. |
| Bloqueo | Espera que ocupa el hilo e impide que ejecute otro trabajo durante ese tiempo. |
| `suspend` | Modificador que permite que una función llame a operaciones de suspensión. No crea por sí solo otra corrutina ni obliga a suspender en cada llamada. |
| `delay` | Función que suspende la corrutina durante una espera sin bloquear el hilo. |
| `Thread.sleep` | Operación que bloquea el hilo durante la espera indicada. |
| Scope | Ámbito responsable de un grupo de corrutinas y de su duración. |
| `coroutineScope` | Función suspendida que agrupa subtareas y espera a que terminen su bloque y sus hijos antes de retornar. |
| `runBlocking` | Entrada a código de corrutinas desde el programa de consola; no retorna a su llamador hasta completar el trabajo de su bloque. |
| `launch` | Constructor que inicia una corrutina y devuelve un `Job`; se usa para realizar una acción. |
| `Job` | Referencia que representa una tarea y permite consultar su estado, solicitar cancelación o esperar su finalización. |
| `join` | Espera la finalización de un `Job`; no recupera un resultado de la tarea. |
| `async` | Constructor que inicia una corrutina para producir un resultado y devuelve un `Deferred<T>`. |
| `Deferred<T>` | Referencia a una tarea que producirá un resultado de tipo `T`. |
| `await` | Obtiene el resultado de un `Deferred<T>`; suspende a quien espera si ese resultado todavía no está disponible. |
| Ejecución secuencial | Una operación comienza después de que termine la anterior. |
| Concurrencia | Varias tareas progresan en periodos que se solapan, por ejemplo alternando sobre un hilo. |
| Paralelismo | Varias tareas ejecutan instrucciones simultáneamente sobre distintos recursos de ejecución. La concurrencia no lo exige. |
| Concurrencia estructurada | Organización de tareas relacionadas como padre e hijos, con duración, fallos y cancelación vinculados. |
| Dispatcher | Determina qué hilo o conjunto de hilos puede utilizar una corrutina. |
| Cancelación | Solicitud de terminar una tarea. En las relaciones estructuradas, la cancelación del padre se propaga a sus hijos. |
| `CancellationException` | Excepción utilizada para comunicar cancelación; no debe ocultarse si se captura. |
| Supervisión | Organización de fallos que permite que hijos puedan fallar de forma independiente, manteniendo el manejo de sus errores. |
| `measureTime` | Función de Kotlin que mide la duración de un bloque. En la demo permite comparar las dos versiones del dashboard. |

<br/><br/>

## Temas aprendidos

1. Distinguir la corrutina del hilo que ejecuta su código.
2. Reconocer lo que permite `suspend` y lo que no inicia automáticamente.
3. Seguir una llamada suspendida desde la entrada hasta su retorno.
4. Observar la diferencia entre bloquear el hilo y suspender la corrutina.
5. Iniciar una tarea con `launch` y esperar su finalización mediante `join`.
6. Iniciar una tarea que produce un valor con `async` y obtenerlo mediante `await`.
7. Crear las tareas independientes antes de esperar sus resultados para permitir concurrencia.
8. Agrupar subtareas con `coroutineScope` y reconocer que espera a sus hijos.
9. Comparar las consultas secuenciales y concurrentes del dashboard con `measureTime`.
10. Reconocer las recomendaciones de las hojas sobre responsabilidad, cancelación, elección de hilo y tratamiento de fallos.

