# The Legend of Belga

Simple top-down 2D "Zelda-like" made with plain Java2D.

- Procedurally generated cave-like map
- Arrow keys or WASD to move
- Space to slash in the facing direction

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
