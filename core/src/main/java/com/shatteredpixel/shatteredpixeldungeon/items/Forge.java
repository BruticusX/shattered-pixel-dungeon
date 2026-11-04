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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

//MOD: the rules of the anvil and furnace.
//  reforge : change gear to any material of its kind, for its weight cost in that material
//  enhance : +1 level, for its weight cost in its own material
//  repair  : +50% durability, for 1 of its own material
//  infuse  : +1 level to wands, rings and other upgradable items, for 1 obsidian ingot
//  melt    : unused gear back into half its cost (rounded up) in its material
//  smelt   : ore into ingots, one for one
public class Forge {

	public static final float REPAIR_AMOUNT = 50f;

	//weapons and armor that are made of a material
	public static boolean isGear(Item item) {
		if (item instanceof Weapon) return ((Weapon) item).materialKind() != null;
		if (item instanceof Armor)  return ((Armor) item).materialKind() != null;
		return false;
	}

	public static Material.Kind kind(Item item) {
		if (item instanceof Weapon) return ((Weapon) item).materialKind();
		if (item instanceof Armor)  return ((Armor) item).materialKind();
		return null;
	}

	public static Material material(Item item) {
		if (item instanceof Weapon) return ((Weapon) item).material();
		if (item instanceof Armor)  return ((Armor) item).material();
		return null;
	}

	private static void setMaterial(Item item, Material m) {
		if (item instanceof Weapon) ((Weapon) item).material = m;
		if (item instanceof Armor)  ((Armor) item).material = m;
	}

	//light weapons 2, medium 3, heavy 4; thrown weapons 1 per set;
	//cloth & leather armor 2, mail 3, scale 4, plate 5
	public static int cost(Item item) {
		if (item instanceof MissileWeapon) return 1;
		if (item instanceof Weapon) {
			int t = ((Weapon) item).typeTier();
			return t <= 2 ? 2 : (t == 3 ? 3 : 4);
		}
		if (item instanceof Armor) return Math.max(2, ((Armor) item).tier);
		return 0;
	}

	public static int meltYield(Item item) {
		return (cost(item) + 1) / 2;
	}

	public static boolean canRepair(Item item) {
		return RepairKit.repairable(item);
	}

	public static boolean canInfuse(Item item) {
		return item.isUpgradable() && !isGear(item) && !(item instanceof MaterialItem);
	}

	private static void effects(Hero hero) {
		hero.sprite.operate(hero.pos);
		hero.sprite.emitter().burst(Speck.factory(Speck.FORGE), 4);
		Sample.INSTANCE.play(Assets.Sounds.EVOKE);
		hero.spend(1f);
		hero.busy();
		Item.updateQuickslot();
	}

	//unique weapons are always their one material
	public static boolean canReforge(Item item) {
		return isGear(item) && !(item instanceof Weapon && ((Weapon) item).fixedMaterial() != null);
	}

	public static boolean reforge(Hero hero, Item item, Material to) {
		if (!canReforge(item) || to.kind != kind(item) || to == material(item)) return false;
		if (!MaterialItem.spend(hero, to, cost(item))) return false;
		setMaterial(item, to);
		effects(hero);
		GLog.p(Messages.get(Forge.class, "reforged", item.name()));
		return true;
	}

	public static boolean enhance(Hero hero, Item item) {
		if (!isGear(item) || !item.isUpgradable()) return false;
		if (!MaterialItem.spend(hero, material(item), cost(item))) return false;
		upgrade(hero, item);
		effects(hero);
		GLog.p(Messages.get(Forge.class, "enhanced", item.name()));
		return true;
	}

	public static boolean repair(Hero hero, Item item) {
		if (!canRepair(item)) return false;
		if (!MaterialItem.spend(hero, material(item), 1)) return false;
		if (item instanceof Weapon) ((Weapon) item).repair(REPAIR_AMOUNT);
		else                        ((Armor) item).repair(REPAIR_AMOUNT);
		effects(hero);
		GLog.p(Messages.get(Forge.class, "repaired", item.name()));
		return true;
	}

	public static boolean infuse(Hero hero, Item item) {
		if (!canInfuse(item)) return false;
		if (!MaterialItem.spend(hero, Material.OBSIDIAN, 1)) return false;
		upgrade(hero, item);
		effects(hero);
		GLog.p(Messages.get(Forge.class, "infused", item.name()));
		return true;
	}

	//uses the scroll of upgrade's logic so curses and enchantments behave as before
	private static void upgrade(Hero hero, Item item) {
		Item.curUser = hero;
		new ScrollOfUpgrade().upgradeItem(item);
	}

	public static boolean canMelt(Hero hero, Item item) {
		if (item instanceof MaterialItem) return ((MaterialItem) item).ore;
		return isGear(item) && !item.isEquipped(hero) && hero.belongings.backpack.contains(item);
	}

	public static boolean melt(Hero hero, Item item) {
		if (!canMelt(hero, item)) return false;
		MaterialItem out;
		if (item instanceof MaterialItem) {
			out = new MaterialItem(((MaterialItem) item).material);
			out.quantity(item.quantity());
		} else {
			out = new MaterialItem(material(item));
			out.quantity(meltYield(item));
		}
		String name = item.name();
		item.detachAll(hero.belongings.backpack);
		if (!out.collect(hero.belongings.backpack)) {
			com.shatteredpixel.shatteredpixeldungeon.Dungeon.level.drop(out, hero.pos).sprite.drop();
		}
		hero.sprite.operate(hero.pos);
		hero.sprite.emitter().burst(Speck.factory(Speck.STEAM), 6);
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		hero.spend(1f);
		hero.busy();
		Item.updateQuickslot();
		GLog.p(Messages.get(Forge.class, "melted", name, out.quantity(), out.name()));
		return true;
	}
}
