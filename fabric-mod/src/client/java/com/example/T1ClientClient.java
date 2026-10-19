package com.example;

import com.example.gui.ClickGui;
import com.example.modules.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class T1ClientClient implements ClientModInitializer {
    private static KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("t1client", "main")
        );
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.t1client.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ModuleManager.tick();
            while (openMenuKey.consumeClick()) {
                if (client.screen instanceof ClickGui) {
                    client.setScreen(null);
                } else {
                    client.setScreen(new ClickGui());
                }
            }
        });
    }

    public static Minecraft client() {
        return Minecraft.getInstance();
    }
}
