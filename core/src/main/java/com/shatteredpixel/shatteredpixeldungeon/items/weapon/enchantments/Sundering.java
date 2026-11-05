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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Random;

//MOD: new enchantment. Hits can crack the target open, making it take 33% more damage for a few turns.
public class Sundering extends Weapon.Enchantment {

	private static ItemSprite.Glowing STEEL = new ItemSprite.Glowing( 0xAAB4C8 );

	@Override
	public int proc(Weapon weapon, Char attacker, Char defender, int damage) {
		//20% base chance, scaling up with arcana like other enchantments
		float procChance = 0.2f * procChanceMultiplier(attacker);
		if (defender.isAlive() && Random.Float() < procChance) {
			Buff.prolong(defender, Vulnerable.class, 3f * Math.max(1f, procChance / 0.2f));
		}
		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return STEEL;
	}
}
