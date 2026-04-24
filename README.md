# 🐦 Construyendo Flappy Bird — Rama `3.World`
## La gravedad entra en escena

> *El pájaro flota, inerte. Para que el juego tenga sentido necesita caer. Esta rama introduce Box2D, el motor de físicas que hará que el pájaro obedezca a la gravedad y choque contra el suelo.*

---

## ¿Por qué un motor de físicas?

Podrías mover el pájaro manualmente:

```java
velocidadY -= 9.8f * delta;
posY += velocidadY * delta;
if (posY < 0) { posY = 0; velocidadY = 0; }
```

Funciona para un caso sencillo. Pero cuando tienes tuberías, suelo, techo y detección de colisiones entre todos ellos, el código se convierte en un laberinto de ifs. Box2D resuelve todo eso con una arquitectura de tres capas.

---

## Acto 1 — Los tres conceptos de Box2D

### `World`: el universo físico

Es el contenedor de toda la simulación. En el constructor defines la gravedad:

```java
// Vector2(0, -10f): sin fuerza horizontal, gravedad hacia abajo
// true: los cuerpos inmóviles se "duermen" para ahorrar CPU
World world = new World(new Vector2(0, -10f), true);
```

Cada frame haces avanzar la simulación un paso:

```java
world.step(delta, 6, 2);
//          ↑     ↑  ↑
//          │     │  └── iteraciones de posición (precisión)
//          │     └───── iteraciones de velocidad (precisión)
//          └─────────── tiempo transcurrido
```

### `Body`: el objeto físico

Un `Body` tiene posición y velocidad. Hay tres tipos:

| Tipo | Comportamiento | Uso en esta rama |
|------|---------------|-----------------|
| `DynamicBody` | Afectado por gravedad y fuerzas | El pájaro |
| `KinematicBody` | Velocidad controlada, sin gravedad | (tuberías, rama 4) |
| `StaticBody` | Inmóvil, participa en colisiones | Suelo y techo |

### `Fixture`: la forma

Un `Body` sin `Fixture` no ocupa espacio: existe en el mundo pero no choca con nada. La `Fixture` le da forma geométrica:

```
World
  └── Body (posición, velocidad, tipo)
        └── Fixture (forma, densidad, fricción)
              └── Shape (Circle, Polygon, Edge...)
```

---

## Acto 2 — El desdoblamiento: Actor vs Body

Este es el concepto más importante de la rama. **Box2D y el Stage son dos mundos independientes**. El pájaro existe en ambos al mismo tiempo:

```
STAGE (visual)              BOX2D (físico)
──────────────              ──────────────
Bird extends Actor           Body (DynamicBody)
getX(), getY()               getPosition().x / .y
Se dibuja en draw()          Calcula físicas en world.step()
```

Box2D calcula dónde debería estar el pájaro. El `Actor` lo dibuja allí. La sincronización ocurre en `act()` de `Bird`, que se ejecuta justo después de `world.step()`:

```java
@Override
public void act(float delta) {
    super.act(delta);
    stateTime += delta;

    // body.getPosition() devuelve el CENTRO del cuerpo
    // setPosition() espera la esquina INFERIOR IZQUIERDA → restar la mitad del tamaño
    setPosition(
        body.getPosition().x - WIDTH  / 2f,
        body.getPosition().y - HEIGHT / 2f
    );
}
```

---

## Acto 3 — Crear el cuerpo del pájaro

Crear un cuerpo en Box2D siempre sigue el mismo patrón: **definir → crear → dar forma → limpiar**:

```java
private void createBody() {
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = BodyDef.BodyType.DynamicBody;
    bodyDef.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT * 0.65f);
    bodyDef.fixedRotation = true;   // no queremos que rote físicamente

    body = world.createBody(bodyDef);
    body.setUserData(USER_BIRD);    // identificador para colisiones (lo usarás en rama 5)

    CircleShape shape = new CircleShape();
    shape.setRadius(0.30f);

    FixtureDef fixtureDef = new FixtureDef();
    fixtureDef.shape       = shape;
    fixtureDef.density     = 1f;
    fixtureDef.friction    = 0f;
    fixtureDef.restitution = 0f;   // sin rebote

    body.createFixture(fixtureDef);

    // SIEMPRE dispose() la shape: Box2D ya copió los datos internamente
    shape.dispose();
}
```

> ⚠️ **`shape.dispose()` es obligatorio.** La `Shape` ocupa memoria nativa. Si no la liberas, tendrás una fuga de memoria silenciosa.

En esta rama `Utils` incorpora los identificadores de los cuerpos físicos que existen ya: el pájaro, el suelo y el techo.

---

## Acto 4 — Suelo y techo con `EdgeShape`

El suelo y el techo son `StaticBody` con forma de línea. Un `StaticBody` no se mueve nunca pero el pájaro chocará contra él:

```java
private Body createEdge(float x1, float y1, float x2, float y2, String userData) {
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = BodyDef.BodyType.StaticBody;
    Body body = world.createBody(bodyDef);
    body.setUserData(userData);

    EdgeShape edge = new EdgeShape();
    edge.set(x1, y1, x2, y2);
    body.createFixture(edge, 0);   // densidad 0 en cuerpos estáticos
    edge.dispose();

    return body;
}

// En show() de GameScreen:
createEdge(0, 0, WORLD_WIDTH, 0, USER_FLOOR);                        // suelo en y=0
createEdge(0, WORLD_HEIGHT, WORLD_WIDTH, WORLD_HEIGHT, USER_ROOF);   // techo
```

---

## Acto 5 — El orden en `render()` importa

```java
@Override
public void render(float delta) {
    Gdx.gl.glClearColor(0.5f, 0.8f, 1f, 1f);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

    // 1º Box2D calcula las nuevas posiciones
    world.step(delta, 6, 2);

    // 2º act() sincroniza Actor ← Body (usa las posiciones recién calculadas)
    stage.act(delta);

    // 3º draw() dibuja en las posiciones ya actualizadas
    stage.draw();

    // DEBUG: dibuja las formas Box2D encima del juego
    debugRenderer.render(world, stage.getCamera().combined);
}
```

Si inviertes el orden de `world.step()` y `stage.act()`, el actor se dibujará en la posición del frame anterior. Se verá "retardado" respecto a la física.

---

## Acto 6 — `Box2DDebugRenderer`: la radiografía del mundo

El `debugRenderer` dibuja las formas geométricas de Box2D sobre el juego. Actívalo siempre durante el desarrollo:

```java
debugRenderer = new Box2DDebugRenderer();
```

Para distinguir fácilmente los tipos de body por color usa `.set()` (los campos son `public final`, no admiten `=`):

```java
debugRenderer.SHAPE_STATIC.set(0f, 1f, 0f, 1f);    // verde  → suelo y techo
debugRenderer.SHAPE_AWAKE.set(1f, 0.5f, 0f, 1f);   // naranja → pájaro activo
```

---

## Lo que cambia en `Utils` en esta rama

Se añaden los identificadores `userData` para los cuerpos que existen en este punto del proyecto: el pájaro, el suelo y el techo. Las tuberías llegarán en la rama siguiente:

```java
// Añadido en esta rama:
public static final String USER_BIRD  = "bird";
public static final String USER_FLOOR = "floor";
public static final String USER_ROOF  = "roof";
```

---

## Código completo de esta rama

<details>
<summary>📄 <code>Utils.java</code></summary>

```java
package com.mygdx.game.extra;

public class Utils {
    public static final int    SCREEN_WIDTH  = 480;
    public static final int    SCREEN_HEIGHT = 800;
    public static final float  WORLD_WIDTH   = 4.8f;
    public static final float  WORLD_HEIGHT  = 8f;
    public static final String ATLAS_MAP     = "FBAtlas";
    public static final String BIRD1         = "bird1";
    public static final String BIRD2         = "bird2";
    public static final String BIRD3         = "bird3";
    // Añadido en esta rama:
    public static final String USER_BIRD  = "bird";
    public static final String USER_FLOOR = "floor";
    public static final String USER_ROOF  = "roof";
}
```

</details>

<details>
<summary>📄 <code>Bird.java</code></summary>

```java
package com.mygdx.game.actors;

import static com.mygdx.game.extra.Utils.*;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class Bird extends Actor {

    public static final float WIDTH  = 0.58f;
    public static final float HEIGHT = 0.40f;

    private Animation<TextureRegion> animation;
    private float stateTime = 0f;
    private World world;
    private Body  body;

    public Bird(World world, Animation<TextureRegion> animation) {
        this.world     = world;
        this.animation = animation;
        setSize(WIDTH, HEIGHT);
        createBody();
    }

    private void createBody() {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT * 0.65f);
        bd.fixedRotation = true;
        body = world.createBody(bd);
        body.setUserData(USER_BIRD);

        CircleShape s = new CircleShape();
        s.setRadius(0.30f);
        FixtureDef fd = new FixtureDef();
        fd.shape = s; fd.density = 1f; fd.friction = 0f; fd.restitution = 0f;
        body.createFixture(fd);
        s.dispose();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta;
        setPosition(
            body.getPosition().x - WIDTH  / 2f,
            body.getPosition().y - HEIGHT / 2f
        );
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.draw(
            animation.getKeyFrame(stateTime, true),
            getX(), getY(), getWidth(), getHeight()
        );
    }

    public void detach() { world.destroyBody(body); }
    public Body getBody() { return body; }
}
```

</details>

<details>
<summary>📄 <code>GameScreen.java</code></summary>

```java
package com.mygdx.game.screens;

import static com.mygdx.game.extra.Utils.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.game.MainGame;
import com.mygdx.game.actors.Bird;

public class GameScreen extends BaseScreen {

    private Stage stage;
    private Bird  bird;
    private World world;
    private Box2DDebugRenderer debugRenderer;

    public GameScreen(MainGame mainGame) {
        super(mainGame);
        world         = new World(new Vector2(0, -10f), true);
        stage         = new Stage(new FitViewport(WORLD_WIDTH, WORLD_HEIGHT));
        debugRenderer = new Box2DDebugRenderer();
    }

    @Override
    public void show() {
        createEdge(0, 0, WORLD_WIDTH, 0, USER_FLOOR);
        createEdge(0, WORLD_HEIGHT, WORLD_WIDTH, WORLD_HEIGHT, USER_ROOF);
        bird = new Bird(world, mainGame.assetMan.getBirdAnimation());
        stage.addActor(bird);
    }

    private Body createEdge(float x1, float y1, float x2, float y2, String userData) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        Body body = world.createBody(bd);
        body.setUserData(userData);
        EdgeShape edge = new EdgeShape();
        edge.set(x1, y1, x2, y2);
        body.createFixture(edge, 0);
        edge.dispose();
        return body;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.5f, 0.8f, 1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        world.step(delta, 6, 2);
        stage.act(delta);
        stage.draw();
        debugRenderer.render(world, stage.getCamera().combined);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, false);
    }

    @Override
    public void dispose() {
        stage.dispose();
        world.dispose();
        debugRenderer.dispose();
    }
}
```

</details>

---

## 🛠️ Ejercicio práctico

**Objetivo:** Experimentar con los parámetros físicos para entender su efecto.

1. Ejecuta el proyecto. El pájaro debe caer y detenerse en el suelo (línea verde del debugRenderer).
2. Cambia la gravedad a `new Vector2(0, -30f)`. ¿Cómo cambia la sensación?
3. Vuelve a `-10f`. Cambia el radio del `CircleShape` a `0.60f`. ¿Qué ves en el debugRenderer?
4. Cambia `fixedRotation = true` a `false`. ¿Qué hace el pájaro al chocar con el suelo?
5. Restaura todos los valores originales. **Pregunta:** ¿Por qué `fixedRotation = true` es importante para este juego?
