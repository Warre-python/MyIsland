package be.warrox.myIsland;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;
import org.mvplugins.multiverse.core.world.reasons.CreateFailureReason;

public class CreateMyIsland {
    private final MyIsland plugin;

    public CreateMyIsland(MyIsland plugin) {
        this.plugin = plugin;
    }

    public void createIsland(Player player) {
        String playerName = player.getName();
        String worldName = "island_" + player.getName();

        MultiverseCoreApi api = MultiverseCoreApi.get();
        WorldManager worldManager = api.getWorldManager();

        // Controleer of de wereld al bestaat in Multiverse
        if (worldManager.isWorld(worldName)) {
            plugin.getLogger().info("Wereld van " + playerName + "bestaat al! ("+ worldName + ")");
            player.sendMessage("§aWereld van " + playerName + "bestaat al!");
            return;
        }


        Attempt<LoadedMultiverseWorld, CreateFailureReason> attempt = worldManager.createWorld(CreateWorldOptions.worldName(worldName)
                .environment(World.Environment.NORMAL)
                .seed(null)
                .worldType(WorldType.FLAT)
                .generateStructures(false)
                .generator(null));


        if (attempt.isSuccess()) {
            plugin.getLogger().info("Eiland succesvol aangemaakt voor " + playerName);

            // Wacht even tot de wereld geladen is en bouw dan het 16x16 platform
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    generateWorldBorder(world);
                }
            }, 20L); // 1 seconde vertraging
        } else {
            plugin.getLogger().severe("Aanmaken van eiland mislukt voor " + playerName);
        }
    }

    private void generateWorldBorder(World world) {
        // Set the world border to enclose the 16x16 platform
        WorldBorder worldBorder = world.getWorldBorder();
        worldBorder.setCenter(0, 0);
        worldBorder.setSize(16);
    }
}
