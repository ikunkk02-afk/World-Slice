package com.shouyun.worldslice.mixin;

import com.shouyun.worldslice.WorldSliceBounds;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps cached chunk render regions obeying a width changed at runtime. */
@Mixin(RenderChunkRegion.class)
public abstract class RenderChunkRegionMixin {
    @Shadow
    @Final
    protected Level level;

    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    private void worldslice$outsideBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (WorldSliceBounds.isWorldSliceLevel(this.level) && !WorldSliceBounds.isInside(this.level, pos)) {
            cir.setReturnValue(Blocks.VOID_AIR.defaultBlockState());
        }
    }

    @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
    private void worldslice$outsideFluidState(BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
        if (WorldSliceBounds.isWorldSliceLevel(this.level) && !WorldSliceBounds.isInside(this.level, pos)) {
            cir.setReturnValue(Fluids.EMPTY.defaultFluidState());
        }
    }

    @Inject(method = "getBlockEntity", at = @At("HEAD"), cancellable = true)
    private void worldslice$outsideBlockEntity(BlockPos pos, CallbackInfoReturnable<BlockEntity> cir) {
        if (WorldSliceBounds.isWorldSliceLevel(this.level) && !WorldSliceBounds.isInside(this.level, pos)) {
            cir.setReturnValue(null);
        }
    }
}
