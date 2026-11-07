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
		FACTORIES.put(Blazing.class,    ElementalVfx.factory(ElementalVfx.FIRE, 3.9f));
		FACTORIES.put(Chilling.class,   ElementalVfx.factory(ElementalVfx.FROST, 3.3f));
		FACTORIES.put(Shocking.class,   ElementalVfx.factory(ElementalVfx.LIGHTNING, 3.9f));
		FACTORIES.put(Kinetic.class,    ElementalVfx.factory(ElementalVfx.GLINT, 3.3f));
		FACTORIES.put(Venomous.class,   ElementalVfx.factory(ElementalVfx.POISON, 2.8f));
		FACTORIES.put(Vampiric.class,   ElementalVfx.factory(ElementalVfx.BLOOD, 2.8f));
		FACTORIES.put(Vorpal.class,     ElementalVfx.factory(ElementalVfx.BLOOD, 2.8f));
		FACTORIES.put(Grim.class,       ElementalVfx.factory(ElementalVfx.SMOKE, 3.9f));
		FACTORIES.put(Eldritch.class,   ElementalVfx.factory(ElementalVfx.SMOKE, 3.9f));
		FACTORIES.put(Corrupting.class, ElementalVfx.factory(ElementalVfx.ARCANE, 3.3f));
		FACTORIES.put(Blooming.class,   LeafParticle.GENERAL);
		FACTORIES.put(Lucky.class,      ElementalVfx.factory(ElementalVfx.GLINT, 3.3f));
		FACTORIES.put(Blocking.class,   ElementalVfx.factory(ElementalVfx.GLINT, 3.3f));
		FACTORIES.put(Elastic.class,    Speck.factory(Speck.BUBBLE));
		FACTORIES.put(Projecting.class, ElementalVfx.factory(ElementalVfx.GLINT, 3.9f));
		FACTORIES.put(Unstable.class,   RainbowParticle.BURST);
		FACTORIES.put(Crystal.class,    ElementalVfx.factory(ElementalVfx.FROST, 2.8f));
		FACTORIES.put(com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Sundering.class, Speck.factory(Speck.ROCK)); //MOD
		FACTORIES.put(com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Frenzied.class,  ElementalVfx.factory(ElementalVfx.FIRE, 3.9f)); //MOD
		FACTORIES.put(com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Enduring.class,  ElementalVfx.factory(ElementalVfx.GLINT, 3.3f)); //MOD

		//armor glyphs
		FACTORIES.put(Obfuscation.class, ElementalVfx.factory(ElementalVfx.SMOKE, 3.9f));
		FACTORIES.put(Swiftness.class,   ElementalVfx.factory(ElementalVfx.GLINT, 2.8f));
		FACTORIES.put(Viscosity.class,   ElementalVfx.factory(ElementalVfx.ARCANE, 3.3f));
		FACTORIES.put(Potential.class,   ElementalVfx.factory(ElementalVfx.LIGHTNING, 3.9f));
		FACTORIES.put(Affection.class,   Speck.factory(Speck.HEART));
		FACTORIES.put(AntiMagic.class,   ElementalVfx.factory(ElementalVfx.GLINT, 3.3f));
		FACTORIES.put(Brimstone.class,   ElementalVfx.factory(ElementalVfx.FIRE, 3.9f));
		FACTORIES.put(Camouflage.class,  LeafParticle.GENERAL);
		FACTORIES.put(Entanglement.class,EarthParticle.SMALL);
		FACTORIES.put(Flow.class,        Speck.factory(Speck.BUBBLE));
		FACTORIES.put(Repulsion.class,   ElementalVfx.factory(ElementalVfx.GLINT, 3.3f));
		FACTORIES.put(Stone.class,       EarthParticle.SMALL);
		FACTORIES.put(Thorns.class,      ElementalVfx.factory(ElementalVfx.BLOOD, 2.8f));
	}

	private static Factory factoryFor(Object effect, boolean curse) {
		if (curse) return ElementalVfx.factory(ElementalVfx.SMOKE, 3.9f);
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

	//world size of one item pixel on the sprite this aura follows
	public float unit() {
		return target == null ? 1f : Math.abs(target.scale.x);
	}

	@Override
	protected void emit(int index) {
		if (target == null) {
			super.emit(index);
			return;
		}
		//spawn on a random blade/outline pixel of the sprite the aura is attached to, following the
		//sprite's flip and rotation (a weapon held in a hero's hand is turned to the swing)
		int p = pixels[Random.Int(pixels.length)];
		float lx = (p % 16) + Random.Float(), ly = (p / 16) + Random.Float();
		float ox = 0, oy = 0, ang = 0;
		if (target instanceof com.watabou.noosa.Image) {
			com.watabou.noosa.Image im = (com.watabou.noosa.Image) target;
			if (im.flipHorizontal) lx = im.width - lx;
			ox = im.origin.x; oy = im.origin.y; ang = im.angle;
		}
		float dx = (lx - ox) * target.scale.x, dy = (ly - oy) * target.scale.y;
		double a = Math.toRadians(ang);
		float rx = (float)(dx * Math.cos(a) - dy * Math.sin(a));
		float ry = (float)(dx * Math.sin(a) + dy * Math.cos(a));
		factory.emit(this, index, target.x + ox + rx, target.y + oy + ry);
	}
}
