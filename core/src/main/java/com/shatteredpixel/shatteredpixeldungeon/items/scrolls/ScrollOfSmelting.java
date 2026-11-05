/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
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

package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Forge;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//MOD: new scroll. Always identified. A furnace on paper: melts one weapon or armor into its
//material, wherever you are.
public class ScrollOfSmelting extends InventoryScroll {

	private static final int IMG = ItemSpriteSheet.SCROLL_SMELTING;

	{
		image = IMG;
	}

	@Override
	public void reset() {
		super.reset();
		image = IMG; //not part of the rune system, so keep our own sprite
	}

	@Override
	public boolean isKnown() {
		return true;
	}

	@Override
	public void setKnown() {
		//always known, nothing to record
	}

	@Override
	protected boolean usableOnItem(Item item) {
		return Forge.isGear(item) && Forge.canMelt(Dungeon.hero, item);
	}

	@Override
	protected void onItemSelected(Item item) {
		Forge.melt(Dungeon.hero, item);
	}

	@Override
	public int value() {
		return 30 * quantity;
	}
}
