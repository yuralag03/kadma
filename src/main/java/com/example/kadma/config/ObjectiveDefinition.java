package com.example.kadma.config;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** One objective inside a quest definition. */
public class ObjectiveDefinition {

    public String id = "";
    public String type = "goto";
    public String description = "";
    public boolean hidden = false;
    public boolean optional = false;

    public String zone = "";
    public String npc = "";
    public String npcId = "";
    public String itemId = "";
    public int amount = 1;
    public boolean consume = false;
    public String entityType = "";
    public String blockId = "";
    public String flagKey = "";
    public String flagValue = "";
    public String dialogueId = "";
    public String nodeId = "";
    public int choiceIndex = -1;
    public String layer = "";
    public int ticks = 100;
    public boolean persistProgress = true;

    public static ObjectiveDefinition from(JsonObject raw) {
        ObjectiveDefinition objective = new ObjectiveDefinition();
        if (raw == null) {
            return objective;
        }
        objective.id = com.example.kadma.data.JsonUtils.str(raw, "id", "");
        objective.type = com.example.kadma.data.JsonUtils.str(raw, "type", "goto").toLowerCase(java.util.Locale.ROOT);
        objective.description = com.example.kadma.data.JsonUtils.str(raw, "description", "");
        objective.hidden = com.example.kadma.data.JsonUtils.bool(raw, "hidden", false);
        objective.optional = com.example.kadma.data.JsonUtils.bool(raw, "optional", false);
        objective.zone = com.example.kadma.data.JsonUtils.str(raw, "zone", "");
        objective.npc = com.example.kadma.data.JsonUtils.str(raw, "npc", "");
        objective.npcId = com.example.kadma.data.JsonUtils.str(raw, "npcId", objective.npc);
        objective.itemId = com.example.kadma.data.JsonUtils.str(raw, "itemId", "");
        objective.amount = Math.max(1, com.example.kadma.data.JsonUtils.integer(raw, "amount", 1));
        objective.consume = com.example.kadma.data.JsonUtils.bool(raw, "consume", false);
        objective.entityType = com.example.kadma.data.JsonUtils.str(raw, "entityType", "");
        objective.blockId = com.example.kadma.data.JsonUtils.str(raw, "blockId", "");
        objective.flagKey = com.example.kadma.data.JsonUtils.str(raw, "flagKey", "");
        objective.flagValue = com.example.kadma.data.JsonUtils.str(raw, "flagValue", "true");
        objective.dialogueId = com.example.kadma.data.JsonUtils.str(raw, "dialogueId", "");
        objective.nodeId = com.example.kadma.data.JsonUtils.str(raw, "nodeId", "");
        objective.choiceIndex = com.example.kadma.data.JsonUtils.integer(raw, "choiceIndex", -1);
        objective.layer = com.example.kadma.data.JsonUtils.str(raw, "layer", "");
        objective.ticks = Math.max(1, com.example.kadma.data.JsonUtils.integer(raw, "ticks", 100));
        objective.persistProgress = com.example.kadma.data.JsonUtils.bool(raw, "persistProgress", true);
        return objective;
    }

    /** Progress text that is derived from live server state, never from the client. */
    public String progressLabel(int progress) {
        switch (type) {
            case "collect":
            case "kill":
            case "deliver":
                return "\u0441\u0434\u0435\u043b\u0430\u043d\u043e " + Math.min(progress, amount) + "/" + amount;
            case "timer":
            case "survive":
                return "\u043e\u0441\u0442\u0430\u043b\u043e\u0441\u044c " + Math.max(0, ticks - progress) / 20 + "\u0441";
            case "enter_layer":
            case "goto":
            case "talk":
            case "interact":
            case "use_item":
            case "choose":
            case "reach_flag":
            default:
                return progress > 0 ? "\u0433\u043e\u0442\u043e\u0432\u043e" : "\u043d\u0435 \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u043e";
        }
    }

    public List<String> validate() {
        List<String> problems = new ArrayList<>();
        if (id.isEmpty()) {
            problems.add("objective without id");
        }
        if (description.isEmpty()) {
            problems.add("objective " + id + " without description");
        }
        return problems;
    }
}
