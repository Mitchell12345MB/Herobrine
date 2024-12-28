package com.sausaliens.herobrine.managers;

import com.sausaliens.herobrine.HerobrinePlugin;
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
        
        // Add fog effect
        if (plugin.getConfigManager().isFogEnabled()) {
            createFogEffect(player, location);
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
                if (random.nextDouble() < 0.3) { // 30% chance to flicker
                    Location playerLoc = player.getLocation();
                    player.playSound(playerLoc, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.2f, 0.5f);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
        playerTasks.add(flickerTask);

        // Ambient sounds task with randomized timing
        BukkitTask soundTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
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
        }.runTaskTimer(plugin, 40L, 30L + random.nextInt(60));
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
        
        // Unsettling particle effects
        location.getWorld().spawnParticle(Particle.SMOKE, location, 3, 0.2, 0.2, 0.2, 0.01);
        if (random.nextDouble() < 0.2) {
            location.getWorld().spawnParticle(Particle.SOUL, location, 1, 0.1, 0.1, 0.1, 0.01);
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
    }

    public void startEffects() {
        if (effectTask != null) {
            effectTask.cancel();
        }
        
        effectTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getConfigManager().isEnabled()) {
                return;
            }
            
            // Ambient effects for all players
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (Math.random() < 0.1) { // 10% chance per player
                    playRandomAmbientEffect(player);
                }
            }
        }, 100L, 100L); // Check every 5 seconds
    }

    public void stopEffects() {
        if (effectTask != null) {
            effectTask.cancel();
            effectTask = null;
        }
        
        // Stop effects for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            stopEffects(player);
        }
    }

    private void playRandomAmbientEffect(Player player) {
        if (Math.random() < 0.5) {
            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);
        } else {
            Location loc = player.getLocation().add(
                (Math.random() - 0.5) * 20,
                (Math.random() - 0.5) * 10,
                (Math.random() - 0.5) * 20
            );
            player.spawnParticle(Particle.CLOUD, loc, 10, 0.5, 0.5, 0.5, 0.01);
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

        double density = plugin.getConfigManager().getFogDensity();
        int duration = plugin.getConfigManager().getFogDuration();

        // Calculate effect amplifiers based on density
        int blindnessAmplifier = (int) Math.round(density * 2); // 0-2
        int darknessAmplifier = (int) Math.round(density * 3); // 0-3

        // Apply initial effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, blindnessAmplifier, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, duration, darknessAmplifier, false, false));

        // Create ambient effects task
        BukkitTask fogTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= duration) {
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
                    } else if (block.getType().name().contains("CANDLE")) {
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
} 