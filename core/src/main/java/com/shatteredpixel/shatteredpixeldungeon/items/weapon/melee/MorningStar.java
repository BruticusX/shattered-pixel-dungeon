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
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.WeaponPerks;

//MOD: a spiked club: hits can make the target bleed.
public class MorningStar extends MeleeWeapon {

	{
		image = ItemSpriteSheet.MORNING_STAR;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1.0f;

		tier = 4;
		ACC = 1.1f;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		WeaponPerks.bleed(defender, damage, 0.3f);
		return super.proc(attacker, defender, damage);
	}
}
