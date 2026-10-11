package com.example.kadma.config;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** ArmorStand based NPC definition. */
public class NpcConfig {

    public String id = "";
    public String displayName = "";
    public String title = "";
    public String layer = "";
    public String zone = "";
    public String dialogueRoot = "";
    public String dialogueId = "";

    public boolean invisible = false;
    public boolean marker = false;
    public boolean small = false;
    public String headItem = "";

    public Point spawn = new Point();
    public List<String> equipment = new ArrayList<>();

    public static class Point {
        public String world = "";
        public double x;
        public double y = 80.0D;
        public double z;
        public double yaw = 0.0D;

        public boolean hasWorld() {
            return world != null && !world.isEmpty();
        }

        static Point from(JsonObject raw) {
            Point point = new Point();
            point.world = com.example.kadma.data.JsonUtils.str(raw, "world", "");
            point.x = com.example.kadma.data.JsonUtils.decimal(raw, "x", 0);
            point.y = com.example.kadma.data.JsonUtils.decimal(raw, "y", 80);
            point.z = com.example.kadma.data.JsonUtils.decimal(raw, "z", 0);
            point.yaw = com.example.kadma.data.JsonUtils.decimal(raw, "yaw", 0);
            return point;
        }
    }

    public static NpcConfig from(JsonObject raw) {
        NpcConfig npc = new NpcConfig();
        if (raw == null) {
            return npc;
        }
        npc.id = com.example.kadma.data.JsonUtils.str(raw, "id", "");
        npc.displayName = com.example.kadma.data.JsonUtils.str(raw, "displayName", npc.id);
        npc.title = com.example.kadma.data.JsonUtils.str(raw, "title", "");
        npc.layer = com.example.kadma.data.JsonUtils.str(raw, "layer", "");
        npc.zone = com.example.kadma.data.JsonUtils.str(raw, "zone", "");
        npc.dialogueRoot = com.example.kadma.data.JsonUtils.str(raw, "dialogueRoot", "");
        npc.dialogueId = com.example.kadma.data.JsonUtils.str(raw, "dialogueId", npc.dialogueRoot);
        npc.invisible = com.example.kadma.data.JsonUtils.bool(raw, "invisible", npc.invisible);
        npc.marker = com.example.kadma.data.JsonUtils.bool(raw, "marker", npc.marker);
        npc.small = com.example.kadma.data.JsonUtils.bool(raw, "small", npc.small);
        npc.headItem = com.example.kadma.data.JsonUtils.str(raw, "headItem", "");
        npc.equipment = com.example.kadma.data.JsonUtils.stringList(raw, "equipment");
        JsonObject spawn = com.example.kadma.data.JsonUtils.object(raw, "spawn");
        if (spawn != null) {
            npc.spawn = Point.from(spawn);
        }
        return npc;
    }

    public String fullTitle() {
        if (title == null || title.isEmpty()) {
            return displayName;
        }
        return displayName + " \u00b7 " + title;
    }
}
