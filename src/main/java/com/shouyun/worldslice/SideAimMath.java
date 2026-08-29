package com.shouyun.worldslice;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Pure projection and angle math used by the client-only side-camera aim controller. */
public final class SideAimMath {
    private SideAimMath() {
    }

    public static Vec3 flattenToSidePlane(Vec3 direction, Vec3 fallback) {
        Vec3 flattened = new Vec3(0.0D, direction.y, direction.z);
        if (flattened.lengthSqr() > SideCameraConfig.AIM_EPSILON * SideCameraConfig.AIM_EPSILON) {
            return flattened.normalize();
        }

        Vec3 fallbackFlattened = new Vec3(0.0D, fallback.y, fallback.z);
        if (fallbackFlattened.lengthSqr() > SideCameraConfig.AIM_EPSILON * SideCameraConfig.AIM_EPSILON) {
            return fallbackFlattened.normalize();
        }

        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    public static ScreenPosition projectPoint(
        Vec3 point,
        Vec3 cameraPosition,
        Vec3 cameraForward,
        Vec3 cameraLeft,
        Vec3 cameraUp,
        double screenWidth,
        double screenHeight,
        double verticalFovDegrees,
        double aspectRatio
    ) {
        if (screenWidth <= 0.0D || screenHeight <= 0.0D || aspectRatio <= 0.0D) {
            return null;
        }

        Vec3 relative = point.subtract(cameraPosition);
        double depth = relative.dot(cameraForward);
        if (depth <= SideCameraConfig.AIM_EPSILON) {
            return null;
        }

        double halfVerticalFovTangent = Math.tan(Math.toRadians(verticalFovDegrees) * 0.5D);
        double horizontalPixelsPerSlope = screenWidth / (2.0D * halfVerticalFovTangent * aspectRatio);
        double verticalPixelsPerSlope = screenHeight / (2.0D * halfVerticalFovTangent);
        double leftOffset = relative.dot(cameraLeft);
        double upOffset = relative.dot(cameraUp);

        return new ScreenPosition(
            screenWidth * 0.5D - leftOffset / depth * horizontalPixelsPerSlope,
            screenHeight * 0.5D - upOffset / depth * verticalPixelsPerSlope,
            depth
        );
    }

    public static Vec3 directionFromScreen(
        double crosshairX,
        double crosshairY,
        ScreenPosition playerPosition,
        double screenWidth,
        double screenHeight,
        double verticalFovDegrees,
        double aspectRatio,
        Vec3 screenRight,
        Vec3 screenUp
    ) {
        if (playerPosition == null || screenWidth <= 0.0D || screenHeight <= 0.0D || aspectRatio <= 0.0D) {
            return null;
        }

        double halfVerticalFovTangent = Math.tan(Math.toRadians(verticalFovDegrees) * 0.5D);
        double horizontalSlope = (crosshairX - playerPosition.x()) / screenWidth
            * 2.0D * halfVerticalFovTangent * aspectRatio;
        double verticalSlope = (playerPosition.y() - crosshairY) / screenHeight
            * 2.0D * halfVerticalFovTangent;

        Vec3 direction = screenRight.scale(horizontalSlope).add(screenUp.scale(verticalSlope));
        Vec3 sidePlaneDirection = new Vec3(0.0D, direction.y, direction.z);
        if (sidePlaneDirection.lengthSqr() <= SideCameraConfig.AIM_EPSILON * SideCameraConfig.AIM_EPSILON) {
            return null;
        }

        return sidePlaneDirection.normalize();
    }

    public static float yawFor(Vec3 direction, float fallbackYaw) {
        double horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (horizontalLength <= SideCameraConfig.AIM_EPSILON) {
            return fallbackYaw;
        }

        return Mth.wrapDegrees((float)Math.toDegrees(Math.atan2(-direction.x, direction.z)));
    }

    public static float pitchFor(Vec3 direction) {
        double horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        return Mth.clamp((float)Math.toDegrees(Math.atan2(-direction.y, horizontalLength)), -90.0F, 90.0F);
    }

    public record ScreenPosition(double x, double y, double depth) {
    }
}
