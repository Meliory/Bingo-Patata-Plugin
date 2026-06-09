package org;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.Inventory;


public class BingoListener implements Listener {

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        try {
            if(event.getEntity() instanceof Player){
                Player player = (Player) event.getEntity();
                Material item = event.getItem().getItemStack().getType();

                if(BingoCard.isItemOnBingo(item)){
                    BingoProcess.processItemPlayer(player, item);
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BingoListener] Error crítico en onItemPickup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onItemCrafted(CraftItemEvent event) {
        try {
            if(event.getWhoClicked() instanceof Player){
                Player player = (Player) event.getWhoClicked();
                Material item = event.getRecipe().getResult().getType();

                if(BingoCard.isItemOnBingo(item)){
                    BingoProcess.processItemPlayer(player, item);
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BingoListener] Error crítico en onItemCrafted: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void inInventoryClick(InventoryClickEvent event){
        try {
            if(!(event.getWhoClicked() instanceof Player)) return;
            if(event.getCurrentItem() == null) return;

            Player player = (Player) event.getWhoClicked();
            InventoryAction action = event.getAction();

            if(!isPlayerInventory(event.getClickedInventory(), player)) {
                Material item = event.getCurrentItem().getType();

                if (action == InventoryAction.PICKUP_ALL ||
                        action == InventoryAction.PICKUP_HALF ||
                        action == InventoryAction.PICKUP_ONE ||
                        action == InventoryAction.PICKUP_SOME ||
                        action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    if (BingoCard.isItemOnBingo(item)) {
                        BingoProcess.processItemPlayer(player, item);
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BingoListener] Error crítico en inInventoryClick: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isPlayerInventory(Inventory inventory, Player player){
        if(inventory == null) return false;

        return inventory.equals(player.getInventory()) || inventory.getType() == InventoryType.CRAFTING || inventory.getType() == InventoryType.CREATIVE;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        try {
            Player player = event.getPlayer();
            if(TeamManager.getplayerTeam(player) != null){
                BingoScoreboard.showBingoCard(player);
            }
            Team team = TeamManager.getplayerTeam(event.getPlayer());
            if(team != null){
                int teamID = team.getID();
                BingoWorldManager.onPlayerJoinTeamWorld(event.getPlayer(), teamID);
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BingoListener] Error crítico en onPlayerJoin para jugador " + event.getPlayer().getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
