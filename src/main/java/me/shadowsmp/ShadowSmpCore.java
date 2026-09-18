package me.shadowsmp;

import org.bukkit.plugin.java.JavaPlugin;

public class ShadowSmpCore extends JavaPlugin {

    private HeartManager heartManager;
    private LifestealManager lifestealManager;
    private HeadManager headManager;

    @Override
    public void onEnable() {

        heartManager = new HeartManager(this);
        lifestealManager = new LifestealManager(heartManager);
        headManager = new HeadManager(this);

        getServer().getPluginManager().registerEvents(
                heartManager,
                this
        );

        getServer().getPluginManager().registerEvents(
                lifestealManager,
                this
        );

        getServer().getPluginManager().registerEvents(
                headManager,
                this
        );

        if (getCommand("withdraw") != null) {
            getCommand("withdraw")
                    .setExecutor(lifestealManager);
        }

        getLogger().info("ShadowSmpCore enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ShadowSmpCore disabled!");
    }
} 
