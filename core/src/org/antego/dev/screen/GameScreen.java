package org.antego.dev.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;

import org.antego.dev.PlanesGame;
import org.antego.dev.events.AccelEvent;
import org.antego.dev.events.FireEvent;
import org.antego.dev.events.GameEvent;
import org.antego.dev.events.RotateEvent;
import org.antego.dev.events.ShootEvent;
import org.antego.dev.events.StatusEvent;
import org.antego.dev.network.OnlineSession;
import org.antego.dev.util.BulletData;
import org.antego.dev.util.Constants;
import org.antego.dev.util.PlaneData;
import org.antego.dev.util.WorldUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.antego.dev.util.Constants.*;

public class GameScreen extends InputAdapter implements Screen {
    private final Queue<GameEvent> networkEvents = new ConcurrentLinkedQueue<GameEvent>();
    private final PlanesGame game;
    private final OnlineSession session;
    private final ScheduledExecutorService updatePlaneStateService = Executors.newSingleThreadScheduledExecutor();

    private final World world;
    private final Body plane;
    private final Body enemyPlane;
    private final Box2DDebugRenderer renderer;
    private final OrthographicCamera camera;
    private final int fontHeight;

    private final Set<Body> bodiesToDestroy = Collections.synchronizedSet(new HashSet<Body>());
    private final Map<Long, Body> bulletMap = new HashMap<Long, Body>();
    private final SpriteBatch batch = new SpriteBatch();
    private final ParticleEffect stars = new ParticleEffect();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final Vector2 bulletPos = new Vector2();
    private final BitmapFont font;

    private volatile boolean doShoot;
    private volatile boolean changeScreen;
    private long lastShootTime;
    private float accumulator;

    public GameScreen(PlanesGame game, final OnlineSession session) {
        this.game = game;
        this.session = session;

        world = WorldUtils.createWorld();
        plane = WorldUtils.createPlane(world, session.getWorldParameters());
        enemyPlane = WorldUtils.createExternalPlane(world, session.getWorldParameters());

        font = new BitmapFont(Gdx.files.internal("data/arialBig.fnt"));
        font.setColor(Color.WHITE);
        this.fontHeight = Gdx.graphics.getHeight() / 8;
        float scale = (float)fontHeight / 128;
        font.getData().setScale(scale);

        camera = new OrthographicCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0f);
        camera.update();

        renderer = new Box2DDebugRenderer();
        Gdx.input.setInputProcessor(this);
        updatePlaneStateService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                session.getSenderThread().addToQueue(new StatusEvent(plane.getPosition(), plane.getLinearVelocity().cpy(), plane.getAngle()));
            }
        }, 0, 1, TimeUnit.SECONDS);

        int pixelWidth = Gdx.graphics.getWidth();
        int pixelHeight = Gdx.graphics.getHeight();
        stars.load(new FileHandle("stars.particles"), new FileHandle(""));
        ParticleEmitter emitter = stars.getEmitters().first();
        emitter.getSpawnHeight().setHigh(pixelHeight);
        emitter.getSpawnWidth().setHigh(pixelWidth);
        emitter.setMaxParticleCount((int) (pixelHeight * pixelWidth * STARS_DENSITY));
        emitter.setMinParticleCount((int) (pixelHeight * pixelWidth * STARS_DENSITY));
        emitter.setPosition(pixelWidth / 2, pixelHeight / 2);
        stars.start();
        stars.update(1);

        world.setContactListener(new ContactListener() {
            private void doContactLogic(Body plane, Body bullet) {
                bodiesToDestroy.add(bullet);
//                bullet.getUserData().setStateExplode();
                if(plane == enemyPlane) {
                    int numOfHits = ++((PlaneData) plane.getUserData()).numOfHits;
                    session.getSenderThread().addToQueue(new ShootEvent());
                    if (numOfHits >= 3) {
//                    ((PlaneData) plane.getUserData()).setStateExplode();
                        glyphLayout.setText(font, "You won!");
                        swapInputProcessor();
                    }
                }
            }

            @Override
            public void beginContact(Contact contact) {
                Body body1 = contact.getFixtureA().getBody();
                Body body2 = contact.getFixtureB().getBody();
                if (body1.getUserData() instanceof BulletData &&
                        body2.getUserData() instanceof PlaneData) {
                    doContactLogic(body2, body1);
                } else if (body2.getUserData() instanceof BulletData &&
                        body1.getUserData() instanceof PlaneData) {
                    doContactLogic(body1, body2);
                }
            }

            @Override
            public void endContact(Contact contact) {
            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {
            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {
            }
        });
    }

    private void swapInputProcessor() {
        Gdx.input.setInputProcessor(new EndGameInputProcessor());
    }

    @Override
    public void show() {
        session.registerGameScreen(this);
    }

    @Override
    public void render(float delta) {
        //Clear the screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Fixed timestep
        accumulator += delta;

        while (accumulator >= TIME_STEP) {
            destroyBullets();

            GameEvent gameEvent = networkEvents.poll();
            if (gameEvent != null) {
                applyEvent(gameEvent);
            }
            checkBoundsForBullets();
            checkBounds(plane);
            checkBounds(enemyPlane);
            accelerate(plane);
            accelerate(enemyPlane);
            rotateAndTruncateVelocity(plane);
            rotateAndTruncateVelocity(enemyPlane);

            if (doShoot) {
                doShoot();
                doShoot = false;
            }

            world.step(TIME_STEP, 6, 2);
            accumulator -= TIME_STEP;
        }
        batch.begin();
        stars.draw(batch);
        Iterator<Body> iter = bulletMap.values().iterator();
        while (iter.hasNext()) {
            Body bullet = iter.next();
            Sprite sprite = ((BulletData) bullet.getUserData()).getSprite();
            sprite.rotate(bullet.getLinearVelocity().angle() - sprite.getRotation() - 90);
            bulletPos.x = bullet.getPosition().x;
            bulletPos.y = bullet.getPosition().y;
            WorldUtils.toScreen(bulletPos);
            bulletPos.x = bulletPos.x - sprite.getWidth() / 2;
            bulletPos.y = bulletPos.y - sprite.getHeight() / 2;
            sprite.setPosition(bulletPos.x, bulletPos.y);
            sprite.draw(batch);
        }
        Sprite sprite = drawSpriteOnBody(enemyPlane);
        sprite.draw(batch);
        sprite = drawSpriteOnBody(plane);
        sprite.draw(batch);
        if (!glyphLayout.toString().isEmpty()) {
            float w = glyphLayout.width;
            float h = glyphLayout.height;
            font.draw(batch, glyphLayout, Gdx.graphics.getWidth() / 2 - w / 2, Gdx.graphics.getHeight() / 2 + h / 2);
        }
        batch.end();
        renderer.render(world, camera.combined);
        if (changeScreen) {
            dispose();
            game.setScreen(new StartGameScreen(game));
        }
    }

    private Vector2 posToConvert = new Vector2();
    private Sprite drawSpriteOnBody(Body plane) {
        Sprite sprite = ((PlaneData)plane.getUserData()).getSprite();
        Vector2 spriteOffset = ((PlaneData)plane.getUserData()).getSpriteOffset().cpy();
        spriteOffset = WorldUtils.toScreen(spriteOffset);
        sprite.rotate(plane.getLinearVelocity().angle() - sprite.getRotation());
        posToConvert.x = plane.getPosition().x;
        posToConvert.y = plane.getPosition().y;
        WorldUtils.toScreen(posToConvert);
        posToConvert.sub(spriteOffset);
        sprite.setPosition(posToConvert.x, posToConvert.y);
        return sprite;
    }

    private void checkBoundsForBullets() {
        for (Body bullet : bulletMap.values()) {
            checkBounds(bullet);
        }
    }

    private void destroyBullets() {
        Iterator<Map.Entry<Long, Body>> iter = bulletMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, Body> entry = iter.next();
            Body body = entry.getValue();
            if(System.nanoTime() - entry.getKey() > BULLET_TTL || bodiesToDestroy.contains(body)) {
                world.destroyBody(body);
                iter.remove();
                bodiesToDestroy.remove(body);
            }
        }
    }

    private void doShoot() {
        Body bullet = WorldUtils.createBullet(world, false);
        Sprite sprite = ((PlaneData)plane.getUserData()).getSprite();
        float originOffset = ((PlaneData)plane.getUserData()).getSpriteOffset().x / WorldUtils.TO_WORLD_WIDTH / sprite.getWidth();
        float bulletOffset = (1 - originOffset) * WorldUtils.TO_WORLD_WIDTH * sprite.getWidth();
        bullet.setTransform(plane.getPosition().add(new Vector2(bulletOffset, 0).rotateRad(plane.getAngle())), 0);
        Vector2 tanVelo = new Vector2(0, plane.getAngularVelocity());
        //todo angular velocity
        Vector2 bulletVelocity = Constants.BULLET_VELOCITY.cpy().add(tanVelo).rotateRad(plane.getAngle());
        bullet.setLinearVelocity(bulletVelocity);
        lastShootTime = System.nanoTime();
        bulletMap.put(lastShootTime, bullet);
        session.getSenderThread().addToQueue(new FireEvent(bulletVelocity, bullet.getPosition()));
    }

    private void checkBounds(Body plane) {
        float planeX = plane.getPosition().x;
        float planeY = plane.getPosition().y;
        float angle = plane.getAngle();

        if (planeX < -VIEWPORT_BUFFER) {
            plane.setTransform(planeX + VIEWPORT_WIDTH + 2 * VIEWPORT_BUFFER, planeY, angle);
        } else if (planeX > VIEWPORT_WIDTH + VIEWPORT_BUFFER) {
            plane.setTransform(planeX - VIEWPORT_WIDTH - 2 * VIEWPORT_BUFFER, planeY, angle);
        }

        if (planeY < -VIEWPORT_BUFFER) {
            plane.setTransform(planeX, planeY + VIEWPORT_HEIGHT + 2 * VIEWPORT_BUFFER, angle);
        } else if (planeY > VIEWPORT_HEIGHT + VIEWPORT_BUFFER) {
            plane.setTransform(planeX, planeY - VIEWPORT_HEIGHT - 2 * VIEWPORT_BUFFER, angle);
        }
    }

    private void rotateAndTruncateVelocity(Body plane) {
        float curAngle = (float) (Math.toDegrees(plane.getAngle()) % 360);
        curAngle = curAngle <= 0 ? curAngle + 360 : curAngle;
        Vector2 vel = plane.getLinearVelocity().cpy();
        float deltaAngle = curAngle - vel.angle();
        vel.rotate(deltaAngle);
        plane.setLinearVelocity(vel);
    }

    private void accelerate(Body plane) {
        Vector2 velocity = plane.getLinearVelocity();
        velocity.scl(1 + TIME_STEP * ((PlaneData) plane.getUserData()).acceleration / velocity.len());
        if (velocity.len() < MIN_VELOCITY) {
            velocity.scl(MIN_VELOCITY / velocity.len());
        } else if (velocity.len() > MAX_VELOCITY) {
            velocity.scl(MAX_VELOCITY / velocity.len());
        }
        plane.setLinearVelocity(velocity);
    }

    private void applyEvent(GameEvent gameEvent) {
        //todo visitor pattern
        if (gameEvent instanceof StatusEvent) {
            enemyPlane.setTransform(((StatusEvent) gameEvent).getPosition(), ((StatusEvent) gameEvent).getAngle());
            enemyPlane.setLinearVelocity(((StatusEvent) gameEvent).getVelocity());
        } else if (gameEvent instanceof RotateEvent) {
            enemyPlane.setAngularVelocity(((RotateEvent) gameEvent).getAngularVelocity());
        } else if (gameEvent instanceof FireEvent) {
            Body bullet = WorldUtils.createBullet(world, true);
            bullet.setTransform(((FireEvent) gameEvent).getPosition(), 0f);
            bullet.setLinearVelocity(((FireEvent) gameEvent).getVelocity());
            bulletMap.put(System.nanoTime(), bullet);
        } else if (gameEvent instanceof ShootEvent) {
            int hits = ++((PlaneData)plane.getUserData()).numOfHits;
            if (hits >= 3) {
                glyphLayout.setText(font, "You lose!");
                swapInputProcessor();
            }
        } else if (gameEvent instanceof AccelEvent) {
            ((PlaneData)enemyPlane.getUserData()).acceleration = ((AccelEvent) gameEvent).getAcceleration();
        }
    }

    public void addGameEvent(GameEvent event) {
        networkEvents.add(event);
    }

    @Override
    public boolean keyDown(int keycode) {
        Float rotateSpeed = null;
        Float acceleration = null;
        if (keycode == Input.Keys.RIGHT) {
            rotateSpeed = -angularSpeed;
        } else if (keycode == Input.Keys.LEFT) {
            rotateSpeed = angularSpeed;
        } else if (keycode == Input.Keys.UP) {
            acceleration = ACCELERATION;
        } else if (keycode == Input.Keys.DOWN) {
            acceleration = -ACCELERATION;
        } else if (keycode == Input.Keys.SPACE && System.nanoTime() - lastShootTime > SHOOT_PERIOD) {
            doShoot = true;
        }

        if (rotateSpeed != null) {
            session.getSenderThread().addToQueue(new RotateEvent(rotateSpeed));
            plane.setAngularVelocity(rotateSpeed);
        } else if (acceleration != null) {
            session.getSenderThread().addToQueue(new AccelEvent(acceleration));
            ((PlaneData)plane.getUserData()).acceleration = acceleration;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        Float rotateSpeed = null;
        Float acceleration = null;
        if (keycode == Input.Keys.RIGHT) {
            rotateSpeed = 0f;
        } else if (keycode == Input.Keys.LEFT) {
            rotateSpeed = 0f;
        } else if (keycode == Input.Keys.UP) {
            acceleration = 0f;
        } else if (keycode == Input.Keys.DOWN) {
            acceleration = 0f;
        }

        if (rotateSpeed != null) {
            session.getSenderThread().addToQueue(new RotateEvent(rotateSpeed));
            plane.setAngularVelocity(rotateSpeed);
        } else if (acceleration != null) {
            session.getSenderThread().addToQueue(new AccelEvent(acceleration));
            ((PlaneData)plane.getUserData()).acceleration = acceleration;
        }
        return false;
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        updatePlaneStateService.shutdown();
        session.closeSocket();
        world.dispose();
        Gdx.app.log(LOG_TAG, "socket closed in GameScreen");
    }

    private class EndGameInputProcessor extends InputAdapter {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            changeScreen = true;
            return false;
        }
    }
}
