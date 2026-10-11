package com.example.kadma.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Axis aligned box helpers shared by layers and zones. */
public final class LocationUtil {

    private LocationUtil() {
    }

    public static AABB box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static boolean inside(AABB box, Vec3 position) {
        return box != null && box.contains(position);
    }

    public static boolean insideBlock(AABB box, BlockPos position) {
        return box != null && box.contains(Vec3.atCenterOf(position));
    }

    public static BlockPos floor(Vec3 position) {
        return BlockPos.containing(position.x, position.y, position.z);
    }

    public static String pretty(Vec3 position) {
        return String.format("%.1f / %.1f / %.1f", position.x, position.y, position.z);
    }

    public static String pretty(Vec3i position) {
        return position.getX() + " / " + position.getY() + " / " + position.getZ();
    }

    /** Distance from a point to the nearest point of the box (0 when inside). */
    public static double distanceTo(AABB box, Vec3 position) {
        if (box == null) {
            return Double.MAX_VALUE;
        }
        if (box.contains(position)) {
            return 0.0D;
        }
        double dx = Math.max(0.0D, Math.max(box.minX - position.x, position.x - box.maxX));
        double dy = Math.max(0.0D, Math.max(box.minY - position.y, position.y - box.maxY));
        double dz = Math.max(0.0D, Math.max(box.minZ - position.z, position.z - box.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
