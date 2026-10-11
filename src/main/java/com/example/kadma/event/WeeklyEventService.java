package com.example.kadma.event;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.EventConfig;
import com.example.kadma.data.GlobalData;
import com.example.kadma.util.TextUtil;
import net.minecraft.server.level.ServerPlayer;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Weekly rites.
 *
 * <p>Rites are scheduled from the config or started by an admin. They show a boss
 * bar, run through phases, modify the world state and hand out consequences. A
 * player who misses the rite entirely can pick up its aftermath quest later.</p>
 */
public class WeeklyEventService {

    private final KadmaServices services;
    private final Map<String, EventRunner> active = new LinkedHashMap<>();
    private long lastScheduleCheck = 0L;

    public WeeklyEventService(KadmaServices services) {
        this.services = services;
    }

    public Map<String, EventRunner> active() {
        return active;
    }

    public EventRunner runner(String eventId) {
        return active.get(eventId);
    }

    public boolean running(String eventId) {
        EventRunner runner = active.get(eventId);
        return runner != null && !runner.isFinished();
    }

    /** Start a configured rite. */
    public boolean start(String eventId) {
        if (running(eventId)) {
            return false;
        }
        EventConfig definition = services.config().event(eventId);
        if (definition == null) {
            return false;
        }
        GlobalData global = services.data().global();
        EventRunner runner = new EventRunner(services, definition);
        active.put(eventId, runner);
        global.activeEvent = eventId;
        global.eventPhase.put(eventId, 0);
        services.data().touch();
        runner.start(global.worldTick);

        for (String rumor : definition.rumorsOnStart) {
            services.rumors().broadcast(definition.layer, rumor);
        }
        for (ServerPlayer player : services.server().getPlayerList().getPlayers()) {
            player.sendSystemMessage(TextUtil.ok("\u0421\u043e\u0431\u044b\u0442\u0438\u0435: " + definition.title));
            services.visual().sound().ritualStart(player);
        }
        return true;
    }

    public boolean stop() {
        GlobalData global = services.data().global();
        boolean stopped = false;
        for (EventRunner runner : active.values()) {
            runner.stop();
            stopped = true;
        }
        active.clear();
        global.activeEvent = "";
        global.eventPhase.clear();
        services.data().touch();
        return stopped;
    }

    public boolean stop(String eventId) {
        EventRunner runner = active.remove(eventId);
        if (runner == null) {
            return false;
        }
        runner.stop();
        GlobalData global = services.data().global();
        if (eventId.equals(global.activeEvent)) {
            global.activeEvent = "";
        }
        global.eventPhase.remove(eventId);
        services.data().touch();
        return true;
    }

    /** Evaluate the schedule. Called at most once per in-game minute. */
    public void tick() {
        GlobalData global = services.data().global();
        global.worldTick++;
        for (EventRunner runner : active.values()) {
            runner.tick(global.worldTick);
        }
        active.entrySet().removeIf(entry -> entry.getValue().isFinished());
        if (!services.config().settings().features.weeklyEvents) {
            return;
        }
        long minute = global.worldTick / 1200L;
        if (minute == lastScheduleCheck) {
            return;
        }
        lastScheduleCheck = minute;
        maybeSchedule(global);
    }

    /** Start whichever rite is due. */
    public void maybeSchedule(GlobalData global) {
        for (EventConfig definition : services.config().events().values()) {
            if (active.containsKey(definition.id)) {
                continue;
            }
            long week = global.worldTick / (1200L * 60L * 24L);
            Long scheduled = services.data().global().chapterHistory.get("event." + definition.id);
            if (scheduled != null && scheduled == week) {
                continue;
            }
            if (!due(definition)) {
                continue;
            }
            global.chapterHistory.put("event." + definition.id, week);
            services.data().touch();
            start(definition.id);
            return;
        }
    }

    private boolean due(EventConfig definition) {
        if (definition.manualOnly) {
            return false;
        }
        LocalDateTime next = new EventRunner(services, definition).nextRun();
        if (next == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !next.isAfter(now) && !next.isBefore(now.minusHours(2));
    }

    public int activeCount() {
        return active.size();
    }
}
