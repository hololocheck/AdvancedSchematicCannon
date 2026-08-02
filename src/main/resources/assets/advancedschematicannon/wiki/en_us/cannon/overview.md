---
title: The cannon screen
id: cannon/overview
tags: [gui]
---

# The cannon screen

[[TOC]]

## Layout

| Where | What |
|---|---|
| Left | **Block list / material slots** (4 columns x 16 rows) |
| Top right | Status, progress bar, current mode |
| Below | Power gauge / storage selector |
| Below | I/O slots and play, pause, stop |
| Below | Mode switch / speed |
| Below | Reuse, preview and EMC toggles |
| Bottom right | Owner icon |

## The grid on the left

In **Schematic mode** it lists the blocks the schematic needs and how many of each.
In **Filler mode** the very same grid becomes the **slots you put materials into**.

Scroll the wheel over the grid to move through the list.

## Switching modes

The mode button takes two different inputs.

- **Hover and scroll** — switch between Schematic mode and Filler mode
- **Click** — the options for the current mode appear directly below the button

Those options are the four [replace modes](schematic-mode.md) in Schematic mode, or the
seven [filler modules](filler-mode.md) in Filler mode.

## Speed

**Speed** is how many blocks are placed per tick, from **1 to 256**. Hover the value and
scroll to change it. Higher values build faster but draw more power.

## Toggles

| Name | Effect |
|---|---|
| **Reuse** | Keeps the schematic after the build finishes instead of consuming it |
| **Preview** | Draws the build area outline in the world |
| **Use EMC** | EMC Schematic Cannon only. Spends EMC to place blocks |

## Owner icon

The player face at the bottom right is whoever placed this cannon. **The border colour is
the permission.**

- **Green = public** — anyone can change the settings
- **Red = private** — only the owner and operators can

Click it to switch, but **only the owner can do so**.
