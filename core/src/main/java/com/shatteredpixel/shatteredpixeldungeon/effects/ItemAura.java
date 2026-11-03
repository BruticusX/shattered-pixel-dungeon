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

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.EarthParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.EnergyParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PoisonParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.RainbowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Affection;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Brimstone;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Camouflage;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Entanglement;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Flow;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Obfuscation;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Potential;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Repulsion;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Stone;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Swiftness;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Thorns;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blooming;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Corrupting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Crystal;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Elastic;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Eldritch;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Grim;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Unstable;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vampiric;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Venomous;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vorpal;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

import java.util.HashMap;

//MOD: particle auras for enchanted weapons (along the blade) and glyphed armor (around the outline).
//Replaces the plain colour pulse that enchantments used to show on item sprites.
public class ItemAura extends Emitter {

	private static final HashMap<Class<?>, Factory> FACTORIES = new HashMap<>();
	static {
		//weapon enchantments
		FACTORIES.put(Blazing.class,    FlameParticle.FACTORY);
		FACTORIES.put(Chilling.class,   SnowParticle.FACTORY);
		FACTORIES.put(Shocking.class,   SparkParticle.FACTORY);
		FACTORIES.put(Kinetic.class,    Speck.factory(Speck.LIGHT));
		FACTORIES.put(Venomous.class,   PoisonParticle.MISSILE);
		FACTORIES.put(Vampiric.class,   BloodParticle.FACTORY);
		FACTORIES.put(Vorpal.class,     BloodParticle.FACTORY);
		FACTORIES.put(Grim.class,       ShadowParticle.UP);
		FACTORIES.put(Eldritch.class,   ShadowParticle.MISSILE);
		FACTORIES.put(Corrupting.class, PurpleParticle.MISSILE);
		FACTORIES.put(Blooming.class,   LeafParticle.GENERAL);
		FACTORIES.put(Lucky.class,      Speck.factory(Speck.YELLOW_LIGHT));
		FACTORIES.put(Blocking.class,   Speck.factory(Speck.BLUE_LIGHT));
		FACTORIES.put(Elastic.class,    Speck.factory(Speck.BUBBLE));
		FACTORIES.put(Projecting.class, Speck.factory(Speck.STAR));
		FACTORIES.put(Unstable.class,   RainbowParticle.BURST);
		FACTORIES.put(Crystal.class,    Speck.factory(Speck.DISCOVER));

		//armor glyphs
		FACTORIES.put(Obfuscation.class, SmokeParticle.FACTORY);
		FACTORIES.put(Swiftness.class,   Speck.factory(Speck.LIGHT));
		FACTORIES.put(Viscosity.class,   PurpleParticle.MISSILE);
		FACTORIES.put(Potential.class,   EnergyParticle.FACTORY);
		FACTORIES.put(Affection.class,   Speck.factory(Speck.HEART));
		FACTORIES.put(AntiMagic.class,   Speck.factory(Speck.BLUE_LIGHT));
		FACTORIES.put(Brimstone.class,   FlameParticle.FACTORY);
		FACTORIES.put(Camouflage.class,  LeafParticle.GENERAL);
		FACTORIES.put(Entanglement.class,EarthParticle.SMALL);
		FACTORIES.put(Flow.class,        Speck.factory(Speck.BUBBLE));
		FACTORIES.put(Repulsion.class,   Speck.factory(Speck.LIGHT));
		FACTORIES.put(Stone.class,       EarthParticle.SMALL);
		FACTORIES.put(Thorns.class,      BloodParticle.FACTORY);
	}

	private static Factory factoryFor(Object effect, boolean curse) {
		if (curse) return ShadowParticle.CURSE;
		Factory f = FACTORIES.get(effect.getClass());
		return f != null ? f : Speck.factory(Speck.LIGHT);
	}

	//blade aura for an enchanted weapon sprite
	public static Emitter weapon(int image, Object enchantment, boolean curse) {
		return make(AuraMaps.blade(image), factoryFor(enchantment, curse), curse ? 0.3f : 0.2f);
	}

	//outline aura for a glyphed armor sprite
	public static Emitter armor(int image, Object glyph, boolean curse) {
		return make(AuraMaps.outline(image), factoryFor(glyph, curse), curse ? 0.3f : 0.25f);
	}

	private static Emitter make(short[] pixels, Factory factory, float interval) {
		if (pixels == null || pixels.length == 0 || factory == null) return null;
		ItemAura aura = new ItemAura(pixels);
		aura.fillTarget = false;
		aura.pour(factory, interval);
		return aura;
	}

	private final short[] pixels;

	private ItemAura(short[] pixels) {
		this.pixels = pixels;
	}

	@Override
	protected void emit(int index) {
		if (target == null) {
			super.emit(index);
			return;
		}
		//spawn on a random blade/outline pixel of the sprite the aura is attached to
		int p = pixels[Random.Int(pixels.length)];
		float px = target.x + ((p % 16) + Random.Float()) * target.scale.x;
		float py = target.y + ((p / 16) + Random.Float()) * target.scale.y;
		factory.emit(this, index, px, py);
	}
}
