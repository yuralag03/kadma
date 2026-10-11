package com.example.kadma.visual;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.ItemConfig;
import com.example.kadma.config.QuestConfig;
import com.example.kadma.layer.LayerRegion;
import com.example.kadma.quest.Objective;
import com.example.kadma.util.TextUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Presentation layer.
 *
 * <p>Everything the player sees comes from a server decision: particles, sounds,
 * titles, action bar, boss bars and floating labels. The client never invents state.</p>
 */
public class VisualService {

    private final KadmaServices services;
    private final ParticleService particles;
    private final SoundService sound;
    private final BossbarService bossbars;
    private final MarkerService markers;
    private final TextDisplayService displays;

    public VisualService(KadmaServices services) {
        this.services = services;
        this.particles = new ParticleService(services);
        this.sound = new SoundService(services);
        this.bossbars = new BossbarService(services);
        this.markers = new MarkerService(services);
        this.displays = new TextDisplayService(services);
    }

    public ParticleService particles() {
        return particles;
    }

    public SoundService sound() {
        return sound;
    }

    public BossbarService bars() {
        return bossbars;
    }

    public MarkerService markers() {
        return markers;
    }

    public TextDisplayService displays() {
        return displays;
    }

    public void rebuild() {
        markers.rescan();
    }

    public void actionbar(ServerPlayer player, Component component) {
        sound().actionbar(player, component);
    }

    public void actionbar(ServerPlayer player, String raw) {
        sound().actionbar(player, raw);
    }

    public void title(ServerPlayer player, String heading, String subtitle, int fadeIn, int stay, int fadeOut) {
        sound().title(player, heading, subtitle, fadeIn, stay, fadeOut);
    }

    public void layerParticles(ServerPlayer player, LayerRegion region) {
        if (!services.config().settings().features.visualEffects) {
            return;
        }
        particles().layerParticles(player, region);
    }

    public void questStart(ServerPlayer player, QuestConfig quest) {
        if (!services.config().settings().features.visualEffects) {
            return;
        }
        sound.questStart(player);
        particles.objectiveSpark(player, player.position());
    }

    public void questComplete(ServerPlayer player, QuestConfig quest) {
        if (!services.config().settings().features.visualEffects) {
            return;
        }
        sound.questComplete(player);
        particles.objectiveSpark(player, player.position().add(0.0D, 1.0D, 0.0D));
    }

    public void objectiveDone(ServerPlayer player, Objective objective) {
        if (!services.config().settings().features.visualEffects) {
            return;
        }
        sound.objectiveDone(player);
        particles.objectiveSpark(player, player.position());
    }

    public void itemFlavour(ServerPlayer player, ItemConfig item) {
        if (!services.config().settings().features.visualEffects) {
            return;
        }
        particles.objectiveSpark(player, player.position());
        sound.playVanilla(player, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                net.minecraft.sounds.SoundSource.AMBIENT, 0.3F, 1.3F);
    }

    public void portalAmbience(ServerPlayer player, Vec3 center, int tick) {
        if (!services.config().settings().features.visualEffects) {
            return;
        }
        particles.portalBreath(player, center, tick);
        if (tick % 60 == 0) {
            sound.portalBreathe(player);
        }
    }

    public void crackAmbience(ServerPlayer player, Vec3 center, int tick) {
        if (!services.config().settings().features.visualEffects) {
            return;
        }
        particles.crackAnomaly(player, center, tick);
    }

    public void ritualAmbience(ServerPlayer player, Vec3 center, int tick) {
        particles.ritualFocus(player, center, tick);
    }

    public void deny(ServerPlayer player) {
        sound.deny(player);
    }

    public void clearBossbars() {
        bossbars.hideAll();
    }
}
