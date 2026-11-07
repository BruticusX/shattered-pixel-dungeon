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
import com.shatteredpixel.shatteredpixeldungeon.items.Material;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.watabou.noosa.Image;

import java.util.Locale;

//MOD: the redrawn heroes (tools/heroes/build_hd_hero.py). Each frame is drawn small enough that the
//character stands 16 pixels tall in the game, so it keeps the detail of the SpriteCook art.
//Classes that are not redrawn yet keep the old 12x15 sprites.
public class HeroHD {

	public static final float HEIGHT = 20f; //art pixels the same size as floor items (ItemSprite.WORLD_SCALE)

	public static boolean has( HeroClass cls ){
		return layout(cls) != null;
	}

	private static String key( HeroClass cls ){
		return cls.name().toLowerCase(Locale.ENGLISH);
	}

	public static String sheet( HeroClass cls, Armor armor ){
		String base = "sprites/hero_hd_" + key(cls);
		if (armor == null || armor instanceof ClassArmor || armor.materialKind() == null) return base + ".png";
		Material m = armor.material();
		if (m == null) return base + ".png";
		return base + "_" + m.name().toLowerCase(Locale.ENGLISH) + ".png";
	}

	public static String hands( HeroClass cls ){
		return "sprites/hero_hd_" + key(cls) + "_hands.png";
	}

	// ---- per-class layouts (generated HeroHD<Class>.java files) ----

	//frame numbers within an armor row, shifted to that row
	public static Object[] keys( int[] frames, int offset ){
		Object[] k = new Object[frames.length];
		for (int i = 0; i < frames.length; i++) k[i] = frames[i] + offset;
		return k;
	}

	//the frame (within a row) a sprite is showing, for placing its held weapon
	public static int shown( int[] frames, int curFrame ){
		if (frames == null || frames.length == 0) return 0;
		return frames[Math.max(0, Math.min(curFrame, frames.length - 1))];
	}

	//both visible hands per frame ({x1[], y1[], x2[], y2[]}, -1 = hidden), for gloves
	private static final java.util.HashMap<Layout, float[][]> GLOVES = new java.util.HashMap<>();
	public static float[][] gloveHands( Layout L ){
		if (!GLOVES.containsKey(L)) {
			float[][] v = null;
			for (HeroClass c : HeroClass.values()) if (layout(c) == L) v = HeroHDGloves.of(key(c));
			GLOVES.put(L, v);
		}
		return GLOVES.get(L);
	}

	//the hand that doesn't hold the weapon ({x[], y[]}, -1 = hidden), for gloves worn on both hands
	private static final java.util.HashMap<Layout, float[][]> FAR = new java.util.HashMap<>();
	public static float[][] farHands( Layout L ){
		if (!FAR.containsKey(L)) {
			float[][] v = null;
			for (HeroClass c : HeroClass.values()) if (layout(c) == L) v = HeroHDFarHands.of(key(c));
			FAR.put(L, v);
		}
		return FAR.get(L);
	}

	public static Layout layout( HeroClass cls ){
		switch (cls) {
			case WARRIOR:  return HeroHDWarrior.LAYOUT;
			case MAGE:     return HeroHDMage.LAYOUT;
			case ROGUE:    return HeroHDRogue.LAYOUT;
			case HUNTRESS: return HeroHDHuntress.LAYOUT;
			case DUELIST:  return HeroHDDuelist.LAYOUT;
			case CLERIC:   return HeroHDCleric.LAYOUT;
		}
		return null;
	}

	public static class Layout {
		public final int frame, frames, bodyHeight;
		public final int[] anims;   //idle, run, attack, death: start and count each
		public final int[] avatar;
		public final float[] handX, handY, angle;
		public final boolean[] behind;

		public Layout( int frame, int frames, int bodyHeight, int[] anims, int[] avatar,
		               float[] handX, float[] handY, float[] angle, boolean[] behind ){
			this.frame = frame; this.frames = frames; this.bodyHeight = bodyHeight;
			this.anims = anims; this.avatar = avatar;
			this.handX = handX; this.handY = handY; this.angle = angle; this.behind = behind;
		}

		//optional: frames of standing while reading from a tome held open on the palm
		public int tomeStart = -1, tomeCount = 0;

		public Layout withTomeIdle( int start, int count ){
			tomeStart = start; tomeCount = count;
			return this;
		}

		public int[] tomeIdle(){
			if (tomeStart < 0) return null;
			int[] r = new int[tomeCount];
			for (int k = 0; k < r.length; k++) r[k] = tomeStart + k;
			return r;
		}

		public float scale(){
			return HEIGHT / bodyHeight;
		}

		public int[] range( String anim ){
			int i;
			switch (anim) {
				case "idle": i = 0; break;
				case "run": i = 1; break;
				case "attack": i = 2; break;
				default: i = 3;
			}
			int[] r = new int[anims[i * 2 + 1]];
			for (int k = 0; k < r.length; k++) r[k] = anims[i * 2] + k;
			return r;
		}

		//the status pane portrait: head to knees of the first idle frame, sized like the old 12x15 avatar
		public Image avatar( String sheet, int tier ){
			FitImage img = new FitImage( sheet );
			img.frame( img.texture.uvRect( avatar[0], tier * frame + avatar[1], avatar[0] + avatar[2], tier * frame + avatar[1] + avatar[3] ) );
			img.fit( 12, 15 );
			return img;
		}
	}

	//an Image whose on-screen size can be set independently of its texture region
	public static class FitImage extends Image {
		public FitImage( Object tx ){ super( tx ); }
		public void fit( float w, float h ){
			width = w;
			height = h;
			updateVertices();
		}
	}
}
