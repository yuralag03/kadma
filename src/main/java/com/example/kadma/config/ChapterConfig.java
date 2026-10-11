package com.example.kadma.config;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Chapter definition with its window, thresholds, entry points and exit conditions. */
public class ChapterConfig {

    public String id = "";
    public String title = "";
    public String description = "";
    public int order = 0;
    public int openDays = 7;
    public int requiredGlobalPoints = 0;
    public int requiredPlayerCount = 1;
    public List<String> entryQuests = new ArrayList<>();
    public List<String> lateEntryQuests = new ArrayList<>();
    public List<String> aftermathQuests = new ArrayList<>();
    public List<String> exitConditions = new ArrayList<>();
    public List<String> exitQuests = new ArrayList<>();
    public String nextChapter = "";
    public String layer = "";
    public MapHolder globalEffects = new MapHolder();
    public List<String> journalOnOpen = new ArrayList<>();
    public List<String> rumorsOnOpen = new ArrayList<>();
    public boolean extendWhenBehind = true;
    public List<String> problems = new ArrayList<>();

    public static class MapHolder {
        public java.util.Map<String, Integer> stats = new java.util.LinkedHashMap<>();
        public List<String> flags = new ArrayList<>();
    }

    public static ChapterConfig from(JsonObject raw) {
        ChapterConfig chapter = new ChapterConfig();
        if (raw == null) {
            chapter.problems.add("\u043f\u0443\u0441\u0442\u0430\u044f \u0433\u043b\u0430\u0432\u0430");
            return chapter;
        }
        chapter.id = com.example.kadma.data.JsonUtils.str(raw, "id", "");
        chapter.title = com.example.kadma.data.JsonUtils.str(raw, "title", chapter.id);
        chapter.description = com.example.kadma.data.JsonUtils.str(raw, "description", "");
        chapter.order = com.example.kadma.data.JsonUtils.integer(raw, "order", 0);
        chapter.openDays = Math.max(1, com.example.kadma.data.JsonUtils.integer(raw, "openDays", 7));
        chapter.requiredGlobalPoints = com.example.kadma.data.JsonUtils.integer(raw, "requiredGlobalPoints", 0);
        chapter.requiredPlayerCount = com.example.kadma.data.JsonUtils.integer(raw, "requiredPlayerCount", 1);
        chapter.entryQuests = com.example.kadma.data.JsonUtils.stringList(raw, "entryQuests");
        chapter.lateEntryQuests = com.example.kadma.data.JsonUtils.stringList(raw, "lateEntryQuests");
        chapter.aftermathQuests = com.example.kadma.data.JsonUtils.stringList(raw, "aftermathQuests");
        chapter.exitQuests = com.example.kadma.data.JsonUtils.stringList(raw, "exitQuests");
        chapter.exitConditions = com.example.kadma.data.JsonUtils.stringList(raw, "exitConditions");
        chapter.nextChapter = com.example.kadma.data.JsonUtils.str(raw, "nextChapter", "");
        chapter.layer = com.example.kadma.data.JsonUtils.str(raw, "layer", "");
        chapter.extendWhenBehind = com.example.kadma.data.JsonUtils.bool(raw, "extendWhenBehind", true);
        chapter.journalOnOpen = com.example.kadma.data.JsonUtils.stringList(raw, "journalOnOpen");
        chapter.rumorsOnOpen = com.example.kadma.data.JsonUtils.stringList(raw, "rumorsOnOpen");

        JsonObject effects = com.example.kadma.data.JsonUtils.object(raw, "globalEffects");
        if (effects != null) {
            JsonObject stats = com.example.kadma.data.JsonUtils.object(effects, "stats");
            if (stats != null) {
                for (MapEntry entry : entries(stats)) {
                    chapter.globalEffects.stats.put(entry.key, entry.value);
                }
            }
            chapter.globalEffects.flags = com.example.kadma.data.JsonUtils.stringList(effects, "flags");
        }
        if (chapter.id.isEmpty()) {
            chapter.problems.add("chapter without id");
        }
        if (chapter.entryQuests.isEmpty() && chapter.lateEntryQuests.isEmpty()) {
            chapter.problems.add("chapter " + chapter.id + " without entry quests");
        }
        return chapter;
    }

    private static List<MapEntry> entries(JsonObject object) {
        List<MapEntry> out = new ArrayList<>();
        for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : object.entrySet()) {
            int value = 0;
            try {
                value = entry.getValue().getAsInt();
            } catch (Exception ex) {
                value = 0;
            }
            out.add(new MapEntry(entry.getKey(), value));
        }
        return out;
    }

    private static final class MapEntry {
        final String key;
        final int value;

        MapEntry(String key, int value) {
            this.key = key;
            this.value = value;
        }
    }
}
