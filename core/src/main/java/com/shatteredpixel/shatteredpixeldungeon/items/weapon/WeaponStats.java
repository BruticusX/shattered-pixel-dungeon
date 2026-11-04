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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon;

import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.*;

import java.util.HashMap;

//MOD: the damage system. Each weapon type has a real-world weight which sets how fast it
//swings and how hard it hits; the metal then multiplies the damage (Material.damageMult).
//
//  speed  : turns per attack = (kg / 1.2)^0.45, a 1.2kg sword is 1 turn; capped at 0.5 (2x) .. 2
//  damage : average hit at tin = 6 x (kg/1.2)^0.6 for weapons up to a sword's weight,
//           ^0.9 for heavier ones (heavy weapons hit disproportionately hard), x the perk factor
//  levels : each +1 adds +1 min and +gain max, also multiplied by the metal
//  thrown : the same idea with a higher baseline (they're consumed) and a flatter light curve
public class WeaponStats {

	public enum Weight { LIGHT, MEDIUM, HEAVY }

	public final float kg, delay;
	public final int min, max, lvlMin, lvlMax;
	public final Weight weight;

	private WeaponStats(float kg, float perk, boolean thrown, Float fixedDelay) {
		this.kg = kg;
		float r = kg / 1.2f;
		float baseline = thrown ? 9f : 6f;
		float lightExp = thrown ? 0.4f : 0.6f;
		float scale = (float) Math.pow(r, r <= 1f ? lightExp : 0.9f);
		float avg = baseline * scale * perk;
		min = Math.max(1, Math.round(avg * 0.4f));
		max = Math.max(min + 1, Math.round(2f * avg - min));
		lvlMin = 1;
		lvlMax = Math.max(1, Math.round(2f * scale * perk));
		float d = fixedDelay != null ? fixedDelay
				: (float) Math.pow(r, thrown ? 0.3f : 0.45f);
		delay = Math.max(0.5f, Math.min(2f, d));
		weight = kg < 1f ? Weight.LIGHT : (kg <= 2f ? Weight.MEDIUM : Weight.HEAVY);
	}

	private static final HashMap<Class<?>, WeaponStats> TABLE = new HashMap<>();

	private static void melee(Class<?> c, float kg, float perk) {
		TABLE.put(c, new WeaponStats(kg, perk, false, null));
	}

	private static void melee(Class<?> c, float kg, float perk, float delay) {
		TABLE.put(c, new WeaponStats(kg, perk, false, delay));
	}

	private static void thrown(Class<?> c, float kg, float perk) {
		TABLE.put(c, new WeaponStats(kg, perk, true, null));
	}

	static {
		//light
		melee(Gloves.class,         0.3f, 0.90f, 0.5f); //the fastest weapon: exactly 2x speed
		melee(Dagger.class,         0.4f, 0.85f);
		melee(Dirk.class,           0.5f, 0.85f);
		melee(AssassinsBlade.class, 0.5f, 0.90f);
		melee(Sickle.class,         0.5f, 1.00f);
		melee(Whip.class,           0.5f, 0.75f);
		melee(Sai.class,            0.6f, 0.90f);
		melee(HandAxe.class,        0.8f, 0.95f);
		//medium
		melee(WornShortsword.class, 1.0f, 0.90f);
		melee(Shortsword.class,     1.0f, 1.00f);
		melee(Cudgel.class,         1.0f, 0.90f);
		melee(Gauntlet.class,       1.0f, 0.95f);
		melee(Rapier.class,         1.1f, 0.90f);
		melee(Sword.class,          1.2f, 1.00f);
		melee(Katana.class,         1.2f, 0.95f);
		melee(Bloodletter.class,    1.2f, 0.85f);
		melee(Scimitar.class,       1.3f, 1.00f);
		melee(RunicBlade.class,     1.3f, 0.85f);
		melee(Mace.class,           1.5f, 0.95f);
		melee(Longsword.class,      1.5f, 1.00f);
		melee(Quarterstaff.class,   1.8f, 0.85f);
		melee(Spear.class,          2.0f, 0.85f);
		melee(BattleAxe.class,      2.0f, 0.95f);
		melee(Flail.class,          2.0f, 1.05f);
		//heavy
		melee(WarHammer.class,      2.5f, 0.95f);
		melee(WarScythe.class,      2.5f, 1.05f);
		melee(RoundShield.class,    2.5f, 0.65f);
		melee(Greatsword.class,     3.0f, 1.00f);
		melee(Glaive.class,         3.0f, 0.85f);
		melee(Greataxe.class,       3.5f, 1.05f);
		melee(Crossbow.class,       3.5f, 0.75f);
		melee(Greatshield.class,    5.0f, 0.60f);

		//thrown (metal ones; stones, clubs, bolas, darts and the force cube keep their own stats)
		thrown(Shuriken.class,       0.10f, 1.00f);
		thrown(ThrowingSpike.class,  0.15f, 1.00f);
		thrown(ThrowingKnife.class,  0.20f, 1.00f);
		thrown(Kunai.class,          0.30f, 1.00f);
		thrown(Tomahawk.class,       0.80f, 1.00f);
		thrown(FishingSpear.class,   1.00f, 0.95f);
		thrown(HeavyBoomerang.class, 1.00f, 0.90f);
		thrown(Javelin.class,        1.20f, 1.00f);
		thrown(ThrowingSpear.class,  1.50f, 1.00f);
		thrown(ThrowingHammer.class, 1.50f, 1.05f);
		thrown(Trident.class,        2.50f, 1.00f);
	}

	public static WeaponStats of(Object weapon) {
		return TABLE.get(weapon.getClass());
	}
}
