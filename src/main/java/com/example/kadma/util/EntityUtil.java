package com.example.kadma.util;

import com.example.kadma.npc.NpcTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/** Safe entity/NBT helpers used by NPCs, markers and item tagging. */
public final class EntityUtil {

    private EntityUtil() {
    }

    /**
     * The NPC id carried by a mod spawned body, or {@code ""} when the entity is not
     * one.
     *
     * <p>There is exactly one reader for this tag ({@link NpcTag#read}) so the writer
     * ({@link #markNpc}) and the reader can never drift apart.</p>
     */
    public static String npcId(Entity entity) {
        String value = NpcTag.read(entity);
        return value == null ? "" : value;
    }

    /** True when the entity is an ArmorStand body owned by this mod. */
    public static boolean isKadmaNpc(Entity entity) {
        return entity instanceof ArmorStand && NpcTag.isNpc(entity);
    }

    public static String markerId(Entity entity) {
        if (entity == null) {
            return null;
        }
        CompoundTag data = entity.getPersistentData();
        if (data == null) {
            return null;
        }
        CompoundTag kadma = data.getCompound("kadma");
        if (kadma == null || kadma.isEmpty()) {
            return null;
        }
        if (!kadma.contains("marker")) {
            return null;
        }
        return kadma.getString("marker");
    }

    public static String questTargetId(Entity entity) {
        if (entity == null) {
            return null;
        }
        CompoundTag data = entity.getPersistentData();
        if (data == null) {
            return null;
        }
        CompoundTag kadma = data.getCompound("kadma");
        if (kadma == null || kadma.isEmpty()) {
            return null;
        }
        return kadma.contains("quest_target") ? kadma.getString("quest_target") : null;
    }

    /**
     * Writes the NPC id onto a body in both shapes:
     * <ul>
     *   <li>a nested {@code kadma: { npc: "nima" }} map, which is what
     *       {@link NpcTag#read} reads;</li>
     *   <li>a flat {@code kadma.npc} alias so {@code /data get entity} prints a
     *       readable value for operators.</li>
     * </ul>
     *
     * <p>The nested compound is built explicitly rather than obtained through
     * {@link CompoundTag#getCompound(String)}, which returns a <em>detached</em> empty
     * tag when the key is absent and therefore silently drops writes.</p>
     */
    public static void markNpc(Entity entity, String npcId) {
        if (entity == null) {
            return;
        }
        String value = npcId == null ? "" : npcId;
        CompoundTag data = entity.getPersistentData();

        CompoundTag nested = new CompoundTag();
        if (data.contains(NpcTag.TAG, CompoundTag.TAG_COMPOUND)) {
            CompoundTag existing = data.getCompound(NpcTag.TAG);
            nested = existing.copy();
        }
        nested.putString(NpcTag.NPC_FIELD, value);
        data.put(NpcTag.TAG, nested);

        data.putString(NpcTag.PATH, value);
    }

    public static void markMarker(Entity entity, String markerId, long expiryTick) {
        CompoundTag data = entity.getPersistentData();
        data.putString("kadma.marker", markerId == null ? "" : markerId);
        data.putLong("kadma.marker_expiry", expiryTick);
        CompoundTag nested = data.getCompound("kadma");
        nested.putString("marker", markerId == null ? "" : markerId);
        nested.putLong("marker_expiry", expiryTick);
        data.put("kadma", nested);
    }

    public static void markQuestTarget(Entity entity, String questId) {
        CompoundTag data = entity.getPersistentData();
        data.putString("kadma.quest_target", questId == null ? "" : questId);
        CompoundTag nested = data.getCompound("kadma");
        nested.putString("quest_target", questId == null ? "" : questId);
        data.put("kadma", nested);
    }

    public static Entity byUuid(ServerLevel level, UUID id) {
        if (id == null || level == null) {
            return null;
        }
        try {
            return level.getEntity(id);
        } catch (Exception ex) {
            return null;
        }
    }

    public static boolean isArmorStand(Entity entity) {
        return entity != null && EntityType.ARMOR_STAND.equals(entity.getType());
    }

    public static boolean isTextDisplay(Entity entity) {
        return entity != null && EntityType.TEXT_DISPLAY.equals(entity.getType());
    }

    public static boolean alive(Entity entity) {
        return entity != null && entity.isAlive();
    }
}
