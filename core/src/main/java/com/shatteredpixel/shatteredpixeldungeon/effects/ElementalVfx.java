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
 */package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

//MOD: a small painted, animated piece of an element (a lick of flame, an ice crystal, a crackle of
//lightning, a drip of venom or blood, a curl of shadow, a glint of light, an arcane swirl) that plays
//its 4 frames once and fades. Used for the enchantment auras on weapons and armor.
public class ElementalVfx extends Image {

	public static final int FIRE = 0, FROST = 1, LIGHTNING = 2, POISON = 3,
			SMOKE = 4, GLINT = 5, BLOOD = 6, ARCANE = 7;

	private static final String TX = "effects/enchant_vfx.png";
	private static TextureFilm film;

	//how long each kind lives, how it drifts (units per second, in item pixels) and whether it glows
	private static final float[] LIFE  = {0.55f, 0.7f, 0.28f, 0.8f, 0.9f, 0.6f, 0.8f, 0.7f};
	private static final float[] RISE  = {3.5f,  0f,   0f,    -3f,  2.5f, 0f,   -3f,  0.5f};
	private static final boolean[] GLOW = {true, true, true,  false, false, true, false, true};

	private int kind;
	private float life, left, unit, vy;

	public ElementalVfx(){
		super( TX );
		if (film == null) film = new TextureFilm( texture, 48, 48 );
	}

	//unit = world size of one item pixel at the spot it appears; size = width in item pixels
	public void reset( int kind, float x, float y, float unit, float size ){
		revive();
		this.kind = kind;
		this.unit = unit;
		life = left = LIFE[kind] * Random.Float(0.8f, 1.2f);
		vy = 0;
		frame( film.get( kind * 4 ) );
		float s = size * unit / 48f * 3f;   //the effect fills about a third of its cell
		scale.set( s );
		//centred on the blade so the effect hugs it; drips hang from it
		origin.set( 24, kind == POISON || kind == BLOOD ? 18 : 24 );
		this.x = x - origin.x;
		this.y = y - origin.y;
		//drips and glints keep their own orientation; flames and smoke lean a little
		angle = (kind == FIRE || kind == SMOKE) ? Random.Float(-12, 12) : 0;
		if (kind == FROST || kind == LIGHTNING || kind == ARCANE) angle = Random.Float(360);
		alpha( 1f );
	}

	public boolean glows(){
		return GLOW[kind];
	}

	@Override
	public void update() {
		super.update();
		float p = 1f - left / life;              //0 -> 1
		frame( film.get( kind * 4 + Math.min(3, (int)(p * 4)) ) );
		//drips speed up as they fall; flames and smoke rise
		if (kind == POISON || kind == BLOOD) vy += 18f * unit * Game.elapsed;
		y -= (RISE[kind] * unit - vy) * Game.elapsed;
		if (p > 0.75f) alpha( (1f - p) * 4f );
		if ((left -= Game.elapsed) <= 0) kill();
	}

	public static Emitter.Factory factory( final int kind, final float size ){
		return new Emitter.Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				float unit = emitter instanceof ItemAura ? ((ItemAura) emitter).unit() : 1f;
				((ElementalVfx) emitter.recycle( ElementalVfx.class )).reset( kind, x, y, unit, size );
			}
			@Override
			public boolean lightMode() {
				return GLOW[kind];
			}
		};
	}
}
