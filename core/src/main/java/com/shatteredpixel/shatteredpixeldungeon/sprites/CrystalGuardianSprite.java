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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalGuardian;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;

public abstract class CrystalGuardianSprite extends MobSprite {

	private Animation crumple;

	public CrystalGuardianSprite() {
		super();

		//MOD: redrawn (tools/mobs). Rows of 16 frames: move 0-3, attack 4-7, death 8-11, idle 12-15
		TextureFilm frames = hdFilm( Assets.Sprites.CRYSTAL_GUARDIAN_HD, 67, 19f / 54f );
		int c = texOffset();

		idle = new MovieClip.Animation( 3, true );
		idle.frames( frames, row( c, 12, 12, 13, 12, 14, 15 ) );

		run = new MovieClip.Animation( 8, true );
		run.frames( frames, row( c, 0, 1, 2, 3 ) );

		attack = new MovieClip.Animation( 10, false );
		attack.frames( frames, row( c, 4, 5, 6, 7, 12 ) );

		die = new MovieClip.Animation( 8, false );
		die.frames( frames, row( c, 8, 9, 10, 11 ) );

		crumple = die.clone();

		play( idle );
	}

	public void crumple(){
		play(crumple);
	}

	public void endCrumple(){
		if (curAnim == crumple){
			idle();
		}
	}

	@Override
	public void link(Char ch) {
		super.link(ch);
		if (ch instanceof CrystalGuardian && ((CrystalGuardian) ch).recovering()){
			crumple();
		}
	}

	protected abstract int texOffset();

	public static class Blue extends CrystalGuardianSprite {
		@Override
		protected int texOffset() {
			return 16;
		}
		@Override
		public int blood() {
			return 0xFF8EE3FF;
		}
	}

	public static class Green extends CrystalGuardianSprite {
		@Override
		protected int texOffset() {
			return 32;
		}
		@Override
		public int blood() {
			return 0xFF85FFC8;
		}
	}

	public static class Red extends CrystalGuardianSprite {
		@Override
		protected int texOffset() {
			return 48;
		}
		@Override
		public int blood() {
			return 0xFFFFBB33;
		}
	}

}
