package com.example.kadma.story;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.QuestConfig;
import com.example.kadma.data.PlayerData;
import com.example.kadma.util.TextUtil;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Catch-up entry points.
 *
 * <p>Late players are never shown the scene they missed. They receive the flags the
 * missed content would have granted, plus a present-time task built out of what the
 * city looks like now.</p>
 */
public class CatchUpService {

    private final KadmaServices services;

    public CatchUpService(KadmaServices services) {
        this.services = services;
    }

    /**
     * Look for a catch-up quest that a player is eligible for: they are behind the
     * world chapter and missing the flags the late entry quest grants.
     */
    public QuestConfig nextCatchUp(PlayerData data) {
        if (!services.config().settings().features.catchUp) {
            return null;
        }
        for (QuestConfig quest : services.config().quests().values()) {
            if (!quest.catchUp || !quest.usable()) {
                continue;
            }
            if (data.isComplete(quest.id) || data.hasActive(quest.id)) {
                continue;
            }
            if (!meetsPrerequisites(data, quest)) {
                continue;
            }
            return quest;
        }
        return null;
    }

    private boolean meetsPrerequisites(PlayerData data, QuestConfig quest) {
        for (String prerequisite : quest.prerequisites) {
            if (prerequisite.isEmpty()) {
                continue;
            }
            if (data.isComplete(prerequisite) || data.flag(prerequisite)) {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * Offer the catch-up path. Returns {@code true} when a quest actually started.
     */
    public boolean offer(ServerPlayer player, PlayerData data) {
        QuestConfig quest = nextCatchUp(data);
        if (quest == null) {
            return false;
        }
        boolean started = services.quests().startQuest(player, data, quest.id, false);
        if (started) {
            player.sendSystemMessage(TextUtil.info("\u0421\u0442\u0435\u043d\u0430 \u0437\u043d\u0430\u0435\u0442 \u0442\u0435\u0431\u044f. \u041e\u043d\u0430 \u043d\u0435 \u043f\u043e\u0432\u0442\u043e\u0440\u0438\u0442 \u0441\u0442\u0430\u0440\u044b\u0435 \u0441\u043b\u043e\u0432\u0430."));
        }
        return started;
    }

    /** Silent variant used by daily reset. */
    public boolean offerQuiet(ServerPlayer player, PlayerData data) {
        QuestConfig quest = nextCatchUp(data);
        if (quest == null) {
            return false;
        }
        return services.quests().startQuest(player, data, quest.id, true);
    }

    /** A late player entering the chapter gets the essential flags immediately. */
    public int grantEssentials(PlayerData data, List<String> flags) {
        int granted = 0;
        for (String flag : flags) {
            if (flag == null || flag.isEmpty()) {
                continue;
            }
            if (!data.flags.containsKey(flag)) {
                data.flags.put(flag, "true");
                granted++;
            }
        }
        if (granted > 0 && data.uuid != null && !data.uuid.isEmpty()) {
            services.data().markPlayer(UUID.fromString(data.uuid));
        }
        return granted;
    }

    public boolean isLate(PlayerData data) {
        return !data.chapter.equals(services.data().global().unlockedChapter);
    }
}
