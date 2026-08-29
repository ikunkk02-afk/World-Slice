package com.shouyun.worldslice.mixin;

import com.shouyun.worldslice.SideCameraController;
import com.shouyun.worldslice.SideMovementController;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces only the gameplay movement directions while Side Camera is active. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void worldslice$applySideMovement(boolean slowDown, float slowDownMultiplier, CallbackInfo ci) {
        if (SideCameraController.isEnabled()) {
            SideMovementController.apply((KeyboardInput)(Object)this, slowDown, slowDownMultiplier);
        }
    }
}
