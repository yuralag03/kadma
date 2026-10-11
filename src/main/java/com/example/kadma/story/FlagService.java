package com.example.kadma.story;

import com.example.kadma.KadmaServices;
import com.example.kadma.data.GlobalData;
import com.example.kadma.data.PlayerData;

/**
 * Flag authority. Everything that can gate content reads flags from here; nothing
 * writes them except the server, in response to validated events.
 */
public class FlagService {

    private final KadmaServices services;

    public FlagService(KadmaServices services) {
        this.services = services;
    }

    public boolean setPlayer(PlayerData data, String key, String value) {
        if (data == null || key == null || key.isEmpty()) {
            return false;
        }
        String payload = value == null ? "true" : value;
        String previous = data.flags.put(key, payload);
        if (previous == null || !previous.equals(payload)) {
            services.data().markPlayer(java.util.UUID.fromString(safeUuid(data)));
            return true;
        }
        return false;
    }

    public boolean removePlayer(PlayerData data, String key) {
        if (data == null || key == null) {
            return false;
        }
        boolean removed = data.flags.remove(key) != null;
        if (removed) {
            services.data().markPlayer(java.util.UUID.fromString(safeUuid(data)));
        }
        return removed;
    }

    public boolean hasPlayer(PlayerData data, String key) {
        return data != null && data.flags.containsKey(key);
    }

    public boolean setGlobal(GlobalData global, String key, String value) {
        if (global == null || key == null || key.isEmpty()) {
            return false;
        }
        String payload = value == null ? "true" : value;
        String previous = global.flags.put(key, payload);
        boolean changed = previous == null || !previous.equals(payload);
        if (changed) {
            services.data().touch();
        }
        return changed;
    }

    public boolean removeGlobal(GlobalData global, String key) {
        if (global == null || key == null) {
            return false;
        }
        boolean removed = global.flags.remove(key) != null;
        if (removed) {
            services.data().touch();
        }
        return removed;
    }

    public boolean hasGlobal(GlobalData global, String key) {
        return global != null && global.flags.containsKey(key);
    }

    /** Flags shared by many players become global progression. */
    public void promoteIfShared(String key, int required) {
        GlobalData global = services.data().global();
        if (global.flags.containsKey(key)) {
            return;
        }
        int hits = 0;
        for (PlayerData data : services.data().snapshot().values()) {
            if (data.flags.containsKey(key)) {
                hits++;
            }
        }
        if (hits >= required) {
            global.flags.put(key, "true");
            services.data().touch();
        }
    }

    private static String safeUuid(PlayerData data) {
        return data.uuid == null || data.uuid.isEmpty() ? "00000000-0000-0000-0000-000000000000" : data.uuid;
    }
}
