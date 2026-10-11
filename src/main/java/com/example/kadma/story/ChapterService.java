package com.example.kadma.story;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.ChapterConfig;
import com.example.kadma.data.GlobalData;
import com.example.kadma.data.PlayerData;
import com.example.kadma.util.TextUtil;
import com.example.kadma.util.TimeUtil;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Chapter windows.
 *
 * <p>Chapter progress is a function of aggregate player activity plus a wall-clock
 * window. Exiting the window never blocks a late player: it opens the aftermath and
 * late entry paths instead.</p>
 */
public class ChapterService {

    private final KadmaServices services;

    public ChapterService(KadmaServices services) {
        this.services = services;
    }

    public ChapterConfig chapterOf(PlayerData data) {
        ChapterConfig chapter = services.config().chapter(data.chapter);
        if (chapter != null) {
            return chapter;
        }
        ChapterConfig fallback = services.config().chapter(services.data().global().unlockedChapter);
        if (fallback != null) {
            return fallback;
        }
        return services.config().firstChapter();
    }

    public ChapterConfig byId(String id) {
        return services.config().chapter(id);
    }

    public ChapterConfig nextOf(ChapterConfig chapter) {
        if (chapter == null || chapter.nextChapter.isEmpty()) {
            return null;
        }
        return services.config().chapter(chapter.nextChapter);
    }

    public long daysOpen(GlobalData global, ChapterConfig chapter) {
        long today = TimeUtil.currentDay(services.config().settings().timezoneOffsetMinutes);
        if (global.chapterOpenDay == 0L) {
            global.chapterOpenDay = today;
            services.data().touch();
        }
        return Math.max(0L, today - global.chapterOpenDay);
    }

    public long daysRemaining(GlobalData global, ChapterConfig chapter) {
        if (chapter == null) {
            return 0L;
        }
        return Math.max(0L, chapter.openDays - daysOpen(global, chapter));
    }

    public int progressPoints(GlobalData global) {
        return global.stat("global_points");
    }

    /** Aggregate activity from completed main stages, shards, side quests and rites. */
    public int recomputePoints(GlobalData global) {
        int points = 0;
        points += (int) Math.min(200L, global.totalMainStagesCompleted * 4L);
        points += Math.min(150, global.stat("portal_shards_total"));
        points += global.participants.size() * 2;
        points += Math.min(120, (int) global.totalEventsCompleted * 10L);
        points += global.stat("global_points_bonus");
        points += global.customFlags.size() * 2;
        points += global.worldJournal.size();
        global.setStat("global_points", points);
        services.data().touch();
        return points;
    }

    /**
     * Advance the world chapter when thresholds are met. Called from the daily
     * reset and from the admin command.
     */
    public boolean tryAdvance(GlobalData global) {
        ChapterConfig current = services.config().chapter(global.unlockedChapter);
        ChapterConfig next = nextOf(current);
        if (next == null) {
            return false;
        }
        long days = daysOpen(global, current);
        boolean thresholdsMet = progressPoints(global) >= next.requiredGlobalPoints
                && contributors(global) >= next.requiredPlayerCount;
        boolean windowClosed = days >= next.openDays;
        if (!thresholdsMet && !windowClosed) {
            return false;
        }
        if (!thresholdsMet && windowClosed && current.extendWhenBehind) {
            // extend quietly instead of stranding content
            global.chapterOpenDay = TimeUtil.currentDay(services.config().settings().timezoneOffsetMinutes);
            services.data().touch();
            return false;
        }
        unlock(global, next, thresholdsMet);
        return true;
    }

    private int contributors(GlobalData global) {
        return Math.max(global.contributors, global.participants.size());
    }

    public void unlock(GlobalData global, ChapterConfig chapter, boolean earned) {
        if (chapter == null) {
            return;
        }
        global.previousChapter = global.unlockedChapter;
        global.unlockedChapter = chapter.id;
        global.chapterOpenDay = TimeUtil.currentDay(services.config().settings().timezoneOffsetMinutes);
        global.chapterHistory.put(chapter.id, global.chapterOpenDay);
        services.data().touch();
        if (chapter.globalEffects != null) {
            for (java.util.Map.Entry<String, Integer> entry : chapter.globalEffects.stats.entrySet()) {
                global.bumpStat(entry.getKey(), entry.getValue() == null ? 0 : entry.getValue());
            }
            for (String flag : chapter.globalEffects.flags) {
                global.flags.put(flag, "true");
            }
        }
        services.rumors().announce(chapter.rumorsOnOpen);
        services.journal().world(chapter.journalOnOpen, chapter.title);
    }

    /** Move one player into the current world chapter without replaying old scenes. */
    public void syncPlayer(PlayerData data, boolean force) {
        GlobalData global = services.data().global();
        if (!force && data.chapter.equals(global.unlockedChapter)) {
            return;
        }
        data.chapter = global.unlockedChapter;
        services.data().markPlayer(java.util.UUID.fromString(data.uuid));
    }

    public List<ChapterConfig> ordered() {
        return services.config().orderedChapters();
    }

    public String describe(ChapterConfig chapter) {
        if (chapter == null) {
            return "\u043d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u0430\u044f \u0433\u043b\u0430\u0432\u0430";
        }
        return chapter.title;
    }

    /** Broadcast a short present-tense line about the chapter state. */
    public void announce(ChapterConfig chapter, List<ChapterConfig> chapters) {
        for (ServerPlayer player : services.server().getPlayerList().getPlayers()) {
            player.sendSystemMessage(TextUtil.info(chapter.title));
        }
    }
}
