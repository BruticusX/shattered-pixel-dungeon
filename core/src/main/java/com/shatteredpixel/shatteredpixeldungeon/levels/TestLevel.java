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
		com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue statue = new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue();
		statue.pos = cell(W / 2 + 6, H - 3);
		mobs.add(statue);
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
