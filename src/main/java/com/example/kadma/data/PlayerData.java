package com.example.kadma.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything the server remembers about one player.
 *
 * <p>Only fields that actually affect gameplay are stored. Nothing here is ever
 * trusted from the client: the client receives a HUD payload and nothing else.</p>
 */
public class PlayerData {

    public String uuid = "";
    public String lastKnownName = "";

    public long firstSeenDay = 0L;
    public long lastSeenDay = 0L;
    public long lastLoginMillis = 0L;
    public long playtimeSeconds = 0L;

    /** Personal main story resource "Прозрение". */
    public int prozrenie = 0;
    public int prozrenieCarried = 0;
    public long prozrenieDay = 0L;

    public String chapter = "act0_threshold";
    public int stage = 0;
    public boolean enteredCity = false;
    public boolean sparkTitle = false;
    public int completedCycles = 0;

    public Map<String, String> flags = new LinkedHashMap<>();
    public Set<String> completedQuests = new LinkedHashSet<>();
    public List<QuestProgress> activeQuests = new ArrayList<>();

    public List<String> journal = new ArrayList<>();
    public Set<String> knownRumors = new LinkedHashSet<>();
    public Set<String> unreadRumors = new LinkedHashSet<>();
    public Set<String> unlockedDialogues = new LinkedHashSet<>();

    public int mainDailyUsed = 0;
    public int sideDailyUsed = 0;
    public long dutyDay = 0L;

    public Map<String, Integer> counters = new LinkedHashMap<>();
    public Map<String, Long> cooldowns = new LinkedHashMap<>();

    public boolean hudEnabled = true;
    public String currentLayer = "";

    public List<String> history = new ArrayList<>();

    public PlayerData() {
    }

    public PlayerData(String uuid, long day) {
        this.uuid = uuid == null ? "" : uuid;
        this.firstSeenDay = day;
        this.lastSeenDay = day;
        this.prozrenieDay = day;
        this.dutyDay = day;
        this.chapter = "act0_threshold";
    }

    public FlagMap flags() {
        return new FlagMap(flags);
    }

    public boolean flag(String key) {
        return key != null && flags.containsKey(key);
    }

    public boolean flagIs(String key, String value) {
        return key != null && value != null && value.equals(flags.get(key));
    }

    public String flagValue(String key) {
        return key == null ? null : flags.get(key);
    }

    public QuestProgress active(String questId) {
        for (QuestProgress progress : activeQuests) {
            if (progress.questId.equals(questId) && !progress.abandoned) {
                return progress;
            }
        }
        return null;
    }

    public boolean hasActive(String questId) {
        return active(questId) != null;
    }

    public boolean isComplete(String questId) {
        return questId != null && completedQuests.contains(questId);
    }

    public int counter(String key) {
        Integer value = counters.get(key);
        return value == null ? 0 : value;
    }

    public void bump(String key, int delta) {
        counters.put(key, counter(key) + delta);
    }

    public boolean ready(String key, long currentDay, int periodDays) {
        Long last = cooldowns.get(key);
        if (last == null) {
            return true;
        }
        return currentDay - last >= periodDays;
    }

    public void touch(String key, long currentDay) {
        cooldowns.put(key, currentDay);
    }

    public boolean knownRumor(String rumorId) {
        return rumorId != null && knownRumors.contains(rumorId);
    }

    public int unreadRumorCount() {
        return unreadRumors.size();
    }

    public void rememberRumor(String rumorId) {
        if (rumorId == null || rumorId.isEmpty()) {
            return;
        }
        if (knownRumors.add(rumorId)) {
            unreadRumors.add(rumorId);
        }
    }

    public void readRumors() {
        unreadRumors.clear();
    }

    public void addJournal(String entry) {
        if (entry == null || entry.isEmpty()) {
            return;
        }
        if (!journal.contains(entry)) {
            journal.add(entry);
            if (journal.size() > 200) {
                journal.remove(0);
            }
        }
    }

    public boolean hasJournal(String entry) {
        return journal.contains(entry);
    }

    public void rememberHistory(String line) {
        if (line == null || line.isEmpty()) {
            return;
        }
        history.add(line);
        if (history.size() > 60) {
            history.remove(0);
        }
    }
}
