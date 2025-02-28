package dev.shadowsoffire.apotheosis.garden;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class EnderLeadItem extends Item {

    public EnderLeadItem() {
        super(new Properties().stacksTo(1).durability(15));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (stack.getOrCreateTagElement("entity_data").isEmpty() && (entity instanceof Animal || entity instanceof OwnableEntity)) {
            CompoundTag tag = entity.serializeNBT();
            if (!player.level().isClientSide) {
                entity.discard();
                stack.getTag().put("entity_data", tag);
                stack.getTag().putString("name", entity.getDisplayName().getString());
                this.playSound(player);
            }
            return true;
        }
        return super.onLeftClickEntity(stack, player, entity);

    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand usedHand) {
        player.displayClientMessage(Component.translatable("info.zenith.ender_lead_left_click"), true);
        return super.interactLivingEntity(stack, player, entity, usedHand);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        CompoundTag tag = ctx.getItemInHand().getOrCreateTagElement("entity_data");
        if (!tag.isEmpty()) {
            BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
            if (!ctx.getLevel().isClientSide) {
                Entity e = EntityType.loadEntityRecursive(tag, ctx.getLevel(), a -> a);
                if (e != null) {
                    e.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    ctx.getLevel().addFreshEntity(e);
                    ctx.getItemInHand().getTag().remove("entity_data");
                    ctx.getItemInHand().getTag().remove("name");
                    this.playSound(ctx.getPlayer());
                    ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), pl -> pl.broadcastBreakEvent(ctx.getHand()));
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.FAIL;
    }

    public static CompoundTag getShareTag(CompoundTag tag) {
        if (tag == null) return null;
        tag = tag.copy();
        CompoundTag entity = new CompoundTag();
        if (tag.getCompound("entity_data").contains("id")) entity.putString("id", tag.getCompound("entity_data").getString("id"));
        tag.put("entity_data", entity);
        return tag;
    }

    void playSound(Player player) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.AMBIENT, 1, 1);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if (stack.hasTag()) {
            CompoundTag tag = stack.getOrCreateTagElement("entity_data");
            if (tag.isEmpty()) tooltip.add(Component.translatable("info.zenith.noentity").withStyle(ChatFormatting.GRAY));
            else {
                tooltip.add(Component.translatable("info.zenith.containedentity", stack.getTag().getString("name")).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && !stack.getOrCreateTagElement("entity_data").isEmpty();
    }

}
