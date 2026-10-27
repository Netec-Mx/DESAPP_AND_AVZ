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
