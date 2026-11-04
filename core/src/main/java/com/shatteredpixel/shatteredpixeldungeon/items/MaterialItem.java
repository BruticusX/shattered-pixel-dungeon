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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//MOD: a crafting material for the anvil: a metal ingot, a bolt of cloth or a piece of leather.
//Metal can also come as ore, which the furnace smelts into ingots.
public class MaterialItem extends Item {

	{
		stackable = true;
	}

	public Material material = Material.TIN;
	public boolean ore = false;

	public MaterialItem() {
		updateImage();
	}

	public MaterialItem(Material material) {
		this(material, false);
	}

	public MaterialItem(Material material, boolean ore) {
		this.material = material;
		this.ore = ore && material.kind == Material.Kind.METAL;
		updateImage();
	}

	private void updateImage() {
		if (ore) {
			image = ItemSpriteSheet.METAL_ORE;
		} else switch (material.kind) {
			case METAL: default: image = ItemSpriteSheet.INGOT; break;
			case CLOTH:          image = ItemSpriteSheet.CLOTH_BOLT; break;
			case LEATHER:        image = ItemSpriteSheet.LEATHER_PIECE; break;
		}
	}

	//a random material of this kind for the given depth (never obsidian)
	public static MaterialItem randomFor(Material.Kind kind, int depth) {
		return new MaterialItem(Material.randomFor(kind, Math.max(1, depth), false));
	}

	//metal half the time, otherwise cloth or leather
	public static MaterialItem randomAny(int depth) {
		switch (Random.chances(new float[]{2, 1, 1})) {
			case 0: default: return randomFor(Material.Kind.METAL, depth);
			case 1:          return randomFor(Material.Kind.CLOTH, depth);
			case 2:          return randomFor(Material.Kind.LEATHER, depth);
		}
	}

	//bosses always leave materials behind, a bit better than the floor they guard
	public static void dropBossLoot(int pos) {
		int depth = com.shatteredpixel.shatteredpixeldungeon.Dungeon.depth + 2;
		java.util.ArrayList<Item> loot = new java.util.ArrayList<>();
		loot.add(randomFor(Material.Kind.METAL, depth).quantity(2));
		loot.add(randomFor(Material.Kind.METAL, depth));
		loot.add(randomFor(Random.Int(2) == 0 ? Material.Kind.CLOTH : Material.Kind.LEATHER, depth).quantity(2));
		//bosses from the prison down also leave an umbralite ingot
		if (com.shatteredpixel.shatteredpixeldungeon.Dungeon.depth >= 10) loot.add(obsidian());
		for (Item i : loot) {
			com.shatteredpixel.shatteredpixeldungeon.Dungeon.level.drop(i, pos).sprite.drop();
		}
	}

	public static MaterialItem obsidian() {
		return new MaterialItem(Material.OBSIDIAN);
	}

	//how many of a material the hero carries (finished materials only, not ore)
	public static int count(Hero hero, Material m) {
		int total = 0;
		for (Item i : hero.belongings.backpack) { //includes the forge satchel
			if (i instanceof MaterialItem && ((MaterialItem) i).material == m && !((MaterialItem) i).ore) {
				total += i.quantity();
			}
		}
		return total;
	}

	//removes this many of a material from the hero's bag; returns false if they don't have enough
	public static boolean spend(Hero hero, Material m, int amount) {
		if (count(hero, m) < amount) return false;
		java.util.ArrayList<Item> all = new java.util.ArrayList<>();
		for (Item i : hero.belongings.backpack) all.add(i); //includes the forge satchel
		for (Item i : all) {
			if (amount <= 0) break;
			if (i instanceof MaterialItem && ((MaterialItem) i).material == m && !((MaterialItem) i).ore) {
				int take = Math.min(amount, i.quantity());
				if (take >= i.quantity()) {
					i.detachAll(hero.belongings.backpack);
				} else {
					i.quantity(i.quantity() - take);
				}
				amount -= take;
			}
		}
		Item.updateQuickslot();
		return true;
	}

	public static final String AC_MELD = "MELD";

	private boolean isUmbralite() {
		return material == Material.OBSIDIAN && !ore;
	}

	@Override
	public java.util.ArrayList<String> actions(Hero hero) {
		java.util.ArrayList<String> actions = super.actions(hero);
		if (isUmbralite()) actions.add(AC_MELD);
		return actions;
	}

	@Override
	public String defaultAction() {
		return isUmbralite() ? AC_MELD : super.defaultAction();
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_MELD) && isUmbralite()) {
			curUser = hero;
			com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.selectItem(meldSelector);
		}
	}

	//umbralite can be melded on the spot, no anvil needed
	public static final com.shatteredpixel.shatteredpixeldungeon.windows.WndBag.ItemSelector meldSelector =
			new com.shatteredpixel.shatteredpixeldungeon.windows.WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(MaterialItem.class, "meld_prompt");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return Forge.canMeld(item);
		}

		@Override
		public void onSelect(Item item) {
			if (item != null) Forge.meld(curUser, item);
		}
	};

	@Override
	public com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite.Glowing glowing() {
		return isUmbralite() ? UMBRA_GLOW : null;
	}

	@Override
	public String spriteSheet() {
		return material.spriteSheet();
	}

	@Override
	public boolean isSimilar(Item item) {
		return super.isSimilar(item) && ((MaterialItem) item).material == material
				&& ((MaterialItem) item).ore == ore;
	}

	private String formKey() {
		if (ore) return "ore";
		switch (material.kind) {
			case METAL: default: return "ingot";
			case CLOTH:          return "bolt";
			case LEATHER:        return "leather";
		}
	}

	@Override
	public String name() {
		return Messages.get(MaterialItem.class, formKey(), material.title());
	}

	@Override
	public String desc() {
		String d = Messages.get(MaterialItem.class, "desc_" + formKey(), material.title());
		if (material == Material.OBSIDIAN && !ore) d += "\n\n" + Messages.get(MaterialItem.class, "desc_obsidian");
		return d;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		int each = Math.round(10 * material.power);
		if (material == Material.OBSIDIAN) each = 100;
		if (ore) each /= 2;
		return each * quantity;
	}

	private static final String MATERIAL = "material";
	private static final String ORE = "ore";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(MATERIAL, material.name());
		bundle.put(ORE, ore);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		material = Material.fromName(bundle.getString(MATERIAL), Material.TIN);
		ore = bundle.getBoolean(ORE);
		updateImage();
	}

	//shown in the hero's material list at the anvil
	public static String summary(Hero hero) {
		StringBuilder sb = new StringBuilder();
		for (Material m : Material.values()) {
			int n = count(hero, m);
			if (n > 0) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(m.title()).append(" x").append(n);
			}
		}
		return sb.length() == 0 ? Messages.get(MaterialItem.class, "none") : sb.toString();
	}
}
