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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.MaterialItem;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//MOD: elite minions. A rare, slightly stronger version of a normal enemy with a gold aura.
//Elites are not hostile until provoked: they wander the floor and leave the hero alone
//unless something damages them. They give double experience and drop ingots.
public class Elite extends ChampionEnemy {

	public static final float SPAWN_CHANCE = 1/25f;

	{
		color = 0xFFCC33;
		rays = 6;
	}

	public boolean provoked = false;
	private boolean boosted = false;
	private transient Emitter sparkles;

	public static void rollForElite(Mob m) {
		//always roll so the number of random calls stays the same for every mob
		float roll = Random.Float();
		if (roll >= SPAWN_CHANCE) return;
		if (m.properties().contains(Char.Property.BOSS) || m.properties().contains(Char.Property.MINIBOSS)) return;
		if (m instanceof Mimic || !m.buffs(ChampionEnemy.class).isEmpty()) return;
		make(m);
	}

	public static Elite make(Mob m) {
		Elite e = Buff.affect(m, Elite.class);
		m.state = m.WANDERING;
		return e;
	}

	public static boolean isPassive(Char ch) {
		Elite e = ch.buff(Elite.class);
		return e != null && !e.provoked;
	}

	public static void provoke(Char ch) {
		Elite e = ch.buff(Elite.class);
		if (e != null) e.provoked = true;
	}

	@Override
	public boolean attachTo(Char target) {
		if (super.attachTo(target)) {
			if (!boosted) {
				target.HT = Math.round(target.HT * 1.3f);
				target.HP = target.HT;
				boosted = true;
			}
			return true;
		}
		return false;
	}

	@Override
	public float meleeDamageFactor() {
		return 1.15f;
	}

	@Override
	public void fx(boolean on) {
		super.fx(on);
		if (on) {
			if (sparkles == null || !sparkles.alive) {
				sparkles = target.sprite.emitter();
				sparkles.pour(Speck.factory(Speck.STAR), 0.6f);
			}
		} else if (sparkles != null) {
			sparkles.on = false;
			sparkles = null;
		}
	}

	//1-2 materials, chosen for the current depth; obsidian never drops from elites
	public static void dropLoot(Mob m) {
		int count = Random.Int(2) == 0 ? 1 : 2;
		for (int i = 0; i < count; i++) {
			if (Random.Float() < 0.75f) {
				Dungeon.level.drop(MaterialItem.randomFor(com.shatteredpixel.shatteredpixeldungeon.items.Material.Kind.METAL, Dungeon.depth), m.pos).sprite.drop();
			} else {
				Dungeon.level.drop(MaterialItem.randomAny(Dungeon.depth), m.pos).sprite.drop();
			}
		}
	}

	public static String prefix(Char ch, String name) {
		if (ch.buff(Elite.class) == null) return name;
		return Messages.get(Elite.class, "prefix", name);
	}

	@Override
	public String desc() {
		return Messages.get(this, provoked ? "desc_provoked" : "desc");
	}

	private static final String PROVOKED = "provoked";
	private static final String BOOSTED = "boosted";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(PROVOKED, provoked);
		bundle.put(BOOSTED, boosted);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		provoked = bundle.getBoolean(PROVOKED);
		boosted = bundle.getBoolean(BOOSTED);
	}
}
