package be.warrox.myIsland;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IslandCommand implements CommandExecutor {

    private final MyIsland plugin;
    private final CreateMyIsland islandCreator;
    // Slaat op: DoelSpelerUUID -> AanvragerUUID
    private final Map<UUID, UUID> visitRequests = new HashMap<>();

    public IslandCommand(MyIsland plugin) {
        this.plugin = plugin;
        this.islandCreator = new CreateMyIsland(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            player.sendMessage("§cGebruik: /myi <create|tpisland|tpmain|accept>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                islandCreator.createIsland(player.getName());
                player.sendMessage("§aEiland creatie gestart...");
            }
            case "tpmain" -> {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                player.sendMessage("§aTerug naar de hoofdwereld!");
            }
            case "tpisland" -> {
                if (args.length < 2) {
                    // Teleporteer naar eigen eiland
                    teleportToIsland(player, player.getName());
                } else {
                    // Verzoek om naar iemand anders te gaan
                    handleVisitRequest(player, args[1]);
                }
            }
            case "accept" -> handleAccept(player);
            default -> player.sendMessage("§cOnbekend commando.");
        }
        return true;
    }

    private void teleportToIsland(Player player, String targetName) {
        String worldName = "island_" + targetName;
        var mvWorld = plugin.getMultiverseCore().getMVWorldManager().getMVWorld(worldName);

        if (mvWorld != null) {
            player.teleport(mvWorld.getCBWorld().getSpawnLocation());
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

