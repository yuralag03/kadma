package com.example.kadma.layer;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.LayerConfig;
import com.example.kadma.util.TextUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vertical layer detection.
 *
 * <p>Layers can be plain Y ranges or additionally bounded on X/Z. The most specific
 * match wins, so a fenced-off upper platform still reports the right layer while a
 * player standing on open scaffolding falls through to the broad layer.</p>
 */
public class LayerService {

    private final KadmaServices services;
    private final Map<String, LayerRegion> regions = new LinkedHashMap<>();
    private final List<String> order = new ArrayList<>();

    /** Y range of each layer, cheap sorting so the tightest range matches first. */
    private long ambientCounter = 0L;

    public LayerService(KadmaServices services) {
        this.services = services;
        rebuild();
    }

    /** Re-read layer definitions after a config reload. */
    public final void rebuild() {
        regions.clear();
        order.clear();
        for (Map.Entry<String, LayerConfig> entry : services.config().layers().entrySet()) {
            LayerConfig config = entry.getValue();
            if (config == null || config.id.isEmpty()) {
                continue;
            }
            regions.put(config.id, new LayerRegion(config, config.hasBox()));
            order.add(config.id);
        }
        if (regions.isEmpty()) {
            // built-in safety layer so the HUD never reports "nowhere"
            LayerConfig fallback = new LayerConfig();
            fallback.id = "city";
            fallback.displayName = "\u0413\u043e\u0440\u043e\u0434";
            fallback.minY = -64;
            fallback.maxY = 320;
            regions.put(fallback.id, new LayerRegion(fallback, false));
            order.add(fallback.id);
        }
    }

    public List<LayerRegion> all() {
        return new ArrayList<>(regions.values());
    }

    public Map<String, LayerRegion> map() {
        return regions;
    }

    public LayerRegion region(Vec3 position) {
        if (position == null) {
            return null;
        }
        return LayerRegion.resolve(all(), position.x, position.y, position.z);
    }

    public LayerRegion region(ServerPlayer player) {
        return player == null ? null : region(player.position());
    }

    public LayerRegion byId(String id) {
        return regions.get(id);
    }

    public String currentLayerId(ServerPlayer player) {
        LayerRegion region = region(player);
        return region == null ? "" : region.id();
    }

    public String currentLayerDisplay(ServerPlayer player) {
        LayerRegion region = region(player);
        if (region != null) {
            return region.displayName();
        }
        String configured = services.config().settings().defaultLayer;
        LayerRegion fallback = regions.get(configured);
        return fallback != null ? fallback.displayName() : "\u0411\u0435\u0437 \u0441\u043b\u043e\u044f";
    }

    public List<String> ids() {
        return new ArrayList<>(order);
    }

    /**
     * Layer travel. Teleporting across layers must be an explicit admin or story
     * action; there is no client side path into restricted layers.
     */
    public boolean teleportTo(ServerPlayer player, String layerId) {
        LayerRegion region = regions.get(layerId);
        if (player == null || region == null) {
            return false;
        }
        double x = player.getX();
        double z = player.getZ();
        double y = clampY(region, player.getY());
        if (region.hasBox) {
            x = clamp(region.config.minX, region.config.maxX, x);
            z = clamp(region.config.minZ, region.config.maxZ, z);
        }
        player.teleportTo(player.serverLevel(), x + 0.5D, safeY(player, y), z + 0.5D, player.getYRot(), player.getXRot());
        return true;
    }

    private double clampY(LayerRegion region, double y) {
        double target = y;
        if (target < region.config.minY + 1) {
            target = region.config.minY + 1;
        }
        if (target > region.config.maxY - 1) {
            target = region.config.maxY - 1;
        }
        return target;
    }

    private double safeY(ServerPlayer player, double y) {
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(
                (int) Math.floor(player.getX()), (int) Math.floor(y + 1.0D), (int) Math.floor(player.getZ()));
        while (y > player.level().getMinBuildHeight() + 1
                && !player.level().getBlockState(pos.below()).isAir()) {
            y += 1.0D;
            pos = pos.above();
        }
        return y;
    }

    private static double clamp(double min, double max, double value) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    public boolean acceptsQuestType(String layerId, String questType) {
        LayerRegion region = regions.get(layerId);
        if (region == null) {
            return true;
        }
        List<String> types = region.config.questTypesOrAll();
        return types == null || types.isEmpty() || types.contains(questType);
    }

    public boolean canEnter(ServerPlayer player, String layerId) {
        LayerRegion region = regions.get(layerId);
        if (player == null || region == null) {
            return true;
        }
        String rule = region.config.accessRule == null ? "open" : region.config.accessRule;
        if (rule.isEmpty() || "open".equalsIgnoreCase(rule)) {
            return true;
        }
        if ("denied".equalsIgnoreCase(rule)) {
            return player.hasPermissions(2);
        }
        if (rule.startsWith("quest:") || rule.startsWith("require_quest:")) {
            String quest = region.config.accessQuest;
            return services.data().player(player.getUUID()).isComplete(quest);
        }
        return true;
    }

    public void refreshAmbient(ServerPlayer player, long tick) {
        if (!services.config().settings().features.visualEffects) {
            return;
        }
        LayerRegion region = region(player);
        if (region == null) {
            return;
        }
        LayerConfig config = region.config;
        if (config.ambientSound != null && !config.ambientSound.isEmpty()
                && config.ambientPeriodSeconds > 0) {
            if (tick % (config.ambientPeriodSeconds * 20L) == 0L) {
                services.visual().sound().ambientSound(player, config.ambientSound, config.ambientVolume / 100.0F);
            }
        }
        if (config.particleType != null && !config.particleType.isEmpty() && config.particleCount > 0) {
            long period = Math.max(1, config.particlePeriodSeconds) * 20L;
            if (tick % period == 0L) {
                services.visual().layerParticles(player, region);
            }
        }
    }

    /** Actionbar hint when the player crossed into a layer that restricts quests. */
    public Component accessNotice(String layerId) {
        LayerRegion region = regions.get(layerId);
        if (region == null || region.config.accessMessage == null || region.config.accessMessage.isEmpty()) {
            return null;
        }
        return TextUtil.info(region.config.accessMessage);
    }

    public long ambientCounter() {
        return ambientCounter;
    }

    public void bumpAmbient() {
        ambientCounter++;
    }
}
