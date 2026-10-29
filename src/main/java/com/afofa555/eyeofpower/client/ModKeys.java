package com.afofa555.eyeofpower.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ModKeys {
    // Клавиша H (на русской раскладке это та же физическая клавиша "Р")
    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.eyeofpower.open_menu",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.eyeofpower");
}
