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
import com.shatteredpixel.shatteredpixeldungeon.effects.ShieldHalo;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ElmoParticle;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;

public class WandmakerSprite extends MobSprite {
	
	private ShieldHalo shield;
	
	public WandmakerSprite() {
		super();
		
		//MOD: redrawn (tools/mobs). Rows of 16 frames: move 0-3, attack 4-7, death 8-11, idle 12-15
		TextureFilm frames = hdFilm( Assets.Sprites.MAKER_HD, 50, 14f / 44f );
		int c = 0;

		idle = new Animation( 3, true );
		idle.frames( frames, row( c, 12, 12, 13, 12, 14, 15, 12, 4, 5, 6, 7 ) );

		run = new Animation( 8, true );
		run.frames( frames, row( c, 0, 1, 2, 3 ) );

		die = new Animation( 8, false );
		die.frames( frames, row( c, 8, 9, 10, 11 ) );

		play( idle );
	}
	
	@Override
	public void link( Char ch ) {
		super.link( ch );
		add(State.SHIELDED);
	}
	
	@Override
	public void die() {
		super.die();

		processStateRemoval(State.SHIELDED);
		emitter().start( ElmoParticle.FACTORY, 0.03f, 60 );

		if (visible) {
			Sample.INSTANCE.play( Assets.Sounds.BURNING );
		}
	}

}
