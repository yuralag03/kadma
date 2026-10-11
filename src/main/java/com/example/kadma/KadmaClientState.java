package com.example.kadma;

import com.example.kadma.hud.HudPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only storage for the last HUD payload and the last cosmetic effect.
 *
 * <p>Loaded only on {@link Dist#CLIENT}: the dedicated server never touches this class
 * because {@link KadmaNetwork} only dispatches client bound packets.</p>
 */
@Mod.EventBusSubscriber(modid = KadmaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class KadmaClientState {

    private static volatile HudPayload payload;
    private static volatile long receivedAt;

    private KadmaClientState() {
    }

    public static void receive(String json, boolean visible) {
        HudPayload parsed = HudPayload.fromJson(json);
        parsed.visible = visible;
        payload = parsed;
        receivedAt = System.currentTimeMillis();
    }

    public static HudPayload payload() {
        return payload;
    }

    public static long receivedAt() {
        return receivedAt;
    }

    public static void clear() {
        payload = null;
        receivedAt = 0L;
    }

    /** Play a local cosmetic effect. Never changes state, purely visual. */
    public static void playEffect(String kind, double x, double y, double z) {
        if (kind == null || kind.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (minecraft.options.particles().equals(ParticleStatus.MINIMAL) && requiresParticles(kind)) {
            return;
        }
        switch (kind) {
            case "spark":
                spawn(minecraft, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, x, y, z, 8, 0.4D);
                break;
            case "seal":
                spawn(minecraft, net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 6, 0.5D);
                break;
            case "crack":
                spawn(minecraft, net.minecraft.core.particles.ParticleTypes.DRIPPING_DRIPSTONE_WATER, x, y, z, 4, 0.3D);
                break;
            case "chime":
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(),
                        1.4F, 0.5F));
                break;
            default:
                break;
        }
    }

    private static boolean requiresParticles(String kind) {
        return "spark".equals(kind) || "seal".equals(kind) || "crack".equals(kind);
    }

    private static void spawn(Minecraft minecraft, SimpleParticleType type, double x, double y, double z,
                             int count, double spread) {
        for (int index = 0; index < count; index++) {
            double dx = x + (minecraft.level.random.nextDouble() - 0.5D) * spread;
            double dy = y + (minecraft.level.random.nextDouble() - 0.5D) * spread;
            double dz = z + (minecraft.level.random.nextDouble() - 0.5D) * spread;
            minecraft.level.addParticle(type, dx, dy, dz, 0.0D, 0.02D, 0.0D);
        }
    }

    /** Resolve a particle id for a cosmetic effect, ignoring unknown ids. */
    public static ParticleOptions particle(String raw) {
        ResourceLocation location = ResourceLocation.tryParse(raw);
        if (location == null) {
            return null;
        }
        return (ParticleOptions) net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.get(location);
    }
}
