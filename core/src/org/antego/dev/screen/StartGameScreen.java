package org.antego.dev.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import org.antego.dev.PlanesGame;
import org.antego.dev.events.GameEvent;
import org.antego.dev.network.OnlineSession;
import org.antego.dev.util.Constants;

import static org.antego.dev.util.Constants.STARS_DENSITY;

/**
 * Created by anton on 02.01.2016.
 */
public class StartGameScreen implements Screen {
    private SpriteBatch batch = new SpriteBatch();
    private Skin skin;
    private Stage stage = new Stage();
    private PlanesGame game;
    private OnlineSession onlineSession;
    private Thread onlineSessionThread;
    private final ParticleEffect stars = new ParticleEffect();


    public StartGameScreen(PlanesGame game) {
        this.game = game;

        skin = new Skin(Gdx.files.internal("data/uiskin.json"));
        onlineSession = new OnlineSession();
        onlineSessionThread = new Thread(onlineSession);
        int pixelWidth = Gdx.graphics.getWidth();
        int pixelHeight = Gdx.graphics.getHeight();
        stars.load(new FileHandle("menuStars.particles"), new FileHandle(""));
        ParticleEmitter emitter = stars.getEmitters().first();
        emitter.setPosition(pixelWidth / 2, pixelHeight / 2);
        stars.start();
//        stars.update(8);
        final TextButton button = new TextButton("Connect and start game", skin, "default");
        button.setWidth(200f);
        button.setHeight(20f);
        button.setPosition(Gdx.graphics.getWidth() /2 - 100f, Gdx.graphics.getHeight()/2 - 10f);
        button.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                try {
                    onlineSessionThread.start();
                } catch (IllegalThreadStateException e) {
                    e.printStackTrace();
                }
                button.setText("Session started");
            }
        });
        stage.addActor(button);
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (onlineSession.getWorldParameters() != null) {
            GameScreen gameScreen = new GameScreen(game, onlineSession);
//            game.gameScreen = gameScreen;
            game.setScreen(gameScreen);
        }
        batch.begin();
        stars.draw(batch, delta);
        stage.draw();
        batch.end();
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
        if (onlineSession.getWorldParameters() == null) {
            onlineSession.closeSocket();
            Gdx.app.log(Constants.LOG_TAG, "socket closed in StartGameScreen");
        }
        batch.dispose();
    }
}
