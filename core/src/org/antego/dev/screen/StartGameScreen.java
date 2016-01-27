package org.antego.dev.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import org.antego.dev.PlanesGame;
import org.antego.dev.network.OnlineSession;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by anton on 02.01.2016.
 */
public class StartGameScreen implements Screen {
    private SpriteBatch batch = new SpriteBatch();
    private Skin skin;
    private Stage stage = new Stage();
    private PlanesGame game;
    private volatile OnlineSession onlineSession;
    private final Object sessionLock = new Object();
    private ExecutorService sessionExecutor = Executors.newSingleThreadExecutor();
    private final ParticleEffect stars = new ParticleEffect();
    private volatile boolean toMainScreen;
    private Animation loadingAnimation;
    private Label statusLabel;
    private volatile Future<Void> result;


    public StartGameScreen(PlanesGame game) {
        this.game = game;

        skin = new Skin(Gdx.files.internal("data/uiskin.json"));
        Skin skinSmall = new Skin(Gdx.files.internal("data/uiskin-old.json"));

        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("loadingIndicator.atlas"));
        loadingAnimation = new Animation(1/30, atlas.getRegions());
        int pixelWidth = Gdx.graphics.getWidth();
        int pixelHeight = Gdx.graphics.getHeight();
        stars.load(new FileHandle("menuStars.particles"), new FileHandle(""));
        ParticleEmitter emitter = stars.getEmitters().first();
        emitter.setPosition(pixelWidth / 2, pixelHeight / 2);
        stars.start();

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        Table table = new Table(skin);
        table.debug();
        final Label startLabel = new Label("Connect", skin);
        final Label cancelLabel = new Label("Back", skin);
        statusLabel = new Label("", skinSmall);

        startLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                synchronized (sessionLock) {
                    if (onlineSession == null) {
                        onlineSession = new OnlineSession();
                        result = sessionExecutor.submit(onlineSession);
                        onlineSession.setStarted(true);
                        statusLabel.setColor(Color.WHITE);
                        statusLabel.setText("Waiting for opponent");
                    }
                }
            }
        });
        cancelLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                synchronized (sessionLock) {
                    if (onlineSession != null && onlineSession.isStarted()) {
                        onlineSession.cancel();
                    }
                }
                toMainScreen = true;
            }
        });
        table.add(startLabel).spaceBottom(40);
        table.row();
        table.add(cancelLabel);
        table.row();
        table.add(statusLabel);
        rootTable.add(table);
        stage.addActor(rootTable);
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        stars.draw(batch, delta);
        stage.draw();
        //batch.draw(loadingAnimation.getKeyFrame(delta, true), statusLabel.getX() - 20, statusLabel.getY());
        batch.end();
        if (toMainScreen) {
            game.setScreen(new MenuScreen(game));
            dispose();
        } else if (result != null && result.isDone()) {
            try {
                result.get(10, TimeUnit.MILLISECONDS);
                GameScreen gameScreen = new GameScreen(game, onlineSession);
                game.setScreen(gameScreen);
                dispose();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                onlineSession = null;
                statusLabel.setText("Couldn't connect to server");
                statusLabel.setColor(Color.RED);
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }
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
        if (onlineSession != null && onlineSession.getWorldParameters() == null) {
            onlineSession.closeSocket();
        }
        batch.dispose();
        sessionExecutor.shutdown();
    }
}
