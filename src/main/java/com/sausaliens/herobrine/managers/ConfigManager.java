package com.sausaliens.herobrine.managers;

import com.sausaliens.herobrine.HerobrinePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final HerobrinePlugin plugin;
    private final FileConfiguration config;
    private boolean enabled;
    private int appearanceFrequency;
    private double appearanceChance;
    private boolean ambientSoundsEnabled;
    private boolean structureManipulationEnabled;
    private boolean stalkingEnabled;
    private int maxStalkDistance;
    private boolean fogEnabled;
    private double fogDensity;
    private int fogDuration;
    private boolean footstepsEnabled;
    private int maxFootsteps;
    private boolean torchManipulationEnabled;
    private int torchManipulationRadius;
    private double torchConversionChance;
    private double torchRemovalChance;

    public ConfigManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        
        enabled = config.getBoolean("enabled", true);
        appearanceFrequency = config.getInt("appearance.frequency", 300);
        appearanceChance = config.getDouble("appearance.chance", 0.3);
        ambientSoundsEnabled = config.getBoolean("effects.ambient_sounds", true);
        structureManipulationEnabled = config.getBoolean("effects.structure_manipulation", true);
        stalkingEnabled = config.getBoolean("effects.stalking_enabled", true);
        maxStalkDistance = config.getInt("effects.max_stalk_distance", 50);
        fogEnabled = config.getBoolean("effects.fog_enabled", true);
        fogDensity = config.getDouble("effects.fog_density", 0.8);
        fogDuration = config.getInt("effects.fog_duration", 200);
        footstepsEnabled = config.getBoolean("effects.footsteps_enabled", true);
        maxFootsteps = config.getInt("effects.max_footsteps", 10);
        torchManipulationEnabled = config.getBoolean("effects.torch_manipulation", true);
        torchManipulationRadius = config.getInt("effects.torch_manipulation_radius", 10);
        torchConversionChance = config.getDouble("effects.torch_conversion_chance", 0.7);
        torchRemovalChance = config.getDouble("effects.torch_removal_chance", 0.3);
        
        saveConfig();
    }

    public void saveConfig() {
        config.set("enabled", enabled);
        config.set("appearance.frequency", appearanceFrequency);
        config.set("appearance.chance", appearanceChance);
        config.set("effects.ambient_sounds", ambientSoundsEnabled);
        config.set("effects.structure_manipulation", structureManipulationEnabled);
        config.set("effects.stalking_enabled", stalkingEnabled);
        config.set("effects.max_stalk_distance", maxStalkDistance);
        config.set("effects.fog_enabled", fogEnabled);
        config.set("effects.fog_density", fogDensity);
        config.set("effects.fog_duration", fogDuration);
        config.set("effects.footsteps_enabled", footstepsEnabled);
        config.set("effects.max_footsteps", maxFootsteps);
        config.set("effects.torch_manipulation", torchManipulationEnabled);
        config.set("effects.torch_manipulation_radius", torchManipulationRadius);
        config.set("effects.torch_conversion_chance", torchConversionChance);
        config.set("effects.torch_removal_chance", torchRemovalChance);
        plugin.saveConfig();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveConfig();
    }

    public int getAppearanceFrequency() {
        return appearanceFrequency;
    }

    public void setAppearanceFrequency(int frequency) {
        this.appearanceFrequency = Math.max(1, frequency);
        saveConfig();
        plugin.getAppearanceManager().startAppearanceTimer();
    }

    public double getAppearanceChance() {
        return appearanceChance;
    }

    public void setAppearanceChance(double chance) {
        this.appearanceChance = Math.min(1.0, Math.max(0.0, chance));
        saveConfig();
    }

    public boolean isAmbientSoundsEnabled() {
        return ambientSoundsEnabled;
    }

    public void setAmbientSoundsEnabled(boolean enabled) {
        this.ambientSoundsEnabled = enabled;
        saveConfig();
    }

    public boolean isStructureManipulationEnabled() {
        return structureManipulationEnabled;
    }

    public void setStructureManipulationEnabled(boolean enabled) {
        this.structureManipulationEnabled = enabled;
        saveConfig();
    }

    public boolean isStalkingEnabled() {
        return stalkingEnabled;
    }

    public void setStalkingEnabled(boolean enabled) {
        this.stalkingEnabled = enabled;
        saveConfig();
    }

    public int getMaxStalkDistance() {
        return maxStalkDistance;
    }

    public void setMaxStalkDistance(int distance) {
        this.maxStalkDistance = Math.max(10, Math.min(100, distance));
        saveConfig();
    }

    // Structure settings
    public boolean isStructureEnabled(String structureType) {
        return config.getBoolean("structures.enabled_types." + structureType, true);
    }

    public int getStructureWeight(String structureType) {
        return config.getInt("structures.weights." + structureType, 10);
    }

    // Sand Pyramids
    public int getPyramidSize() {
        return config.getInt("structures.sand_pyramids.size", 5);
    }

    // Redstone Caves
    public int getRedstoneCaveMinLength() {
        return config.getInt("structures.redstone_caves.min_length", 15);
    }

    public int getRedstoneCaveMaxLength() {
        return config.getInt("structures.redstone_caves.max_length", 25);
    }

    public int getRedstoneTorchInterval() {
        return config.getInt("structures.redstone_caves.torch_interval", 3);
    }

    // Mysterious Tunnels
    public int getMysteriousTunnelMinLength() {
        return config.getInt("structures.mysterious_tunnels.min_length", 20);
    }

    public int getMysteriousTunnelMaxLength() {
        return config.getInt("structures.mysterious_tunnels.max_length", 40);
    }

    public int getMysteriousTunnelDepth() {
        return config.getInt("structures.mysterious_tunnels.depth", 10);
    }

    // Stripped Trees
    public int getStrippedTreesRadius() {
        return config.getInt("structures.stripped_trees.radius", 5);
    }

    public int getStrippedTreesMaxHeight() {
        return config.getInt("structures.stripped_trees.max_height", 10);
    }

    // Glowstone E
    public int getGlowstoneEDepth() {
        return config.getInt("structures.glowstone_e.depth", 5);
    }

    // Wooden Crosses
    public int getWoodenCrossHeight() {
        return config.getInt("structures.wooden_crosses.height", 3);
    }

    // Tripwire Traps
    public int getTripwireTrapTNTCount() {
        return config.getInt("structures.tripwire_traps.tnt_count", 4);
    }

    // Calculate structure chances based on weights
    public Map<String, Double> getStructureChances() {
        Map<String, Double> chances = new HashMap<>();
        int totalWeight = 0;
        
        String[] structures = {
            "sand_pyramids", "redstone_caves", "stripped_trees", 
            "mysterious_tunnels", "glowstone_e", "wooden_crosses",
            "tripwire_traps", "creepy_signs"
        };

        // Sum up weights of enabled structures
        for (String structure : structures) {
            if (isStructureEnabled(structure)) {
                totalWeight += getStructureWeight(structure);
            }
        }

        // Calculate chances
        for (String structure : structures) {
            if (isStructureEnabled(structure)) {
                double chance = (double) getStructureWeight(structure) / totalWeight;
                chances.put(structure, chance);
            } else {
                chances.put(structure, 0.0);
            }
        }

        return chances;
    }

    public boolean isFogEnabled() {
        return fogEnabled;
    }

    public void setFogEnabled(boolean fogEnabled) {
        this.fogEnabled = fogEnabled;
        saveConfig();
    }

    public double getFogDensity() {
        return fogDensity;
    }

    public void setFogDensity(double fogDensity) {
        this.fogDensity = fogDensity;
        saveConfig();
    }

    public int getFogDuration() {
        return fogDuration;
    }

    public void setFogDuration(int fogDuration) {
        this.fogDuration = fogDuration;
        saveConfig();
    }

    public boolean isFootstepsEnabled() {
        return footstepsEnabled;
    }

    public void setFootstepsEnabled(boolean enabled) {
        this.footstepsEnabled = enabled;
        saveConfig();
    }

    public int getMaxFootsteps() {
        return maxFootsteps;
    }

    public void setMaxFootsteps(int steps) {
        this.maxFootsteps = Math.max(1, Math.min(20, steps));
        saveConfig();
    }

    public boolean isTorchManipulationEnabled() {
        return torchManipulationEnabled;
    }

    public void setTorchManipulationEnabled(boolean enabled) {
        this.torchManipulationEnabled = enabled;
        saveConfig();
    }

    public int getTorchManipulationRadius() {
        return torchManipulationRadius;
    }

    public void setTorchManipulationRadius(int radius) {
        this.torchManipulationRadius = Math.max(5, Math.min(20, radius));
        saveConfig();
    }

    public double getTorchConversionChance() {
        return torchConversionChance;
    }

    public void setTorchConversionChance(double chance) {
        this.torchConversionChance = Math.min(1.0, Math.max(0.0, chance));
        saveConfig();
    }

    public double getTorchRemovalChance() {
        return torchRemovalChance;
    }

    public void setTorchRemovalChance(double chance) {
        this.torchRemovalChance = Math.min(1.0, Math.max(0.0, chance));
        saveConfig();
    }
} 