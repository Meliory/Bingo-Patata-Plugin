package org;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Team {

    private int id;
    private String name;
    private ChatColor color;
    private List<UUID> players;

    public Team(int id, String name, ChatColor color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.players = new ArrayList<>();
    }

    public int getID() {return id;}
    public String getName() { return name;}
    public ChatColor getColor() { return color;}
    public List<UUID> getPlayers() { return players;}

    public void AddPlayer(UUID uuid){
        if(!players.contains(uuid)){
            players.add(uuid);
        }
    }

    public void RemovePlayer(UUID uuid){
        players.remove(uuid);
    }

    public boolean HasPlayer(UUID uuid){
        return players.contains(uuid);
    }

    public String getColoredName(){
        return color + name + ChatColor.RESET;
    }

    /**
     * Obtiene el color como string para MiniMessage
     */
    public String getColorTag() {
        return switch (color) {
            case BLACK -> "black";
            case DARK_BLUE -> "dark_blue";
            case DARK_GREEN -> "dark_green";
            case DARK_AQUA -> "dark_aqua";
            case DARK_RED -> "dark_red";
            case DARK_PURPLE -> "dark_purple";
            case GOLD -> "gold";
            case GRAY -> "gray";
            case DARK_GRAY -> "dark_gray";
            case BLUE -> "blue";
            case GREEN -> "green";
            case AQUA -> "aqua";
            case RED -> "red";
            case LIGHT_PURPLE -> "light_purple";
            case YELLOW -> "yellow";
            case WHITE -> "white";
            default -> "white";
        };
    }

    public void changeTeamName(String newTeamName){
        this.name =  newTeamName;
    }

    public void changeTeamColor(ChatColor color){
        this.color = color;
    }


}
