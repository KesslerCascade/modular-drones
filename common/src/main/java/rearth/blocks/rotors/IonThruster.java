package rearth.blocks.rotors;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class IonThruster extends Block {
    public IonThruster(Properties settings) {
        super(settings);
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
