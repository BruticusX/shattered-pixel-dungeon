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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

//MOD: a stationary, indestructible crafting object (the furnace and the anvil).
public abstract class ForgeStation extends NPC {

	{
		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
	}

	@Override
	protected boolean act() {
		spend(TICK);
		return true;
	}

	@Override
	public int defenseSkill(Char enemy) {
		return INFINITE_EVASION;
	}

	@Override
	public void damage(int dmg, Object src) {
		//indestructible
	}

	@Override
	public boolean add(Buff buff) {
		return false;
	}

	@Override
	public boolean reset() {
		return true;
	}

	@Override
	public boolean interact(Char c) {
		if (c != Dungeon.hero) return true;
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				openMenu();
			}
		});
		return true;
	}

	protected abstract void openMenu();

	protected static void select(WndBag.ItemSelector selector) {
		GameScene.selectItem(selector);
	}

	//puts a furnace and an anvil on open floor next to every alchemy pot on the level
	public static void placeAll(Level level) {
		for (int cell = 0; cell < level.length(); cell++) {
			if (level.map[cell] != Terrain.ALCHEMY) continue;
			int placed = 0;
			for (int i : PathFinder.NEIGHBOURS8) {
				if (placed >= 2) break;
				int n = cell + i;
				if (n < 0 || n >= level.length()) continue;
				if (!level.passable[n] || level.findMob(n) != null || level.heaps.get(n) != null
						|| level.traps.get(n) != null || level.map[n] == Terrain.ENTRANCE
						|| level.map[n] == Terrain.EXIT || level.map[n] == Terrain.DOOR
						|| level.map[n] == Terrain.OPEN_DOOR || level.map[n] == Terrain.ALCHEMY) continue;
				ForgeStation s = placed == 0 ? new Furnace() : new Anvil();
				s.pos = n;
				level.mobs.add(s);
				placed++;
			}
		}
	}
}
