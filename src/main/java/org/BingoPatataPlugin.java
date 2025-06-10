package org;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class BingoPatataPlugin extends JavaPlugin {

    private static BingoPatataPlugin instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        getLogger().info("BingoPatataPlugin has been enabled!");
        getServer().getPluginManager().registerEvents(new BingoListener(), this);

        BingoCommands commands = new BingoCommands();
        this.getCommand("bingoPatata").setExecutor(commands);
        this.getCommand("bingoPatata").setTabCompleter(commands);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static BingoPatataPlugin getInstance() {
        return instance;
    }

}
