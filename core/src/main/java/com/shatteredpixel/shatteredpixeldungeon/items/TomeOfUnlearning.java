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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Proficiency;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//MOD: refunds every skill point spent in one weapon family, so it can be re-spent
public class TomeOfUnlearning extends Item {

	private static final String AC_READ = "READ";

	{
		image = ItemSpriteSheet.TOME_UNLEARNING;
		stackable = true;
		defaultAction = AC_READ;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_READ);
		return actions;
	}

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		if (!action.equals(AC_READ)) return;

		final ArrayList<Proficiency.Family> spent = new ArrayList<>();
		for (Proficiency.Family f : Proficiency.Family.values()) {
			if (Proficiency.pointsSpent(hero.heroClass, f) > 0) spent.add(f);
		}
		if (spent.isEmpty()) {
			GLog.w(Messages.get(this, "nothing"));
			return;
		}
		String[] names = new String[spent.size()];
		for (int i = 0; i < names.length; i++) {
			names[i] = Messages.get(this, "option", Messages.titleCase(spent.get(i).title()),
					Proficiency.pointsSpent(hero.heroClass, spent.get(i)));
		}
		final TomeOfUnlearning tome = this;
		GameScene.show(new WndOptions(Messages.titleCase(name()), Messages.get(this, "choose"), names) {
			@Override
			protected void onSelect(int index) {
				Proficiency.Family f = spent.get(index);
				Proficiency.unlearn(hero.heroClass, f);
				tome.detach(hero.belongings.backpack);
				hero.sprite.operate(hero.pos);
				hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 8);
				Sample.INSTANCE.play(Assets.Sounds.READ);
				hero.spendAndNext(1f);
				GameScene.updateSkillBar();
				GLog.p(Messages.get(TomeOfUnlearning.class, "unlearned", f.title()));
			}
		});
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
		return 150 * quantity;
	}
}
