package com.example.kadma.hud;

import com.example.kadma.KadmaServices;
import com.example.kadma.data.PlayerData;
import com.example.kadma.network.KadmaNetwork;
import com.example.kadma.network.HudSyncPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Builds and pushes the HUD payload.
 *
 * <p>Runs on the server thread at a configurable cadence (default every 30 ticks) so
 * a busy server never pays for per-tick network traffic.</p>
 */
public class HudSyncServer {

    private final KadmaServices services;
    private long counter = 0L;

    public HudSyncServer(KadmaServices services) {
        this.services = services;
    }

    public long counter() {
        return counter;
    }

    /** Push a payload to one player. */
    public void send(ServerPlayer player, PlayerData data) {
        if (player == null || data == null) {
            return;
        }
        HudPayload payload = build(data, services.layers().currentLayerDisplay(player));
        payload.visible = data.hudEnabled && services.config().settings().features.hud;
        KadmaNetwork.HudSyncPacket packet = HudSyncPayload.text(payload.toJson(), payload.visible);
        KadmaNetwork.sendToPlayer(player, packet);
    }

    /** Push to every loaded online player. */
    public int broadcast() {
        int sent = 0;
        for (ServerPlayer player : services.server().getPlayerList().getPlayers()) {
            PlayerData data = services.data().player(player.getUUID());
            send(player, data);
            sent++;
        }
        return sent;
    }

    public HudPayload build(PlayerData data, String layerName) {
        HudPayload payload = new HudPayload();
        com.example.kadma.config.ChapterConfig chapter = services.config().chapter(data.chapter);
        payload.chapter = chapter == null ? data.chapter : chapter.title;
        payload.layer = layerName;
        payload.stage = data.stage;
        payload.spark = services.data().global().stat("spark");
        payload.faith = services.data().global().stat("faith");
        payload.corruption = services.data().global().stat("corruption");
        payload.schism = services.data().global().stat("schism");
        payload.event = services.data().global().activeEvent;
        payload.rumors = data.unreadRumorCount();
        payload.prozrenie = services.story().prozrenieOf(data);
        payload.maxProzrenie = services.story().prozrenieMax(data);

        com.example.kadma.quest.QuestInstance primary = services.quests().primary(data);
        if (primary == null) {
            payload.quest = "";
            payload.objective = "";
            payload.progress = "";
        } else {
            payload.quest = primary.title();
            payload.objective = primary.hudObjective();
            payload.progress = primary.hudProgress();
        }
        return payload;
    }

    /** Cadence driver called from the server tick. */
    public void tick() {
        counter++;
        if (!services.config().settings().features.hud) {
            return;
        }
        int period = Math.max(10, services.config().settings().hudUpdateTicks);
        if (counter % (long) period != 0L) {
            return;
        }
        broadcast();
    }

    public boolean enabled(UUID id) {
        PlayerData data = services.data().peek(id);
        return data == null || data.hudEnabled;
    }

    public void setEnabled(UUID id, boolean value) {
        PlayerData data = services.data().peek(id);
        if (data == null) {
            return;
        }
        data.hudEnabled = value;
        services.data().markPlayer(id);
    }
}
