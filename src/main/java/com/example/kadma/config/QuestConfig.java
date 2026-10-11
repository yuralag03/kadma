package com.example.kadma.config;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Quest definition. Purely data, all validation happens on the server. */
public class QuestConfig {

    public String id = "";
    public String title = "";
    public String description = "";
    public String type = "side";
    public String chapter = "";
    public int stage = 0;
    public String layer = "";
    public String zone = "";
    public int dailyLimit = 0;
    public int requiresProzrenie = 0;
    public List<String> prerequisites = new ArrayList<>();
    public List<ObjectiveDefinition> objectives = new ArrayList<>();
    public RewardDefinition rewards = new RewardDefinition();
    public List<String> unlockQuests = new ArrayList<>();
    public boolean catchUp = false;
    public boolean aftermath = false;
    public boolean repeatable = false;
    public boolean autoStart = false;
    public List<String> journalEntries = new ArrayList<>();
    public List<String> rumorsOnStart = new ArrayList<>();
    public List<String> rumorsOnComplete = new ArrayList<>();
    public List<String> flagsOnComplete = new ArrayList<>();
    public List<String> globalFlagsOnComplete = new ArrayList<>();
    public int minStage = 0;
    public int maxStage = Integer.MAX_VALUE;
    public String marker = "";
    public double markerX;
    public double markerY;
    public double markerZ;
    public boolean markerAbsolute = false;
    public List<String> problems = new ArrayList<>();

    public static QuestConfig from(JsonObject raw) {
        QuestConfig quest = new QuestConfig();
        if (raw == null) {
            quest.problems.add("\u043f\u0443\u0441\u0442\u043e\u0435 \u043e\u043f\u0440\u0435\u0434\u0435\u043b\u0435\u043d\u0438\u0435");
            return quest;
        }
        quest.id = com.example.kadma.data.JsonUtils.str(raw, "id", "");
        quest.title = com.example.kadma.data.JsonUtils.str(raw, "title", quest.id);
        quest.description = com.example.kadma.data.JsonUtils.str(raw, "description", "");
        quest.type = com.example.kadma.data.JsonUtils.str(raw, "type", "side").toLowerCase(java.util.Locale.ROOT);
        quest.chapter = com.example.kadma.data.JsonUtils.str(raw, "chapter", "");
        quest.stage = com.example.kadma.data.JsonUtils.integer(raw, "stage", 0);
        quest.layer = com.example.kadma.data.JsonUtils.str(raw, "layer", "");
        quest.zone = com.example.kadma.data.JsonUtils.str(raw, "zone", "");
        quest.dailyLimit = Math.max(0, com.example.kadma.data.JsonUtils.integer(raw, "dailyLimit", 0));
        quest.requiresProzrenie = Math.max(0, com.example.kadma.data.JsonUtils.integer(raw, "requiresProzrenie", 0));
        quest.prerequisites = com.example.kadma.data.JsonUtils.stringList(raw, "prerequisites");
        quest.unlockQuests = com.example.kadma.data.JsonUtils.stringList(raw, "unlockQuests");
        quest.catchUp = com.example.kadma.data.JsonUtils.bool(raw, "catchUp", false);
        quest.aftermath = com.example.kadma.data.JsonUtils.bool(raw, "aftermath", false);
        quest.repeatable = com.example.kadma.data.JsonUtils.bool(raw, "repeatable", false);
        quest.autoStart = com.example.kadma.data.JsonUtils.bool(raw, "autoStart", false);
        quest.journalEntries = com.example.kadma.data.JsonUtils.stringList(raw, "journalEntries");
        quest.rumorsOnStart = com.example.kadma.data.JsonUtils.stringList(raw, "rumorsOnStart");
        quest.rumorsOnComplete = com.example.kadma.data.JsonUtils.stringList(raw, "rumorsOnComplete");
        quest.flagsOnComplete = com.example.kadma.data.JsonUtils.stringList(raw, "flagsOnComplete");
        quest.globalFlagsOnComplete = com.example.kadma.data.JsonUtils.stringList(raw, "globalFlagsOnComplete");
        quest.minStage = com.example.kadma.data.JsonUtils.integer(raw, "minStage", 0);
        quest.maxStage = com.example.kadma.data.JsonUtils.integer(raw, "maxStage", Integer.MAX_VALUE);
        quest.marker = com.example.kadma.data.JsonUtils.str(raw, "marker", "");
        JsonObject markerPoint = com.example.kadma.data.JsonUtils.object(raw, "markerPosition");
        if (markerPoint != null) {
            quest.markerX = com.example.kadma.data.JsonUtils.decimal(markerPoint, "x", 0);
            quest.markerY = com.example.kadma.data.JsonUtils.decimal(markerPoint, "y", 0);
            quest.markerZ = com.example.kadma.data.JsonUtils.decimal(markerPoint, "z", 0);
            quest.markerAbsolute = com.example.kadma.data.JsonUtils.bool(markerPoint, "absolute", false);
        }
        com.google.gson.JsonArray rawObjectives = com.example.kadma.data.JsonUtils.array(raw, "objectives");
        if (rawObjectives != null) {
            for (com.google.gson.JsonElement element : rawObjectives) {
                if (element.isJsonObject()) {
                    quest.objectives.add(ObjectiveDefinition.from(element.getAsJsonObject()));
                }
            }
        }
        quest.rewards = RewardDefinition.from(com.example.kadma.data.JsonUtils.object(raw, "rewards"));
        return normalise(quest);
    }

    private static QuestConfig normalise(QuestConfig quest) {
        if (quest.id.isEmpty()) {
            quest.problems.add("quest without id");
        }
        if (quest.title.isEmpty()) {
            quest.problems.add("quest " + quest.id + " without title");
        }
        List<String> known = java.util.Arrays.asList(
                "main", "side", "daily", "catch_up", "event", "layer", "aftermath", "prologue");
        if (!known.contains(quest.type)) {
            quest.problems.add("quest " + quest.id + " unknown type " + quest.type);
        }
        if (quest.objectives.isEmpty()) {
            quest.problems.add("quest " + quest.id + " without objectives");
        }
        int seen = 0;
        for (ObjectiveDefinition objective : quest.objectives) {
            if (objective.id.isEmpty()) {
                objective.id = "obj" + (++seen);
            }
            quest.problems.addAll(objective.validate());
        }
        return quest;
    }

    public boolean isMain() {
        return "main".equals(type) || "prologue".equals(type);
    }

    public boolean usable() {
        return id != null && !id.isEmpty() && !objectives.isEmpty();
    }

    public ObjectiveDefinition firstObjective() {
        return objectives.isEmpty() ? null : objectives.get(0);
    }
}
