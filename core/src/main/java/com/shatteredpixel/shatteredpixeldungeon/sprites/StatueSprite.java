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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.GameMath;

//MOD: statues are a stone version of the redrawn (HD) warrior, in the armor they wear, holding
//the weapon they fight with (sprites/hero_hd_warrior_stone.png, made by tools/heroes/stone_sheet.py)
public class StatueSprite extends MobSprite {

	public static final String STONE = "sprites/hero_hd_warrior_stone.png";

	private final HeroHD.Layout L = HeroHD.layout( HeroClass.WARRIOR );
	private int[] idleF, runF, attackF, deathF;

	public StatueSprite() {
		super();
		setArmor( 0 );
	}

	public void setArmor( int tier ){
		hdScale = L.scale();
		texture( STONE );
		TextureFilm film = new TextureFilm( texture, L.frame, L.frame );
		//armor tiers 1-5 are cloth, leather, mail, scale and plate: rows 1-5 of the hero sheet
		int row = (int) GameMath.gate( 0, tier, 5 ) * L.frames;
		idleF = L.range("idle");
		runF = L.range("run");
		attackF = L.range("attack");
		deathF = L.range("death");

		idle = new Animation( 4, true );
		idle.frames( film, HeroHD.keys(idleF, row) );
		run = new Animation( 15, true );
		run.frames( film, HeroHD.keys(runF, row) );
		attack = new Animation( 18, false );
		attack.frames( film, HeroHD.keys(attackF, row) );
		die = new Animation( 12, false );
		die.frames( film, HeroHD.keys(deathF, row) );

		play( idle, true );
	}

	private final HeldWeapon held = new HeldWeapon();

	@Override
	public void update() {
		super.update();
		held.update();
	}

	@Override
	public void draw() {
		if (!(ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue) || !ch.isAlive()) { super.draw(); return; }
		int f;
		if (curAnim == idle) f = HeroHD.shown(idleF, curFrame);
		else if (curAnim == run) f = HeroHD.shown(runF, curFrame);
		else if (curAnim == attack) f = HeroHD.shown(attackF, curFrame);
		else f = -1;
		//the weapon is in the far hand: drawn first so the body covers its grip
		if (f >= 0) held.drawHD(this, ((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue) ch).weapon(), f, hdScale, null, L);
		super.draw();
	}

	@Override
	public int blood() {
		return 0xFFcdcdb7;
	}
}
