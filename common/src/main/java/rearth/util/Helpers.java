package rearth.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.ShapeContext;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class Helpers {

    public record DronePathResult(PathStatus status, @Nullable Vec3d waypoint) {
        public enum PathStatus {
            DIRECT, VIA_WAYPOINT, NO_PATH
        }

        public static final DronePathResult DIRECT = new DronePathResult(PathStatus.DIRECT, null);
        public static final DronePathResult NO_PATH = new DronePathResult(PathStatus.NO_PATH, null);

        public static DronePathResult waypoint(Vec3d intermediate) {
            return new DronePathResult(PathStatus.VIA_WAYPOINT, intermediate);
        }

        public boolean isReachable() {
            return status != PathStatus.NO_PATH;
        }
    }

    public static DronePathResult getDronePath(World world, Vec3d start, Vec3d target) {
        if (isLineAvailable(world, start, target))
            return DronePathResult.DIRECT;

        var dx = target.x - start.x;
        var dz = target.z - start.z;
        var xzDist = Math.sqrt(dx * dx + dz * dz);

        // Candidate A: fly to target XZ at drone's Y, then drop to target
        var candidateA = new Vec3d(target.x, start.y, target.z);
        if (isLineAvailable(world, start, candidateA) && isLineAvailable(world, candidateA, target))
            return DronePathResult.waypoint(candidateA);

        // Candidate B: climb/descend to target's Y first, then fly XZ to target (helps escape holes)
        var candidateB = new Vec3d(start.x, target.y, start.z);
        if (isLineAvailable(world, start, candidateB) && isLineAvailable(world, candidateB, target))
            return DronePathResult.waypoint(candidateB);

        // Candidate C: fly to above target (30° climb angle), then drop to target
        var climbHeight = xzDist * Math.tan(Math.toRadians(30));
        var candidateC = new Vec3d(target.x, target.y + climbHeight, target.z);
        if (isLineAvailable(world, start, candidateC) && isLineAvailable(world, candidateC, target))
            return DronePathResult.waypoint(candidateC);

        // Candidates D & E: flank 30° left/right in XZ at half distance
        if (xzDist >= 0.001) {
            var xzDirX = dx / xzDist;
            var xzDirZ = dz / xzDist;
            var halfDist = start.distanceTo(target) / 2.0;
            var cos30 = Math.cos(Math.toRadians(30));
            var sin30 = Math.sin(Math.toRadians(30));

            // Candidate D: 30° left (CCW)
            var leftX = xzDirX * cos30 - xzDirZ * sin30;
            var leftZ = xzDirX * sin30 + xzDirZ * cos30;
            var candidateD = new Vec3d(start.x + leftX * halfDist, start.y, start.z + leftZ * halfDist);
            if (isLineAvailable(world, start, candidateD) && isLineAvailable(world, candidateD, target))
                return DronePathResult.waypoint(candidateD);

            // Candidate E: 30° right (CW)
            var rightX = xzDirX * cos30 + xzDirZ * sin30;
            var rightZ = -xzDirX * sin30 + xzDirZ * cos30;
            var candidateE = new Vec3d(start.x + rightX * halfDist, start.y, start.z + rightZ * halfDist);
            if (isLineAvailable(world, start, candidateE) && isLineAvailable(world, candidateE, target))
                return DronePathResult.waypoint(candidateE);
        }

        return DronePathResult.NO_PATH;
    }
    
    public static PacketCodec<ByteBuf, Vec3i> VEC3I_PACKET_CODEC = new PacketCodec<>() {
        @Override
        public Vec3i decode(ByteBuf buf) {
            var x = buf.readInt();
            var y = buf.readInt();
            var z = buf.readInt();
            return new Vec3i(x, y, z);
        }
        
        @Override
        public void encode(ByteBuf buf, Vec3i value) {
            buf.writeInt(value.getX());
            buf.writeInt(value.getY());
            buf.writeInt(value.getZ());
        }
    };
    public static PacketCodec<ByteBuf, Vec3d> VEC3D_PACKET_CODEC = new PacketCodec<>() {
        @Override
        public Vec3d decode(ByteBuf buf) {
            var x = buf.readDouble();
            var y = buf.readDouble();
            var z = buf.readDouble();
            return new Vec3d(x, y, z);
        }
        
        @Override
        public void encode(ByteBuf buf, Vec3d value) {
            buf.writeDouble(value.x);
            buf.writeDouble(value.y);
            buf.writeDouble(value.z);
        }
    };
    
    // in degrees
    public static float calculateYaw(Vec3d self, Vec3d target) {
        var direction = target.subtract(self).normalize();
        return (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
    }
    
    public static Vec3d lerp(Vec3d a, Vec3d b, float f) {
        return new Vec3d(lerp(a.x, b.x, f), lerp(a.y, b.y, f / 2f), lerp(a.z, b.z, f));
    }
    
    public static double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }
    
    public static boolean isLineAvailable(World world, Vec3d to, Vec3d from) {
        var context = new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, ShapeContext.absent());
        var result = world.raycast(context);
        return result.getType() == HitResult.Type.MISS;
    }
    
    public static boolean isPositionAvailable(World world, Vec3d pos, Vec3d from) {
        var backDir = from.subtract(pos).normalize();
        var start = pos.add(backDir.multiply(0.5f));
        
        var context = new RaycastContext(start, pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, ShapeContext.absent());
        var result = world.raycast(context);
        return result.getType() == HitResult.Type.MISS;
    }
}
