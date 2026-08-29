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

        // Keep D/A screen-relative: when the player turns left, forward/backward must be swapped.
        boolean facingRight = minecraft.player == null || isFacingRight(minecraft.player.getYRot());
        boolean forward = isKeyDown(window, facingRight ? InputConstants.KEY_D : InputConstants.KEY_A);
        boolean backward = isKeyDown(window, facingRight ? InputConstants.KEY_A : InputConstants.KEY_D);
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

    static boolean isFacingRight(float yaw) {
        // In Minecraft yaw 0 faces +Z (the right side of the fixed Side Camera view).
        return Math.cos(Math.toRadians(yaw)) >= 0.0D;
    }

    private static boolean isKeyDown(long window, int key) {
        return InputConstants.isKeyDown(window, key);
    }
}
