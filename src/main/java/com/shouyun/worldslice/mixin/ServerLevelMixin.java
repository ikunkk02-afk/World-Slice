package com.shouyun.worldslice.mixin;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.shouyun.worldslice.WorldSliceBounds;
import com.shouyun.worldslice.WorldSliceChunkGenerator;
import com.shouyun.worldslice.WorldSliceGenerator;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @ModifyArgs(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;<init>")
    )
    private static void worldslice$wrapGenerator(Args args) {
        ServerLevel level = args.get(0);
        ChunkGenerator generator = args.get(5);
        if (WorldSliceBounds.isSupportedDimension(level.dimension()) && !(generator instanceof WorldSliceGenerator)) {
            args.set(5, WorldSliceChunkGenerator.wrap(generator, level));
        }
    }
}
