package org;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BingoData {
    private static final Map<String, Set<Material>> teamsItems = new HashMap<>();

    public static boolean hasTeamItem(Team team, Material item){
        String teamName = team.getName();
        return teamsItems.getOrDefault(teamName, new HashSet<>()).contains(item);
    }

    public static void addTeamItem(Team team, Material item){
        String teamName = team.getName();
        teamsItems.computeIfAbsent(teamName, k -> new HashSet<>()).add(item);
    }

    public static Set<Material> getTeamItems(Team team){
        return teamsItems.getOrDefault(team.getName(), new HashSet<>());
    }
}
