package com.example.kadma.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Resolved named zone. */
public class ZoneRegion {

    public final String id;
    public final String displayName;
    public final String layer;
    public final AABB box;
    public final boolean safe;
    public final java.util.List<String> allowedQuestTypes;

    public ZoneRegion(String id, String displayName, String layer, AABB box, boolean safe,
                      java.util.List<String> allowedQuestTypes) {
        this.id = id;
        this.displayName = displayName;
        this.layer = layer;
        this.box = box;
        this.safe = safe;
        this.allowedQuestTypes = allowedQuestTypes == null ? java.util.List.of() : allowedQuestTypes;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName == null || displayName.isEmpty() ? id : displayName;
    }

    public boolean contains(Vec3 position) {
        return position != null && box != null && box.contains(position);
    }

    public boolean contains(BlockPos position) {
        return position != null && box != null && box.contains(Vec3.atCenterOf(position));
    }

    public boolean isMostlyInside(Vec3 position) {
        return contains(position);
    }

    public boolean allows(String questType) {
        return allowedQuestTypes.isEmpty() || allowedQuestTypes.contains(questType);
    }

    public Vec3 center() {
        if (box == null) {
            return Vec3.ZERO;
        }
        return box.getCenter();
    }
}
