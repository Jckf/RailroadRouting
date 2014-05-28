package it.flaten.railroadrouting;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

public class RailroadRouting extends JavaPlugin {
    private List<BlockFace> poi = new ArrayList<>();
    private Map<UUID, String> targets = new HashMap<>();
    private List<UUID> teleports = new ArrayList<>();
    private EventListener eventListener;

    @Override
    public void onEnable() {
        this.poi.add(BlockFace.NORTH);
        this.poi.add(BlockFace.SOUTH);
        this.poi.add(BlockFace.EAST);
        this.poi.add(BlockFace.WEST);

        this.eventListener = new EventListener(this);

        this.getServer().getPluginManager().registerEvents(this.eventListener, this);
    }

    public boolean inDeparture(Minecart minecart) {
        BlockState upBlock = minecart.getLocation().getBlock().getRelative(BlockFace.UP).getState();

        if (!(upBlock instanceof Sign))
            return false;

        Sign sign = (Sign) upBlock;

        if (!sign.getLine(0).equals("") || !sign.getLine(1).equalsIgnoreCase("Departure") || !sign.getLine(3).equalsIgnoreCase(""))
            return false;

        if (!sign.getLine(2).equals(""))
            return true;

        return false;
    }

    public boolean inJunction(Minecart minecart) {
        Location location = minecart.getLocation();

        int directions = 0;

        for (BlockFace face : this.poi) {
            if (location.getBlock().getRelative(face).getType() == Material.RAILS)
                directions++;
        }

        if (directions > 2)
            return true;

        return false;
    }

    public boolean inArrival(Minecart minecart) {
        BlockState upBlock = minecart.getLocation().getBlock().getRelative(BlockFace.UP).getState();

        if (!(upBlock instanceof Sign))
            return false;

        Sign sign = (Sign) upBlock;

        if (!sign.getLine(0).equals("") || !sign.getLine(1).equalsIgnoreCase("Arrival") || !sign.getLine(3).equalsIgnoreCase(""))
            return false;

        if (!sign.getLine(2).equals(""))
            return true;

        return false;
    }

    public void handleDeparture(Player player, Minecart minecart) {
        this.targets.put(
            player.getUniqueId(),
            ((Sign) minecart.getLocation().getBlock().getRelative(BlockFace.UP).getState()).getLine(2)
        );

        player.sendMessage(ChatColor.GREEN + "You are now heading to " + this.targets.get(player.getUniqueId()));
    }

    public void handleJunction(Player player, Minecart minecart) {
        if (!this.targets.containsKey(minecart.getPassenger().getUniqueId()))
            return;

        BlockFace target = null;
        for (BlockFace face : this.poi) {
            BlockState blockState = minecart.getLocation().getBlock().getRelative(BlockFace.UP).getRelative(face).getState();

            if (!(blockState instanceof Sign))
                continue;

            List<String> stations = Arrays.asList(StringUtils.join(((Sign) blockState).getLines(), ",").split(","));

            if (stations.contains(this.targets.get(minecart.getPassenger().getUniqueId()))) {
                target = face;

                break;
            }
        }

        if (target == null)
            return;

        this.teleports.add(player.getUniqueId());

        minecart.eject();
        minecart.teleport(minecart.getLocation().getBlock().getRelative(target).getLocation());
        minecart.setPassenger(player);
        minecart.setVelocity(new Vector(target.getModX(), target.getModY(), target.getModZ()));

        this.teleports.remove(player.getUniqueId());
    }

    public void handleArrival(Player player, Minecart minecart) {
        player.sendMessage(ChatColor.GREEN + "You have arrived at " + ((Sign) minecart.getLocation().getBlock().getRelative(BlockFace.UP).getState()).getLine(2));

        this.targets.remove(player.getUniqueId());

        Location end = minecart.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);

        minecart.eject();
        player.teleport(end);
        minecart.getWorld().dropItem(end, new ItemStack(Material.MINECART, 1));
        minecart.remove();
    }

    public void handleExit(Player player, Minecart minecart) {
        if (!this.targets.containsKey(player.getUniqueId()))
            return;

        if (this.teleports.contains(player.getUniqueId()))
            return;

        player.sendMessage(ChatColor.RED + "You cancelled your trip to " + this.targets.get(player.getUniqueId()));

        this.targets.remove(player.getUniqueId());
    }
}
