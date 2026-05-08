package com.iesfa.fb.screens;

import static com.iesfa.fb.extra.Utils.SCREEN_HEIGHT;
import static com.iesfa.fb.extra.Utils.SCREEN_WIDTH;
import static com.iesfa.fb.extra.Utils.USER_FLOOR;
import static com.iesfa.fb.extra.Utils.USER_ROOF;
import static com.iesfa.fb.extra.Utils.WORLD_HEIGTH;
import static com.iesfa.fb.extra.Utils.WORLD_WIDTH;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
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
import com.iesfa.fb.MainGame;
import com.iesfa.fb.actors.Bird;
import com.iesfa.fb.actors.Pipes;

public class GameScreen extends BaseScreen {


    //Todo 1* Tenemos que crear una constante para indicar cada cuanto tiempo queremos que se cree
    // una tubería
    private final float TIME_TO_SPAWN_PIPES = 1.5f;
    private float timeToCreatePipe;


    private Stage stage;
    private Bird bird;
    //Todo 1.1* Borramos el atributo unico Pipes, y creamos un array de Pipes. ATENCIÓN SE USA LA CLASE 'Array' de la biblioteca de LIBGDX NO DE JAVA!!!!
    private Array<Pipes> arrayPipes;

    private Image background;
    private Music musicbg;

    private World world;

    //TODO 9.INICIO SCORE: Para añadir un texto con la puntuación es necesario una cámara extra,
    // ya que las fuentes son uno de los pocos elementos que no se pueden añadir en función
    // de las medidas del mundo, sino que se hará en función de las medidas de la pantalla.
    // Para ello necesitaremos otra cámara que proyectarán simultaneamente, una el mundo del juego
    // y otra solo la fuente con la puntuación. Así como crearnos un Bitmap font para manejar el texto
    private OrthographicCamera fontCamera;
    private BitmapFont score;

    // ----- DEPURACIÓN DE LA FÍSICA ----- //
    private Box2DDebugRenderer debugRenderer;
    private OrthographicCamera ortCamera;

    public GameScreen(MainGame mainGame){
        super(mainGame);

        this.world = new World(new Vector2(0,-10),true);

        FitViewport fitViewport = new FitViewport(WORLD_WIDTH,WORLD_HEIGTH);
        this.stage = new Stage(fitViewport);

        //Todo 1.2 Inicializamos el array y la variable que almacenará el tiempo
        this.arrayPipes = new Array();
        this.timeToCreatePipe = 0f;

        this.musicbg = this.mainGame.assetManager.getMusicBG();

        // ---- DEPURACIÓN ---- //
        this.ortCamera = (OrthographicCamera) this.stage.getCamera();
        this.debugRenderer = new Box2DDebugRenderer();

        prepareScore();


    }

    @Override
    public void show() {
        addBackground();
        addRoof();
        addFloor();
        addBird();


        //loop
        this.musicbg.setLooping(true);
        //ajustamos el volumen (0 min - 1 max)
        this.musicbg.setVolume(0.3f);
        //Reproducimos
        this.musicbg.play();

    }

    //Creamos un método para configurar tod o lo relacionado con el texto de la puntuación
    //Nos acordamos de llamar a dicho método en el constructor
    private void prepareScore(){
        //Todo 11. Cargamos la fuente y configuramos la escala (vamos probando el tamaño
        this.score = this.mainGame.assetManager.getFont();
        this.score.getData().scale(1f);

        //Todo 12. Creamos la cámara, y se le da el tamaño de la PANTALLA (EN PIXELES) y luego se actualiza
        this.fontCamera = new OrthographicCamera();
        this.fontCamera.setToOrtho(false, SCREEN_WIDTH,SCREEN_HEIGHT);
        this.fontCamera.update();

    }

    public void addRoof(){
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        Body body = world.createBody(bodyDef);

        EdgeShape edge = new EdgeShape();
        edge.set(0,WORLD_HEIGTH,WORLD_WIDTH,WORLD_HEIGTH);
        body.createFixture(edge, 1).setUserData(USER_ROOF);
        edge.dispose();
    }

    private void addFloor() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(WORLD_WIDTH / 2f, 0.6f);
        bodyDef.type = BodyDef.BodyType.StaticBody;
        Body body = world.createBody(bodyDef);


        PolygonShape edge = new PolygonShape();
        edge.setAsBox(2.3f, 0.5f);
        body.createFixture(edge, 3).setUserData(USER_FLOOR);
        edge.dispose();
    }

    public void addBird(){
        //Cargamos la animación del pájaro
        Animation<AtlasRegion> birdSprite = mainGame.assetManager.getBirdAnimation();
        Sound soundBird = this.mainGame.assetManager.getJumpSound();
        //Creamos la instancia del pajaro pasandole la referencia del mundo, su animación,
        // el sonido y posición en el mundo físico
        this.bird = new Bird(this.world,birdSprite,soundBird, new Vector2(1.35f ,4.75f ));
        //Añadimos el pajaro a la escena
        this.stage.addActor(this.bird);
    }

    public void addPipes(float delta){
        TextureRegion pipeDownTexture = mainGame.assetManager.getPipeBottom();
        TextureRegion pipeTopTexture = mainGame.assetManager.getPipeTop();

        //Como ambas tuberías están en la misma clase solo debemos instanciar un objeto

        if(bird.getState() == Bird.STATE_NORMAL) {
            //Todo 3. Acumulamos delta hasta que llegue al tiempo que hemos establecido para que cree la siguiente tubería.
            this.timeToCreatePipe+=delta;
            //Todo 4. Si el tiempo acumulado es mayor que el tiempo que hemos establecido, se crea una tubería...
            if(this.timeToCreatePipe >= TIME_TO_SPAWN_PIPES) {
                //Todo 4.1 ... y le restamos el tiempo a la variable acumulada para que vuelva el contador a 0.
                this.timeToCreatePipe-=TIME_TO_SPAWN_PIPES;
                float posRandomY = MathUtils.random(0f, 2f);
                //Cambiamos la coordenada x para que se cree fuera de la pantalla (5f)
                Pipes pipes = new Pipes(this.world, pipeDownTexture, pipeTopTexture, new Vector2(5f, posRandomY)); //Posición de la tubería inferior
                arrayPipes.add(pipes);
                this.stage.addActor(pipes);
            }
        }
    }

    //Creamos un método para eliminar pipes
    public void removePipes(){
        for (Pipes pipe : this.arrayPipes) {
            //Todo 6. Si el mundo no está bloqueado, es decir, que no esté actualizando la física en ese preciso momento...
            if(!world.isLocked()) {
                //Todo 6.1...y la tubería en cuestión está fuera de la pantalla.
                if(pipe.isOutOfScreen()) {
                    //Todo 6.2 Eliminamos los recursos
                    pipe.detach();
                    //Todo 6.3 La eliminamos del escenario
                    pipe.remove();

                    //Todo 6.4 La eliminamos del array
                    arrayPipes.removeValue(pipe,false);
                }
            }
        }
    }

    public void addBackground(){
        this.background = new Image(mainGame.assetManager.getBackground());
        this.background.setPosition(0,0);
        this.background.setSize(WORLD_WIDTH,WORLD_HEIGTH);
        this.stage.addActor(this.background);
    }

    @Override
    public void render(float delta) {

        //Todo 7.Añadimos las tuberías en función del tiempo (delta)
        addPipes(delta);

        //Todo 13.1 Justo antes de dibujar el mundo, le volvemos a pasar al batch, los datos de
        // la cámara del mundo, para que vuelva a representar tod o en función del tamaño de este
        this.stage.getBatch().setProjectionMatrix(ortCamera.combined);
        this.stage.act();
        this.world.step(delta,6,2); //Porqué 6 y 2? Por que así lo dice la documentación.
        this.stage.draw();


        //Actualizamos la cámara para que aplique cualquier cambio en las matrices internas.
        this.ortCamera.update();
        // Se le pasa el mundo físico y las matrices de la camara (combined)
        this.debugRenderer.render(this.world, this.ortCamera.combined);

        //Todo 8 Final. Eliminamos las tuberías que vayan saliendose de la pantalla
        removePipes();


        //Todo 13.Cargamos la matriz de proyección con los datos de la cámara de la fuente,
        // para que proyecte el texto con las dimensiones en píxeles
        this.stage.getBatch().setProjectionMatrix(this.fontCamera.combined);
        this.stage.getBatch().begin();
        this.score.draw(this.stage.getBatch(), ""+arrayPipes.size,SCREEN_WIDTH/2, 725);
        this.stage.getBatch().end();
    }

    @Override
    public void hide() {


        //detach
        this.bird.detach();
        //remove
        this.bird.remove();


        //Todo 11.Paramos la música cuando se oculte la pantalla
        this.musicbg.stop();
    }

    @Override
    public void dispose() {


        this.stage.dispose();

        this.world.dispose();

    }

}
