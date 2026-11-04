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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.MagicalInfusion;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//MOD: alchemy recipes that used a scroll of upgrade now take one obsidian ingot instead
public abstract class ObsidianRecipe extends Recipe {

	protected abstract Class<? extends Item> output();
	protected abstract int energy();

	private static boolean isObsidian(Item i) {
		return i instanceof MaterialItem && ((MaterialItem) i).material == Material.OBSIDIAN
				&& !((MaterialItem) i).ore;
	}

	@Override
	public boolean testIngredients(ArrayList<Item> ingredients) {
		return ingredients.size() == 1 && isObsidian(ingredients.get(0)) && ingredients.get(0).quantity() >= 1;
	}

	@Override
	public int cost(ArrayList<Item> ingredients) {
		return energy();
	}

	@Override
	public Item brew(ArrayList<Item> ingredients) {
		if (!testIngredients(ingredients)) return null;
		ingredients.get(0).quantity(ingredients.get(0).quantity() - 1);
		return sampleOutput(null);
	}

	@Override
	public Item sampleOutput(ArrayList<Item> ingredients) {
		return Reflection.newInstance(output());
	}

	public static class ToInfusion extends ObsidianRecipe {
		@Override protected Class<? extends Item> output() { return MagicalInfusion.class; }
		@Override protected int energy() { return 12; }
	}

	public static class ToEnchantment extends ObsidianRecipe {
		@Override protected Class<? extends Item> output() { return ScrollOfEnchantment.class; }
		@Override protected int energy() { return 6; }
	}
}
