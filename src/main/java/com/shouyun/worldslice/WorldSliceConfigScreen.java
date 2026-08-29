package com.shouyun.worldslice;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A vanilla-style settings screen shared by every World Slice entry point.
 *
 * <p>The {@link ConfigScreenContext} decides what the thickness field edits:
 * in {@link ConfigScreenContext#MAIN_MENU} it edits the new-world default and
 * never touches the network or world state; in
 * {@link ConfigScreenContext#IN_WORLD} it edits the current world's
 * server-authoritative value. The side-camera distance is a client setting in
 * both contexts.</p>
 */
public final class WorldSliceConfigScreen extends Screen {
    private static final int CONTENT_WIDTH = 360;
    private static final int FIELD_WIDTH = 88;

    private final Screen parent;
    private final ConfigScreenContext context;
    private EditBox worldThicknessBox;
    private CameraDistanceSlider cameraDistanceSlider;
    private Button doneButton;
    private int serverWorldThickness;
    private boolean worldThicknessDirty;
    private boolean syncing;
    private boolean updatingWorldThicknessBox;
    private String validationMessage;
    private long serverSettingsRevision;

    public WorldSliceConfigScreen(Screen parent, ConfigScreenContext context) {
        super(Component.translatable("screen.worldslice.title"));
        this.parent = parent;
        this.context = context;
        this.serverWorldThickness = WorldSliceWorldSettings.clientWorldThickness();
        this.serverSettingsRevision = WorldSliceWorldSettings.clientSettingsRevision();
        this.syncing = context == ConfigScreenContext.IN_WORLD && !WorldSliceWorldSettings.isClientWorldSliceActive();
        SideCameraConfig.beginPreview();
    }

    private boolean canEditWorldThickness() {
        if (context == ConfigScreenContext.MAIN_MENU) {
            return true;
        }

        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.hasSingleplayerServer()
            || minecraft.player != null && minecraft.player.hasPermissions(2);
    }

    private boolean isWorldThicknessEditable() {
        return canEditWorldThickness() && !this.syncing;
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int left = center - CONTENT_WIDTH / 2;
        int top = Math.max(30, (this.height - 220) / 2);

        int initialThickness = context == ConfigScreenContext.MAIN_MENU
            ? WorldSliceDefaultsConfig.defaultWorldThickness()
            : this.serverWorldThickness;

        this.worldThicknessBox = this.addRenderableWidget(new EditBox(
            this.font,
            center - FIELD_WIDTH / 2,
            top + 40,
            FIELD_WIDTH,
            20,
            Component.translatable(thicknessLabelKey())
        ));
        this.worldThicknessBox.setMaxLength(4);
        this.worldThicknessBox.setFilter(value -> value.isEmpty()
            || value.chars().allMatch(character -> character >= '0' && character <= '9'));
        this.worldThicknessBox.setValue(Integer.toString(initialThickness));
        this.worldThicknessBox.setEditable(isWorldThicknessEditable());
        this.worldThicknessBox.setResponder(value -> {
            if (!this.updatingWorldThicknessBox) {
                this.worldThicknessDirty = true;
                this.validationMessage = null;
            }
        });

        this.cameraDistanceSlider = this.addRenderableWidget(new CameraDistanceSlider(
            left + 48,
            top + 130,
            CONTENT_WIDTH - 96,
            20,
            SideCameraConfig.cameraDistance()
        ));

        this.doneButton = this.addRenderableWidget(Button.builder(
            Component.translatable("gui.done"),
            button -> applyAndClose()
        ).bounds(center - 100, this.height - 32, 200, 20).build());

        // Ask the server again whenever the screen is opened in-world, so the
        // displayed value is not based only on a stale client-side cache.
        if (context == ConfigScreenContext.IN_WORLD && this.minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(new WorldSliceSettingsRequestPayload(0, false));
        }
    }

    @Override
    public void tick() {
        if (context != ConfigScreenContext.IN_WORLD) {
            return;
        }

        long revision = WorldSliceWorldSettings.clientSettingsRevision();
        if (revision != this.serverSettingsRevision) {
            this.serverSettingsRevision = revision;
            this.syncing = false;
            this.serverWorldThickness = WorldSliceWorldSettings.clientWorldThickness();
            this.worldThicknessBox.setEditable(isWorldThicknessEditable());
            if (!this.worldThicknessDirty) {
                setWorldThicknessBox(this.serverWorldThickness);
            }
        }
    }

    private void setWorldThicknessBox(int thickness) {
        this.updatingWorldThicknessBox = true;
        this.worldThicknessBox.setValue(Integer.toString(thickness));
        this.updatingWorldThicknessBox = false;
    }

    private Integer parseWorldThickness() {
        String value = this.worldThicknessBox.getValue();
        if (value.isEmpty()) {
            return null;
        }

        try {
            int thickness = Integer.parseInt(value);
            return thickness >= WorldSliceWorldSettings.MIN_WORLD_THICKNESS
                && thickness <= WorldSliceWorldSettings.MAX_WORLD_THICKNESS ? thickness : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void applyAndClose() {
        Integer thickness = parseWorldThickness();
        if (thickness == null) {
            this.validationMessage = Component.translatable(
                "worldslice.screen.invalid_thickness",
                WorldSliceWorldSettings.MIN_WORLD_THICKNESS,
                WorldSliceWorldSettings.MAX_WORLD_THICKNESS
            ).getString();
            return;
        }

        SideCameraConfig.commitPreview(this.cameraDistanceSlider.distance());

        if (context == ConfigScreenContext.MAIN_MENU) {
            WorldSliceDefaultsConfig.setDefaultWorldThickness(thickness);
        } else if (isWorldThicknessEditable() && this.worldThicknessDirty && this.minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(new WorldSliceSettingsRequestPayload(thickness, true));
        }
        closeScreen();
    }

    private void closeScreen() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void onClose() {
        SideCameraConfig.cancelPreview();
        closeScreen();
    }

    private String thicknessLabelKey() {
        return context == ConfigScreenContext.MAIN_MENU
            ? "worldslice.screen.default_world_thickness"
            : "worldslice.screen.world_thickness";
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int center = this.width / 2;
        int top = Math.max(30, (this.height - 220) / 2);
        guiGraphics.drawCenteredString(this.font, this.title, center, top, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("worldslice.screen.world"), center - CONTENT_WIDTH / 2, top + 18, 0xFFFFFF);
        guiGraphics.drawString(
            this.font,
            Component.translatable(thicknessLabelKey()),
            center - CONTENT_WIDTH / 2,
            top + 46,
            0xFFFFFF
        );

        if (context == ConfigScreenContext.MAIN_MENU) {
            guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("worldslice.screen.default_thickness_hint"),
                center,
                top + 66,
                0x808080
            );
        } else {
            Integer thickness = parseWorldThickness();
            int displayedThickness = thickness == null ? this.serverWorldThickness : thickness;
            if (this.syncing) {
                guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("worldslice.screen.syncing"),
                    center,
                    top + 66,
                    0xA0A0A0
                );
            } else {
                guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable(
                        "worldslice.screen.chunk_summary",
                        displayedThickness,
                        WorldSliceBounds.chunkWidth(displayedThickness)
                    ),
                    center,
                    top + 66,
                    0xA0A0A0
                );
                guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("worldslice.screen.world_thickness_hint"),
                    center,
                    top + 82,
                    0x808080
                );
            }

            if (!isWorldThicknessEditable()) {
                guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("worldslice.screen.server_only"),
                    center,
                    top + 101,
                    0xFFAA00
                );
            }
        }

        guiGraphics.drawString(this.font, Component.translatable("worldslice.screen.camera"), center - CONTENT_WIDTH / 2, top + 112, 0xFFFFFF);
        guiGraphics.drawCenteredString(
            this.font,
            Component.translatable("worldslice.screen.camera_distance_label"),
            center,
            top + 122,
            0xFFFFFF
        );

        if (this.validationMessage != null) {
            guiGraphics.drawCenteredString(this.font, this.validationMessage, center, this.height - 48, 0xFF5555);
        }
    }

    private final class CameraDistanceSlider extends AbstractSliderButton {
        private CameraDistanceSlider(int x, int y, int width, int height, int distance) {
            super(x, y, width, height, Component.empty(), toSliderValue(distance));
            updateMessage();
        }

        private int distance() {
            return (int)Math.round(Mth.lerp(
                this.value,
                SideCameraConfig.MIN_CAMERA_DISTANCE,
                SideCameraConfig.MAX_CAMERA_DISTANCE
            ));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("worldslice.screen.camera_distance", distance()));
        }

        @Override
        protected void applyValue() {
            SideCameraConfig.previewCameraDistance(distance());
        }
    }

    private static double toSliderValue(int distance) {
        return (double)(SideCameraConfig.clampCameraDistance(distance) - SideCameraConfig.MIN_CAMERA_DISTANCE)
            / (SideCameraConfig.MAX_CAMERA_DISTANCE - SideCameraConfig.MIN_CAMERA_DISTANCE);
    }
}
