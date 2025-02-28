package dev.shadowsoffire.apotheosis.adventure.loot;

import dev.shadowsoffire.apotheosis.adventure.Adventure.Affixes;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.affix.socket.SocketHelper;
import dev.shadowsoffire.apotheosis.adventure.compat.GameStagesCompat.IStaged;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.placebo.reload.WeightedDynamicRegistry.IDimensional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nullable;
import java.util.*;

public class LootController {

    /**
     * @see {link LootController#createLootItem(ItemStack, LootCategory, LootRarity, Random)}
     */
    public static ItemStack createLootItem(ItemStack stack, LootRarity rarity, RandomSource rand) {
        LootCategory cat = LootCategory.forItem(stack);
        if (cat.isNone()) return stack;
        return createLootItem(stack, cat, rarity, rand);
    }

    static Random jRand = new Random();

    /**
     * Modifies an ItemStack with affixes of the target category and rarity.
     *
     * @param stack  The ItemStack.
     * @param cat    The LootCategory. Should be valid for the item being passed.
     * @param rarity The target Rarity.
     * @param rand   The Random
     * @return The modifed ItemStack (note the original is not preserved, but the stack is returned for simplicity).
     */
    public static ItemStack createLootItem(ItemStack stack, LootCategory cat, LootRarity rarity, RandomSource rand) {
        Set<DynamicHolder<Affix>> selected = new LinkedHashSet<>();
        MutableInt sockets = new MutableInt(0);
        float durability = 0;
        for (LootRarity.LootRule rule : rarity.getRules()) {
            if (rule.type() == AffixType.DURABILITY) durability = rule.chance();
            else rule.execute(stack, rarity, selected, sockets, rand);
        }

        Map<DynamicHolder<? extends Affix>, AffixInstance> loaded = new HashMap<>();
        List<AffixInstance> nameList = new ArrayList<>(selected.size());
        for (DynamicHolder<Affix> a : selected) {
            AffixInstance inst = new AffixInstance(a, stack, RarityRegistry.INSTANCE.holder(rarity), rand.nextFloat());
            loaded.put(a, inst);
            nameList.add(inst);
        }
        if (nameList.size() == 0) {
            throw new RuntimeException(String.format("Failed to locate any affixes for %s{%s} with category %s and rarity %s.", stack.getItem(), stack.getTag(), cat, rarity));
        }

        // Socket and Durability handling, which is non-standard.
        if (sockets.intValue() > 0) {
            SocketHelper.setSockets(stack, sockets.intValue());
        }

        if (durability > 0) {
            loaded.put(Affixes.DURABLE, new AffixInstance(Affixes.DURABLE, stack, RarityRegistry.INSTANCE.holder(rarity), durability + AffixHelper.step(-0.07F, 14, 0.01F).get(rand.nextFloat())));
        }

        jRand.setSeed(rand.nextLong());
        Collections.shuffle(nameList, jRand);
        String key = nameList.size() > 1 ? "misc.zenith.affix_name.three" : "misc.zenith.affix_name.two";
        MutableComponent name = Component.translatable(key, nameList.get(0).getName(true), "", nameList.size() > 1 ? nameList.get(1).getName(false) : "").withStyle(Style.EMPTY.withColor(rarity.getColor()));

        AffixHelper.setRarity(stack, rarity);
        AffixHelper.setAffixes(stack, loaded);
        AffixHelper.setName(stack, name);

        return stack;
    }

    /**
     * Pulls a random LootRarity and AffixLootEntry, and generates an Affix Item
     *
     * @param rand   Random
     * @param rarity The rarity, or null if it should be randomly selected.
     * @param level  The world, since affix loot entries are per-dimension.
     * @return An affix item, or an empty ItemStack if no entries were available for the dimension.
     */
    public static ItemStack createRandomLootItem(RandomSource rand, @Nullable LootRarity rarity, Player player, ServerLevelAccessor level) {
        AffixLootEntry entry = AffixLootRegistry.INSTANCE.getRandomItem(rand, player.getLuck(), IDimensional.matches(level.getLevel()), IStaged.matches(player));
        if (entry == null) return ItemStack.EMPTY;
        if (rarity == null) rarity = LootRarity.random(rand, player.getLuck(), entry);
        return createLootItem(entry.getStack(), entry.getType(), rarity, rand);
    }

}
