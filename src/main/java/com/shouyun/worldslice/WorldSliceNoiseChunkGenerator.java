package com.shouyun.worldslice;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * World Slice wrapper for vanilla noise generation.
 *
 * This class deliberately remains a {@link NoiseBasedChunkGenerator}. ChunkMap
 * uses that runtime type to select the world's real NoiseGeneratorSettings
 * when it creates RandomState. A plain ChunkGenerator decorator would make
 * ChunkMap use NoiseGeneratorSettings.dummy(), flattening seed-dependent
 * terrain and biome noise before any delegated generation method is called.
 */
public final class WorldSliceNoiseChunkGenerator extends NoiseBasedChunkGenerator implements WorldSliceGenerator {
    public static final MapCodec<WorldSliceNoiseChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        NoiseBasedChunkGenerator.CODEC.codec().fieldOf("parent").forGetter(WorldSliceNoiseChunkGenerator::parentNoiseGenerator)
    ).apply(instance, WorldSliceNoiseChunkGenerator::new));

    private final NoiseBasedChunkGenerator parent;
    private final IntSupplier thicknessSupplier;

    public WorldSliceNoiseChunkGenerator(NoiseBasedChunkGenerator parent) {
        this(parent, () -> WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS);
    }

    public WorldSliceNoiseChunkGenerator(NoiseBasedChunkGenerator parent, IntSupplier thicknessSupplier) {
        super(parent.getBiomeSource(), parent.generatorSettings());
        this.parent = parent;
        this.thicknessSupplier = thicknessSupplier;
    }

    @Override
    public ChunkGenerator parent() {
        return parent;
    }

    @Override
    public int worldThickness() {
        return WorldSliceWorldSettings.sanitize(thicknessSupplier.getAsInt());
    }

    private NoiseBasedChunkGenerator parentNoiseGenerator() {
        return parent;
    }

    private boolean isPlayable(ChunkAccess chunk) {
        return WorldSliceBounds.doesChunkIntersectSlice(chunk.getPos(), worldThickness());
    }

    private boolean isPartial(ChunkAccess chunk) {
        return WorldSliceBounds.isPartialBoundaryChunk(chunk.getPos(), worldThickness());
    }

    private void trimIfPartial(ChunkAccess chunk) {
        if (isPartial(chunk)) {
            WorldSliceBounds.trimChunkToSlice(chunk, worldThickness());
        }
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(
        RandomState randomState, Blender blender, StructureManager structureManager, ChunkAccess chunk
    ) {
        return isPlayable(chunk) ? parent.createBiomes(randomState, blender, structureManager, chunk)
            : super.createBiomes(randomState, blender, structureManager, chunk);
    }

    @Override
    public void applyCarvers(
        WorldGenRegion level,
        long seed,
        RandomState random,
        BiomeManager biomeManager,
        StructureManager structureManager,
        ChunkAccess chunk,
        GenerationStep.Carving step
    ) {
        if (isPlayable(chunk)) {
            parent.applyCarvers(level, seed, random, biomeManager, structureManager, chunk, step);
            trimIfPartial(chunk);
        }
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        if (isPlayable(chunk)) {
            parent.buildSurface(level, structureManager, random, chunk);
            trimIfPartial(chunk);
        }
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        if (WorldSliceBounds.doesChunkIntersectSlice(level.getCenter(), worldThickness())) {
            parent.spawnOriginalMobs(level);
            trimIfPartial(level.getChunk(level.getCenter().x, level.getCenter().z));
        }
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        if (isPlayable(chunk)) {
            parent.applyBiomeDecoration(level, chunk, structureManager);
            trimIfPartial(chunk);
        }
    }

    @Override
    public void createStructures(
        RegistryAccess registryAccess,
        ChunkGeneratorStructureState structureState,
        StructureManager structureManager,
        ChunkAccess chunk,
        StructureTemplateManager structureTemplateManager
    ) {
        if (isPlayable(chunk)) {
            parent.createStructures(registryAccess, structureState, structureManager, chunk, structureTemplateManager);
        }
    }

    @Override
    public void createReferences(WorldGenLevel level, StructureManager structureManager, ChunkAccess chunk) {
        if (isPlayable(chunk)) {
            parent.createReferences(level, structureManager, chunk);
        }
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
        Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk
    ) {
        if (!isPlayable(chunk)) {
            return CompletableFuture.completedFuture(chunk);
        }

        return parent.fillFromNoise(blender, randomState, structureManager, chunk)
            .thenApply(generated -> {
                trimIfPartial(generated);
                return generated;
            });
    }

    @Override
    public int getSeaLevel() {
        return parent.getSeaLevel();
    }

    @Override
    public int getMinY() {
        return parent.getMinY();
    }

    @Override
    public int getGenDepth() {
        return parent.getGenDepth();
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor level) {
        return parent.getSpawnHeight(level);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return WorldSliceBounds.isInsideX(x, worldThickness()) ? parent.getBaseHeight(x, z, type, level, random) : getMinY();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        if (WorldSliceBounds.isInsideX(x, worldThickness())) {
            return parent.getBaseColumn(x, z, height, random);
        }

        BlockState[] air = new BlockState[height.getHeight()];
        java.util.Arrays.fill(air, Blocks.AIR.defaultBlockState());
        return new NoiseColumn(height.getMinBuildHeight(), air);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        parent.addDebugScreenInfo(info, random, pos);
        info.add("World Slice: X=" + WorldSliceBounds.minX() + ".." + WorldSliceBounds.maxX(worldThickness()));
    }
}
