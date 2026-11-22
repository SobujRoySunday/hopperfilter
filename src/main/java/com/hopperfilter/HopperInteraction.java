package com.hopperfilter;

import com.hopperfilter.FilterConfig.Layout;
import com.hopperfilter.FilterConfig.SlotSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class HopperInteraction {
	/** Main inventory (hotbar + 3 rows). Armour slots are deliberately never touched. */
	private static final int MAIN_INVENTORY_SIZE = 36;

	private HopperInteraction() {}

	public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND || player.isSpectator()) return InteractionResult.PASS;

		BlockPos pos = hit.getBlockPos();
		if (!(level.getBlockEntity(pos) instanceof HopperBlockEntity hopper)) return InteractionResult.PASS;

		boolean server = level instanceof ServerLevel;
		FilterConfig cfg = FilterConfig.get();
		ItemStack held = player.getMainHandItem();
		boolean hopperEmpty = hopper.isEmpty();

		// 1. Sneak + empty hand on a filled hopper = save its layout as your template.
		if (player.isShiftKeyDown() && held.isEmpty() && !hopperEmpty) {
			if (server) {
				Layout layout = captureLayout(hopper, cfg.firstSlotIsHeldItem);
				cfg.setLayout(player.getUUID(), layout);
				message(player, "Hopper filter layout saved: " + describe(layout));
				level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.3f, 0.8f);
			}
			return InteractionResult.SUCCESS;
		}

		// 2. Right-click an empty hopper = fill it from your inventory.
		if (!hopperEmpty) return InteractionResult.PASS;
		if (cfg.requireSneakToFill && !player.isShiftKeyDown()) return InteractionResult.PASS;

		Layout layout = cfg.layoutFor(player.getUUID());
		if (layout.usesHeldItem() && held.isEmpty()) return InteractionResult.PASS;

		ItemStack heldTemplate = held.copy();
		boolean free = player.isCreative() && !cfg.consumeInCreative;

		List<String> missing = new ArrayList<>();
		ItemStack[] plan;
		if (free) {
			plan = createFree(layout, heldTemplate, missing);
		} else {
			// Dry run against copies first so we never take half the items.
			plan = collect(copyOf(sources(player)), layout, heldTemplate, missing);
		}

		if (!missing.isEmpty()) {
			if (server && (!held.isEmpty() || player.isShiftKeyDown())) {
				message(player, "Hopper filter: missing " + String.join(", ", missing));
			}
			return InteractionResult.PASS; // falls through to the normal hopper GUI
		}

		if (server) {
			if (!free) {
				plan = collect(sources(player), layout, heldTemplate, missing);
				player.getInventory().setChanged();
			}
			int size = Math.min(FilterConfig.HOPPER_SLOTS, hopper.getContainerSize());
			for (int i = 0; i < size; i++) {
				hopper.setItem(i, plan[i]);
			}
			hopper.setChanged();
			level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.3f, 1.4f);
			message(player, "Hopper filter set: " + describe(plan));
		}
		return InteractionResult.SUCCESS;
	}

	// ---------- layout capture ----------

	private static Layout captureLayout(HopperBlockEntity hopper, boolean firstSlotIsHeld) {
		Layout layout = new Layout();
		int size = Math.min(FilterConfig.HOPPER_SLOTS, hopper.getContainerSize());
		for (int i = 0; i < FilterConfig.HOPPER_SLOTS; i++) {
			ItemStack s = i < size ? hopper.getItem(i) : ItemStack.EMPTY;
			if (s.isEmpty()) {
				layout.slots.add(new SlotSpec());
			} else if (i == 0 && firstSlotIsHeld) {
				layout.slots.add(new SlotSpec(FilterConfig.HELD, "", s.getCount()));
			} else {
				layout.slots.add(new SlotSpec(itemId(s.getItem()), customName(s), s.getCount()));
			}
		}
		return layout;
	}

	// ---------- item gathering ----------

	/** Live stacks the mod may take from: main inventory then offhand. */
	private static List<ItemStack> sources(Player player) {
		Inventory inv = player.getInventory();
		List<ItemStack> list = new ArrayList<>(MAIN_INVENTORY_SIZE + 1);
		for (int i = 0; i < MAIN_INVENTORY_SIZE; i++) {
			list.add(inv.getItem(i));
		}
		list.add(player.getOffhandItem());
		return list;
	}

	private static List<ItemStack> copyOf(List<ItemStack> stacks) {
		List<ItemStack> copy = new ArrayList<>(stacks.size());
		for (ItemStack s : stacks) copy.add(s.copy());
		return copy;
	}

	/** Pulls each slot's items out of {@code sources} (mutating them). Records shortfalls in {@code missing}. */
	private static ItemStack[] collect(List<ItemStack> sources, Layout layout, ItemStack heldTemplate, List<String> missing) {
		ItemStack[] plan = emptyPlan();
		for (int slot = 0; slot < FilterConfig.HOPPER_SLOTS; slot++) {
			SlotSpec spec = layout.slots.get(slot);
			if (spec.isEmpty()) continue;

			Matcher m = matcher(spec, heldTemplate);
			if (m == null) {
				missing.add("unknown item '" + spec.item + "' in slot " + (slot + 1));
				continue;
			}

			int remaining = m.count;
			ItemStack out = ItemStack.EMPTY;
			for (ItemStack src : sources) {
				if (remaining <= 0) break;
				if (src.isEmpty() || !m.test.test(src)) continue;
				if (!out.isEmpty() && !ItemStack.isSameItemSameComponents(out, src)) continue;
				int n = Math.min(remaining, src.getCount());
				if (out.isEmpty()) {
					out = src.copyWithCount(n);
				} else {
					out.grow(n);
				}
				src.shrink(n);
				remaining -= n;
			}
			if (remaining > 0) missing.add(remaining + "x " + m.label);
			plan[slot] = out;
		}
		return plan;
	}

	/** Creative mode: build the stacks without touching the inventory. */
	private static ItemStack[] createFree(Layout layout, ItemStack heldTemplate, List<String> missing) {
		ItemStack[] plan = emptyPlan();
		for (int slot = 0; slot < FilterConfig.HOPPER_SLOTS; slot++) {
			SlotSpec spec = layout.slots.get(slot);
			if (spec.isEmpty()) continue;
			if (spec.isHeld()) {
				plan[slot] = heldTemplate.copyWithCount(Math.min(spec.count, heldTemplate.getMaxStackSize()));
				continue;
			}
			Optional<Item> item = resolve(spec.item);
			if (item.isEmpty()) {
				missing.add("unknown item '" + spec.item + "' in slot " + (slot + 1));
				continue;
			}
			ItemStack s = new ItemStack(item.get());
			s.setCount(Math.min(spec.count, s.getMaxStackSize()));
			if (!spec.name.isEmpty()) s.set(DataComponents.CUSTOM_NAME, Component.literal(spec.name));
			plan[slot] = s;
		}
		return plan;
	}

	private record Matcher(Predicate<ItemStack> test, int count, String label) {}

	private static Matcher matcher(SlotSpec spec, ItemStack heldTemplate) {
		if (spec.isHeld()) {
			int count = Math.min(spec.count, heldTemplate.getMaxStackSize());
			return new Matcher(s -> ItemStack.isSameItemSameComponents(s, heldTemplate), count,
					itemId(heldTemplate.getItem()));
		}
		Optional<Item> item = resolve(spec.item);
		if (item.isEmpty()) return null;
		Item it = item.get();
		int count = Math.min(spec.count, new ItemStack(it).getMaxStackSize());
		String name = spec.name;
		Predicate<ItemStack> test = name.isEmpty()
				? s -> s.is(it)
				: s -> s.is(it) && name.equals(customName(s));
		String label = spec.item + (name.isEmpty() ? "" : " named \"" + name + "\"");
		return new Matcher(test, count, label);
	}

	// ---------- helpers ----------

	private static ItemStack[] emptyPlan() {
		ItemStack[] plan = new ItemStack[FilterConfig.HOPPER_SLOTS];
		for (int i = 0; i < plan.length; i++) plan[i] = ItemStack.EMPTY;
		return plan;
	}

	private static Optional<Item> resolve(String id) {
		Identifier key = Identifier.tryParse(id);
		if (key == null) return Optional.empty();
		return BuiltInRegistries.ITEM.getOptional(key);
	}

	private static String itemId(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	private static String customName(ItemStack s) {
		Component name = s.get(DataComponents.CUSTOM_NAME);
		return name == null ? "" : name.getString();
	}

	private static String describe(Layout layout) {
		List<String> parts = new ArrayList<>();
		for (SlotSpec s : layout.slots) {
			if (s.isEmpty()) {
				parts.add("-");
			} else {
				String id = s.isHeld() ? "held item" : s.item.replace("minecraft:", "");
				parts.add(s.count + "x " + id + (s.name.isEmpty() ? "" : " \"" + s.name + "\""));
			}
		}
		return String.join(" | ", parts);
	}

	private static String describe(ItemStack[] plan) {
		List<String> parts = new ArrayList<>();
		for (ItemStack s : plan) {
			parts.add(s.isEmpty() ? "-" : s.getCount() + "x " + s.getHoverName().getString());
		}
		return String.join(" | ", parts);
	}

	private static void message(Player player, String text) {
		if (player instanceof ServerPlayer sp) {
			sp.sendSystemMessage(Component.literal(text), true);
		}
	}
}
