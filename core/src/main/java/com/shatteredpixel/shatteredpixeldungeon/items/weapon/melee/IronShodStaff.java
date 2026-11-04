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

//MOD: a heavier, iron-capped staff: blocks more than a quarterstaff, but swings slower.
public class IronShodStaff extends Quarterstaff {

	{
		image = ItemSpriteSheet.IRONSHOD_STAFF;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 0.95f;

		tier = 4;
	}

	//blocks half again as much as a quarterstaff
	@Override
	public int defenseFactor( Char owner ) {
		return Math.round(super.defenseFactor(owner) * 1.5f) + 1;
	}
}
