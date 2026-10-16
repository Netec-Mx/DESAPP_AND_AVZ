<p align="center">
  <img src="https://raw.githubusercontent.com/Netec-Mx/260928-DESAPP-AND-AVZ/main/assets/LogoNetec.png" alt="NETEC" width="180" />
</p>

# Desarrollo de aplicaciones móviles para Sistemas Operativos Android Nivel Avanzado

Este curso avanzado está orientado a desarrolladores Android que ya dominan los fundamentos y desean adquirir las habilidades necesarias para construir aplicaciones modernas, escalables y de alto nivel utilizando exclusivamente Kotlin.

A lo largo de las sesiones, el participante profundizará en conceptos avanzados del desarrollo Android actual, incluyendo corutinas, arquitecturas profesionales (MVVM y Clean Architecture), interfaces declarativas con Jetpack Compose, networking con APIs REST, manejo avanzado de persistencia local, geolocalización, notificaciones, así como el uso de herramientas de Inteligencia Artificial a través de JetBrains AI Assistant para acelerar y optimizar su flujo de trabajo profesional.

El curso culmina con un proyecto final integrador, donde el participante construirá una aplicación completa que combine UI moderna, consumo de servicios, arquitectura, persistencia y capacidades del dispositivo —siguiendo buenas prácticas y estándares actuales de la industria.

## Accesos rápidos

- [**Setup Guide del curso**](https://github.com/Netec-Mx/260928-DESAPP-AND-AVZ/blob/main/SETUP_GUIDE.md)
- [Laboratorios por capítulo](#lista-de-laboratorios)


<br/>
<br/>

## Lista de laboratorios

### Capítulo 1

- [Configuración del proyecto Android avanzado y desarrollo de concurrencia con Kotlin 2.3.10, Coroutines, StateFlow y SharedFlow sobre API 30–37](Capitulo01/README.md#configuración-del-proyecto-android-avanzado-y-desarrollo-de-concurrencia-con-kotlin-2310-coroutines-stateflow-y-sharedflow-sobre-api-3037)
  - Descripción: Desarrollar en Kotlin una aplicación Android que aplique funciones de orden superior, sealed classes, corrutinas con suspend, launch, async y await, StateFlow y SharedFlow, además de Dispatchers y cancelación para resolver tareas asíncronas y flujos de datos. La práctica se configurará con Android Studio, Kotlin DSL, Android Gradle Plugin, minSdk = 31, compileSdk = 37 y targetSdk = 37, utilizando Kotlin como versión estándar del curso y sin dependencias dinámicas. El laboratorio partirá de la plantilla Empty Views Activity y declarará las versiones de dependencias en libs.versions.toml.
  - Duración estimada: 216 min
  - [Ver capítulo completo](Capitulo01/README.md)

<br/>
<br/>

### Capítulo 2

- [Construcción de interfaz declarativa con Jetpack Compose BOM 2026.02.01, Material 3, estado, recomposición, navegación y LazyColumn](Capitulo02/README.md#construcción-de-interfaz-declarativa-con-jetpack-compose-bom-20260201-material-3-estado-recomposición-navegación-y-lazycolumn)
  - Descripción: Construir una interfaz Android declarativa con Jetpack Compose que integre Text, Button, Image, Column, Row, manejo de estado con remember y mutableStateOf, State Hoisting, recomposición, navegación con Compose Navigation, Material 3, temas, tipografías y listas dinámicas con LazyColumn y tarjetas. La práctica utilizará la plantilla Empty Activity, Compose BOM 2026.02.01, Activity Compose 1.13.0, Compose UI, Compose UI Graphics y Compose Tooling Preview, con minSdk = 30, compileSdk = 37 y targetSdk = 37. Con Kotlin 2.x se utilizará el mecanismo de Compose Compiler compatible con Kotlin 2.3.10.
  - Duración estimada: 288 min
  - [Ver capítulo completo](Capitulo02/README.md)


<br/>
<br/>

### Capítulo 3

- [Implementación de arquitectura MVVM + Clean Architecture con ViewModel, StateFlow, Repository, Use Cases e inyección de dependencias con Hilt](Capitulo03/README.md#implementación-de-arquitectura-mvvm-clean-architecture-con-viewmodel-stateflow-repository-use-cases-e-inyección-de-dependencias-con-hilt)
  - Descripción: Estructurar una aplicación Android aplicando MVVM, ViewModel con corutinas y StateFlow, patrón Repository, Use Cases, separación por capas de Clean Architecture, fundamentos de inyección de dependencias con Hilt y manejo de eventos, estados y errores de UI. El proyecto deberá mantener desacoplamiento entre presentación, dominio y datos y utilizar Kotlin DSL, minSdk = 30, compileSdk = 37 y targetSdk = 37, con las versiones del baseline tecnológico definidas explícitamente en libs.versions.toml. Se empleará Core KTX 1.19.0 y Lifecycle Runtime KTX 2.6.1 cuando correspondan al manejo de ciclo de vida y estado.
  - Duración estimada: 216 min
  - [Ver capítulo completo](Capitulo03/README.md)

<br/>
<br/>

### Capítulo 4

- [Integración de API REST con Retrofit 3.0.0 y OkHttp BOM 5.5.0: GET, POST, autenticación, serialización, interceptores y manejo de estados de red](Capitulo04/README.md#integración-de-api-rest-con-retrofit-300-y-okhttp-bom-550-get-post-autenticación-serialización-interceptores-y-manejo-de-estados-de-red)
  - Descripción: Implementar el consumo de una API REST mediante Retrofit 3.0.0 y OkHttp BOM 5.5.0, incorporando operaciones GET y POST, autenticación básica con Bearer o API Key, serialización JSON, interceptores, logs de red, timeouts, manejo de errores con corutinas y Flow, y estados Loading, Success y Error. Todas las versiones deberán declararse de forma explícita en libs.versions.toml, sin latest ni +, y el proyecto usará minSdk = 30, compileSdk = 37 y targetSdk = 37.
  - Duración estimada: 216 min
  - [Ver capítulo completo](Capitulo04/README.md)

<br/>
<br/>

### Capítulo 5

- [Persistencia avanzada con Room 2.8.4 y DataStore: entidades, DAO, migraciones, relaciones, caché y repositorio offline-first API + Room](Capitulo05/README.md#persistencia-avanzada-con-room-284-y-datastore-entidades-dao-migraciones-relaciones-caché-y-repositorio-offline-first-api-room)
  - Descripción: Construir una capa de persistencia avanzada con Room 2.8.4, entidades, DAO, migraciones, consultas, relaciones y paginación, complementada con DataStore Preferences o ProtoDataStore. Integrar una estrategia offline-first con caché y red mediante un repositorio híbrido API + Room. Las dependencias deberán quedar fijadas en libs.versions.toml y ser compatibles con Kotlin 2.3.10, AGP 9.3.2, el SDK del curso y el mecanismo de procesamiento de código requerido.
  - Duración estimada: 216 min
  - [Ver capítulo completo](Capitulo05/README.md)

<br/>
<br/>

### Capítulo 6

- [Integración de geolocalización con Google Play Services Location 21.4.0, permisos sensibles, mapas y sensores en Android API 30–37](Capitulo06/README.md#integración-de-geolocalización-con-google-play-services-location-2140-permisos-sensibles-mapas-y-sensores-en-android-api-3037)
  - Descripción: Desarrollar una aplicación que gestione permisos sensibles mediante flujos modernos, obtenga la ubicación con FusedLocationProvider utilizando Google Play Services Location 21.4.0, integre mapas y utilice sensores básicos del dispositivo. La práctica deberá validarse en AVD con API 30, 35, 36 y 37 cuando corresponda y mantener minSdk = 30, compileSdk = 37 y targetSdk = 37, con dependencias explícitas y sin versiones dinámicas.
  - Duración estimada: 144 min
  - [Ver capítulo completo](Capitulo06/README.md)

<br/>
<br/>

### Capítulo 7

- [Proyecto final integrador Android con Jetpack Compose, MVVM, Retrofit 3.0.0, OkHttp 5.5.0, Room 2.8.4, Flows, geolocalización y JetBrains AI Assistant](Capitulo07/README.md#proyecto-final-integrador-android-con-jetpack-compose-mvvm-retrofit-300-okhttp-550-room-284-flows-geolocalización-y-jetbrains-ai-assistant)
  - Descripción: Construir una aplicación Android completa que integre Compose UI, arquitectura MVVM con repositorios, consumo de API REST con Retrofit 3.0.0 y OkHttp BOM 5.5.0, persistencia con Room 2.8.4 o DataStore, manejo de estado con Flows y geolocalización con Google Play Services Location 21.4.0. De manera opcional, utilizar JetBrains AI Assistant para optimizar código, generar funciones, sugerir arquitectura y mejorar documentación. El proyecto se configurará con Kotlin DSL, Android Gradle Plugin 9.3.2, minSdk = 30, compileSdk = 37 y targetSdk = 37, y todas las versiones se especificarán en libs.versions.toml sin valores dinámicos. La validación incluirá pruebas con JUnit 4.13.2, AndroidX JUnit 1.3.0 y Espresso Core 3.7.0 cuando correspondan al alcance funcional implementado.
  - Duración estimada: 144 min
  - [Ver capítulo completo](Capitulo07/README.md)


---

*Material didáctico preparado por Global K, S.A. de C.V.*
