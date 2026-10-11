package com.example.kadma;

import com.example.kadma.config.ConfigManager;
import com.example.kadma.data.PlayerData;
import com.example.kadma.util.TextUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Server-side Forge event bridge.
 *
 * <p>Every handler here runs on the server thread, which is what makes the mod
 * authoritative: no client packet can reach these code paths directly. Listeners are
 * registered explicitly from {@link KadmaMod} so a dedicated server never loads the
 * client subscriber and nothing is subscribed twice.</p>
 */
public final class KadmaEvents {

    private KadmaEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        KadmaRuntime runtime = KadmaBootstrap.runtime(server);
        runtime.registerCommands(server.getCommands().getDispatcher());
        if (runtime.config().settings().verboseLogging) {
            KadmaMod.LOG.info("[kadma] server starting, commands registered");
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        KadmaRuntime runtime = KadmaBootstrap.runtime(server);
        runtime.registerCommands(server.getCommands().getDispatcher());
        runtime.daily().reset(runtime.data().global().lastDailyResetDay);
        runtime.npcs().spawnAll(server.overworld(), null);
        runtime.visual().markers().rescan();
        runtime.hud().broadcast();
        if (server.getPlayerList().getPlayerCount() > 0) {
            for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
                PlayerData data = runtime.data().player(player.getUUID());
                runtime.hud().send(player, data);
            }
        }
        KadmaMod.LOG.info("[kadma] server started: {} players online", server.getPlayerList().getPlayerCount());
        maybeSelfTest(runtime, server);
    }

    /**
     * Runs the in-process command smoke test when the launch requests it with
     * {@code -Dkadma.selftest=1}. Used by the integration harness so the real
     * Brigadier dispatcher is exercised without a game client. The server shuts
     * itself down once the report is printed.
     */
    private static void maybeSelfTest(KadmaRuntime runtime, MinecraftServer server) {
        if (!"1".equals(System.getProperty("kadma.selftest"))) {
            return;
        }
        try {
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withPermission(4),
                    "kadma admin debug selftest");
            KadmaMod.LOG.info("[kadma] selftest finished, shutting down");
            server.execute(() -> {
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                server.halt(false);
            });
        } catch (Exception ex) {
            KadmaMod.LOG.error("[kadma] selftest failed", ex);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        KadmaRuntime runtime = KadmaBootstrap.runtime(server);
        runtime.onServerStopping();
        KadmaMod.LOG.info("[kadma] server stopping, player data flushed");
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            KadmaRuntime runtime = KadmaBootstrap.runtime(player.getServer());
            if (runtime == null) {
                return;
            }
            com.example.kadma.data.PlayerData data = runtime.data().player(player.getUUID());
            data.lastKnownName = player.getGameProfile().getName();
            data.lastLoginMillis = System.currentTimeMillis();
            runtime.daily().onLogin(player, data);
            runtime.story().onboard(player, data);
            runtime.lobby().enterIfNeeded(player, data);
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            KadmaRuntime runtime = KadmaBootstrap.runtime(player.getServer());
            if (runtime == null) {
                return;
            }
            runtime.data().unload(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            KadmaRuntime runtime = KadmaBootstrap.runtime(player.getServer());
            if (runtime == null) {
                return;
            }
            runtime.hud().send(player, runtime.data().player(player.getUUID()));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        KadmaRuntime runtime = KadmaBootstrap.runtime(server);
        runtime.tick();
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }
        KadmaRuntime runtime = KadmaBootstrap.runtime(player.getServer());
        if (runtime == null) {
            return;
        }
        String npcId = runtime.npcs().idOf(event.getTarget());
        if (npcId == null || npcId.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        runtime.npcs().talk(player, npcId);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }
        KadmaRuntime runtime = KadmaBootstrap.runtime(player.getServer());
        if (runtime == null) {
            return;
        }
        String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(event.getLevel().getBlockState(event.getPos()).getBlock()).toString();
        runtime.quests().validator().onInteractBlock(player, runtime.data().player(player.getUUID()), blockId);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }
        KadmaRuntime runtime = KadmaBootstrap.runtime(player.getServer());
        if (runtime == null) {
            return;
        }
        runtime.quests().validator().onUseItem(player, runtime.data().player(player.getUUID()), event.getItemStack());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer killer)) {
            return;
        }
        if (event.getEntity() instanceof net.minecraft.world.entity.LivingEntity) {
            livingDead(killer, event);
        }
    }

    private static void livingDead(net.minecraft.server.level.ServerPlayer killer, LivingDeathEvent event) {
        net.minecraft.world.entity.LivingEntity dead = (net.minecraft.world.entity.LivingEntity) event.getEntity();
        KadmaRuntime runtime = KadmaBootstrap.runtime(killer.getServer());
        if (runtime == null) {
            return;
        }
        runtime.quests().validator().onKill(killer, runtime.data().player(killer.getUUID()), dead,
                event.getSource().getDirectEntity());

        String questTarget = com.example.kadma.util.EntityUtil.questTargetId(dead);
        if (questTarget != null && !questTarget.isEmpty()) {
            runtime.data().global().bumpStat("quest_target." + questTarget, 1);
            runtime.data().touch();
        }
    }

    /** Client side overlay hook, kept in the same class so the mod stays small. */
    @Mod.EventBusSubscriber(modid = KadmaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE,
            value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }
    }
}
