package org;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

import static org.apache.logging.log4j.LogManager.getLogger;

public class BingoScoreboard {

    // UN scoreboard por equipo (no por jugador)
    private static final Map<Team, Scoreboard> teamScoreboards = new HashMap<>();

    //Mapeo de materiales a caracteres Unicode
    private static final Map<Material, String> ITEM_CHARS = new HashMap<>();
    private static final Map<Material, String> ITEM_CHARS_DONE = new HashMap<>();

    static {
        List<Material> bingoItems = BingoCard.getBingoItems();
        int i = 1;
        for(Material bingoItem : bingoItems) {
            ITEM_CHARS.put(bingoItem, String.valueOf((char)(0xE000 + i)));
            i++;
            ITEM_CHARS_DONE.put(bingoItem, String.valueOf((char)(0xE000 + i)));
            i++;
        }
    }

    public static void showBingoCard(Player player) {
        hideBingoCard(player);
        Team team = TeamManager.getplayerTeam(player);

        if (team == null) {
            player.sendMessage("Debes estar en un equipo para ver el bingo");
            return;
        }

        // Obtener o crear el scoreboard del equipo
        Scoreboard teamScoreboard = getOrCreateTeamScoreboard(team);

        // Asignar el scoreboard del equipo al jugador
        player.setScoreboard(teamScoreboard);

        //player.sendMessage("Carta de bingo mostrada");
    }

    public static void showAllTeamCards() {
        int playersShown = 0;

        for (Team team : TeamManager.getAllTeams()) {
            for (UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    showBingoCard(player);
                    playersShown++;
                }
            }
        }

        Bukkit.broadcastMessage("Cartas de bingo mostradas a " + playersShown + " jugadores");
    }

    public static void hideAllTeamCards() {
        int playersHidden = 0;

        for (Team team : TeamManager.getAllTeams()) {
            for (UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    hideBingoCard(player);
                    playersHidden++;
                }
            }
        }

        Bukkit.broadcastMessage("Cartas de bingo ocultadas a " + playersHidden + " jugadores");
    }

    public static void hideBingoCard(Player player) {
        // Crear scoreboard vacío o usar el principal del servidor
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard emptyScoreboard = manager.getMainScoreboard();

        player.setScoreboard(emptyScoreboard);
        //player.sendMessage("Carta de bingo ocultada");
    }

    private static Scoreboard getOrCreateTeamScoreboard(Team team) {
        // Si ya existe el scoreboard del equipo, devolverlo
        if (teamScoreboards.containsKey(team)) {
            return teamScoreboards.get(team);
        }

        // Crear nuevo scoreboard para el equipo
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();

        Objective objective = scoreboard.registerNewObjective("bingo", "dummy",
                ChatColor.GOLD + "BINGO PATATA T.7");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Guardar el scoreboard del equipo
        teamScoreboards.put(team, scoreboard);

        // Actualizar contenido inicial
        updateTeamScoreboardContent(team);

        return scoreboard;
    }

    public static void updateTeamScoreboard(Team team) {
        if (!teamScoreboards.containsKey(team)) {
            return; // No tiene scoreboard, no actualizar
        }

        updateTeamScoreboardContent(team);
    }

    /*public static void createBingoScoreboard(Player player) {
        Team team = TeamManager.getplayerTeam(player);

        if (team == null) {
            player.sendMessage("Debes estar en un equipo para ver el bingo");
            return;
        }

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();

        Objective objective = scoreboard.registerNewObjective("bingo", "dummy", "CARTA BINGO");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        updateScoreboardContent(objective, team);

        player.setScoreboard(scoreboard);
    }*/

    private static void updateTeamScoreboardContent(Team team) {
        Scoreboard scoreboard = teamScoreboards.get(team);
        if (scoreboard == null) return;

        Objective objective = scoreboard.getObjective("bingo");
        if (objective == null) return;

        // Limpiar scoreboard anterior
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        Set<Material> teamItems = BingoData.getTeamItems(team);
        int teamPoints = BingoData.getTeamPoints(team);
        List<Material> bingoItems = BingoCard.getBingoItems();

        int score = 40;

        //Título Equipo
        objective.getScore("Equipo: " + team.getColoredName()).setScore(score--);
        objective.getScore("").setScore(score--);
        objective.getScore("§" + (200 * 2) + "§r").setScore(score--);

        //Filas de items con doble espaciado
        for(int i = 0; i <= 4; i++){
            StringBuilder itemLine = new StringBuilder();

            for(int j = 1; j <= 5; j++) {
                Material item = bingoItems.get(j + (5 * i) - 1);
                if (teamItems.contains(item)) {
                    itemLine.append(ITEM_CHARS_DONE.get(item));
                } else {
                    itemLine.append(ITEM_CHARS.get(item));
                }
                itemLine.append(" ");
            }

            objective.getScore(itemLine.toString()).setScore(score--);

            // Espaciado
            if(i < 4) {
                String spacer1 = "§" + (i * 2) + "§r";
                objective.getScore(spacer1).setScore(score--);
            }
        }

        objective.getScore(" ").setScore(score--);

        // Progreso
        String progress = ChatColor.YELLOW + "Items: " + ChatColor.GREEN +
                teamItems.size() + ChatColor.GRAY + "/" + ChatColor.WHITE + bingoItems.size();
        objective.getScore(progress).setScore(score--);

        String points = ChatColor.YELLOW + "Puntos: " + ChatColor.GREEN + teamPoints;
        objective.getScore(points).setScore(score--);
    }

    public static void updateAllTeamScoreboards() {
        for (Team team : teamScoreboards.keySet()) {
            updateTeamScoreboard(team);
        }
    }

    public static void clearAllScoreboards() {
        teamScoreboards.clear();

        // Devolver a todos los jugadores al scoreboard principal
        for (Player player : Bukkit.getOnlinePlayers()) {
            hideBingoCard(player);
        }
    }

    public static boolean hasPlayerBingoScoreboard(Player player) {
        Team team = TeamManager.getplayerTeam(player);
        if (team == null) return false;

        Scoreboard playerScoreboard = player.getScoreboard();
        Scoreboard teamScoreboard = teamScoreboards.get(team);

        return teamScoreboard != null && playerScoreboard.equals(teamScoreboard);
    }
}
