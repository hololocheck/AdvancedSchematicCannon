---
title: Air Placement Wand
id: tools/air-placement-wand
tags: [tool, item]
---

# Air Placement Wand

```embed:item id=advancedschematicannon:air_placement_wand size=48 label=true
```

Spends FE to place **Frame Blocks** in mid-air, for scaffolding or temporary supports.

[[TOC]]

## Controls

| Input | Effect |
|---|---|
| **Right click** | Place a Frame Block where you are looking |
| **Shift + right click** | Remove every block placed with this wand |
| **Shift + scroll** | Adjust the placement distance (**1 to 15 blocks**) |
| **Shift + middle click** | Reset the placement distance to its default |

While the wand is held, a **white outline** marks the target position. No outline is shown
when the target is not air.

## Energy

The remaining charge is on the item tooltip as `Energy: n / max FE`. When it runs out the
wand reports that energy is insufficient and places nothing.

## Frame Block

```embed:item id=advancedschematicannon:frame_block size=48 label=true
```

The scaffolding block this wand places. Because **Shift + right click removes them all at
once**, you can put them up while building and clear them away when you are done.
