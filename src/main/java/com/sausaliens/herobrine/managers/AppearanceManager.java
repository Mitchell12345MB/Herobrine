package com.sausaliens.herobrine.managers;

import com.sausaliens.herobrine.HerobrinePlugin;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.PlayerFilter;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
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

    private static final int VIEW_DISTANCE = 32; // 2 chunks

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
                
                // Check if player is too close (within 10 blocks)
                if (player.getLocation().distance(npc.getEntity().getLocation()) < 10) {
                    // 80% chance to vanish when player gets too close
                    if (Math.random() < 0.8) {
                        Location disappearLoc = npc.getEntity().getLocation();
                        plugin.getEffectManager().playAppearanceEffects(player, disappearLoc);
                        removeAppearance(player);
                        cancel();
                        return;
                    }
                }
                
                // Every 2 seconds (40 ticks), decide what to do
                if (ticksExisted % 40 == 0) {
                    double rand = Math.random();
                    Location playerLoc = player.getLocation();
                    
                    if (rand < 0.3) { // 30% chance to create a trap or structure
                        Location trapLoc = findSuitableLocation(playerLoc, 10, 20);
                        if (trapLoc != null) {
                            createRandomStructure(trapLoc);
                        }
                    } else if (rand < 0.7) { // 40% chance to stalk
                        Location stalkLoc = findStalkLocation(player);
                        if (stalkLoc != null) {
                            npc.teleport(stalkLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                            // Play stalk effects
                            plugin.getEffectManager().playStalkEffects(player, stalkLoc);
                            // Add footsteps
                            plugin.getEffectManager().playFootstepEffects(player);
                            // Manipulate nearby torches
                            if (random.nextDouble() < 0.3) { // 30% chance to mess with torches
                                plugin.getEffectManager().manipulateTorches(stalkLoc, 10);
                            }
                            // Chance to leave items in nearby chests
                            if (random.nextDouble() < 0.15) { // 15% chance to leave items
                                Location chestLoc = findNearbyChest(stalkLoc);
                                if (chestLoc != null) {
                                    plugin.getEffectManager().leaveChestDonation(chestLoc);
                                }
                            }
                        }
                    } else { // 30% chance to vanish and reappear elsewhere
                        Location disappearLoc = npc.getEntity().getLocation();
                        plugin.getEffectManager().playAppearanceEffects(player, disappearLoc);
                        
                        // Find a new location to appear
                        Location newLoc = findAppearanceLocation(player);
                        if (newLoc != null) {
                            npc.teleport(newLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                            plugin.getEffectManager().playAppearanceEffects(player, newLoc);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 1L);
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
                
                // Make Herobrine always face the player
                Vector direction = playerLoc.toVector().subtract(npcLoc.toVector()).normalize();
                npcLoc.setDirection(direction);
                npc.teleport(npcLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                
                // If player is looking directly at Herobrine and is within 20 blocks, higher chance to vanish
                if (isPlayerLookingAt(player, npc.getEntity().getLocation()) && 
                    playerLoc.distance(npcLoc) < 20 && 
                    Math.random() < 0.4) { // 40% chance to vanish when looked at
                    
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
        
        // Clear the area first
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    base.clone().add(x, y, z).getBlock().setType(Material.AIR);
                }
            }
        }

        // Place TNT in a 2x2 pattern
        base.clone().add(0, 0, 0).getBlock().setType(Material.TNT);
        base.clone().add(1, 0, 0).getBlock().setType(Material.TNT);
        base.clone().add(0, 0, 1).getBlock().setType(Material.TNT);
        base.clone().add(1, 0, 1).getBlock().setType(Material.TNT);
        
        // Place tripwire hooks facing each other
        Location hook1 = base.clone().add(0, 1, 1);
        Location hook2 = base.clone().add(2, 1, 1);
        
        hook1.getBlock().setType(Material.TRIPWIRE_HOOK);
        hook2.getBlock().setType(Material.TRIPWIRE_HOOK);
        
        // Configure hooks to face each other
        BlockData hook1Data = hook1.getBlock().getBlockData();
        BlockData hook2Data = hook2.getBlock().getBlockData();
        if (hook1Data instanceof TripwireHook && hook2Data instanceof TripwireHook) {
            ((TripwireHook) hook1Data).setFacing(BlockFace.EAST);
            ((TripwireHook) hook1Data).setAttached(true);
            ((TripwireHook) hook2Data).setFacing(BlockFace.WEST);
            ((TripwireHook) hook2Data).setAttached(true);
            hook1.getBlock().setBlockData(hook1Data);
            hook2.getBlock().setBlockData(hook2Data);
        }
        
        // Place string between hooks
        base.clone().add(1, 1, 1).getBlock().setType(Material.TRIPWIRE);
        
        // Cover the trap with natural blocks
        Material surfaceMaterial = location.getBlock().getType();
        if (surfaceMaterial == Material.AIR || surfaceMaterial == Material.CAVE_AIR) {
            surfaceMaterial = Material.GRASS_BLOCK;
        }

        // Fill sides with stone to prevent TNT from being visible
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                // Fill sides at TNT level
                if (x == 0 || x == 2 || z == 0 || z == 2) {
                    base.clone().add(x, 0, z).getBlock().setType(Material.STONE);
                }
                
                // Cover top layer with surface material
                Block coverBlock = base.clone().add(x, 2, z).getBlock();
                if (x == 1 && z == 1) {
                    // Leave the center uncovered for the tripwire
                    coverBlock.setType(Material.AIR);
                } else {
                    coverBlock.setType(surfaceMaterial);
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

    private Location findStalkLocation(Player player) {
        Location playerLoc = player.getLocation();
        
        // Try to find a location just outside view distance
        for (int attempts = 0; attempts < 10; attempts++) {
            // Get random angle and distance
            double angle = Math.random() * 2 * Math.PI;
            double distance = VIEW_DISTANCE + (Math.random() * 5); // Between view distance and +5 blocks
            
            // Calculate offset
            double x = Math.cos(angle) * distance;
            double z = Math.sin(angle) * distance;
            
            // Find a suitable Y coordinate
            Location loc = playerLoc.clone().add(x, 0, z);
            loc.setY(playerLoc.getY());
            
            // Check if location is suitable
            Block block = loc.getBlock();
            Block above = block.getRelative(BlockFace.UP);
            Block below = block.getRelative(BlockFace.DOWN);
            
            if (below.getType().isSolid() && 
                !block.getType().isSolid() && 
                !above.getType().isSolid()) {
                
                // Make Herobrine face the player
                Vector direction = playerLoc.toVector().subtract(loc.toVector()).normalize();
                loc.setDirection(direction);
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
                
                if (block.getType().name().endsWith("_LOG")) {
                    for (int y = 0; y < maxHeight; y++) {
                        for (int lx = -2; lx <= 2; lx++) {
                            for (int lz = -2; lz <= 2; lz++) {
                                Block leafBlock = block.getRelative(lx, y, lz);
                                if (leafBlock.getType().name().endsWith("_LEAVES")) {
                                    leafBlock.setType(Material.AIR);
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