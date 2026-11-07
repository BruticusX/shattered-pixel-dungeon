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

import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

//MOD: the battlemage's disintegration ray: a thick, unstable beam of violet energy that writhes in
//thickness and brightness while it burns, stretched from the caster to where it stops
public class DisintBeam extends Image {

	private static final float DURATION = 0.7f;
	private float left = DURATION;
	private final float baseY;

	public DisintBeam( PointF s, PointF e ){
		super( "effects/disint_beam.png" );
		origin.set( 0, height / 2f );
		x = s.x - origin.x;
		y = s.y - origin.y;
		float dx = e.x - s.x, dy = e.y - s.y;
		angle = (float) Math.toDegrees( Math.atan2( dy, dx ) );
		scale.x = (float) Math.sqrt( dx * dx + dy * dy ) / width;
		//a bit over half a tile thick
		baseY = 10f / height;
		scale.y = baseY;
	}

	@Override
	public void update() {
		super.update();
		float p = left / DURATION;              //1 -> 0
		//volatile: the beam surges and flickers, then collapses
		scale.y = baseY * (0.35f + 0.65f * p) * Random.Float( 0.75f, 1.3f );
		alpha( Math.min( 1f, p * 1.6f ) * Random.Float( 0.8f, 1f ) );
		if ((left -= Game.elapsed) <= 0) killAndErase();
	}

	@Override
	public void draw() {
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}
}
