package com.example.kadma.npc;

import com.example.kadma.KadmaMod;
import com.example.kadma.KadmaServices;
import com.example.kadma.config.NpcConfig;
import com.example.kadma.data.PlayerData;
import com.example.kadma.dialogue.DialogueService;
import com.example.kadma.util.EntityUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * NPC bookkeeping. Keeps the mapping {@code npcId -> body UUID} so the mod can
 * interact with bodies without scanning the world on every click.
 */
public class NpcService {

    private final KadmaServices services;
    private final NpcSpawner spawner;
    private final Map<String, UUID> bodies = new HashMap<>();
    private final Map<String, Long> lastSpawn = new HashMap<>();

    public NpcService(KadmaServices services) {
        this.services = services;
        this.spawner = new NpcSpawner(services);
    }

    public NpcSpawner spawner() {
        return spawner;
    }

    public List<NpcConfig> configured() {
        return new ArrayList<>(services.config().npcs().values());
    }

    /** Spawn a configured NPC. */
    public boolean spawn(String npcId, ServerLevel level, ServerPlayer relativeTo) {
        NpcConfig config = services.config().npc(npcId);
        if (config == null || level == null) {
            return false;
        }
        UUID existing = bodies.get(npcId);
        if (existing != null && com.example.kadma.util.EntityUtil.alive(
                com.example.kadma.util.EntityUtil.byUuid(level, existing))) {
            // already alive in the world, nothing to do
            return true;
        }
        bodies.remove(npcId);
        UUID id = spawner.spawnConfigured(level, config, relativeTo);
        if (id == null) {
            KadmaMod.LOG.warn("[kadma] cannot spawn body for npc {} (world {}, spawn {})",
                    npcId, level.dimension().location(), config.spawn == null ? "unset" : config.spawn.world);
            return false;
        }
        bodies.put(npcId, id);
        lastSpawn.put(npcId, System.currentTimeMillis());
        return true;
    }

    public int spawnAll(ServerLevel level, ServerPlayer relativeTo) {
        int spawned = 0;
        for (NpcConfig config : services.config().npcs().values()) {
            UUID id = spawner.spawnConfigured(level, config, relativeTo);
            if (id != null) {
                bodies.put(config.id, id);
                spawned++;
            }
        }
        return spawned;
    }

    public int clear(ServerLevel level) {
        int removed = spawner.clear(level);
        bodies.clear();
        lastSpawn.clear();
        return removed;
    }

    public void clearAll() {
        if (services.server() == null) {
            return;
        }
        int total = 0;
        for (ServerLevel level : services.server().getAllLevels()) {
            total += clear(level);
        }
    }

    /** Restore tracking after a reload, without duplicating bodies. */
    public int rescan(ServerLevel level) {
        int found = 0;
        for (ArmorStand stand : spawner.findAll(level)) {
            String npcId = EntityUtil.npcId(stand);
            if (npcId != null && !npcId.isEmpty()) {
                bodies.put(npcId, stand.getUUID());
                found++;
            }
        }
        return found;
    }

    public ArmorStand body(ServerLevel level, String npcId) {
        UUID id = bodies.get(npcId);
        if (id != null) {
            net.minecraft.world.entity.Entity entity = com.example.kadma.util.EntityUtil.byUuid(level, id);
            if (entity instanceof ArmorStand stand) {
                return stand;
            }
            bodies.remove(npcId);
        }
        ArmorStand found = spawner.find(level, npcId);
        if (found != null) {
            bodies.put(npcId, found.getUUID());
        }
        return found;
    }

    /** Resolve the npc id behind an interact event. */
    public String idOf(net.minecraft.world.entity.Entity entity) {
        return EntityUtil.npcId(entity);
    }

    /** Opens the dialogue for an NPC, if the player is allowed to talk. */
    public boolean talk(ServerPlayer player, String npcId) {
        NpcConfig config = services.config().npc(npcId);
        if (config == null) {
            return false;
        }
        PlayerData data = services.data().player(player.getUUID());
        spawner.showMetLine(player, data, config);
        String dialogueId = config.dialogueId == null || config.dialogueId.isEmpty()
                ? config.dialogueRoot : config.dialogueId;
        if (dialogueId == null || dialogueId.isEmpty()) {
            player.sendSystemMessage(com.example.kadma.util.TextUtil.warning(
                    "\u042d\u0442\u043e\u0442 \u0433\u043e\u043b\u043e\u0441 \u043c\u043e\u043b\u0447\u0438\u0442."));
            return false;
        }
        return services.dialogues().open(player, dialogueId, null);
    }

    public int tracked() {
        return bodies.size();
    }

    public Map<String, UUID> snapshot() {
        return new HashMap<>(bodies);
    }

    public void rebuild() {
        bodies.clear();
    }
}
