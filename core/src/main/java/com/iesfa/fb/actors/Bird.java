package com.iesfa.fb.actors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
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

    public static final int STATE_NORMAL = 0;
    public static final int STATE_DEAD = 1;
    private static final float JUMP_SPEED = 5f;

    public int state;

    private Animation<AtlasRegion> birdAnimation;
    private Vector2 position;

    private World world;
    private float stateTime;

    private Body body;
    private Fixture fixture;

    //Todo 3. Creamos sonido para el objeto bird.
    private Sound jumpSound;

    //Todo 4. Modificar el constructor para añadir el recurso del sonido.
    public Bird(World world, Animation<AtlasRegion> animation,Sound sound, Vector2 position) {
        this.birdAnimation = animation;
        this.position      = position;
        this.world         = world;
        this.jumpSound     = sound;
        stateTime = 0f;
        createBody();
        createFixture();

    }

    public int getState(){
        return this.state;
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
        //setUserData  --> Utils -> identificadores de cuerpos
        this.body.setUserData(Utils.USER_BIRD);
    }


    public void createFixture(){
        //Shape
        CircleShape circle = new CircleShape();
        //radio
        circle.setRadius(0.3f);

        //createFixture
        this.fixture = this.body.createFixture(circle,8);
        //dispose
        circle.dispose();
    }



    @Override
    public void act(float delta) {
        boolean jump = Gdx.input.justTouched();
        if(jump && this.state == STATE_NORMAL){

            this.jumpSound.play();
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
