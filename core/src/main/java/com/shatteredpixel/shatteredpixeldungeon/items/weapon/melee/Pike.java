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

//MOD: reach 3, weak against adjacent enemies, and hits can knock the enemy back.
public class Pike extends MeleeWeapon {

	{
		image = ItemSpriteSheet.PIKE;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 0.85f;

		tier = 5;
		RCH = 3;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = WeaponPerks.upClose(attacker, defender, damage, 0.6f);
		WeaponPerks.knockBack(attacker, defender, 1, 0.25f);
		return super.proc(attacker, defender, damage);
	}
}
