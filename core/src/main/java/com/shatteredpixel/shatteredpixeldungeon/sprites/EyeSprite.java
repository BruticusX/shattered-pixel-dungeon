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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Eye;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;

public class EyeSprite extends MobSprite {

	private int zapPos;

	private Animation charging;
	private Emitter chargeParticles;
	
	public EyeSprite() {
		super();
		
		//MOD: redrawn (tools/mobs). Rows of 16 frames: move 0-3, attack 4-7, death 8-11, idle 12-15
		TextureFilm frames = hdFilm( Assets.Sprites.EYE_HD, 74, 15f / 46f );
		int c = 0;

		idle = new Animation( 4, true );
		idle.frames( frames, row( c, 12, 12, 13, 12, 14, 15 ) );

		charging = new Animation( 8, true );
		charging.frames( frames, row( c, 4, 5 ) );

		run = new Animation( 8, true );
		run.frames( frames, row( c, 0, 1, 2, 3 ) );

		attack = new Animation( 8, false );
		attack.frames( frames, row( c, 5, 6, 7, 12 ) );

		zap = attack.clone();

		die = new Animation( 8, false );
		die.frames( frames, row( c, 8, 9, 10, 11 ) );

		play( idle );
	}

	@Override
	public void link(Char ch) {
		super.link(ch);
		
		chargeParticles = centerEmitter();
		chargeParticles.autoKill = false;
		chargeParticles.pour(MagicMissile.MagicParticle.ATTRACTING, 0.05f);
		chargeParticles.on = false;
		
		if (((Eye)ch).beamCharged) play(charging);
	}

	@Override
	public void update() {
		super.update();
		if (chargeParticles != null){
			chargeParticles.pos( center() );
			chargeParticles.visible = visible;
		}
	}

	@Override
	public void die() {
		super.die();
		if (chargeParticles != null){
			chargeParticles.on = false;
		}
	}

	@Override
	public void kill() {
		super.kill();
		if (chargeParticles != null){
			chargeParticles.killAndErase();
		}
	}

	public void charge( int pos ){
		turnTo(ch.pos, pos);
		play(charging);
		if (visible) Sample.INSTANCE.play( Assets.Sounds.CHARGEUP );
	}

	@Override
	public void play(Animation anim) {
		if (chargeParticles != null) chargeParticles.on = anim == charging;
		super.play(anim);
	}

	@Override
	public void zap( int pos ) {
		zapPos = pos;
		super.zap( pos );
	}
	
	@Override
	public void onComplete( Animation anim ) {
		super.onComplete( anim );
		
		if (anim == zap) {
			idle();
			if (Actor.findChar(zapPos) != null){
				parent.add(new Beam.DeathRay(center(), Actor.findChar(zapPos).sprite.center()));
			} else {
				parent.add(new Beam.DeathRay(center(), DungeonTilemap.raisedTileCenterToWorld(zapPos)));
			}
			Sample.INSTANCE.play( Assets.Sounds.RAY );
			((Eye)ch).deathGaze();
			ch.next();
		} else if (anim == die){
			chargeParticles.killAndErase();
		}
	}
}
