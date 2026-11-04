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
		if (weaponSprite != null) weaponSprite.update();
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

	//per frame: the weapon hand (x, y in the 12x15 frame, facing right) and the blade's angle
	// (0 = tip up and back, 90 = up and forward, 135 = straight forward). null = not shown
	// (death, and the two-handed operate and read poses). All six classes share these poses.
	private static final float[][] HOLD = new float[21][];
	static {
		float[] rest = {2.5f, 10.5f, 30};
		HOLD[0] = rest; HOLD[1] = rest; HOLD[2] = rest; HOLD[3] = rest;
		HOLD[4] = new float[]{3.5f, 11.5f, 30};   //the low step of the run
		HOLD[5] = rest;
		HOLD[6] = new float[]{1.5f, 10.5f, 35};
		HOLD[7] = new float[]{1.5f, 10.5f, 35};
		HOLD[13] = new float[]{1.5f, 10.5f, -15}; //wind up
		HOLD[14] = new float[]{10.5f, 9.5f, 135}; //strike
		HOLD[15] = new float[]{7.5f, 10.5f, 80};  //follow through
		HOLD[18] = rest;
	}

	private enum Hold { DIAGONAL, SHIELD, FIST, COILED }

	private ItemSprite weaponSprite;
	private com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon shownWeapon;
	private int shownImage = -1;
	private String shownSheet;
	private ItemSprite.Glowing shownGlow;

	private static Hold holdOf( com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon w ){
		if (com.shatteredpixel.shatteredpixeldungeon.actors.hero.Proficiency.familyOf(w)
				== com.shatteredpixel.shatteredpixeldungeon.actors.hero.Proficiency.Family.SHIELDS) return Hold.SHIELD;
		if (w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves
				|| w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gauntlet
				|| w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.TigerClaws) return Hold.FIST;
		if (w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip) return Hold.COILED;
		return Hold.DIAGONAL;
	}

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
		if (Dungeon.hero == null || ch == null || !ch.isAlive() || !visible) return;
		com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon w = Dungeon.hero.belongings.weapon();
		if (!(w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon)) return;

		//rebuild the sprite when the weapon, its look or its glow changes
		if (weaponSprite == null || w != shownWeapon || w.image() != shownImage
				|| !String.valueOf(w.spriteSheet()).equals(shownSheet) || w.glowing() != shownGlow) {
			if (weaponSprite == null) weaponSprite = new ItemSprite();
			weaponSprite.view(w);
			shownWeapon = w;
			shownImage = w.image();
			shownSheet = String.valueOf(w.spriteSheet());
			shownGlow = w.glowing();
		}

		float[] h = HOLD[shownFrame()];
		if (h == null) return;

		boolean faceLeft = flipHorizontal;
		Hold hold = holdOf(w);
		ItemSprite ws = weaponSprite;
		float sc;
		switch (hold) {
			case SHIELD: sc = 0.5f; break;
			case FIST:   sc = 0.35f; break;
			case COILED: sc = 0.45f; break;
			default:     sc = 0.6f;
		}
		ws.scale.set(sc);
		if (hold == Hold.DIAGONAL) {
			//item sprites point up and to the right; mirror them so the tip points away from the body
			ws.setFlipped(!faceLeft);
			ws.origin.set(faceLeft ? ws.width * 0.18f : ws.width * 0.82f, ws.height * 0.82f);
			ws.angle = faceLeft ? -h[2] : h[2];
		} else {
			ws.setFlipped(faceLeft);
			ws.origin.set(ws.width / 2f, ws.height / 2f);
			ws.angle = 0;
		}
		float ax = faceLeft ? FRAME_WIDTH - h[0] : h[0];
		ws.x = x + ax - ws.origin.x;
		ws.y = y + h[1] - ws.origin.y;
		ws.alpha(alpha());
		ws.camera = camera();
		ws.draw();
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
