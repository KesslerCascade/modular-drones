package rearth.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class FloodFill {
    
    public static List<BlockPos> Run(Level world, BlockPos start, Predicate<BlockState> filter, Predicate<BlockPos> positionFilter, int maxCount, boolean allowDiagonal) {
        
        var checked = new HashSet<BlockPos>();
        var results = new ArrayList<BlockPos>();
        var open = new HashSet<BlockPos>();
        
        open.add(start);
        
        do {
            
            var nextSet = new HashSet<BlockPos>();
            
            for (var checkPos : open) {
                if (checked.contains(checkPos)) continue;
                checked.add(checkPos);
                
                var checkState = world.getBlockState(checkPos);
                if (filter.test(checkState)) {
                    results.add(checkPos);
                    
                    // add neighbors to next set
                    nextSet.addAll(Stream.of(GetNeighbors(checkPos, allowDiagonal)).filter(positionFilter).toList());
                    
                }
            }
            
            open = nextSet;
            
        } while (!open.isEmpty() && results.size() <= maxCount);
        
        return results;
        
    }
    
    public static BlockPos[] GetNeighbors(BlockPos from, boolean diagonal) {
        return diagonal ? GetNeighborsDiagonal(from) : GetNeighbors(from);
    }
    
    public static BlockPos[] GetNeighbors(BlockPos from) {
        return new BlockPos[] {from.above(), from.below(), from.north(), from.east(), from.south(), from.west()};
    }
    
    public static BlockPos[] GetNeighborsDiagonal(BlockPos from) {
        return new BlockPos[] {from.above(), from.below(),
          from.north(), from.east(), from.south(), from.west(),
          from.north().east(), from.east().south(), from.south().west(), from.west().north(),
          from.north().above(), from.east().above(), from.south().above(), from.west().above(),
          from.north().below(), from.east().below(), from.south().below(), from.west().below()
        };
    }
    
    public static BlockPos[] GetHorizontalNeighbors(BlockPos from) {
        return new BlockPos[] {from.north(), from.east(), from.south(), from.west()};
    }
    
}
