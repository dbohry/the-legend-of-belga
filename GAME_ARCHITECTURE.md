# The Legend of Belga - Game Architecture Documentation

## Overview
The Legend of Belga (TLOB) is a 2D action-adventure game built in Java using Swing for the UI and a custom game engine. The game features procedurally generated maps, an XP/leveling system, perk-based character progression, and both single-player and multiplayer modes.

## Core Architecture

### 1. Game Engine Structure
```
CoreGame (Main Entry Point)
├── BaseGameManager (Abstract base class)
│   ├── SinglePlayerGameManager (SP implementation)
│   └── MultiplayerGameManager (MP implementation)
├── Game Loop (60 FPS simulation)
├── Input Management
├── Audio Management
└── Rendering System
```

### 2. Key Components

#### 2.1 Game Managers
- **BaseGameManager**: Abstract base class handling common game logic
  - Game state management (PLAYING, PAUSED, VICTORY, GAME_OVER)
  - Input handling and camera management
  - Perk system integration
  - Level-up event handling
  - Victory/defeat logic

- **SinglePlayerGameManager**: Single-player specific implementation
  - Save/load system integration
  - Enemy spawning and management
  - Death notification system

- **MultiplayerGameManager**: Multiplayer implementation (referenced but not shown in current files)

#### 2.2 Entity System
- **Entity**: Base class for all game objects
  - Health, armor, and damage system
  - Death notification system via DeathListener interface
  - Position and movement handling

- **Player**: Player character with enhanced features
  - XP and leveling system
  - Perk application and management
  - Level-up notification system via LevelUpListener interface

- **Enemy Types**:
  - **Soldier**: Basic enemy unit
  - **Archer**: Ranged enemy unit
  - **Golen**: Special enemy with 3x XP reward

#### 2.3 World and Maps
- **LevelManager**: Handles map progression and generation
- **TileMap**: Procedurally generated tile-based maps
- **Biome**: Different environment types (Forest, Desert, Cave, Meadows, Vulcan)
- **BiomeEnemySpawner**: Enemy spawning based on biome and map completion

#### 2.4 Perk System
- **PerkManager**: Core perk management
  - Perk rolling and selection
  - Perk application to entities
  - Perk choice presentation

- **Perk Types**: Various stat improvements
  - Health, Stamina, Mana
  - Speed, Damage, Range
  - Armor and other combat stats

- **ActivePerks**: Tracks currently applied perks
- **AppliedPerk**: Individual perk instance with value

## Game Features

### 1. XP and Leveling System
- **XP Sources**:
  - Base XP per enemy: 10 XP
  - **XP Formula**: `XP = 10 × (1 + perk count)`
  - **Examples**:
    - 0 perks: 10 × (1 + 0) = **10 XP**
    - 1 perk: 10 × (1 + 1) = **20 XP**
    - 2 perks: 10 × (1 + 2) = **30 XP**
    - 3 perks: 10 × (1 + 3) = **40 XP**
    - 4 perks: 10 × (1 + 4) = **50 XP**
    - 5 perks: 10 × (1 + 5) = **60 XP**
    - 6 perks: 10 × (1 + 6) = **70 XP**
    - etc.

- **Level Progression**:
  - Level 1 → 2: 100 XP
  - Level 2 → 3: 250 XP
  - Level 3 → 4: 475 XP
  - Formula: `XP = 100 * 1.5^(level-2)`

- **Level-Up Benefits**:
  - Perk selection screen appears
  - Game pauses for perk choice
  - Background music continues (not stopped)

### 2. Perk Selection System
- **Triggered by**:
  - Player leveling up (every level)
  - Map completion (additional perk)

- **Perk Priority**:
  1. Level-up perks are shown first
  2. Map completion perks are shown after level-up perks
  3. Game resumes after perk selection

### 3. Map Progression
- **Map Completion**: Occurs when all enemies are defeated
- **Procedural Generation**: Each map is uniquely generated based on seed
- **Biome Variety**: Different environments with unique enemy types
- **Enemy Scaling**: Enemy difficulty increases with map completion count

### 4. Combat System
- **Weapon System**: Base Weapon class with Sword implementation
- **Damage Calculation**: Health, armor, and damage mechanics
- **Screen Shake**: Visual feedback for attacks
- **Death System**: Comprehensive death notification system

### 5. Save System
- **SaveState**: Stores game progress
  - World seed
  - Completed maps count
  - Active perks
  - Player XP and level
- **Auto-save**: Automatic saving at key moments
- **SaveManager**: Handles save file operations

### 6. Audio System
- **AudioManager**: Centralized audio control
- **Music**: Background music with volume control
- **Sound Effects**: Various game sounds with volume adjustment
- **Level-up Sound**: Reduced volume (-10dB) to prevent overwhelming

## Technical Implementation Details

### 1. Event System
- **DeathListener**: Interface for entity death notifications
- **LevelUpListener**: Interface for player level-up events
- **Observer Pattern**: Used for game state changes

### 2. Game Loop
- **60 FPS Simulation**: Fixed time step for consistent gameplay
- **Input Processing**: Real-time input handling
- **State Management**: Clean state transitions between game modes

### 3. Rendering System
- **Renderer Classes**:
  - HudRenderer: In-game HUD with XP/level display
  - VictoryScreenRenderer: Perk selection interface
  - PauseMenuRenderer: Game pause and settings
  - StatsRenderer: Player statistics display

### 4. Input Handling
- **KeyManager**: Keyboard input processing
- **Mouse Handling**: Aiming and UI interaction
- **InputState**: Current input state for game logic

### 5. Camera System
- **Camera**: Follows player with smooth movement
- **Screen Shake**: Visual feedback for combat
- **Bounds Checking**: Prevents camera from going outside map

## File Structure

```
src/main/java/com/lhamacorp/games/tlob/
├── client/
│   ├── entities/          # Game objects (Player, Enemies)
│   ├── managers/          # Game management classes
│   │   └── renderers/     # UI rendering classes
│   ├── maps/              # Map generation and management
│   ├── perks/             # Perk system
│   ├── save/              # Save/load system
│   ├── weapons/           # Combat system
│   └── world/             # Game world logic
├── core/                  # Core game engine
└── server/                # Multiplayer server logic

src/main/resources/
├── assets/                # Game assets
│   ├── sfx/              # Sound effects
│   └── logo/             # Game logos
└── META-INF/             # Native image configuration
```

## Testing

### Test Coverage
- **PlayerTest**: XP system, leveling, enemy kill rewards
- **BaseGameManagerTest**: Level-up perk selection logic
- **Entity Tests**: Combat and death system
- **Manager Tests**: Game flow and perk management

### Test Strategy
- Unit tests for individual components
- Integration tests for perk selection flow
- Mock objects for testing game manager interactions

## Configuration

### Game Settings
- **Music Volume**: Configurable via pause menu
- **Enemy Behavior Indicators**: Toggle for debugging
- **World Seed**: Configurable for reproducible maps

### Audio Settings
- **Music Volume Range**: -40dB to 0dB
- **Default Music Volume**: -12dB
- **Sound Effect Volume**: Configurable per sound

## Future Enhancements

### Potential Features
- Additional enemy types
- More complex perk trees
- Achievement system
- Multiplayer enhancements
- Additional biomes and map types

### Technical Improvements
- Performance optimization
- Enhanced rendering effects
- Better audio management
- Improved save system

## Development Notes

### Key Design Principles
1. **Separation of Concerns**: Clear separation between game logic and rendering
2. **Event-Driven Architecture**: Loose coupling through event systems
3. **Extensible Design**: Easy to add new features and content
4. **Testable Code**: Comprehensive unit testing coverage

### Performance Considerations
- 60 FPS target for smooth gameplay
- Efficient entity management
- Optimized rendering pipeline
- Memory-conscious save system

This architecture provides a solid foundation for a feature-rich 2D action game with room for expansion and enhancement.
