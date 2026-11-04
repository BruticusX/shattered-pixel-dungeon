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
		return cls == HeroClass.WARRIOR;
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

	// ---- layout (only the warrior so far) ----
	public static final int FRAME = HeroHDWarrior.FRAME;
	public static final int FRAMES = HeroHDWarrior.FRAMES;

	public static float scale( HeroClass cls ){
		return HEIGHT / HeroHDWarrior.BODY_HEIGHT;
	}

	public static int[] range( String anim ){
		switch (anim) {
			case "idle":   return seq(HeroHDWarrior.IDLE_START, HeroHDWarrior.IDLE_COUNT);
			case "run":    return seq(HeroHDWarrior.RUN_START, HeroHDWarrior.RUN_COUNT);
			case "attack": return seq(HeroHDWarrior.ATTACK_START, HeroHDWarrior.ATTACK_COUNT);
			case "death":  return seq(HeroHDWarrior.DEATH_START, HeroHDWarrior.DEATH_COUNT);
		}
		return new int[]{0};
	}

	private static int[] seq( int start, int count ){
		int[] r = new int[count];
		for (int i = 0; i < count; i++) r[i] = start + i;
		return r;
	}

	public static float handX( int f ){ return HeroHDWarrior.HAND_X[f]; }
	public static float handY( int f ){ return HeroHDWarrior.HAND_Y[f]; }
	public static float angle( int f ){ return HeroHDWarrior.ANGLE[f]; }
	public static boolean behind( int f ){ return HeroHDWarrior.BEHIND[f]; }

	//the status pane portrait: head to knees of the first idle frame, sized like the old 12x15 avatar
	public static Image avatar( String sheet, int tier ){
		int[] a = HeroHDWarrior.AVATAR;
		FitImage img = new FitImage( sheet );
		img.frame( img.texture.uvRect( a[0], tier * FRAME + a[1], a[0] + a[2], tier * FRAME + a[1] + a[3] ) );
		img.fit( 12, 15 );
		return img;
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
