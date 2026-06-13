package rearth.blocks.rotors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import rearth.util.RotationUtil;

public class DrillBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public DrillBlock(Properties settings) {
        super(settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        var rotation = RotationUtil.rotationFromTo(Direction.SOUTH, state.getValue(FACING));
        return rotateShape(makeShape(), rotation);
    }

    public VoxelShape makeShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.or(shape, Shapes.box(0, 0.125, 0, 1, 0.875, 0.1875));
        shape = Shapes.or(shape, Shapes.box(0.1875, 0.1875, 0.1875, 0.8125, 0.8125, 0.375));
        shape = Shapes.or(shape, Shapes.box(0.25, 0.25, 0.375, 0.75, 0.75, 0.5625));
        shape = Shapes.or(shape, Shapes.box(0.3125, 0.3125, 0.5625, 0.6875, 0.6875, 0.75));
        shape = Shapes.or(shape, Shapes.box(0.375, 0.375, 0.75, 0.625, 0.625, 0.875));
        shape = Shapes.or(shape, Shapes.box(0.4375, 0.4375, 0.875, 0.5625, 0.5625, 1));

        return shape;
    }

    private static VoxelShape rotateShape(VoxelShape shape, Rotation rotation) {
        if (rotation == Rotation.NONE) return shape;

        var result = Shapes.empty();
        for (var box : shape.toAabbs()) {
            var min = rotatePoint(box.minX, box.minZ, rotation);
            var max = rotatePoint(box.maxX, box.maxZ, rotation);
            result = Shapes.or(result, Shapes.box(
              Math.min(min[0], max[0]), box.minY, Math.min(min[1], max[1]),
              Math.max(min[0], max[0]), box.maxY, Math.max(min[1], max[1])
            ));
        }
        return result;
    }

    private static double[] rotatePoint(double x, double z, Rotation rotation) {
        var rx = x - 0.5;
        var rz = z - 0.5;
        return switch (rotation) {
            case CLOCKWISE_90 -> new double[]{0.5 - rz, 0.5 + rx};
            case CLOCKWISE_180 -> new double[]{0.5 - rx, 0.5 - rz};
            case COUNTERCLOCKWISE_90 -> new double[]{0.5 + rz, 0.5 - rx};
            default -> new double[]{x, z};
        };
    }
}
