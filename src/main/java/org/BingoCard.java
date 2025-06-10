package org;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public class BingoCard {
    private static final List<Material> BINGO_ITEMS = Arrays.asList(
            Material.DIAMOND, Material.IRON_INGOT, Material.GOLD_INGOT
    );

    public static boolean isItemOnBingo(Material item){
        return BINGO_ITEMS.contains(item);
    }

    public static List<Material> getBingoItems() {
        return BINGO_ITEMS;
    }
}
