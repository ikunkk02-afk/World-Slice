package com.shouyun.worldslice.mixin;

import com.shouyun.worldslice.WorldSliceBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Keeps the Nether portal coordinate mapping slice-aware.
 *
 * <p>Vanilla scales both X and Z by 8 when crossing between the Overworld and
 * the Nether. For World Slice, X is the slice depth and must not be scaled as
 * a horizontal distance: the destination X is kept at the source depth and
 * clamped into the shared slice.</p>
 */
@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin {
    /**
     * Replaces the scaled exit X with the source depth before the portal is
     * found or created, so {@code findClosestPortalPosition} and
     * {@code createPortal} both operate inside the slice.
     */
    @ModifyArgs(
        method = "getPortalDestination",
        at = @At(value = "INVOKE", target = "getExitPortal")
    )
    private void worldslice$keepSliceDepth(Args args) {
        ServerLevel destination = args.get(0);
        Entity entity = args.get(1);
        BlockPos exitPos = args.get(3);
        if (!WorldSliceBounds.isWorldSliceLevel(destination)) {
            return;
        }

        int sourceX = BlockPos.containing(entity.position()).getX();
        int x = WorldSliceBounds.clampBlockX(destination, sourceX);
        args.set(3, new BlockPos(x, exitPos.getY(), exitPos.getZ()));
    }

    /**
     * Forces auto-created portals to orient their width along Z so the frame
     * occupies a single X column and cannot cross the virtual void boundary.
     */
    @ModifyArgs(
        method = "getExitPortal",
        at = @At(value = "INVOKE", target = "createPortal")
    )
    private void worldslice$forcePortalZAxis(Args args) {
        args.set(1, Direction.Axis.Z);
    }

    @Inject(method = "getPortalDestination", at = @At("RETURN"), cancellable = true)
    private void worldslice$clampPortalDestination(
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
