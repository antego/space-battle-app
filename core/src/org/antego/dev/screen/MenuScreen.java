package org.antego.dev.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import org.antego.dev.PlanesGame;

/**
 * Created by anton on 24.01.2016.
 */
public class MenuScreen extends InputAdapter implements Screen {
    private enum ScreenType {QUICK_GAME, FRIEND_GAME, NONE}
    private SpriteBatch batch = new SpriteBatch();
    private Skin skin;
    private Stage stage = new Stage();
    private Table table;
    private PlanesGame game;
    private final ParticleEffect stars = new ParticleEffect();
    private volatile ScreenType changeTo;

    public MenuScreen(final PlanesGame game) {
        this.game = game;

        skin = new Skin(Gdx.files.internal("data/uiskin.json"));
        table = new Table(skin);
        final TextButton quickGameButton = new TextButton("Quick Game", skin, "default");
        final TextButton friendGameButton = new TextButton("Friend Game", skin, "default");
        final TextButton quitGameButton = new TextButton("Quit", skin, "default");
        quickGameButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                changeTo = ScreenType.QUICK_GAME;
            }
        });
        friendGameButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                changeTo = ScreenType.FRIEND_GAME;
            }
        });
        quitGameButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                changeTo = ScreenType.NONE;
            }
        });
        table.add(quickGameButton);
        table.row();
        table.add(friendGameButton);
        table.row();
        table.add(quitGameButton);
        stage.addActor(table);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if(changeTo != null) {
            switch (changeTo) {
                case QUICK_GAME:
                    game.setScreen(new StartGameScreen(game));
                    break;
                case FRIEND_GAME:
                    game.setScreen(new StartGameScreen(game));
                    break;
                case NONE:
                    Gdx.app.exit();
            }
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

    }
}
