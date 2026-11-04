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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Halberd;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Khopesh;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Stiletto;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//MOD: the on-hit perks of the Phase 4 weapon types, shared so each weapon class stays small.
public class WeaponPerks {

	//how much of the target's armor the hero's weapon leaves in place
	public static float drKeep(Hero hero) {
		KindOfWeapon w = hero.belongings.attackingWeapon();
		if (w instanceof Stiletto) return 0.25f; //slips between plates
		if (w instanceof Halberd)  return 0.5f;  //the axe head cleaves armor
		if (w instanceof Khopesh)  return 0.7f;  //hooks the guard aside
		return 1f;
	}

	//elites can't parry a khopesh: the hook drags their guard aside
	public static boolean ignoresParry(Char attacker) {
		return attacker instanceof Hero && ((Hero) attacker).belongings.attackingWeapon() instanceof Khopesh;
	}

	public static void bleed(Char defender, int damage, float chance) {
		if (!defender.isAlive() || Random.Float() >= chance) return;
		Buff.affect(defender, Bleeding.class).set(Math.max(1, Math.round(damage * 0.4f)));
	}

	public static void cripple(Char attacker, Char defender, float chance) {
		if (!defender.isAlive() || Random.Float() >= chance) return;
		Buff.prolong(defender, Cripple.class, 3f);
	}

	public static void daze(Char defender, float chance) {
		if (!defender.isAlive() || Random.Float() >= chance) return;
		Buff.prolong(defender, Daze.class, 3f);
	}

	//the first hit on each enemy is stronger
	public static class Struck extends Buff {
		{ revivePersists = true; }
	}

	public static int firstHit(Char defender, int damage, float mult) {
		if (defender.buff(Struck.class) != null) return damage;
		Buff.affect(defender, Struck.class);
		return Math.round(damage * mult);
	}

	//weaker against adjacent enemies (long polearms)
	public static int upClose(Char attacker, Char defender, int damage, float mult) {
		if (Dungeon.level.adjacent(attacker.pos, defender.pos)) return Math.round(damage * mult);
		return damage;
	}

	public static void knockBack(Char attacker, Char defender, int power, float chance) {
		if (!defender.isAlive() || Random.Float() >= chance) return;
		if (defender.properties().contains(Char.Property.IMMOVABLE)) return;
		int dest = defender.pos + (defender.pos - attacker.pos);
		Ballistica traj = new Ballistica(defender.pos, dest, Ballistica.MAGIC_BOLT);
		WandOfBlastWave.throwChar(defender, traj, power, false, true, attacker);
	}

	//pulls an enemy that was hit from range into the free tile next to the attacker closest to it
	public static void pull(Char attacker, Char defender, float chance) {
		if (!defender.isAlive() || Random.Float() >= chance) return;
		if (Dungeon.level.adjacent(attacker.pos, defender.pos)) return;
		if (defender.properties().contains(Char.Property.IMMOVABLE) || defender.properties().contains(Char.Property.BOSS)) return;
		int best = -1;
		for (int i : PathFinder.NEIGHBOURS8) {
			int c = attacker.pos + i;
			if (!Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
			if (best == -1 || Dungeon.level.distance(c, defender.pos) < Dungeon.level.distance(best, defender.pos)) best = c;
		}
		if (best == -1) return;
		Actor.add(new Pushing(defender, defender.pos, best));
		defender.pos = best;
		Dungeon.level.occupyCell(defender);
	}
}
