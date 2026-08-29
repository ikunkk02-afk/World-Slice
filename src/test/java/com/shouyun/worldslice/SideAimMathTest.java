package com.shouyun.worldslice;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SideAimMathTest {
    @Test
    void screenRightAndUpMapToTheSidePlane() {
        SideAimMath.ScreenPosition player = new SideAimMath.ScreenPosition(960.0D, 540.0D, 28.0D);
        Vec3 direction = SideAimMath.directionFromScreen(
            1200.0D,
            300.0D,
            player,
            1920.0D,
            1080.0D,
            30.0D,
            1920.0D / 1080.0D,
            new Vec3(0.0D, 0.0D, 1.0D),
            new Vec3(0.0D, 1.0D, 0.0D)
        );

        assertNotNull(direction);
        assertEquals(0.0D, direction.x, 1.0E-9D);
        assertEquals(1.0D, direction.z > 0.0D ? 1.0D : -1.0D, 1.0D);
        assertEquals(1.0D, direction.y > 0.0D ? 1.0D : -1.0D, 1.0D);
    }

    @Test
    void zeroScreenDeltaKeepsThePreviousDirection() {
        SideAimMath.ScreenPosition player = new SideAimMath.ScreenPosition(1280.0D, 800.0D, 28.0D);
        Vec3 direction = SideAimMath.directionFromScreen(
            1280.0D,
            800.0D,
            player,
            2560.0D,
            1600.0D,
            30.0D,
            2560.0D / 1600.0D,
            new Vec3(0.0D, 0.0D, 1.0D),
            new Vec3(0.0D, 1.0D, 0.0D)
        );

        assertNull(direction);
    }

    @Test
    void sideMovementUsesOppositeKeysWithoutDiagonalBias() {
        assertEquals(1.0F, SideMovementController.calculateImpulse(true, false));
        assertEquals(-1.0F, SideMovementController.calculateImpulse(false, true));
        assertEquals(0.0F, SideMovementController.calculateImpulse(true, true));
        assertEquals(0.0F, SideMovementController.calculateImpulse(false, false));
        assertTrue(SideMovementController.isFacingRight(0.0F));
        assertFalse(SideMovementController.isFacingRight(180.0F));
        assertFalse(SideMovementController.isFacingRight(-180.0F));
    }

    @Test
    void projectionUsesTheViewportAspectRatio() {
        SideAimMath.ScreenPosition widescreen = SideAimMath.projectPoint(
            new Vec3(28.0D, 0.0D, 1.0D),
            Vec3.ZERO,
            new Vec3(1.0D, 0.0D, 0.0D),
            new Vec3(0.0D, 0.0D, -1.0D),
            new Vec3(0.0D, 1.0D, 0.0D),
            1920.0D,
            1080.0D,
            30.0D,
            1920.0D / 1080.0D
        );
        SideAimMath.ScreenPosition ultrawide = SideAimMath.projectPoint(
            new Vec3(28.0D, 0.0D, 1.0D),
            Vec3.ZERO,
            new Vec3(1.0D, 0.0D, 0.0D),
            new Vec3(0.0D, 0.0D, -1.0D),
            new Vec3(0.0D, 1.0D, 0.0D),
            2560.0D,
            1600.0D,
            30.0D,
            2560.0D / 1600.0D
        );

        assertNotNull(widescreen);
        assertNotNull(ultrawide);
        double tangent = Math.tan(Math.toRadians(30.0D) * 0.5D);
        assertEquals(
            1.0D / (28.0D * 2.0D * tangent * (1920.0D / 1080.0D)),
            (widescreen.x() - 960.0D) / 1920.0D,
            1.0E-9D
        );
        assertEquals(
            1.0D / (28.0D * 2.0D * tangent * (2560.0D / 1600.0D)),
            (ultrawide.x() - 1280.0D) / 2560.0D,
            1.0E-9D
        );
    }
}
