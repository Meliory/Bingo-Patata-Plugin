package org;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static org.apache.logging.log4j.LogManager.getLogger;

public class BingoScoreboard {

    // UN scoreboard por equipo (no por jugador)
    private static final Map<Team, Scoreboard> teamScoreboards = new HashMap<>();

    // Task para actualización periódica
    private static BukkitRunnable periodicUpdateTask;

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

        // Obtener título desde config usando MiniMessage
        Component title = MessageManager.get("scoreboard.title");
        Objective objective = scoreboard.registerNewObjective("bingo", "dummy", title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // QUITAR NÚMEROS ROJOS - Múltiples métodos para máxima compatibilidad
        try {
            // Método 1: numberFormat (Paper 1.20.5+)
            objective.numberFormat(NumberFormat.blank());
        } catch (Exception e) {
            try {
                // Método 2: setDisplayName con formato vacío
                objective.setRenderType(RenderType.INTEGER);
            } catch (Exception ex) {
                Bukkit.getLogger().warning("[BingoScoreboard] No se pudo ocultar números del scoreboard: " + ex.getMessage());
            }
        }

        // Guardar el scoreboard del equipo
        teamScoreboards.put(team, scoreboard);

        // Actualizar contenido inicial
        updateTeamScoreboardContent(team);

        return scoreboard;
    }

    public static void updateTeamScoreboard(Team team) {
        try {
            if(team == null) return;

            if (!teamScoreboards.containsKey(team)) {
                return; // No tiene scoreboard, no actualizar
            }

            // Validar que todos los jugadores del equipo tengan el scoreboard correcto
            ensureAllPlayersHaveCorrectScoreboard(team);

            updateTeamScoreboardContent(team);
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BingoScoreboard] Error crítico en updateTeamScoreboard - Team: " +
                (team != null ? team.getName() : "null"));
            e.printStackTrace();
        }
    }

    /**
     * Asegura que todos los jugadores del equipo tengan el scoreboard correcto asignado
     */
    private static void ensureAllPlayersHaveCorrectScoreboard(Team team) {
        if (team == null) return;

        Scoreboard teamScoreboard = teamScoreboards.get(team);
        if (teamScoreboard == null) return;

        for (UUID uuid : team.getPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Si el jugador no tiene el scoreboard correcto, reasignarlo
                if (!player.getScoreboard().equals(teamScoreboard)) {
                    player.setScoreboard(teamScoreboard);
                    Bukkit.getLogger().warning("[BingoScoreboard] Jugador " + player.getName() +
                        " tenía scoreboard incorrecto, reasignando...");
                }
            }
        }
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
        try {
            if(team == null) {
                Bukkit.getLogger().severe("[BingoScoreboard] Error crítico: team es null en updateTeamScoreboardContent");
                return;
            }

            Scoreboard scoreboard = teamScoreboards.get(team);
            if (scoreboard == null) return;

            Objective objective = scoreboard.getObjective("bingo");

            // Si el objective no existe o está corrupto, recrearlo completamente
            if (objective == null) {
                Bukkit.getLogger().warning("[BingoScoreboard] Objective null para equipo " + team.getName() + ", recreando...");
                rebuildTeamScoreboard(team);
                return;
            }

            // Limpiar scoreboard anterior - método más robusto
            try {
                objective.unregister();
            } catch (Exception e) {
                Bukkit.getLogger().warning("[BingoScoreboard] Error al desregistrar objective, recreando scoreboard completo");
                rebuildTeamScoreboard(team);
                return;
            }

            // Recrear el objective
            Component title = MessageManager.get("scoreboard.title");
            objective = scoreboard.registerNewObjective("bingo", "dummy", title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            // QUITAR NÚMEROS ROJOS
            try {
                objective.numberFormat(NumberFormat.blank());
            } catch (Exception e) {
                try {
                    objective.setRenderType(RenderType.INTEGER);
                } catch (Exception ex) {
                    // Ignorar si no se puede
                }
            }

            Set<Material> teamItems = BingoData.getTeamItems(team);
            int teamPoints = BingoData.getTeamPoints(team);
            List<Material> bingoItems = BingoCard.getBingoItems();

            int score = 40;

        //Título Equipo - Usar mensaje del config con color del equipo
        var teamPlaceholders = MessageManager.builder()
                .add("team", team.getName())
                .add("team_color", team.getColorTag())
                .build();
        Component teamComponent = MessageManager.get("scoreboard.line_team", teamPlaceholders);
        String teamLine = LegacyComponentSerializer.legacySection().serialize(teamComponent);
        objective.getScore(teamLine).setScore(score--);

        // Líneas separadoras invisibles usando códigos de color únicos (invisibles pero únicos)
        objective.getScore(ChatColor.RESET + "").setScore(score--);
        objective.getScore(ChatColor.RESET + " ").setScore(score--);

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

        // Línea separadora invisible
        objective.getScore(ChatColor.RESET + "  ").setScore(score--);

        // Progreso - Usar mensajes decorados del config
        var itemsPlaceholders = MessageManager.builder()
                .add("items", teamItems.size())
                .build();
        Component itemsComponent = MessageManager.get("scoreboard.line_items", itemsPlaceholders);
        String itemsLine = LegacyComponentSerializer.legacySection().serialize(itemsComponent);
        objective.getScore(itemsLine).setScore(score--);

        var pointsPlaceholders = MessageManager.builder()
                .add("points", teamPoints)
                .build();
        Component pointsComponent = MessageManager.get("scoreboard.line_points", pointsPlaceholders);
        String pointsLine = LegacyComponentSerializer.legacySection().serialize(pointsComponent);
        objective.getScore(pointsLine).setScore(score--);

        } catch (Exception e) {
            Bukkit.getLogger().severe("[BingoScoreboard] Error crítico en updateTeamScoreboardContent - Team: " +
                (team != null ? team.getName() : "null"));
            e.printStackTrace();
        }
    }

    public static void updateAllTeamScoreboards() {
        for (Team team : teamScoreboards.keySet()) {
            updateTeamScoreboard(team);
        }
    }

    /**
     * Recarga los títulos y contenido de todos los scoreboards
     * Se usa cuando se hace reload de la configuración
     */
    public static void reloadAllScoreboards() {
        for (Team team : teamScoreboards.keySet()) {
            Scoreboard scoreboard = teamScoreboards.get(team);
            if (scoreboard == null) continue;

            Objective objective = scoreboard.getObjective("bingo");
            if (objective == null) continue;

            // Actualizar título desde config
            Component newTitle = MessageManager.get("scoreboard.title");
            objective.displayName(newTitle);

            // Actualizar contenido
            updateTeamScoreboardContent(team);
        }
    }

    /**
     * Reconstruye completamente el scoreboard de un equipo
     * Se usa cuando hay un error crítico con el scoreboard
     */
    private static void rebuildTeamScoreboard(Team team) {
        if (team == null) return;

        Bukkit.getLogger().info("[BingoScoreboard] Reconstruyendo scoreboard completo para equipo " + team.getName());

        // Eliminar el scoreboard viejo
        teamScoreboards.remove(team);

        // Crear uno nuevo
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();

        Component title = MessageManager.get("scoreboard.title");
        Objective objective = scoreboard.registerNewObjective("bingo", "dummy", title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        try {
            objective.numberFormat(NumberFormat.blank());
        } catch (Exception e) {
            try {
                objective.setRenderType(RenderType.INTEGER);
            } catch (Exception ex) {
                // Ignorar
            }
        }

        // Guardar el nuevo scoreboard
        teamScoreboards.put(team, scoreboard);

        // Reasignar a todos los jugadores del equipo
        for (UUID uuid : team.getPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.setScoreboard(scoreboard);
            }
        }

        // Actualizar contenido (esto ya no intentará desregistrar porque es nuevo)
        updateTeamScoreboardContent(team);
    }

    /**
     * Inicia la actualización periódica de todos los scoreboards
     * Se ejecuta cada 60 segundos para prevenir desincronización
     */
    public static void startPeriodicUpdate() {
        stopPeriodicUpdate(); // Por si ya estaba corriendo

        periodicUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (Team team : teamScoreboards.keySet()) {
                        // Forzar actualización completa
                        rebuildTeamScoreboard(team);
                    }
                    Bukkit.getLogger().info("[BingoScoreboard] Actualización periódica completada para " +
                        teamScoreboards.size() + " equipos");
                } catch (Exception e) {
                    Bukkit.getLogger().severe("[BingoScoreboard] Error en actualización periódica");
                    e.printStackTrace();
                }
            }
        };

        // Ejecutar cada 60 segundos (1200 ticks)
        periodicUpdateTask.runTaskTimer(BingoPatataPlugin.getInstance(), 1200L, 1200L);
        Bukkit.getLogger().info("[BingoScoreboard] Actualización periódica de scoreboards iniciada (cada 60 segundos)");
    }

    /**
     * Detiene la actualización periódica
     */
    public static void stopPeriodicUpdate() {
        if (periodicUpdateTask != null) {
            periodicUpdateTask.cancel();
            periodicUpdateTask = null;
            Bukkit.getLogger().info("[BingoScoreboard] Actualización periódica de scoreboards detenida");
        }
    }

    public static void clearAllScoreboards() {
        stopPeriodicUpdate(); // Detener actualización periódica
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
