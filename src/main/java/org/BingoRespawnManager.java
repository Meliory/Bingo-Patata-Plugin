package org;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class BingoRespawnManager implements Listener {

    /**
     * Establece el spawn point de un jugador en su mundo del equipo
     */
    public static void setPlayerTeamSpawn(Player player, int teamId) {
        World teamWorld = BingoWorldManager.getTeamOverworld(teamId);

        if (teamWorld != null) {
            // Crear ubicación de spawn en el overworld del equipo
            Location teamSpawn = new Location(teamWorld, 0.5, teamWorld.getHighestBlockYAt(0, 0) + 2, 0.5);

            // Establecer como spawn point del jugador (forzado)
            player.setBedSpawnLocation(teamSpawn, true);
        }
    }

    /**
     * Establece el spawn de todos los jugadores de un equipo
     */
    public static void setTeamSpawns(Team team) {
        for (java.util.UUID uuid : team.getPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                setPlayerTeamSpawn(player, team.getID());
            }
        }
    }

    /**
     * Establece el spawn de todos los jugadores de todos los equipos
     */
    public static void setAllTeamSpawns() {
        for (Team team : TeamManager.getAllTeams()) {
            setTeamSpawns(team);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Team team = TeamManager.getplayerTeam(player);

        // Si no está en un equipo, respawn vanilla normal
        if (team == null) {
            return;
        }

        World teamWorld = BingoWorldManager.getTeamOverworld(team.getID());
        if (teamWorld == null) {
            // Si no existe el mundo del equipo, respawn vanilla normal
            return;
        }

        // Verificar si el jugador tiene un spawnpoint válido (cama o respawn anchor)
        Location spawnLocation = player.getBedSpawnLocation();

        // Si tiene spawnpoint válido, dejar que Minecraft maneje el respawn normalmente
        if (spawnLocation != null) {
            return;
        }

        // No tiene spawnpoint válido - forzar respawn en el mundo del equipo
        Location teamSpawn = new Location(teamWorld, 0.5, teamWorld.getHighestBlockYAt(0, 0) + 2, 0.5);
        event.setRespawnLocation(teamSpawn);
    }
}