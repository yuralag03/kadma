package com.example.kadma.config;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reward bundle applied transactionally on quest completion. */
public class RewardDefinition {

    public List<ItemReward> items = new ArrayList<>();
    public int prozrenie = 0;
    public int experience = 0;
    public Map<String, Integer> stats = new LinkedHashMap<>();
    public List<FlagReward> playerFlags = new ArrayList<>();
    public List<FlagReward> globalFlags = new ArrayList<>();
    public List<String> journal = new ArrayList<>();
    public List<String> rumors = new ArrayList<>();
    public List<String> commands = new ArrayList<>();

    public static class ItemReward {
        public String itemId = "";
        public int amount = 1;

        public static ItemReward from(JsonObject raw) {
            ItemReward reward = new ItemReward();
            if (raw == null) {
                return reward;
            }
            reward.itemId = com.example.kadma.data.JsonUtils.str(raw, "itemId", com.example.kadma.data.JsonUtils.str(raw, "item", ""));
            reward.amount = Math.max(1, com.example.kadma.data.JsonUtils.integer(raw, "amount", 1));
            return reward;
        }
    }

    public static class FlagReward {
        public String key = "";
        public String value = "true";

        public static FlagReward from(JsonObject raw) {
            FlagReward reward = new FlagReward();
            if (raw == null) {
                return reward;
            }
            reward.key = com.example.kadma.data.JsonUtils.str(raw, "key", "");
            reward.value = com.example.kadma.data.JsonUtils.str(raw, "value", "true");
            return reward;
        }
    }

    public static RewardDefinition from(JsonObject raw) {
        RewardDefinition rewards = new RewardDefinition();
        if (raw == null) {
            return rewards;
        }
        com.google.gson.JsonArray rawItems = com.example.kadma.data.JsonUtils.array(raw, "items");
        if (rawItems != null) {
            for (com.google.gson.JsonElement element : rawItems) {
                if (element.isJsonObject()) {
                    rewards.items.add(ItemReward.from(element.getAsJsonObject()));
                }
            }
        }
        for (Map.Entry<String, JsonObject> entry : com.example.kadma.data.JsonUtils.toObjectMap(raw, "stats").entrySet()) {
            rewards.stats.put(entry.getKey(), com.example.kadma.data.JsonUtils.integer(entry.getValue(), "value", 0));
        }
        for (Map.Entry<String, JsonObject> entry : com.example.kadma.data.JsonUtils.toObjectMap(raw, "playerFlags").entrySet()) {
            rewards.playerFlags.add(FlagReward.from(entry.getValue()));
        }
        for (Map.Entry<String, JsonObject> entry : com.example.kadma.data.JsonUtils.toObjectMap(raw, "globalFlags").entrySet()) {
            rewards.globalFlags.add(FlagReward.from(entry.getValue()));
        }
        rewards.prozrenie = com.example.kadma.data.JsonUtils.integer(raw, "prozrenie", 0);
        rewards.experience = com.example.kadma.data.JsonUtils.integer(raw, "experience", 0);
        rewards.journal = com.example.kadma.data.JsonUtils.stringList(raw, "journal");
        rewards.rumors = com.example.kadma.data.JsonUtils.stringList(raw, "rumors");
        rewards.commands = com.example.kadma.data.JsonUtils.stringList(raw, "commands");
        return rewards;
    }

    public boolean empty() {
        return items.isEmpty() && prozrenie == 0 && experience == 0 && stats.isEmpty()
                && playerFlags.isEmpty() && globalFlags.isEmpty() && journal.isEmpty()
                && rumors.isEmpty() && commands.isEmpty();
    }
}
