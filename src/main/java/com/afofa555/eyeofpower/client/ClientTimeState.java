package com.afofa555.eyeofpower.client;

import java.util.UUID;

/** Состояние остановки времени на стороне клиента (приходит с сервера). */
public class ClientTimeState {
    public static boolean active = false;
    public static UUID stopper = new UUID(0L, 0L);

    public static boolean isFrozenFor(UUID self) {
        return active && !stopper.equals(self);
    }

    public static void reset() {
        active = false;
        stopper = new UUID(0L, 0L);
    }
}
