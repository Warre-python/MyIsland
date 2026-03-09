package be.warrox.myIsland;

import com.onarandombox.multiversecore.api.MVWorldManager;
import com.onarandombox.multiversecore.api.world.WorldCreationSettings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;

public class CreateMyIsland {
    private final MyIsland plugin;

    public CreateMyIsland(MyIsland plugin) {
        this.plugin = plugin;
    }

    public void createIsland(Player player) {
        String playerName = player.getName();
        String worldName = "island_" + playerName;

        // Haal de WorldManager op via de Multiverse-Core API
        MVWorldManager worldManager = plugin.getMultiverseCore().getMVWorldManager();

        // Controleer of de wereld al bestaat in Multiverse
        if (worldManager.hasWorld(worldName)) {
            player.sendMessage("§cJe hebt al een eiland!");
            return;
        }

        // Maak de instellingen voor de nieuwe wereld
        WorldCreationSettings settings = new WorldCreationSettings(worldName)
                .env(World.Environment.NORMAL)
                .type(WorldType.FLAT)
                .generateStructures(false)
                .generator("minecraft:air;minecraft:plains"); // Zorgt voor een lege wereld

        // Maak de wereld asynchroon aan
        worldManager.createWorld(settings).thenAccept(success -> {
            if (success) {
                plugin.getLogger().info("Eiland succesvol aangemaakt voor " + playerName);
                player.sendMessage("§aEiland wordt voorbereid...");
                // Wacht even tot de wereld geladen is en bouw dan het platform
                Bukkit.getScheduler().runTask(plugin, () -> {
                    generatePlatform(worldManager.getMVWorld(worldName).getBukkitWorld());
                    player.sendMessage("§aJe eiland is klaar! Gebruik /myi tpisland om ernaartoe te gaan.");
                });
            } else {
                plugin.getLogger().severe("Aanmaken van eiland mislukt voor " + playerName);
                player.sendMessage("§cEr is iets misgegaan bij het aanmaken van je eiland.");
            }
        });
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
