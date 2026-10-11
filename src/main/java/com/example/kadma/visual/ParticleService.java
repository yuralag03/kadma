package com.example.kadma.visual;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.LayerConfig;
import com.example.kadma.layer.LayerRegion;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/** Server triggered particles and sounds. The client only renders what it is told. */
public class ParticleService {

    private final KadmaServices services;

    public ParticleService(KadmaServices services) {
        this.services = services;
    }

    public ParticleOptions resolve(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String id = name.toLowerCase(Locale.ROOT);
        net.minecraft.resources.ResourceLocation location =
                net.minecraft.resources.ResourceLocation.tryParse(id.contains(":") ? id : "minecraft:" + id);
        if (location == null) {
            return null;
        }
        net.minecraft.core.particles.ParticleType<?> type =
                net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.get(location);
        return type instanceof ParticleOptions ? (ParticleOptions) type : null;
    }

    public void spawn(ServerPlayer player, ParticleOptions options, Vec3 at, int count,
                      double spread, double speed) {
        if (player == null || options == null || at == null) {
            return;
        }
        player.serverLevel().sendParticles(player, options, false, at.x, at.y, at.z,
                Math.max(0, count), spread, spread, spread, Math.max(0.0D, speed));
    }

    public void spawnAround(ServerPlayer player, ParticleOptions options, Vec3 at, int count,
                            double radius, double speed) {
        spawn(player, options, at, count, radius, speed);
    }

    public void layerParticles(ServerPlayer player, LayerRegion region) {
        LayerConfig config = region == null ? null : region.config;
        if (config == null || config.particleType == null || config.particleType.isEmpty()) {
            return;
        }
        ParticleOptions options = resolve(config.particleType);
        if (options == null) {
            return;
        }
        Vec3 at = player.position();
        spawn(player, options, at, config.particleCount, config.particleSpread, config.particleSpeed);
    }

    /** Breathing effect used by the sealed portal. */
    public void portalBreath(ServerPlayer player, Vec3 center, int tick) {
        double wave = Math.sin(tick / 12.0D) * 0.35D;
        ParticleOptions flame = ParticleTypes.SOUL_FIRE_FLAME;
        ParticleOptions smoke = ParticleTypes.SMOKE;
        spawnAround(player, flame, center.add(0.0D, 0.4D + wave, 0.0D), 3, 0.8D, 0.01D);
        if (tick % 40 == 0) {
            spawnAround(player, smoke, center, 2, 1.2D, 0.02D);
        }
    }

    /** Cold, wet stone anomaly used by the crack beat. */
    public void crackAnomaly(ServerPlayer player, Vec3 center, int tick) {
        spawnAround(player, ParticleTypes.DRIPPING_DRIPSTONE_WATER, center, 2, 0.5D, 0.0D);
        double pulse = (Math.sin(tick / 8.0D) + 1.0D) * 0.5D;
        if (pulse > 0.7D) {
            spawnAround(player, ParticleTypes.SCULK_SOUL, center, 1, 0.35D, 0.0D);
        }
    }

    public void ritualFocus(ServerPlayer player, Vec3 center, int tick) {
        double angle = (tick % 200) / 200.0D * Math.PI * 2.0D;
        Vec3 at = center.add(Math.cos(angle) * 1.5D, 0.2D, Math.sin(angle) * 1.5D);
        spawnAround(player, ParticleTypes.END_ROD, at, 1, 0.1D, 0.0D);
        if (tick % 60 == 0) {
            spawnAround(player, ParticleTypes.TOTEM_OF_UNDYING, center.add(0.0D, 1.0D, 0.0D), 4, 0.6D, 0.02D);
        }
    }

    public void objectiveSpark(ServerPlayer player, Vec3 at) {
        spawnAround(player, ParticleTypes.HAPPY_VILLAGER, at == null ? player.position() : at, 8, 0.5D, 0.0D);
    }

    public void danger(ServerPlayer player, Vec3 at) {
        spawnAround(player, ParticleTypes.LARGE_SMOKE, at == null ? player.position() : at, 6, 0.4D, 0.01D);
    }
}
