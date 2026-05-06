# 🐦 Construyendo Flappy Bird — Rama `6.Sounds`
## El juego cobra vida con audio y límites del mundo

> *Un juego sin sonido es como una película muda: funciona, pero le falta algo esencial. En esta rama añadimos música de fondo, un efecto de sonido para el salto, y de paso completamos los límites físicos del mundo con un suelo sólido y un techo. El juego empieza a sentirse como un juego de verdad.*

---

## ¿Qué cambia en esta rama?

La rama 5 dejó las tuberías moviéndose con un sensor, pero el mundo no tenía límites físicos y el juego era silencioso. Esta rama añade cuatro cosas:

1. **Sonido de salto**: un efecto `Sound` que suena cada vez que el pájaro salta.
2. **Música de fondo**: un `Music` que se reproduce en bucle durante la partida.
3. **Suelo y techo**: cuerpos estáticos que impiden que el pájaro salga de la pantalla.
4. **Ajuste de `JUMP_SPEED`**: la velocidad del salto se reduce de `50f` a `5f`, un cambio drástico que afecta a toda la jugabilidad.

---

## Paso 1 — `Sound` vs `Music`: dos formas de reproducir audio

### La diferencia fundamental

LibGDX distingue entre dos tipos de audio que se usan para propósitos distintos:

| Característica | `Sound` | `Music` |
|---------------|---------|---------|
| Se carga en... | Memoria RAM (completo) | Se lee del disco (streaming) |
| Ideal para... | Efectos cortos (<10 segundos) | Pistas largas, música de fondo |
| Latencia | Muy baja (inmediato) | Mayor (necesita buffering) |
| Memoria | Ocupa RAM proporcional al tamaño | Casi nada en RAM |
| Instancias simultáneas | Múltiples a la vez | Una por archivo |

### Analogía: disco de vinilo vs Spotify

Un `Sound` es como tener un disco de vinilo en casa: lo cargas una vez en el tocadiscos (memoria) y puedes reproducirlo al instante cuantas veces quieras. Es rápido pero ocupa espacio físico. Un `Music` es como Spotify: no almacena la canción entera, la va descargando poco a poco (streaming desde disco), así que no ocupa casi memoria, pero tarda un instante en empezar.

Para el salto necesitamos latencia cero — el sonido debe sonar en el frame exacto del toque. Por eso usamos `Sound`. Para la música de fondo, que suena continuamente y es un archivo largo, usamos `Music`.

---

## Paso 2 — Cargar audio con `AssetManager`

### Registrar los recursos

En el constructor de `AssetMan`, se registran los dos archivos de audio para que el `AssetManager` los cargue:

```java
public AssetMan() {
    this.assetManager = new AssetManager();

    assetManager.load(ATLAS_MAP, TextureAtlas.class);
    assetManager.load(SOUND_JUMP, Sound.class);    // ← NUEVO
    assetManager.load(MUSIC_BG, Music.class);      // ← NUEVO
    assetManager.finishLoading();

    this.textureAtlas = assetManager.get(ATLAS_MAP);
}
```

Fíjate en que los tres `load()` se llaman **antes** de `finishLoading()`. El `AssetManager` los encola todos y los carga en un solo paso. Si pusieras un `finishLoading()` después de cada `load()`, funcionaría pero sería menos eficiente — estarías forzando tres cargas secuenciales en lugar de una batch.

### ¿Dónde deben estar los archivos?

Los archivos de audio deben estar en la carpeta `android/assets/`, al mismo nivel que el atlas:

```
android/assets/
├── FBAtlas.png
├── FBAtlas.atlas
├── jump.mp3          ← efecto de salto
└── musicbg.mp3       ← música de fondo
```

LibGDX busca los recursos en esta carpeta por defecto, tanto en la versión Android como en la de escritorio.

### Métodos de acceso

```java
public Sound getJumpSound() {
    return this.assetManager.get(SOUND_JUMP);
}

public Music getMusicBG() {
    return this.assetManager.get(MUSIC_BG);
}
```

El `AssetManager` usa genéricos internamente: `assetManager.get(SOUND_JUMP)` devuelve un `Sound` porque fue registrado con `Sound.class`. Si intentaras hacer `assetManager.get(SOUND_JUMP, Music.class)`, lanzaría una excepción porque el tipo no coincide.

### Nuevas constantes en `Utils`

```java
public static final String SOUND_JUMP = "jump.mp3";
public static final String MUSIC_BG = "musicbg.mp3";
```

Seguimos el mismo patrón que con las texturas: los nombres de archivo como constantes en `Utils` para evitar strings dispersos por el código.

---

## Paso 3 — El sonido del salto en `Bird`

### Recibir el `Sound` por constructor

El sonido de salto se pasa al pájaro como parámetro del constructor. El pájaro no debería saber *de dónde* viene el sonido — eso es responsabilidad de `GameScreen`:

```java
private Sound jumpSound;

public Bird(World world, Animation<TextureRegion> animation, Sound sound, Vector2 position) {
    this.birdAnimation = animation;
    this.position = position;
    this.world = world;
    this.stateTime = 0f;
    this.state = STATE_NORMAL;
    this.jumpSound = sound;       // ← NUEVO

    createBody();
    createFixture();
}
```

### Reproducir al saltar

En `act()`, el sonido se reproduce **justo antes** de aplicar la velocidad:

```java
@Override
public void act(float delta) {
    boolean jump = Gdx.input.justTouched();

    if (jump && this.state == STATE_NORMAL) {
        this.jumpSound.play();                      // ← NUEVO
        this.body.setLinearVelocity(0, JUMP_SPEED);
    }
}
```

`sound.play()` es no bloqueante: dispara la reproducción y continúa inmediatamente. No detiene el juego mientras suena. Puedes llamarlo múltiples veces seguidas y cada llamada creará una **nueva instancia** del sonido, así que si el jugador toca muy rápido, se oirán varios sonidos superpuestos.

### El cambio silencioso: `JUMP_SPEED` baja de `50f` a `5f`

```java
private static final float JUMP_SPEED = 5f;  // Antes era 50f
```

Este cambio no tiene relación directa con el audio, pero ocurre en esta rama. La velocidad del salto se reduce **diez veces**. ¿Por qué? Con `50f`, el pájaro salía disparado fuera de la pantalla. Con `5f` y la gravedad a `-10`, el salto produce un arco pequeño y controlable, mucho más parecido al Flappy Bird original.

La relación entre gravedad y velocidad de salto determina la "sensación" del juego:

```
Gravedad = -10,  JUMP_SPEED = 50  →  Salto exagerado, difícil de controlar
Gravedad = -10,  JUMP_SPEED = 5   →  Salto corto, control preciso ✓
Gravedad = -10,  JUMP_SPEED = 2   →  Salto mínimo, demasiado difícil
```

---

## Paso 4 — La música de fondo en `GameScreen`

### Obtener y almacenar la referencia

La música se obtiene en el constructor de `GameScreen`:

```java
private Music musicbg;

public GameScreen(MainGame mainGame) {
    super(mainGame);

    this.world = new World(new Vector2(0, -10), true);
    FitViewport fitViewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
    this.stage = new Stage(fitViewport);

    this.musicbg = this.mainGame.assetManager.getMusicBG();  // ← NUEVO
    this.ortCamera = (OrthographicCamera) this.stage.getCamera();
    this.debugRenderer = new Box2DDebugRenderer();
}
```

### Iniciar con loop en `show()`

```java
@Override
public void show() {
    addBackground();
    addFloor();
    addRoof();
    addBird();

    // ... creación de tuberías ...

    this.musicbg.setLooping(true);   // ← NUEVO: repetir indefinidamente
    this.musicbg.play();             // ← NUEVO: comenzar reproducción
}
```

`setLooping(true)` hace que la pista se reinicie automáticamente al terminar. Sin esta línea, la música sonaría una sola vez y se detendría. En un juego como Flappy Bird, donde las partidas pueden durar más que la pista musical, el loop es esencial.

### Detener en `hide()`

```java
@Override
public void hide() {
    this.bird.detach();
    this.bird.remove();

    this.pipes.detach();
    this.pipes.remove();

    this.musicbg.stop();   // ← NUEVO: detener la música al salir
}
```

Si no detienes la música en `hide()`, seguiría sonando incluso después de cambiar a otra pantalla (por ejemplo, `GameOverScreen`). Cada pantalla es responsable de gestionar su propia música.

### `play()` vs `stop()` vs `pause()`

| Método | Efecto |
|--------|--------|
| `play()` | Inicia reproducción desde el principio (o reanuda si estaba en pausa) |
| `pause()` | Pausa la reproducción, mantiene la posición actual |
| `stop()` | Detiene la reproducción, vuelve al principio |

Si usaras `pause()` en lugar de `stop()`, al volver a `show()` y llamar `play()`, la música continuaría desde donde se quedó en vez de empezar de nuevo. Para este juego usamos `stop()` porque cada partida debe empezar con la música desde el inicio.

---

## Paso 5 — El suelo: de `EdgeShape` a `PolygonShape`

### Un suelo con volumen

En la rama 3, el suelo era una línea invisible (`EdgeShape`). Esta rama cambia a un `PolygonShape` con posición y tamaño definidos:

```java
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
```

Analicemos las dimensiones:

**Posición del centro**: `(WORLD_WIDTH / 2f, 0.6f)` = `(2.4, 0.6)`. El suelo está centrado horizontalmente y ligeramente elevado respecto al borde inferior de la pantalla.

**Tamaño**: `setAsBox(2.3f, 0.5f)` crea un rectángulo de `4.6 × 1.0` unidades (recuerda: `setAsBox` recibe mitades). Esto cubre casi todo el ancho del mundo (`WORLD_WIDTH = 4.8`), dejando un margen mínimo a los lados.

```
WORLD_WIDTH = 4.8
                    
  ┌──────────────────────────────────────┐  y = 1.1 (0.6 + 0.5)
  │           SUELO (4.6 × 1.0)         │
  │         centro en (2.4, 0.6)        │
  └──────────────────────────────────────┘  y = 0.1 (0.6 - 0.5)
  ↑ 0.1                              4.7 ↑
```

### ¿Por qué un rectángulo y no una línea?

Un `EdgeShape` es infinitamente fino — es una línea sin grosor. Un `PolygonShape` tiene volumen real, lo que produce colisiones más predecibles. Cuando el pájaro cae sobre un rectángulo sólido, la respuesta física es más estable que cuando cae sobre una línea infinitamente fina.

Además, el suelo con volumen permite que su borde superior esté ligeramente por encima de `y=0`, lo que da la impresión visual de que el pájaro aterriza "sobre" algo en lugar de quedarse en el borde exacto de la pantalla.

### La constante `USER_FLOOR`

Se añade en `Utils`:

```java
public static final String USER_FLOOR = "floor";
```

Esto permitirá identificar el suelo en el `ContactListener` de ramas futuras. Si el pájaro toca el suelo, es game over.

---

## Paso 6 — El techo con `EdgeShape`

A diferencia del suelo, el techo sigue siendo una línea simple:

```java
public void addRoof() {
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = BodyDef.BodyType.StaticBody;
    Body body = world.createBody(bodyDef);

    EdgeShape edge = new EdgeShape();
    edge.set(0, WORLD_HEIGHT, WORLD_WIDTH, WORLD_HEIGHT);
    body.createFixture(edge, 1);
    edge.dispose();
}
```

El techo es una línea horizontal que va de `(0, 8)` a `(4.8, 8)` — el borde superior del mundo. A diferencia del suelo, el techo no necesita volumen: solo impide que el pájaro salga por arriba.

Observa que el techo **no tiene `userData`**. Esto significa que si el pájaro toca el techo, el `ContactListener` no podrá identificar ese cuerpo por su userData. En esta implementación, chocar con el techo no causa game over — simplemente rebota o se detiene.

---

## Paso 7 — El orden en `show()` y la creación del Bird

### `addBird()` actualizado

El pájaro ahora recibe el sonido de salto como parámetro adicional:

```java
public void addBird() {
    Animation<TextureRegion> birdSprite = mainGame.assetManager.getBirdAnimation();
    Sound sound = mainGame.assetManager.getJumpSound();
    this.bird = new Bird(this.world, birdSprite, sound, new Vector2(1f, 4f));
    this.stage.addActor(this.bird);
}
```

### El orden de `show()`

```java
@Override
public void show() {
    addBackground();    // 1. Fondo (capa más profunda)
    addFloor();         // 2. Suelo físico (sin visual propia)
    addRoof();          // 3. Techo físico (sin visual propia)
    addBird();          // 4. Pájaro (visible)

    // 5. Tuberías (visible)
    TextureRegion pipeTRDown = mainGame.assetManager.getPipeDownTR();
    TextureRegion pipeTRTop = mainGame.assetManager.getPipeTopTR();
    this.pipes = new Pipes(this.world, pipeTRDown, pipeTRTop, new Vector2(3.75f, 0f));
    this.stage.addActor(this.pipes);

    // 6. Música
    this.musicbg.setLooping(true);
    this.musicbg.play();
}
```

`addFloor()` y `addRoof()` crean bodies en el mundo pero no actores en el Stage — son puramente físicos, sin representación visual. Solo los verás con el `Box2DDebugRenderer` activado.

---

## Paso 8 — Detalle: el typo `SCREEN_HEIGTH`

En esta rama, `Utils` introduce un typo que se mantiene en el proyecto:

```java
public static final int SCREEN_HEIGTH = 800;   // ← "HEIGTH" en vez de "HEIGHT"
```

Y en `DesktopLauncher`:

```java
config.setWindowedMode(Utils.SCREEN_WIDTH, Utils.SCREEN_HEIGTH);
```

Fíjate en que `WORLD_HEIGHT` sigue bien escrito pero `SCREEN_HEIGTH` tiene el error. Este tipo de inconsistencias son comunes en proyectos reales. Lo importante es ser consistente: si usas el nombre con typo en un sitio, debes usarlo igual en todos los demás, o renombrarlo con el refactoring del IDE.

---

## Errores comunes en esta rama

### 1. "No se oye el sonido de salto"

Comprueba que:
- El archivo `jump.mp3` existe en `android/assets/`.
- El nombre en `Utils.SOUND_JUMP` coincide exactamente con el nombre del archivo (case-sensitive).
- Has añadido `assetManager.load(SOUND_JUMP, Sound.class)` **antes** de `finishLoading()`.

### 2. "La música no se repite"

```java
// ❌ MAL: olvidar setLooping
this.musicbg.play();

// ✓ BIEN: activar loop antes de play
this.musicbg.setLooping(true);
this.musicbg.play();
```

### 3. "La música sigue sonando en la pantalla de Game Over"

Asegúrate de que `hide()` incluye `this.musicbg.stop()`. Sin esto, la música continúa al cambiar de pantalla.

### 4. "El pájaro apenas salta"

`JUMP_SPEED` bajó de `50f` a `5f`. Si el salto parece demasiado débil, asegúrate de que la gravedad del mundo es `-10` y no un valor más alto. La combinación `JUMP_SPEED = 5` + gravedad `-10` produce un arco moderado.

### 5. "FileNotFoundException al cargar audio"

Los archivos de audio deben estar directamente en `android/assets/`, no en una subcarpeta. Si los tienes en `android/assets/sounds/`, deberías usar `"sounds/jump.mp3"` como ruta o moverlos al directorio raíz.

---

## Código completo de esta rama

### 📄 Utils.java

```java
package com.mygdx.game.extra;

public class Utils {

    public static final int SCREEN_HEIGTH = 800;
    public static final int SCREEN_WIDTH = 480;

    public static final float WORLD_HEIGHT = 8f;
    public static final float WORLD_WIDTH = 4.8f;

    // Identificadores de texturas y audio
    public static final String ATLAS_MAP = "FBAtlas";
    public static final String BACKGROUND_IMAGE = "flappy_background";
    public static final String BIRD1 = "bird1";
    public static final String BIRD2 = "bird2";
    public static final String BIRD3 = "bird3";
    public static final String PIPE_DOWN = "pipeDown";
    public static final String PIPE_UP = "pipeUp";
    public static final String SOUND_JUMP = "jump.mp3";
    public static final String MUSIC_BG = "musicbg.mp3";

    // Identificadores de cuerpos
    public static final String USER_BIRD = "bird";
    public static final String USER_PIPE_DOWN = "pipeDown";
    public static final String USER_PIPE_UP = "pipeUp";
    public static final String USER_COUNTER = "counter";
    public static final String USER_FLOOR = "floor";
}
```

### 📄 AssetMan.java

```java
package com.mygdx.game.extra;

import static com.mygdx.game.extra.Utils.ATLAS_MAP;
import static com.mygdx.game.extra.Utils.BACKGROUND_IMAGE;
import static com.mygdx.game.extra.Utils.BIRD1;
import static com.mygdx.game.extra.Utils.BIRD2;
import static com.mygdx.game.extra.Utils.BIRD3;
import static com.mygdx.game.extra.Utils.MUSIC_BG;
import static com.mygdx.game.extra.Utils.PIPE_DOWN;
import static com.mygdx.game.extra.Utils.PIPE_UP;
import static com.mygdx.game.extra.Utils.SOUND_JUMP;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Animation;
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

        this.textureAtlas = assetManager.get(ATLAS_MAP);
    }

    // IMAGEN DE FONDO
    public TextureRegion getBackground() {
        return this.textureAtlas.findRegion(BACKGROUND_IMAGE);
    }

    // ANIMACIÓN PÁJARO
    public Animation<TextureRegion> getBirdAnimation() {
        return new Animation<TextureRegion>(0.33f,
                textureAtlas.findRegion(BIRD1),
                textureAtlas.findRegion(BIRD2),
                textureAtlas.findRegion(BIRD3));
    }

    // TEXTURAS DE TUBERÍAS
    public TextureRegion getPipeDownTR() {
        return this.textureAtlas.findRegion(PIPE_DOWN);
    }

    public TextureRegion getPipeTopTR() {
        return this.textureAtlas.findRegion(PIPE_UP);
    }

    // AUDIO
    public Sound getJumpSound() {
        return this.assetManager.get(SOUND_JUMP);
    }

    public Music getMusicBG() {
        return this.assetManager.get(MUSIC_BG);
    }
}
```

### 📄 Bird.java

```java
package com.mygdx.game.actors;

import static com.mygdx.game.extra.Utils.USER_BIRD;

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

public class Bird extends Actor {

    private static final int STATE_NORMAL = 0;
    private static final int STATE_DEAD = 1;
    private static final float JUMP_SPEED = 5f;

    private int state;

    private Animation<TextureRegion> birdAnimation;
    private Vector2 position;

    private float stateTime;

    private World world;
    private Body body;
    private Fixture fixture;

    private Sound jumpSound;

    public Bird(World world, Animation<TextureRegion> animation, Sound sound, Vector2 position) {
        this.birdAnimation = animation;
        this.position = position;
        this.world = world;
        this.stateTime = 0f;
        this.state = STATE_NORMAL;
        this.jumpSound = sound;

        createBody();
        createFixture();
    }

    private void createBody() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(this.position);
        bodyDef.type = BodyDef.BodyType.DynamicBody;

        this.body = this.world.createBody(bodyDef);
    }

    private void createFixture() {
        CircleShape circle = new CircleShape();
        circle.setRadius(0.30f);

        this.fixture = this.body.createFixture(circle, 8);
        this.fixture.setUserData(USER_BIRD);

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
        setPosition(body.getPosition().x - 0.4f, body.getPosition().y - 0.25f);
        batch.draw(this.birdAnimation.getKeyFrame(stateTime, true),
            getX(), getY(), 0.8f, 0.5f);

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
package com.mygdx.game.actors;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.mygdx.game.extra.Utils;

public class Pipes extends Actor {

    private static final float PIPE_WIDTH = 1f;
    private static final float PIPE_HEIGHT = 4f;
    private static final float COUNTER_HEIGHT = 2f;
    private static final float SPEED = -0.2f;

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
        createCounter();
        createFixture();
    }

    private void createBodyPipeDown(Vector2 position) {
        BodyDef def = new BodyDef();
        def.position.set(position);
        def.type = BodyDef.BodyType.KinematicBody;

        bodyDown = world.createBody(def);
        bodyDown.setUserData(Utils.USER_PIPE_DOWN);
        bodyDown.setLinearVelocity(SPEED, 0);
    }

    private void createBodyPipeTop() {
        BodyDef def = new BodyDef();
        def.position.x = bodyDown.getPosition().x;
        def.position.y = bodyDown.getPosition().y + PIPE_HEIGHT + COUNTER_HEIGHT;

        def.type = BodyDef.BodyType.KinematicBody;
        bodyTop = world.createBody(def);
        bodyTop.setUserData(Utils.USER_PIPE_UP);
        bodyTop.setLinearVelocity(SPEED, 0);
    }

    public void createCounter() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.x = this.bodyDown.getPosition().x;
        bodyDef.position.y = (this.bodyDown.getPosition().y + this.bodyTop.getPosition().y) / 2f;
        bodyDef.type = BodyDef.BodyType.KinematicBody;

        this.bodyCounter = this.world.createBody(bodyDef);
        this.bodyCounter.setLinearVelocity(SPEED, 0);

        PolygonShape polygonShape = new PolygonShape();
        polygonShape.setAsBox(0.1f, 0.90f);

        this.fixtureCounter = bodyCounter.createFixture(polygonShape, 3);
        this.fixtureCounter.setSensor(true);
        this.fixtureCounter.setUserData(Utils.USER_COUNTER);
        polygonShape.dispose();
    }

    private void createFixture() {
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(PIPE_WIDTH / 2, PIPE_HEIGHT / 2);

        this.fixtureDown = bodyDown.createFixture(shape, 8);
        this.fixtureTop = bodyTop.createFixture(shape, 8);

        shape.dispose();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        setPosition(
            this.bodyDown.getPosition().x - (PIPE_WIDTH / 2),
            this.bodyDown.getPosition().y - (PIPE_HEIGHT / 2)
        );
        batch.draw(this.pipeDownTR, getX(), getY(), PIPE_WIDTH, PIPE_HEIGHT);

        setPosition(
            this.bodyTop.getPosition().x - (PIPE_WIDTH / 2),
            this.bodyTop.getPosition().y - (PIPE_HEIGHT / 2)
        );
        batch.draw(this.pipeTopTR, getX(), getY(), PIPE_WIDTH, PIPE_HEIGHT);
    }

    public void detach() {
        bodyDown.destroyFixture(fixtureDown);
        world.destroyBody(bodyDown);

        this.bodyTop.destroyFixture(fixtureTop);
        this.world.destroyBody(this.bodyTop);
    }
}
```

### 📄 GameScreen.java

```java
package com.mygdx.game.screens;

import static com.mygdx.game.extra.Utils.USER_FLOOR;
import static com.mygdx.game.extra.Utils.WORLD_HEIGHT;
import static com.mygdx.game.extra.Utils.WORLD_WIDTH;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.game.MainGame;
import com.mygdx.game.actors.Bird;
import com.mygdx.game.actors.Pipes;

public class GameScreen extends BaseScreen {

    private Stage stage;
    private Bird bird;

    private Image background;

    private World world;

    private Pipes pipes;

    private Music musicbg;

    private Box2DDebugRenderer debugRenderer;
    private OrthographicCamera ortCamera;

    public GameScreen(MainGame mainGame) {
        super(mainGame);

        this.world = new World(new Vector2(0, -10), true);
        FitViewport fitViewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
        this.stage = new Stage(fitViewport);

        this.musicbg = this.mainGame.assetManager.getMusicBG();
        this.ortCamera = (OrthographicCamera) this.stage.getCamera();
        this.debugRenderer = new Box2DDebugRenderer();
    }

    public void addBackground() {
        this.background = new Image(mainGame.assetManager.getBackground());
        this.background.setPosition(0, 0);
        this.background.setSize(WORLD_WIDTH, WORLD_HEIGHT);
        this.stage.addActor(this.background);
    }

    public void addBird() {
        Animation<TextureRegion> birdSprite = mainGame.assetManager.getBirdAnimation();
        Sound sound = mainGame.assetManager.getJumpSound();
        this.bird = new Bird(this.world, birdSprite, sound, new Vector2(1f, 4f));
        this.stage.addActor(this.bird);
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

    public void addRoof() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        Body body = world.createBody(bodyDef);

        EdgeShape edge = new EdgeShape();
        edge.set(0, WORLD_HEIGHT, WORLD_WIDTH, WORLD_HEIGHT);
        body.createFixture(edge, 1);
        edge.dispose();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        this.stage.act();
        this.world.step(delta, 6, 2);
        this.stage.draw();

        this.debugRenderer.render(this.world, this.ortCamera.combined);
    }

    @Override
    public void show() {
        addBackground();
        addFloor();
        addRoof();
        addBird();

        TextureRegion pipeTRDown = mainGame.assetManager.getPipeDownTR();
        TextureRegion pipeTRTop = mainGame.assetManager.getPipeTopTR();
        this.pipes = new Pipes(this.world, pipeTRDown, pipeTRTop, new Vector2(3.75f, 0f));
        this.stage.addActor(this.pipes);

        this.musicbg.setLooping(true);
        this.musicbg.play();
    }

    @Override
    public void hide() {
        this.bird.detach();
        this.bird.remove();

        this.pipes.detach();
        this.pipes.remove();

        this.musicbg.stop();
    }

    @Override
    public void dispose() {
        this.stage.dispose();
        this.world.dispose();
    }
}
```

### 📄 DesktopLauncher.java

```java
package com.mygdx.game;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mygdx.game.extra.Utils;

public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

        config.setWindowedMode(Utils.SCREEN_WIDTH, Utils.SCREEN_HEIGTH);
        config.setForegroundFPS(60);
        config.setTitle("FB2223");
        new Lwjgl3Application(new MainGame(), config);
    }
}
```

---

## 🛠️ Ejercicio práctico

**Objetivo:** Entender el sistema de audio de LibGDX y la relación entre los parámetros físicos.

1. **Ejecuta el proyecto.** Deberías oír la música de fondo en bucle y el sonido del salto al tocar la pantalla. El pájaro ahora salta mucho menos que antes (JUMP_SPEED = 5).

2. **Silencia el salto:** Comenta la línea `this.jumpSound.play()` en `Bird.act()`. ¿Notas la diferencia en la experiencia de juego? El feedback auditivo es más importante de lo que parece.

3. **Experimenta con el volumen:** `Sound.play()` devuelve un `long` (el ID de la instancia). Usa la versión con volumen: `this.jumpSound.play(0.5f)` para reproducir al 50%. ¿Cuál suena mejor?

4. **Prueba `pause()` vs `stop()`:** En `hide()`, cambia `this.musicbg.stop()` por `this.musicbg.pause()`. Si el juego vuelve a llamar `show()`, ¿la música empieza desde el principio o continúa?

5. **Ajusta el salto:** Prueba `JUMP_SPEED` con valores `3f`, `5f`, `8f` y `12f`. ¿Cuál combina mejor con la velocidad de las tuberías (`SPEED = -0.2f`)? ¿Y si subes `SPEED` a `-1f`?

6. **El suelo visible:** El suelo es un rectángulo de `4.6 × 1.0` centrado en `(2.4, 0.6)`. Con el debugRenderer, verifica que el pájaro aterriza sobre él. ¿Qué pasa si cambias la posición y del suelo a `0.0f`?

7. **Pregunta para reflexionar:** ¿Por qué el `Sound` se pasa al `Bird` por constructor en lugar de que `Bird` lo cargue él mismo con `Gdx.audio.newSound()`? Piensa en términos de responsabilidad y reutilización.
