# The Legend of Belga — Codebase Analysis & Improvement Plan

Reviewed: entity/AI code, weapon system, perk system, texture manager, biome spawner, config, and renderers (~14k LOC across `client`, `core`, `server`). The game is functional and reasonably organized (managers, entities, perks, maps as separate packages), but three systemic patterns work against both simplicity and extensibility: **copy-pasted AI across enemy classes**, **one-getter-per-asset in `TextureManager`**, and **hardcoded enum/array/switch wiring for enemy types**. Perks are the one system already built the right way (data-driven registry) — that's the template to copy elsewhere.

## 1. What's already good

- `PerkManager` uses a registry (`register(id, rarity, eligibility, factory)`) with weighted random rolls. Adding a perk is a one-line call, no other file needs to change. This is the pattern the rest of the codebase should follow.
- Clear package separation: `entities/`, `weapons/`, `perks/`, `maps/`, `managers/`, `managers/renderers/`.
- `Entity` centralizes health/armor/shield/knockback/perk-multiplier logic so `Player` and enemies don't reimplement combat math.

## 2. Biggest simplification opportunity: duplicated enemy AI

`Soldier` (664 lines), `Archer` (733 lines), `Golen` (951 lines) each implement their own copy of the same behavior system: `RANDOM`/`PATROL`/`CIRCULAR`/`LINEAR`/`IDLE` wander states, `pickNewWanderDir`, `moveWithCollision`, `findNearbyAllies`/`groupWander`, personality traits (`isAggressive`, `isCautious`, `curiosityLevel`, `prefersGroupMovement`), and a custom LCG random generator (`rand01`/`lcg`). A direct method-name diff of `Soldier` vs. `Golen` shows 16 of 19 methods are identical in name and near-identical in body — only the combat specifics (attack range, projectile vs. melee) differ.

That means roughly 1,500+ lines across the three classes are the same wander/personality logic maintained three times. A balance tweak (e.g. "cautious enemies retreat sooner") today requires editing three files identically, and a bug fix in one routinely doesn't make it into the others.

**Fix:** extract a shared `EnemyBehavior`/`Wanderer` component (composition, not inheritance) that owns wandering, patrol points, group behavior, and personality traits. `Soldier`/`Archer`/`Golen` become thin classes that own stats + attack logic + sprite lookup and delegate movement to the shared component. This alone would cut the entity package by roughly half and make a new melee/ranged mob a ~50-100 line class instead of a ~700 line one.

## 3. Biggest extensibility blocker: enemy types are hardcoded in three places

Adding a new mob today means touching:

1. `BiomeEnemySpawner.EnemyType` enum (`SOLDIER, ARCHER, GOLEN`) — ordinal-indexed.
2. `BIOME_ENEMY_WEIGHTS` — a raw `double[5][3]` array keyed by `biome.ordinal()` / `enemyType.ordinal()`. Adding a mob means resizing every row of this matrix by hand; get the column count wrong and it silently corrupts other biomes' weights.
3. `spawnBiomeEnemy(...)` — an `if/else` that only knows about two types (Golen is spawned separately above it with its own bespoke loop and a magic "replaces 10 regular enemies" ratio).
4. `TextureManager` — a new `private static BufferedImage xFrame` field + `getXFrame()` getter + manual wiring into `sliceIntoAnimations`/`ensureLoaded` (see below). Notably, **Golen currently has no sprite at all** — it falls back to a hand-drawn vector shape while Soldier/Archer/Player use real pixel art, so the enemy roster already looks visually inconsistent.

**Fix:** replace the enum + parallel-array + if/else with a `MobRegistry` mirroring `PerkManager`'s pattern: each mob registers an id, a biome-weight map, a spawn-count cost (Golen's "replaces 10" becomes a `spawnWeight`/`slotCost` field on the entry instead of a special case), and a factory `(x, y, weapon) -> Entity`. `BiomeEnemySpawner.spawn()` then becomes a generic weighted-pick loop with zero per-mob branches. New mob = one `register(...)` call, same as a new perk today.

## 4. `TextureManager` (1,720 lines) needs to become data-driven

Right now every texture is: a dedicated `private static BufferedImage` field, a dedicated public getter (`getMeadowGrassTexture()`, `getCaveCrystalTexture()`, `getVulcanAshTexture()`, ...30+ of these), and manual loading/generation code wired into `ensureLoaded()`. Sprite sheets (`soldierAnimations`, `archerAnimations`, `playerAnimations`) get similar dedicated treatment. This is the same shape as the enemy-type problem: adding one biome tile means adding a field, a getter, and editing the load routine in 3 places, and every entity/tile draw call references the specific getter by name rather than a shared texture ID.

**Fix:** collapse to a single `Map<String, BufferedImage>` (or `Map<TextureKey, BufferedImage>`) keyed by `"<biome>_<tile>"` / `"<mob>_<dir>_<motion>"`, loaded by convention from the `assets/` folder (the filenames already follow a `biome_tile.png` convention, e.g. `forest_tree.png`, `vulcan_lava.png` — the code just doesn't take advantage of it). One generic `getTexture(String key)` / `getMobFrame(String mobId, Direction, Motion, long timeMs)` replaces all 30+ specific getters. New biome or mob art = drop a PNG with the right filename, no Java changes. This is the single highest-leverage refactor for both "simplify" and "add new content easily."

## 5. Weapons: type enum exists but only 2 of 7 types are implemented

`Weapon.WeaponType` already declares `SWORD, AXE, BOW, DAGGER, MACE, SPEAR, WAND`, but only `Sword` and `Bow` exist, and both are near-identical (constructor just sets `name`/`type`/`damage`/`reach`/`width`/`duration`/`cooldown` — no behavioral difference at the class level; melee-vs-ranged branching actually lives inside `Player.java` via `getType() == WeaponType.BOW` checks). Weapon stats are also instantiated with magic-number literals in three different places (`BaseGameManager`, `SpawnManager`, and a duplicated `new Sword(2, 28, 12, 10, 16)` hardcoded again inside `BiomeEnemySpawner.getEnemyWeapon()` because, per its own comment, it "can't access the private field" on the parent — a sign the class hierarchy needs a protected accessor, not a copy-pasted workaround).

**Fix:** a small `WeaponRegistry` (same pattern again) mapping `WeaponType` → stat presets, so "add a Dagger" is a data entry, not a new Java file plus edits in three managers. Fix the `BiomeEnemySpawner`/`SpawnManager` weapon-access duplication by exposing the configured weapon via a protected getter on `SpawnManager` instead of re-instantiating it.

## 6. UI/texture polish

- **No shared visual theme.** Colors and fonts are declared ad hoc in every renderer (72 raw `new Color(...)` literals spread across the 7 renderer classes, each renderer picking its own `Font("Arial", ...)` sizes). A `UiTheme`/`Palette` class with named constants (`PANEL_BG`, `TEXT_PRIMARY`, `GOLD_ACCENT`, `HP_RED`, `HEADER_FONT`, `BODY_FONT`) would make the HUD/pause/inventory/stats/victory screens visually consistent and let a future palette swap (e.g. a dark/light or colorblind mode) be a one-file change instead of a 7-file hunt.
- **Golen has no sprite** — it's the only enemy still rendered as a procedurally-drawn vector shape (`drawSoldierEnemy`-style fallback) while Soldier/Archer/Player use real 80×80/88×88 sprite sheets. Worth commissioning/generating `golen.png` at the same 80×80, 4-direction × 4-frame layout as the others so the "elite enemy" doesn't look visually cheaper than basic mobs.
- **Debug-looking overlays are the only enemy affordances.** Behavior letters (`R/P/C/L/I`), colored dot/square/triangle personality markers, and gold perk-count numbers are drawn directly above enemies with `Font("Arial", Font.BOLD, 10)` — functional for debugging but reads like a dev overlay rather than game UI. Consider either gating these fully behind the existing debug toggle (it already is, via `GameConfig`) and designing a lighter "elite" visual tell for release builds (e.g. a subtle aura/outline scaling with `perkCount`, which `getPerkIndicatorColor()`/`getPerkIndicatorSize()` already compute but nothing currently consumes).
- **Tile art is consistent** (all biome tiles are clean 32×32 PNGs, mob sprites consistently 80×80 or 88×88), so the raw asset set itself is in good shape — the polish need is in *how* the UI layer uses color/typography, not the tile art.

## 7. Suggested priority order

1. Extract shared `EnemyBehavior` component from Soldier/Archer/Golen (biggest simplification, lowest risk — pure refactor, behavior-preserving).
2. Introduce `MobRegistry` + generic biome-weighted spawn loop (unlocks "add a mob" as a one-liner).
3. Collapse `TextureManager` to a keyed map + generic getters (unlocks "add a texture/mob sprite" with zero code changes).
4. Add `WeaponRegistry` and fix the `BiomeEnemySpawner` weapon-duplication workaround.
5. Add a `UiTheme` constants class and apply it across the 7 renderers; generate a Golen sprite sheet.

Each step is independently shippable and de-risks the next — steps 1-4 are backend refactors with no visible gameplay change (good candidates for a test-covered PR each), step 5 is the visible polish pass.

---
*Want me to implement any of these? Step 1 (behavior extraction) or step 3 (TextureManager consolidation) are the best starting points — highest leverage, lowest risk.*
