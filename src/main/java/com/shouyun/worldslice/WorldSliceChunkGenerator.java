package com.shouyun.worldslice;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.server.level.WorldGenRegion;

/**
 * Keeps the vanilla generator for the single playable chunk column and makes
 * every dependency chunk a lightweight empty chunk through the normal status
 * pipeline. It is installed at runtime and is never serialized as a new
 * dimension generator type.
 *
 * The wrapper intentionally owns no seed, biome source, noise sampler or
 * random instance. Every playable-generation call delegates the original
 * {@code RandomState}, carver {@code seed}, structure state and parent
 * generator unchanged, so the active ServerLevel's World Seed remains the
 * sole source of terrain variation.
 */
public final class WorldSliceChunkGenerator extends ChunkGenerator {
    public static final MapCodec<WorldSliceChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ChunkGenerator.CODEC.fieldOf("parent").forGetter(WorldSliceChunkGenerator::parent)
    ).apply(instance, WorldSliceChunkGenerator::new));

    private final ChunkGenerator parent;

    public WorldSliceChunkGenerator(ChunkGenerator parent) {
        super(parent.getBiomeSource());
        this.parent = parent;
    }

    public static ChunkGenerator wrap(ChunkGenerator generator) {
        return generator instanceof WorldSliceChunkGenerator ? generator : new WorldSliceChunkGenerator(generator);
    }

    public ChunkGenerator parent() {
        return parent;
    }

    private boolean isPlayable(ChunkAccess chunk) {
        return WorldSliceBounds.isValidChunk(chunk.getPos());
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
        }
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        if (isPlayable(chunk)) {
            parent.buildSurface(level, structureManager, random, chunk);
        }
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        if (WorldSliceBounds.isValidChunk(level.getCenter())) {
            parent.spawnOriginalMobs(level);
        }
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        if (isPlayable(chunk)) {
            parent.applyBiomeDecoration(level, chunk, structureManager);
        }
    }

    @Override
    public void createStructures(
        RegistryAccess registryAccess,
        net.minecraft.world.level.chunk.ChunkGeneratorStructureState structureState,
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
        return isPlayable(chunk) ? parent.fillFromNoise(blender, randomState, structureManager, chunk)
            : CompletableFuture.completedFuture(chunk);
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
        return WorldSliceBounds.isInsideX(x) ? parent.getBaseHeight(x, z, type, level, random) : getMinY();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        if (WorldSliceBounds.isInsideX(x)) {
            return parent.getBaseColumn(x, z, height, random);
        }

        BlockState[] air = new BlockState[height.getHeight()];
        java.util.Arrays.fill(air, Blocks.AIR.defaultBlockState());
        return new NoiseColumn(height.getMinBuildHeight(), air);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        parent.addDebugScreenInfo(info, random, pos);
        info.add("World Slice: X=" + WorldSliceBounds.minX() + ".." + WorldSliceBounds.maxX());
    }
}
