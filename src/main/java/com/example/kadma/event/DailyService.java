package com.example.kadma.event;

import com.example.kadma.KadmaServices;
import com.example.kadma.data.GlobalData;
import com.example.kadma.data.PlayerData;
import com.example.kadma.util.TextUtil;
import com.example.kadma.util.TimeUtil;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Daily duties.
 *
 * <p>The reset runs on a bounded cadence, resets per-day counters, regenerates
 * Прозрение (capped), rotates duty quests, processes the chapter window and refreshes
 * rumors. A player who was offline across several resets still gets their carried
 * resources and can enter through the catch-up path, so nobody is walled out.</p>
 */
public class DailyService {

    private final KadmaServices services;
    private long lastCheckedDay = -1L;
    private long nextResetSecond = -1L;

    public DailyService(KadmaServices services) {
        this.services = services;
        GlobalData global = services.data().global();
        this.lastCheckedDay = global.lastDailyResetDay;
    }

    public long lastResetDay() {
        return lastCheckedDay;
    }

    /** Called on the server tick. */
    public void tick() {
        com.example.kadma.config.ModSettings settings = services.config().settings();
        long today = TimeUtil.currentDay(settings.timezoneOffsetMinutes);
        if (lastCheckedDay == today) {
            return;
        }
        lastCheckedDay = today;
        reset(today);
    }

    /**
     * Perform the reset. Safe to call on boot to catch up on missed resets.
     * Regeneration is capped so a long downtime can never mint unlimited resources.
     */
    public void reset(long day) {
        GlobalData global = services.data().global();
        global.lastDailyResetDay = day;
        services.data().touch();

        int offset = services.config().settings().timezoneOffsetMinutes;
        Set<UUID> online = services.server().getPlayerList().getPlayers().stream()
                .map(ServerPlayer::getUUID).collect(Collectors.toSet());

        for (UUID id : services.data().cachedIds()) {
            PlayerData data = services.data().peek(id);
            if (data == null) {
                continue;
            }
            offlineReset(data);
            if (online.contains(id)) {
                ServerPlayer player = services.server().getPlayerList().getPlayer(id);
                if (player != null) {
                    services.story().prozrenie().regenerate(player, data);
                    services.story().onDailyReset(player, data);
                    notify(player, data);
                }
            }
        }
        services.data().saveAll();
        services.story().onDailyResetWorld(global);
        services.rumors().refresh();
        services.journal().noteReset(day);
    }

    private void offlineReset(PlayerData data) {
        int gained = services.story().prozrenie().regenerateOffline(data);
        if (gained != 0) {
            services.data().markPlayer(valid(data));
        }
        if (data.dutyDay != data.lastSeenDay) {
            data.dutyDay = data.lastSeenDay;
            data.mainDailyUsed = 0;
            data.sideDailyUsed = 0;
            data.counters.clear();
        }
        data.dutyDay = lastCheckedDay;
        data.mainDailyUsed = 0;
        data.sideDailyUsed = 0;
        data.counters.clear();
        if (valid(data) != null) {
            services.data().markPlayer(valid(data));
        }
    }

    private UUID valid(PlayerData data) {
        if (data.uuid == null || data.uuid.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(data.uuid);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void notify(ServerPlayer player, PlayerData data) {
        com.example.kadma.config.ModSettings settings = services.config().settings();
        int prozrenie = services.story().prozrenieOf(data);
        player.sendSystemMessage(TextUtil.info("\u0414\u0435\u043d\u044c \u0441\u043c\u0435\u043d\u0438\u043b\u0441\u044f. "
                + "\u041f\u0440\u043e\u0437\u0440\u0435\u043d\u0438\u0435: " + prozrenie + "/"
                + services.story().prozrenieMax(data) + "."));
        if (data.unreadRumorCount() > 0) {
            player.sendSystemMessage(TextUtil.info("\u041d\u043e\u0432\u044b\u0435 \u0441\u043b\u0443\u0445\u0438: "
                    + data.unreadRumorCount() + ". /kadma journal"));
        }
        List<String> announcements = settings == null ? List.of() : List.of();
        if (!announcements.isEmpty()) {
            // reserved for per-day announcements
        }
    }

    /** Manual reset for a single player (admin command). */
    public void resetPlayer(ServerPlayer player, PlayerData data) {
        services.story().prozrenie().regenerate(player, data);
        data.mainDailyUsed = 0;
        data.sideDailyUsed = 0;
        data.counters.clear();
        data.dutyDay = TimeUtil.currentDay(services.config().settings().timezoneOffsetMinutes);
        services.data().markPlayer(player.getUUID());
        notify(player, data);
    }

    /** Called when a player logs in: catch up their duties without spamming resources. */
    public void onLogin(ServerPlayer player, PlayerData data) {
        com.example.kadma.config.ModSettings settings = services.config().settings();
        long today = TimeUtil.currentDay(settings.timezoneOffsetMinutes);
        if (data.dutyDay != today) {
            services.story().prozrenie().regenerate(player, data);
            data.dutyDay = today;
            services.data().markPlayer(player.getUUID());
        }
    }

    public long secondsUntilReset() {
        com.example.kadma.config.ModSettings settings = services.config().settings();
        if (nextResetSecond < 0) {
            nextResetSecond = TimeUtil.secondsUntilNextReset(settings.timezoneOffsetMinutes, settings.dailyResetHour);
        }
        long now = System.currentTimeMillis() / 1000L;
        return Math.max(0L, nextResetSecond - now);
    }
}
