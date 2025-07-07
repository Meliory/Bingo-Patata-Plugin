package org;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.security.PublicKey;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BingoTimer {

    private static BukkitRunnable timerTask;
    private static int timeLeftInSeconds;
    private static boolean isRunning = false;

    private static final int TOTAL_time = 3 * 60 * 60;

    // Tiempos para anuncios (en segundos)
    private static final int TIME_2_HOURS_30_MIN = 9000;
    private static final int TIME_2_HOURS = 7200;
    private static final int TIME_1_HOUR_30_MIN = 5400;
    private static final int TIME_1_HOUR = 3600;
    private static final int TIME_30_MIN = 1800;
    private static final int TIME_15_MIN = 900;
    private static final int TIME_5_MIN = 300;

    private static final Set<Integer> announcedTimes = new HashSet<>();

    public static void startTimer(){
        if(isRunning){
            return;
        }

        timeLeftInSeconds = TOTAL_time;
        setupTimer();
    }

    public static void resumeTimer(){
        if(isRunning){
            return;
        }
        setupTimer();
    }

    public static void setTimer(int seconds){
        timeLeftInSeconds = seconds;
    }

    private static void setupTimer(){
        isRunning = true;

        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                //Si el tiempo se acabó
                if(timeLeftInSeconds <= 0){
                    endGame();
                    this.cancel();
                    return;
                }

                //Mostrar tiempo a los jugadores
                showTimeToAllPlayers();

                checkTimeAnnouncements();

                //Restar 1 segundo
                timeLeftInSeconds--;
            }
        };

        timerTask.runTaskTimer(BingoPatataPlugin.getInstance(), 0L, 20L);
    }

    private static void checkTimeAnnouncements() {

        // ===== ANUNCIO: 2 HORAS Y 30 MINUTOS =====
        if (timeLeftInSeconds == TIME_2_HOURS_30_MIN && !announcedTimes.contains(TIME_2_HOURS_30_MIN)) {
            announceTimeRemaining("2 horas y 30 minutos", ChatColor.YELLOW);
            announcedTimes.add(TIME_2_HOURS_30_MIN);
        }

        // ===== ANUNCIO: 2 HORAS =====
        else if (timeLeftInSeconds == TIME_2_HOURS && !announcedTimes.contains(TIME_2_HOURS)) {
            announceTimeRemaining("2 horas", ChatColor.YELLOW);
            announcedTimes.add(TIME_2_HOURS);
        }

        // ===== ANUNCIO ESPECIAL: 1 HORA Y 30 MINUTOS (MITAD DEL TIEMPO) =====
        else if (timeLeftInSeconds == TIME_1_HOUR_30_MIN && !announcedTimes.contains(TIME_1_HOUR_30_MIN)) {
            announceTimeRemaining("1 hora y 30 minutos", ChatColor.GOLD); // true = anuncio especial de mitad
            announcedTimes.add(TIME_1_HOUR_30_MIN);
        }

        // ===== ANUNCIO: 1 HORA =====
        else if (timeLeftInSeconds == TIME_1_HOUR && !announcedTimes.contains(TIME_1_HOUR)) {
            announceTimeRemaining("1 hora", ChatColor.GOLD);
            announcedTimes.add(TIME_1_HOUR);
        }

        // ===== ANUNCIO: 30 MINUTOS =====
        else if (timeLeftInSeconds == TIME_30_MIN && !announcedTimes.contains(TIME_30_MIN)) {
            announceTimeRemaining("30 minutos", ChatColor.RED);
            announcedTimes.add(TIME_30_MIN);
        }

        // ===== ANUNCIO: 15 MINUTOS (1 CUARTO DE HORA) =====
        else if (timeLeftInSeconds == TIME_15_MIN && !announcedTimes.contains(TIME_15_MIN)) {
            announceTimeRemaining("15 minutos", ChatColor.RED);
            announcedTimes.add(TIME_15_MIN);
        }

        // ===== ANUNCIO: 5 MINUTOS =====
        else if (timeLeftInSeconds == TIME_5_MIN && !announcedTimes.contains(TIME_5_MIN)) {
            announceTimeRemaining("5 minutos", ChatColor.DARK_RED);
            announcedTimes.add(TIME_5_MIN);
        }

        // ===== CUENTA ATRÁS FINAL: 5, 4, 3, 2, 1 SEGUNDOS =====
        else if (timeLeftInSeconds <= 5 && timeLeftInSeconds >= 1) {
            announceCountdown(timeLeftInSeconds);
            // No se agrega a announcedTimes porque queremos que se ejecute cada segundo
        }
    }

    private static void announceTimeRemaining(String timeString, ChatColor color) {

        // Anuncio en el chat
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(color + "═══════════════════════════");
        Bukkit.broadcastMessage(color + "TIEMPO RESTANTE: " + timeString.toUpperCase());
        Bukkit.broadcastMessage(color + "═══════════════════════════");
        Bukkit.broadcastMessage("");

        // Sonido y título para todos los jugadores
        for (Player player : Bukkit.getOnlinePlayers()) {

            // Sonido de campana
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
        }
    }

    private static void announceCountdown(int seconds) {
        ChatColor color = seconds <= 3 ? ChatColor.DARK_RED : ChatColor.RED;

        // Anuncio en el chat
        Bukkit.broadcastMessage(color + "" + ChatColor.BOLD + "⏰ " + seconds + " SEGUNDOS RESTANTES");

        // Título y sonido para todos los jugadores
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Título dramático con el número grande
            player.sendTitle(
                    color + "" + ChatColor.BOLD + String.valueOf(seconds),
                    " ",
                    5,   // fadeIn (0.25 segundos)
                    15,  // stay (0.75 segundos)
                    5    // fadeOut (0.25 segundos)
            );

            // Sonido más intenso para cuenta atrás
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f,
                    seconds <= 3 ? 2.0f : 1.5f); // Tono más agudo para 3,2,1
        }
    }

    public static void stopTimer(){
        if(timerTask != null){
            timerTask.cancel();
        }
        isRunning = false;
    }

    private static void showTimeToAllPlayers(){
        String timeDisplay = formatTime(timeLeftInSeconds);

        for(Player player : Bukkit.getOnlinePlayers()){
            player.sendActionBar(timeDisplay);
        }
    }

    private static String formatTime(int seconds){
        int hours = seconds/3600;
        int minutes = (seconds % 3600) / 60;
        int second = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, second);
    }

    private static void endGame(){
        isRunning = false;

        BingoCommands.endGame();
    }

    public static boolean isRunning(){
        return isRunning;
    }

    public static int getTimeLeft(){
        return timeLeftInSeconds;
    }

    public static String getActualTimeFormatted(){
         return formatTime(TOTAL_time - timeLeftInSeconds);
    }

}
