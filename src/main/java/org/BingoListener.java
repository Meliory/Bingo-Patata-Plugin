package org;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class BingoListener implements Listener {

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        try {
            if (event.getEntity() instanceof Player player) {
                Material item = event.getItem().getItemStack().getType();
                if (BingoCard.isItemOnBingo(item)) {
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
            if (event.getWhoClicked() instanceof Player player) {
                Material item = event.getRecipe().getResult().getType();
                if (BingoCard.isItemOnBingo(item)) {
                    BingoProcess.processItemPlayer(player, item);
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BingoListener] Error crítico en onItemCrafted: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void inInventoryClick(InventoryClickEvent event) {
        try {
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (event.getCurrentItem() == null) return;
            // El editor tiene su propio handler, no procesar aquí
            if (event.getInventory().getHolder() instanceof BingoCardEditor) return;

            InventoryAction action = event.getAction();
            if (!isPlayerInventory(event.getClickedInventory(), player)) {
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

    // ---- Editor de tarjeta ----

    @EventHandler
    public void onCardEditorClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BingoCardEditor editor)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int rawSlot = event.getRawSlot();

        // Shift-click desde el inventario del jugador → colocar en primer slot bingo libre
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && event.getClickedInventory() != null
                && event.getClickedInventory().equals(player.getInventory())) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() != Material.AIR) {
                int emptySlot = editor.getFirstEmptyBingoSlot();
                if (emptySlot != -1) {
                    event.getInventory().setItem(emptySlot, item.clone());
                    item.setAmount(0);
                }
            }
            return;
        }

        // Click en slots del editor que NO son bingo → cancelar
        if (rawSlot < 54 && !BingoCardEditor.isBingoSlot(rawSlot)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCardEditorClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BingoCardEditor editor)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        int filled = editor.filledSlots();
        if (filled == 25) {
            editor.saveCard();
            player.sendMessage(ChatColor.GREEN + "✔ Tarjeta guardada (" + filled + "/25 items)");
        } else {
            player.sendMessage(ChatColor.RED + "✖ Tarjeta no guardada: solo hay " + filled + "/25 items");
        }
    }

    // ---- Utilidades ----

    private boolean isPlayerInventory(Inventory inventory, Player player) {
        if (inventory == null) return false;
        return inventory.equals(player.getInventory())
                || inventory.getType() == InventoryType.CRAFTING
                || inventory.getType() == InventoryType.CREATIVE;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            Player player = event.getPlayer();
            if (TeamManager.getplayerTeam(player) != null) {
                BingoScoreboard.showBingoCard(player);
            }
            Team team = TeamManager.getplayerTeam(player);
            if (team != null) {
                BingoWorldManager.onPlayerJoinTeamWorld(player, team.getID());
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BingoListener] Error crítico en onPlayerJoin para jugador "
                    + event.getPlayer().getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
