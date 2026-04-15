# 🐦 Construyendo Flappy Bird — Rama `2.AssetManager`
## El pájaro aparece en pantalla

> *La estructura ya existe. Ahora el juego necesita ver algo. En esta rama preparas los sprites con una herramienta externa, los cargas en memoria de forma eficiente y consigues que el pájaro aparezca animado en pantalla por primera vez.*

---

## Acto 1 — Crear el atlas de sprites con GDX Texture Packer GUI

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

## Acto 2 — La clase `AssetMan`

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
    //    El segundo argumento es obligatorio: sin él el compilador no infiere el tipo genérico
    textureAtlas = assetManager.get(ATLAS_MAP, TextureAtlas.class);
}
```

El patrón `load → finishLoading → get` es el flujo estándar de `AssetManager`. `finishLoading()` bloquea el hilo hasta que todos los recursos encolados están listos. Es adecuado aquí porque ocurre una sola vez al arrancar la aplicación, no dentro del game loop.

### Método `getBirdAnimation()`

```java
    public Animation<TextureRegion> getBirdAnimation() {
        return new Animation<>(
            0.33f,                              // duración de cada frame: 0.33 segundos
            textureAtlas.findRegion(BIRD1),     // región "bird1" del atlas
            textureAtlas.findRegion(BIRD2),     // región "bird2" del atlas
            textureAtlas.findRegion(BIRD3)      // región "bird3" del atlas
        );
    }
```

`findRegion(nombre)` devuelve una "ventana" al PNG grande correspondiente a ese nombre. No crea una textura nueva: señala una zona de la imagen ya cargada. `Animation<TextureRegion>` encapsula la secuencia de frames y el tiempo entre ellos.

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
