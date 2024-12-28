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

            case "fog":
                if (args.length < 3) {
                    sendFogConfigUsage(sender);
                    return true;
                }
                switch (args[2].toLowerCase()) {
                    case "enable":
                        plugin.getConfigManager().setFogEnabled(true);
                        sender.sendMessage(ChatColor.GREEN + "Fog effects enabled");
                        return true;
                    case "disable":
                        plugin.getConfigManager().setFogEnabled(false);
                        sender.sendMessage(ChatColor.GREEN + "Fog effects disabled");
                        return true;
                    case "density":
                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.RED + "Please specify a density value (0.0 - 1.0)");
                            return true;
                        }
                        try {
                            double density = Double.parseDouble(args[3]);
                            if (density < 0 || density > 1) {
                                sender.sendMessage(ChatColor.RED + "Density must be between 0 and 1!");
                                return true;
                            }
                            plugin.getConfigManager().setFogDensity(density);
                            sender.sendMessage(ChatColor.GREEN + "Set fog density to " + density);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "Invalid density value!");
                        }
                        return true;
                    case "duration":
                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.RED + "Please specify a duration in ticks");
                            return true;
                        }
                        try {
                            int duration = Integer.parseInt(args[3]);
                            if (duration < 20) {
                                sender.sendMessage(ChatColor.RED + "Duration must be at least 20 ticks (1 second)!");
                                return true;
                            }
                            plugin.getConfigManager().setFogDuration(duration);
                            sender.sendMessage(ChatColor.GREEN + "Set fog duration to " + duration + " ticks");
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "Invalid duration value!");
                        }
                        return true;
                    default:
                        sendFogConfigUsage(sender);
                        return true;
                }

            case "footsteps":
                if (args.length < 3) {
                    sendFootstepsConfigUsage(sender);
                    return true;
                }
                switch (args[2].toLowerCase()) {
                    case "enable":
                        plugin.getConfigManager().setFootstepsEnabled(true);
                        sender.sendMessage(ChatColor.GREEN + "Footstep effects enabled");
                        return true;
                    case "disable":
                        plugin.getConfigManager().setFootstepsEnabled(false);
                        sender.sendMessage(ChatColor.GREEN + "Footstep effects disabled");
                        return true;
                    case "max":
                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.RED + "Please specify the maximum number of footsteps");
                            return true;
                        }
                        try {
                            int max = Integer.parseInt(args[3]);
                            if (max < 1) {
                                sender.sendMessage(ChatColor.RED + "Maximum footsteps must be at least 1!");
                                return true;
                            }
                            plugin.getConfigManager().setMaxFootsteps(max);
                            sender.sendMessage(ChatColor.GREEN + "Set maximum footsteps to " + max);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "Invalid number!");
                        }
                        return true;
                    default:
                        sendFootstepsConfigUsage(sender);
                        return true;
                }

            case "torches":
                if (args.length < 3) {
                    sendTorchConfigUsage(sender);
                    return true;
                }
                switch (args[2].toLowerCase()) {
                    case "enable":
                        plugin.getConfigManager().setTorchManipulationEnabled(true);
                        sender.sendMessage(ChatColor.GREEN + "Torch manipulation enabled");
                        return true;
                    case "disable":
                        plugin.getConfigManager().setTorchManipulationEnabled(false);
                        sender.sendMessage(ChatColor.GREEN + "Torch manipulation disabled");
                        return true;
                    case "radius":
                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.RED + "Please specify the radius in blocks");
                            return true;
                        }
                        try {
                            int radius = Integer.parseInt(args[3]);
                            if (radius < 5 || radius > 20) {
                                sender.sendMessage(ChatColor.RED + "Radius must be between 5 and 20 blocks!");
                                return true;
                            }
                            plugin.getConfigManager().setTorchManipulationRadius(radius);
                            sender.sendMessage(ChatColor.GREEN + "Set torch manipulation radius to " + radius);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "Invalid radius value!");
                        }
                        return true;
                    case "conversion":
                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.RED + "Please specify the conversion chance (0.0 - 1.0)");
                            return true;
                        }
                        try {
                            double chance = Double.parseDouble(args[3]);
                            if (chance < 0 || chance > 1) {
                                sender.sendMessage(ChatColor.RED + "Chance must be between 0 and 1!");
                                return true;
                            }
                            plugin.getConfigManager().setTorchConversionChance(chance);
                            sender.sendMessage(ChatColor.GREEN + "Set torch conversion chance to " + chance);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "Invalid chance value!");
                        }
                        return true;
                    case "removal":
                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.RED + "Please specify the removal chance (0.0 - 1.0)");
                            return true;
                        }
                        try {
                            double chance = Double.parseDouble(args[3]);
                            if (chance < 0 || chance > 1) {
                                sender.sendMessage(ChatColor.RED + "Chance must be between 0 and 1!");
                                return true;
                            }
                            plugin.getConfigManager().setTorchRemovalChance(chance);
                            sender.sendMessage(ChatColor.GREEN + "Set torch removal chance to " + chance);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "Invalid chance value!");
                        }
                        return true;
                    default:
                        sendTorchConfigUsage(sender);
                        return true;
                }

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
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config fog " + ChatColor.WHITE + "- Configure fog effects");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config footsteps " + ChatColor.WHITE + "- Configure footstep effects");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config torches " + ChatColor.WHITE + "- Configure torch manipulation");
    }

    private void sendFogConfigUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Fog Configuration Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config fog enable " + ChatColor.WHITE + "- Enable fog effects");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config fog disable " + ChatColor.WHITE + "- Disable fog effects");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config fog density <0-1> " + ChatColor.WHITE + "- Set fog density");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config fog duration <ticks> " + ChatColor.WHITE + "- Set fog duration (20 ticks = 1 second)");
    }

    private void sendFootstepsConfigUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Footsteps Configuration Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config footsteps enable " + ChatColor.WHITE + "- Enable footstep effects");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config footsteps disable " + ChatColor.WHITE + "- Disable footstep effects");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config footsteps max <number> " + ChatColor.WHITE + "- Set maximum number of footsteps");
    }

    private void sendTorchConfigUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Torch Manipulation Configuration Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config torches enable " + ChatColor.WHITE + "- Enable torch manipulation");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config torches disable " + ChatColor.WHITE + "- Disable torch manipulation");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config torches radius <blocks> " + ChatColor.WHITE + "- Set manipulation radius (5-20)");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config torches conversion <0-1> " + ChatColor.WHITE + "- Set torch conversion chance");
        sender.sendMessage(ChatColor.YELLOW + "/herobrine config torches removal <0-1> " + ChatColor.WHITE + "- Set torch removal chance");
    }
} 