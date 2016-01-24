package org.antego.dev.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.Socket;
import com.badlogic.gdx.net.SocketHints;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.GdxRuntimeException;

import org.antego.dev.screen.GameScreen;
import org.antego.dev.util.Constants;

import java.io.IOException;

import javax.management.Query;

/**
 * Created by anton on 02.01.2016.
 */
public class OnlineSession implements Runnable {
    //1. подклюение к серверу (открытие сокета)
    //2. приянть ответ от сервера о возможности подключения
    // здесь же проверка, что имя не занято
    // хэндшейкинг: абоненто -> сервер -> абонент/разрыв соединения
    //4. сервер - создание сессии
    // клиент - создание игрового окружения
    //5. сервер - переход в режим игры, обратный отсчет
    //6. конец игры - сервер отсылает специальный пакет с причиной остановки и дополнительными данными
    //7. клиент - итоги боя - возврат на экран подключения - закрытие соединения

    private Socket socket;
    private SenderThread senderThread;
    private UpdateThread updateThread;
    private volatile WorldParameters worldParameters;

    @Override
    public void run() {
        try {
            socket = openSocket();
        } catch (GdxRuntimeException e) {
            Gdx.app.error(Constants.LOG_TAG, "Exception on socket opening", e);
            if (socket != null) {
                socket.dispose();
            }
        }
        doHandshake();
        worldParameters = setupMultiplayerWorld();
        senderThread = new SenderThread(socket);
        updateThread = new UpdateThread(socket);
        senderThread.start();
        updateThread.start();
    }

    public void closeSocket() {
        if (socket != null) {
            socket.dispose();
        }
    }

    public SenderThread getSenderThread() {
        return senderThread;
    }

    public UpdateThread getUpdateThread() {
        return updateThread;
    }

    public WorldParameters getWorldParameters() {
        return worldParameters;
    }

    public void registerGameScreen(GameScreen gameScreen) {
        updateThread.registerScreen(gameScreen);
    }

    private WorldParameters setupMultiplayerWorld() {
        byte[] worldParam = new byte[1];
        try {
            int len = socket.getInputStream().read(worldParam);
        } catch (IOException e) {
            Gdx.app.error(Constants.LOG_TAG, "Exception while reading world param", e);
        }
        //todo len == -1
        return new WorldParameters(worldParam[0] == 1); //если 1, то левый игрок - человек
    }

    private boolean doHandshake() {
        //todo handshake
        return true;
    }

    private Socket openSocket() throws GdxRuntimeException {
        return Gdx.net.newClientSocket(
                Net.Protocol.TCP,
                Constants.HOST_ADDRESS,
                Constants.HOST_PORT,
                new SocketHints());
    }
}
