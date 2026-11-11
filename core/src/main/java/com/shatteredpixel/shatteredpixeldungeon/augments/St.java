/*
 * Augment challenge - every numeric effect an augment or set bonus can carry.
 * Values of all owned augments and active set bonuses are simply added together.
 * The description of an augment is generated from these, so text and effect always agree.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments;

public enum St {

	// ---------------------------------------------------------------- core stats
	HP          (K.FLAT,   "최대 체력 +{}"),
	HP_PCT      (K.PCT,    "최대 체력 +{}%"),
	ACC         (K.FLAT,   "명중 +{}"),
	ACC_PCT     (K.PCT,    "명중 +{}%"),
	EVA         (K.FLAT,   "회피 +{}"),
	EVA_PCT     (K.PCT,    "회피 +{}%"),
	ARMOR       (K.FLAT,   "방어력 +{}"),
	STR         (K.FLAT,   "힘 +{}"),

	// ---------------------------------------------------------------- offense
	DMG_PCT     (K.PCT,    "공격 피해 +{}%"),
	DMG_FLAT    (K.FLAT,   "공격 피해 +{}"),
	MELEE_PCT   (K.PCT,    "근접 공격 피해 +{}%"),
	THROWN_PCT  (K.PCT,    "투척 무기 피해 +{}%"),
	WAND_PCT    (K.PCT,    "마법 막대 피해 +{}%"),
	ASPD        (K.PCT,    "공격 속도 +{}%"),
	MSPD        (K.PCT,    "이동 속도 +{}%"),
	CRIT        (K.PCT,    "치명타 확률 +{}%"),
	CRITD       (K.PCT,    "치명타 피해 +{}%"),
	EXECUTE     (K.PCT,    "체력 30% 이하인 적에게 피해 +{}%"),
	OPENER      (K.PCT,    "체력이 가득 찬 적에게 피해 +{}%"),
	SURPRISE    (K.PCT,    "기습 공격 피해 +{}%"),
	BOSS        (K.PCT,    "보스·정예 적에게 피해 +{}%"),
	LOW_DMG     (K.PCT,    "체력 40% 이하일 때 공격 피해 +{}%"),
	FULL_DMG    (K.PCT,    "체력이 가득 찼을 때 공격 피해 +{}%"),
	BURNING_DMG (K.PCT,    "불타는 적에게 피해 +{}%"),
	CHILLED_DMG (K.PCT,    "냉기·빙결 상태인 적에게 피해 +{}%"),
	POISONED_DMG(K.PCT,    "중독된 적에게 피해 +{}%"),
	DEBUFF_DMG  (K.PCT,    "해로운 효과에 걸린 적에게 피해 +{}%"),
	BLOOD_RAGE  (K.PCT,    "잃은 체력에 비례해 공격 피해 증가 (최대 +{}%)"),
	RAGE        (K.PCT,    "연속으로 적중할 때마다 피해 +{}% (최대 5중첩)"),

	// ---------------------------------------------------------------- defense and recovery
	TAKEN       (K.PCT,    "받는 피해 -{}%"),
	LOW_TAKEN   (K.PCT,    "체력 40% 이하일 때 받는 피해 -{}%"),
	DODGE       (K.PCT,    "{}% 확률로 공격을 완전히 회피"),
	THORNS      (K.PCT,    "근접 공격을 받으면 피해의 {}%를 되돌려줌"),
	LIFESTEAL   (K.PCT,    "준 피해의 {}%만큼 체력 회복"),
	REGEN       (K.PCT,    "자연 회복 속도 +{}%"),
	HEALAMP     (K.PCT,    "받는 치유 효과 +{}%"),
	HUNGER      (K.PCT,    "허기 증가 속도 -{}%"),
	EAT_HEAL    (K.PCT,    "음식을 먹으면 체력 {}% 회복"),
	GRASS_EVA   (K.PCT,    "풀 위에서 회피 +{}%"),
	GOLD_ARMOR  (K.FLAT,   "보유 골드 300당 방어력 +1 (최대 +{})"),
	WATER_REGEN (K.TEXT,   "물 위에서 자연 회복 3배"),

	// ---------------------------------------------------------------- utility
	VISION      (K.FLAT,   "시야 +{}"),
	STEALTH     (K.FLAT,   "은신 +{}"),
	GOLD        (K.PCT,    "골드 획득량 +{}%"),
	LOOT        (K.PCT,    "적의 전리품 드롭 확률 +{}%"),
	EXP         (K.PCT,    "경험치 획득량 +{}%"),
	DISCOUNT    (K.PCT,    "상점 가격 -{}%"),
	WAND_CHARGE (K.PCT,    "마법 막대 충전 속도 +{}%"),
	ARTI_CHARGE (K.PCT,    "유물 충전 속도 +{}%"),
	WAND_FREE   (K.PCT,    "마법 막대 사용 시 {}% 확률로 충전 소모 없음"),
	THROW_DUR   (K.PCT,    "투척 무기 내구도 +{}%"),
	TRAP_SENSE  (K.TEXT,   "주변 2칸의 숨겨진 함정과 문을 자동으로 발견"),

	// ---------------------------------------------------------------- on hit
	P_BURN      (K.PCT,    "공격 시 {}% 확률로 적을 불태움"),
	P_POISON    (K.PCT,    "공격 시 {}% 확률로 적을 중독시킴"),
	P_CHILL     (K.PCT,    "공격 시 {}% 확률로 적에게 냉기"),
	P_FREEZE    (K.PCT,    "공격 시 {}% 확률로 적을 빙결시킴"),
	P_PARA      (K.PCT,    "공격 시 {}% 확률로 적을 마비시킴"),
	P_BLEED     (K.PCT,    "공격 시 {}% 확률로 출혈"),
	P_CRIPPLE   (K.PCT,    "공격 시 {}% 확률로 불구"),
	P_WEAK      (K.PCT,    "공격 시 {}% 확률로 약화"),
	P_VULN      (K.PCT,    "공격 시 {}% 확률로 취약"),
	P_TERROR    (K.PCT,    "공격 시 {}% 확률로 공포"),
	P_BLIND     (K.PCT,    "공격 시 {}% 확률로 실명"),
	P_SLOW      (K.PCT,    "공격 시 {}% 확률로 둔화"),
	P_HEX       (K.PCT,    "공격 시 {}% 확률로 저주 (명중·회피 감소)"),
	P_LIGHTNING (K.PCT,    "공격 시 {}% 확률로 연쇄 번개 (피해의 50%)"),
	P_BLAST     (K.PCT,    "공격 시 {}% 확률로 폭발 (주변 적에게 피해의 60%)"),
	P_KNOCK     (K.PCT,    "공격 시 {}% 확률로 적을 밀쳐냄"),
	P_ECHO      (K.PCT,    "공격 시 {}% 확률로 메아리 일격 (피해의 60% 추가)"),
	CLEAVE      (K.PCT,    "근접 공격이 주변의 다른 적에게 피해의 {}%를 줌"),

	// ---------------------------------------------------------------- on kill
	K_HEAL      (K.FLAT,   "적 처치 시 체력 {} 회복"),
	K_SHIELD    (K.FLAT,   "적 처치 시 보호막 +{}"),
	K_GOLD      (K.PCT,    "적 처치 시 {}% 확률로 골드 드롭"),
	K_BLAST     (K.PCT,    "적 처치 시 {}% 확률로 시체 폭발"),
	K_HASTE     (K.FLAT,   "적 처치 시 {}턴간 가속"),
	K_INVIS     (K.PCT,    "적 처치 시 {}% 확률로 2턴간 투명"),
	K_GAS       (K.PCT,    "적 처치 시 {}% 확률로 독가스 분출"),
	K_FIRE      (K.PCT,    "적 처치 시 {}% 확률로 화염 폭발"),
	K_FROST     (K.PCT,    "적 처치 시 {}% 확률로 서리 폭발"),
	K_RECHARGE  (K.PCT,    "적 처치 시 {}% 확률로 마법 막대 충전 +1"),
	K_WRAITH    (K.PCT,    "적 처치 시 {}% 확률로 망령 아군 소환"),
	K_BLESS     (K.FLAT,   "적 처치 시 {}턴간 축복"),

	// ---------------------------------------------------------------- when hit
	H_SHIELD    (K.PCT,    "피격 시 {}% 확률로 보호막 (최대 체력의 10%)"),
	H_FROST     (K.PCT,    "피격 시 {}% 확률로 주변에 서리 폭발"),
	H_SHOCK     (K.PCT,    "근접 피격 시 {}% 확률로 공격자 감전"),
	H_BURN      (K.PCT,    "근접 피격 시 {}% 확률로 공격자를 불태움"),
	H_POISON    (K.PCT,    "근접 피격 시 {}% 확률로 공격자 중독"),
	H_HASTE     (K.PCT,    "피격 시 {}% 확률로 2턴간 가속"),

	// ---------------------------------------------------------------- arriving on a new floor
	F_SHIELD    (K.PCT,    "새 층에 처음 도착하면 보호막 (최대 체력의 {}%)"),
	F_HEAL      (K.PCT,    "새 층에 처음 도착하면 체력 {}% 회복"),
	F_MAP       (K.PCT,    "새 층에 처음 도착하면 {}% 확률로 지도 공개"),
	F_MIND      (K.FLAT,   "새 층에 처음 도착하면 {}턴간 적 위치 감지"),
	F_GOLD      (K.FLAT,   "새 층에 처음 도착하면 골드 획득 (층 × {})"),
	F_RECHARGE  (K.FLAT,   "새 층에 처음 도착하면 {}턴간 마법 막대 고속 충전"),

	// ---------------------------------------------------------------- auras and timed effects
	AURA_FIRE   (K.PCT,    "매 턴 인접한 적을 {}% 확률로 불태움"),
	AURA_FROST  (K.PCT,    "매 턴 인접한 적에게 {}% 확률로 냉기"),
	AURA_SHOCK  (K.PCT,    "매 턴 인접한 적을 {}% 확률로 감전"),
	ORB         (K.PCT,    "6턴마다 가까운 적에게 비전 구체 발사 (위력 {}%)"),

	// ---------------------------------------------------------------- unique effects
	LAST_STAND  (K.TEXT,   "층마다 한 번, 치명상을 체력 1로 버티고 3턴간 무적"),
	REVIVE      (K.TEXT,   "한 번, 죽음에서 체력 50%로 되살아남"),
	BLINK_LOW   (K.TEXT,   "체력이 25% 이하가 되면 층마다 한 번 순간이동"),
	IMM_FIRE    (K.TEXT,   "화염 면역"),
	IMM_POISON  (K.TEXT,   "독·독가스 면역"),
	IMM_COLD    (K.TEXT,   "냉기·빙결 면역"),
	IMM_ELEC    (K.TEXT,   "전기 면역"),
	IMM_PARA    (K.TEXT,   "마비 면역"),

	// ---------------------------------------------------------------- legendary combination effects
	ARC_FREEZE     (K.TEXT,  "연쇄 번개에 맞은 적이 잠시 빙결"),
	CLEAVE_STEAL   (K.TEXT,  "회전 베기 피해로도 체력 회복 (최소 10%)"),
	EXECUTE_CRIT   (K.TEXT,  "체력 30% 이하인 적에게는 항상 치명타"),
	LAST_STAND_HEAL(K.TEXT,  "최후의 저항이 발동하면 체력 50% 회복"),
	GOLD_DMG       (K.PCT,   "보유 골드 100당 공격 피해 +1% (최대 +{}%)"),

	// ---------------------------------------------------------------- unique effects of each family
	//blood
	U_REOPEN    (K.TEXT,  "출혈 중인 적을 공격하면 남은 출혈이 한꺼번에 터짐"),
	U_THRILL    (K.TEXT,  "적을 처치하면 3턴간 공격 속도 +40%"),
	U_BLOODARMOR(K.TEXT,  "체력이 가득 차 있으면 흡혈 회복량이 보호막으로 쌓임 (최대 체력의 20%까지)"),
	U_BLOODPACT (K.TEXT,  "체력이 50% 이하가 되면 층마다 한 번, 시야 안 모든 적의 생명력을 빨아들임"),
	W_CAP       (K.TEXT,  "망령 아군은 최대 2체까지 함께함"),
	//flame
	U_EMBERARMOR(K.TEXT,  "불타는 적에게 받는 피해 -30%"),
	U_SPREAD    (K.TEXT,  "불타는 적을 처치하면 불길이 주변 적에게 옮겨붙음"),
	U_STEAM     (K.TEXT,  "불타는 적이 냉기·빙결에 걸리면 증기 폭발 (주변에 큰 피해)"),
	U_FIREDASH  (K.TEXT,  "문을 지나면 주변 적을 불태움 (5턴마다)"),
	U_FIREELE   (K.TEXT,  "새 층에 처음 도착하면 화염 정령 아군 소환"),
	//frost
	U_COLDHEAL  (K.TEXT,  "냉기·빙결 상태인 적을 처치하면 체력 회복 (3 + 층의 절반)"),
	U_ICEDANCE  (K.TEXT,  "냉기·빙결에 걸린 적이 곁에 있으면 회피 +30%"),
	U_SHATTER   (K.TEXT,  "빙결된 적에게 주는 피해 2배, 얼음이 산산조각 남"),
	U_FROSTARMOR(K.TEXT,  "냉기·빙결 상태인 적에게 받는 피해 -40%"),
	U_FROSTELE  (K.TEXT,  "새 층에 처음 도착하면 서리 정령 아군 소환"),
	//storm
	U_STATIC    (K.TEXT,  "이동할 때마다 전하가 쌓이고 (최대 10), 다음 공격에 전하만큼 번개 피해"),
	U_DOORHASTE (K.TEXT,  "문을 지나면 3턴간 가속 (10턴마다)"),
	U_CONDUCT   (K.TEXT,  "물 위의 적을 공격하면 물을 타고 번개가 퍼짐"),
	U_GUST      (K.TEXT,  "같은 방향으로 3칸 이상 달린 뒤 첫 공격 피해 +60%, 적을 밀쳐냄"),
	U_TIMEWARP  (K.TEXT,  "5번째 공격마다 시간이 2턴간 멈춤"),
	//venom
	U_ANTIDOTE  (K.TEXT,  "독 피해를 받으면 오히려 그만큼 체력 회복"),
	U_GASCLOAK  (K.TEXT,  "독가스 속에서 회피 +40%, 은신 +2"),
	U_PLAGUE    (K.TEXT,  "중독된 적이 죽으면 독이 주변 적에게 옮겨감"),
	U_DEEPEN    (K.TEXT,  "중독된 적을 공격할 때마다 독이 더 깊어짐"),
	U_ROT       (K.TEXT,  "적이 받는 독 피해 2배, 독으로 죽은 적은 독가스를 남김"),
	POISON_PLUS (K.FLAT,  "적이 받는 독 피해 +{}"),
	//shadow
	U_WALLSHADOW(K.TEXT,  "벽에 붙어 있으면 은신 +2, 기습 피해 +25%"),
	U_THIEF     (K.TEXT,  "기습 공격으로 적을 처치하면 30% 확률로 물건을 훔침"),
	U_SHADOWSTEP(K.TEXT,  "적을 처치하면 시야 안 다른 적의 곁으로 그림자 이동 (5턴마다)"),
	U_BETRAY    (K.TEXT,  "공포·실명·마비 상태인 적에게 피해 +50%, 치명타 확률 +20%"),
	U_MIRROR    (K.TEXT,  "새 층에 처음 도착하면 그림자 환영 2체 소환"),
	//guard
	U_WALLBACK  (K.TEXT,  "벽에 붙어 있으면 받는 피해 -15%"),
	U_SHOVE     (K.TEXT,  "근접 피격 시 20% 확률로 공격자를 밀쳐내고 기절시킴"),
	U_COUNTER   (K.TEXT,  "적의 공격을 회피하면 즉시 반격"),
	U_STEADFAST (K.TEXT,  "같은 자리에 머무는 턴마다 보호막 +1 (최대 체력의 15%까지)"),
	U_UNBREAK   (K.TEXT,  "한 번에 받는 피해가 최대 체력의 25%를 넘지 않음"),
	//arcane
	U_MANACYCLE (K.TEXT,  "마법 막대를 쓸 때마다 체력 회복 (2 + 층의 1/3)"),
	U_POTIONWARD(K.TEXT,  "물약을 마시면 2턴간 무적"),
	U_SCROLLRES (K.TEXT,  "주문서를 읽으면 모든 마법 막대 충전 +1"),
	U_CHAINSPELL(K.TEXT,  "마법 막대 피해가 25% 확률로 근처의 다른 적에게 튕김 (50%)"),
	U_ELESTORM  (K.TEXT,  "마법 막대에 맞은 적 주변에 30% 확률로 원소 폭발"),
	//hunt
	U_MARK      (K.TEXT,  "같은 적을 연속으로 공격할 때마다 피해 +5% (최대 +40%)"),
	U_TRAPPER   (K.TEXT,  "적이 함정을 밟으면 4턴간 속박되고 추가 피해"),
	U_RICOCHET  (K.TEXT,  "투척 무기가 근처의 다른 적에게 튕겨 피해의 60%를 줌"),
	U_SNIPE     (K.TEXT,  "멀리 있는 적일수록 투척 피해 증가 (칸당 +8%, 최대 +60%)"),
	U_HAWK      (K.TEXT,  "새 층에 처음 도착하면 영혼 매 아군 소환"),
	//greed
	U_INTEREST  (K.TEXT,  "새 층에 처음 도착하면 보유 골드의 5%를 이자로 받음 (최대 층 × 25)"),
	U_GRASSCOIN (K.TEXT,  "높은 풀을 헤칠 때 10% 확률로 동전 발견"),
	U_GOLDSHOT  (K.TEXT,  "투척 공격이 골드 (층 × 2)를 소모해 피해 +50%"),
	U_DIGNITY   (K.TEXT,  "보유 골드 1000당 받는 피해 -3% (최대 -15%)"),
	U_APPRAISE  (K.TEXT,  "새 층의 보물 위치가 드러나고, 상자를 열면 골드가 쏟아짐"),
	//wild
	U_DEVOUR    (K.TEXT,  "적을 처치하면 허기가 채워짐"),
	U_FULLBELLY (K.TEXT,  "배고프지 않을 때 받는 피해 -10%, 자연 회복 +50%"),
	U_HERBALIST (K.TEXT,  "식물을 밟으면 체력 15% 회복"),
	U_HIBERNATE (K.TEXT,  "쉬는 동안 자연 회복 4배"),
	U_SEED      (K.TEXT,  "체력이 30% 이하가 되면 층마다 한 번 생명의 씨앗이 터져 체력 40% 회복, 주변 적 속박"),
	//fury
	U_INTIMIDATE(K.TEXT,  "적을 처치하면 주변 적이 3턴간 공포에 질림"),
	U_CHARGE    (K.TEXT,  "이동 직후의 공격 피해 +25%"),
	U_FRENZY    (K.TEXT,  "10턴 안에 3마리를 처치하면 5턴간 광란 (공격 속도 +50%, 피해 +25%)"),
	U_EXECSWING (K.TEXT,  "적을 처치하면 시간이 1턴 멈춤 (8턴마다)"),
	U_AVATAR    (K.TEXT,  "보스가 보이면 모든 증강 효과 1.5배"),

	// ---------------------------------------------------------------- jackpots: rare, huge moments
	U_GUILLOTINE(K.TEXT,  "공격 시 1% 확률로 적을 즉사시킴 (보스는 최대 체력의 10% 피해)"),
	U_CHAINBLAST(K.TEXT,  "적 처치 시 5% 확률로 대폭발, 휘말려 쓰러진 적도 다시 터질 수 있음"),
	U_JACKPOT   (K.TEXT,  "적 처치 시 1% 확률로 골드가 분수처럼 쏟아짐"),
	U_DICE      (K.TEXT,  "마법 막대 사용 시 5% 확률로 위력 5배"),

	// ---------------------------------------------------------------- growing augments, described by their growth
	GR_BLOOD    (K.TEXT,  "적 5마리를 처치할 때마다 최대 체력 +1 (끝없이 성장)"),
	GR_SLAUGHTER(K.TEXT,  "적 10마리를 처치할 때마다 공격 피해 +1% (끝없이 성장)"),
	GR_SOUL     (K.TEXT,  "적 12마리를 처치할 때마다 치명타 확률 +1% (최대 +30%)"),
	GR_STEEL    (K.TEXT,  "피해를 100 받을 때마다 방어력 +1 (끝없이 성장)"),
	GR_JOURNEY  (K.TEXT,  "새 층에 갈 때마다 최대 체력 +3, 자연 회복 +3%"),
	GR_ABYSS    (K.TEXT,  "레벨이 오를 때마다 마법 막대 피해 +4%"),
	GR_VETERAN  (K.TEXT,  "레벨이 오를 때마다 명중 +2%, 투척 피해 +2% (지금까지 레벨의 절반도 계산)"),
	GR_FIRE     (K.TEXT,  "불타는 적을 처치할 때마다 불타는 적에게 주는 피해 +1%"),
	GR_COMPOUND (K.TEXT,  "골드를 300 주울 때마다 골드 획득량 +2%, 공격 피해 +1%"),
	GR_VENOM    (K.TEXT,  "적이 독 피해를 20번 받을 때마다 독 피해 +1"),
	GR_ICE      (K.TEXT,  "적에게 냉기나 빙결을 2번 걸 때마다 냉기 상태인 적에게 주는 피해 +1%"),
	GR_STORM    (K.TEXT,  "연쇄 번개가 60번 적중할 때마다 거대한 낙뢰가 주변 모든 적을 강타"),
	GR_DOUBLE   (K.TEXT,  "처치가 두 배로 계산되어 성장하고, 처치 100마다 공격 속도 +5% (최대 +30%)"),
	GR_MARCH    (K.TEXT,  "새 층에 갈 때마다 받는 피해 -2%, 최대 체력 +3 (끝없이 성장)"),

	// ---------------------------------------------------------------- new legendary combination effects
	L_STEAM     (K.TEXT,  "증기 폭발의 피해 2배, 범위가 넓어지고 휘말린 적은 앞을 보지 못함"),
	L_SLAYER    (K.TEXT,  "그림자 이동의 대기 시간이 사라지고, 이동 직후 첫 공격은 확정 치명타"),
	L_ELEMENT   (K.TEXT,  "정령이 2배 강해지고, 번개 정령이 함께 소환됨"),
	L_TIME      (K.TEXT,  "시간이 멈춘 동안 주는 피해 2배"),

	// ---------------------------------------------------------------- verbs: augments that change how an attack works
	U_FLURRY    (K.TEXT,  "근접 공격이 4번에 나뉘어 들어감 (피해 총합은 같고, 타격마다 치명타와 발동 효과를 따로 판정)"),
	U_GALE      (K.TEXT,  "근접 공격마다 칼바람이 같은 방향 4칸 안의 적들을 벰 (피해의 50%)"),
	U_FINISHER  (K.TEXT,  "시야 안에 적이 하나뿐이면 그 적에게 주는 피해 3배"),
	U_BRAND     (K.TEXT,  "같은 적을 3번 때리면 낙인이 터짐 (피해의 150%)"),
	U_SOULCHAIN (K.TEXT,  "적을 공격하면 가까운 다른 적과 영혼의 사슬로 묶여, 한쪽이 받는 피해의 50%를 다른 쪽도 받음"),
	U_BOOMERANG (K.TEXT,  "던진 투척 무기가 손으로 돌아옴"),
	U_SPLIT     (K.TEXT,  "투척 무기를 맞히면 분열체가 근처의 다른 적 2명에게 날아감 (피해의 50%)"),
	U_SOULSTEAL (K.TEXT,  "적을 처치하면 그 적의 능력을 이 층이 끝날 때까지 빌려 씀 (마지막으로 처치한 종류)"),
	U_CALM      (K.TEXT,  "한 턴 쉬면 다음 공격 피해 2.5배"),
	U_MELODY    (K.TEXT,  "서로 다른 적 3종을 연달아 처치하면 죽음의 선율이 울려 주변 적에게 큰 피해"),
	U_REFLECT   (K.TEXT,  "마법 막대 탄환이 벽에 맞으면 튕겨 나가 가까운 적을 맞힘 (피해 +30%)"),
	U_RITUAL    (K.TEXT,  "같은 적 곁에서 상하좌우 네 방향을 모두 거치면 의식이 완성되어 그 적이 즉사 (보스는 최대 체력의 20% 피해)"),

	// ---------------------------------------------------------------- the prices and promises of the newer gambles
	SURE_CRIT   (K.TEXT,  "모든 공격이 치명타"),
	NO_WAND     (K.TEXT,  "마법 막대를 사용할 수 없음"),
	TIME_DEBT   (K.TEXT,  "20턴마다 3턴 동안 몸이 굳음"),
	CHAMPIONS   (K.TEXT,  "모든 적이 챔피언으로 나타남 (보스 제외)"),

	// ---------------------------------------------------------------- late bloomers: grow by completing stages
	Q_GIANT     (K.TEXT,  "단계 조건을 완성할 때마다 몸집이 커지며 강해짐 (진행 중에는 변화 없음)"),
	Q_STARS     (K.TEXT,  "단계 조건을 완성할 때마다 주위를 도는 별이 늘어남. 별은 매 턴 곁의 적을 공격"),

	// ---------------------------------------------------------------- newer legends
	L_TWIN      (K.TEXT,  "공격할 때마다 그림자가 같은 적을 한 번 더 공격 (피해 100%)"),
	L_REPLAY    (K.TEXT,  "5턴마다, 지난 5턴 동안의 공격이 유령처럼 다시 재생됨 (피해 70%)"),
	L_DEVOUR    (K.TEXT,  "얻을 때 가진 증강 하나를 삼켜, 그 효과를 2배로 흡수함"),
	L_DASH      (K.TEXT,  "같은 방향으로 3칸 이상 달린 뒤 공격하면 번개가 되어 일직선으로 돌파 (전하만큼 피해, 처치하면 다시 돌파)"),
	L_MOUNTAIN  (K.TEXT,  "제자리에 있으면 근접 공격을 받을 때마다 100% 반격, 3턴마다 충격파로 주변 적을 밀쳐내고 기절, 넉백 면역"),
	L_ROULETTE  (K.TEXT,  "새 층에 처음 도착할 때마다 운명의 룰렛이 돌아 그 층 동안 축복 하나가 걸림, 잭팟 확률 2배"),
	L_ICEDANCER (K.TEXT,  "냉기에 걸린 적 곁을 지나가면 즉시 빙결, 산산조각 난 얼음 파편이 주변 적을 빙결시키며 연쇄, 파쇄마다 체력 회복"),
	L_GARDEN    (K.TEXT,  "독에 걸린 채 죽은 적의 자리에 독꽃이 피어 20턴 동안 주변 적에게 독을 뱉음, 독꽃 곁에서는 체력 회복과 회피 +30%"),

	// ---------------------------------------------------------------- received once, when the augment is taken
	G_GOLD      (K.ONCE,   "즉시 골드 {} 획득"),
	G_UPGRADE   (K.ONCE,   "즉시 강화 주문서 {}개 획득"),
	G_STRENGTH  (K.ONCE,   "즉시 힘의 물약 {}개 획득"),
	G_HEALING   (K.ONCE,   "즉시 치유 물약 {}개 획득"),
	G_EXPPOT    (K.ONCE,   "즉시 경험의 물약 {}개 획득");

	enum K { FLAT, PCT, TEXT, ONCE }

	final K kind;
	final String text;

	St( K kind, String text ){
		this.kind = kind;
		this.text = text;
	}

	//one line of description for a value of this stat
	public String describe( float value ){
		if (kind == K.TEXT) return text;
		int n = kind == K.PCT ? Math.round(value * 100f) : Math.round(value);
		if (n < 0){
			//negative values flip the sign written in the text
			String abs = Integer.toString(-n);
			if (text.contains("+{}")) return text.replace("+{}", "-" + abs);
			if (text.contains("-{}")) return text.replace("-{}", "+" + abs);
			return text.replace("{}", "-" + abs);
		}
		return text.replace("{}", Integer.toString(n));
	}

	//whether a value of this kind is a one-off reward rather than a lasting effect
	public boolean once(){
		return kind == K.ONCE;
	}

	//effects whose strength doesn't add up (immunities etc.)
	public boolean flag(){
		return kind == K.TEXT;
	}

	//an on/off effect held twice: every extra copy turns into this instead of going to waste
	public St dupStat(){
		switch (this){
			case IMM_FIRE:   return BURNING_DMG;
			case IMM_COLD:   return CHILLED_DMG;
			case IMM_POISON: return POISONED_DMG;
			case IMM_ELEC:   return P_LIGHTNING;
			case IMM_PARA:   return TAKEN;
			case LAST_STAND: return LOW_TAKEN;
			case REVIVE:     return HP;
			case TRAP_SENSE: return VISION;
			default:         return null;
		}
	}

	public float dupValue(){
		switch (this){
			case IMM_FIRE: case IMM_COLD: case IMM_POISON: return 0.10f;
			case IMM_ELEC:   return 0.04f;
			case IMM_PARA:   return 0.03f;
			case LAST_STAND: return 0.08f;
			case REVIVE:     return 15f;
			case TRAP_SENSE: return 1f;
			default:         return 0f;
		}
	}
}
