package com.example.advancedschematicannon.block;

import java.util.UUID;

/**
 * Who owns a cannon and what that lets a player do: the decisions alone, on UUIDs and flags, so a test holds them
 * without Minecraft - this repo's unit-test classpath has none (see AscScreenCatalogueTest). The two cannon blocks,
 * {@link EMCSchematicCannonBlockEntity}, its menu and screen, and {@code network.CannonData} supply the facts and
 * apply the answers. The rules, as the user chose them on 2026-10-07:
 *
 * <ul>
 *   <li>The owner is the player who placed the cannon. Opening its screen or starting a job does not change it -
 *       until 2026-10-07 both did, so whoever opened an idle cannon took it over and the private setting did
 *       nothing. A cannon nobody owns (placed before this rule and never opened, or placed by a fake player such as
 *       Create's deployer) goes to the first player who opens it or starts a job on it. Nothing reassigns an owner:
 *       break the cannon and place it again.</li>
 *   <li>Ops count as the owner in everything.</li>
 *   <li>Public: anyone may open the cannon and change its settings. Private: only the owner and ops may.</li>
 *   <li>Starting, pausing, resuming and stopping a job, and switching public / private, are the owner's and the
 *       ops' alone, public or not. A job runs on the owner: their EMC, and only while they are online.</li>
 * </ul>
 */
public final class CannonOwnership {

    private CannonOwnership() {
    }

    /**
     * The owner once the cannon has been placed: the player who placed it. {@code placer} is null when no player did
     * (a fake player, a dispenser) - then the owner stays what it was, which for a new cannon is nobody.
     */
    public static UUID placed(UUID owner, UUID placer) {
        return placer != null ? placer : owner;
    }

    /** The owner once {@code player} has opened the cannon or started a job on it: unchanged, unless nobody owned it. */
    public static UUID used(UUID owner, UUID player) {
        return owner != null ? owner : player;
    }

    /**
     * Counts as the owner - starts, pauses, resumes and stops jobs and switches public / private: the owner, an op,
     * or anyone while nobody owns the cannon.
     */
    public static boolean actsAsOwner(UUID owner, UUID player, boolean op) {
        return owner == null || op || owner.equals(player);
    }

    /** May open the screen and change the settings: anyone on a public cannon, the owner and ops on a private one. */
    public static boolean mayUse(UUID owner, UUID player, boolean op, boolean publicAccess) {
        return publicAccess || actsAsOwner(owner, player, op);
    }
}
