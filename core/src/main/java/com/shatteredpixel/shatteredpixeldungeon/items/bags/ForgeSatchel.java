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

package com.shatteredpixel.shatteredpixeldungeon.items.bags;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.MaterialItem;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//MOD: a smith's satchel for crafting materials: ingots, cloth, leather and ore.
//Every stack still counts as one slot, so it holds up to 12 different materials.
public class ForgeSatchel extends Bag {

	{
		image = ItemSpriteSheet.FORGE_SATCHEL;
	}

	@Override
	public boolean canHold(Item item) {
		if (item instanceof MaterialItem) {
			return super.canHold(item);
		}
		return false;
	}

	@Override
	public int capacity() {
		return 12;
	}

	@Override
	public int value() {
		return 30;
	}
}
