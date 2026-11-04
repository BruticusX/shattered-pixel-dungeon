/*
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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Random;

import java.util.ArrayList;

//MOD: what a weapon or armor is made of. The material sets its power (damage / armor),
//how many hits it takes before breaking, magic resistance (cloth & leather) and its look.
public enum Material {

	//metals, weakest to strongest by Mohs hardness; power = 0.7 x hardness (obsidian is special)
	TIN        (Kind.METAL,   1.05f, 150, 0f,    1,  4),
	COPPER     (Kind.METAL,   2.10f, 250, 0f,    2,  7),
	BRONZE     (Kind.METAL,   2.45f, 300, 0f,    4,  9),
	IRON       (Kind.METAL,   2.80f, 400, 0f,    7, 12),
	STEEL      (Kind.METAL,   3.50f, 500, 0f,   10, 15),
	OBSIDIAN   (Kind.METAL,   5.00f, 600, 0f,   13, 18), //shown as "umbralite": the rare top metal
	TITANIUM   (Kind.METAL,   4.20f, 600, 0f,   16, 21),
	TUNGSTEN   (Kind.METAL,   5.25f, 800, 0f,   19, 26),

	//cloth: light, little armor, strong magic resistance
	LINEN      (Kind.CLOTH,   0.80f, 200, 0.10f, 1,  7),
	WOOL       (Kind.CLOTH,   1.10f, 300, 0.20f, 6, 13),
	SILK       (Kind.CLOTH,   1.40f, 400, 0.30f, 12, 19),
	SPIDER_SILK(Kind.CLOTH,   1.70f, 500, 0.40f, 18, 26),

	//leather: between cloth and metal, some magic resistance
	HIDE          (Kind.LEATHER, 1.40f, 250, 0.05f, 1,  7),
	BOILED_LEATHER(Kind.LEATHER, 1.90f, 350, 0.10f, 6, 13),
	WYVERN_HIDE   (Kind.LEATHER, 2.40f, 450, 0.15f, 12, 19),
	DRAGONHIDE    (Kind.LEATHER, 2.90f, 600, 0.20f, 18, 26);

	public enum Kind { METAL, CLOTH, LEATHER }

	public final Kind kind;
	public final float power;       //effective tier: replaces the item type's own tier in its formulas
	public final int hits;          //uses from 100% to 0% durability
	public final float magicResist; //fraction of magic damage / debuff duration resisted
	public final int minDepth, maxDepth;

	Material(Kind kind, float power, int hits, float magicResist, int minDepth, int maxDepth) {
		this.kind = kind;
		this.power = power;
		this.hits = hits;
		this.magicResist = magicResist;
		this.minDepth = minDepth;
		this.maxDepth = maxDepth;
	}

	//how much a weapon's base damage is multiplied by when made of this metal (see WeaponStats)
	public float damageMult() {
		switch (this) {
			case TIN: default: return 1.0f;
			case COPPER:   return 1.4f;
			case BRONZE:   return 2.0f;
			case IRON:     return 2.5f;
			case STEEL:    return 2.8f;
			case TITANIUM: return 3.0f;
			case TUNGSTEN: return 3.3f;
			case OBSIDIAN: return 4.0f; //umbralite
		}
	}

	//how much an armor's base block is multiplied by when made of this material
	public float armorMult() {
		switch (this) {
			case LINEN: case HIDE: return 1.0f;
			case WOOL:  case BOILED_LEATHER: return 1.6f;
			case SILK:  case WYVERN_HIDE:    return 2.2f;
			case SPIDER_SILK: return 2.6f;
			case DRAGONHIDE:  return 2.8f;
			default: return damageMult(); //metals match their weapon multiplier
		}
	}

	//durability lost per use, in percentage points
	public float wearPerUse() {
		return 100f / hits;
	}

	public String title() {
		return Messages.get(Material.class, name().toLowerCase());
	}

	//the item sprite sheet recoloured for this material (see tools/recolor_metals.py)
	public String spriteSheet() {
		return "sprites/items_" + name().toLowerCase() + ".png";
	}

	public static Material lowest(Kind kind) {
		for (Material m : values()) if (m.kind == kind) return m;
		return null;
	}

	//a random material of this kind suited to the given depth; the middle of a material's
	//range is the most likely, so finds shift smoothly as you go deeper
	public static Material randomFor(Kind kind, int depth) {
		return randomFor(kind, depth, true);
	}

	//obsidian is the rare metal: it only comes from where scrolls of upgrade used to be
	public static Material randomFor(Kind kind, int depth, boolean allowObsidian) {
		ArrayList<Material> options = new ArrayList<>();
		ArrayList<Float> weights = new ArrayList<>();
		for (Material m : values()) {
			if (m.kind != kind || depth < m.minDepth || depth > m.maxDepth) continue;
			if (m == OBSIDIAN && !allowObsidian) continue;
			float mid = (m.minDepth + m.maxDepth) / 2f;
			float half = Math.max(1f, (m.maxDepth - m.minDepth) / 2f);
			options.add(m);
			weights.add(Math.max(0.15f, 1f - Math.abs(depth - mid) / (half + 1f)));
		}
		if (options.isEmpty()) {
			//past the deepest range: strongest of the kind
			Material best = lowest(kind);
			for (Material m : values()) if (m.kind == kind && m.maxDepth >= best.maxDepth && (allowObsidian || m != OBSIDIAN)) best = m;
			return best;
		}
		float[] w = new float[weights.size()];
		for (int i = 0; i < w.length; i++) w[i] = weights.get(i);
		return options.get(Random.chances(w));
	}

	public static Material randomFor(Kind kind) {
		return randomFor(kind, Math.max(1, Dungeon.depth));
	}

	public static Material fromName(String name, Material fallback) {
		if (name == null) return fallback;
		try {
			return valueOf(name);
		} catch (IllegalArgumentException e) {
			return fallback;
		}
	}
}
