package org;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class BingoDisplayManager implements Listener {

    /**
     * Actualiza el color del nombre de un jugador según su equipo
     */
    public static void updatePlayerDisplay(Player player) {
        Team bingoTeam = TeamManager.getplayerTeam(player);

        if (bingoTeam != null) {
            // Tiene equipo - aplicar color del equipo
            ChatColor teamColor = bingoTeam.getColor();
            player.setDisplayName(teamColor + player.getName() + ChatColor.RESET);
            player.setPlayerListName(teamColor + player.getName());
        } else {
            // No tiene equipo - color blanco (vanilla)
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
        }
    }

    /**
     * Actualiza el display de todos los jugadores online
     */
    public static void updateAllPlayersDisplay() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerDisplay(player);
        }
    }

    /**
     * Resetea los nombres de todos los jugadores a vanilla
     */
    public static void resetAllPlayersDisplay() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Team bingoTeam = TeamManager.getplayerTeam(player);

        if (bingoTeam != null) {
            // Aplicar color del equipo al nombre en el chat
            ChatColor teamColor = bingoTeam.getColor();
            String coloredName = teamColor + player.getName() + ChatColor.RESET;

            // Reemplazar el nombre en el formato del chat
            String format = event.getFormat();
            String newFormat = format.replace(player.getDisplayName(), coloredName);
            event.setFormat(newFormat);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Pequeño delay para asegurar que el jugador esté completamente cargado
        Bukkit.getScheduler().runTaskLater(BingoPatataPlugin.getInstance(), () -> {
            updatePlayerDisplay(player);
        }, 5L);
    }
}