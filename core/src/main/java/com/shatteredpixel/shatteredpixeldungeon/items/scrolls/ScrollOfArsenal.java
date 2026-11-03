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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

//MOD: new scroll. Always identified (it has its own sprite instead of a random rune).
//Reading it conjures an identified, uncursed melee weapon at your feet.
public class ScrollOfArsenal extends Scroll {

	{
		image = ItemSpriteSheet.SCROLL_ARSENAL;
	}

	@Override
	public void reset() {
		super.reset();
		//not part of the rune system, so keep our own sprite
		image = ItemSpriteSheet.SCROLL_ARSENAL;
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

		MeleeWeapon wep = Generator.randomWeapon();
		wep.cursed = false;
		if (wep.hasCurseEnchant()) {
			wep.enchant(null);
		}
		wep.identify();

		Dungeon.level.drop(wep, curUser.pos).sprite.drop();

		GLog.p(Messages.get(this, "summon", wep.name()));
		Sample.INSTANCE.play(Assets.Sounds.READ);

		readAnimation();
	}

	@Override
	public int value() {
		return 50 * quantity;
	}
}
