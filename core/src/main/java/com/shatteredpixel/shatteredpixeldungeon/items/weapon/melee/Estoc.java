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

//MOD: a long, stiff thrusting sword made for finishing wounded foes.
public class Estoc extends MeleeWeapon {

	{
		image = ItemSpriteSheet.RAPIER; //placeholder art until its own sprite is made
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 0.9f;

		tier = 4;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//finishing thrust: +50% against enemies below half health
		if (defender.HP < defender.HT / 2) damage = Math.round(damage * 1.5f);
		return super.proc(attacker, defender, damage);
	}
}
