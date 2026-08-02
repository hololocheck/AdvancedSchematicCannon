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

| Shown as | Behaviour |
|---|---|
| **Don't replace solid blocks** | Only place into air |
| **Replace solid with solid** | Replace existing solid blocks with the schematic's solid blocks |
| **Replace solid with any** | Replace existing solid blocks with whatever the schematic has, air included |
| **Replace solid with empty** | Break solid blocks that the schematic leaves as air |

## Other settings

- **Skip missing blocks** — skip blocks with no matching item and carry on. With this off
  the build stops there and the status becomes an error.
- **Protect block entities** — never overwrite blocks that hold contents, such as chests.

> [!NOTE]
> Both toggles are shown only in Schematic mode; they have no meaning in Filler mode.

## Reading the progress

The status panel shows `placed/total (%)` and `Remaining: N blocks`. If a block is missing
its name is shown in red.
