package net.claustra01.iaftfc.mixin;

import net.dries007.tfc.common.container.KnappingContainer;
import net.dries007.tfc.util.data.KnappingType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void closeKnappingBeforeSave(ServerPlayer player, CallbackInfo callback) {
        if (player.containerMenu instanceof KnappingContainer menu
            && KnappingType.MANAGER.getId(menu.getKnappingType()).getNamespace().equals("iaftfc")) {
            player.closeContainer();
        }
    }
}
