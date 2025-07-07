package org;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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

        return true;
    }

    public static void removePlayerFromAllTeams(Player player){
        for(Team team : teams){
            team.RemovePlayer(player.getUniqueId());
        }

        BingoDisplayManager.updatePlayerDisplay(player);
    }

    public static List<Team> getAllTeams(){
        return teams;
    }

    public static boolean deleteTeam(String teamName){
        Team team = getTeamByName(teamName);
        if(team != null){
            teams.remove(team);
            return true;
        }
        return false;
    }

    public static boolean deleteAllTeams(){
        for(Team team : teams){
            deleteTeam(team.getName());
        }

        BingoDisplayManager.updateAllPlayersDisplay();
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
        }
    }
}
