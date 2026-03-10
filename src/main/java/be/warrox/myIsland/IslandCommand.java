package be.warrox.myIsland;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.core.world.MultiverseWorld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IslandCommand {

    private final MyIsland plugin;
    private final CreateMyIsland islandCreator;
    // Slaat op: DoelSpelerUUID -> AanvragerUUID
    private final Map<UUID, UUID> visitRequests = new HashMap<>();

    public IslandCommand(MyIsland plugin) {
        this.plugin = plugin;
        this.islandCreator = new CreateMyIsland(plugin);
    }

    public void init() {
        // Registreer het commando via de Paper Lifecycle API
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            // Je hoofdcommando "myi"
            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("myi")
                    .executes(context -> {
                        if (context.getSource().getExecutor() instanceof Player player) {
                            player.sendMessage("§cGebruik: /myi <create | tpisland | tpmain | accept>");
                        }
                        return 1;
                    });

            // Subcommand: create
            root.then(Commands.literal("create").executes(context -> {
                if (context.getSource().getExecutor() instanceof Player player) {
                    islandCreator.createIsland(player);
                    player.sendMessage("§aEiland creatie gestart...");
                }
                return 1;
            }));

            // Subcommand: tpmain
            root.then(Commands.literal("tpmain").executes(context -> {
                if (context.getSource().getExecutor() instanceof Player player) {
                    Location previousLocation = plugin.getLocationManager().getPreviousLocation(player);
                    if (previousLocation != null) {
                        player.teleport(previousLocation);
                        player.sendMessage("§aTerug naar de hoofdwereld!");
                    } else {
                        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                        player.sendMessage("§aTerug naar de hoofdwereld!");
                    }
                }
                return 1;
            }));

            // Subcommand: tpisland
            LiteralArgumentBuilder<CommandSourceStack> tpisland = Commands.literal("tpisland")
                    .executes(context -> {
                        if (context.getSource().getExecutor() instanceof Player player) {
                            plugin.getLocationManager().saveLocation(player);
                            teleportToIsland(player, player.getName());
                        }
                        return 1;
                    });

            // Argument voor tpisland: /myi tpisland <player>
            tpisland.then(Commands.argument("player", StringArgumentType.word())
                    .executes(context -> {
                        if (context.getSource().getExecutor() instanceof Player player) {
                            String targetName = context.getArgument("player", String.class);
                            handleVisitRequest(player, targetName);
                        }
                        return 1;
                    }));

            root.then(tpisland);

            // Subcommand: accept
            root.then(Commands.literal("accept").executes(context -> {
                if (context.getSource().getExecutor() instanceof Player player) {
                    handleAccept(player);
                }
                return 1;
            }));

            // DAWERKELIJKE REGISTRATIE
            commands.register(root.build(), "Hoofdcommando voor MyIsland", List.of("island", "mi"));
        });
    }



    private void teleportToIsland(Player player, String targetName) {
        String worldName = "island_" + targetName;

        MultiverseWorld mvWorld = MyIsland.getApi().getWorldManager().getWorld(worldName).get();

        if (mvWorld != null) {
            player.teleport(mvWorld.getSpawnLocation());
            player.sendMessage("§aGeteleporteerd naar " + targetName + "'s eiland!");
        } else {
            player.sendMessage("§cDit eiland bestaat niet.");
        }
    }

    private void handleVisitRequest(Player requester, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            requester.sendMessage("§cSpeler niet online.");
            return;
        }
        plugin.getLocationManager().saveLocation(requester);
        visitRequests.put(target.getUniqueId(), requester.getUniqueId());
        requester.sendMessage("§eVerzoek verzonden naar " + targetName);
        target.sendMessage("§e" + requester.getName() + " wil je eiland bezoeken. Typ §a/myi accept §eom toe te staan.");
    }

    private void handleAccept(Player owner) {
        UUID requesterUUID = visitRequests.remove(owner.getUniqueId());
        if (requesterUUID == null) {
            owner.sendMessage("§cGeen openstaande verzoeken.");
            return;
        }
        Player requester = Bukkit.getPlayer(requesterUUID);
        if (requester != null) {
            teleportToIsland(requester, owner.getName());
            owner.sendMessage("§aVerzoek geaccepteerd.");
        }
    }
}
