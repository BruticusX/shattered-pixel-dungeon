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
import com.shatteredpixel.shatteredpixeldungeon.items.Forge;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.MaterialItem;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.FurnaceSprite;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;

//MOD: melts unused gear back into materials and smelts ore into ingots
public class Furnace extends ForgeStation {

	{
		spriteClass = FurnaceSprite.class;
	}

	@Override
	protected void openMenu() {
		GameScene.show(new WndOptions(sprite(), Messages.titleCase(name()),
				Messages.get(this, "menu"),
				Messages.get(this, "melt"), Messages.get(this, "smelt")) {
			@Override
			protected void onSelect(int index) {
				if (index == 0) select(meltSelector);
				else if (index == 1) select(smeltSelector);
			}
		});
	}

	private com.watabou.noosa.Image sprite() {
		return new FurnaceSprite();
	}

	private final WndBag.ItemSelector meltSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(Furnace.class, "melt_prompt");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return Forge.isGear(item) && Forge.canMelt(Dungeon.hero, item);
		}

		@Override
		public void onSelect(Item item) {
			if (item == null || !Forge.canMelt(Dungeon.hero, item)) return;
			MaterialItem result = new MaterialItem(Forge.material(item));
			GameScene.show(new WndOptions(Messages.titleCase(item.name()),
					Messages.get(Furnace.class, "melt_confirm", item.name(), Forge.meltYield(item), result.name()),
					Messages.get(Furnace.class, "yes"), Messages.get(Furnace.class, "no")) {
				@Override
				protected void onSelect(int index) {
					if (index == 0) Forge.melt(Dungeon.hero, item);
				}
			});
		}
	};

	private final WndBag.ItemSelector smeltSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(Furnace.class, "smelt_prompt");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof MaterialItem && ((MaterialItem) item).ore;
		}

		@Override
		public void onSelect(Item item) {
			if (item != null) Forge.melt(Dungeon.hero, item);
		}
	};
}
