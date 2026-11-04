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
		DAGGERS, SWORDS, AXES, BLUNT, POLEARMS, STAVES, FIST, SHIELDS, FLEXIBLE, RANGED, WANDS;

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
		if (weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand) return Family.WANDS;
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
		if (hero == null) return;
		onKill(mob, familyOf(hero.belongings.attackingWeapon()));
	}

	//each hero trains their specialties 50% faster; the duelist trains everything 25% faster
	public static float classBonus(HeroClass cls, Family f) {
		switch (cls) {
			case WARRIOR:  return (f == Family.SWORDS || f == Family.AXES || f == Family.SHIELDS) ? 1.5f : 1f;
			case MAGE:     return (f == Family.STAVES || f == Family.WANDS) ? 1.5f : 1f;
			case ROGUE:    return (f == Family.DAGGERS || f == Family.FIST) ? 1.5f : 1f;
			case HUNTRESS: return (f == Family.RANGED || f == Family.FLEXIBLE) ? 1.5f : 1f;
			case CLERIC:   return (f == Family.BLUNT || f == Family.STAVES) ? 1.5f : 1f;
			case DUELIST:  return 1.25f;
			default:       return 1f;
		}
	}

	public static void onKill(Mob mob, Family f) {
		Hero hero = Dungeon.hero;
		if (hero == null || Dungeon.testRoom) return; //the test room doesn't train you
		if (f == null) return;

		float base = 1 + Math.max(0, mob.EXP) / 5;
		if (mob.buff(Elite.class) != null) base *= 2;
		base *= classBonus(hero.heroClass, f);
		//fractional xp is kept as a chance of an extra point
		int gain = (int) base + (Random.Float() < base - (int) base ? 1 : 0);

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

	// ---- skills (ranks and active slots are saved per hero class, like proficiency) ----

	public static int rank(HeroClass cls, Skill s) {
		load();
		String k = cls.name() + "_skill_" + s.name();
		return data.contains(k) ? data.getInt(k) : 0;
	}

	public static int rank(Skill s) {
		return Dungeon.hero == null ? 0 : rank(Dungeon.hero.heroClass, s);
	}

	public static int pointsSpent(HeroClass cls, Family f) {
		int spent = 0;
		for (Skill s : Skill.of(f)) spent += rank(cls, s);
		return spent;
	}

	public static int pointsAvailable(HeroClass cls, Family f) {
		return level(cls, f) - pointsSpent(cls, f);
	}

	public static boolean canLearn(HeroClass cls, Skill s) {
		if (!s.implemented) return false;
		if (rank(cls, s) >= s.maxRank()) return false;
		if (pointsAvailable(cls, s.family) <= 0) return false;
		if (s.kind == Skill.Kind.ULTIMATE && level(cls, s.family) < Skill.ULTIMATE_LEVEL) return false;
		return true;
	}

	public static void learn(HeroClass cls, Skill s) {
		if (!canLearn(cls, s)) return;
		load();
		data.put(cls.name() + "_skill_" + s.name(), rank(cls, s) + 1);
		saveNeeded = true;
		save();
	}

	//respec: refunds every point in one family (used by the tome of unlearning)
	public static void unlearn(HeroClass cls, Family f) {
		load();
		for (Skill s : Skill.of(f)) {
			data.remove(cls.name() + "_skill_" + s.name());
			for (int i = 0; i < MAX_SLOTS; i++) if (slot(cls, i) == s) setSlot(cls, i, null);
		}
		saveNeeded = true;
		save();
	}

	public static final int MAX_SLOTS = 3;

	//2 active skill slots, a third at hero level 20
	public static int slotsUnlocked(Hero hero) {
		return hero.lvl >= 20 ? 3 : 2;
	}

	public static Skill slot(HeroClass cls, int i) {
		load();
		String k = cls.name() + "_slot" + i;
		if (!data.contains(k)) return null;
		try {
			return Skill.valueOf(data.getString(k));
		} catch (Exception e) {
			return null;
		}
	}

	public static void setSlot(HeroClass cls, int i, Skill s) {
		load();
		String k = cls.name() + "_slot" + i;
		if (s == null) data.remove(k);
		else {
			//a skill sits in one slot at a time
			for (int j = 0; j < MAX_SLOTS; j++) if (j != i && slot(cls, j) == s) data.remove(cls.name() + "_slot" + j);
			data.put(k, s.name());
		}
		saveNeeded = true;
		save();
	}

	//passive helpers: rank of a passive, but only while a weapon of its family is equipped
	public static int activeRank(Hero hero, Skill s) {
		if (hero == null) return 0;
		int r = rank(hero.heroClass, s);
		if (r == 0) return 0;
		return familyOf(hero.belongings.attackingWeapon()) == s.family ? r : 0;
	}
}
