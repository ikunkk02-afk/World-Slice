package com.shouyun.worldslice.mixin;

import com.shouyun.worldslice.WorldSliceBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    private void worldslice$outsideBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        Level level = (Level) (Object) this;
        if (WorldSliceBounds.isWorldSliceLevel(level) && !WorldSliceBounds.isInside(pos)) {
            cir.setReturnValue(Blocks.VOID_AIR.defaultBlockState());
        }
    }

    @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
    private void worldslice$outsideFluidState(BlockPos pos, CallbackInfoReturnable<net.minecraft.world.level.material.FluidState> cir) {
        Level level = (Level) (Object) this;
        if (WorldSliceBounds.isWorldSliceLevel(level) && !WorldSliceBounds.isInside(pos)) {
            cir.setReturnValue(Fluids.EMPTY.defaultFluidState());
        }
    }

    @Inject(
        method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void worldslice$rejectOutsideWrites(
        BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir
    ) {
        Level level = (Level) (Object) this;
        if (WorldSliceBounds.isWorldSliceLevel(level) && !WorldSliceBounds.isInside(pos)) {
            cir.setReturnValue(false);
        }
    }
}
