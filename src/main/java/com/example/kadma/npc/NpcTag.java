package com.example.kadma.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/**
 * Helpers for the persistent NBT tag the mod writes onto NPC bodies.
 *
 * <p>Shape on disk:</p>
 *
 * <pre>
 * kadma: { npc: "nima" }
 * </pre>
 *
 * <p>The mod also writes a flat {@code kadma.npc} alias so {@code /data get}
 * prints a readable value in vanilla. {@link #read(Entity)} prefers the flat key and
 * falls back to the nested map, which is what {@link com.example.kadma.util.EntityUtil}
 * reads.</p>
 */
public final class NpcTag {

    public static final String TAG = "kadma";
    public static final String NPC_FIELD = "npc";
    public static final String PATH = "kadma.npc";

    private NpcTag() {
    }

    /**
     * Read the NPC id from any entity carrying the tag.
     *
     * <p>This is the single source of truth for the tag layout. The nested
     * {@code kadma: { npc: ... }} map is canonical; the flat {@code kadma.npc} alias
     * is accepted as a fallback for bodies tagged by hand.</p>
     *
     * @return the NPC id, or {@code null} when the entity is not a mod NPC.
     */
    public static String read(Entity entity) {
        if (entity == null) {
            return null;
        }
        CompoundTag data = entity.getPersistentData();
        if (data == null) {
            return null;
        }
        CompoundTag nested = data.getCompound(TAG);
        if (!nested.isEmpty() && nested.contains(NPC_FIELD)) {
            String value = nested.getString(NPC_FIELD);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        if (data.contains(PATH)) {
            String value = data.getString(PATH);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    /** True when the entity belongs to this NPC definition. */
    public static boolean is(Entity entity, String npcId) {
        return npcId != null && npcId.equals(read(entity));
    }

    /** True when the entity is a mod spawned NPC at all. */
    public static boolean isNpc(Entity entity) {
        return read(entity) != null;
    }
}
