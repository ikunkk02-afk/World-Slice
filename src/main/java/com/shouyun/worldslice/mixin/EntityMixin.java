package com.shouyun.worldslice.mixin;

import java.util.List;

import com.shouyun.worldslice.WorldSliceBounds;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds World Slice's side walls to vanilla entity collision resolution.
 *
 * <p>{@code Entity.collectColliders} runs for every entity that moves, so the
 * player check below is the single gate that keeps the invisible walls
 * player-only. Mobs, animals, items, projectiles and vehicles pass through the
 * boundary; only non-spectator players receive the collision shapes.</p>
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "collectColliders", at = @At("RETURN"), cancellable = true)
    private static void worldslice$addPlayerBoundaryCollisions(
        Entity entity,
        Level level,
        List<VoxelShape> collisions,
        AABB boundingBox,
        CallbackInfoReturnable<List<VoxelShape>> cir
    ) {
        if (entity instanceof Player && WorldSliceBounds.affectsPlayerCollision(entity, level)) {
            cir.setReturnValue(WorldSliceBounds.addPlayerCollisionWalls(cir.getReturnValue(), level, boundingBox));
        }
    }
}
