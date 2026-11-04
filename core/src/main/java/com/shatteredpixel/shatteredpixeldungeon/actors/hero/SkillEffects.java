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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.HashMap;

//MOD: what each weapon skill does. Actives are used from the skill buttons; passives hook into combat.
public class SkillEffects {

	// ---------------- using actives ----------------

	//why a skill can't be used right now, or null if it can
	public static String unusableReason(Hero hero, Skill s) {
		if (Proficiency.rank(hero.heroClass, s) <= 0) return Messages.get(SkillEffects.class, "not_learned");
		if (Proficiency.familyOf(hero.belongings.attackingWeapon()) != s.family) {
			return Messages.get(SkillEffects.class, "wrong_weapon", s.family.title());
		}
		int cd = Cooldowns.left(hero, s);
		if (cd > 0) return Messages.get(SkillEffects.class, "cooling", cd);
		return null;
	}

	public static void use(final Hero hero, final Skill s) {
		if (!hero.ready) return;
		String why = unusableReason(hero, s);
		if (why != null) {
			GLog.w(why);
			return;
		}
		final int rank = Proficiency.rank(hero.heroClass, s);
		switch (s) {
			case CLEAVE:
				cleave(hero, rank);
				finish(hero, s, rank, hero.attackDelay());
				break;
			case RIPOSTE_STANCE:
				Buff.affect(hero, Riposte.class, 2f).rank = rank;
				hero.sprite.showStatus(CharSprite.POSITIVE, s.title());
				Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY);
				finish(hero, s, rank, 1f);
				break;
			case THOUSAND_CUTS:
				Char only = soleAdjacentEnemy(hero);
				if (only != null) {
					thousandCuts(hero, only);
					finish(hero, s, rank, hero.attackDelay());
				} else {
					GameScene.selectCell(new CellSelector.Listener() {
						@Override
						public void onSelect(Integer cell) {
							if (cell == null) return;
							Char ch = Actor.findChar(cell);
							if (ch == null || ch == hero || !Dungeon.level.adjacent(hero.pos, cell)
									|| ch.alignment == Char.Alignment.ALLY) {
								GLog.w(Messages.get(SkillEffects.class, "need_adjacent"));
								return;
							}
							thousandCuts(hero, ch);
							finish(hero, s, rank, hero.attackDelay());
						}

						@Override
						public String prompt() {
							return Messages.get(SkillEffects.class, "choose_target");
						}
					});
				}
				break;
			default:
				GLog.w(Messages.get(SkillEffects.class, "not_ready"));
		}
	}

	private static void finish(Hero hero, Skill s, int rank, float time) {
		Cooldowns.start(hero, s, s.cooldown(rank));
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(time);
		GameScene.updateSkillBar();
	}

	private static Char soleAdjacentEnemy(Hero hero) {
		Char found = null;
		for (int i : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar(hero.pos + i);
			if (ch != null && ch.alignment == Char.Alignment.ENEMY) {
				if (found != null) return null;
				found = ch;
			}
		}
		return found;
	}

	// ---------------- swords ----------------

	private static void cleave(Hero hero, int rank) {
		float mult = 0.6f + 0.2f * rank; //80 / 100 / 120%
		ArrayList<Char> targets = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar(hero.pos + i);
			if (ch != null && ch.alignment == Char.Alignment.ENEMY) targets.add(ch);
		}
		for (Char ch : targets) hero.attack(ch, mult, 0, 1f);
		Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH);
	}

	private static void thousandCuts(Hero hero, Char target) {
		for (int i = 0; i < 6 && target.isAlive(); i++) {
			hero.attack(target, 0.6f, 0, 1f);
		}
		Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
	}

	//riposte stance: parry melee attacks against you and counter for 100/125/150% damage
	public static class Riposte extends FlavourBuff {
		{ type = buffType.POSITIVE; announced = true; }
		public int rank = 1;

		@Override
		public int icon() { return BuffIndicator.MOMENTUM; }

		@Override
		public String desc() { return Messages.get(this, "desc", 75 + 25 * rank, dispTurns()); }

		@Override
		public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("rank", rank); }
		@Override
		public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); rank = b.getInt("rank"); }
	}

	//called when something attacks the hero in melee; returns true if the attack was parried
	public static boolean tryRiposte(Char attacker, Char defender) {
		if (!(defender instanceof Hero)) return false;
		Riposte r = defender.buff(Riposte.class);
		if (r == null || !Dungeon.level.adjacent(attacker.pos, defender.pos)) return false;
		if (defender.sprite != null) defender.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(SkillEffects.class, "parried"));
		Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY);
		if (attacker.isAlive()) defender.attack(attacker, 0.75f + 0.25f * r.rank, 0, Char.INFINITE_ACCURACY);
		return true;
	}

	// ---------------- passives ----------------

	//blade discipline: +5/10/15% accuracy with swords, and with daggers too
	public static float accuracyFactor(Hero hero) {
		int r = Proficiency.rank(hero.heroClass, Skill.BLADE_DISCIPLINE);
		if (r == 0) return 1f;
		Proficiency.Family f = Proficiency.familyOf(hero.belongings.attackingWeapon());
		if (f == Proficiency.Family.SWORDS || f == Proficiency.Family.DAGGERS) return 1f + 0.05f * r;
		return 1f;
	}

	//momentum: each hit in a row on the same target adds +4/6/8% damage, up to 5 stacks
	public static int onHeroHit(Hero hero, Char enemy, int damage) {
		int r = Proficiency.activeRank(hero, Skill.MOMENTUM);
		if (r > 0) {
			MomentumStacks m = Buff.affect(hero, MomentumStacks.class);
			if (m.target != enemy.id()) {
				m.target = enemy.id();
				m.stacks = 0;
			}
			damage = Math.round(damage * (1f + (0.02f + 0.02f * r) * m.stacks));
			m.stacks = Math.min(5, m.stacks + 1);
		}
		return damage;
	}

	public static class MomentumStacks extends Buff {
		{ type = buffType.POSITIVE; }
		public int target = -1;
		public int stacks = 0;

		@Override
		public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("target", target); b.put("stacks", stacks); }
		@Override
		public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); target = b.getInt("target"); stacks = b.getInt("stacks"); }
	}

	// ---------------- cooldowns ----------------

	public static class Cooldowns extends Buff {
		{ type = buffType.NEUTRAL; revivePersists = true; }

		private final HashMap<Skill, Integer> left = new HashMap<>();

		public static int left(Hero hero, Skill s) {
			Cooldowns c = hero.buff(Cooldowns.class);
			if (c == null) return 0;
			Integer v = c.left.get(s);
			return v == null ? 0 : v;
		}

		public static void start(Hero hero, Skill s, int turns) {
			if (turns <= 0) return;
			Buff.affect(hero, Cooldowns.class).left.put(s, turns);
		}

		@Override
		public boolean act() {
			boolean changed = false;
			for (Skill s : new ArrayList<>(left.keySet())) {
				int v = left.get(s) - 1;
				if (v <= 0) left.remove(s); else left.put(s, v);
				changed = true;
			}
			if (changed) GameScene.updateSkillBar();
			spend(TICK);
			return true;
		}

		@Override
		public void storeInBundle(Bundle b) {
			super.storeInBundle(b);
			for (Skill s : left.keySet()) b.put("cd_" + s.name(), left.get(s));
		}

		@Override
		public void restoreFromBundle(Bundle b) {
			super.restoreFromBundle(b);
			for (Skill s : Skill.values()) {
				if (b.contains("cd_" + s.name())) left.put(s, b.getInt("cd_" + s.name()));
			}
		}
	}
}
