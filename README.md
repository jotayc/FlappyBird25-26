# 🐦 Construyendo Flappy Bird — Rama `7.MultiplePipes&Score`
## Generación continua de tuberías y puntuación en pantalla

> *Hasta ahora solo existía un par de tuberías. Pero Flappy Bird necesita un flujo continuo de obstáculos que aparecen periódicamente y desaparecen cuando salen de la pantalla. Además, el jugador necesita ver su puntuación. Esta rama introduce cuatro conceptos clave de desarrollo de videojuegos: la generación temporizada de objetos (spawning), la gestión de colecciones de actores, la limpieza segura de recursos durante la simulación física, y el renderizado de texto con una cámara independiente.*

---

## ¿Qué cambia en esta rama?

La rama 6 tenía un solo par de tuberías creado en `show()`. Esta rama lo transforma en un sistema completo:

1. **Generación temporizada**: cada 1.5 segundos aparece un nuevo par de tuberías fuera de la pantalla.
2. **Posición vertical aleatoria**: el hueco de cada tubería está a una altura diferente.
3. **Eliminación automática**: cuando una tubería sale de la pantalla, se destruyen sus bodies y se libera la memoria.
4. **Colección dinámica**: un `Array<Pipes>` gestiona todos los pares de tuberías activos.
5. **Puntuación visual**: un `BitmapFont` dibuja en pantalla cuántas tuberías hay activas, usando una cámara independiente.
6. **Preparación de pantallas**: `MainGame` crea las tres pantallas del juego (`GameScreen`, `GameOverScreen`, `GetReadyScreen`).

---

## Paso 1 — El problema: ¿cómo generar tuberías periódicamente?

### El concepto de "spawning"

En videojuegos, **spawning** es la creación de nuevos objetos durante la partida. No podemos crear todas las tuberías al inicio porque no sabemos cuántas necesitaremos — la partida puede durar 10 segundos o 10 minutos. Necesitamos un sistema que cree tuberías **mientras se juega**.

### La técnica: acumulador de tiempo

El bucle `render()` se ejecuta ~60 veces por segundo. Cada ejecución recibe un `delta` (tiempo transcurrido desde el frame anterior, típicamente ~0.016 segundos). La idea es ir acumulando estos deltas hasta alcanzar el tiempo deseado:

```
Frame 1:  timeToCreatePipe = 0.000 + 0.016 = 0.016
Frame 2:  timeToCreatePipe = 0.016 + 0.017 = 0.033
Frame 3:  timeToCreatePipe = 0.033 + 0.016 = 0.049
...
Frame 90: timeToCreatePipe = 1.489 + 0.016 = 1.505  → ¡CREAR TUBERÍA!
          timeToCreatePipe = 1.505 - 1.500 = 0.005   → reiniciar contador
```

### Analogía: un temporizador de cocina

Es como poner un temporizador de 1.5 minutos para hornear galletas. Cada segundo miras el reloj. Cuando llega a 1:30, sacas las galletas y vuelves a poner el temporizador. Aquí el "reloj" es el acumulador `timeToCreatePipe` y las "galletas" son los pares de tuberías.

---

## Paso 2 — El `Array` de LibGDX: colecciones para juegos

### ¿Por qué `Array` de LibGDX y no `ArrayList` de Java?

LibGDX proporciona su propia clase `Array<T>` (en `com.badlogic.gdx.utils.Array`) optimizada para videojuegos:

| Característica | `java.util.ArrayList` | `com.badlogic.gdx.utils.Array` |
|---------------|----------------------|-------------------------------|
| Creación de basura (GC) | Genera basura al redimensionar | Minimiza la creación de objetos temporales |
| Iteración | Iterator crea objetos nuevos | Iteración directa sin allocations |
| Rendimiento en juegos | Pausas por Garbage Collector | Predecible, sin pausas |

En un juego que ejecuta 60 frames por segundo, cada milisegundo cuenta. El Garbage Collector de Java puede provocar **micro-pausas** cuando limpia objetos temporales. La clase `Array` de LibGDX está diseñada para evitar estas pausas.

> ⚠️ **Error muy común en clase:** Importar `java.util.Array` o `java.util.ArrayList` en lugar de `com.badlogic.gdx.utils.Array`. El IDE puede sugerir la importación incorrecta. Asegúrate de que el import sea:
> ```java
> import com.badlogic.gdx.utils.Array;
> ```

### Nuevos atributos y constantes en `GameScreen`

```java
private final float TIME_TO_SPAWN_PIPES = 1.5f;  // segundos entre tuberías
private float timeToCreatePipe;                    // acumulador de tiempo
private Array<Pipes> arrayPipes;                   // colección de pares activos
```

Y en el constructor:

```java
this.arrayPipes = new Array();
this.timeToCreatePipe = 0f;
```

---

## Paso 3 — El método `addPipes()`: spawn temporizado

Este es el método central de la rama. Se llama en cada frame desde `render()`:

```java
public void addPipes(float delta) {

    TextureRegion pipeDownTexture = mainGame.assetManager.getPipeDownTR();
    TextureRegion pipeTopTexture = mainGame.assetManager.getPipeUpTR();

    if (bird.state == Bird.STATE_NORMAL) {
        this.timeToCreatePipe += delta;

        if (this.timeToCreatePipe >= TIME_TO_SPAWN_PIPES) {
            this.timeToCreatePipe -= TIME_TO_SPAWN_PIPES;

            float posRandomY = MathUtils.random(0f, 2f);
            Pipes pipes = new Pipes(this.world, pipeDownTexture, pipeTopTexture,
                                    new Vector2(5f, posRandomY));
            arrayPipes.add(pipes);
            this.stage.addActor(pipes);
        }
    }
}
```

Analicemos cada parte:

### 3.1 — Acceso directo a `bird.state`

```java
if (bird.state == Bird.STATE_NORMAL) { ... }
```

En esta rama, el atributo `state` de `Bird` pasa de `private` a `public`. Esto permite que `GameScreen` acceda directamente sin un getter. Es una solución rápida y funcional, aunque desde el punto de vista de la encapsulación no es la más elegante. Un getter `getState()` o un método `isAlive()` sería más limpio, pero para este proyecto educativo es perfectamente válido.

### 3.2 — ¿Por qué restar y no resetear a cero?

```java
this.timeToCreatePipe -= TIME_TO_SPAWN_PIPES;  // Resta, no asigna 0
```

Si `timeToCreatePipe` llega a `1.505` y restamos `1.5`, queda `0.005` — ese "sobrante" se conserva para el siguiente ciclo. Si lo pusiéramos a `0`, perderíamos esos `0.005` segundos. En la práctica, la diferencia es mínima, pero restar evita **drift temporal** (desviación progresiva del timing).

### 3.3 — Posición aleatoria con `MathUtils.random()`

```java
float posRandomY = MathUtils.random(0f, 2f);
```

La posición Y del bodyDown se elige aleatoriamente entre `0f` y `2f`. La tubería superior se posiciona automáticamente a `PIPE_HEIGHT + SPACE_BETWEEN_PIPES` por encima, así que el hueco varía en cada par:

```
posRandomY = 0.0  →  hueco bajo, cerca del suelo
posRandomY = 1.0  →  hueco a media altura
posRandomY = 2.0  →  hueco más alto
```

### 3.4 — Posición X fuera de pantalla

```java
new Vector2(5f, posRandomY)
```

La coordenada X es `5f`, fuera del borde derecho (`WORLD_WIDTH = 4.8f`). La tubería aparece invisible y se desliza hacia la izquierda con `SPEED = -2f`.

### 3.5 — Añadir al array Y al Stage

```java
arrayPipes.add(pipes);           // Para poder iterar y limpiar después
this.stage.addActor(pipes);      // Para que se dibuje y actualice
```

Ambas líneas son necesarias: el Stage llama a `act()` y `draw()`, y el array permite iterar para la eliminación.

---

## Paso 4 — `isOutOfScreen()` en `Pipes`

Para saber cuándo eliminar una tubería, `Pipes` expone un nuevo método:

```java
public boolean isOutOfScreen() {
    return this.bodyDown.getPosition().x <= -2f;
}
```

¿Por qué `-2f` y no `0f`? Porque `bodyDown.getPosition().x` es el **centro** del body. Con `PIPE_WIDTH = 0.85f`, el borde derecho está a `centro + 0.425f`. Usando `-2f` como umbral, nos aseguramos de que la tubería ha desaparecido completamente antes de eliminarla:

```
    Pantalla visible
    ←─────────────────────→
    0                    4.8

    ┌─┐ x = -0.5  → borde derecho aún cerca de 0 (podría verse)
    │ │
    └─┘
    
    ┌─┐ x = -2.0  → completamente fuera → isOutOfScreen() = true ✓
    │ │
    └─┘
```

---

## Paso 5 — `removePipes()`: limpieza segura

### El problema: no puedes destruir bodies durante `world.step()`

Box2D procesa la física durante `world.step()`. Si destruyes un body mientras la simulación está en marcha, el motor puede crashear. Por eso existe `world.isLocked()`:

```java
public void removePipes() {
    for (Pipes pipe : this.arrayPipes) {
        if (!world.isLocked()) {
            if (pipe.isOutOfScreen()) {
                pipe.detach();
                pipe.remove();
                arrayPipes.removeValue(pipe, false);
            }
        }
    }
}
```

### Desglose

**`!world.isLocked()`**: Verifica que el mundo no está en medio de un step. `removePipes()` se llama **después** de `world.step()`, así que normalmente no estará bloqueado, pero es una comprobación defensiva.

**`pipe.detach()`**: Destruye fixtures y bodies Box2D. En esta rama, `detach()` de `Pipes` se ha mejorado para destruir los **tres** bodies (incluyendo el counter):

```java
public void detach() {
    bodyDown.destroyFixture(fixtureDown);
    world.destroyBody(bodyDown);

    bodyTop.destroyFixture(fixtureTop);
    world.destroyBody(bodyTop);

    bodyCounter.destroyFixture(fixtureCounter);  // ← NUEVO: ahora sí se limpia
    world.destroyBody(bodyCounter);
}
```

Este era un problema pendiente de la rama 5 (donde `detach()` no destruía el counter). Ahora la limpieza es completa.

**`pipe.remove()`**: Quita el Actor del Stage.

**`arrayPipes.removeValue(pipe, false)`**: Elimina la referencia del array. El parámetro `false` indica que usa `==` (identidad de referencia) en lugar de `.equals()`.

---

## Paso 6 — La puntuación: `BitmapFont` y doble cámara

### El problema de las fuentes en LibGDX

Los elementos del juego (pájaro, tuberías, fondo) se dibujan en **coordenadas del mundo** (0 a 4.8 de ancho, 0 a 8 de alto). Pero las fuentes (`BitmapFont`) trabajan en **píxeles**. Si intentas dibujar texto con la cámara del mundo, las letras serán enormes o microscópicas porque la cámara interpreta cada unidad como metros, no como píxeles.

La solución es usar **dos cámaras**:
- `worldCamera`: proyecta el mundo del juego (4.8 × 8 unidades).
- `fontCamera`: proyecta el texto en píxeles (480 × 800 píxeles).

### Analogía: dos proyectores en un cine

Imagina un cine con dos proyectores apuntando a la misma pantalla. Uno proyecta la película (el mundo del juego) y otro proyecta los subtítulos (la puntuación). Cada proyector tiene su propia escala y configuración, pero el espectador ve ambos superpuestos.

### Preparar la fuente y la cámara

```java
private OrthographicCamera fontCamera;
private BitmapFont score;
```

El método `prepareScore()` se llama en el constructor de `GameScreen`:

```java
private void prepareScore() {
    this.score = this.mainGame.assetManager.getFont();
    this.score.getData().scale(1f);

    this.fontCamera = new OrthographicCamera();
    this.fontCamera.setToOrtho(false, SCREEN_WIDTH, SCREEN_HEIGHT);
    this.fontCamera.update();
}
```

**`getFont()`** en `AssetMan` crea un `BitmapFont` a partir de dos archivos:

```java
public BitmapFont getFont() {
    return new BitmapFont(
        Gdx.files.internal(FONT_FNT),   // archivo .fnt (descripción del font)
        Gdx.files.internal(FONT_PNG),    // archivo .png (textura con los caracteres)
        false                             // no voltear verticalmente
    );
}
```

Los archivos `.fnt` y `.png` se generan con la herramienta **Hiero** (incluida con LibGDX). Hiero permite crear fuentes bitmap a partir de fuentes TrueType, eligiendo tamaño, color, borde y sombra.

**`scale(1f)`** ajusta el tamaño del texto. Un valor de `1f` duplica el tamaño base. Prueba diferentes valores hasta que el texto se vea bien.

**`setToOrtho(false, SCREEN_WIDTH, SCREEN_HEIGHT)`** configura la cámara de fuente en coordenadas de pantalla (píxeles). `false` significa que el eje Y apunta hacia arriba (como en el mundo del juego).

### Nuevas constantes en `Utils`

```java
public static final String FONT_FNT = "FBFont.fnt";
public static final String FONT_PNG = "FBFont.png";
```

Los archivos de fuente deben estar en `android/assets/`:

```
android/assets/
├── atlasFB.txt
├── atlasFB.png
├── jump.mp3
├── musicbg.mp3
├── FBFont.fnt        ← descripción del font
└── FBFont.png        ← textura con los caracteres
```

---

## Paso 7 — Dibujar la puntuación en `render()`

### El cambio de cámara en el batch

Esta es la parte más delicada de la rama. En `render()`, el batch del Stage cambia de cámara **dos veces**:

```java
@Override
public void render(float delta) {

    addPipes(delta);

    // --- FASE 1: Dibujar el mundo del juego ---
    this.stage.getBatch().setProjectionMatrix(worldCamera.combined);
    this.stage.act();
    this.world.step(delta, 6, 2);
    this.stage.draw();

    this.worldCamera.update();
    this.debugRenderer.render(this.world, this.worldCamera.combined);

    removePipes();

    // --- FASE 2: Dibujar la puntuación en píxeles ---
    this.stage.getBatch().setProjectionMatrix(this.fontCamera.combined);
    this.stage.getBatch().begin();
    this.score.draw(this.stage.getBatch(), "" + arrayPipes.size, SCREEN_WIDTH / 2, 725);
    this.stage.getBatch().end();
}
```

### ¿Qué hace `setProjectionMatrix()`?

El `batch` necesita saber cómo convertir coordenadas del juego a coordenadas de pantalla. La **matriz de proyección** contiene esta información. Al cambiarla, le decimos al batch que use una escala diferente:

```
worldCamera.combined  →  1 unidad = ~100 píxeles (mundo de 4.8 × 8)
fontCamera.combined   →  1 unidad = 1 píxel    (pantalla de 480 × 800)
```

### ¿Por qué `begin()` y `end()` manuales?

`stage.draw()` gestiona internamente su propio `batch.begin()` / `batch.end()`. Pero para dibujar el texto **fuera** del Stage, necesitamos abrir y cerrar el batch manualmente:

```java
this.stage.getBatch().begin();    // Abrir el batch
this.score.draw(...);             // Dibujar el texto
this.stage.getBatch().end();      // Cerrar el batch
```

### La puntuación temporal: `arrayPipes.size`

```java
this.score.draw(this.stage.getBatch(), "" + arrayPipes.size, SCREEN_WIDTH / 2, 725);
```

Por ahora, la "puntuación" es simplemente el número de tuberías activas en el array. No es la puntuación real del juego (que será el número de huecos cruzados), pero sirve como placeholder visual para verificar que el sistema de texto funciona.

Los parámetros de posición (`SCREEN_WIDTH / 2`, `725`) están en **píxeles** porque estamos usando `fontCamera`. El texto aparece centrado horizontalmente y cerca de la parte superior de la pantalla.

---

## Paso 8 — Cambios en `Pipes.draw()` y `detach()`

### `draw()` simplificado

En esta versión, `draw()` dibuja las texturas directamente desde las posiciones de los bodies, sin usar `setPosition()` del Actor para cada tubería:

```java
@Override
public void draw(Batch batch, float parentAlpha) {
    setPosition(bodyDown.getPosition().x, bodyDown.getPosition().y);
    batch.draw(this.pipeDownTR,
        bodyDown.getPosition().x - PIPE_WIDTH / 2,
        bodyDown.getPosition().y - PIPE_HEIGHT / 2,
        PIPE_WIDTH, PIPE_HEIGHT);

    batch.draw(this.pipeTopTR,
        bodyTop.getPosition().x - PIPE_WIDTH / 2,
        bodyTop.getPosition().y - PIPE_HEIGHT / 2,
        PIPE_WIDTH, PIPE_HEIGHT);
}
```

Se llama a `setPosition()` una sola vez (con la posición del bodyDown) para mantener actualizada la posición del Actor, pero las coordenadas del `batch.draw()` se calculan directamente desde cada body.

### `detach()` completo

```java
public void detach() {
    bodyDown.destroyFixture(fixtureDown);
    world.destroyBody(bodyDown);

    bodyTop.destroyFixture(fixtureTop);
    world.destroyBody(bodyTop);

    bodyCounter.destroyFixture(fixtureCounter);
    world.destroyBody(bodyCounter);
}
```

Ahora se destruyen los **tres** bodies y sus fixtures. Esto resuelve el problema de la rama 5 donde el counter quedaba sin limpiar.

---

## Paso 9 — `MainGame` prepara las tres pantallas

```java
public class MainGame extends Game {

    public GameScreen gameScreen;
    public GetReadyScreen getReadyScreen;
    public GameOverScreen gameOverScreen;
    public AssetMan assetManager;

    @Override
    public void create() {
        this.assetManager = new AssetMan();

        this.gameScreen = new GameScreen(this);
        this.gameOverScreen = new GameOverScreen(this);
        this.getReadyScreen = new GetReadyScreen(this);

        setScreen(this.gameScreen);
    }
}
```

Las tres pantallas se crean al inicio pero solo `gameScreen` se muestra. En ramas futuras, las transiciones entre pantallas se harán con `setScreen()`.

---

## Paso 10 — Cambios menores en `Bird`

### `state` pasa a `public`

```java
public int state;  // Antes era private
```

Esto permite el acceso directo `bird.state` desde `GameScreen`. Funcional pero no ideal en términos de encapsulación.

### `userData` en el body (no en la fixture)

A diferencia de la rama 6, ahora `userData` se asigna al `Body`:

```java
this.body.setUserData(Utils.USER_BIRD);
```

Y la fixture no lleva `userData`. Esto simplifica el código pero cambia cómo se accederá al identificador en el `ContactListener` de ramas futuras.

---

## Errores comunes en esta rama

### 1. "Las tuberías no aparecen"

Verifica que `addPipes(delta)` se llama en `render()` **antes** de `stage.act()`. Si se llama después, las tuberías recién creadas no se actualizarán hasta el frame siguiente.

### 2. "Import incorrecto de Array"

```java
// ❌ MAL:
import java.util.ArrayList;

// ✓ BIEN:
import com.badlogic.gdx.utils.Array;
```

### 3. "El texto de la puntuación no se ve o es gigante"

Verifica que `fontCamera` usa `SCREEN_WIDTH` y `SCREEN_HEIGHT` (píxeles), no `WORLD_WIDTH` y `WORLD_HEIGTH`. Si usas las coordenadas del mundo, el texto será enorme.

### 4. "Crash al eliminar tuberías"

Asegúrate de que `removePipes()` se ejecuta **después** de `world.step()` y comprueba `!world.isLocked()` antes de destruir bodies.

### 5. "El mundo del juego desaparece al dibujar la puntuación"

Si olvidas restaurar `setProjectionMatrix(worldCamera.combined)` antes de `stage.draw()`, todo el mundo se dibujará en coordenadas de píxeles (aparecerá como un punto minúsculo o se saldrá de pantalla).

---

## Código completo de esta rama

### 📄 Utils.java

```java
package com.iesfa.flappy.extra;

public class Utils {

    public static final int SCREEN_HEIGHT = 800;
    public static final int SCREEN_WIDTH = 480;

    public static final float WORLD_HEIGTH = 8f;
    public static final float WORLD_WIDTH = 4.8f;

    // Identificadores de assets
    public static final String ATLAS_MAP = "atlasFB.txt";
    public static final String BACKGROUND_IMAGE = "flappy_background";
    public static final String PIPE_DAWN = "pipeDown";
    public static final String PIPE_UP = "pipeUp";
    public static final String SOUND_JUMP = "jump.mp3";
    public static final String MUSIC_BG = "musicbg.mp3";

    // Identificadores de fuentes
    public static final String FONT_FNT = "FBFont.fnt";
    public static final String FONT_PNG = "FBFont.png";

    // Identificadores de cuerpos
    public static final String USER_BIRD = "bird";
    public static final String USER_FLOOR = "floor";
    public static final String USER_PIPE_DOWN = "pipeDown";
    public static final String USER_PIPE_TOP = "pipeTop";
    public static final String USER_COUNTER = "pipeTop";
}
```

### 📄 AssetMan.java

```java
package com.iesfa.flappy.extra;

import static com.iesfa.flappy.extra.Utils.ATLAS_MAP;
import static com.iesfa.flappy.extra.Utils.BACKGROUND_IMAGE;
import static com.iesfa.flappy.extra.Utils.FONT_FNT;
import static com.iesfa.flappy.extra.Utils.FONT_PNG;
import static com.iesfa.flappy.extra.Utils.MUSIC_BG;
import static com.iesfa.flappy.extra.Utils.PIPE_DAWN;
import static com.iesfa.flappy.extra.Utils.PIPE_UP;
import static com.iesfa.flappy.extra.Utils.SOUND_JUMP;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class AssetMan {

    private AssetManager assetManager;
    private TextureAtlas textureAtlas;

    public AssetMan() {
        this.assetManager = new AssetManager();

        assetManager.load(ATLAS_MAP, TextureAtlas.class);
        assetManager.load(SOUND_JUMP, Sound.class);
        assetManager.load(MUSIC_BG, Music.class);
        assetManager.finishLoading();

        textureAtlas = assetManager.get(ATLAS_MAP);
    }

    public TextureRegion getBackground() {
        return this.textureAtlas.findRegion(BACKGROUND_IMAGE);
    }

    public Animation<TextureRegion> getBirdAnimation() {
        return new Animation<TextureRegion>(0.33f,
                textureAtlas.findRegion("bird1"),
                textureAtlas.findRegion("bird2"),
                textureAtlas.findRegion("bird3"));
    }

    public TextureRegion getPipeDownTR() {
        return this.textureAtlas.findRegion(PIPE_DAWN);
    }

    public TextureRegion getPipeUpTR() {
        return this.textureAtlas.findRegion(PIPE_UP);
    }

    public Sound getJumpSound() {
        return this.assetManager.get(SOUND_JUMP);
    }

    public Music getMusicBG() {
        return this.assetManager.get(MUSIC_BG);
    }

    public BitmapFont getFont() {
        return new BitmapFont(
            Gdx.files.internal(FONT_FNT),
            Gdx.files.internal(FONT_PNG),
            false
        );
    }
}
```

### 📄 Bird.java

```java
package com.iesfa.flappy.actors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.iesfa.flappy.extra.Utils;

public class Bird extends Actor {

    public static final int STATE_NORMAL = 0;
    public static final int STATE_DEAD = 1;
    private static final float JUMP_SPEED = 5f;

    public int state;

    private Animation<TextureRegion> birdAnimation;
    private Sound jumpSound;
    private Vector2 position;

    private World world;
    private float stateTime;
    private Body body;
    private Fixture fixture;

    public Bird(World world, Animation<TextureRegion> animation, Sound sound, Vector2 position) {
        this.birdAnimation = animation;
        this.position = position;
        this.world = world;
        this.jumpSound = sound;
        stateTime = 0f;
        state = STATE_NORMAL;

        createBody();
        createFixture();
    }

    public void createBody() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(position);
        bodyDef.type = BodyDef.BodyType.DynamicBody;

        this.body = this.world.createBody(bodyDef);
        this.body.setUserData(Utils.USER_BIRD);
    }

    public void createFixture() {
        CircleShape circle = new CircleShape();
        circle.setRadius(0.25f);

        this.fixture = this.body.createFixture(circle, 3);
        circle.dispose();
    }

    @Override
    public void act(float delta) {
        boolean jump = Gdx.input.justTouched();

        if (jump && this.state == STATE_NORMAL) {
            this.jumpSound.play();
            this.body.setLinearVelocity(0, JUMP_SPEED);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        setPosition(body.getPosition().x - 0.3f, body.getPosition().y - 0.25f);
        batch.draw(this.birdAnimation.getKeyFrame(stateTime, true),
            getX(), getY(), 0.6f, 0.5f);

        stateTime += Gdx.graphics.getDeltaTime();
    }

    public void detach() {
        this.body.destroyFixture(this.fixture);
        this.world.destroyBody(this.body);
    }
}
```

### 📄 Pipes.java

```java
package com.iesfa.flappy.actors;

import static com.iesfa.flappy.extra.Utils.USER_COUNTER;
import static com.iesfa.flappy.extra.Utils.USER_PIPE_DOWN;
import static com.iesfa.flappy.extra.Utils.USER_PIPE_TOP;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class Pipes extends Actor {

    private static final float PIPE_WIDTH = 0.85f;
    private static final float PIPE_HEIGHT = 4f;
    private static final float SPACE_BETWEEN_PIPES = 2f;
    private static final float SPEED = -2f;

    private TextureRegion pipeDownTR;
    private TextureRegion pipeTopTR;

    private Body bodyDown;
    private Body bodyTop;
    private Body bodyCounter;

    private Fixture fixtureDown;
    private Fixture fixtureTop;
    private Fixture fixtureCounter;

    private World world;

    public Pipes(World world, TextureRegion trpDown, TextureRegion trpTop, Vector2 position) {
        this.world = world;
        this.pipeDownTR = trpDown;
        this.pipeTopTR = trpTop;

        createBodyPipeDown(position);
        createBodyPipeTop();
        createFixture();
        createCounter();
    }

    private void createBodyPipeDown(Vector2 position) {
        BodyDef def = new BodyDef();
        def.position.set(position);
        def.type = BodyDef.BodyType.KinematicBody;

        bodyDown = world.createBody(def);
        bodyDown.setUserData(USER_PIPE_DOWN);
        bodyDown.setLinearVelocity(SPEED, 0);
    }

    private void createBodyPipeTop() {
        BodyDef def = new BodyDef();
        def.position.x = bodyDown.getPosition().x;
        def.position.y = bodyDown.getPosition().y + PIPE_HEIGHT + SPACE_BETWEEN_PIPES;

        def.type = BodyDef.BodyType.KinematicBody;
        bodyTop = world.createBody(def);
        bodyTop.setUserData(USER_PIPE_TOP);
        bodyTop.setLinearVelocity(SPEED, 0);
    }

    private void createFixture() {
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(PIPE_WIDTH / 2, PIPE_HEIGHT / 2);

        this.fixtureDown = bodyDown.createFixture(shape, 8);
        this.fixtureTop = bodyTop.createFixture(shape, 8);

        shape.dispose();
    }

    public void createCounter() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(
            bodyDown.getPosition().x,
            (bodyDown.getPosition().y + bodyTop.getPosition().y) / 2f
        );
        bodyDef.type = BodyDef.BodyType.KinematicBody;

        this.bodyCounter = this.world.createBody(bodyDef);
        this.bodyCounter.setLinearVelocity(Pipes.SPEED, 0f);

        PolygonShape polygonShape = new PolygonShape();
        polygonShape.setAsBox(0.1f, 0.90f);

        this.fixtureCounter = bodyCounter.createFixture(polygonShape, 3);
        this.fixtureCounter.setSensor(true);
        this.fixtureCounter.setUserData(USER_COUNTER);
        polygonShape.dispose();
    }

    public boolean isOutOfScreen() {
        return this.bodyDown.getPosition().x <= -2f;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        setPosition(bodyDown.getPosition().x, bodyDown.getPosition().y);
        batch.draw(this.pipeDownTR,
            bodyDown.getPosition().x - PIPE_WIDTH / 2,
            bodyDown.getPosition().y - PIPE_HEIGHT / 2,
            PIPE_WIDTH, PIPE_HEIGHT);

        batch.draw(this.pipeTopTR,
            bodyTop.getPosition().x - PIPE_WIDTH / 2,
            bodyTop.getPosition().y - PIPE_HEIGHT / 2,
            PIPE_WIDTH, PIPE_HEIGHT);
    }

    public void detach() {
        bodyDown.destroyFixture(fixtureDown);
        world.destroyBody(bodyDown);

        bodyTop.destroyFixture(fixtureTop);
        world.destroyBody(bodyTop);

        bodyCounter.destroyFixture(fixtureCounter);
        world.destroyBody(bodyCounter);
    }
}
```

### 📄 GameScreen.java

```java
package com.iesfa.flappy.screens;

import static com.iesfa.flappy.extra.Utils.*;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.iesfa.flappy.MainGame;
import com.iesfa.flappy.actors.Bird;
import com.iesfa.flappy.actors.Pipes;

public class GameScreen extends BaseScreen {

    private final float TIME_TO_SPAWN_PIPES = 1.5f;
    private float timeToCreatePipe;

    private Stage stage;
    private Image background;
    private Bird bird;

    private World world;
    private Music musicbg;

    private Box2DDebugRenderer debugRenderer;
    private OrthographicCamera worldCamera;

    private OrthographicCamera fontCamera;
    private BitmapFont score;

    private Array<Pipes> arrayPipes;

    public GameScreen(MainGame mainGame) {
        super(mainGame);

        this.world = new World(new Vector2(0, -10), true);
        FitViewport fitViewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGTH);
        this.stage = new Stage(fitViewport);

        this.arrayPipes = new Array();
        this.timeToCreatePipe = 0f;

        this.musicbg = this.mainGame.assetManager.getMusicBG();
        this.worldCamera = (OrthographicCamera) this.stage.getCamera();
        this.debugRenderer = new Box2DDebugRenderer();

        prepareScore();
    }

    @Override
    public void show() {
        addBackground();
        addFloor();
        addRoof();
        addBird();

        this.musicbg.setLooping(true);
        this.musicbg.play();
    }

    private void addBird() {
        Animation<TextureRegion> birdSprite = mainGame.assetManager.getBirdAnimation();
        Sound soundBird = this.mainGame.assetManager.getJumpSound();
        this.bird = new Bird(this.world, birdSprite, soundBird, new Vector2(1.35f, 4.75f));
        this.stage.addActor(this.bird);
    }

    private void prepareScore() {
        this.score = this.mainGame.assetManager.getFont();
        this.score.getData().scale(1f);

        this.fontCamera = new OrthographicCamera();
        this.fontCamera.setToOrtho(false, SCREEN_WIDTH, SCREEN_HEIGHT);
        this.fontCamera.update();
    }

    public void addPipes(float delta) {
        TextureRegion pipeDownTexture = mainGame.assetManager.getPipeDownTR();
        TextureRegion pipeTopTexture = mainGame.assetManager.getPipeUpTR();

        if (bird.state == Bird.STATE_NORMAL) {
            this.timeToCreatePipe += delta;

            if (this.timeToCreatePipe >= TIME_TO_SPAWN_PIPES) {
                this.timeToCreatePipe -= TIME_TO_SPAWN_PIPES;

                float posRandomY = MathUtils.random(0f, 2f);
                Pipes pipes = new Pipes(this.world, pipeDownTexture, pipeTopTexture,
                                        new Vector2(5f, posRandomY));
                arrayPipes.add(pipes);
                this.stage.addActor(pipes);
            }
        }
    }

    public void removePipes() {
        for (Pipes pipe : this.arrayPipes) {
            if (!world.isLocked()) {
                if (pipe.isOutOfScreen()) {
                    pipe.detach();
                    pipe.remove();
                    arrayPipes.removeValue(pipe, false);
                }
            }
        }
    }

    public void addRoof() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        Body body = world.createBody(bodyDef);

        EdgeShape edge = new EdgeShape();
        edge.set(0, WORLD_HEIGTH, WORLD_WIDTH, WORLD_HEIGTH);
        body.createFixture(edge, 1);
        edge.dispose();
    }

    private void addFloor() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(WORLD_WIDTH / 2f, 0.6f);
        bodyDef.type = BodyDef.BodyType.StaticBody;
        Body body = world.createBody(bodyDef);
        body.setUserData(USER_FLOOR);

        PolygonShape edge = new PolygonShape();
        edge.setAsBox(2.3f, 0.5f);
        body.createFixture(edge, 3);
        edge.dispose();
    }

    public void addBackground() {
        this.background = new Image(mainGame.assetManager.getBackground());
        this.background.setPosition(0, 0);
        this.background.setSize(WORLD_WIDTH, WORLD_HEIGTH);
        this.stage.addActor(this.background);
    }

    @Override
    public void render(float delta) {

        addPipes(delta);

        this.stage.getBatch().setProjectionMatrix(worldCamera.combined);
        this.stage.act();
        this.world.step(delta, 6, 2);
        this.stage.draw();

        this.worldCamera.update();
        this.debugRenderer.render(this.world, this.worldCamera.combined);

        removePipes();

        this.stage.getBatch().setProjectionMatrix(this.fontCamera.combined);
        this.stage.getBatch().begin();
        this.score.draw(this.stage.getBatch(), "" + arrayPipes.size, SCREEN_WIDTH / 2, 725);
        this.stage.getBatch().end();
    }

    @Override
    public void hide() {
        this.bird.detach();
        this.bird.remove();

        this.musicbg.stop();
    }

    @Override
    public void dispose() {
        this.stage.dispose();
        this.world.dispose();
    }
}
```

### 📄 MainGame.java

```java
package com.iesfa.flappy;

import com.badlogic.gdx.Game;
import com.iesfa.flappy.extra.AssetMan;
import com.iesfa.flappy.screens.GameOverScreen;
import com.iesfa.flappy.screens.GameScreen;
import com.iesfa.flappy.screens.GetReadyScreen;

public class MainGame extends Game {

    public GameScreen gameScreen;
    public GetReadyScreen getReadyScreen;
    public GameOverScreen gameOverScreen;
    public AssetMan assetManager;

    @Override
    public void create() {
        this.assetManager = new AssetMan();

        this.gameScreen = new GameScreen(this);
        this.gameOverScreen = new GameOverScreen(this);
        this.getReadyScreen = new GetReadyScreen(this);

        setScreen(this.gameScreen);
    }
}
```

---

## 🛠️ Ejercicio práctico

**Objetivo:** Entender la generación temporizada, la gestión de memoria y el sistema de doble cámara.

1. **Ejecuta el proyecto.** Deberías ver tuberías apareciendo cada 1.5 segundos con el hueco a alturas diferentes, y un número en pantalla que muestra cuántas tuberías hay activas.

2. **Frecuencia de aparición:** Cambia `TIME_TO_SPAWN_PIPES` de `1.5f` a `0.5f`. ¿Qué pasa? ¿Y con `3f`?

3. **Rango de altura:** Cambia `MathUtils.random(0f, 2f)` a `MathUtils.random(-1f, 4f)`. ¿Aparecen tuberías inalcanzables? ¿Cuál es el rango ideal?

4. **Velocidad y frecuencia:** Si subes `SPEED` a `-4f`, ¿debes ajustar también `TIME_TO_SPAWN_PIPES`?

5. **Posición del texto:** Cambia las coordenadas `(SCREEN_WIDTH / 2, 725)` a `(100, 400)`. ¿Dónde aparece ahora? Recuerda que está en píxeles.

6. **Escala de la fuente:** Prueba `this.score.getData().scale(0.5f)` y `scale(2f)`. ¿Cómo afecta al tamaño del texto?

7. **Pregunta para reflexionar:** `arrayPipes.size` no es la puntuación real del juego (que sería el número de huecos cruzados). ¿Cómo implementarías un contador que sume 1 cada vez que el pájaro cruza un sensor? ¿Qué componente necesitarías que aún no hemos implementado?
