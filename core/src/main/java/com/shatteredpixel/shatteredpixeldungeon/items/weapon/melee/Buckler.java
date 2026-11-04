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

//MOD: a small fist shield: blocks little, but perfectly parries the first attack from each enemy.
public class Buckler extends RoundShield {

	{
		image = ItemSpriteSheet.BUCKLER;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1.1f;

		tier = 2;
	}

	//half a round shield's block
	@Override
	public int DRMax(int lvl){
		if (material() != null) return Math.max(1, Math.round((1 + lvl/2f) * material().damageMult()));
		return 2 + lvl/2;
	}

	@Override
	public String statsInfo(){
		return com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(this, "stats_desc", DRMax());
	}

	//put on an enemy once the buckler has parried its first attack
	public static class Parried extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff {
		{ revivePersists = true; }
	}

	//set while a parry is being resolved, so the hero's defense verb can say so
	public static class ParryTracker extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff {}
}
