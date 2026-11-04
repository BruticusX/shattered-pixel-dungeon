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
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Javelin;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.watabou.utils.Reflection;


//MOD: the Test Room, a sandbox floor reached from the main menu. Every item in the game lies
//on its own tile, identified and grouped by category, next to an alchemy pot.
public class TestLevel extends Level {

	private static final int W = 36, H = 34;

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

		//alchemy pot (the enchanting cauldron) at the top; furnace and anvil will join it
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

	@Override
	protected void createMobs() {
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

		//every weapon and armor type, in iron/hide/linen so they're comparable
		for (Generator.Category cat : new Generator.Category[]{Generator.Category.WEP_T1, Generator.Category.WEP_T2,
				Generator.Category.WEP_T3, Generator.Category.WEP_T4, Generator.Category.WEP_T5}) {
			for (Class<?> c : cat.classes) {
				Weapon w = (Weapon) Reflection.newInstance(c);
				if (w.materialKind() == Material.Kind.METAL) w.material = Material.IRON;
				place(w);
			}
		}
		newRow();
		for (Generator.Category cat : new Generator.Category[]{Generator.Category.MIS_T1, Generator.Category.MIS_T2,
				Generator.Category.MIS_T3, Generator.Category.MIS_T4, Generator.Category.MIS_T5}) {
			for (Class<?> c : cat.classes) {
				Weapon w = (Weapon) Reflection.newInstance(c);
				if (w.materialKind() == Material.Kind.METAL) w.material = Material.IRON;
				place(w);
			}
		}
		newRow();
		placeAll(Generator.Category.ARMOR.classes);
		newRow();

		//one item in every material, to compare stats, durability and looks
		for (Material m : Material.values()) {
			if (m.kind != Material.Kind.METAL) continue;
			Sword s = new Sword(); s.material = m; place(s);
		}
		for (Material m : Material.values()) {
			if (m.kind != Material.Kind.METAL) continue;
			PlateArmor a = new PlateArmor(); a.material = m; place(a);
		}
		for (Material m : Material.values()) {
			if (m.kind == Material.Kind.CLOTH)   { ClothArmor a = new ClothArmor(); a.material = m; place(a); }
			if (m.kind == Material.Kind.LEATHER) { LeatherArmor a = new LeatherArmor(); a.material = m; place(a); }
		}
		for (Material m : Material.values()) {
			if (m.kind != Material.Kind.METAL) continue;
			Javelin j = new Javelin(); j.material = m; place(j);
		}
		//a worn-out sword and armor to test durability and repair kits
		Sword worn = new Sword(); worn.material = Material.IRON; worn.condition = 8; place(worn);
		PlateArmor broken = new PlateArmor(); broken.material = Material.IRON; broken.condition = 0; place(broken);
		place(new RepairKit().quantity(10));
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
