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
        try {
            if (!BingoTimer.isRunning() || BingoTimer.isPaused()) return;
            if (player == null || item == null) {
                Bukkit.getLogger().severe("[BingoProcess] Error crítico: player o item es null");
                return;
            }
            Team team = TeamManager.getplayerTeam(player);
            if (team == null) return;
            if (BingoData.hasTeamItem(team, item)) return;

            Bukkit.getScheduler().runTaskLater(BingoPatataPlugin.getInstance(), () ->
                removeItemFromInventory(player, item), 1L);

            processItemForTeam(team, player, item);

        } catch (Exception e) {
            Bukkit.getLogger().severe("[BingoProcess] Error crítico en processItemPlayer - Jugador: " +
                (player != null ? player.getName() : "null") + " Item: " + (item != null ? item.name() : "null"));
            e.printStackTrace();
        }
    }

    private static void processItemForTeam(Team team, Player player, Material item) {
        List<Material> bingoItems = BingoCard.getActiveCard();
        Set<Material> teamItemsBefore = new HashSet<>(BingoData.getTeamItems(team));
        int itemsBefore = teamItemsBefore.size();

        BingoData.addTeamItem(team, item);
        BingoScoreboard.updateTeamScoreboard(team);

        String gameTime = BingoTimer.getActualTimeFormatted();
        if (player != null) BingoLogger.logItem(team, player, item, gameTime);

        Set<Material> teamItemsAfter = BingoData.getTeamItems(team);
        int itemsAfter = teamItemsAfter.size();

        String playerName = player != null ? player.getName() : "Admin";
        var placeholders = MessageManager.builder()
                .add("player", playerName)
                .add("team", team.getName())
                .add("team_color", team.getColorTag())
                .add("item", BingoItemText.miniMessageWithHover(item))
                .build();

        if (BingoConfig.isBroadcastItemAnnouncements()) {
            MessageManager.broadcast("item.obtained_broadcast", placeholders);
        } else {
            MessageManager.broadcastToTeam(team, "item.obtained_team", placeholders);
        }

        for (UUID uuid : team.getPlayers()) {
            Player tp = Bukkit.getPlayer(uuid);
            if (tp != null && tp.isOnline())
                tp.playSound(tp.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!team.HasPlayer(onlinePlayer.getUniqueId()))
                onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.2f);
        }

        // Detectar líneas nuevas completadas
        Set<BingoData.CompletedLine> linesBef = BingoData.getCompletedLines(teamItemsBefore, bingoItems);
        Set<BingoData.CompletedLine> linesAft = BingoData.getCompletedLines(teamItemsAfter, bingoItems);
        Set<BingoData.CompletedLine> newLines = new HashSet<>(linesAft);
        newLines.removeAll(linesBef);

        if (!newLines.isEmpty()) {
            var linePlaceholders = MessageManager.builder()
                    .add("team", team.getName())
                    .add("team_color", team.getColorTag())
                    .build();

            for (BingoData.CompletedLine line : newLines) {
                String messageKey;
                String lineTypeStr;
                switch (line.getType()) {
                    case HORIZONTAL:
                        messageKey = BingoConfig.isBroadcastItemAnnouncements()
                                ? "item.line_completed_horizontal_broadcast"
                                : "item.line_completed_horizontal_team";
                        lineTypeStr = "HORIZONTAL";
                        break;
                    case VERTICAL:
                        messageKey = BingoConfig.isBroadcastItemAnnouncements()
                                ? "item.line_completed_vertical_broadcast"
                                : "item.line_completed_vertical_team";
                        lineTypeStr = "VERTICAL";
                        break;
                    case DIAGONAL:
                        messageKey = BingoConfig.isBroadcastItemAnnouncements()
                                ? "item.line_completed_diagonal_broadcast"
                                : "item.line_completed_diagonal_team";
                        lineTypeStr = "DIAGONAL";
                        break;
                    default:
                        continue;
                }

                BingoLogger.logEvent(team.getName() + " completó LÍNEA " + lineTypeStr, gameTime);

                if (BingoConfig.isBroadcastItemAnnouncements()) {
                    MessageManager.broadcast(messageKey, linePlaceholders);
                } else {
                    MessageManager.broadcastToTeam(team, messageKey, linePlaceholders);
                }

                for (UUID uuid : team.getPlayers()) {
                    Player tp = Bukkit.getPlayer(uuid);
                    if (tp != null && tp.isOnline())
                        tp.playSound(tp.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
            }

            if (BingoConfig.isSpeedrunMode() && "line".equalsIgnoreCase(BingoConfig.getSpeedrunGoal())) {
                Bukkit.getScheduler().runTaskLater(BingoPatataPlugin.getInstance(), () -> {
                    BingoTimer.stopTimer();
                    Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "¡SPEEDRUN COMPLETADO!");
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Equipo " + team.getColoredName() + ChatColor.YELLOW + " completó una línea!");
                    Bukkit.broadcastMessage(ChatColor.GRAY + "Tiempo final: " + ChatColor.WHITE + BingoTimer.getActualTimeFormatted());
                    for (Player p : Bukkit.getOnlinePlayers())
                        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }, 100L);
                return;
            }
        }

        // Detectar bingo completo
        if (itemsBefore < bingoItems.size() && itemsAfter == bingoItems.size()) {
            BingoLogger.logEvent(team.getName() + " completó el BINGO COMPLETO! (25/25 items)", gameTime);

            var bingoPlaceholders = MessageManager.builder()
                    .add("team", team.getName())
                    .add("team_color", team.getColorTag())
                    .build();

            if (BingoConfig.isBroadcastItemAnnouncements()) {
                MessageManager.broadcast("item.bingo_completed_broadcast", bingoPlaceholders);
            } else {
                MessageManager.broadcastToTeam(team, "item.bingo_completed_team", bingoPlaceholders);
            }

            for (UUID uuid : team.getPlayers()) {
                Player tp = Bukkit.getPlayer(uuid);
                if (tp != null && tp.isOnline())
                    tp.playSound(tp.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.5f);
            }

            boolean shouldEndGame = BingoConfig.isEndGameOnBingoComplete() ||
                                   (BingoConfig.isSpeedrunMode() && "bingo".equalsIgnoreCase(BingoConfig.getSpeedrunGoal()));
            if (shouldEndGame) {
                Bukkit.getScheduler().runTaskLater(BingoPatataPlugin.getInstance(), () -> {
                    BingoTimer.stopTimer();
                    if (BingoConfig.isSpeedrunMode()) {
                        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "¡SPEEDRUN COMPLETADO!");
                        Bukkit.broadcastMessage(ChatColor.YELLOW + "Equipo " + team.getColoredName() + ChatColor.YELLOW + " completó el bingo completo!");
                        Bukkit.broadcastMessage(ChatColor.GRAY + "Tiempo final: " + ChatColor.WHITE + BingoTimer.getActualTimeFormatted());
                    } else {
                        for (Player p : Bukkit.getOnlinePlayers())
                            p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "¡PARTIDA TERMINADA!");
                    }
                    for (Player p : Bukkit.getOnlinePlayers())
                        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }, 100L);
            }
        }
    }

    public static void processItemTeam(Team team, Material item) {
        if (BingoData.hasTeamItem(team, item)) return;
        processItemForTeam(team, null, item);
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
        try {
            if(player == null || item == null) return;

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
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BingoProcess] Error crítico en removeItemFromInventory - Jugador: " +
                (player != null ? player.getName() : "null") + " Item: " + (item != null ? item.name() : "null"));
            e.printStackTrace();
        }
    }
}
