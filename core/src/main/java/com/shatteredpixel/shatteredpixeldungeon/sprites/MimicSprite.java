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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.watabou.noosa.TextureFilm;

public class MimicSprite extends MobSprite {

	protected Animation advancedHiding;

	protected Animation hiding;

	{
		//adjust shadow slightly to account for 1 empty bottom pixel (used for border while hiding)
		perspectiveRaise    = 5 / 16f; //5 pixels
		shadowWidth         = 1f;
		shadowOffset        = -0.4f;
	}

	protected int texOffset(){
		return 0;
	}

	public MimicSprite() {
		super();

		int c = texOffset();

		//MOD: redrawn (tools/mobs). Rows of 16 frames: move 0-3, attack 4-7, death 8-11, idle 12-15
		TextureFilm frames = hdFilm( Assets.Sprites.MIMIC_HD, 66, 13f / 40f );

		advancedHiding = new Animation( 1, true );
		advancedHiding.frames( frames, row( c, 12 ) );

		hiding = new Animation( 1, true );
		hiding.frames( frames, row( c, 12, 12, 12, 12, 12, 13 ) );

		idle = new Animation( 4, true );
		idle.frames( frames, row( c, 3, 3, 3, 0 ) );

		run = new Animation( 10, true );
		run.frames( frames, row( c, 0, 1, 2, 3 ) );

		attack = new Animation( 12, false );
		attack.frames( frames, row( c, 4, 5, 6, 7 ) );

		die = new Animation( 6, false );
		die.frames( frames, row( c, 8, 9, 10, 11 ) );

		play( idle );
	}
	
	@Override
	public void linkVisuals(Char ch) {
		super.linkVisuals(ch);
		if (ch.alignment == Char.Alignment.NEUTRAL) {
			hideMimic(ch);
		}
	}

	public void hideMimic(Char ch){
		if (ch instanceof Mimic && ((Mimic) ch).stealthy()){
			play(advancedHiding);
		} else {
			play(hiding);
		}
		hideSleep();
	}

	@Override
	public void showSleep() {
		if (curAnim == hiding || curAnim == advancedHiding){
			return;
		}
		super.showSleep();
	}

	public static class Golden extends MimicSprite{
		@Override
		protected int texOffset() {
			return 16;
		}
	}

	public static class Crystal extends MimicSprite{
		@Override
		protected int texOffset() {
			return 32;
		}
	}

	public static class Ebony extends MimicSprite{
		@Override
		protected int texOffset() {
			return 48;
		}

		@Override
		public void hideMimic(Char ch) {
			super.hideMimic(ch);
			alpha(0.2f);
		}

		@Override
		public void resetColor() {
			super.resetColor();
			if (advancedHiding != null && curAnim == advancedHiding){
				alpha(0.2f);
			}
		}

		@Override
		public void play(Animation anim) {
			if (curAnim == advancedHiding && anim != advancedHiding){
				alpha(1f);
			}
			super.play(anim);
		}
	}

}
