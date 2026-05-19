package org;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.PortalCreateEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class BingoPortalManager implements Listener {

    private static final Logger LOGGER = Logger.getLogger("BingoPortalManager");

    // Almacena portales creados: "teamID-worldType-x-z" -> Location
    private static final Map<String, Location> portalCache = new HashMap<>();

    // Radio de búsqueda reducido para evitar confusiones
    private static final int SEARCH_RADIUS_HORIZONTAL = 16;
    private static final int SEARCH_RADIUS_VERTICAL = 10;

    // Coordenadas del portal estándar de Minecraft
    private static final int PORTAL_WIDTH = 4;
    private static final int PORTAL_HEIGHT = 5;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        Team team = TeamManager.getplayerTeam(player);

        if (team == null) return;

        World fromWorld = event.getFrom().getWorld();
        int teamID = team.getID();
        Location playerLocation = event.getFrom();

        // Si está en modo compartido, dejar que Minecraft maneje los portales normalmente
        if (BingoConfig.isShareWorld()) {
            return;
        }

        // Detectar tipo de viaje según el ambiente
        World.Environment fromEnv = fromWorld.getEnvironment();

        //1. Desde Overworld
        if (fromEnv == World.Environment.NORMAL) {
            // Determinar si va a Nether o End
            if (event.getCause() == PlayerPortalEvent.TeleportCause.NETHER_PORTAL) {
                handleNetherPortalFromOverworld(event, player, teamID, playerLocation);
            } else if (event.getCause() == PlayerPortalEvent.TeleportCause.END_PORTAL) {
                handleEndPortalFromOverworld(event, player, teamID, playerLocation);
            }
        }
        //2. Desde Nether
        else if (fromEnv == World.Environment.NETHER) {
            if (event.getCause() == PlayerPortalEvent.TeleportCause.NETHER_PORTAL) {
                handleNetherPortalFromNether(event, player, teamID, playerLocation);
            }
        }
        //3. Desde End
        else if (fromEnv == World.Environment.THE_END) {
            if (event.getCause() == PlayerPortalEvent.TeleportCause.END_PORTAL) {
                handleEndPortalFromEnd(event, player, teamID, playerLocation);
            }
        }
    }

    /**
     * Maneja viaje Overworld -> Nether
     */
    private void handleNetherPortalFromOverworld(PlayerPortalEvent event, Player player, int teamID, Location playerLocation) {
        World netherWorld = BingoWorldManager.getTeamNether(teamID);
        if (netherWorld != null) {
            // Calculamos coords nether (división por 8)
            double netherX = playerLocation.getX() / 8.0;
            double netherZ = playerLocation.getZ() / 8.0;
            double netherY = Math.max(40, Math.min(100, playerLocation.getY()));

            Location netherLocation = new Location(netherWorld, netherX, netherY, netherZ);
            Location portalLocation = findOrCreatePortal(netherLocation, true, teamID);

            event.setTo(portalLocation);
            LOGGER.info("Jugador " + player.getName() + " del equipo " + teamID +
                       " viajó de Overworld a Nether: " + formatLocation(portalLocation));
        }
    }

    /**
     * Maneja viaje Nether -> Overworld
     */
    private void handleNetherPortalFromNether(PlayerPortalEvent event, Player player, int teamID, Location playerLocation) {
        World overworldWorld = BingoWorldManager.getTeamOverworld(teamID);
        if (overworldWorld != null) {
            // Calculamos coords overworld (multiplicación por 8)
            double overworldX = playerLocation.getX() * 8.0;
            double overworldZ = playerLocation.getZ() * 8.0;
            double overworldY = overworldWorld.getHighestBlockYAt((int) overworldX, (int) overworldZ) + 1;

            Location overworldLocation = new Location(overworldWorld, overworldX, overworldY, overworldZ);
            Location portalLocation = findOrCreatePortal(overworldLocation, false, teamID);

            event.setTo(portalLocation);
            LOGGER.info("Jugador " + player.getName() + " del equipo " + teamID +
                       " viajó de Nether a Overworld: " + formatLocation(portalLocation));
        }
    }

    /**
     * Maneja viaje Overworld -> End
     */
    private void handleEndPortalFromOverworld(PlayerPortalEvent event, Player player, int teamID, Location playerLocation) {
        World endWorld = BingoWorldManager.getTeamEnd(teamID);
        if (endWorld != null) {
            // Ir a la plataforma de spawn del End (coordenadas fijas)
            Location endSpawn = new Location(endWorld, 100, 48, 0);
            event.setTo(endSpawn);
            LOGGER.info("Jugador " + player.getName() + " del equipo " + teamID +
                       " viajó de Overworld a End: " + formatLocation(endSpawn));
        }
    }

    /**
     * Maneja viaje End -> Overworld (después de matar dragón)
     */
    private void handleEndPortalFromEnd(PlayerPortalEvent event, Player player, int teamID, Location playerLocation) {
        World overworldWorld = BingoWorldManager.getTeamOverworld(teamID);
        if (overworldWorld != null) {
            // Ir al spawn del Overworld
            Location spawn = overworldWorld.getSpawnLocation();
            event.setTo(spawn);
            LOGGER.info("Jugador " + player.getName() + " del equipo " + teamID +
                       " regresó del End a Overworld: " + formatLocation(spawn));
        }
    }

    /**
     * Busca un portal cercano o crea uno nuevo
     * @param targetLocation Ubicación aproximada donde debe estar el portal
     * @param isNether Si estamos yendo al nether o al overworld
     * @param teamID ID del equipo
     * @return Ubicación exacta del portal
     */
    private Location findOrCreatePortal(Location targetLocation, boolean isNether, int teamID) {
        World world = targetLocation.getWorld();

        // Generar clave de cache para este portal
        String cacheKey = generatePortalCacheKey(teamID, world.getName(), targetLocation);

        // 1. Verificar si ya existe en cache
        if (portalCache.containsKey(cacheKey)) {
            Location cachedPortal = portalCache.get(cacheKey);
            // Verificar que el portal sigue existiendo
            if (isPortalValid(cachedPortal)) {
                LOGGER.info("Portal encontrado en cache: " + cacheKey);
                return findSafeLocationNearPortal(cachedPortal);
            } else {
                // Portal destruido, eliminar de cache
                portalCache.remove(cacheKey);
                LOGGER.warning("Portal en cache destruido, se recreará: " + cacheKey);
            }
        }

        // 2. Buscar portales existentes cerca
        Location existingPortal = searchForNearbyPortal(targetLocation);
        if (existingPortal != null) {
            // Guardar en cache
            portalCache.put(cacheKey, existingPortal);
            LOGGER.info("Portal existente encontrado cerca de " + formatLocation(targetLocation));
            return findSafeLocationNearPortal(existingPortal);
        }

        // 3. No existe, crear nuevo portal
        LOGGER.info("Creando nuevo portal en " + formatLocation(targetLocation));
        Location newPortal = createNewPortal(targetLocation, isNether);

        // Guardar en cache
        portalCache.put(cacheKey, newPortal);

        return newPortal;
    }

    /**
     * Busca un portal existente cerca de la ubicación objetivo
     */
    private Location searchForNearbyPortal(Location targetLocation) {
        World world = targetLocation.getWorld();

        // Buscar portales existentes en un radio reducido
        for (int x = -SEARCH_RADIUS_HORIZONTAL; x <= SEARCH_RADIUS_HORIZONTAL; x++) {
            for (int y = -SEARCH_RADIUS_VERTICAL; y <= SEARCH_RADIUS_VERTICAL; y++) {
                for (int z = -SEARCH_RADIUS_HORIZONTAL; z <= SEARCH_RADIUS_HORIZONTAL; z++) {
                    Location checkLoc = targetLocation.clone().add(x, y, z);

                    // Buscar bloques de portal activo
                    if (checkLoc.getBlock().getType() == Material.NETHER_PORTAL) {
                        return checkLoc;
                    }

                    // También buscar marcos de obsidiana (portales apagados)
                    if (checkLoc.getBlock().getType() == Material.OBSIDIAN) {
                        if (isPartOfPortalFrame(checkLoc)) {
                            return checkLoc;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Verifica si un bloque de obsidiana es parte de un marco de portal válido
     */
    private boolean isPartOfPortalFrame(Location obsidianLoc) {
        // Buscar configuración de marco 4x5 alrededor de este bloque de obsidiana
        for (int dx = -3; dx <= 0; dx++) {
            for (int dy = -4; dy <= 0; dy++) {
                Location frameLoc = obsidianLoc.clone().add(dx, dy, 0);
                if (isValidPortalFrame(frameLoc)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Verifica si en una ubicación hay un marco de portal válido
     */
    private boolean isValidPortalFrame(Location baseLoc) {
        // Verificar esquinas del marco 4x5
        return baseLoc.getBlock().getType() == Material.OBSIDIAN &&
               baseLoc.clone().add(3, 0, 0).getBlock().getType() == Material.OBSIDIAN &&
               baseLoc.clone().add(0, 4, 0).getBlock().getType() == Material.OBSIDIAN &&
               baseLoc.clone().add(3, 4, 0).getBlock().getType() == Material.OBSIDIAN;
    }

    /**
     * Verifica si un portal sigue siendo válido (no ha sido destruido)
     */
    private boolean isPortalValid(Location portalLoc) {
        if (portalLoc == null || portalLoc.getWorld() == null) return false;

        // Verificar que sigue habiendo al menos un bloque de portal o marco de obsidiana cerca
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    Location checkLoc = portalLoc.clone().add(x, y, z);
                    Material type = checkLoc.getBlock().getType();
                    if (type == Material.NETHER_PORTAL || type == Material.OBSIDIAN) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Crea un nuevo portal en la ubicación especificada
     */
    private Location createNewPortal(Location targetLocation, boolean isNether) {
        World world = targetLocation.getWorld();

        // Ajustar Y para que sea segura
        int safeY;
        if (isNether) {
            safeY = findSafeNetherHeight(targetLocation);
        } else {
            safeY = world.getHighestBlockYAt(targetLocation) + 1;
        }

        Location portalBase = new Location(world, targetLocation.getBlockX(), safeY, targetLocation.getBlockZ());

        // Crear marco y activar portal
        createPortalFrame(portalBase);
        activatePortal(portalBase);

        LOGGER.info("Nuevo portal creado en: " + formatLocation(portalBase));

        // Retornar ubicación segura dentro del portal
        return portalBase.clone().add(1.5, 1, 0.5);
    }

    /**
     * Encuentra una altura segura en el Nether para crear el portal
     */
    private int findSafeNetherHeight(Location targetLocation) {
        World world = targetLocation.getWorld();
        int x = targetLocation.getBlockX();
        int z = targetLocation.getBlockZ();

        // Buscar desde Y=40 hacia arriba un lugar seguro: (suelo sólido + aire + aire + aire)
        // Evitamos Y < 40 para alejarnos del lava ocean
        for (int y = 40; y < 100; y++) {
            if (world.getBlockAt(x, y - 1, z).getType().isSolid() &&
                world.getBlockAt(x, y, z).getType().isAir() &&
                world.getBlockAt(x, y + 1, z).getType().isAir() &&
                world.getBlockAt(x, y + 2, z).getType().isAir()) {
                return y;
            }
        }

        // Si no encuentra, forzar Y=64 y crear plataforma
        createObsidianPlatform(new Location(world, x, 63, z));
        return 64;
    }

    /**
     * Crea una plataforma de obsidiana de seguridad
     */
    private void createObsidianPlatform(Location portalBase) {
        // Crear plataforma 5x5 de obsidiana
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                portalBase.clone().add(x, 0, z).getBlock().setType(Material.OBSIDIAN);
            }
        }
    }

    /**
     * Crea el marco de obsidiana del portal (4x5)
     */
    private void createPortalFrame(Location portalBase) {
        // Marco de portal 4x5 de obsidiana (portal estándar de Minecraft)

        // Base y techo (4 bloques de ancho)
        for (int x = 0; x < PORTAL_WIDTH; x++) {
            portalBase.clone().add(x, 0, 0).getBlock().setType(Material.OBSIDIAN);
            portalBase.clone().add(x, PORTAL_HEIGHT - 1, 0).getBlock().setType(Material.OBSIDIAN);
        }

        // Lados (5 bloques de alto)
        for (int y = 0; y < PORTAL_HEIGHT; y++) {
            portalBase.clone().add(0, y, 0).getBlock().setType(Material.OBSIDIAN);
            portalBase.clone().add(PORTAL_WIDTH - 1, y, 0).getBlock().setType(Material.OBSIDIAN);
        }

        // Limpiar el interior (2 bloques ancho x 3 bloques alto)
        for (int x = 1; x < PORTAL_WIDTH - 1; x++) {
            for (int y = 1; y < PORTAL_HEIGHT - 1; y++) {
                portalBase.clone().add(x, y, 0).getBlock().setType(Material.AIR);
            }
        }
    }

    /**
     * Activa el portal poniendo bloques de portal en el interior
     */
    private void activatePortal(Location portalBase) {
        // Activar el portal poniendo bloques de portal en el interior (2x3)
        for (int x = 1; x < PORTAL_WIDTH - 1; x++) {
            for (int y = 1; y < PORTAL_HEIGHT - 1; y++) {
                portalBase.clone().add(x, y, 0).getBlock().setType(Material.NETHER_PORTAL);
            }
        }
    }

    /**
     * Encuentra una ubicación segura cerca del portal para teleportar al jugador
     */
    private Location findSafeLocationNearPortal(Location portalLocation) {
        // Buscar una ubicación segura cerca del portal encontrado
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -1; y <= 1; y++) {
                    Location testLoc = portalLocation.clone().add(x, y, z);
                    if (testLoc.getBlock().getType().isAir() &&
                        testLoc.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                        return testLoc.add(0.5, 0, 0.5);
                    }
                }
            }
        }
        return portalLocation.clone().add(0.5, 0, 0.5);
    }

    /**
     * Cancela la creación automática de portales por Minecraft
     * Esto previene que se generen portales fuera de control
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPortalCreate(PortalCreateEvent event) {
        // Si está en modo compartido, permitir creación normal de portales
        if (BingoConfig.isShareWorld()) {
            return;
        }

        // Cancelar creación automática de portales en mundos de equipos
        String worldName = event.getWorld().getName();
        if (worldName.startsWith("overworldteam") || worldName.startsWith("netherteam") || worldName.startsWith("endteam")) {
            event.setCancelled(true);
            LOGGER.info("Creación automática de portal cancelada en: " + worldName);
        }
    }

    /**
     * Genera una clave única para el cache de portales
     */
    private String generatePortalCacheKey(int teamID, String worldName, Location loc) {
        // Redondear coordenadas a chunks (16 bloques) para agrupar portales cercanos
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        return teamID + "-" + worldName + "-" + chunkX + "-" + chunkZ;
    }

    /**
     * Formatea una ubicación para logging
     */
    private String formatLocation(Location loc) {
        return String.format("%s (%.1f, %.1f, %.1f)",
            loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    /**
     * Limpia el cache de portales (útil al reiniciar partida)
     */
    public static void clearPortalCache() {
        portalCache.clear();
        LOGGER.info("Cache de portales limpiado");
    }
}
