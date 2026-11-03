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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//MOD: new tier 3 weapon. Weaker than a sword, but every hit heals the wielder.
//Extends Sword so it shares the Duelist's cleave ability.
public class Bloodletter extends Sword {

	{
		image = ItemSpriteSheet.BLOODLETTER;
		tier = 3;
	}

	@Override
	public int max(int lvl) {
		return  4*(tier+1) +    //16 base, down from sword's 20
				lvl*(tier+1);   //scaling unchanged
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		//heals for 20% of damage dealt (min 1) against real enemies
		if (attacker.isAlive()
				&& attacker.alignment != defender.alignment
				&& (defender.alignment != Char.Alignment.NEUTRAL || defender instanceof Mimic)) {
			int healAmt = Math.max(1, Math.round(damage * 0.2f));
			healAmt = Math.min(healAmt, attacker.HT - attacker.HP);
			if (healAmt > 0) {
				attacker.HP += healAmt;
				if (attacker.sprite != null) {
					attacker.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healAmt), FloatingText.HEALING);
				}
			}
		}

		return damage;
	}
}
