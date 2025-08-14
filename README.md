# The Legend of Belga

A top-down 2D action-adventure game with roguelike progression, featuring procedurally generated maps and dynamic enemy difficulty scaling.

**Version:** 0.0.50  
**Java Version:** 21.0.3-tem

## 🎮 Game Overview

The Legend of Belga is a challenging action game where you explore procedurally generated cave-like maps, battle increasingly difficult enemies, and collect powerful perks to become stronger. As you complete more maps, enemies become more dangerous through an intelligent perk system.

## 🎯 Core Features

- **Procedurally Generated Maps** - Each playthrough offers unique exploration
- **Roguelike Progression** - Collect permanent perks to enhance your abilities
- **Dynamic Difficulty** - Enemies scale with your progress through perks
- **Multiple Enemy Types** - Each with unique AI behaviors and personalities
- **Combat System** - Melee weapons, blocking, dashing, and strategic positioning
- **Save System** - Progress persists between sessions

## 🕹️ Controls

- **Movement:** Arrow keys or WASD
- **Attack:** Space to slash in the facing direction
- **Block:** Alt key to block incoming attacks (consumes stamina)
- **Dash:** Ctrl key to dash (consumes mana)
- **Sprint:** Shift key for faster movement
- **Pause:** ESC to pause/unpause
- **Stats:** I key to view character statistics

## 🧙‍♂️ Player Progression

### Perk System
Collect powerful upgrades that permanently enhance your abilities:
- **Health & Stamina** - Increase your survivability
- **Speed & Damage** - Become faster and deadlier
- **Weapon Enhancements** - Extend reach and attack width
- **Resource Management** - Better mana and stamina regeneration

### Difficulty Scaling
- **Early Game (Maps 0-2):** Basic enemies, no perks
- **Mid Game (Maps 3-5):** Some enemies have 1-2 perks
- **Late Game (Maps 6+):** Most enemies have 2-3 perks
- **End Game:** All enemies have maximum perks

## 👹 Enemy System

### Enemy Types

#### **Soldier** 🗡️
- **Combat Style:** Melee fighters with swords and shields
- **AI Behaviors:**
  - **R** = Random movement
  - **P** = Patrol (follows set paths)
  - **C** = Circular movement
  - **L** = Linear (back and forth)
  - **I** = Idle (stays still)

#### **Archer** 🏹
- **Combat Style:** Ranged attackers with bows
- **AI Behaviors:**
  - **T** = Tactical positioning
  - **C** = Cover-seeking
  - **F** = Flanking maneuvers
  - **R** = Retreat when threatened
  - **A** = Ambush tactics

#### **Golen** 🗿
- **Combat Style:** Stone golems with heavy attacks
- **AI Behaviors:** Same as Soldiers but larger and stronger

### 🎨 Enemy Personality Indicators

When you enable enemy indicators in the settings, you'll see colored shapes that reveal enemy personalities:

#### **Red Circle** 🔴 = **Aggressive**
- Attacks more aggressively
- Chases you further
- Less likely to retreat

#### **Blue Square** 🔵 = **Cautious**
- More defensive behavior
- Maintains distance
- Retreats when low on health

#### **Yellow Triangle** 🟡 = **Group Movement**
- Stays near other enemies
- Coordinates attacks
- Prefers fighting in groups

#### **Green Square** 🟢 = **Tactical** (Archers only)
- Uses cover and positioning
- Flanking maneuvers
- Strategic retreats

#### **Orange Circle** 🟠 = **Cowardly** (Archers only)
- Retreats easily
- Avoids direct confrontation
- Prefers safe distance

#### **Purple Triangle** 🟣 = **High Precision** (Archers only)
- Very accurate shots
- Rarely misses
- Dangerous at range

### 📊 Perk Count Display

Enemies with perks show a **gold number** next to their behavior indicator:
```
    [P] [🔴] [3] ← Patrol + Aggressive + 3 perks
   [Enemy]        ← Enemy sprite
```

**Perk Count Meaning:**
- **0 perks:** Basic enemy (easy)
- **1 perk:** Enhanced enemy (moderate)
- **2 perks:** Strong enemy (challenging)
- **3 perks:** Elite enemy (very dangerous)

**Perk Types Enemies Can Have:**
- **Health Boost** - More hit points
- **Speed Boost** - Faster movement
- **Damage Boost** - Stronger attacks
- **Stamina Boost** - More endurance
- **Range Boost** - Longer reach
- **Width Boost** - Wider attacks

## ⚙️ Configuration

### Enable Enemy Indicators
1. **Pause the game** (ESC key)
2. **Click "Settings"**
3. **Toggle "Show Enemy Indicators"** ON
4. **See behavior letters and personality indicators above enemies**

### Audio Settings
- Adjust music volume
- Toggle sound effects
- All settings save automatically

## 🚀 Getting Started

### Quick Start
```bash
# Ensure Java 21 is available
sdk use java 21.0.3-tem

# Run the game
./gradlew run
```

### Building from Source
```bash
./gradlew build
./gradlew test
```

## 💾 Save System

Your progress automatically saves when:
- Completing a level
- Selecting perks
- Game over or victory

Save files persist:
- World seed (for consistent maps)
- Completed maps count
- All collected perks

## 🎯 Strategy Tips

- **Study enemy indicators** to understand their behavior
- **Prioritize aggressive enemies** - they're the most dangerous
- **Use terrain** to avoid ranged enemies
- **Block strategically** - it consumes stamina but reduces damage by 80%
- **Dash to reposition** - especially useful against archers
- **Collect perks early** - they compound and make you much stronger

## 🔧 Technical Details

- **Engine:** Java2D with Swing
- **Map Generation:** Procedural with consistent seeds
- **AI System:** State-based behavior trees
- **Performance:** Optimized for 60 FPS gameplay

---

**The Legend of Belga** - Where every enemy tells a story through their behavior, and every map completion makes the world more dangerous. Can you survive the escalating challenge?
