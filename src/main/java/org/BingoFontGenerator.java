package org;

import org.bukkit.Material;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BingoFontGenerator {

    private static final int HEIGHT = 16;
    private static final int ASCENT = 16;
    // Para sprites cuadrados: advance = HEIGHT + 1 → spacer = -(HEIGHT + 1)
    private static final int SPACER_ADVANCE = -(HEIGHT + 1);

    // Ruta del default.json del RP relativa al directorio de trabajo del servidor
    private static final String RP_FONT_PATH = "../resourcepack/assets/minecraft/font/default.json";

    public static File generate(BingoPatataPlugin plugin) {
        List<Material> allItems = new ArrayList<>();
        allItems.addAll(BingoItemsConfig.getEnabledItems());
        allItems.addAll(BingoItemsConfig.getDisabledItems());

        String json = buildJson(allItems);

        // Intentar escribir directamente en el default.json del RP
        File rpFile = new File(RP_FONT_PATH);
        if (rpFile.getParentFile().exists()) {
            if (write(rpFile, json, plugin)) {
                plugin.getLogger().info("[BingoFontGenerator] default.json del RP actualizado: " + rpFile.getAbsolutePath());
                return rpFile;
            }
        }

        // Fallback: plugin data folder
        File fallback = new File(plugin.getDataFolder(), "default.json");
        write(fallback, json, plugin);
        plugin.getLogger().warning("[BingoFontGenerator] No se encontró el RP en ruta relativa. Guardado en: " + fallback.getAbsolutePath());
        return fallback;
    }

    private static String buildJson(List<Material> allItems) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"providers\": [\n");

        boolean first = true;
        for (Material m : allItems) {
            if (!first) sb.append(",\n");
            first = false;

            String charEscaped = String.format("\\u%04X", (int) BingoItemsConfig.getItemChar(m));
            sb.append("    {\n")
              .append("      \"type\": \"bitmap\",\n")
              .append("      \"file\": \"minecraft:font/").append(m.name().toLowerCase()).append(".png\",\n")
              .append("      \"ascent\": ").append(ASCENT).append(",\n")
              .append("      \"height\": ").append(HEIGHT).append(",\n")
              .append("      \"chars\": [\"").append(charEscaped).append("\"]\n")
              .append("    }");
        }

        // Spacer negativo: rebobina el cursor al inicio del sprite anterior
        sb.append(",\n    {\n")
          .append("      \"type\": \"space\",\n")
          .append("      \"advances\": { \"\\uF8FE\": ").append(SPACER_ADVANCE).append(" }\n")
          .append("    }");

        // Overlay de completado (sprite único que se superpone sobre el item)
        sb.append(",\n    {\n")
          .append("      \"type\": \"bitmap\",\n")
          .append("      \"file\": \"minecraft:font/done_overlay.png\",\n")
          .append("      \"ascent\": ").append(ASCENT).append(",\n")
          .append("      \"height\": ").append(HEIGHT).append(",\n")
          .append("      \"chars\": [\"\\uF8FF\"]\n")
          .append("    }");

        sb.append("\n  ]\n}");
        return sb.toString();
    }

    private static boolean write(File file, String content, BingoPatataPlugin plugin) {
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("[BingoFontGenerator] Error al escribir " + file.getPath() + ": " + e.getMessage());
            return false;
        }
    }
}
