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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM200;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

public class DM200Sprite extends MobSprite {

	public DM200Sprite () {
		super();

		//MOD: redrawn (tools/mobs). Rows of 16 frames: move 0-3, attack 4-7, death 8-11, idle 12-15
		TextureFilm frames = hdFilm( Assets.Sprites.DM200_HD, 70, 17f / 54f );
		int c = texOffset();

		idle = new Animation( 3, true );
		idle.frames( frames, row( c, 12, 12, 13, 12, 14, 15 ) );

		run = new Animation( 8, true );
		run.frames( frames, row( c, 0, 1, 2, 3 ) );

		attack = new Animation( 14, false );
		attack.frames( frames, row( c, 4, 5, 6, 7, 12 ) );

		zap = new Animation( 12, false );
		zap.frames( frames, row( c, 4, 5, 5, 4, 12 ) );

		die = new Animation( 8, false );
		die.frames( frames, row( c, 8, 9, 10, 11 ) );

		play( idle );
	}

	protected int texOffset(){
		return 0;
	}

	public void zap( int cell ) {

		super.zap( cell );

		MagicMissile.boltFromChar( parent,
				MagicMissile.SPECK + Speck.TOXIC,
				this,
				cell,
				new Callback() {
					@Override
					public void call() {
						((DM200) ch).onZapComplete();
					}
				} );
		Sample.INSTANCE.play( Assets.Sounds.GAS );
		GLog.w(Messages.get(DM200.class, "vent"));
	}

	@Override
	public void place(int cell) {
		if (parent != null) parent.bringToFront(this);
		super.place(cell);
	}

	@Override
	public void die() {
		emitter().burst( Speck.factory( Speck.WOOL ), 8 );
		super.die();
	}

	@Override
	public void onComplete( Animation anim ) {
		if (anim == zap) {
			idle();
		}
		super.onComplete( anim );
	}

	@Override
	public int blood() {
		return 0xFFFFFF88;
	}

	public static class Vault extends DM200Sprite {

		@Override
		protected int texOffset() {
			return 16;
		}
	}

}
