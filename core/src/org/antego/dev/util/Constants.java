package org.antego.dev.util;

import com.badlogic.gdx.math.Vector2;

/**
 * Created by anton on 28.12.2015.
 */
public class Constants {
    public static final int APP_WIDTH = 800;
    public static final int APP_HEIGHT = 480;
    public static final String LOG_TAG = "Planes log";
    public static final Vector2 WORLD_GRAVITY = new Vector2(0, 0);
    public static final float TIME_STEP = 1 / 300f;

    public static final int VIEWPORT_WIDTH = 40;
    public static final int VIEWPORT_HEIGHT = 24;
    public static final float VIEWPORT_BUFFER = 1.5f;

    public static final float GROUND_X = VIEWPORT_WIDTH / 2;
    public static final float GROUND_Y = 0;
    public static final float GROUND_WIDTH = 50f;
    public static final float GROUND_HEIGHT = 2f;
    public static final float GROUND_DENSITY = 0f;

    public static final float CEILING_WIDTH = 50f;
    public static final float CEILING_HEIGHT = 2f;
    public static final float CEILING_DENSITY = 0f;
    public static final float CEILING_X = VIEWPORT_WIDTH / 2;
    public static final float CEILING_Y = VIEWPORT_HEIGHT + CEILING_HEIGHT / 2;

    public static final float PLANE_WIDTH = 2f;
    public static final float PLANE_HEIGHT = 1f;
    public static final float PLANE_DENSITY = 0.5f;
    public static final float PLANE_GRAVITY_SCALE = 1f;
    public static final Vector2 leftPlanePos = new Vector2(2, 15);
    public static final Vector2 rightPlanePos = new Vector2(18, 15);
    public static final Vector2 velocityVector = new Vector2(3, 0);
    public static final float MAX_VELOCITY = 7;
    public static final float MIN_VELOCITY = 3;

    public static final float angularSpeed = 2 * (float)Math.PI / 3; //оборот за 3 секунды
    public static final String HOST_ADDRESS = "127.0.0.1";

    public static final int HOST_PORT = 9998;
    public static final float BULLET_DENSITY = 1f;
    public static final long SHOOT_PERIOD = 1000*1000*1000;
    public static final Vector2 BULLET_VELOCITY = new Vector2(10, 0);
    public static final long BULLET_TTL = 2000*1000*1000;
    public static final float BULLET_RADIUS = 0.1f;
    public static final float ACCELERATION = 0.5f;
    public static final float STARS_DENSITY = 1 / 800f;
}
