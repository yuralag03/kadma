package com.example.kadma.data;

import com.example.kadma.config.ConfigManager;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative persistence for player and world state.
 *
 * <p>Every write goes through {@link AtomicFileWriter}, so a crash mid-save never
 * destroys a file. Writes are debounced and batched on autosave ticks.</p>
 */
public class DataStore {

    public static final Logger LOG = LogUtils.getLogger();

    private final ConfigManager config;
    private final Map<UUID, PlayerData> players = new ConcurrentHashMap<>();
    private final List<UUID> loadedOrder = new ArrayList<>();
    private GlobalData global = new GlobalData();

    private final Map<UUID, Boolean> dirtyPlayers = new ConcurrentHashMap<>();
    private volatile boolean dirtyGlobal = true;

    private int savedPlayers = 0;
    private long lastSaveMillis = 0L;

    public DataStore(ConfigManager config) {
        this.config = config;
    }

    // ------------------------------------------------------------------ player

    /** Get (or lazily create) the authoritative record for a player. */
    public PlayerData player(UUID id) {
        PlayerData data = players.get(id);
        if (data != null) {
            return data;
        }
        data = loadPlayer(id);
        players.put(id, data);
        loadedOrder.add(id);
        return data;
    }

    public PlayerData peek(UUID id) {
        return players.get(id);
    }

    public PlayerData loadPlayer(UUID id) {
        Path file = config.playersDir().resolve(id.toString() + ".json");
        PlayerData data = null;
        if (Files.isRegularFile(file)) {
            String raw = null;
            try {
                raw = AtomicFileWriter.readAll(file);
            } catch (IOException ex) {
                LOG.warn("[kadma] cannot read player file {}", file, ex);
            }
            if (raw != null && !raw.isEmpty()) {
                data = com.example.kadma.data.JsonUtils.fromJson(raw, PlayerData.class);
                if (data == null) {
                    LOG.warn("[kadma] player file {} is corrupt, starting fresh", file);
                    quarantine(file, raw);
                }
            }
        }
        if (data == null) {
            data = new PlayerData(id.toString(), 0L);
        }
        if (data.uuid == null || data.uuid.isEmpty()) {
            data.uuid = id.toString();
        }
        sanitise(data);
        return data;
    }

    private void sanitise(PlayerData data) {
        if (data.flags == null) {
            data.flags = new java.util.LinkedHashMap<>();
        }
        if (data.completedQuests == null) {
            data.completedQuests = new java.util.LinkedHashSet<>();
        }
        if (data.activeQuests == null) {
            data.activeQuests = new ArrayList<>();
        }
        if (data.journal == null) {
            data.journal = new ArrayList<>();
        }
        if (data.knownRumors == null) {
            data.knownRumors = new java.util.LinkedHashSet<>();
        }
        if (data.unreadRumors == null) {
            data.unreadRumors = new java.util.LinkedHashSet<>();
        }
        if (data.unlockedDialogues == null) {
            data.unlockedDialogues = new java.util.LinkedHashSet<>();
        }
        if (data.counters == null) {
            data.counters = new java.util.LinkedHashMap<>();
        }
        if (data.cooldowns == null) {
            data.cooldowns = new java.util.LinkedHashMap<>();
        }
        if (data.history == null) {
            data.history = new ArrayList<>();
        }
        if (data.chapter == null || data.chapter.isEmpty()) {
            data.chapter = "act0_threshold";
        }
        data.unreadRumors.removeIf(rumor -> !data.knownRumors.contains(rumor));
    }

    private void quarantine(Path file, String raw) {
        try {
            SettingsBackup.write(file.resolveSibling(file.getFileName() + ".broken"), raw);
        } catch (Exception ex) {
            LOG.warn("[kadma] cannot quarantine {}", file, ex);
        }
    }

    public void unload(UUID id) {
        PlayerData data = players.remove(id);
        loadedOrder.remove(id);
        dirtyPlayers.remove(id);
        if (data != null) {
            // save before dropping the reference so a logout never loses story data
            Path file = config.playersDir().resolve(id.toString() + ".json");
            try {
                AtomicFileWriter.write(file, com.example.kadma.data.JsonUtils.toJson(data));
                savedPlayers++;
            } catch (IOException ex) {
                LOG.warn("[kadma] cannot save player {} on logout", id, ex);
            }
        }
    }

    public void markPlayer(UUID id) {
        if (players.containsKey(id)) {
            dirtyPlayers.put(id, Boolean.TRUE);
        }
    }

    public void touch() {
        dirtyGlobal = true;
    }

    // ------------------------------------------------------------------- global

    public GlobalData global() {
        return global;
    }

    public void loadGlobal() {
        Path file = config.globalFile();
        if (Files.isRegularFile(file)) {
            try {
                String raw = AtomicFileWriter.readAll(file);
                GlobalData parsed = com.example.kadma.data.JsonUtils.fromJson(raw, GlobalData.class);
                if (parsed != null) {
                    global = parsed;
                    sanitiseGlobal();
                } else {
                    LOG.warn("[kadma] global.json is corrupt, rebuilding from defaults");
                    quarantine(file, raw);
                }
            } catch (IOException ex) {
                LOG.warn("[kadma] cannot read global.json", ex);
            }
        }
        applyDefaults();
        dirtyGlobal = true;
    }

    private void sanitiseGlobal() {
        GlobalData data = global;
        if (data.stats == null) {
            data.stats = new java.util.LinkedHashMap<>();
        }
        if (data.flags == null) {
            data.flags = new java.util.LinkedHashMap<>();
        }
        if (data.customFlags == null) {
            data.customFlags = new java.util.LinkedHashMap<>();
        }
        if (data.participants == null) {
            data.participants = new java.util.LinkedHashSet<>();
        }
        if (data.eventPhase == null) {
            data.eventPhase = new java.util.LinkedHashMap<>();
        }
        if (data.chapterHistory == null) {
            data.chapterHistory = new java.util.LinkedHashMap<>();
        }
        if (data.firedRumors == null) {
            data.firedRumors = new java.util.LinkedHashSet<>();
        }
        if (data.worldJournal == null) {
            data.worldJournal = new ArrayList<>();
        }
        if (data.unlockedChapter == null || data.unlockedChapter.isEmpty()) {
            data.unlockedChapter = "act0_threshold";
        }
    }

    /** Merge the configured initial world state into whatever was loaded. */
    public void applyDefaults() {
        JsonObject defaults = config.worldDefaults();
        if (defaults == null) {
            return;
        }
        for (String key : new String[]{"faith", "corruption", "schism", "spark", "portal_shards_total",
                "global_points", "players_total"}) {
            if (defaults.has(key) && !global.stats.containsKey(key)) {
                try {
                    global.stats.put(key, defaults.get(key).getAsInt());
                } catch (Exception ex) {
                    // ignore malformed default
                }
            }
        }
        if (defaults.has("unlocked_chapter") && (global.unlockedChapter == null || global.unlockedChapter.isEmpty())) {
            try {
                global.unlockedChapter = defaults.get("unlocked_chapter").getAsString();
            } catch (Exception ex) {
                // ignore
            }
        }
        if (defaults.has("flags") && defaults.get("flags").isJsonObject()) {
            for (Map.Entry<String, com.google.gson.JsonElement> entry : defaults.getAsJsonObject("flags").entrySet()) {
                global.flags.putIfAbsent(entry.getKey(), entry.getValue().getAsString());
            }
        }
        if (global.stats.containsKey("global_points")) {
            global.stats.putIfAbsent("global_points", global.stats.getOrDefault("global_points", 0));
        }
    }

    // ---------------------------------------------------------------------- io

    public void savePlayer(UUID id) {
        PlayerData data = players.get(id);
        if (data == null) {
            dirtyPlayers.remove(id);
            return;
        }
        Path file = config.playersDir().resolve(id.toString() + ".json");
        try {
            AtomicFileWriter.write(file, com.example.kadma.data.JsonUtils.toJson(data));
            dirtyPlayers.remove(id);
            savedPlayers++;
        } catch (IOException ex) {
            LOG.warn("[kadma] cannot save player {}", id, ex);
        }
    }

    public void saveGlobal() {
        Path file = config.globalFile();
        try {
            AtomicFileWriter.write(file, com.example.kadma.data.JsonUtils.toJson(global));
            dirtyGlobal = false;
        } catch (IOException ex) {
            LOG.warn("[kadma] cannot save global.json", ex);
        }
    }

    public void saveState(String json) {
        try {
            AtomicFileWriter.write(config.stateFile(), json);
        } catch (IOException ex) {
            LOG.warn("[kadma] cannot save state.json", ex);
        }
    }

    /** Flush everything flagged dirty. Called on autosave ticks and at shutdown. */
    public int saveAll() {
        int count = 0;
        for (UUID id : new ArrayList<>(dirtyPlayers.keySet())) {
            savePlayer(id);
            count++;
        }
        if (dirtyGlobal) {
            saveGlobal();
            count++;
        }
        lastSaveMillis = System.currentTimeMillis();
        return count;
    }

    public void flushAll() {
        for (UUID id : new ArrayList<>(players.keySet())) {
            savePlayer(id);
        }
        saveGlobal();
        lastSaveMillis = System.currentTimeMillis();
    }

    public boolean isDirty() {
        return dirtyGlobal || !dirtyPlayers.isEmpty();
    }

    public int cachedPlayers() {
        return players.size();
    }

    public int savedPlayers() {
        return savedPlayers;
    }

    public long lastSaveMillis() {
        return lastSaveMillis;
    }

    public List<UUID> cachedIds() {
        return new ArrayList<>(players.keySet());
    }

    /** Small helper kept separate so the backup path never throws at boot. */
    static final class SettingsBackup {
        private SettingsBackup() {
        }

        static void write(Path target, String content) throws IOException {
            AtomicFileWriter.write(target, content == null ? "" : content);
        }
    }

    public int loadedOrderSize() {
        return loadedOrder.size();
    }

    public Map<UUID, PlayerData> snapshot() {
        return new HashMap<>(players);
    }
}
