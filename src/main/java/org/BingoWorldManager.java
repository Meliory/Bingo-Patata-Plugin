package org;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class BingoWorldManager {

    private static final Map<Integer, World> loadedOverworlds = new ConcurrentHashMap<>();
    private static final Map<Integer, World> loadedNethers = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> lastPlayerActivity = new ConcurrentHashMap<>();

    // Configuración optimizada
    private static final long WORLD_UNLOAD_DELAY = 300000; // 5 minutos sin jugadores
    private static final int AUTO_SAVE_INTERVAL = 6000; // 5 minutos en ticks

    private static Plugin plugin;
    private static BukkitRunnable cleanupTask;

    public static void initialize(Plugin pluginInstance) {
        plugin = pluginInstance;
        startCleanupTask();
    }

    // ==================== CARGA BAJO DEMANDA ====================

    /**
     * Carga mundos del equipo si no están cargados
     */
    public static void loadTeamWorldsIfNeeded(int teamId) {
        if (!isTeamWorldLoaded(teamId)) {
            loadTeamWorlds(teamId);
        }
        updatePlayerActivity(teamId);
    }

    /**
     * Carga mundos de un equipo específico con optimizaciones
     */
    public static void loadTeamWorlds(int teamId) {
        try {
            Bukkit.getLogger().info("[BingoWorldManager] Cargando mundos para equipo " + teamId);

            // Cargar Overworld con optimizaciones
            World overworldWorld = loadOptimizedOverworld(teamId);
            if (overworldWorld != null) {
                loadedOverworlds.put(teamId, overworldWorld);
                optimizeWorldSettings(overworldWorld);
            }

            // Cargar Nether con optimizaciones
            World netherWorld = loadOptimizedNether(teamId);
            if (netherWorld != null) {
                loadedNethers.put(teamId, netherWorld);
                optimizeWorldSettings(netherWorld);
            }

            updatePlayerActivity(teamId);

        } catch (Exception e) {
            Bukkit.getLogger().severe("[BingoWorldManager] Error cargando mundos del equipo " + teamId + ": " + e.getMessage());
        }
    }

    /**
     * Carga solo mundos de equipos que tienen jugadores asignados
     */
    public static void loadAllActiveTeamWorlds() {
        Bukkit.getLogger().info("[BingoWorldManager] Cargando mundos para equipos activos...");

        for (Team team : TeamManager.getAllTeams()) {
            if (hasPlayersInTeam(team.getID())) {
                loadTeamWorldsIfNeeded(team.getID());
            }
        }

        Bukkit.getLogger().info("[BingoWorldManager] Mundos de equipos activos cargados");
    }

    /**
     * Fuerza la carga de todos los mundos existentes (para inicio de partida)
     */
    public static void forceLoadAllTeamWorlds() {
        Bukkit.getLogger().info("[BingoWorldManager] Forzando carga de todos los mundos...");

        for (Team team : TeamManager.getAllTeams()) {
            if (!isTeamWorldLoaded(team.getID())) {
                loadTeamWorlds(team.getID());
            }
        }

        Bukkit.getLogger().info("[BingoWorldManager] Todos los mundos cargados forzosamente");
    }

    // ==================== OPTIMIZACIONES DE CARGA ====================

    private static World loadOptimizedOverworld(int teamId) {
        String worldName = "overworldteam" + teamId;
        deleteUidFile(worldName);

        WorldCreator creator = new WorldCreator(worldName);
        // Optimizaciones de generación
        creator.generateStructures(false); // Menos estructuras = menos lag

        World world = creator.createWorld();
        if (world != null) {
            configureGameRules(world);
            Bukkit.getLogger().info("[BingoWorldManager] Overworld cargado: " + worldName);
        }
        return world;
    }

    private static World loadOptimizedNether(int teamId) {
        String worldName = "netherteam" + teamId;
        deleteUidFile(worldName);

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NETHER);
        creator.type(WorldType.NORMAL);
        creator.generateStructures(false); // Menos fortalezas = mejor rendimiento

        World world = creator.createWorld();
        if (world != null) {
            configureGameRules(world);
            Bukkit.getLogger().info("[BingoWorldManager] Nether cargado: " + worldName);
        }
        return world;
    }

    private static void configureGameRules(World world) {
        // Configuración básica requerida
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, true);
        world.setDifficulty(Difficulty.HARD);

        // Optimizaciones adicionales de rendimiento
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        //world.setGameRule(GameRule.DO_WEATHER_CYCLE, false); // Menos procesamiento de clima
        //world.setGameRule(GameRule.RANDOM_TICK_SPEED, 2); // Reduce ticks aleatorios (default 3)
        world.setGameRule(GameRule.DO_FIRE_TICK, true);
        world.setGameRule(GameRule.MOB_GRIEFING, true);

        // Configuraciones de spawn más eficientes
        //world.setGameRule(GameRule.DO_PATROL_SPAWNING, false); // Sin patrullas innecesarias
        //world.setGameRule(GameRule.DO_TRADER_SPAWNING, false); // Sin comerciantes errantes
    }

    private static void optimizeWorldSettings(World world) {
        // Configuraciones optimizadas de tiempo y clima
        if (world.getEnvironment() == World.Environment.NORMAL) {
            world.setTime(1000);
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(0);
        }

        // Optimización de autosave
        world.setAutoSave(false); // Lo manejaremos manualmente
    }

    // ==================== GESTIÓN INTELIGENTE DE RECURSOS ====================

    private static void startCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupUnusedWorlds();
                performOptimizedSave();
            }
        };

        // Ejecutar cada 2 minutos
        cleanupTask.runTaskTimer(plugin, 2400L, 2400L);
    }

    private static void cleanupUnusedWorlds() {
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<Integer, Long> entry : lastPlayerActivity.entrySet()) {
            int teamId = entry.getKey();
            long lastActivity = entry.getValue();

            if (currentTime - lastActivity > WORLD_UNLOAD_DELAY) {
                if (getPlayersInTeamWorlds(teamId) == 0) {
                    unloadTeamWorlds(teamId);
                    Bukkit.getLogger().info("[BingoWorldManager] Mundos del equipo " + teamId + " descargados por inactividad");
                }
            }
        }
    }

    private static void performOptimizedSave() {
        // Guardar solo mundos con jugadores activos
        for (Map.Entry<Integer, World> entry : loadedOverworlds.entrySet()) {
            int teamId = entry.getKey();
            if (getPlayersInTeamWorlds(teamId) > 0) {
                entry.getValue().save();
                World nether = loadedNethers.get(teamId);
                if (nether != null) {
                    nether.save();
                }
            }
        }
    }

    // ==================== TELEPORTACIÓN OPTIMIZADA ====================

    public static void teleportPlayerToTeamSpawn(Player player, int teamId) {
        // Cargar mundo bajo demanda si no está cargado
        loadTeamWorldsIfNeeded(teamId);
        updatePlayerActivity(teamId);

        World world = getTeamOverworld(teamId);
        if (world != null) {
            Location spawnLocation = new Location(world, 0.5, world.getHighestBlockYAt(0, 0) + 2, 0.5);
            spawnLocation.setYaw(0);
            spawnLocation.setPitch(0);

            player.teleport(spawnLocation);

            // Pre-cargar chunks críticos de forma SÍNCRONA (obligatorio en Paper)
            preloadCriticalChunks(world, spawnLocation);

            Bukkit.getLogger().info("[BingoWorldManager] " + player.getName() + " teleportado al equipo " + teamId);
        } else {
            Bukkit.getLogger().warning("[BingoWorldManager] No se pudo cargar mundo para equipo " + teamId);
        }
    }

    /**
     * Teleporta jugador al spawn de su equipo automáticamente
     */
    public static void teleportPlayerToTeamSpawn(Player player) {
        Team team = TeamManager.getplayerTeam(player);
        if (team != null) {
            teleportPlayerToTeamSpawn(player, team.getID());
        } else {
            Bukkit.getLogger().warning("[BingoWorldManager] Jugador " + player.getName() + " no tiene equipo asignado");
        }
    }

    private static void preloadCriticalChunks(World world, Location center) {
        // Cargar chunks en un radio de 3x3 de forma SÍNCRONA (obligatorio en Paper)
        Bukkit.getScheduler().runTask(plugin, () -> {
            int centerX = center.getChunk().getX();
            int centerZ = center.getChunk().getZ();

            for (int x = centerX - 1; x <= centerX + 1; x++) {
                for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                    world.getChunkAt(x, z).load(true);
                }
            }
        });
    }

    // ==================== INTEGRACIÓN CON SISTEMA DE EQUIPOS ====================

    /**
     * Verifica si un equipo tiene jugadores asignados (integrado con TeamManager)
     */
    private static boolean hasPlayersInTeam(int teamId) {
        Team team = TeamManager.getTeamById(teamId);
        if (team == null) {
            return false;
        }

        // Verificar si hay jugadores online en el equipo
        for (UUID playerUUID : team.getPlayers()) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Obtiene el número de jugadores online en los mundos de un equipo
     */
    public static int getPlayersInTeamWorlds(int teamId) {
        int count = 0;

        World overworld = loadedOverworlds.get(teamId);
        if (overworld != null) {
            count += overworld.getPlayers().size();
        }

        World nether = loadedNethers.get(teamId);
        if (nether != null) {
            count += nether.getPlayers().size();
        }

        return count;
    }

    /**
     * Obtiene el ID del equipo de un jugador
     */
    public static int getPlayerTeamId(Player player) {
        Team team = TeamManager.getplayerTeam(player);
        return team != null ? team.getID() : -1;
    }

    // ==================== MÉTODOS DE VERIFICACIÓN ====================

    /**
     * Verifica si los mundos de un equipo están cargados
     */
    public static boolean isTeamWorldLoaded(int teamId) {
        return loadedOverworlds.containsKey(teamId) && loadedNethers.containsKey(teamId);
    }

    /**
     * Verifica si todos los mundos de equipos activos están cargados
     */
    public static boolean areActiveWorldsLoaded() {
        for (Team team : TeamManager.getAllTeams()) {
            if (hasPlayersInTeam(team.getID()) && !isTeamWorldLoaded(team.getID())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verifica si todos los mundos de todos los equipos están cargados
     */
    public static boolean areAllWorldsLoaded() {
        for (Team team : TeamManager.getAllTeams()) {
            if (!isTeamWorldLoaded(team.getID())) {
                return false;
            }
        }
        return true;
    }

    // ==================== GETTERS DE MUNDOS ====================

    public static World getTeamOverworld(int teamId) {
        return loadedOverworlds.get(teamId);
    }

    public static World getTeamNether(int teamId) {
        return loadedNethers.get(teamId);
    }

    // ==================== CONFIGURACIÓN DE MUNDOS ====================

    public static void setupWorldConditions() {
        Bukkit.getLogger().info("[BingoWorldManager] Configurando condiciones de mundos...");

        // Solo configurar mundos cargados
        for (Map.Entry<Integer, World> entry : loadedOverworlds.entrySet()) {
            optimizeWorldSettings(entry.getValue());
        }

        for (Map.Entry<Integer, World> entry : loadedNethers.entrySet()) {
            entry.getValue().setDifficulty(Difficulty.HARD);
        }

        Bukkit.getLogger().info("[BingoWorldManager] Condiciones configuradas para mundos activos");
    }

    // ==================== GESTIÓN DE ACTIVIDAD ====================

    public static void updatePlayerActivity(int teamId) {
        lastPlayerActivity.put(teamId, System.currentTimeMillis());
    }

    public static void onPlayerJoinTeamWorld(Player player, int teamId) {
        updatePlayerActivity(teamId);
    }

    public static void onPlayerLeaveTeamWorld(Player player, int teamId) {
        // El cleanup automático se encargará de esto
    }

    // ==================== DESCARGA DE MUNDOS ====================

    public static void unloadTeamWorlds(int teamId) {
        World overworld = loadedOverworlds.remove(teamId);
        World nether = loadedNethers.remove(teamId);

        if (overworld != null) {
            // Guardar antes de descargar
            overworld.save();
            Bukkit.unloadWorld(overworld, true);
        }

        if (nether != null) {
            nether.save();
            Bukkit.unloadWorld(nether, true);
        }

        lastPlayerActivity.remove(teamId);
    }

    public static void unloadAllWorlds() {
        Bukkit.getLogger().info("[BingoWorldManager] Descargando todos los mundos...");

        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        // Crear copia para evitar ConcurrentModificationException
        for (int teamId : loadedOverworlds.keySet().toArray(new Integer[0])) {
            unloadTeamWorlds(teamId);
        }

        Bukkit.getLogger().info("[BingoWorldManager] Todos los mundos descargados");
    }

    // ==================== MÉTODOS DE UTILIDAD ====================

    /**
     * Pre-carga mundos para equipos específicos
     */
    public static void preloadTeamWorlds(int... teamIds) {
        for (int teamId : teamIds) {
            loadTeamWorldsIfNeeded(teamId);
        }
    }

    /**
     * Obtiene el número de mundos actualmente cargados
     */
    public static int getLoadedWorldsCount() {
        return loadedOverworlds.size() + loadedNethers.size();
    }

    /**
     * Elimina archivo uid.dat duplicado
     */
    private static void deleteUidFile(String worldName) {
        try {
            java.io.File worldFolder = new java.io.File(Bukkit.getWorldContainer(), worldName);
            java.io.File uidFile = new java.io.File(worldFolder, "uid.dat");

            if (uidFile.exists()) {
                boolean deleted = uidFile.delete();
                if (deleted) {
                    Bukkit.getLogger().info("[BingoWorldManager] Eliminado uid.dat duplicado de: " + worldName);
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BingoWorldManager] Error eliminando uid.dat de " + worldName + ": " + e.getMessage());
        }
    }

    // ==================== MÉTODOS PARA DEBUGGING ====================

    /**
     * Muestra información de estado de mundos
     */
    public static void logWorldStatus() {
        Bukkit.getLogger().info("[BingoWorldManager] === ESTADO DE MUNDOS ===");
        Bukkit.getLogger().info("[BingoWorldManager] Mundos cargados: " + getLoadedWorldsCount());

        for (Team team : TeamManager.getAllTeams()) {
            int teamId = team.getID();
            boolean loaded = isTeamWorldLoaded(teamId);
            int playersInWorld = getPlayersInTeamWorlds(teamId);
            int playersInTeam = team.getPlayers().size();

            Bukkit.getLogger().info("[BingoWorldManager] Equipo " + teamId + " (" + team.getName() + "): " +
                    (loaded ? "Cargado" : "No cargado") +
                    " | Jugadores en mundo: " + playersInWorld +
                    " | Jugadores en equipo: " + playersInTeam);
        }
        Bukkit.getLogger().info("[BingoWorldManager] ========================");
    }
}