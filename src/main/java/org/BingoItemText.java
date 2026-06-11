package org;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class BingoItemText {

    // String MiniMessage con hover para usar en MessageManager placeholders
    public static String miniMessageWithHover(Material material) {
        String displayName = formatName(material);
        String key = material.getKey().toString();
        return "<hover:show_item:'" + key + "':1><yellow>" + displayName + "</yellow></hover>";
    }

    // Component con el char del RP + overlay si está hecho + hover del item real (para card chat)
    public static Component charWithHover(Material material, boolean done) {
        String text = String.valueOf(BingoItemsConfig.getItemChar(material));
        if (done) {
            text += BingoItemsConfig.NEGATIVE_SPACER + String.valueOf(BingoItemsConfig.DONE_OVERLAY);
        }
        return Component.text(text)
                .hoverEvent(new ItemStack(material).asHoverEvent());
    }

    // Nombre formateado en Title Case: "GOLDEN_CARROT" → "Golden Carrot"
    public static String formatName(Material material) {
        String[] words = material.name().toLowerCase().split("_");
        TextComponent.Builder sb = Component.text();
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
}
