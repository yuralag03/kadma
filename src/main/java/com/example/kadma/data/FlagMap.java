package com.example.kadma.data;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Ordered, JSON friendly {@code String -> String} flag bag used for player and global flags. */
public class FlagMap {

    private final Map<String, String> values = new LinkedHashMap<>();

    public FlagMap() {
    }

    public FlagMap(Map<String, String> source) {
        if (source != null) {
            values.putAll(source);
        }
    }

    public boolean set(String key, String value) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        String sanitised = value == null ? "" : value;
        String previous = values.put(key, sanitised);
        return previous == null || !previous.equals(sanitised);
    }

    public boolean setTrue(String key) {
        return set(key, "true");
    }

    public boolean remove(String key) {
        return key != null && values.remove(key) != null;
    }

    public boolean has(String key) {
        return key != null && values.containsKey(key);
    }

    public String get(String key) {
        return values.get(key);
    }

    public String get(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    public boolean isTrue(String key) {
        return "true".equalsIgnoreCase(values.get(key));
    }

    public int getInt(String key, int fallback) {
        String raw = values.get(key);
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public Set<String> keys() {
        return values.keySet();
    }

    public Collection<Map.Entry<String, String>> entries() {
        return values.entrySet();
    }

    public Collection<String> values() {
        return values.values();
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public void clear() {
        values.clear();
    }

    public void putAll(Map<String, String> other) {
        if (other != null) {
            values.putAll(other);
        }
    }

    public Map<String, String> asMap() {
        return values;
    }
}
