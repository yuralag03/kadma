package com.example.kadma;

import com.example.kadma.command.KadmaCommand;
import com.example.kadma.command.KadmaCommandContext;
import com.example.kadma.config.ConfigManager;
import com.example.kadma.data.DataStore;
import com.example.kadma.dialogue.DialogueService;
import com.example.kadma.event.DailyService;
import com.example.kadma.event.WeeklyEventService;
import com.example.kadma.hud.HudSyncServer;
import com.example.kadma.journal.JournalService;
import com.example.kadma.layer.LayerService;
import com.example.kadma.lobby.LobbyBridgeService;
import com.example.kadma.network.KadmaNetwork;
import com.example.kadma.network.KadmaNetwork;
import com.example.kadma.npc.NpcService;
import com.example.kadma.quest.QuestService;
import com.example.kadma.rumor.RumorService;
import com.example.kadma.story.StoryService;
import com.example.kadma.visual.VisualService;
import com.example.kadma.zone.ZoneService;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Per-server wiring of every subsystem.
 *
 * <p>Created once per dedicated server or integrated server so the mod never holds
 * stale level references across a world reload.</p>
 */
public final class KadmaRuntime implements KadmaServices {

    private final MinecraftServer server;
    private final ConfigManager config;
    private final DataStore data;

    private final StoryService story;
    private final QuestService quests;
    private final DialogueService dialogues;
    private final NpcService npcs;
    private final LayerService layers;
    private final ZoneService zones;
    private final VisualService visual;
    private final HudSyncServer hud;
    private final DailyService daily;
    private final WeeklyEventService events;
    private final RumorService rumors;
    private final JournalService journal;
    private final LobbyBridgeService lobbyBridge;

    public KadmaRuntime(MinecraftServer server) {
        this.server = server;
        this.config = new ConfigManager();
        this.config.load();
        this.data = new DataStore(config);
        this.data.loadGlobal();
        this.layers = new LayerService(this);
        this.zones = new ZoneService(this);
        this.visual = new VisualService(this);
        this.quests = new QuestService(this);
        this.dialogues = new DialogueService(this);
        this.npcs = new NpcService(this);
        this.story = new StoryService(this);
        this.rumors = new RumorService(this);
        this.journal = new JournalService(this);
        this.hud = new HudSyncServer(this);
        this.daily = new DailyService(this);
        this.events = new WeeklyEventService(this);
        this.lobbyBridge = new LobbyBridgeService(this);

        KadmaCommandContext.bind(this);
        logStartup();
    }

    private void logStartup() {
        KadmaMod.LOG.info("[kadma] config root: {}", config.root());
        KadmaMod.LOG.info("[kadma] content loaded: {}", config.countContent());
        for (String problem : config.problems()) {
            KadmaMod.LOG.warn("[kadma] config problem: {}", problem);
        }
    }

    /** Register {@code /kadma} using the shared dispatcher. */
    public void registerCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        dispatcher.register(KadmaCommand.build(this));
    }

    /** Server tick: duties, events, HUD cadence, objectives and periodic saves. */
    public void tick() {
        net.minecraftforge.event.TickEvent.ServerTickEvent phase = null;
        tickWorld();
        tickPlayers();
        tickHousekeeping();
    }

    private long tickCounter = 0L;
    private long lastAutosave = 0L;
    private long lastLayerCheck = 0L;
    private long lastMarkerSweep = 0L;

    private void tickWorld() {
        tickCounter++;
        com.example.kadma.data.GlobalData global = data.global();
        global.worldTick = tickCounter;
        daily.tick();
        events.tick();
    }

    private void tickPlayers() {
        if (server.getPlayerList().getPlayerCount() == 0) {
            hud.tick();
            return;
        }
        // Objective validation is the expensive path, so it runs at half a second
        // cadence. The HUD tick itself is already throttled by the config.
        boolean validate = tickCounter % 10L == 0L;
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            com.example.kadma.data.PlayerData playerData = data.player(player.getUUID());
            playerData.lastSeenDay = com.example.kadma.util.TimeUtil.currentDay(
                    config.settings().timezoneOffsetMinutes);
            if (validate) {
                quests.tickPlayer(player, playerData, tickCounter);
            }
            if (tickCounter % 20L == 0L) {
                tickLayerOnce(player, playerData);
            }
        }
        hud.tick();
    }

    private void tickLayerOnce(net.minecraft.server.level.ServerPlayer player, com.example.kadma.data.PlayerData playerData) {
        String layer = layers.currentLayerId(player);
        if (layer == null) {
            layer = "";
        }
        if (!layer.equals(playerData.currentLayer)) {
            String previous = playerData.currentLayer;
            playerData.currentLayer = layer;
            data.markPlayer(player.getUUID());
            onLayerChange(player, playerData, previous, layer);
        }
        layers.refreshAmbient(player, tickCounter);
        tickStoryAmbience(player, playerData);
    }

    private void tickStoryAmbience(net.minecraft.server.level.ServerPlayer player, com.example.kadma.data.PlayerData playerData) {
        if (!config.settings().features.visualEffects) {
            return;
        }
        if (zones.isInside(player, "portal_clearing") && tickCounter % 12L == 0L) {
            com.example.kadma.zone.ZoneRegion zone = zones.byId("portal_clearing");
            visual.portalAmbience(player, zone == null ? player.position() : zone.center(), (int) (tickCounter % 240L));
        }
        if (zones.isInside(player, "sealed_crack") && tickCounter % 10L == 0L) {
            com.example.kadma.zone.ZoneRegion zone = zones.byId("sealed_crack");
            visual.crackAmbience(player, zone == null ? player.position() : zone.center(), (int) (tickCounter % 200L));
        }
    }

    private void onLayerChange(net.minecraft.server.level.ServerPlayer player, com.example.kadma.data.PlayerData playerData,
                               String previous, String layer) {
        net.minecraft.network.chat.Component notice = layers.accessNotice(layer);
        if (notice != null) {
            player.sendSystemMessage(notice);
        }
        if (!playerData.enteredCity) {
            return;
        }
        story.catchUp().offer(player, playerData);
        visual.actionbar(player, "\u0421\u043b\u043e\u0439: " + layers.currentLayerDisplay(player));
    }

    private void tickHousekeeping() {
        if (tickCounter % 1200L == 0L) {
            events.maybeSchedule(data.global());
        }
        if (tickCounter % 200L == 0L) {
            visual.markers().sweep();
        }
        long autosave = config.settings().autosaveSeconds * 1000L;
        if (autosave > 0 && System.currentTimeMillis() - lastAutosave >= autosave) {
            lastAutosave = System.currentTimeMillis();
            int saved = data.saveAll();
            if (saved > 0) {
                KadmaMod.LOG.debug("[kadma] autosave flushed {} records", saved);
            }
        }
    }

    public void onServerStopping() {
        // Order matters: flush data first, then drop purely cosmetic entities.
        data.flushAll();
        data.saveState(com.example.kadma.data.JsonUtils.toJson(stateSnapshot()));
        try {
            visual.bars().hideAll();
            visual.markers().releaseAll();
            npcs.clearAll();
        } catch (Exception ex) {
            KadmaMod.LOG.warn("[kadma] cosmetic cleanup on shutdown failed", ex);
        }
        KadmaCommandContext.clear();
    }

    public java.util.Map<String, Object> stateSnapshot() {
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("players", data.cachedPlayers());
        out.put("savedRecords", data.savedPlayers());
        out.put("worldTick", tickCounter);
        out.put("unlockedChapter", data.global().unlockedChapter);
        out.put("activeEvent", data.global().activeEvent);
        out.put("configReloads", config.reloadCount());
        return out;
    }

    @Override
    public ConfigManager config() {
        return config;
    }

    @Override
    public DataStore data() {
        return data;
    }

    @Override
    public StoryService story() {
        return story;
    }

    @Override
    public QuestService quests() {
        return quests;
    }

    @Override
    public DialogueService dialogues() {
        return dialogues;
    }

    @Override
    public NpcService npcs() {
        return npcs;
    }

    @Override
    public LayerService layers() {
        return layers;
    }

    @Override
    public ZoneService zones() {
        return zones;
    }

    @Override
    public VisualService visual() {
        return visual;
    }

    @Override
    public HudSyncServer hud() {
        return hud;
    }

    @Override
    public DailyService daily() {
        return daily;
    }

    @Override
    public WeeklyEventService events() {
        return events;
    }

    @Override
    public RumorService rumors() {
        return rumors;
    }

    @Override
    public JournalService journal() {
        return journal;
    }

    @Override
    public LobbyBridgeService lobby() {
        return lobbyBridge;
    }

    @Override
    public MinecraftServer server() {
        return server;
    }

    public long tickCounter() {
        return tickCounter;
    }

    public static SimpleChannel channel() {
        return KadmaNetwork.channel();
    }
}
