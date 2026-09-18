package me.shadowsmp;

import org.bukkit.plugin.java.JavaPlugin;

public class ShadowSmpCore extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("ShadowSmpCore enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ShadowSmpCore disabled!");
    }
}
