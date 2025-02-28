package dev.shadowsoffire.apotheosis.adventure.affix.reforging;

import blue.endless.jankson.annotation.Nullable;
import dev.shadowsoffire.apotheosis.adventure.Adventure;
import dev.shadowsoffire.apotheosis.adventure.Adventure.Items;
import dev.shadowsoffire.apotheosis.adventure.Adventure.Menus;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.cca.ZenithComponents;
import dev.shadowsoffire.apotheosis.util.ApothMiscUtil;
import dev.shadowsoffire.placebo.menu.MenuUtil;
import dev.shadowsoffire.placebo.menu.PlaceboContainerMenu;
import dev.shadowsoffire.placebo.util.EnchantmentUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;


public class ReforgingMenu extends PlaceboContainerMenu {

    public static final String REFORGE_SEED = "apoth_reforge_seed";
    protected final BlockPos pos;
    protected final ReforgingTableTile tile;
    protected final Player player;
    protected SimpleContainer itemInventory = new SimpleContainer(1);

    protected final RandomSource random = new XoroshiroRandomSource(0);
    protected final int[] seed = new int[2];
    protected final int[] costs = new int[3];
    protected DataSlot needsReset = DataSlot.standalone();

    public ReforgingMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    public ReforgingMenu(int id, Inventory inv, BlockPos pos) {
        super(Menus.REFORGING, id, inv);
        this.player = inv.player;
        this.pos = pos;
        this.tile = (ReforgingTableTile) this.level.getBlockEntity(pos);
        this.addSlot(new UpdatingSlot(this.itemInventory, 0, 25, 24, stack -> !LootCategory.forItem(stack).isNone()){
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
        });
        this.addSlot(new Slot(this.tile.inventory, 0, 15, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ReforgingMenu.this.tile.isValidRarityMat(stack);
            }
        });
        this.addSlot(new Slot(this.tile.inventory, 1, 35, 45){
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.GEM_DUST);
            }
        });
        this.addSlot(new UpdatingSlot(this.tile.inventory, 0, 15, 45, this.tile::isValidRarityMat));
        this.addSlot(new UpdatingSlot(this.tile.inventory, 1, 35, 45, stack -> stack.is(Items.GEM_DUST)));
        this.addPlayerSlots(inv, 8, 84);
        this.mover.registerRule((stack, slot) -> slot >= this.playerInvStart && !LootCategory.forItem(stack).isNone(), 0, 1);
        this.mover.registerRule((stack, slot) -> slot >= this.playerInvStart && this.tile.isValidRarityMat(stack), 1, 2);
        this.mover.registerRule((stack, slot) -> slot >= this.playerInvStart && stack.getItem() == Items.GEM_DUST, 2, 3);
        this.mover.registerRule((stack, slot) -> slot < this.playerInvStart, this.playerInvStart, this.hotbarStart + 9);
        this.registerInvShuffleRules();

        this.updateSeed();
        this.addDataSlot(this.needsReset);
        this.addDataSlot(DataSlot.shared(this.seed, 0));
        this.addDataSlot(DataSlot.shared(this.seed, 1));
        this.addDataSlot(DataSlot.shared(this.costs, 0));
        this.addDataSlot(DataSlot.shared(this.costs, 1));
        this.addDataSlot(DataSlot.shared(this.costs, 2));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.itemInventory);
    }

    protected void updateSeed() {
        if (this.player.getCustomData().contains(REFORGE_SEED)) {
            this.player.getCustomData().remove(REFORGE_SEED);
        }
        int seed = ZenithComponents.REFORGING_SEED.get(this.player).getValue();
        if (seed == 0) {
            seed = this.player.getRandom().nextInt();
            ZenithComponents.REFORGING_SEED.get(this.player).setValue(seed);
        }
        this.seed[0] = MenuUtil.split(seed, false);
        this.seed[1] = MenuUtil.split(seed, true);
    }

    public int getSeed() {
        return MenuUtil.merge(this.seed[0], this.seed[1], true);
    }

    @Override
    public boolean clickMenuButton(Player player, int slot) {
        if (slot >= 0 && slot < 3) {

            ItemStack input = this.getSlot(0).getItem();
            LootRarity rarity = this.getRarity();
            ReforgingRecipe recipe = this.tile.getRecipeFor(rarity);
            if (recipe == null || input.isEmpty() || this.needsReset()) return false;

            int dust = this.getDustCount();
            int dustCost = this.getDustCost(slot);
            int mats = this.getMatCount();
            int matCost = this.getMatCost(slot);
            int levels = this.player.experienceLevel;
            int levelCost = this.getLevelCost(slot);

            if ((dust < dustCost || mats < matCost || levels < levelCost) && !player.isCreative()) return false;

            if (!player.level().isClientSide) {
                RandomSource rand = this.random;
                rand.setSeed(this.getSeed() ^ BuiltInRegistries.ITEM.getKey(input.getItem()).hashCode() + slot);
                ItemStack output = LootController.createLootItem(input.copy(), rarity, rand);
                this.getSlot(0).set(output);

                if (!player.isCreative()) {
                    this.getSlot(1).getItem().shrink(matCost);
                    this.getSlot(2).getItem().shrink(dustCost);
                }
                EnchantmentUtils.chargeExperience(player, ApothMiscUtil.getExpCostForSlot(levelCost, slot));
                ZenithComponents.REFORGING_SEED.get(this.player).setValue(player.getRandom().nextInt());
                this.updateSeed();
                this.needsReset.set(1);
            }

            player.playSound(SoundEvents.EVOKER_CAST_SPELL, 0.99F, this.level.random.nextFloat() * 0.25F + 1F);
            player.playSound(SoundEvents.AMETHYST_CLUSTER_STEP, 0.34F, this.level.random.nextFloat() * 0.2F + 0.8F);
            player.playSound(SoundEvents.SMITHING_TABLE_USE, 0.45F, this.level.random.nextFloat() * 0.5F + 0.75F);
            return true;
        }
        return super.clickMenuButton(player, slot);
    }

    public int getMatCount() {
        return this.getSlot(1).getItem().getCount();
    }

    public int getDustCount() {
        return this.getSlot(2).getItem().getCount();
    }

    @Nullable
    public LootRarity getRarity() {
        ItemStack s = this.getSlot(1).getItem();
        if (s.isEmpty()) return null;
        return RarityRegistry.getMaterialRarity(s.getItem()).getOptional().orElse(null);
    }

    public int getDustCost(int slot) {
        return this.costs[0] * ++slot;
    }

    public int getMatCost(int slot) {
        return this.costs[1] * ++slot;
    }

    public int getLevelCost(int slot) {
        return this.costs[2] * ++slot;
    }

    public boolean needsReset() {
        return this.needsReset.get() != 0;
    }

    @Override
    public void slotsChanged(Container pContainer) {
        LootRarity rarity = this.getRarity();
        if (rarity != null) {
            ReforgingRecipe recipe = this.tile.getRecipeFor(rarity);
            if (recipe != null) {
                this.costs[0] = recipe.dustCost();
                this.costs[1] = recipe.matCost();
                this.costs[2] = recipe.levelCost();
            }
        }
        if (ReforgingMenu.this.needsReset()) {
            ReforgingMenu.this.needsReset.set(0);
        }
        super.slotsChanged(pContainer);
        this.tile.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level.isClientSide) return true;
        return this.level.getBlockState(this.pos).getBlock() == Adventure.Blocks.REFORGING_TABLE || this.level.getBlockState(this.pos).getBlock() == Adventure.Blocks.SIMPLE_REFORGING_TABLE;
    }

}
