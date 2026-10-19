package com.example.modules;

import net.minecraft.client.Minecraft;

public abstract class Module {
    protected final Minecraft mc = Minecraft.getInstance();
    private final String name;
    private final String category;
    private final String description;
    private boolean enabled;

    protected Module(String name, String category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
    }

    public final String getName() { return name; }
    public final String getCategory() { return category; }
    public final String getDescription() { return description; }
    public final boolean isEnabled() { return enabled; }

    public final void toggle() {
        setEnabled(!enabled);
    }

    public final void setEnabled(boolean next) {
        if (enabled == next) return;
        enabled = next;
        if (enabled) onEnable(); else onDisable();
        ModuleManager.saveConfig();
    }

    public void onEnable() { }
    public void onDisable() { }
    public void onTick() { }
}
