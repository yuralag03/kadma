package com.example.kadma.config;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** One vertical layer of the city. */
public class LayerConfig {

    public String id = "";
    public String displayName = "";
    public String description = "";
    public double minY = -64.0D;
    public double maxY = 320.0D;
    public Double minX;
    public Double maxX;
    public Double minZ;
    public Double maxZ;

    public String ambientSound = "";
    public int ambientPeriodSeconds = 45;
    public int ambientVolume = 60;

    public String particleType = "";
    public int particleCount = 4;
    public double particleSpread = 0.4D;
    public double particleSpeed = 0.01D;
    public int particlePeriodSeconds = 6;

    public String accessRule = "open";
    public String accessQuest = "";
    public String accessMessage = "";

    public List<String> questTypes = new ArrayList<>();

    public LayerConfig() {
    }

    public static LayerConfig from(JsonObject raw) {
        LayerConfig layer = new LayerConfig();
        if (raw == null) {
            return layer;
        }
        layer.id = com.example.kadma.data.JsonUtils.str(raw, "id", "");
        layer.displayName = com.example.kadma.data.JsonUtils.str(raw, "displayName", layer.id);
        layer.description = com.example.kadma.data.JsonUtils.str(raw, "description", "");
        layer.minY = com.example.kadma.data.JsonUtils.decimal(raw, "minY", layer.minY);
        layer.maxY = com.example.kadma.data.JsonUtils.decimal(raw, "maxY", layer.maxY);
        layer.minX = optional(raw, "minX");
        layer.maxX = optional(raw, "maxX");
        layer.minZ = optional(raw, "minZ");
        layer.maxZ = optional(raw, "maxZ");
        layer.ambientSound = com.example.kadma.data.JsonUtils.str(raw, "ambientSound", "");
        layer.ambientPeriodSeconds = com.example.kadma.data.JsonUtils.integer(raw, "ambientPeriodSeconds", layer.ambientPeriodSeconds);
        layer.ambientVolume = com.example.kadma.data.JsonUtils.integer(raw, "ambientVolume", layer.ambientVolume);
        layer.particleType = com.example.kadma.data.JsonUtils.str(raw, "particleType", "");
        layer.particleCount = com.example.kadma.data.JsonUtils.integer(raw, "particleCount", layer.particleCount);
        layer.particleSpread = com.example.kadma.data.JsonUtils.decimal(raw, "particleSpread", layer.particleSpread);
        layer.particleSpeed = com.example.kadma.data.JsonUtils.decimal(raw, "particleSpeed", layer.particleSpeed);
        layer.particlePeriodSeconds = com.example.kadma.data.JsonUtils.integer(raw, "particlePeriodSeconds", layer.particlePeriodSeconds);
        layer.accessRule = com.example.kadma.data.JsonUtils.str(raw, "accessRule", layer.accessRule);
        layer.accessQuest = com.example.kadma.data.JsonUtils.str(raw, "accessQuest", layer.accessQuest);
        layer.accessMessage = com.example.kadma.data.JsonUtils.str(raw, "accessMessage", "");
        layer.questTypes = com.example.kadma.data.JsonUtils.stringList(raw, "questTypes");
        if (layer.minY > layer.maxY) {
            double swap = layer.minY;
            layer.minY = layer.maxY;
            layer.maxY = swap;
        }
        return layer;
    }

    private static Double optional(JsonObject raw, String key) {
        if (raw == null || !raw.has(key) || raw.get(key).isJsonNull()) {
            return null;
        }
        try {
            return raw.get(key).getAsDouble();
        } catch (Exception ex) {
            return null;
        }
    }

    public boolean hasBox() {
        return minX != null && maxX != null && minZ != null && maxZ != null;
    }

    public boolean containsY(double y) {
        return y >= minY && y <= maxY;
    }

    public boolean containsXZ(double x, double z) {
        if (!hasBox()) {
            return true;
        }
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public List<String> questTypesOrAll() {
        if (questTypes == null || questTypes.isEmpty()) {
            List<String> all = new ArrayList<>();
            for (String type : new String[]{"main", "side", "daily", "catch_up", "event", "layer"}) {
                all.add(type);
            }
            return all;
        }
        return questTypes;
    }
}
