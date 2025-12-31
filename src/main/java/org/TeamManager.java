package org;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamManager {

    private static final List<Team> teams = new ArrayList<>();

    public static Team createTeam(String name, ChatColor color){
        if(getTeamByName(name) != null){
            return null;
        }

        int newID = teams.size();

        Team team = new Team(newID, name, color);
        teams.add(team);
        saveTeams();
        return team;
    }

    public static Team getTeamByName(String name){
        for(Team team : teams){
            if(team.getName().equalsIgnoreCase(name)){
                return team;
            }
        }
        return null;
    }

    public static Team getTeamById(int id){
        if(teams.get(id) != null){
            return teams.get(id);
        } else {
            return null;
        }
    }

    public static Team getplayerTeam(Player player){
        return getPlayerTeam(player.getUniqueId());
    }

    public static Team getPlayerTeam(UUID uuid){
        for(Team team : teams){
            if(team.HasPlayer(uuid)){
                return team;
            }
        }
        return null;
    }

    public static boolean addPlayerToTeam(Player player, String teamName){
        Team team = getTeamByName(teamName);
        if(team == null){
            return false;
        }

        removePlayerFromAllTeams(player);
        team.AddPlayer(player.getUniqueId());

        BingoDisplayManager.updatePlayerDisplay(player);
        saveTeams();

        return true;
    }

    public static void removePlayerFromAllTeams(Player player){
        for(Team team : teams){
            team.RemovePlayer(player.getUniqueId());
        }

        BingoDisplayManager.updatePlayerDisplay(player);
        saveTeams();
    }

    public static List<Team> getAllTeams(){
        return teams;
    }

    public static boolean deleteTeam(String teamName){
        Team team = getTeamByName(teamName);
        if(team != null){
            teams.remove(team);
            saveTeams();
            return true;
        }
        return false;
    }

    public static boolean deleteAllTeams(){
        teams.clear();
        BingoDisplayManager.updateAllPlayersDisplay();
        saveTeams();
        return true;
    }

    public static List<String> getTeamPlayersNames(Team team){
        List<String> players = new ArrayList<>();
        for(UUID uuid : team.getPlayers()){
            Player player = Bukkit.getPlayer(uuid);
            if(player != null){
                players.add(player.getName());
            }
        }
        return players;
    }

    public static void changeTeamName(String oldTeamName, String newTeamName){
        Team team = getTeamByName(oldTeamName);
        if(team != null){
            team.changeTeamName(newTeamName);

            for(UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if(player != null && player.isOnline()) {
                    BingoDisplayManager.updatePlayerDisplay(player);
                }
            }

            saveTeams();
        }
    }

    public static void changeTeamColor(String teamName, ChatColor color){
        Team team = getTeamByName(teamName);
        if(team != null){
            team.changeTeamColor(color);

            for(UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if(player != null && player.isOnline()) {
                    BingoDisplayManager.updatePlayerDisplay(player);
                }
            }

            saveTeams();
        }
    }

    // ==================== PERSISTENCIA ====================

    private static File getTeamsFile() {
        return new File(BingoPatataPlugin.getInstance().getDataFolder(), "teams.yml");
    }

    public static void loadTeams() {
        File file = getTeamsFile();
        if (!file.exists()) {
            BingoPatataPlugin.getInstance().getLogger().info("[TeamManager] teams.yml no existe, se creará al guardar equipos");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        teams.clear();

        if (!config.contains("teams")) {
            return;
        }

        for (String teamName : config.getConfigurationSection("teams").getKeys(false)) {
            String path = "teams." + teamName;
            String colorStr = config.getString(path + ".color", "WHITE");
            ChatColor color = parseColorFromString(colorStr);

            // Crear equipo sin jugadores
            Team team = createTeam(teamName, color);

            // Cargar jugadores si existen
            List<String> playerUUIDs = config.getStringList(path + ".players");
            if (team != null && playerUUIDs != null) {
                for (String uuidStr : playerUUIDs) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        team.AddPlayer(uuid);
                    } catch (IllegalArgumentException e) {
                        BingoPatataPlugin.getInstance().getLogger().warning("[TeamManager] UUID inválido en teams.yml: " + uuidStr);
                    }
                }
            }
        }

        BingoPatataPlugin.getInstance().getLogger().info("[TeamManager] Cargados " + teams.size() + " equipos desde teams.yml");
    }

    public static void saveTeams() {
        File file = getTeamsFile();
        FileConfiguration config = new YamlConfiguration();

        for (Team team : teams) {
            String path = "teams." + team.getName();
            config.set(path + ".color", team.getColor().name());

            List<String> playerUUIDs = new ArrayList<>();
            for (UUID uuid : team.getPlayers()) {
                playerUUIDs.add(uuid.toString());
            }
            config.set(path + ".players", playerUUIDs);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            BingoPatataPlugin.getInstance().getLogger().severe("[TeamManager] Error al guardar teams.yml: " + e.getMessage());
        }
    }

    private static ChatColor parseColorFromString(String colorStr) {
        try {
            return ChatColor.valueOf(colorStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ChatColor.WHITE;
        }
    }
}
