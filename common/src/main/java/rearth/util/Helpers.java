package rearth.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.ShapeContext;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class Helpers {
    
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
