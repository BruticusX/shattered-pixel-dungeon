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
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;

//MOD: new enchantment. The weapon knits itself back together: every killing blow restores
//5% of its durability (more with arcana).
public class Enduring extends Weapon.Enchantment {

	private static ItemSprite.Glowing BRONZE = new ItemSprite.Glowing( 0xD9A35A );

	public static final float REPAIR = 5f;

	@Override
	public int proc(Weapon weapon, Char attacker, Char defender, int damage) {
		if (damage >= defender.HP) {
			weapon.repair(REPAIR * Math.max(1f, procChanceMultiplier(attacker)));
		}
		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return BRONZE;
	}
}
