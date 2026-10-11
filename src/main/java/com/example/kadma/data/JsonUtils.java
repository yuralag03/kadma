package com.example.kadma.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central JSON helpers used by the config manager and the data store.
 *
 * <p>Every parse is defensive: malformed files never throw towards the caller,
 * they simply return {@code null} so the owning system can fall back to safe
 * defaults. This keeps a broken content pack from crashing the server.</p>
 */
public final class JsonUtils {

    /** Pretty printing, plain (no HTML escaping of cyrillic). */
    public static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    public static final Gson COMPACT = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    private JsonUtils() {
    }

    /** Parse a file into a {@link JsonObject}; returns {@code null} on any failure. */
    public static JsonObject readObject(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element == null || !element.isJsonObject()) {
                return null;
            }
            return element.getAsJsonObject();
        } catch (Exception ex) {
            return null;
        }
    }

    /** Parse a raw JSON string into a {@link JsonObject}; returns {@code null} on any failure. */
    public static JsonObject parseObject(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            JsonElement element = JsonParser.parseString(raw);
            if (element == null || !element.isJsonObject()) {
                return null;
            }
            return element.getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException ex) {
            return null;
        }
    }

    public static Map<String, String> toStringMap(JsonObject object, String member) {
        Map<String, String> out = new HashMap<>();
        if (object == null || !object.has(member) || !object.get(member).isJsonObject()) {
            return out;
        }
        for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject(member).entrySet()) {
            out.put(entry.getKey(), entry.getValue().isJsonNull() ? "" : entry.getValue().getAsString());
        }
        return out;
    }

    public static Map<String, JsonObject> toObjectMap(JsonObject object, String member) {
        Map<String, JsonObject> out = new LinkedHashMap<>();
        if (object == null || !object.has(member) || !object.get(member).isJsonObject()) {
            return out;
        }
        for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject(member).entrySet()) {
            if (entry.getValue().isJsonObject()) {
                out.put(entry.getKey(), entry.getValue().getAsJsonObject());
            }
        }
        return out;
    }

    public static String str(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ex) {
            return fallback;
        }
    }

    public static int integer(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ex) {
            return fallback;
        }
    }

    public static double decimal(JsonObject object, String key, double fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsDouble();
        } catch (Exception ex) {
            return fallback;
        }
    }

    public static boolean bool(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsBoolean();
        } catch (Exception ex) {
            return fallback;
        }
    }

    /** Null-safe {@code obj.get(member).getAsJsonObject()}. */
    public static JsonObject object(JsonObject object, String member) {
        if (object == null || !object.has(member) || !object.get(member).isJsonObject()) {
            return null;
        }
        return object.getAsJsonObject(member);
    }

    /** Null-safe {@code obj.get(member).getAsJsonArray()}. */
    public static com.google.gson.JsonArray array(JsonObject object, String member) {
        if (object == null || !object.has(member) || !object.get(member).isJsonArray()) {
            return null;
        }
        return object.getAsJsonArray(member);
    }

    public static java.util.List<String> stringList(JsonObject object, String member) {
        java.util.List<String> out = new java.util.ArrayList<>();
        com.google.gson.JsonArray array = array(object, member);
        if (array == null) {
            return out;
        }
        for (JsonElement element : array) {
            if (element.isJsonNull()) {
                continue;
            }
            try {
                out.add(element.getAsString());
            } catch (Exception ex) {
                // skip malformed entry instead of dropping the whole list
            }
        }
        return out;
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    public static String toCompactJson(Object value) {
        return COMPACT.toJson(value);
    }

    public static <T> T fromJson(String raw, Class<T> type) {
        try {
            return GSON.fromJson(raw, type);
        } catch (JsonSyntaxException ex) {
            return null;
        }
    }

    public static void writePretty(Writer writer, Object value) {
        GSON.toJson(value, writer);
    }
}
