package com.afofa555.eyeofpower.network;

import com.afofa555.eyeofpower.EyeOfPowerMod;
import com.afofa555.eyeofpower.client.ClientTimeState;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Сервер -> клиент: состояние остановки времени. */
public record TimeStopSyncPayload(boolean active, UUID stopper) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TimeStopSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EyeOfPowerMod.MODID, "time_stop_sync"));

    public static final StreamCodec<ByteBuf, TimeStopSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, TimeStopSyncPayload::active,
            UUIDUtil.STREAM_CODEC, TimeStopSyncPayload::stopper,
            TimeStopSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TimeStopSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientTimeState.active = payload.active();
            ClientTimeState.stopper = payload.stopper();
        });
    }
}
