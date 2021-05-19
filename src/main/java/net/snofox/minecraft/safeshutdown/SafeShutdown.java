package net.snofox.minecraft.safeshutdown;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class SafeShutdown extends JavaPlugin implements Listener {
    private int taskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if(args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Syntax error: /stop <seconds>");
            return true;
        }
        final int seconds = Integer.parseInt(args[0]);
        sender.sendMessage(ChatColor.RED + "Server shutting down in " + seconds + " seconds");
        if(taskId != -1) getServer().getScheduler().cancelTask(taskId);
        final ShutdownTask task = new ShutdownTask(this, seconds);
        task.preCountdown();
        taskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, task, 1, 20);
        if(taskId == -1) sender.sendMessage("Failed to schedule task");
        return true;
    }
}
