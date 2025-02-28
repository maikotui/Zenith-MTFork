package dev.shadowsoffire.apotheosis.adventure.spawner;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.adventure.AdventureConfig;
import dev.shadowsoffire.apotheosis.mixin.accessors.BaseSpawnerAccessor;
import dev.shadowsoffire.apotheosis.util.SpawnerStats;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.reload.WeightedDynamicRegistry.ILuckyWeighted;
import dev.shadowsoffire.placebo.util.ChestBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class RogueSpawner implements CodecProvider<RogueSpawner>, ILuckyWeighted {

    public static final Codec<RogueSpawner> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    Codec.INT.fieldOf("weight").forGetter(RogueSpawner::getWeight),
                    SpawnerStats.CODEC.fieldOf("stats").forGetter(RogueSpawner::getStats),
                    ResourceLocation.CODEC.fieldOf("loot_table").forGetter(RogueSpawner::getLootTableId),
                    SimpleWeightedRandomList.wrappedCodec(SpawnData.CODEC).fieldOf("spawn_potentials").forGetter(s -> s.spawnPotentials))
            .apply(inst, RogueSpawner::new));

    public static final Block[] FILLER_BLOCKS = { Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_COBBLESTONE, Blocks.CRYING_OBSIDIAN, Blocks.LODESTONE };

    protected final int weight;
    protected final SpawnerStats stats;
    protected final SimpleWeightedRandomList<SpawnData> spawnPotentials;
    protected final ResourceLocation lootTable;

    public RogueSpawner(int weight, SpawnerStats stats, ResourceLocation lootTable, SimpleWeightedRandomList<SpawnData> potentials) {
        this.weight = weight;
        this.stats = stats;
        this.lootTable = lootTable;
        this.spawnPotentials = potentials;
    }

    @Override
    public int getWeight() {
        return this.weight;
    }

    @Override
    public float getQuality() {
        return 0;
    }

    public SpawnerStats getStats() {
        return this.stats;
    }

    public ResourceLocation getLootTableId() {
        return this.lootTable;
    }

    @SuppressWarnings("deprecation")
    public void place(WorldGenLevel world, BlockPos pos, RandomSource rand) {
        world.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
        SpawnerBlockEntity entity = (SpawnerBlockEntity) world.getBlockEntity(pos);
        this.stats.apply(entity);
        ((BaseSpawnerAccessor) entity.getSpawner()).setSpawnPotentials(this.spawnPotentials);
        ((BaseSpawnerAccessor) entity.getSpawner()).callSetNextSpawnData(null, pos, this.spawnPotentials.getRandomValue(rand).get());
        ChestBuilder.place(world, pos.below(), rand.nextFloat() <= AdventureConfig.spawnerValueChance ? Apoth.LootTables.CHEST_VALUABLE : this.lootTable);
        world.setBlock(pos.above(), FILLER_BLOCKS[rand.nextInt(FILLER_BLOCKS.length)].defaultBlockState(), 2);
        for (Direction f : Plane.HORIZONTAL) {
            if (world.getBlockState(pos.relative(f)).isAir()) {
                BooleanProperty side = (BooleanProperty) Blocks.VINE.getStateDefinition().getProperty(f.getOpposite().getName());
                world.setBlock(pos.relative(f), Blocks.VINE.defaultBlockState().setValue(side, true), 2);
            }
        }
    }

    @Override
    public Codec<? extends RogueSpawner> getCodec() {
        return CODEC;
    }

}
