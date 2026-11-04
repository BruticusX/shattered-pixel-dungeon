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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Proficiency.Family;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import java.util.ArrayList;

//MOD: weapon skills. Each family has 2 actives, 2 passives (3 ranks each) and 1 ultimate
//(1 rank, needs proficiency 8). Points come from proficiency levels: one per level.
//Skills marked implemented=false are shown in the tree but can't be learned yet.
public enum Skill {

	//swords
	CLEAVE           (Family.SWORDS,   Kind.ACTIVE,   true,  15, 13, 11),
	RIPOSTE_STANCE   (Family.SWORDS,   Kind.ACTIVE,   true,  25, 22, 19),
	BLADE_DISCIPLINE (Family.SWORDS,   Kind.PASSIVE,  true),
	MOMENTUM         (Family.SWORDS,   Kind.PASSIVE,  true),
	THOUSAND_CUTS    (Family.SWORDS,   Kind.ULTIMATE, true,  70),

	//daggers
	BACKSTAB         (Family.DAGGERS,  Kind.ACTIVE,   false, 20, 17, 14),
	FAN_OF_KNIVES    (Family.DAGGERS,  Kind.ACTIVE,   false, 25, 25, 25),
	LACERATE         (Family.DAGGERS,  Kind.PASSIVE,  false),
	SHADOW_STEP      (Family.DAGGERS,  Kind.PASSIVE,  false),
	DEATH_MARK       (Family.DAGGERS,  Kind.ULTIMATE, false, 80),

	//axes
	SUNDER           (Family.AXES,     Kind.ACTIVE,   false, 18, 16, 14),
	WHIRLWIND        (Family.AXES,     Kind.ACTIVE,   false, 25, 22, 19),
	BLOODLUST        (Family.AXES,     Kind.PASSIVE,  false),
	EXECUTIONER      (Family.AXES,     Kind.PASSIVE,  false),
	RAMPAGE          (Family.AXES,     Kind.ULTIMATE, false, 90),

	//blunt
	SKULL_CRACK      (Family.BLUNT,    Kind.ACTIVE,   false, 20, 18, 15),
	GROUND_SLAM      (Family.BLUNT,    Kind.ACTIVE,   false, 28, 25, 22),
	CRUSHING_WEIGHT  (Family.BLUNT,    Kind.PASSIVE,  false),
	CONCUSSIVE       (Family.BLUNT,    Kind.PASSIVE,  false),
	EARTHSHAKER      (Family.BLUNT,    Kind.ULTIMATE, false, 100),

	//polearms
	IMPALE           (Family.POLEARMS, Kind.ACTIVE,   false, 18, 16, 14),
	SWEEP            (Family.POLEARMS, Kind.ACTIVE,   false, 22, 20, 18),
	HOLD_THE_LINE    (Family.POLEARMS, Kind.PASSIVE,  false),
	LONG_REACH       (Family.POLEARMS, Kind.PASSIVE,  false),
	REAPING_STORM    (Family.POLEARMS, Kind.ULTIMATE, false, 85),

	//staves
	ARCANE_STRIKE    (Family.STAVES,   Kind.ACTIVE,   false, 15, 13, 11),
	WARD             (Family.STAVES,   Kind.ACTIVE,   false, 30, 27, 24),
	CHANNELING       (Family.STAVES,   Kind.PASSIVE,  false),
	DEFLECTION       (Family.STAVES,   Kind.PASSIVE,  false),
	ARCANE_OVERLOAD  (Family.STAVES,   Kind.ULTIMATE, false, 90),

	//fist
	FLURRY           (Family.FIST,     Kind.ACTIVE,   false, 12, 10, 8),
	UPPERCUT         (Family.FIST,     Kind.ACTIVE,   false, 20, 18, 16),
	COMBO_COUNTER    (Family.FIST,     Kind.PASSIVE,  false),
	FOOTWORK         (Family.FIST,     Kind.PASSIVE,  false),
	HUNDRED_FISTS    (Family.FIST,     Kind.ULTIMATE, false, 60),

	//shields
	SHIELD_BASH      (Family.SHIELDS,  Kind.ACTIVE,   false, 16, 14, 12),
	BULWARK          (Family.SHIELDS,  Kind.ACTIVE,   false, 30, 27, 24),
	THORNS           (Family.SHIELDS,  Kind.PASSIVE,  false),
	STEADFAST        (Family.SHIELDS,  Kind.PASSIVE,  false),
	AEGIS            (Family.SHIELDS,  Kind.ULTIMATE, false, 100),

	//flexible
	LASH_PULL        (Family.FLEXIBLE, Kind.ACTIVE,   false, 18, 16, 14),
	CHAIN_SPIN       (Family.FLEXIBLE, Kind.ACTIVE,   false, 24, 21, 18),
	ENTANGLE         (Family.FLEXIBLE, Kind.PASSIVE,  false),
	WILD_SWING       (Family.FLEXIBLE, Kind.PASSIVE,  false),
	GRAND_LASSO      (Family.FLEXIBLE, Kind.ULTIMATE, false, 90),

	//ranged
	PIERCING_SHOT    (Family.RANGED,   Kind.ACTIVE,   false, 16, 14, 12),
	VOLLEY           (Family.RANGED,   Kind.ACTIVE,   false, 26, 23, 20),
	STEADY_AIM       (Family.RANGED,   Kind.PASSIVE,  false),
	RECOVERY         (Family.RANGED,   Kind.PASSIVE,  false),
	RAIN_OF_ARROWS   (Family.RANGED,   Kind.ULTIMATE, false, 85),

	//wands
	OVERCHARGE       (Family.WANDS,    Kind.ACTIVE,   false, 20, 17, 14),
	ELEMENTAL_INFUSION(Family.WANDS,   Kind.ACTIVE,   false, 30, 27, 24),
	ATTUNEMENT       (Family.WANDS,    Kind.PASSIVE,  false),
	RESONANCE        (Family.WANDS,    Kind.PASSIVE,  false),
	SPELL_FUSION     (Family.WANDS,    Kind.ULTIMATE, false, 60);

	public enum Kind { ACTIVE, PASSIVE, ULTIMATE }

	public static final int ULTIMATE_LEVEL = 8;

	public final Family family;
	public final Kind kind;
	public final boolean implemented;
	private final int[] cooldowns;

	Skill(Family family, Kind kind, boolean implemented, int... cooldowns) {
		this.family = family;
		this.kind = kind;
		this.implemented = implemented;
		this.cooldowns = cooldowns;
	}

	public int maxRank() {
		return kind == Kind.ULTIMATE ? 1 : 3;
	}

	public boolean usable() {
		return kind != Kind.PASSIVE;
	}

	public int cooldown(int rank) {
		if (cooldowns.length == 0) return 0;
		return cooldowns[Math.max(0, Math.min(rank, cooldowns.length) - 1)];
	}

	public String title() {
		return Messages.get(Skill.class, name().toLowerCase() + ".name");
	}

	public String desc() {
		return Messages.get(Skill.class, name().toLowerCase() + ".desc");
	}

	public static ArrayList<Skill> of(Family f) {
		ArrayList<Skill> list = new ArrayList<>();
		for (Skill s : values()) if (s.family == f) list.add(s);
		return list;
	}
}
