package be.warrox.myIsland;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldType;

public class CreateMyIsland {
    private final MyIsland plugin;

    public CreateMyIsland(MyIsland plugin) {
        this.plugin = plugin;
    }

    public void createIsland(String playerName) {
        String worldName = "island_" + playerName;

        // Haal de WorldManager op via de Multiverse-Core API
        MVWorldManager worldManager = plugin.getMultiverseCore().getMVWorldManager();

        // Controleer of de wereld al bestaat in Multiverse
        if (worldManager.isMVWorld(worldName)) {
            plugin.getLogger().info("Wereld " + worldName + " bestaat al.");
            return;
        }

        // Wereld aanmaken: Naam, Omgeving, Seed, Type, Structures, Generator
        // We gebruiken "CleanroomGenerator" of een lege string voor een void world,
        // maar FLAT met de juiste instellingen werkt ook.
        boolean success = worldManager.addWorld(
                worldName,
                World.Environment.NORMAL,
                null, // Random seed
                WorldType.FLAT,
                false, // Geen dorpen/structures
                null   // Standaard generator
        );

        if (success) {
            plugin.getLogger().info("Eiland succesvol aangemaakt voor " + playerName);

            // Wacht even tot de wereld geladen is en bouw dan het 16x16 platform
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    generatePlatform(world);
                }
            }, 20L); // 1 seconde vertraging
        } else {
            plugin.getLogger().severe("Aanmaken van eiland mislukt voor " + playerName);
        }
    }

    private void generatePlatform(World world) {
        // Maak een 16x16 (1 chunk) platform op y=64
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                world.getBlockAt(x, 64, z).setType(Material.GRASS_BLOCK);
            }
        }
        // Zet de spawn in het midden van het platform
        world.setSpawnLocation(8, 65, 8);
    }
}

