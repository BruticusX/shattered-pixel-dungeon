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
import com.watabou.noosa.particles.PixelParticle;

public class ShopkeeperSprite extends MobSprite {
	
	private PixelParticle coin;
	
	public ShopkeeperSprite() {
		super();
		
		//MOD: redrawn (tools/mobs). Rows of 16 frames: move 0-3, attack 4-7, death 8-11, idle 12-15
		TextureFilm film = hdFilm( Assets.Sprites.KEEPER_HD, 53, 14f / 44f );
		int c = 0;

		idle = new Animation( 3, true );
		idle.frames( film, row( c, 0, 0, 0, 0, 4, 5, 6, 5 ) );

		die = new Animation( 10, false );
		die.frames( film, row( c, 8, 9, 10, 11 ) );

		run = new Animation( 10, true );
		run.frames( film, row( c, 0, 1, 2, 3 ) );
		attack = idle.clone();

		idle();
	}
	
	@Override
	public void onComplete( Animation anim ) {
		super.onComplete( anim );
		
		if (visible && anim == idle) {
			if (coin == null) {
				coin = new PixelParticle();
				parent.add( coin );
			}
			coin.reset( x + (flipHorizontal ? width * 0.1f : width * 0.9f), y + height * 0.5f, 0xFFFF00, 1, 0.5f );
			coin.speed.y = -40;
			coin.acc.y = +160;
		}
	}
}
