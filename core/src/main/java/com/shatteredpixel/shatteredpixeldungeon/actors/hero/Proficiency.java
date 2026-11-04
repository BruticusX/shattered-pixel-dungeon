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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Elite;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import com.watabou.utils.Random;

import java.io.IOException;
import java.util.HashMap;

//MOD: weapon proficiency. Kills with a weapon family raise that family's proficiency (0-10),
//which raises accuracy (x0.85 at 0 up to x1.35 at 10), damage (+2% per level) and the chance that
//a miss still lands as a glancing blow. Proficiency is kept per hero class across runs.
public class Proficiency {

	public enum Family {
		DAGGERS, SWORDS, AXES, BLUNT, POLEARMS, STAVES, FIST, SHIELDS, FLEXIBLE, RANGED;

		public String title() {
			return Messages.get(Proficiency.class, name().toLowerCase());
		}
	}

	public static final int MAX_LEVEL = 10;
	//total proficiency xp needed for each level (about one xp per kill, more for tough enemies)
	private static final int[] THRESHOLDS = {5, 12, 22, 38, 60, 90, 130, 190, 280, 400};

	private static final HashMap<Class<?>, Family> FAMILIES = new HashMap<>();
	private static void fam(Family f, Class<?>... classes) {
		for (Class<?> c : classes) FAMILIES.put(c, f);
	}
	static {
		fam(Family.DAGGERS,  Dagger.class, Dirk.class, AssassinsBlade.class);
		fam(Family.SWORDS,   WornShortsword.class, Shortsword.class, Sword.class, Longsword.class, Greatsword.class,
				Scimitar.class, Katana.class, Rapier.class, RunicBlade.class, Bloodletter.class);
		fam(Family.AXES,     HandAxe.class, BattleAxe.class, Greataxe.class);
		fam(Family.BLUNT,    Cudgel.class, Mace.class, WarHammer.class);
		fam(Family.POLEARMS, Spear.class, Glaive.class, Sickle.class, WarScythe.class);
		fam(Family.STAVES,   Quarterstaff.class, MagesStaff.class);
		fam(Family.FIST,     Gloves.class, Gauntlet.class, Sai.class);
		fam(Family.SHIELDS,  RoundShield.class, Greatshield.class);
		fam(Family.FLEXIBLE, Whip.class, Flail.class);
		fam(Family.RANGED,   Crossbow.class, SpiritBow.class);
	}

	public static Family familyOf(Object weapon) {
		if (weapon == null) return null;
		if (weapon instanceof MissileWeapon || weapon instanceof SpiritBow) return Family.RANGED;
		Family f = FAMILIES.get(weapon.getClass());
		if (f == null) {
			//subclasses (e.g. modded variants) use their parent's family
			for (Class<?> c = weapon.getClass().getSuperclass(); c != null && f == null; c = c.getSuperclass()) {
				f = FAMILIES.get(c);
			}
		}
		return f;
	}

	// ---- storage: per hero class, across runs ----

	private static final String FILE = "proficiency.dat";
	private static Bundle data = null;
	private static boolean saveNeeded = false;

	private static void load() {
		if (data != null) return;
		try {
			data = FileUtils.bundleFromFile(FILE);
		} catch (IOException e) {
			data = new Bundle();
		}
	}

	public static void save() {
		if (!saveNeeded || data == null) return;
		try {
			FileUtils.bundleToFile(FILE, data);
			saveNeeded = false;
		} catch (IOException e) {
			ShatteredPixelDungeon.reportException(e);
		}
	}

	private static String key(HeroClass cls, Family f) {
		return cls.name() + "_" + f.name();
	}

	public static int xp(HeroClass cls, Family f) {
		load();
		return data.contains(key(cls, f)) ? data.getInt(key(cls, f)) : 0;
	}

	public static int level(HeroClass cls, Family f) {
		int xp = xp(cls, f);
		int lvl = 0;
		while (lvl < MAX_LEVEL && xp >= THRESHOLDS[lvl]) lvl++;
		return lvl;
	}

	//xp needed for the next level (or the max threshold at level 10)
	public static int nextThreshold(int level) {
		return THRESHOLDS[Math.min(level, MAX_LEVEL - 1)];
	}

	public static int prevThreshold(int level) {
		return level <= 0 ? 0 : THRESHOLDS[level - 1];
	}

	public static int level(Hero hero, Object weapon) {
		Family f = familyOf(weapon);
		return f == null ? 0 : level(hero.heroClass, f);
	}

	// ---- effects ----

	public static float accuracyFactor(Hero hero, Object weapon) {
		if (familyOf(weapon) == null) return 1f;
		return 0.85f + 0.05f * level(hero, weapon);
	}

	public static float damageFactor(Hero hero, Object weapon) {
		if (familyOf(weapon) == null) return 1f;
		return 1f + 0.02f * level(hero, weapon);
	}

	//chance that a missed weapon attack still lands as a glancing blow for 40% damage
	public static boolean rollGlancing(Char attacker, Char defender) {
		if (!(attacker instanceof Hero)) return false;
		//parries, guards and other perfect dodges can't be glanced through
		if (defender.defenseSkill(attacker) >= Char.INFINITE_EVASION) return false;
		Hero hero = (Hero) attacker;
		KindOfWeapon wep = hero.belongings.attackingWeapon();
		if (familyOf(wep) == null) return false;
		return Random.Float() < 0.40f + 0.03f * level(hero, wep);
	}

	public static final float GLANCING_DAMAGE = 0.4f;

	// ---- gaining proficiency ----

	public static void onKill(Mob mob) {
		Hero hero = Dungeon.hero;
		if (hero == null || Dungeon.testRoom) return; //the test room doesn't train you
		KindOfWeapon wep = hero.belongings.attackingWeapon();
		Family f = familyOf(wep);
		if (f == null) return;

		int gain = 1 + Math.max(0, mob.EXP) / 5;
		if (mob.buff(Elite.class) != null) gain *= 2;

		int before = level(hero.heroClass, f);
		load();
		data.put(key(hero.heroClass, f), xp(hero.heroClass, f) + gain);
		saveNeeded = true;
		int after = level(hero.heroClass, f);

		if (after > before) {
			GLog.p(Messages.get(Proficiency.class, "level_up", f.title(), after));
			if (hero.sprite != null) {
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, f.title() + " " + after, FloatingText.EXPERIENCE);
			}
			save();
		}
	}
}
