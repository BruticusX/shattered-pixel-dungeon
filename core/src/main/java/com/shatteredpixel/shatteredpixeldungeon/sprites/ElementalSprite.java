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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ElmoParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.RainbowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Callback;

public abstract class ElementalSprite extends MobSprite {
	
	protected int boltType;
	
	protected abstract int texOffset();
	
	private Emitter particles;
	protected abstract Emitter createEmitter();
	
	public ElementalSprite() {
		super();
		
		int c = texOffset();
		
		//MOD: redrawn (tools/mobs). Rows of 16 frames: move 0-3, attack 4-7, death 8-11, idle 12-15
		TextureFilm frames = hdFilm( Assets.Sprites.ELEMENTAL_HD, 57, 13f / 42f );

		idle = new Animation( 6, true );
		idle.frames( frames, row( c, 12, 12, 13, 12, 14, 15 ) );

		run = new Animation( 10, true );
		run.frames( frames, row( c, 0, 1, 2, 3 ) );

		attack = new Animation( 14, false );
		attack.frames( frames, row( c, 4, 5, 6, 7, 12 ) );

		zap = attack.clone();

		die = new Animation( 8, false );
		die.frames( frames, row( c, 8, 9, 10, 11 ) );

		play( idle );
	}
	
	@Override
	public void link( Char ch ) {
		super.link( ch );
		
		if (particles == null) {
			particles = createEmitter();
		}
	}
	
	@Override
	public void update() {
		super.update();
		
		if (particles != null){
			particles.visible = visible;
		}
	}
	
	@Override
	public void die() {
		super.die();
		if (particles != null){
			particles.on = false;
		}
	}
	
	@Override
	public void kill() {
		super.kill();
		if (particles != null){
			particles.killAndErase();
		}
	}
	
	public void zap( int cell ) {
		super.zap( cell );
		
		MagicMissile.boltFromChar( parent,
				boltType,
				this,
				cell,
				new Callback() {
					@Override
					public void call() {
						((Elemental)ch).onZapComplete();
					}
				} );
		Sample.INSTANCE.play( Assets.Sounds.ZAP );
	}
	
	@Override
	public void onComplete( Animation anim ) {
		if (anim == zap) {
			idle();
		}
		super.onComplete( anim );
	}
	
	public static class Fire extends ElementalSprite {
		
		{
			boltType = MagicMissile.FIRE;
		}
		
		@Override
		protected int texOffset() {
			return 0;
		}
		
		@Override
		protected Emitter createEmitter() {
			Emitter emitter = emitter();
			emitter.pour( FlameParticle.FACTORY, 0.06f );
			return emitter;
		}
		
		@Override
		public int blood() {
			return 0xFFFFBB33;
		}
	}
	
	public static class NewbornFire extends ElementalSprite {
		
		{
			boltType = MagicMissile.ELMO;
		}
		
		@Override
		protected int texOffset() {
			return 16;
		}
		
		@Override
		protected Emitter createEmitter() {
			Emitter emitter = emitter();
			emitter.pour( ElmoParticle.FACTORY, 0.06f );
			return emitter;
		}
		
		@Override
		public int blood() {
			return 0xFF85FFC8;
		}
	}
	
	public static class Frost extends ElementalSprite {
		
		{
			boltType = MagicMissile.FROST;
		}
		
		@Override
		protected int texOffset() {
			return 32;
		}
		
		@Override
		protected Emitter createEmitter() {
			Emitter emitter = emitter();
			emitter.pour( MagicMissile.MagicParticle.FACTORY, 0.06f );
			return emitter;
		}
		
		@Override
		public int blood() {
			return 0xFF8EE3FF;
		}
	}
	
	public static class Shock extends ElementalSprite {
		
		//different bolt, so overrides zap
		@Override
		public void zap( int cell ) {
			super.zap( cell, null );
			
			((Elemental)ch).onZapComplete();
			parent.add( new Beam.LightRay(center(), DungeonTilemap.raisedTileCenterToWorld(cell)));
			Sample.INSTANCE.play( Assets.Sounds.RAY );
		}
		
		@Override
		protected int texOffset() {
			return 48;
		}
		
		@Override
		protected Emitter createEmitter() {
			Emitter emitter = emitter();
			emitter.pour( SparkParticle.STATIC, 0.06f );
			return emitter;
		}
		
		@Override
		public int blood() {
			return 0xFFFFFF85;
		}
	}
	
	public static class Chaos extends ElementalSprite {

		@Override
		public void zap(int cell) {
			zap( cell, null ); //effectively super.super.zap
			//relies on cursed wand for effects
			((Elemental)ch).onZapComplete();
		}

		@Override
		protected int texOffset() {
			return 64;
		}
		
		@Override
		protected Emitter createEmitter() {
			Emitter emitter = emitter();
			emitter.pour( RainbowParticle.BURST, 0.025f );
			return emitter;
		}
		
		@Override
		public int blood() {
			return 0xFFE3E3E3;
		}
	}
}
