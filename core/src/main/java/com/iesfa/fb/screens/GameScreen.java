package com.iesfa.fb.screens;

import static com.iesfa.fb.extra.Utils.USER_FLOOR;
import static com.iesfa.fb.extra.Utils.USER_ROOF;
import static com.iesfa.fb.extra.Utils.WORLD_HEIGTH;
import static com.iesfa.fb.extra.Utils.WORLD_WIDTH;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.iesfa.fb.MainGame;
import com.iesfa.fb.actors.Bird;
import com.iesfa.fb.actors.Pipes;

public class GameScreen extends BaseScreen {

    private Stage stage;
    private Bird bird;
    private Pipes pipes;

    private Image background;
    //Todo 8. Creamos objeto MusicGame para la musica de fondo
    private Music musicbg;

    private World world;

    // ----- DEPURACIÓN DE LA FÍSICA ----- //
    private Box2DDebugRenderer debugRenderer;
    private OrthographicCamera ortCamera;

    public GameScreen(MainGame mainGame){
        super(mainGame);

        this.world = new World(new Vector2(0,-10),true);

        FitViewport fitViewport = new FitViewport(WORLD_WIDTH,WORLD_HEIGTH);
        this.stage = new Stage(fitViewport);

        //Todo 9. Inicializamos el objeto desde la instancia desde assetMan
        this.musicbg = this.mainGame.assetManager.getMusicBG();

        // ---- DEPURACIÓN ---- //
        this.ortCamera = (OrthographicCamera) this.stage.getCamera();
        this.debugRenderer = new Box2DDebugRenderer();

    }

    @Override
    public void show() {
        addBackground();
        addRoof();
        addFloor();
        addBird();
        addPipes();

        //Todo 10. Reproducimos la música cuando aparezca la pantalla
        //loop
        this.musicbg.setLooping(true);
        //ajustamos el volumen (0 min - 1 max)
        this.musicbg.setVolume(0.3f);
        //Reproducimos
        this.musicbg.play();

    }

    public void addRoof(){
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        Body body = world.createBody(bodyDef);
        body.setUserData(USER_ROOF);

        EdgeShape edge = new EdgeShape();
        edge.set(0,WORLD_HEIGTH,WORLD_WIDTH,WORLD_HEIGTH);
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

    public void addBird(){
        //Cargamos la animación del pájaro
        Animation<AtlasRegion> birdSprite = mainGame.assetManager.getBirdAnimation();
        //Todo 6. Pedimmos a assetManager que nos de el sonido.
        Sound soundBird = this.mainGame.assetManager.getJumpSound();
        //Creamos la instancia del pajaro pasandole la referencia del mundo, su animación,
        // el sonido y posición en el mundo físico
        //Todo 7. Le pasamos al constructor el sonido
        this.bird = new Bird(this.world,birdSprite,soundBird, new Vector2(1.35f ,4.75f ));
        //Añadimos el pajaro a la escena
        this.stage.addActor(this.bird);
    }

    public void addPipes(){
        //Cargamos la textura de la tubería inferior
        TextureRegion pipeDownTexture = mainGame.assetManager.getPipeBottom();
        TextureRegion pipeTopTexture = mainGame.assetManager.getPipeTop();

        float posRandomY = MathUtils.random(0f,2f);
        this.pipes = new Pipes(this.world, pipeDownTexture,pipeTopTexture,new Vector2(3.75f,2f));

        this.stage.addActor(this.pipes);
    }

    public void addBackground(){
        this.background = new Image(mainGame.assetManager.getBackground());
        this.background.setPosition(0,0);
        this.background.setSize(WORLD_WIDTH,WORLD_HEIGTH);
        this.stage.addActor(this.background);
    }

    @Override
    public void render(float delta) {

        this.stage.getBatch().setProjectionMatrix(ortCamera.combined);

        this.stage.act();
        this.world.step(delta,6,2); //Porqué 6 y 2? Por que así lo dice la documentación.
        this.stage.draw();

        //Actualizamos la cámara para que aplique cualquier cambio en las matrices internas.
        this.ortCamera.update();
        // Se le pasa el mundo físico y las matrices de la camara (combined)
        this.debugRenderer.render(this.world, this.ortCamera.combined);
    }

    @Override
    public void hide() {


        //detach
        this.bird.detach();
        //remove
        this.bird.remove();

        this.pipes.detach();
        this.pipes.remove();

        //Todo 11.Paramos la música cuando se oculte la pantalla
        this.musicbg.stop();
    }

    @Override
    public void dispose() {


        this.stage.dispose();

        this.world.dispose();

    }

}
