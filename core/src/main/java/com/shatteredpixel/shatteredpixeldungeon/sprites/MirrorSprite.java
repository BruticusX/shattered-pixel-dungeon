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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.MirrorImage;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.PointF;

//MOD: mirror images use the hero's redrawn (HD) sprite, armor look and held weapon
public class MirrorSprite extends MobSprite {

	private HeroHD.Layout L;
	private int[] idleF, runF, attackF;

	public MirrorSprite() {
		super();
		updateArmor( 0 );
		idle();
	}

	@Override
	public void link( Char ch ) {
		super.link( ch );
		updateArmor();
	}

	private final HeldWeapon held = new HeldWeapon();

	@Override
	public void update() {
		super.update();
		held.update();
	}

	@Override
	public void draw() {
		if (Dungeon.hero == null || ch == null || !ch.isAlive() || L == null) { super.draw(); return; }
		int f;
		if (curAnim == idle) f = HeroHD.shown(idleF, curFrame);
		else if (curAnim == run) f = HeroHD.shown(runF, curFrame);
		else if (curAnim == attack) f = HeroHD.shown(attackF, curFrame);
		else f = -1;
		//the weapon is in the far hand: drawn first so the body covers its grip
		if (f >= 0) held.drawHD(this, Dungeon.hero.belongings.weapon(), f, hdScale, null, L);
		super.draw();
	}

	@Override
	public void bloodBurstA(PointF from, int damage) {
		//do nothing
	}

	public void updateArmor(){
		updateArmor( ((MirrorImage)ch).armTier );
	}

	public void updateArmor( int tier ) {
		HeroClass cls = Dungeon.hero != null ? Dungeon.hero.heroClass : HeroClass.WARRIOR;
		L = HeroHD.layout( cls );
		hdScale = L.scale();
		texture( HeroHD.sheet( cls, Dungeon.hero != null ? Dungeon.hero.belongings.armor() : null ) );
		TextureFilm film = new TextureFilm( texture, L.frame, L.frame );
		int row = tier * L.frames;
		idleF = L.range("idle");
		runF = L.range("run");
		attackF = L.range("attack");

		idle = new Animation( 6, true );
		idle.frames( film, HeroHD.keys(idleF, row) );
		run = new Animation( 20, true );
		run.frames( film, HeroHD.keys(runF, row) );
		die = new Animation( 20, false );
		die.frames( film, idleF[0] + row );
		attack = new Animation( 22, false );
		attack.frames( film, HeroHD.keys(attackF, row) );

		idle();
	}
}
