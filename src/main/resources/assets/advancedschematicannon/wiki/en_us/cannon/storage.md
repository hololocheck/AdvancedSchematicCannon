---
title: Materials and EMC
id: cannon/storage
tags: [ae2, projecte]
---

# Materials and EMC

[[TOC]]

## Storage selection

The **Storage** control decides where materials are taken from. Hover the value and scroll
to change it.

| Setting | Source |
|---|---|
| **Extract from AE2 & Chests** | Both the ME network and adjacent containers |
| **AE only** | The ME network only |
| **Chests only** | Adjacent containers only |

In Remove mode the meaning flips to the destination, and the choice narrows to
**Insert into AE / Insert into chest**.

## AE2 integration

Connecting an AE2 cable to the cannon provides **power and ME storage access at the same
time**.

When a material is not in the ME network the cannon **requests autocrafting** (which runs on
the AE2 side if a pattern exists).

> [!NOTE]
> Autocrafting only goes as far as submitting the request. **The cannon does not wait for
> the craft to finish** — that pass counts the block as missing and extraction is retried
> once the item arrives.

## EMC (ProjectE)

The **EMC Schematic Cannon** is only added when ProjectE is installed.

- The screen gains a **Use EMC** toggle and an **EMC fuel slot**
- With Use EMC on, blocks can be placed by spending EMC even with no material in stock
- In Remove mode, collected blocks can be converted to EMC
- Items put into the fuel slot are converted to EMC and added to the **owner's** EMC.
  **Ctrl + left click** sends an item straight from your inventory into that slot

> [!NOTE]
> Conversion needs three things: the cannon must **have an owner**, that **owner must be
> online**, and the item must **have an EMC value**. While the owner is offline nothing is
> added and the fuel stays in the slot. Whether the cannon is running does not matter —
> fuel is converted while idle too.

The Enhanced Schematic Cannon shows nothing EMC-related; both the slot and the toggle are
hidden.
