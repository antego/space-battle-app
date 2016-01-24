package org.antego.dev.util;


import com.badlogic.gdx.graphics.g2d.Sprite;

public class PlaneData {
    public int numOfHits;
    public volatile float acceleration;
    public Sprite sprite;

    public Sprite getSprite() {
        return sprite;
    }

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
    }
}
