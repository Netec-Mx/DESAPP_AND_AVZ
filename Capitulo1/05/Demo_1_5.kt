package tema15

// Tema 1.5: suspend, launch, async y await.
// Codigo completo de consola. Los cinco bloques se ejecutan sin modificarlo.
// Requiere kotlinx-coroutines-core-jvm en el classpath al compilar y ejecutar.
// Los comandos de CMD y la definicion de COR13 estan en Guia_1_5.md.

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.measureTime

// Mostrar el hilo permite observar que una corrutina no equivale a otro hilo.
private fun anotar(mensaje: String) {
    println("[${Thread.currentThread().name}] $mensaje")
}

private fun cabecera(numero: Int, titulo: String) {
    println("\n=== DEMO $numero: $titulo ===")
}

private suspend fun mensajeInmediato(): String {
    // Puede llamarse desde codigo suspendido, pero esta llamada no suspende.
    // El modificador suspend PERMITE suspension; no obliga a que suceda.
    return "Disponible sin espera"
}

private suspend fun obtenerMensaje(): String {
    anotar("2. Dentro de obtenerMensaje, antes de delay")
    delay(250) // Suspende la corrutina que llamo a esta funcion.
    anotar("3. Dentro de obtenerMensaje, despues de delay")
    return "Datos disponibles"
}

private suspend fun demo1() {
    cabecera(1, "Llamar a una funcion suspend")
    anotar("Mensaje inmediato: ${mensajeInmediato()}")
    anotar("1. Antes de llamar a obtenerMensaje")
    val mensaje = obtenerMensaje()
    // Esta linea se alcanza cuando obtenerMensaje devuelve el resultado.
    anotar("4. Resultado: $mensaje")
    // Llamar a una funcion suspend no crea otra corrutina ni cambia de hilo.
    // Esta llamada secuencial ocurre dentro de la corrutina ya existente.
}

private suspend fun demo2(): Unit = coroutineScope {
    cabecera(2, "launch devuelve Job; join espera el final")
    anotar("1. Antes de launch")
    val tarea: Job = launch {
        anotar("3. La corrutina hija comienza")
        delay(250)
        anotar("4. La corrutina hija termina")
    }
    // En este programa usamos el hilo heredado de runBlocking. El cuerpo
    // del hijo empezara cuando la corrutina actual permita usar ese hilo.
    anotar("2. launch devolvio el Job; ahora llamamos a join")
    tarea.join() // Suspende al que espera si el hijo todavia no termino.
    anotar("5. join termino; el hijo ya finalizo")
    anotar("6. Job completado: ${tarea.isCompleted}")
    // join no recupera un dato calculado por el hijo: su resultado es Unit.
}

private suspend fun demo3(): Unit = coroutineScope {
    cabecera(3, "Thread.sleep bloquea; delay permite compartir el hilo")

    anotar("A1. Crear un hijo y bloquear con Thread.sleep")
    val hijoA = launch {
        anotar("A3. El hijo consigue ejecutar su mensaje")
    }
    Thread.sleep(300) // El hilo NO puede ejecutar al hijo durante esta espera.
    anotar("A2. Termino Thread.sleep; el hijo aun no ejecuto su mensaje")
    hijoA.join() // Aqui la corrutina actual cede el hilo mientras espera.

    anotar("B1. Crear otro hijo y esperar con delay")
    val hijoB = launch {
        anotar("B2. El hijo ejecuta su mensaje durante el delay del padre")
    }
    delay(300) // Suspende a quien llama, permitiendo ejecutar al hijo.
    anotar("B3. Termino delay")
    hijoB.join()
    // El nombre del hilo es el mismo en ambos casos. En B compartieron el hilo.
}

private suspend fun demo4(): Unit = coroutineScope {
    cabecera(4, "async produce un resultado; await lo obtiene")
    anotar("1. Crear las dos tareas antes de esperar sus resultados")
    val precio: Deferred<Int> = async {
        anotar("Precio: inicio")
        delay(300)
        anotar("Precio: listo")
        80 // Ultima expresion: resultado del async, de tipo Int.
    }
    val envio: Deferred<Int> = async {
        anotar("Envio: inicio")
        delay(150)
        anotar("Envio: listo")
        15
    }

    anotar("2. Ambos Deferred creados; llamar a precio.await()")
    val precioObtenido = precio.await()
    // Mientras esperabamos el precio, el envio tambien pudo avanzar.
    // Si envio ya termino, su await devuelve el valor sin suspender.
    val envioObtenido = envio.await()
    anotar("3. Valores obtenidos: precio=$precioObtenido, envio=$envioObtenido")
    anotar("4. Total: ${precioObtenido + envioObtenido}")
    // await obtiene un resultado. No inicia una nueva consulta aqui.
}

// Modelos del dashboard que aparece en la PPT.
private data class UserProfile(val id: String, val name: String, val email: String)
private data class UserPost(val id: String, val title: String, val content: String)
private data class UserDashboard(val profile: UserProfile, val posts: List<UserPost>)

private class UserApiClient {
    suspend fun fetchUserProfile(userId: String): UserProfile {
        anotar("Perfil: inicio de consulta")
        delay(1500)
        anotar("Perfil: datos disponibles")
        return UserProfile(userId, "Sofia", "sofia@example.com")
    }

    suspend fun fetchUserPosts(userId: String): List<UserPost> {
        anotar("Publicaciones: inicio de consulta")
        delay(1000)
        anotar("Publicaciones: datos disponibles")
        return listOf(UserPost("$userId-post-1", "Corrutinas", "Contenido de ejemplo"))
    }
}

private class LocalDatabase {
    suspend fun saveQueryLog(userId: String) {
        anotar("Registro: inicio")
        delay(500)
        anotar("Registro: completado para $userId")
        // Simulacion: no escribimos en una base de datos real.
    }
}

private class UserRepository(
    private val apiClient: UserApiClient,
    private val database: LocalDatabase
) {
    suspend fun getUserDashboardSequentially(userId: String): UserDashboard = coroutineScope {
        // El registro se inicia tambien en esta version para comparar
        // exactamente el mismo trabajo que en la version concurrente.
        launch { database.saveQueryLog(userId) }

        // Las dos consultas son SECUENCIALES: posts comienza al terminar profile.
        val profile = apiClient.fetchUserProfile(userId)
        val posts = apiClient.fetchUserPosts(userId)
        UserDashboard(profile, posts)
        // coroutineScope retorna tras completar su bloque y todos sus hijos.
    }

    suspend fun getUserDashboardConcurrently(userId: String): UserDashboard = coroutineScope {
        launch { database.saveQueryLog(userId) }

        // Crear AMBOS async antes del primer await permite solapar las esperas.
        val profileDeferred: Deferred<UserProfile> = async {
            apiClient.fetchUserProfile(userId)
        }
        val postsDeferred: Deferred<List<UserPost>> = async {
            apiClient.fetchUserPosts(userId)
        }

        // Esperar uno despues del otro no vuelve secuenciales las consultas:
        // ambos async ya se crearon y pueden avanzar durante las suspensiones.
        val profile = profileDeferred.await()
        val posts = postsDeferred.await()
        UserDashboard(profile, posts)
        // El scope tambien espera al registro, aunque no llamemos a su join.
    }
}

private suspend fun demo5() {
    cabecera(5, "Dashboard: consultas secuenciales y concurrentes")
    val repository = UserRepository(UserApiClient(), LocalDatabase())

    anotar("VERSION SECUENCIAL: perfil y despues publicaciones")
    val tiempoSecuencial = measureTime {
        val dashboard = repository.getUserDashboardSequentially("usr_99")
        anotar("Dashboard secuencial: ${dashboard.profile.name}, ${dashboard.posts.size} publicacion")
    }

    anotar("VERSION CONCURRENTE: iniciar ambas consultas antes de await")
    val tiempoConcurrente = measureTime {
        val dashboard = repository.getUserDashboardConcurrently("usr_99")
        anotar("Dashboard concurrente: ${dashboard.profile.name}, ${dashboard.posts.size} publicacion")
    }

    println("Tiempo secuencial observado: ${tiempoSecuencial.inWholeMilliseconds} ms")
    println("Tiempo concurrente observado: ${tiempoConcurrente.inWholeMilliseconds} ms")
    // Valores orientativos: unos 2500 ms y 1500 ms, mas sobrecarga de ejecucion.
    // El registro de 500 ms se solapa con las consultas en ambas versiones.
}

fun main(args: Array<String>) {
    // main es una funcion normal. runBlocking crea la corrutina desde la
    // que podemos llamar a las funciones suspend de esta consola.
    // runBlocking bloquea a su llamador hasta terminar, mientras su hilo puede
    // ejecutar las corrutinas del bloque mediante su procesamiento interno.
    runBlocking {
        val opcion = if (args.isEmpty()) "todas" else args.singleOrNull()
        when (opcion) {
            "todas" -> {
                demo1()
                demo2()
                demo3()
                demo4()
                demo5()
            }
            "1" -> demo1()
            "2" -> demo2()
            "3" -> demo3()
            "4" -> demo4()
            "5" -> demo5()
            else -> println("Opcion no valida. Usa un numero del 1 al 5 o ejecuta sin argumentos.")
        }
    }
}
