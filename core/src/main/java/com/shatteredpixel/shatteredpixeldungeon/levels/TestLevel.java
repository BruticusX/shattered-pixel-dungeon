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

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Material;
import com.shatteredpixel.shatteredpixeldungeon.items.RepairKit;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfArsenal;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Elite;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Crab;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Gnoll;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.MaterialItem;
import com.watabou.utils.Reflection;


//MOD: the Test Room, a sandbox floor reached from the main menu. Every item in the game lies
//on its own tile, identified and grouped by category, next to an alchemy pot.
public class TestLevel extends Level {

	private static final int W = 40, H = 64;

	{
		color1 = 0x48763c;
		color2 = 0x59994a;
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_SEWERS;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_SEWERS;
	}

	private int cell(int x, int y) {
		return x + y * width();
	}

	@Override
	protected boolean build() {
		setSize(W, H);
		for (int i = 0; i < length(); i++) map[i] = Terrain.WALL;
		for (int y = 1; y < H - 1; y++) {
			for (int x = 1; x < W - 1; x++) {
				map[cell(x, y)] = Terrain.EMPTY;
			}
		}

		//alchemy pot (the enchanting cauldron) at the top; a furnace and anvil are placed beside it
		map[cell(W / 2, 1)] = Terrain.ALCHEMY;

		int entrance = cell(W / 2, H - 2);
		map[entrance] = Terrain.ENTRANCE;
		transitions.add(new LevelTransition(this, entrance, LevelTransition.Type.REGULAR_ENTRANCE));
		return true;
	}

	@Override
	public Mob createMob() {
		return null;
	}

	//a few elite enemies to test: they wander and stay peaceful until hit
	@Override
	protected void createMobs() {
		Mob[] elites = {new Rat(), new Gnoll(), new Crab()};
		int[] xs = {6, W / 2 - 6, W - 7};
		for (int i = 0; i < elites.length; i++) {
			elites[i].pos = cell(xs[i], H - 3);
			Elite.make(elites[i]);
			mobs.add(elites[i]);
		}
		//a statue in random gear: it stands still until you attack it
		//(Statue.random gives it its weapon and armor; a bare constructor leaves both empty, which crashes on a hit)
		com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue statue = com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue.random(false);
		statue.pos = cell(W / 2 + 6, H - 3);
		mobs.add(statue);
		//redrawn monsters on show: they pace and bite at the air, but never fight back
		Mob rat = new ShowRat();
		rat.pos = cell(W / 2 - 3, H - 5);
		mobs.add(rat);
		Mob goo = new ShowGoo();
		goo.pos = cell(W / 2 + 3, H - 5);
		mobs.add(goo);
		//the redrawn sewer creatures and their variants, two tiles apart so they can pace
		Class<?>[] lineup = {
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Albino.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Snake.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Gnoll.class,
				ShowSwarm.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Crab.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Slime.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.FetidRat.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollTrickster.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GreatCrab.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CausticSlime.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Skeleton.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Thief.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bandit.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM100.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Guard.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Necromancer.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.SpectralNecromancer.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RotHeart.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RotLasher.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bat.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Brute.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredBrute.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Shaman.RedShaman.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Shaman.BlueShaman.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Shaman.PurpleShaman.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Spinner.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM200.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM201.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGuard.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollSapper.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Ghoul.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.FireElemental.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.NewbornFireElemental.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.FrostElemental.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.ShockElemental.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.ChaosElemental.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Warlock.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Monk.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Senior.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Golem.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RipperDemon.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DemonSpawner.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Succubus.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Eye.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Scorpio.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Acidic.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Piranha.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.PhantomPiranha.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GoldenMimic.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalMimic.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EbonyMimic.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bee.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalGuardian.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalWisp.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.FungalSentry.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.FungalSpinner.class,
				com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SentryRoom.Sentry.class,
				com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.EarthGuardian.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.TormentedSpirit.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sheep.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RatKing.class,
		};
		for (int i = 0; i < lineup.length; i++) {
			Mob m = lineup[i] == ShowSwarm.class ? new ShowSwarm() : new ShowMob().as( (Class<? extends Mob>) lineup[i] );
			m.pos = cell(3 + (i % 5) * 7, H - 9 - (i / 5) * 3);
			mobs.add(m);
		}
	}

	//shows off a monster's animations: steps one tile and back, then attacks the air.
	//returns how long the monster should wait
	private static float showcase( Mob m, int beat ){
		int phase = beat % 4;
		if (m.sprite == null || !m.sprite.visible) return 1f;
		if (phase == 0 || phase == 1) {
			int to = m.pos + (phase == 0 ? 1 : -1);
			if (Dungeon.level.passable[to] && Actor.findChar( to ) == null) {
				int from = m.pos;
				m.move( to );
				m.sprite.move( from, to );
			}
		} else if (phase == 2) {
			m.sprite.showcaseAttack( m.pos, m.pos + 1 );
		} else {
			m.sprite.idle();
		}
		return 1.5f;
	}

	public static class ShowRat extends Rat {
		{ state = PASSIVE; EXP = 0; maxLvl = -1; }
		private int beat = 0;
		@Override
		protected boolean act() {
			if (!isAlive()) return super.act();
			state = PASSIVE;
			spend( showcase( this, beat++ ) );
			return true;
		}
		@Override
		public void onMotionComplete() {
			//the run loop would otherwise keep playing after the single step
			if (sprite != null && isAlive()) sprite.idle();
		}
	}

	public static class ShowGoo extends Mob {
		{
			spriteClass = com.shatteredpixel.shatteredpixeldungeon.sprites.GooSprite.class;
			HP = HT = 100;
			EXP = 0; maxLvl = -1;
			state = PASSIVE;
		}
		private int beat = 0;
		@Override
		protected boolean act() {
			if (!isAlive()) return super.act();
			state = PASSIVE;
			spend( showcase( this, beat++ ) );
			return true;
		}
		@Override
		public void onMotionComplete() {
			//the run loop would otherwise keep playing after the single step
			if (sprite != null && isAlive()) sprite.idle();
		}
	}

	//MOD: a harmless stand-in that looks like (and is named after) any monster, for the test room's
	//line-up of redrawn creatures
	public static class ShowMob extends Mob {
		{
			HP = HT = 30;
			EXP = 0; maxLvl = -1;
			state = PASSIVE;
		}
		private Class<? extends Mob> of = com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat.class;
		private int beat = 0;

		public ShowMob as( Class<? extends Mob> cls ){
			of = cls;
			spriteClass = Reflection.newInstance( cls ).spriteClass;
			return this;
		}
		@Override
		public String name() {
			return com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get( of, "name" );
		}
		@Override
		public String description() {
			return com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get( of, "desc" );
		}
		@Override
		protected boolean act() {
			if (!isAlive()) return super.act();
			state = PASSIVE;
			spend( showcase( this, beat++ ) );
			return true;
		}
		@Override
		public void onMotionComplete() {
			if (sprite != null && isAlive()) sprite.idle();
		}
		@Override
		public void storeInBundle( com.watabou.utils.Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( "of", of );
		}
		@Override
		public void restoreFromBundle( com.watabou.utils.Bundle bundle ) {
			super.restoreFromBundle( bundle );
			Class<?> c = bundle.getClass( "of" );
			if (c != null) as( (Class<? extends Mob>) c );
		}
	}

	//the real swarm, so hitting it splits it like in the dungeon, but harmless like the others
	public static class ShowSwarm extends com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Swarm {
		{ state = PASSIVE; EXP = 0; maxLvl = -1; }
		private int beat = 0;
		@Override
		protected boolean act() {
			if (!isAlive()) return super.act();
			state = PASSIVE;
			spend( showcase( this, beat++ ) );
			return true;
		}
		@Override
		protected com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Swarm newClone() {
			return new ShowSwarm();
		}
		@Override
		public void onMotionComplete() {
			if (sprite != null && isAlive()) sprite.idle();
		}
	}

	public Actor addRespawner() {
		return null;
	}

	//items are laid out left to right, a new row per category with a gap between
	private int cx = 2, cy = 3;

	private void place(Item item) {
		if (item == null) return;
		if (cx > W - 3) { cx = 2; cy++; }
		if (cy > H - 4) return; //room is full
		item.identify();
		drop(item, cell(cx, cy));
		cx++;
	}

	private void newRow() {
		if (cx != 2) { cx = 2; cy++; }
		cy++;
	}

	private void placeAll(Class<?>... classes) {
		for (Class<?> c : classes) {
			Object o = Reflection.newInstance(c);
			if (o instanceof Item) place((Item) o);
		}
	}

	@Override
	protected void createItems() {
		Dungeon.energy = Math.max(Dungeon.energy, 200);

		//one chest beside the forge with every crafting material (10 of each) and some ore
		//a loot chest beside the entrance to try the chest window
		int loot = cell(W / 2 - 3, 1);
		drop(new Gold(150), loot).type = Heap.Type.CHEST;
		drop(Generator.random(Generator.Category.POTION), loot);
		drop(Generator.random(Generator.Category.SCROLL), loot);
		drop(Generator.random(Generator.Category.RING), loot);
		drop(Generator.randomWeapon(), loot);

		int chest = cell(W / 2 + 3, 1);
		for (Material m : Material.values()) {
			drop(new MaterialItem(m).quantity(10), chest).type = Heap.Type.CHEST;
		}
		for (Material m : Material.values()) {
			if (m.kind == Material.Kind.METAL) drop(new MaterialItem(m, true).quantity(3), chest);
		}

		Generator.Category[] melee = {Generator.Category.WEP_T1, Generator.Category.WEP_T2,
				Generator.Category.WEP_T3, Generator.Category.WEP_T4, Generator.Category.WEP_T5};
		Generator.Category[] thrown = {Generator.Category.MIS_T1, Generator.Category.MIS_T2,
				Generator.Category.MIS_T3, Generator.Category.MIS_T4, Generator.Category.MIS_T5};

		//one row per metal: every metal melee weapon type, weakest metal first
		for (Material m : Material.values()) {
			if (m.kind != Material.Kind.METAL) continue;
			for (Generator.Category cat : melee) {
				for (Class<?> c : cat.classes) {
					Weapon w = (Weapon) Reflection.newInstance(c);
					if (w.materialKind() != Material.Kind.METAL || w.fixedMaterial() != null) continue;
					w.material = m;
					place(w);
				}
			}
			newRow();
		}

		//unique weapons come in one material only
		for (Generator.Category cat : melee) {
			for (Class<?> c : cat.classes) {
				Weapon w = (Weapon) Reflection.newInstance(c);
				if (w.fixedMaterial() != null) place(w);
			}
		}
		newRow();

		//one row per metal: every metal thrown weapon, then mail, scale and plate armor
		for (Material m : Material.values()) {
			if (m.kind != Material.Kind.METAL) continue;
			for (Generator.Category cat : thrown) {
				for (Class<?> c : cat.classes) {
					Weapon w = (Weapon) Reflection.newInstance(c);
					if (w.materialKind() != Material.Kind.METAL) continue;
					w.material = m;
					place(w);
				}
			}
			cx++;
			for (Class<?> c : new Class<?>[]{com.shatteredpixel.shatteredpixeldungeon.items.armor.Brigandine.class, MailArmor.class, ScaleArmor.class,
					com.shatteredpixel.shatteredpixeldungeon.items.armor.HalfPlate.class, PlateArmor.class}) {
				Armor a = (Armor) Reflection.newInstance(c);
				a.material = m;
				place(a);
			}
			newRow();
		}

		//cloth and leather in every material, leather whips, non-metal thrown weapons
		for (Material m : Material.values()) {
			if (m.kind == Material.Kind.CLOTH)   { ClothArmor a = new ClothArmor(); a.material = m; place(a); }
		}
		for (Material m : Material.values()) {
			if (m.kind == Material.Kind.CLOTH)   { ClothArmor a = new com.shatteredpixel.shatteredpixeldungeon.items.armor.Gambeson(); a.material = m; place(a); }
		}
		cx++;
		for (Material m : Material.values()) {
			if (m.kind == Material.Kind.LEATHER) { LeatherArmor a = new LeatherArmor(); a.material = m; place(a); }
		}
		for (Material m : Material.values()) {
			if (m.kind == Material.Kind.LEATHER) { LeatherArmor a = new com.shatteredpixel.shatteredpixeldungeon.items.armor.StuddedLeather(); a.material = m; place(a); }
		}
		cx++;
		for (Material m : Material.values()) {
			if (m.kind == Material.Kind.LEATHER) { Whip wh = new Whip(); wh.material = m; place(wh); }
		}
		cx++;
		for (Generator.Category cat : thrown) {
			for (Class<?> c : cat.classes) {
				Weapon w = (Weapon) Reflection.newInstance(c);
				if (w.materialKind() == null) place(w);
			}
		}
		place(new MagesStaff());
		newRow();

		//a worn-out sword and armor to test durability and repair kits
		Sword worn = new Sword(); worn.material = Material.IRON; worn.condition = 8; place(worn);
		PlateArmor broken = new PlateArmor(); broken.material = Material.IRON; broken.condition = 0; place(broken);
		place(new RepairKit().quantity(10));
		place(new com.shatteredpixel.shatteredpixeldungeon.items.TomeOfUnlearning().quantity(3));
		newRow();

		placeAll(Generator.Category.POTION.classes);
		placeAll(ExoticPotion.regToExo.values().toArray(new Class<?>[0]));
		newRow();
		placeAll(Generator.Category.SCROLL.classes);
		placeAll(ExoticScroll.regToExo.values().toArray(new Class<?>[0]));
		place(new ScrollOfArsenal());
		//MOD: the new scrolls, and a sword with each new enchantment
		place(new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMending().quantity(5));
		place(new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfFocus().quantity(5));
		place(new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfSmelting().quantity(5));
		for (Class<?> e : new Class<?>[]{com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Sundering.class, com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Frenzied.class, com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Enduring.class}) {
			com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword s = new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();
			s.enchant((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment) com.watabou.utils.Reflection.newInstance(e));
			s.identify();
			place(s);
		}
		newRow();
		placeAll(Generator.Category.STONE.classes);
		placeAll(Generator.Category.SEED.classes);
		newRow();
		placeAll(Generator.Category.WAND.classes);
		placeAll(Generator.Category.RING.classes);
		newRow();
		placeAll(Generator.Category.ARTIFACT.classes);
		placeAll(Generator.Category.TRINKET.classes);
		newRow();
		placeAll(Generator.Category.FOOD.classes);
		place(new Gold(5000));
	}

	@Override
	public int randomRespawnCell(Char ch) {
		return entrance() - width();
	}
}
