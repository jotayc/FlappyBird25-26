package com.iesfa.fb.screens;

import static com.iesfa.fb.extra.Utils.WORLD_HEIGTH;
import static com.iesfa.fb.extra.Utils.WORLD_WIDTH;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.iesfa.fb.MainGame;
import com.iesfa.fb.actors.Bird;

public class GameScreen extends BaseScreen {
    //Todo alumno: Toda pantalla debe tener una 'escena' que controle los elementos que aparecen en cada
    // pantalla (Podríamos decir que sería nuestro director de escena)

    private Stage stage;
    private Bird bird;
    private Image background;


    public GameScreen(MainGame mainGame) {
        super(mainGame);
        //Inicializamos el stage creando previamente nuestro viewport
        FitViewport fitViewport = new FitViewport(WORLD_WIDTH,WORLD_HEIGTH);
        this.stage = new Stage(fitViewport);
    }

    @Override
    public void show() {
        super.show();

        addBackground();
        addBird();


    }

    public void addBird(){
        Animation<TextureAtlas.AtlasRegion> birdSprite = mainGame.assetManager.getBirdAnimation();
        this.bird = new Bird(birdSprite, new Vector2(1.35f ,4.75f ));
        this.stage.addActor(this.bird);
    }

    //Creamos un metodo que configure el fondos
    public void addBackground() {
        this.background = new Image(mainGame.assetManager.getBackground());
        this.background.setPosition(0, 0);
        this.background.setSize(WORLD_WIDTH, WORLD_HEIGTH);
        this.stage.addActor(this.background);
    }
    @Override
    public void render(float delta) {

        //Ordenamos al stage que dibuje.
        this.stage.draw();

    }

    @Override
    public void dispose() {

        //Nos acordamos de eliminar los recursos del stage
        this.stage.dispose();

    }

}
