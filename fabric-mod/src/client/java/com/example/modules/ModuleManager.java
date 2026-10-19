package com.example.modules;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static Path configFile;
    private static boolean initialized;

    private ModuleManager() { }

    public static void init() {
        if (initialized) return;
        initialized = true;

        MODULES.add(new BlockESPModule());
        MODULES.add(new BaseFinderModule());
        MODULES.add(new SusChunkFinderModule());

        configFile = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("t1client.properties");
        loadConfig();
    }

    public static List<Module> getModules() {
        return Collections.unmodifiableList(MODULES);
    }

    public static void tick() {
        for (Module module : MODULES) {
            if (module.isEnabled()) {
                try {
                    module.onTick();
                } catch (RuntimeException exception) {
                    // A misbehaving module should not crash the entire client.
                    module.setEnabled(false);
                    Minecraft.getInstance().execute(() -> {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.displayClientMessage(
                                    net.minecraft.network.chat.Component.literal(
                                            "[T1] Disabled " + module.getName() + " after an error: " + exception.getClass().getSimpleName()
                                    ), false
                            );
                        }
                    });
                }
            }
        }
    }

    private static void loadConfig() {
        if (configFile == null || !Files.isRegularFile(configFile)) return;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configFile)) {
            properties.load(input);
            for (Module module : MODULES) {
                boolean enabled = Boolean.parseBoolean(properties.getProperty(keyFor(module), "false"));
                if (enabled) module.setEnabled(enabled);
            }
        } catch (IOException exception) {
            System.err.println("[T1 Client] Could not read config: " + exception.getMessage());
        }
    }

    public static void saveConfig() {
        if (configFile == null) return;
        Properties properties = new Properties();
        for (Module module : MODULES) {
            properties.setProperty(keyFor(module), Boolean.toString(module.isEnabled()));
        }
        try {
            Files.createDirectories(configFile.getParent());
            try (OutputStream output = Files.newOutputStream(configFile)) {
                properties.store(output, "T1 Client module settings");
            }
        } catch (IOException exception) {
            System.err.println("[T1 Client] Could not save config: " + exception.getMessage());
        }
    }

    private static String keyFor(Module module) {
        return "module." + module.getName().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_") + ".enabled";
    }
}
