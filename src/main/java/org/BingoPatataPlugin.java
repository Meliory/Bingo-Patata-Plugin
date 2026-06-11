package org;

import org.bukkit.plugin.java.JavaPlugin;

public final class BingoPatataPlugin extends JavaPlugin {

    private static BingoPatataPlugin instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        getLogger().info("BingoPatataPlugin has been enabled!");

        // Cargar configuración
        saveDefaultConfig();
        BingoConfig.loadConfig(getConfig());
        BingoItemsConfig.loadOrGenerate(this);
        BingoCard.initialize();
        BingoFontGenerator.generate(this);
        MessageManager.loadMessages();

        // Cargar equipos guardados
        TeamManager.loadTeams();

        getServer().getPluginManager().registerEvents(new BingoListener(), this);
        getServer().getPluginManager().registerEvents(new BingoPortalManager(), this);
        getServer().getPluginManager().registerEvents(new BingoRespawnManager(), this);
        getServer().getPluginManager().registerEvents(new BingoDisplayManager(), this);

        BingoCommands commands = new BingoCommands();
        this.getCommand("bingoPatata").setExecutor(commands);
        this.getCommand("bingoPatata").setTabCompleter(commands);

        BingoWorldManager.initialize(this);
        BingoLogger.initialize();
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
