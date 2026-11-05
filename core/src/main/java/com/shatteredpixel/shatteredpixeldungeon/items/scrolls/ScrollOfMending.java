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

package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

//MOD: new scroll. Always identified. Restores half the durability of everything you have equipped.
public class ScrollOfMending extends Scroll {

	public static final float REPAIR = 50f;
	private static final int IMG = ItemSpriteSheet.SCROLL_MENDING;

	{
		image = IMG;
	}

	@Override
	public void reset() {
		super.reset();
		image = IMG; //not part of the rune system, so keep our own sprite
	}

	@Override
	public boolean isKnown() {
		return true;
	}

	@Override
	public void setKnown() {
		//always known, nothing to record
	}

	@Override
	public void doRead() {
		detach(curUser.belongings.backpack);

		for (KindOfWeapon w : new KindOfWeapon[]{curUser.belongings.weapon(), curUser.belongings.secondWep()}) {
			if (w instanceof Weapon) ((Weapon) w).repair(REPAIR);
		}
		Armor a = curUser.belongings.armor();
		if (a != null) a.repair(REPAIR);

		GLog.p(Messages.get(this, "mended"));
		Sample.INSTANCE.play(Assets.Sounds.READ);
		readAnimation();
	}

	@Override
	public int value() {
		return 40 * quantity;
	}
}
