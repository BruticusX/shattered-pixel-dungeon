/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Forge;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Material;
import com.shatteredpixel.shatteredpixeldungeon.items.MaterialItem;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.AnvilSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;

import java.util.ArrayList;

//MOD: reforges gear into other materials, enhances and repairs it, and melds umbralite into
//wands, rings and other upgradable items with obsidian
public class Anvil extends ForgeStation {

	{
		spriteClass = AnvilSprite.class;
	}

	@Override
	protected void openMenu() {
		Hero hero = Dungeon.hero;
		GameScene.show(new WndOptions(new AnvilSprite(), Messages.titleCase(name()),
				Messages.get(this, "menu", MaterialItem.summary(hero)),
				Messages.get(this, "reforge"), Messages.get(this, "enhance"),
				Messages.get(this, "repair"), Messages.get(this, "meld")) {
			@Override
			protected void onSelect(int index) {
				switch (index) {
					case 0: select(reforgeSelector); break;
					case 1: select(enhanceSelector); break;
					case 2: select(repairSelector);  break;
					case 3: select(infuseSelector);  break;
				}
			}
		});
	}

	private static void notEnough(Material m, int need) {
		GLog.w(Messages.get(Anvil.class, "not_enough", need, new MaterialItem(m).name(),
				MaterialItem.count(Dungeon.hero, m)));
	}

	private final WndBag.ItemSelector reforgeSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(Anvil.class, "reforge_prompt");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return Forge.canReforge(item);
		}

		@Override
		public void onSelect(Item item) {
			if (item == null || !Forge.canReforge(item)) return;
			Hero hero = Dungeon.hero;
			int cost = Forge.cost(item);
			ArrayList<Material> options = new ArrayList<>();
			ArrayList<String> labels = new ArrayList<>();
			for (Material m : Material.values()) {
				if (m.kind != Forge.kind(item) || m == Forge.material(item)) continue;
				options.add(m);
				labels.add(Messages.get(Anvil.class, "material_option", Messages.titleCase(m.title()),
						MaterialItem.count(hero, m), Forge.reforgeCost(item, m)));
			}
			GameScene.show(new WndOptions(Messages.titleCase(item.name()),
					Messages.get(Anvil.class, "reforge_choose", cost),
					labels.toArray(new String[0])) {
				@Override
				protected boolean enabled(int index) {
					return MaterialItem.count(Dungeon.hero, options.get(index)) >= Forge.reforgeCost(item, options.get(index));
				}

				@Override
				protected void onSelect(int index) {
					Forge.reforge(Dungeon.hero, item, options.get(index));
				}
			});
		}
	};

	private final WndBag.ItemSelector enhanceSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(Anvil.class, "enhance_prompt");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return Forge.isGear(item) && item.isUpgradable();
		}

		@Override
		public void onSelect(Item item) {
			if (item == null) return;
			if (!Forge.enhance(Dungeon.hero, item)) notEnough(Forge.material(item), Forge.cost(item));
		}
	};

	private final WndBag.ItemSelector repairSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(Anvil.class, "repair_prompt");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return Forge.canRepair(item);
		}

		@Override
		public void onSelect(Item item) {
			if (item == null) return;
			if (!Forge.repair(Dungeon.hero, item)) notEnough(Forge.material(item), 1);
		}
	};

	private final WndBag.ItemSelector infuseSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(MaterialItem.class, "meld_prompt");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return Forge.canMeld(item);
		}

		@Override
		public void onSelect(Item item) {
			if (item == null) return;
			if (!Forge.meld(Dungeon.hero, item)) notEnough(Material.OBSIDIAN, 1);
		}
	};
}
