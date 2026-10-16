
# Calculo de la distancia

En SismoExplorer usamos la **fórmula de Haversine** para calcular la distancia aproximada sobre la superficie terrestre entre dos puntos:

- **Origen:** la ubicación del usuario.
- **Destino:** las coordenadas del epicentro del sismo.

Representamos la Tierra como una esfera de radio **6371 km**.

Primero convertimos las latitudes y longitudes de grados a radianes:

\[
\text{radianes}=\text{grados}\times\frac{\pi}{180}
\]

Si \(\varphi_1,\varphi_2\) son las latitudes y \(\lambda_1,\lambda_2\) las longitudes, calculamos:

\[
a=\sin^2\left(\frac{\varphi_2-\varphi_1}{2}\right)
+\cos(\varphi_1)\cos(\varphi_2)
\sin^2\left(\frac{\lambda_2-\lambda_1}{2}\right)
\]

<br/>

Después obtenemos el ángulo central entre los puntos:

\[
c=2\,\operatorname{atan2}\left(\sqrt{a},\sqrt{1-a}\right)
\]

<br/>

Y multiplicamos ese ángulo por el radio terrestre:

\[
\boxed{d=6371\,c\quad\text{km}}
\]

<br/>

Por ejemplo, entre **(0°, 0°)** y **(0°, 1°)** hay un grado sobre el ecuador:

\[
d=6371\times\frac{\pi}{180}\approx111.195\text{ km}
\]

<br/>

Ese es el valor esperado de una de nuestras pruebas.

El resultado corresponde al **arco más corto sobre la esfera hasta el epicentro**. No incluye la profundidad del sismo ni representa una ruta de transporte. Su exactitud también depende de la ubicación aproximada que proporciona el dispositivo.