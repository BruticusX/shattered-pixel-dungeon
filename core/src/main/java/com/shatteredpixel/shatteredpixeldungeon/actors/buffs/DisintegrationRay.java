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
 */package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.DisintBeam;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellBurst;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagicTome;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//MOD: the battlemage's innate powers, no tome needed: the disintegration ray (an ability button with a
//cooldown) and the always-on disintegration aura
public class DisintegrationRay extends Buff implements ActionIndicator.Action {

	public static final float COOLDOWN = 20f;

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	public float cooldown = 0;

	//grows with the hero: 0 at level 1, +1 every 3 levels
	public static int power( Hero hero ){
		return Math.max( 0, (hero.lvl - 1) / 3 );
	}

	public static int minDamage( Hero hero ){ return 6 + 2 * power( hero ); }
	public static int maxDamage( Hero hero ){ return 14 + 5 * power( hero ); }
	public static int range( Hero hero ){ return 6 + power( hero ) / 2; }

	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo( target )) {
			if (cooldown <= 0) ActionIndicator.setAction( this );
			return true;
		}
		return false;
	}

	@Override
	public void detach() {
		super.detach();
		ActionIndicator.clearAction( this );
	}

	@Override
	public boolean act() {
		if (!(target instanceof Hero) || ((Hero) target).subClass != HeroSubClass.BATTLEMAGE) {
			detach();
			return true;
		}
		Hero hero = (Hero) target;
		//the aura is always on for a battlemage
		MagicTome.DisintAura aura = hero.buff( MagicTome.DisintAura.class );
		if (aura == null) aura = Buff.affect( hero, MagicTome.DisintAura.class );
		aura.power = power( hero );

		if (cooldown > 0) {
			cooldown -= hero.buff( Recharging.class ) != null ? 2f : 1f;
			if (cooldown <= 0) {
				cooldown = 0;
				ActionIndicator.setAction( this );
			}
		}
		spend( TICK );
		return true;
	}

	// ---- the ability button ----

	@Override
	public String actionName() {
		return Messages.get( this, "action" );
	}

	@Override
	public Visual primaryVisual() {
		Image ic = new Image( "effects/disint_flare.png" );
		ic.scale.set( 16f / Math.max( ic.width, ic.height ) );
		return ic;
	}

	@Override
	public int indicatorColor() {
		return 0x8A2BE2;
	}

	@Override
	public void doAction() {
		if (cooldown > 0) return;
		GameScene.selectCell( new CellSelector.Listener() {
			@Override
			public void onSelect( Integer cell ) {
				if (cell == null || cell == target.pos) return;
				fire( (Hero) target, cell );
			}
			@Override
			public String prompt() {
				return Messages.get( DisintegrationRay.class, "prompt" );
			}
		} );
	}

	private void fire( Hero hero, int cell ){
		Ballistica beam = new Ballistica( hero.pos, cell, Ballistica.WONT_STOP );
		int dist = Math.min( beam.dist, range( hero ) );
		if (dist < 1) return;
		ArrayList<Char> chars = new ArrayList<>();
		for (int c : beam.subPath( 1, dist )) {
			Char ch = Actor.findChar( c );
			if (ch != null && ch != hero) chars.add( ch );
			CellEmitter.center( c ).burst( PurpleParticle.BURST, 6 );
		}
		hero.sprite.zap( cell );
		com.watabou.utils.PointF from = hero.sprite.center();
		com.watabou.utils.PointF to = DungeonTilemap.raisedTileCenterToWorld( beam.path.get( dist ) );
		hero.sprite.parent.add( new DisintBeam( from, to ) );
		hero.sprite.parent.add( new SpellBurst( "effects/disint_flare.png", from, 14f ) );
		hero.sprite.parent.add( new SpellBurst( "effects/disint_impact.png", to, 26f ) );
		Camera.main.shake( 2.5f, 0.35f );
		Sample.INSTANCE.play( Assets.Sounds.RAY, 1f, 0.7f );
		Sample.INSTANCE.play( Assets.Sounds.BLAST, 0.6f, 1.2f );

		for (Char ch : chars) {
			if (!ch.isAlive()) continue;
			//10% to be disintegrated outright (never bosses or minibosses)
			if (ch instanceof Mob && Random.Int( 10 ) == 0 && !ch.properties().contains( Char.Property.BOSS )
					&& !ch.properties().contains( Char.Property.MINIBOSS )) {
				if (ch.sprite != null) ch.sprite.emitter().burst( PurpleParticle.BURST, 25 );
				GLog.p( Messages.get( DisintegrationRay.class, "disintegrated", ch.name() ) );
				ch.die( this );
			} else {
				ch.damage( Random.NormalIntRange( minDamage( hero ), maxDamage( hero ) ), this );
				if (ch.sprite != null) ch.sprite.centerEmitter().burst( PurpleParticle.BURST, 10 );
			}
		}
		Dungeon.observe();
		cooldown = COOLDOWN;
		ActionIndicator.clearAction( this );
		hero.spendAndNext( 1f );
	}

	// ---- buff info ----

	@Override
	public int icon() {
		return cooldown > 0 ? com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.TIME : com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.NONE;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight( 0.6f, 0.2f, 1f );
	}

	@Override
	public float iconFadePercent() {
		return Math.max( 0, cooldown / COOLDOWN );
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString( (int) Math.ceil( cooldown ) );
	}

	@Override
	public String desc() {
		Hero hero = (Hero) target;
		return Messages.get( this, "desc", minDamage( hero ), maxDamage( hero ), range( hero ), (int) Math.ceil( cooldown ) );
	}

	private static final String COOLDOWN_KEY = "cooldown";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( COOLDOWN_KEY, cooldown );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		cooldown = bundle.getFloat( COOLDOWN_KEY );
	}
}
