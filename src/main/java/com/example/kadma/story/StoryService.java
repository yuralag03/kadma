package com.example.kadma.story;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.ChapterConfig;
import com.example.kadma.data.GlobalData;
import com.example.kadma.data.PlayerData;
import com.example.kadma.util.TextUtil;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Front door of the story system: chains chapters, Prozrenie, catch-up and flags
 * into a single place that commands and dialogue can talk to.
 */
public class StoryService {

    private final KadmaServices services;
    private final FlagService flags = new FlagService(null);
    private final ProzrenieService prozrenie;
    private final ChapterService chapters;
    private final CatchUpService catchUp;

    public StoryService(KadmaServices services) {
        this.services = services;
        this.prozrenie = new ProzrenieService(services);
        this.chapters = new ChapterService(services);
        this.catchUp = new CatchUpService(services);
    }

    public ProzrenieService prozrenie() {
        return prozrenie;
    }

    public ChapterService chapters() {
        return chapters;
    }

    public CatchUpService catchUp() {
        return catchUp;
    }

    /** Called when the player first joins or the server boots for them. */
    public void onboard(ServerPlayer player, PlayerData data) {
        GlobalData global = services.data().global();
        chapters.syncPlayer(data, false);
        if (data.prozrenie == 0 && data.prozrenieCarried == 0) {
            // first contact with the city, give a starting unit of Прозрение
            data.prozrenie = Math.max(0, prozrenie.max(data));
            services.data().markPlayer(player.getUUID());
        }
        if (global.chapterOpenDay == 0L) {
            global.chapterOpenDay = com.example.kadma.util.TimeUtil.currentDay(
                    services.config().settings().timezoneOffsetMinutes);
            services.data().touch();
        }
        services.data().markPlayer(player.getUUID());
        autoStart(player, data);
    }

    /**
     * Start every quest flagged {@code autoStart} that the player is eligible for,
     * once. This is the entry point into the story for a brand new player.
     */
    private void autoStart(ServerPlayer player, PlayerData data) {
        for (com.example.kadma.config.QuestConfig quest : services.config().quests().values()) {
            if (!quest.autoStart || !quest.usable()) {
                continue;
            }
            if (data.isComplete(quest.id) || data.hasActive(quest.id)) {
                continue;
            }
            if (!services.quests().canStart(data, quest.id)) {
                continue;
            }
            services.quests().startQuest(player, data, quest.id, true);
            break;
        }
    }

    public boolean grantProzrenie(ServerPlayer player, PlayerData data, int amount) {
        if (amount < 0) {
            return prozrenie.spend(player, data, -amount);
        }
        return prozrenie.grant(player, data, amount) > 0;
    }

    public int prozrenieOf(PlayerData data) {
        return prozrenie.value(data);
    }

    public int prozrenieMax(PlayerData data) {
        return prozrenie.max(data);
    }

    /** Set a player chapter directly (admin / dialogue effect). */
    public boolean setChapter(ServerPlayer player, PlayerData data, String chapterId) {
        ChapterConfig chapter = services.config().chapter(chapterId);
        if (chapter == null) {
            return false;
        }
        data.chapter = chapter.id;
        services.data().markPlayer(player.getUUID());
        catchUp.grantEssentials(data, chapter.lateEntryQuests);
        return true;
    }

    public void onMainStageCompleted(PlayerData data) {
        GlobalData global = services.data().global();
        global.totalMainStagesCompleted++;
        services.data().touch();
        chapters.recomputePoints(global);
    }

    public void onEventParticipation(PlayerData data) {
        GlobalData global = services.data().global();
        if (data.uuid != null && global.participants.add(data.uuid)) {
            services.data().touch();
        }
        global.contributors = Math.max(global.contributors, global.participants.size());
    }

    public void onEventCompleted() {
        GlobalData global = services.data().global();
        global.totalEventsCompleted++;
        services.data().touch();
        chapters.recomputePoints(global);
    }

    /** Daily hook: chapter windows, catch-up offers, world progression. */
    public void onDailyReset(ServerPlayer player, PlayerData data) {
        GlobalData global = services.data().global();
        ChapterConfig chapter = chapters.chapterOf(data);
        long remaining = chapters.daysRemaining(global, chapter);
        chapters.syncPlayer(data, false);
        catchUp.offerQuiet(player, data);
        if (remaining == 0L) {
            player.sendSystemMessage(TextUtil.info("\u041e\u043a\u043d\u043e \u0437\u0430\u043a\u0440\u044b\u0442\u043e. \u0413\u043e\u0440\u043e\u0434 \u0433\u043e\u0432\u043e\u0440\u0438\u0442 \u043e \u0441\u043b\u0443\u0447\u0438\u0432\u0448\u0435\u043c\u0441\u044f."));
        }
    }

    public void onDailyResetWorld(GlobalData global) {
        chapters.recomputePoints(global);
        chapters.tryAdvance(global);
        if (global.activeEvent.isEmpty() && services.config().settings().features.weeklyEvents) {
            services.events().maybeSchedule(global);
        }
    }

    public void afterWorldChange(GlobalData global) {
        chapters.recomputePoints(global);
    }

    public java.util.List<ChapterConfig> chapterList() {
        return chapters.ordered();
    }
}
