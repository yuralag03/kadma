package com.example.kadma.dialogue;

import com.example.kadma.config.ConfigManager;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Server side condition. Every field is evaluated against authoritative server state,
 * never against anything the client sends.
 */
public class DialogueCondition {

    public String type = "";
    public String key = "";
    public String value = "";
    public String itemId = "";
    public String questId = "";
    public String npcId = "";
    public String dialogueId = "";
    public String layer = "";
    public String zone = "";
    public String chapter = "";
    public String rumorId = "";
    public String stat = "";
    public String flagScope = "player";
    public int count = 1;
    public int amount = 0;
    public boolean negate = false;

    public static List<DialogueCondition> readList(JsonObject raw, String member) {
        List<DialogueCondition> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        com.google.gson.JsonArray array = com.example.kadma.data.JsonUtils.array(raw, member);
        if (array == null) {
            return out;
        }
        for (com.google.gson.JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            DialogueCondition condition = from(element.getAsJsonObject());
            if (!condition.type.isEmpty()) {
                out.add(condition);
            }
        }
        return out;
    }

    public static DialogueCondition from(JsonObject raw) {
        DialogueCondition condition = new DialogueCondition();
        if (raw == null) {
            return condition;
        }
        condition.type = com.example.kadma.data.JsonUtils.str(raw, "type", "").toLowerCase(java.util.Locale.ROOT);
        condition.key = com.example.kadma.data.JsonUtils.str(raw, "key", "");
        condition.value = com.example.kadma.data.JsonUtils.str(raw, "value", "");
        condition.itemId = com.example.kadma.data.JsonUtils.str(raw, "itemId", "");
        condition.questId = com.example.kadma.data.JsonUtils.str(raw, "questId", "");
        condition.npcId = com.example.kadma.data.JsonUtils.str(raw, "npcId", "");
        condition.dialogueId = com.example.kadma.data.JsonUtils.str(raw, "dialogueId", "");
        condition.layer = com.example.kadma.data.JsonUtils.str(raw, "layer", "");
        condition.zone = com.example.kadma.data.JsonUtils.str(raw, "zone", "");
        condition.chapter = com.example.kadma.data.JsonUtils.str(raw, "chapter", "");
        condition.rumorId = com.example.kadma.data.JsonUtils.str(raw, "rumorId", "");
        condition.stat = com.example.kadma.data.JsonUtils.str(raw, "stat", "");
        condition.flagScope = com.example.kadma.data.JsonUtils.str(raw, "scope", "player").toLowerCase(java.util.Locale.ROOT);
        condition.count = com.example.kadma.data.JsonUtils.integer(raw, "count", 1);
        condition.amount = com.example.kadma.data.JsonUtils.integer(raw, "amount", 0);
        condition.negate = com.example.kadma.data.JsonUtils.bool(raw, "negate", false);
        if (condition.key.isEmpty()) {
            condition.key = condition.itemId.isEmpty() ? condition.questId : condition.itemId;
        }
        return condition;
    }

    public boolean matches(net.minecraft.server.level.ServerPlayer player, com.example.kadma.KadmaServices services) {
        boolean result;
        switch (type) {
            case "player_flag":
                result = playerFlag(player, services);
                break;
            case "global_flag":
                result = globalFlag(services);
                break;
            case "has_item":
                result = hasItem(player, services);
                break;
            case "quest_completed":
                result = services.data().player(player.getUUID()).isComplete(questId);
                break;
            case "quest_active":
                result = services.data().player(player.getUUID()).hasActive(questId);
                break;
            case "quest_not_completed":
                result = !services.data().player(player.getUUID()).isComplete(questId);
                break;
            case "layer":
                result = services.layers().currentLayerId(player).equals(layer);
                break;
            case "zone":
                result = services.zones().isInside(player, zone);
                break;
            case "chapter":
                result = services.data().player(player.getUUID()).chapter.equals(chapter);
                break;
            case "stage_min":
                result = services.data().player(player.getUUID()).stage >= amount;
                break;
            case "stage_max":
                result = services.data().player(player.getUUID()).stage <= amount;
                break;
            case "prozrenie_min":
                result = services.data().player(player.getUUID()).prozrenie >= amount;
                break;
            case "stat_min":
                result = services.data().global().stat(stat) >= amount;
                break;
            case "stat_max":
                result = services.data().global().stat(stat) <= amount;
                break;
            case "rumor_known":
                result = services.data().player(player.getUUID()).knownRumor(rumorId);
                break;
            case "rumor_unknown":
                result = !services.data().player(player.getUUID()).knownRumor(rumorId);
                break;
            case "npc_met":
                result = services.data().player(player.getUUID()).flag("npc.met." + (npcId.isEmpty() ? key : npcId));
                break;
            case "dialogue_unlocked":
                result = services.data().player(player.getUUID()).unlockedDialogues.contains(dialogueId);
                break;
            case "spark":
                result = services.data().player(player.getUUID()).sparkTitle;
                break;
            case "entered_city":
                result = services.data().player(player.getUUID()).enteredCity;
                break;
            case "always":
                result = true;
                break;
            default:
                result = true;
                break;
        }
        return negate != result;
    }

    private boolean playerFlag(net.minecraft.server.level.ServerPlayer player, com.example.kadma.KadmaServices services) {
        String target = key.isEmpty() ? itemId : key;
        com.example.kadma.data.PlayerData data = services.data().player(player.getUUID());
        if (value.isEmpty()) {
            return data.flag(target);
        }
        return value.equals(data.flagValue(target));
    }

    private boolean globalFlag(com.example.kadma.KadmaServices services) {
        if (key.isEmpty()) {
            return false;
        }
        com.example.kadma.data.GlobalData global = services.data().global();
        if (global.flags.containsKey(key)) {
            return value.isEmpty() || value.equals(global.flags.get(key));
        }
        return value.isEmpty() && global.customFlags.containsKey(key);
    }

    private boolean hasItem(net.minecraft.server.level.ServerPlayer player, com.example.kadma.KadmaServices services) {
        String target = itemId.isEmpty() ? key : itemId;
        com.example.kadma.config.ItemConfig item = services.config().items().get(target);
        if (item == null) {
            return false;
        }
        return com.example.kadma.util.InventoryUtil.countStoryItem(player, target) >= Math.max(1, count);
    }
}
