package com.iesfa.fb.screens;

import static com.iesfa.fb.extra.Utils.WORLD_HEIGTH;
import static com.iesfa.fb.extra.Utils.WORLD_WIDTH;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
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


    private World world;


    private Box2DDebugRenderer debugRenderer;
    private OrthographicCamera ortCamera;

    public GameScreen(MainGame mainGame){
        super(mainGame);

        this.world = new World(new Vector2(0,-10),true);

        FitViewport fitViewport = new FitViewport(WORLD_WIDTH,WORLD_HEIGTH);
        this.stage = new Stage(fitViewport);

        this.ortCamera = (OrthographicCamera) this.stage.getCamera();
        this.debugRenderer = new Box2DDebugRenderer();



    }


    //Todo alumno: Crear un método que añada el 'cuerpo' y la 'forma' del suelo


    @Override
    public void show() {


        addBackground();
        Animation<AtlasRegion> birdSprite = mainGame.assetManager.getBirdAnimation();
        //Cargamos la textura de la tubería inferior
        TextureRegion pipeDownTexture = mainGame.assetManager.getPipeDownTR();

        //Añadimos el pajaro a la escena
        this.bird = new Bird(this.world,birdSprite, new Vector2(1.35f ,4.75f ));

        //Todo 12. Creamos una instancia de Pipes y se lo pasamos al escenario
        this.pipes = new Pipes(this.world, pipeDownTexture,new Vector2(3.75f,2f));
        this.stage.addActor(this.bird);
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

        //Todo 13. liberamos el objeto pipe
        this.pipes.detach();
        this.pipes.remove();
    }

    @Override
    public void dispose() {


        this.stage.dispose();

        this.world.dispose();

    }

}
