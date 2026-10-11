package com.example.kadma.command;

import com.example.kadma.KadmaMod;
import com.example.kadma.KadmaServices;
import com.example.kadma.data.PlayerData;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-process smoke test for the command tree.
 *
 * <p>Runs a fixed sequence of commands through the real Brigadier dispatcher and
 * reports success or failure for each. It exists so an operator can verify a content
 * pack without a second player, and so the build can be regression tested without a
 * game client.</p>
 */
final class KadmaSelfTest {

    private final KadmaServices services;
    private final List<String> results = new ArrayList<>();

    private KadmaSelfTest(KadmaServices services) {
        this.services = services;
    }

    static int run(KadmaServices services, CommandContext<net.minecraft.commands.CommandSourceStack> context) {
        return new KadmaSelfTest(services).execute(context.getSource());
    }

    private int execute(CommandSourceStack source) {
        MinecraftServer server = services.server();
        results.clear();

        int npcsBefore = countNpcs(server);
        int questsBefore = services.config().quests().size();

        check("kadma admin reload", "kadma admin reload");
        check("kadma (root help)", "kadma");
        check("kadma admin flag global set", "kadma admin flag global set selftest 1");
        check("kadma admin flag global remove", "kadma admin flag global remove selftest");
        check("kadma admin npc spawn nima", "kadma admin npc spawn nima");
        check("kadma admin npc spawn mira", "kadma admin npc spawn mira");
        check("kadma admin npc spawnall", "kadma admin npc spawnall");
        check("kadma admin event start", "kadma admin event start ritual_light");
        check("kadma admin event stop", "kadma admin event stop");
        check("kadma admin rumor add", "kadma admin rumor add wall_breathes");
        check("kadma admin debug on", "kadma admin debug on");
        check("kadma admin debug off", "kadma admin debug off");
        check("kadma admin marker clear", "kadma admin marker clear");
        check("kadma admin npc clear", "kadma admin npc clear");
        check("kadma admin save", "kadma admin save");
        // Player-only leaves ("kadma player world", flags on a player) are covered by
        // the service level checks below, since the console has no @s player.

        // ---- NBT round trip: the writer and the reader must agree.
        // A body is spawned into a force loaded chunk, read back through both entry
        // points, and then removed through the very same path the interact handler and
        // "/kadma admin npc clear" rely on.
        check("npc nbt round trip", () -> {
            com.example.kadma.npc.NpcSpawner spawner = services.npcs().spawner();
            com.example.kadma.config.NpcConfig config = services.config().npc("nima");
            if (config == null) {
                KadmaMod.LOG.warn("[kadma] selftest: no nima config");
                return false;
            }
            ServerLevel level = server.overworld();
            java.util.UUID bodyId = spawner.spawnConfigured(level, config, null);
            if (bodyId == null) {
                KadmaMod.LOG.warn("[kadma] selftest: spawnConfigured returned null");
                return false;
            }
            net.minecraft.world.entity.Entity body = level.getEntity(bodyId);
            if (body == null) {
                KadmaMod.LOG.warn("[kadma] selftest: body {} is not in the level", bodyId);
                return false;
            }
            String viaEntityUtil = com.example.kadma.util.EntityUtil.npcId(body);
            String viaNpcTag = com.example.kadma.npc.NpcTag.read(body);
            boolean isKadmaNpc = com.example.kadma.util.EntityUtil.isKadmaNpc(body);
            KadmaMod.LOG.info("[kadma] selftest nbt: EntityUtil.npcId='{}' NpcTag.read='{}' isKadmaNpc={}",
                    viaEntityUtil, viaNpcTag, isKadmaNpc);

            boolean idsMatch = "nima".equals(viaEntityUtil) && "nima".equals(viaNpcTag);
            boolean recognisable = isKadmaNpc && spawner.find(level, "nima") != null;
            if (!idsMatch || !recognisable) {
                return false;
            }

            int before = spawner.findAll(level).size();
            int removed = spawner.clear(level);
            boolean bodyGone = level.getEntity(bodyId) == null;
            boolean clearWorked = removed == before && removed >= 1;
            KadmaMod.LOG.info("[kadma] selftest clear: before={} removed={} bodyGone={}",
                    before, removed, bodyGone);
            return clearWorked && bodyGone;
        });

        int npcsAfter = countNpcs(server);

        // Service level checks that do not need a player.
        check("player data lifecycle", () -> {
            UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
            PlayerData data = services.data().player(id);
            data.flags.put("selftest", "true");
            services.data().markPlayer(id);
            services.data().saveAll();
            return services.data().peek(id) != null && services.data().peek(id).flagIs("selftest", "true");
        });

        check("prozrenie cap", () -> {
            UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
            PlayerData data = services.data().player(id);
            int before = services.story().prozrenieOf(data);
            int max = services.story().prozrenieMax(data);
            services.story().prozrenie().set(null, data, 99);
            int capped = services.story().prozrenieOf(data);
            services.story().prozrenie().set(null, data, Math.min(before, max));
            return capped == max && max >= 1;
        });

        check("quest definition valid", () -> {
            for (com.example.kadma.config.QuestConfig quest : services.config().quests().values()) {
                if (!quest.usable()) {
                    KadmaMod.LOG.warn("[kadma] unusable quest {}", quest.id);
                    return false;
                }
            }
            return true;
        });

        check("dialogue trees parse", () -> {
            for (com.example.kadma.config.DialogueConfig dialogue : services.config().dialogues().values()) {
                if (dialogue.nodes.isEmpty() || dialogue.root() == null) {
                    KadmaMod.LOG.warn("[kadma] dialogue {} has no root", dialogue.id);
                    return false;
                }
            }
            return true;
        });

        check("hud payload builds", () -> {
            UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
            com.example.kadma.hud.HudPayload payload =
                    services.hud().build(services.data().player(id), "selftest-layer");
            return payload.toJson().contains("layer");
        });


        check("layer resolution", () -> {
            List<String> ids = services.layers().ids();
            for (String id : new String[]{"lower", "craft", "trade", "temple"}) {
                if (!ids.contains(id)) {
                    return false;
                }
            }
            return true;
        });

        check("zone resolution", () -> services.zones().all().containsKey("portal_clearing"));

        check("world state persisted", () -> {
            com.example.kadma.data.GlobalData global = services.data().global();
            return global.stat("faith") > 0 && "act0_threshold".equals(global.unlockedChapter);
        });

        check("catch-up quest exists", () -> services.config().quest("catchup_lower_after_shards") != null);

        check("aftermath quest exists", () -> services.config().quest("aftermath_lower_channel") != null);

        // Report
        int ok = 0;
        for (String line : results) {
            if (line.startsWith("[ok]")) {
                ok++;
            } else {
                KadmaMod.LOG.warn("[kadma] selftest: {}", line);
            }
        }
        String summary = "\u0421\u0430\u043c\u043e\u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0430: " + ok + "/" + results.size()
                + " \u043f\u0440\u043e\u0432\u0435\u0440\u043e\u043a, NPC \u0434\u043e/\u043f\u043e\u0441\u043b\u0435: " + npcsBefore + "/" + npcsAfter
                + ", \u0437\u0430\u043f\u0438\u0441\u0435\u0439 \u0432 \u043a\u043e\u043d\u0444\u0438\u0433\u0435: " + questsBefore;
        source.sendSuccess(() -> com.example.kadma.util.TextUtil.ok(summary), false);
        KadmaMod.LOG.info("[kadma] selftest {}", summary);
        return ok == results.size() ? 1 : 0;
    }

    private int countNpcs(MinecraftServer server) {
        int total = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (net.minecraft.world.entity.Entity entity : level.getEntities().getAll()) {
                if (entity instanceof ArmorStand stand && com.example.kadma.npc.NpcTag.isNpc(stand)) {
                    total++;
                }
            }
        }
        return total;
    }

    private void check(String name, String command) {
        try {
            int code = services.server().getCommands().performPrefixedCommand(
                    services.server().createCommandSourceStack().withSuppressedOutput(), command);
            record(name, code > 0);
        } catch (Exception ex) {
            KadmaMod.LOG.warn("[kadma] selftest command '{}' threw", command, ex);
            record(name, false);
        }
    }

    private void check(String name, java.util.function.BooleanSupplier test) {
        boolean value;
        try {
            value = test.getAsBoolean();
        } catch (Exception ex) {
            KadmaMod.LOG.warn("[kadma] selftest '{}' threw", name, ex);
            value = false;
        }
        record(name, value);
    }

    private void record(String name, boolean passed) {
        results.add((passed ? "[ok] " : "[FAIL] ") + name);
    }
}
