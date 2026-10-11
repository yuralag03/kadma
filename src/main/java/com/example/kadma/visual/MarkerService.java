package com.example.kadma.visual;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.QuestConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Quest markers built from TextDisplay entities.
 *
 * <p>Markers are bounded: they carry a TTL, are tracked in memory, are released when
 * the quest closes and are wiped by {@code /kadma marker clear}.</p>
 */
public class MarkerService {

    private final KadmaServices services;

    private final Map<String, UUID> active = new HashMap<>();

    public MarkerService(KadmaServices services) {
        this.services = services;
    }

    /** Place the marker described in quests.json, relative or absolute. */
    public void anchor(String questId, QuestConfig quest, ServerPlayer player) {
        if (quest == null || quest.marker == null || quest.marker.isEmpty()) {
            return;
        }
        Vec3 at = position(player, quest);
        if (at == null) {
            return;
        }
        place(questId, quest, at);
    }

    private Vec3 position(ServerPlayer player, QuestConfig quest) {
        if (quest.markerAbsolute) {
            return new Vec3(quest.markerX, quest.markerY, quest.markerZ);
        }
        if (player == null) {
            return null;
        }
        return new Vec3(quest.markerX + player.getX(), quest.markerY + player.getY(), quest.markerZ + player.getZ());
    }

    public void place(String questId, QuestConfig quest, Vec3 at) {
        if (at == null || services.server() == null || quest == null) {
            return;
        }
        ServerLevel level = services.server().overworld();
        Entity existing = find(questId);
        if (existing != null && existing.isAlive()) {
            if (existing.blockPosition().closerThan(BlockPos.containing(at), 2.0D)) {
                return;
            }
            existing.discard();
            active.remove(questId);
        }
        Display.TextDisplay display = services.visual().displays().spawn(level,
                at.x, at.y + 2.2D, at.z, quest.marker);
        if (display == null) {
            return;
        }
        display.setInvulnerable(true);
        display.setNoGravity(true);
        display.setSilent(true);
        long expiry = System.currentTimeMillis()
                + Math.max(1, services.config().settings().markerTtlSeconds) * 1000L;
        com.example.kadma.util.EntityUtil.markMarker(display, questId, expiry);
        active.put(questId, display.getUUID());
    }

    public Entity find(String questId) {
        if (questId == null || services.server() == null) {
            return null;
        }
        UUID id = active.get(questId);
        return id == null ? null : com.example.kadma.util.EntityUtil.byUuid(services.server().overworld(), id);
    }

    /** Drop a marker early, for instance when its quest closes. */
    public void release(String questId) {
        if (questId == null) {
            return;
        }
        Entity entity = find(questId);
        if (entity != null) {
            entity.discard();
        }
        active.remove(questId);
    }

    public void releaseAll() {
        for (String questId : new HashMap<>(active).keySet()) {
            release(questId);
        }
    }

    /** Remove markers whose TTL has passed. Bounded, called a couple of times a minute. */
    public int sweep() {
        if (services.server() == null) {
            return 0;
        }
        long now = System.currentTimeMillis();
        int removed = 0;
        Iterator<Map.Entry<String, UUID>> iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, UUID> entry = iterator.next();
            Entity entity = com.example.kadma.util.EntityUtil.byUuid(services.server().overworld(), entry.getValue());
            if (entity == null) {
                iterator.remove();
                continue;
            }
            net.minecraft.nbt.CompoundTag data = entity.getPersistentData();
            long expiry = data.getLong("kadma.marker_expiry");
            if (expiry > 0L && now > expiry) {
                entity.discard();
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    /** Rebuild tracking by reading tags off the world, used after a config reload. */
    public int rescan() {
        if (services.server() == null) {
            return 0;
        }
        int found = 0;
        for (ServerLevel level : services.server().getAllLevels()) {
            for (Entity entity : level.getEntities().getAll()) {
                String markerId = com.example.kadma.util.EntityUtil.markerId(entity);
                if (markerId == null || markerId.isEmpty()) {
                    continue;
                }
                if (entity instanceof Display.TextDisplay) {
                    active.put(markerId, entity.getUUID());
                    found++;
                }
            }
        }
        return found;
    }

    /** Hard clear, also removes markers left behind by an older config. */
    public int clearWorld() {
        int removed = 0;
        for (ServerLevel level : services.server().getAllLevels()) {
            for (Entity entity : level.getEntities().getAll()) {
                String markerId = com.example.kadma.util.EntityUtil.markerId(entity);
                if (markerId == null || markerId.isEmpty()) {
                    continue;
                }
                if (entity instanceof Display.TextDisplay) {
                    entity.discard();
                    removed++;
                }
            }
        }
        active.clear();
        return removed;
    }

    public int tracked() {
        return active.size();
    }
}
