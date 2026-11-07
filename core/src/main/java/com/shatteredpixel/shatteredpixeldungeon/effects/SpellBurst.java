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

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;

//MOD: a quick flash where a tome spell lands: grows a little and fades out
public class SpellBurst extends Image {

	private static final float DURATION = 0.3f;
	private float left = DURATION;
	private final float size;

	public SpellBurst( String texture, PointF at, float size ){
		super( texture );
		this.size = size / width;
		origin.set( width / 2f, height / 2f );
		x = at.x - origin.x;
		y = at.y - origin.y;
		angle = com.watabou.utils.Random.Float(360);
		scale.set( this.size * 0.6f );
	}

	@Override
	public void update() {
		super.update();
		float p = left / DURATION;          //1 -> 0
		scale.set( size * (1.2f - 0.6f * p) );
		alpha( p );
		if ((left -= Game.elapsed) <= 0) killAndErase();
	}

	@Override
	public void draw() {
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}
}
