package com.afofa555.eyeofpower.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(UsePowerPayload.TYPE, UsePowerPayload.CODEC, UsePowerPayload::handle);
        registrar.playToClient(TimeStopSyncPayload.TYPE, TimeStopSyncPayload.CODEC, TimeStopSyncPayload::handle);
    }
}
