package org;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public class BingoCard {

    private static final List<Material> BINGO_ITEMS = Arrays.asList(
            Material.PUFFERFISH_BUCKET, Material.BROWN_CONCRETE, Material.WARPED_FUNGUS, Material.ANVIL, Material.GOLDEN_AXE,
            Material.SNIFFER_EGG, Material.REDSTONE_TORCH, Material.CHERRY_PLANKS, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Material.TRIAL_KEY,
            Material.SPYGLASS, Material.SMOOTH_STONE, Material.DRIED_GHAST, Material.BAMBOO, Material.DIRT,
            Material.DIAMOND_BLOCK, Material.RESIN_CLUMP, Material.BLUE_EGG, Material.YELLOW_STAINED_GLASS, Material.REDSTONE_LAMP,
            Material.RABBIT_FOOT, Material.CHISELED_SANDSTONE, Material.BAKED_POTATO, Material.BLAZE_POWDER, Material.MUD_BRICKS
    );

    public static boolean isItemOnBingo(Material item){
        return BINGO_ITEMS.contains(item);
    }

    public static List<Material> getBingoItems() {
        return BINGO_ITEMS;
    }
}
