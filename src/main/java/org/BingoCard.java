package org;

import org.bukkit.Material;

import java.util.*;

public class BingoCard {

    private static List<Material> itemPool = new ArrayList<>();
    private static List<Material> activeCard = new ArrayList<>();

    public static void initialize() {
        itemPool = new ArrayList<>(BingoItemsConfig.getEnabledItems());
        activeCard = new ArrayList<>(itemPool.subList(0, Math.min(25, itemPool.size())));
    }

    public static List<Material> getItemPool() {
        return Collections.unmodifiableList(itemPool);
    }

    public static List<Material> getActiveCard() {
        return Collections.unmodifiableList(activeCard);
    }

    public static boolean isItemOnBingo(Material item) {
        return activeCard.contains(item);
    }

    public static void generateRandomCard() {
        List<Material> pool = new ArrayList<>(itemPool);
        Collections.shuffle(pool);
        activeCard = new ArrayList<>(pool.subList(0, Math.min(25, pool.size())));
    }

    public static void setActiveCard(List<Material> items) {
        activeCard = new ArrayList<>(items);
    }

    public static void resetToDefault() {
        activeCard = new ArrayList<>(itemPool.subList(0, Math.min(25, itemPool.size())));
    }

    public static void reload() {
        BingoItemsConfig.reload(BingoPatataPlugin.getInstance());
        itemPool = new ArrayList<>(BingoItemsConfig.getEnabledItems());
        activeCard = new ArrayList<>(itemPool.subList(0, Math.min(25, itemPool.size())));
    }
}
