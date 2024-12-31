# Herobrine Plugin [BETA]

A Minecraft plugin that brings the legendary Herobrine to your server, complete with stalking behavior, structure manipulation, and atmospheric effects.

## ⚠️ Beta Notice
This plugin is currently in BETA. While functional, you may encounter bugs or incomplete features. Please report any issues on our GitHub page.

## 📋 Requirements

- Minecraft Server 1.21.3+
- [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) 5.3.0+
- [Citizens](https://www.spigotmc.org/resources/citizens.13811/) 2.0.37+

## 🚀 Installation

1. Download the latest release from the releases page
2. Place the JAR file in your server's `plugins` folder
3. Install the required dependencies (ProtocolLib and Citizens)
4. Start/restart your server
5. The plugin will generate a default configuration file at `plugins/Herobrine/config.yml`

## ✨ Features

- **Realistic Herobrine Appearances**
  - Random appearances with configurable frequency
  - Intelligent pathfinding and obstacle navigation
  - Stalking behavior with customizable distances

- **Environmental Effects**
  - Ambient fog effects with adjustable density
  - Creepy sound effects and ambient noises
  - Footstep trails
  - Torch manipulation (removal and conversion)

- **Structure Manipulation**
  - Sand pyramids
  - Redstone caves
  - Stripped trees
  - Mysterious tunnels
  - Glowstone formations
  - Wooden crosses
  - Tripwire traps
  - Creepy signs

## 🎮 Commands

- `/herobrine reload` - Reloads the plugin configuration
- `/herobrine toggle` - Enables/disables the plugin
- `/herobrine debug` - Toggles debug mode
- `/herobrine config <setting> <value>` - Modifies configuration settings in-game

## 🔒 Permissions

- `herobrine.admin` - Access to all plugin commands and features
- `herobrine.reload` - Permission to reload the plugin
- `herobrine.toggle` - Permission to enable/disable the plugin
- `herobrine.debug` - Permission to use debug mode
- `herobrine.config` - Permission to modify configuration in-game

## ⚙️ Default Configuration

```yaml
# Plugin state
enabled: true

# Appearance settings
appearance:
  frequency: 300  # Time in seconds between possible appearances
  chance: 0.3     # Chance of appearing when frequency timer triggers

# Effect settings
effects:
  ambient_sounds: true
  ambient_sound_frequency: 30
  ambient_sound_chance: 0.3
  structure_manipulation: true
  stalking_enabled: true
  max_stalk_distance: 50
  fog_enabled: true
  fog_density: 0.3
  fog_duration: 200
  footsteps_enabled: true
  max_footsteps: 10
  torch_manipulation: true
  torch_manipulation_radius: 10
  torch_conversion_chance: 0.7
  torch_removal_chance: 0.3

# Structure settings
structures:
  enabled_types:
    sand_pyramids: true
    redstone_caves: true
    stripped_trees: true
    mysterious_tunnels: true
    glowstone_e: true
    wooden_crosses: true
    tripwire_traps: true
    creepy_signs: true
  
  weights:
    sand_pyramids: 15
    redstone_caves: 15
    stripped_trees: 15
    mysterious_tunnels: 15
    glowstone_e: 10
    wooden_crosses: 10
    tripwire_traps: 10
    creepy_signs: 10

# Advanced settings
advanced:
  debug: false
  max_appearances: 1
  appearance_duration: 10
  min_appearance_distance: 15
  max_appearance_distance: 25
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🐛 Bug Reports

Found a bug? Please create an issue on our GitHub page with:
1. Description of the bug
2. Steps to reproduce
3. Expected behavior
4. Server version and plugin versions
5. Any relevant error messages or screenshots

## 📚 Documentation

For more detailed information about the plugin's features and configuration options, please visit our [Wiki](../../wiki). 