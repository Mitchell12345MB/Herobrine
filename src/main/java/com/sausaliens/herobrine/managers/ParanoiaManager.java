package com.sausaliens.herobrine.managers;

import com.sausaliens.herobrine.HerobrinePlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the Paranoia System - a psychological horror experience that gradually
 * intensifies based on player exposure to Herobrine.
 */
public class ParanoiaManager implements Listener {
    private final HerobrinePlugin plugin;
    private final Map<UUID, PlayerExposure> playerExposures;
    
    // Default exposure values
    private boolean enabled;
    private double initialExposure;
    private double exposureGrowthRate;
    private int farAppearanceDistance;
    private boolean distantSilhouettesEnabled;
    private boolean peripheralAppearancesEnabled;
    private double vanishWhenSeenChance;
    private boolean adaptiveEffectsEnabled;

    public ParanoiaManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.playerExposures = new HashMap<>();
        
        // Load configuration
        loadConfig();
        
        // Start data cleanup task
        startCleanupTask();
    }
    
    /**
     * Loads paranoia system configuration from config.yml
     */
    public void loadConfig() {
        enabled = plugin.getConfigManager().isParanoiaEnabled();
        initialExposure = plugin.getConfigManager().getInitialExposure();
        exposureGrowthRate = plugin.getConfigManager().getExposureGrowthRate();
        farAppearanceDistance = plugin.getConfigManager().getFarAppearanceDistance();
        distantSilhouettesEnabled = plugin.getConfigManager().isDistantSilhouettesEnabled();
        peripheralAppearancesEnabled = plugin.getConfigManager().isPeripheralAppearancesEnabled();
        vanishWhenSeenChance = plugin.getConfigManager().getVanishWhenSeenChance();
        adaptiveEffectsEnabled = plugin.getConfigManager().isAdaptiveEffectsEnabled();
    }
    
    /**
     * Record an encounter for a player, increasing their exposure level
     * @param player The player who encountered Herobrine
     * @param type The type of encounter (direct, distant, peripheral, etc.)
     */
    public void recordEncounter(Player player, EncounterType type) {
        if (!enabled) return;
        
        PlayerExposure exposure = getPlayerExposure(player);
        double exposureIncrease = exposureGrowthRate;
        
        // Adjust exposure increase based on encounter type
        switch (type) {
            case DIRECT:
                // Full exposure increase for direct encounters
                break;
                
            case DISTANT:
                // Reduced exposure for distant sightings
                exposureIncrease *= 0.5;
                break;
                
            case PERIPHERAL:
                // Minimal exposure for peripheral sightings
                exposureIncrease *= 0.3;
                break;
                
            case STRUCTURE:
                // Medium exposure for finding structures
                exposureIncrease *= 0.7;
                break;
                
            case AMBIENT:
                // Very small exposure for ambient effects
                exposureIncrease *= 0.2;
                break;
        }
        
        // Increase exposure level
        exposure.increaseExposure(exposureIncrease);
        exposure.lastEncounterTime = System.currentTimeMillis();
        
        // Debug message
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Player " + player.getName() + " exposure increased to " + 
                    exposure.exposureLevel + " after " + type + " encounter");
        }
    }
    
    /**
     * Check if Herobrine should use paranoia behavior for this player
     * Determines if Herobrine should hide/vanish when seen directly
     * @param player The player to check
     * @return True if Herobrine should vanish when seen directly
     */
    public boolean shouldVanishWhenSeen(Player player) {
        if (!enabled) return false;
        
        PlayerExposure exposure = getPlayerExposure(player);
        
        // Lower exposure = higher chance to vanish
        double adjustedChance = vanishWhenSeenChance * (1.0 - exposure.exposureLevel);
        
        return Math.random() < adjustedChance;
    }
    
    /**
     * Check if a distant appearance should be created for a player
     * @param player The player to check
     * @return True if a distant appearance should be created
     */
    public boolean shouldCreateDistantAppearance(Player player) {
        if (!enabled || !distantSilhouettesEnabled) return false;
        
        PlayerExposure exposure = getPlayerExposure(player);
        
        // Higher chance for distant appearances at low exposure levels
        // As exposure increases, these become less common in favor of closer encounters
        double chance = 0.3 * (1.0 - exposure.exposureLevel);
        
        return Math.random() < chance;
    }
    
    /**
     * Check if a peripheral vision appearance should be created for a player
     * @param player The player to check
     * @return True if a peripheral appearance should be created
     */
    public boolean shouldCreatePeripheralAppearance(Player player) {
        if (!enabled || !peripheralAppearancesEnabled) return false;
        
        PlayerExposure exposure = getPlayerExposure(player);
        
        // Higher chance for peripheral appearances at low-medium exposure levels
        double exposureFactor = exposure.exposureLevel;
        double chance;
        
        if (exposureFactor < 0.4) {
            // Rising chance in early phase
            chance = exposureFactor * 0.75;
        } else if (exposureFactor < 0.7) {
            // Peak chance in mid phase
            chance = 0.3;
        } else {
            // Declining chance in late phase as direct encounters take over
            chance = 0.3 * (1.0 - ((exposureFactor - 0.7) / 0.3));
        }
        
        return Math.random() < chance;
    }
    
    /**
     * Get the far appearance distance for a player
     * Distance is reduced as exposure increases
     * @param player The player
     * @return The far appearance distance in blocks
     */
    public int getFarAppearanceDistance(Player player) {
        if (!enabled) return farAppearanceDistance;
        
        PlayerExposure exposure = getPlayerExposure(player);
        
        // As exposure increases, far appearances occur closer
        int minDistance = plugin.getConfigManager().getMaxAppearanceDistance() + 5;
        int distanceRange = farAppearanceDistance - minDistance;
        
        return (int)(minDistance + (distanceRange * (1.0 - exposure.exposureLevel)));
    }
    
    /**
     * Modify effect intensity based on player exposure
     * @param player The player
     * @param baseIntensity The base intensity value
     * @return The modified intensity value
     */
    public double getModifiedEffectIntensity(Player player, double baseIntensity) {
        if (!enabled || !adaptiveEffectsEnabled) return baseIntensity;
        
        PlayerExposure exposure = getPlayerExposure(player);
        
        // Subtle effects at low exposure, intense effects at high exposure
        double intensityMultiplier = 0.5 + (exposure.exposureLevel * 0.5);
        
        return baseIntensity * intensityMultiplier;
    }
    
    /**
     * Get a player's current exposure level
     * @param player The player
     * @return The player's exposure level (0.0 - 1.0)
     */
    public double getExposureLevel(Player player) {
        if (!enabled) return 0.0;
        
        return getPlayerExposure(player).exposureLevel;
    }
    
    /**
     * Check if a location is in a player's peripheral vision
     * @param player The player
     * @param location The location to check
     * @return True if the location is in peripheral vision
     */
    public boolean isInPeripheralVision(Player player, Location location) {
        // Get vector from player to location
        Location playerLoc = player.getEyeLocation();
        double playerYaw = Math.toRadians(playerLoc.getYaw());
        
        // Calculate horizontal vector to location
        double dx = location.getX() - playerLoc.getX();
        double dz = location.getZ() - playerLoc.getZ();
        
        // Calculate angle between player's look direction and vector to location
        double angle = Math.atan2(dz, dx) - playerYaw;
        
        // Normalize angle to -PI to PI
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        
        // Convert to degrees and take absolute value
        double angleDegrees = Math.abs(Math.toDegrees(angle));
        
        // Check if angle is in peripheral vision (70-110 degrees off center)
        return angleDegrees > 70 && angleDegrees < 110;
    }
    
    /**
     * Get or create player exposure data
     * @param player The player
     * @return The player's exposure data
     */
    private PlayerExposure getPlayerExposure(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerExposure exposure = playerExposures.get(playerId);
        
        if (exposure == null) {
            exposure = new PlayerExposure(initialExposure);
            playerExposures.put(playerId, exposure);
        }
        
        return exposure;
    }
    
    /**
     * Handle player join event to ensure exposure data exists
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        
        // Ensure player has exposure data
        getPlayerExposure(event.getPlayer());
    }
    
    /**
     * Start task to clean up old exposure data
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long twoWeeksAgo = currentTime - (14 * 24 * 60 * 60 * 1000); // Two weeks
                
                // Remove exposure data for players who haven't been seen in two weeks
                playerExposures.entrySet().removeIf(entry -> 
                    entry.getValue().lastEncounterTime < twoWeeksAgo);
            }
        }.runTaskTimer(plugin, 72000L, 72000L); // Run every hour
    }
    
    /**
     * Encounter types for the paranoia system
     */
    public enum EncounterType {
        DIRECT,     // Direct face-to-face encounter
        DISTANT,    // Distant sighting
        PERIPHERAL, // Peripheral vision
        STRUCTURE,  // Found a structure
        AMBIENT     // Ambient effects (sounds, etc.)
    }
    
    /**
     * Tracks a player's exposure level to Herobrine
     */
    private static class PlayerExposure {
        double exposureLevel;
        long lastEncounterTime;
        
        PlayerExposure(double initialExposure) {
            this.exposureLevel = initialExposure;
            this.lastEncounterTime = System.currentTimeMillis();
        }
        
        /**
         * Increase exposure level
         * @param amount Amount to increase
         */
        void increaseExposure(double amount) {
            exposureLevel += amount;
            
            // Cap at 1.0
            if (exposureLevel > 1.0) {
                exposureLevel = 1.0;
            }
        }
    }
} 