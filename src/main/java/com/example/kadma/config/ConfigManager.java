package com.example.kadma.config;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads, validates and reloads every JSON content pack under
 * {@code config/kadma}.
 *
 * <p>Missing files are seeded from the bundled samples in
 * {@code /default-configs/}. Any file that fails to parse leaves the whole system on
 * its previous, valid state - a broken content pack can never take the server down.</p>
 */
public class ConfigManager {

    public static final Logger LOG = LogUtils.getLogger();

    /** Files owned by the mod, copied out of the jar when absent. */
    public static final String[] MANAGED_FILES = {
            "settings.json",
            "layers.json",
            "zones.json",
            "items.json",
            "npcs.json",
            "dialogues.json",
            "quests.json",
            "chapters.json",
            "rumors.json",
            "events.json",
            "world_states.json"
    };

    private final Path root;
    private final ModSettings settings = new ModSettings();
    private final Map<String, LayerConfig> layers = new LinkedHashMap<>();
    private final Map<String, ZoneConfig> zones = new LinkedHashMap<>();
    private final Map<String, ItemConfig> items = new LinkedHashMap<>();
    private final Map<String, NpcConfig> npcs = new LinkedHashMap<>();
    private final Map<String, DialogueConfig> dialogues = new LinkedHashMap<>();
    private final Map<String, QuestConfig> quests = new LinkedHashMap<>();
    private final Map<String, ChapterConfig> chapters = new LinkedHashMap<>();
    private final Map<String, RumorConfig> rumors = new LinkedHashMap<>();
    private final Map<String, EventConfig> events = new LinkedHashMap<>();
    private final JsonObject worldDefaults = new JsonObject();

    private final List<String> problems = new ArrayList<>();
    private long loadedAt = 0L;
    private int reloadCount = 0;

    public ConfigManager(Path root) {
        this.root = root;
    }

    public ConfigManager() {
        this(defaultRoot());
    }

    public static Path defaultRoot() {
        Path forgeConfig = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
        return forgeConfig.resolve("kadma");
    }

    public Path root() {
        return root;
    }

    public Path playersDir() {
        return root.resolve("players");
    }

    public Path globalFile() {
        return root.resolve("global.json");
    }

    public Path stateFile() {
        return root.resolve("state.json");
    }

    /** Create the folder, seed defaults and parse everything. Never throws. */
    public void load() {
        problems.clear();
        try {
            Files.createDirectories(root);
            Files.createDirectories(playersDir());
        } catch (IOException ex) {
            problems.add("cannot create config root: " + ex.getMessage());
            LOG.error("[kadma] cannot create {}", root, ex);
        }
        seedDefaults();
        parseAll();
        loadedAt = System.currentTimeMillis();
    }

    /** Reload every file. Config errors keep the previous valid state. */
    public int reload() {
        Map<String, LayerConfig> layerCopy = new LinkedHashMap<>(layers);
        Map<String, ZoneConfig> zoneCopy = new LinkedHashMap<>(zones);
        Map<String, ItemConfig> itemCopy = new LinkedHashMap<>(items);
        Map<String, NpcConfig> npcCopy = new LinkedHashMap<>(npcs);
        Map<String, DialogueConfig> dialogueCopy = new LinkedHashMap<>(dialogues);
        Map<String, QuestConfig> questCopy = new LinkedHashMap<>(quests);
        Map<String, ChapterConfig> chapterCopy = new LinkedHashMap<>(chapters);
        Map<String, RumorConfig> rumorCopy = new LinkedHashMap<>(rumors);
        Map<String, EventConfig> eventCopy = new LinkedHashMap<>(events);
        JsonObject worldCopy = worldDefaults.deepCopy();

        problems.clear();
        parseAll();        if (layers.isEmpty() && !layerCopy.isEmpty()) {
            layers.putAll(layerCopy);
            problems.add("layers.json produced no layer, kept previous");
        }
        if (quests.isEmpty() && !questCopy.isEmpty()) {
            quests.putAll(questCopy);
            problems.add("quests.json produced no quest, kept previous");
        }
        if (dialogues.isEmpty() && !dialogueCopy.isEmpty()) {
            dialogues.putAll(dialogueCopy);
            problems.add("dialogues.json produced no dialogue, kept previous");
        }
        if (chapters.isEmpty() && !chapterCopy.isEmpty()) {
            chapters.putAll(chapterCopy);
            problems.add("chapters.json produced no chapter, kept previous");
        }
        if (items.isEmpty() && !itemCopy.isEmpty()) {
            items.putAll(itemCopy);
        }
        if (zones.isEmpty() && !zoneCopy.isEmpty()) {
            zones.putAll(zoneCopy);
        }
        if (npcs.isEmpty() && !npcCopy.isEmpty()) {
            npcs.putAll(npcCopy);
        }
        if (rumors.isEmpty() && !rumorCopy.isEmpty()) {
            rumors.putAll(rumorCopy);
        }
        if (events.isEmpty() && !eventCopy.isEmpty()) {
            events.putAll(eventCopy);
        }
        if (worldDefaults.size() == 0 && worldCopy.size() > 0) {
            for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : worldCopy.entrySet()) {
                worldDefaults.add(entry.getKey(), entry.getValue());
            }
        }
        reloadCount++;
        loadedAt = System.currentTimeMillis();
        return reloadCount;
    }

    private void parseAll() {
        ModSettings parsed = ModSettings.from(read("settings.json"));
        if (parsed.dailyResetHour != settings.dailyResetHour) {
            LOG.info("[kadma] settings.json: dailyResetHour={}", parsed.dailyResetHour);
        }
        settings.assign(parsed);

        layers.clear();
        forEach("layers.json", "layers", (id, raw) -> layers.put(id, LayerConfig.from(raw)));
        zones.clear();
        forEach("zones.json", "zones", (id, raw) -> zones.put(id, ZoneConfig.from(raw)));
        items.clear();
        forEach("items.json", "items", (id, raw) -> items.put(id, ItemConfig.from(raw)));
        npcs.clear();
        forEach("npcs.json", "npcs", (id, raw) -> npcs.put(id, NpcConfig.from(raw)));
        dialogues.clear();
        forEach("dialogues.json", "dialogues", (id, raw) -> dialogues.put(id, DialogueConfig.from(raw)));
        quests.clear();
        forEach("quests.json", "quests", (id, raw) -> quests.put(id, QuestConfig.from(raw)));
        chapters.clear();
        forEach("chapters.json", "chapters", (id, raw) -> chapters.put(id, ChapterConfig.from(raw)));
        rumors.clear();
        forEach("rumors.json", "rumors", (id, raw) -> rumors.put(id, RumorConfig.from(raw)));
        events.clear();
        forEach("events.json", "events", (id, raw) -> events.put(id, EventConfig.from(raw)));

        worldDefaults.getAsJsonObject().entrySet().clear();
        JsonObject world = read("world_states.json");
        if (world != null) {
            JsonObject defaults = world.has("defaults") && world.get("defaults").isJsonObject()
                    ? world.getAsJsonObject("defaults") : world;
            for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : defaults.entrySet()) {
                worldDefaults.add(entry.getKey(), entry.getValue());
            }
        }
    }

    private interface Collector {
        void accept(String id, JsonObject raw);
    }

    private void forEach(String file, String member, Collector collector) {
        JsonObject object = read(file);
        if (object == null) {
            if (Files.isRegularFile(root.resolve(file))) {
                problems.add(file + ": unreadable or invalid JSON, ignored");
                LOG.warn("[kadma] {} is not readable JSON, ignoring", file);
            }
            return;
        }
        collectFrom(object, member, collector);
    }

    private void collectFrom(JsonObject object, String member, Collector collector) {
        com.google.gson.JsonArray array = com.example.kadma.data.JsonUtils.array(object, member);
        if (array != null) {
            for (com.google.gson.JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject entry = element.getAsJsonObject();
                String id = com.example.kadma.data.JsonUtils.str(entry, "id", "");
                if (id.isEmpty()) {
                    problems.add(member + ": entry without id skipped");
                    continue;
                }
                collector.accept(id, entry);
            }
            return;
        }
        JsonObject map = com.example.kadma.data.JsonUtils.object(object, member);
        if (map != null) {
            for (Map.Entry<String, com.google.gson.JsonElement> entry : map.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                collector.accept(entry.getKey(), entry.getValue().getAsJsonObject());
            }
            return;
        }
        // tolerates a bare top level map {"lower": {...}}
        if (object.keySet().size() > 0) {
            for (Map.Entry<String, com.google.gson.JsonElement> entry : object.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    collector.accept(entry.getKey(), entry.getValue().getAsJsonObject());
                }
            }
        }
    }

    // ---------------------------------------------------------------- accessors

    public ModSettings settings() {
        return settings;
    }

    public Map<String, LayerConfig> layers() {
        return layers;
    }

    public Map<String, ZoneConfig> zones() {
        return zones;
    }

    public Map<String, ItemConfig> items() {
        return items;
    }

    public Map<String, NpcConfig> npcs() {
        return npcs;
    }

    public Map<String, DialogueConfig> dialogues() {
        return dialogues;
    }

    public Map<String, QuestConfig> quests() {
        return quests;
    }

    public Map<String, ChapterConfig> chapters() {
        return chapters;
    }

    public Map<String, RumorConfig> rumors() {
        return rumors;
    }

    public Map<String, EventConfig> events() {
        return events;
    }

    public JsonObject worldDefaults() {
        return worldDefaults;
    }

    public List<String> problems() {
        return problems;
    }

    public long loadedAt() {
        return loadedAt;
    }

    public int reloadCount() {
        return reloadCount;
    }

    public LayerConfig layer(String id) {
        return layers.get(id);
    }

    public ZoneConfig zone(String id) {
        return zones.get(id);
    }

    public NpcConfig npc(String id) {
        return npcs.get(id);
    }

    public DialogueConfig dialogue(String id) {
        return dialogues.get(id);
    }

    public QuestConfig quest(String id) {
        return quests.get(id);
    }

    public ChapterConfig chapter(String id) {
        return chapters.get(id);
    }

    public RumorConfig rumor(String id) {
        return rumors.get(id);
    }

    public EventConfig event(String id) {
        return events.get(id);
    }

    public ItemConfig item(String id) {
        return items.get(id);
    }

    /** Chapter list sorted by declared order. */
    public List<ChapterConfig> orderedChapters() {
        List<ChapterConfig> list = new ArrayList<>(chapters.values());
        list.sort((a, b) -> Integer.compare(a.order, b.order));
        return list;
    }

    public ChapterConfig firstChapter() {
        List<ChapterConfig> ordered = orderedChapters();
        return ordered.isEmpty() ? null : ordered.get(0);
    }

    // --------------------------------------------------------------------- io

    private JsonObject read(String name) {
        Path target = root.resolve(name);
        if (!Files.isRegularFile(target)) {
            return fromResource("default-configs/" + name);
        }
        return com.example.kadma.data.JsonUtils.readObject(target);
    }

    private JsonObject fromResource(String resource) {
        try (InputStream stream = ConfigManager.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new java.io.InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ex) {
            LOG.warn("[kadma] cannot read bundled fallback {}", resource);
            return null;
        }
    }

    private void seedDefaults() {
        for (String name : MANAGED_FILES) {
            Path target = root.resolve(name);
            if (Files.exists(target)) {
                continue;
            }
            JsonObject fallback = fromResource("default-configs/" + name);
            if (fallback == null) {
                LOG.warn("[kadma] no bundled default for {}", name);
                continue;
            }
            try {
                com.example.kadma.data.AtomicFileWriter.write(target,
                        com.example.kadma.data.JsonUtils.toJson(fallback));
            } catch (IOException ex) {
                LOG.warn("[kadma] cannot write default {}", name, ex);
            }
        }
    }

    public int countContent() {
        return layers.size() + zones.size() + items.size() + npcs.size() + dialogues.size()
                + quests.size() + chapters.size() + rumors.size() + events.size();
    }
}
