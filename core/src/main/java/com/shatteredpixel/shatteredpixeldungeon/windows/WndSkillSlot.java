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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Proficiency;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Skill;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;

import java.util.ArrayList;

//MOD: choose which learned active skill goes in a skill button
public class WndSkillSlot extends WndOptions {

	private final int index;
	private final ArrayList<Skill> options;

	public WndSkillSlot(int index) {
		this(index, learnedActives(Dungeon.hero.heroClass));
	}

	private WndSkillSlot(int index, ArrayList<Skill> options) {
		super(Messages.get(WndSkillSlot.class, "title"),
				options.isEmpty() ? Messages.get(WndSkillSlot.class, "none") : Messages.get(WndSkillSlot.class, "message"),
				labels(options));
		this.index = index;
		this.options = options;
	}

	private static ArrayList<Skill> learnedActives(HeroClass cls) {
		ArrayList<Skill> list = new ArrayList<>();
		for (Skill s : Skill.values()) {
			if (s.usable() && Proficiency.rank(cls, s) > 0) list.add(s);
		}
		return list;
	}

	private static String[] labels(ArrayList<Skill> options) {
		String[] l = new String[options.size() + 1];
		for (int i = 0; i < options.size(); i++) {
			l[i] = Messages.titleCase(options.get(i).title()) + " (" + Messages.titleCase(options.get(i).family.title()) + ")";
		}
		l[options.size()] = Messages.get(WndSkillSlot.class, "clear");
		return l;
	}

	@Override
	protected void onSelect(int i) {
		HeroClass cls = Dungeon.hero.heroClass;
		if (i < options.size()) Proficiency.setSlot(cls, index, options.get(i));
		else Proficiency.setSlot(cls, index, null);
		GameScene.updateSkillBar();
	}
}
