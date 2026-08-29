package com.shouyun.worldslice.mixin;

import java.util.List;

import com.shouyun.worldslice.WorldSliceBounds;

import net.minecraft.world.entity.Entity;
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
 * <p>{@code Entity.collectColliders} runs for every entity that moves, so
 * {@link WorldSliceBounds#affectsBoundaryCollision(Entity, Level)} is the single
 * gate that decides who receives the invisible walls. Players and ordinary
 * living entities are blocked; the Ender Dragon, items, experience orbs,
 * projectiles and vehicles pass through the boundary.</p>
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "collectColliders", at = @At("RETURN"), cancellable = true)
    private static void worldslice$addBoundaryCollisions(
        Entity entity,
        Level level,
        List<VoxelShape> collisions,
        AABB boundingBox,
        CallbackInfoReturnable<List<VoxelShape>> cir
    ) {
        if (WorldSliceBounds.affectsBoundaryCollision(entity, level)) {
            cir.setReturnValue(WorldSliceBounds.addBoundaryCollisionWalls(cir.getReturnValue(), level, boundingBox));
        }
    }
}
