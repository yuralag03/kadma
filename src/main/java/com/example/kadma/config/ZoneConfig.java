package com.example.kadma.config;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Named cubic area used by objectives and events. */
public class ZoneConfig {

    public String id = "";
    public String displayName = "";
    public String layer = "";
    public double minX;
    public double minY;
    public double minZ;
    public double maxX;
    public double maxY;
    public double maxZ;
    public boolean safe = false;
    public List<String> allowedQuestTypes = new ArrayList<>();
    public String description = "";

    public static ZoneConfig from(JsonObject raw) {
        ZoneConfig zone = new ZoneConfig();
        if (raw == null) {
            return zone;
        }
        zone.id = com.example.kadma.data.JsonUtils.str(raw, "id", "");
        zone.displayName = com.example.kadma.data.JsonUtils.str(raw, "displayName", zone.id);
        zone.layer = com.example.kadma.data.JsonUtils.str(raw, "layer", "");
        zone.description = com.example.kadma.data.JsonUtils.str(raw, "description", "");
        zone.safe = com.example.kadma.data.JsonUtils.bool(raw, "safe", zone.safe);
        zone.minX = com.example.kadma.data.JsonUtils.decimal(raw, "minX", 0);
        zone.minY = com.example.kadma.data.JsonUtils.decimal(raw, "minY", -64);
        zone.minZ = com.example.kadma.data.JsonUtils.decimal(raw, "minZ", 0);
        zone.maxX = com.example.kadma.data.JsonUtils.decimal(raw, "maxX", zone.minX);
        zone.maxY = com.example.kadma.data.JsonUtils.decimal(raw, "maxY", zone.minY);
        zone.maxZ = com.example.kadma.data.JsonUtils.decimal(raw, "maxZ", zone.minZ);
        zone.allowedQuestTypes = com.example.kadma.data.JsonUtils.stringList(raw, "allowedQuestTypes");
        return normalise(zone);
    }

    private static ZoneConfig normalise(ZoneConfig zone) {
        double x0 = Math.min(zone.minX, zone.maxX);
        double x1 = Math.max(zone.minX, zone.maxX);
        double y0 = Math.min(zone.minY, zone.maxY);
        double y1 = Math.max(zone.minY, zone.maxY);
        double z0 = Math.min(zone.minZ, zone.maxZ);
        double z1 = Math.max(zone.minZ, zone.maxZ);
        zone.minX = x0;
        zone.maxX = x1;
        zone.minY = y0;
        zone.maxY = y1;
        zone.minZ = z0;
        zone.maxZ = z1;
        return zone;
    }
}
