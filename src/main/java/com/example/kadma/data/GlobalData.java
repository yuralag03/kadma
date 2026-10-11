package com.example.kadma.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server wide world state. Persisted as a single small JSON file and reloaded on
 * startup, so the city keeps its memory across restarts.
 */
public class GlobalData {

    public Map<String, Integer> stats = new LinkedHashMap<>();
    public Map<String, String> flags = new LinkedHashMap<>();
    public String unlockedChapter = "act0_threshold";
    public String previousChapter = "";
    public String activeEvent = "";

    public long chapterOpenDay = 0L;
    public long chapterOpenTicks = 0L;
    public long lastDailyResetDay = 0L;
    public long totalMainStagesCompleted = 0L;
    public long totalEventsCompleted = 0L;
    public long worldTick = 0L;

    public int contributors = 0;
    public Set<String> participants = new LinkedHashSet<>();
    public Map<String, Integer> eventPhase = new LinkedHashMap<>();
    public Map<String, Long> chapterHistory = new LinkedHashMap<>();
    public Set<String> firedRumors = new LinkedHashSet<>();
    public List<String> worldJournal = new ArrayList<>();
    public Map<String, String> customFlags = new LinkedHashMap<>();

    public GlobalData() {
    }

    public int stat(String key) {
        Integer value = stats.get(key);
        return value == null ? 0 : value;
    }

    public int stat(String key, int fallback) {
        Integer value = stats.get(key);
        return value == null ? fallback : value;
    }

    public void setStat(String key, int value) {
        if (key != null) {
            stats.put(key, value);
        }
    }

    public void bumpStat(String key, int delta) {
        if (key != null) {
            stats.put(key, stat(key) + delta);
        }
    }

    public boolean clampStat(String key, int min, int max) {
        int value = stat(key);
        int clamped = Math.max(min, Math.min(max, value));
        setStat(key, clamped);
        return clamped != value;
    }

    public FlagMap flags() {
        return new FlagMap(flags);
    }

    public void rememberWorld(String line) {
        if (line == null || line.isEmpty()) {
            return;
        }
        if (!worldJournal.isEmpty() && worldJournal.get(worldJournal.size() - 1).equals(line)) {
            return;
        }
        worldJournal.add(line);
        if (worldJournal.size() > 100) {
            worldJournal.remove(0);
        }
    }
}
