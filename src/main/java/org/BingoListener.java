package org;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;

public class BingoListener implements Listener {

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if(event.getEntity() instanceof Player){
            Player player = (Player) event.getEntity();
            Material item = event.getItem().getItemStack().getType();

            if(BingoCard.isItemOnBingo(item)){
                BingoProcess.processItemPlayer(player, item);
            }
        }
    }

    @EventHandler
    public void onItemCrafted(CraftItemEvent event) {
        if(event.getWhoClicked() instanceof Player){
            Player player = (Player) event.getWhoClicked();
            Material item = event.getRecipe().getResult().getType();

            if(BingoCard.isItemOnBingo(item)){
                BingoProcess.processItemPlayer(player, item);
            }
        }
    }
}
