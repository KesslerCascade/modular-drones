package rearth.init;

import com.mojang.serialization.Codec;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Wrapper around ItemStack for use as a DataComponentType value.
 * NeoForge requires DataComponent values to implement equals and hashCode.
 */
public record CarriedItemComponent(ItemStack stack) {

    public static final Codec<CarriedItemComponent> CODEC =
            ItemStack.CODEC.xmap(CarriedItemComponent::new, CarriedItemComponent::stack);

    public static final StreamCodec<RegistryFriendlyByteBuf, CarriedItemComponent> PACKET_CODEC =
            ItemStack.STREAM_CODEC.map(CarriedItemComponent::new, CarriedItemComponent::stack);

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CarriedItemComponent c)) return false;
        return ItemStack.isSameItemSameComponents(stack, c.stack)
                && stack.getCount() == c.stack.getCount();
    }

    @Override
    public int hashCode() {
        if (stack.isEmpty()) return 0;
        return Objects.hash(stack.getItem(), stack.getCount(), stack.getComponents());
    }
}
