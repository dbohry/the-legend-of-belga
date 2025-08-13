# The Legend of Belga

Simple top-down 2D "Zelda-like" made with plain Java2D.

**Version:** 0.0.50  
**Java Version:** 21.0.3-tem

## Features

- **Procedurally generated cave-like maps** with consistent world seeds
- **Roguelike progression system** with perks and level-ups
- **Multiple enemy types** with different AI behaviors
- **Combat system** with melee weapons and ranged abilities
- **Save system** that persists progress between sessions
- **Audio system** with background music and sound effects
- **Multiplayer support** with dedicated server

## Controls

- **Movement:** Arrow keys or WASD
- **Attack:** Space to slash in the facing direction
- **Block:** Left Ctrl to block incoming attacks (consumes stamina)
- **Pause:** ESC to pause/unpause
- **Stats:** I key to view character statistics
- **Mouse:** Aim for ranged abilities (when available)

## Gameplay Mechanics

### Core Systems
- **Health & Stamina Management**: Players must manage health, stamina, and mana resources
- **Combat System**: Melee combat with weapons, blocking, and dash mechanics
- **Perk System**: Collectible upgrades that permanently enhance player abilities
- **Enemy Perk System**: Enemies get progressively stronger based on map completion
- **Save System**: Automatic progress saving with perk persistence

### Player Abilities
- **Movement**: WASD keys for movement with sprint capability (Shift)
- **Attack**: Left-click to swing weapon with cooldown system
- **Block**: Hold Left Ctrl to block incoming damage (consumes stamina)
- **Dash**: Double-tap Shift to dash (consumes mana)
- **Blocking Restriction**: Cannot attack while blocking

### Block System
- **Activation**: Hold Left Ctrl to activate blocking
- **Stamina Cost**: 0.5 stamina consumed per hit blocked
- **Damage Reduction**: 80% damage reduction when blocking
- **Restriction**: Blocking prevents attacking while active
- **Visual**: Clean interface without "BLOCK" text overlay

### Attack Restriction
- **Blocking State**: Players cannot attack while holding the block key
- **Stamina Check**: Blocking requires sufficient stamina (≥0.5)
- **Combat Balance**: Forces players to choose between offense and defense

### Perk System
The game features a comprehensive perk system that enhances player abilities:

#### Perk Types
- **Health Perks**: Increase maximum health by percentage
- **Stamina Perks**: Increase maximum stamina and regeneration rate
- **Mana Perks**: Increase maximum mana and regeneration rate
- **Speed Perks**: Increase movement speed
- **Damage Perks**: Increase attack damage
- **Weapon Perks**: Increase weapon range and width
- **Shield Perks**: Increase maximum shield capacity

#### Perk Rarity
- **Common**: Health, Stamina, Mana (10-20% increases)
- **Uncommon**: Speed, Damage (5-10% increases)
- **Rare**: Regeneration, Weapon Range (5-10% increases)
- **Epic**: Weapon Width, Shield (fixed amounts)
- **Legendary**: Special combinations

#### Perk Application
- Perks are applied immediately upon selection
- Effects stack multiplicatively
- Some perks have eligibility requirements
- Perks persist across game sessions

### Enemy Perk System
Enemies now receive perks based on map completion, creating natural difficulty progression:

#### Difficulty Scaling
- **Map 0-2**: Basic enemies with no perks
- **Map 3-5**: Some enemies have 1-2 perks
- **Map 6-8**: Most enemies have 2-3 perks
- **Map 9+**: All enemies have maximum perks

#### Perk Distribution
- **Perk Chance**: Increases from 0% to 80% (capped)
- **Perk Count**: Maximum of 3 perks per enemy
- **Perk Strength**: Scales with map completion (5% base + 2% per map)

#### Enemy Perk Types
- **Health Boost**: Increases enemy maximum health
- **Speed Boost**: Increases enemy movement speed
- **Damage Boost**: Increases enemy attack damage
- **Stamina Boost**: Increases enemy stamina (if applicable)
- **Range Boost**: Increases enemy weapon range
- **Width Boost**: Increases enemy weapon width

#### Visual Indicators
- **Perk Count Display**: Shows total perks on each enemy
- **Color Coding**: White (0), Yellow (1), Orange (2), Red (3+ perks)
- **Size Scaling**: Larger indicators for more dangerous enemies
- **Perk Summary**: Text display of active perk types

### Enemy Types
- **Soldiers:** Melee fighters with various AI patterns (Random, Patrol, Circular, Linear, Idle)
- **Archers:** Ranged attackers with tactical behaviors (Tactical, Cover-seeking, Flanking, Retreat, Ambush)

## Configuration

### In-Game Settings
The game includes an in-game configuration menu accessible from the pause screen:

1. **Pause the game** (ESC key)
2. **Click "Settings"** to open the configuration menu
3. **Toggle "Show Enemy Indicators"** to show/hide enemy behavior letters
4. **Adjust "Music Volume"** using the slider
5. **Click "Back"** to return to the main pause menu

All settings are automatically saved to `~/.tlob/config.properties` and persist between game sessions.

### Enemy Behavior Indicators
When enabled, letters appear above enemies showing their wandering behavior:
- **Soldiers:** R (Random), P (Patrol), C (Circular), L (Linear), I (Idle)
- **Archers:** T (Tactical), C (Cover-seeking), F (Flanking), R (Retreat), A (Ambush)

### Legacy Command-Line Options
For advanced users, you can still use command-line flags:
```bash
./gradlew run -Dtlob.show.enemy.behavior=true
```

## Project Structure

### Modules/Packages
- **core:** `com.lhamacorp.games.tlob.core` — Shared logic, protocol, math, map generation. Includes a headless runner (no UI).
- **client:** `com.lhamacorp.games.tlob.client` — UI client built with Swing/Java2D.
- **server:** `com.lhamacorp.games.tlob.server` — Authoritative server and simulation.

### Key Components
- **Entity System:** Player, enemies, and NPCs with alignment-based classification
- **World Generation:** Procedural map generation with customizable parameters
- **Audio Management:** Music and sound effect system
- **Save System:** Persistent game state and perk progression
- **Input Handling:** Keyboard and mouse input management

## Running the Game

### Prerequisites
- Java 21.0.3-tem (recommended to use SDKMAN: `sdk use java 21.0.3-tem`)
- Gradle (included via wrapper)

### UI Client (Default)
```bash
./gradlew run
```

### Headless Core (No UI)
```bash
./gradlew runCore
# With custom seed and tickrate:
./gradlew runCore -PappArgs=12345,60
```

### Dedicated Multiplayer Server
```bash
# Default port (7777):
./gradlew runServer

# Custom port:
./gradlew runServer -Pport=8888

# Custom port and seed:
./gradlew runServer -Pport=8888 -Pseed=12345
```

### Building
```bash
# Build JAR file:
./gradlew jar

# Run tests:
./gradlew test
```

## Save System

The game automatically saves your progress when:
- Completing a level
- Selecting a perk (saves the enhanced stats before advancing to next level)
- Game over or victory

Save files are stored in your home directory under `.tlob/save.dat` and contain:
- World seed (for consistent map generation)
- Number of completed maps
- Active perks (list of selected perks that will be re-applied)

When starting a new game, the system will check for existing saves and ask if you want to continue your previous game or start fresh.

## Development

### Building from Source
1. Clone the repository
2. Ensure Java 21 is available: `sdk use java 21.0.3-tem`
3. Run `./gradlew build`

### Testing
```bash
./gradlew test
```

### Performance Tuning
The game includes several JVM optimizations:
- G1 Garbage Collector
- Optimized string concatenation
- Memory settings: 256MB min, 1GB max

## License

This project is a private development project.
