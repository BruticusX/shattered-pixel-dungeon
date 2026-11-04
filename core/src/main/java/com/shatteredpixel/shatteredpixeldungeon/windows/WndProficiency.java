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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Proficiency;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ColorBlock;

//MOD: lists the hero class's proficiency with every weapon family
public class WndProficiency extends Window {

	private static final int WIDTH = 120;
	private static final int BAR_H = 2;

	public WndProficiency(final HeroClass cls) {
		super();

		RenderedTextBlock title = PixelScene.renderTextBlock(Messages.get(this, "title"), 9);
		title.hardlight(TITLE_COLOR);
		title.setPos((WIDTH - title.width()) / 2f, 2);
		add(title);

		float pos = title.bottom() + 4;

		RenderedTextBlock info = PixelScene.renderTextBlock(Messages.get(this, "info"), 6);
		info.maxWidth(WIDTH);
		info.setPos(0, pos);
		add(info);
		pos = info.bottom() + 4;

		for (Proficiency.Family f : Proficiency.Family.values()) {
			int lvl = Proficiency.level(cls, f);
			int xp = Proficiency.xp(cls, f);
			float rowTop = pos;

			RenderedTextBlock name = PixelScene.renderTextBlock(Messages.titleCase(f.title()), 7);
			name.setPos(0, pos);
			add(name);

			String right = lvl >= Proficiency.MAX_LEVEL
					? Messages.get(this, "max")
					: Messages.get(this, "level", lvl, xp, Proficiency.nextThreshold(lvl));
			RenderedTextBlock lv = PixelScene.renderTextBlock(right, 6);
			lv.setPos(WIDTH - lv.width(), pos + (name.height() - lv.height()) / 2f);
			if (lvl > 0) lv.hardlight(0xFFFF44);
			add(lv);

			pos = Math.max(name.bottom(), lv.bottom()) + 1;

			ColorBlock bg = new ColorBlock(WIDTH, BAR_H, 0xFF333333);
			bg.x = 0; bg.y = pos;
			add(bg);

			float progress;
			if (lvl >= Proficiency.MAX_LEVEL) {
				progress = 1f;
			} else {
				int lo = Proficiency.prevThreshold(lvl), hi = Proficiency.nextThreshold(lvl);
				progress = (xp - lo) / (float) Math.max(1, hi - lo);
			}
			ColorBlock fg = new ColorBlock(Math.max(0, WIDTH * Math.min(1f, progress)), BAR_H, 0xFFDDAA33);
			fg.x = 0; fg.y = pos;
			add(fg);

			//tap a row to open that family's skill tree
			final Proficiency.Family fam = f;
			com.shatteredpixel.shatteredpixeldungeon.ui.Button row = new com.shatteredpixel.shatteredpixeldungeon.ui.Button() {
				@Override
				protected void onClick() {
					com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.show(new WndSkillTree(cls, fam));
				}
			};
			row.setRect(0, rowTop, WIDTH, pos + BAR_H - rowTop);
			add(row);
			if (Proficiency.pointsAvailable(cls, f) > 0) name.hardlight(0x66FF66);

			pos += BAR_H + 3;
		}

		resize(WIDTH, (int) pos);
	}
}
