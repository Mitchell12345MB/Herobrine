package com.sausaliens.herobrine.managers;

import com.sausaliens.herobrine.HerobrinePlugin;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.PlayerFilter;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SkinTrait;
import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.NavigatorParameters;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.TripwireHook;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.*;

public class AppearanceManager implements Listener {
    private final HerobrinePlugin plugin;
    private final Random random;
    private final Map<UUID, NPC> activeAppearances;
    private final NPCRegistry registry;
    private BukkitTask appearanceTask;
    private BukkitTask cleanupTask;
    
    // Memory system to track player encounters
    private final Map<UUID, PlayerMemory> playerMemories;
    private final Map<UUID, Long> lastPlayerInteractions;

    // Herobrine's skin texture and signature from MineSkin.org
    private static final String TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTYxMTk3MTk0NTY0NiwKICAicHJvZmlsZUlkIiA6ICIwNWQ0NTNiZWE0N2Y0MThiOWI2ZDUzODg0MWQxMDY2MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJFY2hvcnJhIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzhkYzYyZDA5ZmEyNmEyMmNkNjU0MWU5N2UwNjE0ZjA4MTQ3YzBjNWFmNGU0MzM3NzY3ZjMxMThmYWQyODExOTYiCiAgICB9CiAgfQp9";
    private static final String SIGNATURE = "CFivZK2Du6OXoEa/G7znPDAv0eGMLOc69aKF6HUvk1woJCzqwIfx/aIZdaKyu0SMPQAcX5ta6zmp6FndHzBc4ehqQCvSlNQQxhYrAG4eaxGGDMYm6uFdPK0l1QamqZ+4EHR0VCayhtYKQcwghr1GkOoR8E3+FibwPZ0MICmovd6by9z/fbPymMIAkpgimsLe583OYO2ab7jsGMkpW5/mf10JQCLcRz2i8QAo0gLTJV5cyx7g2/v1mleLsV1JY3fFO7CmWsWtoamsJtCfW+z4Rs8xqvQunSDngWOIHvPDgAjTKAoGyCg8PlRu4om1URAIOi4xPX+B7z4kPpmEs7cWtlOgABWdsG6IUAZGe5nrL+OVgfJ5wSA+SPk882btwOdLzLa2FfEOOa169Gpfax4sFaQ6Y89ZM3RjtgEimjjUEbQvbj9tkOoT1FzRJ9UJXe933M92q82ikack8/VVOpzYgVbcEeO7hlzC/MfzEV1Iox4ZxYrUB899qDmQWgc4DuJ31V71bEP208ZmvFDffDOOFlO73yoyGt4LO2/IqynVRsnc9vMrf8e5z1WYCjopH6cs1cf/vov+oxZVsIL97Di3c8Ufr7YlUl4Rkp8G2nDHdMYIHKTKhwFMs9MBs/2wR9SUBUDi/2NIZvlbV/Efhk8fyDC0PYAbZJvEC5w01KBhRTg=";

    public AppearanceManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.activeAppearances = new HashMap<>();
        this.registry = CitizensAPI.getNPCRegistry();
        this.playerMemories = new HashMap<>();
        this.lastPlayerInteractions = new HashMap<>();
        startAppearanceTimer();
        startCleanupTask();
        startMemoryCleanupTask();
    }

    public void startAppearanceTimer() {
        if (appearanceTask != null) {
            appearanceTask.cancel();
        }
        
        int frequency = plugin.getConfigManager().getAppearanceFrequency() * 20; // Convert to ticks
        appearanceTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getConfigManager().isEnabled()) {
                return;
            }
            
            // Check for all online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Skip if Herobrine is already active for this player
                if (isHerobrineNearby(player)) {
                    continue;
                }
                
                // Get player memory
                PlayerMemory memory = getPlayerMemory(player);
                
                // Calculate adaptive appearance chance based on player's memory
                double adaptiveChance = calculateAdaptiveAppearanceChance(player, memory);
                
                if (Math.random() < adaptiveChance) {
                    // Create appearance with delay based on player's context
                    scheduleAppearanceForPlayer(player, memory);
                }
                
                // Check for paranoia-based appearances (if enabled)
                checkParanoiaAppearances(player, memory);
            }
        }, frequency, frequency);
    }

    public void stopAppearanceTimer() {
        if (appearanceTask != null) {
            appearanceTask.cancel();
            appearanceTask = null;
        }
    }

    public void removeAllAppearances() {
        for (UUID playerId : activeAppearances.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                removeAppearance(player);
            }
        }
        activeAppearances.clear();
    }

    public void createAppearance(Player player) {
        // Check if player already has an active appearance and remove it first
        if (activeAppearances.containsKey(player.getUniqueId())) {
            removeAppearance(player);
        }
        
        Location location = findAppearanceLocation(player);
        if (location == null) return;
        createAppearance(player, location);
        
        // Record encounter for paranoia system
        if (plugin.getParanoiaManager() != null) {
            plugin.getParanoiaManager().recordEncounter(player, ParanoiaManager.EncounterType.DIRECT);
        }
    }

    public void createAppearance(Player player, Location location) {
        // Double check no existing NPC
        if (activeAppearances.containsKey(player.getUniqueId())) {
            removeAppearance(player);
        }
        
        // Update player memories
        updatePlayerMemory(player);
        
        // Create Herobrine NPC
        NPC npc = registry.createNPC(EntityType.PLAYER, "Herobrine");
        npc.setProtected(true);
        
        // Set skin using Citizens API
        SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
        skinTrait.setSkinPersistent("Herobrine", SIGNATURE, TEXTURE);
        
        // Spawn NPC at location
        npc.spawn(location);
        
        // Configure navigation
        Navigator navigator = npc.getNavigator();
        NavigatorParameters params = navigator.getLocalParameters();
        params.speedModifier(1.4f); // Slightly faster than player
        params.distanceMargin(1.5); // How close to get to target
        params.baseSpeed(0.3f); // Base movement speed
        params.range(40); // Maximum pathfinding range
        params.stuckAction(null); // Disable default stuck action
        params.stationaryTicks(50); // More time before considering NPC stuck
        params.updatePathRate(10); // Update path less frequently
        params.straightLineTargetingDistance(20); // Use straight line movement when close
        
        // Make NPC look at player
        LookClose lookTrait = npc.getOrAddTrait(LookClose.class);
        lookTrait.setRange(50);
        lookTrait.setRealisticLooking(true);
        lookTrait.lookClose(true);
        
        // Set player filter
        PlayerFilter filterTrait = npc.getOrAddTrait(PlayerFilter.class);
        filterTrait.setAllowlist();
        filterTrait.addPlayer(player.getUniqueId());

        activeAppearances.put(player.getUniqueId(), npc);
        
        // Start behavior check task
        startBehaviorCheckTask(player, npc);
        
        // Play initial effects
        plugin.getEffectManager().playAppearanceEffects(player, location);
    }

    private void startBehaviorCheckTask(Player player, NPC npc) {
        // Get player memory data
        PlayerMemory memory = getPlayerMemory(player);
        
        new BukkitRunnable() {
            int ticksExisted = 0;
            boolean isRunningAway = false;
            Location runAwayTarget = null;
            Navigator navigator = npc.getNavigator();
            Location lastLocation = npc.getEntity().getLocation();
            int stationaryTicks = 0;
            Vector lastDirection = null;
            int stuckTicks = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || !npc.isSpawned()) {
                    cancel();
                    if (npc.isSpawned()) {
                        removeAppearance(player);
                    }
                    return;
                }

                ticksExisted++;
                Location npcLoc = npc.getEntity().getLocation();
                Location playerLoc = player.getLocation();
                double distanceToPlayer = calculateSafeDistance(npcLoc, playerLoc);
                
                // Check if player is too close (within 10 blocks)
                if (distanceToPlayer < 10) {
                    // Adjust vanish chance based on player history
                    double vanishChance = player.isSprinting() ? 0.9 : 0.8;
                    
                    // Adjust based on how aggressively player has chased Herobrine in the past
                    if (memory.hasAggressivelyChased) {
                        vanishChance += 0.05; // More likely to flee from aggressive players
                    }
                    
                    // Adjust based on how many times player has seen Herobrine
                    vanishChance -= Math.min(0.2, memory.encounterCount * 0.01); // Up to 20% less likely to disappear for regular encounters
                    
                    if (Math.random() < vanishChance) {
                        // Remember that we fled from this player
                        memory.fleeCount++;
                        memory.lastFleeTime = System.currentTimeMillis();
                        
                        Location disappearLoc = npc.getEntity().getLocation();
                        plugin.getEffectManager().playAppearanceEffects(player, disappearLoc);
                        removeAppearance(player);
                        cancel();
                        return;
                    }
                }

                // Check if NPC is stuck
                if (navigator.isNavigating()) {
                    Vector currentDirection = navigator.getTargetAsLocation().toVector().subtract(npcLoc.toVector()).normalize();
                    
                    // Only consider stuck if we're actually not moving AND trying to go in the same direction
                    if (npcLoc.distanceSquared(lastLocation) < 0.01 && 
                        (lastDirection != null && currentDirection.dot(lastDirection) > 0.95)) {
                        stuckTicks++;
                        if (stuckTicks > 20) { // Stuck for 1 second
                            handleStuckNPC(npc, navigator.getTargetAsLocation());
                            stuckTicks = 0;
                        }
                    } else {
                        stuckTicks = Math.max(0, stuckTicks - 1); // Gradually reduce stuck ticks
                    }
                    lastDirection = currentDirection;
                } else {
                    stuckTicks = 0;
                    lastDirection = null;
                }

                // Check if Herobrine has been stationary for too long
                if (npcLoc.distanceSquared(lastLocation) < 0.01) {
                    stationaryTicks++;
                    // If stationary for more than 5 seconds (100 ticks) and player is moving away
                    if (stationaryTicks > 100 && distanceToPlayer > 20) {
                        // Either follow the player or teleport to a new stalking position
                        if (Math.random() < 0.7) { // 70% chance to follow
                            Location stalkLoc = findStalkLocation(player);
                            if (stalkLoc != null) {
                                npc.teleport(stalkLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                                plugin.getEffectManager().playAppearanceEffects(player, stalkLoc);
                            }
                        } else { // 30% chance to create structure and disappear
                            createRandomStructure(npcLoc);
                            plugin.getEffectManager().playAppearanceEffects(player, npcLoc);
                            removeAppearance(player);
                            cancel();
                            return;
                        }
                        stationaryTicks = 0;
                    }
                } else {
                    stationaryTicks = 0;
                }

                // Every 2 seconds (40 ticks), decide what to do
                if (ticksExisted % 40 == 0) {
                    double rand = Math.random();
                    
                    // Increase chance to run if player is looking at Herobrine
                    if (isPlayerLookingAt(player, npcLoc)) {
                        // More sophisticated response to being spotted
                        memory.spottedCount++;
                        
                        // Check paranoia system for vanish chance
                        if (plugin.getParanoiaManager() != null && 
                            plugin.getParanoiaManager().shouldVanishWhenSeen(player)) {
                            // Vanish dramatically
                            player.playSound(npcLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.5f);
                            player.spawnParticle(Particle.CLOUD, npcLoc, 20, 0.5, 1, 0.5, 0.1);
                            removeAppearance(player);
                            cancel();
                            return;
                        }
                        
                        // Adjust reaction based on how often this player spots Herobrine
                        if (memory.spottedCount < 5) {
                            rand += 0.3; // New players who spot Herobrine trigger stronger flee response
                        } else if (memory.spottedCount < 10) {
                            rand += 0.2; // Regular spotters get standard response
                        } else {
                            rand += 0.1; // Frequent spotters get diminished response (Herobrine is less afraid)
                        }
                    }
                    
                    // Adjust behavior based on memory
                    // If player chases a lot, increase structure building or hiding behavior
                    if (memory.hasAggressivelyChased) {
                        rand -= 0.1; // More likely to create structures or stalk
                    }
                    
                    // If player has destroyed Herobrine structures before, more likely to be elusive
                    if (memory.hasDestroyedStructures) {
                        rand += 0.1; // More likely to run
                    }
                    
                    // Adjust behavior based on more personality factors
                    if (memory.playerIsAggressive) {
                        // If player is aggressive, Herobrine is more likely to confront with structures
                        rand -= 0.15; // Even more likely to build structures or stalk
                    }
                    
                    // Use fleeCount to adjust behavior
                    if (memory.fleeCount > 5) {
                        // If Herobrine has fled many times, mix up behavior to be less predictable
                        rand = rand * 0.7 + (Math.random() * 0.3); // Add randomness to behavior
                    }
                    
                    if (rand < 0.3) { // ~30% chance to create a trap or structure
                        Location trapLoc = findSuitableLocation(playerLoc, 10, 20);
                        if (trapLoc != null && !isNearExistingStructure(trapLoc)) {
                            createRandomStructure(trapLoc);
                        }
                    } else if (rand < 0.6 && !isRunningAway) { // 30% chance to stalk if not already running
                        Location stalkLoc = findStalkLocation(player);
                        if (stalkLoc != null) {
                            navigator.setTarget(stalkLoc);
                            plugin.getEffectManager().playStalkEffects(player, stalkLoc);
                            plugin.getEffectManager().playFootstepEffects(player);
                            if (random.nextDouble() < 0.3) {
                                plugin.getEffectManager().manipulateTorches(stalkLoc, 10);
                            }
                            if (random.nextDouble() < 0.15) {
                                Location chestLoc = findNearbyChest(stalkLoc);
                                if (chestLoc != null) {
                                    plugin.getEffectManager().leaveChestDonation(chestLoc);
                                }
                            }
                        }
                    } else { // 40% chance to run away
                        if (!isRunningAway) {
                            runAwayTarget = findRunAwayLocation(player);
                            if (runAwayTarget != null) {
                                isRunningAway = true;
                                navigator.setTarget(runAwayTarget);
                                if (Math.random() < 0.5) { // 50% chance to create a structure before running
                                    createRandomStructure(npc.getEntity().getLocation());
                                }
                            }
                        }
                    }
                }

                lastLocation = npcLoc;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void handleStuckNPC(NPC npc, Location target) {
        if (target == null) return;

        Location npcLoc = npc.getEntity().getLocation();
        Vector direction = target.toVector().subtract(npcLoc.toVector()).normalize();
        
        // Try to find an alternative path first
        Location[] alternativePoints = {
            npcLoc.clone().add(direction.clone().rotateAroundY(Math.PI / 4).multiply(3)),
            npcLoc.clone().add(direction.clone().rotateAroundY(-Math.PI / 4).multiply(3))
        };
        
        for (Location point : alternativePoints) {
            if (isLocationSafe(point)) {
                npc.getNavigator().setTarget(point);
                return;
            }
        }
        
        // If no alternative path, try building steps
        if (isHighObstacle(npcLoc.clone().add(direction))) {
            buildSteps(npcLoc, direction);
        }
    }

    private boolean isLocationSafe(Location location) {
        Block ground = location.getBlock();
        Block above = ground.getRelative(BlockFace.UP);
        Block below = ground.getRelative(BlockFace.DOWN);
        
        return below.getType().isSolid() && 
               !ground.getType().isSolid() && 
               !above.getType().isSolid();
    }

    private boolean isHighObstacle(Location location) {
        Block ground = location.getBlock();
        Block above = ground.getRelative(BlockFace.UP);
        Block twoAbove = above.getRelative(BlockFace.UP);
        
        return ground.getType().isSolid() && above.getType().isSolid() && !twoAbove.getType().isSolid();
    }

    private void buildSteps(Location start, Vector direction) {
        Location stepLoc = start.clone().add(direction);
        Block stepBlock = stepLoc.getBlock();
        Block above = stepBlock.getRelative(BlockFace.UP);
        
        // Only build if we're not destroying anything important
        if (!stepBlock.getType().isSolid() && !above.getType().isSolid()) {
            // Place a temporary block (soul sand for thematic effect)
            stepBlock.setType(Material.SOUL_SAND);
            
            // Schedule block removal
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (stepBlock.getType() == Material.SOUL_SAND) {
                        stepBlock.setType(Material.AIR);
                        // Add some particles for effect
                        stepBlock.getWorld().spawnParticle(Particle.SOUL, stepBlock.getLocation().add(0.5, 0.5, 0.5), 
                            10, 0.2, 0.2, 0.2, 0.02);
                    }
                }
            }.runTaskLater(plugin, 100L); // Remove after 5 seconds
        }
    }

    private boolean isNearExistingStructure(Location location) {
        return plugin.getAggressionManager().hasStructureWithin(location, 10);
    }

    private Location findSuitableLocation(Location center, int minDistance, int maxDistance) {
        for (int attempts = 0; attempts < 10; attempts++) {
            // Get random angle and distance
            double angle = Math.random() * 2 * Math.PI;
            double distance = minDistance + Math.random() * (maxDistance - minDistance);
            
            // Calculate offset
            double x = Math.cos(angle) * distance;
            double z = Math.sin(angle) * distance;
            
            // Find a suitable Y coordinate
            Location loc = center.clone().add(x, 0, z);
            loc.setY(center.getY());
            
            // Check if location is suitable (not in air or inside blocks)
            Block block = loc.getBlock();
            Block above = block.getRelative(BlockFace.UP);
            Block below = block.getRelative(BlockFace.DOWN);
            
            if (below.getType().isSolid() && 
                !block.getType().isSolid() && 
                !above.getType().isSolid()) {
                return loc;
            }
        }
        return null;
    }

    private Location findAppearanceLocation(Player player) {
        Location playerLoc = player.getLocation();
        int minDist = plugin.getConfigManager().getMinAppearanceDistance();
        int maxDist = plugin.getConfigManager().getMaxAppearanceDistance();
        
        // Check if we should use a far appearance distance from paranoia system
        if (plugin.getParanoiaManager() != null && 
            plugin.getParanoiaManager().shouldCreateDistantAppearance(player)) {
            // Use farAppearanceDistance instead
            maxDist = plugin.getParanoiaManager().getFarAppearanceDistance(player);
        }
        
        // Try multiple positions to find a valid location
        for (int i = 0; i < 10; i++) {
            // Choose random distance between min and max
            double distance = minDist + random.nextDouble() * (maxDist - minDist);
            
            // Choose random angle
            double angle = random.nextDouble() * 2 * Math.PI;
            
            // Calculate position at that distance and angle
            double x = Math.sin(angle) * distance;
            double z = Math.cos(angle) * distance;
            
            Location loc = playerLoc.clone().add(x, 0, z);
            
            // Adjust Y to ground level
            int highestY = loc.getWorld().getHighestBlockYAt(loc);
            loc.setY(highestY + 1); // +1 to stand on top of the block
            
            // Check if this location is suitable
            if (isSuitableLocation(loc, player)) {
                return loc;
            }
        }
        
        // Couldn't find a suitable location
        return null;
    }

    private boolean isSuitableLocation(Location location, Player player) {
        // Check if the location is safe
        if (!isLocationSafe(location)) {
            return false;
        }
        
        // Check if the location is dark
        if (isEnvironmentDark(player)) {
            return false;
        }
        
        // Check if the location is isolated
        if (isPlayerIsolated(player)) {
            return false;
        }
        
        return true;
    }

    private void removeAppearance(Player player) {
        NPC npc = activeAppearances.remove(player.getUniqueId());
        if (npc != null) {
            if (npc.isSpawned()) {
                npc.destroy();
            }
            // Ensure NPC is fully removed from Citizens registry
            if (registry.getById(npc.getId()) != null) {
                registry.deregister(npc);
            }
        }
        // Clean up any lingering effects
        plugin.getEffectManager().stopEffects(player);
    }

    public void cleanup() {
        // Remove all active appearances
        for (Map.Entry<UUID, NPC> entry : new HashMap<>(activeAppearances).entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                removeAppearance(player);
            }
        }
        activeAppearances.clear();
        
        // Cancel tasks
        if (appearanceTask != null) {
            appearanceTask.cancel();
            appearanceTask = null;
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (activeAppearances.containsKey(player.getUniqueId())) {
            NPC npc = activeAppearances.get(player.getUniqueId());
            if (npc != null && npc.isSpawned()) {
                Location playerLoc = player.getLocation();
                Location npcLoc = npc.getEntity().getLocation();
                
                // Get player memory
                PlayerMemory memory = getPlayerMemory(player);
                
                // Track if player is chasing Herobrine
                if (player.isSprinting() && calculateSafeDistance(playerLoc, npcLoc) < 15) {
                    // Mark player as having chased Herobrine
                    memory.chaseCount++;
                    if (memory.chaseCount >= 3) {
                        memory.hasAggressivelyChased = true;
                    }
                    
                    Navigator navigator = npc.getNavigator();
                    Location runTo = findRunAwayLocation(player);
                    if (runTo != null) {
                        navigator.setTarget(runTo);
                    }
                }
                
                // If player is looking directly at Herobrine and is within 20 blocks, higher chance to vanish
                if (isPlayerLookingAt(player, npcLoc) && 
                    calculateSafeDistance(playerLoc, npcLoc) < 20) {
                    
                    // Adjust vanish chance based on memory
                    double vanishChance = 0.4;
                    
                    // If player frequently chases, increase vanish chance
                    if (memory.hasAggressivelyChased) {
                        vanishChance += 0.1;
                    }
                    
                    // If player has seen Herobrine many times, slightly decrease chance
                    vanishChance -= Math.min(0.15, memory.encounterCount * 0.01);
                    
                    if (Math.random() < vanishChance) {
                        memory.fleeCount++;
                        memory.lastFleeTime = System.currentTimeMillis();
                        plugin.getEffectManager().playAppearanceEffects(player, npcLoc);
                        removeAppearance(player);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        
        // Check if Herobrine is nearby
        if (isHerobrineNearby(player)) {
            event.setCancelled(true);
            plugin.getEffectManager().playSleepPreventionEffects(player);
            return;
        }
        
        // Even if Herobrine isn't actively stalking, there's a small chance he'll appear
        if (Math.random() < 0.2) { // 20% chance
            event.setCancelled(true);
            plugin.getEffectManager().playSleepPreventionEffects(player);
            createWindowAppearance(player, event.getBed().getLocation());
        }
    }

    private void createWindowAppearance(Player player, Location targetLocation) {
        // Find a suitable window location near the target
        Location windowLoc = findWindowLocation(targetLocation);
        if (windowLoc == null) return;

        // Create Herobrine at the window
        NPC npc = registry.createNPC(EntityType.PLAYER, "Herobrine");
        npc.setProtected(true);
        
        // Set skin
        SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
        skinTrait.setSkinPersistent("Herobrine", SIGNATURE, TEXTURE);
        
        // Make NPC look at player
        npc.spawn(windowLoc);
        Vector direction = player.getLocation().toVector().subtract(windowLoc.toVector()).normalize();
        windowLoc.setDirection(direction);
        npc.teleport(windowLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        
        // Set player filter
        PlayerFilter filterTrait = npc.getOrAddTrait(PlayerFilter.class);
        filterTrait.setAllowlist();
        filterTrait.addPlayer(player.getUniqueId());

        // Store the NPC
        activeAppearances.put(player.getUniqueId(), npc);
        
        // Schedule removal after a short time
        new BukkitRunnable() {
            @Override
            public void run() {
                if (npc.isSpawned()) {
                    Location disappearLoc = npc.getEntity().getLocation();
                    plugin.getEffectManager().playAppearanceEffects(player, disappearLoc);
                    removeAppearance(player);
                }
            }
        }.runTaskLater(plugin, 100L); // 5 seconds
        
        // Play effects
        plugin.getEffectManager().playAppearanceEffects(player, windowLoc);
    }

    private Location findWindowLocation(Location targetLocation) {
        // Search for glass panes or glass blocks near the target location
        int radius = 5;
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = 0; y <= 2; y++) { // Search up to 2 blocks above ground level
                for (int z = -radius; z <= radius; z++) {
                    Location loc = targetLocation.clone().add(x, y, z);
                    Block block = loc.getBlock();
                    
                    // Check if block is a window (glass pane or glass block)
                    if (block.getType().name().contains("GLASS")) {
                        // Find a position just outside the window
                        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
                        for (BlockFace face : faces) {
                            Block relative = block.getRelative(face);
                            if (relative.getType() == Material.AIR) {
                                Location windowLoc = relative.getLocation().add(0.5, 0, 0.5);
                                // Make sure there's room for Herobrine to stand
                                if (relative.getRelative(BlockFace.UP).getType() == Material.AIR) {
                                    return windowLoc;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks if a Herobrine entity is currently present near a player
     * @param player The player to check for nearby Herobrine
     * @return true if Herobrine is already nearby, false otherwise
     */
    private boolean isHerobrineNearby(Player player) {
        // Search for any Herobrine entities within 50 blocks of the player
        for (Entity entity : player.getWorld().getEntities()) {
            // Check if the entity is a NPC/Citizens entity with Herobrine traits
            if (entity.hasMetadata("NPC") && calculateSafeDistance(entity.getLocation(), player.getLocation()) < 50) {
                // Check if this NPC is a Herobrine entity
                if (entity.hasMetadata("herobrine")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void createTripwireTrap(Location location) {
        // Create a 3x3x3 chamber underground
        Location base = location.clone().subtract(1, 3, 1); // Moved down by 3 blocks
        
        // Clear the area first and create stone walls
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    Location blockLoc = base.clone().add(x, y, z);
                    // Make walls out of stone
                    if (x == 0 || x == 2 || z == 0 || z == 2 || y == 0) {
                        blockLoc.getBlock().setType(Material.STONE);
                    } else {
                        blockLoc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }

        // Place TNT under where the tripwire hooks will be
        base.clone().add(0, 0, 1).getBlock().setType(Material.TNT);
        base.clone().add(2, 0, 1).getBlock().setType(Material.TNT);
        
        // Place tripwire hooks and string
        Block hook1 = base.clone().add(0, 1, 1).getBlock();
        Block hook2 = base.clone().add(2, 1, 1).getBlock();
        Block wire = base.clone().add(1, 1, 1).getBlock();
        
        // Set hooks
        hook1.setType(Material.TRIPWIRE_HOOK);
        hook2.setType(Material.TRIPWIRE_HOOK);
        
        // Configure hooks to face each other
        TripwireHook hook1Data = (TripwireHook) hook1.getBlockData();
        TripwireHook hook2Data = (TripwireHook) hook2.getBlockData();
        
        hook1Data.setFacing(BlockFace.EAST);
        hook2Data.setFacing(BlockFace.WEST);
        hook1Data.setAttached(true);
        hook2Data.setAttached(true);
        
        hook1.setBlockData(hook1Data);
        hook2.setBlockData(hook2Data);
        
        // Place tripwire string
        wire.setType(Material.TRIPWIRE);
        
        // Cover the trap with natural blocks
        Material surfaceMaterial = location.getBlock().getType();
        if (surfaceMaterial == Material.AIR || surfaceMaterial == Material.CAVE_AIR) {
            surfaceMaterial = Material.GRASS_BLOCK;
        }

        // Cover top layer with surface material, leaving center open for tripwire
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                if (x != 1 || z != 1) { // Skip center block
                    base.clone().add(x, 2, z).getBlock().setType(surfaceMaterial);
                }
            }
        }
        
        // Add some natural camouflage around the trap
        addNaturalBlocks(location, 2);

        // Register the structure with AggressionManager
        plugin.getAggressionManager().registerStructure(location);
    }

    private void placeCreepySign(Location location) {
        // Find a suitable location for the sign
        Location signLoc = findSignLocation(location);
        if (signLoc == null) return;

        // Place a fence post first if it's not against a wall
        Block targetBlock = signLoc.getBlock();
        Block belowBlock = targetBlock.getRelative(BlockFace.DOWN);
        
        if (belowBlock.getType() == Material.AIR || belowBlock.getType() == Material.CAVE_AIR) {
            belowBlock.setType(Material.OAK_FENCE);
            targetBlock.setType(Material.OAK_SIGN);
        } else {
            targetBlock.setType(Material.OAK_WALL_SIGN);
        }
        
        // Set the sign text
        if (targetBlock.getState() instanceof Sign) {
            Sign sign = (Sign) targetBlock.getState();
            String[] messages = {
                "WAKE UP",
                "I AM WATCHING",
                "YOU ARE NOT SAFE",
                "BEHIND YOU",
                "I SEE YOU",
                "RUN"
            };
            String message = messages[random.nextInt(messages.length)];
            
            // Update sign text using the new API
            sign.getSide(Side.FRONT).setLine(1, message);
            sign.update();
        }

        // Register the structure with AggressionManager
        plugin.getAggressionManager().registerStructure(signLoc);
    }

    private Location findSignLocation(Location center) {
        // Try to find a suitable wall or ground location
        Block targetBlock = center.getBlock();
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        
        // First try to find a solid wall to place the sign on
        for (BlockFace face : faces) {
            Block relative = targetBlock.getRelative(face);
            if (relative.getType().isSolid()) {
                return targetBlock.getLocation();
            }
        }
        
        // If no wall found, place on the ground with a fence post
        if (targetBlock.getType() == Material.AIR && 
            targetBlock.getRelative(BlockFace.DOWN).getType().isSolid()) {
            return targetBlock.getLocation();
        }
        
        return null;
    }

    private void addNaturalBlocks(Location center, int radius) {
        Material[] naturalBlocks = {
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.STONE,
            Material.COBBLESTONE,
            Material.MOSS_BLOCK
        };
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (random.nextDouble() < 0.3) { // 30% chance to place a block
                    Location loc = center.clone().add(x, 0, z);
                    if (loc.getBlock().getType() == Material.AIR) {
                        loc.getBlock().setType(naturalBlocks[random.nextInt(naturalBlocks.length)]);
                    }
                }
            }
        }
    }

    private int getServerViewDistance() {
        // Get server view distance in blocks (16 blocks per chunk)
        return plugin.getServer().getViewDistance() * 16;
    }

    private Location findStalkLocation(Player player) {
        Location playerLoc = player.getLocation();
        int viewDistance = getServerViewDistance();
        
        // Try to find a location within view distance but not too close
        for (int attempts = 0; attempts < 10; attempts++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = viewDistance * 0.3 + random.nextDouble() * (viewDistance * 0.7); // Between 30% and 100% of view distance
            
            Location loc = playerLoc.clone().add(
                Math.cos(angle) * distance,
                0,
                Math.sin(angle) * distance
            );
            
            // Adjust Y coordinate to ground level
            loc.setY(loc.getWorld().getHighestBlockYAt(loc));
            
            // Check if location is suitable
            if (isLocationSafe(loc)) {
                return loc;
            }
        }
        return null;
    }

    private Location findRunAwayLocation(Player player) {
        Location playerLoc = player.getLocation();
        int viewDistance = getServerViewDistance();
        
        // Try to find a location just at the edge of view distance
        for (int attempts = 0; attempts < 10; attempts++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = viewDistance * 0.8 + random.nextDouble() * (viewDistance * 0.2); // Between 80% and 100% of view distance
            
            Location loc = playerLoc.clone().add(
                Math.cos(angle) * distance,
                0,
                Math.sin(angle) * distance
            );
            
            // Adjust Y coordinate to ground level
            loc.setY(loc.getWorld().getHighestBlockYAt(loc));
            
            // Check if location is suitable
            if (isLocationSafe(loc)) {
                return loc;
            }
        }
        return null;
    }

    private boolean isPlayerLookingAt(Player player, Location target) {
        Location eyeLocation = player.getEyeLocation();
        Vector toEntity = target.toVector().subtract(eyeLocation.toVector());
        double dot = toEntity.normalize().dot(eyeLocation.getDirection());
        
        // Check if player is looking within 10 degrees of Herobrine
        return dot > 0.985; // cos(10 degrees) ≈ 0.985
    }

    private void startCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        
        // Run cleanup every 5 minutes
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Remove any NPCs for offline players
            for (UUID playerId : new HashSet<>(activeAppearances.keySet())) {
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline()) {
                    removeAppearance(player);
                }
            }
            
            // Check for any Herobrine NPCs that might have been missed
            for (NPC npc : registry.sorted()) {
                if (npc.getName().equals("Herobrine") && !activeAppearances.containsValue(npc)) {
                    if (npc.isSpawned()) {
                        npc.destroy();
                    }
                    registry.deregister(npc);
                }
            }
        }, 6000L, 6000L); // 5 minutes = 6000 ticks
    }

    private void createRandomStructure(Location location) {
        Map<String, Double> chances = plugin.getConfigManager().getStructureChances();
        
        // If this is tied to a player, potentially use their preferences
        Player nearestPlayer = findNearestPlayer(location, 50);
        if (nearestPlayer != null) {
            PlayerMemory memory = getPlayerMemory(nearestPlayer);
            
            // 60% chance to use adaptive behavior if player has enough encounters
            if (memory.encounterCount > 3 && Math.random() < 0.6) {
                // Choose structure based on what has been most effective for this player
                String mostEffectiveType = memory.getMostEffectiveStructure();
                
                createStructureByType(mostEffectiveType, location);
                
                // Register structure with nearest player for tracking reactions
                registerStructureWithPlayer(location, nearestPlayer, mostEffectiveType);
                return;
            }
        }
        
        // Default random behavior if not using adaptive logic
        double random = Math.random();
        double cumulative = 0.0;
        
        for (Map.Entry<String, Double> entry : chances.entrySet()) {
            cumulative += entry.getValue();
            if (random < cumulative) {
                createStructureByType(entry.getKey(), location);
                
                // Register structure with nearest player for tracking reactions
                if (nearestPlayer != null) {
                    registerStructureWithPlayer(location, nearestPlayer, entry.getKey());
                }
                return;
            }
        }
    }
    
    private void createStructureByType(String structureType, Location location) {
        switch (structureType) {
            case "sand_pyramids":
                createSandPyramid(location);
                break;
            case "redstone_caves":
                createRedstoneTorchCave(location);
                break;
            case "stripped_trees":
                createStrippedTrees(location);
                break;
            case "mysterious_tunnels":
                createMysteriousTunnel(location);
                break;
            case "glowstone_e":
                createGlowstoneE(location);
                break;
            case "wooden_crosses":
                createWoodenCross(location);
                break;
            case "tripwire_traps":
                createTripwireTrap(location);
                break;
            case "creepy_signs":
                placeCreepySign(location);
                break;
        }
    }
    
    // Track structures with associated players to measure reactions
    private final Map<Location, StructureTracking> structureTracking = new HashMap<>();
    
    /**
     * Tracks player interactions with Herobrine structures
     * Used for adaptive behavior learning system
     */
    private static class StructureTracking {
        final UUID playerId; // Player who encountered this structure
        final String structureType; // Type of structure created
        final long creationTime; // When the structure was created (for timing-based events)
        boolean playerHasSeen = false; // Tracks if player has seen the structure
        boolean playerHasReacted = false; // Tracks if player actively interacted with structure
        int reactionStrength = 0; // How strongly player reacted (0-10)
        
        StructureTracking(UUID playerId, String structureType) {
            this.playerId = playerId;
            this.structureType = structureType;
            this.creationTime = System.currentTimeMillis();
        }
    }
    
    private void registerStructureWithPlayer(Location location, Player player, String structureType) {
        StructureTracking tracking = new StructureTracking(player.getUniqueId(), structureType);
        structureTracking.put(location, tracking);
        
        // Schedule a task to check for player reaction
        Bukkit.getScheduler().runTaskLater(plugin, () -> 
            checkPlayerReactionToStructure(location, player), 100L); // Check after 5 seconds
        
        // Schedule cleanup
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            StructureTracking existingTracking = structureTracking.get(location);
            if (existingTracking != null) {
                // If the player reacted to the structure, record a final effectiveness boost
                if (existingTracking.playerHasReacted) {
                    Player targetPlayer = Bukkit.getPlayer(existingTracking.playerId);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        PlayerMemory memory = getPlayerMemory(targetPlayer);
                        // Structures that players actively interact with are more effective
                        memory.recordStructureReaction(existingTracking.structureType, existingTracking.reactionStrength + 2);
                    }
                }
                structureTracking.remove(location);
            }
        }, 12000L); // Remove after 10 minutes
    }
    
    private void checkPlayerReactionToStructure(Location location, Player player) {
        StructureTracking tracking = structureTracking.get(location);
        if (tracking == null || !player.isOnline()) return;
        
        // Calculate distance to structure
        double distance = calculateSafeDistance(player.getLocation(), location);
        
        // Check if player has seen the structure (came within 15 blocks)
        if (distance < 15) {
            tracking.playerHasSeen = true;
            
            // Player is examining the structure closely - strong reaction
            if (distance < 5) {
                tracking.playerHasReacted = true;
                tracking.reactionStrength = 8;
                
                // Record effectiveness in player memory
                PlayerMemory memory = getPlayerMemory(player);
                memory.recordStructureReaction(tracking.structureType, tracking.reactionStrength);
                
                // Examine how long the structure has existed
                long structureAge = System.currentTimeMillis() - tracking.creationTime;
                long fiveMinutesInMs = 5 * 60 * 1000;
                
                // If structure was found quickly, it was more effective
                if (structureAge < fiveMinutesInMs) {
                    // Increase the reaction strength based on how quickly it was found
                    int quickDiscoveryBonus = (int)(2.0 * (1.0 - (structureAge / (double)fiveMinutesInMs)));
                    tracking.reactionStrength += quickDiscoveryBonus;
                    
                    // Record updated effectiveness with bonus
                    memory.recordStructureReaction(tracking.structureType, tracking.reactionStrength);
                }
                
                // Chance for Herobrine to make an appearance if player is examining structure
                if (Math.random() < 0.3) {
                    Location appearLoc = findAppearanceLocation(player);
                    if (appearLoc != null) {
                        createAppearance(player, appearLoc);
                    }
                }
                return;
            }
            
            // Schedule another check if player has seen it but not reacted strongly yet
            Bukkit.getScheduler().runTaskLater(plugin, () -> 
                checkPlayerReactionToStructure(location, player), 100L); // Check again after 5 seconds
        } else if (!tracking.playerHasSeen) {
            // Player hasn't seen it yet, check again later
            Bukkit.getScheduler().runTaskLater(plugin, () -> 
                checkPlayerReactionToStructure(location, player), 200L); // Check again after 10 seconds
            
            // If structure has existed for more than 10 minutes without being seen,
            // record it as less effective
            long structureAge = System.currentTimeMillis() - tracking.creationTime;
            if (structureAge > 10 * 60 * 1000) { // 10 minutes
                // Use player ID to find the player memory
                UUID playerId = tracking.playerId;
                Player targetPlayer = Bukkit.getPlayer(playerId);
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    PlayerMemory memory = getPlayerMemory(targetPlayer);
                    // Record that this structure type wasn't very effective (not noticed)
                    memory.recordStructureReaction(tracking.structureType, 2); // Low effectiveness score
                }
            }
        }
    }
    
    // Find the nearest player to a location
    private Player findNearestPlayer(Location location, double maxDistance) {
        Player nearestPlayer = null;
        double nearestDistance = maxDistance;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(location.getWorld())) {
                double distance = calculateSafeDistance(player.getLocation(), location);
                if (distance < nearestDistance) {
                    nearestPlayer = player;
                    nearestDistance = distance;
                }
            }
        }
        
        return nearestPlayer;
    }

    // Memory management methods
    private PlayerMemory getPlayerMemory(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerMemory memory = playerMemories.get(playerId);
        if (memory == null) {
            memory = new PlayerMemory();
            playerMemories.put(playerId, memory);
        }
        return memory;
    }
    
    private void updatePlayerMemory(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerMemory memory = getPlayerMemory(player);
        memory.encounterCount++;
        memory.lastEncounterTime = System.currentTimeMillis();
        lastPlayerInteractions.put(playerId, System.currentTimeMillis());
        
        // If there's an aggression level for this player, use it to inform behavior
        float aggressionLevel = plugin.getAggressionManager().getAggressionLevel(player);
        if (aggressionLevel > 0.5f) {
            memory.playerIsAggressive = true;
        }
    }
    
    public void recordStructureDestroyed(Player player) {
        PlayerMemory memory = getPlayerMemory(player);
        memory.structuresDestroyed++;
        if (memory.structuresDestroyed >= 2) {
            memory.hasDestroyedStructures = true;
        }
    }
    
    private void startMemoryCleanupTask() {
        // Clean up old memories every hour
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            long oneWeekAgo = currentTime - (7 * 24 * 60 * 60 * 1000); // One week in milliseconds
            
            // Remove memories for players who haven't been seen in a week
            playerMemories.entrySet().removeIf(entry -> {
                PlayerMemory memory = entry.getValue();
                return memory.lastEncounterTime < oneWeekAgo;
            });
            
            // Remove last interaction records for players who haven't been seen in a week
            lastPlayerInteractions.entrySet().removeIf(entry -> entry.getValue() < oneWeekAgo);
            
        }, 72000L, 72000L); // Run every hour (72000 ticks)
    }
    
    /**
     * Player memory system to make Herobrine "learn" player behaviors and preferences
     * Creates personalized haunting experiences that evolve over time
     */
    private static class PlayerMemory {
        // Core encounter tracking
        int encounterCount = 0; // Total number of Herobrine encounters
        int spottedCount = 0; // Times player directly spotted Herobrine
        int chaseCount = 0; // Times player chased Herobrine
        int fleeCount = 0; // Times Herobrine fled from player
        int structuresDestroyed = 0; // Structures player has destroyed
        
        // Behavior flags
        boolean hasAggressivelyChased = false; // Player actively pursues Herobrine
        boolean hasDestroyedStructures = false; // Player destroys Herobrine's structures
        boolean playerIsAggressive = false; // Player has high aggression level
        
        // Timestamps
        long lastEncounterTime = 0; // Last time player encountered Herobrine
        long lastFleeTime = 0; // Last time Herobrine fled from player
        
        // Behavior adaptation tracking
        Map<String, Integer> structureEffectiveness = new HashMap<>(); // Track which structures are most effective
        
        /*
         * FUTURE EXPANSION FIELDS:
         * These fields will be used in future updates to enhance Herobrine's adaptive behaviors
         * by learning which tactics are most effective for each individual player
         */
        // int torchManipulationEffectiveness = 0; // How effective torch manipulation is for scaring this player
        // int windowAppearanceEffectiveness = 0; // How effective window appearances are
        // int stalkedPlayerReactions = 0; // How player reacts to being stalked
        // int footstepEffectiveness = 0; // How effective footstep sounds are
        
        /**
         * Records player's reaction to a structure type
         * Used for learning which structures are most effective
         */
        void recordStructureReaction(String structureType, int reactionStrength) {
            int current = structureEffectiveness.getOrDefault(structureType, 5);
            // Weighted average - 70% old value, 30% new reaction
            int newValue = (int)((current * 0.7) + (reactionStrength * 0.3));
            structureEffectiveness.put(structureType, Math.max(1, Math.min(10, newValue)));
        }
        
        /**
         * Returns the structure type that has been most effective for this player
         */
        String getMostEffectiveStructure() {
            return structureEffectiveness.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("sand_pyramids"); // Default if no data
        }
    }

    private void createSandPyramid(Location location) {
        int size = plugin.getConfigManager().getPyramidSize();
        Location base = location.clone();
        
        base.setY(base.getWorld().getHighestBlockYAt(base));
        
        for (int y = 0; y < size; y++) {
            int layerSize = size - y;
            for (int x = -layerSize; x <= layerSize; x++) {
                for (int z = -layerSize; z <= layerSize; z++) {
                    Location blockLoc = base.clone().add(x, y, z);
                    blockLoc.getBlock().setType(Material.SAND);
                }
            }
        }

        plugin.getAggressionManager().registerStructure(location);
        plugin.getEffectManager().playStructureManipulationEffects(location);
    }

    private void createRedstoneTorchCave(Location location) {
        Location entrance = location.clone();
        entrance.setY(entrance.getWorld().getHighestBlockYAt(entrance));
        
        Vector direction = new Vector(
            Math.round(Math.random() * 2 - 1),
            -0.5,
            Math.round(Math.random() * 2 - 1)
        ).normalize();

        int minLength = plugin.getConfigManager().getRedstoneCaveMinLength();
        int maxLength = plugin.getConfigManager().getRedstoneCaveMaxLength();
        int length = minLength + random.nextInt(maxLength - minLength + 1);
        int torchInterval = plugin.getConfigManager().getRedstoneTorchInterval();
        
        Location current = entrance.clone();
        
        for (int i = 0; i < length; i++) {
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    Location blockLoc = current.clone().add(x, y, 0);
                    blockLoc.getBlock().setType(Material.AIR);
                }
            }
            
            if (i % torchInterval == 0) {
                Location torchLoc = current.clone().add(1, 0, 0);
                torchLoc.getBlock().setType(Material.REDSTONE_WALL_TORCH);
            }
            
            current.add(direction.clone().multiply(1));
        }
        
        createTrapChamber(current);
        
        plugin.getAggressionManager().registerStructure(location);
        plugin.getEffectManager().playStructureManipulationEffects(location);
    }

    private void createTrapChamber(Location location) {
        // Create a 4x4x4 chamber
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    location.clone().add(x, y, z).getBlock().setType(Material.AIR);
                }
            }
        }
        
        location.clone().add(1, 1, 0).getBlock().setType(Material.REDSTONE_WALL_TORCH);
        location.clone().add(2, 1, 0).getBlock().setType(Material.REDSTONE_WALL_TORCH);
        
        int tntCount = plugin.getConfigManager().getTripwireTrapTNTCount();
        for (int i = 0; i < tntCount; i++) {
            int x = 1 + (i % 2);
            int z = 1 + (i / 2);
            location.clone().add(x, 0, z).getBlock().setType(Material.TNT);
        }
        
        location.clone().add(2, 1, 2).getBlock().setType(Material.STONE_PRESSURE_PLATE);
        location.clone().add(1, 1, 2).getBlock().setType(Material.CHEST);
    }

    private void createStrippedTrees(Location location) {
        int radius = plugin.getConfigManager().getStrippedTreesRadius();
        int maxHeight = plugin.getConfigManager().getStrippedTreesMaxHeight();
        Location base = location.clone();
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Location treeLoc = base.clone().add(x, 0, z);
                treeLoc.setY(treeLoc.getWorld().getHighestBlockYAt(treeLoc));
                Block block = treeLoc.getBlock();
                
                if (block.getType().name().endsWith("LOG")) {
                    // Find the actual height of the tree
                    int treeHeight = 0;
                    while (treeHeight < maxHeight && 
                           block.getRelative(0, treeHeight, 0).getType().name().endsWith("LOG")) {
                        treeHeight++;
                    }
                    
                    // Remove leaves in a larger radius around the entire trunk
                    for (int y = 0; y < treeHeight + 3; y++) { // Go slightly above trunk height
                        for (int lx = -3; lx <= 3; lx++) {
                            for (int lz = -3; lz <= 3; lz++) {
                                Block leafBlock = block.getRelative(lx, y, lz);
                                if (leafBlock.getType().name().endsWith("LEAVES")) {
                                    leafBlock.setType(Material.AIR);
                                    // Add some particle effects for the leaves breaking
                                    leafBlock.getWorld().spawnParticle(
                                        Particle.CLOUD,
                                        leafBlock.getLocation().add(0.5, 0.5, 0.5),
                                        5, 0.2, 0.2, 0.2, 0.02
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }
        
        plugin.getAggressionManager().registerStructure(location);
        plugin.getEffectManager().playStructureManipulationEffects(location);
    }

    private void createMysteriousTunnel(Location location) {
        Location start = location.clone();
        int depth = plugin.getConfigManager().getMysteriousTunnelDepth();
        start.setY(start.getWorld().getHighestBlockYAt(start) - depth);
        
        Vector direction = new Vector(
            Math.round(Math.random() * 2 - 1),
            0,
            Math.round(Math.random() * 2 - 1)
        ).normalize();

        int minLength = plugin.getConfigManager().getMysteriousTunnelMinLength();
        int maxLength = plugin.getConfigManager().getMysteriousTunnelMaxLength();
        int length = minLength + random.nextInt(maxLength - minLength + 1);
        
        Location current = start.clone();
        
        for (int i = 0; i < length; i++) {
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    Location blockLoc = current.clone().add(x, y, 0);
                    blockLoc.getBlock().setType(Material.AIR);
                    for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH}) {
                        Block adjacent = blockLoc.getBlock().getRelative(face);
                        if (adjacent.getType() != Material.AIR) {
                            adjacent.setType(Material.SMOOTH_STONE);
                        }
                    }
                }
            }
            current.add(direction.clone().multiply(1));
        }
        
        plugin.getAggressionManager().registerStructure(location);
        plugin.getEffectManager().playStructureManipulationEffects(location);
    }

    private void createGlowstoneE(Location location) {
        Location base = location.clone();
        int depth = plugin.getConfigManager().getGlowstoneEDepth();
        base.setY(base.getWorld().getHighestBlockYAt(base) - depth);
        
        // Clear space for the E
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 2; z++) {
                    base.clone().add(x, y, z).getBlock().setType(Material.AIR);
                }
            }
        }
        
        // Create E pattern with glowstone
        int[][] ePattern = {
            {1,1}, // Top
            {1,0},
            {1,1}, // Middle
            {1,0},
            {1,1}  // Bottom
        };
        
        for (int y = 0; y < ePattern.length; y++) {
            for (int x = 0; x < ePattern[y].length; x++) {
                if (ePattern[y][x] == 1) {
                    base.clone().add(x, 4-y, 0).getBlock().setType(Material.GLOWSTONE);
                }
            }
        }
        
        // Register and play effects
        plugin.getAggressionManager().registerStructure(location);
        plugin.getEffectManager().playStructureManipulationEffects(location);
    }

    private void createWoodenCross(Location location) {
        Location base = location.clone();
        base.setY(base.getWorld().getHighestBlockYAt(base));
        
        int height = plugin.getConfigManager().getWoodenCrossHeight();
        
        // Create vertical part
        for (int y = 0; y < height; y++) {
            base.clone().add(0, y, 0).getBlock().setType(Material.OAK_PLANKS);
        }
        
        // Create horizontal part
        base.clone().add(-1, height/2, 0).getBlock().setType(Material.OAK_PLANKS);
        base.clone().add(1, height/2, 0).getBlock().setType(Material.OAK_PLANKS);
        
        plugin.getAggressionManager().registerStructure(location);
        plugin.getEffectManager().playStructureManipulationEffects(location);
    }

    private Location findNearbyChest(Location center) {
        int radius = 10;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location loc = center.clone().add(x, y, z);
                    Block block = loc.getBlock();
                    if (block.getType() == Material.CHEST) {
                        // Make sure the chest isn't full
                        Chest chest = (Chest) block.getState();
                        if (chest.getInventory().firstEmpty() != -1) {
                            return loc;
                        }
                    }
                }
            }
        }
        return null;
    }

    private double calculateAdaptiveAppearanceChance(Player player, PlayerMemory memory) {
        double baseChance = plugin.getConfigManager().getAppearanceChance();
        double adjustedChance = baseChance;
        
        // Increase chance slightly for players who have seen Herobrine more often
        // (familiarity breeds curiosity)
        if (memory.encounterCount > 5) {
            adjustedChance += Math.min(0.1, memory.encounterCount * 0.01);
        }
        
        // Decrease chance for players who have seen Herobrine very recently
        // to avoid flooding them with appearances
        long timeSinceLastEncounter = System.currentTimeMillis() - memory.lastEncounterTime;
        long oneHourInMillis = 60 * 60 * 1000;
        
        if (memory.lastEncounterTime > 0 && timeSinceLastEncounter < oneHourInMillis) {
            // Reduce chance if it's been less than an hour since last encounter
            adjustedChance *= (0.3 + (0.7 * timeSinceLastEncounter / oneHourInMillis));
        }
        
        // Increase chance based on player's observed behavior patterns
        // More active players see Herobrine more often
        if (isPlayerActive(player)) {
            adjustedChance *= 1.2;
        }
        
        // Increase chance during night time or in dark areas
        if (isEnvironmentDark(player)) {
            adjustedChance *= 1.5;
        }
        
        // Increase chance if player is in an isolated/vulnerable location
        if (isPlayerIsolated(player)) {
            adjustedChance *= 1.3;
        }
        
        // Use additional memory metrics to adjust chance
        
        // Players who actively look for Herobrine (high spotted count) see him more often
        if (memory.spottedCount > 5) {
            adjustedChance *= 1.0 + Math.min(0.3, memory.spottedCount * 0.02);
        }
        
        // Aggressive players who scare Herobrine away often (high flee count) see him less frequently
        if (memory.fleeCount > 3) {
            // Herobrine becomes more cautious around players who made him flee a lot
            adjustedChance *= Math.max(0.6, 1.0 - (memory.fleeCount * 0.05));
        }
        
        // Adjust based on time since last flee
        if (memory.lastFleeTime > 0) {
            long timeSinceLastFlee = System.currentTimeMillis() - memory.lastFleeTime;
            // Gradually increase chance as more time passes since last flee
            if (timeSinceLastFlee < 30 * 60 * 1000) { // 30 minutes
                adjustedChance *= Math.max(0.3, timeSinceLastFlee / (30.0 * 60 * 1000));
            }
        }
        
        // Players with high aggression from AggressionManager have higher chance
        if (memory.playerIsAggressive) {
            adjustedChance *= 1.4; // More aggressive haunting for players who trigger aggression
        }
        
        // Cap the final chance
        return Math.min(0.5, adjustedChance);
    }

    private void scheduleAppearanceForPlayer(Player player, PlayerMemory memory) {
        // Determine how long to wait before appearing
        long delay = calculateAppearanceDelay(player, memory);
        
        // Schedule the appearance
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Double-check player is still online and Herobrine isn't already present
            if (player.isOnline() && !isHerobrineNearby(player)) {
                createAppearance(player);
            }
        }, delay);
    }
    
    private long calculateAppearanceDelay(Player player, PlayerMemory memory) {
        // Base delay between 5-30 seconds
        long baseDelay = 5 + random.nextInt(25);
        
        // Shorter delays for players who chase/pursue Herobrine (reward curiosity)
        if (memory.hasAggressivelyChased) {
            baseDelay = Math.max(5, baseDelay - 10);
        }
        
        // Players with high spotting count get faster appearances (Herobrine likes an audience)
        if (memory.spottedCount > 5) {
            baseDelay = Math.max(3, baseDelay - (memory.spottedCount / 5));
        }
        
        // Longer delays when player is in a safe/lit area
        if (!isEnvironmentDark(player)) {
            baseDelay += 10;
        }
        
        // Longer delays for players who recently made Herobrine flee
        if (memory.lastFleeTime > 0) {
            long timeSinceLastFlee = System.currentTimeMillis() - memory.lastFleeTime;
            if (timeSinceLastFlee < 10 * 60 * 1000) { // 10 minutes
                // Add up to 20 seconds delay for recent flee events
                baseDelay += Math.max(0, 20 - (timeSinceLastFlee / (30 * 1000)));
            }
        }
        
        // Aggressive players get more immediate responses (Herobrine fights back)
        if (memory.playerIsAggressive) {
            baseDelay = Math.max(2, baseDelay - 15); // Much quicker responses
        }
        
        return baseDelay * 20L; // Convert to ticks
    }
    
    private boolean isPlayerActive(Player player) {
        // This could be expanded with more sophisticated activity tracking
        return player.getStatistic(org.bukkit.Statistic.WALK_ONE_CM) > 
               player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 2;
    }
    
    private boolean isEnvironmentDark(Player player) {
        // Check both world time and block light level
        boolean isNightTime = player.getWorld().getTime() > 12000 && player.getWorld().getTime() < 24000;
        boolean isDarkLocation = player.getLocation().getBlock().getLightLevel() < 8;
        
        return isNightTime || isDarkLocation;
    }
    
    /**
     * Determines if a player is in an isolated location
     * Isolated players are more likely to encounter Herobrine
     */
    private boolean isPlayerIsolated(Player player) {
        // Check if player is alone (no other players nearby)
        for (Player otherPlayer : player.getWorld().getPlayers()) {
            if (otherPlayer != player && calculateSafeDistance(otherPlayer.getLocation(), player.getLocation()) < 50) {
                return false; // Not isolated - other players are nearby
            }
        }
        
        // Check if player is underground (underground locations feel more isolated)
        int highestY = player.getWorld().getHighestBlockYAt(player.getLocation());
        boolean isUnderground = player.getLocation().getBlockY() < highestY - 5;
        
        // Being underground increases the feeling of isolation
        return isUnderground || player.getWorld().getTime() > 13000; // Underground or nighttime
    }

    /**
     * Check if paranoia-based appearances (distant or peripheral) should be created
     * @param player The player
     * @param memory The player's memory
     */
    private void checkParanoiaAppearances(Player player, PlayerMemory memory) {
        // Skip if player already has an active appearance
        if (activeAppearances.containsKey(player.getUniqueId())) {
            return;
        }
        
        ParanoiaManager paranoiaManager = plugin.getParanoiaManager();
        if (paranoiaManager == null) return;
        
        // Check for distant silhouette appearances
        if (paranoiaManager.shouldCreateDistantAppearance(player)) {
            createDistantAppearance(player);
            return;
        }
        
        // Check for peripheral vision appearances
        if (paranoiaManager.shouldCreatePeripheralAppearance(player)) {
            createPeripheralAppearance(player);
            return;
        }
    }
    
    /**
     * Create a distant silhouette appearance of Herobrine
     * @param player The player to create the appearance for
     */
    private void createDistantAppearance(Player player) {
        // Get a far distance based on player's exposure level
        int distance = plugin.getParanoiaManager().getFarAppearanceDistance(player);
        
        // Find a valid location for the distant appearance
        Location playerLoc = player.getLocation();
        
        // Choose random angle
        double angle = Math.random() * 2 * Math.PI;
        
        // Calculate position at that distance and angle
        double x = Math.sin(angle) * distance;
        double z = Math.cos(angle) * distance;
        
        Location targetLoc = playerLoc.clone().add(x, 0, z);
        
        // Adjust Y to ground level
        targetLoc.setY(targetLoc.getWorld().getHighestBlockYAt(targetLoc) + 1);
        
        // Create the appearance
        createAppearance(player, targetLoc);
        
        // Record the distant encounter in paranoia system
        plugin.getParanoiaManager().recordEncounter(player, ParanoiaManager.EncounterType.DISTANT);
        
        // Schedule quick disappearance
        final UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeAppearances.containsKey(playerId)) {
                removeAppearance(Bukkit.getPlayer(playerId));
            }
        }, 100L); // 5 seconds
    }
    
    /**
     * Create a peripheral vision appearance (only visible from the corner of the eye)
     * @param player The player to create the appearance for
     */
    private void createPeripheralAppearance(Player player) {
        Location playerLoc = player.getLocation();
        double playerYaw = Math.toRadians(playerLoc.getYaw());
        
        // Calculate position 90 degrees to the side of player's view
        double sideAngle = playerYaw + (Math.PI / 2) * (Math.random() > 0.5 ? 1 : -1);
        double distance = 15 + (Math.random() * 10); // 15-25 blocks away
        
        double x = Math.sin(sideAngle) * distance;
        double z = Math.cos(sideAngle) * distance;
        
        Location targetLoc = playerLoc.clone().add(x, 0, z);
        
        // Adjust Y to ground level
        targetLoc.setY(targetLoc.getWorld().getHighestBlockYAt(targetLoc) + 1);
        
        // Create the appearance
        createAppearance(player, targetLoc);
        
        // Record the peripheral encounter in paranoia system
        plugin.getParanoiaManager().recordEncounter(player, ParanoiaManager.EncounterType.PERIPHERAL);
        
        // Set up a task to check if player is looking directly at Herobrine
        final UUID playerId = player.getUniqueId();
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                Player p = Bukkit.getPlayer(playerId);
                if (p == null || !p.isOnline() || !activeAppearances.containsKey(playerId)) {
                    cancel();
                    return;
                }
                
                NPC npc = activeAppearances.get(playerId);
                if (!npc.isSpawned()) {
                    cancel();
                    return;
                }
                
                // If player looks directly at Herobrine, check if he should vanish
                if (isPlayerLookingAt(p, npc.getEntity().getLocation())) {
                    if (plugin.getParanoiaManager().shouldVanishWhenSeen(p)) {
                        removeAppearance(p);
                        cancel();
                    }
                }
                
                // Auto-disappear after a while
                ticks++;
                if (ticks >= 200) { // 10 seconds max
                    removeAppearance(p);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    /**
     * Safely calculates distance between two locations, handling cross-dimension cases
     * @param loc1 First location
     * @param loc2 Second location
     * @return Distance between locations, or a large value if in different worlds
     */
    private double calculateSafeDistance(Location loc1, Location loc2) {
        // If locations are in different worlds, return a large distance
        if (loc1 == null || loc2 == null || !loc1.getWorld().equals(loc2.getWorld())) {
            return Double.MAX_VALUE; // Essentially "infinite" distance
        }
        
        // Safe to calculate distance when in the same world
        return loc1.distance(loc2);
    }
} 