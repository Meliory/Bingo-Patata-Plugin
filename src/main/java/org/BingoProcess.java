package org;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class BingoProcess {
    public static void processItemPlayer(Player player, Material item) {
        Team team = TeamManager.getplayerTeam(player);

        //Si no está en un equipo, fuera
        if(team == null){
            player.sendMessage(ChatColor.RED + "No estás en un equipo");
            return;
        }

        //Si el equipo tiene el objeto, fuera
        if(BingoData.hasTeamItem(team, item)){
            player.sendMessage(ChatColor.BLUE + "You already have that item!");
            return;
        }

        player.sendMessage("Has conseguido el siguiente item: " + item.toString());

        Bukkit.getScheduler().runTaskLater(BingoPatataPlugin.getInstance(), () -> {
            removeItemFromInventory(player, item);
        },1L);

        BingoData.addTeamItem(team, item);
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
                player.sendMessage("Se ha eliminado de tu inventario");
                break;
            }
        }
    }
}
