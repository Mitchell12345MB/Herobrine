package com.sausaliens.herobrine.commands;

import com.sausaliens.herobrine.HerobrinePlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HerobrineCommand implements CommandExecutor {
    private final HerobrinePlugin plugin;

    public HerobrineCommand(HerobrinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn":
                return handleSpawn(sender, args);
            case "enable":
                return handleEnable(sender);
            case "disable":
                return handleDisable(sender);
            case "config":
                return handleConfig(sender, args);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("herobrine.command.spawn")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to spawn Herobrine!");
            return true;
        }

        Player target;
        if (args.length > 1) {
            // Check permission for spawning on other players
            if (!sender.hasPermission("herobrine.command.spawn.others")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to spawn Herobrine on other players!");
                return true;
            }
            
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player " + args[1] + " is not online!");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player target!");
                return true;
            }
            target = (Player) sender;
        }

        // Create appearance near target player
        plugin.getAppearanceManager().createAppearance(target);
        
        if (sender != target) {
            sender.sendMessage(ChatColor.GREEN + "Spawned Herobrine near " + target.getName());
        }
        return true;
    }

    private boolean handleEnable(CommandSender sender) {
        if (!sender.hasPermission("herobrine.command.enable")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to enable Herobrine!");
            return true;
        }

        if (plugin.getConfigManager().isEnabled()) {
            sender.sendMessage(ChatColor.YELLOW + "Herobrine is already enabled!");
            return true;
        }

        plugin.getConfigManager().setEnabled(true);
        plugin.getAppearanceManager().startAppearanceTimer();
        plugin.getEffectManager().startEffects();
        sender.sendMessage(ChatColor.GREEN + "Herobrine has been enabled!");
        return true;
    }

    private boolean handleDisable(CommandSender sender) {
        if (!sender.hasPermission("herobrine.command.disable")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to disable Herobrine!");
            return true;
        }

        if (!plugin.getConfigManager().isEnabled()) {
            sender.sendMessage(ChatColor.YELLOW + "Herobrine is already disabled!");
            return true;
        }

        plugin.getConfigManager().setEnabled(false);
        plugin.getAppearanceManager().stopAppearanceTimer();
        plugin.getAppearanceManager().removeAllAppearances();
        plugin.getEffectManager().stopEffects();
        sender.sendMessage(ChatColor.GREEN + "Herobrine has been disabled!");
        return true;
    }

    private boolean handleConfig(CommandSender sender, String[] args) {
        if (!sender.hasPermission("herobrine.command.config")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to modify Herobrine's configuration!");
            return true;
        }

        if (args.length < 2) {
            sendConfigUsage(sender);
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "frequency":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Please specify a frequency value in seconds!");
                    return true;
                }
                try {
                    int frequency = Integer.parseInt(args[2]);
                    if (frequency < 1) {
                        sender.sendMessage(ChatColor.RED + "Frequency must be at least 1 second!");
                        return true;
                    }
                    plugin.getConfigManager().setAppearanceFrequency(frequency);
                    sender.sendMessage(ChatColor.GREEN + "Set appearance check frequency to " + frequency + " seconds!");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid frequency value!");
                }
                return true;

            case "chance":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Please specify a chance value between 0 and 1!");
                    return true;
                }
                try {
                    double chance = Double.parseDouble(args[2]);
                    if (chance < 0 || chance > 1) {
                        sender.sendMessage(ChatColor.RED + "Chance must be between 0 and 1!");
                        return true;
                    }
                    plugin.getConfigManager().setAppearanceChance(chance);
                    sender.sendMessage(ChatColor.GREEN + "Set appearance chance to " + chance);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid chance value!");
                }
                return true;

            default:
                sendConfigUsage(sender);
                return true;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Herobrine Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine spawn [player] " + ChatColor.WHITE + "- Spawn Herobrine near a player");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine enable " + ChatColor.WHITE + "- Enable Herobrine's activities");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine disable " + ChatColor.WHITE + "- Disable Herobrine's activities");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config " + ChatColor.WHITE + "- Modify Herobrine's configuration");
    }

    private void sendConfigUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Herobrine Configuration Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config frequency <seconds> " + ChatColor.WHITE + "- Set how often Herobrine might appear");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config chance <0-1> " + ChatColor.WHITE + "- Set the chance of Herobrine appearing");
    }
} 