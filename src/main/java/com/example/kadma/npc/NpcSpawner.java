package com.example.kadma.npc;

import com.example.kadma.KadmaMod;
import com.example.kadma.KadmaServices;
import com.example.kadma.config.NpcConfig;
import com.example.kadma.data.PlayerData;
import com.example.kadma.util.EntityUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * NPC bodies built from vanilla ArmorStands with a TextDisplay label above them.
 *
 * <p>No custom entity types, no models, no textures. Every spawned body carries the
 * persistent tag {@code kadma.npc=<npcId>} so the mod can find it again after a reload
 * and so an operator can identify it in NBT editors.</p>
 *
 * <p>World scans go through {@code level.getEntities().getAll()} rather than a giant
 * bounding box: a box sized to the world border is huge enough to overflow the
 * entity-section index and crash the server.</p>
 */
public class NpcSpawner {

    private final KadmaServices services;

    public NpcSpawner(KadmaServices services) {
        this.services = services;
    }

    /**
     * Spawn one NPC at the requested position.
     *
     * @return the body UUID, or {@code null} when the spawn failed.
     */
    public UUID spawn(ServerLevel level, NpcConfig config, Vec3 at, float yaw) {
        if (level == null || config == null || at == null) {
            return null;
        }
        ArmorStand stand = new ArmorStand(net.minecraft.world.entity.EntityType.ARMOR_STAND, level);
        stand.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        stand.setCustomNameVisible(true);
        stand.setCustomName(net.minecraft.network.chat.Component.literal(config.displayName)
                .withStyle(net.minecraft.ChatFormatting.YELLOW));
        stand.setInvulnerable(true);
        stand.setNoGravity(true);
        stand.setNoBasePlate(true);
        stand.setSilent(true);
        stand.setInvisible(config.invisible);
        stand.setShowArms(!config.invisible);
        EntityUtil.markNpc(stand, config.id);

        String head = config.headItem == null ? "" : config.headItem;
        if (!head.isEmpty()) {
            Item item = com.example.kadma.util.InventoryUtil.resolveItem(head);
            if (item != null) {
                stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(item));
            }
        }

        ensureChunkLoaded(level, at);
        KadmaMod.LOG.debug("[kadma] adding npc body {} at {} / {} / {}", config.id, at.x, at.y, at.z);
        if (!level.addFreshEntity(stand)) {
            KadmaMod.LOG.warn("[kadma] addFreshEntity refused the npc body {} at {} {} {}", config.id, at.x, at.y, at.z);
            stand.discard();
            return null;
        }
        releaseChunkTicket(level, at);

        if (config.title != null && !config.title.isEmpty()) {
            services.visual().displays().spawn(level, at.x, at.y + 2.05D, at.z,
                    net.minecraft.network.chat.Component.literal(config.title)
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        return stand.getUUID();
    }

    /** Spawn at the configured anchor of an NPC, falling back to a player-relative point. */
    public UUID spawnConfigured(ServerLevel level, NpcConfig config, ServerPlayer relativeTo) {
        Vec3 at = resolvePosition(level, config, relativeTo);
        if (at == null) {
            return null;
        }
        return spawn(level, config, at, 0.0F);
    }

    private Vec3 resolvePosition(ServerLevel level, NpcConfig config, ServerPlayer relativeTo) {
        if (config.spawn != null && config.spawn.hasWorld()) {
            net.minecraft.resources.ResourceLocation worldId =
                    net.minecraft.resources.ResourceLocation.tryParse(config.spawn.world);
            if (worldId == null || !worldId.equals(level.dimension().location())) {
                KadmaMod.LOG.warn("[kadma] npc {} spawn world '{}' does not match '{}'",
                        config.id, config.spawn.world, level.dimension().location());
                // fall through to the relative anchor instead of refusing to spawn
            } else {
                return new Vec3(config.spawn.x, config.spawn.y, config.spawn.z);
            }
        }
        if (relativeTo != null) {
            return relativeTo.position().add(1.5D, 0.0D, 0.0D);
        }
        return Vec3.atBottomCenterOf(level.getSharedSpawnPos().above());
    }

    /**
     * Make sure the chunk holding {@code at} exists before the body is added. Vanilla
     * silently drops entities whose section is not loaded, so the mod keeps them by
     * force-loading the target section and remembering it, then releases the ticket.
     */
    private void ensureChunkLoaded(ServerLevel level, Vec3 at) {
        if (level == null || at == null) {
            return;
        }
        int chunkX = net.minecraft.util.Mth.floor(at.x) >> 4;
        int chunkZ = net.minecraft.util.Mth.floor(at.z) >> 4;
        try {
            level.setChunkForced(chunkX, chunkZ, true);
            // The chunk now loads asynchronously; getChunk ensures a synchronous read
            // so addFreshEntity lands inside a live section.
            level.getChunk(chunkX, chunkZ);
        } catch (Exception ex) {
            KadmaMod.LOG.debug("[kadma] cannot force chunk {} {} for an NPC body", chunkX, chunkZ, ex);
        }
    }

    private void releaseChunkTicket(ServerLevel level, Vec3 at) {
        if (level == null || at == null) {
            return;
        }
        int chunkX = net.minecraft.util.Mth.floor(at.x) >> 4;
        int chunkZ = net.minecraft.util.Mth.floor(at.z) >> 4;
        try {
            level.setChunkForced(chunkX, chunkZ, false);
        } catch (Exception ex) {
            KadmaMod.LOG.debug("[kadma] cannot release chunk {} {}", chunkX, chunkZ, ex);
        }
    }

    /** Resolve an existing body through the persistent tag. */
    public ArmorStand find(ServerLevel level, String npcId) {
        if (level == null || npcId == null || npcId.isEmpty()) {
            return null;
        }
        for (Entity entity : loaded(level)) {
            if (entity instanceof ArmorStand stand && npcId.equals(EntityUtil.npcId(stand))) {
                return stand;
            }
        }
        return null;
    }

    /** Every mod spawned body currently loaded in the level. */
    public List<ArmorStand> findAll(ServerLevel level) {
        List<ArmorStand> out = new ArrayList<>();
        if (level == null) {
            return out;
        }
        for (Entity entity : loaded(level)) {
            if (entity instanceof ArmorStand stand && !EntityUtil.npcId(stand).isEmpty()) {
                out.add(stand);
            }
        }
        return out;
    }

    /** Remove every mod-spawned NPC body in a level. */
    public int clear(ServerLevel level) {
        if (level == null) {
            return 0;
        }
        int removed = 0;
        for (Entity entity : loaded(level)) {
            String npcId = EntityUtil.npcId(entity);
            if (npcId == null || npcId.isEmpty()) {
                continue;
            }
            entity.discard();
            removed++;
        }
        return removed;
    }

    /** Restore name and body state of an existing stand, used after reloads. */
    public void refresh(ServerLevel level, NpcConfig config) {
        if (config == null) {
            return;
        }
        for (Entity entity : loaded(level)) {
            if (!(entity instanceof ArmorStand stand)) {
                continue;
            }
            if (!config.id.equals(EntityUtil.npcId(stand))) {
                continue;
            }
            stand.setCustomNameVisible(true);
            stand.setCustomName(net.minecraft.network.chat.Component.literal(config.displayName)
                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
            stand.setInvulnerable(true);
            stand.setNoGravity(true);
            stand.setNoBasePlate(true);
            stand.setSilent(true);
        }
    }

    public void showMetLine(ServerPlayer player, PlayerData data, NpcConfig config) {
        String flag = "npc.met." + config.id;
        if (!data.flag(flag)) {
            data.flags.put(flag, "true");
            services.data().markPlayer(player.getUUID());
        }
        player.sendSystemMessage(com.example.kadma.util.TextUtil.speaker(config.displayName,
                config.title == null || config.title.isEmpty()
                        ? "\u043e\u043d\u0430 \u0441\u043c\u043e\u0442\u0440\u0438\u0442 \u043d\u0430 \u0442\u0435\u0431\u044f"
                        : config.title));
    }

    /** Look up the dialogue id configured for a body. */
    public String dialogueOf(ArmorStand stand) {
        String npcId = EntityUtil.npcId(stand);
        if (npcId == null) {
            return null;
        }
        NpcConfig config = services.config().npc(npcId);
        if (config == null) {
            return null;
        }
        if (config.dialogueId != null && !config.dialogueId.isEmpty()) {
            return config.dialogueId;
        }
        return config.dialogueRoot;
    }

    public UUID id(ArmorStand stand) {
        return stand == null ? null : stand.getUUID();
    }

    /**
     * Loaded entities, without a bounding box. Never throws: the entity section
     * storage is only guaranteed while the level is running, so a level that is
     * already tearing down simply reports nothing.
     */
    private static Iterable<Entity> loaded(ServerLevel level) {
        try {
            return level.getEntities().getAll();
        } catch (Exception ex) {
            return java.util.List.of();
        }
    }
}
