package com.shouyun.worldslice.mixin;

import com.shouyun.worldslice.WorldSliceBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin {
    @Inject(method = "ensureCanWrite", at = @At("HEAD"), cancellable = true)
    private void worldslice$rejectOutsideGeneration(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        WorldGenRegion region = (WorldGenRegion) (Object) this;
        if (WorldSliceBounds.isWorldSliceLevel(region.getLevel()) && !WorldSliceBounds.isInside(region.getLevel(), pos)) {
            cir.setReturnValue(false);
        }
    }
}
