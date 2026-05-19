package org;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public class BingoCard {

    private static final List<Material> BINGO_ITEMS = Arrays.asList(
            Material.GOLDEN_CARROT, Material.OXIDIZED_CUT_COPPER, Material.GLOW_BERRIES, Material.TRIDENT, Material.BEETROOT_SOUP,
            Material.RABBIT, Material.COPPER_NAUTILUS_ARMOR, Material.LAPIS_BLOCK, Material.PHANTOM_MEMBRANE, Material.LIGHT_GRAY_DYE,
            Material.RAW_GOLD_BLOCK, Material.RED_NETHER_BRICK_WALL, Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, Material.DECORATED_POT, Material.GREEN_GLAZED_TERRACOTTA,
            Material.GLOW_INK_SAC, Material.PINK_CANDLE, Material.BAMBOO_MOSAIC, Material.POISONOUS_POTATO, Material.OMINOUS_TRIAL_KEY,
            Material.SPONGE, Material.NETHERITE_SPEAR, Material.LILY_PAD, Material.GLOBE_BANNER_PATTERN, Material.ENCHANTING_TABLE
    );

    public static boolean isItemOnBingo(Material item){
        return BINGO_ITEMS.contains(item);
    }

    public static List<Material> getBingoItems() {
        return BINGO_ITEMS;
    }
}
