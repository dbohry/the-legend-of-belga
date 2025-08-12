# Shield Visual Integration Example

## Before (Separate Bars)
```
[HP Bar: ██████░░░░] [Shield Bar: ████░░░░░░]
Health: 6.0/6.0      Shield: 4.0/4.0
```

## After (Integrated Overlay)
```
[HP Bar: ████████████████]
Health: 6.0/6.0 + Shield: 4.0/4.0
```

## Visual Breakdown

### HP Bar Structure
```
┌─────────────────────────────────────────────────────────┐
│ ████████████████████████████████████████████████████████ │ ← HP Fill (Red)
│ ████████████████████████████████████████████████████████ │
└─────────────────────────────────────────────────────────┘
     ↑ Health (6.0/6.0)    ↑ Shield Overlay (4.0/4.0)
```

### Shield Overlay Details
- **Position**: Starts immediately after health ends
- **Color**: Semi-transparent blue (100, 180, 255, 120)
- **Border**: Thin blue border for definition
- **Transparency**: 120 alpha for subtle overlay effect

## Benefits of Integration

1. **Space Efficient**: No need for separate bar positioning
2. **Visual Clarity**: Shield clearly extends the health bar
3. **Consistent Layout**: All bars maintain fixed positions
4. **Better UX**: Players see total protection in one view
5. **Cleaner Design**: Less visual clutter in the HUD

## Shield Behavior Examples

### Full Health + Full Shield
```
[██████████████████████████████████████████████████████████]
Health: 6.0/6.0 + Shield: 4.0/4.0 = Total: 10.0
```

### Partial Health + Partial Shield
```
[██████████████████████████████████████████████████████████]
Health: 3.0/6.0 + Shield: 2.0/4.0 = Total: 5.0
```

### Low Health + No Shield
```
[██░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]
Health: 1.0/6.0 + Shield: 0.0/4.0 = Total: 1.0
```

## Technical Implementation

- **Fixed Bar Width**: 200 pixels (no dynamic adjustment)
- **Shield Overlay**: Rendered on top of health fill
- **Position Calculation**: `shieldStartX = x + healthWidth`
- **Width Calculation**: `shieldWidth = min(shieldRatio * BAR_WIDTH, BAR_WIDTH - healthWidth)`
- **Rendering Order**: Background → Health Fill → Shield Overlay → Borders → Text
