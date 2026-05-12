# 🐦 Construyendo Flappy Bird — Rama `8.Collisions`
## El juego detecta colisiones, cuenta puntos y cambia de pantalla

> *Las tuberías se mueven y el pájaro cae, pero ninguno sabe que existe el otro. En esta rama conectamos el motor físico con la lógica del juego: cuando el pájaro toca una tubería, muere; cuando cruza el sensor, suma un punto. Es el momento en que el juego realmente empieza a ser un juego.*

---

## ¿Qué cambia en esta rama?

La rama 7 tenía tuberías en movimiento y una "puntuación" falsa basada en cuántas tuberías había activas. Esta rama añade la lógica real:

1. **`ContactListener`**: interfaz de Box2D que notifica cuando dos fixtures se tocan.
2. **Puntuación real**: `scoreNumber` se incrementa cuando el pájaro cruza el sensor.
3. **Muerte del pájaro**: al tocar una tubería, el suelo o el techo, el juego se detiene.
4. **Parada de tuberías**: `stopPipes()` congela todos los pares activos al morir.
5. **Transición de pantalla**: tras 1.5 segundos de gracia, el juego pasa a `GameOverScreen`.
6. **Corrección del `userData`**: los identificadores se mueven a las fixtures (no los bodies) para que el `ContactListener` pueda distinguirlos. También se corrige el bug de `USER_COUNTER` de la rama anterior.

---

## Paso 1 — El `ContactListener`: escuchar los choques del mundo físico

### ¿Qué es y cómo funciona?

Hasta ahora Box2D simulaba físicas — los cuerpos colisionaban — pero el juego no se enteraba. Para recibir notificaciones de colisión, Box2D ofrece la interfaz `ContactListener`. Tiene cuatro métodos:

| Método | Cuándo se llama |
|--------|----------------|
| `beginContact(Contact)` | Al inicio del contacto, cuando dos fixtures se tocan por primera vez |
| `endContact(Contact)` | Cuando las dos fixtures se separan |
| `preSolve(Contact, Manifold)` | Justo antes de que Box2D calcule la respuesta física |
| `postSolve(Contact, ContactImpulse)` | Justo después de calcular la respuesta |

En este proyecto solo usamos `beginContact()`. Los otros tres se implementan vacíos para cumplir el contrato de la interfaz.

### Analogía: un árbitro de fútbol

El `ContactListener` es como el árbitro de un partido: observa constantemente el campo y cuando dos jugadores entran en contacto, pita. Box2D es el campo donde ocurre el juego, los bodies son los jugadores, y el árbitro (ContactListener) decide qué consecuencias tiene cada contacto: si es un gol (sensor = punto), una falta (tubería = muerte) o irrelevante (el pájaro rozando el borde del hueco sin consecuencias).

### Implementación

`GameScreen` implementa la interfaz directamente:

```java
public class GameScreen extends BaseScreen implements ContactListener {
    // ...
}
```

Y en el constructor, se registra como oyente del mundo físico:

```java
this.world = new World(new Vector2(0, -10), true);
this.world.setContactListener(this);   // ← registrar el listener
```

A partir de este momento, cada vez que dos fixtures del mundo entren en contacto, Box2D llamará a `beginContact()` de `GameScreen`.

---

## Paso 2 — El problema: ¿cómo saber qué ha chocado con qué?

### El objeto `Contact`

`beginContact(Contact contact)` recibe un objeto `Contact` que tiene dos métodos clave:

```java
contact.getFixtureA()   // primera fixture del contacto
contact.getFixtureB()   // segunda fixture del contacto
```

Pero Box2D no garantiza el orden: en un contacto entre el pájaro y la tubería inferior, `fixtureA` podría ser el pájaro o la tubería, dependiendo del frame. Tenemos que comprobar ambas combinaciones.

### El método auxiliar `areColider()`

Para hacer esta comprobación legible, `GameScreen` introduce un método helper:

```java
public boolean areColider(Contact contact, Object objA, Object objB) {
    return (contact.getFixtureA().getUserData().equals(objA) &&
            contact.getFixtureB().getUserData().equals(objB))
        ||
           (contact.getFixtureA().getUserData().equals(objB) &&
            contact.getFixtureB().getUserData().equals(objA));
}
```

Comprueba si las dos fixtures tienen exactamente los `userData` indicados, en cualquier orden. Así se puede preguntar con claridad:

```java
areColider(contact, USER_BIRD, USER_COUNTER)    // ¿ha cruzado el pájaro el sensor?
areColider(contact, USER_BIRD, USER_PIPE_DOWN)  // ¿ha tocado la tubería inferior?
```

---

## Paso 3 — `userData` en las fixtures: el cambio crítico

### ¿Por qué el cambio?

`areColider()` llama a `contact.getFixtureA().getUserData()`. Esto significa que el identificador debe estar en la **fixture**, no en el body. En la rama 7, algunos `userData` estaban en los bodies (que `getFixtureA()` no devuelve). En esta rama se reorganiza todo:

**Antes (rama 7):**
```java
// En Pipes — userData en el body
bodyDown.setUserData(USER_PIPE_DOWN);
bodyTop.setUserData(USER_PIPE_TOP);

// El counter tenía userData en la fixture (correcto)
this.fixtureCounter.setUserData(USER_COUNTER);
```

**Ahora (rama 8):**
```java
// En Pipes — userData en las FIXTURES
this.fixtureDown.setUserData(USER_PIPE_DOWN);
this.fixtureTop.setUserData(USER_PIPE_TOP);
this.fixtureCounter.setUserData(USER_COUNTER);

// Los bodies ya NO tienen setUserData
```

```java
// En Bird — userData en la FIXTURE
this.fixture.setUserData(Utils.USER_BIRD);

// El body ya NO tiene setUserData
```

```java
// En GameScreen — addFloor y addRoof también asignan userData a la fixture
body.createFixture(edge, 3).setUserData(USER_FLOOR);   // suelo
body.createFixture(edge, 1).setUserData(USER_ROOF);    // techo
```

La regla es consistente: **todos los `userData` están en fixtures** en esta rama.

### Corrección del bug `USER_COUNTER`

En la rama 7, `Utils` tenía un error:

```java
// Rama 7 — BUG:
public static final String USER_COUNTER = "pipeTop";  // ← igual que USER_PIPE_TOP!
```

Esto hacía imposible distinguir entre el sensor y la tubería superior. En esta rama se corrige:

```java
// Rama 8 — CORRECTO:
public static final String USER_COUNTER = "counter";   // ← valor único
```

Además se añade `USER_ROOF`:

```java
public static final String USER_ROOF = "roof";
```

---

## Paso 4 — `beginContact()`: la lógica del juego

Este es el método central de la rama. Toda la lógica de colisiones vive aquí:

```java
@Override
public void beginContact(Contact contact) {

    if (areColider(contact, USER_BIRD, USER_COUNTER)) {
        // El pájaro ha cruzado el sensor → punto
        this.scoreNumber++;

    } else {
        // El pájaro ha tocado cualquier otra cosa → muerte
        this.bird.hurt();

        for (Pipes pipe : this.arrayPipes) {
            pipe.stopPipes();
        }

        this.musicbg.stop();

        this.stage.addAction(Actions.sequence(
            Actions.delay(1.5f),
            Actions.run(new Runnable() {
                @Override
                public void run() {
                    mainGame.setScreen(mainGame.gameOverScreen);
                }
            })
        ));
    }
}
```

### 4.1 — Sumar punto al cruzar el sensor

```java
if (areColider(contact, USER_BIRD, USER_COUNTER)) {
    this.scoreNumber++;
}
```

`scoreNumber` es ahora un contador real. Cuando el pájaro (fixture con `USER_BIRD`) toca el sensor (fixture con `USER_COUNTER`), se incrementa. El score se muestra en pantalla en `render()`:

```java
this.score.draw(this.stage.getBatch(), "" + this.scoreNumber, SCREEN_WIDTH / 2, 725);
```

### 4.2 — Muerte por cualquier otro contacto

Si el contacto no es pájaro+sensor, es pájaro+algo_letal (tubería, suelo, techo). Las consecuencias se ejecutan en cuatro pasos:

**`bird.hurt()`**: Cambia el estado del pájaro a `STATE_DEAD` y resetea `stateTime`:

```java
// En Bird.java:
public void hurt() {
    this.state = STATE_DEAD;
    this.stateTime = 0;
}
```

Resetear `stateTime` reinicia la animación del pájaro desde el frame 0. A partir de ahora, `act()` ya no procesará input porque la condición `this.state == STATE_NORMAL` es falsa, y `addPipes()` en `GameScreen` tampoco creará nuevas tuberías.

**`pipe.stopPipes()`**: Congela los tres bodies de cada par de tuberías:

```java
// En Pipes.java:
public void stopPipes() {
    this.bodyDown.setLinearVelocity(0, 0);
    this.bodyTop.setLinearVelocity(0, 0);
    this.bodyCounter.setLinearVelocity(0, 0);
}
```

Las tuberías se detienen en seco. Esto da al jugador la sensación de que el tiempo se para al morir — un efecto de feedback inmediato antes de ir a la pantalla de game over.

**`musicbg.stop()`**: La música se detiene en el momento del impacto.

**`Actions.sequence()`**: Espera 1.5 segundos y después cambia de pantalla.

---

## Paso 5 — `Actions`: animaciones del Stage sin gestión manual de tiempo

### ¿Qué son los Actions?

En LibGDX, los `Actions` son tareas que el Stage ejecuta automáticamente en cada `act()`. Son como instrucciones programadas: "espera 1.5 segundos, después haz esto". Sin Actions, tendríamos que gestionar un temporizador manual similar al de las tuberías.

### `Actions.sequence()`: encadenar acciones

`Actions.sequence()` ejecuta sus argumentos uno tras otro:

```java
Actions.sequence(
    Actions.delay(1.5f),          // esperar 1.5 segundos
    Actions.run(new Runnable() {  // después ejecutar este código
        @Override
        public void run() {
            mainGame.setScreen(mainGame.gameOverScreen);
        }
    })
)
```

**`Actions.delay(1.5f)`**: pausa de 1.5 segundos. Durante este tiempo el juego sigue renderizando (el pájaro cae sobre el suelo, las tuberías están quietas) pero no ocurre nada nuevo.

**`Actions.run(Runnable)`**: ejecuta código arbitrario. Aquí se usa para cambiar de pantalla. La clase anónima `Runnable` es un patrón de Java para pasar código como parámetro — el equivalente a una lambda en versiones modernas.

### ¿Por qué `stage.addAction()` y no `bird.addAction()`?

La acción se añade al **Stage** entero, no a un Actor concreto. Si se añadiera al pájaro y este fuera eliminado del Stage, la acción se cancelaría. Al añadirla al Stage, se garantiza que se ejecuta aunque el pájaro sea eliminado.

---

## Paso 6 — El nuevo `scoreNumber` en `prepareScore()`

```java
private void prepareScore() {
    this.scoreNumber = 0;                         // ← inicializar a 0
    this.score = this.mainGame.assetManager.getFont();
    this.score.getData().scale(1f);

    this.fontCamera = new OrthographicCamera();
    this.fontCamera.setToOrtho(false, SCREEN_WIDTH, SCREEN_HEIGHT);
    this.fontCamera.update();
}
```

`scoreNumber` se inicializa a `0` en `prepareScore()`, que se llama en el constructor. Esto significa que el score se resetea cuando se crea `GameScreen`, no cuando se llama a `show()`. Si el jugador vuelve a jugar (y el juego recrea la pantalla), el score empezará desde 0.

---

## Errores comunes en esta rama

### 1. "`NullPointerException` en `beginContact()`"

Ocurre cuando `getUserData()` devuelve `null` para alguna fixture. Asegúrate de que **todas** las fixtures que participan en colisiones relevantes tienen `userData` asignado: `fixtureDown`, `fixtureTop`, `fixtureCounter` en Pipes, `fixture` en Bird, y las fixtures de suelo y techo en `GameScreen`.

### 2. "El pájaro cruza la tubería sin morir"

Si `areColider()` no detecta el contacto correcto, revisa que los valores de `USER_PIPE_DOWN`, `USER_PIPE_TOP` y `USER_BIRD` en `Utils` coincidan exactamente con los strings asignados como `userData`.

### 3. "El score no sube al cruzar el hueco"

Comprueba que `USER_COUNTER = "counter"` (no `"pipeTop"` como en la rama 7). Este fue el bug de la rama anterior.

### 4. "El juego pasa a GameOver inmediatamente"

`beginContact()` se llama durante `world.step()`. Si dentro de este callback intentas modificar el mundo físico directamente (crear/destruir bodies), Box2D lanzará una excepción porque el mundo está bloqueado. En esta implementación solo se cambia el estado del pájaro y la velocidad de los bodies (operaciones seguras), así que no hay problema. Pero si en el futuro añades más lógica, ten esto en cuenta.

### 5. "La pantalla de GameOver no aparece tras 1.5 segundos"

Asegúrate de que `stage.addAction()` se llama sobre `this.stage` (el Stage de `GameScreen`), no sobre un Stage diferente. Y verifica que `this.stage.act()` se sigue ejecutando en `render()` después de la colisión para que los Actions avancen.

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

    // Identificadores de cuerpos / fixtures
    public static final String USER_BIRD = "bird";
    public static final String USER_FLOOR = "floor";
    public static final String USER_ROOF = "roof";
    public static final String USER_PIPE_DOWN = "pipeDown";
    public static final String USER_PIPE_TOP = "pipeTop";
    public static final String USER_COUNTER = "counter";
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
        // Nota: el body ya NO tiene setUserData en esta rama
    }

    public void createFixture() {
        CircleShape circle = new CircleShape();
        circle.setRadius(0.25f);

        this.fixture = this.body.createFixture(circle, 3);
        this.fixture.setUserData(Utils.USER_BIRD);   // ← userData en la FIXTURE

        circle.dispose();
    }

    // Cambiar estado al recibir un golpe
    public void hurt() {
        this.state = STATE_DEAD;
        this.stateTime = 0;
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
    static final float SPEED = -2f;

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
        // Nota: el body ya NO tiene setUserData
        bodyDown.setLinearVelocity(SPEED, 0);
    }

    private void createBodyPipeTop() {
        BodyDef def = new BodyDef();
        def.position.x = bodyDown.getPosition().x;
        def.position.y = bodyDown.getPosition().y + PIPE_HEIGHT + SPACE_BETWEEN_PIPES;
        def.type = BodyDef.BodyType.KinematicBody;

        bodyTop = world.createBody(def);
        // Nota: el body ya NO tiene setUserData
        bodyTop.setLinearVelocity(SPEED, 0);
    }

    private void createFixture() {
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(PIPE_WIDTH / 2, PIPE_HEIGHT / 2);

        this.fixtureDown = bodyDown.createFixture(shape, 8);
        this.fixtureDown.setUserData(USER_PIPE_DOWN);   // ← userData en FIXTURE

        this.fixtureTop = bodyTop.createFixture(shape, 8);
        this.fixtureTop.setUserData(USER_PIPE_TOP);     // ← userData en FIXTURE

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
        this.fixtureCounter.setUserData(USER_COUNTER);  // ← userData en FIXTURE

        polygonShape.dispose();
    }

    public boolean isOutOfScreen() {
        return this.bodyDown.getPosition().x <= -2f;
    }

    // Detener el movimiento de todos los bodies al morir el pájaro
    public void stopPipes() {
        this.bodyDown.setLinearVelocity(0, 0);
        this.bodyTop.setLinearVelocity(0, 0);
        this.bodyCounter.setLinearVelocity(0, 0);
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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.iesfa.flappy.MainGame;
import com.iesfa.flappy.actors.Bird;
import com.iesfa.flappy.actors.Pipes;

public class GameScreen extends BaseScreen implements ContactListener {

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

    private int scoreNumber;

    private Array<Pipes> arrayPipes;

    public GameScreen(MainGame mainGame) {
        super(mainGame);

        this.world = new World(new Vector2(0, -10), true);
        this.world.setContactListener(this);         // ← registrar el listener

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
        this.scoreNumber = 0;
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
        body.createFixture(edge, 1).setUserData(USER_ROOF);  // ← userData en fixture
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
        body.createFixture(edge, 3).setUserData(USER_FLOOR);  // ← userData también en fixture
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
        this.score.draw(this.stage.getBatch(), "" + this.scoreNumber, SCREEN_WIDTH / 2, 725);
        this.stage.getBatch().end();
    }

    @Override
    public void hide() {
        this.bird.detach();
        // this.bird.remove();  ← comentado en el código original

        this.musicbg.stop();
    }

    @Override
    public void dispose() {
        this.stage.dispose();
        this.world.dispose();
    }

    // ─────────────────────────────────────────────
    //              COLISIONES
    // ─────────────────────────────────────────────

    public boolean areColider(Contact contact, Object objA, Object objB) {
        return (contact.getFixtureA().getUserData().equals(objA) &&
                contact.getFixtureB().getUserData().equals(objB))
            ||
               (contact.getFixtureA().getUserData().equals(objB) &&
                contact.getFixtureB().getUserData().equals(objA));
    }

    @Override
    public void beginContact(Contact contact) {
        if (areColider(contact, USER_BIRD, USER_COUNTER)) {
            this.scoreNumber++;
        } else {
            this.bird.hurt();

            for (Pipes pipe : this.arrayPipes) {
                pipe.stopPipes();
            }

            this.musicbg.stop();

            this.stage.addAction(Actions.sequence(
                Actions.delay(1.5f),
                Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        mainGame.setScreen(mainGame.gameOverScreen);
                    }
                })
            ));
        }
    }

    @Override
    public void endContact(Contact contact) { }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) { }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) { }
}
```

---

## 🛠️ Ejercicio práctico

**Objetivo:** Entender el sistema de colisiones y la relación entre fixtures, userData y el ContactListener.

1. **Ejecuta el proyecto.** El pájaro debe morir al tocar las tuberías o el suelo, el score debe incrementarse al cruzar el hueco, y tras 1.5 segundos debe aparecer (o al menos intentar mostrar) la pantalla de GameOver.

2. **Identifica los contactos:** Añade temporalmente un log en `beginContact()` para ver qué objetos colisionan:
   ```java
   Gdx.app.log("CONTACTO",
       contact.getFixtureA().getUserData() + " vs " +
       contact.getFixtureB().getUserData());
   ```
   ¿Cuántas veces se dispara al cruzar el sensor? ¿Y al tocar el suelo?

3. **El else colateral:** En `beginContact()`, el `else` captura cualquier colisión que no sea pájaro+sensor. ¿Qué ocurre si el pájaro toca simultáneamente la tubería y el suelo? ¿Se llama a `beginContact()` dos veces? Pruébalo y observa si el score cambia de manera extraña.

4. **Tiempo de gracia:** Cambia `Actions.delay(1.5f)` a `Actions.delay(0f)`. ¿Qué efecto visual produce en el momento de morir?

5. **Quita `stopPipes()`:** Comenta el bucle de `stopPipes()` en `beginContact()`. ¿Qué aspecto tiene morir ahora? ¿Es mejor o peor la experiencia de juego?

6. **Ejercicio de alumno:** En `hide()` hay un `TODO` que indica que deberías liberar la física del suelo y el techo. ¿Cómo guardarías las referencias a esos bodies para poder hacer `world.destroyBody()` en `hide()`?

7. **Pregunta para reflexionar:** `beginContact()` se ejecuta dentro del `world.step()` (el mundo está bloqueado en ese momento). ¿Por qué `stopPipes()` (que llama a `setLinearVelocity()`) es seguro dentro del callback pero `pipe.detach()` (que llama a `destroyBody()`) no lo sería?
