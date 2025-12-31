package org;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BingoProcess {
    public static void processItemPlayer(Player player, Material item) {
        // Si la partida no ha empezado, no contar items
        if(!BingoTimer.isRunning()) {
            return;
        }

        Team team = TeamManager.getplayerTeam(player);

        //Si no está en un equipo, fuera
        if(team == null){
            return;
        }

        //Si el equipo tiene el objeto, fuera
        if(BingoData.hasTeamItem(team, item)){
            return;
        }

        // Datos antes de añadir el item
        Set<Material> teamItems = BingoData.getTeamItems(team);
        List<Material> bingoItems = BingoCard.getBingoItems();
        int linesBefore = BingoData.getCompletedLinesCount(teamItems, bingoItems);
        int itemsBefore = teamItems.size();

        // Hacer una COPIA del Set para poder comparar después
        Set<Material> teamItemsCopy = new HashSet<>(teamItems);

        // Eliminar item del inventario después de 1 tick
        Bukkit.getScheduler().runTaskLater(BingoPatataPlugin.getInstance(), () -> {
            removeItemFromInventory(player, item);
        },1L);

        // Añadir item al equipo
        BingoData.addTeamItem(team, item);

        // Actualizar scoreboard
        BingoScoreboard.updateTeamScoreboard(team);

        // Datos después de añadir el item
        Set<Material> teamItemsAfter = BingoData.getTeamItems(team);
        int linesAfter = BingoData.getCompletedLinesCount(teamItemsAfter, bingoItems);
        int itemsAfter = teamItemsAfter.size();

        // Preparar placeholders para el mensaje
        var placeholders = MessageManager.builder()
                .add("player", player.getName())
                .add("team", team.getName())
                .add("team_color", team.getColorTag())
                .add("item", item.name())
                .build();

        // Anunciar item conseguido
        if (BingoConfig.isBroadcastItemAnnouncements()) {
            MessageManager.broadcast("item.obtained_broadcast", placeholders);
        } else {
            MessageManager.broadcastToTeam(team, "item.obtained_team", placeholders);
        }

        // Sonido para el equipo que consiguió (positivo) - SIEMPRE
        for(UUID uuid : team.getPlayers()){
            Player teamPlayer = Bukkit.getPlayer(uuid);
            if(teamPlayer != null && teamPlayer.isOnline()){
                teamPlayer.playSound(teamPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }

        // Sonido para otros equipos (negativo y dramático) - SIEMPRE
        for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if(!team.HasPlayer(onlinePlayer.getUniqueId())) {
                onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.2f);
            }
        }

        // Detectar qué líneas nuevas se completaron
        Set<BingoData.CompletedLine> linesBef = BingoData.getCompletedLines(teamItemsCopy, bingoItems);
        Set<BingoData.CompletedLine> linesAft = BingoData.getCompletedLines(teamItemsAfter, bingoItems);

        // Encontrar las líneas nuevas (las que están en linesAft pero no en linesBef)
        Set<BingoData.CompletedLine> newLines = new HashSet<>(linesAft);
        newLines.removeAll(linesBef);

        // Anunciar cada línea completada por separado
        if (!newLines.isEmpty()) {
            var linePlaceholders = MessageManager.builder()
                    .add("team", team.getName())
                    .add("team_color", team.getColorTag())
                    .build();

            for (BingoData.CompletedLine line : newLines) {
                String messageKey;

                // Determinar el tipo de mensaje según el tipo de línea
                switch (line.getType()) {
                    case HORIZONTAL:
                        messageKey = BingoConfig.isBroadcastItemAnnouncements()
                                ? "item.line_completed_horizontal_broadcast"
                                : "item.line_completed_horizontal_team";
                        break;
                    case VERTICAL:
                        messageKey = BingoConfig.isBroadcastItemAnnouncements()
                                ? "item.line_completed_vertical_broadcast"
                                : "item.line_completed_vertical_team";
                        break;
                    case DIAGONAL:
                        messageKey = BingoConfig.isBroadcastItemAnnouncements()
                                ? "item.line_completed_diagonal_broadcast"
                                : "item.line_completed_diagonal_team";
                        break;
                    default:
                        continue; // Por si acaso
                }

                // Enviar el mensaje
                if (BingoConfig.isBroadcastItemAnnouncements()) {
                    MessageManager.broadcast(messageKey, linePlaceholders);
                } else {
                    MessageManager.broadcastToTeam(team, messageKey, linePlaceholders);
                }

                // Sonido especial para línea completada
                for(UUID uuid : team.getPlayers()){
                    Player teamPlayer = Bukkit.getPlayer(uuid);
                    if(teamPlayer != null && teamPlayer.isOnline()){
                        teamPlayer.playSound(teamPlayer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    }
                }
            }
        }

        // Detectar si se completó el bingo (todos los items)
        if(itemsBefore < bingoItems.size() && itemsAfter == bingoItems.size()) {
            var bingoPlaceholders = MessageManager.builder()
                    .add("team", team.getName())
                    .add("team_color", team.getColorTag())
                    .build();

            if (BingoConfig.isBroadcastItemAnnouncements()) {
                MessageManager.broadcast("item.bingo_completed_broadcast", bingoPlaceholders);
            } else {
                MessageManager.broadcastToTeam(team, "item.bingo_completed_team", bingoPlaceholders);
            }

            // Sonido épico para bingo completo
            for(UUID uuid : team.getPlayers()){
                Player teamPlayer = Bukkit.getPlayer(uuid);
                if(teamPlayer != null && teamPlayer.isOnline()){
                    teamPlayer.playSound(teamPlayer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.5f);
                }
            }

            // Terminar partida si está configurado
            if(BingoConfig.isEndGameOnBingoComplete()) {
                Bukkit.getScheduler().runTaskLater(BingoPatataPlugin.getInstance(), () -> {
                    BingoTimer.stopTimer();
                    for(Player p : Bukkit.getOnlinePlayers()) {
                        p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "¡PARTIDA TERMINADA!");
                        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                    }
                }, 100L); // 5 segundos de delay
            }
        }
    }

    public static void processItemTeam(Team team, Material item){
        if(BingoData.hasTeamItem(team, item)){
            //player.sendMessage(ChatColor.BLUE + "You already have that item!");
            return;
        }

        BingoData.addTeamItem(team, item);

        BingoScoreboard.updateTeamScoreboard(team);

        for(UUID uuid : team.getPlayers()){
            Player teamplayer =  Bukkit.getPlayer(uuid);
            if(teamplayer != null && teamplayer.isOnline()){
                teamplayer.sendMessage(ChatColor.GREEN + "El equipo ha conseguido: " + ChatColor.WHITE + item.toString());
            }
        }
    }

    public static void removeItemTeam(Team team, Material item){
        if(BingoData.hasTeamItem(team, item)){
            BingoData.removeTeamItem(team, item);

            BingoScoreboard.updateTeamScoreboard(team);

            for(UUID uuid : team.getPlayers()){
                Player teamplayer =  Bukkit.getPlayer(uuid);
                if(teamplayer != null && teamplayer.isOnline()){
                    teamplayer.sendMessage(ChatColor.GREEN + "Al equipo se le ha quitado: " + ChatColor.WHITE + item.toString());
                }
            }
        }
    }

    private static void removeItemFromInventory(Player player, Material item) {
        PlayerInventory inventory = player.getInventory();
        for(ItemStack itemStack : inventory.getContents()) {
            if(itemStack != null && itemStack.getType() == item){
                if(itemStack.getAmount() > 1) {
                    itemStack.setAmount(itemStack.getAmount() - 1);
                } else {
                    inventory.remove(itemStack);
                }
                //player.sendMessage("Se ha eliminado de tu inventario");
                break;
            }
        }
    }
}
