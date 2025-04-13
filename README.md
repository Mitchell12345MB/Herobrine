# Herobrine Plugin

A feature-rich Minecraft plugin that adds the infamous Herobrine entity to your server. This plugin creates an immersive horror experience with advanced AI behavior, adaptive learning, and numerous creepy effects.

## Features

- **Dynamic Herobrine NPC**: Uses Citizens API to create realistic Herobrine entities with custom behavior
- **Adaptive Intelligence**: Learns player reactions and adapts behavior over time
- **Environmental Effects**: Fog, ambient sounds, footsteps, torch manipulation and more
- **Structure Creation**: Builds mysterious structures like sand pyramids, 2×2 tunnels, and wooden crosses
- **Player Memory System**: Remembers interactions with each player for personalized experiences
- **Shrine Summoning**: Special ritual to summon Herobrine directly
- **Extensive Configuration**: Highly customizable with detailed settings

## Requirements

- Spigot/Paper 1.21.x
- Citizens 2.0.37+ (Required dependency)
- ProtocolLib 5.3.0+ (Required dependency)

## Installation

1. Download the plugin JAR from the releases page
2. Place it in your server's `plugins` folder
3. Make sure you have Citizens and ProtocolLib installed
4. Start/restart your server
5. Configure options in `plugins/Herobrine/config.yml`

## Usage

### Commands

- `/herobrine spawn [player]` - Spawn Herobrine near yourself or another player
- `/herobrine enable` - Enable Herobrine's activities
- `/herobrine disable` - Disable Herobrine's activities
- `/herobrine config` - View and modify configuration

### Permissions

- `herobrine.command.spawn` - Allows spawning Herobrine near yourself
- `herobrine.command.spawn.others` - Allows spawning Herobrine near other players
- `herobrine.command.enable` - Allows enabling Herobrine
- `herobrine.command.disable` - Allows disabling Herobrine
- `herobrine.command.config` - Allows access to configuration commands
- `herobrine.admin` - Grants all permissions

## Configuration

The plugin is highly configurable through the `config.yml` file. Here are the key settings:

### Basic Settings

```yaml
enabled: true
appearance:
  # Time in seconds between possible appearances
  frequency: 300
  # Chance of appearing when timer triggers (0.0 - 1.0)
  chance: 0.3
```

### Effect Settings

```yaml
effects:
  # Enable/disable ambient sounds
  ambient_sounds: true
  # Frequency of ambient sounds (in seconds)
  ambient_sound_frequency: 30
  # Chance for ambient sounds to play (0.0 - 1.0)
  ambient_sound_chance: 0.3
  # Enable/disable structure manipulation
  structure_manipulation: true
  # Enable/disable stalking behavior
  stalking_enabled: true
  # Maximum distance for stalking (in blocks)
  max_stalk_distance: 50
  # Enable/disable fog effects (uses blindness and darkness)
  fog_enabled: true
  # Fog intensity (0.0 - 1.0)
  fog_density: 0.3
  # Duration of fog effects in ticks (20 ticks = 1 second)
  fog_duration: 200
  # Enable/disable footstep effects
  footsteps_enabled: true
  # Maximum number of footsteps to create
  max_footsteps: 10
  # Enable/disable torch manipulation
  torch_manipulation: true
  # Radius for torch manipulation (in blocks)
  torch_manipulation_radius: 10
  # Chance to convert torches to redstone torches (0.0 - 1.0)
  torch_conversion_chance: 0.7
  # Chance to remove torches completely (0.0 - 1.0)
  torch_removal_chance: 0.3
```

### Structure Settings

```yaml
structures:
  # Enable/disable individual structure types
  enabled_types:
    sand_pyramids: true
    redstone_caves: true
    stripped_trees: true
    mysterious_tunnels: true
    glowstone_e: true
    wooden_crosses: true
    tripwire_traps: true
    creepy_signs: true
  
  # Chance weights for each structure type (higher number = more common)
  weights:
    sand_pyramids: 15
    redstone_caves: 15
    stripped_trees: 15
    mysterious_tunnels: 15
    glowstone_e: 10
    wooden_crosses: 10
    tripwire_traps: 10
    creepy_signs: 10
  
  # Structure-specific settings
  sand_pyramids:
    size: 5  # Base size of the pyramid
  
  redstone_caves:
    min_length: 15  # Minimum tunnel length
    max_length: 25  # Maximum tunnel length
    torch_interval: 3  # Blocks between torches
  
  mysterious_tunnels:
    min_length: 20  # Minimum tunnel length
    max_length: 40  # Maximum tunnel length
    depth: 10  # Blocks below surface
  
  stripped_trees:
    radius: 5  # Radius to search for trees
    max_height: 10  # Maximum height to strip leaves
  
  glowstone_e:
    depth: 5  # Blocks below surface
  
  wooden_crosses:
    height: 3  # Height of the cross
  
  tripwire_traps:
    tnt_count: 4  # Number of TNT blocks
```

### Advanced Settings

```yaml
advanced:
  # Debug mode (prints additional information to console)
  debug: false
  # Maximum number of simultaneous appearances
  max_appearances: 1
  # Time in seconds before Herobrine disappears
  appearance_duration: 10
  # Minimum distance from player for appearances (in blocks)
  min_appearance_distance: 15
  # Maximum distance from player for appearances (in blocks)
  max_appearance_distance: 25
```

## Summoning Shrine

You can summon Herobrine directly by building a special shrine:

1. Place a **Mossy Cobblestone** block as the center
2. Place four **Gold Blocks** in a cross pattern around the mossy cobblestone (north, south, east, west)
3. Place **Redstone Torches** on top of each gold block
4. Place a **Netherrack** block on top of the mossy cobblestone
5. Light the netherrack on **Fire**

When built correctly, lightning will strike, Herobrine will appear, and the shrine will be consumed.

## Features In Depth

### Player Memory System

The plugin remembers each player's encounters with Herobrine and adapts behavior accordingly:
- Players who ignore Herobrine may see him more frequently
- Players who actively engage with structures will see more personalized structures
- Players who attack Herobrine might face increased aggression

### Structure Creation

Herobrine will occasionally create mysterious structures in the world:
- **Sand Pyramids**: Small pyramids in oceans or deserts
- **Redstone Caves**: Tunnels with redstone torches leading underground
- **Stripped Trees**: Trees with leaves removed in a forest
- **Mysterious Tunnels**: 2×2 tunnels leading deep underground
- **Glowstone E**: Glowstone arranged in an E pattern underground
- **Wooden Crosses**: Wooden cross structures
- **Tripwire Traps**: Hidden traps with TNT underneath
- **Creepy Signs**: Signs with disturbing messages

## Contributing

Feel free to fork this project and submit pull requests. For major changes, please open an issue first to discuss what you would like to change.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Inspired by the classic Herobrine creepypasta
- Uses Citizens API for NPC creation
- Uses ProtocolLib for advanced effects 