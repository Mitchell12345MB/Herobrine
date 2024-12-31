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

import java.util.*;

public class AppearanceManager implements Listener {
    private final HerobrinePlugin plugin;
    private final Random random;
    private final Map<UUID, NPC> activeAppearances;
    private final NPCRegistry registry;
    private BukkitTask appearanceTask;
    private BukkitTask cleanupTask;

    // Herobrine's skin texture and signature from MineSkin.org
    private static final String TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTYxMTk3MTk0NTY0NiwKICAicHJvZmlsZUlkIiA6ICIwNWQ0NTNiZWE0N2Y0MThiOWI2ZDUzODg0MWQxMDY2MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJFY2hvcnJhIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzhkYzYyZDA5ZmEyNmEyMmNkNjU0MWU5N2UwNjE0ZjA4MTQ3YzBjNWFmNGU0MzM3NzY3ZjMxMThmYWQyODExOTYiCiAgICB9CiAgfQp9";
    private static final String SIGNATURE = "CFivZK2Du6OXoEa/G7znPDAv0eGMLOc69aKF6HUvk1woJCzqwIfx/aIZdaKyu0SMPQAcX5ta6zmp6FndHzBc4ehqQCvSlNQQxhYrAG4eaxGGDMYm6uFdPK0l1QamqZ+4EHR0VCayhtYKQcwghr1GkOoR8E3+FibwPZ0MICmovd6by9z/fbPymMIAkpgimsLe583OYO2ab7jsGMkpW5/mf10JQCLcRz2i8QAo0gLTJV5cyx7g2/v1mleLsV1JY3fFO7CmWsWtoamsJtCfW+z4Rs8xqvQunSDngWOIHvPDgAjTKAoGyCg8PlRu4om1URAIOi4xPX+B7z4kPpmEs7cWtlOgABWdsG6IUAZGe5nrL+OVgfJ5wSA+SPk882btwOdLzLa2FfEOOa169Gpfax4sFaQ6Y89ZM3RjtgEimjjUEbQvbj9tkOoT1FzRJ9UJXe933M92q82ikack8/VVOpzYgVbcEeO7hlzC/MfzEV1Iox4ZxYrUB899qDmQWgc4DuJ31V71bEP208ZmvFDffDOOFlO73yoyGt4LO2/IqynVRsnc9vMrf8e5z1WYCjopH6cs1cf/vov+oxZVsIL97Di3c8Ufr7YlUl4Rkp8G2nDHdMYIHKTKhwFMs9MBs/2wR9SUBUDi/2NIZvlbV/Efhk8fyDC0PYAbZJvEC5w01KBhRTg=";

    public AppearanceManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.activeAppearances = new HashMap<>();
        this.registry = CitizensAPI.getNPCRegistry();
        startAppearanceTimer();
        startCleanupTask();
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
            
            double chance = plugin.getConfigManager().getAppearanceChance();
            if (Math.random() < chance) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!isHerobrineNearby(player) && Math.random() < 0.3) { // 30% chance per player
                        createAppearance(player);
                    }
                }
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
    }

    public void createAppearance(Player player, Location location) {
        // Double check no existing NPC
        if (activeAppearances.containsKey(player.getUniqueId())) {
            removeAppearance(player);
        }
        
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
        params.useNewPathfinder(true); // Use the newer pathfinder for better results
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
                double distanceToPlayer = npcLoc.distance(playerLoc);
                
                // Check if player is too close (within 10 blocks)
                if (distanceToPlayer < 10) {
                    // If player is sprinting towards Herobrine, increase chance to run
                    double vanishChance = player.isSprinting() ? 0.9 : 0.8;
                    if (Math.random() < vanishChance) {
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
                        rand += 0.2; // Bias towards running away when looked at
                    }
                    
                    if (rand < 0.3) { // 30% chance to create a trap or structure
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
        int attempts = 0;
        int maxAttempts = 10;

        while (attempts++ < maxAttempts) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 10 + random.nextDouble() * 15; // Between 10-25 blocks away
            
            Location loc = playerLoc.clone().add(
                Math.cos(angle) * distance,
                0,
                Math.sin(angle) * distance
            );

            // Find the highest block at this location
            loc.setY(loc.getWorld().getHighestBlockYAt(loc));

            if (loc.getBlock().getType().isSolid()) {
                loc.add(0, 1, 0);
                // Make Herobrine face the player
                Vector direction = playerLoc.toVector().subtract(loc.toVector()).normalize();
                loc.setDirection(direction);
                return loc;
            }
        }
        return null;
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
                
                // React to player sprinting
                if (player.isSprinting() && playerLoc.distance(npcLoc) < 15) {
                    Navigator navigator = npc.getNavigator();
                    Location runTo = findRunAwayLocation(player);
                    if (runTo != null) {
                        navigator.setTarget(runTo);
                    }
                }
                
                // If player is looking directly at Herobrine and is within 20 blocks, higher chance to vanish
                if (isPlayerLookingAt(player, npcLoc) && 
                    playerLoc.distance(npcLoc) < 20 && 
                    Math.random() < 0.4) {
                    plugin.getEffectManager().playAppearanceEffects(player, npcLoc);
                    removeAppearance(player);
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

    private boolean isHerobrineNearby(Player player) {
        return activeAppearances.containsKey(player.getUniqueId());
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
        double random = Math.random();
        double cumulative = 0.0;
        
        for (Map.Entry<String, Double> entry : chances.entrySet()) {
            cumulative += entry.getValue();
            if (random < cumulative) {
                switch (entry.getKey()) {
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
                return;
            }
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
} 