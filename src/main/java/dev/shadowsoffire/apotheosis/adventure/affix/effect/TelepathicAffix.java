package dev.shadowsoffire.apotheosis.adventure.affix.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Teleport drops to the player
 */
public class TelepathicAffix extends Affix {

    public static final Codec<TelepathicAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            LootRarity.CODEC.fieldOf("min_rarity").forGetter(a -> a.minRarity))
        .apply(inst, TelepathicAffix::new));

    public static Vec3 blockDropTargetPos = null;

    protected LootRarity minRarity;

    public TelepathicAffix(LootRarity minRarity) {
        super(AffixType.ABILITY);
        this.minRarity = minRarity;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return (cat.isRanged() || cat.isLightWeapon() || cat.isBreaker()) && rarity.isAtLeast(this.minRarity);
    }

    @Override
    public void addInformation(ItemStack stack, LootRarity rarity, float level, Consumer<Component> list) {
        LootCategory cat = LootCategory.forItem(stack);
        String type = cat.isRanged() || cat.isWeapon() ? "weapon" : "tool";
        list.accept(Component.translatable("affix." + this.getId() + ".desc." + type));
    }

    @Override
    public boolean enablesTelepathy() {
        return true;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    public static void drops(DamageSource source, Collection<ItemEntity> drops) {
        boolean canTeleport = false;
        Vec3 targetPos = null;
        if (source.getDirectEntity() instanceof AbstractArrow arrow && arrow.getOwner() != null) {
            canTeleport = AffixHelper.streamAffixes(arrow).anyMatch(AffixInstance::enablesTelepathy);
            targetPos = arrow.getOwner().position();
        }
        else if (source.getDirectEntity() instanceof LivingEntity living) {
            ItemStack weapon = living.getMainHandItem();
            canTeleport = AffixHelper.streamAffixes(weapon).anyMatch(AffixInstance::enablesTelepathy);
            targetPos = living.position();
        }

        if (canTeleport) {
            for (ItemEntity item : drops) {
                item.setPos(targetPos.x, targetPos.y, targetPos.z);
                item.setPickUpDelay(0);
            }
        }
    }

}
