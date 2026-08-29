package com.shouyun.worldslice;

/**
 * Distinguishes the two ways the World Slice settings screen can be opened.
 *
 * <p>{@link #MAIN_MENU} is used by the NeoForge mod-list "Config" button. It
 * runs without a {@code Minecraft.level}, {@code Minecraft.player} or
 * connection, so it must never send packets or touch world state.</p>
 *
 * <p>{@link #IN_WORLD} is used by the O shortcut and the pause-menu button. It
 * may read the current server-provided world thickness and, with sufficient
 * permission, request changes through the normal packet flow.</p>
 */
public enum ConfigScreenContext {
    MAIN_MENU,
    IN_WORLD
}
