package com.example.kadma.config;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Rumor definition. Rumors are signals, never guaranteed truth. */
public class RumorConfig {

    public String id = "";
    public String text = "";
    public String trigger = "always";
    public String stat = "";
    public int threshold = 0;
    public String questId = "";
    public String globalFlag = "";
    public String playerFlag = "";
    public String eventId = "";
    public String layer = "";
    public String reliability = "uncertain";
    public List<String> unlocks = new ArrayList<>();
    public List<String> contradicts = new ArrayList<>();
    public int cooldownMinutes = 60;
    public boolean allowRepeat = false;
    public String npc = "";
    public List<String> problems = new ArrayList<>();

    public static RumorConfig from(JsonObject raw) {
        RumorConfig rumor = new RumorConfig();
        if (raw == null) {
            rumor.problems.add("\u043f\u0443\u0441\u0442\u043e\u0439 \u0441\u043b\u0443\u0445");
            return rumor;
        }
        rumor.id = com.example.kadma.data.JsonUtils.str(raw, "id", "");
        rumor.text = com.example.kadma.data.JsonUtils.str(raw, "text", "");
        rumor.trigger = com.example.kadma.data.JsonUtils.str(raw, "trigger", "always").toLowerCase(java.util.Locale.ROOT);
        rumor.stat = com.example.kadma.data.JsonUtils.str(raw, "stat", "");
        rumor.threshold = com.example.kadma.data.JsonUtils.integer(raw, "threshold", 0);
        rumor.questId = com.example.kadma.data.JsonUtils.str(raw, "questId", "");
        rumor.globalFlag = com.example.kadma.data.JsonUtils.str(raw, "globalFlag", "");
        rumor.playerFlag = com.example.kadma.data.JsonUtils.str(raw, "playerFlag", "");
        rumor.eventId = com.example.kadma.data.JsonUtils.str(raw, "eventId", "");
        rumor.layer = com.example.kadma.data.JsonUtils.str(raw, "layer", "");
        rumor.reliability = com.example.kadma.data.JsonUtils.str(raw, "reliability", "uncertain");
        rumor.npc = com.example.kadma.data.JsonUtils.str(raw, "npc", "");
        rumor.cooldownMinutes = Math.max(0, com.example.kadma.data.JsonUtils.integer(raw, "cooldownMinutes", 60));
        rumor.allowRepeat = com.example.kadma.data.JsonUtils.bool(raw, "allowRepeat", false);
        rumor.unlocks = com.example.kadma.data.JsonUtils.stringList(raw, "unlocks");
        rumor.contradicts = com.example.kadma.data.JsonUtils.stringList(raw, "contradicts");
        if (rumor.id.isEmpty()) {
            rumor.problems.add("rumor without id");
        }
        if (rumor.text.isEmpty()) {
            rumor.problems.add("rumor " + rumor.id + " without text");
        }
        return rumor;
    }
}
