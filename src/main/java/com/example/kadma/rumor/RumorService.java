package com.example.kadma.rumor;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.RumorConfig;
import com.example.kadma.data.GlobalData;
import com.example.kadma.data.PlayerData;
import com.example.kadma.util.TextUtil;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Rumors.
 *
 * <p>Rumors are present-time signals, never exposition. They contradict each other,
 * arrive from the world state, and can unlock side content or gate dialogue.</p>
 */
public class RumorService {

    private final KadmaServices services;
    private final Map<String, Long> lastFired = new HashMap<>();

    public RumorService(KadmaServices services) {
        this.services = services;
    }

    public int count() {
        return services.config().rumors().size();
    }

    public List<RumorConfig> all() {
        return new ArrayList<>(services.config().rumors().values());
    }

    /** Re-evaluate every rumor trigger. Called on the daily reset and on world changes. */
    public int refresh() {
        if (!services.config().settings().features.rumors) {
            return 0;
        }
        int fired = 0;
        GlobalData global = services.data().global();
        for (RumorConfig rumor : services.config().rumors().values()) {
            if (!fires(rumor, global)) {
                continue;
            }
            if (onCooldown(rumor)) {
                continue;
            }
            lastFired.put(rumor.id, System.currentTimeMillis());
            global.firedRumors.add(rumor.id);
            services.data().touch();
            for (ServerPlayer player : services.server().getPlayerList().getPlayers()) {
                if (matchesLayer(player, rumor)) {
                    deliver(global, player, rumor.id);
                    fired++;
                }
            }
        }
        return fired;
    }

    private boolean fires(RumorConfig rumor, GlobalData global) {
        switch (rumor.trigger) {
            case "stat_min":
                return !rumor.stat.isEmpty() && global.stat(rumor.stat) >= rumor.threshold;
            case "stat_max":
                return !rumor.stat.isEmpty() && global.stat(rumor.stat) <= rumor.threshold;
            case "global_flag":
                return !rumor.globalFlag.isEmpty() && global.flags.containsKey(rumor.globalFlag);
            case "event_completed":
                return !rumor.eventId.isEmpty() && global.flags.containsKey("event." + rumor.eventId + ".done");
            case "quest_completed":
                return !rumor.questId.isEmpty() && global.flags.containsKey("quest." + rumor.questId);
            case "contradiction":
                for (String other : rumor.contradicts) {
                    if (global.firedRumors.contains(other)) {
                        return true;
                    }
                }
                return false;
            case "shards_total":
                return global.stat("portal_shards_total") >= Math.max(1, rumor.threshold);
            default:
                return false;
        }
    }

    private boolean onCooldown(RumorConfig rumor) {
        Long previous = lastFired.get(rumor.id);
        if (previous == null) {
            return global(rumor);
        }
        long wait = Math.max(0L, rumor.cooldownMinutes) * 60_000L;
        if (System.currentTimeMillis() - previous < wait) {
            return true;
        }
        return global(rumor);
    }

    private boolean global(RumorConfig rumor) {
        return services.data().global().firedRumors.contains(rumor.id) && !rumor.allowRepeat && lastFired.containsKey(rumor.id);
    }

    private boolean matchesLayer(ServerPlayer player, RumorConfig rumor) {
        if (rumor.layer == null || rumor.layer.isEmpty()) {
            return true;
        }
        return services.layers().currentLayerId(player).equals(rumor.layer);
    }

    /** Teach one player a rumor and write it into their journal. */
    public boolean deliver(GlobalData global, ServerPlayer player, String rumorId) {
        RumorConfig rumor = services.config().rumor(rumorId);
        if (rumor == null || player == null) {
            return false;
        }
        PlayerData data = services.data().player(player.getUUID());
        boolean fresh = !data.knownRumor(rumorId);
        data.rememberRumor(rumorId);
        services.data().markPlayer(player.getUUID());
        if (fresh) {
            data.addJournal("\u0421\u043b\u0443\u0445: " + rumor.text);
            player.sendSystemMessage(TextUtil.info("\u0421\u043b\u0443\u0445: " + rumor.text));
            if (rumor.unlocks != null) {
                for (String unlock : rumor.unlocks) {
                    if (!unlock.isEmpty() && data.flags.putIfAbsent("rumor.unlock." + unlock, "true") == null) {
                        services.data().markPlayer(player.getUUID());
                    }
                }
            }
            if (rumor.contradicts != null) {
                for (String other : rumor.contradicts) {
                    if (data.knownRumor(other)) {
                        data.addJournal("\u0421\u043b\u0443\u0445\u0438 \u043f\u0440\u043e\u0442\u0438\u0432\u043e\u0440\u0435\u0447\u0430\u0442: " + rumor.id + " \u0438 " + other + ".");
                    }
                }
            }
        }
        return fresh;
    }

    /** Broadcast a rumor to everyone on a layer (or everywhere when the layer is blank). */
    public int broadcast(String layer, String rumorId) {
        int sent = 0;
        for (ServerPlayer player : services.server().getPlayerList().getPlayers()) {
            if (layer == null || layer.isEmpty()) {
                sent += deliver(services.data().global(), player, rumorId) ? 1 : 0;
                continue;
            }
            if (services.layers().currentLayerId(player).equals(layer)) {
                sent += deliver(services.data().global(), player, rumorId) ? 1 : 0;
            }
        }
        return sent;
    }

    /** Register a rumor that already happened, without showing it again. */
    public void announce(List<String> rumorIds) {
        if (rumorIds == null) {
            return;
        }
        for (String id : rumorIds) {
            if (id == null || id.isEmpty()) {
                continue;
            }
            RumorConfig rumor = services.config().rumor(id);
            if (rumor == null) {
                continue;
            }
            broadcast(rumor.layer, id);
        }
    }

    public String reliability(String rumorId) {
        RumorConfig rumor = services.config().rumor(rumorId);
        if (rumor == null || rumor.reliability == null) {
            return "unknown".toLowerCase(Locale.ROOT);
        }
        return rumor.reliability.toLowerCase(Locale.ROOT);
    }
}
