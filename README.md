# 🐦 Construyendo Flappy Bird — Rama `5.PipeTop&Counter`
## La tubería se completa y las tuberías se mueven

> *En la rama anterior colocamos una tubería inferior estática. Pero Flappy Bird necesita pares de tuberías — una abajo y otra arriba — con un hueco entre ellas para que el pájaro pase. Además, las tuberías deben moverse hacia la izquierda, y necesitamos un sensor invisible para detectar cuándo el pájaro cruza el hueco. Esta rama transforma un obstáculo aislado en la mecánica central del juego.*

---

## ¿Qué cambia en esta rama?

La rama 4 dejó una sola tubería inferior, quieta, sin compañera. En esta rama damos tres pasos grandes:

1. **Tubería superior**: un segundo body y textura colocados encima del hueco.
2. **Movimiento**: los tres cuerpos se desplazan hacia la izquierda a velocidad constante.
3. **Sensor contador**: un cuerpo invisible entre las dos tuberías que servirá para detectar cuándo el pájaro cruza el hueco y sumar puntos.

Al terminar esta rama, tendrás el par completo de tuberías moviéndose por la pantalla — el obstáculo fundamental de Flappy Bird.

---

## Paso 1 — Entender la estructura completa de un par de tuberías

### La anatomía del obstáculo

Cada par de tuberías es un único `Actor` (`Pipes`) que gestiona internamente **tres cuerpos Box2D**:

```
  ┌──────────┐
  │          │  ← bodyTop (KinematicBody)
  │ pipeTop  │     Textura: pipeUp del atlas
  │          │     
  └──────────┘
                ← COUNTER_HEIGHT (2 unidades de hueco)
   [counter]    ← bodyCounter (KinematicBody + sensor)
                   No se dibuja, solo detecta al pájaro
  ┌──────────┐
  │          │
  │ pipeDown │  ← bodyDown (KinematicBody)
  │          │     Textura: pipeDown del atlas
  └──────────┘
```

¿Por qué tres cuerpos y no uno solo con una forma en "U"? Porque Box2D no permite formas cóncavas (con entrantes). Una "U" tendría un hueco interior, que es exactamente una forma cóncava. La solución es usar dos rectángulos independientes (las tuberías) y un tercer cuerpo separado para el sensor.

### Analogía: una puerta automática

Piensa en las puertas automáticas de un centro comercial. Hay dos elementos físicos (los dos paneles de cristal) y un sensor de movimiento invisible entre ellos. Cuando pasas, el sensor te detecta pero no te bloquea — no puedes atravesar los cristales, pero el sensor no ofrece resistencia. Es exactamente lo que construimos: dos tuberías sólidas y un sensor invisible en el hueco.

---

## Paso 2 — Nuevas constantes y atributos

### Constantes añadidas en `Pipes`

```java
private static final float COUNTER_HEIGHT = 2f;  // altura del hueco entre tuberías
private static final float SPEED = -0.2f;         // velocidad horizontal (negativa = izquierda)
```

`COUNTER_HEIGHT` define la distancia entre la parte superior de `bodyDown` y la parte inferior de `bodyTop`. Es el espacio por el que el pájaro debe pasar. Un valor de `2f` unidades del mundo es suficiente para que quepa el pájaro (cuyo radio es `0.30f`), pero exige precisión.

`SPEED` es la velocidad horizontal de las tuberías. El valor `-0.2f` las mueve lentamente hacia la izquierda. En ramas posteriores ajustaremos este valor para encontrar la dificultad adecuada.

### Nuevos atributos

En la rama 4, `Pipes` solo tenía un body y una fixture. Ahora tiene tres de cada uno:

```java
// Texturas
private TextureRegion pipeDownTR;
private TextureRegion pipeTopTR;       // ← NUEVO

// Bodies
private Body bodyDown;
private Body bodyTop;                  // ← NUEVO
private Body bodyCounter;              // ← NUEVO

// Fixtures
private Fixture fixtureDown;
private Fixture fixtureTop;            // ← NUEVO
private Fixture fixtureCounter;        // ← NUEVO
```

---

## Paso 3 — El constructor actualizado

El constructor ahora recibe **dos texturas** en lugar de una:

```java
public Pipes(World world, TextureRegion trpDown, TextureRegion trpTop, Vector2 position) {
    this.world = world;
    this.pipeDownTR = trpDown;
    this.pipeTopTR = trpTop;

    createBodyPipeDown(position);
    createBodyPipeTop();
    createCounter();
    createFixture();
}
```

El orden de creación es importante:
1. **Primero `bodyDown`**: porque es el que recibe la posición como parámetro. Los demás se posicionan relativamente a él.
2. **Después `bodyTop`**: se posiciona a partir de la posición de `bodyDown`.
3. **Después `bodyCounter`**: se posiciona entre los dos anteriores.
4. **Por último `createFixture()`**: necesita que ambos bodies existan para asignarles sus formas.

Si alteraras este orden — por ejemplo, creando `bodyTop` antes de `bodyDown` — obtendrías un `NullPointerException` porque `createBodyPipeTop()` accede a `bodyDown.getPosition()`.

---

## Paso 4 — Posicionar la tubería superior relativamente

### El cálculo de posición

La posición de `bodyTop` se calcula a partir de `bodyDown`:

```java
private void createBodyPipeTop() {
    BodyDef def = new BodyDef();
    def.position.x = bodyDown.getPosition().x;
    def.position.y = bodyDown.getPosition().y + PIPE_HEIGHT + COUNTER_HEIGHT;

    def.type = BodyDef.BodyType.KinematicBody;
    bodyTop = world.createBody(def);
    bodyTop.setUserData(Utils.USER_PIPE_UP);
    bodyTop.setLinearVelocity(SPEED, 0);
}
```

La línea clave es el cálculo de `def.position.y`. Recuerda que `bodyDown.getPosition()` devuelve el **centro** del body inferior. Para llegar al centro del body superior, necesitamos sumar:

```
Centro bodyDown:    bodyDown.getPosition().y
                         │
                    ┌─────┴─────┐
                    │  bodyDown │  ← mitad superior = PIPE_HEIGHT/2
                    └───────────┘
                         ↑ PIPE_HEIGHT/2
                         
                    (hueco)        ← COUNTER_HEIGHT completo
                         
                         ↑ PIPE_HEIGHT/2
                    ┌───────────┐
                    │  bodyTop  │  ← mitad inferior = PIPE_HEIGHT/2
                    └─────┬─────┘
                         │
Centro bodyTop:     bodyDown.y + PIPE_HEIGHT + COUNTER_HEIGHT
```

Pero espera: ¿no deberían ser `PIPE_HEIGHT/2 + COUNTER_HEIGHT + PIPE_HEIGHT/2`? Sí, y eso simplifica exactamente a `PIPE_HEIGHT + COUNTER_HEIGHT`. Las dos mitades de `PIPE_HEIGHT` (la mitad superior de bodyDown y la mitad inferior de bodyTop) se suman en un `PIPE_HEIGHT` completo.

### ¿Por qué posicionamiento relativo?

Usar la posición del bodyDown como referencia tiene una ventaja: si cambias la posición del par de tuberías en `GameScreen`, solo tocas un parámetro. La tubería superior y el sensor se recalculan automáticamente.

---

## Paso 5 — El movimiento: velocidad en los tres cuerpos

### `setLinearVelocity()` en `KinematicBody`

Hasta ahora las tuberías estaban quietas. Esta rama añade movimiento horizontal con una sola línea por body:

```java
bodyDown.setLinearVelocity(SPEED, 0);     // en createBodyPipeDown()
bodyTop.setLinearVelocity(SPEED, 0);      // en createBodyPipeTop()
bodyCounter.setLinearVelocity(SPEED, 0);  // en createCounter()
```

Recordemos de la rama 3 que un `KinematicBody` no se ve afectado por la gravedad ni por fuerzas externas. Su velocidad solo cambia si tú la cambias explícitamente. Al establecer `SPEED = -0.2f`, los tres cuerpos se moverán 0.2 unidades por segundo hacia la izquierda, indefinidamente.

### ¿Por qué la misma velocidad en los tres?

Si un body se moviera más rápido que otro, el par de tuberías se "desalinearía" — la tubería superior se separaría de la inferior. Los tres deben moverse siempre juntos, como si fueran una única pieza rígida.

> ⚠️ **Error típico:** Olvidar poner `setLinearVelocity()` en uno de los tres bodies. El resultado es que dos tuberías se mueven y una se queda quieta — un bug visual muy evidente pero cuya causa no siempre es obvia.

### ¿Por qué no mover el Actor en vez de los bodies?

Podrías pensar: "¿por qué no mover el `Actor` con `setPosition()` en `act()` y sincronizar los bodies?". La respuesta tiene que ver con las colisiones. Box2D detecta colisiones entre **bodies**, no entre actores. Si mueves el actor pero no el body, el pájaro no chocará con la tubería aunque visualmente estén en la misma posición. El body es la "verdad física"; el actor es solo la representación visual.

---

## Paso 6 — El sensor contador: colisión sin contacto

### ¿Qué es un sensor?

Un sensor en Box2D es una fixture que **detecta solapamiento** pero **no produce respuesta física**. El pájaro puede atravesarlo sin rebotar, pero Box2D notifica que ha ocurrido un contacto. Es perfecto para detectar cuándo el pájaro cruza el hueco entre las tuberías.

### Analogía: una célula fotoeléctrica

Piensa en los sensores de las puertas de ascensor: un haz de luz invisible cruza la puerta. Cuando algo lo interrumpe, el sensor lo detecta, pero el haz no bloquea físicamente el paso. Nuestro `bodyCounter` funciona igual: ocupa un espacio entre las tuberías, y cuando el pájaro lo cruza, Box2D lo registra como un contacto, pero el pájaro pasa sin obstáculo.

### Implementación del sensor

```java
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
    this.fixtureCounter.setSensor(true);        // ← esto lo convierte en sensor
    this.fixtureCounter.setUserData(Utils.USER_COUNTER);
    polygonShape.dispose();
}
```

Analicemos cada parte:

**Posición vertical**: `(bodyDown.y + bodyTop.y) / 2f` — el punto medio entre los centros de ambos bodies. Esto coloca el sensor exactamente en el centro del hueco.

**Forma estrecha**: `setAsBox(0.1f, 0.90f)` crea un rectángulo muy estrecho (0.2 de ancho total) y relativamente alto (1.8 de alto total). Es estrecho para que el pájaro lo cruce en pocos frames, evitando detecciones dobles. Si fuera ancho, el pájaro podría estar "dentro" del sensor durante varios frames y contar puntos múltiples veces.

**`setSensor(true)`**: esta es la línea crucial. Sin ella, el body sería sólido y bloquearía al pájaro en el hueco. Con `setSensor(true)`, la fixture deja de producir respuestas físicas pero sigue generando eventos de contacto que podremos escuchar con un `ContactListener` en ramas futuras.

**`userData` en la fixture**: igual que en Bird, el identificador `USER_COUNTER` se asigna a la fixture, no al body. Esto nos permitirá distinguir en el `ContactListener` si el pájaro ha tocado una tubería (muerte) o el sensor (punto).

### Diferencia clave: `userData` en body vs fixture

Observa que en esta clase se mezclan ambos enfoques:

```java
// userData en el BODY:
bodyDown.setUserData(Utils.USER_PIPE_DOWN);
bodyTop.setUserData(Utils.USER_PIPE_UP);

// userData en la FIXTURE:
this.fixtureCounter.setUserData(Utils.USER_COUNTER);
```

Ambos funcionan. Cuando implementemos el `ContactListener`, necesitaremos comprobar tanto `body.getUserData()` como `fixture.getUserData()` dependiendo de qué cuerpo estemos inspeccionando.

---

## Paso 7 — Reutilizar la `PolygonShape` para ambas tuberías

### Optimización sutil en `createFixture()`

```java
private void createFixture() {
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(PIPE_WIDTH / 2, PIPE_HEIGHT / 2);

    this.fixtureDown = bodyDown.createFixture(shape, 8);
    this.fixtureTop = bodyTop.createFixture(shape, 8);

    shape.dispose();
}
```

Fíjate en que se crea **una sola** `PolygonShape` y se usa para las dos fixtures. Esto es posible porque ambas tuberías tienen exactamente las mismas dimensiones (`PIPE_WIDTH × PIPE_HEIGHT`). Cuando llamas a `createFixture()`, Box2D **copia** los datos de la shape internamente, así que puedes reutilizar el mismo objeto shape y hacer `dispose()` una sola vez al final.

Es el mismo principio de la rama 3: `shape.dispose()` libera la memoria nativa, y no afecta a las fixtures ya creadas porque Box2D ya copió lo que necesitaba.

### ¿Por qué el sensor tiene su propia shape?

El sensor usa dimensiones diferentes (`0.1f × 0.90f`) y se crea en un método separado (`createCounter()`). No comparte shape con las tuberías.

---

## Paso 8 — Dibujar ambas tuberías

### El `draw()` actualizado

```java
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
```

Se dibuja primero la tubería inferior y después la superior. Para cada una:
1. Se sincroniza `setPosition()` con la posición del body correspondiente (restando la mitad para ir del centro a la esquina inferior izquierda).
2. Se dibuja con `batch.draw()` usando la textura correspondiente.

**Detalle importante:** se llama a `setPosition()` dos veces. Esto significa que la posición final del Actor (la que usaría `getX()`/`getY()` fuera de `draw()`) será la de la tubería superior. Esto no causa problemas porque no usamos la posición del Actor para nada más — todo se calcula desde los bodies.

### ¿Y el sensor? ¿No se dibuja?

No. El sensor es **invisible**. No tiene textura asociada ni debe tenerla — es un concepto puramente físico. Solo lo verás con el `Box2DDebugRenderer` activado: aparecerá como un rectángulo estrecho entre las dos tuberías.

---

## Paso 9 — Liberar recursos: el `detach()` ampliado

```java
public void detach() {
    bodyDown.destroyFixture(fixtureDown);
    world.destroyBody(bodyDown);

    this.bodyTop.destroyFixture(fixtureTop);
    this.world.destroyBody(this.bodyTop);
}
```

Se destruyen las fixtures y bodies de ambas tuberías. Observa que **no se destruye `bodyCounter`**. Esto podría considerarse un olvido, pero en la práctica, cuando destruyes el `World` completo al hacer `dispose()` de `GameScreen`, se destruyen automáticamente todos los bodies restantes.

> ⚠️ **Nota para el alumno:** En un juego completo, sería buena práctica destruir también el `bodyCounter` y su fixture en `detach()` para no dejar recursos sueltos. Podrías añadirlo como ejercicio.

---

## Paso 10 — Cambios en `GameScreen` y `AssetMan`

### Cargar la textura de la tubería superior

En `AssetMan` se añade un nuevo método:

```java
public TextureRegion getPipeTopTR() {
    return this.textureAtlas.findRegion(PIPE_UP);
}
```

### `show()` actualizado

```java
@Override
public void show() {
    addBackground();
    addBird();

    TextureRegion pipeTRDown = mainGame.assetManager.getPipeDownTR();
    TextureRegion pipeTRTop = mainGame.assetManager.getPipeTopTR();
    this.pipes = new Pipes(this.world, pipeTRDown, pipeTRTop, new Vector2(3.75f, 0f));
    this.stage.addActor(this.pipes);
}
```

Fíjate en que la posición ha cambiado de `(3.75f, 2f)` en la rama 4 a `(3.75f, 0f)`. Este `0f` es la posición vertical del **centro** de la tubería inferior. Como `PIPE_HEIGHT = 4f`, la mitad inferior del body (`4/2 = 2 unidades`) queda por debajo de `y=0`, es decir, fuera de la pantalla visible. La tubería inferior asoma parcialmente por el borde inferior de la pantalla.

### `hide()` ahora limpia las tuberías

```java
@Override
public void hide() {
    this.bird.detach();
    this.bird.remove();

    this.pipes.detach();    // ← NUEVO: destruir bodies de tuberías
    this.pipes.remove();    // ← NUEVO: quitar del Stage
}
```

En la rama 4, `hide()` solo limpiaba el pájaro. Ahora también limpia las tuberías. Si no hiciéramos esto, los bodies de las tuberías seguirían existiendo en el `World` después de cambiar de pantalla.

### Nuevas constantes en `Utils`

```java
// Identificador de cuerpos — NUEVO en esta rama:
public static final String USER_PIPE_UP = "pipeUp";
public static final String USER_COUNTER = "counter";
```

Ahora tenemos cuatro tipos de cuerpo identificados: `USER_BIRD`, `USER_PIPE_DOWN`, `USER_PIPE_UP` y `USER_COUNTER`. Estos identificadores serán fundamentales cuando implementemos el `ContactListener` para distinguir qué ha chocado con qué.

---

## Errores comunes en esta rama

### 1. "La tubería superior aparece pegada a la inferior"

```java
// ❌ MAL: olvidar sumar COUNTER_HEIGHT
def.position.y = bodyDown.getPosition().y + PIPE_HEIGHT;

// ✓ BIEN: incluir el hueco
def.position.y = bodyDown.getPosition().y + PIPE_HEIGHT + COUNTER_HEIGHT;
```

Sin `COUNTER_HEIGHT`, las dos tuberías quedan pegadas sin hueco entre ellas.

### 2. "El pájaro choca con algo invisible en el hueco"

```java
// ❌ MAL: olvidar setSensor(true)
this.fixtureCounter = bodyCounter.createFixture(polygonShape, 3);
// Sin setSensor → el counter es sólido y bloquea al pájaro

// ✓ BIEN: marcar como sensor
this.fixtureCounter = bodyCounter.createFixture(polygonShape, 3);
this.fixtureCounter.setSensor(true);
```

### 3. "Una tubería se queda atrás mientras las otras se mueven"

Recuerda: los tres bodies necesitan `setLinearVelocity(SPEED, 0)`. Si olvidas ponerlo en uno, ese body se queda quieto mientras los otros dos avanzan.

### 4. "NullPointerException al crear bodyTop"

Si creas `bodyTop` antes de `bodyDown`, la línea `bodyDown.getPosition().x` lanza NPE porque `bodyDown` todavía es `null`. El orden de creación importa.

### 5. "La textura de la tubería superior está al revés"

En el atlas, `pipeDown` y `pipeUp` son texturas diferentes (una apunta hacia arriba y la otra hacia abajo). Si intercambias las texturas en el constructor, la visual quedará invertida aunque las colisiones funcionen correctamente. Verifica que `trpDown` corresponde a `PIPE_DOWN` y `trpTop` a `PIPE_UP`.

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
    public static final String USER_PIPE_UP = "pipeUp";
    public static final String USER_COUNTER = "counter";
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
import static com.mygdx.game.extra.Utils.PIPE_UP;

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

    // TEXTURA DE LA TUBERÍA SUPERIOR
    public TextureRegion getPipeTopTR() {
        return this.textureAtlas.findRegion(PIPE_UP);
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
        TextureRegion pipeTRTop = mainGame.assetManager.getPipeTopTR();
        this.pipes = new Pipes(this.world, pipeTRDown, pipeTRTop, new Vector2(3.75f, 0f));
        this.stage.addActor(this.pipes);
    }

    @Override
    public void hide() {
        this.bird.detach();
        this.bird.remove();

        this.pipes.detach();
        this.pipes.remove();
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

**Objetivo:** Entender la relación entre los tres cuerpos y experimentar con los parámetros del par de tuberías.

1. **Ejecuta el proyecto.** Deberías ver el par de tuberías (inferior y superior) moviéndose lentamente hacia la izquierda. Con el debugRenderer activado, verás tres cajas: las dos tuberías y el rectángulo estrecho del sensor entre ellas.

2. **Ajusta el hueco:** Cambia `COUNTER_HEIGHT` de `2f` a `1f`. ¿Puede el pájaro pasar por el hueco? ¿Y con `4f`?

3. **Velocidad de las tuberías:** Cambia `SPEED` de `-0.2f` a `-1f` y luego a `-3f`. ¿En qué punto se vuelve injugable?

4. **Desincroniza los bodies:** Comenta la línea `bodyTop.setLinearVelocity(SPEED, 0)` en `createBodyPipeTop()`. ¿Qué ocurre visualmente? ¿Por qué?

5. **Quita el sensor:** Comenta `this.fixtureCounter.setSensor(true)`. ¿Qué pasa cuando el pájaro intenta pasar por el hueco?

6. **Posición inicial:** Cambia la posición en `GameScreen.show()` de `(3.75f, 0f)` a `(3.75f, 2f)`. ¿Cómo afecta esto a la posición del hueco? Calcula mentalmente: si `bodyDown` está en `y=2`, ¿en qué `y` estará el centro del hueco?

7. **Pregunta para reflexionar:** El `detach()` no destruye el `bodyCounter` ni su fixture. ¿Es esto un problema real? ¿Cuándo se liberaría esa memoria? ¿Cómo lo arreglarías?
