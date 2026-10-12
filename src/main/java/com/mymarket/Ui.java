package com.mymarket;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.Locale;

/** Small helpers for building GUI buttons and text. */
public final class Ui {
	private Ui() {}

	public static Component text(String s, ChatFormatting color) {
		return Component.literal(s).withStyle(Style.EMPTY.withColor(color).withItalic(false));
	}

	public static String money(long v) {
		return String.format(Locale.US, "%,d", v);
	}

	public static ItemStack button(Item item, Component name, Component... lore) {
		ItemStack s = new ItemStack(item);
		s.set(DataComponents.CUSTOM_NAME, name);
		if (lore.length > 0) s.set(DataComponents.LORE, new ItemLore(List.of(lore)));
		return s;
	}

	public static ItemStack filler() {
		return button(Items.GRAY_STAINED_GLASS_PANE, text(" ", ChatFormatting.GRAY));
	}
}
