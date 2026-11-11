/*
 * Augment families. Holding 2, 4 and 6 augments of one family unlocks its set bonuses.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments;

import java.util.EnumMap;

public enum AugSet {

	BLOOD  ("혈투", 0xE04848),
	FLAME  ("화염", 0xFF8A30),
	FROST  ("서리", 0x9AD8FF),
	STORM  ("뇌전", 0xF0E060),
	VENOM  ("맹독", 0xA0D040),
	SHADOW ("그림자", 0xA080E0),
	GUARD  ("수호", 0xB8C4D0),
	ARCANE ("비전", 0xE070E8),
	HUNT   ("사냥", 0x5FC8A8),
	GREED  ("탐욕", 0xFFC830),
	WILD   ("생존", 0x5FB050),
	FURY   ("광전", 0xFF7050);

	public static final int[] THRESHOLDS = {2, 4, 6};

	public final String title;
	public final int color;

	//bonus stats per threshold
	@SuppressWarnings("unchecked")
	final EnumMap<St, Float>[] bonus = new EnumMap[3];

	AugSet( String title, int color ){
		this.title = title;
		this.color = color;
		for (int i = 0; i < 3; i++) bonus[i] = new EnumMap<>(St.class);
	}

	private AugSet b( int tier, St s, float v ){
		bonus[tier].put(s, v);
		return this;
	}

	public EnumMap<St, Float> bonus( int tier ){
		return bonus[tier];
	}

	//how many thresholds are reached with this many augments of the family
	public static int reached( int count ){
		int r = 0;
		for (int t : THRESHOLDS) if (count >= t) r++;
		return r;
	}

	static {
		BLOOD .b(0, St.LIFESTEAL, 0.03f)
		      .b(1, St.K_HEAL, 3)
		      .b(2, St.BLOOD_RAGE, 0.40f);
		FLAME .b(0, St.BURNING_DMG, 0.15f)
		      .b(1, St.P_BURN, 0.10f).b(1, St.IMM_FIRE, 1)
		      .b(2, St.K_FIRE, 0.35f).b(2, St.AURA_FIRE, 0.15f);
		FROST .b(0, St.CHILLED_DMG, 0.15f)
		      .b(1, St.P_CHILL, 0.15f).b(1, St.IMM_COLD, 1)
		      .b(2, St.H_FROST, 0.20f).b(2, St.P_FREEZE, 0.05f);
		STORM .b(0, St.ASPD, 0.05f).b(0, St.MSPD, 0.05f)
		      .b(1, St.P_LIGHTNING, 0.10f).b(1, St.IMM_ELEC, 1)
		      .b(2, St.P_LIGHTNING, 0.10f).b(2, St.AURA_SHOCK, 0.10f).b(2, St.ASPD, 0.10f);
		VENOM .b(0, St.POISONED_DMG, 0.15f)
		      .b(1, St.P_POISON, 0.15f).b(1, St.IMM_POISON, 1)
		      .b(2, St.K_GAS, 0.30f).b(2, St.P_WEAK, 0.10f);
		SHADOW.b(0, St.STEALTH, 2).b(0, St.EVA_PCT, 0.10f)
		      .b(1, St.CRIT, 0.08f)
		      .b(2, St.SURPRISE, 0.50f).b(2, St.K_INVIS, 0.25f);
		GUARD .b(0, St.ARMOR, 2)
		      .b(1, St.F_SHIELD, 0.25f).b(1, St.TAKEN, 0.05f)
		      .b(2, St.TAKEN, 0.10f).b(2, St.THORNS, 0.20f);
		ARCANE.b(0, St.WAND_CHARGE, 0.15f)
		      .b(1, St.WAND_PCT, 0.20f)
		      .b(2, St.WAND_FREE, 0.15f).b(2, St.ORB, 1f);
		HUNT  .b(0, St.ACC_PCT, 0.10f)
		      .b(1, St.THROWN_PCT, 0.20f).b(1, St.THROW_DUR, 0.30f)
		      .b(2, St.CRIT, 0.10f).b(2, St.CRITD, 0.30f);
		GREED .b(0, St.GOLD, 0.25f)
		      .b(1, St.LOOT, 0.25f).b(1, St.DISCOUNT, 0.15f)
		      .b(2, St.K_GOLD, 0.25f).b(2, St.GOLD_ARMOR, 10);
		WILD  .b(0, St.HUNGER, 0.15f).b(0, St.REGEN, 0.20f)
		      .b(1, St.HEALAMP, 0.30f).b(1, St.EAT_HEAL, 0.20f)
		      .b(2, St.HP_PCT, 0.15f).b(2, St.LAST_STAND, 1);
		FURY  .b(0, St.DMG_PCT, 0.05f)
		      .b(1, St.ASPD, 0.10f)
		      .b(2, St.RAGE, 0.06f);
	}
}
