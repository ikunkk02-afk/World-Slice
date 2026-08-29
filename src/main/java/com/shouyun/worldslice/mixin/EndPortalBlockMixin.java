package com.shouyun.worldslice.mixin;

import com.shouyun.worldslice.WorldSliceBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Relocates the End arrival and its obsidian platform into the slice. Vanilla
 * always lands at X=100, which is outside World Slice. The platform centre is
 * moved to the slice centre X (with vanilla's Y/Z kept), so the player never
 * arrives over the void. Returning from the End to the Overworld is left to
 * vanilla bed/spawn logic.
 */
@Mixin(EndPortalBlock.class)
public abstract class EndPortalBlockMixin {
    @Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
    private void worldslice$relocateEndArrival(
        ServerLevel level, Entity entity, BlockPos pos, CallbackInfoReturnable<DimensionTransition> cir
    ) {
        if (level.dimension() == Level.END) {
            return; // End -> Overworld: keep vanilla respawn logic.
        }

        ServerLevel endLevel = level.getServer().getLevel(Level.END);
        if (endLevel == null || !WorldSliceBounds.isWorldSliceLevel(endLevel)) {
            return;
        }

        int centerX = WorldSliceBounds.centerX(endLevel);
        int platformY = ServerLevel.END_SPAWN_POINT.getY() - 1;
        int platformZ = ServerLevel.END_SPAWN_POINT.getZ();
        EndPlatformFeature.createEndPlatform(endLevel, new BlockPos(centerX, platformY, platformZ), true);

        Vec3 arrival = new BlockPos(centerX, ServerLevel.END_SPAWN_POINT.getY(), platformZ)
            .getBottomCenter()
            .subtract(0.0, 1.0, 0.0);

        cir.setReturnValue(new DimensionTransition(
            endLevel,
            arrival,
            entity.getDeltaMovement(),
            Direction.WEST.toYRot(),
            entity.getXRot(),
            DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET)
        ));
    }
}
