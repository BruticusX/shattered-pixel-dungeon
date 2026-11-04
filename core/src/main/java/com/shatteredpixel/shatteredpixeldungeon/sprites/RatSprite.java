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
import com.watabou.noosa.TextureFilm;

public class RatSprite extends MobSprite {
	
	public RatSprite() {
		super();
		
		//MOD: redrawn. One row of 16 frames: run 0-3, bite 4-7, death 8-11, idle 12-15
		texture( Assets.Sprites.RAT_HD );
		hdScale = 11f / 34f;

		TextureFilm frames = new TextureFilm( texture, 57, 57 );

		idle = new Animation( 3, true );
		idle.frames( frames, 12, 12, 12, 13, 13, 12, 12, 14, 15, 14 );

		run = new Animation( 10, true );
		run.frames( frames, 0, 1, 2, 3 );

		attack = new Animation( 14, false );
		attack.frames( frames, 4, 5, 6, 7, 12 );

		die = new Animation( 8, false );
		die.frames( frames, 9, 10, 11 );

		play( idle );
	}
}
