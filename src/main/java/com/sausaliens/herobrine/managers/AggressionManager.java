package com.sausaliens.herobrine.managers;

import com.sausaliens.herobrine.HerobrinePlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.regex.Pattern;

public class AggressionManager implements Listener {
    private final HerobrinePlugin plugin;
    private final Map<UUID, Integer> nameCount;
    private final Map<UUID, Float> aggressionLevels;
    private final Map<Location, Long> herobrineStructures;
    private final Map<UUID, Long> markedPlayers;
    private final Map<UUID, Integer> structureBlocksDestroyed;
    private final Random random;
    private final Set<Location> structures = new HashSet<>();
    
    private static final Pattern INSULT_PATTERN = Pattern.compile(
        ".*(pussy|asshole|fart|cuck|dipshit|idiot|inbred|ass|dumbass|bastard|eat|shit|poof|git|wanker|sped|dusty|fuck|gay|screw|faggot|hate|cock|sucker|drop|soap|retard|l|looser|stupid|dumb|noob|weak|fake|trash|bad).*herobrine.*|" +
        ".*herobrine.*(pussy|asshole|fart|cuck|dipshit|idiot|inbred|ass|dumbass|bastard|eat|shit|poof|git|wanker|sped|dusty|fuck|gay|screw|faggot|hate|cock|sucker|drop|soap|retard|l|looser|stupid|dumb|noob|weak|fake|trash|bad).*|" +
        ".*herobrine.*joke.*|.*joke.*herobrine.*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final String[] DEATH_MESSAGES = {
        "§c%s learned not to mock ancient powers",
        "§c%s's arrogance led to their demise",
        "§c%s vanished without a trace",
        "§c%s should have shown more respect",
        "§c%s was claimed by darkness",
        "§c%s's last words were regrettable",
        "§c%s discovered the price of mockery",
        "§c%s found out Herobrine is very real"
    };

    public AggressionManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.nameCount = new HashMap<>();
        this.aggressionLevels = new HashMap<>();
        this.herobrineStructures = new HashMap<>();
        this.markedPlayers = new HashMap<>();
        this.structureBlocksDestroyed = new HashMap<>();
        this.random = new Random();
        
        // Start the revenge timer
        startRevengeTimer();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage().toLowerCase();
        Player player = event.getPlayer();
        
        if (INSULT_PATTERN.matcher(message).matches()) {
            // Mark the player for revenge
            markPlayerForRevenge(player);
            return;
        }
        
        if (message.contains("herobrine")) {
            UUID playerId = player.getUniqueId();
            
            // Increment name count
            nameCount.putIfAbsent(playerId, 0);
            int count = nameCount.get(playerId) + 1;
            nameCount.put(playerId, count);
            
            // Check if player has said the name three times
            if (count >= 3) {
                // Reset count
                nameCount.put(playerId, 0);
                
                // Increase aggression
                increaseAggression(player, 0.3f); // 30% increase
                
                // Schedule increased activity
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getAppearanceManager().createAppearance(player);
                });
            }
        }
    }

    public void markPlayerForRevenge(Player player) {
        // Mark player with current timestamp
        markedPlayers.put(player.getUniqueId(), System.currentTimeMillis());
        
        // Increase aggression to maximum
        increaseAggression(player, 1.0f);
        
        // Schedule immediate stalking and scare
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getAppearanceManager().createAppearance(player);
            playCreeperScare(player);
        });
    }

    private void startRevengeTimer() {
        // Check every 30 seconds for marked players
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            
            // Process each marked player
            for (Iterator<Map.Entry<UUID, Long>> it = markedPlayers.entrySet().iterator(); it.hasNext();) {
                Map.Entry<UUID, Long> entry = it.next();
                Player player = Bukkit.getPlayer(entry.getKey());
                
                if (player != null && player.isOnline()) {
                    long markedTime = entry.getValue();
                    
                    // Wait between 2-5 minutes before taking revenge
                    if (currentTime - markedTime > (2 * 60 * 1000) && random.nextDouble() < 0.3) {
                        executeRevenge(player);
                        it.remove();
                    }
                } else {
                    // Remove offline players
                    it.remove();
                }
            }
        }, 600L, 600L); // 30 seconds = 600 ticks
    }

    private void executeRevenge(Player player) {
        // Only execute if player is in survival mode
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        
        // Create one final appearance
        plugin.getAppearanceManager().createAppearance(player);
        
        // Play creeper hiss for extra scare
        playCreeperScare(player);
        
        // Schedule the player's doom
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Kill the player
            player.setHealth(0);
            
            // Set custom death message
            String deathMessage = String.format(
                DEATH_MESSAGES[random.nextInt(DEATH_MESSAGES.length)],
                player.getName()
            );
            
            // Broadcast the death message
            Bukkit.broadcastMessage(deathMessage);
        }, 60L); // 3 seconds after appearance
    }

    public void playCreeperScare(Player player) {
        // Get player's location
        Location loc = player.getLocation();
        
        // Play creeper hiss sound behind the player
        Location behindPlayer = loc.clone();
        behindPlayer.add(loc.getDirection().multiply(-2)); // 2 blocks behind player
        
        // Play the sound only for this player
        player.playSound(behindPlayer, Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.0f);
        
        // Schedule a task to check if the player turns around
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // If player turned to look, increase aggression
            if (isPlayerLookingAtLocation(player, behindPlayer)) {
                increaseAggression(player, 0.2f);
                // Chance to make Herobrine appear where they look
                if (random.nextDouble() < 0.3) {
                    plugin.getAppearanceManager().createAppearance(player);
                }
            }
        }, 15L); // Check after 0.75 seconds
    }

    private boolean isPlayerLookingAtLocation(Player player, Location target) {
        // Get the direction vector from player to target
        Location eyeLocation = player.getEyeLocation();
        Vector toTarget = target.toVector().subtract(eyeLocation.toVector()).normalize();
        
        // Get player's looking direction
        Vector lookDirection = eyeLocation.getDirection().normalize();
        
        // Calculate angle between vectors (in degrees)
        double angle = Math.toDegrees(Math.acos(toTarget.dot(lookDirection)));
        
        // Return true if player is looking within 30 degrees of the target
        return angle < 30;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // If this player was marked, override their death message
        if (markedPlayers.containsKey(event.getEntity().getUniqueId())) {
            String deathMessage = String.format(
                DEATH_MESSAGES[random.nextInt(DEATH_MESSAGES.length)],
                event.getEntity().getName()
            );
            event.setDeathMessage(deathMessage);
            markedPlayers.remove(event.getEntity().getUniqueId());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location blockLoc = event.getBlock().getLocation();
        
        // Check if the broken block was part of a Herobrine structure
        if (isHerobrineStructure(blockLoc)) {
            Player player = event.getPlayer();
            UUID playerId = player.getUniqueId();
            
            // Increment the count of blocks destroyed
            int blocksDestroyed = structureBlocksDestroyed.getOrDefault(playerId, 0) + 1;
            structureBlocksDestroyed.put(playerId, blocksDestroyed);
            
            // Start reacting after 3 blocks are broken
            if (blocksDestroyed > 3) {
                // Calculate aggression increase based on blocks destroyed
                float aggressionIncrease = Math.min(0.2f * (blocksDestroyed - 3), 0.8f);
                increaseAggression(player, aggressionIncrease);
                
                // Chance for immediate effects increases with blocks destroyed
                float effectChance = Math.min(0.1f * (blocksDestroyed - 3), 0.9f);
                
                if (random.nextFloat() < effectChance) {
                    // Choose a random effect based on current aggression
                    float currentAggression = getAggressionLevel(player);
                    List<Runnable> possibleEffects = new ArrayList<>();
                    
                    // Add basic effects
                    possibleEffects.add(() -> playCreeperScare(player));
                    possibleEffects.add(() -> damagePlayer(player));
                    
                    // Add more severe effects at higher aggression
                    if (currentAggression > 0.5f) {
                        possibleEffects.add(() -> {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                plugin.getAppearanceManager().createAppearance(player);
                            });
                        });
                    }
                    
                    if (currentAggression > 0.7f) {
                        possibleEffects.add(() -> markPlayerForRevenge(player));
                    }
                    
                    // Execute a random effect
                    possibleEffects.get(random.nextInt(possibleEffects.size())).run();
                }
            }
        }
    }

    public void registerStructure(Location location) {
        structures.add(location.clone());
    }

    public boolean hasStructureWithin(Location location, int radius) {
        return structures.stream()
            .anyMatch(struct -> struct.getWorld().equals(location.getWorld()) &&
                     struct.distanceSquared(location) <= radius * radius);
    }

    private boolean isHerobrineStructure(Location location) {
        // Check if any registered structure is within 5 blocks
        return herobrineStructures.keySet().stream()
            .anyMatch(structureLoc -> 
                structureLoc.getWorld().equals(location.getWorld()) &&
                structureLoc.distance(location) <= 5);
    }

    public void increaseAggression(Player player, float amount) {
        UUID playerId = player.getUniqueId();
        float currentLevel = aggressionLevels.getOrDefault(playerId, 0.0f);
        float newLevel = Math.min(1.0f, currentLevel + amount);
        aggressionLevels.put(playerId, newLevel);
    }

    public float getAggressionLevel(Player player) {
        return aggressionLevels.getOrDefault(player.getUniqueId(), 0.0f);
    }

    private void damagePlayer(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            double currentHealth = player.getHealth();
            double newHealth = Math.max(1.0, currentHealth - 2.0); // Remove one heart (2 health points)
            player.setHealth(newHealth);
            
            // Play effects
            plugin.getEffectManager().playStalkEffects(player, player.getLocation());
        });
    }

    public void cleanup() {
        nameCount.clear();
        aggressionLevels.clear();
        herobrineStructures.clear();
        markedPlayers.clear();
        structures.clear();
        structureBlocksDestroyed.clear();
    }
} 