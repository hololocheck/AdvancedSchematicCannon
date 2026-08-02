---
title: Range Board
id: tools/range-board
tags: [tool, item]
---

# Range Board

```embed:item id=advancedschematicannon:range_board size=48 label=true
```

Marks the cuboid region that Filler mode works on. While you hold it, a **mode bar** is
shown at the bottom of the screen.

[[TOC]]

## Normal mode

| Input | Effect |
|---|---|
| **Right click** a block | Set Pos1, then Pos2 |
| **Shift + right click** | Clear the whole region |

With only Pos1 set, **Pos2 follows where you look** and the cyan outline moves with it.

## Edit mode

For fixing just one corner of a region you already marked.

| Input | Effect |
|---|---|
| **Alt + scroll** | Switch between Normal and Edit |
| **Shift + scroll** | Pick the point to edit: none, Pos1, Pos2 |
| **Shift + left click** | Clear only the selected point |

The selected point follows where you look.

## The mode bar

While Alt is held, a panel slides up under the bar with **a description of the current
mode**. The bar is also hidden while the HUD is hidden with F1.

> [!NOTE]
> A board with a region on it has to be **put into the cannon's schematic slot** to be used.
> Without that, Filler mode has no region to work on.
