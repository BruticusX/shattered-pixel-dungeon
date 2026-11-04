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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HeroDisguise;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;

public class HeroSprite extends CharSprite {
	
	private static final int FRAME_WIDTH	= 12;
	private static final int FRAME_HEIGHT	= 15;
	
	private static final int RUN_FRAMERATE	= 20;
	
	private static TextureFilm tiers;
	
	private Animation fly;
	private Animation read;

	//MOD: the class whose sheet is drawn (the hero's own, or a disguise)
	private HeroClass sheetClass;

	public HeroSprite() {
		super();
		
		sheetClass = Dungeon.hero.heroClass;
		updateArmor();
		
		link( Dungeon.hero );

		if (ch.isAlive())
			idle();
		else
			die();
	}

	public void disguise(HeroClass cls){
		sheetClass = cls;
		updateArmor();
	}

	//MOD: the hero's sheet with the worn armor tinted by its material (tools/recolor_hero_armor.py)
	public static String sheetFor( HeroClass cls, com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor armor ){
		if (armor == null || armor instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor
				|| armor.materialKind() == null) {
			return cls.spritesheet();
		}
		com.shatteredpixel.shatteredpixeldungeon.items.Material m = armor.material();
		if (m == null) return cls.spritesheet();
		return "sprites/hero_" + cls.name().toLowerCase(java.util.Locale.ENGLISH) + "_" + m.name().toLowerCase(java.util.Locale.ENGLISH) + ".png";
	}
	
	public void updateArmor() {

		texture( sheetFor( sheetClass != null ? sheetClass : Dungeon.hero.heroClass, Dungeon.hero.belongings.armor() ) );

		TextureFilm film = new TextureFilm( tiers(), Dungeon.hero.tier(), FRAME_WIDTH, FRAME_HEIGHT );
		
		idle = new Animation( 1, true );
		idle.frames( film, 0, 0, 0, 1, 0, 0, 1, 1 );
		
		run = new Animation( RUN_FRAMERATE, true );
		run.frames( film, 2, 3, 4, 5, 6, 7 );
		
		die = new Animation( 20, false );
		die.frames( film, 8, 9, 10, 11, 12, 11 );
		
		attack = new Animation( 15, false );
		attack.frames( film, 13, 14, 15, 0 );
		
		zap = attack.clone();
		
		operate = new Animation( 8, false );
		operate.frames( film, 16, 17, 16, 17 );
		
		fly = new Animation( 1, true );
		fly.frames( film, 18 );

		read = new Animation( 20, false );
		read.frames( film, 19, 20, 20, 20, 20, 20, 20, 20, 20, 19 );
		
		if (Dungeon.hero.isAlive())
			idle();
		else
			die();
	}
	
	@Override
	public void place( int p ) {
		super.place( p );
		if (Game.scene() instanceof GameScene) Camera.main.panFollow(this, 5f);
	}

	@Override
	public void move( int from, int to ) {
		super.move( from, to );
		if (ch != null && ch.flying) {
			play( fly );
		}
		Camera.main.panFollow(this, 20f);
	}

	@Override
	public void idle() {
		super.idle();
		if (ch != null && ch.flying) {
			play( fly );
		}
	}

	@Override
	public void jump( int from, int to, float height, float duration,  Callback callback ) {
		super.jump( from, to, height, duration, callback );
		play( fly );
		Camera.main.panFollow(this, 20f);
	}

	public synchronized void read() {
		animCallback = new Callback() {
			@Override
			public void call() {
				idle();
				ch.onOperateComplete();
			}
		};
		play( read );
	}

	@Override
	public void bloodBurstA(PointF from, int damage) {
		//Does nothing.

		/*
		 * This is both for visual clarity, and also for content ratings regarding violence
		 * towards human characters. The heroes are the only human or human-like characters which
		 * participate in combat, so removing all blood associated with them is a simple way to
		 * reduce the violence rating of the game.
		 */
	}

	@Override
	public void update() {
		sleeping = ch.isAlive() && ((Hero)ch).resting;
		
		super.update();
		held.update();
	}

	// ---- MOD: the equipped weapon drawn in the hero's hand ----

	//the frame numbers each animation plays (matching updateArmor)
	private static final int[] IDLE_F    = {0, 0, 0, 1, 0, 0, 1, 1};
	private static final int[] RUN_F     = {2, 3, 4, 5, 6, 7};
	private static final int[] DIE_F     = {8, 9, 10, 11, 12, 11};
	private static final int[] ATTACK_F  = {13, 14, 15, 0};
	private static final int[] OPERATE_F = {16, 17, 16, 17};
	private static final int[] FLY_F     = {18};
	private static final int[] READ_F    = {19, 20, 20, 20, 20, 20, 20, 20, 20, 19};

	private final HeldWeapon held = new HeldWeapon();

	private int shownFrame(){
		int[] f = null;
		if (curAnim == idle) f = IDLE_F;
		else if (curAnim == run) f = RUN_F;
		else if (curAnim == die) f = DIE_F;
		else if (curAnim == attack || curAnim == zap) f = ATTACK_F;
		else if (curAnim == operate) f = OPERATE_F;
		else if (curAnim == fly) f = FLY_F;
		else if (curAnim == read) f = READ_F;
		if (f == null) return 0;
		return f[Math.max(0, Math.min(curFrame, f.length - 1))];
	}

	@Override
	public void draw() {
		super.draw();
		drawWeapon();
	}

	private void drawWeapon(){
		if (Dungeon.hero == null || ch == null || !ch.isAlive()) return;
		held.draw(this, Dungeon.hero.belongings.weapon(), shownFrame());
	}

	
	public void sprint( float speed ) {
		run.delay = 1f / speed / RUN_FRAMERATE;
	}
	
	public static TextureFilm tiers() {
		if (tiers == null) {
			SmartTexture texture = TextureCache.get( Assets.Sprites.ROGUE );
			tiers = new TextureFilm( texture, texture.width, FRAME_HEIGHT );
		}
		
		return tiers;
	}

	public static Image avatar( Hero hero ){
		HeroClass cls = hero.buff(HeroDisguise.class) != null ? hero.buff(HeroDisguise.class).getDisguise() : hero.heroClass;
		return avatar(sheetFor(cls, hero.belongings.armor()), hero.tier());
	}
	
	public static Image avatar( HeroClass cl, int armorTier ) {
		return avatar( cl.spritesheet(), armorTier );
	}

	public static Image avatar( String sheet, int armorTier ) {
		
		RectF patch = tiers().get( armorTier );
		Image avatar = new Image( sheet );
		RectF frame = avatar.texture.uvRect( 1, 0, FRAME_WIDTH, FRAME_HEIGHT );
		frame.shift( patch.left, patch.top );
		avatar.frame( frame );
		
		return avatar;
	}
}
