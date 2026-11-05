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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.WeaponPerks;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//MOD: a light, quick staff. Blocks like a quarterstaff, and its rapid strikes can daze.
public class BoStaff extends Quarterstaff {

	{
		image = ItemSpriteSheet.QUARTERSTAFF; //placeholder art until its own sprite is made
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1.2f;

		tier = 1;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		WeaponPerks.daze(defender, 0.2f);
		return super.proc(attacker, defender, damage);
	}
}
