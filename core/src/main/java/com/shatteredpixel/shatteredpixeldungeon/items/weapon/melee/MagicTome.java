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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Proficiency;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge;
import com.shatteredpixel.shatteredpixeldungeon.items.Material;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTomeSpells;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//MOD: the magic tome. Held open in the hand like a weapon (a weak bash in melee), it casts spells
//chosen from a list, each with its own cooldown instead of charges. The Mage starts with one;
//other heroes need magic proficiency 1 to use it. Spare tomes can be deciphered.
public class MagicTome extends MeleeWeapon {

	public static final String AC_CAST     = "CAST";
	public static final String AC_DECIPHER = "DECIPHER";

	public static final int MAX_RANK = 3;

	public enum Spell {
		MAGIC_MISSILES  (3,  true),
		FIREBALL        (12, true),
		RAY_OF_FROST    (18, true),
		CHAIN_LIGHTNING (10, true),
		ARCANE_WARD     (25, false),
		BLINK           (20, true),
		//battlemage only: one of these two is chosen
		DISINT_RAY      (20, true),
		DISINT_AURA     (30, false);

		public final int cooldown;
		public final boolean targeted;
		Spell(int cooldown, boolean targeted){
			this.cooldown = cooldown;
			this.targeted = targeted;
		}

		public String title(){
			return Messages.get(MagicTome.class, "spell_" + name().toLowerCase());
		}
		public boolean battlemage(){
			return this == DISINT_RAY || this == DISINT_AURA;
		}
	}

	//spells that deciphering can teach (the battlemage spells come from the subclass instead)
	public static final Spell[] LEARNABLE = { Spell.CHAIN_LIGHTNING, Spell.ARCANE_WARD, Spell.BLINK };

	public int[] ranks = new int[Spell.values().length];
	public float[] cooldowns = new float[Spell.values().length];

	{
		image = ItemSpriteSheet.MAGIC_TOME; //placeholder art until its own sprite is made
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.1f;

		tier = 1;
		defaultAction = AC_CAST;

		ranks[Spell.MAGIC_MISSILES.ordinal()] = 1;
		ranks[Spell.FIREBALL.ordinal()] = 1;
		ranks[Spell.RAY_OF_FROST.ordinal()] = 1;
	}

	//paper and leather: no metal, never wears out
	@Override
	public Material.Kind materialKind() {
		return null;
	}

	@Override
	public int baseMin(int lvl) {
		return 1 + lvl;
	}

	@Override
	public int baseMax(int lvl) {
		return 4 + lvl;
	}

	@Override
	public String defaultAction() {
		return AC_CAST;
	}

	// ---- who can use it ----

	public static boolean canUse(Hero hero){
		return hero.heroClass == HeroClass.MAGE
				|| Proficiency.level(hero.heroClass, Proficiency.Family.WANDS) >= 1;
	}

	@Override
	public boolean doEquip(Hero hero) {
		if (!canUse(hero)){
			GLog.w(Messages.get(MagicTome.class, "cant_read"));
			return false;
		}
		return super.doEquip(hero);
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero)) actions.add(AC_CAST);
		else if (hero.belongings.backpack.contains(this)) actions.add(AC_DECIPHER);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_CAST)) return Messages.get(MagicTome.class, "ac_cast");
		if (action.equals(AC_DECIPHER)) return Messages.get(MagicTome.class, "ac_decipher");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_CAST)) {
			if (!isEquipped(hero)) {
				GLog.i(Messages.get(MagicTome.class, "need_equip"));
			} else if (hero.subClass == HeroSubClass.BATTLEMAGE && !battlemageChosen()) {
				chooseBattlemageSpell(hero);
			} else {
				GameScene.show(new WndTomeSpells(this, hero));
			}
		} else if (action.equals(AC_DECIPHER)) {
			decipher(hero);
		}
	}

	// ---- battlemage ----

	public boolean battlemageChosen(){
		return ranks[Spell.DISINT_RAY.ordinal()] > 0 || ranks[Spell.DISINT_AURA.ordinal()] > 0;
	}

	public void chooseBattlemageSpell(Hero hero){
		GameScene.show(new WndOptions(Messages.titleCase(name()), Messages.get(MagicTome.class, "bm_choose"),
				Spell.DISINT_RAY.title(), Spell.DISINT_AURA.title()) {
			@Override
			protected void onSelect(int index) {
				ranks[(index == 0 ? Spell.DISINT_RAY : Spell.DISINT_AURA).ordinal()] = 1;
				GLog.p(Messages.get(MagicTome.class, "learned", (index == 0 ? Spell.DISINT_RAY : Spell.DISINT_AURA).title()));
				GameScene.show(new WndTomeSpells(MagicTome.this, hero));
			}
		});
	}

	// ---- deciphering spare tomes ----

	public void decipher(Hero hero){
		if (hero.heroClass == HeroClass.MAGE) {
			MagicTome main = hero.belongings.weapon() instanceof MagicTome ? (MagicTome) hero.belongings.weapon() : null;
			if (main == null || main == this) {
				GLog.w(Messages.get(MagicTome.class, "decipher_need_main"));
				return;
			}
			ArrayList<Spell> unlearned = new ArrayList<>();
			for (Spell s : LEARNABLE) if (main.ranks[s.ordinal()] == 0) unlearned.add(s);
			ArrayList<Spell> upgradable = new ArrayList<>();
			for (Spell s : Spell.values()) if (main.ranks[s.ordinal()] > 0 && main.ranks[s.ordinal()] < MAX_RANK) upgradable.add(s);
			if (unlearned.isEmpty() && upgradable.isEmpty()) {
				GLog.w(Messages.get(MagicTome.class, "decipher_nothing"));
				return;
			}
			detach(hero.belongings.backpack);
			if (!unlearned.isEmpty() && (upgradable.isEmpty() || Random.Int(2) == 0)) {
				Spell s = Random.element(unlearned);
				main.ranks[s.ordinal()] = 1;
				GLog.p(Messages.get(MagicTome.class, "learned", s.title()));
			} else {
				Spell s = Random.element(upgradable);
				main.ranks[s.ordinal()]++;
				GLog.p(Messages.get(MagicTome.class, "improved", s.title(), main.ranks[s.ordinal()]));
			}
			hero.sprite.operate(hero.pos);
			Sample.INSTANCE.play(Assets.Sounds.READ);
			hero.spendAndNext(5f);
		} else {
			detach(hero.belongings.backpack);
			Proficiency.grantLevel(hero.heroClass, Proficiency.Family.WANDS);
			GLog.p(Messages.get(MagicTome.class, "deciphered", Proficiency.level(hero.heroClass, Proficiency.Family.WANDS)));
			hero.sprite.operate(hero.pos);
			Sample.INSTANCE.play(Assets.Sounds.READ);
			hero.spendAndNext(10f);
		}
		updateQuickslot();
	}

	// ---- spell numbers ----

	public int rank(Spell s){
		return ranks[s.ordinal()];
	}

	//spell power: the tome's level plus extra spell ranks
	public int power(Spell s){
		return Math.max(0, buffedLvl()) + Math.max(0, rank(s) - 1);
	}

	public float cooldownFor(Spell s){
		return s.cooldown * (1f - 0.1f * (Math.max(1, rank(s)) - 1));
	}

	public boolean ready(Spell s){
		return rank(s) > 0 && cooldowns[s.ordinal()] <= 0;
	}

	public int cooldownLeft(Spell s){
		return (int) Math.ceil(Math.max(0, cooldowns[s.ordinal()]));
	}

	public void tick(float amount){
		for (int i = 0; i < cooldowns.length; i++) {
			if (cooldowns[i] > 0) cooldowns[i] = Math.max(0, cooldowns[i] - amount);
		}
		updateQuickslot();
	}

	public String spellDesc(Spell s){
		int p = power(s);
		switch (s) {
			case MAGIC_MISSILES:  return Messages.get(MagicTome.class, "desc_magic_missiles", 2 + p, 4 + 2*p);
			case FIREBALL:        return Messages.get(MagicTome.class, "desc_fireball", 6 + 2*p, 12 + 4*p, 30 + 10*(rank(s)-1));
			case RAY_OF_FROST:    return Messages.get(MagicTome.class, "desc_ray_of_frost", 3 + p, 6 + 2*p, 15 + 5*(rank(s)-1));
			case CHAIN_LIGHTNING: return Messages.get(MagicTome.class, "desc_chain_lightning", 4 + p, 9 + 2*p, 1 + rank(s));
			case ARCANE_WARD:     return Messages.get(MagicTome.class, "desc_arcane_ward", 6 + 3*p);
			case BLINK:           return Messages.get(MagicTome.class, "desc_blink", 3 + rank(s));
			case DISINT_RAY:      return Messages.get(MagicTome.class, "desc_disint_ray", 6 + 2*p, 14 + 5*p);
			case DISINT_AURA:     return Messages.get(MagicTome.class, "desc_disint_aura", 3 + rank(s));
		}
		return "";
	}

	// ---- casting ----

	public void cast(Hero hero, Spell s){
		if (!ready(s) || !isEquipped(hero)) return;
		if (!s.targeted) {
			resolve(hero, s, hero.pos);
			return;
		}
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				if (cell != null) resolve(hero, s, cell);
			}
			@Override
			public String prompt() {
				return Messages.get(MagicTome.class, "prompt", s.title());
			}
		});
	}

	private Char visibleEnemyAt(int cell){
		Char ch = Actor.findChar(cell);
		if (ch == null || ch == Dungeon.hero || !Dungeon.level.heroFOV[cell]) return null;
		return ch;
	}

	private void startCooldown(Spell s){
		boolean refund = com.shatteredpixel.shatteredpixeldungeon.actors.hero.SkillEffects.afterTomeUsed(source(s));
		cooldowns[s.ordinal()] = refund ? 0 : cooldownFor(s);
		updateQuickslot();
	}

	//the wand each spell stands in for, for magic skills that care which wand was used
	private static String source(Spell s){
		String w;
		switch (s) {
			case FIREBALL:        w = "WandOfFireblast"; break;
			case RAY_OF_FROST:    w = "WandOfFrost"; break;
			case CHAIN_LIGHTNING: w = "WandOfLightning"; break;
			case DISINT_RAY: case DISINT_AURA: w = "WandOfDisintegration"; break;
			default:              w = "WandOfMagicMissile";
		}
		return "com.shatteredpixel.shatteredpixeldungeon.items.wands." + w;
	}

	private static int amp(Char target, int dmg, Spell s){
		return com.shatteredpixel.shatteredpixeldungeon.actors.hero.SkillEffects.modifyTomeDamage(target, dmg, source(s));
	}

	private void resolve(Hero hero, Spell s, int cell){
		switch (s) {
			case MAGIC_MISSILES: {
				Char target = visibleEnemyAt(cell);
				if (target == null) { GLog.w(Messages.get(MagicTome.class, "no_target")); return; }
				startCooldown(s);
				hero.sprite.zap(cell);
				hero.busy();
				fireMissile(hero, target, 3, power(s));
				return;
			}
			case FIREBALL: {
				if (cell == hero.pos) { GLog.w(Messages.get(MagicTome.class, "no_target")); return; }
				startCooldown(s);
				final Ballistica path = new Ballistica(hero.pos, cell, Ballistica.PROJECTILE);
				final int impact = path.collisionPos;
				final int p = power(s);
				final int burnChance = 30 + 10*(rank(s)-1);
				hero.sprite.zap(cell);
				hero.busy();
				Sample.INSTANCE.play(Assets.Sounds.ZAP);
				MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.BIG_FIRE, hero.sprite, impact, new Callback() {
					@Override
					public void call() {
						explode(hero, impact, p, burnChance);
						hero.spendAndNext(1f);
					}
				});
				return;
			}
			case RAY_OF_FROST: {
				Char target = visibleEnemyAt(cell);
				if (target == null) { GLog.w(Messages.get(MagicTome.class, "no_target")); return; }
				startCooldown(s);
				FrostChannel fc = Buff.affect(hero, FrostChannel.class);
				fc.target = target.id();
				fc.pulses = 4;
				fc.power = power(s);
				fc.freeze = 15 + 5*(rank(s)-1);
				fc.pulse(hero);
				hero.spendAndNext(1f);
				return;
			}
			case CHAIN_LIGHTNING: {
				Char target = visibleEnemyAt(cell);
				if (target == null) { GLog.w(Messages.get(MagicTome.class, "no_target")); return; }
				startCooldown(s);
				chainLightning(hero, target, power(s), 1 + rank(s));
				hero.sprite.zap(cell);
				hero.spendAndNext(1f);
				return;
			}
			case ARCANE_WARD: {
				startCooldown(s);
				Buff.affect(hero, Barrier.class).setShield(6 + 3*power(s));
				hero.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 8);
				hero.sprite.operate(hero.pos);
				Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
				hero.spendAndNext(1f);
				return;
			}
			case BLINK: {
				int range = 3 + rank(s);
				if (!Dungeon.level.heroFOV[cell] || !Dungeon.level.passable[cell] || Actor.findChar(cell) != null
						|| Dungeon.level.distance(hero.pos, cell) > range) {
					GLog.w(Messages.get(MagicTome.class, "blink_bad"));
					return;
				}
				startCooldown(s);
				ScrollOfTeleportation.appear(hero, cell);
				Dungeon.level.occupyCell(hero);
				Dungeon.observe();
				GameScene.updateFog();
				Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
				hero.spendAndNext(1f);
				return;
			}
			case DISINT_RAY: {
				if (cell == hero.pos) { GLog.w(Messages.get(MagicTome.class, "no_target")); return; }
				startCooldown(s);
				disintRay(hero, cell, power(s));
				hero.spendAndNext(1f);
				return;
			}
			case DISINT_AURA: {
				startCooldown(s);
				Buff.affect(hero, DisintAura.class, 10f).power = power(s);
				hero.sprite.emitter().burst(PurpleParticle.BURST, 15);
				Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
				hero.spendAndNext(1f);
				return;
			}
		}
	}

	//three arcane missiles, one after another; if the target dies the rest seek out other enemies
	private void fireMissile(Hero hero, Char target, int left, int p){
		Sample.INSTANCE.play(Assets.Sounds.ZAP, 1f, 1.2f);
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.ARCANE, hero.sprite, target.pos, new Callback() {
			@Override
			public void call() {
				if (target.isAlive()) {
					target.damage(amp(target, Random.NormalIntRange(2 + p, 4 + 2*p), Spell.MAGIC_MISSILES), MagicTome.this);
					target.sprite.burst(0xFFCC66FF, 4);
				}
				if (left > 1) {
					Char next = target.isAlive() ? target : nearestEnemy(hero);
					if (next != null) {
						fireMissile(hero, next, left - 1, p);
						return;
					}
				}
				hero.spendAndNext(1f);
			}
		});
	}

	private Char nearestEnemy(Hero hero){
		Char best = null;
		for (Mob m : Dungeon.level.mobs) {
			if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && Dungeon.level.heroFOV[m.pos]
					&& (best == null || Dungeon.level.distance(hero.pos, m.pos) < Dungeon.level.distance(hero.pos, best.pos))) {
				best = m;
			}
		}
		return best;
	}

	private void explode(Hero hero, int impact, int p, int burnChance){
		Sample.INSTANCE.play(Assets.Sounds.BLAST);
		ArrayList<Integer> cells = new ArrayList<>();
		cells.add(impact);
		for (int n : PathFinder.NEIGHBOURS8) cells.add(impact + n);
		for (int c : cells) {
			if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) continue;
			CellEmitter.center(c).burst(FlameParticle.FACTORY, c == impact ? 14 : 6);
			Char ch = Actor.findChar(c);
			if (ch == null || ch == hero) continue;
			int dmg = Random.NormalIntRange(6 + 2*p, 12 + 4*p);
			if (c != impact) dmg = Math.round(dmg * 0.5f);
			ch.damage(amp(ch, dmg, Spell.FIREBALL), this);
			if (ch.isAlive() && Random.Int(100) < burnChance) Buff.affect(ch, Burning.class).reignite(ch);
		}
	}

	private void chainLightning(Hero hero, Char first, int p, int jumps){
		ArrayList<Char> hit = new ArrayList<>();
		ArrayList<Lightning.Arc> arcs = new ArrayList<>();
		Char from = hero, to = first;
		while (to != null && hit.size() <= jumps) {
			hit.add(to);
			arcs.add(new Lightning.Arc(from.sprite.center(), to.sprite.center()));
			from = to;
			to = null;
			for (Mob m : Dungeon.level.mobs) {
				if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && !hit.contains(m)
						&& Dungeon.level.distance(from.pos, m.pos) <= 2 && Dungeon.level.heroFOV[m.pos]) {
					to = m;
					break;
				}
			}
		}
		hero.sprite.parent.addToFront(new Lightning(arcs, null));
		Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
		for (Char ch : hit) ch.damage(amp(ch, Random.NormalIntRange(4 + p, 9 + 2*p), Spell.CHAIN_LIGHTNING), this);
	}

	private void disintRay(Hero hero, int cell, int p){
		Ballistica beam = new Ballistica(hero.pos, cell, Ballistica.WONT_STOP);
		int dist = Math.min(beam.dist, 6 + p);
		ArrayList<Char> chars = new ArrayList<>();
		for (int c : beam.subPath(1, dist)) {
			Char ch = Actor.findChar(c);
			if (ch != null && ch != hero) chars.add(ch);
			CellEmitter.center(c).burst(PurpleParticle.BURST, 4);
		}
		Beam.ThickDeathRay ray = new Beam.ThickDeathRay(hero.sprite.center(),
				com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.raisedTileCenterToWorld(beam.path.get(dist)));
		hero.sprite.parent.add(ray);
		hero.sprite.zap(cell);
		Sample.INSTANCE.play(Assets.Sounds.RAY);
		for (Char ch : chars) {
			//10% to disintegrate outright (never bosses or minibosses)
			if (ch instanceof Mob && Random.Int(10) == 0 && !ch.properties().contains(Char.Property.BOSS)
					&& !ch.properties().contains(Char.Property.MINIBOSS)) {
				ch.sprite.emitter().burst(PurpleParticle.BURST, 20);
				GLog.p(Messages.get(MagicTome.class, "disintegrated", ch.name()));
				ch.die(this);
			} else {
				ch.damage(amp(ch, Random.NormalIntRange(6 + 2*p, 14 + 5*p), Spell.DISINT_RAY), this);
				ch.sprite.centerEmitter().burst(PurpleParticle.BURST, 8);
			}
		}
	}

	// ---- melee ----

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//the old staff talents carry over to the tome
		if (attacker instanceof Hero && ((Hero) attacker).hasTalent(Talent.MYSTICAL_CHARGE)) {
			Hero hero = (Hero) attacker;
			ArtifactRecharge.chargeArtifacts(hero, hero.pointsInTalent(Talent.MYSTICAL_CHARGE)/2f);
		}
		Talent.EmpoweredStrikeTracker empoweredStrike = attacker.buff(Talent.EmpoweredStrikeTracker.class);
		if (empoweredStrike != null) {
			damage = Math.round(damage * (1f + Dungeon.hero.pointsInTalent(Talent.EMPOWERED_STRIKE)/6f));
			if (!empoweredStrike.delayedDetach) empoweredStrike.detach();
		}
		return super.proc(attacker, defender, damage);
	}

	@Override
	public void activate(Char ch) {
		super.activate(ch);
		Buff.affect(ch, Cooldowns.class);
	}

	@Override
	public String info() {
		String info = super.info();
		StringBuilder sb = new StringBuilder();
		for (Spell s : Spell.values()) {
			if (rank(s) > 0) sb.append("\n- ").append(s.title()).append(" ").append(rank(s)).append("/").append(MAX_RANK);
		}
		return info + "\n\n" + Messages.get(MagicTome.class, "spells_known") + sb;
	}

	// ---- saving ----

	private static final String RANKS = "ranks";
	private static final String COOLDOWNS = "cooldowns";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(RANKS, ranks);
		bundle.put(COOLDOWNS, cooldowns);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(RANKS)) {
			int[] r = bundle.getIntArray(RANKS);
			System.arraycopy(r, 0, ranks, 0, Math.min(r.length, ranks.length));
		}
		if (bundle.contains(COOLDOWNS)) {
			float[] c = bundle.getFloatArray(COOLDOWNS);
			System.arraycopy(c, 0, cooldowns, 0, Math.min(c.length, cooldowns.length));
		}
	}

	// ---- buffs ----

	//ticks down the equipped tome's cooldowns; faster while recharging (scroll, talents)
	public static class Cooldowns extends Buff {
		{ revivePersists = true; }
		@Override
		public boolean act() {
			if (target instanceof Hero && ((Hero) target).belongings.weapon() instanceof MagicTome) {
				((MagicTome) ((Hero) target).belongings.weapon()).tick(target.buff(Recharging.class) != null ? 2f : 1f);
			}
			spend(TICK);
			return true;
		}
	}

	//ray of frost: 4 pulses, one per turn; the hero can't do anything else, and getting hit breaks it
	public static class FrostChannel extends Buff {
		public int target = -1, pulses = 0, power = 0, freeze = 15;

		//returns true if it pulsed (the hero's turn is used up)
		public boolean pulse(Hero hero) {
			Actor a = Actor.findById(target);
			if (!(a instanceof Char) || !((Char) a).isAlive() || pulses <= 0
					|| !Dungeon.level.heroFOV[((Char) a).pos]) {
				detach();
				return false;
			}
			Char t = (Char) a;
			hero.sprite.zap(t.pos);
			hero.sprite.parent.add(new Beam.IceRay(hero.sprite.center(), t.sprite.center()));
			Sample.INSTANCE.play(Assets.Sounds.RAY, 1f, 1.4f);
			//stronger up close: full power next to the target, falling off with distance
			int d = Dungeon.level.distance(hero.pos, t.pos);
			float falloff = Math.max(0.3f, 1.2f - 0.1f * d);
			t.damage(amp(t, Math.round(Random.NormalIntRange(3 + power, 6 + 2*power) * falloff), Spell.RAY_OF_FROST), this);
			if (t.isAlive()) {
				if (Random.Int(100) < freeze) Buff.affect(t, Frost.class, Frost.DURATION);
				else Buff.prolong(t, Chill.class, 2f);
			}
			pulses--;
			if (pulses <= 0) detach();
			return true;
		}

		public void interrupt() {
			GLog.w(Messages.get(MagicTome.class, "frost_broken"));
			detach();
		}

		private static final String TARGET = "target", PULSES = "pulses", POWER = "power", FREEZE = "freeze";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(TARGET, target); bundle.put(PULSES, pulses); bundle.put(POWER, power); bundle.put(FREEZE, freeze);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			target = bundle.getInt(TARGET); pulses = bundle.getInt(PULSES); power = bundle.getInt(POWER); freeze = bundle.getInt(FREEZE);
		}
	}

	//battlemage aura: magic bolts can't hurt the hero, and adjacent enemies slowly disintegrate
	public static class DisintAura extends FlavourBuff {
		public int power = 0;
		{
			type = buffType.POSITIVE;
			announced = true;
		}

		@Override
		public boolean act() {
			for (int n : PathFinder.NEIGHBOURS8) {
				Char ch = Actor.findChar(target.pos + n);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY) {
					//3% of max health per turn (+1% per rank), at least 1
					int dmg = Math.max(1, Math.round(ch.HT * (0.02f + 0.01f * Math.min(3, 1 + power / 3))));
					ch.damage(dmg, this);
					if (ch.sprite != null) ch.sprite.centerEmitter().burst(PurpleParticle.BURST, 3);
				}
			}
			if (target.sprite != null) target.sprite.emitter().burst(PurpleParticle.BURST, 2);
			return super.act();
		}

		@Override
		public int icon() {
			return BuffIndicator.INVISIBLE;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.7f, 0.3f, 1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (10f - visualcooldown()) / 10f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}

		private static final String POWER = "power";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(POWER, power);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			power = bundle.getInt(POWER);
		}
	}
}
