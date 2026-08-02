---
title: Filler mode
id: cannon/filler-mode
tags: [mode]
---

# Filler mode

Works on **the region marked with the [Range Board](../tools/range-board.md)** instead of a
schematic.

[[TOC]]

## How to use it

1. Mark a region with the [Range Board](../tools/range-board.md).
2. Put that board into the cannon's **schematic slot** in the I/O row.
3. Put the **materials to use** into the grid on the left (4 columns x 16 rows).
4. Pick a module and press play.

## Modules

Click the mode button to reveal the seven icons.

| Module | Behaviour |
|---|---|
| **Fill** | Fill the air inside the region with your materials |
| **Erase** | Clear the whole region to air |
| **Remove** | Collect the blocks inside the region |
| **Wall** | Build only the side faces of the region |
| **Tower** | Stack columns up from the base of the region |
| **Box** | Build the outer shell of the region (all six faces) |
| **Circular wall** | Build a cylindrical wall inscribed in the region |

## What is different in Remove

In **Remove**, the grid on the left is not material slots — it becomes the **list of blocks
found inside the region**. Where they go follows the storage setting.

- **Insert into AE** — into the ME network
- **Insert into chest** — into adjacent containers

On the EMC Schematic Cannon with **Use EMC** enabled, collected blocks can be converted to
EMC instead.
