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
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import java.util.ArrayList;
import java.util.HashMap;

//MOD: what each weapon skill does. Actives are used from the skill buttons; passives hook into combat.
public class SkillEffects {

	// ---------------- using actives ----------------

	//why a skill can't be used right now, or null if it can
	//is the hero holding a weapon of this family right now (the equipped weapon, not a thrown one)
	public static boolean holding(Hero hero, Proficiency.Family f) {
		return Proficiency.familyOf(hero.belongings.weapon()) == f;
	}

	//MOD: the duelist can use skills from any family with any melee weapon, except skills that
	// need a specific kind of weapon: ranged and wand skills, shield skills, and the polearm reach skills
	private static final java.util.EnumSet<Skill> NEEDS_OWN_WEAPON =
			java.util.EnumSet.of(Skill.IMPALE, Skill.LONG_REACH, Skill.REAPING_STORM);

	public static boolean duelistFree(Hero hero, Skill s) {
		if (hero == null || hero.heroClass != HeroClass.DUELIST) return false;
		if (hero.belongings.weapon() == null) return false;
		if (s.family == Proficiency.Family.RANGED || s.family == Proficiency.Family.WANDS
				|| s.family == Proficiency.Family.SHIELDS || NEEDS_OWN_WEAPON.contains(s)) return false;
		return true;
	}

	//holding a weapon that can use this skill (its family, or any melee weapon for the duelist)
	public static boolean canUseWith(Hero hero, Skill s) {
		return holding(hero, s.family) || duelistFree(hero, s);
	}

	public static String unusableReason(Hero hero, Skill s) {
		if (Proficiency.rank(hero.heroClass, s) <= 0) return Messages.get(SkillEffects.class, "not_learned");
		if (s.family == Proficiency.Family.RANGED) {
			if (rangedWeapon(hero) == null) return Messages.get(SkillEffects.class, "wrong_weapon", s.family.title());
		} else if (s.family == Proficiency.Family.WANDS) {
			if (!hasWand(hero)) return Messages.get(SkillEffects.class, "need_wand");
		} else if (!canUseWith(hero, s)) {
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
		markSkillStrike(hero);
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
			//polearms
			case IMPALE:
				withVisibleTarget(hero, s, rank, new TargetAction() {
					public void run(Char t) {
						int max = rank == 1 ? 2 : 3;
						for (Char ch : lineTargets(hero, t.pos, 2 + (Proficiency.activeRank(hero, Skill.LONG_REACH) >= 3 ? 1 : 0), max)) {
							hero.attack(ch, 0.95f + 0.15f * rank, 0, 1f);
						}
						Sample.INSTANCE.play(Assets.Sounds.HIT_STAB);
					}
				}, hero.attackDelay());
				break;
			case SWEEP:
				withAdjacentTarget(hero, s, rank, new TargetAction() {
					public void run(Char t) {
						for (Char ch : arcTargets(hero, t.pos)) {
							hero.attack(ch, 0.65f + 0.15f * rank, 0, 1f);
							knockBack(hero, ch, rank == 3 ? 2 : 1);
						}
						Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH);
					}
				});
				break;
			case REAPING_STORM:
				Buff.affect(hero, ReapingStorm.class, 5f);
				hero.sprite.showStatus(CharSprite.POSITIVE, s.title());
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
				finish(hero, s, rank, 1f);
				break;
			//staves
			case ARCANE_STRIKE:
				withAdjacentTarget(hero, s, rank, new TargetAction() {
					public void run(Char t) {
						hero.attack(t, 0.8f + 0.2f * rank, 0, 1f);
						if (t.isAlive()) arcaneBurst(hero, t);
					}
				});
				break;
			case WARD: {
				int shield = Math.round(hero.HT * (0.10f + 0.05f * rank));
				Buff.affect(hero, Ward.class, 10f);
				Buff.affect(hero, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier.class).setShield(shield);
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.SHIELDING);
				Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
				finish(hero, s, rank, 1f);
				break;
			}
			case ARCANE_OVERLOAD:
				Buff.affect(hero, ArcaneOverload.class, 6f);
				hero.sprite.showStatus(CharSprite.POSITIVE, s.title());
				Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
				finish(hero, s, rank, 1f);
				break;
			//fist
			case FLURRY:
				withAdjacentTarget(hero, s, rank, new TargetAction() {
					public void run(Char t) {
						for (int i = 0; i < 3 && t.isAlive(); i++) hero.attack(t, 0.5f + 0.1f * rank, 0, 1f);
						Sample.INSTANCE.play(Assets.Sounds.HIT);
					}
				});
				break;
			case UPPERCUT:
				withAdjacentTarget(hero, s, rank, new TargetAction() {
					public void run(Char t) {
						hero.attack(t, 1.1f + 0.2f * rank, 0, 1f);
						if (t.isAlive()) {
							Buff.prolong(t, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze.class, 3f);
							knockBack(hero, t, 2);
						}
						Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
					}
				});
				break;
			case HUNDRED_FISTS:
				withAdjacentTarget(hero, s, rank, new TargetAction() {
					public void run(Char t) {
						ComboPoints cp = hero.buff(ComboPoints.class);
						int n = Math.max(1, cp == null ? 0 : cp.points);
						if (cp != null) cp.detach();
						for (int i = 0; i < n && t.isAlive(); i++) hero.attack(t, 0.7f, 0, 1f);
						Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
					}
				});
				break;
			//shields
			case SHIELD_BASH:
				withAdjacentTarget(hero, s, rank, new TargetAction() {
					public void run(Char t) {
						int block = hero.belongings.weapon() == null ? 0 : hero.belongings.weapon().defenseFactor(hero);
						hero.attack(t, 0.7f + 0.2f * rank, block, 1f);
						if (t.isAlive()) Buff.prolong(t, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class, rank == 1 ? 1 : 2);
						Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH);
					}
				});
				break;
			case BULWARK:
				Buff.affect(hero, Bulwark.class, 2f + rank);
				hero.sprite.showStatus(CharSprite.POSITIVE, s.title());
				Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY);
				finish(hero, s, rank, 1f);
				break;
			case AEGIS:
				Buff.affect(hero, Aegis.class, 5f);
				hero.sprite.showStatus(CharSprite.POSITIVE, s.title());
				Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
				finish(hero, s, rank, 1f);
				break;
			//flexible
			case LASH_PULL:
				withVisibleTarget(hero, s, rank, new TargetAction() {
					public void run(Char t) {
						if (Dungeon.level.distance(hero.pos, t.pos) > 2 + rank) {
							GLog.w(Messages.get(SkillEffects.class, "too_far"));
							return;
						}
						pullTo(hero, t);
						hero.attack(t, 0.8f, 0, 1f);
						if (t.isAlive()) {
							Buff.prolong(t, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple.class, 2f);
							if (Proficiency.rank(hero.heroClass, Skill.ENTANGLE) > 0) Buff.prolong(t, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots.class, 2f);
						}
						Sample.INSTANCE.play(Assets.Sounds.HIT);
					}
				}, hero.attackDelay());
				break;
			case CHAIN_SPIN: {
				for (Char t : visibleEnemies(hero, 2)) hero.attack(t, 0.55f + 0.15f * rank, 0, 1f);
				Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH);
				finish(hero, s, rank, hero.attackDelay());
				break;
			}
			case GRAND_LASSO: {
				for (Char t : visibleEnemies(hero, 8)) {
					if (t.properties().contains(Char.Property.IMMOVABLE)) continue;
					pullTo(hero, t);
					if (t.isAlive()) Buff.prolong(t, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots.class, 3f);
				}
				for (Char t : adjacentEnemies(hero)) hero.attack(t, 1f, 0, 1f);
				Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
				finish(hero, s, rank, hero.attackDelay());
				break;
			}
			//ranged
			case PIERCING_SHOT:
				withVisibleTarget(hero, s, rank, new TargetAction() {
					public void run(Char t) {
						for (Char ch : lineTargets(hero, t.pos, 99, 99)) rangedAttack(hero, ch, 0.8f + 0.2f * rank);
						Sample.INSTANCE.play(Assets.Sounds.HIT_ARROW);
					}
				}, 1f);
				break;
			case VOLLEY: {
				ArrayList<Char> targets = visibleEnemies(hero, 99);
				java.util.Collections.shuffle(targets);
				int n = 2 + rank;
				for (int i = 0; i < n && !targets.isEmpty(); i++) rangedAttack(hero, targets.get(i % targets.size()), 0.6f);
				Sample.INSTANCE.play(Assets.Sounds.HIT_ARROW);
				finish(hero, s, rank, 1f);
				break;
			}
			case RAIN_OF_ARROWS:
				GameScene.selectCell(new CellSelector.Listener() {
					@Override
					public void onSelect(Integer cell) {
						if (cell == null) return;
						if (!hero.fieldOfView[cell]) { GLog.w(Messages.get(SkillEffects.class, "need_visible_cell")); return; }
						RainOfArrows r = Buff.affect(hero, RainOfArrows.class);
						r.center = cell; r.left = 4;
						Sample.INSTANCE.play(Assets.Sounds.HIT_ARROW);
						finish(hero, s, rank, 1f);
					}
					@Override
					public String prompt() { return Messages.get(SkillEffects.class, "choose_area"); }
				});
				break;
			//wands
			case OVERCHARGE:
				Buff.affect(hero, Overcharge.class).rank = rank;
				hero.sprite.showStatus(CharSprite.POSITIVE, s.title());
				Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
				finish(hero, s, rank, 1f);
				break;
			case ELEMENTAL_INFUSION:
				Buff.affect(hero, Infusion.class, 8f).rank = rank;
				hero.sprite.showStatus(CharSprite.POSITIVE, s.title());
				Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
				finish(hero, s, rank, 1f);
				break;
			case SPELL_FUSION:
				Buff.affect(hero, FusionReady.class);
				hero.sprite.showStatus(CharSprite.POSITIVE, s.title());
				Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
				finish(hero, s, rank, 1f);
				break;
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
			markSkillStrike(hero);
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
				markSkillStrike(hero);
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
				markSkillStrike(hero);
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


	//enemies along a straight line from the hero toward a cell (passing through), up to a range and count
	private static ArrayList<Char> lineTargets(Hero hero, int cell, int range, int max) {
		ArrayList<Char> list = new ArrayList<>();
		com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica b = new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(
				hero.pos, cell, com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.STOP_SOLID);
		for (int c : b.subPath(1, b.dist)) {
			if (Dungeon.level.distance(hero.pos, c) > range) break;
			Char ch = Actor.findChar(c);
			if (ch != null && ch.alignment == Char.Alignment.ENEMY) list.add(ch);
			if (list.size() >= max) break;
		}
		return list;
	}

	//the tile in front of the hero toward a target and the two tiles beside it
	private static ArrayList<Char> arcTargets(Hero hero, int front) {
		ArrayList<Char> list = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8) {
			int c = hero.pos + i;
			if (c == front || (Dungeon.level.adjacent(c, front) && Dungeon.level.adjacent(c, hero.pos))) {
				Char ch = Actor.findChar(c);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY) list.add(ch);
			}
		}
		return list;
	}

	//moves an enemy to the free tile next to the hero closest to it
	private static void pullTo(Hero hero, Char t) {
		if (Dungeon.level.adjacent(hero.pos, t.pos) || t.properties().contains(Char.Property.IMMOVABLE)) return;
		int best = -1;
		for (int i : PathFinder.NEIGHBOURS8) {
			int c = hero.pos + i;
			if (!Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
			if (best == -1 || Dungeon.level.distance(c, t.pos) < Dungeon.level.distance(best, t.pos)) best = c;
		}
		if (best == -1) return;
		Actor.add(new com.shatteredpixel.shatteredpixeldungeon.effects.Pushing(t, t.pos, best));
		t.pos = best;
		Dungeon.level.occupyCell(t);
	}

	// ---------------- ranged ----------------

	//the hero's ranged weapon: an equipped crossbow or spirit bow, else the first thrown weapon carried
	public static com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon rangedWeapon(Hero hero) {
		com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon w = hero.belongings.weapon();
		if (Proficiency.familyOf(w) == Proficiency.Family.RANGED) return w;
		for (Item i : hero.belongings.backpack) {
			if (i instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon) {
				return (com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon) i;
			}
		}
		return null;
	}

	//a ranged skill hit that uses the thrown weapon's damage without using it up
	private static void rangedAttack(Hero hero, Char t, float mult) {
		com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon w = rangedWeapon(hero);
		boolean thrown = w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
		if (thrown) hero.belongings.thrownWeapon = w;
		hero.attack(t, mult, 0, 1f);
		if (thrown) hero.belongings.thrownWeapon = null;
		if (t.sprite != null) t.sprite.flash();
	}

	public static class RainOfArrows extends Buff {
		{ type = buffType.POSITIVE; }
		public int center = -1, left = 0;
		@Override
		public boolean act() {
			if (center >= 0 && left > 0) {
				Hero hero = (Hero) target;
				for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m : Dungeon.level.mobs.toArray(new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob[0])) {
					if (m.alignment == Char.Alignment.ENEMY && Dungeon.level.distance(m.pos, center) <= 2) rangedAttack(hero, m, 0.8f);
				}
				left--;
			}
			if (left <= 0) detach();
			spend(TICK);
			return true;
		}
		@Override public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("center", center); b.put("left", left); }
		@Override public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); center = b.getInt("center"); left = b.getInt("left"); }
	}

	// ---------------- polearms ----------------

	public static class ReapingStorm extends FlavourBuff {
		{ type = buffType.POSITIVE; announced = true; }
		@Override public int icon() { return BuffIndicator.WEAPON; }
		@Override public void tintIcon(com.watabou.noosa.Image icon) { icon.hardlight(0.6f, 1f, 0.6f); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//hold the line / reaping storm: an enemy stepping into the hero's reach takes a free hit
	public static void onMobMoved(Char mob, int from) {
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive() || mob.alignment != Char.Alignment.ENEMY) return;
		int reach = hero.belongings.weapon() == null ? 1 : hero.belongings.weapon().reachFactor(hero);
		boolean wasIn = Dungeon.level.distance(from, hero.pos) <= reach;
		boolean nowIn = Dungeon.level.distance(mob.pos, hero.pos) <= reach;
		if (wasIn || !nowIn) return;
		if (hero.buff(ReapingStorm.class) != null && canUseWith(hero, Skill.REAPING_STORM)) {
			hero.attack(mob, 1f, 0, 1f);
			return;
		}
		int hl = Proficiency.activeRank(hero, Skill.HOLD_THE_LINE);
		if (hl > 0 && Dungeon.level.adjacent(mob.pos, hero.pos)) hero.attack(mob, 0.2f + 0.2f * hl, 0, 1f);
	}

	// ---------------- staves ----------------

	public static class Ward extends FlavourBuff {
		{ type = buffType.POSITIVE; }
	}

	public static class ArcaneOverload extends FlavourBuff {
		{ type = buffType.POSITIVE; announced = true; }
		@Override public int icon() { return BuffIndicator.RECHARGING; }
		@Override public void tintIcon(com.watabou.noosa.Image icon) { icon.hardlight(0.7f, 0.4f, 1f); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//a burst of the staff's magic: the imbued wand's effect if it has one, else a magic missile
	private static void arcaneBurst(Hero hero, Char t) {
		com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon w = hero.belongings.weapon();
		if (w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff
				&& ((com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff) w).wandInside() != null) {
			((com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff) w).wandInside().onZap(
					new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos, t.pos, com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.MAGIC_BOLT));
		} else {
			int dmg = 2 + Dungeon.scalingDepth() / 2 + com.watabou.utils.Random.Int(1 + Dungeon.scalingDepth() / 2);
			t.damage(dmg, new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile());
			if (t.sprite != null) t.sprite.burst(0xFF99CCFF, 6);
		}
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
	}

	// ---------------- fist ----------------

	public static class ComboPoints extends Buff {
		{ type = buffType.POSITIVE; }
		public int points = 0, idle = 0;
		@Override public int icon() { return BuffIndicator.COMBO; }
		@Override public String iconTextDisplay() { return Integer.toString(points); }
		@Override public String desc() { return Messages.get(this, "desc", points); }
		@Override
		public boolean act() {
			if (++idle >= 3) detach();
			spend(TICK);
			return true;
		}
		@Override public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("points", points); b.put("idle", idle); }
		@Override public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); points = b.getInt("points"); idle = b.getInt("idle"); }
	}

	public static class FootworkReady extends Buff {
		{ type = buffType.POSITIVE; }
	}

	//called when the hero dodges an attack
	public static void onHeroDodge(Hero hero) {
		if (Proficiency.activeRank(hero, Skill.FOOTWORK) > 0) Buff.affect(hero, FootworkReady.class);
	}

	public static float evasionFactor(Hero hero) {
		int fw = Proficiency.activeRank(hero, Skill.FOOTWORK);
		return 1f + 0.05f * fw;
	}

	// ---------------- shields ----------------

	public static class Bulwark extends FlavourBuff {
		{ type = buffType.POSITIVE; announced = true; }
		@Override public int icon() { return BuffIndicator.ARMOR; }
		@Override public void tintIcon(com.watabou.noosa.Image icon) { icon.hardlight(0.5f, 0.7f, 1f); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	public static class Aegis extends FlavourBuff {
		{ type = buffType.POSITIVE; announced = true; }
		public int absorbed = 0;
		@Override public int icon() { return BuffIndicator.ARMOR; }
		@Override public void tintIcon(com.watabou.noosa.Image icon) { icon.hardlight(1f, 0.85f, 0.3f); }
		@Override public String desc() { return Messages.get(this, "desc", absorbed, dispTurns()); }
		@Override
		public void detach() {
			Hero hero = (Hero) target;
			if (absorbed > 0 && hero.isAlive()) {
				for (int i : PathFinder.NEIGHBOURS8) {
					Char ch = Actor.findChar(hero.pos + i);
					if (ch != null && ch.alignment == Char.Alignment.ENEMY) ch.damage(Math.max(1, absorbed / 2), hero);
				}
				if (hero.sprite != null) hero.sprite.burst(0xFFFFDD55, 10);
				Sample.INSTANCE.play(Assets.Sounds.BLAST);
			}
			super.detach();
		}
		@Override public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("absorbed", absorbed); }
		@Override public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); absorbed = b.getInt("absorbed"); }
	}

	//aegis: physical damage to the hero is absorbed (returns the damage still taken)
	public static int onHeroDamaged(Hero hero, int dmg, Object src) {
		Aegis a = hero.buff(Aegis.class);
		if (a != null && src instanceof Char && canUseWith(hero, Skill.AEGIS)) {
			a.absorbed += dmg;
			if (hero.sprite != null) hero.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(SkillEffects.class, "blocked"));
			return 0;
		}
		return dmg;
	}

	//thorns: reflect part of the shield's block back at melee attackers
	public static void onHeroHitBy(Hero hero, Char attacker) {
		int th = Proficiency.activeRank(hero, Skill.THORNS);
		if (th == 0 || hero.belongings.weapon() == null || !Dungeon.level.adjacent(hero.pos, attacker.pos)) return;
		int block = hero.belongings.weapon().defenseFactor(hero);
		float pct = 0.1f * th;
		if (hero.buff(Bulwark.class) != null && Proficiency.rank(hero.heroClass, Skill.STEADFAST) > 0) pct *= 2;
		int dmg = Math.round(block * pct);
		if (dmg > 0 && attacker.isAlive()) attacker.damage(dmg, hero);
	}

	//extra block: bulwark doubles shield block, steadfast and deflection add to it
	public static int bonusBlock(Hero hero, int weaponBlock) {
		int extra = 0;
		if (hero.buff(Bulwark.class) != null && canUseWith(hero, Skill.BULWARK)) extra += weaponBlock;
		float mult = 1f;
		com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon w = hero.belongings.weapon();
		if (w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon
				&& ((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon) w).material() != null) {
			mult = ((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon) w).material().armorMult();
		}
		extra += Math.round(Proficiency.activeRank(hero, Skill.STEADFAST) * mult);
		extra += Math.round(Proficiency.activeRank(hero, Skill.DEFLECTION) * mult);
		return extra;
	}

	// ---------------- wands ----------------

	public static class Overcharge extends Buff {
		{ type = buffType.POSITIVE; announced = true; }
		public int rank = 1;
		@Override public int icon() { return BuffIndicator.RECHARGING; }
		@Override public void tintIcon(com.watabou.noosa.Image icon) { icon.hardlight(1f, 1f, 0.3f); }
		@Override public String desc() { return Messages.get(this, "desc", 125 + 25 * rank); }
		@Override public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("rank", rank); }
		@Override public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); rank = b.getInt("rank"); }
	}

	public static class Infusion extends FlavourBuff {
		{ type = buffType.POSITIVE; announced = true; }
		public int rank = 1;
		@Override public int icon() { return BuffIndicator.WEAPON; }
		@Override public void tintIcon(com.watabou.noosa.Image icon) { icon.hardlight(1f, 0.5f, 0.9f); }
		@Override public String desc() { return Messages.get(this, "desc", 10 + 10 * rank, dispTurns()); }
		@Override public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("rank", rank); }
		@Override public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); rank = b.getInt("rank"); }
	}

	public static class FusionReady extends Buff {
		{ type = buffType.POSITIVE; announced = true; }
		@Override public int icon() { return BuffIndicator.UPGRADE; }
		@Override public void tintIcon(com.watabou.noosa.Image icon) { icon.hardlight(1f, 0.4f, 0.8f); }
		@Override public String desc() { return Messages.get(this, "desc"); }
	}

	//which wand last struck this target, for resonance
	public static class ResonanceMark extends FlavourBuff {
		public String wand = "";
		@Override public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("wand", wand); }
		@Override public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); wand = b.getString("wand"); }
	}

	public static boolean hasWand(Hero hero) {
		if (hero.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.class) != null) return true;
		com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon w = hero.belongings.weapon();
		return w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff
				&& ((com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff) w).wandInside() != null;
	}

	//wand damage: proficiency, overcharge and resonance
	public static int modifyWandDamage(Char target, int dmg, com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand) {
		Hero hero = Dungeon.hero;
		if (hero == null || target == hero) return dmg;
		float mult = 1f + 0.03f * Proficiency.level(hero.heroClass, Proficiency.Family.WANDS);
		Overcharge oc = hero.buff(Overcharge.class);
		if (oc != null) mult += 0.25f + 0.25f * oc.rank;
		int res = Proficiency.rank(hero.heroClass, Skill.RESONANCE);
		if (res > 0) {
			ResonanceMark m = target.buff(ResonanceMark.class);
			if (m != null && !m.wand.equals(wand.getClass().getName())) mult += 0.05f + 0.1f * res;
			Buff.affect(target, ResonanceMark.class, 3f).wand = wand.getClass().getName();
		}
		return Math.round(dmg * mult);
	}

	//after a wand is used: overcharge and arcane overload refund the charge; the last wand used is remembered
	public static void afterWandUsed(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand) {
		Hero hero = Dungeon.hero;
		if (hero == null) return;
		boolean refund = false;
		Overcharge oc = hero.buff(Overcharge.class);
		if (oc != null) { oc.detach(); refund = true; }
		if (hero.buff(ArcaneOverload.class) != null) refund = true;
		if (refund) wand.curCharges = Math.min(wand.maxCharges, wand.curCharges + 1);
		Buff.affect(hero, LastWand.class).wand = wand.getClass().getName();
		Item.updateQuickslot();
	}

	public static class LastWand extends Buff {
		public String wand = "";
		@Override public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("wand", wand); }
		@Override public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); wand = b.getString("wand"); }
	}

	//spell fusion: right after a zap, a second wand fires at the same spot
	public static void afterZap(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand,
	                            com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica shot) {
		Hero hero = Dungeon.hero;
		if (hero == null) return;
		FusionReady f = hero.buff(FusionReady.class);
		if (f == null) return;
		f.detach();
		com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand other = null;
		for (Item i : hero.belongings.backpack) {
			if (i instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand && i != wand && i.getClass() != wand.getClass()) {
				if (other == null || i.level() > other.level()) other = (com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand) i;
			}
		}
		if (other == null) { GLog.w(Messages.get(SkillEffects.class, "no_fusion")); return; }
		GLog.p(Messages.get(SkillEffects.class, "fused", wand.name(), other.name()));
		other.onZap(shot);
	}

	//attunement: extra max charges on all wands
	public static int bonusCharges() {
		Hero hero = Dungeon.hero;
		return hero == null ? 0 : Proficiency.rank(hero.heroClass, Skill.ATTUNEMENT);
	}

	//wand recharge speed: wand proficiency +2% per level, channeling +10/20/30% with a staff
	public static float rechargeFactor(Char owner) {
		if (!(owner instanceof Hero)) return 1f;
		Hero hero = (Hero) owner;
		float f = 1f + 0.02f * Proficiency.level(hero.heroClass, Proficiency.Family.WANDS);
		f += 0.1f * Proficiency.activeRank(hero, Skill.CHANNELING);
		return f;
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
		//lethal haste (duelist): a killing blow from a skill grants haste
		if (skillStrike(hero) && hero.heroClass == HeroClass.DUELIST && hero.hasTalent(Talent.LETHAL_HASTE)
				&& hero.buff(Talent.LethalHasteCooldown.class) == null) {
			Buff.affect(hero, Talent.LethalHasteCooldown.class, 100f);
			Buff.affect(hero, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreaterHaste.class).set(2 + 2 * hero.pointsInTalent(Talent.LETHAL_HASTE));
		}
		GameScene.updateSkillBar();
	}

	//MOD: marks hits as coming from a skill for this turn (duelist talents)
	public static class SkillStrike extends FlavourBuff {}

	private static void markSkillStrike(Hero hero) {
		Buff.affect(hero, SkillStrike.class, 0f);
	}

	public static boolean skillStrike(Hero hero) {
		return hero.buff(SkillStrike.class) != null;
	}

	public static class LastSkillFamily extends Buff {
		public String family = "";
		@Override public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("family", family); }
		@Override public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); family = b.getString("family"); }
	}

	private static void finish(Hero hero, Skill s, int rank, float time) {
		int cd = s.cooldown(rank);
		//varied charge (champion): switching skill families shortens the cooldown by 15/30/45%
		LastSkillFamily last = Buff.affect(hero, LastSkillFamily.class);
		if (hero.hasTalent(Talent.VARIED_CHARGE) && !last.family.isEmpty() && !last.family.equals(s.family.name())) {
			cd = Math.round(cd * (1f - 0.15f * hero.pointsInTalent(Talent.VARIED_CHARGE)));
		}
		last.family = s.family.name();
		//aggressive barrier: shielding when using a skill at half health or less
		if (hero.hasTalent(Talent.AGGRESSIVE_BARRIER) && hero.HP <= hero.HT / 2) {
			int shield = 1 + 2 * hero.pointsInTalent(Talent.AGGRESSIVE_BARRIER);
			Buff.affect(hero, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier.class).setShield(shield);
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.SHIELDING);
		}
		//precise assault: the next attack after a skill is far more accurate
		if (hero.hasTalent(Talent.PRECISE_ASSAULT)) {
			Buff.affect(hero, Talent.PreciseAssaultTracker.class, hero.cooldown() + time + 1f);
		}
		//combined energy (monk): skills give monk energy
		if (hero.hasTalent(Talent.COMBINED_ENERGY)) {
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy me = hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy.class);
			if (me != null) me.energy = Math.min(me.energyCap(), me.energy + hero.pointsInTalent(Talent.COMBINED_ENERGY));
		}
		Cooldowns.start(hero, s, cd);
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
		if (!canUseWith((Hero) defender, Skill.RIPOSTE_STANCE)) return false;
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

	//wild swing and steady aim
	public static float accuracyFactor2(Hero hero) {
		float f = 1f;
		f *= 1f + 0.1f * Proficiency.activeRank(hero, Skill.WILD_SWING);
		f *= 1f + 0.05f * Proficiency.activeRank(hero, Skill.STEADY_AIM);
		return f;
	}

	//momentum: each hit in a row on the same target adds +4/6/8% damage, up to 5 stacks
	public static int onHeroHit(Hero hero, Char enemy, int damage) {
		//combined lethality (champion): skill hits finish off non-boss enemies below 5/10/15% health
		if (skillStrike(hero) && hero.hasTalent(Talent.COMBINED_LETHALITY)
				&& !enemy.properties().contains(Char.Property.BOSS) && !enemy.properties().contains(Char.Property.MINIBOSS)
				&& enemy.HP - damage <= enemy.HT * 0.05f * hero.pointsInTalent(Talent.COMBINED_LETHALITY)) {
			damage = Math.max(damage, enemy.HP);
		}
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
		if (hero.buff(Rampage.class) != null && canUseWith(hero, Skill.RAMPAGE)) {
			mult += 0.4f;
			int heal = Math.max(1, Math.round(damage * mult * 0.1f));
			heal = Math.min(heal, hero.HT - hero.HP);
			if (heal > 0) {
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.HEALING);
			}
		}
		//long reach / steady aim: bonus damage at range
		int dist = Dungeon.level.distance(hero.pos, enemy.pos);
		int lr = Proficiency.activeRank(hero, Skill.LONG_REACH);
		if (lr > 0 && dist >= 2) mult += 0.1f * lr;
		int sa = Proficiency.activeRank(hero, Skill.STEADY_AIM);
		if (sa > 0 && dist >= 4) mult += 0.1f * sa;
		//combo counter: +2/3/4% per point
		int cc0 = Proficiency.activeRank(hero, Skill.COMBO_COUNTER);
		if (cc0 > 0) {
			ComboPoints cp = Buff.affect(hero, ComboPoints.class);
			mult += (0.01f + 0.01f * cc0) * cp.points;
			cp.points = Math.min(10, cp.points + 1);
			cp.idle = 0;
		}
		//footwork: next hit after a dodge
		FootworkReady fr = hero.buff(FootworkReady.class);
		if (fr != null) { mult += 0.3f; fr.detach(); }
		//arcane overload: hits carry extra magic
		if (hero.buff(ArcaneOverload.class) != null && canUseWith(hero, Skill.ARCANE_OVERLOAD)) mult += 0.5f;
		//elemental infusion
		Infusion inf = hero.buff(Infusion.class);
		if (inf != null) mult += 0.1f + 0.1f * inf.rank;
		damage = Math.round(damage * mult);

		//entangle: chance to root
		int en = Proficiency.activeRank(hero, Skill.ENTANGLE);
		if (en > 0 && enemy.isAlive() && com.watabou.utils.Random.Float() < 0.05f + 0.05f * en) {
			Buff.prolong(enemy, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots.class, 2f);
		}

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

		public static void reduceAll(Hero hero, int turns) {
			Cooldowns c = hero.buff(Cooldowns.class);
			if (c == null) return;
			for (Skill s : new ArrayList<>(c.left.keySet())) reduce(hero, s, turns);
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
			//weapon recharging (duelist): while recharging, cooldowns sometimes tick twice
			int tick = 1;
			if (target instanceof Hero && ((Hero) target).heroClass == HeroClass.DUELIST
					&& ((Hero) target).hasTalent(Talent.WEAPON_RECHARGING)
					&& (target.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging.class) != null
					|| target.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge.class) != null)
					&& com.watabou.utils.Random.Float() < 0.5f * ((Hero) target).pointsInTalent(Talent.WEAPON_RECHARGING)) {
				tick = 2;
			}
			for (Skill s : new ArrayList<>(left.keySet())) {
				int v = left.get(s) - tick;
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
