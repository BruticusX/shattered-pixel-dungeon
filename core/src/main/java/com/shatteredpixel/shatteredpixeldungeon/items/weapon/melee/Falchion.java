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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.WeaponPerks;

//MOD: a cleaver-edged sword: the first hit on each enemy deals 40% more damage.
public class Falchion extends MeleeWeapon {

	{
		image = ItemSpriteSheet.FALCHION;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 0.95f;

		tier = 3;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = WeaponPerks.firstHit(defender, damage, 1.4f);
		return super.proc(attacker, defender, damage);
	}
}
