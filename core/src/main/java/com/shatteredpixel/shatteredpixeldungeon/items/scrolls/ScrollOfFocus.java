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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

//MOD: new scroll. Always identified. Kills train weapon proficiency twice as fast for a while.
public class ScrollOfFocus extends Scroll {

	public static final float DURATION = 300f;
	private static final int IMG = ItemSpriteSheet.SCROLL_FOCUS;

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
		Buff.affect(curUser, Focused.class, DURATION);
		GLog.p(Messages.get(this, "focused"));
		Sample.INSTANCE.play(Assets.Sounds.READ);
		readAnimation();
	}

	@Override
	public int value() {
		return 40 * quantity;
	}

	public static class Focused extends FlavourBuff {
		{
			type = buffType.POSITIVE;
			announced = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.BLESS;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.4f, 0.8f, 1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (DURATION - visualcooldown()) / DURATION);
		}
	}
}
