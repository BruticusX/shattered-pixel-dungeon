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
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//MOD: restores durability to one weapon or piece of armor
public class RepairKit extends Item {

	public static final float REPAIR_AMOUNT = 50f;

	private static final String AC_REPAIR = "REPAIR";

	{
		image = ItemSpriteSheet.REPAIR_KIT;
		stackable = true;
		defaultAction = AC_REPAIR;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_REPAIR);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_REPAIR)) {
			curItem = this;
			GameScene.selectItem(selector);
		}
	}

	public static boolean repairable(Item item) {
		if (item instanceof Weapon) return ((Weapon) item).usesCondition() && ((Weapon) item).condition < 100f;
		if (item instanceof Armor)  return ((Armor) item).usesCondition() && ((Armor) item).condition < 100f;
		return false;
	}

	private final WndBag.ItemSelector selector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(RepairKit.class, "prompt");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return repairable(item);
		}

		@Override
		public void onSelect(Item item) {
			if (item == null || !(curItem instanceof RepairKit) || !repairable(item)) return;

			if (item instanceof Weapon) ((Weapon) item).repair(REPAIR_AMOUNT);
			else                        ((Armor) item).repair(REPAIR_AMOUNT);

			curItem.detach(curUser.belongings.backpack);

			curUser.sprite.operate(curUser.pos);
			curUser.sprite.emitter().burst(Speck.factory(Speck.UP), 5);
			Sample.INSTANCE.play(Assets.Sounds.EVOKE);
			curUser.spend(1f);
			curUser.busy();
			GLog.p(Messages.get(RepairKit.class, "repaired", item.name()));
		}
	};

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
		return 40 * quantity;
	}
}
