package com.example.kadma;

import com.example.kadma.hud.HudClientRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
/**
 * Client entry point.
 *
 * <p>Contains only presentation code. Nothing here can mutate story state, and the
 * class is never loaded on a dedicated server.</p>
 */
@Mod.EventBusSubscriber(modid = KadmaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KadmaClient {

    private KadmaClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        KadmaMod.LOG.info("[kadma] client setup, HUD renderer armed");
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        // Overlays are drawn straight from the event in HudClientRenderer; nothing
        // else is registered so a stale overlay can never outlive the payload.
    }

    /** Called from the client tick so overlays refresh with the window size. */
    public static void reset() {
        KadmaClientState.clear();
    }
}
