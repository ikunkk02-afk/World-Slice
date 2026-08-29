package com.shouyun.worldslice.mixin;

import com.mojang.blaze3d.audio.Listener;
import com.mojang.blaze3d.audio.ListenerTransform;
import com.shouyun.worldslice.SideCameraController;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps positional audio centered on the player while the render camera is offset for Side Camera. */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Shadow
    @Final
    private Listener listener;

    @Shadow
    @Final
    private SoundEngineExecutor executor;

    @Inject(method = "updateSource", at = @At("HEAD"), cancellable = true)
    private void worldslice$keepListenerAtPlayer(Camera renderInfo, CallbackInfo ci) {
        if (!SideCameraController.isEnabled() || !renderInfo.isInitialized()) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        Vec3 playerEyePosition = player.getEyePosition(renderInfo.getPartialTickTime());
        ListenerTransform listenerTransform = new ListenerTransform(
            playerEyePosition,
            new Vec3(renderInfo.getLookVector()),
            new Vec3(renderInfo.getUpVector())
        );
        this.executor.execute(() -> this.listener.setTransform(listenerTransform));
        ci.cancel();
    }
}
