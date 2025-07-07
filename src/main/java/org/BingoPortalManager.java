package org;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.PortalCreateEvent;

public class BingoPortalManager implements Listener {

    //PARA LAS DIFERENTES DIMENSIONES
    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        Team team = TeamManager.getplayerTeam(player);

        if (team == null) return;

        World fromWorld = event.getFrom().getWorld();
        int ID = team.getID();
        Location location = event.getFrom();

        //1. Overworld -> Nether
        if (fromWorld.getName().equals("overworldteam" + ID)) {
            World netherWorld = Bukkit.getWorld("netherteam" + ID);
            if (netherWorld != null) {

                //Calculamos coords nether
                double netherX = location.getX() / 8.0;
                double netherZ = location.getZ() / 8.0;
                double netherY = Math.max(32, Math.min(120, location.getY())); //Y segura en el nether

                Location netherLocation = new Location(netherWorld, netherX, netherY, netherZ);

                //Buscar o crear portal en el destino
                Location portalLocation = findOrCreatePortal(netherLocation, true);

                event.setTo(portalLocation);
            }
        }

        //2. Nether -> Overworld
        else if (fromWorld.getName().equals("netherteam" + ID)) {
            World overworldWorld = Bukkit.getWorld("overworldteam" + ID);
            if (overworldWorld != null) {

                //Calculamos coords over
                double overworldX = location.getX() * 8.0;
                double overworldZ = location.getZ() * 8.0;
                double overworldY = overworldWorld.getHighestBlockYAt((int) overworldX, (int) overworldZ + 1);

                Location overworldLocation = new Location(overworldWorld, overworldX, overworldY, overworldZ);

                Location portalLocation = findOrCreatePortal(overworldLocation, false);

                event.setTo(portalLocation);
            }
        }
    }

    /**
     * Busca un portal cercano o crea uno nuevo
     * @param targetLocation Ubicación aproximada donde debe estar el portal
     * @param isNether Si estamos yendo al nether o al overworld
     * @return Ubicación exacta del portal
     */
    private Location findOrCreatePortal(Location targetLocation, boolean isNether){
        World world = targetLocation.getWorld();
        int searchRadius = 32; //Buscar portales en este radio

        //Buscar portales existentes cerca
        for(int x = -searchRadius; x <= searchRadius; x++){
            for(int y = -20; y <= 20; y++){
                for(int z = -searchRadius; z <= searchRadius; z++){
                    Location checkLoc = targetLocation.clone().add(x, y, z);
                    if(checkLoc.getBlock().getType() == Material.NETHER_PORTAL){
                        return findSafeLocationNearPortal(checkLoc);
                    }
                }
            }
        }

        return createNewPortal(targetLocation, isNether);
    }

    private Location createNewPortal(Location targetLocation, boolean isNether){
        World world = targetLocation.getWorld();

        //Ajustar Y para que sea segura
        int safeY;
        if(isNether){
            safeY = findSafeNetherHeight(targetLocation);
        } else {
            safeY = world.getHighestBlockYAt(targetLocation) + 1;
        }

        Location portalBase = new Location(world, targetLocation.getBlockX(), safeY, targetLocation.getBlockZ());

        /*createObsidianPlatform(portalBase);*/

        createPortalFrame(portalBase);

        activatePortal(portalBase);

        return portalBase.clone().add(1.5,1,0.5);
    }

    private int findSafeNetherHeight(Location targetLocation){
        World world = targetLocation.getWorld();
        int x = targetLocation.getBlockX();
        int z = targetLocation.getBlockZ();

        //Buscar desde Y=32 hacia arriba un lugar seguro: (Aire + aire + suelo sólido)
        for(int y= 32; y < 120; y++){
            if(world.getBlockAt(x, y-1, z).getType().isSolid() &&
                world.getBlockAt(x, y, z).getType().isAir() &&
                world.getBlockAt(x, y+1, z).getType().isAir() &&
                world.getBlockAt(x, y+2, z).getType().isAir()){
                    return y;
            }
        }
        return 64;
    }

    private void createObsidianPlatform(Location portalBase){
        //Crear plataforma 5x5 de obsidiana
        for(int x = -2; x <= 2; x++){
            for(int z = -2; z <= 2; z++){
                portalBase.clone().add(x, -1, z).getBlock().setType(Material.OBSIDIAN);
            }
        }
    }

    private void createPortalFrame(Location portalBase){
        //Marco de portal 4x5 de obsidiana

        //Base
        for(int x = 0; x <= 3; x++){
            portalBase.clone().add(x, 0, 0).getBlock().setType(Material.OBSIDIAN);
            portalBase.clone().add(x, 4, 0).getBlock().setType(Material.OBSIDIAN);
        }

        //Lados
        for(int y = 0; y <= 4; y++) {
            portalBase.clone().add(0, y, 0).getBlock().setType(Material.OBSIDIAN);
            portalBase.clone().add(3, y, 0).getBlock().setType(Material.OBSIDIAN);
        }

        // Limpiar el interior
        for(int x = 1; x <= 2; x++) {
            for(int y = 1; y <= 3; y++) {
                portalBase.clone().add(x, y, 0).getBlock().setType(Material.AIR);
            }
        }
    }

    private void activatePortal(Location portalBase){
        // Activar el portal poniendo bloques de portal en el interior
        for(int x = 1; x <= 2; x++) {
            for(int y = 1; y <= 3; y++) {
                portalBase.clone().add(x, y, 0).getBlock().setType(Material.NETHER_PORTAL);
            }
        }
    }

    private Location findSafeLocationNearPortal(Location location){
        //Buscar una ubicación segura carca del portal encontrado
        for(int x = -2; x <= 2; x++){
            for(int z = -2; z <= 2; z++){
                for(int y = -1; y <= 1; y++){
                    Location testLoc = location.clone().add(x,y,z);
                    if(testLoc.getBlock().getType().isAir() && testLoc.clone().add(0, 1, 0).getBlock().getType().isAir()){
                        return testLoc.add(0.5, 0, 0.5);
                    }
                }
            }
        }
        return location.clone().add(0.5, 0, 0.5);
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        // Permitir creación de portales normalmente
        // Este evento se dispara cuando se crean portales con flint & steel
        // No necesitamos modificar nada aquí, solo asegurarnos de que funcione
    }

}
