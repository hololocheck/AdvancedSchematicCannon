---
title: Schematic mode
id: cannon/schematic-mode
tags: [mode]
---

# Schematic mode

Prints a Create schematic as-is.

[[TOC]]

## Replace modes

These decide what happens where something already stands. Click the mode button to reveal
the four icons, with two switches to their right ([see below](#switches)).

![](bws:advancedschematicannon:wiki/screens/emc-schematic-cannon__options__en_us.png)

| Shown as | Behaviour |
|---|---|
| **Don't replace solid blocks** | Only place into air |
| **Replace solid with solid** | Replace existing solid blocks with the schematic's solid blocks |
| **Replace solid with any** | Replace existing solid blocks with whatever the schematic has, air included |
| **Replace solid with empty** | Break solid blocks that the schematic leaves as air |

## The skip and protect switches {#switches}

The two switches to the right of the replace icons: the skip icon and the shield icon. Both start off.

| Shown as | Behaviour |
|---|---|
| **Skip Missing Blocks** | On: a block whose material cannot be had is skipped and the cannon moves on (a skipped block counts as placed). Off: the cannon waits at that spot for the material |
| **Protect Block Entities** | On: where a block with a block entity stands, such as a chest or a furnace, it is left as it is. Off: it is overwritten as the replace mode says |

The switches show only in the schematic mode's strip, but the cannon keeps the settings, and they act the same
way when it runs in the filler mode.

## When a material runs short

While Skip Missing Blocks is off, a missing block does not stop the cannon with an error: it **waits at that spot
for the material** and carries on once it arrives (the missing block's name is shown in red in the progress readout
below). While it is on, the block is skipped - a block AE2 was asked to craft is skipped too, without waiting for
the craft.

## Reading the progress

The status panel shows `placed/total (%)` and `Remaining: N blocks`. If a block is missing
its name is shown in red.
