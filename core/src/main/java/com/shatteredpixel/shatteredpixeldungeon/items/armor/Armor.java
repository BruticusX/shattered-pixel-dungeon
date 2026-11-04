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

package com.shatteredpixel.shatteredpixeldungeon.items.armor;

import com.shatteredpixel.shatteredpixeldungeon.items.Material;
import com.shatteredpixel.shatteredpixeldungeon.effects.ItemAura;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Momentum;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.AuraOfProtection;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BodyForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LifeLinkSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.PrismaticImage;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.AntiEntropy;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Bulk;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Displacement;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Metabolism;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Multiplicity;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Overgrowth;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Stench;
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
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ParchmentScrap;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;

public class Armor extends EquipableItem {

	protected static final String AC_DETACH       = "DETACH";
	
	public enum Augment {
		EVASION (2f , -1f),
		DEFENSE (-2f, 1f),
		NONE	(0f   ,  0f);
		
		private float evasionFactor;
		private float defenceFactor;
		
		Augment(float eva, float df){
			evasionFactor = eva;
			defenceFactor = df;
		}
		
		public int evasionFactor(int level){
			return Math.round((2 + level) * evasionFactor);
		}
		
		public int defenseFactor(int level){
			return Math.round((2 + level) * defenceFactor);
		}
	}
	
	public Augment augment = Augment.NONE;
	
	public Glyph glyph;
	public boolean glyphHardened = false;
	public boolean curseInfusionBonus = false;
	public boolean masteryPotionBonus = false;
	
	protected BrokenSeal seal;
	
	public int tier;
	
	private static final int USES_TO_ID = 10;
	private float usesLeftToID = USES_TO_ID;
	private float availableUsesToID = USES_TO_ID/2f;
	
	public Armor( int tier ) {
		this.tier = tier;
	}
	
	private static final String USES_LEFT_TO_ID = "uses_left_to_id";
	private static final String AVAILABLE_USES  = "available_uses";
	private static final String GLYPH			= "glyph";
	private static final String GLYPH_HARDENED	= "glyph_hardened";
	private static final String CURSE_INFUSION_BONUS = "curse_infusion_bonus";
	private static final String MASTERY_POTION_BONUS = "mastery_potion_bonus";
	private static final String SEAL            = "seal";
	private static final String AUGMENT			= "augment";

	//MOD: ---- materials and condition (durability) ----
	public Material material = null;
	public float condition = 100f;

	public Material.Kind materialKind() {
		if (this instanceof ClothArmor)   return Material.Kind.CLOTH;
		if (this instanceof LeatherArmor) return Material.Kind.LEATHER;
		if (this instanceof MailArmor || this instanceof ScaleArmor || this instanceof PlateArmor) return Material.Kind.METAL;
		return null; //class armor etc. keep vanilla stats
	}

	public Material material() {
		if (material == null && materialKind() != null) material = Material.lowest(materialKind());
		return material;
	}

	public void rollMaterial() {
		//MOD (1b): all found gear is the base material; better materials come from the anvil
		if (materialKind() != null) material = Material.lowest(materialKind());
	}

	//MOD: the armor system. Each armor type has a real-world weight which sets its base block
	// and its evasion/stealth penalty; the material multiplies the block (Material.armorMult).
	public float armorKg() {
		if (this instanceof ClothArmor)   return 3f;
		if (this instanceof LeatherArmor) return 6f;
		if (this instanceof MailArmor)    return 12f;
		if (this instanceof ScaleArmor)   return 15f;
		if (this instanceof PlateArmor)   return 25f;
		return 0f;
	}

	//block at +0 in the base material: about 1.6 x kg^0.6
	public int baseBlock() {
		return Math.round(1.6f * (float) Math.pow(armorKg(), 0.6));
	}

	//block added per level, before the material multiplier
	public int levelBlock() {
		if (this instanceof PlateArmor) return 3;
		if (this instanceof MailArmor || this instanceof ScaleArmor) return 2;
		return 1;
	}

	//evasion (and stealth) lost to the armor's weight: kg / 80 for metal armor.
	//leather has no penalty, and cloth is a bonus: +5% evasion and a little quieter
	public float weightPenalty() {
		if (material() == null) return 0f;
		if (this instanceof ClothArmor)   return -0.05f;
		if (this instanceof LeatherArmor) return 0f;
		return armorKg() / 80f;
	}

	public String weightLabel() {
		if (material() == null) return Messages.get(Armor.class, "tier_label", tier);
		float p = weightPenalty();
		if (this instanceof ClothArmor)   return Messages.get(Armor.class, "label_light");
		if (this instanceof LeatherArmor) return Messages.get(Armor.class, "label_medium");
		if (p <= 0.05f) return Messages.get(Armor.class, "label_light");
		if (p <= 0.1f)  return Messages.get(Armor.class, "label_medium");
		if (p <= 0.2f)  return Messages.get(Armor.class, "label_heavy");
		return Messages.get(Armor.class, "label_very_heavy");
	}

	public float magicResist() {
		Material m = material();
		return (m == null || broken()) ? 0f : m.magicResist;
	}

	public boolean usesCondition() {
		return material() != null;
	}

	public boolean broken() {
		return usesCondition() && condition <= 0;
	}

	public void wear(float amount) {
		if (!usesCondition() || amount <= 0) return;
		float before = condition;
		condition = Math.max(0, condition - amount);
		if (before > 0 && condition <= 0) {
			GLog.n(Messages.get(Armor.class, "now_broken", name()));
		} else if (before > 10 && condition <= 10) {
			GLog.w(Messages.get(Armor.class, "almost_broken", name()));
		} else if (before > 25 && condition <= 25) {
			GLog.w(Messages.get(Armor.class, "wearing_down", name()));
		}
		updateQuickslot();
	}

	public void repair(float amount) {
		condition = Math.min(100f, condition + amount);
		updateQuickslot();
	}

	protected String materialName(String name) {
		Material m = material();
		return m == null ? name : Messages.get(Armor.class, "material_name", m.title(), name);
	}

	@Override
	public String spriteSheet() {
		return material() != null ? material().spriteSheet() : super.spriteSheet();
	}

	private static final String MATERIAL  = "material";
	private static final String CONDITION = "condition";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		if (material != null) bundle.put( MATERIAL, material.name() );
		bundle.put( CONDITION, condition );
		bundle.put( USES_LEFT_TO_ID, usesLeftToID );
		bundle.put( AVAILABLE_USES, availableUsesToID );
		bundle.put( GLYPH, glyph );
		bundle.put( GLYPH_HARDENED, glyphHardened );
		bundle.put( CURSE_INFUSION_BONUS, curseInfusionBonus );
		bundle.put( MASTERY_POTION_BONUS, masteryPotionBonus );
		bundle.put( SEAL, seal);
		bundle.put( AUGMENT, augment);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		usesLeftToID = bundle.getInt( USES_LEFT_TO_ID );
		availableUsesToID = bundle.getInt( AVAILABLE_USES );
		inscribe((Glyph) bundle.get(GLYPH));
		glyphHardened = bundle.getBoolean(GLYPH_HARDENED);
		curseInfusionBonus = bundle.getBoolean( CURSE_INFUSION_BONUS );
		masteryPotionBonus = bundle.getBoolean( MASTERY_POTION_BONUS );
		seal = (BrokenSeal)bundle.get(SEAL);
		
		augment = bundle.getEnum(AUGMENT, Augment.class);

		if (bundle.contains(MATERIAL)) material = Material.fromName(bundle.getString(MATERIAL), null);
		if (bundle.contains(CONDITION)) condition = bundle.getFloat(CONDITION);
	}

	@Override
	public boolean isSimilar( Item item ) {
		return super.isSimilar(item) && (!(item instanceof Armor) || ((Armor) item).material() == material());
	}

	@Override
	public void reset() {
		super.reset();
		usesLeftToID = USES_TO_ID;
		availableUsesToID = USES_TO_ID/2f;
		//armor can be kept in bones between runs, the seal cannot.
		seal = null;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (seal != null) actions.add(AC_DETACH);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_DETACH) && seal != null){
			BrokenSeal detaching = detachSeal();
			GLog.i( Messages.get(Armor.class, "detach_seal") );
			hero.sprite.operate(hero.pos);
			if (!detaching.collect()){
				Dungeon.level.drop(detaching, hero.pos);
			}
			updateQuickslot();
		}
	}

	@Override
	public boolean collect(Bag container) {
		if(super.collect(container)){
			if (Dungeon.hero != null && Dungeon.hero.isAlive() && isIdentified() && glyph != null){
				Catalog.setSeen(glyph.getClass());
				Statistics.itemTypesDiscovered.add(glyph.getClass());
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Item identify(boolean byHero) {
		if (glyph != null && byHero && Dungeon.hero != null && Dungeon.hero.isAlive()){
			Catalog.setSeen(glyph.getClass());
			Statistics.itemTypesDiscovered.add(glyph.getClass());
		}
		return super.identify(byHero);
	}

	public void setIDReady(){
		usesLeftToID = -1;
	}

	public boolean readyToIdentify(){
		return !isIdentified() && usesLeftToID <= 0;
	}

	@Override
	public boolean doEquip( Hero hero ) {

		// 15/25% chance
		if (hero.heroClass != HeroClass.CLERIC && hero.hasTalent(Talent.HOLY_INTUITION)
				&& cursed && !cursedKnown
				&& Random.Int(20) < 1 + 2*hero.pointsInTalent(Talent.HOLY_INTUITION)){
			cursedKnown = true;
			GLog.p(Messages.get(this, "curse_detected"));
			return false;
		}

		detach(hero.belongings.backpack);

		Armor oldArmor = hero.belongings.armor;
		if (hero.belongings.armor == null || hero.belongings.armor.doUnequip( hero, true, false )) {
			
			hero.belongings.armor = this;
			
			cursedKnown = true;
			if (cursed) {
				equipCursed( hero );
				GLog.n( Messages.get(Armor.class, "equip_cursed") );
			}
			
			((HeroSprite)hero.sprite).updateArmor();
			activate(hero);
			Talent.onItemEquipped(hero, this);
			hero.spend( timeToEquip( hero ) );

			if (Dungeon.hero.heroClass == HeroClass.WARRIOR && checkSeal() == null){
				BrokenSeal seal = oldArmor != null ? oldArmor.checkSeal() : null;
				if (seal != null && (!cursed || (seal.getGlyph() != null && seal.getGlyph().curse()))){

					GameScene.show(new WndOptions(new ItemSprite(ItemSpriteSheet.SEAL),
							Messages.titleCase(seal.trueName()),
							Messages.get(Armor.class, "seal_transfer"),
							Messages.get(Armor.class, "seal_transfer_yes"),
							Messages.get(Armor.class, "seal_transfer_no")){
						@Override
						protected void onSelect(int index) {
							super.onSelect(index);
							if (index == 0){
								seal.affixToArmor(Armor.this, oldArmor);
								updateQuickslot();
							}
							super.hide();
						}

						@Override
						public void hide() {
							//do nothing, must press button
						}
					});
				} else {
					hero.next();
				}
			} else {
				hero.next();
			}
			return true;
			
		} else {
			
			collect( hero.belongings.backpack );
			return false;
			
		}
	}

	@Override
	public void activate(Char ch) {
		if (seal != null) Buff.affect(ch, BrokenSeal.WarriorShield.class).setArmor(this);
	}

	public void affixSeal(BrokenSeal seal){
		this.seal = seal;
		if (seal.level() > 0){
			//doesn't trigger upgrading logic such as affecting curses/glyphs
			int newLevel = trueLevel()+1;
			level(newLevel);
			Badges.validateItemLevelAquired(this);
		}
		if (seal.getGlyph() != null){
			inscribe(seal.getGlyph());
		}
		if (isEquipped(Dungeon.hero)){
			Buff.affect(Dungeon.hero, BrokenSeal.WarriorShield.class).setArmor(this);
		}
	}

	public BrokenSeal detachSeal(){
		if (seal != null){

			if (isEquipped(Dungeon.hero)) {
				BrokenSeal.WarriorShield sealBuff = Dungeon.hero.buff(BrokenSeal.WarriorShield.class);
				if (sealBuff != null) sealBuff.setArmor(null);
			}

			BrokenSeal detaching = seal;
			seal = null;

			if (detaching.level() > 0){
				degrade();
			}
			if (detaching.canTransferGlyph()){
				inscribe(null);
			} else {
				detaching.setGlyph(null);
			}
			return detaching;
		} else {
			return null;
		}
	}

	public BrokenSeal checkSeal(){
		return seal;
	}

	@Override
	public boolean doUnequip( Hero hero, boolean collect, boolean single ) {
		if (super.doUnequip( hero, collect, single )) {

			hero.belongings.armor = null;
			((HeroSprite)hero.sprite).updateArmor();

			BrokenSeal.WarriorShield sealBuff = hero.buff(BrokenSeal.WarriorShield.class);
			if (sealBuff != null) sealBuff.setArmor(null);

			return true;

		} else {

			return false;

		}
	}
	
	@Override
	public boolean isEquipped( Hero hero ) {
		return hero != null && hero.belongings.armor() == this;
	}

	public final int DRMax(){
		return DRMax(buffedLvl());
	}

	public int DRMax(int lvl){
		if (Dungeon.isChallenged(Challenges.NO_ARMOR)){
			return 1 + tier + lvl + augment.defenseFactor(lvl);
		}

		if (broken()) return 0; //MOD: broken armor blocks nothing
		int max;
		if (material() != null) {
			//MOD: (base + level gain) x the material's multiplier
			max = Math.round((baseBlock() + lvl * levelBlock()) * material().armorMult()) + augment.defenseFactor(lvl);
		} else {
			max = tier * (2 + lvl) + augment.defenseFactor(lvl);
		}
		if (lvl > max){
			return ((lvl - max)+1)/2;
		} else {
			return max;
		}
	}

	public final int DRMin(){
		return DRMin(buffedLvl());
	}

	public int DRMin(int lvl){
		if (Dungeon.isChallenged(Challenges.NO_ARMOR) || broken()){
			return 0;
		}

		int max = DRMax(lvl);
		if (lvl >= max){
			return (lvl - max);
		} else {
			return lvl;
		}
	}

	//This exists so we can test what a char's base evasion would be without armor affecting it
	//more ugly static vars yaaay~
	public static boolean testingNoArmDefSkill = false;
	
	public float evasionFactor( Char owner, float evasion ){
		if (testingNoArmDefSkill) return evasion;
		
		if (hasGlyph(Stone.class, owner) && !Stone.testingEvasion()){
			return 0;
		}
		
		if (owner instanceof Hero){
			int aEnc = STRReq() - ((Hero) owner).STR();
			if (aEnc > 0) evasion /= Math.pow(1.5, aEnc);
			
			Momentum momentum = owner.buff(Momentum.class);
			if (momentum != null){
				//MOD: evasive armor rewards light armor instead of excess strength
				int lightness = material() == null ? 0 : Math.max(0, Math.round((25f - armorKg()) / 6f));
				evasion += momentum.evasionBonus(((Hero) owner).lvl, lightness);
			}
		}
		
		evasion *= 1f - weightPenalty(); //MOD: heavy metal armor instead of strength penalties
		return evasion + augment.evasionFactor(buffedLvl());
	}
	
	public float speedFactor( Char owner, float speed ){
		
		if (owner instanceof Hero) {
			int aEnc = STRReq() - ((Hero) owner).STR();
			if (aEnc > 0) speed /= Math.pow(1.2, aEnc);
		}
		
		return speed;
		
	}
	
	@Override
	public int level() {
		int level = super.level();
		//TODO warrior's seal upgrade should probably be considered here too
		// instead of being part of true level
		if (curseInfusionBonus) level += 1 + level/6;
		return level;
	}
	
	@Override
	public Item upgrade() {
		return upgrade( false );
	}
	
	public Item upgrade( boolean inscribe ) {

		if (inscribe){
			if (glyph == null){
				inscribe( Glyph.random() );
			}
		} else if (glyph != null) {
			//chance to lose harden buff is 10/20/40/80/100% when upgrading from +6/7/8/9/10
			if (glyphHardened) {
				if (level() >= 6 && Random.Float(10) < Math.pow(2, level()-6)){
					glyphHardened = false;
				}

			//chance to remove curse is a static 33%
			} else if (hasCurseGlyph()){
				if (Random.Int(3) == 0) inscribe(null);

			//otherwise chance to lose glyph is 10/20/40/80/100% when upgrading from +4/5/6/7/8
			} else {

				//the chance from +4/5, and then +6 can be set to 0% with metamorphed runic transference
				int lossChanceStart = 4;
				if (Dungeon.hero != null && Dungeon.hero.heroClass != HeroClass.WARRIOR && Dungeon.hero.hasTalent(Talent.RUNIC_TRANSFERENCE)){
					lossChanceStart += 1+Dungeon.hero.pointsInTalent(Talent.RUNIC_TRANSFERENCE);
				}

				if (level() >= lossChanceStart && Random.Float(10) < Math.pow(2, level()-4)) {
					inscribe(null);
				}
			}
		}
		
		cursed = false;

		if (seal != null && seal.level() == 0)
			seal.upgrade();

		return super.upgrade();
	}
	
	public int proc( Char attacker, Char defender, int damage ) {

		if (defender.buff(MagicImmune.class) == null) {
			Glyph trinityGlyph = null;
			//only when it's the hero or a char that uses the hero's armor
			if (Dungeon.hero.buff(BodyForm.BodyFormBuff.class) != null
					&& (defender == Dungeon.hero || defender instanceof PrismaticImage || defender instanceof ShadowClone.ShadowAlly)){
				trinityGlyph = Dungeon.hero.buff(BodyForm.BodyFormBuff.class).glyph();
				if (glyph != null && trinityGlyph != null && trinityGlyph.getClass() == glyph.getClass()){
					trinityGlyph = null;
				}
			}

			if (defender instanceof Hero && isEquipped((Hero) defender)
					&& defender.buff(HolyWard.HolyArmBuff.class) != null){
				if (glyph != null &&
						(((Hero) defender).subClass == HeroSubClass.PALADIN || hasCurseGlyph())){
					damage = glyph.proc( this, attacker, defender, damage );
				}
				if (trinityGlyph != null){
					damage = trinityGlyph.proc( this, attacker, defender, damage );
				}
				int blocking = ((Hero) defender).subClass == HeroSubClass.PALADIN ? 3 : 1;
				damage -= Math.round(blocking * Glyph.genericProcChanceMultiplier(defender));

			} else {
				if (glyph != null) {
					damage = glyph.proc(this, attacker, defender, damage);
				}
				if (trinityGlyph != null){
					damage = trinityGlyph.proc( this, attacker, defender, damage );
				}
				//so that this effect procs for allies using this armor via aura of protection
				if (defender.alignment == Dungeon.hero.alignment
						&& Dungeon.hero.buff(AuraOfProtection.AuraBuff.class) != null
						&& (Dungeon.level.distance(defender.pos, Dungeon.hero.pos) <= 2 || defender.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null)
						&& Dungeon.hero.buff(HolyWard.HolyArmBuff.class) != null) {
					int blocking = Dungeon.hero.subClass == HeroSubClass.PALADIN ? 3 : 1;
					damage -= Math.round(blocking * Glyph.genericProcChanceMultiplier(defender));
				}
			}
			damage = Math.max(damage, 0);
		}
		
		if (!levelKnown && defender == Dungeon.hero) {
			float uses = Math.min( availableUsesToID, Talent.itemIDSpeedFactor(Dungeon.hero, this) );
			availableUsesToID -= uses;
			usesLeftToID -= uses;
			if (usesLeftToID <= 0) {
				if (ShardOfOblivion.passiveIDDisabled()){
					if (usesLeftToID > -1){
						GLog.p(Messages.get(ShardOfOblivion.class, "identify_ready"), name());
					}
					setIDReady();
				} else {
					identify();
					GLog.p(Messages.get(Armor.class, "identify"));
					Badges.validateItemLevelAquired(this);
				}
			}
		}
		
		if (defender instanceof Hero && material() != null) wear(material().wearPerUse() * (masteryPotionBonus ? 0.5f : 1f)); //MOD
		return damage;
	}
	
	@Override
	public void onHeroGainExp(float levelPercent, Hero hero) {
		levelPercent *= Talent.itemIDSpeedFactor(hero, this);
		if (!levelKnown && isEquipped(hero) && availableUsesToID <= USES_TO_ID/2f) {
			//gains enough uses to ID over 0.5 levels
			availableUsesToID = Math.min(USES_TO_ID/2f, availableUsesToID + levelPercent * USES_TO_ID);
		}
	}
	
	@Override
	public String name() {
		if (isEquipped(Dungeon.hero) && !hasCurseGlyph() && Dungeon.hero.buff(HolyWard.HolyArmBuff.class) != null
			&& (Dungeon.hero.subClass != HeroSubClass.PALADIN || glyph == null)){
				return Messages.get(HolyWard.class, "glyph_name", materialName(super.name()));
			} else {
				return glyph != null && (cursedKnown || !glyph.curse()) ? glyph.name( materialName(super.name()) ) : materialName(super.name());

		}
	}
	
	@Override
	public String info() {
		String info = super.info();
		
		if (levelKnown) {

			info += "\n\n" + Messages.get(Armor.class, "curr_absorb", weightLabel(), DRMin(), DRMax(), STRReq());
			if (material() != null) {
				info += "\n\n" + Messages.get(Armor.class, "material_info", material().title()) + " "
						+ (broken() ? Messages.get(Armor.class, "condition_broken") : Messages.get(Armor.class, "condition_info", (int)Math.ceil(condition)));
				if (magicResist() > 0)   info += " " + Messages.get(Armor.class, "magic_resist", Math.round(magicResist()*100));
				if (weightPenalty() > 0) info += " " + Messages.get(Armor.class, "weight_penalty", Math.round(weightPenalty()*100));
				else if (weightPenalty() < 0) info += " " + Messages.get(Armor.class, "weight_bonus", Math.round(-weightPenalty()*100));
			}
			
			if (Dungeon.hero != null && STRReq() > Dungeon.hero.STR()) {
				info += " " + Messages.get(Armor.class, "too_heavy");
			}
		} else {
			info += "\n\n" + Messages.get(Armor.class, "avg_absorb", weightLabel(), DRMin(0), DRMax(0), STRReq(0));

			if (Dungeon.hero != null && STRReq(0) > Dungeon.hero.STR()) {
				info += " " + Messages.get(Armor.class, "probably_too_heavy");
			}
		}

		switch (augment) {
			case EVASION:
				info += " " + Messages.get(Armor.class, "evasion");
				break;
			case DEFENSE:
				info += " " + Messages.get(Armor.class, "defense");
				break;
			case NONE:
		}

		if (isEquipped(Dungeon.hero) && !hasCurseGlyph() && Dungeon.hero.buff(HolyWard.HolyArmBuff.class) != null
				&& (Dungeon.hero.subClass != HeroSubClass.PALADIN || glyph == null)){
			info += "\n\n" + Messages.capitalize(Messages.get(Armor.class, "inscribed", Messages.get(HolyWard.class, "glyph_name", Messages.get(Glyph.class, "glyph"))));
			info += " " + Messages.get(HolyWard.class, "glyph_desc");
		} else if (glyph != null  && (cursedKnown || !glyph.curse())) {
			info += "\n\n" +  Messages.capitalize(Messages.get(Armor.class, "inscribed", glyph.name()));
			if (glyphHardened) info += " " + Messages.get(Armor.class, "glyph_hardened");
			info += " " + glyph.desc();
		} else if (glyphHardened){
			info += "\n\n" + Messages.get(Armor.class, "hardened_no_glyph");
		}
		
		if (cursed && isEquipped( Dungeon.hero )) {
			info += "\n\n" + Messages.get(Armor.class, "cursed_worn");
		} else if (cursedKnown && cursed) {
			info += "\n\n" + Messages.get(Armor.class, "cursed");
		} else if (!isIdentified() && cursedKnown){
			if (glyph != null && glyph.curse()) {
				info += "\n\n" + Messages.get(Armor.class, "weak_cursed");
			} else {
				info += "\n\n" + Messages.get(Armor.class, "not_cursed");
			}
		}

		if (seal != null) {
			info += "\n\n" + Messages.get(Armor.class, "seal_attached", seal.maxShield(tier, level()));
		}
		
		return info;
	}

	@Override
	public Emitter emitter() {
		//MOD: glyph aura around the armor outline (see effects/ItemAura)
		if (seal == null) {
			if (glyph != null && (cursedKnown || !glyph.curse())) {
				return ItemAura.armor(image(), glyph, glyph.curse());
			}
			return super.emitter();
		}
		Emitter emitter = new Emitter();
		emitter.pos(ItemSpriteSheet.film.width(image)/2f + 2f, ItemSpriteSheet.film.height(image)/3f);
		emitter.fillTarget = false;
		emitter.pour(Speck.factory( Speck.RED_LIGHT ), 0.6f);
		return emitter;
	}

	@Override
	public Item random() {
		//+0: 75% (3/4)
		//+1: 20% (4/20)
		//+2: 5%  (1/20)
		int n = 0;
		if (Random.Int(4) == 0) {
			n++;
			if (Random.Int(5) == 0) {
				n++;
			}
		}
		//MOD: found gear is capped by region: +1 on floors 1-5, +2 on 6-10, +3 on 11-15...
		level(Math.min(n, 1 + Math.max(0, Dungeon.depth - 1) / 5));

		//we use a separate RNG here so that variance due to things like parchment scrap
		//does not affect levelgen
		Random.pushGenerator(Random.Long());

			//30% chance to be cursed
			//15% chance to be inscribed
			float effectRoll = Random.Float();
			if (effectRoll < 0.3f * ParchmentScrap.curseChanceMultiplier()) {
				inscribe(Glyph.randomCurse());
				cursed = true;
			} else if (effectRoll >= 1f - (0.15f * ParchmentScrap.enchantChanceMultiplier())){
				inscribe();
			}

		Random.popGenerator();

		rollMaterial(); //MOD
		return this;
	}

	public int STRReq(){
		return 0; //MOD: strength requirements removed
	}

	public int STRReq(int lvl){
		return 0; //MOD: strength requirements removed
	}

	protected static int STRReq(int tier, int lvl){
		lvl = Math.max(0, lvl);

		//MOD: strength requirements removed from the game
		return 0;
	}
	
	@Override
	public int value() {
		if (seal != null) return 0;

		int price = 20 * tier;
		if (hasGoodGlyph()) {
			price *= 1.5;
		}
		if (cursedKnown && (cursed || hasCurseGlyph())) {
			price /= 2;
		}
		if (levelKnown && level() > 0) {
			price *= (level() + 1);
		}
		if (price < 1) {
			price = 1;
		}
		return price;
	}

	public Armor inscribe( Glyph glyph ) {
		if (glyph == null || !glyph.curse()) curseInfusionBonus = false;
		this.glyph = glyph;
		updateQuickslot();
		//the hero needs runic transference to actually transfer, but we still attach the glyph here
		// in case they take that talent in the future
		if (seal != null){
			seal.setGlyph(glyph);
		}
		if (glyph != null && isIdentified() && Dungeon.hero != null
				&& Dungeon.hero.isAlive() && Dungeon.hero.belongings.contains(this)){
			Catalog.setSeen(glyph.getClass());
			Statistics.itemTypesDiscovered.add(glyph.getClass());
		}
		return this;
	}

	public Armor inscribe() {

		Class<? extends Glyph> oldGlyphClass = glyph != null ? glyph.getClass() : null;
		Glyph gl = Glyph.random( oldGlyphClass );

		return inscribe( gl );
	}

	public boolean hasGlyph(Class<?extends Glyph> type, Char owner) {
		if (owner.buff(MagicImmune.class) != null) {
			return false;
		} else if (glyph != null
				&& !glyph.curse()
				&& owner instanceof Hero
				&& isEquipped((Hero) owner)
				&& owner.buff(HolyWard.HolyArmBuff.class) != null
				&& ((Hero) owner).subClass != HeroSubClass.PALADIN){
			return false;
		} else if (owner.buff(BodyForm.BodyFormBuff.class) != null
				&& owner.buff(BodyForm.BodyFormBuff.class).glyph() != null
				&& owner.buff(BodyForm.BodyFormBuff.class).glyph().getClass().equals(type)){
			return true;
		} else if (glyph != null) {
			return glyph.getClass() == type;
		} else {
			return false;
		}
	}

	//these are not used to process specific glyph effects, so magic immune doesn't affect them
	public boolean hasGoodGlyph(){
		return glyph != null && !glyph.curse();
	}

	public boolean hasCurseGlyph(){
		return glyph != null && glyph.curse();
	}

	private static ItemSprite.Glowing HOLY = new ItemSprite.Glowing( 0xFFFF00 );

	@Override
	public ItemSprite.Glowing glowing() {
		if (isEquipped(Dungeon.hero) && !hasCurseGlyph() && Dungeon.hero.buff(HolyWard.HolyArmBuff.class) != null
				&& (Dungeon.hero.subClass != HeroSubClass.PALADIN || glyph == null)){
			return HOLY;
		} else {
			//MOD: umbralite gear pulses purple; enchantments/glyphs show as particle auras instead
			if (material() == com.shatteredpixel.shatteredpixeldungeon.items.Material.OBSIDIAN) return UMBRA_GLOW;
			//cloth armor shows its glyph as a pulsing sigil on the fabric
			if (this instanceof ClothArmor && glyph != null && (cursedKnown || !glyph.curse()) && glyph.glowing() != null) {
				return new ItemSprite.Glowing(glyph.glowing().color, 1.5f, true);
			}
			return null;
		}
	}
	
	public static abstract class Glyph implements Bundlable {
		
		public static final Class<?>[] common = new Class<?>[]{
				Obfuscation.class, Swiftness.class, Viscosity.class, Potential.class };

		public static final Class<?>[] uncommon = new Class<?>[]{
				Brimstone.class, Stone.class, Entanglement.class,
				Repulsion.class, Camouflage.class, Flow.class };

		public static final Class<?>[] rare = new Class<?>[]{
				Affection.class, AntiMagic.class, Thorns.class };

		public static final float[] typeChances = new float[]{
				50, //12.5% each
				40, //6.67% each
				10  //3.33% each
		};

		public static final Class<?>[] curses = new Class<?>[]{
				AntiEntropy.class, Corrosion.class, Displacement.class, Metabolism.class,
				Multiplicity.class, Stench.class, Overgrowth.class, Bulk.class
		};
		
		public abstract int proc( Armor armor, Char attacker, Char defender, int damage );

		protected float procChanceMultiplier( Char defender ){
			return genericProcChanceMultiplier( defender );
		}

		public static float genericProcChanceMultiplier( Char defender ){
			float multi = RingOfArcana.enchantPowerMultiplier(defender);

			if (Dungeon.hero.alignment == defender.alignment
					&& Dungeon.hero.buff(AuraOfProtection.AuraBuff.class) != null
					&& (Dungeon.level.distance(defender.pos, Dungeon.hero.pos) <= 2 || defender.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null)){
				multi += 0.25f + 0.25f*Dungeon.hero.pointsInTalent(Talent.AURA_OF_PROTECTION);
			}

			return multi;
		}
		
		public String name() {
			if (!curse())
				return name( Messages.get(this, "glyph") );
			else
				return name( Messages.get(Item.class, "curse"));
		}
		
		public String name( String armorName ) {
			return Messages.get(this, "name", armorName);
		}

		public String desc() {
			return Messages.get(this, "desc");
		}

		public boolean curse() {
			return false;
		}
		
		@Override
		public void restoreFromBundle( Bundle bundle ) {
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
		}
		
		public abstract ItemSprite.Glowing glowing();

		@SuppressWarnings("unchecked")
		public static Glyph random( Class<? extends Glyph> ... toIgnore ) {
			switch(Random.chances(typeChances)){
				case 0: default:
					return randomCommon( toIgnore );
				case 1:
					return randomUncommon( toIgnore );
				case 2:
					return randomRare( toIgnore );
			}
		}
		
		@SuppressWarnings("unchecked")
		public static Glyph randomCommon( Class<? extends Glyph> ... toIgnore ){
			ArrayList<Class<?>> glyphs = new ArrayList<>(Arrays.asList(common));
			glyphs.removeAll(Arrays.asList(toIgnore));
			if (glyphs.isEmpty()) {
				return random();
			} else {
				return (Glyph) Reflection.newInstance(Random.element(glyphs));
			}
		}
		
		@SuppressWarnings("unchecked")
		public static Glyph randomUncommon( Class<? extends Glyph> ... toIgnore ){
			ArrayList<Class<?>> glyphs = new ArrayList<>(Arrays.asList(uncommon));
			glyphs.removeAll(Arrays.asList(toIgnore));
			if (glyphs.isEmpty()) {
				return random();
			} else {
				return (Glyph) Reflection.newInstance(Random.element(glyphs));
			}
		}
		
		@SuppressWarnings("unchecked")
		public static Glyph randomRare( Class<? extends Glyph> ... toIgnore ){
			ArrayList<Class<?>> glyphs = new ArrayList<>(Arrays.asList(rare));
			glyphs.removeAll(Arrays.asList(toIgnore));
			if (glyphs.isEmpty()) {
				return random();
			} else {
				return (Glyph) Reflection.newInstance(Random.element(glyphs));
			}
		}
		
		@SuppressWarnings("unchecked")
		public static Glyph randomCurse( Class<? extends Glyph> ... toIgnore ){
			ArrayList<Class<?>> glyphs = new ArrayList<>(Arrays.asList(curses));
			glyphs.removeAll(Arrays.asList(toIgnore));
			if (glyphs.isEmpty()) {
				return random();
			} else {
				return (Glyph) Reflection.newInstance(Random.element(glyphs));
			}
		}
		
	}
}
