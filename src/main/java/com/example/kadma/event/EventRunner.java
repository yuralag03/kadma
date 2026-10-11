package com.example.kadma.event;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.EventConfig;
import com.example.kadma.data.GlobalData;
import com.example.kadma.data.PlayerData;
import com.example.kadma.quest.RewardService;
import com.example.kadma.util.TextUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Runner state for one executed weekly rite. */
public class EventRunner {

    private final KadmaServices services;
    private final EventConfig definition;
    private final List<EventPhase> phases = new ArrayList<>();
    private final Map<UUID, Boolean> joined = new HashMap<>();

    private int currentPhase = 0;
    private long startedTick = 0L;
    private boolean finished = false;
    private boolean succeeded = false;

    public EventRunner(KadmaServices services, EventConfig definition) {
        this.services = services;
        this.definition = definition;
        for (com.example.kadma.config.EventPhase phase : definition.phases) {
            phases.add(new EventPhase(phase));
        }
    }

    public EventConfig definition() {
        return definition;
    }

    public String id() {
        return definition.id;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean succeeded() {
        return succeeded;
    }

    public int currentIndex() {
        return currentPhase;
    }

    public EventPhase current() {
        return phases.isEmpty() ? null : phases.get(Math.min(currentPhase, phases.size() - 1));
    }

    public int participantCount() {
        int total = 0;
        for (Boolean value : joined.values()) {
            if (Boolean.TRUE.equals(value)) {
                total++;
            }
        }
        return total;
    }

    public void start(long worldTick) {
        startedTick = worldTick;
        if (!phases.isEmpty()) {
            EventPhase first = phases.get(0);
            first.state = EventPhase.State.RUNNING;
            announcePhase(first);
        }
        collectParticipation();
        for (String announcement : definition.announceText) {
            for (ServerPlayer player : services.server().getPlayerList().getPlayers()) {
                player.sendSystemMessage(TextUtil.info(announcement));
            }
        }
        services.data().touch();
    }

    private void announcePhase(EventPhase phase) {
        String heading = phase.definition.bossTitle == null || phase.definition.bossTitle.isEmpty()
                ? phase.title()
                : phase.definition.bossTitle;
        for (ServerPlayer player : services.server().getPlayerList().getPlayers()) {
            services.visual().bars().show(player, heading, 0.0F,
                    definition.bossColor, definition.bossOverlay);
            services.visual().title(player, phase.title(), phase.definition.description, 10, 50, 15);
            for (String message : phase.definition.messages) {
                player.sendSystemMessage(TextUtil.info(message));
            }
        }
    }

    /** Called from the server tick while the rite runs. */
    public void tick(long worldTick) {
        if (finished || phases.isEmpty()) {
            return;
        }
        EventPhase phase = phases.get(currentPhase);
        collectParticipation();

        switch (phase.definition.type) {
            case "gather", "ritual", "chat_vote" -> tickPassive(worldTick, phase);
            case "collect" -> tickCollect(phase);
            case "defend" -> tickDefend(phase);
            case "survive" -> tickSurvive(worldTick, phase);
            default -> tickPassive(worldTick, phase);
        }

        float progress = phase.progress();
        for (ServerPlayer player : services.server().getPlayerList().getPlayers()) {
            services.visual().bars().update(player, progress);
        }

        if (phase.state == EventPhase.State.DONE || phase.state == EventPhase.State.FAILED) {
            completePhase(phase);
        }
    }

    private void collectParticipation() {
        for (ServerPlayer player : services.server().getPlayerList().getPlayers()) {
            PlayerData data = services.data().player(player.getUUID());
            boolean inside = definition.zone.isEmpty()
                    || services.zones().isInside(player, definition.zone)
                    || definition.layer.isEmpty()
                    || services.layers().currentLayerId(player).equals(definition.layer);
            if (inside) {
                joined.putIfAbsent(player.getUUID(), Boolean.TRUE);
                data.flags.put("event." + definition.id, "true");
                services.data().markPlayer(player.getUUID());
            }
        }
    }

    private void tickPassive(long worldTick, EventPhase phase) {
        phase.elapsedTicks = (int) Math.min(Integer.MAX_VALUE, worldTick - startedTick);
        int participants = participantCount();
        if (participants < phase.definition.requiredPlayers) {
            return;
        }
        phase.score = Math.max(phase.score, participants);
        if (phase.elapsedTicks >= phase.definition.seconds * 20) {
            phase.state = EventPhase.State.DONE;
        }
    }

    private void tickCollect(EventPhase phase) {
        int found = 0;
        for (ServerPlayer player : services.server().getPlayerList().getPlayers()) {
            found += com.example.kadma.util.InventoryUtil.countStoryItem(player, phase.definition.objective);
        }
        phase.score = found;
        if (found >= phase.definition.amount) {
            phase.state = EventPhase.State.DONE;
        }
    }

    private void tickDefend(EventPhase phase) {
        ServerLevel level = services.server().overworld();
        int alive = services.quests().validator().countEntitiesIn(level, phase.definition.zone,
                phase.definition.objective);
        phase.score = Math.max(0, phase.definition.amount - alive);
        if (phase.score >= phase.definition.amount) {
            phase.state = EventPhase.State.DONE;
        }
    }

    private void tickSurvive(long worldTick, EventPhase phase) {
        long elapsed = worldTick - startedTick;
        phase.elapsedTicks = (int) Math.min(Integer.MAX_VALUE, elapsed);
        if (elapsed >= phase.definition.seconds * 20L) {
            phase.state = participantCount() >= phase.definition.requiredPlayers
                    ? EventPhase.State.DONE : EventPhase.State.FAILED;
        }
    }

    private void completePhase(EventPhase phase) {
        boolean ok = phase.state == EventPhase.State.DONE;
        for (ServerPlayer player : services.server().getPlayerList().getPlayers()) {
            services.visual().bars().update(player, 1.0F);
            if (ok) {
                player.sendSystemMessage(TextUtil.ok("\u0424\u0430\u0437\u0430 \u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043d\u0430: " + phase.title()));
            } else {
                player.sendSystemMessage(TextUtil.warning("\u0424\u0430\u0437\u0430 \u043f\u0440\u0435\u0440\u0432\u0430\u043b\u0430\u0441\u044c: " + phase.title()));
            }
        }

        if (phase.definition.globalChanges != null) {
            for (Map.Entry<String, Integer> entry : phase.definition.globalChanges.stats.entrySet()) {
                services.data().global().bumpStat(entry.getKey(), entry.getValue());
                services.data().touch();
            }
            for (String flag : phase.definition.globalChanges.flags) {
                services.data().global().flags.put(flag, "true");
            }
        }
        if (!phase.definition.onCompleteRumor.isEmpty()) {
            services.rumors().broadcast(definition.layer, phase.definition.onCompleteRumor);
        }

        currentPhase++;
        if (currentPhase >= phases.size()) {
            finish(ok);
            return;
        }
        EventPhase next = phases.get(currentPhase);
        next.state = EventPhase.State.RUNNING;
        startedTick = services.data().global().worldTick;
        announcePhase(next);
    }

    private void finish(boolean ok) {
        finished = true;
        succeeded = ok;
        services.story().onEventCompleted();
        if (ok) {
            for (UUID id : participants()) {
                ServerPlayer player = services.server().getPlayerList().getPlayer(id);
                if (player == null) {
                    continue;
                }
                PlayerData data = services.data().player(id);
                data.flags.put("event." + definition.id + ".done", "true");
                RewardService.apply(services, player, data, services.data().global(), definition.rewards, false);
            }
            for (String rumor : definition.rumorsOnEnd) {
                services.rumors().broadcast(definition.layer, rumor);
            }
            for (String flag : definition.globalFlags) {
                services.data().global().flags.put(flag, "true");
            }
            for (Map.Entry<String, Integer> entry : definition.globalChanges.entrySet()) {
                services.data().global().bumpStat(entry.getKey(), entry.getValue());
            }
        }
        services.data().touch();
        services.visual().bars().hideAll();
        services.story().chapters().recomputePoints(services.data().global());
    }

    public void stop() {
        finished = true;
        succeeded = false;
        services.visual().bars().hideAll();
    }

    public List<EventPhase> phases() {
        return phases;
    }

    public String statusLine() {
        if (finished) {
            return succeeded ? "\u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043d\u043e" : "\u043f\u0440\u0435\u0440\u0432\u0430\u043d\u043e";
        }
        EventPhase phase = current();
        if (phase == null) {
            return "\u043d\u0435 \u0437\u0430\u043f\u0443\u0449\u0435\u043d\u043e";
        }
        return phase.title() + " (" + (currentPhase + 1) + "/" + phases.size() + ")";
    }

    public Vec3 anchor() {
        com.example.kadma.zone.ZoneRegion zone = services.zones().byId(definition.zone);
        return zone == null ? null : zone.center();
    }

    public List<UUID> participants() {
        List<UUID> out = new ArrayList<>();
        for (Map.Entry<UUID, Boolean> entry : joined.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                out.add(entry.getKey());
            }
        }
        return out;
    }

    /** Next scheduled wall-clock start of this rite, or {@code null} when manual. */
    public LocalDateTime nextRun() {
        if (definition.manualOnly) {
            return null;
        }
        LocalDateTime moment = LocalDateTime.now();
        for (int step = 0; step < 14; step++) {
            LocalDateTime candidate = moment.plusDays(step).withHour(Math.max(0, Math.min(23, definition.hour)))
                    .withMinute(Math.max(0, Math.min(59, definition.minute))).withSecond(0).withNano(0);
            if (candidate.isAfter(moment) && candidate.getDayOfWeek() == parseDay(definition.weekday)) {
                return candidate;
            }
        }
        return null;
    }

    private static DayOfWeek parseDay(String raw) {
        if (raw == null) {
            return DayOfWeek.FRIDAY;
        }
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day.name().equalsIgnoreCase(raw)) {
                return day;
            }
        }
        return DayOfWeek.FRIDAY;
    }

    public LocalDate day() {
        return LocalDate.now();
    }

    public GlobalData world() {
        return services.data().global();
    }

    public Map<EventPhase.State, Integer> tally() {
        Map<EventPhase.State, Integer> out = new EnumMap<>(EventPhase.State.class);
        for (EventPhase phase : phases) {
            out.merge(phase.state, 1, Integer::sum);
        }
        return out;
    }
}
