package com.iesfa.fb.actors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.iesfa.fb.extra.Utils;

public class Bird extends Actor {

    //Todo 1. Creamos diferentes estados del juego y la velocidad de impulso que se le dará al pj
    private static final int STATE_NORMAL = 0;
    private static final int STATE_DEAD = 1;
    private static final float JUMP_SPEED = 5f;

    //Todo 2.Controlamos el estado con un atributo
    private int state;

    private Animation<AtlasRegion> birdAnimation;
    private Vector2 position;


    private World world;

    private float stateTime;


    private Body body;

    private Fixture fixture;


    public Bird(World world, Animation<AtlasRegion> animation, Vector2 position) {
        this.birdAnimation = animation;
        this.position      = position;
        this.world         = world;
        stateTime = 0f;
        createBody();
        createFixture();

    }


    public void createBody(){
        //Creamos BodyDef
        BodyDef bodyDef = new BodyDef();
        //Position
        bodyDef.position.set(position);

        //tipo
        bodyDef.type = BodyDef.BodyType.DynamicBody;

        //createBody de mundo
        this.body = this.world.createBody(bodyDef);
    }


    public void createFixture(){
        //Shape
        CircleShape circle = new CircleShape();
        //radio
        circle.setRadius(0.3f);

        //createFixture
        this.fixture = this.body.createFixture(circle,8);
        //setUserData  --> Utils -> identificadores de cuerpos
        this.fixture.setUserData(Utils.USER_BIRD);
        //dispose
        circle.dispose();
    }



    @Override
    public void act(float delta) {
        //Todo 3. Controlamos el toque de la pantalla
        boolean jump = Gdx.input.justTouched();
        //Todo 4. Si el estado es normal, se le da un impulso
        if(jump && this.state == STATE_NORMAL){
            this.body.setLinearVelocity(0, JUMP_SPEED);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        setPosition(body.getPosition().x, body.getPosition().y);
        batch.draw(this.birdAnimation.getKeyFrame(stateTime,true),getX() - 0.3f,getY()- 0.25f, 0.6f,0.5f);

        stateTime += Gdx.graphics.getDeltaTime();

    }


    public void detach(){

        //(body) destroyFixture
        this.body.destroyFixture(this.fixture);
        //(world) destroyBody
        this.world.destroyBody(this.body);

    }
}
