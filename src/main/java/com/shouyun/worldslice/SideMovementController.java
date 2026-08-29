package com.shouyun.worldslice;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;

/** Applies the Terraria-style physical movement layout while Side Camera is active. */
public final class SideMovementController {
    private SideMovementController() {
    }

    public static void apply(KeyboardInput input, boolean slowDown, float slowDownMultiplier) {
        Minecraft minecraft = Minecraft.getInstance();
        long window = minecraft.getWindow().getWindow();

        // Side Camera layout: D/A are forward/back and W/S are left/right.
        boolean forward = isKeyDown(window, InputConstants.KEY_D);
        boolean backward = isKeyDown(window, InputConstants.KEY_A);
        boolean left = isKeyDown(window, InputConstants.KEY_W);
        boolean right = isKeyDown(window, InputConstants.KEY_S);

        input.up = forward;
        input.down = backward;
        input.left = left;
        input.right = right;
        input.forwardImpulse = calculateImpulse(forward, backward);
        input.leftImpulse = calculateImpulse(left, right);

        if (slowDown) {
            input.leftImpulse *= slowDownMultiplier;
            input.forwardImpulse *= slowDownMultiplier;
        }
    }

    static float calculateImpulse(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        }
        return positive ? 1.0F : -1.0F;
    }

    private static boolean isKeyDown(long window, int key) {
        return InputConstants.isKeyDown(window, key);
    }
}
