# The Legend of Belga

Simple top-down 2D "Zelda-like" made with plain Java2D.

- Procedurally generated cave-like map
- Arrow keys or WASD to move
- Space to slash in the facing direction
- ESC to pause/unpause
- I key to view stats

## Configuration

### In-Game Settings
The game now includes an in-game configuration menu accessible from the pause screen:

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

## Modules/Packages
- core: com.lhamacorp.games.tlob.core — shared logic, protocol, math, map generation. Includes a headless runner (no UI).
- client: com.lhamacorp.games.tlob.client — UI client built with Swing/Java2D.
- server: com.lhamacorp.games.tlob.server — authoritative server and simulation.

## How to run
- UI client (default):
  - ./gradlew run
- Headless core (no UI):
  - ./gradlew runCore
  - Pass seed and tickrate optionally: ./gradlew runCore -PappArgs=12345,60
- Dedicated server (accepts clients on port):
  - ./gradlew run --args='...' can be customized by setting application.mainClass to server, or run from your IDE using com.lhamacorp.games.tlob.server.Server

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
