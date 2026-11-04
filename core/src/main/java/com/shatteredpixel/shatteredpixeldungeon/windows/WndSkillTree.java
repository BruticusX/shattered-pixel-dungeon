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

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Proficiency;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Skill;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

//MOD: one weapon family's skill tree: learn ranks with the points proficiency gives you
public class WndSkillTree extends Window {

	private static final int WIDTH = 130;
	private static final int MAX_HEIGHT = 200;

	public WndSkillTree(final HeroClass cls, final Proficiency.Family f) {
		super();

		RenderedTextBlock title = PixelScene.renderTextBlock(Messages.titleCase(f.title()), 9);
		title.hardlight(TITLE_COLOR);
		title.setPos((WIDTH - title.width()) / 2f, 2);
		add(title);

		RenderedTextBlock pts = PixelScene.renderTextBlock(Messages.get(this, "points",
				Proficiency.pointsAvailable(cls, f), Proficiency.level(cls, f)), 6);
		pts.setPos((WIDTH - pts.width()) / 2f, title.bottom() + 2);
		add(pts);

		Component content = new Component();
		//buttons inside a scroll pane don't get taps directly; the pane hands them over (see below)
		final java.util.ArrayList<RedButton> buttons = new java.util.ArrayList<>();
		final java.util.ArrayList<Skill> buttonSkills = new java.util.ArrayList<>();
		float pos = 0;
		for (final Skill s : Skill.of(f)) {
			int rank = Proficiency.rank(cls, s);
			String kind = Messages.get(this, "kind_" + s.kind.name().toLowerCase());
			RenderedTextBlock name = PixelScene.renderTextBlock(
					"_" + Messages.titleCase(s.title()) + "_  " + kind + "  " + rank + "/" + s.maxRank(), 7);
			name.maxWidth(WIDTH);
			name.setPos(0, pos);
			content.add(name);
			pos = name.bottom() + 1;

			String d = s.desc();
			if (s.cooldown(Math.max(1, rank)) > 0) d += " " + Messages.get(this, "cooldown", s.cooldown(Math.max(1, rank)));
			if (!s.implemented) d += " _" + Messages.get(this, "coming_soon") + "_";
			RenderedTextBlock desc = PixelScene.renderTextBlock(d, 6);
			desc.maxWidth(WIDTH);
			desc.setPos(0, pos);
			content.add(desc);
			pos = desc.bottom() + 2;

			if (s.implemented && rank < s.maxRank()) {
				String why = null;
				if (s.kind == Skill.Kind.ULTIMATE && Proficiency.level(cls, f) < Skill.ULTIMATE_LEVEL) {
					why = Messages.get(this, "need_level", Skill.ULTIMATE_LEVEL);
				}
				RedButton learn = new RedButton(why != null ? why : Messages.get(this, "learn"), 6) {
					@Override
					protected void onClick() {
						Proficiency.learn(cls, s);
						hide();
						reopen(cls, f);
						GameScene.updateSkillBar();
					}
				};
				learn.enable(Proficiency.canLearn(cls, s));
				learn.setRect(0, pos, WIDTH, 14);
				content.add(learn);
				buttons.add(learn);
				buttonSkills.add(s);
				pos = learn.bottom() + 6;
			} else {
				pos += 4;
			}
		}
		content.setSize(WIDTH, pos);

		float top = pts.bottom() + 4;
		int h = (int) Math.min(MAX_HEIGHT, top + pos);
		resize(WIDTH, h);

		ScrollPane pane = new ScrollPane(content) {
			@Override
			public void onClick(float x, float y) {
				for (int i = 0; i < buttons.size(); i++) {
					if (buttons.get(i).inside(x, y)) {
						Skill s = buttonSkills.get(i);
						if (Proficiency.canLearn(cls, s)) {
							Proficiency.learn(cls, s);
							com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.LEVELUP);
							WndSkillTree.this.hide();
							reopen(cls, f);
							GameScene.updateSkillBar();
						}
						return;
					}
				}
			}
		};
		add(pane);
		pane.setRect(0, top, WIDTH, h - top);
	}

	private static void reopen(HeroClass cls, Proficiency.Family f) {
		if (ShatteredPixelDungeon.scene() instanceof GameScene) {
			GameScene.show(new WndSkillTree(cls, f));
		} else {
			ShatteredPixelDungeon.scene().addToFront(new WndSkillTree(cls, f));
		}
	}
}
