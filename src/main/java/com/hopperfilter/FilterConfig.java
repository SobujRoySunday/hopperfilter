package com.hopperfilter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stored at config/hopperfilter.json. Edits to the file are picked up automatically
 * (checked on each hopper click), so a server admin can tweak it without a restart.
 */
public class FilterConfig {
	public static final int HOPPER_SLOTS = 5;
	public static final String HELD = "$held";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hopperfilter.json");

	private static volatile FilterConfig instance;
	private static long lastModified = -1L;

	// ---- user-facing settings ----

	/** If true, you must sneak while right-clicking an empty hopper to fill it. */
	public boolean requireSneakToFill = false;

	/** If true, slot 1 always uses the item in your main hand (one layout works for every item you sort). */
	public boolean firstSlotIsHeldItem = true;

	/** If false, creative players get the items for free instead of having them taken from inventory. */
	public boolean consumeInCreative = false;

	/** Layout used by anyone who has not saved their own. */
	public Layout defaultLayout = Layout.defaults();

	/** Per-player layouts saved in game, keyed by player UUID. */
	public Map<String, Layout> playerLayouts = new HashMap<>();

	public static class SlotSpec {
		/** "" = leave empty, "$held" = item in main hand, otherwise an item id like "minecraft:stick". */
		public String item = "";
		/** Optional custom (anvil) name the item must have. "" = any name. */
		public String name = "";
		public int count = 0;

		public SlotSpec() {}

		public SlotSpec(String item, String name, int count) {
			this.item = item;
			this.name = name;
			this.count = count;
		}

		public boolean isEmpty() {
			return item == null || item.isBlank() || count <= 0;
		}

		public boolean isHeld() {
			return HELD.equals(item);
		}
	}

	public static class Layout {
		public List<SlotSpec> slots = new ArrayList<>();

		static Layout defaults() {
			Layout l = new Layout();
			l.slots.add(new SlotSpec(HELD, "", 18));
			for (int i = 1; i < HOPPER_SLOTS; i++) {
				l.slots.add(new SlotSpec("minecraft:stick", "Filter", 1));
			}
			return l;
		}

		boolean usesHeldItem() {
			for (SlotSpec s : slots) {
				if (!s.isEmpty() && s.isHeld()) return true;
			}
			return false;
		}

		void normalise() {
			if (slots == null) slots = new ArrayList<>();
			slots.replaceAll(s -> s == null ? new SlotSpec() : s);
			while (slots.size() < HOPPER_SLOTS) slots.add(new SlotSpec());
			while (slots.size() > HOPPER_SLOTS) slots.remove(slots.size() - 1);
			for (SlotSpec s : slots) {
				if (s.item == null) s.item = "";
				if (s.name == null) s.name = "";
				s.item = s.item.trim();
			}
		}
	}

	// ---- access ----

	public static FilterConfig get() {
		try {
			long mod = Files.exists(FILE) ? Files.getLastModifiedTime(FILE).toMillis() : -1L;
			if (instance == null || mod != lastModified) {
				synchronized (FilterConfig.class) {
					if (instance == null || mod != lastModified) {
						load();
					}
				}
			}
		} catch (IOException e) {
			HopperFilterMod.LOGGER.warn("Could not check config timestamp", e);
			if (instance == null) instance = new FilterConfig();
		}
		return instance;
	}

	public Layout layoutFor(UUID player) {
		Layout l = playerLayouts.get(player.toString());
		return l != null ? l : defaultLayout;
	}

	public synchronized void setLayout(UUID player, Layout layout) {
		playerLayouts.put(player.toString(), layout);
		save();
	}

	private static void load() {
		FilterConfig cfg = null;
		if (Files.exists(FILE)) {
			try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
				cfg = GSON.fromJson(r, FilterConfig.class);
			} catch (Exception e) {
				HopperFilterMod.LOGGER.error("Bad config at {}, using defaults (file left untouched)", FILE, e);
				instance = new FilterConfig();
				instance.normalise();
				lastModified = timestamp();
				return;
			}
		}
		boolean fresh = cfg == null;
		if (fresh) cfg = new FilterConfig();
		cfg.normalise();
		instance = cfg;
		if (fresh) {
			cfg.save();
		} else {
			lastModified = timestamp();
		}
	}

	private void normalise() {
		if (defaultLayout == null) defaultLayout = Layout.defaults();
		defaultLayout.normalise();
		if (playerLayouts == null) playerLayouts = new HashMap<>();
		playerLayouts.values().removeIf(l -> l == null);
		playerLayouts.values().forEach(Layout::normalise);
	}

	private void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(this, w);
			}
			lastModified = timestamp();
		} catch (IOException e) {
			HopperFilterMod.LOGGER.error("Could not save config to {}", FILE, e);
		}
	}

	private static long timestamp() {
		try {
			return Files.getLastModifiedTime(FILE).toMillis();
		} catch (IOException e) {
			return -1L;
		}
	}
}
