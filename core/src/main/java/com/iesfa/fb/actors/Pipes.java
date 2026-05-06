package com.iesfa.fb.actors;

import static com.iesfa.fb.extra.Utils.USER_COUNTER;
import static com.iesfa.fb.extra.Utils.USER_PIPE_BOTTOM;
import static com.iesfa.fb.extra.Utils.USER_PIPE_TOP;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class Pipes extends Actor {

    private static final float PIPE_WIDTH = 0.85f;
    private static final float PIPE_HEIGHT = 4f;
    private static final float SPACE_BETWEEN_PIPES = 2f;
    private static final float PIPES_SPEED = 0f;


    private TextureRegion pipeDownTR;
    private TextureRegion pipeTopTR;

    private Body bodyDown;
    private Body bodyTop;
    private Body bodyCounter;

    private Fixture fixtureDown;
    private Fixture fixtureTop;
    private Fixture fixtureCounter;

    private World world;

    public Pipes(World world, TextureRegion trpDown, TextureRegion trpTop, Vector2 position) {
        this.world = world;
        this.pipeDownTR = trpDown;
        this.pipeTopTR = trpTop;
        createBodyPipeDown(position);
        createBodyPipeTop(); //No se le pasa la posición porque irá en función de la posición de la tubería de abajo
        createFixture();
        createCounter();
    }



    private void createBodyPipeDown(Vector2 position) {
        BodyDef def = new BodyDef();
        def.position.set(position);
        def.type = BodyDef.BodyType.KinematicBody;
        bodyDown = world.createBody(def);
        bodyDown.setUserData(USER_PIPE_BOTTOM);
        bodyDown.setLinearVelocity(PIPES_SPEED,0);

    }

    private void createBodyPipeTop() {
        BodyDef def = new BodyDef();
        def.position.x = bodyDown.getPosition().x;  //
        def.position.y =   bodyDown.getPosition().y + PIPE_HEIGHT + SPACE_BETWEEN_PIPES;
        def.type = BodyDef.BodyType.KinematicBody;
        bodyTop = world.createBody(def);
        bodyTop.setUserData(USER_PIPE_TOP);
        bodyTop.setLinearVelocity(PIPES_SPEED,0);

    }



    private void createFixture() {
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(PIPE_WIDTH /2, PIPE_HEIGHT /2 );

        this.fixtureDown = bodyDown.createFixture(shape,8);

        this.fixtureTop = bodyTop.createFixture(shape, 8);
        shape.dispose();
    }

    //Todo **alumno**: crear body y forma del contador y posicionarlo en el mundo
    public void createCounter(){
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(bodyDown.getPosition().x, (bodyDown.getPosition().y + bodyTop.getPosition().y) / 2f);
        bodyDef.type = BodyDef.BodyType.KinematicBody;
        this.bodyCounter = this.world.createBody(bodyDef);
        this.bodyCounter.setLinearVelocity(Pipes.PIPES_SPEED,0f);

        PolygonShape polygonShape = new PolygonShape();
        polygonShape.setAsBox(0.1f,0.90f);

        this.fixtureCounter = bodyCounter.createFixture(polygonShape,3);
        this.fixtureCounter.setSensor(true); // Sensor
        this.fixtureCounter.setUserData(USER_COUNTER);
        polygonShape.dispose();
    }

    //Todo 5. Creamos un método que nos diga si el objeto pipes está fuera de la pantalla
    public boolean isOutOfScreen(){
        return this.bodyDown.getPosition().x <= -2f;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        setPosition(bodyDown.getPosition().x, bodyDown.getPosition().y);
        batch.draw(this.pipeDownTR,
            bodyDown.getPosition().x - PIPE_WIDTH/2,
            bodyDown.getPosition().y - PIPE_HEIGHT/2,
            PIPE_WIDTH,
            PIPE_HEIGHT);

        batch.draw(this.pipeTopTR,
            bodyTop.getPosition().x - PIPE_WIDTH/2,
            bodyTop.getPosition().y - PIPE_HEIGHT/2,
            PIPE_WIDTH,
            PIPE_HEIGHT);
    }


    public void detach(){
        bodyDown.destroyFixture(fixtureDown);
        world.destroyBody(bodyDown);

        bodyTop.destroyFixture(fixtureTop);
        world.destroyBody(bodyTop);
    }
}
