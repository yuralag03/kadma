package com.example.kadma.visual;

import com.example.kadma.KadmaServices;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Boss bars for weekly rites and event phases. One bar per player so leaving or
 * joining during a rite never desynchronises progress.
 */
public class BossbarService {

    private final KadmaServices services;
    private final Map<UUID, net.minecraft.server.level.ServerBossEvent> bars = new HashMap<>();

    public BossbarService(KadmaServices services) {
        this.services = services;
    }

    public void show(ServerPlayer player, String heading, float progress, String color, String overlay) {
        if (player == null) {
            return;
        }
        net.minecraft.server.level.ServerBossEvent bar = bars.get(player.getUUID());
        if (bar == null) {
            bar = new net.minecraft.server.level.ServerBossEvent(
                    Component.literal(heading == null ? "" : heading).withStyle(net.minecraft.ChatFormatting.GOLD),
                    parseColor(color), parseOverlay(overlay));
            bars.put(player.getUUID(), bar);
            bar.addPlayer(player);
        }
        bar.setName(Component.literal(heading == null ? "" : heading).withStyle(net.minecraft.ChatFormatting.GOLD));
        bar.setProgress(clamp(progress));
        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
    }

    public void update(ServerPlayer player, float progress) {
        net.minecraft.server.level.ServerBossEvent bar = player == null ? null : bars.get(player.getUUID());
        if (bar != null) {
            bar.setProgress(clamp(progress));
        }
    }

    public void hide(ServerPlayer player) {
        if (player == null) {
            return;
        }
        net.minecraft.server.level.ServerBossEvent bar = bars.remove(player.getUUID());
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    public void hideAll() {
        for (net.minecraft.server.level.ServerBossEvent bar : bars.values()) {
            bar.removeAllPlayers();
        }
        bars.clear();
    }

    public boolean visible(ServerPlayer player) {
        return player != null && bars.containsKey(player.getUUID());
    }

    private static float clamp(float value) {
        if (value < 0.0F) {
            return 0.0F;
        }
        return Math.min(1.0F, value);
    }

    public static BossEvent.BossBarColor parseColor(String raw) {
        if (raw == null) {
            return BossEvent.BossBarColor.YELLOW;
        }
        try {
            return BossEvent.BossBarColor.valueOf(raw.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BossEvent.BossBarColor.YELLOW;
        }
    }

    public static BossEvent.BossBarOverlay parseOverlay(String raw) {
        if (raw == null) {
            return BossEvent.BossBarOverlay.PROGRESS;
        }
        try {
            return BossEvent.BossBarOverlay.valueOf(raw.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BossEvent.BossBarOverlay.PROGRESS;
        }
    }
}
