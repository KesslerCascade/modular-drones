package rearth;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rearth.drone.DroneController;
import rearth.drone.behaviour.DroneLight;
import rearth.init.*;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class Drones {
    public static final String MOD_ID = "drones";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    public static final List<Runnable> DELAYED_ACTIONS = new ArrayList<>();
    
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void init() {
        
        LOGGER.info("May your drones assemble without issue!");
        
        BlockContent.BLOCKS.register();
        BlockEntitiesContent.BLOCK_ENTITIES.register();
        ItemContent.ITEMS.register();
        ComponentContent.COMPONENT_TYPES.register();
        ItemGroups.TABS.register();
        
        TickEvent.SERVER_POST.register(event -> event.getAllLevels().forEach(world -> world.players().forEach(DroneController::tickPlayer)));
        TickEvent.SERVER_PRE.register(event -> event.getAllLevels().forEach(DroneLight::removeOldLights));
        TickEvent.SERVER_POST.register(event -> {
            DELAYED_ACTIONS.forEach(Runnable::run);
            DELAYED_ACTIONS.clear();
        });
        
        PlayerEvent.ATTACK_ENTITY.register(DroneController::onPlayerAttackEntityEvent);
        PlayerEvent.PLAYER_QUIT.register(DroneController::clearPlayerData);
        
        NetworkContent.init();
        
        BlockContent.registerItems();
        
    }
}
