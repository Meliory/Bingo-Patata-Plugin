package org;

import org.bukkit.configuration.file.FileConfiguration;

public class BingoConfig {

    private static FileConfiguration config;

    // ==================== MUNDOS ====================
    private static boolean shareWorld;
    private static String vanillaOverworld;
    private static String vanillaNether;
    private static String vanillaEnd;

    // ==================== TIMER ====================
    private static int timerDuration;

    // ==================== ANUNCIOS ====================
    private static boolean broadcastItemAnnouncements;

    // ==================== PARTIDA ====================
    private static boolean endGameOnBingoComplete;

    // ==================== OPTIMIZACIONES ====================
    private static long worldUnloadDelay;
    private static int autoSaveInterval;

    // ==================== GAMERULES ====================
    private static boolean showAdvancementMessages;
    private static boolean spawnMobs;
    private static boolean advanceTime;
    private static boolean mobGriefing;
    private static int fireSpreadRadius;

    /**
     * Carga la configuración desde el archivo config.yml
     */
    public static void loadConfig(FileConfiguration fileConfig) {
        config = fileConfig;

        // Mundos
        shareWorld = config.getBoolean("share_world", false);
        vanillaOverworld = config.getString("vanilla_worlds.overworld", "world");
        vanillaNether = config.getString("vanilla_worlds.nether", "world_nether");
        vanillaEnd = config.getString("vanilla_worlds.end", "world_the_end");

        // Timer
        timerDuration = config.getInt("timer_duration", 10800);

        // Anuncios
        broadcastItemAnnouncements = config.getBoolean("broadcast_item_announcements", false);

        // Partida
        endGameOnBingoComplete = config.getBoolean("end_game_on_bingo_complete", false);

        // Optimizaciones
        worldUnloadDelay = config.getLong("world_unload_delay", 300000L);
        autoSaveInterval = config.getInt("auto_save_interval", 6000);

        // GameRules
        showAdvancementMessages = config.getBoolean("gamerules.show_advancement_messages", false);
        spawnMobs = config.getBoolean("gamerules.spawn_mobs", true);
        advanceTime = config.getBoolean("gamerules.advance_time", true);
        mobGriefing = config.getBoolean("gamerules.mob_griefing", true);
        fireSpreadRadius = config.getInt("gamerules.fire_spread_radius", 128);

        BingoPatataPlugin.getInstance().getLogger().info("[BingoConfig] Configuración cargada correctamente");
        BingoPatataPlugin.getInstance().getLogger().info("[BingoConfig] Modo compartir mundos: " + shareWorld);
    }

    /**
     * Recarga la configuración
     */
    public static void reloadConfig() {
        BingoPatataPlugin plugin = BingoPatataPlugin.getInstance();
        plugin.reloadConfig();
        loadConfig(plugin.getConfig());
    }

    // ==================== GETTERS ====================

    public static boolean isShareWorld() {
        return shareWorld;
    }

    public static String getVanillaOverworld() {
        return vanillaOverworld;
    }

    public static String getVanillaNether() {
        return vanillaNether;
    }

    public static String getVanillaEnd() {
        return vanillaEnd;
    }

    public static int getTimerDuration() {
        return timerDuration;
    }

    public static long getWorldUnloadDelay() {
        return worldUnloadDelay;
    }

    public static int getAutoSaveInterval() {
        return autoSaveInterval;
    }

    public static boolean isShowAdvancementMessages() {
        return showAdvancementMessages;
    }

    public static boolean isSpawnMobs() {
        return spawnMobs;
    }

    public static boolean isAdvanceTime() {
        return advanceTime;
    }

    public static boolean isMobGriefing() {
        return mobGriefing;
    }

    public static int getFireSpreadRadius() {
        return fireSpreadRadius;
    }

    public static boolean isBroadcastItemAnnouncements() {
        return broadcastItemAnnouncements;
    }

    public static boolean isEndGameOnBingoComplete() {
        return endGameOnBingoComplete;
    }
}
