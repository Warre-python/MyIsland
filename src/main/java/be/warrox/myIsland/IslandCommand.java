package be.warrox.myIsland;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.mvplugins.multiverse.core.world.MultiverseWorld;

import java.util.*;

public class IslandCommand {

    private final MyIsland plugin;
    private final CreateMyIsland islandCreator;
    // Slaat op: DoelSpelerUUID -> AanvragerUUID
    private final Map<UUID, Map<String, Integer>> activeRequests = new HashMap<>();

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS = (context, builder) -> {
        String remaining = builder.getRemaining().toLowerCase();

        Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(remaining))
                .forEach(builder::suggest);

        return builder.buildFuture();
    };


    public IslandCommand(MyIsland plugin) {
        this.plugin = plugin;
        this.islandCreator = new CreateMyIsland(plugin);
    }

    public void init() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("myi")
                    .executes(context -> {
                        if (context.getSource().getExecutor() instanceof Player player) {
                            plugin.send(player, "myisland");
                        }
                        return 1;
                    });

            // Subcommand: create
            root.then(Commands.literal("create").executes(context -> {
                if (context.getSource().getExecutor() instanceof Player player) {
                    islandCreator.createIsland(player);
                }
                return 1;
            }));

            // Subcommand: tpmain
            root.then(Commands.literal("tpmain").executes(context -> {
                if (context.getSource().getExecutor() instanceof Player player) {
                    Location previousLocation = plugin.getLocationManager().getPreviousLocation(player);
                    if (previousLocation != null) {
                        player.teleport(previousLocation);
                    } else {
                        player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
                    }
                    plugin.send(player, "back_to_main");
                }
                return 1;
            }));

            // Subcommand: tpisland <player>
            root.then(Commands.literal("tpisland")
                    .executes(context -> {
                        if (context.getSource().getExecutor() instanceof Player player) {
                            plugin.getLocationManager().saveLocation(player);
                            teleportToIsland(player, player.getName());
                        }
                        return 1;
                    })
                    .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(ONLINE_PLAYERS) // <--- Hier toegevoegd
                            .executes(context -> {
                                if (context.getSource().getExecutor() instanceof Player player) {
                                    String targetName = context.getArgument("player", String.class);
                                    handleVisitRequest(player, targetName);
                                }
                                return 1;
                            })));

            // Subcommand: accept <requester>
            root.then(Commands.literal("accept")
                    .then(Commands.argument("requester", StringArgumentType.word())
                            .suggests(ONLINE_PLAYERS) // <--- Hier toegevoegd
                            .executes(context -> {
                                if (context.getSource().getExecutor() instanceof Player player) {
                                    String requesterName = context.getArgument("requester", String.class);
                                    handleAccept(player, requesterName);
                                }
                                return 1;
                            })));

            // Subcommand: kick <player>
            root.then(Commands.literal("kick")
                    .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(ONLINE_PLAYERS) // <--- Hier toegevoegd
                            .executes(context -> {
                                if (context.getSource().getExecutor() instanceof Player player) {
                                    String kickPlayer = context.getArgument("player", String.class);
                                    Player target = Bukkit.getPlayer(kickPlayer);
                                    if (target != null) {
                                        kickPlayer(player, target);
                                    }
                                }
                                return 1;
                            })));

            commands.register(root.build(), "Hoofdcommando voor MyIsland", List.of("island", "mi"));
        });
    }



    private void teleportToIsland(Player player, String targetName) {
        String worldName = "island_" + targetName;
        var worldOptional = MyIsland.getApi().getWorldManager().getWorld(worldName);

        if (worldOptional.isEmpty()) {
            plugin.send(player, "island_not_exist");
            return;
        }

        Location startLocation = player.getLocation();
        plugin.send(player, "teleport_starting_5s"); // "Blijf 5 seconden stilstaan..."

        new BukkitRunnable() {
            int secondsLeft = 5;

            @Override
            public void run() {
                // Check of de speler nog online is
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Check of de speler bewogen is (negeer kijken, check alleen x, y, z)
                if (startLocation.distanceSquared(player.getLocation()) > 0.1) {
                    plugin.send(player, "teleport_cancelled_moved");
                    this.cancel();
                    return;
                }

                secondsLeft--;

                if (secondsLeft <= 0) {
                    // Tijd is om, teleportatie uitvoeren
                    MultiverseWorld mvWorld = worldOptional.get();
                    player.teleport(mvWorld.getSpawnLocation());
                    plugin.send(player, "teleported_to_island", targetName);
                    this.cancel();
                } else {
                    // Optioneel: stuur elke seconde een melding (bijv. in de actionbar)
                    player.sendActionBar(Component.text("Teleportatie over " + secondsLeft + " seconden...", NamedTextColor.YELLOW));
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Start na 1 sec (20 ticks), herhaal elke seconde
    }


    private void handleVisitRequest(Player requester, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            plugin.send(requester, "player_not_online");
            return;
        }

        UUID targetUUID = target.getUniqueId();
        String reqName = requester.getName();

        // Verwijder vorig verzoek van deze speler indien aanwezig (om dubbele tasks te voorkomen)
        cancelRequest(targetUUID, reqName);

        // Sla het verzoek op
        activeRequests.computeIfAbsent(targetUUID, k -> new HashMap<>());

        // Start de 30 seconden timer (30 * 20 ticks)
        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            cancelRequest(targetUUID, reqName);
        }, 30 * 20L);

        activeRequests.get(targetUUID).put(reqName, taskId);

        plugin.getLocationManager().saveLocation(requester);
        plugin.send(requester, "send_request", target.getName());

        // Update je visit_request message in de .properties naar:
        // visit_request=<green>{0} wants to visit. Type <yellow>/myi accept {0}</yellow> to allow (30s).
        plugin.send(target, "visit_request", reqName);
    }

    private void handleAccept(Player owner, String requesterName) {
        Map<String, Integer> ownerRequests = activeRequests.get(owner.getUniqueId());

        if (ownerRequests == null || !ownerRequests.containsKey(requesterName)) {
            plugin.send(owner, "no_pending_requests"); // Of maak een "request_expired" key
            return;
        }

        // Stop de timer en verwijder uit map
        cancelRequest(owner.getUniqueId(), requesterName);

        Player requester = Bukkit.getPlayer(requesterName);
        if (requester != null) {
            teleportToIsland(requester, owner.getName());
            plugin.send(owner, "request_accepted");
        } else {
            plugin.send(owner, "player_not_online");
        }
    }

    private void cancelRequest(UUID ownerUUID, String reqName) {
        Map<String, Integer> reqs = activeRequests.get(ownerUUID);
        if (reqs != null && reqs.containsKey(reqName)) {
            int taskId = reqs.remove(reqName);
            Bukkit.getScheduler().cancelTask(taskId);
            if (reqs.isEmpty()) activeRequests.remove(ownerUUID);
        }
    }

    public void kickPlayer(Player player, Player kickedPlayer) {
        String worldName = "island_" + player.getName();

        if (kickedPlayer.getWorld().getName().equals(worldName)) {

            Location previousLocation = plugin.getLocationManager().getPreviousLocation(kickedPlayer);

            if (previousLocation != null) {
                kickedPlayer.teleport(previousLocation);
            } else {
                kickedPlayer.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }

            plugin.send(kickedPlayer, "got_kicked", player.getName());
            plugin.send(player, "player_kicked", kickedPlayer.getName());
        }
    }
}
