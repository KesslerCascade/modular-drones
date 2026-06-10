package rearth.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import rearth.Drones;

public class TagContent {
    
    public static final TagKey<Block> THRUSTER_BLOCKS = TagKey.create(Registries.BLOCK, Drones.id("thruster"));
    public static final TagKey<Block> LOW_THRUSTER = TagKey.create(Registries.BLOCK, Drones.id("low_thruster"));
    public static final TagKey<Block> MEDIUM_THRUSTER = TagKey.create(Registries.BLOCK, Drones.id("medium_thruster"));
    public static final TagKey<Block> HIGH_THRUSTER = TagKey.create(Registries.BLOCK, Drones.id("strong_thruster"));
    public static final TagKey<Block> ULTRA_THRUSTER = TagKey.create(Registries.BLOCK, Drones.id("ultra_thruster"));
    
    public static final TagKey<Block> ARROW_LAUNCHER = TagKey.create(Registries.BLOCK, Drones.id("arrow_launcher"));
    public static final TagKey<Block> BEAM_SOURCE = TagKey.create(Registries.BLOCK, Drones.id("beam_source"));
    public static final TagKey<Block> MELEE_DAMAGE = TagKey.create(Registries.BLOCK, Drones.id("melee_damage"));
    public static final TagKey<Block> MINING_TOOLS = TagKey.create(Registries.BLOCK, Drones.id("mining_tools"));
    public static final TagKey<Block> PICKUP_TOOLS = TagKey.create(Registries.BLOCK, Drones.id("pickup_tools"));
    public static final TagKey<Block> AXE_TOOLS = TagKey.create(Registries.BLOCK, Drones.id("axe_tools"));   // todo usage
    
    // todo abilities:
    // chest
    // tree chopping
    
}
