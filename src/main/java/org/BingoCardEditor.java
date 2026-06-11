package org;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class BingoCardEditor implements InventoryHolder {

    // Slots de la cuadrícula 5x5 centrada en un inventario de 9x6
    public static final int[] BINGO_SLOTS = {
             2,  3,  4,  5,  6,
            11, 12, 13, 14, 15,
            20, 21, 22, 23, 24,
            29, 30, 31, 32, 33,
            38, 39, 40, 41, 42
    };

    private static final Set<Integer> BINGO_SLOT_SET = new HashSet<>();

    static {
        for (int slot : BINGO_SLOTS) {
            BINGO_SLOT_SET.add(slot);
        }
    }

    private final Inventory inventory;

    public BingoCardEditor() {
        this.inventory = Bukkit.createInventory(this, 54,
                Component.text("Editor de Tarjeta Bingo"));

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.empty());
        glass.setItemMeta(meta);

        for (int i = 0; i < 54; i++) {
            if (!BINGO_SLOT_SET.contains(i)) {
                inventory.setItem(i, glass);
            }
        }

        List<Material> activeCard = BingoCard.getActiveCard();
        for (int i = 0; i < BINGO_SLOTS.length && i < activeCard.size(); i++) {
            inventory.setItem(BINGO_SLOTS[i], new ItemStack(activeCard.get(i)));
        }
    }

    public static boolean isBingoSlot(int slot) {
        return BINGO_SLOT_SET.contains(slot);
    }

    public int getFirstEmptyBingoSlot() {
        for (int slot : BINGO_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() == Material.AIR) {
                return slot;
            }
        }
        return -1;
    }

    public int filledSlots() {
        int count = 0;
        for (int slot : BINGO_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                count++;
            }
        }
        return count;
    }

    public void saveCard() {
        List<Material> newCard = new ArrayList<>();
        for (int slot : BINGO_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                newCard.add(item.getType());
            }
        }
        if (newCard.size() == 25) {
            BingoCard.setActiveCard(newCard);
            BingoScoreboard.refreshCardDisplay();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
