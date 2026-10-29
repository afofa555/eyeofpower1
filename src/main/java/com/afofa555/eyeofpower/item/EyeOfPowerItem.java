package com.afofa555.eyeofpower.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class EyeOfPowerItem extends Item {

    public EyeOfPowerItem(Properties properties) {
        super(properties);
    }

    // Разрешаем надевать предмет в слот головы (в инвентаре и по Shift+клик)
    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, LivingEntity entity) {
        return armorType == EquipmentSlot.HEAD;
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.HEAD;
    }

    // ПКМ с предметом в руке - надеть на голову
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            player.setItemSlot(EquipmentSlot.HEAD, stack.copy());
            stack.setCount(0);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("✦ ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal(" afofa555").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD, ChatFormatting.ITALIC))
                .append(Component.literal(" ✦").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.eyeofpower.line1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.eyeofpower.line2").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.eyeofpower.line3").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
