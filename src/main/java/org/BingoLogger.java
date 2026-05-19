package org;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BingoLogger {

    private static File logFile;
    private static final String LOG_FILENAME = "bingo_items_log.txt";

    /**
     * Inicializa el logger y crea el directorio si no existe
     */
    public static void initialize() {
        try {
            File dataFolder = BingoPatataPlugin.getInstance().getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            logFile = new File(dataFolder, LOG_FILENAME);
        } catch (Exception e) {
            Bukkit.getLogger().severe("[BingoLogger] Error crítico al inicializar logger: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Borra el archivo de logs existente y crea uno nuevo
     * Se llama al inicio de cada partida
     */
    public static void resetLog() {
        try {
            if (logFile == null) {
                initialize();
            }

            // Borrar archivo si existe
            if (logFile.exists()) {
                boolean deleted = logFile.delete();
                if (!deleted) {
                    Bukkit.getLogger().severe("[BingoLogger] Error crítico: No se pudo borrar el archivo de logs");
                    return;
                }
            }

            // Crear nuevo archivo con cabecera
            logFile.createNewFile();

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String timestamp = sdf.format(new Date());

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, false))) {
                writer.write("========================================");
                writer.newLine();
                writer.write("   BINGO PATATA - LOG DE ITEMS");
                writer.newLine();
                writer.write("   Partida iniciada: " + timestamp);
                writer.newLine();
                writer.write("========================================");
                writer.newLine();
                writer.newLine();
            }

            Bukkit.getLogger().info("[BingoLogger] Archivo de logs reiniciado correctamente");

        } catch (IOException e) {
            Bukkit.getLogger().severe("[BingoLogger] Error crítico al resetear logs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registra un item conseguido en el log
     * @param team Equipo que consiguió el item
     * @param player Jugador que consiguió el item
     * @param item Item conseguido
     * @param gameTime Tiempo de partida en formato HH:MM:SS
     */
    public static void logItem(Team team, Player player, Material item, String gameTime) {
        try {
            if (logFile == null || !logFile.exists()) {
                Bukkit.getLogger().severe("[BingoLogger] Error crítico: Archivo de logs no existe");
                return;
            }

            // Formato: [HH:MM:SS] Equipo: TeamName | Jugador: PlayerName | Item: ITEM_NAME
            String logEntry = String.format("[%s] Equipo: %s | Jugador: %s | Item: %s",
                    gameTime,
                    team.getName(),
                    player.getName(),
                    item.name()
            );

            // Escribir al archivo
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write(logEntry);
                writer.newLine();
            }

        } catch (IOException e) {
            Bukkit.getLogger().severe("[BingoLogger] Error crítico al escribir log - Jugador: " +
                    (player != null ? player.getName() : "null") + " Item: " + (item != null ? item.name() : "null"));
            e.printStackTrace();
        }
    }

    /**
     * Registra un evento especial en el log (línea completada, bingo completo, etc.)
     * @param event Descripción del evento
     * @param gameTime Tiempo de partida
     */
    public static void logEvent(String event, String gameTime) {
        try {
            if (logFile == null || !logFile.exists()) {
                return;
            }

            String logEntry = String.format("[%s] *** EVENTO: %s ***", gameTime, event);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write(logEntry);
                writer.newLine();
            }

        } catch (IOException e) {
            Bukkit.getLogger().severe("[BingoLogger] Error crítico al escribir evento: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtiene la ruta del archivo de logs
     */
    public static String getLogFilePath() {
        return logFile != null ? logFile.getAbsolutePath() : "No disponible";
    }
}
