package com.iesfa.fb;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    //Todo 1. Objeto encargado de 'comunicarse' con la tarjeta gráfica
    private SpriteBatch batch;

    //Todo 2. Objeto encargado de cargar la imagen de nuestros archvios a la memoria de la tarjeta gráfica.
    private Texture image;

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");
    }


    @Override
    public void render() {
        //Todo 3. Estas lineas son necesarias para limpiar la pantalla y lo contenido en el buffer gráfico
        // de la iteración anterior
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        //Todo 4.Cuando vayamos a decirle a la gráfica lo que queramos dibujar debemos hacerlo por lotes.
        batch.begin();
        batch.draw(image, 140, 210);
        batch.end();
    }

    @Override
    public void dispose() {
        //Todo 5. Recordamos que tanto la imagen como el batch están cargados en la gráfica. Por lo que
        // cuando salgamos de la pantalla debemos liberar los recursos
        batch.dispose();
        image.dispose();
    }
}
