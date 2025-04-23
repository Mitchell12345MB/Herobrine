package com.sausaliens.herobrine;

import com.sausaliens.herobrine.commands.HerobrineCommand;
import com.sausaliens.herobrine.managers.AggressionManager;
import com.sausaliens.herobrine.managers.AppearanceManager;
import com.sausaliens.herobrine.managers.ConfigManager;
import com.sausaliens.herobrine.managers.EffectManager;
import com.sausaliens.herobrine.managers.ParanoiaManager;
import com.sausaliens.herobrine.managers.StructureManager;
import org.bukkit.plugin.java.JavaPlugin;

public class HerobrinePlugin extends JavaPlugin {
    private ConfigManager configManager;
    private AppearanceManager appearanceManager;
    private EffectManager effectManager;
    private AggressionManager aggressionManager;
    private StructureManager structureManager;
    private ParanoiaManager paranoiaManager;

    @Override
    public void onEnable() {
        // Initialize managers
        configManager = new ConfigManager(this);
        effectManager = new EffectManager(this);
        aggressionManager = new AggressionManager(this);
        appearanceManager = new AppearanceManager(this);
        structureManager = new StructureManager(this);
        paranoiaManager = new ParanoiaManager(this);
        
        // Register events
        getServer().getPluginManager().registerEvents(appearanceManager, this);
        getServer().getPluginManager().registerEvents(effectManager, this);
        getServer().getPluginManager().registerEvents(aggressionManager, this);
        getServer().getPluginManager().registerEvents(structureManager, this);
        getServer().getPluginManager().registerEvents(paranoiaManager, this);
        
        // Register commands
        getCommand("herobrine").setExecutor(new HerobrineCommand(this));
        
        // Start timers
        appearanceManager.startAppearanceTimer();
        effectManager.startEffects();

        getLogger().info("I have returned...");
    }

    @Override
    public void onDisable() {
        // Cleanup
        if (appearanceManager != null) {
            appearanceManager.cleanup();
        }
        if (effectManager != null) {
            effectManager.stopEffects();
        }
        if (aggressionManager != null) {
            aggressionManager.cleanup();
        }

        getLogger().info("I will return...");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public AppearanceManager getAppearanceManager() {
        return appearanceManager;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public AggressionManager getAggressionManager() {
        return aggressionManager;
    }

    public StructureManager getStructureManager() {
        return structureManager;
    }
    
    public ParanoiaManager getParanoiaManager() {
        return paranoiaManager;
    }
} 