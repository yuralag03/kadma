package com.example.kadma.quest;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.QuestConfig;
import com.example.kadma.data.GlobalData;
import com.example.kadma.data.PlayerData;
import com.example.kadma.data.ObjectiveProgress;
import com.example.kadma.data.QuestProgress;
import com.example.kadma.util.TextUtil;
import com.example.kadma.util.TimeUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quest engine.
 *
 * <p>Every entry point is a server call. A client can only ask the server to start a
 * dialogue, never to complete a quest, change a flag or take a reward.</p>
 */
public class QuestService {

    private final KadmaServices services;
    private final ObjectiveValidator validator;

    /** goto anchors for quests that have no zone configured. */
    private final Map<String, Vec3> gotoAnchors = new LinkedHashMap<>();

    public QuestService(KadmaServices services) {
        this.services = services;
        this.validator = new ObjectiveValidator(services);
    }

    public ObjectiveValidator validator() {
        return validator;
    }

    public void rebuild() {
        gotoAnchors.clear();
    }

    // ------------------------------------------------------------------ queries

    public QuestConfig byId(String id) {
        return services.config().quest(id);
    }

    public boolean exists(String id) {
        return id != null && services.config().quests().containsKey(id);
    }

    public boolean meetsPrerequisites(PlayerData data, QuestConfig quest) {
        if (quest == null) {
            return false;
        }
        for (String prerequisite : quest.prerequisites) {
            if (prerequisite == null || prerequisite.isEmpty()) {
                continue;
            }
            if (data.isComplete(prerequisite) || data.flag(prerequisite)) {
                continue;
            }
            return false;
        }
        com.example.kadma.config.ChapterConfig chapter = services.config().chapter(data.chapter);
        if (chapter != null && quest.minStage > 0 && data.stage < quest.minStage) {
            return false;
        }
        return true;
    }

    public boolean dailyAvailable(PlayerData data, QuestConfig quest) {
        if (quest == null || quest.dailyLimit <= 0) {
            return true;
        }
        return data.counter(quest.id) < quest.dailyLimit;
    }

    public String blockers(PlayerData data, QuestConfig quest) {
        if (quest == null) {
            return "\u0417\u0430\u0434\u0430\u043d\u0438\u0435 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e";
        }
        if (!quest.usable()) {
            return "\u0417\u0430\u0434\u0430\u043d\u0438\u0435 \u043e\u0442\u043a\u043b\u044e\u0447\u0435\u043d\u043e ("
                    + String.join("; ", quest.problems) + ")";
        }
        if (data.isComplete(quest.id) && !quest.repeatable) {
            return "\u0423\u0436\u0435 \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u043e";
        }
        if (data.hasActive(quest.id)) {
            return "\u0423\u0436\u0435 \u0432 \u0440\u0430\u0431\u043e\u0442\u0435";
        }
        if (!meetsPrerequisites(data, quest)) {
            return "\u041d\u0435\u0442 \u0434\u043e\u0441\u0442\u0443\u043f\u0430";
        }
        if (!dailyAvailable(data, quest)) {
            return "\u041b\u0438\u043c\u0438\u0442 \u043d\u0430 \u0434\u0435\u043d\u044c";
        }
        if (!services.layers().acceptsQuestType(quest.layer, quest.type)) {
            return "\u0421\u043b\u043e\u0439 \u043d\u0435 \u043f\u0440\u0438\u043d\u0438\u043c\u0430\u0435\u0442 \u0442\u0430\u043a\u0438\u0435 \u0437\u0430\u0434\u0430\u043d\u0438\u044f";
        }
        return null;
    }

    public boolean canStart(PlayerData data, String questId) {
        return blockers(data, services.config().quest(questId)) == null;
    }

    // ------------------------------------------------------------------- start

    public boolean startQuest(ServerPlayer player, PlayerData data, String questId, boolean silent) {
        QuestConfig quest = services.config().quest(questId);
        String problem = blockers(data, quest);
        if (problem != null) {
            if (!silent) {
                player.sendSystemMessage(TextUtil.warning("\u041d\u0435\u043b\u044c\u0437\u044f \u043d\u0430\u0447\u0430\u0442\u044c: " + problem));
            }
            return false;
        }
        if (quest.requiresProzrenie > 0) {
            if (!services.story().prozrenie().spend(player, data, quest.requiresProzrenie)) {
                if (!silent) {
                    player.sendSystemMessage(TextUtil.warning("\u041d\u0435\u0434\u043e\u0441\u0442\u0430\u0442\u043e\u0447\u043d\u043e \u041f\u0440\u043e\u0437\u0440\u0435\u043d\u0438\u044f."));
                }
                return false;
            }
        }
        QuestProgress progress = new QuestProgress(quest.id, quest.type,
                TimeUtil.currentDay(services.config().settings().timezoneOffsetMinutes),
                System.currentTimeMillis());
        for (com.example.kadma.config.ObjectiveDefinition spec : quest.objectives) {
            progress.ensureObjective(spec.id);
        }
        data.activeQuests.add(progress);
        data.counters.put(quest.id, data.counter(quest.id) + 1);
        services.data().markPlayer(player.getUUID());

        for (String entry : quest.journalEntries) {
            data.addJournal(entry);
        }
        for (String rumor : quest.rumorsOnStart) {
            if (!data.knownRumor(rumor)) {
                data.rememberRumor(rumor);
                services.rumors().deliver(services.data().global(), player, rumor);
            }
        }
        if (quest.isMain()) {
            services.story().onMainStageCompleted(data);
            data.stage = Math.max(data.stage, quest.stage);
        }
        if (quest.aftermath) {
            services.story().onEventParticipation(data);
        }
        if (!quest.marker.isEmpty()) {
            services.visual().markers().anchor(quest.id, quest, player);
        }
        if (!silent) {
            player.sendSystemMessage(TextUtil.ok("\u0417\u0430\u043f\u0438\u0441\u044c \u043e\u0442\u043a\u0440\u044b\u0442\u0430: " + quest.title));
            services.visual().questStart(player, quest);
        }
        services.journal().note(data, quest);
        return true;
    }

    // --------------------------------------------------------------- complete

    public boolean completeQuest(ServerPlayer player, PlayerData data, String questId, boolean silent) {
        QuestConfig quest = services.config().quest(questId);
        if (quest == null) {
            return false;
        }
        QuestProgress progress = data.active(questId);
        if (progress == null) {
            if (data.isComplete(questId) && !quest.repeatable) {
                return false;
            }
            progress = new QuestProgress(quest.id, quest.type,
                    TimeUtil.currentDay(services.config().settings().timezoneOffsetMinutes),
                    System.currentTimeMillis());
            for (com.example.kadma.config.ObjectiveDefinition spec : quest.objectives) {
                progress.ensureObjective(spec.id);
                ObjectiveProgress stored = progress.objectives.get(spec.id);
                stored.complete = true;
                if (spec.type.equals("collect") || spec.type.equals("kill") || spec.type.equals("deliver")) {
                    stored.setProgress(spec.amount);
                } else {
                    stored.setProgress(1);
                }
            }
        } else {
            boolean allDone = true;
            for (com.example.kadma.config.ObjectiveDefinition spec : quest.objectives) {
                ObjectiveProgress stored = progress.objectives.get(spec.id);
                if (stored == null) {
                    stored = new ObjectiveProgress(spec.id);
                    progress.put(stored);
                }
                if (spec.optional) {
                    continue;
                }
                if (!stored.complete) {
                    allDone = false;
                }
            }
            if (!allDone) {
                // server side re-validation of the objective counts, ignoring the client
                validator.tickPlayer(player, data, 0L);
                QuestProgress check = data.active(questId);
                if (check == null) {
                    return false;
                }
                for (com.example.kadma.config.ObjectiveDefinition spec : quest.objectives) {
                    if (spec.optional) {
                        continue;
                    }
                    ObjectiveProgress stored = check.objectives.get(spec.id);
                    if (stored == null || !stored.complete) {
                        return false;
                    }
                }
            }
        }

        for (String flag : quest.flagsOnComplete) {
            data.flags.put(flag, "true");
        }
        for (String flag : quest.globalFlagsOnComplete) {
            services.data().global().flags.put(flag, "true");
        }
        for (String rumor : quest.rumorsOnComplete) {
            data.rememberRumor(rumor);
            services.rumors().deliver(services.data().global(), player, rumor);
        }
        data.activeQuests.remove(progress);
        data.completedQuests.add(quest.id);
        services.data().markPlayer(player.getUUID());
        services.data().touch();

        RewardService.apply(services, player, data, services.data().global(), quest.rewards, silent);
        services.visual().markers().release(quest.id);
        if (quest.isMain()) {
            services.story().onMainStageCompleted(data);
            data.stage = Math.max(data.stage, quest.stage);
        }
        if (quest.aftermath) {
            services.story().onEventParticipation(data);
        }
        for (String unlock : quest.unlockQuests) {
            QuestConfig next = services.config().quest(unlock);
            if (next != null && next.autoStart && canStart(data, unlock)) {
                startQuest(player, data, unlock, true);
            }
        }
        services.journal().noteCompletion(data, quest);
        if (!silent) {
            player.sendSystemMessage(TextUtil.ok("\u0417\u0430\u043f\u0438\u0441\u044c \u0437\u0430\u043a\u0440\u044b\u0442\u0430: " + quest.title));
            services.visual().questComplete(player, quest);
        }
        services.story().chapters().recomputePoints(services.data().global());
        return true;
    }

    public boolean resetProgress(ServerPlayer player, PlayerData data, String questId) {
        boolean removed = false;
        data.activeQuests.removeIf(progress -> progress.questId.equals(questId));
        data.completedQuests.remove(questId);
        data.counters.remove(questId);
        services.data().markPlayer(player.getUUID());
        services.visual().markers().release(questId);
        return true;
    }

    public boolean abandon(ServerPlayer player, PlayerData data, String questId) {
        QuestProgress progress = data.active(questId);
        if (progress == null) {
            return false;
        }
        progress.abandoned = true;
        data.activeQuests.remove(progress);
        services.data().markPlayer(player.getUUID());
        return true;
    }

    // ---------------------------------------------------------------- instances

    public QuestInstance instance(PlayerData data, String questId) {
        QuestProgress progress = data.active(questId);
        if (progress == null) {
            return null;
        }
        return new QuestInstance(services.config().quest(questId), progress);
    }

    public List<QuestInstance> active(PlayerData data) {
        List<QuestInstance> out = new ArrayList<>();
        for (QuestProgress progress : data.activeQuests) {
            QuestConfig definition = services.config().quest(progress.questId);
            if (definition == null) {
                continue;
            }
            out.add(new QuestInstance(definition, progress));
        }
        return out;
    }

    /** Active quest driven by the HUD: main story first, then duty, then anything. */
    public QuestInstance primary(PlayerData data) {
        List<QuestInstance> list = active(data);
        for (QuestInstance instance : list) {
            if ("main".equals(instance.type()) || "catch_up".equals(instance.type())) {
                return instance;
            }
        }
        for (QuestInstance instance : list) {
            if ("daily".equals(instance.type()) || "side".equals(instance.type())) {
                return instance;
            }
        }
        return list.isEmpty() ? null : list.get(0);
    }

    // ------------------------------------------------------------------ anchors

    public void setAnchor(String questId, Vec3 position) {
        if (questId != null && position != null) {
            gotoAnchors.put(questId, position);
        }
    }

    public boolean hasGotoAnchor(String questId) {
        return gotoAnchors.containsKey(questId);
    }

    public double gotoAnchorDistance(String questId, Vec3 position) {
        Vec3 anchor = gotoAnchors.get(questId);
        return anchor == null || position == null ? Double.MAX_VALUE : anchor.distanceTo(position);
    }

    public void clearAnchors() {
        gotoAnchors.clear();
    }

    public void tickPlayer(ServerPlayer player, PlayerData data, long worldTick) {
        validator.tickPlayer(player, data, worldTick);
    }

    public UUID id(ServerPlayer player) {
        return player.getUUID();
    }
}
