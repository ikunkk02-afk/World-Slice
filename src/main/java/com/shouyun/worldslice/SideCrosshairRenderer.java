package com.shouyun.worldslice;

import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Replaces the fixed center crosshair with the Side Camera virtual cursor. */
@EventBusSubscriber(modid = WorldSlice.MODID, value = Dist.CLIENT)
public final class SideCrosshairRenderer {
    private static final ResourceLocation CROSSHAIR_SPRITE = ResourceLocation.withDefaultNamespace("hud/crosshair");
    private static final ResourceLocation ATTACK_INDICATOR_FULL_SPRITE =
        ResourceLocation.withDefaultNamespace("hud/crosshair_attack_indicator_full");
    private static final ResourceLocation ATTACK_INDICATOR_BACKGROUND_SPRITE =
        ResourceLocation.withDefaultNamespace("hud/crosshair_attack_indicator_background");
    private static final ResourceLocation ATTACK_INDICATOR_PROGRESS_SPRITE =
        ResourceLocation.withDefaultNamespace("hud/crosshair_attack_indicator_progress");

    private SideCrosshairRenderer() {
    }

    @SubscribeEvent
    public static void hideVanillaCrosshair(RenderGuiLayerEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (SideCameraController.isEnabled()
            && minecraft.screen == null
            && VanillaGuiLayers.CROSSHAIR.equals(event.getName())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void renderVirtualCrosshair(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!SideCameraController.isEnabled()
            || minecraft.screen != null
            || minecraft.getOverlay() != null
            || minecraft.player == null) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int x = Mth.clamp((int)Math.round(SideAimController.getVirtualCrosshairX()), 0, guiGraphics.guiWidth());
        int y = Mth.clamp((int)Math.round(SideAimController.getVirtualCrosshairY()), 0, guiGraphics.guiHeight());
        guiGraphics.blitSprite(CROSSHAIR_SPRITE, x - 7, y - 7, 15, 15);
        renderAttackIndicator(guiGraphics, minecraft, x, y);
    }

    private static void renderAttackIndicator(GuiGraphics guiGraphics, Minecraft minecraft, int x, int y) {
        if (minecraft.options.attackIndicator().get() != AttackIndicatorStatus.CROSSHAIR || minecraft.player == null) {
            return;
        }

        float attackStrength = minecraft.player.getAttackStrengthScale(0.0F);
        boolean fullyChargedAgainstLivingEntity = minecraft.crosshairPickEntity instanceof LivingEntity livingEntity
            && attackStrength >= 1.0F
            && minecraft.player.getCurrentItemAttackStrengthDelay() > 5.0F
            && livingEntity.isAlive();
        int indicatorX = x - 8;
        int indicatorY = y + 9;

        if (fullyChargedAgainstLivingEntity) {
            guiGraphics.blitSprite(ATTACK_INDICATOR_FULL_SPRITE, indicatorX, indicatorY, 16, 16);
        } else if (attackStrength < 1.0F) {
            int progressWidth = (int)(attackStrength * 17.0F);
            guiGraphics.blitSprite(ATTACK_INDICATOR_BACKGROUND_SPRITE, indicatorX, indicatorY, 16, 4);
            guiGraphics.blitSprite(
                ATTACK_INDICATOR_PROGRESS_SPRITE,
                16,
                4,
                0,
                0,
                indicatorX,
                indicatorY,
                progressWidth,
                4
            );
        }
    }
}
