package com.example.kadma.lobby;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.ModSettings;
import com.example.kadma.data.PlayerData;
import com.example.kadma.util.TextUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The lobby bridge "Порог".
 *
 * <p>The lobby is a narrative space, not a menu. It moves players between the lobby
 * and the city, speaks with the Stranger, closes a cycle and hands out cycle rewards
 * without ever destroying the player's history.</p>
 */
public class LobbyBridgeService {

    private final KadmaServices services;

    public LobbyBridgeService(KadmaServices services) {
        this.services = services;
    }

    private ModSettings settings() {
        return services.config().settings();
    }

    private ServerLevel lobbyLevel(MinecraftServer server) {
        ModSettings.LobbyPoint point = settings().lobby;
        ResourceLocation id = ResourceLocation.tryParse(point.world == null ? "" : point.world);
        if (id == null) {
            return server.overworld();
        }
        return resolve(server, id);
    }

    private ServerLevel resolve(MinecraftServer server, ResourceLocation id) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().equals(id)) {
                return level;
            }
        }
        return server.overworld();
    }

    private ServerLevel cityLevel(MinecraftServer server) {
        ModSettings.LobbyPoint point = settings().citySpawn;
        ResourceLocation id = ResourceLocation.tryParse(point.world == null ? "" : point.world);
        if (id == null) {
            return server.overworld();
        }
        return resolve(server, id);
    }

    /** Teleport the player into the lobby. */
    public boolean toLobby(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        ModSettings.LobbyPoint point = settings().lobby;
        ServerLevel level = lobbyLevel(server);
        Vec3 at = new Vec3(point.x, point.y, point.z);
        if (!point.valid()) {
            at = Vec3.atBottomCenterOf(level.getSharedSpawnPos());
        }
        player.teleportTo(level, at.x, at.y, at.z, (float) point.yaw, (float) point.pitch);
        PlayerData data = services.data().player(player.getUUID());
        data.flags.put("lobby.entered", "true");
        services.data().markPlayer(player.getUUID());
        player.sendSystemMessage(TextUtil.info("\u041f\u043e\u0440\u043e\u0433. \u0417\u0434\u0435\u0441\u044c \u043d\u0435\u0442 \u043d\u0438 \u0432\u0440\u0435\u043c\u0435\u043d\u0438, \u043d\u0438 \u0438\u043c\u0435\u043d\u0438."));
        return true;
    }

    /** Move the player from the lobby into the city. */
    public boolean enterCity(ServerPlayer player) {
        if (player == null || !settings().features.lobbyBridge) {
            return false;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        ModSettings.LobbyPoint point = settings().citySpawn;
        ServerLevel level = cityLevel(server);
        Vec3 at = new Vec3(point.x, point.y, point.z);
        player.teleportTo(level, at.x, at.y, at.z, (float) point.yaw, (float) point.pitch);
        PlayerData data = services.data().player(player.getUUID());
        data.enteredCity = true;
        services.data().markPlayer(player.getUUID());
        services.story().onboard(player, data);
        services.visual().title(player, "\u041a\u0430\u0434\u043c\u0430", "\u0412\u0435\u0440\u0442\u0438\u043a\u0430\u043b\u044c\u043d\u044b\u0439 \u0433\u043e\u0440\u043e\u0434", 10, 60, 20);
        player.sendSystemMessage(TextUtil.info(
                "\u0422\u044b \u0432\u043d\u0443\u0442\u0440\u0438 \u0433\u043e\u0440\u043e\u0434\u0430. \u0421\u0442\u0435\u043d\u044b \u043c\u043e\u043a\u0440\u044b\u0435, \u0432\u043e\u0437\u0434\u0443\u0445 \u0442\u044f\u0436\u0451\u043b\u044b\u0439."));
        services.story().catchUp().offer(player, data);
        return true;
    }

    /**
     * First login nudge: a player who never entered the city is put in the lobby,
     * because the whole story opens on the Threshold.
     */
    public boolean enterIfNeeded(ServerPlayer player, PlayerData data) {
        if (data.enteredCity || !settings().features.lobbyBridge) {
            return false;
        }
        if (data.flags.containsKey("lobby.skipped")) {
            return false;
        }
        if (inLobby(player)) {
            return true;
        }
        return toLobby(player);
    }

    /** Speak with the Stranger once the player already stands in the lobby. */
    public boolean talkToStranger(ServerPlayer player) {
        PlayerData data = services.data().player(player.getUUID());
        String dialogue = "stranger_threshold";
        boolean opened = services.dialogues().open(player, dialogue, null);
        if (!opened) {
            player.sendSystemMessage(TextUtil.warning("\u041d\u0435\u0437\u043d\u0430\u043a\u043e\u043c\u044b\u0439 \u043d\u0435 \u0433\u043e\u0432\u043e\u0440\u0438\u0442."));
            return false;
        }
        data.flags.put("npc.met.stranger", "true");
        services.data().markPlayer(player.getUUID());
        return true;
    }

    /** Return the player to the lobby after an arc is complete. */
    public boolean completeCycle(ServerPlayer player) {
        PlayerData data = services.data().player(player.getUUID());
        boolean entered = data.enteredCity;
        if (!entered) {
            player.sendSystemMessage(TextUtil.warning(
                    "\u0422\u044b \u0435\u0449\u0451 \u043d\u0435 \u0432\u0445\u043e\u0434\u0438\u043b \u0432 \u0433\u043e\u0440\u043e\u0434."));
            return false;
        }
        data.completedCycles++;
        data.flags.put("cycle_completed", "true");
        if (!data.sparkTitle) {
            data.sparkTitle = true;
            data.flags.put("title.spark", "true");
            services.visual().title(player, "\u0418\u0441\u043a\u0440\u0430", "\u0422\u0435\u0431\u044f \u0442\u0430\u043a \u043d\u0430\u0437\u044b\u0432\u0430\u044e\u0442", 10, 70, 20);
        }
        data.addJournal("\u0426\u0438\u043a\u043b " + data.completedCycles + " \u0437\u0430\u043a\u0440\u044b\u0442. \u0413\u043e\u0440\u043e\u0434 \u0433\u043e\u0432\u043e\u0440\u0438\u0442 \u043e\u0431 \u044d\u0442\u043e\u043c.");
        services.data().markPlayer(player.getUUID());
        services.data().touch();
        services.journal().noteCompletedCycle(data, data.completedCycles);
        grantCycleReward(player, data);
        player.sendSystemMessage(TextUtil.ok("\u0426\u0438\u043a\u043b \u0437\u0430\u0432\u0435\u0440\u0448\u0451\u043d. \u041f\u043e\u0440\u043e\u0433 \u043e\u0442\u043a\u0440\u044b\u0442."));
        return true;
    }

    /** Cycle reward bundle. Meta only, never pays out the main story twice. */
    public void grantCycleReward(ServerPlayer player, PlayerData data) {
        com.example.kadma.config.RewardDefinition reward = new com.example.kadma.config.RewardDefinition();
        reward.prozrenie = Math.max(1, settings().prozrenieRegenPerDay);
        reward.experience = 25 * Math.max(1, data.completedCycles);
        reward.journal = java.util.List.of(
                "\u041d\u0430\u0433\u0440\u0430\u0434\u0430 \u0437\u0430 \u0446\u0438\u043a\u043b: \u0437\u043d\u0430\u043d\u0438\u0435, \u043a\u043e\u0442\u043e\u0440\u043e\u0435 \u043d\u0435 \u0441\u0442\u0438\u0440\u0430\u0435\u0442\u0441\u044f.");
        com.example.kadma.quest.RewardService.apply(services, player, data, services.data().global(), reward, false);
    }

    /**
     * Optional cycle reset. Keeps player history and meta flags, moves the story back
     * to the first chapter so the vertical city can be walked again.
     */
    public boolean resetCycle(ServerPlayer player) {
        PlayerData data = services.data().player(player.getUUID());
        if (data.completedCycles <= 0) {
            player.sendSystemMessage(TextUtil.warning(
                    "\u0426\u0438\u043a\u043b \u0435\u0449\u0451 \u043d\u0435 \u0437\u0430\u043a\u0440\u044b\u0442. \u0421\u0431\u0440\u0430\u0441\u044b\u0432\u0430\u0442\u044c \u043d\u0435\u0447\u0435\u0433\u043e."));
            return false;
        }
        data.enteredCity = false;
        data.stage = 0;
        data.chapter = firstChapter();
        data.activeQuests.clear();
        data.completedQuests.clear();
        data.flags.clear();
        data.flags.put("cycle_completed", "true");
        data.flags.put("title.spark", "true");
        services.data().markPlayer(player.getUUID());
        player.sendSystemMessage(TextUtil.info(
                "\u0426\u0438\u043a\u043b \u0441\u0431\u0440\u043e\u0448\u0435\u043d. \u0418\u0441\u0442\u043e\u0440\u0438\u044f \u043d\u0430\u0447\u0438\u043d\u0430\u0435\u0442\u0441\u044f \u0441\u043d\u0430\u0447\u0430\u043b\u0430, "
                        + "\u043f\u0430\u043c\u044f\u0442\u044c \u043e\u0441\u0442\u0430\u0451\u0442\u0441\u044f."));
        return true;
    }

    private String firstChapter() {
        com.example.kadma.config.ChapterConfig chapter = services.config().firstChapter();
        return chapter == null ? "act0_threshold" : chapter.id;
    }

    /** Whether the player is currently standing in the lobby world. */
    public boolean inLobby(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        ServerLevel lobby = lobbyLevel(player.getServer());
        return lobby != null && player.level() == lobby;
    }
}
