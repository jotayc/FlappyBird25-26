# 🐦 Construyendo Flappy Bird — Rama `4.Input&Pipes`
## El pájaro salta y aparece la primera tubería

> *El pájaro cae por gravedad y se queda en el suelo. Un juego sin interacción no es un juego. En esta rama le damos vida al jugador: el toque de pantalla hará saltar al pájaro. Además, colocamos en escena el primer obstáculo real — una tubería inferior — sentando las bases para todo lo que vendrá después.*

---

## ¿Qué cambia en esta rama?

En la rama anterior (`3.World`) el pájaro caía por gravedad y se detenía en el suelo. No había forma de controlarlo ni había obstáculos. Esta rama introduce **tres novedades**:

1. **Input del jugador**: al tocar la pantalla, el pájaro salta hacia arriba.
2. **Un nuevo actor**: `Pipes`, la tubería inferior, que introduce el concepto de `KinematicBody` y `PolygonShape`.
3. **Fondo visual**: el background se añade como `Image` del Stage, integrándolo en el sistema de actores.

Antes de escribir código, conviene entender la mecánica que estamos construyendo. Piensa en Flappy Bird como un péndulo vertical: la gravedad tira constantemente del pájaro hacia abajo, y cada toque de pantalla lo empuja brevemente hacia arriba. El jugador no controla la posición del pájaro directamente — controla su velocidad vertical. Esta distinción es fundamental para entender cómo funciona el salto.

---

## Paso 1 — Capturar el toque de pantalla

### El problema: ¿cuándo ha tocado el jugador?

LibGDX ofrece dos formas de detectar si el jugador está tocando la pantalla. Parecen similares, pero su comportamiento es radicalmente distinto:

```java
Gdx.input.isTouched()      // true MIENTRAS el dedo está en pantalla
Gdx.input.justTouched()    // true solo en el PRIMER frame del toque
```

Para entender la diferencia, imagina que mantienes el dedo apoyado durante medio segundo. A 60 FPS, eso son aproximadamente 30 frames:

```
Frame:    1    2    3    4    5   ...  28   29   30   31
Dedo:    [TOCA────────────────────────────────────SUELTA]

isTouched():   ✓    ✓    ✓    ✓    ✓   ...   ✓    ✓    ✓    ✗
justTouched():  ✓    ✗    ✗    ✗    ✗   ...   ✗    ✗    ✗    ✗
```

Con `isTouched()`, el pájaro recibiría el impulso de salto **30 veces seguidas** — se dispararía fuera del mapa. Con `justTouched()`, solo lo recibe **una vez**, en el frame exacto del contacto. Un toque = un salto.

### Analogía: el timbre de una puerta

`isTouched()` es como un timbre que suena mientras mantienes el dedo pulsado: **RIIIIIIING**. `justTouched()` es como un timbre digital que emite un solo pitido por pulsación: **DING**. Para el salto del pájaro necesitas el pitido, no el timbrazo continuo.

### ¿Dónde colocamos la lectura del input?

En esta rama, el input se lee **dentro del método `act()` de `Bird`**, no en el `render()` de `GameScreen`. Esta es una decisión de diseño importante que conecta con lo que ya sabéis de Programación Orientada a Objetos: el pájaro es responsable de su propio comportamiento. La pantalla no le dice cuándo saltar; el pájaro lo decide por sí mismo.

```java
@Override
public void act(float delta) {
    boolean jump = Gdx.input.justTouched();

    if (jump && this.state == STATE_NORMAL) {
        this.body.setLinearVelocity(0, JUMP_SPEED);
    }
}
```

Fíjate en la condición doble: el salto solo ocurre si `justTouched()` devuelve `true` **y** el pájaro está en estado `STATE_NORMAL`. Esto previene que un pájaro muerto siga saltando — algo que implementaremos completamente cuando añadamos las colisiones.

> ⚠️ **Error típico en clase:** Algunos alumnos ponen la lectura de input en `render()` de `GameScreen` y llaman a un método `bird.jump()`. Funciona, pero rompe la encapsulación: ahora `GameScreen` necesita conocer la lógica interna de Bird. Si mañana quisiéramos que el pájaro saltara con doble toque, tendríamos que modificar `GameScreen` en lugar de solo modificar `Bird`.

---

## Paso 2 — El salto: `setLinearVelocity()`

### ¿Cómo salta el pájaro en Box2D?

Recordemos de la rama anterior que Box2D controla la física del pájaro. Su `DynamicBody` tiene una velocidad que la gravedad va modificando cada frame. Para hacer saltar al pájaro, necesitamos **cambiar su velocidad vertical** de golpe.

Box2D ofrece varias formas de mover un cuerpo dinámico:

| Método | Efecto | Analogía |
|--------|--------|----------|
| `setLinearVelocity(x, y)` | Establece la velocidad exacta | Cambiar la marcha de un coche instantáneamente |
| `applyLinearImpulse(v, p, w)` | Suma un impulso a la velocidad actual | Dar un empujón a alguien que ya se mueve |
| `applyForce(v, p, w)` | Aplica fuerza continua | Soplar una vela: efecto gradual |

En esta rama usamos `setLinearVelocity()`:

```java
private static final float JUMP_SPEED = 50f;

// En act():
this.body.setLinearVelocity(0, JUMP_SPEED);
```

**¿Qué hace esta línea exactamente?** Dos cosas simultáneas:
- Establece la velocidad **horizontal** en `0` — el pájaro no se mueve lateralmente.
- Establece la velocidad **vertical** en `JUMP_SPEED` (50 unidades/segundo hacia arriba).

Después de este instante, la gravedad del mundo (`-10`) comienza a frenar la subida, la velocidad va disminuyendo hasta llegar a 0 (punto más alto del salto), y luego el pájaro empieza a caer de nuevo. Esto crea la parábola característica de Flappy Bird.

### ¿Por qué `setLinearVelocity()` y no `applyLinearImpulse()`?

Ambas opciones son válidas, pero tienen comportamientos diferentes:

```
Situación: el pájaro cae a -30 unidades/s

Con setLinearVelocity(0, 50):
  Velocidad antes: -30       → Velocidad después: +50
  El salto SIEMPRE produce el mismo arco

Con applyLinearImpulse(0, 50):
  Velocidad antes: -30       → Velocidad después: -30 + 50 = +20
  El salto depende de la velocidad actual (menor arco si cae rápido)
```

`setLinearVelocity()` **reemplaza** la velocidad, así que cada salto es idéntico sin importar si el pájaro estaba subiendo o cayendo. Esto da un control más predecible al jugador. Es la opción más simple y directa para este proyecto.

### El valor de `JUMP_SPEED`

El valor `50f` puede parecer arbitrario. Es el resultado de prueba y error hasta encontrar un salto que "se sienta bien". Demasiado bajo (`20f`) y el pájaro apenas sube; demasiado alto (`100f`) y se sale de la pantalla. La relación entre la gravedad (`-10`) y la velocidad del salto (`50`) determina la altura y duración del arco.

---

## Paso 3 — Estados del pájaro: preparando el futuro

### ¿Por qué estados tan pronto?

Aunque en esta rama todavía no hay colisiones que maten al pájaro, introducimos un sistema de estados sencillo. Es una práctica habitual en desarrollo de videojuegos: **preparar la estructura antes de necesitarla**, para no tener que refactorizar después.

```java
private static final int STATE_NORMAL = 0;
private static final int STATE_DEAD = 1;

private int state;
```

El estado se inicializa a `STATE_NORMAL` en el constructor y se comprueba antes de permitir el salto:

```java
if (jump && this.state == STATE_NORMAL) {
    this.body.setLinearVelocity(0, JUMP_SPEED);
}
```

En ramas futuras, cuando implementemos colisiones, bastará con poner `this.state = STATE_DEAD` para que el pájaro deje de responder al input. No necesitaremos tocar la lógica del salto.

### Conexión con POO: el patrón State simplificado

En Programación Orientada a Objetos habéis visto el patrón **State**, donde cada estado es una clase independiente con su propio comportamiento. Aquí usamos una versión simplificada con constantes enteras. Para un juego con solo dos estados (vivo/muerto), esto es suficiente. Si tuviéramos más estados (volando, cayendo, herido, invencible...), convendría evolucionar hacia el patrón completo.

---

## Paso 4 — Cambios en `Bird.java` respecto a la rama anterior

### Constructor parametrizado

En la rama 3, la posición del pájaro estaba fija en el código de `createBody()`. Ahora el constructor recibe un `Vector2` con la posición inicial, lo que permite colocar el pájaro donde queramos desde `GameScreen`:

```java
public Bird(World world, Animation<TextureRegion> animation, Vector2 position) {
    this.birdAnimation = animation;
    this.position = position;
    this.world = world;
    this.stateTime = 0f;
    this.state = STATE_NORMAL;

    createBody();
    createFixture();
}
```

**¿Por qué este cambio?** Porque la posición del pájaro es una decisión de la pantalla de juego, no del pájaro. Si mañana quisiéramos una pantalla de tutorial donde el pájaro empieza en otra posición, no tendríamos que tocar `Bird.java`.

### Separación de `createBody()` y `createFixture()`

En la rama 3, `createBody()` hacía todo: creaba el body, la shape y la fixture en un solo método. Ahora están separados en dos métodos:

```java
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
```

Esta separación no es casual: al guardar la referencia a la `Fixture` en una variable de instancia, podemos destruirla individualmente en `detach()`.

### `userData` en la Fixture, no en el Body

En la rama 3, el identificador `USER_BIRD` se asignaba al `Body`:

```java
// Rama 3:
body.setUserData(USER_BIRD);
```

Ahora se asigna a la `Fixture`:

```java
// Rama 4:
this.fixture.setUserData(USER_BIRD);
```

**¿Por qué este cambio?** Un `Body` puede tener varias fixtures (varias formas). Al asignar el `userData` a la fixture, cuando detectemos colisiones podremos saber exactamente **qué parte** del cuerpo ha colisionado. Para el pájaro (que solo tiene una fixture circular) da igual, pero es una buena práctica que nos será útil con las tuberías.

### El `draw()` con valores literales

El método `draw()` usa valores literales para el offset y el tamaño del sprite en lugar de constantes `WIDTH`/`HEIGHT`:

```java
@Override
public void draw(Batch batch, float parentAlpha) {
    setPosition(body.getPosition().x - 0.4f, body.getPosition().y - 0.25f);
    batch.draw(this.birdAnimation.getKeyFrame(stateTime, true),
        getX(), getY(), 0.8f, 0.5f);

    stateTime += Gdx.graphics.getDeltaTime();
}
```

Los valores `0.4f` y `0.25f` son las mitades de `0.8f` y `0.5f` (ancho y alto del sprite). Recuerda de la rama 3: `body.getPosition()` devuelve el **centro** del cuerpo, pero `batch.draw()` dibuja desde la **esquina inferior izquierda**, así que hay que restar la mitad.

Otro detalle importante: `stateTime` se incrementa aquí con `Gdx.graphics.getDeltaTime()` en lugar de con el `delta` de `act()`. Ambos valores son prácticamente idénticos — `getDeltaTime()` devuelve el tiempo transcurrido desde el último frame —, pero al hacerlo en `draw()` nos aseguramos de que la animación avanza incluso si `act()` cambia en el futuro.

### El nuevo `detach()`

Ahora `detach()` destruye la fixture antes del body:

```java
public void detach() {
    this.body.destroyFixture(this.fixture);
    this.world.destroyBody(this.body);
}
```

**¿Por qué destruir la fixture explícitamente?** En realidad, `destroyBody()` destruye automáticamente todas las fixtures del body. Hacerlo explícitamente es una práctica defensiva que deja claro qué recursos se liberan y en qué orden. Además, en un escenario más complejo donde quisieras quitar solo una fixture sin destruir el body entero, necesitarías esta referencia.

---

## Paso 5 — La clase `Pipes`: el primer obstáculo

### ¿Qué es una tubería en términos de Box2D?

Una tubería es un obstáculo que:
- **No se mueve por gravedad**: no debe caer.
- **Participa en colisiones**: el pájaro debe poder chocar con ella.
- **Se moverá en el futuro**: en ramas posteriores las tuberías avanzarán hacia la izquierda.

Revisemos los tres tipos de body que conocemos de la rama 3 y por qué `KinematicBody` es la elección correcta:

```
DynamicBody      → Afectado por gravedad y fuerzas    → El pájaro
StaticBody       → Inmóvil, no se puede mover nunca   → Suelo y techo
KinematicBody    → No le afecta la gravedad,           → Las tuberías ✓
                   pero SE PUEDE mover por código
```

¿Por qué no `StaticBody`? Porque en ramas posteriores las tuberías se moverán a velocidad constante hacia la izquierda, y un `StaticBody` no puede moverse. Usar `KinematicBody` desde el principio evita tener que cambiar el tipo más adelante.

### La analogía: una cinta transportadora

Un `KinematicBody` es como un objeto sobre una cinta transportadora en una fábrica: tú controlas su velocidad (puedes acelerarla, pararla, invertirla), pero la gravedad no le afecta — el objeto no se cae de la cinta. Cuando otros objetos chocan contra él, Box2D detecta la colisión normalmente.

### Estructura de la clase

La clase `Pipes` sigue exactamente el mismo patrón que ya conoces de `Bird`: extiende `Actor`, tiene un `Body` y una `Fixture`, y se crea con el patrón **definir → crear → dar forma → limpiar**:

```java
public class Pipes extends Actor {

    private static final float PIPE_WIDTH = 1f;
    private static final float PIPE_HEIGHT = 4f;

    private TextureRegion pipeDownTR;
    private Body bodyDown;
    private Fixture fixtureDown;
    private World world;

    public Pipes(World world, TextureRegion trpDown, Vector2 position) {
        this.world = world;
        this.pipeDownTR = trpDown;

        createBodyPipeDown(position);
        createFixture();
    }
}
```

Observa que los nombres incluyen `Down` (inferior): `pipeDownTR`, `bodyDown`, `fixtureDown`. Esto es intencional — en la rama siguiente añadiremos la tubería superior, y estos nombres nos ayudarán a distinguirlas.

### Crear el body cinemático

```java
private void createBodyPipeDown(Vector2 position) {
    BodyDef def = new BodyDef();
    def.position.set(position);
    def.type = BodyDef.BodyType.KinematicBody;

    bodyDown = world.createBody(def);
    bodyDown.setUserData(Utils.USER_PIPE_DOWN);
}
```

La posición se recibe como parámetro. En `GameScreen.show()` pasamos `new Vector2(3.75f, 2f)`, que coloca el **centro** del body en las coordenadas (3.75, 2) del mundo.

### La fixture: `PolygonShape` en vez de `CircleShape`

El pájaro usaba un `CircleShape` porque su forma es redondeada. La tubería es rectangular, así que usamos `PolygonShape.setAsBox()`:

```java
private void createFixture() {
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(PIPE_WIDTH / 2, PIPE_HEIGHT / 2);

    this.fixtureDown = bodyDown.createFixture(shape, 8);
    shape.dispose();
}
```

> ⚠️ **Cuidado con `setAsBox()`**: este es uno de los errores más frecuentes con Box2D. `setAsBox()` recibe las **mitades** del ancho y alto, no las dimensiones completas. Si la tubería mide 1×4 unidades, le pasas `(0.5, 2)`. Si le pasas `(1, 4)`, la forma será de 2×8 — ¡el doble de grande!

```
setAsBox(PIPE_WIDTH / 2, PIPE_HEIGHT / 2)
         └── 0.5 ──┘     └── 2.0 ──┘

Resultado:  rectángulo de 1.0 × 4.0 ✓

Si te equivocas y pones setAsBox(PIPE_WIDTH, PIPE_HEIGHT):
                          └── 1.0 ──┘   └── 4.0 ──┘

Resultado:  rectángulo de 2.0 × 8.0 ✗  (¡el doble!)
```

Puedes verificar visualmente si la forma coincide con la textura gracias al `Box2DDebugRenderer` que ya tienes activo.

---

## Paso 6 — Dibujar la tubería

### El método `draw()` de Pipes

Al igual que en `Bird`, necesitamos sincronizar la posición visual del `Actor` con la posición física del `Body`:

```java
@Override
public void draw(Batch batch, float parentAlpha) {
    setPosition(
        this.bodyDown.getPosition().x - (PIPE_WIDTH / 2),
        this.bodyDown.getPosition().y - (PIPE_HEIGHT / 2)
    );
    batch.draw(this.pipeDownTR, getX(), getY(), PIPE_WIDTH, PIPE_HEIGHT);
}
```

El patrón es idéntico al del pájaro y se repite en prácticamente todos los actores con cuerpo físico:

```
Body.getPosition()  →  devuelve el CENTRO del cuerpo
batch.draw()        →  espera la ESQUINA INFERIOR IZQUIERDA
                       → restar mitad del ancho y mitad del alto

  ┌─────────────┐
  │             │  ↑
  │   CENTRO ●  │  PIPE_HEIGHT
  │             │  ↓
  └─────────────┘
  ↑── PIPE_WIDTH ──↑
  
  batch.draw necesita este punto:
  ↓
  ●─────────────┐
  │             │
  │   CENTRO    │
  │             │
  └─────────────┘
```

### `act()` vacío: ¿por qué existe?

```java
@Override
public void act(float delta) {
    super.act(delta);
}
```

En esta rama la tubería no se mueve ni tiene lógica propia. El método `act()` solo llama a `super.act()`. ¿Por qué dejarlo entonces? Porque en ramas posteriores necesitaremos añadir lógica aquí (mover la tubería, auto-eliminarse al salir de la pantalla), y tener el método ya preparado facilita la evolución del código.

### `detach()`: liberar recursos

```java
public void detach() {
    bodyDown.destroyFixture(fixtureDown);
    world.destroyBody(bodyDown);
}
```

El mismo patrón que en `Bird`: destruir fixture, después body. Es fundamental para evitar fugas de memoria nativa.

---

## Paso 7 — El fondo como `Image` del Stage

### ¿Por qué un `Image` y no `batch.draw()`?

En ramas anteriores podríamos haber dibujado el fondo directamente con `batch.draw()` en el `render()` de `GameScreen`. Esta rama usa un enfoque diferente: crear un `Image` (que es un `Actor` de Scene2D) y añadirlo al Stage:

```java
public void addBackground() {
    this.background = new Image(mainGame.assetManager.getBackground());
    this.background.setPosition(0, 0);
    this.background.setSize(WORLD_WIDTH, WORLD_HEIGHT);
    this.stage.addActor(this.background);
}
```

**Ventajas de usar `Image`:**
- Se integra en el sistema de actores del Stage y se dibuja automáticamente con `stage.draw()`.
- Al ser el **primer actor añadido**, se dibuja **primero** (queda detrás de todo lo demás). El Stage dibuja los actores en el orden en que fueron añadidos.
- No necesitas gestionar manualmente el `batch.begin()` / `batch.end()`.

### Orden de añadido = orden de dibujado

```
stage.addActor(background);   // Se dibuja 1º → capa más profunda
stage.addActor(bird);         // Se dibuja 2º → encima del fondo
stage.addActor(pipes);        // Se dibuja 3º → encima de todo
```

Si añadieras el background después del pájaro, el fondo lo taparía. El Stage funciona como capas: lo primero que añades queda debajo, lo último queda encima.

---

## Paso 8 — Integración en `GameScreen`

### El método `show()` completo

```java
@Override
public void show() {
    addBackground();
    addBird();

    TextureRegion pipeTRDown = mainGame.assetManager.getPipeDownTR();
    this.pipes = new Pipes(this.world, pipeTRDown, new Vector2(3.75f, 2f));
    this.stage.addActor(this.pipes);
}
```

El orden es deliberado:
1. Primero el fondo (se dibuja detrás de todo).
2. Después el pájaro.
3. Por último la tubería (se dibuja encima del fondo pero podría quedar delante o detrás del pájaro según sus posiciones).

### Añadir `getPipeDownTR()` en `AssetMan`

```java
public TextureRegion getPipeDownTR() {
    return this.textureAtlas.findRegion(PIPE_DOWN);
}
```

El método devuelve un `TextureRegion` porque es el tipo que `batch.draw()` espera. Internamente, `findRegion()` devuelve un `AtlasRegion` (que extiende `TextureRegion`), pero no necesitamos las funcionalidades adicionales de `AtlasRegion`.

### Nuevas constantes en `Utils`

```java
// Identificadores de texturas en el atlas
public static final String PIPE_DOWN = "pipeDown";
public static final String PIPE_UP = "pipeUp";

// Identificador del cuerpo físico
public static final String USER_PIPE_DOWN = "pipeDown";
```

Fíjate en que `PIPE_UP` ya se define aunque en esta rama no lo usamos. Es una anticipación: la tubería superior llegará en la rama siguiente. Definir la constante ahora no cuesta nada y evita tener que volver a tocar `Utils` solo para añadir un String.

---

## Errores comunes en esta rama

### 1. "El pájaro salta sin parar"

```java
// ❌ MAL: usar isTouched() en vez de justTouched()
if (Gdx.input.isTouched()) { ... }
```

Solución: usar siempre `justTouched()` para acciones puntuales como el salto.

### 2. "La tubería es el doble de grande que la textura"

```java
// ❌ MAL: pasar dimensiones completas a setAsBox
shape.setAsBox(PIPE_WIDTH, PIPE_HEIGHT);  // → crea un box de 2×8
```

Solución: `setAsBox()` recibe mitades: `shape.setAsBox(PIPE_WIDTH / 2, PIPE_HEIGHT / 2)`.

### 3. "La tubería no se ve, solo la caja del debugRenderer"

Si ves la caja de colisión (las líneas verdes/amarillas) pero no la textura, revisa que:
- `this.stage.addActor(this.pipes)` esté presente en `show()`.
- La textura que pasas a `Pipes` no sea `null` (verifica que el nombre en `Utils.PIPE_DOWN` coincide exactamente con el nombre de la región en el atlas: `"pipeDown"`).

### 4. "El pájaro salta muerto"

```java
// ❌ MAL: no comprobar el estado
if (jump) { this.body.setLinearVelocity(0, JUMP_SPEED); }

// ✓ BIEN: comprobar que está vivo
if (jump && this.state == STATE_NORMAL) { ... }
```

---

## Código completo de esta rama

### 📄 Utils.java

```java
package com.mygdx.game.extra;

public class Utils {

    public static final int SCREEN_HEIGHT = 800;
    public static final int SCREEN_WIDTH = 480;

    public static final float WORLD_HEIGHT = 8f;
    public static final float WORLD_WIDTH = 4.8f;

    // Identificadores de texturas
    public static final String ATLAS_MAP = "FBAtlas";
    public static final String BACKGROUND_IMAGE = "flappy_background";
    public static final String BIRD1 = "bird1";
    public static final String BIRD2 = "bird2";
    public static final String BIRD3 = "bird3";
    public static final String PIPE_DOWN = "pipeDown";
    public static final String PIPE_UP = "pipeUp";

    // Identificadores de cuerpos
    public static final String USER_BIRD = "bird";
    public static final String USER_PIPE_DOWN = "pipeDown";
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
import static com.mygdx.game.extra.Utils.PIPE_DOWN;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class AssetMan {

    private AssetManager assetManager;
    private TextureAtlas textureAtlas;

    public AssetMan() {
        this.assetManager = new AssetManager();

        assetManager.load(ATLAS_MAP, TextureAtlas.class);
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

    // TEXTURA DE LA TUBERÍA INFERIOR
    public TextureRegion getPipeDownTR() {
        return this.textureAtlas.findRegion(PIPE_DOWN);
    }
}
```

### 📄 Bird.java

```java
package com.mygdx.game.actors;

import static com.mygdx.game.extra.Utils.USER_BIRD;

import com.badlogic.gdx.Gdx;
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
    private static final float JUMP_SPEED = 50f;

    private int state;

    private Animation<TextureRegion> birdAnimation;
    private Vector2 position;

    private float stateTime;

    private World world;
    private Body body;
    private Fixture fixture;

    public Bird(World world, Animation<TextureRegion> animation, Vector2 position) {
        this.birdAnimation = animation;
        this.position = position;
        this.world = world;
        this.stateTime = 0f;
        this.state = STATE_NORMAL;

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

    private TextureRegion pipeDownTR;
    private Body bodyDown;
    private Fixture fixtureDown;
    private World world;

    public Pipes(World world, TextureRegion trpDown, Vector2 position) {
        this.world = world;
        this.pipeDownTR = trpDown;

        createBodyPipeDown(position);
        createFixture();
    }

    private void createBodyPipeDown(Vector2 position) {
        BodyDef def = new BodyDef();
        def.position.set(position);
        def.type = BodyDef.BodyType.KinematicBody;

        bodyDown = world.createBody(def);
        bodyDown.setUserData(Utils.USER_PIPE_DOWN);
    }

    private void createFixture() {
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(PIPE_WIDTH / 2, PIPE_HEIGHT / 2);

        this.fixtureDown = bodyDown.createFixture(shape, 8);
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
    }

    public void detach() {
        bodyDown.destroyFixture(fixtureDown);
        world.destroyBody(bodyDown);
    }
}
```

### 📄 GameScreen.java

```java
package com.mygdx.game.screens;

import static com.mygdx.game.extra.Utils.WORLD_HEIGHT;
import static com.mygdx.game.extra.Utils.WORLD_WIDTH;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
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

    Pipes pipes;

    private Box2DDebugRenderer debugRenderer;
    private OrthographicCamera ortCamera;

    public GameScreen(MainGame mainGame) {
        super(mainGame);

        this.world = new World(new Vector2(0, -10), true);
        FitViewport fitViewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
        this.stage = new Stage(fitViewport);

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
        this.bird = new Bird(this.world, birdSprite, new Vector2(1f, 4f));
        this.stage.addActor(this.bird);
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
        addBird();

        TextureRegion pipeTRDown = mainGame.assetManager.getPipeDownTR();
        this.pipes = new Pipes(this.world, pipeTRDown, new Vector2(3.75f, 2f));
        this.stage.addActor(this.pipes);
    }

    @Override
    public void hide() {
        this.bird.detach();
        this.bird.remove();
    }

    @Override
    public void dispose() {
        this.stage.dispose();
        this.world.dispose();
    }
}
```

---

## 🛠️ Ejercicio práctico

**Objetivo:** Experimentar con las mecánicas recién implementadas y entender la relación entre parámetros.

1. **Ejecuta el proyecto.** Deberías ver el fondo de Flappy Bird, el pájaro cayendo por gravedad y la tubería fija con su textura. Al tocar/hacer clic, el pájaro salta.

2. **Ajusta la fuerza del salto:** Cambia `JUMP_SPEED` de `50f` a `20f`. ¿Puede el pájaro superar la tubería? Ahora prueba con `100f`. ¿Qué ocurre?

3. **Mueve la tubería:** Cambia la posición en `GameScreen.show()` de `new Vector2(3.75f, 2f)` a `new Vector2(2f, 5f)`. ¿Dónde aparece ahora? ¿Y con `new Vector2(0f, 0f)`? ¿Por qué aparece parcialmente fuera de la pantalla?

4. **Experimenta con el tamaño:** Cambia `PIPE_WIDTH` a `2f` y `PIPE_HEIGHT` a `6f`. ¿Coincide la caja de colisión del debugRenderer con la textura dibujada? ¿Y si cambias `setAsBox()` sin ajustar el `draw()`?

5. **Compara formas:** Cambia el `PolygonShape` de la tubería por un `CircleShape` con radio `0.5f`. ¿Qué forma ves en el debugRenderer? ¿Tiene sentido para una tubería?

6. **Pregunta para reflexionar:** La tubería usa `KinematicBody` aunque en esta rama no se mueve. ¿Qué pasaría si usaras `StaticBody` ahora y quisieras mover la tubería en la rama siguiente? Pruébalo: cambia el tipo a `StaticBody` y en `act()` intenta `bodyDown.setLinearVelocity(-2f, 0f)`. ¿Se mueve?
