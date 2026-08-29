package com.shouyun.worldslice.mixin;

import com.shouyun.worldslice.SideAimController;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Redirects only vanilla player turning; MouseHandler still owns relative capture and button state. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void worldslice$handleSideAim(double ignoredDeltaSeconds, CallbackInfo ci) {
        if (SideAimController.handleMouseMovement()) {
            ci.cancel();
        }
    }
}
