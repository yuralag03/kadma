package com.example.kadma.config;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Logical story item. Maps a story id onto a vanilla base item so the mod never
 * needs custom models or textures.
 */
public class ItemConfig {

    public String id = "";
    public String baseItem = "minecraft:amethyst_shard";
    public String displayName = "";
    public List<String> lore = new ArrayList<>();
    public boolean glow = true;
    public boolean unbreakable = false;
    public int customModelData = 0;

    public static ItemConfig from(JsonObject raw) {
        ItemConfig item = new ItemConfig();
        if (raw == null) {
            return item;
        }
        item.id = com.example.kadma.data.JsonUtils.str(raw, "id", "");
        item.baseItem = com.example.kadma.data.JsonUtils.str(raw, "baseItem", item.baseItem);
        item.displayName = com.example.kadma.data.JsonUtils.str(raw, "displayName", item.id);
        item.lore = com.example.kadma.data.JsonUtils.stringList(raw, "lore");
        item.glow = com.example.kadma.data.JsonUtils.bool(raw, "glow", item.glow);
        item.unbreakable = com.example.kadma.data.JsonUtils.bool(raw, "unbreakable", item.unbreakable);
        item.customModelData = com.example.kadma.data.JsonUtils.integer(raw, "customModelData", 0);
        return item;
    }

    public String tagKey() {
        return id;
    }
}
