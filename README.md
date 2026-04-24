# 🐦 Construyendo Flappy Bird — Rama `2.AssetManager`
## El pájaro aparece en pantalla

> *La estructura ya existe. Ahora el juego necesita ver algo. En esta rama preparas los sprites con una herramienta externa, los cargas en memoria de forma eficiente y consigues que el pájaro aparezca animado en pantalla por primera vez.*

---

## Paso 1 — Crear el atlas de sprites con GDX Texture Packer GUI

Antes de escribir una sola línea de código Java, necesitas preparar los recursos gráficos. Un **atlas** es una única imagen PNG que contiene todos los sprites del juego organizados, acompañada de un fichero `.atlas` que describe exactamente dónde está cada sprite dentro de esa imagen.

### ¿Por qué un atlas y no un PNG por sprite?

Cada vez que la GPU cambia de textura para dibujar algo diferente, realiza un **texture swap**. Es una operación costosa. Con un atlas, todos los sprites están en la misma textura: sin importar cuántos sprites distintos dibujes en un frame, solo hay **un texture swap por frame**.

```
Sin atlas:                          Con atlas:
bird1.png  → texture swap           FBAtlas.png → texture swap (único)
bird2.png  → texture swap               bird1   → solo coordenadas internas
bird3.png  → texture swap               bird2   → solo coordenadas internas
pipe.png   → texture swap               bird3   → solo coordenadas internas
...                                     pipe    → solo coordenadas internas
```

### GDX Texture Packer GUI: la herramienta

**GDX Texture Packer GUI** es una aplicación de escritorio gratuita y de código abierto que actúa como interfaz visual sobre el `TexturePacker` de LibGDX. Permite crear y gestionar atlases sin escribir código. Descárgala desde:

```
https://github.com/crashinvaders/gdx-texture-packer-gui/releases
```

Requiere Java instalado en el sistema. Está disponible para Windows, Linux y macOS.

### Flujo de trabajo paso a paso

**1. Abrir la aplicación y crear un nuevo proyecto**

Al arrancar la app verás el panel principal con una lista de packs vacía. Haz clic en el botón **+** (New pack) para crear un nuevo pack. Asígnale el nombre `FBAtlas`.

**2. Configurar el directorio de entrada**

En el panel del pack, localiza la sección **Input files** y añade la carpeta que contiene tus PNGs individuales usando el botón **Add input**:

```
bird1.png
bird2.png
bird3.png
pipe.png
```

El nombre de cada región en el atlas será el nombre del fichero sin extensión: `bird1.png` → región `"bird1"`. Este nombre es el que usarás en el código para recuperar cada sprite.

**3. Configurar el directorio de salida**

En la sección **Output directory**, establece la ruta `android/assets` de tu proyecto. Los ficheros generados se escribirán directamente ahí, donde LibGDX los encontrará al arrancar.

**4. Ajustar la configuración del pack**

Para este proyecto, la configuración relevante es:

| Ajuste | Valor | Motivo |
|--------|-------|--------|
| Max width / Max height | 1024 | Tamaño máximo del PNG generado |
| Filter min / Filter mag | Nearest | Evita borrosidad en pixel art |
| Legacy format | ✅ activado | Compatible con proyectos LibGDX anteriores a 1.9.13 |

**5. Empaquetar**

Pulsa el botón **Pack**. La herramienta genera dos ficheros en `android/assets/`:

```
android/assets/
├── FBAtlas.png      ← imagen única con todos los sprites empaquetados
└── FBAtlas.atlas    ← descriptor de texto con las coordenadas de cada región
```

### El fichero `.atlas`: qué contiene

El fichero `.atlas` es texto plano. Cada entrada describe el nombre de la región y sus coordenadas dentro del PNG:

```
FBAtlas.png
size: 256,128
format: RGBA8888
filter: Nearest,Nearest
repeat: none

bird1
  rotate: false
  xy: 2, 2
  size: 34, 24
  orig: 34, 24
  offset: 0, 0
  index: -1
bird2
  rotate: false
  xy: 38, 2
  size: 34, 24
  ...
```

No editarás este fichero a mano. Lo genera la herramienta y lo lee LibGDX. Lo único que necesitas saber son los **nombres** de las regiones (`"bird1"`, `"bird2"`, `"pipe"`...) porque los usarás en el código.

> ⚠️ Solo hay que ejecutar GDX Texture Packer GUI cuando añades o modificas sprites. Una vez generado el atlas no es necesario volver a ejecutarlo hasta el próximo cambio de assets.

---

## Paso 2 — La clase `AssetMan`

Con el atlas ya en `android/assets/`, necesitas cargarlo en memoria al arrancar el juego. Para eso existe `AssetMan`: una clase que centraliza la carga, el acceso y la liberación de todos los recursos.

### ¿Por qué no cargar el atlas directamente en cada pantalla?

Podrías hacer `new TextureAtlas("FBAtlas.atlas")` dentro de `GameScreen`. Funcionaría, pero tendrías el recurso duplicado si varias pantallas lo necesitan, y tendrías que acordarte de llamar a `dispose()` en cada sitio. Con `AssetMan` en `MainGame`, hay **una sola instancia del atlas** compartida por todas las pantallas, y se libera todo en un único `dispose()`.

### Atributos

```java
public class AssetMan {

    private AssetManager assetManager;  // el gestor de LibGDX
    private TextureAtlas textureAtlas;  // el atlas cargado y listo para usar

    // ... (constructor y métodos a continuación)
}
```

`AssetManager` es la clase de LibGDX que gestiona la carga y liberación de recursos. `TextureAtlas` es el atlas una vez cargado, a partir del cual se extraen las regiones individuales.

### Constructor: encolar, cargar y almacenar

```java
    public AssetMan() {
        assetManager = new AssetManager();

        // 1. Encolar: indica qué recursos quieres cargar (aún no los carga)
        assetManager.load(ATLAS_MAP, TextureAtlas.class);

        // 2. Cargar: bloquea el hilo hasta que todo lo encolado esté listo
        assetManager.finishLoading();

        // 3. Obtener: recupera el atlas ya cargado
        textureAtlas = assetManager.get(ATLAS_MAP);
    }
```

El patrón `load → finishLoading → get` es el flujo estándar de `AssetManager`. `finishLoading()` bloquea el hilo hasta que todos los recursos encolados están listos. Es adecuado aquí porque ocurre una sola vez al arrancar la aplicación, no dentro del game loop.

### Método `getBirdAnimation()`

```java
    public Animation<TextureAtlas.AtlasRegion> getBirdAnimation() {
        return new Animation<>(
            0.33f,                              // duración de cada frame: 0.33 segundos
            textureAtlas.findRegion("bird1"),   // región "bird1" del atlas
            textureAtlas.findRegion("bird2"),   // región "bird2" del atlas
            textureAtlas.findRegion("bird3")    // región "bird3" del atlas
        );
    }
```

`findRegion(nombre)` devuelve un `AtlasRegion`, que contiene información adicional sobre la región (posición original, si fue rotada al empaquetar, etc.). No crea una textura nueva: apunta a una zona de la imagen ya cargada.

> ⚠️ **`findRegion` puede devolver `null`.** Si el nombre que le pasas no existe en el atlas — porque el PNG tiene un nombre distinto o el atlas no se regeneró tras cambiar los assets — el método devuelve `null`. Cuando `Animation` intente usar ese `null` como frame, el juego lanzará un `NullPointerException` en tiempo de ejecución. El error más frecuente: las constantes `BIRD1`, `BIRD2`, `BIRD3` en `Utils` no coinciden exactamente con los nombres de fichero que se le dieron a los sprites al empaquetar el atlas.

### Método `dispose()`

```java
public class AssetMan {

    // ...

    public void dispose() {
        assetManager.dispose();  // libera el atlas y cualquier otro recurso cargado
    }
}
```

Llamar a `assetManager.dispose()` libera **todo** lo que él cargó. No hay que llamar a `dispose()` de forma individual en cada recurso.

### Dónde vive `AssetMan` y cuándo se crea

`AssetMan` es un atributo público de `MainGame` y se instancia **antes** que las pantallas, porque las pantallas lo necesitan:

```java
// En MainGame:
public AssetMan assetMan;

@Override
public void create() {
    assetMan       = new AssetMan();           // ← primero los recursos
    gameScreen     = new GameScreen(this);
    gameOverScreen = new GameOverScreen(this);
    getReadyScreen = new GetReadyScreen(this);
    setScreen(gameScreen);
}
```

---

## Paso 3 — Stage y Actor: el teatro donde vive el juego

Imagina un **teatro**: hay un escenario con actores que representan una obra. En cada momento del espectáculo, los actores saben qué hacer (moverse, hablar, actuar) y el público los puede ver en sus posiciones correctas. LibGDX funciona igual.

### Stage: el escenario

`Stage` es el escenario donde ocurre todo el juego. Como un director teatral, se encarga de organizar a todos los personajes (llamados `Actor`) y coordinar lo que hacen en cada momento.

```java
Stage stage = new Stage();

// En cada frame:
stage.act(delta);   // "¡Acción!" → todos los actores hacen su parte
stage.draw();       // El público ve el resultado final
```

**El orden es importante**: primero los actores actúan (`act`), luego el público los ve (`draw`). Como en una obra de teatro real.

### Actor: los personajes del teatro

Un `Actor` es cualquier elemento del juego que puede moverse, cambiar o interactuar: el pájaro, las tuberías, el fondo, un botón de menú. Cada `Actor` sabe dos cosas esenciales:

1. **Qué hacer en cada momento** → método `act(float delta)`
2. **Cómo aparecer en pantalla** → método `draw(Batch batch, float parentAlpha)`

```java
public class Bird extends Actor {
    @Override
    public void act(float delta) {
        // "¿Qué hago ahora?" → actualizar animación, moverme, etc.
    }
    
    @Override 
    public void draw(Batch batch, float parentAlpha) {
        // "¿Cómo me ven?" → dibujarme en mi posición actual
    }
}
```

### `delta`: el metrónomo del teatro

En una obra teatral, todos los actores se mueven al mismo ritmo marcado por el director. En LibGDX, `delta` es ese ritmo: el tiempo en segundos que ha pasado desde la última vez que todos actuaron.

Si `delta = 0.016` (60 FPS), han pasado 16 milisegundos. Si el pájaro se mueve "1 unidad por segundo", en este frame se moverá `1 × 0.016 = 0.016` unidades. **La clave**: sin importar si el juego va a 30, 60 o 120 FPS, la velocidad real del pájaro será siempre la misma.

```java
// ❌ Mal: velocidad depende de los FPS
posicionY += 2f;          

// ✅ Bien: velocidad consistente en cualquier máquina  
posicionY += 2f * delta;  
```

### `stateTime`: el cronómetro personal de cada actor

Cada `Actor` puede tener su propio cronómetro interno. Para el pájaro, `stateTime` cuenta cuánto tiempo lleva volando desde que empezó la animación:

```java
@Override
public void act(float delta) {
    super.act(delta);
    stateTime += delta;  // sumar tiempo → el cronómetro avanza
}
```

Con `stateTime = 0.66` segundos, la animación sabe que debe mostrar el frame número 2. Es como decirle a un actor: "han pasado 40 segundos desde que empezó tu escena, ¿qué deberías estar haciendo?"

### Ejemplo práctico: la clase `Bird`

```java
public class Bird extends Actor {

    private Animation<TextureAtlas.AtlasRegion> birdAnimation;
    private Vector2 position;
    float stateTime;

    public Bird(Animation<TextureAtlas.AtlasRegion> animation, Vector2 position) {
        this.birdAnimation = animation;
        this.position = position;
        stateTime = 0f;
    }

    @Override
    public void act(float delta) {
        // act() permanece vacío en esta implementación
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.draw(
            birdAnimation.getKeyFrame(stateTime, true),  // frame actual según el tiempo
            position.x, position.y, 0.6f, 0.5f         // posición y tamaño fijos
        );
        stateTime += Gdx.graphics.getDeltaTime();       // actualizar cronómetro
    }
}
```

---

## Paso 4 — Viewport: la cámara del teatro

Ahora que tienes actores en un escenario, necesitas una **cámara** para filmar la obra y que se vea en la pantalla. El viewport es esa cámara: decide qué parte del escenario se ve y cómo se adapta a diferentes tamaños de pantalla.

### La analogía del pintor y el cuadro

Imagina que eres un **pintor** que quiere hacer un cuadro de un paisaje. El paisaje real (el **mundo del juego**) mide "10 metros de ancho × 15 metros de alto". Pero tu lienzo (la **pantalla**) mide solo "30 cm × 40 cm". ¿Cómo haces que el paisaje entero encaje en tu cuadro sin deformarlo?

Eso es exactamente lo que hace un **viewport**: toma el mundo del juego y lo "pinta" en la pantalla manteniendo las proporciones correctas.

### Los dos sistemas de coordenadas

**Sistema del mundo (el paisaje real):**
- Medidas: 4.8 × 8 unidades de juego
- Origen (0,0): esquina inferior izquierda
- No son píxeles: son medidas "del universo del juego"

**Sistema de pantalla (el lienzo):**
- Medidas: 480 × 800 píxeles físicos
- Origen (0,0): esquina inferior izquierda
- Son píxeles reales de tu móvil o monitor

```java
// En el mundo del juego:
bird.setPosition(2.4f, 5.2f);  // pájaro en el centro horizontal, arriba

// En la pantalla se verá en:
// x = 2.4 / 4.8 * 480 = 240 píxeles desde la izquierda  
// y = 5.2 / 8.0 * 800 = 520 píxeles desde abajo
```

### Tipos de viewport: diferentes estrategias de cámara

LibGDX ofrece varias "estrategias de cámara" según lo que quieras conseguir:

**StretchViewport** → "Estira el paisaje para llenar todo el lienzo"
- Usa toda la pantalla disponible
- ❌ Deforma las proporciones si la pantalla no coincide

**FitViewport** → "Encaja el paisaje completo manteniendo las proporciones"
- Nunca deforma nada
- ✅ Añade bandas negras si es necesario

**ExtendViewport** → "Muestra el paisaje completo y algo más si cabe"
- Aumenta el área visible en pantallas más grandes
- ✅ Útil para juegos que se adaptan al tamaño

**ScreenViewport** → "1 unidad del mundo = 1 píxel de pantalla"
- Sin escalado, tamaño fijo
- ✅ Para interfaces de usuario

### FitViewport: la elección de este proyecto

En Flappy Bird usamos `FitViewport` porque queremos que el juego se vea **exactamente igual** en cualquier dispositivo. Es como un pintor que dice: "prefiero un marco con bandas negras antes que deformar mi obra".

```java
FitViewport viewport = new FitViewport(4.8f, 8f);
```

**Ejemplo práctico:**

Pantalla 480×800 (móvil): proporción 480/800 = 0.6  
Mundo 4.8×8: proporción 4.8/8 = 0.6  
→ **Coinciden perfectamente**: sin bandas, escalado directo

Pantalla 600×800 (tablet): proporción 600/800 = 0.75  
Mundo 4.8×8: proporción 4.8/8 = 0.6  
→ **No coinciden**: bandas negras de 60px a cada lado

### Conectar Stage con Viewport

El `Stage` necesita saber qué cámara usar:

```java
Stage stage = new Stage(viewport);

// En resize():
stage.getViewport().update(width, height, false);
```

Cuando cambias el tamaño de la ventana (o giras el móvil), `update()` recalcula cómo encajar el mundo en la nueva pantalla.

---

## Paso 5 — `GameScreen` con Stage y Bird

```java
public class GameScreen extends BaseScreen {

    private Stage stage;
    private Bird  bird;

    public GameScreen(MainGame mainGame) {
        super(mainGame);
        stage = new Stage(new FitViewport(WORLD_WIDTH, WORLD_HEIGHT));
    }

    @Override
    public void show() {
        // show() se llama cada vez que esta pantalla pasa a ser la activa
        bird = new Bird(mainGame.assetMan.getBirdAnimation(), new Vector2(1.35f, 4.75f));
        stage.addActor(bird);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.5f, 0.8f, 1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, false);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
```

Al ejecutar verás el pájaro animado en la posición `(1.35, 4.75)` del mundo.

---

## Código completo de esta rama

### 📄 Utils.java

```java
package com.mygdx.game.extra;

public class Utils {
    public static final int    SCREEN_WIDTH  = 480;
    public static final int    SCREEN_HEIGHT = 800;
    public static final float  WORLD_WIDTH   = 4.8f;
    public static final float  WORLD_HEIGTH  = 8f;    // Nota: typo en el código real
    public static final String ATLAS_MAP     = "FBAtlas";
    public static final String BACKGROUND_IMAGE = "flappy_background";
}
```

### 📄 AssetMan.java

```java
package com.mygdx.game.extra;

import static com.mygdx.game.extra.Utils.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.*;

public class AssetMan {

    private AssetManager assetManager;
    private TextureAtlas textureAtlas;

    public AssetMan() {
        assetManager = new AssetManager();
        assetManager.load(ATLAS_MAP, TextureAtlas.class);
        assetManager.finishLoading();
        textureAtlas = assetManager.get(ATLAS_MAP);
    }

    public AtlasRegion getBackground() {
        return textureAtlas.findRegion(BACKGROUND_IMAGE);
    }

    public Animation<TextureAtlas.AtlasRegion> getBirdAnimation() {
        return new Animation<>(0.33f,
            textureAtlas.findRegion("bird1"),
            textureAtlas.findRegion("bird2"),
            textureAtlas.findRegion("bird3")
        );
    }

    public void dispose() {
        assetManager.dispose();
    }
}
```

### 📄 Bird.java

```java
package com.mygdx.game.actors;

import static com.mygdx.game.extra.Utils.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class Bird extends Actor {

    private Animation<TextureAtlas.AtlasRegion> birdAnimation;
    private Vector2 position;
    float stateTime;

    public Bird(Animation<TextureAtlas.AtlasRegion> animation, Vector2 position) {
        this.birdAnimation = animation;
        this.position = position;
        stateTime = 0f;
    }

    @Override
    public void act(float delta) {
        // act() permanece vacío en esta implementación
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.draw(
            birdAnimation.getKeyFrame(stateTime, true),
            position.x, position.y, 0.6f, 0.5f
        );
        stateTime += Gdx.graphics.getDeltaTime();
    }
}
```

### 📄 MainGame.java

```java
package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.mygdx.game.extra.AssetMan;
import com.mygdx.game.screens.GameOverScreen;
import com.mygdx.game.screens.GameScreen;
import com.mygdx.game.screens.GetReadyScreen;

public class MainGame extends Game {

    public AssetMan       assetMan;
    public GameScreen     gameScreen;
    public GameOverScreen gameOverScreen;
    public GetReadyScreen getReadyScreen;

    @Override
    public void create() {
        assetMan       = new AssetMan();
        gameScreen     = new GameScreen(this);
        gameOverScreen = new GameOverScreen(this);
        getReadyScreen = new GetReadyScreen(this);
        setScreen(gameScreen);
    }
}
```

### 📄 GameScreen.java

```java
package com.mygdx.game.screens;

import static com.mygdx.game.extra.Utils.*;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygdx.game.MainGame;
import com.mygdx.game.actors.Bird;

public class GameScreen extends BaseScreen {

    private Stage stage;
    private Bird bird;
    private Image background;

    public GameScreen(MainGame mainGame) {
        super(mainGame);
        FitViewport fitViewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
        this.stage = new Stage(fitViewport);
    }

    @Override
    public void show() {
        super.show();
        addBackground();
        addBird();
    }

    public void addBird() {
        Animation<AtlasRegion> birdSprite = mainGame.assetMan.getBirdAnimation();
        this.bird = new Bird(birdSprite, new Vector2(1.35f, 4.75f));
        this.stage.addActor(this.bird);
    }

    public void addBackground() {
        this.background = new Image(mainGame.assetMan.getBackground());
        this.background.setPosition(0, 0);
        this.background.setSize(WORLD_WIDTH, WORLD_HEIGHT);
        this.stage.addActor(this.background);
    }

    @Override
    public void render(float delta) {
        this.stage.draw();
    }

    @Override
    public void dispose() {
        this.stage.dispose();
    }
}
```

---

## 🛠️ Ejercicio práctico

**Objetivo:** Entender el flujo completo: atlas → carga → pantalla.

1. Abre GDX Texture Packer GUI. Crea un pack llamado `FBAtlas`, añade los sprites del pájaro y genera el atlas en `android/assets/`. Abre el fichero `.atlas` generado y localiza las entradas `bird1`, `bird2`, `bird3`. ¿Qué coordenadas `xy` tienen?

2. Ejecuta el proyecto. El pájaro debe aparecer animado en el centro de la pantalla.

3. En `Utils`, cambia `WORLD_WIDTH` a `2.4f`. Ejecuta. ¿Qué ocurre con el tamaño visual del pájaro? ¿Por qué cambia si no has tocado el código de `Bird`?

4. Restaura `WORLD_WIDTH = 4.8f`. Cambia `WORLD_HEIGHT` a `16f`. Ejecuta. ¿Aparecen bandas negras? ¿Dónde queda el pájaro en pantalla?

5. Restaura `WORLD_HEIGHT = 8f`. En `getBirdAnimation()` cambia el tiempo de frame de `0.33f` a `0.5f`. ¿Cómo afecta a la animación?
