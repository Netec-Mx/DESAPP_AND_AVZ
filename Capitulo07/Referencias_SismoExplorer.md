# Referencias adicionales

<br/>
<br/>

### Datos de sismos y formato de la respuesta

**Catálogo de sismos de USGS**

https://earthquake.usgs.gov/fdsnws/event/1/

Describe consultas personalizadas por fechas, magnitud y ubicación, además de formatos y parámetros. **Opcional:** Vale la pena si quieres ampliar el proyecto con búsquedas; SismoExplorer utiliza el feed predefinido.

<br/>

**Feed utilizado en SismoExplorer**

https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.geojson

Devuelve los datos GeoJSON de eventos de magnitud 2.5 o superior de los últimos siete días. **Consulta puntual:** Ábrelo para examinar la respuesta real y contrastarla con los DTO. Es un recurso de datos, no un tutorial.

<br/>

**Estructura GeoJSON de USGS**

https://earthquake.usgs.gov/earthquakes/feed/v1.0/geojson.php

Explica la colección de eventos, sus propiedades y el orden longitud, latitud y profundidad. **Recomendada:** Especialmente útil para entender y revisar el mapper del Paso 3.

<br/>
<br/>

### ViewModel, estado de interfaz y corrutinas

**Creación de ViewModels con dependencias**

https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories

Explica ViewModelProvider.Factory, CreationExtras y la creación de SavedStateHandle. **Recomendada:** Aclara cómo recibe SismosViewModel sus repositorios sin incorporar Hilt.

<br/>

**Estado en Jetpack Compose**

https://developer.android.com/develop/ui/compose/state

Presenta estado observable, recomposición, remember, elevación de estado y observación de flujos. **Recomendada:** Es una guía fundamental para comprender la relación entre el estado y lo que muestra la pantalla.

<br/>

**SavedStateHandle en el ViewModel**

https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate

Describe cómo guardar y recuperar valores de estado mediante SavedStateHandle, incluida su integración con flujos. **Recomendada:** Ayuda a entender la restauración de filtros y selección después de una recreación del proceso.

<br/>

**Guardado del estado en Compose**

https://developer.android.com/develop/ui/compose/state-saving

Compara mecanismos de guardado de estado y muestra rememberSaveable y SavedStateHandle.getStateFlow. **Recomendada:** Permite distinguir el estado necesario para reconstruir la pantalla de los datos que se persisten en Room.

<br/>

**Buenas prácticas de corrutinas en Android**

https://developer.android.com/kotlin/coroutines/coroutines-best-practices

Reúne recomendaciones sobre dispatchers, trabajo desde el ViewModel, cancelación, excepciones y pruebas. **Recomendada:** Vale la pena leerla para revisar que las consultas no bloqueen la interfaz y respeten la cancelación.

<br/>
<br/>

### Dependencias de Retrofit y Gson

**Artefacto Retrofit 3.0.0**

https://central.sonatype.com/artifact/com.squareup.retrofit2/retrofit/3.0.0

Ficha de Maven Central con las coordenadas, declaración de dependencia y metadatos de esa versión. **Consulta puntual:** Útil para verificar el artefacto usado en el laboratorio; no sustituye una guía de uso de Retrofit.

<br/>

**Convertidor Gson 3.0.0**

https://central.sonatype.com/artifact/com.squareup.retrofit2/converter-gson/3.0.0

Ficha del módulo que integra la conversión JSON con Gson en Retrofit. **Consulta puntual:** Sirve para revisar la dependencia y su versión cuando configures o diagnostiques la serialización.

<br/>
<br/>

### Persistencia local con Room y herramientas

**Introducción y componentes de Room**

https://developer.android.com/training/data-storage/room

Explica la base de datos, las entidades y los DAO, con una implementación básica. **Recomendada:** Es el punto de entrada para comprender cómo se guardan los favoritos.

<br/>

**Acceso a datos mediante DAO**

https://developer.android.com/training/data-storage/room/accessing-data

Describe consultas SQL y operaciones de inserción, actualización y eliminación mediante DAO. **Recomendada:** Ayuda a interpretar las operaciones de FavoritosDao y su relación con las entidades.

<br/>

**Consultas asíncronas y observables de Room**

https://developer.android.com/training/data-storage/room/async-queries

Explica consultas puntuales con suspend y consultas observables con Flow, entre otras alternativas. **Recomendada:** Relaciona las modificaciones de la base de datos con las nuevas listas que recibe el ViewModel.

<br/>

**Referencia de @Upsert**

https://developer.android.com/reference/androidx/room/Upsert

Documenta la operación que inserta un registro o lo actualiza si ya existe, según su clave primaria. **Consulta puntual:** Conviene revisarla para entender el guardado de un favorito cuyo identificador ya está registrado.

<br/>

**Database Inspector**

https://developer.android.com/studio/inspect/database

Muestra cómo inspeccionar bases SQLite compatibles, examinar tablas y ejecutar consultas desde Android Studio. **Recomendada:** Tiene utilidad práctica para comprobar el contenido de la tabla de favoritos mientras se ejecuta la aplicación.

<br/>

**Versiones y notas de Room**

https://developer.android.com/jetpack/androidx/releases/room

Recoge publicaciones, cambios, correcciones y configuración de las distintas versiones de Room. **Consulta puntual:** Léela al investigar una incompatibilidad o planear una actualización; no necesitas recorrer todo el historial.

<br/>

**Publicación de KSP 2.3.6**

https://github.com/google/ksp/releases/tag/2.3.6

Contiene las notas y los recursos de una publicación específica de Kotlin Symbol Processing. **Consulta puntual:** Permite verificar la versión citada en el laboratorio; no es una introducción a KSP ni una indicación de actualizar.

<br/>

**Configuración y migración de kapt a KSP**

https://developer.android.com/build/migrate-to-ksp

Explica cómo incorporar KSP y sustituir el procesamiento con kapt cuando las bibliotecas lo admiten. **Consulta puntual:** Útil para revisar la configuración del procesador de Room; el laboratorio ya utiliza KSP.

<br/>
<br/>

### Consolidación del estado con Flows

**StateFlow**

https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/

Documenta el flujo de estado, su valor actual y la actualización de MutableStateFlow. **Recomendada:** Es la base para entender cómo SismosViewModel expone el estado observable de la pantalla.

<br/>

**combine**

https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/combine.html

Describe cómo combinar los valores más recientes de varios flujos mediante una transformación. **Recomendada:** Se relaciona directamente con la unión de sismos, favoritos, filtros, controles y ubicación.

<br/>

**stateIn**

https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/state-in.html

Explica cómo compartir un Flow como StateFlow dentro de un alcance de corrutinas. **Recomendada:** Aclara el valor inicial y la conversión del flujo combinado en el estado observado por Compose.

<br/>

**SharingStarted.WhileSubscribed**

https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-sharing-started/-companion/-while-subscribed.html

Detalla cuándo comienza y se detiene la recolección compartida, así como sus tiempos de espera y caché. **Consulta puntual:** Vale la pena para interpretar WhileSubscribed(5_000): el plazo corresponde a la ausencia de suscriptores.

<br/>
<br/>

### Permisos, ubicación y emulador

**Panorama de permisos en Android**

https://developer.android.com/guide/topics/permissions/overview

Presenta los tipos de permisos, sus objetivos y las formas de conceder acceso. **Recomendada:** Es útil para distinguir declarar un permiso en el manifiesto de solicitarlo durante la ejecución.

<br/>

**Permisos de ubicación**

https://developer.android.com/develop/sensors-and-location/location/permissions

Explica las categorías de acceso a la ubicación y los permisos relacionados. **Recomendada:** Ayuda a justificar el uso de ubicación aproximada para la consulta puntual del proyecto.

<br/>

**Ubicación aproximada y autorización en tiempo de ejecución**

https://developer.android.com/develop/sensors-and-location/location/permissions/runtime

Describe la solicitud de acceso a ubicación y las opciones de precisión disponibles para el usuario. **Recomendada:** Es especialmente importante para comprender por qué la posición obtenida puede tener una incertidumbre considerable.

<br/>

**Solicitud de permisos durante la ejecución**

https://developer.android.com/training/permissions/requesting

Explica la comprobación del permiso, su solicitud y el tratamiento de la respuesta del usuario. **Recomendada:** Relaciona el launcher de Compose con los recorridos de autorización y denegación.

<br/>

**Configuración de Google Play services**

https://developers.google.com/android/guides/setup

Describe los requisitos del entorno y cómo agregar los módulos de Google Play services. **Consulta puntual:** Útil al preparar el dispositivo, la imagen del emulador y la dependencia de ubicación.

<br/>

**FusedLocationProviderClient**

https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient

Documenta el cliente de ubicación y operaciones como getCurrentLocation. **Recomendada:** Consulta la sección de getCurrentLocation para entender cómo solicita una posición el repositorio del Paso 6.

<br/>

**CurrentLocationRequest.Builder**

https://developers.google.com/android/reference/com/google/android/gms/location/CurrentLocationRequest.Builder

Describe opciones de la solicitud, como prioridad, granularidad, antigüedad admitida y duración. **Consulta puntual:** Aclara los valores configurados en el repositorio y por qué puede devolver una posición reciente o no conseguirla.

<br/>

**Versiones publicadas de Google Play services**

https://developers.google.com/android/guides/releases

Reúne notas de publicación de los distintos módulos de Google Play services. **Consulta puntual:** Útil para revisar una versión o un cambio específico; no requiere leer todas las publicaciones.

<br/>

**Controles extendidos del emulador**

https://developer.android.com/studio/run/emulator-extended-controls

Explica controles para simular ubicación y otras condiciones del dispositivo emulado. **Consulta puntual:** Conviene revisarla si quieres retomar la prueba de ubicación pendiente en el emulador.

<br/>

**Consola del emulador y geo fix**

https://developer.android.com/studio/run/emulator-console

Documenta comandos para controlar el emulador, incluida la simulación de posiciones geográficas. **Consulta puntual:** Sirve para revisar geo fix y recordar que sus argumentos van en orden longitud y latitud.

<br/>
<br/>

### Pruebas unitarias y documentación

**Pruebas unitarias locales en Android**

https://developer.android.com/training/testing/local-tests

Explica las pruebas que se ejecutan en la JVM de la computadora, sus dependencias y sus límites. **Recomendada:** Es la lectura principal para entender las pruebas de coordenadas y distancia del Paso 7.

<br/>

**Ejecución de pruebas por línea de comandos**

https://developer.android.com/studio/test/command-line

Describe tareas Gradle, selección de clases o métodos y ubicación de informes de resultados. **Consulta puntual:** Útil al ejecutar testDebugUnitTest y revisar el informe; también distingue pruebas locales e instrumentadas.

<br/>

**Aserciones de JUnit 4.13.2**

https://junit.org/junit4/javadoc/4.13.2/org/junit/Assert.html

Documenta aserciones como assertEquals, assertTrue, assertFalse y assertThrows. **Consulta puntual:** Conviene revisar las firmas utilizadas, especialmente la tolerancia para Double y la comprobación de excepciones.

<br/>

**Documentación KDoc**

https://kotlinlang.org/docs/kotlin-doc.html

Explica los comentarios de documentación de Kotlin, sus etiquetas y los enlaces a elementos del código. **Recomendada:** Útil para revisar que los comentarios describan parámetros, resultados y excepciones reales.

<br/>
<br/>

### APIs opcionales para otros proyectos

**Open-Meteo**

https://open-meteo.com/en/docs

Documenta una API de pronóstico meteorológico con variables y consultas asociadas a coordenadas. **Opcional:** Vale la pena para una aplicación de clima o un ejercicio adicional de Retrofit; no se utiliza en SismoExplorer.

<br/>

**Overpass API de OpenStreetMap**

https://wiki.openstreetmap.org/wiki/Overpass_API

Presenta consultas de datos de OpenStreetMap mediante filtros geográficos y etiquetas. **Opcional:** Útil para explorar lugares y elementos geográficos; no interviene en el cálculo de distancia del laboratorio.

<br/>
<br/>

### Ruta de lectura sugerida

Para un repaso breve, comienza por estas guías y consulta después las referencias de API según las dudas que surjan:

1. Estado en Jetpack Compose.
2. Creación de ViewModels con dependencias.
3. StateFlow, combine y stateIn.
4. Introducción a Room y consultas asíncronas.
5. Ubicación aproximada y autorización en tiempo de ejecución.
6. Pruebas unitarias locales en Android.
7. Documentación KDoc.

