package com.example.kadma;

import com.example.kadma.config.ConfigManager;
import com.example.kadma.data.DataStore;
import com.example.kadma.dialogue.DialogueService;
import com.example.kadma.event.DailyService;
import com.example.kadma.event.WeeklyEventService;
import com.example.kadma.hud.HudSyncServer;
import com.example.kadma.journal.JournalService;
import com.example.kadma.layer.LayerService;
import com.example.kadma.lobby.LobbyBridgeService;
import com.example.kadma.npc.NpcService;
import com.example.kadma.quest.QuestService;
import com.example.kadma.rumor.RumorService;
import com.example.kadma.story.StoryService;
import com.example.kadma.visual.VisualService;
import com.example.kadma.zone.ZoneService;

/**
 * Service locator shared by every subsystem.
 *
 * <p>Keeping this as an interface is what lets the client entry point stay inert on
 * a dedicated server and keeps server-only classes out of the client classpath at
 * runtime.</p>
 */
public interface KadmaServices {

    ConfigManager config();

    DataStore data();

    StoryService story();

    QuestService quests();

    DialogueService dialogues();

    NpcService npcs();

    LayerService layers();

    ZoneService zones();

    VisualService visual();

    HudSyncServer hud();

    DailyService daily();

    WeeklyEventService events();

    RumorService rumors();

    JournalService journal();

    LobbyBridgeService lobby();

    net.minecraft.server.MinecraftServer server();
}
