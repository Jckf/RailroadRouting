package it.flaten.railroadrouting;

import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventListener implements Listener {
    private RailroadRouting plugin;

    private Map<UUID, Location> locations = new HashMap<>();

    public EventListener(RailroadRouting plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        Vehicle vehicle = event.getVehicle();

        if (!(vehicle instanceof Minecart))
            return;

        Minecart minecart = (Minecart) vehicle;

        Entity passenger = event.getEntered();

        if (!(passenger instanceof Player))
            return;

        Player player = (Player) passenger;

        this.locations.put(passenger.getUniqueId(), vehicle.getLocation());

        if (this.plugin.inDeparture(minecart))
            this.plugin.handleDeparture(player, minecart);
    }

    @EventHandler
    public void onVehicleUpdate(VehicleUpdateEvent event) {
        Vehicle vehicle = event.getVehicle();

        if (!(vehicle instanceof Minecart))
            return;

        Minecart minecart = (Minecart) vehicle;

        if (vehicle.isEmpty())
            return;

        Entity passenger = vehicle.getPassenger();

        if (!(passenger instanceof Player))
            return;

        Player player = (Player) passenger;

        if (this.locations.containsKey(passenger.getUniqueId()) && this.locations.get(passenger.getUniqueId()).getBlock().equals(vehicle.getLocation().getBlock()))
            return;

        this.locations.put(passenger.getUniqueId(), vehicle.getLocation());

        if (this.plugin.inDeparture(minecart)) {
            this.plugin.handleDeparture(player, minecart);

            return;
        }

        if (this.plugin.inJunction(minecart)) {
            this.plugin.handleJunction(player, minecart);

            return;
        }

        if (this.plugin.inArrival(minecart)) {
            this.plugin.handleArrival(player, minecart);

            return;
        }
    }

    @EventHandler
    public void onVehicleEntityCollision(VehicleEntityCollisionEvent event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof Minecart))
            return;

        Minecart minecart = (Minecart) entity;

        if (minecart.isEmpty())
            return;

        event.setCancelled(true);
        event.setCollisionCancelled(true);
        event.setPickupCancelled(true);
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        Vehicle vehicle = event.getVehicle();

        if (!(vehicle instanceof Minecart))
            return;

        Minecart minecart = (Minecart) vehicle;

        LivingEntity passenger = event.getExited();

        if (!(passenger instanceof Player))
            return;

        Player player = (Player) passenger;

        this.plugin.handleExit(player, minecart);

        this.locations.remove(passenger.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (!player.isInsideVehicle())
            return;

        Entity entity = player.getVehicle();

        if (!(entity instanceof Minecart))
            return;

        Minecart minecart = (Minecart) entity;

        this.plugin.handleExit(player, minecart);
    }
}
