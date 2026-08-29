package com.shouyun.worldslice;

/**
 * The inclusive block-column range of a World Slice in one dimension.
 *
 * <p>{@code minX} is {@code 0} for the Overworld and Nether, and
 * {@code -thickness / 2} for the End so the slice stays centred on the vanilla
 * dragon-fight origin at {@code X=0}. {@code maxX} is always
 * {@code minX + thickness - 1}, so the total width is exactly {@code thickness}.</p>
 */
public record SliceBounds(int minX, int maxX) {
    /** Total number of block columns in this slice. */
    public int thickness() {
        return maxX - minX + 1;
    }

    /**
     * The block column closest to the slice's horizontal centre. For the End
     * this is always {@code X=0} (or one of the two central columns for even
     * widths), which keeps the vanilla exit portal at the slice centre.
     */
    public int centerX() {
        return (minX + maxX) / 2;
    }

    public boolean contains(int x) {
        return x >= minX && x <= maxX;
    }
}
