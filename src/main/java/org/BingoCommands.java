package org;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BingoCommands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usa: /bingo team <create|add|list|remove>");
            return true;
        }

        if(args[0].equalsIgnoreCase("team")) {
            return handleTeamCommand(sender, args);
        }

        //if(args[0].equalsIgnoreCase("player")) {
            //return handePlayerCommand(sender, args);
        //}

        return true;
    }

    public boolean handleTeamCommand(CommandSender sender, String[] args) {
        if (args.length < 2){
            sender.sendMessage("Usa: /bingo team <create|add|list|remove>");
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "create":
                return createTeam(sender, args);

            case "add":
                return addPlayerToTeam(sender, args);

            case "list":
                return listTeam(sender);

            //case "remove":
                //return removePlayer(sender, args);

            default:
                sender.sendMessage("Subcomando desconocido");
                return true;
        }
    }

    private boolean createTeam(CommandSender sender, String[] args) {
        if (args.length < 4){
            sender.sendMessage("Usa: /bingo team create <name> <color>");
            return true;
        }

        String teamName = args[2];
        ChatColor teamColor = parseColor(args[3]);

        if(teamColor == null){
            sender.sendMessage("Introduce un color válido");
            return true;
        }

        Team team = TeamManager.createTeam(teamName, teamColor);
        if(team == null){
            sender.sendMessage("Ya existe un equipo con ese nombre");
            return true;
        }

        sender.sendMessage("Equipo " + team.getColoredName() + " creado!");
        return true;
    }

    private boolean addPlayerToTeam(CommandSender sender, String[] args) {
        if (args.length < 4){
            sender.sendMessage("Usa: /bingo team add <jugador> <equipo>");
            return true;
        }

        Player player = sender.getServer().getPlayer(args[2]);
        if(player == null){
            sender.sendMessage("Jugador no encontrado");
            return true;
        }

        String teamName = args[3];
        if(TeamManager.addPlayerToTeam(player, teamName)){
            Team team = TeamManager.getTeamByName(teamName);
            sender.sendMessage("Jugador " + player.getName() + " añadido al equipo " + team.getColoredName());
            player.sendMessage("¡Has sido añadido al equipo " + team.getColoredName() + "!");
        } else {
            sender.sendMessage("Equipo no encontrado");
        }
        return true;
    }

    private boolean listTeam(CommandSender sender) {
        List<Team> teams = TeamManager.getAllTeams();

        if(!teams.isEmpty()){
            for(Team team : teams){
                ChatColor teamColor = team.getColor();
                sender.sendMessage(teamColor + " -- " + team.getName() + " --");
                List<UUID> players = team.getPlayers();
                for(UUID player : players){
                    sender.sendMessage("  - " + Bukkit.getPlayer(player).getName());
                }
            }
        } else {
            sender.sendMessage("No hay equipos disponibles");
        }
        return true;
    }


    private ChatColor parseColor(String color){
        switch (color.toLowerCase()){
            case "red": return  ChatColor.RED;
            case "green": return  ChatColor.GREEN;
            case "yellow": return  ChatColor.YELLOW;
            case "blue": return  ChatColor.BLUE;
            case "purple": return  ChatColor.LIGHT_PURPLE;
            case "orange" : return ChatColor.GOLD;
            case "aqua": return  ChatColor.AQUA;
            default: return ChatColor.WHITE;
        }

    }

    ////////////////////////////////////////////

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args){
        List<String> completions = new ArrayList<>();

        if(args.length == 1){
            completions.add("team");
            completions.add("player");
        }

        else if(args.length == 2 && args[0].equalsIgnoreCase("team")){
            completions.add("create");
            completions.add("add");
            completions.add("list");
        }

        else if(args.length == 3 && args[1].equalsIgnoreCase("create")){
            for(Player player : Bukkit.getOnlinePlayers()){
                if(player.getName().toLowerCase().startsWith(args[3].toLowerCase())){
                    completions.add(player.getName());
                }
            }
        }

        else if(args.length == 4 && args[1].equalsIgnoreCase("create")){
            completions.add("red");
            completions.add("green");
            completions.add("yellow");
            completions.add("blue");
            completions.add("purple");
            completions.add("orange");
            completions.add("aqua");
        }

        return completions;
    }
}
