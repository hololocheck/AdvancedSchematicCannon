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
the four icons.

![](bws:advancedschematicannon:wiki/screens/emc-schematic-cannon__options__en_us.png)

| Shown as | Behaviour |
|---|---|
| **Don't replace solid blocks** | Only place into air |
| **Replace solid with solid** | Replace existing solid blocks with the schematic's solid blocks |
| **Replace solid with any** | Replace existing solid blocks with whatever the schematic has, air included |
| **Replace solid with empty** | Break solid blocks that the schematic leaves as air |

## When a material runs short

A missing block does not stop the cannon with an error: it **waits at that spot for the material** and carries
on once it arrives (the missing block's name is shown in red in the progress readout below).

> [!NOTE]
> The "skip missing blocks" and "protect block entities" switches of the earlier screen are not on the current
> one. The cannon works as if both were off: it waits for what is missing, and blocks that hold contents, such as
> chests, are overwritten as the replace mode says.

## Reading the progress

The status panel shows `placed/total (%)` and `Remaining: N blocks`. If a block is missing
its name is shown in red.
