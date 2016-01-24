package org.antego.dev.events;


public class ShootEvent implements GameEvent {
    @Override
    public byte[] toByteMessage() {
        return new byte[]{3};
    }
}
