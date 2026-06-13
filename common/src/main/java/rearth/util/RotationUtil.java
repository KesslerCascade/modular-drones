package rearth.util;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;

public class RotationUtil {

    public static Rotation rotationFromTo(Direction from, Direction to) {
        var dir = from;
        var steps = 0;
        while (dir != to) {
            dir = dir.getClockWise();
            steps++;
        }
        return switch (steps) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static Vec3i rotate(Vec3i pos, Rotation rotation) {
        return switch (rotation) {
            case NONE -> pos;
            case CLOCKWISE_90 -> new Vec3i(-pos.getZ(), pos.getY(), pos.getX());
            case CLOCKWISE_180 -> new Vec3i(-pos.getX(), pos.getY(), -pos.getZ());
            case COUNTERCLOCKWISE_90 -> new Vec3i(pos.getZ(), pos.getY(), -pos.getX());
        };
    }

}
