package com.iesfa.fb.screens;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.iesfa.fb.MainGame;
import com.iesfa.fb.actors.Bird;

public class GameScreen extends BaseScreen{
    //Todo alumno: Toda pantalla debe tener una 'escena' que controle los elementos que aparecen en cada
    // pantalla (Podríamos decir que sería nuestro director de escena)
    private Stage stage;
    private Bird bird;

    public GameScreen(MainGame mainGame) {
        super(mainGame);
    }

    @Override
    public void show() {
        super.show();
    }
}
