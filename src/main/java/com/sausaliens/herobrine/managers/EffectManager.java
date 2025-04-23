package com.sausaliens.herobrine.managers;

import com.sausaliens.herobrine.HerobrinePlugin;
import com.sausaliens.herobrine.managers.ParanoiaManager.EncounterType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Candle;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.Chest;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;

public class EffectManager implements Listener {
    private final HerobrinePlugin plugin;
    private final Random random;
    private final Map<UUID, List<BukkitTask>> activeTasks;
    private final Map<UUID, BukkitTask> activeFogTasks;
    private final Map<UUID, BukkitTask> footstepTasks;
    private final Sound[] creepySounds = {
        Sound.AMBIENT_CAVE,
        Sound.ENTITY_ENDERMAN_STARE,
        Sound.ENTITY_GHAST_AMBIENT,
        Sound.ENTITY_WITHER_AMBIENT,
        Sound.BLOCK_PORTAL_AMBIENT,
        Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD,
        Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT,
        Sound.AMBIENT_BASALT_DELTAS_MOOD,
        Sound.BLOCK_SCULK_SENSOR_CLICKING,
        Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM
    };

    private BukkitTask effectTask;
    private BukkitTask ambientSoundTask;

    // Map to track temporary block replacements
    private final Map<Location, BlockReplacement> temporaryReplacements = new HashMap<>();
    private BukkitTask blockReplacementTask;

    public EffectManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.activeTasks = new HashMap<>();
        this.activeFogTasks = new HashMap<>();
        this.footstepTasks = new HashMap<>();
    }

    public void playAppearanceEffects(Player player, Location location) {
        if (!plugin.getConfigManager().isAmbientSoundsEnabled()) return;

        // Create unsettling initial effects
        player.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.3f);
        player.playSound(location, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.5f, 0.5f);
        location.getWorld().spawnParticle(Particle.SMOKE, location, 50, 0.5, 1, 0.5, 0.02);
        location.getWorld().spawnParticle(Particle.SOUL, location, 20, 0.5, 1, 0.5, 0.02);
        
        // Add screen shake effect for dramatic appearance
        playScreenShakeEffect(player, 0.5, 40); // medium intensity, 2 second duration
        
        // Add fog effect
        if (plugin.getConfigManager().isFogEnabled()) {
            createFogEffect(player, location);
        }
        
        // Add flashing text for a brief moment
        if (random.nextDouble() < 0.7) { // 70% chance for appearance to include flashing text
            playFlashingTextEffect(player, 60, 0.6); // 3 seconds, medium intensity
        }
        
        // Randomly show a fake error message
        if (random.nextDouble() < 0.3) { // 30% chance
            displayFakeErrorMessage(player);
        }
        
        // Very small chance to trigger maze teleportation (extremely rare)
        if (random.nextDouble() < 0.1) { // 10% chance
            // Maze teleportation is a rare and intense event - 10-15 seconds duration
            performMazeTeleportEvent(player, 200 + random.nextInt(100), 0.7); 
        }
        
        // Schedule ambient effects
        List<BukkitTask> playerTasks = activeTasks.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());

        // Random flickering lights effect
        BukkitTask flickerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (random.nextDouble() < plugin.getConfigManager().getAmbientSoundChance()) {
                    Location playerLoc = player.getLocation();
                    player.playSound(playerLoc, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.2f, 0.5f);
                }
            }
        }.runTaskTimer(plugin, 20L, plugin.getConfigManager().getAmbientSoundFrequency() * 20L);
        playerTasks.add(flickerTask);

        // Ambient sounds task with randomized timing
        BukkitTask soundTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (random.nextDouble() < plugin.getConfigManager().getAmbientSoundChance()) {
                    playRandomCreepySound(player, location);
                    // Randomly play distant sounds from different directions
                    if (random.nextDouble() < 0.3) {
                        Location soundLoc = player.getLocation().add(
                            (random.nextDouble() - 0.5) * 20,
                            0,
                            (random.nextDouble() - 0.5) * 20
                        );
                        player.playSound(soundLoc, Sound.ENTITY_WARDEN_NEARBY_CLOSE, 0.2f, 0.5f);
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, plugin.getConfigManager().getAmbientSoundFrequency() * 20L);
        playerTasks.add(soundTask);

        // Particle effects task
        BukkitTask particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                location.getWorld().spawnParticle(Particle.SMOKE, location, 10, 0.5, 1, 0.5, 0.02);
                if (random.nextDouble() < 0.3) {
                    location.getWorld().spawnParticle(Particle.SOUL, location, 5, 0.3, 0.5, 0.3, 0.02);
                }
            }
        }.runTaskTimer(plugin, 20L, 15L);
        playerTasks.add(particleTask);
    }

    private void playRandomCreepySound(Player player, Location location) {
        Sound sound = creepySounds[random.nextInt(creepySounds.length)];
        float volume = 0.3f + random.nextFloat() * 0.3f;
        float pitch = 0.5f + random.nextFloat() * 0.3f;
        player.playSound(location, sound, volume, pitch);
    }

    public void playStalkingEffects(Player player, Location location) {
        if (!plugin.getConfigManager().isAmbientSoundsEnabled()) return;

        // Subtle whisper effects
        float pitch = 0.3f + random.nextFloat() * 0.2f;
        player.playSound(location, Sound.ENTITY_WARDEN_NEARBY_CLOSER, 0.15f, pitch);
        
        // Very subtle screen shake to create unease
        playScreenShakeEffect(player, 0.15, 20); // low intensity, 1 second duration
        
        // Unsettling particle effects
        location.getWorld().spawnParticle(Particle.SMOKE, location, 3, 0.2, 0.2, 0.2, 0.01);
        if (random.nextDouble() < 0.2) {
            location.getWorld().spawnParticle(Particle.SOUL, location, 1, 0.1, 0.1, 0.1, 0.01);
        }
        
        // Random chance to trigger skinwalker effect
        if (random.nextDouble() < 0.15) { // 15% chance
            // Choose a mode randomly
            int mode = random.nextInt(3); // 0=stare, 1=attack after staring, 2=attack if provoked
            
            // Duration and intensity varies
            int duration = 100 + random.nextInt(200); // 5-15 seconds
            double intensity = 0.3 + (random.nextDouble() * 0.5); // 0.3-0.8 intensity
            
            // Create the effect
            createSkinwalkerEffect(player, duration, intensity, mode);
        }
    }

    public void playStructureManipulationEffects(Location location) {
        if (!plugin.getConfigManager().isAmbientSoundsEnabled()) return;

        // Play breaking sounds
        location.getWorld().playSound(location, Sound.BLOCK_STONE_BREAK, 1.0f, 0.5f);
        
        // Spawn particles
        location.getWorld().spawnParticle(Particle.DUST_PLUME, location, 50, 0.5, 0.5, 0.5, 0.05);
    }

    public void stopEffects(Player player) {
        List<BukkitTask> tasks = activeTasks.remove(player.getUniqueId());
        if (tasks != null) {
            tasks.forEach(BukkitTask::cancel);
        }

        // Also stop fog effects
        BukkitTask fogTask = activeFogTasks.remove(player.getUniqueId());
        if (fogTask != null) {
            fogTask.cancel();
            // Remove fog potion effects
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.DARKNESS);
        }
    }

    public void cleanup() {
        // Cancel all active tasks
        for (List<BukkitTask> tasks : activeTasks.values()) {
            tasks.forEach(BukkitTask::cancel);
        }
        activeTasks.clear();

        // Cancel all fog tasks
        for (BukkitTask task : activeFogTasks.values()) {
            task.cancel();
        }
        activeFogTasks.clear();

        // Cancel all footstep tasks
        for (BukkitTask task : footstepTasks.values()) {
            task.cancel();
        }
        footstepTasks.clear();

        // Make sure we revert all temporary block replacements
        if (!temporaryReplacements.isEmpty()) {
            for (BlockReplacement replacement : temporaryReplacements.values()) {
                Block block = replacement.location.getBlock();
                block.setBlockData(replacement.originalData);
            }
            temporaryReplacements.clear();
        }
        
        // Cancel the replacement task if running
        if (blockReplacementTask != null) {
            blockReplacementTask.cancel();
            blockReplacementTask = null;
        }
    }

    public void startEffects() {
        if (plugin.getConfigManager().isAmbientSoundsEnabled()) {
            startAmbientSounds();
        }
    }

    public void startAmbientSounds() {
        if (ambientSoundTask != null) {
            ambientSoundTask.cancel();
        }
        
        int frequency = plugin.getConfigManager().getAmbientSoundFrequency() * 20; // Convert to ticks
        ambientSoundTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getConfigManager().isEnabled() || 
                !plugin.getConfigManager().isAmbientSoundsEnabled()) {
                return;
            }
            
            // Check for all online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Random chance for ambient sounds
                double chance = plugin.getConfigManager().getAmbientSoundChance();
                if (Math.random() < chance) {
                    playRandomAmbientSound(player);
                }
            }
        }, frequency, frequency);
    }

    /**
     * Play a random ambient sound for a player
     * @param player The player to play the sound for
     */
    private void playRandomAmbientSound(Player player) {
        if (!plugin.getConfigManager().isAmbientSoundsEnabled()) return;
        
        // Get a random sound
        Sound sound = getRandomAmbientSound(player);
        if (sound == null) return;
        
        // Determine base volume and pitch
        float baseVolume = 0.5f;
        float basePitch = 0.8f + (random.nextFloat() * 0.4f); // 0.8-1.2
        
        // Adjust based on paranoia level if available
        if (plugin.getParanoiaManager() != null) {
            // Increase volume based on exposure
            baseVolume = (float)plugin.getParanoiaManager().getModifiedEffectIntensity(player, baseVolume);
            
            // Record ambient encounter
            plugin.getParanoiaManager().recordEncounter(player, EncounterType.AMBIENT);
        }
        
        // Calculate from where the sound should originate
        Location playerLoc = player.getLocation();
        Location soundLoc;
        
        // Either from behind player or from random nearby location
        if (random.nextBoolean()) {
            // Behind player
            double angle = Math.toRadians(playerLoc.getYaw() + 180);
            double distance = 5 + (random.nextDouble() * 10);
            double x = Math.sin(angle) * distance;
            double z = Math.cos(angle) * distance;
            soundLoc = playerLoc.clone().add(x, 0, z);
        } else {
            // Random nearby location
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 5 + (random.nextDouble() * 15);
            double x = Math.sin(angle) * distance;
            double z = Math.cos(angle) * distance;
            soundLoc = playerLoc.clone().add(x, 0, z);
        }
        
        // Play the sound
        player.playSound(soundLoc, sound, baseVolume, basePitch);
        
        // Debug message
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Played ambient sound " + sound.toString() + 
                    " to player " + player.getName() + " at volume " + baseVolume);
        }
    }

    /**
     * Get a random ambient sound appropriate for the player's context
     * @param player The player
     * @return A random sound from the ambient sound pool
     */
    private Sound getRandomAmbientSound(Player player) {
        // Define ambient sound pools
        Sound[] creepySounds = {
            Sound.AMBIENT_CAVE,
            Sound.AMBIENT_BASALT_DELTAS_MOOD,
            Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD,
            Sound.AMBIENT_NETHER_WASTES_MOOD,
            Sound.ENTITY_ENDERMAN_STARE,
            Sound.ENTITY_ENDERMAN_AMBIENT,
            Sound.ENTITY_WARDEN_NEARBY_CLOSER,
            Sound.ENTITY_WARDEN_AMBIENT,
            Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE
        };
        
        Sound[] distantSounds = {
            Sound.BLOCK_WOODEN_DOOR_OPEN,
            Sound.BLOCK_WOODEN_DOOR_CLOSE,
            Sound.BLOCK_WOODEN_TRAPDOOR_OPEN,
            Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE,
            Sound.BLOCK_CHEST_CLOSE,
            Sound.BLOCK_STONE_BREAK,
            Sound.ENTITY_ZOMBIE_AMBIENT
        };
        
        Sound[] movementSounds = {
            Sound.BLOCK_GRASS_STEP,
            Sound.BLOCK_STONE_STEP,
            Sound.BLOCK_GRAVEL_STEP,
            Sound.BLOCK_WOOD_STEP
        };
        
        // Determine context-appropriate sound pool based on player environment
        Sound[] soundPool;
        
        // Player underground or in dark area
        if (player.getLocation().getBlock().getLightLevel() < 8) {
            // Higher chance of creepy sounds underground
            soundPool = random.nextDouble() < 0.7 ? creepySounds : 
                        (random.nextDouble() < 0.6 ? movementSounds : distantSounds);
        } else if (player.getWorld().getTime() > 13000) {
            // Nighttime - mix of all sound types with emphasis on creepy
            soundPool = random.nextDouble() < 0.5 ? creepySounds : 
                        (random.nextDouble() < 0.6 ? distantSounds : movementSounds);
        } else {
            // Daytime - mostly distant sounds, rarely creepy
            soundPool = random.nextDouble() < 0.7 ? distantSounds : 
                        (random.nextDouble() < 0.6 ? movementSounds : creepySounds);
        }
        
        // Select random sound from the chosen pool
        return soundPool[random.nextInt(soundPool.length)];
    }

    /**
     * Stop all effects and ambient sounds
     */
    public void stopEffects() {
        if (effectTask != null) {
            effectTask.cancel();
            effectTask = null;
        }
        
        stopAmbientSounds();
        
        // Stop effects for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            stopEffects(player);
        }
    }

    /**
     * Stop only the ambient sounds
     */
    public void stopAmbientSounds() {
        if (ambientSoundTask != null) {
            ambientSoundTask.cancel();
            ambientSoundTask = null;
        }
    }

    public void playStalkEffects(Player player, Location location) {
        // Play creepy ambient sounds
        player.playSound(location, Sound.AMBIENT_CAVE, 1.0f, 0.5f);
        
        // Add some particle effects
        location.getWorld().spawnParticle(Particle.CLOUD, location, 15, 0.5, 1, 0.5, 0.01);
    }

    private void createFogEffect(Player player, Location location) {
        // Cancel any existing fog task for this player
        BukkitTask existingTask = activeFogTasks.remove(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }

        // Get base fog parameters
        double baseDensity = plugin.getConfigManager().getFogDensity();
        int baseDuration = plugin.getConfigManager().getFogDuration();
        
        // Adjust based on paranoia level if available
        if (plugin.getParanoiaManager() != null) {
            // Modify fog intensity based on exposure
            baseDensity = plugin.getParanoiaManager().getModifiedEffectIntensity(player, baseDensity);
            
            // Record effect encounter
            plugin.getParanoiaManager().recordEncounter(player, EncounterType.AMBIENT);
        }

        // Calculate effect amplifiers based on density
        // Scale density to appropriate ranges:
        // Blindness: 0-1 (2 levels, less intense)
        // Darkness: 0-1 (2 levels, reduced from 0-2)
        int blindnessAmplifier = Math.min(1, (int)(baseDensity * 1.5)); // Scales 0.0-1.0 to 0-1
        int darknessAmplifier = Math.min(1, (int)(baseDensity * 1.5)); // Scales 0.0-1.0 to 0-1, reduced max level
        
        // Adjust effect durations based on density
        int adjustedDuration = (int)(baseDuration * (0.3 + (baseDensity * 0.4))); // Lower density = much shorter duration

        // Apply initial effects with reduced duration for darkness
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, adjustedDuration, blindnessAmplifier, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, (int)(adjustedDuration * 0.5), darknessAmplifier, false, false));

        // Debug message
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Created fog effect for player " + player.getName() + 
                    " with density " + baseDensity + " and duration " + adjustedDuration);
        }

        // Create ambient effects task
        BukkitTask fogTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= adjustedDuration) {
                    cancel();
                    activeFogTasks.remove(player.getUniqueId());
                    // Remove effects when done
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                    player.removePotionEffect(PotionEffectType.DARKNESS);
                    return;
                }

                // Add some ambient effects
                if (random.nextDouble() < 0.2) { // 20% chance each tick
                    Location effectLoc = player.getLocation().add(
                        (random.nextDouble() - 0.5) * 10,
                        random.nextDouble() * 3,
                        (random.nextDouble() - 0.5) * 10
                    );
                    
                    // Minimal particle effects for atmosphere
                    player.spawnParticle(Particle.CLOUD, effectLoc, 1, 0.5, 0.5, 0.5, 0);
                    
                    // Play subtle ambient sounds
                    if (random.nextDouble() < 0.3) {
                        float pitch = 0.5f + random.nextFloat() * 0.2f;
                        player.playSound(effectLoc, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.2f, pitch);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 5L);

        activeFogTasks.put(player.getUniqueId(), fogTask);
    }

    public void playFootstepEffects(Player player) {
        // Cancel any existing footstep task for this player
        BukkitTask existingTask = footstepTasks.remove(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }

        BukkitTask footstepTask = new BukkitRunnable() {
            int steps = 0;
            final int MAX_STEPS = 10;

            @Override
            public void run() {
                if (!player.isOnline() || steps >= MAX_STEPS) {
                    cancel();
                    footstepTasks.remove(player.getUniqueId());
                    return;
                }

                Location playerLoc = player.getLocation();
                double angle = Math.toRadians(playerLoc.getYaw() + 180); // Behind the player
                double distance = 5 + random.nextDouble() * 3; // 5-8 blocks behind
                
                Location stepLoc = playerLoc.clone().add(
                    Math.sin(angle) * distance,
                    0,
                    Math.cos(angle) * distance
                );
                
                // Adjust Y to ground level
                stepLoc.setY(stepLoc.getWorld().getHighestBlockYAt(stepLoc));
                
                // Play footstep sound
                player.playSound(stepLoc, Sound.BLOCK_STONE_STEP, 0.15f, 0.5f);
                
                // Add some dust particles
                player.spawnParticle(Particle.CLOUD, stepLoc.add(0, 0.1, 0), 3, 0.1, 0, 0.1, 0.01);
                
                steps++;
            }
        }.runTaskTimer(plugin, 20L, 20L); // One step per second

        footstepTasks.put(player.getUniqueId(), footstepTask);
    }

    public void manipulateTorches(Location center, int radius) {
        World world = center.getWorld();
        int startX = center.getBlockX() - radius;
        int startY = center.getBlockY() - radius;
        int startZ = center.getBlockZ() - radius;
        
        for (int x = startX; x <= center.getBlockX() + radius; x++) {
            for (int y = startY; y <= center.getBlockY() + radius; y++) {
                for (int z = startZ; z <= center.getBlockZ() + radius; z++) {
                    Location loc = new Location(world, x, y, z);
                    Block block = loc.getBlock();
                    
                    if (block.getType() == Material.TORCH) {
                        if (random.nextDouble() < 0.7) { // 70% chance to modify torch
                            if (random.nextDouble() < 0.3) { // 30% chance to remove
                                block.setType(Material.AIR);
                                world.spawnParticle(Particle.SMOKE, loc, 5, 0.2, 0.2, 0.2, 0.01);
                                world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.3f, 1.0f);
                            } else { // 70% chance to convert to redstone torch
                                block.setType(Material.REDSTONE_TORCH);
                                world.spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, 5, 0.2, 0.2, 0.2, 0.01);
                                world.playSound(loc, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.3f, 0.5f);
                            }
                        }
                    } else if (block.getType() == Material.LANTERN) {
                        if (random.nextDouble() < 0.5) { // 50% chance to convert lantern
                            block.setType(Material.SOUL_LANTERN);
                            world.spawnParticle(Particle.SOUL, loc, 5, 0.2, 0.2, 0.2, 0.01);
                            world.playSound(loc, Sound.BLOCK_SOUL_SAND_BREAK, 0.3f, 0.5f);
                        }
                    } else if (block.getType().toString().contains("CANDLE")) {
                        if (random.nextDouble() < 0.8) { // 80% chance to extinguish candle
                            Candle candle = (Candle) block.getBlockData();
                            if (candle.isLit()) {
                                candle.setLit(false);
                                block.setBlockData(candle);
                                world.spawnParticle(Particle.SMOKE, loc.add(0.5, 0.5, 0.5), 5, 0.1, 0.1, 0.1, 0.01);
                                world.playSound(loc, Sound.BLOCK_CANDLE_EXTINGUISH, 0.3f, 1.0f);
                            }
                        }
                    }
                }
            }
        }
    }

    public void playSleepPreventionEffects(Player player) {
        if (!plugin.getConfigManager().isAmbientSoundsEnabled()) return;

        // Play creepy sounds
        float pitch = 0.5f + random.nextFloat() * 0.2f;
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_NEARBY_CLOSER, 0.3f, pitch);
        player.playSound(player.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.3f, pitch);

        // Add some unsettling particles around the bed
        Location bedLocation = player.getLocation();
        bedLocation.getWorld().spawnParticle(Particle.SOUL, bedLocation, 20, 1, 0.5, 1, 0.02);
        bedLocation.getWorld().spawnParticle(Particle.SMOKE, bedLocation, 30, 1, 0.5, 1, 0.02);

        // Add intense screen shake to create a nightmare effect
        playScreenShakeEffect(player, 0.7, 60); // High intensity, 3 second duration
        
        // Send a creepy message
        String[] messages = {
            "You cannot rest now...",
            "He is watching...",
            "Too close...",
            "Not safe here..."
        };
        player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.ITALIC + messages[random.nextInt(messages.length)]);
    }

    public void leaveChestDonation(Location location) {
        Block block = location.getBlock();
        if (!(block.getState() instanceof Chest)) return;

        Chest chest = (Chest) block.getState();
        Inventory inventory = chest.getInventory();

        // Don't add items if the chest is completely full
        if (inventory.firstEmpty() == -1) return;

        // Define possible "donations" with their chances
        Map<ItemStack, Double> possibleItems = new HashMap<>();
        
        // Redstone-related items (common)
        possibleItems.put(new ItemStack(Material.REDSTONE, random.nextInt(5) + 1), 0.4);
        possibleItems.put(new ItemStack(Material.REDSTONE_TORCH, random.nextInt(3) + 1), 0.3);
        
        // Soul-related items (uncommon)
        possibleItems.put(new ItemStack(Material.SOUL_SAND, random.nextInt(3) + 1), 0.2);
        possibleItems.put(new ItemStack(Material.SOUL_SOIL, random.nextInt(2) + 1), 0.2);
        possibleItems.put(new ItemStack(Material.SOUL_LANTERN, 1), 0.15);
        
        // Creepy items (rare)
        possibleItems.put(new ItemStack(Material.BONE, random.nextInt(3) + 1), 0.1);
        possibleItems.put(new ItemStack(Material.WITHER_ROSE, 1), 0.05);
        
        // Special named items (very rare)
        ItemStack mysteriousBook = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) mysteriousBook.getItemMeta();
        bookMeta.setTitle("His Journal");
        bookMeta.setAuthor("Unknown");
        bookMeta.addPage("I see you...\n\nYou can't hide...\n\nI am always watching...");
        mysteriousBook.setItemMeta(bookMeta);
        possibleItems.put(mysteriousBook, 0.02);

        ItemStack cursedCompass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = cursedCompass.getItemMeta();
        compassMeta.setDisplayName(ChatColor.DARK_RED + "Cursed Compass");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "It always points to Him...");
        compassMeta.setLore(lore);
        cursedCompass.setItemMeta(compassMeta);
        possibleItems.put(cursedCompass, 0.01);

        // Attempt to add 1-3 random items
        int itemsToAdd = random.nextInt(3) + 1;
        for (int i = 0; i < itemsToAdd; i++) {
            if (inventory.firstEmpty() == -1) break; // Stop if chest becomes full

            // Select a random item based on probabilities
            double rand = random.nextDouble();
            double cumulativeProbability = 0.0;
            
            for (Map.Entry<ItemStack, Double> entry : possibleItems.entrySet()) {
                cumulativeProbability += entry.getValue();
                if (rand <= cumulativeProbability) {
                    inventory.addItem(entry.getKey().clone());
                    break;
                }
            }
        }

        // Play effects
        location.getWorld().spawnParticle(Particle.SOUL, location.clone().add(0.5, 1.0, 0.5), 20, 0.2, 0.2, 0.2, 0.02);
        location.getWorld().playSound(location, Sound.BLOCK_CHEST_CLOSE, 0.3f, 0.5f);
        location.getWorld().playSound(location, Sound.ENTITY_WARDEN_NEARBY_CLOSER, 0.2f, 0.5f);
    }
    
    /**
     * Creates a screen shake effect for the player using ProtocolLib
     * @param player The player to apply the effect to
     * @param intensity The intensity of the shake (0.0-1.0)
     * @param duration Duration in ticks
     */
    public void playScreenShakeEffect(Player player, double intensity, int duration) {
        if (!plugin.getConfigManager().isAmbientSoundsEnabled() || 
            !plugin.getConfigManager().isScreenShakeEnabled()) return;
        
        // Adjust intensity based on paranoia level if available
        if (plugin.getParanoiaManager() != null) {
            // Increase intensity based on exposure
            intensity = plugin.getParanoiaManager().getModifiedEffectIntensity(player, intensity);
            
            // Record effect encounter
            plugin.getParanoiaManager().recordEncounter(player, EncounterType.AMBIENT);
        }
        
        // Clamp intensity between 0.1 and 1.0
        final double finalIntensity = Math.max(0.1, Math.min(1.0, intensity));
        
        // Create the screen shaking task
        BukkitTask shakeTask = new BukkitRunnable() {
            int ticks = 0;
            final Random random = new Random();
            
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= duration) {
                    cancel();
                    return;
                }
                
                // Calculate shake intensity (reduces over time)
                double currentIntensity = finalIntensity * (1.0 - ((double) ticks / duration));
                
                // Use the entity relative move packet instead
                // A simpler method that directly uses relative teleportation
                Location currentLoc = player.getLocation();
                
                // Random small offsets scaled by intensity
                double offsetX = (random.nextDouble() - 0.5) * 0.08 * currentIntensity;
                double offsetY = (random.nextDouble() - 0.5) * 0.04 * currentIntensity;
                double offsetZ = (random.nextDouble() - 0.5) * 0.08 * currentIntensity;
                
                // Add rotation shake occasionally
                float yawOffset = 0;
                float pitchOffset = 0;
                
                if (random.nextDouble() < 0.3) {
                    yawOffset = (float) ((random.nextFloat() - 0.5) * 2.0 * currentIntensity);
                    pitchOffset = (float) ((random.nextFloat() - 0.5) * 1.0 * currentIntensity);
                }
                
                // Teleport the player with minimal offset
                Location newLoc = currentLoc.clone().add(offsetX, offsetY, offsetZ);
                newLoc.setYaw(currentLoc.getYaw() + yawOffset);
                newLoc.setPitch(currentLoc.getPitch() + pitchOffset);
                
                // Use the teleport cause UNKNOWN to prevent server validation issues
                player.teleport(newLoc);
                
                // Immediately teleport back to maintain actual position
                // This creates a visual shake without actually moving the player
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.teleport(currentLoc);
                    }
                }, 1L);
                
                // Play subtle sound effect with the shake
                if (random.nextDouble() < 0.2) {
                    float pitch = 0.5f + random.nextFloat() * 0.5f;
                    player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_TENDRIL_CLICKS, 0.05f, pitch);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick for smooth effect
        
        // Add to active tasks to ensure cleanup
        List<BukkitTask> playerTasks = activeTasks.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        playerTasks.add(shakeTask);
        
        // Debug message
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Created screen shake effect for player " + player.getName() + 
                    " with intensity " + finalIntensity + " and duration " + duration);
        }
    }

    /**
     * Creates temporary block replacements in an area that will revert back after a set time
     * @param center The center location of the area
     * @param radius The radius of the area to affect
     * @param duration The duration in ticks before the blocks revert
     * @param intensity The intensity of the effect (0.0-1.0)
     */
    public void createTemporaryBlockReplacements(Location center, int radius, int duration, double intensity) {
        if (!plugin.getConfigManager().isStructureManipulationEnabled()) return;
        
        World world = center.getWorld();
        Random random = this.random;
        
        // Calculate actual radius to use (limited to avoid excessive changes)
        int actualRadius = Math.min(radius, 10);
        
        // Calculate how many blocks to replace based on intensity
        int maxBlocks = (int)(40 * intensity);
        
        // Track how many blocks we've changed
        int blocksChanged = 0;
        
        // Define block transformations
        Map<Material, Material> transformations = new HashMap<>();
        
        // Unsettling transformations - stone to obsidian, dirt to soul soil, etc.
        transformations.put(Material.STONE, Material.OBSIDIAN);
        transformations.put(Material.DIRT, Material.SOUL_SOIL);
        transformations.put(Material.GRASS_BLOCK, Material.MYCELIUM);
        transformations.put(Material.OAK_LOG, Material.CRIMSON_STEM);
        transformations.put(Material.BIRCH_LOG, Material.CRIMSON_STEM);
        transformations.put(Material.SPRUCE_LOG, Material.WARPED_STEM);
        transformations.put(Material.DARK_OAK_LOG, Material.WARPED_STEM);
        transformations.put(Material.OAK_PLANKS, Material.CRIMSON_PLANKS);
        transformations.put(Material.BIRCH_PLANKS, Material.CRIMSON_PLANKS);
        transformations.put(Material.SPRUCE_PLANKS, Material.WARPED_PLANKS);
        transformations.put(Material.DARK_OAK_PLANKS, Material.WARPED_PLANKS);
        transformations.put(Material.COBBLESTONE, Material.BLACKSTONE);
        transformations.put(Material.SAND, Material.SOUL_SAND);
        transformations.put(Material.WATER, Material.LAVA);  // Careful with this one!
        transformations.put(Material.OAK_LEAVES, Material.SHROOMLIGHT);
        transformations.put(Material.BIRCH_LEAVES, Material.SHROOMLIGHT);
        transformations.put(Material.SHORT_GRASS, Material.CRIMSON_ROOTS);
        transformations.put(Material.TALL_GRASS, Material.CRIMSON_FUNGUS);
        transformations.put(Material.DANDELION, Material.WITHER_ROSE);
        transformations.put(Material.POPPY, Material.WITHER_ROSE);
        
        // Create lists of block positions to potentially change
        List<Block> validBlocks = new ArrayList<>();
        
        // Scan all blocks in the area
        for (int x = center.getBlockX() - actualRadius; x <= center.getBlockX() + actualRadius; x++) {
            for (int y = center.getBlockY() - actualRadius; y <= center.getBlockY() + actualRadius; y++) {
                for (int z = center.getBlockZ() - actualRadius; z <= center.getBlockZ() + actualRadius; z++) {
                    // Check if this block is within our spherical radius
                    Location loc = new Location(world, x, y, z);
                    if (loc.distance(center) > actualRadius) continue;
                    
                    Block block = world.getBlockAt(loc);
                    
                    // Skip air blocks and blocks that aren't in our transformation map
                    if (block.getType() == Material.AIR || !transformations.containsKey(block.getType())) continue;
                    
                    // Skip blocks that have already been temporarily changed
                    if (temporaryReplacements.containsKey(block.getLocation())) continue;
                    
                    // This is a valid block for replacement
                    validBlocks.add(block);
                }
            }
        }
        
        // Shuffle the list to randomize which blocks get changed
        Collections.shuffle(validBlocks, random);
        
        // Replace blocks up to our max limit
        for (Block block : validBlocks) {
            if (blocksChanged >= maxBlocks) break;
            
            // Get the replacement material
            Material originalType = block.getType();
            Material replacementType = transformations.get(originalType);
            
            // Skip water to lava transformations if we're more than 3 blocks away from the center
            // (This is to prevent creating large lava pools that could be dangerous)
            if (originalType == Material.WATER && replacementType == Material.LAVA) {
                if (block.getLocation().distance(center) > 3) continue;
            }
            
            // Store the original state for reverting later
            BlockReplacement replacement = new BlockReplacement(
                block.getLocation(),
                block.getBlockData().clone(),
                System.currentTimeMillis() + (duration * 50) // Convert ticks to ms
            );
            
            // Change the block
            block.setType(replacementType);
            
            // Store the replacement info
            temporaryReplacements.put(block.getLocation(), replacement);
            
            // Play a subtle effect
            world.spawnParticle(Particle.SOUL, block.getLocation().add(0.5, 0.5, 0.5), 5, 0.2, 0.2, 0.2, 0.01);
            
            // Increment counter
            blocksChanged++;
        }
        
        // Start the reversion task if not already running
        if (blockReplacementTask == null || blockReplacementTask.isCancelled()) {
            blockReplacementTask = new BukkitRunnable() {
                @Override
                public void run() {
                    checkBlockReversions();
                }
            }.runTaskTimer(plugin, 20L, 20L); // Check every second
        }
        
        // Debug message
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Created " + blocksChanged + " temporary block replacements with duration " + 
                    duration + " ticks and intensity " + intensity);
        }
    }
    
    /**
     * Check for block replacements that need to be reverted
     */
    private void checkBlockReversions() {
        long currentTime = System.currentTimeMillis();
        List<Location> toRemove = new ArrayList<>();
        
        // Find all replacements that have expired
        for (Map.Entry<Location, BlockReplacement> entry : temporaryReplacements.entrySet()) {
            BlockReplacement replacement = entry.getValue();
            
            if (currentTime >= replacement.revertTime) {
                // Revert this block
                Block block = replacement.location.getBlock();
                block.setBlockData(replacement.originalData);
                
                // Play reversion effect
                block.getWorld().spawnParticle(Particle.REVERSE_PORTAL, 
                    block.getLocation().add(0.5, 0.5, 0.5), 8, 0.2, 0.2, 0.2, 0.01);
                
                // Add to removal list
                toRemove.add(entry.getKey());
            }
        }
        
        // Remove all reverted blocks from our tracking map
        for (Location loc : toRemove) {
            temporaryReplacements.remove(loc);
        }
        
        // If there are no more replacements to track, cancel the task
        if (temporaryReplacements.isEmpty() && blockReplacementTask != null) {
            blockReplacementTask.cancel();
            blockReplacementTask = null;
        }
    }
    
    /**
     * Class to track temporary block replacements
     */
    private static class BlockReplacement {
        final Location location;
        final BlockData originalData;
        final long revertTime;
        
        BlockReplacement(Location location, BlockData originalData, long revertTime) {
            this.location = location;
            this.originalData = originalData;
            this.revertTime = revertTime;
        }
    }

    /**
     * Creates flashing text effects on the player's screen that appear and disappear rapidly
     * @param player The player to show the effect to
     * @param duration The duration in ticks of the entire effect
     * @param intensity The intensity of the effect (0.0-1.0)
     */
    public void playFlashingTextEffect(Player player, int duration, double intensity) {
        if (!plugin.getConfigManager().isAmbientSoundsEnabled()) return;
        
        // Adjust intensity based on paranoia level if available
        if (plugin.getParanoiaManager() != null) {
            // Increase intensity based on exposure
            intensity = plugin.getParanoiaManager().getModifiedEffectIntensity(player, intensity);
            
            // Record effect encounter
            plugin.getParanoiaManager().recordEncounter(player, EncounterType.AMBIENT);
        }
        
        // Clamp intensity between 0.1 and 1.0
        final double finalIntensity = Math.max(0.1, Math.min(1.0, intensity));
        
        // Define creepy messages
        final String[] creepyTitles = {
            "HE WATCHES",
            "BEHIND YOU",
            "RUN",
            "HIDE",
            "I SEE YOU",
            "LEAVE",
            "NOT ALONE",
            "LOOK AWAY",
            "TOO LATE",
            "FOUND YOU"
        };
        
        final String[] creepySubtitles = {
            "he is coming closer",
            "don't turn around",
            "there's no escape",
            "you shouldn't be here",
            "the darkness follows",
            "you are being watched",
            "he knows where you are",
            "he remembers",
            "the shadows whisper",
            "your time is short"
        };
        
        // Calculate flash frequency based on intensity
        // Higher intensity = more frequent flashes
        final int minInterval = 5;  // Minimum 5 ticks between flashes (1/4 second)
        final int maxInterval = 15; // Maximum 15 ticks (3/4 second)
        final int baseInterval = maxInterval - (int)((maxInterval - minInterval) * finalIntensity);
        
        // Track active tasks for this effect
        List<BukkitTask> playerTasks = activeTasks.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        
        // Create the flashing text task
        BukkitTask flashTask = new BukkitRunnable() {
            int ticks = 0;
            int nextFlash = 0;
            boolean titleVisible = false;
            final Random random = new Random();
            
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= duration) {
                    // Clear any visible title when done
                    if (titleVisible) {
                        player.sendTitle("", "", 0, 0, 0);
                    }
                    cancel();
                    return;
                }
                
                // Time for a flash?
                if (ticks >= nextFlash) {
                    if (!titleVisible) {
                        // Show a title with random message
                        String title = creepyTitles[random.nextInt(creepyTitles.length)];
                        String subtitle = "";
                        
                        // Higher chance of subtitle at higher intensities
                        if (random.nextDouble() < finalIntensity * 0.7) {
                            subtitle = creepySubtitles[random.nextInt(creepySubtitles.length)];
                        }
                        
                        // Format the messages
                        title = formatFlashMessage(title, random);
                        subtitle = formatFlashMessage(subtitle, random);
                        
                        // Flash duration is shorter at higher intensities
                        int flashDuration = (int)(10 - (finalIntensity * 5)); // 5-10 ticks visible
                        
                        // Display the title
                        player.sendTitle(title, subtitle, 0, flashDuration, 0);
                        
                        // Randomly also send action bar message
                        if (random.nextDouble() < finalIntensity * 0.4) {
                            String actionMessage = creepySubtitles[random.nextInt(creepySubtitles.length)];
                            actionMessage = formatFlashMessage(actionMessage, random);
                            // Use an alternative approach that doesn't use deprecated methods
                            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                                new net.md_5.bungee.api.chat.TextComponent(actionMessage));
                        }
                        
                        // Play a subtle sound with the flash
                        float pitch = 0.5f + random.nextFloat() * 0.2f;
                        
                        // Choose from different unsettling sounds
                        Sound[] flashSounds = {
                            Sound.ENTITY_ENDERMAN_STARE,
                            Sound.ENTITY_WARDEN_TENDRIL_CLICKS,
                            Sound.ENTITY_GHAST_AMBIENT,
                            Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD,
                            Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM
                        };
                        
                        player.playSound(player.getLocation(), 
                            flashSounds[random.nextInt(flashSounds.length)], 
                            0.2f, pitch);
                        
                        titleVisible = true;
                        
                        // Schedule next state change
                        nextFlash = ticks + flashDuration;
                    } else {
                        // Clear the title
                        player.sendTitle("", "", 0, 0, 0);
                        titleVisible = false;
                        
                        // Schedule next flash
                        // Interval varies randomly within our calculated range
                        int randomVariation = random.nextInt(5) - 2; // -2 to +2 ticks variation
                        nextFlash = ticks + Math.max(minInterval, baseInterval + randomVariation);
                    }
                }
                
                // Every now and then, add a chat message if intensity is high
                if (finalIntensity > 0.7 && random.nextDouble() < 0.01) {
                    // Very rare whisper in chat
                    String whisper = creepySubtitles[random.nextInt(creepySubtitles.length)];
                    player.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + 
                        "<" + getRandomWhisperName(random) + "> " + whisper);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick for smooth effect
        
        playerTasks.add(flashTask);
        
        // Debug message
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Created flashing text effect for player " + player.getName() + 
                    " with intensity " + finalIntensity + " and duration " + duration);
        }
    }
    
    /**
     * Format a flash message with random styling
     */
    private String formatFlashMessage(String message, Random random) {
        if (message.isEmpty()) return message;
        
        // Choose a random format type
        int formatType = random.nextInt(5);
        
        switch (formatType) {
            case 0: // Blood red
                return ChatColor.DARK_RED + message;
            case 1: // Glitchy alternating colors
                StringBuilder glitchy = new StringBuilder();
                boolean altColor = random.nextBoolean();
                for (char c : message.toCharArray()) {
                    if (random.nextDouble() < 0.3) altColor = !altColor;
                    glitchy.append(altColor ? ChatColor.GRAY : ChatColor.WHITE).append(c);
                }
                return glitchy.toString();
            case 2: // Bold warning
                return ChatColor.RED + ChatColor.BOLD.toString() + message;
            case 3: // Whisper style
                return ChatColor.DARK_GRAY + ChatColor.ITALIC.toString() + message;
            case 4: // Corrupted text with obfuscation
                StringBuilder corrupted = new StringBuilder();
                for (char c : message.toCharArray()) {
                    if (random.nextDouble() < 0.2) {
                        corrupted.append(ChatColor.MAGIC).append(c).append(ChatColor.RESET)
                            .append(ChatColor.WHITE);
                    } else {
                        corrupted.append(ChatColor.WHITE).append(c);
                    }
                }
                return corrupted.toString();
            default:
                return ChatColor.WHITE + message;
        }
    }
    
    /**
     * Generate a randomly glitched name for whisper messages
     */
    private String getRandomWhisperName(Random random) {
        String[] baseNames = {"Him", "H3r0br1n3", "Unknown", "Watcher", "Shadow", "Void", "Whisperer"};
        String baseName = baseNames[random.nextInt(baseNames.length)];
        
        // 30% chance to glitch the name with obfuscation
        if (random.nextDouble() < 0.3) {
            StringBuilder glitched = new StringBuilder();
            for (char c : baseName.toCharArray()) {
                if (random.nextDouble() < 0.3) {
                    glitched.append(ChatColor.MAGIC).append(c).append(ChatColor.RESET)
                        .append(ChatColor.DARK_GRAY).append(ChatColor.ITALIC);
                } else {
                    glitched.append(c);
                }
            }
            return glitched.toString();
        }
        
        return baseName;
    }
    
    /**
     * Displays a fake system error message in chat
     * @param player The player to send the message to
     */
    public void displayFakeErrorMessage(Player player) {
        Random random = this.random;
        
        // Decide error type
        int errorType = random.nextInt(5);
        String message;
        
        switch (errorType) {
            case 0: // Connection error
                message = ChatColor.RED + "[CONNECTION ERROR] " + ChatColor.GRAY + 
                        "Connection interrupted by an unknown entity. Reconnecting...";
                break;
                
            case 1: // File corruption
                String[] fileNames = {
                    "player.dat", 
                    "level.dat", 
                    "world/region/r." + random.nextInt(10) + "." + random.nextInt(10) + ".mca",
                    "playerdata/" + player.getUniqueId().toString().substring(0, 8) + ".dat",
                    "herobrine_data.bin"
                };
                message = ChatColor.RED + "[FILE ERROR] " + ChatColor.GRAY + 
                        "File " + fileNames[random.nextInt(fileNames.length)] + 
                        " appears to be corrupted. Data may be lost.";
                break;
                
            case 2: // Memory issue
                message = ChatColor.RED + "[MEMORY WARNING] " + ChatColor.GRAY + 
                        "Unexpected memory access at address 0x" + 
                        Integer.toHexString(random.nextInt(16777215)) + 
                        ". Unknown process detected.";
                break;
                
            case 3: // Paranormal activity
                message = ChatColor.RED + "[SYSTEM WARNING] " + ChatColor.GRAY + 
                        "Unusual activity detected in world '" + player.getWorld().getName() + 
                        "'. Chunk data verification failed.";
                break;
                
            case 4: // Entity error
                message = ChatColor.RED + "[ENTITY ERROR] " + ChatColor.GRAY + 
                        "Failed to load entity data. Encountered unknown entity type 'h̴͇̯̳̪̭̙̣̎͛̌̔͋ͦ̊e̷͚̬̙̗̩̻̽ͭ̑ͪr̦̻̈́ͦ̃̓͂̌ͫo̧̫̝̠̰̗͈̝͚͌͐ͬ̾̏̒b̴͚̤̱̖̙̺̭̩̅͐́r̦̬̙̘̫̫̺̓̌͝i̥̣̞̭̲̫̻̓ͬ͘n̰̭̖̝̼̦̣̯̆̍e̱̬̘͕̬̼̰ͧ̑̍̈ͭ̉́͜'";
                // Use string that includes some Unicode corruption for effect
                break;
                
            default:
                message = ChatColor.RED + "[ERROR] " + ChatColor.GRAY + 
                        "Unknown system error occurred. Please contact server administrator.";
                break;
        }
        
        // Send the message with a delay to make it more believable
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage(message);
            
            // Add a follow-up glitchy message sometimes
            if (random.nextDouble() < 0.3) {
                final StringBuilder followUpBuilder = new StringBuilder(ChatColor.DARK_GRAY + "");
                
                // Add some "glitchy" characters
                for (int i = 0; i < 3 + random.nextInt(5); i++) {
                    if (random.nextBoolean()) {
                        followUpBuilder.append(ChatColor.MAGIC)
                                      .append("abcdef".charAt(random.nextInt(6)))
                                      .append(ChatColor.RESET)
                                      .append("").append(ChatColor.DARK_GRAY);
                    } else {
                        followUpBuilder.append(".¿?!@#$%^&*".charAt(random.nextInt(10)));
                    }
                }
                
                // Get the final string for use in lambda
                final String followUp = followUpBuilder.toString();
                
                // Wait a short time and send the follow-up
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendMessage(followUp);
                }, 5 + random.nextInt(10));
            }
        }, 30 + random.nextInt(50)); // Random delay between 1.5-4 seconds
    }

    /**
     * Temporarily teleports a player to a maze-like structure and returns them after a set duration
     * @param player The player to teleport
     * @param duration Duration in ticks before returning the player (20 ticks = 1 second)
     * @param intensity The intensity of the effect (0.0-1.0), affecting maze complexity and atmosphere
     */
    public void performMazeTeleportEvent(Player player, int duration, double intensity) {
        if (!plugin.getConfigManager().isEnabled()) return;
        
        // Limit duration to 30 seconds max (600 ticks)
        final int finalDuration = Math.min(duration, 600);
        
        // Store the player's original location for return teleport
        final Location originalLocation = player.getLocation().clone();
        
        // Adjust intensity based on paranoia level if available
        if (plugin.getParanoiaManager() != null) {
            // Increase intensity based on exposure
            intensity = plugin.getParanoiaManager().getModifiedEffectIntensity(player, intensity);
            
            // Record effect encounter
            plugin.getParanoiaManager().recordEncounter(player, EncounterType.DIRECT);
        }
        
        // Clamp intensity between 0.1 and 1.0
        final double finalIntensity = Math.max(0.1, Math.min(1.0, intensity));
        
        // Create "teleportation in progress" effect
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 0.8f);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 100, 0.5, 1, 0.5, 0.5);
        
        // Show warning title
        player.sendTitle(
            ChatColor.DARK_RED + "YOU ARE BEING TAKEN", 
            ChatColor.GRAY + "The maze awaits...", 
            5, 40, 5
        );
        
        // Add heavy screen shake
        playScreenShakeEffect(player, 0.8, 40); // high intensity, 2 second duration
        
        // Schedule teleportation
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            // Begin teleportation sequence
            Location mazeLocation = selectMazeLocation(player.getWorld(), finalIntensity);
            if (mazeLocation == null) {
                // Fallback if no maze location can be found
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().warning("[DEBUG] Could not find a suitable maze location, aborting teleportation");
                }
                return;
            }
            
            // Start fog effect before teleport
            if (plugin.getConfigManager().isFogEnabled()) {
                createFogEffect(player, player.getLocation());
            }
            
            // Teleport the player to the maze
            player.teleport(mazeLocation);
            
            // Play arrival effects
            player.playSound(mazeLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            player.getWorld().spawnParticle(Particle.SOUL, mazeLocation, 40, 0.5, 1, 0.5, 0.1);
            
            // Add some atmosphere to the maze
            addMazeAtmosphere(player, mazeLocation, finalIntensity);
            
            // Flashing text for high intensity encounters
            if (finalIntensity > 0.6) {
                playFlashingTextEffect(player, Math.min(finalDuration - 60, 100), finalIntensity * 0.7);
            }
            
            // Track this teleportation
            final UUID playerId = player.getUniqueId();
            
            // Schedule return teleportation - GUARANTEED to happen within 30 seconds maximum
            BukkitTask returnTask = new BukkitRunnable() {
                @Override
                public void run() {
                    Player targetPlayer = Bukkit.getPlayer(playerId);
                    if (targetPlayer == null || !targetPlayer.isOnline()) return;
                    
                    // Warning before return
                    targetPlayer.sendTitle(
                        ChatColor.DARK_GRAY + "THE MAZE RELEASES YOU", 
                        ChatColor.GRAY + "For now...", 
                        5, 20, 5
                    );
                    
                    // Return teleport effects
                    targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                    targetPlayer.getWorld().spawnParticle(Particle.PORTAL, targetPlayer.getLocation(), 100, 0.5, 1, 0.5, 0.5);
                    
                    // Medium screen shake for return
                    playScreenShakeEffect(targetPlayer, 0.5, 30);
                    
                    // Teleport back to original location after a brief delay
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        // Double check player is still online
                        if (targetPlayer != null && targetPlayer.isOnline()) {
                            targetPlayer.teleport(originalLocation);
                            
                            // Clear any remaining effects
                            targetPlayer.removePotionEffect(PotionEffectType.BLINDNESS);
                            targetPlayer.removePotionEffect(PotionEffectType.NAUSEA);
                            
                            // Final return effect
                            targetPlayer.playSound(originalLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.7f);
                            targetPlayer.getWorld().spawnParticle(Particle.REVERSE_PORTAL, 
                                originalLocation, 50, 0.5, 1, 0.5, 0.2);
                            
                            // Randomly show error message after return for added effect
                            if (random.nextDouble() < 0.3) {
                                displayFakeErrorMessage(targetPlayer);
                            }
                        }
                    }, 20L); // 1 second delay for return teleport
                }
            // Return within 15-30 seconds (300-600 ticks) based on intensity
            }.runTaskLater(plugin, 300 + (int)(300 * finalIntensity)); // Higher intensity = longer stay (up to 30 seconds)
            
            // Add to active tasks to ensure cleanup
            List<BukkitTask> playerTasks = activeTasks.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
            playerTasks.add(returnTask);
            
            // Debug message
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("[DEBUG] Created maze teleportation event for player " + player.getName() + 
                        " with intensity " + finalIntensity + " and duration " + finalDuration);
            }
        }, 60L); // 3 second delay before teleport
    }
    
    /**
     * Adds atmospheric effects to make the maze experience more unsettling
     * @param player The player in the maze
     * @param mazeLocation The central maze location
     * @param intensity The intensity level (0.0-1.0)
     */
    private void addMazeAtmosphere(Player player, Location mazeLocation, double intensity) {
        // Apply disorienting effects based on intensity
        if (intensity > 0.3) {
            // Add confusion/nausea for higher intensities
            if (intensity > 0.7) {
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.NAUSEA, 
                    (int)(100 * intensity), 
                    0, false, false));
            }
            
            // Make surroundings darker based on intensity
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS, 
                (int)(80 * intensity), 
                0, false, false));
        }
        
        // Create ambient sound task
        BukkitTask ambientTask = new BukkitRunnable() {
            int ticks = 0;
            final int MAX_TICKS = 400; // Maximum 20 seconds of ambience
            final Random random = new Random();
            
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= MAX_TICKS) {
                    cancel();
                    return;
                }
                
                // Play ambient sounds
                if (random.nextDouble() < 0.3) {
                    Sound[] mazeSounds = {
                        Sound.ENTITY_WARDEN_NEARBY_CLOSER,
                        Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD,
                        Sound.ENTITY_ENDERMAN_AMBIENT,
                        Sound.BLOCK_ANCIENT_DEBRIS_BREAK,
                        Sound.AMBIENT_CAVE
                    };
                    
                    // Randomize sound location for disorientation
                    Location soundLoc = player.getLocation().add(
                        (random.nextDouble() - 0.5) * 10,
                        (random.nextDouble() - 0.5) * 5,
                        (random.nextDouble() - 0.5) * 10
                    );
                    
                    float volume = 0.3f + (float)(random.nextDouble() * 0.3);
                    float pitch = 0.5f + (float)(random.nextDouble() * 0.5);
                    
                    player.playSound(soundLoc, mazeSounds[random.nextInt(mazeSounds.length)], volume, pitch);
                }
                
                // Spawn occasional particles
                if (random.nextDouble() < 0.2) {
                    Location particleLoc = player.getLocation().add(
                        (random.nextDouble() - 0.5) * 5,
                        random.nextDouble() * 2,
                        (random.nextDouble() - 0.5) * 5
                    );
                    
                    player.spawnParticle(Particle.SOUL, particleLoc, 3, 0.3, 0.3, 0.3, 0.01);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 20L, 10L);
        
        // Add to active tasks to ensure cleanup
        List<BukkitTask> playerTasks = activeTasks.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        playerTasks.add(ambientTask);
        
        // Whispered voice messages - disabled to reduce chat spam
        /*
        if (random.nextDouble() < 0.3) {
            String[] whispers = {
                "lost...",
                "trapped...",
                "no escape...",
                "forever here...",
                "watching you...",
                "follow the signs...",
                "deeper...",
                "I'm waiting...",
                "find me..."
            };
            
            player.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC +
                "<" + getRandomWhisperName(random) + "> " + whispers[random.nextInt(whispers.length)]);
        }
        */
    }
    
    /**
     * Selects a suitable location for a maze based on world and intensity
     * @param world The player's world
     * @param intensity The intensity factor
     * @return A location within a maze-like structure, or null if none available
     */
    private Location selectMazeLocation(World world, double intensity) {
        // Instead of trying to create a maze in a different dimension,
        // create it in the player's current world to avoid cross-dimension errors
        
        Random random = this.random;
        
        try {
            // Always create the maze in the player's current world
            World mazeWorld = world;
            
            // Create a "maze-like" location not too far from the center
            // Use smaller distances to ensure the maze loads well
            int x = random.nextInt(100) - 50; // Random x between -50 and 50
            int z = random.nextInt(100) - 50; // Random z between -50 and 50
            
            // Y position is more controlled to ensure a reasonable height
            int y;
            
            if (mazeWorld.getEnvironment() == World.Environment.NETHER) {
                // In the nether, find a suitable y coordinate (avoid open lava areas)
                y = 40 + random.nextInt(30); // Between y=40 and y=70
            } else {
                // In other worlds, try to find ground level
                int groundY = mazeWorld.getHighestBlockYAt(x, z);
                y = groundY + 1; // 1 block above the highest block
            }
            
            Location mazeLocation = new Location(mazeWorld, x, y, z);
            
            // Make sure the area is safe (air blocks for the player)
            if (mazeLocation.getBlock().getType() != Material.AIR && 
                mazeLocation.getBlock().getType() != Material.CAVE_AIR) {
                // Try to find air blocks nearby
                for (int searchY = y; searchY < y + 10; searchY++) {
                    Location testLoc = new Location(mazeWorld, x, searchY, z);
                    if (testLoc.getBlock().getType() == Material.AIR || 
                        testLoc.getBlock().getType() == Material.CAVE_AIR) {
                        mazeLocation.setY(searchY);
                        break;
                    }
                }
            }
            
            // Ensure there's a safe platform to stand on
            Location floorLocation = mazeLocation.clone().add(0, -1, 0);
            if (floorLocation.getBlock().getType() == Material.AIR || 
                floorLocation.getBlock().getType() == Material.CAVE_AIR || 
                floorLocation.getBlock().getType() == Material.LAVA) {
                floorLocation.getBlock().setType(Material.BLACKSTONE);
            }
            
            // Add some blocks around to create a maze-like feel
            createSimpleMazeStructure(mazeLocation, intensity);
            
            return mazeLocation;
            
        } catch (Exception e) {
            // Log any errors and return null
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().warning("[DEBUG] Error selecting maze location: " + e.getMessage());
            }
            return null;
        }
    }
    
    /**
     * Creates a simple maze-like structure at the given location
     * @param center The center location of the maze
     * @param intensity The intensity factor (affects complexity)
     */
    private void createSimpleMazeStructure(Location center, double intensity) {
        World world = center.getWorld();
        Random random = this.random;
        
        // Calculate maze size based on intensity (5-10 blocks radius)
        int size = 5 + (int)(5 * intensity);
        
        // Materials to use based on world environment
        Material wallMaterial;
        Material floorMaterial;
        Material ceilingMaterial;
        
        if (world.getEnvironment() == World.Environment.NETHER) {
            wallMaterial = Material.BLACKSTONE;
            floorMaterial = Material.SOUL_SOIL;
            ceilingMaterial = Material.NETHER_BRICKS;
        } else if (world.getEnvironment() == World.Environment.THE_END) {
            wallMaterial = Material.PURPUR_BLOCK;
            floorMaterial = Material.END_STONE;
            ceilingMaterial = Material.PURPUR_BLOCK;
        } else {
            wallMaterial = Material.COBBLESTONE;
            floorMaterial = Material.MOSS_BLOCK;
            ceilingMaterial = Material.DEEPSLATE;
        }
        
        // Map to track temporary block replacements
        Map<Location, BlockReplacement> mazeReplacements = new HashMap<>();
        
        // Create a proper maze structure using a grid-based pattern
        // We'll use a simple algorithm that creates corridors with walls
        
        // First, create a solid platform as the floor
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                Location loc = center.clone().add(x, -1, z);
                
                // Store original block data for later reversion
                BlockReplacement replacement = new BlockReplacement(
                    loc,
                    loc.getBlock().getBlockData().clone(),
                    System.currentTimeMillis() + (20 * 60 * 1000) // 20 minute failsafe
                );
                
                // Change the block
                loc.getBlock().setType(floorMaterial);
                
                // Store the replacement
                temporaryReplacements.put(loc, replacement);
                mazeReplacements.put(loc, replacement);
            }
        }
        
        // Create a grid-based maze layout
        // We'll start with solid walls and then carve passages
        boolean[][] isWall = new boolean[size*2+1][size*2+1];
        
        // Initialize all cells as walls (except the center area for the player)
        for (int x = 0; x < size*2+1; x++) {
            for (int z = 0; z < size*2+1; z++) {
                // Leave the center area open (3x3 blocks)
                if (Math.abs(x - size) <= 1 && Math.abs(z - size) <= 1) {
                    isWall[x][z] = false;
                } else {
                    isWall[x][z] = true;
                }
            }
        }
        
        // Create some passages in the maze
        int passages = 10 + (int)(20 * intensity); // More intensity = more complex maze
        for (int i = 0; i < passages; i++) {
            // Pick a random starting point (but not too close to center or edges)
            int startX = 2 + random.nextInt(size*2-3);
            int startZ = 2 + random.nextInt(size*2-3);
            
            // Create a passage from this point in a random direction
            int length = 2 + random.nextInt(4);
            int direction = random.nextInt(4); // 0=north, 1=east, 2=south, 3=west
            
            // Carve the passage by setting cells to not be walls
            for (int step = 0; step < length; step++) {
                // Don't carve too close to the edge
                if (startX < 1 || startX >= size*2 || startZ < 1 || startZ >= size*2) {
                    break;
                }
                
                isWall[startX][startZ] = false;
                
                // Move to next cell based on direction
                switch (direction) {
                    case 0: startZ--; break; // North
                    case 1: startX++; break; // East
                    case 2: startZ++; break; // South
                    case 3: startX--; break; // West
                }
            }
        }
        
        // Ensure there's a path from center to outside (don't trap the player)
        // Create at least one guaranteed exit corridor
        int exitDirection = random.nextInt(4);
        int exitX = size;
        int exitZ = size;
        int exitLength = size - 2; // Leave a few blocks from edge
        
        for (int step = 0; step < exitLength; step++) {
            switch (exitDirection) {
                case 0: exitZ--; break; // North
                case 1: exitX++; break; // East
                case 2: exitZ++; break; // South
                case 3: exitX--; break; // West
            }
            isWall[exitX][exitZ] = false;
        }
        
        // Now place the blocks according to our maze plan
        for (int x = 0; x < size*2+1; x++) {
            for (int z = 0; z < size*2+1; z++) {
                if (isWall[x][z]) {
                    // Create wall from floor to ceiling
                    for (int y = 0; y <= 3; y++) {
                        Location loc = center.clone().add(x - size, y, z - size);
                        
                        // Skip if already a solid block
                        if (loc.getBlock().getType().isSolid()) continue;
                        
                        // Store original block data
                        BlockReplacement replacement = new BlockReplacement(
                            loc,
                            loc.getBlock().getBlockData().clone(),
                            System.currentTimeMillis() + (20 * 60 * 1000) // 20 minute failsafe
                        );
                        
                        // Different material for top block to make it more interesting
                        Material material = (y == 3) ? ceilingMaterial : wallMaterial;
                        
                        // Change the block
                        loc.getBlock().setType(material);
                        
                        // Store the replacement
                        temporaryReplacements.put(loc, replacement);
                        mazeReplacements.put(loc, replacement);
                    }
                } else {
                    // Place ceiling over corridors
                    Location ceilingLoc = center.clone().add(x - size, 3, z - size);
                    
                    // Skip if already a solid block
                    if (ceilingLoc.getBlock().getType().isSolid()) continue;
                    
                    // Store original block data
                    BlockReplacement replacement = new BlockReplacement(
                        ceilingLoc,
                        ceilingLoc.getBlock().getBlockData().clone(),
                        System.currentTimeMillis() + (20 * 60 * 1000) // 20 minute failsafe
                    );
                    
                    // Change the block
                    ceilingLoc.getBlock().setType(ceilingMaterial);
                    
                    // Store the replacement
                    temporaryReplacements.put(ceilingLoc, replacement);
                    mazeReplacements.put(ceilingLoc, replacement);
                }
            }
        }
        
        // Add some atmosphere and decorations
        addMazeDecorations(center, size, mazeReplacements, isWall, wallMaterial);
        
        // Make sure the blockReplacementTask is running to handle cleanup
        if (blockReplacementTask == null || blockReplacementTask.isCancelled()) {
            blockReplacementTask = new BukkitRunnable() {
                @Override
                public void run() {
                    checkBlockReversions();
                }
            }.runTaskTimer(plugin, 20L, 20L); // Check every second
        }
        
        // Debug message
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Created temporary maze structure with " + 
                    mazeReplacements.size() + " blocks at location " + 
                    center.getWorld().getName() + " [" + center.getBlockX() + "," + 
                    center.getBlockY() + "," + center.getBlockZ() + "]");
        }
    }
    
    /**
     * Adds decorative elements to the maze
     */
    private void addMazeDecorations(Location center, int size, Map<Location, BlockReplacement> mazeReplacements, boolean[][] isWall, Material wallMaterial) {
        Random random = this.random;
        
        // Add some atmospheric elements
        for (int x = 0; x < size*2+1; x++) {
            for (int z = 0; z < size*2+1; z++) {
                // Only add decorations in corridor spaces
                if (!isWall[x][z]) {
                    // 10% chance for a decoration at each valid spot
                    if (random.nextDouble() < 0.1) {
                        Location decorLoc = center.clone().add(x - size, 0, z - size);
                        
                        // Store original block data
                        BlockReplacement replacement = new BlockReplacement(
                            decorLoc,
                            decorLoc.getBlock().getBlockData().clone(),
                            System.currentTimeMillis() + (20 * 60 * 1000) // 20 minute failsafe
                        );
                        
                        // Choose a decoration type
                        int decorType = random.nextInt(5);
                        switch (decorType) {
                            case 0: // Soul fire
                                decorLoc.getBlock().setType(Material.SOUL_FIRE);
                                break;
                            case 1: // Soul lantern
                                decorLoc.getBlock().setType(Material.SOUL_LANTERN);
                                break;
                            case 2: // Cobweb
                                decorLoc.getBlock().setType(Material.COBWEB);
                                break;
                            case 3: // Redstone torch
                                decorLoc.getBlock().setType(Material.REDSTONE_TORCH);
                                break;
                            case 4: // Skull
                                if (random.nextBoolean()) {
                                    decorLoc.getBlock().setType(Material.SKELETON_SKULL);
                                } else {
                                    decorLoc.getBlock().setType(Material.WITHER_SKELETON_SKULL);
                                }
                                break;
                        }
                        
                        // Store the replacement
                        temporaryReplacements.put(decorLoc, replacement);
                        mazeReplacements.put(decorLoc, replacement);
                    }
                }
            }
        }
    }
    
    /**
     * Initiates the Skinwalker Effect, making nearby animals stare at a player and potentially attack
     * @param player The player to target
     * @param duration Duration in ticks for the effect (20 ticks = 1 second)
     * @param intensity Intensity level (0.0-1.0) affecting behavior severity
     * @param mode 0=just stare, 1=attack after staring, 2=attack only if provoked
     */
    public void createSkinwalkerEffect(Player player, int duration, double intensity, int mode) {
        if (!plugin.getConfigManager().isEnabled()) return;
        
        // Adjust intensity based on paranoia level if available
        if (plugin.getParanoiaManager() != null) {
            // Increase intensity based on exposure
            intensity = plugin.getParanoiaManager().getModifiedEffectIntensity(player, intensity);
            
            // Record effect encounter
            plugin.getParanoiaManager().recordEncounter(player, EncounterType.AMBIENT);
        }
        
        // Clamp intensity between 0.1 and 1.0
        final double finalIntensity = Math.max(0.1, Math.min(1.0, intensity));
        
        // Find nearby passive animals
        List<Entity> nearbyAnimals = findNearbyPassiveAnimals(player, 20); // 20 block radius
        
        // Exit if no animals found
        if (nearbyAnimals.isEmpty()) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("[DEBUG] No suitable animals found for skinwalker effect");
            }
            return;
        }
        
        // Limit the number of affected animals based on intensity (1-5)
        int maxAffected = 1 + (int)(4 * finalIntensity);
        
        // Shuffle and limit the list
        Collections.shuffle(nearbyAnimals, random);
        if (nearbyAnimals.size() > maxAffected) {
            nearbyAnimals = nearbyAnimals.subList(0, maxAffected);
        }
        
        // Track affected entities for cleanup
        final List<UUID> affectedEntities = new ArrayList<>();
        
        // Create staring behavior for each animal
        for (Entity entity : nearbyAnimals) {
            if (entity instanceof LivingEntity) {
                LivingEntity animal = (LivingEntity) entity;
                
                // Start staring behavior
                makeEntityStare(animal, player);
                
                // Track this entity
                affectedEntities.add(animal.getUniqueId());
                
                // Add visual indication of possession
                animal.getWorld().spawnParticle(
                    Particle.SOUL, 
                    animal.getEyeLocation(), 
                    5, 0.1, 0.1, 0.1, 0.01
                );
                
                // Play subtle effect
                animal.getWorld().playSound(
                    animal.getLocation(),
                    Sound.ENTITY_ENDERMAN_STARE, 
                    0.3f, 
                    0.5f + random.nextFloat() * 0.3f
                );
                
                // Debug message
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("[DEBUG] Applied skinwalker effect to " + 
                            animal.getType().toString() + " at " + animal.getLocation());
                }
            }
        }
        
        // Save these variables for the tasks
        final int effectMode = mode;
        final double effectIntensity = finalIntensity;
        
        // Create task to maintain staring behavior
        BukkitTask staringTask = new BukkitRunnable() {
            int ticks = 0;
            boolean attackTriggered = false;
            final Map<UUID, Boolean> attackedByPlayer = new HashMap<>(); // Track if player attacked each entity
            
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= duration) {
                    // Duration exceeded, end the effect
                    cleanupSkinwalkerEffect(affectedEntities, effectMode);
                    cancel();
                    return;
                }
                
                // Update staring for each entity and check player interaction
                for (UUID entityId : affectedEntities) {
                    Entity entity = Bukkit.getEntity(entityId);
                    if (entity == null || !(entity instanceof LivingEntity) || entity.isDead()) {
                        continue;
                    }
                    
                    LivingEntity animal = (LivingEntity) entity;
                    
                    // Make sure it keeps staring
                    makeEntityStare(animal, player);
                    
                    // Add occasional creepy effect
                    if (random.nextDouble() < 0.05) {
                        animal.getWorld().spawnParticle(
                            Particle.SOUL, 
                            animal.getEyeLocation(), 
                            2, 0.1, 0.1, 0.1, 0.01
                        );
                        
                        // Add a soul fire/flame effect instead of red particles
                        animal.getWorld().spawnParticle(
                            Particle.SOUL_FIRE_FLAME, 
                            animal.getEyeLocation(), 
                            2, 0.05, 0.05, 0.05, 0.01
                        );
                    }
                    
                    // Check attack conditions based on mode
                    if (effectMode == 1 && !attackTriggered) {
                        // Mode 1: Attack after staring for a while
                        if (ticks > duration * 0.7) { // Attack in last 30% of duration
                            attackTriggered = true;
                            makeEntitiesAttack(affectedEntities, player, effectIntensity);
                        }
                    } else if (effectMode == 2) {
                        // Mode 2: Attack only if player attacks first
                        if (!attackedByPlayer.containsKey(entityId)) {
                            attackedByPlayer.put(entityId, false);
                        }
                        
                        if (!attackedByPlayer.get(entityId) && wasAttackedByPlayer(animal, player)) {
                            attackedByPlayer.put(entityId, true);
                            makeEntityAttack(animal, player, effectIntensity);
                        }
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 5L, 5L); // Update every 1/4 second
        
        // Add to active tasks for cleanup
        List<BukkitTask> playerTasks = activeTasks.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        playerTasks.add(staringTask);
        
        // Debug message
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Started skinwalker effect for player " + player.getName() + 
                    " with " + nearbyAnimals.size() + " animals, mode " + mode + 
                    ", intensity " + finalIntensity + ", duration " + duration);
        }
    }
    
    /**
     * Makes an entity stare continuously at a player
     * @param entity The entity to make stare
     * @param player The player to stare at
     */
    private void makeEntityStare(LivingEntity entity, Player player) {
        // Get entity type to handle differently based on animal
        EntityType type = entity.getType();
        
        // Reduce movement/speed - we want them to stare mostly still
        if (entity.hasAI()) {
            // For animals that should freeze completely
            if (type == EntityType.SHEEP || type == EntityType.COW || 
                type == EntityType.PIG || type == EntityType.CHICKEN) {
                entity.setAI(false);
            } else {
                // For animals that should move very slowly
                // Can't remove AI completely for animals like wolves that need AI to target
                if (entity instanceof Creature) {
                    Creature creature = (Creature) entity;
                    
                    // Clear any current path
                    if (creature.getTarget() != player) {
                        creature.setTarget(null);
                    }
                }
            }
        }
        
        // Make entity look at player (head turning)
        Location playerLoc = player.getLocation();
        Location entityLoc = entity.getLocation();
        
        // Calculate direction vector from entity to player
        double dx = playerLoc.getX() - entityLoc.getX();
        double dz = playerLoc.getZ() - entityLoc.getZ();
        
        // Calculate yaw
        double yaw = Math.atan2(-dx, dz) * (180 / Math.PI);
        
        // Calculate pitch
        double distanceXZ = Math.sqrt(dx * dx + dz * dz);
        double dy = playerLoc.getY() + 1 - entityLoc.getY(); // +1 to look at player's head
        double pitch = -Math.atan(dy / distanceXZ) * (180 / Math.PI);
        
        // Set the entity's rotation
        Location newLoc = entity.getLocation();
        newLoc.setYaw((float) yaw);
        newLoc.setPitch((float) pitch);
        entity.teleport(newLoc);
    }
    
    /**
     * Makes all entities in list attack the player
     * @param entityIds UUIDs of entities to make attack
     * @param player The player to attack
     * @param intensity Intensity of the attack behavior
     */
    private void makeEntitiesAttack(List<UUID> entityIds, Player player, double intensity) {
        for (UUID entityId : entityIds) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity instanceof LivingEntity && !entity.isDead()) {
                makeEntityAttack((LivingEntity) entity, player, intensity);
            }
        }
        
        // Play a warning sound to the player
        player.playSound(player.getLocation(), Sound.ENTITY_WOLF_GROWL, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.7f, 0.7f);
        
        // Show warning title based on intensity
        if (intensity > 0.6) {
            player.sendTitle(
                ChatColor.DARK_RED + "THEY TURN ON YOU", 
                ChatColor.GRAY + "The animals sense your fear...", 
                5, 40, 10
            );
        }
    }
    
    /**
     * Makes an entity attack the player with unnatural behavior
     * @param entity The entity to modify
     * @param player The player to attack
     * @param intensity Intensity of the attack behavior
     */
    private void makeEntityAttack(LivingEntity entity, Player player, double intensity) {
        // Don't affect already aggressive mobs
        if (entity instanceof Monster) return;
        
        // Re-enable AI if it was disabled
        if (!entity.hasAI()) {
            entity.setAI(true);
        }
        
        // Make the entity move faster when attacking
        entity.addPotionEffect(new PotionEffect(
            PotionEffectType.SPEED, 
            400, // 20 seconds
            1,   // Speed II
            false, false
        ));
        
        // Try to make entity stronger - alternative approach using a custom attribute modifier
        // would be better but requires more complex implementation
        
        // For high intensity, add more dangerous effects
        if (intensity > 0.7) {
            entity.addPotionEffect(new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE, 
                400, // 20 seconds
                0,
                false, false
            ));
        }
        
        // Set target if possible
        if (entity instanceof Creature) {
            Creature creature = (Creature) entity;
            creature.setTarget(player);
        }
        
        // Visual effect for transformation
        entity.getWorld().spawnParticle(
            Particle.SOUL_FIRE_FLAME, 
            entity.getLocation().add(0, 0.5, 0), 
            15, 0.2, 0.4, 0.2, 0.01
        );
        
        // Scary sound effect
        entity.getWorld().playSound(
            entity.getLocation(),
            Sound.ENTITY_RAVAGER_ROAR, 
            0.5f, 
            1.2f
        );
    }
    
    /**
     * Determines if an entity was attacked by the specified player recently
     * @param entity The entity to check
     * @param player The player to check as attacker
     * @return True if the player attacked this entity
     */
    private boolean wasAttackedByPlayer(LivingEntity entity, Player player) {
        // Check if entity's last damage cause was from the player
        EntityDamageEvent lastDamage = entity.getLastDamageCause();
        
        if (lastDamage != null && lastDamage instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageByEntity = (EntityDamageByEntityEvent) lastDamage;
            Entity damager = damageByEntity.getDamager();
            
            // Check if damage was directly from the player or indirectly (arrow, etc.)
            if (damager instanceof Player && damager.equals(player)) {
                return true;
            } else if (damager instanceof Projectile) {
                Projectile projectile = (Projectile) damager;
                if (projectile.getShooter() instanceof Player && 
                    ((Player)projectile.getShooter()).equals(player)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Finds passive animals around a player
     * @param player The player to search around
     * @param radius The search radius in blocks
     * @return List of passive animal entities
     */
    private List<Entity> findNearbyPassiveAnimals(Player player, int radius) {
        List<Entity> result = new ArrayList<>();
        
        // Get all nearby entities
        Collection<Entity> nearbyEntities = player.getWorld().getNearbyEntities(
            player.getLocation(), radius, radius, radius, 
            entity -> isAcceptableAnimal(entity)
        );
        
        result.addAll(nearbyEntities);
        return result;
    }
    
    /**
     * Checks if an entity is a suitable animal for the skinwalker effect
     * @param entity The entity to check
     * @return True if the entity can be used for the skinwalker effect
     */
    private boolean isAcceptableAnimal(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        
        // Only affect certain types of animals
        EntityType type = entity.getType();
        
        return type == EntityType.SHEEP || 
               type == EntityType.COW || 
               type == EntityType.PIG || 
               type == EntityType.CHICKEN || 
               type == EntityType.WOLF || 
               type == EntityType.CAT || 
               type == EntityType.FOX || 
               type == EntityType.RABBIT || 
               type == EntityType.HORSE || 
               type == EntityType.DONKEY || 
               type == EntityType.LLAMA;
    }
    
    /**
     * Cleans up after skinwalker effect, restoring normal behavior
     * @param entityIds UUIDs of affected entities
     * @param mode The mode that was used
     */
    private void cleanupSkinwalkerEffect(List<UUID> entityIds, int mode) {
        for (UUID entityId : entityIds) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity instanceof LivingEntity && !entity.isDead()) {
                LivingEntity animal = (LivingEntity) entity;
                
                // Re-enable AI if it was disabled
                if (!animal.hasAI()) {
                    animal.setAI(true);
                }
                
                // Clear targeting if applicable
                if (animal instanceof Creature) {
                    ((Creature) animal).setTarget(null);
                }
                
                // Remove any effects applied
                animal.removePotionEffect(PotionEffectType.SPEED);
                animal.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
                
                // Small return-to-normal effect
                animal.getWorld().spawnParticle(
                    Particle.END_ROD, 
                    animal.getLocation().add(0, 0.5, 0), 
                    5, 0.2, 0.4, 0.2, 0.01
                );
            }
        }
    }
}