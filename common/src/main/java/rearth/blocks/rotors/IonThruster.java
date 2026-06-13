package rearth.blocks.rotors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.context.BlockPlaceContext;

public class IonThruster extends Block {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    public IonThruster(Properties settings) {
        super(settings);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH, SOUTH, EAST, WEST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        return defaultBlockState()
                 .setValue(NORTH, canConnect(level, pos.relative(Direction.NORTH)))
                 .setValue(SOUTH, canConnect(level, pos.relative(Direction.SOUTH)))
                 .setValue(EAST, canConnect(level, pos.relative(Direction.EAST)))
                 .setValue(WEST, canConnect(level, pos.relative(Direction.WEST)));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        switch (direction) {
            case NORTH -> state = state.setValue(NORTH, canConnect(level, pos.relative(Direction.NORTH)));
            case SOUTH -> state = state.setValue(SOUTH, canConnect(level, pos.relative(Direction.SOUTH)));
            case EAST -> state = state.setValue(EAST, canConnect(level, pos.relative(Direction.EAST)));
            case WEST -> state = state.setValue(WEST, canConnect(level, pos.relative(Direction.WEST)));
            default -> {}
        }
        return state;
    }

    private boolean canConnect(LevelAccessor level, BlockPos pos) {
        return level.getBlockState(pos).isSolid();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return makeShape();
    }

    public VoxelShape makeShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.or(shape, Shapes.box(0.3125, 0.5625, 0.125, 0.6875, 0.8125, 0.3125));
        shape = Shapes.or(shape, Shapes.box(0.375, 0.625, 0, 0.625, 0.8125, 0.125));
        shape = Shapes.or(shape, Shapes.box(0.1875, 0.4375, 0.1875, 0.3125, 0.875, 0.3125));
        shape = Shapes.or(shape, Shapes.box(0.6875, 0.4375, 0.1875, 0.8125, 0.875, 0.3125));
        shape = Shapes.or(shape, Shapes.box(0.6875, 0.5625, 0.3125, 0.875, 0.8125, 0.6875));
        shape = Shapes.or(shape, Shapes.box(0.875, 0.625, 0.375, 1, 0.8125, 0.625));
        shape = Shapes.or(shape, Shapes.box(0.6875, 0.4375, 0.6875, 0.8125, 0.875, 0.8125));
        shape = Shapes.or(shape, Shapes.box(0.3125, 0.5625, 0.6875, 0.6875, 0.8125, 0.875));
        shape = Shapes.or(shape, Shapes.box(0.375, 0.625, 0.875, 0.625, 0.8125, 1));
        shape = Shapes.or(shape, Shapes.box(0.1875, 0.4375, 0.6875, 0.3125, 0.875, 0.8125));
        shape = Shapes.or(shape, Shapes.box(0.125, 0.5625, 0.3125, 0.3125, 0.8125, 0.6875));
        shape = Shapes.or(shape, Shapes.box(0, 0.625, 0.375, 0.125, 0.8125, 0.625));
        shape = Shapes.or(shape, Shapes.box(0.3125, 0.25, 0.3125, 0.6875, 0.75, 0.6875));
        shape = Shapes.or(shape, Shapes.box(0.375, -0.125, 0.5, 0.625, 0.25, 0.5));
        shape = Shapes.or(shape, Shapes.box(0.375, -0.125, 0.5, 0.625, 0.25, 0.5));

        return shape;
    }
}
