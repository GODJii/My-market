package com.mymarket;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/**
 * Shop window with every buyable item, 45 per page, sorted from cheapest to most expensive.
 * Left click = buy the chosen amount, right click = buy 1.
 * Bottom row: previous page, rarity filter, amount, balance, page number, next page.
 */
public class BuyMenu extends ChestMenu {
	private static final int PER_PAGE = 45;
	private static final int[] QTY = {1, 16, 64};
	private static final int SLOT_PREV = 45, SLOT_FILTER = 46, SLOT_QTY = 47, SLOT_BALANCE = 49,
			SLOT_PAGE = 51, SLOT_NEXT = 53;

	private final ServerPlayer player;
	private final SimpleContainer top;
	private int page = 0;
	private int qtyIdx = 0;
	private int tierIdx = 0; // 0 = all, 1.. = Tier.values()[tierIdx-1]
	private List<Item> view = List.of();

	private BuyMenu(int syncId, Inventory inv, SimpleContainer top, ServerPlayer player) {
		super(MenuType.GENERIC_9x6, syncId, inv, top, 6);
		this.player = player;
		this.top = top;
		rebuild();
	}

	public static void open(ServerPlayer player) {
		SimpleContainer top = new SimpleContainer(54);
		player.openMenu(new SimpleMenuProvider(
				(syncId, inv, p) -> new BuyMenu(syncId, inv, top, player),
				Component.literal("ร้านค้า")));
	}

	private long balance() {
		return MoneyData.get(player.getServer()).get(player.getUUID());
	}

	private int amountFor(Item item) {
		return Math.min(QTY[qtyIdx], item.getDefaultInstance().getMaxStackSize());
	}

	private void rebuild() {
		List<Item> all = Prices.buyable();
		if (tierIdx == 0) {
			view = all;
		} else {
			Prices.Tier t = Prices.Tier.values()[tierIdx - 1];
			view = all.stream().filter(i -> Prices.tierOf(i) == t).toList();
		}
		int pages = Math.max(1, (view.size() + PER_PAGE - 1) / PER_PAGE);
		page = Math.max(0, Math.min(page, pages - 1));

		for (int i = 0; i < PER_PAGE; i++) {
			int idx = page * PER_PAGE + i;
			top.setItem(i, idx < view.size() ? display(view.get(idx)) : ItemStack.EMPTY);
		}
		for (int i = PER_PAGE; i < 54; i++) top.setItem(i, Ui.filler());

		if (page > 0) top.setItem(SLOT_PREV, Ui.button(Items.ARROW, Ui.text("หน้าก่อนหน้า", ChatFormatting.YELLOW)));
		if (page < pages - 1) top.setItem(SLOT_NEXT, Ui.button(Items.ARROW, Ui.text("หน้าถัดไป", ChatFormatting.YELLOW)));

		String filterName = tierIdx == 0 ? "ทั้งหมด" : Prices.Tier.values()[tierIdx - 1].label;
		ChatFormatting filterColor = tierIdx == 0 ? ChatFormatting.WHITE : Prices.Tier.values()[tierIdx - 1].color;
		top.setItem(SLOT_FILTER, Ui.button(Items.HOPPER,
				Ui.text("กรองความหายาก: " + filterName, filterColor),
				Ui.text("คลิกซ้าย = ถัดไป / คลิกขวา = ย้อนกลับ", ChatFormatting.GRAY)));
		top.setItem(SLOT_QTY, Ui.button(Items.CHEST,
				Ui.text("จำนวนที่ซื้อต่อครั้ง: " + QTY[qtyIdx], ChatFormatting.YELLOW),
				Ui.text("คลิกซ้าย = เปลี่ยน (1 / 16 / 64)", ChatFormatting.GRAY)));
		top.setItem(SLOT_BALANCE, Ui.button(Items.GOLD_INGOT,
				Ui.text("เงินของคุณ: " + Ui.money(balance()), ChatFormatting.GOLD)));
		top.setItem(SLOT_PAGE, Ui.button(Items.PAPER,
				Ui.text("หน้า " + (page + 1) + " / " + pages, ChatFormatting.WHITE)));
	}

	private ItemStack display(Item item) {
		ItemStack s = item.getDefaultInstance();
		Prices.Tier t = Prices.tierOf(item);
		s.set(DataComponents.LORE, new ItemLore(List.of(
				Ui.text("ความหายาก: " + t.label, t.color),
				Ui.text("ราคาซื้อ: " + Ui.money(Prices.buyPrice(item)) + " ต่อชิ้น", ChatFormatting.YELLOW),
				Ui.text("คลิกซ้าย = ซื้อ " + amountFor(item) + " ชิ้น", ChatFormatting.GRAY),
				Ui.text("คลิกขวา = ซื้อ 1 ชิ้น", ChatFormatting.GRAY))));
		return s;
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player p) {
		// Only plain clicks on the top 54 slots do anything. All else is ignored (no item moving possible).
		if (slotId >= 0 && slotId < 54 && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)) {
			int dir = button == 1 ? -1 : 1;
			if (slotId < PER_PAGE) {
				buy(slotId, button);
			} else if (slotId == SLOT_PREV) {
				page--;
			} else if (slotId == SLOT_NEXT) {
				page++;
			} else if (slotId == SLOT_FILTER) {
				tierIdx = Math.floorMod(tierIdx + dir, Prices.Tier.values().length + 1);
				page = 0;
			} else if (slotId == SLOT_QTY) {
				qtyIdx = Math.floorMod(qtyIdx + dir, QTY.length);
			}
			rebuild();
		}
		sendAllDataToRemote();
	}

	private void buy(int slotId, int button) {
		int idx = page * PER_PAGE + slotId;
		if (idx >= view.size()) return;
		Item item = view.get(idx);

		int amount = button == 1 ? 1 : amountFor(item);
		long cost = Prices.buyPrice(item) * amount;
		MoneyData data = MoneyData.get(player.getServer());

		if (!data.take(player.getUUID(), cost)) {
			player.displayClientMessage(
					Ui.text("เงินไม่พอ ต้องใช้ " + Ui.money(cost), ChatFormatting.RED), true);
			return;
		}
		ItemStack give = new ItemStack(item, amount);
		MutableComponent name = give.getHoverName().copy();
		player.getInventory().add(give);
		if (!give.isEmpty()) {
			player.drop(give, false); // inventory full: drop the rest at the player's feet
		}
		player.displayClientMessage(Component.literal("ซื้อ ").withStyle(ChatFormatting.GREEN)
				.append(name)
				.append(Component.literal(" x" + amount + "  -" + Ui.money(cost)).withStyle(ChatFormatting.GOLD)), true);
	}
}
