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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Bundle;

//MOD: new enchantment. Each hit in a row on the same enemy deals 10% more damage, up to +50%.
//Switching targets resets the chain.
public class Frenzied extends Weapon.Enchantment {

	private static ItemSprite.Glowing EMBER = new ItemSprite.Glowing( 0xFF6A30 );

	public static final int MAX_STACKS = 5;

	@Override
	public int proc(Weapon weapon, Char attacker, Char defender, int damage) {
		Frenzy f = Buff.affect(attacker, Frenzy.class);
		if (f.target == defender.id()) {
			f.stacks = Math.min(MAX_STACKS, f.stacks + 1);
		} else {
			f.target = defender.id();
			f.stacks = 0;
		}
		float bonus = 0.1f * f.stacks * Math.max(1f, procChanceMultiplier(attacker));
		return Math.round(damage * (1f + bonus));
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return EMBER;
	}

	//tracks the current chain; invisible, stays until the hero switches targets
	public static class Frenzy extends Buff {
		public int target = -1;
		public int stacks = 0;

		private static final String TARGET = "target";
		private static final String STACKS = "stacks";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(TARGET, target);
			bundle.put(STACKS, stacks);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			target = bundle.getInt(TARGET);
			stacks = bundle.getInt(STACKS);
		}
	}
}
