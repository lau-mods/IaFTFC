package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.registry.IafItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class DragonStewBowlMixin {
    @Inject(method = "finishUsingItem", at = @At("RETURN"), cancellable = true)
    private void iaftfc$returnStewBowl(ItemStack stack, Level level, LivingEntity entity,
        CallbackInfoReturnable<ItemStack> callback) {
        Item item = (Item) (Object) this;
        if (entity instanceof Player player && !player.hasInfiniteMaterials()
            && (item == IafItems.FIRE_STEW.get() || item == IafItems.FROST_STEW.get()
                || item == IafItems.LIGHTNING_STEW.get())) {
            ItemStack bowl = new ItemStack(Items.BOWL);
            if (callback.getReturnValue().isEmpty()) {
                callback.setReturnValue(bowl);
            } else if (!level.isClientSide() && !player.getInventory().add(bowl)) {
                player.drop(bowl, false);
            }
        }
    }
}
