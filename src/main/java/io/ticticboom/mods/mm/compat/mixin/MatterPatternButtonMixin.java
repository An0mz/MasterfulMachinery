package io.ticticboom.mods.mm.compat.mixin;

import com.buuz135.replication.client.gui.addons.MatterPatternButton;
import io.ticticboom.mods.mm.port.replication.link.ReplicationLinkRecipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.buuz135.replication.client.gui.addons.MatterPatternButton")
public abstract class MatterPatternButtonMixin {

    private static final int LINK_AMOUNT = 9999;

    @Inject(method = "recalculateAmount", at = @At("TAIL"))
    private void mm$allowLinkRequests(String network, CallbackInfo ci) {
        var button = (MatterPatternButton) (Object) this;
        if (ReplicationLinkRecipes.isCraftable(button.pattern().getStack())) {
            button.setCachedAmount(Math.max(button.cachedAmount(), LINK_AMOUNT));
        }
    }
}
