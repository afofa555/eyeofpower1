package com.afofa555.eyeofpower.network;

import com.afofa555.eyeofpower.EyeOfPowerMod;
import com.afofa555.eyeofpower.power.PowerManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Клиент -> сервер: игрок выбрал суперсилу в меню. */
public record UsePowerPayload(int power) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UsePowerPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EyeOfPowerMod.MODID, "use_power"));

    public static final StreamCodec<ByteBuf, UsePowerPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, UsePowerPayload::power, UsePowerPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UsePowerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PowerManager.usePower(player, payload.power());
            }
        });
    }
}
