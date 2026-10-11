package com.example.kadma.quest;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Every objective type the engine understands. Unknown types are rejected at load. */
public enum ObjectiveType {

    GOTO("goto"),
    TALK("talk"),
    COLLECT("collect"),
    KILL("kill"),
    INTERACT("interact"),
    CHOOSE("choose"),
    DELIVER("deliver"),
    SURVIVE("survive"),
    TIMER("timer"),
    USE_ITEM("use_item"),
    ENTER_LAYER("enter_layer"),
    REACH_FLAG("reach_flag");

    public final String id;

    ObjectiveType(String id) {
        this.id = id;
    }

    public static final List<String> IDS = Arrays.stream(values()).map(type -> type.id).toList();

    public static boolean isKnown(String raw) {
        if (raw == null) {
            return false;
        }
        String needle = raw.toLowerCase(Locale.ROOT);
        for (ObjectiveType type : values()) {
            if (type.id.equals(needle)) {
                return true;
            }
        }
        return false;
    }

    public static ObjectiveType parse(String raw, ObjectiveType fallback) {
        if (raw == null) {
            return fallback;
        }
        for (ObjectiveType type : values()) {
            if (type.id.equalsIgnoreCase(raw)) {
                return type;
            }
        }
        return fallback;
    }
}
