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

import java.util.*;

public class EffectManager implements Listener {
    private final HerobrinePlugin plugin;
    private final Random random;
    private final Map<UUID, List<BukkitTask>> activeTasks;
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
    }

    public void playAppearanceEffects(Player player, Location location) {
        if (!plugin.getConfigManager().isAmbientSoundsEnabled()) return;

        // Create unsettling initial effects
        player.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.3f);
        player.playSound(location, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.5f, 0.5f);
        location.getWorld().spawnParticle(Particle.SMOKE, location, 50, 0.5, 1, 0.5, 0.02);
        location.getWorld().spawnParticle(Particle.SOUL, location, 20, 0.5, 1, 0.5, 0.02);
        
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
    }

    public void cleanup() {
        // Cancel all active tasks
        for (List<BukkitTask> tasks : activeTasks.values()) {
            tasks.forEach(BukkitTask::cancel);
        }
        activeTasks.clear();
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
} 