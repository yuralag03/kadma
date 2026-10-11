package com.example.kadma.config;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Weekly rite definition. */
public class EventConfig {

    public String id = "";
    public String title = "";
    public String description = "";
    public String layer = "";
    public String zone = "";
    public int durationSeconds = 900;
    public String weekday = "friday";
    public int hour = 20;
    public int minute = 0;
    public boolean manualOnly = false;
    public int minPlayers = 1;
    public String bossColor = "YELLOW";
    public String bossOverlay = "PROGRESS";
    public List<EventPhase> phases = new ArrayList<>();
    public RewardDefinition rewards = new RewardDefinition();
    public java.util.Map<String, Integer> globalChanges = new java.util.LinkedHashMap<>();
    public List<String> globalFlags = new ArrayList<>();
    public String aftermathQuest = "";
    public List<String> rumorsOnStart = new ArrayList<>();
    public List<String> rumorsOnEnd = new ArrayList<>();
    public List<String> announceText = new ArrayList<>();
    public List<String> problems = new ArrayList<>();

    public static EventConfig from(JsonObject raw) {
        EventConfig event = new EventConfig();
        if (raw == null) {
            event.problems.add("\u043f\u0443\u0441\u0442\u043e\u0435 \u0441\u043e\u0431\u044b\u0442\u0438\u0435");
            return event;
        }
        event.id = com.example.kadma.data.JsonUtils.str(raw, "id", "");
        event.title = com.example.kadma.data.JsonUtils.str(raw, "title", event.id);
        event.description = com.example.kadma.data.JsonUtils.str(raw, "description", "");
        event.layer = com.example.kadma.data.JsonUtils.str(raw, "layer", "");
        event.zone = com.example.kadma.data.JsonUtils.str(raw, "zone", "");
        event.durationSeconds = Math.max(60, com.example.kadma.data.JsonUtils.integer(raw, "durationSeconds", 900));
        event.aftermathQuest = com.example.kadma.data.JsonUtils.str(raw, "aftermathQuest", "");
        event.minPlayers = Math.max(1, com.example.kadma.data.JsonUtils.integer(raw, "minPlayers", 1));
        event.bossColor = com.example.kadma.data.JsonUtils.str(raw, "bossColor", "YELLOW").toUpperCase(java.util.Locale.ROOT);
        event.bossOverlay = com.example.kadma.data.JsonUtils.str(raw, "bossOverlay", "PROGRESS").toUpperCase(java.util.Locale.ROOT);
        event.rumorsOnStart = com.example.kadma.data.JsonUtils.stringList(raw, "rumorsOnStart");
        event.rumorsOnEnd = com.example.kadma.data.JsonUtils.stringList(raw, "rumorsOnEnd");
        event.announceText = com.example.kadma.data.JsonUtils.stringList(raw, "announceText");
        event.globalFlags = com.example.kadma.data.JsonUtils.stringList(raw, "globalFlags");

        JsonObject schedule = com.example.kadma.data.JsonUtils.object(raw, "schedule");
        if (schedule != null) {
            event.weekday = com.example.kadma.data.JsonUtils.str(schedule, "day", "friday").toLowerCase(java.util.Locale.ROOT);
            event.hour = com.example.kadma.data.JsonUtils.integer(schedule, "hour", 20);
            event.minute = com.example.kadma.data.JsonUtils.integer(schedule, "minute", 0);
            event.manualOnly = com.example.kadma.data.JsonUtils.bool(schedule, "manualOnly", false);
        }
        JsonObject boss = com.example.kadma.data.JsonUtils.object(raw, "bossbar");
        if (boss != null) {
            event.bossColor = com.example.kadma.data.JsonUtils.str(boss, "color", event.bossColor).toUpperCase(java.util.Locale.ROOT);
            event.bossOverlay = com.example.kadma.data.JsonUtils.str(boss, "overlay", event.bossOverlay).toUpperCase(java.util.Locale.ROOT);
        }
        com.google.gson.JsonArray rawPhases = com.example.kadma.data.JsonUtils.array(raw, "phases");
        if (rawPhases != null) {
            for (com.google.gson.JsonElement element : rawPhases) {
                if (element.isJsonObject()) {
                    event.phases.add(EventPhase.from(element.getAsJsonObject()));
                }
            }
        }
        event.rewards = RewardDefinition.from(com.example.kadma.data.JsonUtils.object(raw, "rewards"));
        JsonObject changes = com.example.kadma.data.JsonUtils.object(raw, "globalChanges");
        if (changes != null) {
            for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : changes.entrySet()) {
                try {
                    event.globalChanges.put(entry.getKey(), entry.getValue().getAsInt());
                } catch (Exception ex) {
                    // ignore non numeric entry
                }
            }
        }
        if (event.id.isEmpty()) {
            event.problems.add("event without id");
        }
        if (event.phases.isEmpty()) {
            event.problems.add("event " + event.id + " without phases");
        }
        return event;
    }
}
