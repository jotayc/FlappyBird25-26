package com.iesfa.fb;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.iesfa.fb.extra.AssetMan;
import com.iesfa.fb.screens.GameOverScreen;
import com.iesfa.fb.screens.GameScreen;
import com.iesfa.fb.screens.GetReadyScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class MainGame extends Game {
    //Instancia de la pantalla durante el juego
    public GameScreen gameScreen;

    // Crear instancia de la pantalla de GetReady
    public GetReadyScreen getReadyScreen;

    // Crear instancia de la pantalla de GameOver
    public GameOverScreen gameOverScreen;

    @Override
    public void create() {

        this.gameScreen = new GameScreen(this);

        //Scene2d nos ayuda a manejar las diferentes instancias de las diferentes pantallas que
        //compondrá nuestro juego.
        setScreen(this.gameScreen);
    }
}
