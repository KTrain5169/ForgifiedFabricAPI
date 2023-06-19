/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.test.rendering;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class VoidChunkGenerator extends ChunkGenerator {
	public static final Codec<VoidChunkGenerator> CODEC = RecordCodecBuilder.create((instance) ->
			instance.group(RegistryOps.retrieveGetter(Registries.BIOME))
					.apply(instance, instance.stable(VoidChunkGenerator::new)));

	public VoidChunkGenerator(HolderGetter<Biome> biomeRegistry) {
		super(new FixedBiomeSource(biomeRegistry.getOrThrow(Biomes.PLAINS)));
	}

	@Override
	protected Codec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	@Override
	public void applyCarvers(WorldGenRegion pLevel, long pSeed, RandomState pRandom, BiomeManager pBiomeManager, StructureManager pStructureManager, ChunkAccess pChunk, GenerationStep.Carving pStep) {
		
	}

	@Override
	public void buildSurface(WorldGenRegion pLevel, StructureManager pStructureManager, RandomState pRandom, ChunkAccess pChunk) {

	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion pLevel) {

	}

	@Override
	public int getGenDepth() {
		return 0;
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Executor pExecutor, Blender pBlender, RandomState pRandom, StructureManager pStructureManager, ChunkAccess pChunk) {
		return CompletableFuture.completedFuture(pChunk);
	}

	@Override
	public int getSeaLevel() {
		return 0;
	}

	@Override
	public int getMinY() {
		return 0;
	}

	@Override
	public int getBaseHeight(int pX, int pZ, Heightmap.Types pType, LevelHeightAccessor pLevel, RandomState pRandom) {
		return 0;
	}

	@Override
	public NoiseColumn getBaseColumn(int pX, int pZ, LevelHeightAccessor pHeight, RandomState pRandom) {
		return new NoiseColumn(0, new BlockState[0]);
	}

	@Override
	public void addDebugScreenInfo(List<String> pInfo, RandomState pRandom, BlockPos pPos) {

	}
}