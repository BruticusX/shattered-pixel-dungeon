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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagicTome;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

//MOD: the magic tome's spell list: tap a spell to cast it, the (i) button explains it
public class WndTomeSpells extends Window {

	private static final int WIDTH = 120;
	private static final int BTN_H = 18;

	public WndTomeSpells(MagicTome tome, Hero hero) {
		IconTitle title = new IconTitle(new ItemSprite(tome), Messages.titleCase(tome.name()));
		title.setRect(0, 0, WIDTH, 0);
		add(title);

		RenderedTextBlock msg = PixelScene.renderTextBlock(Messages.get(this, "prompt"), 6);
		msg.maxWidth(WIDTH);
		msg.setPos(0, title.bottom() + 3);
		add(msg);

		float y = msg.bottom() + 4;
		for (MagicTome.Spell s : MagicTome.Spell.values()) {
			if (tome.rank(s) <= 0) continue;
			String label = s.title() + " " + tome.rank(s) + "/" + MagicTome.MAX_RANK;
			if (!tome.ready(s)) label += "  (" + Messages.get(this, "turns", tome.cooldownLeft(s)) + ")";
			RedButton btn = new RedButton(label, 7) {
				@Override
				protected void onClick() {
					hide();
					tome.cast(hero, s);
				}
			};
			btn.enable(tome.ready(s));
			btn.setRect(0, y, WIDTH - BTN_H - 2, BTN_H);
			add(btn);

			IconButton info = new IconButton(Icons.INFO.get()) {
				@Override
				protected void onClick() {
					GameScene.show(new WndTitledMessage(new ItemSprite(tome), Messages.titleCase(s.title()), tome.spellDesc(s)));
				}
			};
			info.setRect(WIDTH - BTN_H, y, BTN_H, BTN_H);
			add(info);

			y += BTN_H + 2;
		}

		resize(WIDTH, (int) y);
	}
}
