package net.snofox.minecraft.safeshutdown;

import la.loa.crispy1989.LOALib.Teleports.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.snofox.deadlydisconnects.DeadlyDisconnects;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Josh on 2020-02-29
 */
public class ShutdownTask implements Runnable {
    private final SafeShutdown module;
    private final Date shutdownDate;
    private int shutdownTask = -1;

    public ShutdownTask(final SafeShutdown module, final int seconds) {
        this.module = module;
        this.shutdownDate = new Date(new Date().getTime() + seconds * 1000);
    }

    @Override
    public void run() {
        final Collection<? extends Player> players = module.getServer().getOnlinePlayers();
        TextComponent message = new TextComponent("Server restarting " + estimateDuration(shutdownDate) + "; get safe!");
        message.setColor(ChatColor.RED);
        if(players.size() < 1) {
          module.getServer().shutdown();
        } else if(shutdownDate.before(new Date())) {
            module.stopLogins();
            movePlayers();
            if(shutdownTask == -1) {
                shutdownTask = module.getServer().getScheduler().scheduleSyncDelayedTask(module, () -> module.getServer().shutdown(), 20);
            }
        }
        for(final Player player : players) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
        }
    }

    private void movePlayers() {
        for(final Player player : module.getServer().getOnlinePlayers()) {
            try {
                DeadlyDisconnects.untag(player);
            } catch(NoClassDefFoundError ignored) {
                // No combat tagging
            }
            player.sendTitle("Server Restarting", "You were moved to the lobby", 20, 100, 50);
            BungeeCord.movePlayerToServer(player, module.getConfig().getString("lobbyServer", "nexus"));
        }
    }

    public void preCountdown() {
        for(final World world : module.getServer().getWorlds()) {
            if(world.isAutoSave()) world.save();
            world.getPlayers().forEach(Player::saveData);
        }
    }

    /**
     * Takes in a date and the highest time unit; ex "in 1 hour" or "in 3 minutes"
     * Or a few special lines for weird results
     * @param date
     * @return
     */
    String estimateDuration(Date date) {
        if(date == null) return "I guess? This is a bug";
        long timeLeft = (date.getTime() - new Date().getTime())/1000;
        if(timeLeft <= 0) return "soon";
        StringBuilder sb = new StringBuilder();
        sb.append("in ");

        LinkedHashMap<String, Long> units = new LinkedHashMap<String, Long>(4);
        units.put("day", (long)24*3600);
        units.put("hour", (long)3600);
        units.put("minute", (long)60);
        units.put("second", (long)1);

        boolean comma = false;

        for(Map.Entry<String, Long> thisUnit: units.entrySet()) {
            long unitTime = thisUnit.getValue();
            long these = timeLeft / unitTime;
            if(comma && thisUnit.getValue() == 1) break; // skip seconds if we have minutes
            if(these > 0) {
                if(comma) sb.append(", ");
                if(unitTime == 60) ++these; // add an extra minute, since we're cutting off seconds
                sb.append(these).append(" ").append(thisUnit.getKey());
                sb.append((these == 1 ? "" : "s"));
                comma = true;
            }
            timeLeft -= these * unitTime;
        }
        return sb.toString();
    }
}
