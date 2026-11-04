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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Proficiency;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gauntlet;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.TigerClaws;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip;

//MOD: a melee weapon drawn in the hand of a hero-shaped sprite (the hero, mirror images, statues).
//All of them share the hero's 12x15 pose template, so one table of hand positions covers them.
public class HeldWeapon {

	//per hero-sheet frame: the weapon hand (x, y in the 12x15 frame, facing right) and the blade's
	//angle (0 = tip up and back, 90 = up and forward, 135 = straight forward). null = not shown
	// (death, and the two-handed operate and read poses)
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

	private ItemSprite ws;
	private Item shown;
	private int shownImage = -1;
	private String shownSheet;
	private ItemSprite.Glowing shownGlow;

	private static Hold holdOf( Item w ){
		if (Proficiency.familyOf(w) == Proficiency.Family.SHIELDS) return Hold.SHIELD;
		if (w instanceof Gloves || w instanceof Gauntlet || w instanceof TigerClaws) return Hold.FIST;
		if (w instanceof Whip) return Hold.COILED;
		return Hold.DIAGONAL;
	}

	public void update(){
		if (ws != null) ws.update();
	}

	//draws `w` in `owner`'s hand for the given hero-sheet frame number (-1 = hidden)
	public void draw( CharSprite owner, Item w, int heroFrame ){
		if (!(w instanceof MeleeWeapon) || !owner.visible || heroFrame < 0 || heroFrame >= HOLD.length) return;
		float[] h = HOLD[heroFrame];
		if (h == null) return;

		//rebuild the sprite when the weapon, its look or its glow changes
		if (ws == null || w != shown || w.image() != shownImage
				|| !String.valueOf(w.spriteSheet()).equals(shownSheet) || w.glowing() != shownGlow) {
			if (ws == null) ws = new ItemSprite();
			ws.view(w);
			shown = w;
			shownImage = w.image();
			shownSheet = String.valueOf(w.spriteSheet());
			shownGlow = w.glowing();
		}

		boolean faceLeft = owner.flipHorizontal;
		Hold hold = holdOf(w);
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
		float ax = faceLeft ? 12 - h[0] : h[0];
		ws.x = owner.x + ax - ws.origin.x;
		ws.y = owner.y + h[1] - ws.origin.y;
		ws.alpha(owner.alpha());
		ws.camera = owner.camera();
		ws.draw();
	}
}
