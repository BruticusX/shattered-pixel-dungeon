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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.ItemButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//MOD: opening a chest shows what's inside. Tap an item to take it, or take everything;
//anything left behind stays on the floor where the chest was.
public class WndChest extends Window {

	private static final int COLS = 5;
	private static final int SLOT = 24;
	private static final int GAP = 2;
	private static final int WIDTH = COLS * SLOT + (COLS - 1) * GAP;

	private final Heap heap;

	public WndChest( Heap heap ) {
		super();
		this.heap = heap;
		build();
	}

	private final ArrayList<com.watabou.noosa.Gizmo> parts = new ArrayList<>();

	private <T extends com.watabou.noosa.Gizmo> T part( T g ) {
		parts.add(g);
		add(g);
		return g;
	}

	private void build() {
		for (com.watabou.noosa.Gizmo g : parts) {
			remove(g);
			g.destroy();
		}
		parts.clear();

		IconTitle title = new IconTitle(new ItemSprite(ItemSpriteSheet.CHEST), Messages.get(this, "title"));
		title.setRect(0, 0, WIDTH, 0);
		part(title);
		float y = title.bottom() + GAP * 2;

		ArrayList<Item> items = new ArrayList<>(heap.items);
		if (items.isEmpty()) {
			RenderedTextBlock empty = PixelScene.renderTextBlock(Messages.get(this, "empty"), 6);
			empty.maxWidth(WIDTH);
			empty.setPos(0, y);
			part(empty);
			y = empty.bottom() + GAP * 2;
		}
		for (int i = 0; i < items.size(); i++) {
			final Item item = items.get(i);
			ItemButton b = new ItemButton() {
				@Override
				protected void onClick() {
					if (take(item)) Dungeon.hero.spendAndNext(1f);
					refresh();
				}
				@Override
				protected boolean onLongClick() {
					GameScene.show(new WndInfoItem(item));
					return true;
				}
			};
			b.item(item);
			b.setRect((i % COLS) * (SLOT + GAP), y + (i / COLS) * (SLOT + GAP), SLOT, SLOT);
			part(b);
		}
		if (!items.isEmpty()) y += ((items.size() + COLS - 1) / COLS) * (SLOT + GAP) + GAP;

		RedButton all = new RedButton(Messages.get(this, "take_all")) {
			@Override
			protected void onClick() {
				boolean any = false;
				for (Item item : new ArrayList<>(heap.items)) {
					if (take(item)) any = true;
				}
				if (any) Dungeon.hero.spendAndNext(1f);
				refresh();
			}
		};
		all.enable(!items.isEmpty());
		all.setRect(0, y, (WIDTH - GAP) / 2f, 18);
		part(all);

		RedButton close = new RedButton(Messages.get(this, "close")) {
			@Override
			protected void onClick() {
				hide();
			}
		};
		close.setRect(all.right() + GAP, y, (WIDTH - GAP) / 2f, 18);
		part(close);

		resize(WIDTH, (int) close.bottom());
	}

	private void refresh() {
		if (heap.isEmpty() || !heap.items.iterator().hasNext()) {
			hide();
		} else {
			build();
		}
	}

	//takes one item into the hero's bag without spending time (the caller spends it once).
	//Gold, keys, dew and other special pickups use their own pickup, which handles everything.
	private boolean take( Item item ) {
		Hero hero = Dungeon.hero;
		if (hero == null || !heap.items.contains(item)) return false;
		boolean special = item instanceof Gold
				|| item instanceof com.shatteredpixel.shatteredpixeldungeon.items.keys.Key
				|| item instanceof com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
		if (special) {
			if (item.doPickUp(hero, heap.pos)) {
				heap.remove(item);
			}
			return false; //doPickUp already spent the time
		}
		if (item.collect(hero.belongings.backpack)) {
			heap.remove(item);
			GameScene.pickUp(item, heap.pos);
			Sample.INSTANCE.play(Assets.Sounds.ITEM);
			GLog.i(Messages.capitalize(Messages.get(Hero.class, "you_now_have", item.name())));
			return true;
		}
		GLog.n(Messages.capitalize(Messages.get(Hero.class, "you_cant_have", item.name())));
		return false;
	}
}
