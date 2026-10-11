package com.example.kadma.story;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.ModSettings;
import com.example.kadma.data.PlayerData;
import com.example.kadma.util.TimeUtil;
import net.minecraft.server.level.ServerPlayer;

/**
 * "Прозрение" - the personal main story resource.
 *
 * <ul>
 *   <li>capped at {@code maxProzrenie} (default 3)</li>
 *   <li>regenerates once per configured day (default 1)</li>
 *   <li>unused units carry over and never expire instantly</li>
 *   <li>only the server grants or spends it</li>
 * </ul>
 */
public class ProzrenieService {

    private final KadmaServices services;

    public ProzrenieService(KadmaServices services) {
        this.services = services;
    }

    public int max(PlayerData data) {
        ModSettings settings = services.config().settings();
        int base = settings.maxProzrenie;
        int bonus = bonus(data);
        return Math.max(0, base + bonus);
    }

    private int bonus(PlayerData data) {
        int bonus = 0;
        if (data.completedCycles > 0) {
            bonus += Math.min(2, data.completedCycles);
        }
        return bonus;
    }

    public int value(PlayerData data) {
        return Math.max(0, data.prozrenie);
    }

    public int carried(PlayerData data) {
        return Math.max(0, data.prozrenieCarried);
    }

    public boolean spend(ServerPlayer player, PlayerData data, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (value(data) < amount) {
            return false;
        }
        data.prozrenie -= amount;
        mark(services, data);
        return true;
    }

    public int grant(ServerPlayer player, PlayerData data, int amount) {
        if (amount <= 0) {
            return 0;
        }
        int cap = max(data);
        int before = value(data);
        data.prozrenie = Math.min(cap, before + amount);
        int gained = data.prozrenie - before;
        if (gained > 0) {
            mark(services, data);
        }
        return gained;
    }

    public void set(ServerPlayer player, PlayerData data, int amount) {
        data.prozrenie = Math.max(0, Math.min(max(data), amount));
        mark(services, data);
    }

    /** Called on the daily reset. Carries leftovers and adds the daily regeneration. */
    public int regenerate(ServerPlayer player, PlayerData data) {
        long today = TimeUtil.currentDay(services.config().settings().timezoneOffsetMinutes);
        if (data.prozrenieDay == 0L) {
            data.prozrenieDay = today;
            data.prozrenie = value(data);
            mark(services, data);
            return 0;
        }
        long missed = today - data.prozrenieDay;
        if (missed <= 0) {
            return 0;
        }
        long cappedMissed = Math.min(missed, 7L);
        int cap = max(data);
        int before = value(data);
        int regen = (int) (cappedMissed * Math.max(0, services.config().settings().prozrenieRegenPerDay));
        data.prozrenie = Math.min(cap, before + regen);
        data.prozrenieCarried = Math.max(0, before);
        data.prozrenieDay = today;
        mark(services, data);
        return data.prozrenie - before;
    }

    /** Regeneration path used for offline players touched by the daily reset. */
    public int regenerateOffline(PlayerData data) {
        long today = TimeUtil.currentDay(services.config().settings().timezoneOffsetMinutes);
        if (data.prozrenieDay == 0L) {
            data.prozrenieDay = today;
            return 0;
        }
        long missed = today - data.prozrenieDay;
        if (missed <= 0) {
            return 0;
        }
        long cappedMissed = Math.min(missed, 7L);
        int cap = max(data);
        int before = value(data);
        int regen = (int) (cappedMissed * Math.max(0, services.config().settings().prozrenieRegenPerDay));
        data.prozrenie = Math.min(cap, before + regen);
        data.prozrenieCarried = Math.max(0, before);
        data.prozrenieDay = today;
        return data.prozrenie - before;
    }

    public boolean canAfford(PlayerData data, int amount) {
        return value(data) >= Math.max(0, amount);
    }

    /** Marks a record dirty even when there is no online player behind it. */
    private static void mark(com.example.kadma.KadmaServices services, PlayerData data) {
        if (data == null || data.uuid == null || data.uuid.isEmpty()) {
            services.data().touch();
            return;
        }
        try {
            services.data().markPlayer(java.util.UUID.fromString(data.uuid));
        } catch (IllegalArgumentException ex) {
            services.data().touch();
        }
    }
}
