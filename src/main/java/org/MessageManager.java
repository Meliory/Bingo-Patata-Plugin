package org;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestor de mensajes usando MiniMessage
 * Centraliza todos los mensajes del plugin con soporte para placeholders y formato moderno
 */
public class MessageManager {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final Map<String, String> messages = new HashMap<>();

    /**
     * Carga todos los mensajes desde la configuración
     */
    public static void loadMessages() {
        messages.clear();

        // Cargar todos los mensajes del config.yml
        loadMessagesFromSection("messages");

        BingoPatataPlugin.getInstance().getLogger().info("[MessageManager] Mensajes cargados correctamente");
    }

    /**
     * Carga mensajes de una sección de configuración recursivamente
     */
    private static void loadMessagesFromSection(String section) {
        var config = BingoPatataPlugin.getInstance().getConfig();
        var messageSection = config.getConfigurationSection(section);

        if (messageSection == null) return;

        for (String key : messageSection.getKeys(true)) {
            Object value = messageSection.get(key);
            if (value instanceof String) {
                messages.put(key, (String) value);
            }
        }
    }

    /**
     * Obtiene un mensaje del config
     */
    public static String getRaw(String key) {
        return messages.getOrDefault(key, "<red>Missing message: " + key + "</red>");
    }

    /**
     * Obtiene y parsea un mensaje a Component
     */
    public static Component get(String key) {
        return miniMessage.deserialize(getRaw(key));
    }

    /**
     * Obtiene y parsea un mensaje con placeholders
     */
    public static Component get(String key, Map<String, String> placeholders) {
        String message = getRaw(key);

        // Reemplazar placeholders
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return miniMessage.deserialize(message);
    }

    /**
     * Envía un mensaje a un jugador/sender
     */
    public static void send(CommandSender sender, String key) {
        sender.sendMessage(get(key));
    }

    /**
     * Envía un mensaje con placeholders a un jugador/sender
     */
    public static void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(get(key, placeholders));
    }

    /**
     * Envía un mensaje con prefijo
     */
    public static void sendWithPrefix(CommandSender sender, String key) {
        Component prefix = get("prefix");
        Component message = get(key);
        sender.sendMessage(prefix.append(message));
    }

    /**
     * Envía un mensaje con prefijo y placeholders
     */
    public static void sendWithPrefix(CommandSender sender, String key, Map<String, String> placeholders) {
        Component prefix = get("prefix");
        Component message = get(key, placeholders);
        sender.sendMessage(prefix.append(message));
    }

    /**
     * Broadcast a todos los jugadores online
     */
    public static void broadcast(String key) {
        Component message = get(key);
        BingoPatataPlugin.getInstance().getServer().broadcast(message);
    }

    /**
     * Broadcast con placeholders
     */
    public static void broadcast(String key, Map<String, String> placeholders) {
        Component message = get(key, placeholders);
        BingoPatataPlugin.getInstance().getServer().broadcast(message);
    }

    /**
     * Broadcast solo a un equipo
     */
    public static void broadcastToTeam(Team team, String key, Map<String, String> placeholders) {
        Component message = get(key, placeholders);

        for (var playerUUID : team.getPlayers()) {
            Player player = BingoPatataPlugin.getInstance().getServer().getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Helper para crear un mapa de placeholders rápidamente
     */
    public static Map<String, String> placeholders() {
        return new HashMap<>();
    }

    /**
     * Helper para añadir placeholders de forma fluida
     */
    public static class PlaceholderBuilder {
        private final Map<String, String> map = new HashMap<>();

        public PlaceholderBuilder add(String key, String value) {
            map.put(key, value);
            return this;
        }

        public PlaceholderBuilder add(String key, int value) {
            map.put(key, String.valueOf(value));
            return this;
        }

        public Map<String, String> build() {
            return map;
        }
    }

    public static PlaceholderBuilder builder() {
        return new PlaceholderBuilder();
    }
}
