package com.example.kadma.config;

import com.google.gson.JsonObject;

/** Runtime view over {@code settings.json}. */
public class ModSettings {

    public int dailyResetHour = 4;
    public int timezoneOffsetMinutes = 0;
    public int maxProzrenie = 3;
    public int prozrenieRegenPerDay = 1;
    public int mainQuestDailyLimit = 3;
    public int sideQuestDailyLimit = 5;
    public int adminPermissionLevel = 2;
    public int hudUpdateTicks = 30;
    public int autosaveSeconds = 300;
    public int markerTtlSeconds = 300;
    public int dialogueCooldownSeconds = 2;
    public int quotaCatchUpDays = 14;

    public LobbyPoint lobby = new LobbyPoint();
    public LobbyPoint citySpawn = new LobbyPoint();
    public String defaultLayer = "lower";

    public Features features = new Features();
    public boolean verboseLogging = false;

    public static class LobbyPoint {
        public String world = "minecraft:the_overworld";
        public double x = 0.5D;
        public double y = 80.0D;
        public double z = 0.5D;
        public double yaw = 0.0D;
        public double pitch = 0.0D;

        public boolean valid() {
            return world != null && !world.isEmpty();
        }

        static LobbyPoint from(JsonObject raw, LobbyPoint fallback) {
            LobbyPoint point = new LobbyPoint();
            point.world = com.example.kadma.data.JsonUtils.str(raw, "world", fallback.world);
            point.x = com.example.kadma.data.JsonUtils.decimal(raw, "x", fallback.x);
            point.y = com.example.kadma.data.JsonUtils.decimal(raw, "y", fallback.y);
            point.z = com.example.kadma.data.JsonUtils.decimal(raw, "z", fallback.z);
            point.yaw = com.example.kadma.data.JsonUtils.decimal(raw, "yaw", 0);
            point.pitch = com.example.kadma.data.JsonUtils.decimal(raw, "pitch", 0);
            return point;
        }
    }

    public static class Features {
        public boolean dialogues = true;
        public boolean rumors = true;
        public boolean dailyQuests = true;
        public boolean weeklyEvents = true;
        public boolean npcs = true;
        public boolean hud = true;
        public boolean layers = true;
        public boolean catchUp = true;
        public boolean lobbyBridge = true;
        public boolean visualEffects = true;

        static Features from(JsonObject raw) {
            Features features = new Features();
            if (raw == null) {
                return features;
            }
            features.dialogues = com.example.kadma.data.JsonUtils.bool(raw, "dialogues", features.dialogues);
            features.rumors = com.example.kadma.data.JsonUtils.bool(raw, "rumors", features.rumors);
            features.dailyQuests = com.example.kadma.data.JsonUtils.bool(raw, "dailyQuests", features.dailyQuests);
            features.weeklyEvents = com.example.kadma.data.JsonUtils.bool(raw, "weeklyEvents", features.weeklyEvents);
            features.npcs = com.example.kadma.data.JsonUtils.bool(raw, "npcs", features.npcs);
            features.hud = com.example.kadma.data.JsonUtils.bool(raw, "hud", features.hud);
            features.layers = com.example.kadma.data.JsonUtils.bool(raw, "layers", features.layers);
            features.catchUp = com.example.kadma.data.JsonUtils.bool(raw, "catchUp", features.catchUp);
            features.lobbyBridge = com.example.kadma.data.JsonUtils.bool(raw, "lobbyBridge", features.lobbyBridge);
            features.visualEffects = com.example.kadma.data.JsonUtils.bool(raw, "visualEffects", features.visualEffects);
            return features;
        }
    }

    /** Copy freshly parsed values into this live instance (used by reload). */
    public void assign(ModSettings other) {
        if (other == null) {
            return;
        }
        this.dailyResetHour = other.dailyResetHour;
        this.timezoneOffsetMinutes = other.timezoneOffsetMinutes;
        this.maxProzrenie = other.maxProzrenie;
        this.prozrenieRegenPerDay = other.prozrenieRegenPerDay;
        this.mainQuestDailyLimit = other.mainQuestDailyLimit;
        this.sideQuestDailyLimit = other.sideQuestDailyLimit;
        this.adminPermissionLevel = other.adminPermissionLevel;
        this.hudUpdateTicks = other.hudUpdateTicks;
        this.autosaveSeconds = other.autosaveSeconds;
        this.markerTtlSeconds = other.markerTtlSeconds;
        this.dialogueCooldownSeconds = other.dialogueCooldownSeconds;
        this.quotaCatchUpDays = other.quotaCatchUpDays;
        this.lobby = other.lobby;
        this.citySpawn = other.citySpawn;
        this.defaultLayer = other.defaultLayer;
        this.features = other.features;
        this.verboseLogging = other.verboseLogging;
    }

    /** Load with fallbacks. Never throws. */
    public static ModSettings from(JsonObject raw) {
        ModSettings settings = new ModSettings();
        if (raw == null) {
            return settings;
        }
        settings.dailyResetHour = clamp(raw, "dailyResetHour", settings.dailyResetHour, 0, 23);
        settings.timezoneOffsetMinutes = clamp(raw, "timezoneOffsetMinutes", settings.timezoneOffsetMinutes, -840, 840);
        settings.maxProzrenie = clamp(raw, "maxProzrenie", settings.maxProzrenie, 0, 20);
        settings.prozrenieRegenPerDay = clamp(raw, "prozrenieRegenPerDay", settings.prozrenieRegenPerDay, 0, 20);
        settings.mainQuestDailyLimit = clamp(raw, "mainQuestDailyLimit", settings.mainQuestDailyLimit, 0, 100);
        settings.sideQuestDailyLimit = clamp(raw, "sideQuestDailyLimit", settings.sideQuestDailyLimit, 0, 100);
        settings.adminPermissionLevel = clamp(raw, "adminPermissionLevel", settings.adminPermissionLevel, 0, 4);
        settings.hudUpdateTicks = clamp(raw, "hudUpdateTicks", settings.hudUpdateTicks, 10, 200);
        settings.autosaveSeconds = clamp(raw, "autosaveSeconds", settings.autosaveSeconds, 15, 86400);
        settings.markerTtlSeconds = clamp(raw, "markerTtlSeconds", settings.markerTtlSeconds, 10, 86400);
        settings.dialogueCooldownSeconds = clamp(raw, "dialogueCooldownSeconds", settings.dialogueCooldownSeconds, 0, 60);
        settings.quotaCatchUpDays = clamp(raw, "quotaCatchUpDays", settings.quotaCatchUpDays, 1, 365);
        settings.defaultLayer = com.example.kadma.data.JsonUtils.str(raw, "defaultLayer", settings.defaultLayer);
        settings.verboseLogging = com.example.kadma.data.JsonUtils.bool(raw, "verboseLogging", settings.verboseLogging);

        JsonObject lobby = com.example.kadma.data.JsonUtils.object(raw, "lobby");
        if (lobby != null) {
            settings.lobby = LobbyPoint.from(lobby, settings.lobby);
        }
        JsonObject citySpawn = com.example.kadma.data.JsonUtils.object(raw, "citySpawn");
        if (citySpawn != null) {
            settings.citySpawn = LobbyPoint.from(citySpawn, settings.citySpawn);
        }
        JsonObject features = com.example.kadma.data.JsonUtils.object(raw, "features");
        if (features != null) {
            settings.features = Features.from(features);
        }
        return settings;
    }

    private static int clamp(JsonObject raw, String key, int fallback, int min, int max) {
        int value = com.example.kadma.data.JsonUtils.integer(raw, key, fallback);
        return Math.max(min, Math.min(max, value));
    }
}
