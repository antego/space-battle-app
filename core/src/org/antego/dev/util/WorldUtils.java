package org.antego.dev.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;

import org.antego.dev.network.WorldParameters;

import java.util.GregorianCalendar;

/**
 * Created by anton on 28.12.2015.
 */
public class WorldUtils {
    private static TextureAtlas atlas;
    public static World createWorld() {
        atlas = new TextureAtlas(Gdx.files.internal("explosions.atlas"));
        return new World(Constants.WORLD_GRAVITY, true);
    }

    public static Body createGround(World world) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(new Vector2(Constants.GROUND_X, Constants.GROUND_Y));
        Body body = world.createBody(bodyDef);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(Constants.GROUND_WIDTH / 2, Constants.GROUND_HEIGHT / 2);
        body.createFixture(shape, Constants.GROUND_DENSITY);
        shape.dispose();
        return body;
    }

    public static Body createCeiling(World world) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(new Vector2(Constants.CEILING_X, Constants.CEILING_Y));
        Body body = world.createBody(bodyDef);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(Constants.CEILING_WIDTH / 2, Constants.CEILING_HEIGHT / 2);
        body.createFixture(shape, Constants.CEILING_DENSITY);
        shape.dispose();
        return body;
    }

    public static Body createPlane(World world, WorldParameters parameters) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(parameters.isLeftPlayerIsHuman() ? Constants.leftPlanePos : Constants.rightPlanePos);
        Body body = world.createBody(bodyDef);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(Constants.PLANE_WIDTH / 2, Constants.PLANE_HEIGHT / 2);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = Constants.PLANE_DENSITY;
        fixtureDef.filter.groupIndex = -1;
        body.createFixture(fixtureDef);
        shape.dispose();
        body.setGravityScale(Constants.PLANE_GRAVITY_SCALE);
        body.setLinearVelocity(parameters.isLeftPlayerIsHuman() ? Constants.velocityVector.cpy() : Constants.velocityVector.cpy().scl(-1));
        //todo test this with ceiling
        body.setFixedRotation(true);
        PlaneData data = new PlaneData();
        data.setSprite(new Sprite(new Texture(Gdx.files.internal("redship.png"))));
        body.setUserData(data);
        return body;
    }

    public static Body createExternalPlane(World world, WorldParameters parameters) {
        Body plane = createPlane(world, parameters);
        plane.setTransform(parameters.isLeftPlayerIsHuman() ? Constants.rightPlanePos : Constants.leftPlanePos, 0);
        plane.setLinearVelocity(parameters.isLeftPlayerIsHuman() ? Constants.velocityVector.cpy().scl(-1) : Constants.velocityVector.cpy());
        PlaneData data = new PlaneData();
        data.setSprite(new Sprite(new Texture(Gdx.files.internal("blueship.png"))));
        plane.setUserData(data);
        return plane;
    }

    public static Body createBullet(World world, boolean enemy) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.bullet = true;
        Body body = world.createBody(bodyDef);
        FixtureDef fixtureDef = new FixtureDef();
        CircleShape shape = new CircleShape();
        shape.setRadius(Constants.BULLET_RADIUS);
        fixtureDef.shape = shape;
        fixtureDef.density = Constants.BULLET_DENSITY;
        body.createFixture(fixtureDef);
        BulletData data = new BulletData();
        Sprite sprite;
        if (enemy) {
            sprite = new Sprite(atlas.findRegion("bullet_2_blue"));
        } else {
            sprite = new Sprite(atlas.findRegion("bullet_2_orange"));
        }
//        sprite.rotate(-90);
        data.setSprite(sprite);
        body.setUserData(data);
        shape.dispose();
        return body;
    }

    public static Vector2 toScreen(Vector2 pos) {
        pos.x = pos.x / Constants.VIEWPORT_WIDTH * Gdx.graphics.getWidth();
        pos.y = pos.y / Constants.VIEWPORT_HEIGHT * Gdx.graphics.getHeight();
        return pos;
    }

    public static Vector2 toWorld(Vector2 pos) {
        pos.x = pos.x * Constants.VIEWPORT_WIDTH / Gdx.graphics.getWidth();
        pos.y = pos.y * Constants.VIEWPORT_HEIGHT / Gdx.graphics.getHeight();
        return pos;
    }
}
