package com.example.kadma.config;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** A phase of a weekly rite. */
public class EventPhase {

    public String id = "";
    public String title = "";
    public String description = "";
    public int seconds = 60;
    public String type = "gather";
    public String zone = "";
    public String layer = "";
    public int requiredPlayers = 1;
    public String objective = "";
    public int amount = 1;
    public String bossTitle = "";
    public List<String> messages = new ArrayList<>();
    public List<String> completeFlags = new ArrayList<>();
    public boolean failOnEmpty = false;
    public String onCompleteRumor = "";
    public MapHolder globalChanges = new MapHolder();
    public MapHolder rewards = new MapHolder();

    public static class MapHolder {
        public java.util.Map<String, Integer> stats = new java.util.LinkedHashMap<>();
        public List<String> flags = new ArrayList<>();
        public List<String> items = new ArrayList<>();
    }

    public static EventPhase from(JsonObject raw) {
        EventPhase phase = new EventPhase();
        if (raw == null) {
            return phase;
        }
        phase.id = com.example.kadma.data.JsonUtils.str(raw, "id", "");
        phase.title = com.example.kadma.data.JsonUtils.str(raw, "title", phase.id);
        phase.description = com.example.kadma.data.JsonUtils.str(raw, "description", "");
        phase.seconds = Math.max(1, com.example.kadma.data.JsonUtils.integer(raw, "seconds", 60));
        phase.type = com.example.kadma.data.JsonUtils.str(raw, "type", "gather").toLowerCase(java.util.Locale.ROOT);
        phase.zone = com.example.kadma.data.JsonUtils.str(raw, "zone", "");
        phase.layer = com.example.kadma.data.JsonUtils.str(raw, "layer", "");
        phase.requiredPlayers = Math.max(1, com.example.kadma.data.JsonUtils.integer(raw, "requiredPlayers", 1));
        phase.objective = com.example.kadma.data.JsonUtils.str(raw, "objective", "");
        phase.amount = Math.max(1, com.example.kadma.data.JsonUtils.integer(raw, "amount", 1));
        phase.bossTitle = com.example.kadma.data.JsonUtils.str(raw, "bossTitle", "");
        phase.messages = com.example.kadma.data.JsonUtils.stringList(raw, "messages");
        phase.completeFlags = com.example.kadma.data.JsonUtils.stringList(raw, "completeFlags");
        phase.failOnEmpty = com.example.kadma.data.JsonUtils.bool(raw, "failOnEmpty", false);
        phase.onCompleteRumor = com.example.kadma.data.JsonUtils.str(raw, "onCompleteRumor", "");
        JsonObject changes = com.example.kadma.data.JsonUtils.object(raw, "globalChanges");
        if (changes != null) {
            JsonObject stats = com.example.kadma.data.JsonUtils.object(changes, "stats");
            if (stats != null) {
                for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : stats.entrySet()) {
                    phase.globalChanges.stats.put(entry.getKey(), safeInt(entry.getValue()));
                }
            }
            phase.globalChanges.flags = com.example.kadma.data.JsonUtils.stringList(changes, "flags");
        }
        JsonObject rewards = com.example.kadma.data.JsonUtils.object(raw, "rewards");
        if (rewards != null) {
            JsonObject stats = com.example.kadma.data.JsonUtils.object(rewards, "stats");
            if (stats != null) {
                for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : stats.entrySet()) {
                    phase.rewards.stats.put(entry.getKey(), safeInt(entry.getValue()));
                }
            }
            phase.rewards.flags = com.example.kadma.data.JsonUtils.stringList(rewards, "flags");
            phase.rewards.items = com.example.kadma.data.JsonUtils.stringList(rewards, "items");
        }
        return phase;
    }

    private static int safeInt(com.google.gson.JsonElement element) {
        try {
            return element.getAsInt();
        } catch (Exception ex) {
            return 0;
        }
    }
}
