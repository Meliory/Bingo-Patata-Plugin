package org;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BingoCommands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usa: /bingo <team|timer|card|start|stop|pause|resume|points|load-worlds|reload>");
            return true;
        }

        if(args[0].equalsIgnoreCase("team")) {
            return handleTeamCommand(sender, args);
        }

        if(args[0].equalsIgnoreCase("timer")) {
            return handleTimerCommand(sender, args);
        }

        if(args[0].equalsIgnoreCase("card")) {
            return handleCardCommand(sender, args);
        }

        if(args[0].equalsIgnoreCase("reset")) {
            return handleResetCommand(sender, args);
        }

        if(args[0].equalsIgnoreCase("start")) {
            return handleStartCommand(sender, args);
        }

        if(args[0].equalsIgnoreCase("stop")) {
            return handleStopCommand(sender, args);
        }

        if(args[0].equalsIgnoreCase("pause")) {
            return handlePauseCommand(sender, args);
        }

        if(args[0].equalsIgnoreCase("resume")) {
            return handleResumeCommand(sender, args);
        }

        if(args[0].equalsIgnoreCase("points")) {
            return handlePointsCommand(sender, args);
        }

        if(args[0].equalsIgnoreCase("load-worlds")) {
            BingoWorldManager.forceLoadAllTeamWorlds();
            sender.sendMessage(ChatColor.GREEN + "Todos los mundos han sido cargados");
            return true;
        }

        if(args[0].equalsIgnoreCase("reload")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando");
                return true;
            }
            BingoConfig.reloadConfig();
            MessageManager.loadMessages();
            BingoScoreboard.reloadAllScoreboards(); // Actualizar títulos y contenido de los scoreboards
            sender.sendMessage(ChatColor.GREEN + "Configuración recargada correctamente");
            sender.sendMessage(ChatColor.YELLOW + "Modo compartir mundos: " + BingoConfig.isShareWorld());
            return true;
        }

        return true;
    }

    public boolean handleTeamCommand(CommandSender sender, String[] args) {
        if (args.length < 2){
            sender.sendMessage("Usa: /bingo team <ARGS>");
            return true;
        }

        return switch (args[1].toLowerCase()) {
            case "create" -> createTeam(sender, args);
            case "erase" -> eraseTeam(sender, args);
            case "add_player" -> addPlayerToTeam(sender, args);
            case "remove_player" -> removePlayerFromTeam(sender, args);
            case "change_name" -> changeNameTeam(sender, args);
            case "change_color" -> changeColorTeam(sender, args);
            case "list" -> listTeam(sender);
            case "info" -> infoTeam(sender, args);
            default -> {
                sender.sendMessage("Subcomando desconocido");
                yield true;
            }
        };
    }

    public boolean handleTimerCommand(CommandSender sender, String[] args) {
        if (args.length < 2){
            sender.sendMessage("Usa: /bingo timer <start|stop|resume|set>");
            return true;
        }

        return switch (args[1].toLowerCase()){
            case "start" -> startTimer(sender, args);
            case "stop" -> stopTimer(sender, args);
            case "resume" -> resumeTimer(sender, args);
            case "set" -> setTimer(sender, args);
            default -> {
                sender.sendMessage("Subcomando desconocido");
                yield true;
            }
        };
    }

    public boolean handlePauseCommand(CommandSender sender, String[] args) {
        if(!BingoTimer.isRunning()) {
            sender.sendMessage(ChatColor.RED + "No hay ninguna partida en curso");
            return true;
        }

        if(BingoTimer.isPaused()) {
            sender.sendMessage(ChatColor.YELLOW + "La partida ya está en pausa");
            return true;
        }

        BingoTimer.pauseTimer();
        sender.sendMessage(ChatColor.YELLOW + "✔ Partida pausada");
        return true;
    }

    public boolean handleResumeCommand(CommandSender sender, String[] args) {
        if(!BingoTimer.isRunning()) {
            sender.sendMessage(ChatColor.RED + "No hay ninguna partida en curso");
            return true;
        }

        if(!BingoTimer.isPaused()) {
            sender.sendMessage(ChatColor.YELLOW + "La partida no está en pausa");
            return true;
        }

        BingoTimer.unpauseTimer();
        sender.sendMessage(ChatColor.GREEN + "✔ Partida reanudada");
        return true;
    }

    /*public boolean handlePlayerCommand(CommandSender sender, String[] args) {
        if (args.length < 2){
            sender.sendMessage("Usa: /bingo player <add|remove>");
            return true;
        }

        return switch (args[1].toLowerCase()) {
            case "add" -> addPlayerToTeam(sender, args);
            case "remove" -> removePlayerFromTeam(sender, args);
            default -> {
                sender.sendMessage("Subcomando desconocido");
                yield true;
            }
        };
    }*/

    public boolean handleCardCommand(CommandSender sender, String[] args) {
        if (args.length < 2){
            sender.sendMessage("Usa: /bingo card <ARGS>");
            return true;
        }

        return switch (args[1].toLowerCase()){
            case "show" -> showCardPlayer(sender, args);
            case "show_everyone" -> showCardPlayers(sender, args);
            case "restart" -> restartCardTeam(sender, args);
            case "give_item" -> giveItemTeam(sender, args);
            case "remove_item" -> removeItemTeam(sender, args);
            default -> {
                sender.sendMessage("Subcomando desconocido");
                yield true;
            }
        };
    }

    public boolean handleStartCommand(CommandSender sender, String[] args) {
        if(BingoTimer.isRunning()){
            sender.sendMessage("Hay una partida ya en marcha");
            return true;
        }

        // Resetear logs de items (borrar y crear nuevo)
        BingoLogger.resetLog();

        if(!BingoWorldManager.areActiveWorldsLoaded()) {
            sender.sendMessage("Cargando mundos necesarios...");
            BingoWorldManager.loadAllActiveTeamWorlds();
        }

        // Configurar mundos
        BingoWorldManager.setupWorldConditions();

        // PASO 1: Aplicar efectos a todos los jugadores
        applyStartEffects();

        // PASO 2: Tp jugadores a sus mundos respectivos
        teleportPlayersToTeamWorlds();

        // PASO 3: Limpiar inventarios y configurar mundos
        clearPlayersAndSetupWorlds();

        // PASO 4-7: Cuenta atrás y inicio (con delays)
        startCountdownSequence();

        return true;
    }

    public boolean handleStopCommand(CommandSender sender, String[] args) {
        if (!BingoTimer.isRunning()) {
            sender.sendMessage(ChatColor.RED + "No hay ninguna partida en curso");
            return true;
        }

        // Detener timer
        BingoTimer.stopTimer();

        // Detener actualización periódica de scoreboards
        BingoScoreboard.stopPeriodicUpdate();

        // Broadcast
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ChatColor.RED + ChatColor.BOLD.toString() + "✖ PARTIDA DETENIDA");
            // Limpiar scoreboard del jugador
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }

        sender.sendMessage(ChatColor.YELLOW + "Partida detenida correctamente");
        return true;
    }

    public boolean handleResetCommand(CommandSender sender, String[] args) {
        return true;
    }

    public boolean handlePointsCommand(CommandSender sender, String[] args) {
        // Broadcast del encabezado para que todos lo vean
        Bukkit.broadcastMessage(ChatColor.GOLD + "=== RESULTADOS FINALES ===");

        List<Team> teams = TeamManager.getAllTeams();
        if(teams.isEmpty()) {
            Bukkit.broadcastMessage("No hay equipos");
            return true;
        }

        // Ordenar equipos por puntuación (mayor a menor)
        teams.sort((t1, t2) -> Integer.compare(
                BingoData.getTeamPoints(t2),
                BingoData.getTeamPoints(t1)
        ));

        // Mostrar resultados con posiciones
        for(int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            int points = BingoData.getTeamPoints(team);
            int itemsCompleted = BingoData.getTeamItems(team).size();
            int totalItems = BingoCard.getBingoItems().size();

            String position;
            position = (i + 1) + "°";

            Bukkit.broadcastMessage(position + " " + team.getColoredName() +
                    ChatColor.WHITE + ": " + ChatColor.GOLD + points + " puntos " +
                    ChatColor.GRAY + "(" + itemsCompleted + "/" + totalItems + " items)");
        }
        return true;
    }

    /*public boolean handleStopCommand(CommandSender sender, String[] args) {
        if(!BingoTimer.isRunning()) {
            sender.sendMessage(ChatColor.RED + "No hay ningún bingo en curso");
            return true;
        }

        BingoTimer.stopTimer();
        Bukkit.broadcastMessage(ChatColor.RED + "¡Bingo detenido por un administrador!");
        return true;
    }*/

    /* ---- SUBCOMANDOS TEAM ---- */

    private boolean createTeam(CommandSender sender, String[] args) {
        if (args.length < 4){
            sender.sendMessage("Usa: /bingo team create <name> <color>");
            return true;
        }

        String teamName = args[2];
        ChatColor teamColor = parseColor(args[3]);

        if(teamColor == null){
            sender.sendMessage("Introduce un color válido");
            return true;
        }

        Team team = TeamManager.createTeam(teamName, teamColor);
        if(team == null){
            sender.sendMessage("Ya existe un equipo con ese nombre");
            return true;
        }

        sender.sendMessage("Equipo " + team.getColoredName() + " creado!");
        return true;
    }

    private boolean eraseTeam(CommandSender sender, String[] args) {
        if (args.length < 3){
            sender.sendMessage("Usa: /bingo team erase <name>");
            return true;
        }

        String teamName = args[2];

        if(teamName == "*"){
            TeamManager.deleteAllTeams();
            sender.sendMessage("Todos los teams han sido eliminados");
            return true;
        }

        if(TeamManager.getTeamByName(teamName) != null){
            ChatColor teamColor = TeamManager.getTeamByName(teamName).getColor();
            TeamManager.deleteTeam(teamName);
            sender.sendMessage("Team " + teamColor + teamName + ChatColor.WHITE + " eliminado!");
        }
        return true;
    }

    private boolean addPlayerToTeam(CommandSender sender, String[] args) {
        if (args.length < 4){
            sender.sendMessage("Usa: /bingo team add <jugador> <equipo>");
            return true;
        }

        Player player = sender.getServer().getPlayer(args[2]);
        if(player == null){
            sender.sendMessage("Jugador no encontrado");
            return true;
        }

        String teamName = args[3];
        if(TeamManager.addPlayerToTeam(player, teamName)){
            Team team = TeamManager.getTeamByName(teamName);
            BingoScoreboard.showBingoCard(player);
            sender.sendMessage("Jugador " + player.getName() + " añadido al equipo " + team.getColoredName());
            player.sendMessage("¡Has sido añadido al equipo " + team.getColoredName() + "!");
        } else {
            sender.sendMessage("Equipo no encontrado");
        }
        return true;
    }

    private boolean removePlayerFromTeam(CommandSender sender, String[] args) {
        if (args.length < 3){
            sender.sendMessage("Usa: /bingo team remove <jugador>");
            return true;
        }

        Player player = sender.getServer().getPlayer(args[2]);
        if(player == null){
            sender.sendMessage("Jugador no encontrado");
            return true;
        }

        TeamManager.removePlayerFromAllTeams(player);
        BingoScoreboard.hideBingoCard(player);
        sender.sendMessage("Jugador " + player.getName() + " eliminado de todos los equipos.");
        return true;
    }

    private boolean changeNameTeam(CommandSender sender, String[] args) {
        if (args.length < 4){
            sender.sendMessage("Usa: /bingo team name <new_name>");
            return true;
        }

        String teamName = args[2];
        String newName = args[3];

        if(TeamManager.getTeamByName(teamName) == null){
            sender.sendMessage("Team no encontrado");
            return true;
        }

        TeamManager.changeTeamName(teamName, newName);
        sender.sendMessage("Se ha cambiado el nombre de " + teamName + " a " + newName + "!");
        return true;
    }

    private boolean changeColorTeam(CommandSender sender, String[] args) {
        if (args.length < 4){
            sender.sendMessage("Usa: /bingo team name <new_color>");
        }

        String teamName = args[2];
        ChatColor newColor = parseColor(args[3]);

        if(newColor == null){
            sender.sendMessage("Introduce un color válido");
            return true;
        }

        if(TeamManager.getTeamByName(teamName) == null){
            sender.sendMessage("Team no encontrado");
            return true;
        }

        TeamManager.changeTeamColor(teamName, newColor);
        return true;
    }

    private boolean listTeam(CommandSender sender) {
        List<Team> teams = TeamManager.getAllTeams();

        if(!teams.isEmpty()){
            for(Team team : teams){
                showInfoSingleTeam(sender, team);
            }
        } else {
            sender.sendMessage("No hay equipos disponibles");
        }
        return true;
    }

    private boolean infoTeam(CommandSender sender, String[] args){
        if(args.length < 3){
            sender.sendMessage("Usa: /bingo team info <team_name>");
            return true;
        }

        String teamName = args[2];
        Team team = TeamManager.getTeamByName(teamName);
        if(team == null){
            sender.sendMessage("Team no encontrado");
            return true;
        }

        showInfoSingleTeam(sender, team);
        return true;
    }

    private void showInfoSingleTeam(CommandSender sender, Team team){
        ChatColor teamColor = team.getColor();
        sender.sendMessage(teamColor + " -- " + team.getName() + " --");
        List<UUID> players = team.getPlayers();
        for(UUID playerUUID : players){
            Player player = Bukkit.getPlayer(playerUUID);
            if(player != null && player.isOnline()) {
                sender.sendMessage("  - " + player.getName());
            } else {
                sender.sendMessage("  - " + ChatColor.GRAY + "[Offline: " + playerUUID + "]");
            }
        }
    }

    /* ---- SUBCOMANDOS TIMER ---- */

    private boolean startTimer(CommandSender sender, String[] args){
        if(BingoTimer.isRunning()) {
            sender.sendMessage("El timer ya está en curso");
            return true;
        }

        BingoTimer.startTimer();
        return true;
    }

    private boolean stopTimer(CommandSender sender, String[] args){
        if(BingoTimer.isRunning()) {
            BingoTimer.stopTimer();
            sender.sendMessage("Timer parado");
            return true;
        }

        sender.sendMessage("No se ha podido parar el timer, no está en ejecución");
        return true;
    }

    private boolean resumeTimer(CommandSender sender, String[] args){
        if(BingoTimer.isRunning()) {
            sender.sendMessage("El timer ya está en curso");
            return true;
        }

        BingoTimer.resumeTimer();
        sender.sendMessage("Timer vuelta en marcha");
        return true;
    }

    private boolean setTimer(CommandSender sender, String[] args){
        // Nuevo formato: /bingo timer set <horas> <minutos> <segundos>
        if(args.length < 5){
            sender.sendMessage(ChatColor.RED + "Usa: /bingo timer set <horas> <minutos> <segundos>");
            sender.sendMessage(ChatColor.YELLOW + "Ejemplo: /bingo timer set 1 30 0 (1 hora 30 minutos)");
            return true;
        }

        try {
            int hours = Integer.parseInt(args[2]);
            int minutes = Integer.parseInt(args[3]);
            int seconds = Integer.parseInt(args[4]);

            if(hours < 0 || minutes < 0 || seconds < 0 || minutes >= 60 || seconds >= 60){
                sender.sendMessage(ChatColor.RED + "Valores inválidos. Minutos y segundos deben ser 0-59");
                return true;
            }

            // Convertir todo a segundos
            int totalSeconds = (hours * 3600) + (minutes * 60) + seconds;

            if(totalSeconds <= 0){
                sender.sendMessage(ChatColor.RED + "El tiempo total debe ser mayor a 0");
                return true;
            }

            BingoTimer.setTimer(totalSeconds);

            String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            sender.sendMessage(ChatColor.GREEN + "✔ Timer establecido en: " + ChatColor.WHITE + timeString);

        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Error: Debes introducir números válidos");
            return true;
        }

        return true;
    }

    /* ---- SUBCOMANDOS CARTA ---- */

    private boolean showCardPlayer(CommandSender sender, String[] args){
        if(!(sender instanceof Player player)) {
            sender.sendMessage("Solo los jugadores pueden usar este comando");
            return true;
        }

        String option = args[2];

        if(option.equalsIgnoreCase("on")){
            BingoScoreboard.showBingoCard(player);
        } else if(option.equalsIgnoreCase("off")){
            BingoScoreboard.hideBingoCard(player);
        } else {
            sender.sendMessage("Opción no valida");
        }
        return true;
    }

    private boolean showCardPlayers(CommandSender sender, String[] args){
        if(!(sender instanceof Player player)) {
            sender.sendMessage("Solo los jugadores pueden usar este comando");
            return true;
        }

        String option = args[2];

        if(option.equalsIgnoreCase("on")){
            BingoScoreboard.showAllTeamCards();
        } else if(option.equalsIgnoreCase("off")){
            BingoScoreboard.hideAllTeamCards();
        } else {
            sender.sendMessage("Opción no valida");
        }
        return true;
    }

    private boolean restartCardTeam(CommandSender sender, String[] args){
        if(args.length < 3){
            sender.sendMessage("Usa: /bingo card restart <team_name>");
            return true;
        }

        String teamName = args[2];
        Team team = TeamManager.getTeamByName(teamName);
        BingoData.resetTeamItems(team);
        sender.sendMessage("Se han eliminado todos los objetos del equipo: " + team.getColoredName());
        return true;
    }

    private boolean giveItemTeam(CommandSender sender, String[] args){
        if(args.length < 4){
            sender.sendMessage("Usa: /bingo card give_item <team_name> <item_name>");
            return true;
        }

        String teamName = args[2];
        Team team = TeamManager.getTeamByName(teamName);

        if(team == null){
            sender.sendMessage("No se ha encontrado el team");
            return true;
        }

        try {
            Material item = Material.valueOf(args[3].toUpperCase());

            if(!BingoCard.isItemOnBingo(item)){
                sender.sendMessage("El item no está dentro de la carta actual");
                return true;
            }

            BingoProcess.processItemTeam(team, item);
            sender.sendMessage("Se ha dado el item");

        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Material no válido: " + args[3]);
            Bukkit.getLogger().severe("[BingoCommands] Error en giveItemTeam - Material inválido: " + args[3]);
        }

        return true;
    }

    private boolean removeItemTeam(CommandSender sender, String[] args){
        if(args.length < 4){
            sender.sendMessage("Usa: /bingo card remove_item <team_name> <item_name>");
            return true;
        }

        String teamName = args[2];
        Team team = TeamManager.getTeamByName(teamName);

        if(team == null){
            sender.sendMessage("No se ha encontrado el team");
            return true;
        }

        try {
            Material item = Material.valueOf(args[3].toUpperCase());

            if(!BingoCard.isItemOnBingo(item)){
                sender.sendMessage("El item no está dentro de la carta actual");
                return true;
            }

            BingoProcess.removeItemTeam(team, item);
            sender.sendMessage("Se ha quitado el item");

        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Material no válido: " + args[3]);
            Bukkit.getLogger().severe("[BingoCommands] Error en removeItemTeam - Material inválido: " + args[3]);
        }

        return true;
    }

    /* ---- SUBFUNCIONES START ---- */

    private static void applyStartEffects() {
        for(Team team : TeamManager.getAllTeams()) {
            for(UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if(player != null && player.isOnline()) {
                    // Slowness V (no se pueden mover)
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            PotionEffectType.SLOWNESS,
                            Integer.MAX_VALUE,
                            255,
                            false,
                            false
                    ));

                    // Darkness/Blindness (no pueden ver)
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            PotionEffectType.BLINDNESS,
                            Integer.MAX_VALUE,
                            1,
                            false,
                            false
                    ));

                    player.setGameMode(GameMode.SURVIVAL);
                }
            }
        }
    }

    private void teleportPlayersToTeamWorlds() {
        for(Team team : TeamManager.getAllTeams()) {
            for(UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if(player != null && player.isOnline()) {
                    BingoWorldManager.teleportPlayerToTeamSpawn(player, team.getID());
                }
            }
        }
    }

    private void clearPlayersAndSetupWorlds() {
        // Limpiar inventarios de todos los jugadores
        for(Team team : TeamManager.getAllTeams()) {
            for(UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if(player != null && player.isOnline()) {
                    player.getInventory().clear();
                    player.setHealth(20.0); // Vida completa
                    player.setFoodLevel(20); // Hambre completa
                    player.setSaturation(20.0f); // Saturación completa
                }
            }
        }

        // Configurar condiciones de todos los mundos
        BingoWorldManager.setupWorldConditions();
    }

    private void startCountdownSequence() {
        new org.bukkit.scheduler.BukkitRunnable() {
            int countdown = 3;

            @Override
            public void run() {
                if(countdown > 0) {
                    // Mostrar números de cuenta atrás
                    showCountdownTitle(String.valueOf(countdown));
                    countdown--;
                } else {
                    // Mostrar "¡YA!" y finalizar secuencia
                    showCountdownTitle("¡YA!");

                    finishStartSequence();

                    this.cancel(); // Cancelar este runnable
                }
            }
        }.runTaskTimer(BingoPatataPlugin.getInstance(), 40L, 30L); // Cada 2 segundos (40 ticks)
    }

    private void showCountdownTitle(String text) {
        ChatColor color = text.equals("¡YA!") ? ChatColor.GREEN : ChatColor.GOLD;

        for(Team team : TeamManager.getAllTeams()) {
            for(UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if(player != null && player.isOnline()) {
                    player.sendTitle(
                            color + "" + ChatColor.BOLD + text,
                            " ",
                            10, // fadeIn (0.5s)
                            30, // stay (1.5s)
                            10  // fadeOut (0.5s)
                    );
                }
            }
        }
    }

    private void finishStartSequence() {
        // PASO 6: Quitar todos los efectos de los jugadores
        for(Team team : TeamManager.getAllTeams()) {
            for(UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if(player != null && player.isOnline()) {
                    // Quitar todos los efectos de poción
                    for(org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
                        player.removePotionEffect(effect.getType());
                    }

                    // PASO 5: Sonido de inicio
                    player.playSound(
                            player.getLocation(),
                            org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,
                            1.0f,
                            1.0f
                    );

                    // Mostrar carta automáticamente
                    BingoScoreboard.showBingoCard(player);
                }
            }
        }

        // PASO 7: Iniciar el timer
        BingoTimer.startTimer();

        // Iniciar actualización periódica de scoreboards
        BingoScoreboard.startPeriodicUpdate();
    }

    /* ---- SUBFUNCIONES END ---- */

    public static void endGame(){
        // Detener actualización periódica de scoreboards
        BingoScoreboard.stopPeriodicUpdate();

        applyStartEffects();

        World world = org.bukkit.Bukkit.getWorlds().get(0);

        for(Team team : TeamManager.getAllTeams()) {
            for(UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if(player != null && player.isOnline()) {
                    player.getInventory().clear();
                    player.setHealth(20.0); // Vida completa
                    player.setFoodLevel(20); // Hambre completa
                    player.setSaturation(20.0f); // Saturación completa
                    player.setGameMode(GameMode.ADVENTURE);
                    BingoScoreboard.hideBingoCard(player);

                    org.bukkit.Location spawnLocation = new org.bukkit.Location(world, 0, world.getHighestBlockYAt(0, 0) + 2, 0);
                    spawnLocation.setYaw(0);
                    spawnLocation.setPitch(0);

                    player.teleport(spawnLocation);
                }
            }
        }

        for(Team team : TeamManager.getAllTeams()) {
            for(UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if(player != null && player.isOnline()) {
                    for(org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
                        player.removePotionEffect(effect.getType());
                    }
                }
            }
        }
    }

    private ChatColor parseColor(String color){
        return switch (color.toLowerCase()) {
            case "black" -> ChatColor.BLACK;
            case "dark_blue" -> ChatColor.DARK_BLUE;
            case "dark_green" -> ChatColor.DARK_GREEN;
            case "dark_aqua", "dark_cyan" -> ChatColor.DARK_AQUA;
            case "dark_red" -> ChatColor.DARK_RED;
            case "dark_purple", "purple" -> ChatColor.DARK_PURPLE;
            case "gold", "orange" -> ChatColor.GOLD;
            case "gray", "grey" -> ChatColor.GRAY;
            case "dark_gray", "dark_grey" -> ChatColor.DARK_GRAY;
            case "blue" -> ChatColor.BLUE;
            case "green" -> ChatColor.GREEN;
            case "aqua", "cyan" -> ChatColor.AQUA;
            case "red" -> ChatColor.RED;
            case "light_purple", "pink" -> ChatColor.LIGHT_PURPLE;
            case "yellow" -> ChatColor.YELLOW;
            case "white" -> ChatColor.WHITE;
            default -> ChatColor.WHITE;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Solo jugadores OP pueden usar la mayoría de comandos (excepto card show)
        boolean isOp = sender.isOp();
        //boolean isPlayerCardShow = (args.length >= 2 && args[0].equalsIgnoreCase("card") && args[1].equalsIgnoreCase("show"));

        if (!isOp/* && !isPlayerCardShow*/) {
            if(args.length == 1) {
                List<String> mainCommands = Arrays.asList("card");
                completions.addAll(filterCompletions(mainCommands, args[0]));
            } else if(args[0].toLowerCase().equals("card") && args.length == 2) {
                List<String> mainCommands = Arrays.asList("show");
                completions.addAll(filterCompletions(mainCommands, args[1]));
            } else if(args[1].toLowerCase().equals("show") && args.length == 3){
                List<String> mainCommands = Arrays.asList("on", "off");
                completions.addAll(filterCompletions(mainCommands, args[2]));
            }
            return completions; // Solo devolver vacío si no es OP y no es card show
        }

        // NIVEL 1: Comandos principales
        if (args.length == 1) {
            List<String> mainCommands = Arrays.asList("team", "timer", "card", "reset", "start", "stop", "pause", "resume", "points", "load-worlds", "reload");
            completions.addAll(filterCompletions(mainCommands, args[0]));
        }

        // NIVEL 2: Subcomandos
        else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "team":
                    if (isOp) {
                        List<String> teamCommands = Arrays.asList("create", "erase", "add_player", "remove_player",
                                "change_name", "change_color", "list", "info");
                        completions.addAll(filterCompletions(teamCommands, args[1]));
                    }
                    break;

                case "timer":
                    if (isOp) {
                        List<String> timerCommands = Arrays.asList("start", "stop", "resume", "set");
                        completions.addAll(filterCompletions(timerCommands, args[1]));
                    }
                    break;

                case "card":
                    List<String> cardCommands = Arrays.asList("show", "show_everyone", "restart", "give_item", "remove_item");
                    if (!isOp) {
                        // Solo jugadores no-OP pueden usar "show"
                        cardCommands = Arrays.asList("show");
                    }
                    completions.addAll(filterCompletions(cardCommands, args[1]));
                    break;
            }
        }

        // NIVEL 3: Argumentos específicos
        else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "team":
                    if (!isOp) break;

                    switch (args[1].toLowerCase()) {
                        case "erase":
                            // Nombres de equipos + "*" para todos
                            completions.addAll(getTeamNamesFiltered(args[2]));
                            completions.addAll(filterCompletions(Arrays.asList("*"), args[2]));
                            break;

                        case "add_player":
                        case "remove_player":
                            // Jugadores online
                            completions.addAll(getOnlinePlayersFiltered(args[2]));
                            break;

                        case "change_name":
                        case "change_color":
                        case "info":
                            // Nombres de equipos
                            completions.addAll(getTeamNamesFiltered(args[2]));
                            break;

                        case "create":
                            // Cualquier nombre (no podemos predecir)
                            break;
                    }
                    break;

                case "timer":
                    if (!isOp) break;

                    if (args[1].equalsIgnoreCase("set")) {
                        // Sugerencias de tiempo común
                        List<String> timeOptions = Arrays.asList("3600", "7200", "10800"); // 1h, 2h, 3h
                        completions.addAll(filterCompletions(timeOptions, args[2]));
                    }
                    break;

                case "card":
                    switch (args[1].toLowerCase()) {
                        case "show":
                            // on/off - todos los jugadores pueden usar esto
                            List<String> showOptions = Arrays.asList("on", "off");
                            completions.addAll(filterCompletions(showOptions, args[2]));
                            break;

                        case "show_everyone":
                            if (isOp) {
                                List<String> showEveryoneOptions = Arrays.asList("on", "off");
                                completions.addAll(filterCompletions(showEveryoneOptions, args[2]));
                            }
                            break;

                        case "restart":
                        case "give_item":
                        case "remove_item":
                            if (isOp) {
                                // Nombres de equipos
                                completions.addAll(getTeamNamesFiltered(args[2]));
                            }
                            break;
                    }
                    break;
            }
        }

        // NIVEL 4: Argumentos adicionales
        else if (args.length == 4) {
            if (!isOp) return completions; // Solo OP para nivel 4 (excepto casos especiales)

            switch (args[0].toLowerCase()) {
                case "team":
                    switch (args[1].toLowerCase()) {
                        case "create":
                        case "change_color":
                            // Colores disponibles
                            completions.addAll(getColorsFiltered(args[3]));
                            break;

                        case "add_player":
                            // Nombres de equipos (después de seleccionar jugador)
                            completions.addAll(getTeamNamesFiltered(args[3]));
                            break;

                        case "change_name":
                            // Nuevo nombre (no podemos predecir)
                            break;
                    }
                    break;

                case "card":
                    switch (args[1].toLowerCase()) {
                        case "give_item":
                            // Items de la carta de bingo
                            completions.addAll(getBingoItemsFiltered(args[3]));
                            break;

                        case "remove_item":
                            // Items que tiene ese equipo específico
                            String teamName = args[2];
                            completions.addAll(getTeamItemsFiltered(teamName, args[3]));
                            break;
                    }
                    break;
            }
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> options, String input) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>(options);
        }

        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<String> getTeamNamesFiltered(String input) {
        List<String> teamNames = TeamManager.getAllTeams().stream()
                .map(Team::getName)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        return filterCompletions(teamNames, input);
    }

    private List<String> getOnlinePlayersFiltered(String input) {
        List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        return filterCompletions(playerNames, input);
    }

    private List<String> getColorsFiltered(String input) {
        List<String> colors = Arrays.asList("red", "green", "yellow", "blue", "purple", "orange", "aqua", "white");
        return filterCompletions(colors, input);
    }

    private List<String> getBingoItemsFiltered(String input) {
        List<String> itemNames = BingoCard.getBingoItems().stream()
                .map(material -> material.name().toLowerCase())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        return filterCompletions(itemNames, input);
    }

    private List<String> getTeamItemsFiltered(String teamName, String input) {
        Team team = TeamManager.getTeamByName(teamName);
        if (team == null) {
            return new ArrayList<>();
        }

        Set<Material> teamItems = BingoData.getTeamItems(team);
        List<String> itemNames = teamItems.stream()
                .map(material -> material.name().toLowerCase())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        return filterCompletions(itemNames, input);
    }
}