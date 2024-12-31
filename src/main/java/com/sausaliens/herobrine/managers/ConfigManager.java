package com.sausaliens.herobrine.managers;

import com.sausaliens.herobrine.HerobrinePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final HerobrinePlugin plugin;
    private FileConfiguration config;
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
    // Advanced settings
    private boolean debugMode;
    private int maxAppearances;
    private int appearanceDuration;
    private int minAppearanceDistance;
    private int maxAppearanceDistance;
    private int ambientSoundFrequency;
    private double ambientSoundChance;

    public ConfigManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadConfig();
    }

    public void loadConfig() {
        // Save the default config with comments if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        java.io.File configFile = new java.io.File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        // Reload the config to get the latest values
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        
        // Load all values with validation
        enabled = config.getBoolean("enabled", true);
        appearanceFrequency = Math.max(1, config.getInt("appearance.frequency", 300));
        appearanceChance = Math.min(1.0, Math.max(0.0, config.getDouble("appearance.chance", 0.3)));
        ambientSoundsEnabled = config.getBoolean("effects.ambient_sounds", true);
        ambientSoundFrequency = Math.max(5, Math.min(120, config.getInt("effects.ambient_sound_frequency", 30)));
        ambientSoundChance = Math.min(1.0, Math.max(0.0, config.getDouble("effects.ambient_sound_chance", 0.3)));
        
        // Set these values in config to ensure they exist
        config.set("effects.ambient_sound_frequency", ambientSoundFrequency);
        config.set("effects.ambient_sound_chance", ambientSoundChance);
        
        structureManipulationEnabled = config.getBoolean("effects.structure_manipulation", true);
        stalkingEnabled = config.getBoolean("effects.stalking_enabled", true);
        maxStalkDistance = Math.max(10, Math.min(100, config.getInt("effects.max_stalk_distance", 50)));
        fogEnabled = config.getBoolean("effects.fog_enabled", true);
        fogDensity = Math.min(1.0, Math.max(0.0, config.getDouble("effects.fog_density", 0.8)));
        fogDuration = Math.max(20, config.getInt("effects.fog_duration", 200));
        footstepsEnabled = config.getBoolean("effects.footsteps_enabled", true);
        maxFootsteps = Math.max(1, Math.min(20, config.getInt("effects.max_footsteps", 10)));
        torchManipulationEnabled = config.getBoolean("effects.torch_manipulation", true);
        torchManipulationRadius = Math.max(5, Math.min(20, config.getInt("effects.torch_manipulation_radius", 10)));
        torchConversionChance = Math.min(1.0, Math.max(0.0, config.getDouble("effects.torch_conversion_chance", 0.7)));
        torchRemovalChance = Math.min(1.0, Math.max(0.0, config.getDouble("effects.torch_removal_chance", 0.3)));
        
        // Load advanced settings with validation
        debugMode = config.getBoolean("advanced.debug", false);
        maxAppearances = Math.max(1, config.getInt("advanced.max_appearances", 1));
        appearanceDuration = Math.max(1, config.getInt("advanced.appearance_duration", 10));
        minAppearanceDistance = Math.max(5, config.getInt("advanced.min_appearance_distance", 15));
        maxAppearanceDistance = Math.max(minAppearanceDistance, config.getInt("advanced.max_appearance_distance", 25));

        // Validate structure settings
        String[] structureTypes = {
            "sand_pyramids", "redstone_caves", "stripped_trees", 
            "mysterious_tunnels", "glowstone_e", "wooden_crosses", 
            "tripwire_traps", "creepy_signs"
        };

        // Validate structure weights
        for (String type : structureTypes) {
            int weight = config.getInt("structures.weights." + type, 10);
            config.set("structures.weights." + type, Math.max(1, weight)); // Ensure positive weights
        }

        // Validate structure-specific settings
        config.set("structures.sand_pyramids.size", 
            Math.max(3, Math.min(10, config.getInt("structures.sand_pyramids.size", 5))));

        config.set("structures.redstone_caves.min_length",
            Math.max(10, config.getInt("structures.redstone_caves.min_length", 15)));
        config.set("structures.redstone_caves.max_length",
            Math.max(config.getInt("structures.redstone_caves.min_length", 15),
                    config.getInt("structures.redstone_caves.max_length", 25)));
        config.set("structures.redstone_caves.torch_interval",
            Math.max(1, config.getInt("structures.redstone_caves.torch_interval", 3)));

        config.set("structures.mysterious_tunnels.min_length",
            Math.max(10, config.getInt("structures.mysterious_tunnels.min_length", 20)));
        config.set("structures.mysterious_tunnels.max_length",
            Math.max(config.getInt("structures.mysterious_tunnels.min_length", 20),
                    config.getInt("structures.mysterious_tunnels.max_length", 40)));
        config.set("structures.mysterious_tunnels.depth",
            Math.max(5, config.getInt("structures.mysterious_tunnels.depth", 10)));

        config.set("structures.stripped_trees.radius",
            Math.max(3, Math.min(10, config.getInt("structures.stripped_trees.radius", 5))));
        config.set("structures.stripped_trees.max_height",
            Math.max(5, Math.min(20, config.getInt("structures.stripped_trees.max_height", 10))));

        config.set("structures.glowstone_e.depth",
            Math.max(3, Math.min(10, config.getInt("structures.glowstone_e.depth", 5))));

        config.set("structures.wooden_crosses.height",
            Math.max(2, Math.min(5, config.getInt("structures.wooden_crosses.height", 3))));

        config.set("structures.tripwire_traps.tnt_count",
            Math.max(1, Math.min(8, config.getInt("structures.tripwire_traps.tnt_count", 4))));
        
        // Only save if values were changed from defaults
        if (config.getDefaults() != null && !config.getValues(true).equals(config.getDefaults().getValues(true))) {
            saveConfig();
        }
    }

    public void saveConfig() {
        // Update values without changing the structure or comments
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
        
        // Save structure settings
        String[] structureTypes = {
            "sand_pyramids", "redstone_caves", "stripped_trees", 
            "mysterious_tunnels", "glowstone_e", "wooden_crosses", 
            "tripwire_traps", "creepy_signs"
        };
        
        // Save enabled types
        for (String type : structureTypes) {
            config.set("structures.enabled_types." + type, true);
        }
        
        // Save weights
        for (String type : structureTypes) {
            int weight = type.equals("glowstone_e") || type.equals("wooden_crosses") || 
                        type.equals("tripwire_traps") || type.equals("creepy_signs") ? 10 : 15;
            config.set("structures.weights." + type, weight);
        }
        
        // Save structure-specific settings
        config.set("structures.sand_pyramids.size", 5);
        
        config.set("structures.redstone_caves.min_length", 15);
        config.set("structures.redstone_caves.max_length", 25);
        config.set("structures.redstone_caves.torch_interval", 3);
        
        config.set("structures.mysterious_tunnels.min_length", 20);
        config.set("structures.mysterious_tunnels.max_length", 40);
        config.set("structures.mysterious_tunnels.depth", 10);
        
        config.set("structures.stripped_trees.radius", 5);
        config.set("structures.stripped_trees.max_height", 10);
        
        config.set("structures.glowstone_e.depth", 5);
        
        config.set("structures.wooden_crosses.height", 3);
        
        config.set("structures.tripwire_traps.tnt_count", 4);
        
        // Save advanced settings
        config.set("advanced.debug", debugMode);
        config.set("advanced.max_appearances", maxAppearances);
        config.set("advanced.appearance_duration", appearanceDuration);
        config.set("advanced.min_appearance_distance", minAppearanceDistance);
        config.set("advanced.max_appearance_distance", maxAppearanceDistance);
        
        config.set("effects.ambient_sounds", ambientSoundsEnabled);
        config.set("effects.ambient_sound_frequency", ambientSoundFrequency);
        config.set("effects.ambient_sound_chance", ambientSoundChance);
        
        try {
            config.save(new java.io.File(plugin.getDataFolder(), "config.yml"));
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
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

    // Advanced settings getters and setters
    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        saveConfig();
    }

    public int getMaxAppearances() {
        return maxAppearances;
    }

    public void setMaxAppearances(int maxAppearances) {
        this.maxAppearances = Math.max(1, maxAppearances);
        saveConfig();
    }

    public int getAppearanceDuration() {
        return appearanceDuration;
    }

    public void setAppearanceDuration(int duration) {
        this.appearanceDuration = Math.max(1, duration);
        saveConfig();
    }

    public int getMinAppearanceDistance() {
        return minAppearanceDistance;
    }

    public void setMinAppearanceDistance(int distance) {
        this.minAppearanceDistance = Math.max(5, Math.min(distance, maxAppearanceDistance));
        saveConfig();
    }

    public int getMaxAppearanceDistance() {
        return maxAppearanceDistance;
    }

    public void setMaxAppearanceDistance(int distance) {
        this.maxAppearanceDistance = Math.max(minAppearanceDistance, distance);
        saveConfig();
    }

    public int getAmbientSoundFrequency() {
        return ambientSoundFrequency;
    }

    public void setAmbientSoundFrequency(int frequency) {
        this.ambientSoundFrequency = Math.max(5, Math.min(120, frequency));
        saveConfig();
    }

    public double getAmbientSoundChance() {
        return ambientSoundChance;
    }

    public void setAmbientSoundChance(double chance) {
        this.ambientSoundChance = Math.min(1.0, Math.max(0.0, chance));
        saveConfig();
    }
} 