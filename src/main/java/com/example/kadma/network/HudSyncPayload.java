package com.example.kadma.network;

/**
 * Thin alias so the payload type used by the network layer has a stable name.
 *
 * <p>The real serialisable class is {@link KadmaNetwork.HudSyncPacket}; this one only
 * exists so a content author can search for "HudSyncPayload" in the source tree.</p>
 */
public final class HudSyncPayload {

    private HudSyncPayload() {
    }

    /** Build a packet that pushes new HUD text. */
    public static KadmaNetwork.HudSyncPacket text(String json, boolean enabled) {
        return KadmaNetwork.HudSyncPacket.of(json, enabled);
    }
}
