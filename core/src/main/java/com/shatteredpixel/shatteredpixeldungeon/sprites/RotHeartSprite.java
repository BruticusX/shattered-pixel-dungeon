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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;

public class RotHeartSprite extends MobSprite {

	private Emitter cloud;

	public RotHeartSprite(){
		super();

		perspectiveRaise = 0.2f;

		//MOD: redrawn (tools/mobs). Rows of 16 frames: move 0-3, attack 4-7, death 8-11, idle 12-15
		TextureFilm frames = hdFilm( Assets.Sprites.ROT_HEART_HD, 50, 14f / 42f );
		int c = 0;

		idle = new MovieClip.Animation( 4, true );
		idle.frames( frames, row( c, 0, 1, 2, 3, 12, 13, 14, 15 ) );

		run = new MovieClip.Animation( 4, true );
		run.frames( frames, row( c, 0, 1, 2, 3 ) );

		attack = new MovieClip.Animation( 10, false );
		attack.frames( frames, row( c, 4, 5, 6, 7, 0 ) );

		die = new MovieClip.Animation( 8, false );
		die.frames( frames, row( c, 8, 9, 10, 11, 11 ) );

		play( idle );
	}

	@Override
	public void link( Char ch ) {
		super.link( ch );

		renderShadow = false;

		if (cloud == null) {
			cloud = emitter();
			cloud.pour( Speck.factory(Speck.TOXIC), 0.7f );
		}
	}

	@Override
	public void turnTo(int from, int to) {
		//do nothing
	}

	@Override
	public void update() {

		super.update();

		if (cloud != null) {
			cloud.visible = visible;
		}
	}

	@Override
	public void die() {
		super.die();

		if (cloud != null) {
			cloud.on = false;
		}
	}

	@Override
	public int blood() {
		return 0xFF88CC44;
	}
}
