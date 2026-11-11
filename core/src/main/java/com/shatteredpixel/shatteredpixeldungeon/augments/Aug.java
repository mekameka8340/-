/*
 * Augment definitions. Every augment belongs to a tier and (apart from the basic training
 * augments) to a family. Its description is built from its stats.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.shatteredpixel.shatteredpixeldungeon.augments.AugSet.*;
import static com.shatteredpixel.shatteredpixeldungeon.augments.St.*;

public class Aug {

	public static final int SILVER = 0, GOLD = 1, PRISM = 2, LEGEND = 3;
	public static final String[] TIER_NAMES = {"실버", "골드", "프리즘", "전설"};
	public static final int[] TIER_COLORS = {0xD8DEE8, 0xFFD45A, 0xFFFFFF, 0xFF8A50};

	//colour of an augment's name for lists
	public int nameColor(){
		if (tier == PRISM) return 0xF0D0FF;
		return TIER_COLORS[tier];
	}

	public final String name;
	public final int tier;
	public final AugSet set;     //null for the basic training augments
	public final int icon;
	public boolean repeatable = false;
	//big upside with a price attached
	public boolean gamble = false;
	//legendary augments are unlocked by holding all of these
	public String[] requires = null;
	final EnumMap<St, Float> stats = new EnumMap<>(St.class);

	private Aug( String name, int tier, AugSet set, int icon ){
		this.name = name;
		this.tier = tier;
		this.set = set;
		this.icon = icon;
	}

	private Aug v( St s, float value ){
		stats.put(s, value);
		return this;
	}

	public Map<St, Float> stats(){
		return stats;
	}

	public ArrayList<String> lines(){
		ArrayList<String> out = new ArrayList<>();
		//upsides first, then the prices
		for (Map.Entry<St, Float> e : stats.entrySet()){
			if (e.getValue() >= 0) out.add(e.getKey().describe(e.getValue()));
		}
		for (Map.Entry<St, Float> e : stats.entrySet()){
			if (e.getValue() < 0) out.add("대가: " + e.getKey().describe(e.getValue()));
		}
		return out;
	}

	//whether every requirement of a legendary augment is held
	public boolean unlocked( java.util.List<String> owned ){
		if (requires == null) return false;
		for (String r : requires) if (!owned.contains(r)) return false;
		return true;
	}

	public String requiresText(){
		if (requires == null) return "";
		StringBuilder sb = new StringBuilder();
		for (String r : requires){
			if (sb.length() > 0) sb.append(" + ");
			sb.append(r);
		}
		return sb.toString();
	}

	//rare, huge effects
	public boolean jackpot(){
		return stats.containsKey(St.U_GUILLOTINE) || stats.containsKey(St.U_CHAINBLAST)
				|| stats.containsKey(St.U_JACKPOT) || stats.containsKey(St.U_DICE);
	}

	//description for a card on offer: effects already held show what they turn into instead
	public String desc( AugTracker t ){
		if (t == null) return desc();
		//held already: this card is a star upgrade
		if (t.canStar(this)){
			int now = t.stars(name), next = now + 1;
			float pw = AugTracker.STAR_POWER[next];
			StringBuilder sb = new StringBuilder();
			//no arrows here: phones draw symbols with their own font, which may not have them
			sb.append("_").append(stars(next)).append(" 강화_  모든 효과 ").append(pw).append("배");
			for (Map.Entry<St, Float> e : stats.entrySet()){
				St s = e.getKey();
				if (s.once()) continue;
				sb.append('\n').append("- ");
				if (e.getValue() < 0) sb.append("대가: ").append(s.describe(e.getValue())).append(" (그대로)");
				else if (s.flag()) sb.append(s.describe(1)).append(" (강화)");
				else sb.append(s.describe(e.getValue() * pw));
			}
			return sb.toString();
		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<St, Float> e : stats.entrySet()){
			if (e.getValue() < 0) continue;
			St s = e.getKey();
			String line;
			if (s.flag() && t.holds(s)){
				line = s.dupStat() != null
						? s.describe(1) + " (이미 보유, 대신 " + s.dupStat().describe(s.dupValue()) + ")"
						: s.describe(1) + " (보유 중)";
			} else {
				line = s.describe(e.getValue());
			}
			if (sb.length() > 0) sb.append('\n');
			sb.append("- ").append(line);
		}
		for (Map.Entry<St, Float> e : stats.entrySet()){
			if (e.getValue() >= 0) continue;
			if (sb.length() > 0) sb.append('\n');
			sb.append("- 대가: ").append(e.getKey().describe(e.getValue()));
		}
		if (AugQuest.isQuest(this)) sb.append("\n- ").append(AugQuest.cardLine(name));
		return sb.toString();
	}

	public static String stars( int n ){
		//the game's pixel font has no star glyph, so stars are written out
		return Math.max(1, Math.min(3, n)) + "성";
	}

	public String desc(){
		StringBuilder sb = new StringBuilder();
		for (String l : lines()){
			if (sb.length() > 0) sb.append('\n');
			sb.append("- ").append(l);
		}
		if (AugQuest.isQuest(this)) sb.append("\n- ").append(AugQuest.cardLine(name));
		return sb.toString();
	}

	// ================================================================ registry

	static final LinkedHashMap<String, Aug> ALL = new LinkedHashMap<>();

	public static Aug get( String name ){
		return ALL.get(name);
	}

	public static Iterable<Aug> all(){
		return ALL.values();
	}

	public static int count(){
		return ALL.size();
	}

	private static Aug add( String name, int tier, AugSet set, int icon ){
		Aug a = new Aug(name, tier, set, icon);
		if (ALL.put(name, a) != null) throw new RuntimeException("duplicate augment " + name);
		return a;
	}

	private static Aug s( String n, AugSet set, int icon ){ return add(n, SILVER, set, icon); }
	private static Aug g( String n, AugSet set, int icon ){ return add(n, GOLD, set, icon); }
	private static Aug p( String n, AugSet set, int icon ){ return add(n, PRISM, set, icon); }
	private static Aug l( String n, AugSet set, int icon, String... requires ){
		Aug a = add(n, LEGEND, set, icon);
		a.requires = requires;
		return a;
	}
	private Aug gamble(){
		gamble = true;
		return this;
	}

	static {
		// ------------------------------------------------ basic training, can be taken more than once
		s("체력 단련", null, ItemSpriteSheet.HONEYPOT).v(HP, 12).repeatable = true;
		s("정밀 수련", null, ItemSpriteSheet.RING_AGATE).v(ACC, 4).repeatable = true;
		s("회피 수련", null, ItemSpriteSheet.RING_QUARTZ).v(EVA, 4).repeatable = true;
		s("균형 잡힌 성장", null, ItemSpriteSheet.MASTERY).v(HP, 6).v(ACC, 2).v(EVA, 2).repeatable = true;

		// ------------------------------------------------ blood
		s("피의 맛", BLOOD, ItemSpriteSheet.ARTIFACT_CHALICE1).v(LIFESTEAL, 0.04f);
		s("광기의 눈빛", BLOOD, ItemSpriteSheet.SCROLL_RAIDO).v(LOW_DMG, 0.15f);
		s("피의 굶주림", BLOOD, ItemSpriteSheet.BLOOD_VIAL).v(K_HEAL, 2);
		s("칼날 출혈", BLOOD, ItemSpriteSheet.SICKLE).v(P_BLEED, 0.12f);
		g("흡혈귀의 송곳니", BLOOD, ItemSpriteSheet.ARTIFACT_CHALICE2).v(LIFESTEAL, 0.08f).v(HP, 8);
		g("피의 축제", BLOOD, ItemSpriteSheet.PHANTOM_MEAT).v(K_HEAL, 4).v(LOW_DMG, 0.12f);
		g("동맥 절단", BLOOD, ItemSpriteSheet.WAR_SCYTHE).v(P_BLEED, 0.25f).v(EXECUTE, 0.15f);
		g("피의 분노", BLOOD, ItemSpriteSheet.EXOTIC_CRIMSON).v(BLOOD_RAGE, 0.30f);
		g("처형인", BLOOD, ItemSpriteSheet.GREATAXE).v(EXECUTE, 0.35f);
		p("피의 군주", BLOOD, ItemSpriteSheet.ARTIFACT_CHALICE3).v(LIFESTEAL, 0.12f).v(BLOOD_RAGE, 0.30f).v(HP, 20);
		p("불사의 의지", BLOOD, ItemSpriteSheet.ANKH).v(LAST_STAND, 1).v(LOW_TAKEN, 0.20f);
		p("영혼 착취", BLOOD, ItemSpriteSheet.ARTIFACT_CHALICE1).v(U_SOULSTEAL, 1).v(K_HEAL, 2);
		p("되살아나는 심장", BLOOD, ItemSpriteSheet.POTION_CRIMSON).v(REVIVE, 1).v(HP, 15);

		// ------------------------------------------------ flame
		s("불씨", FLAME, ItemSpriteSheet.EMBER).v(P_BURN, 0.10f);
		s("화염 도장", FLAME, ItemSpriteSheet.FIRE_BOMB).v(H_BURN, 0.25f);
		s("잿불 걸음", FLAME, ItemSpriteSheet.ARTIFACT_SANDALS).v(MSPD, 0.08f).v(P_BURN, 0.05f);
		s("소각", FLAME, ItemSpriteSheet.INCENDIARY_DART).v(K_FIRE, 0.15f);
		g("화염의 낙인", FLAME, ItemSpriteSheet.WAND_FIREBOLT).v(P_BURN, 0.22f).v(BURNING_DMG, 0.15f);
		g("불의 갑옷", FLAME, ItemSpriteSheet.POTION_AMBER).v(H_BURN, 0.40f).v(IMM_FIRE, 1).v(ARMOR, 1);
		g("지옥불 폭발", FLAME, ItemSpriteSheet.BREW_INFERNAL).v(K_FIRE, 0.35f);
		g("이글거리는 기운", FLAME, ItemSpriteSheet.CANDLE).v(AURA_FIRE, 0.15f);
		g("용의 숨결", FLAME, ItemSpriteSheet.ELIXIR_DRAGON).v(P_BURN, 0.15f).v(DMG_PCT, 0.08f);
		p("불사조", FLAME, ItemSpriteSheet.ELIXIR_DRAGON).v(REVIVE, 1).v(IMM_FIRE, 1).v(K_FIRE, 0.20f);
		p("태양의 분노", FLAME, ItemSpriteSheet.SUMMON_ELE_FIRE).v(AURA_FIRE, 0.30f).v(BURNING_DMG, 0.30f);
		p("지옥 화염", FLAME, ItemSpriteSheet.EXOTIC_AMBER).v(P_BURN, 0.35f).v(K_FIRE, 0.35f).v(IMM_FIRE, 1);
		p("용의 심장", FLAME, ItemSpriteSheet.RING_GARNET).v(HP_PCT, 0.20f).v(DMG_PCT, 0.15f).v(IMM_FIRE, 1);

		// ------------------------------------------------ frost
		s("서리 손길", FROST, ItemSpriteSheet.SEED_ICECAP).v(P_CHILL, 0.15f);
		s("얼음 피부", FROST, ItemSpriteSheet.POTION_AZURE).v(ARMOR, 1).v(IMM_COLD, 1);
		s("서리 결계", FROST, ItemSpriteSheet.FROST_BOMB).v(H_FROST, 0.12f);
		s("서리 숨결", FROST, ItemSpriteSheet.POTION_TURQUOISE).v(K_FROST, 0.15f);
		g("빙결의 일격", FROST, ItemSpriteSheet.WAND_FROST).v(P_FREEZE, 0.10f).v(CHILLED_DMG, 0.15f);
		g("영구 동토", FROST, ItemSpriteSheet.BREW_BLIZZARD).v(AURA_FROST, 0.25f);
		g("얼음 거울", FROST, ItemSpriteSheet.ELIXIR_ICY).v(H_FROST, 0.25f).v(IMM_COLD, 1).v(EVA, 2);
		g("서리 폭풍", FROST, ItemSpriteSheet.SUMMON_ELE_FROST).v(K_FROST, 0.35f);
		g("냉혹한 집행", FROST, ItemSpriteSheet.GLAIVE).v(CHILLED_DMG, 0.30f).v(EXECUTE, 0.10f);
		p("절대 영도", FROST, ItemSpriteSheet.EXOTIC_AZURE).v(P_FREEZE, 0.20f).v(AURA_FROST, 0.30f).v(IMM_COLD, 1);
		p("빙하의 왕관", FROST, ItemSpriteSheet.CROWN).v(TAKEN, 0.15f).v(H_FROST, 0.30f).v(ARMOR, 3);
		p("서리 여왕의 입맞춤", FROST, ItemSpriteSheet.RING_DIAMOND).v(CHILLED_DMG, 0.40f).v(P_CHILL, 0.30f).v(CRIT, 0.08f);
		p("영원한 겨울", FROST, ItemSpriteSheet.EXOTIC_TURQUOISE).v(K_FROST, 0.50f).v(AURA_FROST, 0.20f).v(HP, 15);

		// ------------------------------------------------ storm
		s("정전기", STORM, ItemSpriteSheet.SHOCKING_DART).v(P_LIGHTNING, 0.08f);
		s("피뢰침", STORM, ItemSpriteSheet.SEED_STORMVINE).v(IMM_ELEC, 1).v(HP, 6);
		s("죽음의 선율", STORM, ItemSpriteSheet.ARTIFACT_HORN1).v(U_MELODY, 1);
		s("질풍 처치", STORM, ItemSpriteSheet.ARTIFACT_BOOTS).v(K_HASTE, 2);
		g("연쇄 번개", STORM, ItemSpriteSheet.WAND_LIGHTNING).v(P_LIGHTNING, 0.18f);
		g("폭풍 갑주", STORM, ItemSpriteSheet.BREW_SHOCKING).v(H_SHOCK, 0.40f).v(IMM_ELEC, 1).v(ARMOR, 1);
		g("뇌신의 가호", STORM, ItemSpriteSheet.SUMMON_ELE_SHOCK).v(AURA_SHOCK, 0.20f);
		g("번개 연타", STORM, ItemSpriteSheet.SAI).v(ASPD, 0.15f).v(P_LIGHTNING, 0.06f);
		g("천둥 망치", STORM, ItemSpriteSheet.THROWING_HAMMER).v(P_KNOCK, 0.15f).v(P_LIGHTNING, 0.08f);
		p("폭풍의 화신", STORM, ItemSpriteSheet.EXOTIC_GOLDEN).v(P_LIGHTNING, 0.30f).v(AURA_SHOCK, 0.20f).v(IMM_ELEC, 1);
		p("신속의 신", STORM, ItemSpriteSheet.RING_ONYX).v(ASPD, 0.30f).v(MSPD, 0.25f);
		p("천벌", STORM, ItemSpriteSheet.TENGU_SHOCKER).v(P_LIGHTNING, 0.20f).v(BOSS, 0.30f).v(CRIT, 0.08f);
		p("폭풍 전야", STORM, ItemSpriteSheet.BREW_SHOCKING).v(U_CALM, 1).v(P_LIGHTNING, 0.08f);

		// ------------------------------------------------ venom
		s("독 바른 칼날", VENOM, ItemSpriteSheet.POISON_DART).v(P_POISON, 0.15f);
		s("해독 체질", VENOM, ItemSpriteSheet.POTION_JADE).v(IMM_POISON, 1).v(HP, 4);
		s("가시 독샘", VENOM, ItemSpriteSheet.SEED_BLINDWEED).v(H_POISON, 0.30f);
		s("부패", VENOM, ItemSpriteSheet.BLOB).v(K_GAS, 0.15f).v(IMM_POISON, 1);
		g("맹독", VENOM, ItemSpriteSheet.EXOTIC_JADE).v(P_POISON, 0.30f).v(POISONED_DMG, 0.15f);
		g("역병의 숨결", VENOM, ItemSpriteSheet.EXOTIC_BISTRE).v(K_GAS, 0.35f).v(IMM_POISON, 1);
		g("독사의 비늘", VENOM, ItemSpriteSheet.ELIXIR_TOXIC).v(H_POISON, 0.45f).v(IMM_POISON, 1).v(ARMOR, 1);
		g("신경독", VENOM, ItemSpriteSheet.PARALYTIC_DART).v(P_PARA, 0.06f).v(P_POISON, 0.10f);
		g("쇠락의 저주", VENOM, ItemSpriteSheet.CURSE_INFUSE).v(P_WEAK, 0.20f).v(P_HEX, 0.10f);
		p("역병의 군주", VENOM, ItemSpriteSheet.RING_EMERALD).v(P_POISON, 0.40f).v(K_GAS, 0.40f).v(IMM_POISON, 1);
		p("히드라의 피", VENOM, ItemSpriteSheet.EXOTIC_CHARCOAL).v(LIFESTEAL, 0.08f).v(IMM_POISON, 1).v(POISONED_DMG, 0.30f);
		p("죽음의 꽃", VENOM, ItemSpriteSheet.SEED_ROTBERRY).v(IMM_POISON, 1).v(K_GAS, 0.30f).v(P_PARA, 0.10f).v(DEBUFF_DMG, 0.20f);
		p("독룡의 이빨", VENOM, ItemSpriteSheet.MIMIC_TOOTH).v(P_POISON, 0.30f).v(P_VULN, 0.20f).v(DMG_PCT, 0.10f);

		// ------------------------------------------------ shadow
		s("기습", SHADOW, ItemSpriteSheet.DIRK).v(SURPRISE, 0.30f);
		s("그림자 도약", SHADOW, ItemSpriteSheet.SMOKE_BOMB).v(K_INVIS, 0.12f);
		s("낙인", SHADOW, ItemSpriteSheet.SCROLL_ISAZ).v(U_BRAND, 1);
		s("섬광 가루", SHADOW, ItemSpriteSheet.FLASHBANG).v(P_BLIND, 0.12f);
		g("암살자의 비수", SHADOW, ItemSpriteSheet.KUNAI).v(CRIT, 0.12f).v(CRITD, 0.20f);
		g("그림자 망토", SHADOW, ItemSpriteSheet.POTION_CHARCOAL).v(K_INVIS, 0.30f).v(STEALTH, 2);
		g("그림자 춤", SHADOW, ItemSpriteSheet.ARMOR_ROGUE).v(DODGE, 0.10f).v(EVA, 3);
		g("첫 번째 칼날", SHADOW, ItemSpriteSheet.ASSASSINS_BLADE).v(OPENER, 0.40f).v(SURPRISE, 0.30f);
		g("공포의 그림자", SHADOW, ItemSpriteSheet.SCROLL_ODAL).v(P_TERROR, 0.15f);
		p("그림자 군주", SHADOW, ItemSpriteSheet.EXOTIC_NAUDIZ).v(SURPRISE, 0.80f).v(K_INVIS, 0.40f).v(CRIT, 0.10f);
		p("영혼의 사슬", SHADOW, ItemSpriteSheet.EXOTIC_ODAL).v(U_SOULCHAIN, 1).v(CRIT, 0.05f);
		p("처형의 그림자", SHADOW, ItemSpriteSheet.KATANA).v(CRIT, 0.15f).v(CRITD, 0.50f);
		p("환영 도주", SHADOW, ItemSpriteSheet.SCROLL_MANNAZ).v(BLINK_LOW, 1).v(DODGE, 0.10f).v(K_INVIS, 0.20f);

		// ------------------------------------------------ guard
		s("가시 갑옷", GUARD, ItemSpriteSheet.ARTIFACT_CAPE).v(THORNS, 0.20f);
		s("수호의 맹세", GUARD, ItemSpriteSheet.SEAL).v(F_SHIELD, 0.15f);
		s("방어 자세", GUARD, ItemSpriteSheet.POTION_SILVER).v(H_SHIELD, 0.10f);
		g("강철 피부", GUARD, ItemSpriteSheet.POTION_BISTRE).v(ARMOR, 4);
		g("거대한 방패", GUARD, ItemSpriteSheet.GREATSHIELD).v(TAKEN, 0.12f);
		g("고슴도치", GUARD, ItemSpriteSheet.SEED_EARTHROOT).v(THORNS, 0.45f);
		g("수호천사", GUARD, ItemSpriteSheet.SEAL_SHARD).v(F_SHIELD, 0.35f).v(H_SHIELD, 0.10f);
		g("바위 같은 몸", GUARD, ItemSpriteSheet.ORE).v(HP, 20).v(ARMOR, 2);
		p("불멸의 요새", GUARD, ItemSpriteSheet.ARMOR_CLERIC).v(TAKEN, 0.20f).v(ARMOR, 4).v(HP, 15);
		p("신성한 보호막", GUARD, ItemSpriteSheet.HOLY_DART).v(F_SHIELD, 0.50f).v(H_SHIELD, 0.20f).v(LAST_STAND, 1);
		p("거인화", GUARD, ItemSpriteSheet.ELIXIR_MIGHT).v(Q_GIANT, 1);
		p("복수의 갑주", GUARD, ItemSpriteSheet.ARTIFACT_CHAINS).v(THORNS, 0.60f).v(TAKEN, 0.10f);

		// ------------------------------------------------ arcane
		s("마나 친화", ARCANE, ItemSpriteSheet.WAND_MAGIC_MISSILE).v(WAND_CHARGE, 0.15f);
		s("반사 마탄", ARCANE, ItemSpriteSheet.WAND_MAGIC_MISSILE).v(U_REFLECT, 1);
		s("지식의 갈망", ARCANE, ItemSpriteSheet.SCROLL_BERKANAN).v(EXP, 0.15f);
		s("마나 순환", ARCANE, ItemSpriteSheet.SCROLL_LAGUZ).v(F_RECHARGE, 10);
		g("대마법사의 지팡이", ARCANE, ItemSpriteSheet.WAND_PRISMATIC_LIGHT).v(WAND_PCT, 0.25f).v(WAND_CHARGE, 0.10f);
		g("무한 마나", ARCANE, ItemSpriteSheet.ARCANE_RESIN).v(WAND_FREE, 0.15f);
		g("비전 구체", ARCANE, ItemSpriteSheet.WAND_WARDING).v(ORB, 1f);
		g("마력 폭주", ARCANE, ItemSpriteSheet.WILD_ENERGY).v(K_RECHARGE, 0.20f).v(WAND_PCT, 0.10f);
		g("의식의 원", ARCANE, ItemSpriteSheet.STONE_CLAIRVOYANCE).v(U_RITUAL, 1);
		p("비전의 화신", ARCANE, ItemSpriteSheet.WAND_DISINTEGRATION).v(WAND_PCT, 0.35f).v(WAND_FREE, 0.20f).v(WAND_CHARGE, 0.20f);
		p("별의 궤도", ARCANE, ItemSpriteSheet.MAGIC_INFUSE).v(Q_STARS, 1);
		p("시간 왜곡", ARCANE, ItemSpriteSheet.ARTIFACT_HOURGLASS).v(ASPD, 0.15f).v(MSPD, 0.15f).v(ARTI_CHARGE, 0.25f);
		p("지혜의 샘", ARCANE, ItemSpriteSheet.POTION_IVORY).v(EXP, 0.40f).v(WAND_CHARGE, 0.15f).v(G_EXPPOT, 1);

		// ------------------------------------------------ hunt
		s("추적자", HUNT, ItemSpriteSheet.POTION_INDIGO).v(F_MIND, 8);
		s("분열 투척", HUNT, ItemSpriteSheet.TOMAHAWK).v(U_SPLIT, 1);
		s("함정 감지", HUNT, ItemSpriteSheet.TRAP_MECHANISM).v(TRAP_SENSE, 1);
		g("명사수", HUNT, ItemSpriteSheet.CROSSBOW).v(ACC_PCT, 0.15f).v(CRIT, 0.08f);
		g("투척의 달인", HUNT, ItemSpriteSheet.THROWING_SPEAR).v(THROWN_PCT, 0.25f).v(THROW_DUR, 0.30f);
		g("사냥꾼의 표식", HUNT, ItemSpriteSheet.FISHING_SPEAR).v(P_VULN, 0.20f).v(BOSS, 0.15f);
		g("부메랑 투척", HUNT, ItemSpriteSheet.BOOMERANG).v(U_BOOMERANG, 1).v(THROWN_PCT, 0.10f);
		g("덫 사냥꾼", HUNT, ItemSpriteSheet.TRIDENT).v(P_CRIPPLE, 0.20f).v(P_SLOW, 0.10f);
		p("백발백중", HUNT, ItemSpriteSheet.SPIRIT_BOW).v(ACC_PCT, 0.30f).v(CRIT, 0.15f).v(CRITD, 0.25f);
		p("무한 화살통", HUNT, ItemSpriteSheet.HOLSTER).v(THROW_DUR, 0.80f).v(THROWN_PCT, 0.20f);
		p("거인 사냥꾼", HUNT, ItemSpriteSheet.FORCE_CUBE).v(BOSS, 0.45f).v(EXECUTE, 0.20f);
		p("사냥의 여신", HUNT, ItemSpriteSheet.ARMOR_HUNTRESS).v(P_ECHO, 0.20f).v(ACC_PCT, 0.15f).v(MSPD, 0.10f);

		// ------------------------------------------------ greed
		s("금화 주머니", GREED, ItemSpriteSheet.POUCH).v(F_GOLD, 10);
		s("약탈자", GREED, ItemSpriteSheet.IRON_KEY).v(K_GOLD, 0.10f);
		s("비상금", GREED, ItemSpriteSheet.GOLDEN_KEY).v(G_GOLD, 150);
		g("황금 갑옷", GREED, ItemSpriteSheet.EBONY_CHEST).v(GOLD_ARMOR, 6);
		g("노획", GREED, ItemSpriteSheet.LOCKED_CHEST).v(LOOT, 0.35f).v(K_GOLD, 0.10f);
		g("황금의 손", GREED, ItemSpriteSheet.RING_GARNET).v(St.GOLD, 0.50f).v(G_GOLD, 200);
		g("연금술 상인", GREED, ItemSpriteSheet.VIAL).v(G_HEALING, 2);
		g("투자", GREED, ItemSpriteSheet.SCROLL_KAUNAN).v(G_UPGRADE, 1);
		p("미다스의 손", GREED, ItemSpriteSheet.CRYSTAL_CHEST).v(St.GOLD, 0.80f).v(GOLD_ARMOR, 10).v(K_GOLD, 0.20f);
		p("행운의 여신", GREED, ItemSpriteSheet.RING_OPAL).v(LOOT, 0.50f).v(DODGE, 0.10f).v(CRIT, 0.10f);
		p("왕의 보물", GREED, ItemSpriteSheet.EXOTIC_GYFU).v(G_UPGRADE, 2).v(G_GOLD, 300);
		p("대부호", GREED, ItemSpriteSheet.POTION_GOLDEN).v(DISCOUNT, 0.40f).v(St.GOLD, 0.40f).v(G_STRENGTH, 1);

		// ------------------------------------------------ wild (survival)
		s("든든한 식사", WILD, ItemSpriteSheet.MEAT_PIE).v(EAT_HEAL, 0.20f);
		s("물의 축복", WILD, ItemSpriteSheet.WATERSKIN).v(WATER_REGEN, 1).v(HP, 4);
		s("풀숲 은신", WILD, ItemSpriteSheet.SEED_FADELEAF).v(GRASS_EVA, 0.30f);
		s("개척자", WILD, ItemSpriteSheet.PASTY).v(F_HEAL, 0.20f);
		g("재생", WILD, ItemSpriteSheet.ELIXIR_HONEY).v(REGEN, 0.50f).v(HP, 8);
		g("치유의 손길", WILD, ItemSpriteSheet.HEALING_DART).v(HEALAMP, 0.40f).v(F_HEAL, 0.20f);
		g("끈질긴 생존자", WILD, ItemSpriteSheet.PETRIFIED_SEED).v(LAST_STAND, 1);
		g("야생의 피", WILD, ItemSpriteSheet.SEED_STARFLOWER).v(K_HEAL, 3).v(REGEN, 0.20f);
		p("생명의 나무", WILD, ItemSpriteSheet.WAND_REGROWTH).v(HP_PCT, 0.20f).v(REGEN, 0.60f).v(HEALAMP, 0.30f);
		p("불굴의 생존자", WILD, ItemSpriteSheet.MOSSY_CLUMP).v(LAST_STAND, 1).v(REVIVE, 1);
		p("대지의 가호", WILD, ItemSpriteSheet.STONE_ENCHANT).v(HP, 30).v(ARMOR, 2).v(TAKEN, 0.08f);
		p("영원한 포만", WILD, ItemSpriteSheet.VANILLA_CAKE).v(HUNGER, 0.60f).v(EAT_HEAL, 0.40f).v(HP, 15);

		// ------------------------------------------------ fury
		s("근력 단련", FURY, ItemSpriteSheet.GAUNTLETS).v(STR, 1);
		g("연격", FURY, ItemSpriteSheet.GLOVES).v(U_FLURRY, 1);
		g("최후의 일격", FURY, ItemSpriteSheet.WAR_HAMMER).v(U_FINISHER, 1);
		s("칼바람", FURY, ItemSpriteSheet.SWORD).v(U_GALE, 1);
		g("광전사의 분노", FURY, ItemSpriteSheet.BATTLE_AXE).v(RAGE, 0.06f).v(LOW_DMG, 0.10f);
		g("회전 베기", FURY, ItemSpriteSheet.LONGSWORD).v(CLEAVE, 0.35f);
		g("메아리 칼날", FURY, ItemSpriteSheet.RUNIC_BLADE).v(P_ECHO, 0.20f);
		g("전쟁의 북", FURY, ItemSpriteSheet.ARTIFACT_HORN4).v(K_BLESS, 5).v(K_HASTE, 1);
		g("거인의 힘", FURY, ItemSpriteSheet.RING_AMETHYST).v(STR, 2).v(HP, 8);
		p("전쟁의 신", FURY, ItemSpriteSheet.EXOTIC_TIWAZ).v(DMG_PCT, 0.25f).v(ASPD, 0.15f).v(RAGE, 0.05f);
		p("학살자", FURY, ItemSpriteSheet.WAR_SCYTHE).v(CLEAVE, 0.60f).v(K_HASTE, 2).v(DMG_PCT, 0.10f);
		p("쌍검", FURY, ItemSpriteSheet.SCIMITAR).v(P_ECHO, 0.35f).v(ASPD, 0.10f);
		p("대지 분쇄", FURY, ItemSpriteSheet.SHRAPNEL_BOMB).v(P_BLAST, 0.20f).v(P_KNOCK, 0.10f).v(DMG_PCT, 0.10f);

		// ------------------------------------------------ unique effects: each one changes how a fight plays out
		s("상처 벌리기", BLOOD, ItemSpriteSheet.SICKLE).v(U_REOPEN, 1).v(P_BLEED, 0.08f);
		g("사냥의 흥분", BLOOD, ItemSpriteSheet.PHANTOM_MEAT).v(U_THRILL, 1);
		g("피의 갑옷", BLOOD, ItemSpriteSheet.ARMOR_SCALE).v(U_BLOODARMOR, 1).v(LIFESTEAL, 0.05f);
		p("피의 계약", BLOOD, ItemSpriteSheet.ARTIFACT_CHALICE3).v(U_BLOODPACT, 1).v(LIFESTEAL, 0.05f);
		p("망령 사역", BLOOD, ItemSpriteSheet.TOMB).v(K_WRAITH, 0.15f).v(W_CAP, 1);

		s("잿불 갑옷", FLAME, ItemSpriteSheet.ARMOR_LEATHER).v(U_EMBERARMOR, 1).v(H_BURN, 0.10f);
		g("연소", FLAME, ItemSpriteSheet.TORCH).v(U_SPREAD, 1).v(P_BURN, 0.06f);
		g("증기 폭발", FLAME, ItemSpriteSheet.BREW_AQUA).v(U_STEAM, 1);
		g("불길 돌진", FLAME, ItemSpriteSheet.ARTIFACT_SANDALS).v(U_FIREDASH, 1).v(IMM_FIRE, 1);
		p("화염 정령", FLAME, ItemSpriteSheet.SUMMON_ELE_FIRE).v(U_FIREELE, 1).v(IMM_FIRE, 1);

		s("냉기 흡수", FROST, ItemSpriteSheet.EXOTIC_CRYSTALS).v(U_COLDHEAL, 1);
		s("얼음 위 춤", FROST, ItemSpriteSheet.ARTIFACT_SHOES).v(U_ICEDANCE, 1).v(P_CHILL, 0.05f);
		g("산산조각", FROST, ItemSpriteSheet.SHARD).v(U_SHATTER, 1).v(P_FREEZE, 0.04f);
		g("동결 갑옷", FROST, ItemSpriteSheet.ARMOR_PLATE).v(U_FROSTARMOR, 1).v(H_FROST, 0.10f);
		p("서리 정령", FROST, ItemSpriteSheet.SUMMON_ELE_FROST).v(U_FROSTELE, 1).v(IMM_COLD, 1);

		s("정전기 축적", STORM, ItemSpriteSheet.STONE_SHOCK).v(U_STATIC, 1);
		s("번개 발걸음", STORM, ItemSpriteSheet.ARTIFACT_BOOTS).v(U_DOORHASTE, 1).v(MSPD, 0.05f);
		g("물 전도", STORM, ItemSpriteSheet.WATERSKIN).v(U_CONDUCT, 1).v(P_LIGHTNING, 0.05f);
		g("질풍 일격", STORM, ItemSpriteSheet.ARTIFACT_GREAVES).v(U_GUST, 1);
		p("시간 가속", STORM, ItemSpriteSheet.ARTIFACT_HOURGLASS).v(U_TIMEWARP, 1).v(ASPD, 0.10f);

		s("해독 흡수", VENOM, ItemSpriteSheet.EXOTIC_JADE).v(U_ANTIDOTE, 1);
		s("독안개 장막", VENOM, ItemSpriteSheet.SEED_SORROWMOSS).v(U_GASCLOAK, 1).v(IMM_POISON, 1);
		g("역병 전염", VENOM, ItemSpriteSheet.BLOB).v(U_PLAGUE, 1).v(P_POISON, 0.06f);
		g("맹독 누적", VENOM, ItemSpriteSheet.POISON_DART).v(U_DEEPEN, 1).v(P_POISON, 0.06f);
		p("부패의 손길", VENOM, ItemSpriteSheet.WAND_CORROSION).v(U_ROT, 1).v(P_POISON, 0.15f);

		s("벽 그림자", SHADOW, ItemSpriteSheet.CLOAK_SCRAP).v(U_WALLSHADOW, 1);
		s("도둑의 손", SHADOW, ItemSpriteSheet.ARTIFACT_KEY).v(U_THIEF, 1).v(SURPRISE, 0.10f);
		g("그림자 이동", SHADOW, ItemSpriteSheet.SCROLL_NAUDIZ).v(U_SHADOWSTEP, 1);
		g("배신의 칼", SHADOW, ItemSpriteSheet.DIRK).v(U_BETRAY, 1).v(P_BLIND, 0.06f);
		p("거울 그림자", SHADOW, ItemSpriteSheet.EXOTIC_MANNAZ).v(U_MIRROR, 1).v(EVA, 3);

		s("벽 등지기", GUARD, ItemSpriteSheet.STATUE).v(U_WALLBACK, 1);
		s("방패 밀치기", GUARD, ItemSpriteSheet.ROUND_SHIELD).v(U_SHOVE, 1);
		g("반격", GUARD, ItemSpriteSheet.ARMOR_DUELIST).v(U_COUNTER, 1).v(EVA, 2);
		g("굳건함", GUARD, ItemSpriteSheet.STONE_AUGMENTATION).v(U_STEADFAST, 1).v(ARMOR, 1);
		p("불굴", GUARD, ItemSpriteSheet.ARMOR_WARRIOR).v(U_UNBREAK, 1).v(HP, 10);

		s("마력 순환", ARCANE, ItemSpriteSheet.ENERGY).v(U_MANACYCLE, 1);
		s("주문 보호", ARCANE, ItemSpriteSheet.POTION_SILVER).v(U_POTIONWARD, 1);
		g("주문 공명", ARCANE, ItemSpriteSheet.SCROLL_SOWILO).v(U_SCROLLRES, 1).v(WAND_CHARGE, 0.05f);
		g("연쇄 마법", ARCANE, ItemSpriteSheet.WAND_MAGIC_MISSILE).v(U_CHAINSPELL, 1).v(WAND_PCT, 0.05f);
		p("원소 폭풍", ARCANE, ItemSpriteSheet.SUMMON_ELE_CHAOS).v(U_ELESTORM, 1).v(WAND_PCT, 0.10f);

		s("사냥 표식", HUNT, ItemSpriteSheet.STONE_DETECT).v(U_MARK, 1);
		s("덫꾼", HUNT, ItemSpriteSheet.TRAP_MECHANISM).v(U_TRAPPER, 1).v(TRAP_SENSE, 1);
		g("도탄", HUNT, ItemSpriteSheet.SHURIKEN).v(U_RICOCHET, 1).v(THROWN_PCT, 0.05f);
		g("원거리 저격", HUNT, ItemSpriteSheet.SPYGLASS).v(U_SNIPE, 1).v(ACC, 2);
		p("영혼 매", HUNT, ItemSpriteSheet.ARTIFACT_HORN2).v(U_HAWK, 1).v(VISION, 1);

		s("이자", GREED, ItemSpriteSheet.POUCH).v(U_INTEREST, 1);
		s("길바닥 동전", GREED, ItemSpriteSheet.TOKEN).v(U_GRASSCOIN, 1);
		g("금화 탄환", GREED, ItemSpriteSheet.THROWING_STONE).v(U_GOLDSHOT, 1);
		g("부자의 위엄", GREED, ItemSpriteSheet.MASK).v(U_DIGNITY, 1).v(St.GOLD, 0.10f);
		p("보물 감별사", GREED, ItemSpriteSheet.SPYGLASS).v(U_APPRAISE, 1).v(LOOT, 0.15f);

		s("포식", WILD, ItemSpriteSheet.CARPACCIO).v(U_DEVOUR, 1);
		s("든든한 배", WILD, ItemSpriteSheet.RATION).v(U_FULLBELLY, 1);
		g("약초 달인", WILD, ItemSpriteSheet.SEED_SUNGRASS).v(U_HERBALIST, 1).v(HEALAMP, 0.10f);
		g("동면", WILD, ItemSpriteSheet.STEAMED_FISH).v(U_HIBERNATE, 1).v(REGEN, 0.10f);
		p("생명의 씨앗", WILD, ItemSpriteSheet.PETRIFIED_SEED).v(U_SEED, 1).v(HP, 10);

		s("위협", FURY, ItemSpriteSheet.STONE_FEAR).v(U_INTIMIDATE, 1);
		s("돌진", FURY, ItemSpriteSheet.SPEAR).v(U_CHARGE, 1);
		g("피의 광란", FURY, ItemSpriteSheet.SCROLL_RAIDO).v(U_FRENZY, 1);
		g("처형 휘두르기", FURY, ItemSpriteSheet.GREATAXE).v(U_EXECSWING, 1).v(DMG_PCT, 0.05f);
		p("전쟁의 화신", FURY, ItemSpriteSheet.CROWN).v(U_AVATAR, 1).v(BOSS, 0.10f);

		// ------------------------------------------------ jackpots: almost never, but unforgettable when they do
		p("단두대", FURY, ItemSpriteSheet.GREATSWORD).v(U_GUILLOTINE, 1).v(EXECUTE, 0.15f);
		g("연쇄 폭발", FLAME, ItemSpriteSheet.DBL_BOMB).v(U_CHAINBLAST, 1).v(K_BLAST, 0.05f);
		g("잭팟", GREED, ItemSpriteSheet.EBONY_CHEST).v(U_JACKPOT, 1).v(K_GOLD, 0.05f);
		p("운명의 주사위", ARCANE, ItemSpriteSheet.UNSTABLE_SPELL).v(U_DICE, 1).v(WAND_PCT, 0.05f);

		// ------------------------------------------------ growing augments: weak at first, monstrous later
		g("피의 수집가", BLOOD, ItemSpriteSheet.BLOOD_VIAL).v(GR_BLOOD, 1).v(HP, 4);
		g("학살의 기억", FURY, ItemSpriteSheet.RAT_SKULL).v(GR_SLAUGHTER, 1);
		g("영혼 수확", SHADOW, ItemSpriteSheet.SCROLL_MANNAZ).v(GR_SOUL, 1).v(CRIT, 0.02f);
		g("단련된 강철", GUARD, ItemSpriteSheet.LIQUID_METAL).v(GR_STEEL, 1).v(ARMOR, 1);
		g("끈질긴 여정", WILD, ItemSpriteSheet.ARTIFACT_SANDALS).v(GR_JOURNEY, 1);
		g("심연의 지식", ARCANE, ItemSpriteSheet.ARTIFACT_TOME).v(GR_ABYSS, 1).v(WAND_PCT, 0.04f);
		g("노련한 사냥꾼", HUNT, ItemSpriteSheet.BOW_FRAGMENT).v(GR_VETERAN, 1);
		g("꺼지지 않는 불", FLAME, ItemSpriteSheet.CANDLE).v(GR_FIRE, 1).v(P_BURN, 0.05f);
		g("복리", GREED, ItemSpriteSheet.GOLDEN_KEY).v(GR_COMPOUND, 1);
		g("맹독 연구", VENOM, ItemSpriteSheet.VIAL).v(GR_VENOM, 1).v(P_POISON, 0.05f);
		g("빙하기", FROST, ItemSpriteSheet.GEO_BOULDER).v(GR_ICE, 1).v(P_FREEZE, 0.05f);
		p("폭풍의 눈", STORM, ItemSpriteSheet.SPARKLING_POTION).v(GR_STORM, 1).v(P_LIGHTNING, 0.10f);

		// ------------------------------------------------ gambles: a large upside with a price
		p("유리 대포", FURY, ItemSpriteSheet.EXOTIC_YNGVI).gamble().v(SURE_CRIT, 1).v(TAKEN, -1.50f);
		g("광기의 계약", SHADOW, ItemSpriteSheet.SCROLL_ODAL).gamble().v(CRIT, 0.25f).v(CRITD, 0.50f).v(TAKEN, -0.20f);
		g("피의 서약", BLOOD, ItemSpriteSheet.BLOOD_VIAL).gamble().v(LIFESTEAL, 0.15f).v(REGEN, -0.80f);
		g("빚진 시간", STORM, ItemSpriteSheet.ARTIFACT_HOURGLASS).gamble().v(ASPD, 0.30f).v(MSPD, 0.30f).v(TIME_DEBT, -1);
		g("침묵의 서약", FURY, ItemSpriteSheet.SCROLL_TIWAZ).gamble().v(MELEE_PCT, 0.60f).v(NO_WAND, -1);
		g("챔피언의 시련", GREED, ItemSpriteSheet.CROWN).gamble().v(EXP, 2.0f).v(LOOT, 2.0f).v(CHAMPIONS, -1);
		g("탐욕의 대가", GREED, ItemSpriteSheet.GOLD).gamble().v(St.GOLD, 1.50f).v(LOOT, 0.60f).v(HP_PCT, -0.15f);
		p("금단의 마법", ARCANE, ItemSpriteSheet.EXOTIC_MAGENTA).gamble().v(WAND_PCT, 0.60f).v(WAND_FREE, 0.25f).v(HP_PCT, -0.25f);
		g("저주받은 갑주", GUARD, ItemSpriteSheet.ARMOR_PLATE).gamble().v(ARMOR, 8).v(TAKEN, 0.10f).v(MSPD, -0.25f).v(ASPD, -0.15f);
		g("굶주린 짐승", WILD, ItemSpriteSheet.MEAT).gamble().v(DMG_PCT, 0.25f).v(K_HEAL, 5).v(HUNGER, -1.00f);
		p("죽음의 춤", SHADOW, ItemSpriteSheet.EXOTIC_NAUDIZ).gamble().v(DODGE, 0.30f).v(EVA_PCT, 0.30f).v(HEALAMP, -0.50f).v(HP_PCT, -0.20f);

		// ------------------------------------------------ legendary combinations, unlocked by holding their parts
		l("얼어붙은 폭풍", STORM, ItemSpriteSheet.WAND_LIGHTNING, "연쇄 번개", "빙결의 일격")
				.v(P_LIGHTNING, 0.20f).v(P_FREEZE, 0.10f).v(ARC_FREEZE, 1);
		l("지옥의 파수꾼", FLAME, ItemSpriteSheet.ELIXIR_DRAGON, "이글거리는 기운", "고슴도치")
				.v(AURA_FIRE, 0.30f).v(THORNS, 0.50f).v(H_BURN, 0.60f).v(IMM_FIRE, 1);
		l("흡혈 군단", BLOOD, ItemSpriteSheet.ARTIFACT_CHALICE3, "흡혈귀의 송곳니", "회전 베기")
				.v(LIFESTEAL, 0.10f).v(CLEAVE, 0.50f).v(CLEAVE_STEAL, 1);
		l("그림자 처형인", SHADOW, ItemSpriteSheet.ASSASSINS_BLADE, "암살자의 비수", "처형인")
				.v(CRIT, 0.15f).v(EXECUTE, 0.40f).v(EXECUTE_CRIT, 1);
		l("역병 폭풍", VENOM, ItemSpriteSheet.EXOTIC_JADE, "역병의 숨결", "질풍 처치")
				.v(K_GAS, 0.50f).v(K_HASTE, 3).v(IMM_POISON, 1).v(POISONED_DMG, 0.25f);
		l("포식자", WILD, ItemSpriteSheet.CARPACCIO, "포식", "피의 굶주림")
				.v(L_DEVOUR, 1).v(HP, 20).v(K_HEAL, 3);
		l("불멸의 군주", WILD, ItemSpriteSheet.ANKH, "불사의 의지", "재생")
				.v(LAST_STAND, 1).v(LAST_STAND_HEAL, 1).v(REGEN, 1.00f).v(HP, 20);
		l("황금 폭풍", GREED, ItemSpriteSheet.CRYSTAL_CHEST, "황금의 손", "약탈자")
				.v(St.GOLD, 0.80f).v(K_GOLD, 0.40f).v(GOLD_DMG, 0.30f);
		l("그림자 분신", SHADOW, ItemSpriteSheet.RUNIC_BLADE, "메아리 칼날", "거울 그림자")
				.v(L_TWIN, 1).v(CRIT, 0.10f);
		l("리플레이 유령", ARCANE, ItemSpriteSheet.ARTIFACT_HOURGLASS, "시간 왜곡", "그림자 춤")
				.v(L_REPLAY, 1).v(EVA, 3);
		l("사냥의 왕", HUNT, ItemSpriteSheet.SPIRIT_BOW, "명사수", "투척의 달인")
				.v(THROWN_PCT, 0.40f).v(ACC_PCT, 0.25f).v(THROW_DUR, 0.50f).v(CRIT, 0.10f);
		l("피의 폭발", BLOOD, ItemSpriteSheet.EXOTIC_CRIMSON, "피의 분노", "지옥불 폭발")
				.v(BLOOD_RAGE, 0.40f).v(K_BLAST, 0.40f).v(K_FIRE, 0.20f);
		l("비전 폭풍", ARCANE, ItemSpriteSheet.WAND_WARDING, "비전 구체", "연쇄 번개")
				.v(ORB, 1.50f).v(P_LIGHTNING, 0.15f).v(WAND_PCT, 0.15f);
		l("대지의 분노", FURY, ItemSpriteSheet.GREATSWORD, "거인의 힘", "바위 같은 몸")
				.v(STR, 3).v(HP, 30).v(ARMOR, 3).v(P_BLAST, 0.15f);

		l("끝없는 학살", FURY, ItemSpriteSheet.RAT_SKULL, "학살의 기억", "피의 수집가")
				.v(GR_DOUBLE, 1).v(DMG_PCT, 0.15f).v(HP, 10);
		l("불멸의 행군", WILD, ItemSpriteSheet.ARTIFACT_GREAVES, "끈질긴 여정", "단련된 강철")
				.v(GR_MARCH, 1).v(HP, 20).v(ARMOR, 2);
		l("지옥 증기", FLAME, ItemSpriteSheet.BREW_INFERNAL, "연소", "증기 폭발")
				.v(L_STEAM, 1).v(P_BURN, 0.15f).v(P_CHILL, 0.15f);
		l("그림자 학살자", SHADOW, ItemSpriteSheet.KATANA, "그림자 이동", "배신의 칼")
				.v(L_SLAYER, 1).v(CRIT, 0.10f).v(CRITD, 0.30f);
		l("정령 군주", ARCANE, ItemSpriteSheet.SUMMON_ELE, "화염 정령", "서리 정령")
				.v(L_ELEMENT, 1).v(IMM_ELEC, 1);
		l("시간의 지배자", STORM, ItemSpriteSheet.SUNDIAL, "시간 가속", "처형 휘두르기")
				.v(L_TIME, 1).v(ASPD, 0.10f).v(MSPD, 0.10f);

		// ------------------------------------------------ legends of three materials
		l("번개 질주자", STORM, ItemSpriteSheet.ARTIFACT_BOOTS, "정전기 축적", "질풍 일격", "돌진")
				.v(L_DASH, 1).v(MSPD, 0.15f);
		l("움직이지 않는 산", GUARD, ItemSpriteSheet.GREATSHIELD, "굳건함", "반격", "방패 밀치기")
				.v(L_MOUNTAIN, 1).v(ARMOR, 3).v(HP, 20);
		l("운명의 룰렛", GREED, ItemSpriteSheet.UNSTABLE_SPELL, "잭팟", "운명의 주사위", "이자")
				.v(L_ROULETTE, 1).v(St.GOLD, 0.30f);
		l("얼음 무희", FROST, ItemSpriteSheet.ARTIFACT_SHOES, "얼음 위 춤", "산산조각", "냉기 흡수")
				.v(L_ICEDANCER, 1).v(IMM_COLD, 1).v(EVA, 3);
		l("독의 정원", VENOM, ItemSpriteSheet.SEED_SORROWMOSS, "독안개 장막", "약초 달인", "부패의 손길")
				.v(L_GARDEN, 1).v(IMM_POISON, 1).v(REGEN, 0.30f);

		for (Aug a : ALL.values()){
			if (a.requires == null) continue;
			for (String r : a.requires){
				if (!ALL.containsKey(r)) throw new RuntimeException("legendary " + a.name + " needs unknown " + r);
			}
		}
	}
}
