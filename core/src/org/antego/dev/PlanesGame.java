package org.antego.dev;

import com.badlogic.gdx.Game;

import org.antego.dev.screen.GameScreen;
import org.antego.dev.screen.StartGameScreen;

public class PlanesGame extends Game {
//	public StartGameScreen startGameScreen;
//    public GameScreen gameScreen;
	
	@Override
	public void create () {
//		startGameScreen = new StartGameScreen(this);
//        setScreen(startGameScreen);
        setScreen(new StartGameScreen(this));

	}
}
