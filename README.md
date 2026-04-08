# 🐦 Construyendo Flappy Bird — Rama `main`
## Introducción a LibGDX y configuración del proyecto base

> *Antes de escribir una sola línea del juego necesitas el suelo bajo tus pies: el entorno instalado, el proyecto creado y el primer resultado ejecutándose. Esta rama no produce ningún juego. Produce la base sobre la que todo lo demás se construirá.*

---

## ¿Qué es LibGDX y por qué lo usamos?

LibGDX es un framework de desarrollo de videojuegos en Java, de código abierto, que permite escribir el código del juego **una sola vez** y ejecutarlo en múltiples plataformas: Android, escritorio (Windows/Linux/macOS) y HTML5.

Para este curso es la herramienta ideal por tres razones concretas:

**Usa Java.** Ya conoces el lenguaje. No hay que aprender Kotlin, C# ni nada nuevo. LibGDX es Java puro: herencia, interfaces, polimorfismo. Todo lo que has estudiado en DAM se aplica directamente.

**Es multiplataforma sin trampa.** El mismo código que pruebas en segundos en tu PC se despliega en Android sin cambiar una línea.

**Es ampliamente usado.** No es un proyecto de un solo desarrollador. Tiene comunidad activa, documentación oficial y decenas de juegos comerciales publicados con él.

---

## Acto 1 — Las tres herramientas que necesitas

### JDK 17

LibGDX requiere Java 17 como mínimo. Verifica que lo tienes:

```
java -version
```

La salida debe mostrar `openjdk 17.x.x`. Si tienes una versión anterior, descarga el JDK 17 desde [adoptium.net](https://adoptium.net).

### Android Studio

Es el entorno de desarrollo principal. Incluye el SDK de Android, el emulador y el depurador integrado. Descárgalo desde [developer.android.com/studio](https://developer.android.com/studio) e instálalo con las opciones por defecto.

Durante la instalación te pedirá instalar el SDK de Android. Acepta.

### gdx-liftoff: el generador de proyectos

LibGDX tiene su propia herramienta para crear proyectos nuevos. Descarga el fichero `.jar` desde:

```
https://github.com/libgdx/gdx-liftoff/releases
```

No hay que instalarlo. Se ejecuta directamente:

```
java -jar gdx-liftoff-x.x.x.jar
```

---

## Acto 2 — Crear el proyecto

Cuando ejecutas gdx-liftoff aparece un formulario. Rellénalo exactamente así:

```
Project name:   FlappyBird
Package:        com.mygdx.game
Main class:     MainGame
Android SDK:    (la versión instalada por Android Studio)
```

En la sección **Platforms**, marca:

```
✅ Core
✅ Android
✅ Desktop (LWJGL3)
```

En la sección **Extensions**, marca:

```
✅ Box2D
```

Box2D es el motor de físicas que usarás más adelante en el curso. Por ahora solo necesitas que esté incluido en el proyecto desde el principio.

Pulsa **Generate** y elige la carpeta donde quieres el proyecto. gdx-liftoff genera toda la estructura y configura Gradle automáticamente.

---

## Acto 3 — La arquitectura de tres módulos

Cuando abres el proyecto en Android Studio verás esta estructura:

```
FlappyBird/
├── core/                    ← AQUÍ va todo el código del juego
│   └── src/
│       └── com/mygdx/game/
│           └── MainGame.java
│
├── android/                 ← Launcher para Android
│   ├── src/
│   │   └── AndroidLauncher.java
│   └── assets/              ← AQUÍ irán los sprites y sonidos
│
├── desktop/                 ← Launcher para PC
│   └── src/
│       └── DesktopLauncher.java
│
└── build.gradle
```

La pregunta que surge de inmediato es: ¿por qué tres módulos?

### `core`: el código que no sabe en qué plataforma está

`core` contiene todo el código del juego: lógica, actores, pantallas. No importa si se ejecuta en un teléfono Android o en un PC. No usa nada específico de ninguna plataforma.

> ⚠️ **Regla de oro:** Nunca importes clases de `android.*` en el módulo `core`. Si lo haces, el juego dejará de compilar en desktop.

### `android` y `desktop`: los lanzadores

`AndroidLauncher.java` y `DesktopLauncher.java` son los puntos de entrada en cada plataforma. Crean una instancia de `MainGame` y se la pasan al sistema operativo correspondiente. Son una docena de líneas de código cada uno. No escribirás casi nada aquí.

### Por qué el módulo `desktop` es tu mejor aliado

Compilar y ejecutar en Android tarda entre 30 y 90 segundos. Compilar y ejecutar en desktop tarda 2-3 segundos. Durante el desarrollo probarás cambios decenas de veces seguidas. Usar desktop en lugar de Android en cada ciclo de prueba te ahorra horas de espera a lo largo del proyecto.

---

## Acto 4 — Configurar el DesktopLauncher

El fichero que genera gdx-liftoff puede variar ligeramente según la versión. Asegúrate de que queda así:

### 📄 DesktopLauncher.java

```java
package com.mygdx.game.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mygdx.game.MainGame;

public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

        config.setTitle("Flappy Bird - LibGDX");
        config.setWindowedMode(480, 800);
        config.setForegroundFPS(60);

        new Lwjgl3Application(new MainGame(), config);
    }
}
```

`480 × 800` es la resolución de la ventana en píxeles. `setForegroundFPS(60)` limita la tasa de refresco a 60 fotogramas por segundo.

---

## Acto 5 — El punto de entrada: `MainGame.java`

El fichero que gdx-liftoff genera en `core/` extiende `ApplicationAdapter` y tiene tres métodos que representan el ciclo de vida de la aplicación:

### 📄 MainGame.java (generado por gdx-liftoff)

```java
public class MainGame extends ApplicationAdapter {

    @Override
    public void create() {
        // Se llama UNA SOLA VEZ al arrancar la aplicación.
        // Aquí se inicializará todo: pantallas, recursos, físicas...
    }

    @Override
    public void render() {
        // Se llama ~60 veces por segundo.
        // Aquí irá toda la lógica de actualización y el dibujo.
    }

    @Override
    public void dispose() {
        // Se llama al cerrar la aplicación.
        // Aquí se liberará toda la memoria: texturas, sonidos...
    }
}
```

Este ciclo — `create → render (×N) → dispose` — es el **game loop**. Es la estructura sobre la que se construye cualquier videojuego. Todo lo que añadirás en las siguientes ramas vive dentro de él.

En la rama siguiente transformarás `MainGame` para que gestione múltiples pantallas. Por ahora, lo importante es entender que existe y que funciona.

---

## Acto 6 — Primera ejecución

1. Abre Android Studio → **Open** → selecciona la carpeta `FlappyBird/`
2. Espera a que Gradle sincronice (puede tardar unos minutos la primera vez)
3. En la barra de herramientas, despliega el selector de configuración de ejecución y selecciona **desktop**
4. Pulsa ▶ (Run)

Aparecerá una ventana negra de 480×800 píxeles. Eso es exactamente el resultado correcto: la aplicación arranca, `render()` limpia la pantalla con el color por defecto (negro) y no hay nada más que dibujar todavía.

---

## La estructura al terminar esta rama

```
FlappyBird/
├── core/src/com/mygdx/game/
│   └── MainGame.java         ← extiende ApplicationAdapter, vacío por ahora
├── android/assets/           ← carpeta vacía, aquí irán los recursos
└── desktop/src/.../
    └── DesktopLauncher.java  ← configurado: 480×800, 60 FPS
```

Al ejecutar: **ventana negra de 480×800**. Es el resultado correcto.

---

## 🛠️ Ejercicio práctico

**Objetivo:** Confirmar que el entorno funciona y observar el game loop.

1. Ejecuta el proyecto en desktop. Confirma que aparece la ventana de 480×800.

2. En `MainGame.create()`, añade la siguiente línea:

```java
Gdx.app.log("CICLO", "create() llamado");
```

En `MainGame.render()`, añade:

```java
Gdx.app.log("CICLO", "render() llamado");
```

Ejecuta 3 segundos y cierra. ¿Cuántas veces aparece `"render() llamado"` en el log?

3. Cambia `setForegroundFPS(60)` a `setForegroundFPS(10)`. Ejecuta 3 segundos. ¿Cuántas veces aparece ahora el log de `render()`?

4. Restaura `setForegroundFPS(60)`.

5. **Pregunta:** Los recursos del juego (sprites, sonidos) irán en `android/assets/`, no en `core/assets/`. Sabiendo que `core/` es donde está todo el código del juego, ¿por qué tiene sentido poner los assets en el módulo `android` y no en `core/`?

