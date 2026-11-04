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

//MOD: a long kite shield: blocks between a round shield and a greatshield, and also blocks magic.
public class KiteShield extends RoundShield {

	{
		image = ItemSpriteSheet.KITE_SHIELD;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 0.9f;

		tier = 5;
	}

	//between a round shield and a greatshield
	@Override
	public int DRMax(int lvl){
		if (material() != null) return Math.round((3 + 1.5f*lvl) * material().damageMult());
		return 5 + Math.round(1.5f*lvl);
	}

	@Override
	public String statsInfo(){
		return com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(this, "stats_desc", DRMax());
	}
}
