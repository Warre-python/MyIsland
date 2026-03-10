package be.warrox.myIsland;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerLocationManager {

    private final Map<UUID, Location> previousLocations = new HashMap<>();

    public void saveLocation(Player player) {
        previousLocations.put(player.getUniqueId(), player.getLocation());
    }

    public Location getPreviousLocation(Player player) {
        return previousLocations.remove(player.getUniqueId());
    }
}
