package com.afofa555.eyeofpower.client;

import com.afofa555.eyeofpower.network.UsePowerPayload;
import com.afofa555.eyeofpower.power.PowerManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Меню выбора суперсилы - в правом нижнем углу экрана. */
public class PowerMenuScreen extends Screen {
    private static final int BTN_W = 176;
    private static final int BTN_H = 22;
    private static final int GAP = 6;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public PowerMenuScreen() {
        super(Component.translatable("screen.eyeofpower.title"));
    }

    @Override
    protected void init() {
        panelW = BTN_W + 16;
        panelH = 22 + BTN_H * 2 + GAP + 22;
        panelX = this.width - panelW - 8;
        panelY = this.height - panelH - 30;

        int bx = panelX + 8;
        int by = panelY + 22;

        Component timeLabel = Component.translatable("power.eyeofpower.time_stop")
                .append(" ")
                .append(Component.translatable(ClientTimeState.active ? "power.eyeofpower.on" : "power.eyeofpower.off"));

        addRenderableWidget(Button.builder(timeLabel, b -> choose(PowerManager.POWER_TIME_STOP))
                .bounds(bx, by, BTN_W, BTN_H).build());
        addRenderableWidget(Button.builder(Component.translatable("power.eyeofpower.black_hole"),
                        b -> choose(PowerManager.POWER_BLACK_HOLE))
                .bounds(bx, by + BTN_H + GAP, BTN_W, BTN_H).build());
    }

    private void choose(int power) {
        PacketDistributor.sendToServer(new UsePowerPayload(power));
        this.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeys.OPEN_MENU.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_1) {
            choose(PowerManager.POWER_TIME_STOP);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_2) {
            choose(PowerManager.POWER_BLACK_HOLE);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Не затемняем и не размываем игру
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xC0101018);
        g.renderOutline(panelX, panelY, panelW, panelH, 0xFF2ECC40);
        g.drawCenteredString(this.font, this.title, panelX + panelW / 2, panelY + 8, 0x55FF55);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, Component.translatable("screen.eyeofpower.hint"),
                panelX + panelW / 2, panelY + panelH - 14, 0xAAAAAA);
    }
}
