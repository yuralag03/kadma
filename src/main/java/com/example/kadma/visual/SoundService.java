package com.example.kadma.visual;

import com.example.kadma.KadmaServices;
import com.example.kadma.util.TextUtil;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Locale;

/** Server driven titles, action bar text and sounds. */
public class SoundService {

    private final KadmaServices services;

    public SoundService(KadmaServices services) {
        this.services = services;
    }

    public SoundEvent resolve(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String id = raw.toLowerCase(Locale.ROOT);
        net.minecraft.resources.ResourceLocation location =
                net.minecraft.resources.ResourceLocation.tryParse(id.contains(":") ? id : "minecraft:" + id);
        if (location == null) {
            return null;
        }
        return net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(location);
    }

    public void play(ServerPlayer player, net.minecraft.world.phys.Vec3 at, String soundId, float volume, float pitch) {
        if (player == null || soundId == null || soundId.isEmpty()) {
            return;
        }
        SoundEvent event = resolve(soundId);
        if (event == null) {
            return;
        }
        double x = at == null ? player.getX() : at.x;
        double y = at == null ? player.getY() : at.y;
        double z = at == null ? player.getZ() : at.z;
        player.serverLevel().playSound(player, x, y, z, event, SoundSource.AMBIENT,
                Math.max(0.05F, Math.min(1.0F, volume)), pitch);
    }

    public void playVanilla(ServerPlayer player, SoundEvent event, SoundSource source, float volume, float pitch) {
        if (player == null || event == null) {
            return;
        }
        player.serverLevel().playSound(player, player.getX(), player.getY(), player.getZ(), event, source, volume, pitch);
    }

    public void ambientSound(ServerPlayer player, String soundId, float scale) {
        float volume = Math.max(0.05F, Math.min(1.0F, 0.6F * scale));
        play(player, player.position(), soundId, volume, 0.8F);
    }

    public void questStart(ServerPlayer player) {
        playVanilla(player, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.5F, 1.2F);
    }

    public void questComplete(ServerPlayer player) {
        playVanilla(player, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.4F);
    }

    public void objectiveDone(ServerPlayer player) {
        playVanilla(player, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.4F, 1.5F);
    }

    public void deny(ServerPlayer player) {
        playVanilla(player, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.35F, 0.7F);
    }

    public void portalBreathe(ServerPlayer player) {
        playVanilla(player, SoundEvents.PORTAL_AMBIENT, SoundSource.AMBIENT, 0.25F, 0.6F);
    }

    public void ritualStart(ServerPlayer player) {
        playVanilla(player, SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 0.6F, 1.0F);
    }

    public void actionbar(ServerPlayer player, net.minecraft.network.chat.Component component) {
        if (player == null || component == null) {
            return;
        }
        player.connection.send(new ClientboundSetActionBarTextPacket(component));
    }

    public void actionbar(ServerPlayer player, String raw) {
        actionbar(player, TextUtil.info(raw));
    }

    public void title(ServerPlayer player, String heading, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return;
        }
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        if (heading != null && !heading.isEmpty()) {
            player.connection.send(new ClientboundSetTitleTextPacket(
                    net.minecraft.network.chat.Component.literal(heading).withStyle(net.minecraft.ChatFormatting.GOLD)));
        }
        if (subtitle != null && !subtitle.isEmpty()) {
            player.connection.send(new ClientboundSetSubtitleTextPacket(
                    net.minecraft.network.chat.Component.literal(subtitle).withStyle(net.minecraft.ChatFormatting.GRAY)));
        }
    }
}
