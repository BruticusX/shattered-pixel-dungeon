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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Proficiency;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Skill;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.SkillEffects;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSkillSlot;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.ui.Component;

//MOD: the active weapon skill buttons (2, a third at hero level 20), stacked above the toolbar.
//Tap to use; tap an empty slot or long-press a full one to choose which skill goes there.
public class SkillBar extends Component {

	public static final int SIZE = 20;

	private SkillSlot[] slots;

	@Override
	protected void createChildren() {
		slots = new SkillSlot[Proficiency.MAX_SLOTS];
		for (int i = 0; i < slots.length; i++) {
			slots[i] = new SkillSlot(i);
			add(slots[i]);
		}
	}

	@Override
	protected void layout() {
		float y = this.y + height;
		for (SkillSlot s : slots) {
			if (!s.visible) continue;
			y -= SIZE;
			s.setRect(x, y, SIZE, SIZE);
			y -= 2;
		}
	}

	public void refresh() {
		Hero hero = Dungeon.hero;
		if (hero == null) return;
		for (int i = 0; i < slots.length; i++) {
			slots[i].visible = slots[i].active = i < Proficiency.slotsUnlocked(hero);
			slots[i].refresh();
		}
		layout();
	}

	public static int familyIcon(Proficiency.Family f) {
		switch (f) {
			case DAGGERS:  return ItemSpriteSheet.DAGGER;
			case SWORDS: default: return ItemSpriteSheet.SWORD;
			case AXES:     return ItemSpriteSheet.HAND_AXE;
			case BLUNT:    return ItemSpriteSheet.MACE;
			case POLEARMS: return ItemSpriteSheet.SPEAR;
			case STAVES:   return ItemSpriteSheet.QUARTERSTAFF;
			case FIST:     return ItemSpriteSheet.GLOVES;
			case SHIELDS:  return ItemSpriteSheet.ROUND_SHIELD;
			case FLEXIBLE: return ItemSpriteSheet.WHIP;
			case RANGED:   return ItemSpriteSheet.CROSSBOW;
			case WANDS:    return ItemSpriteSheet.WAND_MAGIC_MISSILE;
		}
	}

	private static class SkillSlot extends Button {

		private final int index;
		private ColorBlock bg, border;
		private ItemSprite icon;
		private BitmapText label;

		SkillSlot(int index) {
			super();
			this.index = index;
		}

		@Override
		protected void createChildren() {
			super.createChildren();
			border = new ColorBlock(1, 1, 0xFF6A5A3A);
			add(border);
			bg = new ColorBlock(1, 1, 0xCC1E1A16);
			add(bg);
			label = new BitmapText(PixelScene.pixelFont);
			add(label);
		}

		private Skill skill() {
			return Dungeon.hero == null ? null : Proficiency.slot(Dungeon.hero.heroClass, index);
		}

		void refresh() {
			if (icon != null) { remove(icon); icon.destroy(); icon = null; }
			Skill s = skill();
			Hero hero = Dungeon.hero;
			if (s != null && hero != null) {
				icon = new ItemSprite(familyIcon(s.family));
				addToBack(icon);
				bringToFront(label);
				int cd = SkillEffects.Cooldowns.left(hero, s);
				boolean usable = SkillEffects.unusableReason(hero, s) == null;
				icon.alpha(usable ? 1f : 0.4f);
				label.text(cd > 0 ? Integer.toString(cd) : "");
				label.hardlight(0xFFFFFF);
				border.color(s.kind == Skill.Kind.ULTIMATE ? 0xFFCC9933 : 0xFF6A5A3A);
			} else {
				label.text("+");
				label.hardlight(0x888888);
				border.color(0xFF6A5A3A);
			}
			label.measure();
			sendToBack(bg);
			sendToBack(border);
			layout();
		}

		@Override
		protected void layout() {
			super.layout();
			if (border == null) return;
			border.x = x; border.y = y; border.size(width, height);
			bg.x = x + 1; bg.y = y + 1; bg.size(width - 2, height - 2);
			if (icon != null) {
				icon.x = x + (width - icon.width()) / 2f;
				icon.y = y + (height - icon.height()) / 2f;
				PixelScene.align(icon);
			}
			label.x = x + (width - label.width()) / 2f;
			label.y = y + (height - label.baseLine()) / 2f;
			PixelScene.align(label);
		}

		@Override
		protected void onClick() {
			Hero hero = Dungeon.hero;
			if (hero == null) return;
			Skill s = skill();
			if (s == null) {
				GameScene.show(new WndSkillSlot(index));
			} else {
				SkillEffects.use(hero, s);
			}
		}

		@Override
		protected boolean onLongClick() {
			GameScene.show(new WndSkillSlot(index));
			return true;
		}
	}
}
