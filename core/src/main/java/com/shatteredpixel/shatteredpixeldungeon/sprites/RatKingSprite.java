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

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify;
import com.shatteredpixel.shatteredpixeldungeon.utils.Holiday;
import com.watabou.noosa.TextureFilm;

public class RatKingSprite extends MobSprite {

	public boolean festive;
	
	public RatKingSprite() {
		super();

		resetAnims();
	}

	public void resetAnims(){

		int height = 16;

		int c;
		switch (Holiday.getCurrentHoliday()){
			default:
				c = 0;
				break;
			case APRIL_FOOLS:
				height = 17;
				c = 16;
				break;
			case WINTER_HOLIDAYS:
				height = 17;
				c = 24;
				break;
		}

		if (Dungeon.hero != null && Dungeon.hero.armorAbility instanceof Ratmogrify){
			c = 8;
			if (parent != null) aura(0xFFFF00, 5);
		}

		//MOD: redrawn (tools/mobs). Rows of 16 frames: move 0-3, attack 4-7, death 8-11, idle 12-15
		TextureFilm frames = hdFilm( Assets.Sprites.RATKING_HD, 54, 15f / 46f );

		c = 0;   //MOD: one redrawn look for every occasion

		idle = new Animation( 3, true );
		idle.frames( frames, row( c, 12, 12, 13, 12, 14, 15 ) );

		run = new Animation( 8, true );
		run.frames( frames, row( c, 0, 1, 2, 3 ) );

		attack = new Animation( 10, false );
		attack.frames( frames, row( c, 4, 5, 6, 7, 12 ) );

		die = new Animation( 8, false );
		die.frames( frames, row( c, 8, 9, 10, 11 ) );

		play( idle );

	}


	@Override
	public void link(Char ch) {
		super.link(ch);
		if (Dungeon.hero != null && Dungeon.hero.armorAbility instanceof Ratmogrify){
			aura(0xFFFF00, 5);
		}
	}
}
