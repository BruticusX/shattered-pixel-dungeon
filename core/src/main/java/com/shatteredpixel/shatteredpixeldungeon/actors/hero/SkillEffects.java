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
			//daggers
			case BACKSTAB:
				withAdjacentTarget(hero, s, rank, new TargetAction() {
					public void run(Char t) { backstab(hero, t, rank); }
				});
				break;
			case FAN_OF_KNIVES: {
				ArrayList<Char> targets = visibleEnemies(hero, 4);
				java.util.Collections.shuffle(targets);
				int n = Math.min(targets.size(), 2 + rank);
				for (int i = 0; i < n; i++) hero.attack(targets.get(i), 0.5f, 0, 1f);
				Sample.INSTANCE.play(Assets.Sounds.MISS);
				finish(hero, s, rank, hero.attackDelay());
				break;
			}
			case DEATH_MARK:
				withVisibleTarget(hero, s, rank, new TargetAction() {
					public void run(Char t) {
						Buff.affect(t, DeathMarked.class, 10f);
						t.sprite.showStatus(CharSprite.NEGATIVE, s.title());
						Sample.INSTANCE.play(Assets.Sounds.DEBUFF);
					}
				}, 1f);
				break;
			//axes
			case SUNDER:
				withAdjacentTarget(hero, s, rank, new TargetAction() {
					public void run(Char t) {
						hero.attack(t, 1.1f + 0.2f * rank, 0, 1f);
						if (t.isAlive()) Buff.affect(t, Sundered.class, 10f).pct = 0.15f + 0.15f * rank;
						Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH);
					}
				});
				break;
			case WHIRLWIND: {
				for (Char t : adjacentEnemies(hero)) {
					hero.attack(t, 0.7f + 0.2f * rank, 0, 1f);
					knockBack(hero, t, 1);
				}
				Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH);
				finish(hero, s, rank, hero.attackDelay());
				break;
			}
			case RAMPAGE:
				Buff.affect(hero, Rampage.class, 8f);
				hero.sprite.showStatus(CharSprite.POSITIVE, s.title());
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
				finish(hero, s, rank, 1f);
				break;
			//blunt
			case SKULL_CRACK:
				withAdjacentTarget(hero, s, rank, new TargetAction() {
					public void run(Char t) {
						hero.attack(t, (1.05f + 0.15f * rank) * concussedBonus(hero, t), 0, 1f);
						if (t.isAlive()) {
							float stun = rank == 1 ? 2 : 3;
							if (t.properties().contains(Char.Property.BOSS)) stun = 1;
							Buff.prolong(t, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class, stun);
						}
						Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH);
					}
				});
				break;
			case GROUND_SLAM: {
				for (Char t : visibleEnemies(hero, 2)) {
					hero.attack(t, (0.5f + 0.2f * rank) * concussedBonus(hero, t), 0, 1f);
					if (t.isAlive()) Buff.prolong(t, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple.class, 3f);
				}
				com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.shake(1, 0.4f);
				Sample.INSTANCE.play(Assets.Sounds.ROCKS);
				finish(hero, s, rank, hero.attackDelay());
				break;
			}
			case EARTHSHAKER: {
				for (Char t : visibleEnemies(hero, 99)) {
					hero.attack(t, 1.5f * concussedBonus(hero, t), 0, 1f);
					if (t.isAlive()) Buff.prolong(t, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class, 2f);
				}
				com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.shake(3, 0.8f);
				Sample.INSTANCE.play(Assets.Sounds.BLAST);
				finish(hero, s, rank, hero.attackDelay());
				break;
			}
			default:
				GLog.w(Messages.get(SkillEffects.class, "not_ready"));
		}
	}

	// ---------------- targeting helpers ----------------

	private interface TargetAction { void run(Char target); }

	private static ArrayList<Char> adjacentEnemies(Hero hero) {
		ArrayList<Char> list = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar(hero.pos + i);
			if (ch != null && ch.alignment == Char.Alignment.ENEMY) list.add(ch);
		}
		return list;
	}

	private static ArrayList<Char> visibleEnemies(Hero hero, int range) {
		ArrayList<Char> list = new ArrayList<>();
		for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m : Dungeon.level.mobs.toArray(new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob[0])) {
			if (m.alignment == Char.Alignment.ENEMY && hero.fieldOfView[m.pos]
					&& Dungeon.level.distance(hero.pos, m.pos) <= range) list.add(m);
		}
		return list;
	}

	//uses the only adjacent enemy, or asks for one
	private static void withAdjacentTarget(final Hero hero, final Skill s, final int rank, final TargetAction action) {
		Char only = soleAdjacentEnemy(hero);
		if (only != null) {
			action.run(only);
			finish(hero, s, rank, hero.attackDelay());
			return;
		}
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				if (cell == null) return;
				Char ch = Actor.findChar(cell);
				if (ch == null || ch == hero || !Dungeon.level.adjacent(hero.pos, cell) || ch.alignment == Char.Alignment.ALLY) {
					GLog.w(Messages.get(SkillEffects.class, "need_adjacent"));
					return;
				}
				action.run(ch);
				finish(hero, s, rank, hero.attackDelay());
			}
			@Override
			public String prompt() { return Messages.get(SkillEffects.class, "choose_target"); }
		});
	}

	private static void withVisibleTarget(final Hero hero, final Skill s, final int rank, final TargetAction action, final float time) {
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				if (cell == null) return;
				Char ch = Actor.findChar(cell);
				if (ch == null || ch == hero || !hero.fieldOfView[cell] || ch.alignment == Char.Alignment.ALLY) {
					GLog.w(Messages.get(SkillEffects.class, "need_visible"));
					return;
				}
				action.run(ch);
				finish(hero, s, rank, time);
			}
			@Override
			public String prompt() { return Messages.get(SkillEffects.class, "choose_target"); }
		});
	}

	private static void knockBack(Hero hero, Char t, int power) {
		if (!t.isAlive()) return;
		int dest = t.pos + (t.pos - hero.pos);
		com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica traj = new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(
				t.pos, dest, com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.MAGIC_BOLT);
		com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave.throwChar(t, traj, power, false, true, hero);
	}

	// ---------------- daggers ----------------

	private static void backstab(Hero hero, Char t, int rank) {
		//blink to the far side of the target if there's room
		int behind = t.pos + (t.pos - hero.pos);
		if (behind >= 0 && behind < Dungeon.level.length() && Dungeon.level.passable[behind]
				&& Actor.findChar(behind) == null) {
			com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation.appear(hero, behind);
			Dungeon.observe();
		}
		hero.attack(t, 1.25f + 0.25f * rank, 0, Char.INFINITE_ACCURACY);
		if (t.isAlive() && Proficiency.rank(hero.heroClass, Skill.LACERATE) > 0) {
			Buff.affect(t, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding.class).set(2 + Dungeon.scalingDepth() / 2f);
		}
		Sample.INSTANCE.play(Assets.Sounds.HIT_STAB);
	}

	public static class DeathMarked extends FlavourBuff {
		{ type = buffType.NEGATIVE; announced = true; }
		@Override public int icon() { return BuffIndicator.MARK; }
		@Override public void tintIcon(com.watabou.noosa.Image icon) { icon.hardlight(1f, 0.2f, 0.2f); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	// ---------------- axes ----------------

	public static class Sundered extends FlavourBuff {
		{ type = buffType.NEGATIVE; announced = true; }
		public float pct = 0.3f;
		@Override public int icon() { return BuffIndicator.ARMOR; }
		@Override public void tintIcon(com.watabou.noosa.Image icon) { icon.hardlight(1f, 0.5f, 0.2f); }
		@Override public String desc() { return Messages.get(this, "desc", Math.round(pct * 100), dispTurns()); }
		@Override public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("pct", pct); }
		@Override public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); pct = b.getFloat("pct"); }
	}

	public static class Rampage extends FlavourBuff {
		{ type = buffType.POSITIVE; announced = true; }
		@Override public int icon() { return BuffIndicator.FURY; }
		@Override public void tintIcon(com.watabou.noosa.Image icon) { icon.hardlight(1f, 0.3f, 0.1f); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
		public void extend(float turns) { spend(turns); }
	}

	// ---------------- blunt ----------------

	//dazed or stunned targets take +20% from blunt skills when concussive is learned
	private static float concussedBonus(Hero hero, Char t) {
		if (Proficiency.rank(hero.heroClass, Skill.CONCUSSIVE) == 0) return 1f;
		if (t.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze.class) != null
				|| t.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class) != null) return 1.2f;
		return 1f;
	}

	// ---------------- armor and kills ----------------

	//how much of the target's armor still counts against the hero's hit
	public static int modifyDR(Hero hero, Char enemy, int dr) {
		float keep = 1f;
		int cw = Proficiency.activeRank(hero, Skill.CRUSHING_WEIGHT);
		if (cw > 0) keep *= 1f - 0.15f * cw;
		Sundered sd = enemy.buff(Sundered.class);
		if (sd != null) keep *= 1f - sd.pct;
		return Math.round(dr * keep);
	}

	//called when the hero's attack kills an enemy
	public static void onKill(Hero hero, Char enemy) {
		int ss = Proficiency.activeRank(hero, Skill.SHADOW_STEP);
		if (ss > 0) Buff.affect(hero, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility.class, ss);
		if (enemy.buff(DeathMarked.class) != null) {
			Cooldowns.clear(hero, Skill.BACKSTAB);
		}
		if (Proficiency.activeRank(hero, Skill.EXECUTIONER) > 0) {
			Cooldowns.reduce(hero, Skill.SUNDER, 3);
		}
		Rampage r = hero.buff(Rampage.class);
		if (r != null) r.extend(2f);
		GameScene.updateSkillBar();
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
		float mult = 1f;
		//death mark: +50% damage from you
		if (enemy.buff(DeathMarked.class) != null) mult += 0.5f;
		//bloodlust: +1% per 3/2.5/2% of missing HP, up to +30%
		int bl = Proficiency.activeRank(hero, Skill.BLOODLUST);
		if (bl > 0 && hero.HT > 0) {
			float missing = 100f * (hero.HT - hero.HP) / hero.HT;
			mult += Math.min(0.30f, missing / (3.5f - 0.5f * bl) / 100f);
		}
		//executioner: +25/40/55% against enemies below 30% HP
		int ex = Proficiency.activeRank(hero, Skill.EXECUTIONER);
		if (ex > 0 && enemy.HP < enemy.HT * 0.3f) mult += 0.10f + 0.15f * ex;
		//rampage: +40%, and heals for 10% of damage dealt
		if (hero.buff(Rampage.class) != null) {
			mult += 0.4f;
			int heal = Math.max(1, Math.round(damage * mult * 0.1f));
			heal = Math.min(heal, hero.HT - hero.HP);
			if (heal > 0) {
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.HEALING);
			}
		}
		damage = Math.round(damage * mult);

		//lacerate: chance to bleed
		int lc = Proficiency.activeRank(hero, Skill.LACERATE);
		if (lc > 0 && enemy.isAlive() && com.watabou.utils.Random.Float() < 0.05f + 0.10f * lc) {
			Buff.affect(enemy, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding.class).set(Math.max(1, damage / 3f));
		}
		//concussive: chance to daze
		int cc = Proficiency.activeRank(hero, Skill.CONCUSSIVE);
		if (cc > 0 && enemy.isAlive() && com.watabou.utils.Random.Float() < 0.04f + 0.04f * cc) {
			Buff.prolong(enemy, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze.class, 3f);
		}

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

		public static void clear(Hero hero, Skill s) {
			Cooldowns c = hero.buff(Cooldowns.class);
			if (c != null) c.left.remove(s);
		}

		public static void reduce(Hero hero, Skill s, int turns) {
			Cooldowns c = hero.buff(Cooldowns.class);
			if (c == null || !c.left.containsKey(s)) return;
			int v = c.left.get(s) - turns;
			if (v <= 0) c.left.remove(s); else c.left.put(s, v);
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
