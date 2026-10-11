package com.example.kadma;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Kadma Story Engine entry point.
 *
 * <p>All gameplay authority lives on the server. The client only renders the HUD and
 * plays local cosmetic effects that the server explicitly asked for.</p>
 */
@Mod(KadmaMod.MODID)
public final class KadmaMod {

    public static final String MODID = "kadma";
    public static final String NAME = "Kadma Story Engine";
    public static final String VERSION = "1.0.0";

    public static final Logger LOG = LogUtils.getLogger();

    public KadmaMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        // Server side Forge events. Registered explicitly here (instead of a class
        // level @EventBusSubscriber) so a dedicated server never loads the client
        // subscriber and so no handler is subscribed twice.
        MinecraftForge.EVENT_BUS.addListener(KadmaEvents::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(KadmaEvents::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(KadmaEvents::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(KadmaEvents::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(KadmaEvents::onEntityInteract);
        MinecraftForge.EVENT_BUS.addListener(KadmaEvents::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(KadmaEvents::onRightClickItem);
        MinecraftForge.EVENT_BUS.addListener(KadmaEvents::onDeath);
        MinecraftForge.EVENT_BUS.addListener(KadmaEvents::onLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(KadmaEvents::onLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(KadmaEvents::onRespawn);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // The channel is only needed for server to client payloads, so it is created
        // once during common setup on both sides.
        event.enqueueWork(com.example.kadma.network.KadmaNetwork::init);
        LOG.info("[kadma] {} {} initialising (Forge 1.20.1)", NAME, VERSION);
    }

    public static void note(String message) {
        LOG.info("[kadma] {}", message);
    }

    public static void warn(String message) {
        LOG.warn("[kadma] {}", message);
    }
}
