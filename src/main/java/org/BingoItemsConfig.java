package org;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class BingoItemsConfig {

    private static List<Material> enabledItems = new ArrayList<>();
    private static List<Material> disabledItems = new ArrayList<>();

    // Items imposibles o no deseados en survival → desactivados por defecto
    private static final Set<String> DEFAULT_DISABLED = new HashSet<>(Arrays.asList(
            // Creative / comandos únicamente
            "COMMAND_BLOCK", "REPEATING_COMMAND_BLOCK", "CHAIN_COMMAND_BLOCK",
            "COMMAND_BLOCK_MINECART",
            "BARRIER",
            "BEDROCK",
            "STRUCTURE_BLOCK", "STRUCTURE_VOID", "JIGSAW",
            "DEBUG_STICK", "TEST_BLOCK", "TEST_INSTANCE_BLOCK",
            "KNOWLEDGE_BOOK",
            "LIGHT",
            "TRIAL_SPAWNER", "SPAWNER",
            "VAULT",
            "END_PORTAL_FRAME",
            "REINFORCED_DEEPSLATE",
            "PETRIFIED_OAK_SLAB",
            "BUDDING_AMETHYST",
            // Bloques que no dropean como item
            "FARMLAND",
            "LARGE_FERN", "TALL_GRASS", "TALL_DRY_GRASS",
            "OPEN_EYEBLOSSOM", "CLOSED_EYEBLOSSOM",
            "SUSPICIOUS_SAND", "SUSPICIOUS_GRAVEL",
            // Aire
            "AIR", "CAVE_AIR", "VOID_AIR"
    ));

    public static void loadOrGenerate(BingoPatataPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "items.yml");

        if (!file.exists()) {
            generate(plugin, file);
        } else {
            load(file);
        }

        plugin.getLogger().info("[BingoItemsConfig] Items cargados: " + enabledItems.size() +
                " activos, " + disabledItems.size() + " desactivados");
    }

    private static void generate(BingoPatataPlugin plugin, File file) {
        plugin.getLogger().info("[BingoItemsConfig] Generando items.yml con todos los materiales disponibles...");

        List<Material> allItems = Arrays.stream(Material.values())
                .filter(m -> !m.name().startsWith("LEGACY_"))
                .filter(Material::isItem)
                .sorted(Comparator.comparing(Material::name))
                .collect(Collectors.toList());

        List<String> enabled = new ArrayList<>();
        List<String> disabled = new ArrayList<>();

        for (Material m : allItems) {
            if (isDefaultDisabled(m)) {
                disabled.add(m.name());
            } else {
                enabled.add(m.name());
            }
        }

        FileConfiguration config = new YamlConfiguration();
        config.set("enabled", enabled);
        config.set("disabled", disabled);

        try {
            config.save(file);
            plugin.getLogger().info("[BingoItemsConfig] items.yml generado con " + allItems.size() + " materiales");
        } catch (IOException e) {
            plugin.getLogger().severe("[BingoItemsConfig] Error al guardar items.yml: " + e.getMessage());
        }

        loadFromLists(enabled, disabled);
    }

    private static void load(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> enabled = config.getStringList("enabled");
        List<String> disabled = config.getStringList("disabled");
        loadFromLists(enabled, disabled);
    }

    private static void loadFromLists(List<String> enabledNames, List<String> disabledNames) {
        enabledItems.clear();
        disabledItems.clear();

        for (String name : enabledNames) {
            Material m = Material.matchMaterial(name);
            if (m != null) {
                enabledItems.add(m);
            } else {
                BingoPatataPlugin.getInstance().getLogger().warning(
                        "[BingoItemsConfig] Material desconocido en enabled: " + name);
            }
        }

        for (String name : disabledNames) {
            Material m = Material.matchMaterial(name);
            if (m != null) {
                disabledItems.add(m);
            }
        }
    }

    private static boolean isDefaultDisabled(Material m) {
        String name = m.name();
        return DEFAULT_DISABLED.contains(name)
                || name.endsWith("_SPAWN_EGG")
                || name.endsWith("_POTTERY_SHERD")
                || name.endsWith("_BANNER_PATTERN")
                || name.startsWith("INFESTED_");
    }

    public static void reload(BingoPatataPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "items.yml");
        if (file.exists()) {
            load(file);
            plugin.getLogger().info("[BingoItemsConfig] items.yml recargado");
        } else {
            loadOrGenerate(plugin);
        }
    }

    public static List<Material> getEnabledItems() {
        return Collections.unmodifiableList(enabledItems);
    }

    public static List<Material> getDisabledItems() {
        return Collections.unmodifiableList(disabledItems);
    }

    // Carácter Unicode fijo por item (ordinal estable entre versiones de MC)
    public static char getItemChar(Material material) {
        return (char) (0xE000 + material.ordinal());
    }

    // Chars globales fijos para el overlay de "completado"
    // 0xF8FE/0xF8FF: al final del área privada BMP, sin colisión con ordinales de Material
    public static final char NEGATIVE_SPACER = ''; // advance negativo en el RP (rebobina el cursor)
    public static final char DONE_OVERLAY    = ''; // sprite de overlay encima del item
}
