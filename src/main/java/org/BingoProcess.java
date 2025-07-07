package org;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

public class BingoProcess {
    public static void processItemPlayer(Player player, Material item) {
        Team team = TeamManager.getplayerTeam(player);

        //Si no está en un equipo, fuera
        if(team == null){
            //player.sendMessage(ChatColor.RED + "No estás en un equipo");
            return;
        }

        //Si el equipo tiene el objeto, fuera
        if(BingoData.hasTeamItem(team, item)){
            //player.sendMessage(ChatColor.BLUE + "You already have that item!");
            return;
        }

        //player.sendMessage("Has conseguido el siguiente item: " + item.toString());

        Bukkit.getScheduler().runTaskLater(BingoPatataPlugin.getInstance(), () -> {
            removeItemFromInventory(player, item);
        },1L);

        BingoData.addTeamItem(team, item);

        BingoScoreboard.updateTeamScoreboard(team);

        for(UUID uuid : team.getPlayers()){
            Player teamplayer =  Bukkit.getPlayer(uuid);
            if(teamplayer != null && teamplayer.isOnline()){
                teamplayer.sendMessage(
                        "[" + BingoTimer.getActualTimeFormatted() + " | " + BingoData.getTeamItems(team).size() + "/25] " +
                        team.getColor() + player.getName() + ChatColor.WHITE + " ha conseguido [" + item.name() + "]"
                );

                teamplayer.playSound(
                        player.getLocation(),
                        Sound.ENTITY_PLAYER_LEVELUP,
                        1.0f,
                        1.0f
                );
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
