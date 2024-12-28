package com.sausaliens.herobrine.managers;

import com.sausaliens.herobrine.HerobrinePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class StructureManager implements Listener {
    private final HerobrinePlugin plugin;
    private final Random random;
    private final Set<UUID> recentlyModified;
    private final Set<Location> activeShrine;
    private final long MODIFICATION_COOLDOWN = 300000; // 5 minutes in milliseconds
    private final Set<Location> activeStructures = new HashSet<>();

    private final Material[] manipulableBlocks = {
        Material.TORCH,
        Material.REDSTONE_TORCH,
        Material.SOUL_TORCH,
        Material.LANTERN,
        Material.SOUL_LANTERN
    };

    public StructureManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.recentlyModified = new HashSet<>();
        this.activeShrine = new HashSet<>();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfigManager().isStructureManipulationEnabled()) return;

        Player player = event.getPlayer();
        if (recentlyModified.contains(player.getUniqueId())) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        
        // Only check when player moves to a new block
        if (from.getBlockX() == to.getBlockX() && 
            from.getBlockY() == to.getBlockY() && 
            from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        // Random chance to manipulate structures
        if (random.nextDouble() < 0.05) { // 5% chance
            manipulateNearbyStructures(player);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Check for both redstone torch placement and fire placement
        if (block.getType() != Material.REDSTONE_TORCH && block.getType() != Material.FIRE) {
            return;
        }

        plugin.getLogger().info("[Shrine Debug] Block placed: " + block.getType());

        // Find the center of the potential shrine
        Location baseLoc = null;
        if (block.getType() == Material.REDSTONE_TORCH) {
            baseLoc = getShrineBaseLocation(block);
            plugin.getLogger().info("[Shrine Debug] Checking redstone torch placement. Base location: " + 
                (baseLoc != null ? baseLoc.toString() : "null"));
        } else if (block.getType() == Material.FIRE) {
            // If placing fire, check two blocks down for mossy cobblestone
            Block below = block.getRelative(BlockFace.DOWN); // This is netherrack
            if (below.getType() == Material.NETHERRACK) {
                Block twoDown = below.getRelative(BlockFace.DOWN); // This should be mossy cobblestone
                if (twoDown.getType() == Material.MOSSY_COBBLESTONE) {
                    baseLoc = twoDown.getLocation();
                    plugin.getLogger().info("[Shrine Debug] Found shrine base at: " + baseLoc);
                } else {
                    plugin.getLogger().info("[Shrine Debug] Block below netherrack is not mossy cobblestone: " + twoDown.getType());
                }
            } else {
                plugin.getLogger().info("[Shrine Debug] Block below fire is not netherrack: " + below.getType());
            }
        }

        if (baseLoc == null) {
            plugin.getLogger().info("[Shrine Debug] No valid base location found");
            return;
        }
        
        if (activeShrine.contains(baseLoc)) {
            plugin.getLogger().info("[Shrine Debug] Shrine already active at this location");
            return;
        }

        // Check if this completes the shrine
        plugin.getLogger().info("[Shrine Debug] Checking if shrine is complete...");
        if (isCompleteShrine(baseLoc)) {
            plugin.getLogger().info("[Shrine Debug] Shrine complete! Activating...");
            handleShrineActivation(baseLoc, player);
        } else {
            plugin.getLogger().info("[Shrine Debug] Shrine incomplete");
        }
    }

    private Location getShrineBaseLocation(Block block) {
        // Get the block the torch is attached to or the block below the fire
        Block attachedTo = null;
        
        if (block.getType() == Material.REDSTONE_TORCH) {
            // For redstone torch, check the block it's attached to
            BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.DOWN};
            for (BlockFace face : faces) {
                Block relative = block.getRelative(face);
                if (relative.getType() == Material.GOLD_BLOCK) {
                    attachedTo = relative;
                    break;
                }
            }
        } else if (block.getType() == Material.FIRE) {
            // For fire, check two blocks down (netherrack -> mossy cobblestone)
            attachedTo = block.getRelative(BlockFace.DOWN, 2);
            if (attachedTo.getType() != Material.MOSSY_COBBLESTONE) {
                plugin.getLogger().info("[Shrine Debug] Block two blocks below fire is not mossy cobblestone: " + attachedTo.getType());
                return null;
            }
        }
        
        if (attachedTo == null) {
            plugin.getLogger().info("[Shrine Debug] Could not find valid base block");
            return null;
        }

        // For redstone torch placement, verify it's near a valid shrine
        if (block.getType() == Material.REDSTONE_TORCH) {
            // Look for mossy cobblestone adjacent to the gold block
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block adjacent = attachedTo.getRelative(face);
                if (adjacent.getType() == Material.MOSSY_COBBLESTONE) {
                    plugin.getLogger().info("[Shrine Debug] Found mossy cobblestone next to gold block");
                    return adjacent.getLocation();
                }
            }
            plugin.getLogger().info("[Shrine Debug] No mossy cobblestone found next to gold block");
            return null;
        }
        
        // For fire placement, return the mossy cobblestone location
        return attachedTo.getLocation();
    }

    private boolean isCompleteShrine(Location centerLoc) {
        Block center = centerLoc.getBlock();
        
        // Check if the center block is mossy cobblestone
        if (center.getType() != Material.MOSSY_COBBLESTONE) {
            plugin.getLogger().info("[Shrine Debug] Center block is not mossy cobblestone: " + center.getType());
            return false;
        }
        
        // Check for gold blocks in a cross pattern around the mossy cobblestone (at ground level)
        BlockFace[] directions = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace face : directions) {
            Block goldBlock = center.getRelative(face);
            if (goldBlock.getType() != Material.GOLD_BLOCK) {
                plugin.getLogger().info("[Shrine Debug] Missing gold block at " + face);
                return false;
            }
            
            // Check for redstone torch on top of each gold block
            Block above = goldBlock.getRelative(BlockFace.UP);
            if (above.getType() != Material.REDSTONE_TORCH) {
                plugin.getLogger().info("[Shrine Debug] Missing redstone torch above gold block at " + face);
                return false;
            }
        }
        
        // Check for netherrack above mossy cobblestone
        Block netherrack = center.getRelative(BlockFace.UP);
        if (netherrack.getType() != Material.NETHERRACK) {
            plugin.getLogger().info("[Shrine Debug] No netherrack above mossy cobblestone");
            return false;
        }
        
        // Check if the netherrack is on fire
        Block fireBlock = netherrack.getRelative(BlockFace.UP);
        if (fireBlock.getType() != Material.FIRE) {
            plugin.getLogger().info("[Shrine Debug] Netherrack is not on fire");
            return false;
        }
        
        plugin.getLogger().info("[Shrine Debug] All shrine requirements met!");
        return true;
    }

    private void handleShrineActivation(Location centerLoc, Player player) {
        if (activeShrine.contains(centerLoc)) return;
        
        activeShrine.add(centerLoc);
        Block center = centerLoc.getBlock();
        Location spawnLoc = center.getLocation().add(0.5, 2, 0.5); // 2 blocks above netherrack

        // Initial effects
        player.getWorld().strikeLightning(centerLoc);
        plugin.getEffectManager().playStructureManipulationEffects(centerLoc);
        
        // Mark the player for Herobrine's attention
        plugin.getAggressionManager().increaseAggression(player, 0.7f); // High initial aggression for summoning
        
        // Main sequence after delay
        new BukkitRunnable() {
            @Override
            public void run() {
                // Strike lightning
                player.getWorld().strikeLightning(center.getLocation());
                
                // Spawn Herobrine above the fire
                plugin.getAppearanceManager().createAppearance(player, spawnLoc);
                plugin.getEffectManager().playAppearanceEffects(player, spawnLoc);
                
                // Schedule shrine destruction and start stalking behavior
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Remove fire and effects
                        Block fireBlock = center.getRelative(BlockFace.UP);
                        if (fireBlock.getType() == Material.FIRE) {
                            fireBlock.setType(Material.AIR);
                        }
                        
                        // Cleanup shrine
                        destroyShrine(centerLoc);
                        activeShrine.remove(centerLoc);
                        
                        // Start intense stalking behavior
                        plugin.getAggressionManager().markPlayerForRevenge(player);
                        
                        // Schedule creepy message
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.sendMessage("§c§oYou have summoned me...");
                                plugin.getAggressionManager().playCreeperScare(player);
                            }
                        }.runTaskLater(plugin, 60L); // 3 seconds after shrine destruction
                    }
                }.runTaskLater(plugin, 100L); // 5 seconds
            }
        }.runTaskLater(plugin, 40L); // 2 seconds
    }

    private void destroyShrine(Location centerLoc) {
        Block center = centerLoc.getBlock();
        
        // Start from mossy cobblestone (center) and remove everything
        if (center.getType() == Material.MOSSY_COBBLESTONE) {
            // Remove fire, netherrack, and mossy cobblestone
            Block netherrack = center.getRelative(BlockFace.UP);
            Block fire = netherrack.getRelative(BlockFace.UP);
            
            if (fire.getType() == Material.FIRE) {
                fire.setType(Material.AIR);
            }
            if (netherrack.getType() == Material.NETHERRACK) {
                netherrack.setType(Material.AIR);
            }
            center.setType(Material.AIR);
            
            // Remove gold blocks and redstone torches
            BlockFace[] directions = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
            for (BlockFace face : directions) {
                Block goldBlock = center.getRelative(face);
                Block torch = goldBlock.getRelative(BlockFace.UP);
                
                if (torch.getType() == Material.REDSTONE_TORCH) {
                    torch.setType(Material.AIR);
                }
                if (goldBlock.getType() == Material.GOLD_BLOCK) {
                    goldBlock.setType(Material.AIR);
                }
            }
        }
        
        // Play effects
        plugin.getEffectManager().playStructureManipulationEffects(centerLoc);
    }

    private void manipulateNearbyStructures(Player player) {
        // Don't manipulate structures if there's an active shrine nearby
        Location playerLoc = player.getLocation();
        for (Location shrineLoc : activeShrine) {
            if (shrineLoc.getWorld().equals(playerLoc.getWorld()) && 
                shrineLoc.distance(playerLoc) < 20) {
                return;
            }
        }

        int radius = 10;
        List<Block> manipulableTargets = new ArrayList<>();

        // Find manipulable blocks in radius
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location loc = playerLoc.clone().add(x, y, z);
                    Block block = loc.getBlock();
                    
                    if (isManipulable(block)) {
                        manipulableTargets.add(block);
                    }
                }
            }
        }

        if (!manipulableTargets.isEmpty()) {
            // Choose random blocks to manipulate
            int numToManipulate = Math.min(3, manipulableTargets.size());
            Collections.shuffle(manipulableTargets);

            for (int i = 0; i < numToManipulate; i++) {
                Block block = manipulableTargets.get(i);
                manipulateBlock(block);
            }

            // Add cooldown for this player
            recentlyModified.add(player.getUniqueId());
            new BukkitRunnable() {
                @Override
                public void run() {
                    recentlyModified.remove(player.getUniqueId());
                }
            }.runTaskLater(plugin, MODIFICATION_COOLDOWN / 50); // Convert to ticks
        }
    }

    private boolean isManipulable(Block block) {
        Material type = block.getType();
        for (Material manipulable : manipulableBlocks) {
            if (type == manipulable) {
                return true;
            }
        }
        return false;
    }

    private void manipulateBlock(Block block) {
        Location loc = block.getLocation();
        Material originalType = block.getType();
        BlockFace facing = getBlockFacing(block);

        // Schedule the block manipulation
        new BukkitRunnable() {
            @Override
            public void run() {
                // Remove the block
                block.setType(Material.AIR);
                plugin.getEffectManager().playStructureManipulationEffects(loc);
            }
        }.runTask(plugin);

        // Schedule block restoration
        new BukkitRunnable() {
            @Override
            public void run() {
                block.setType(originalType);
                if (facing != null) {
                    setBlockFacing(block, facing);
                }
            }
        }.runTaskLater(plugin, 200L); // 10 seconds later
    }

    private BlockFace getBlockFacing(Block block) {
        // Implementation depends on block type
        // This is a simplified version
        return null;
    }

    private void setBlockFacing(Block block, BlockFace face) {
        // Implementation depends on block type
        // This is a simplified version
    }

    public void cleanup() {
        recentlyModified.clear();
    }

    public void removeAllStructures() {
        for (Location loc : new HashSet<>(activeStructures)) {
            removeStructure(loc);
        }
        activeStructures.clear();
    }

    private void removeStructure(Location location) {
        // Restore the original blocks
        location.getBlock().setType(Material.AIR);
        activeStructures.remove(location);
    }
} 