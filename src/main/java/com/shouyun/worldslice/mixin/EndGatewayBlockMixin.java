package com.shouyun.worldslice.mixin;

import com.shouyun.worldslice.WorldSliceBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps End Gateway destinations inside the slice. The gateway's computed Z
 * destination is preserved; only X is mapped back into the playable columns.
 * No new safe-landing search is performed, so if the mapped column has no
 * terrain the player may need to fly (see README Known Issues).
 */
@Mixin(EndGatewayBlock.class)
public abstract class EndGatewayBlockMixin {
    @Inject(method = "getPortalDestination", at = @At("RETURN"), cancellable = true)
    private void worldslice$clampGatewayDestination(
        ServerLevel level, Entity entity, BlockPos pos, CallbackInfoReturnable<DimensionTransition> cir
    ) {
        DimensionTransition transition = cir.getReturnValue();
        if (transition == null || !WorldSliceBounds.isWorldSliceLevel(transition.newLevel())) {
            return;
        }

        double safeX = WorldSliceBounds.clampPlayerX(
            transition.newLevel(),
            entity,
            transition.pos().x()
        );
        if (safeX != transition.pos().x()) {
            cir.setReturnValue(new DimensionTransition(
                transition.newLevel(),
                new Vec3(safeX, transition.pos().y(), transition.pos().z()),
                transition.speed(),
                transition.yRot(),
                transition.xRot(),
                transition.missingRespawnBlock(),
                transition.postDimensionTransition()
            ));
        }
    }
}
