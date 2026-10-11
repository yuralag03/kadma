package com.example.kadma.zone;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.ZoneConfig;
import com.example.kadma.util.LocationUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

/** Named zone lookup used by goto, survive, kill, interact and event objectives. */
public class ZoneService {

    private final KadmaServices services;
    private final Map<String, ZoneRegion> regions = new LinkedHashMap<>();

    public ZoneService(KadmaServices services) {
        this.services = services;
        rebuild();
    }

    public final void rebuild() {
        regions.clear();
        for (Map.Entry<String, ZoneConfig> entry : services.config().zones().entrySet()) {
            ZoneConfig config = entry.getValue();
            if (config == null || config.id.isEmpty()) {
                continue;
            }
            net.minecraft.world.phys.AABB region = box(config);
            regions.put(config.id, new ZoneRegion(config.id, config.displayName, config.layer, region,
                    config.safe, config.allowedQuestTypes));
        }
    }

    private static net.minecraft.world.phys.AABB box(ZoneConfig config) {
        return LocationUtil.box(config.minX, config.minY, config.minZ, config.maxX, config.maxY, config.maxZ);
    }

    public Map<String, ZoneRegion> all() {
        return regions;
    }

    public ZoneRegion byId(String id) {
        return id == null ? null : regions.get(id);
    }

    public boolean isInside(ServerPlayer player, String zoneId) {
        ZoneRegion region = regions.get(zoneId);
        return region != null && region.contains(player == null ? null : player.position());
    }

    public boolean isInside(Vec3 position, String zoneId) {
        ZoneRegion region = regions.get(zoneId);
        return region != null && region.contains(position);
    }

    /** First configured zone the player is standing in, preferring the bounding layer. */
    public String currentZoneId(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        Vec3 position = player.position();
        String layer = services.layers().currentLayerId(player);
        for (ZoneRegion region : regions.values()) {
            if (region.contains(position) && layer.equals(region.layer)) {
                return region.id;
            }
        }
        for (ZoneRegion region : regions.values()) {
            if (region.contains(position)) {
                return region.id;
            }
        }
        return null;
    }

    public ZoneRegion currentZone(ServerPlayer player) {
        String id = currentZoneId(player);
        return id == null ? null : regions.get(id);
    }
}
