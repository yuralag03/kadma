package com.example.kadma.hud;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compact HUD payload built on the server and serialised to JSON.
 *
 * <p>The client never derives values itself: it renders whatever the last payload
 * said. A poisoned payload can therefore only change text, never gameplay.</p>
 */
public class HudPayload {

    public String layer = "";
    public String chapter = "";
    public int stage = 0;
    public String quest = "";
    public String objective = "";
    public String progress = "";
    public int prozrenie = 0;
    public int maxProzrenie = 0;
    public int faith = 0;
    public int corruption = 0;
    public int schism = 0;
    public int spark = 0;
    public String event = "";
    public int rumors = 0;
    public boolean visible = true;

    public HudPayload() {
    }

    public static HudPayload fromJson(String raw) {
        com.google.gson.JsonObject object = com.example.kadma.data.JsonUtils.parseObject(raw);
        return from(object);
    }

    public static HudPayload from(com.google.gson.JsonObject object) {
        HudPayload payload = new HudPayload();
        if (object == null) {
            return payload;
        }
        payload.layer = com.example.kadma.data.JsonUtils.str(object, "layer", payload.layer);
        payload.chapter = com.example.kadma.data.JsonUtils.str(object, "chapter", payload.chapter);
        payload.stage = com.example.kadma.data.JsonUtils.integer(object, "stage", 0);
        payload.quest = com.example.kadma.data.JsonUtils.str(object, "quest", "");
        payload.objective = com.example.kadma.data.JsonUtils.str(object, "objective", "");
        payload.progress = com.example.kadma.data.JsonUtils.str(object, "progress", "");
        payload.prozrenie = com.example.kadma.data.JsonUtils.integer(object, "prozrenie", 0);
        payload.maxProzrenie = com.example.kadma.data.JsonUtils.integer(object, "maxProzrenie", 0);
        payload.faith = com.example.kadma.data.JsonUtils.integer(object, "faith", 0);
        payload.corruption = com.example.kadma.data.JsonUtils.integer(object, "corruption", 0);
        payload.schism = com.example.kadma.data.JsonUtils.integer(object, "schism", 0);
        payload.spark = com.example.kadma.data.JsonUtils.integer(object, "spark", 0);
        payload.event = com.example.kadma.data.JsonUtils.str(object, "event", "");
        payload.rumors = com.example.kadma.data.JsonUtils.integer(object, "rumors", 0);
        payload.visible = com.example.kadma.data.JsonUtils.bool(object, "visible", true);
        return payload;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("layer", layer);
        out.put("chapter", chapter);
        out.put("stage", stage);
        out.put("quest", quest);
        out.put("objective", objective);
        out.put("progress", progress);
        out.put("prozrenie", prozrenie);
        out.put("maxProzrenie", maxProzrenie);
        out.put("faith", faith);
        out.put("corruption", corruption);
        out.put("schism", schism);
        out.put("spark", spark);
        out.put("event", event);
        out.put("rumors", rumors);
        out.put("visible", visible);
        return out;
    }

    public String toJson() {
        return com.example.kadma.data.JsonUtils.toCompactJson(toMap());
    }
}
