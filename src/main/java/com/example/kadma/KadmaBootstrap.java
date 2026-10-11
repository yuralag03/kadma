package com.example.kadma;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Bootstrap that owns the per-server {@link KadmaRuntime}.
 *
 * <p>Keeping the runtime in a holder keyed by the server instance means the mod
 * survives {@code /reload} and world switches without leaking a stale level.</p>
 */
@Mod.EventBusSubscriber(modid = KadmaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KadmaBootstrap {

    public static final Logger LOG = LogUtils.getLogger();

    private static volatile KadmaRuntime runtime;
    private static volatile MinecraftServer owner;

    private KadmaBootstrap() {
    }

    /** Create or reuse the runtime for a server. */
    public static KadmaRuntime runtime(MinecraftServer server) {
        if (server == null) {
            return runtime;
        }
        if (runtime != null && owner == server) {
            return runtime;
        }
        synchronized (KadmaBootstrap.class) {
            if (runtime != null && owner == server) {
                return runtime;
            }
            if (runtime != null) {
                LOG.info("[kadma] replacing runtime for a new server instance");
            }
            runtime = new KadmaRuntime(server);
            owner = server;
            return runtime;
        }
    }

    /** Current runtime, may be {@code null} between worlds. */
    public static KadmaRuntime active() {
        return runtime;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        if (event.getServer() == owner) {
            runtime = null;
            owner = null;
        }
    }
}
