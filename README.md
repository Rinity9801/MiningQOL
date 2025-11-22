# MiningQOL

A Minecraft Fabric mod for 1.21.8 that provides quality-of-life improvements for mining in Hypixel Skyblock.

## Features

### Corpse ESP
Highlight and track different types of corpses in the Crystal Hollows:
- Lapis Armor Corpses (Blue)
- Tungsten Corpses (Gray)
- Umber Corpses (Brown)
- Vanguard Corpses (Red)
- Fully customizable with individual toggles for each type

### Mining Profit Tracker
Real-time profit tracking for your mining sessions:
- Tracks gemstone drops (Rough, Flawed, Fine, Flawless, Perfect)
- Automatic Hypixel Bazaar price fetching
- Option to use NPC prices instead
- Configurable pristine chance
- Draggable HUD position
- Display options for different gem tiers
- Session-based tracking with reset functionality

### Efficient Miner Overlay
Visual overlay showing efficiency when mining blocks:
- Color-coded efficiency indicators
- Two visualization modes: modern gradient or classic heatmap
- Helps optimize mining patterns

### Pickaxe Cooldown Display
- HUD showing current pickaxe ability cooldown
- Displays when Maniac Miner or similar abilities are active
- Configurable position and title display
- Customizable cooldown threshold for title visibility

### Block Outline Customization
Customize the outline color and style of blocks you're looking at:
- Adjustable RGB color values with transparency
- Three modes: Inside, Outside, or Both
- Smooth color transitions with sliders

### Name Hider
Privacy feature to hide your username:
- Replace your name with custom text
- Solid color or gradient color options
- RGB color picker for both colors
- Live preview in config

### Glass Pane Sync
Fixes gemstone (stained glass) pane connections when mining:
- Makes panes behave like they did in 1.8.9
- Properly updates visual connections when breaking adjacent panes
- Purely cosmetic client-side fix

### Auto Clicker
Automated clicking for mining (use responsibly):
- Configurable mining slot and cooldown
- Rod swap support for Grappling Hook
- Second drill support for dual-drill setups
- Tab list cooldown integration
- Visual HUD indicator

### Command Keybinds
- Bind any chat command to any keyboard key
- Easy-to-use keybind editor in GUI
- Perfect for quick access to frequently used commands

### Miscellaneous
- Auto-skip /sho load: Automatically runs `/sho skipto 1` after `/sho load`
- Lobby Finder: Track and locate specific blocks across lobbies

## Installation

1. Make sure you have [Fabric Loader](https://fabricmc.net/use/) installed for Minecraft 1.21.8
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.8
3. Download the latest release of MiningQOL from the [Releases](https://github.com/Rinity9801/MiningQOL/releases) page
4. Place the downloaded `.jar` file in your `.minecraft/mods` folder
5. Launch Minecraft with the Fabric profile

## Usage

Open the config GUI in-game with `/miningqol` or by pressing the configured keybind (default: Right Control).

The GUI features a modern card-based interface with categories:
- **Corpse ESP** - Configure corpse highlighting
- **Mining Profit** - Set up profit tracking
- **Efficient Miner** - Customize efficiency overlay
- **Pickaxe Cooldown** - Configure cooldown display
- **Block Outline** - Customize block outlines
- **Name Hider** - Set up name replacement
- **Auto Clicker** - Configure auto-clicking (use responsibly)
- **Command Keybinds** - Manage command shortcuts
- **Misc** - Other features and utilities

## Configuration

All settings are saved automatically to `config/miningqol.json` and persist between game sessions.

## Compatibility

- **Minecraft Version:** 1.21.8
- **Loader:** Fabric Loader 0.16.14+
- **API:** Fabric API 0.130.0+1.21.8
- **Java Version:** Java 21

## Disclaimer

This mod is designed for Hypixel Skyblock and includes features like auto-clicking. Use at your own risk. Always check [Hypixel's rules](https://hypixel.net/threads/guide-allowed-modifications.345453/) regarding allowed modifications. The developers are not responsible for any consequences from using this mod.

Most features are purely visual/informational and should be safe, but features like auto-clicking may be against server rules.

## License

This project is open source. Feel free to fork, modify, and contribute!

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## Building from Source

```bash
git clone https://github.com/Rinity9801/MiningQOL.git
cd MiningQOL
./gradlew build
```

The built jar will be in `build/libs/`
