package com.iesfa.fb.screens;

import static com.iesfa.fb.extra.Utils.SCREEN_HEIGHT;
import static com.iesfa.fb.extra.Utils.SCREEN_WIDTH;
import static com.iesfa.fb.extra.Utils.USER_BIRD;
import static com.iesfa.fb.extra.Utils.USER_COUNTER;
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
import com.iesfa.fb.MainGame;
import com.iesfa.fb.actors.Bird;
import com.iesfa.fb.actors.Pipes;

public class GameScreen extends BaseScreen implements ContactListener {



    private final float TIME_TO_SPAWN_PIPES = 1.5f;
    private float timeToCreatePipe;


    private Stage stage;
    private Bird bird;
    private Array<Pipes> arrayPipes;

    private Image background;
    private Music musicbg;

    private World world;


    private OrthographicCamera fontCamera;
    private BitmapFont score;

    //Todo 3. Creamos una valiabre contador....
    private int scoreNumber;

    // ----- DEPURACIÓN DE LA FÍSICA ----- //
    private Box2DDebugRenderer debugRenderer;
    private OrthographicCamera ortCamera;

    public GameScreen(MainGame mainGame){
        super(mainGame);

        this.world = new World(new Vector2(0,-10),true);
        //Todo 5. Le pasamos al mundo el objeto que implemente la interfaz contactListener (en este caso será la propia instancia de GameScreen)
        this.world.setContactListener(this);

        FitViewport fitViewport = new FitViewport(WORLD_WIDTH,WORLD_HEIGTH);
        this.stage = new Stage(fitViewport);

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

    //Creamos un mét odo para configurar tod o lo relacionado con el texto de la puntuación
    //Nos acordamos de llamar a dicho mét odo en el constructor
    private void prepareScore(){

        //Todo 3.1 ...Y la inicializamos a 0
        this.scoreNumber = 0;

        this.score = this.mainGame.assetManager.getFont();
        this.score.getData().scale(1f);

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
            this.timeToCreatePipe+=delta;
            if(this.timeToCreatePipe >= TIME_TO_SPAWN_PIPES) {
                this.timeToCreatePipe-=TIME_TO_SPAWN_PIPES;
                float posRandomY = MathUtils.random(0f, 2f);
                //Cambiamos la coordenada x para que se cree fuera de la pantalla (5f)
                Pipes pipes = new Pipes(this.world, pipeDownTexture, pipeTopTexture, new Vector2(5f, posRandomY)); //Posición de la tubería inferior
                arrayPipes.add(pipes);
                this.stage.addActor(pipes);
            }
        }
    }

    //Creamos un métod o para eliminar pipes
    public void removePipes(){
        for (Pipes pipe : this.arrayPipes) {
            if(!world.isLocked()) {
                if(pipe.isOutOfScreen()) {
                    pipe.detach();
                    pipe.remove();
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

        addPipes(delta);

        this.stage.getBatch().setProjectionMatrix(ortCamera.combined);
        this.stage.act();
        this.world.step(delta,6,2); //Porqué 6 y 2? Por que así lo dice la documentación.
        this.stage.draw();


        //Actualizamos la cámara para que aplique cualquier cambio en las matrices internas.
        this.ortCamera.update();
        // Se le pasa el mundo físico y las matrices de la camara (combined)
        this.debugRenderer.render(this.world, this.ortCamera.combined);

        removePipes();

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

        this.musicbg.stop();
    }

    @Override
    public void dispose() {


        this.stage.dispose();

        this.world.dispose();

    }
    /// ********************************************* ///
    /// *************** COLISIONES ****************** ///
    /// ********************************************* ///
    //Todo 6. Nos creamos un método auxiliar areColider, que nos ayude a determinar qué objetos han colisionado
    public boolean areColider(Contact contact, Object objA, Object objB){
        return (contact.getFixtureA().getUserData().equals(objA) && contact.getFixtureB().getUserData().equals(objB)) ||
            (contact.getFixtureA().getUserData().equals(objB) && contact.getFixtureB().getUserData().equals(objA));

    }

    //Método que se llamará cada vez que se produzca cualquier contacto
    @Override
    public void beginContact(Contact contact) {
        //Todo 7. Si 'han colisionado' el pájaro con el contador sumamos 1 al contador...
        if(areColider(contact,USER_BIRD,USER_COUNTER)){
            this.scoreNumber++;
        }else {
            //Todo 8 En cualquier otro caso significaría que el pájaro ha colisionado con algún otro elemento y se acaba la partida
            //Todo 8.1 Lanzamos el método hurt del pájaro para que se cambie el estado a DEAD
            this.bird.hurt();
            //Todo 8.2 Recorremos el array de Pipes para parar los que se encuentren creados en este momento
            for(Pipes pipe:this.arrayPipes){
                pipe.stopPipes();
            }
            //Todo 8.3 Paramos la música
            this.musicbg.stop();
            //Todo 8.4 Se lanza la secuencia de acciones,cuya última será el pasar a la ventana de GameOverScreen
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
    public void endContact(Contact contact) {

    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }
}
