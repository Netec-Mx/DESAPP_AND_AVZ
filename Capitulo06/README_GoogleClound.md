## Proyecto Google Cloud

<br/>
<br/>

| Elemento | Para qué sirve |
|---|---|
| Proyecto de Google Cloud | Agrupa el servicio de mapas y su configuración. |
| API key | Identifica el proyecto que utilizará Google Maps. |
| SHA-1 | Identifica el certificado con el que se firma tu aplicación de prueba. |


### **1. Crear el proyecto de Google Cloud**

1. Abre [Google Cloud Console](https://console.cloud.google.com/).

2. Inicia sesión con tu cuenta de Google.

3. En la parte superior, pulsa el **selector de proyecto**. Puede mostrar “Seleccionar un proyecto” o el nombre de otro proyecto.

4. Pulsa **Nuevo proyecto**.

5. Escribe como nombre:

   ```text
   TelemetryTracker-Lab
   ```

6. Si aparece **Organización**, puedes seleccionar **Sin organización**, cuando esa opción esté disponible.

7. Pulsa **Crear**.

8. Cuando termine, selecciona `TelemetryTracker-Lab` en la parte superior.

Este proyecto de Google Cloud se configura desde el navegador; continúas trabajando con TelemetryTracker en Android Studio. [developers.google.com](https://developers.google.com/maps/documentation/android-sdk/start)


<br/><br/>

### **2. Habilitar la facturación** en Google Cloud

Con `TelemetryTracker-Lab` seleccionado:

1. Abre el menú lateral **☰**.

2. Entra en **Facturación**.

3. Si el proyecto ya está vinculado a una cuenta de facturación activa, continúa.

4. Si no lo está, selecciona **Vincular una cuenta de facturación** y sigue el formulario.

Si no tienes una cuenta de facturación, Google puede solicitar que crees una y registres información de pago. Si utilizarás una cuenta de la empresa, su administrador puede hacer la vinculación. Google Maps requiere facturación habilitada; el uso puede generar cargos según el servicio y su consumo. [cloud.google.com](https://cloud.google.com/billing/docs/how-to/modify-project)

<br/><br/>

### **3. Activar Maps SDK for Android**

1. Abre **☰ → APIs y servicios → Biblioteca**.

2. En el buscador escribe exactamente:

   ```text
   Maps SDK for Android
   ```

3. Selecciona ese resultado.

4. Pulsa **Habilitar**.

**Resultado esperado:** aparece la opción **Administrar**, indicando que el servicio está habilitado. [developers.google.com](https://developers.google.com/maps/documentation/android-sdk/start)

**Link directo a `Maps SDK for Android`** https://console.cloud.google.com/apis/library/maps-android-backend.googleapis.com?project=telemetrytracker-lab


<br/>
<br/>

### **4. Obtener el SHA-1 desde Android Studio**

Ahora cambia a Android Studio:

1. Abre tu proyecto **TelemetryTracker**.

2. Abre la pestaña **Terminal**, normalmente en la parte inferior.

3. La terminal debe estar en la raíz del proyecto. Si necesitas ubicarte, ejecuta:

   ```powershell
   cd "C:\Users\Netec\AndroidStudioProjects\TelemetryTracker"
   ```

4. Ejecuta este comando, **exactamente así**:

   ```powershell
   .\gradlew.bat :app:signingReport
   ```

5. Espera a que termine.

6. Busca el bloque correspondiente a **`Variant: debug`**. Tendrá una estructura semejante a esta:

   ```text
   Variant: debug
   Config: debug
   Store: ...
   Alias: ...
   SHA1: ...
   SHA-256: ...
   ```

7. Copia **solamente el valor después de `SHA1:`**, incluidos los dos puntos que separan sus parejas de caracteres.

Utiliza el SHA-1 de **debug**, porque esa es la aplicación que ejecutarás durante el laboratorio. [developers.google.com](https://developers.google.com/android/guides/client-auth)


<br/><br/>

### **5. Crear y configurar la API key**

Regresa a Google Cloud y comprueba que sigue seleccionado `TelemetryTracker-Lab`.

1. Entra en **APIs y servicios → Credenciales**.
2. Pulsa **Crear credenciales → Clave de API**.
3. Google mostrará la clave creada. Cópiala y cierra el diálogo. [developers.google.com](https://developers.google.com/maps/documentation/android-sdk/start)
4. En la lista de credenciales, pulsa el nombre de esa clave para editarla.
5. Puedes cambiar su nombre a:

   ```text
   TelemetryTracker-Android-Debug
   ```

6. En **Restricciones de aplicaciones**, selecciona **Aplicaciones de Android**.
7. Pulsa **Agregar** y completa:

   | Campo | Valor |
   |---|---|
   | Nombre del paquete | `com.example.advancedtracker` |
   | Huella SHA-1 | El valor que obtuviste en Android Studio |

8. En **Restricciones de API**, selecciona **Restringir clave**.
9. Selecciona **Maps SDK for Android**.
10. Pulsa **Guardar**.

Estas restricciones autorizan el uso de la clave para tu aplicación y el servicio de mapas. [developers.google.com](https://developers.google.com/maps/api-security-best-practices)

<br/><br/>

### **6. Colocar la clave en TelemetryTracker**

En Android Studio, abre o crea este archivo:

```text
app/src/debug/res/values/maps_api_key.xml
```

Su contenido será:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="maps_api_key" translatable="false">PEGA_AQUI_TU_API_KEY</string>
</resources>
```

Reemplaza únicamente `PEGA_AQUI_TU_API_KEY` por la clave que creó Google.

El laboratorio agrega después esta referencia dentro del `<application>` de `app/src/debug/AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="@string/maps_api_key" />
```
