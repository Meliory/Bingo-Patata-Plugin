package org;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
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
        getServer().getPluginManager().registerEvents(new BingoPortalManager(), this);
        getServer().getPluginManager().registerEvents(new BingoRespawnManager(), this);
        getServer().getPluginManager().registerEvents(new BingoDisplayManager(), this);

        BingoCommands commands = new BingoCommands();
        this.getCommand("bingoPatata").setExecutor(commands);
        this.getCommand("bingoPatata").setTabCompleter(commands);

        /*getServer().getScheduler().runTask(this, () -> {
            BingoWorldManager.loadAllTeamWorlds();
        });*/
        BingoWorldManager.initialize(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        BingoWorldManager.unloadAllWorlds();
    }

    public static BingoPatataPlugin getInstance() {
        return instance;
    }

}
