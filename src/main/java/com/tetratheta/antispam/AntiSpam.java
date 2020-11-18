package com.tetratheta.antispam;

import org.bukkit.BanList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AntiSpam extends JavaPlugin implements Listener, CommandExecutor {
    //UUID playerUuid = new UUID(0, 0);
    Map<UUID, Integer> playerMap = new HashMap<>();
    int limit_second, limit_count, tempban_time;
    String punish_type, message;
    BanList banList = getServer().getBanList(BanList.Type.NAME);

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("AntiSpam is enabled");
        checkConfig();
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("AntiSpam").setExecutor(this); //TODO: handle NullPointerException thing (what should I do?)
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("AntiSpam is disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                checkConfig();
                getLogger().info("AntiSpam config reloaded");
                return true;
            }
        }
        return true;
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (e.getPlayer().hasPermission("AntiSpam.login.bypass")) return;
        // playerMap.put(playerUuid, 0);
        UUID uuid = e.getPlayer().getUniqueId();
        if (playerMap.containsKey(uuid)) {
            playerMap.put(uuid, playerMap.get(uuid) + 1);
        } else {
            playerMap.put(uuid, 1);
        }
        if (playerMap.get(uuid) > limit_count) {
            // do something to player!
            if ("tempban".equals(punish_type)) {
                long expire = System.currentTimeMillis() + (tempban_time * 60 * 1000);
                banList.addBan(e.getPlayer().getName(), message, new Date(expire), "AntiSpam Login");
                e.getPlayer().kickPlayer(message);
                playerMap.remove(uuid);
                return;
            } else if ("kick".equals(punish_type)) {
                e.getPlayer().kickPlayer(message);
                playerMap.remove(uuid);
                return;
            } else if ("ban".equals(punish_type)) {
                banList.addBan(e.getPlayer().getName(), message, null, "AntiSpam Login");
                e.getPlayer().kickPlayer(message);
                playerMap.remove(uuid);
                return;
            }
        }

        BukkitScheduler bukkitScheduler = this.getServer().getScheduler();
        bukkitScheduler.scheduleSyncDelayedTask(this, () -> playerMap.remove(uuid), 20 * limit_second);
    }

    public void checkConfig() {
        File configFile = new File(this.getDataFolder(), "config.yml");
        if (!configFile.exists()) { // config file doesn't exist
            this.saveDefaultConfig();
        }

        FileConfiguration config = this.getConfig();

        // Checking each sections are set (because i am obsessive) and get it
        if (!config.isSet("login-spam.limit-second")) {
            getLogger().warning("login-spam.limit-second is not found. using default value.");
            limit_second = 60;
        } else {
            limit_second = config.getInt("login-spam.limit-second");
        }
        if (!config.isSet("login-spam.limit-count")) {
            getLogger().warning("login-spam.limit-count is not found. using default value.");
            limit_count = 3;
        } else {
            limit_count = config.getInt("login-spam.limit-count");
        }
        if (!config.isSet("login-spam.punish-type")) {
            getLogger().warning("login-spam.punish-type is not found. using default value.");
            punish_type = "tempban";
        } else {
            punish_type = config.getString("login-spam.punish-type");
        }
        if (!config.isSet("login-spam.tempban-time")) {
            getLogger().warning("login-spam.tempban-time is not found. using default value.");
            tempban_time = 120;
        } else {
            tempban_time = config.getInt("login-spam.tempban-time");
        }
        if (!config.isSet("login-spam.message")) {
            getLogger().warning("login-spam.message is not found. using default value.");
            message = "You have been tempbanned for login spamming! Join after 2 minutes.";
        } else {
            message = config.getString("login-spam.message");
        }
    }
}
