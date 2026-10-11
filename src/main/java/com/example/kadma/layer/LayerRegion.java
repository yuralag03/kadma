package com.example.kadma.layer;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.LayerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/** A resolved layer region for one position. */
public class LayerRegion {

    public final LayerConfig config;
    public final boolean hasBox;

    public LayerRegion(LayerConfig config, boolean hasBox) {
        this.config = config;
        this.hasBox = hasBox;
    }

    public String id() {
        return config == null ? "" : config.id;
    }

    public String displayName() {
        return config == null ? "" : config.displayName;
    }

    public boolean contains(double x, double y, double z) {
        if (config == null) {
            return false;
        }
        return config.containsY(y) && config.containsXZ(x, z);
    }

    public boolean contains(Vec3 position) {
        return position != null && contains(position.x, position.y, position.z);
    }

    public static LayerRegion forId(KadmaServices services, String id) {
        LayerConfig config = services.config().layer(id);
        return config == null ? null : new LayerRegion(config, config.hasBox());
    }

    public static LayerRegion resolve(List<LayerRegion> regions, double x, double y, double z) {
        for (LayerRegion region : regions) {
            if (region.contains(x, y, z)) {
                return region;
            }
        }
        return null;
    }

    /** {@link #resolve(LayerRegion[], double, double, double)} */
    public static LayerRegion resolve(List<LayerRegion> regions, Vec3 position) {
        return position == null ? null : resolve(regions, position.x, position.y, position.z);
    }

    public static Map.Entry<String, LayerRegion> best(Map<String, LayerRegion> regions, double x, double y, double z) {
        LayerRegion withBox = null;
        LayerRegion fallback = null;
        for (LayerRegion region : regions.values()) {
            if (!region.contains(x, y, z)) {
                continue;
            }
            if (region.hasBox && withBox == null) {
                withBox = region;
            }
            if (fallback == null) {
                fallback = region;
            }
        }
        LayerRegion chosen = withBox != null ? withBox : fallback;
        return chosen == null ? null : Map.entry(chosen.id(), chosen);
    }

    /** Layer of a player right now. */
    public static String layerOf(KadmaServices services, ServerPlayer player) {
        LayerRegion region = services.layers().region(player);
        return region == null ? "" : region.id();
    }
}
