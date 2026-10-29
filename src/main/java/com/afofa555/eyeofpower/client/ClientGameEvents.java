package com.afofa555.eyeofpower.client;

import com.afofa555.eyeofpower.EyeOfPowerMod;
import com.afofa555.eyeofpower.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = EyeOfPowerMod.MODID, value = Dist.CLIENT)
public class ClientGameEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (ModKeys.OPEN_MENU.consumeClick()) {
            if (mc.player == null || mc.screen != null) {
                continue;
            }
            if (mc.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.EYE_OF_POWER.get())) {
                mc.setScreen(new PowerMenuScreen());
            } else {
                mc.player.displayClientMessage(Component.translatable("message.eyeofpower.not_wearing"), true);
            }
        }
    }

    // Пока время остановлено, обычные игроки не могут двигаться
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (ClientTimeState.isFrozenFor(player.getUUID())) {
            Input in = event.getInput();
            in.up = false;
            in.down = false;
            in.left = false;
            in.right = false;
            in.jumping = false;
            in.shiftKeyDown = false;
            in.forwardImpulse = 0.0f;
            in.leftImpulse = 0.0f;
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientTimeState.active || mc.player == null || mc.options.hideGui) {
            return;
        }
        Component text = Component.translatable("overlay.eyeofpower.time_stopped");
        int color = ClientTimeState.isFrozenFor(mc.player.getUUID()) ? 0x55FF55 : 0xFFFF55;
        event.getGuiGraphics().drawCenteredString(mc.font, text, event.getGuiGraphics().guiWidth() / 2, 18, color);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientTimeState.reset();
    }
}
