package com.shouyun.worldslice.mixin;

import com.shouyun.worldslice.SideCameraController;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Inject(method = "setup", at = @At("TAIL"))
    private void worldslice$applySidePosition(
        net.minecraft.world.level.BlockGetter level,
        net.minecraft.world.entity.Entity entity,
        boolean detached,
        boolean thirdPersonReverse,
        float partialTick,
        CallbackInfo ci
    ) {
        Camera camera = (Camera) (Object) this;
        if (SideCameraController.isEnabled() && camera.getEntity() != null) {
            double partial = camera.getPartialTickTime();
            double x = net.minecraft.util.Mth.lerp(partial, entity.xo, entity.getX())
                - com.shouyun.worldslice.SideCameraConfig.cameraDistance();
            double z = net.minecraft.util.Mth.lerp(partial, entity.zo, entity.getZ());
            this.setPosition(new Vec3(x, camera.getPosition().y, z));
        }
    }
}
