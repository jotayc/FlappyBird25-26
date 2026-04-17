package com.iesfa.fb.actors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class Bird extends Actor {
    //Creamos el atributo con la animación de regiones de texturas
    private Animation<TextureAtlas.AtlasRegion> birdAnimation;
    private Vector2 position;
    //En el constructor debemos pasarle la animación previamente cargada de AssetMan y la posición
    //donde queramos que se dibuje el actor.

    float stateTime;
    public Bird(Animation<TextureAtlas.AtlasRegion> animation, Vector2 position) {
        this.birdAnimation = animation;
        this.position      = position;

        stateTime = 0f;
    }

    @Override
    public void act(float delta) {

    }


    //Sobrecargamos draw

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.draw(this.birdAnimation.getKeyFrame(stateTime,true),position.x,position.y, 0.6f,0.5f);

        stateTime += Gdx.graphics.getDeltaTime();


    }
}
