/*
 * Augment challenge state, kept on the hero as a hidden buff so it is saved with the game.
 * Also drives everything that happens on its own each turn: auras, the arcane orb,
 * floor arrival rewards and the like.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;

public class AugTracker extends Buff {

	{
		revivePersists = true;
		type = buffType.NEUTRAL;
	}

	public final ArrayList<String> owned = new ArrayList<>();
	public int pending = 0;
	public int picks = 0;

	//the cards currently on offer, kept so reopening the game shows the same choice
	public String[] offer = null;
	public int offerTier = 0;
	public boolean[] rerolled = new boolean[4];
	public int[] cardTier = new int[4];
	//0: normal, 1: a fourth card, 2: one card raised a tier
	public int lucky = 0;
	//prism choices earned by defeating bosses
	public int forcePrism = 0;
	public boolean awakenedAnnounced[] = new boolean[AugSet.values().length];
	public final ArrayList<String> shownThisPick = new ArrayList<>();

	public boolean reviveUsed = false;
	public int lastStandFloor = Integer.MIN_VALUE;
	public int blinkFloor = Integer.MIN_VALUE;
	private int lastFloor = Integer.MIN_VALUE;
	private final HashSet<Integer> visitedFloors = new HashSet<>();

	public int rage = 0;
	public float lastHit = -100f;
	private float orbTimer = 0f;

	// ---------------------------------------------------------------- growing augments and shards
	public final int[] cnt = new int[AugGrow.COUNTERS];
	final java.util.LinkedHashMap<String, Integer> growBase = new java.util.LinkedHashMap<>();
	final java.util.LinkedHashMap<String, Integer> growStep = new java.util.LinkedHashMap<>();
	//late game: kills fill up shards, a full set of shards is one more augment
	public int shards = 0;
	public int shardPicks = 0;

	// ---------------------------------------------------------------- unique effect state
	public int pactFloor = Integer.MIN_VALUE;
	public int seedFloor = Integer.MIN_VALUE;
	public int staticCharge = 0;
	//transient: timings and streaks that don't matter across a save
	float thrillUntil = -1, frenzyUntil = -1, stepUntil = -1;
	float fireDashAt = -100, doorHasteAt = -100, shadowStepAt = -100, execSwingAt = -100;
	final float[] killTimes = {-100, -100, -100};
	int moveDir = 0, moveStreak = 0, gustReady = 0;
	float lastMoveAt = -100;
	int stillPos = -1;
	int attacks = 0;
	int pendingStop = 0;
	int markId = -1, markStacks = 0;
	float diceAt = -1; float diceMult = 1f;
	int surpriseId = -1;
	boolean avatar = false;

	// ---------------------------------------------------------------- state of the newer augments
	//transient
	int finisherId = -1;
	boolean calm = false;
	int brandId = -1, brandHits = 0;
	int chainA = -1, chainB = -1;
	int ritualId = -1, ritualMask = 0;
	int stillTurns = 0;
	int pendingFreeze = 0;
	final ArrayList<float[]> replay = new ArrayList<>();
	int replayTimer = 0;
	final ArrayList<AugY.Flower> flowers = new ArrayList<>();
	final ArrayList<String> melody = new ArrayList<>();
	//saved
	public String soulKey = null, soulName = null;
	public int soulFloor = Integer.MIN_VALUE;
	public int blessing = -1, blessingFloor = Integer.MIN_VALUE;
	public String devoured = null;
	public int devouredStars = 1;
	public int debtTimer = 0;
	final java.util.LinkedHashMap<String, Integer> questStage = new java.util.LinkedHashMap<>();
	final java.util.LinkedHashMap<String, Integer> questProg = new java.util.LinkedHashMap<>();
	final HashSet<String> questWands = new HashSet<>();
	public int goalsDone = 0;

	// ---------------------------------------------------------------- stars, targets, rounds, records
	//star level of owned augments: taking one again makes it stronger, up to three stars
	public final java.util.LinkedHashMap<String, Integer> stars = new java.util.LinkedHashMap<>();
	public static final int MAX_STARS = 3;
	public static final float[] STAR_POWER = {1f, 1f, 1.7f, 2.6f};
	//the legend the player is aiming for
	public String goal = null;
	//index of the face down mystery card in the current offer, -1 if none
	public int mystery = -1;
	//every fifth level brings a stronger round
	public int premium = 0;
	//shards start each cycle partly filled
	public boolean shardsPrimed = false;

	//this floor and this run, for the floor summary and the end of run highlights
	public int floorDmg, floorProcs, floorKills, floorCrit;
	public int runDmg, runProcs, bestCrit, bestCritFloor, jackpots, bigMoments, stars3;
	public final ArrayList<String> legendLog = new ArrayList<>();
	public boolean highlightsShown = false;

	public int stars( String name ){
		Integer s = stars.get(name);
		return s == null ? (owned.contains(name) ? 1 : 0) : s;
	}

	public float power( Aug a ){
		return STAR_POWER[Math.max(1, Math.min(MAX_STARS, stars(a.name)))];
	}

	public boolean canStar( Aug a ){
		return a != null && !a.repeatable && a.tier != Aug.LEGEND && owned.contains(a.name) && stars(a.name) < MAX_STARS;
	}

	public int floorsVisited(){
		return visitedFloors.size();
	}

	public int shardsNeeded(){
		return 30 + 10 * shardPicks;
	}

	//shards only start gathering once the hero has grown past the early floors
	public boolean shardsActive(){
		return Dungeon.hero != null && Dungeon.hero.lvl >= 10;
	}

	public boolean hasGrowth(){
		return !growBase.isEmpty();
	}

	//sum of every owned augment and active set bonus
	final float[] total = new float[St.values().length];
	final int[] setCount = new int[AugSet.values().length];

	public void startNew(){
		pending = 1;
		recalc();
	}

	public static int floorKey(){
		return Dungeon.depth * 100 + Dungeon.branch;
	}

	public int count( AugSet set ){
		return setCount[set.ordinal()];
	}

	public int timesOwned( Aug a ){
		int n = 0;
		for (String s : owned) if (s.equals(a.name)) n++;
		return n;
	}

	//how many times each on/off effect is held, so extra copies can be converted
	private final int[] flagCount = new int[St.values().length];

	public void recalc(){
		Arrays.fill(total, 0f);
		Arrays.fill(setCount, 0);
		Arrays.fill(flagCount, 0);
		for (String name : owned){
			Aug a = Aug.get(name);
			if (a == null) continue;
			if (a.set != null) setCount[a.set.ordinal()]++;
			addOwned(a);
		}
		for (AugSet set : AugSet.values()){
			int r = AugSet.reached(setCount[set.ordinal()]);
			for (int i = 0; i < r; i++) addStats(set.bonus(i));
		}
		EnumMap<St, Float> grown = new EnumMap<>(St.class);
		AugGrow.addGrowth(this, grown);
		addStats(grown);

		//the devoured augment: its upsides twice over, still counted for its family
		Aug eaten = devoured == null ? null : Aug.get(devoured);
		if (eaten != null){
			if (eaten.set != null) setCount[eaten.set.ordinal()]++;
			float pw = STAR_POWER[Math.max(1, Math.min(MAX_STARS, devouredStars))] * 2f;
			for (Map.Entry<St, Float> e : eaten.stats.entrySet()){
				St s = e.getKey();
				if (s.once()) continue;
				if (s.flag()){ total[s.ordinal()] = 1f; flagCount[s.ordinal()]++; }
				else total[s.ordinal()] += e.getValue() < 0 ? e.getValue() : e.getValue() * pw;
			}
		}
		EnumMap<St, Float> more = new EnumMap<>(St.class);
		AugY.addStats(this, more);
		addStats(more);

		//duplicates of on/off effects become their replacement bonus
		for (St s : St.values()){
			int extra = flagCount[s.ordinal()] - 1;
			if (extra > 0 && s.dupStat() != null){
				total[s.dupStat().ordinal()] += extra * s.dupValue();
			}
		}
	}

	//whether an on/off effect is already held
	public boolean holds( St s ){
		return total[s.ordinal()] > 0;
	}

	//an owned augment: stars multiply its upsides; gamble costs stay for good
	private void addOwned( Aug a ){
		float pw = power(a);
		for (Map.Entry<St, Float> e : a.stats.entrySet()){
			St s = e.getKey();
			if (s.once()) continue;
			int i = s.ordinal();
			float v = e.getValue();
			if (s.flag()){
				total[i] = 1f;
				flagCount[i]++;
			} else if (v < 0){
				total[i] += v;
			} else {
				total[i] += v * pw;
			}
		}
	}

	private void addStats( EnumMap<St, Float> stats ){
		for (Map.Entry<St, Float> e : stats.entrySet()){
			if (e.getKey().once()) continue;
			int i = e.getKey().ordinal();
			if (e.getKey().flag()){
				total[i] = 1f;
				flagCount[i]++;
			}
			else total[i] += e.getValue();
		}
	}

	//effects tied to the current floor can't be worked out while a save is still loading
	private boolean freshlyLoaded = true;

	@Override
	public boolean act() {
		if (target.isAlive() && target == Dungeon.hero && Augs.active()){
			Hero h = (Hero) target;
			if (freshlyLoaded){
				freshlyLoaded = false;
				recalc();
				h.updateHT(false);
			}

			int key = floorKey();
			if (key != lastFloor){
				int was = lastFloor;
				lastFloor = key;
				if (was != Integer.MIN_VALUE) AugMoments.floorSummary(this, was);
				AugY.onFloorChange(this);
				if (visitedFloors.add(key) && picks > 0){
					Augs.onNewFloor(h);
				}
			}

			if (rage > 0 && Actor.now() - lastHit > 2.5f){
				rage = 0;
			}

			Augs.everyTurn(h, this);
			AugX.everyTurn(h, this);

			if (total[St.ORB.ordinal()] > 0){
				orbTimer += TICK;
				if (orbTimer >= 6f && Augs.fireOrb(h, total[St.ORB.ordinal()], this)){
					orbTimer = 0;
					//wait for the bolt to land
					spend(TICK);
					return false;
				}
			}
		}
		spend(TICK);
		return true;
	}

	// ---------------------------------------------------------------- status icon: growth and shards

	@Override
	public int icon() {
		if (!Augs.active() || (!hasGrowth() && !shardsActive() && questStage.isEmpty())) return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.NONE;
		return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.UPGRADE;
	}

	@Override
	public void tintIcon( com.watabou.noosa.Image icon ) {
		icon.hardlight(1f, 0.82f, 0.35f);
	}

	@Override
	public float iconFadePercent() {
		if (!shardsActive()) return 0;
		return 1f - Math.min(1f, shards / (float) shardsNeeded());
	}

	@Override
	public String iconTextDisplay() {
		return shardsActive() ? Integer.toString(shards) : "";
	}

	@Override
	public String name() {
		return "증강의 성장";
	}

	@Override
	public String desc() {
		StringBuilder sb = new StringBuilder();
		if (shardsActive()){
			sb.append("증강 파편: ").append(shards).append(" / ").append(shardsNeeded())
					.append("\n적을 처치하면 파편이 모이고, 가득 차면 증강을 하나 더 고릅니다.");
		} else {
			sb.append("레벨 10부터 적을 처치하면 증강 파편이 모입니다.");
		}
		if (hasGrowth()){
			sb.append("\n\n성장하는 증강\n").append(AugGrow.status(this));
		}
		String q = AugQuest.status(this);
		if (q.length() > 0){
			sb.append("\n\n단계를 완성해 강해지는 증강\n").append(q);
		}
		return sb.toString();
	}

	private static void putMap( Bundle b, String kn, String kv, Map<String, Integer> m ){
		String[] n = m.keySet().toArray(new String[0]);
		int[] v = new int[n.length];
		for (int i = 0; i < n.length; i++) v[i] = m.get(n[i]);
		b.put(kn, n);
		b.put(kv, v);
	}

	private static void getMap( Bundle b, String kn, String kv, Map<String, Integer> m ){
		m.clear();
		if (!b.contains(kn)) return;
		String[] n = b.getStringArray(kn);
		int[] v = b.getIntArray(kv);
		for (int i = 0; i < n.length && i < v.length; i++) m.put(n[i], v[i]);
	}

	// ---------------------------------------------------------------- saving

	private static final String OWNED       = "owned";
	private static final String PENDING     = "pending";
	private static final String PICKS       = "picks";
	private static final String OFFER       = "offer";
	private static final String OFFER_TIER  = "offer_tier";
	private static final String REROLLED    = "rerolled";
	private static final String SHOWN       = "shown";
	private static final String REVIVED     = "revive_used";
	private static final String LAST_STAND  = "last_stand";
	private static final String BLINK       = "blink";
	private static final String LAST_FLOOR  = "last_floor";
	private static final String VISITED     = "visited";
	private static final String ORB_TIMER   = "orb_timer";
	private static final String CARD_TIER   = "card_tier";
	private static final String LUCKY       = "lucky";
	private static final String FORCE_PRISM = "force_prism";
	private static final String COUNTS      = "counts";
	private static final String STAR_N      = "star_names";
	private static final String STAR_V      = "star_values";
	private static final String GOAL        = "goal";
	private static final String MYSTERY     = "mystery";
	private static final String PREMIUM     = "premium";
	private static final String PRIMED      = "shards_primed";
	private static final String RECORDS     = "records";
	private static final String LEGEND_LOG  = "legend_log";
	private static final String HL_SHOWN    = "highlights_shown";
	private static final String GROW_NAMES  = "grow_names";
	private static final String GROW_BASE   = "grow_base";
	private static final String GROW_STEP   = "grow_step";
	private static final String SHARDS      = "shards";
	private static final String SHARD_PICKS = "shard_picks";
	private static final String PACT        = "pact_floor";
	private static final String SEED        = "seed_floor";
	private static final String STATIC      = "static_charge";
	private static final String AUG_VERSION = "aug_version";
	private static final String SOUL        = "soul";
	private static final String SOUL_NAME   = "soul_name";
	private static final String SOUL_FLOOR  = "soul_floor";
	private static final String BLESSING    = "blessing";
	private static final String BLESS_FLOOR = "blessing_floor";
	private static final String DEVOURED    = "devoured";
	private static final String DEVOURED_ST = "devoured_stars";
	private static final String DEBT        = "debt_timer";
	private static final String Q_STAGE_N   = "quest_stage_names";
	private static final String Q_STAGE_V   = "quest_stage_values";
	private static final String Q_PROG_N    = "quest_prog_names";
	private static final String Q_PROG_V    = "quest_prog_values";
	private static final String Q_WANDS     = "quest_wands";
	private static final String GOALS_DONE  = "goals_done";
	//2: the 2026-11 augment rework, which renamed and removed some augments
	private static final int VERSION = 2;

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(OWNED, owned.toArray(new String[0]));
		bundle.put(PENDING, pending);
		bundle.put(PICKS, picks);
		if (offer != null) bundle.put(OFFER, offer);
		bundle.put(OFFER_TIER, offerTier);
		bundle.put(REROLLED, rerolled);
		bundle.put(SHOWN, shownThisPick.toArray(new String[0]));
		bundle.put(REVIVED, reviveUsed);
		bundle.put(LAST_STAND, lastStandFloor);
		bundle.put(BLINK, blinkFloor);
		bundle.put(LAST_FLOOR, lastFloor);
		int[] v = new int[visitedFloors.size()];
		int i = 0;
		for (int f : visitedFloors) v[i++] = f;
		bundle.put(VISITED, v);
		bundle.put(ORB_TIMER, orbTimer);
		bundle.put(CARD_TIER, cardTier);
		bundle.put(LUCKY, lucky);
		bundle.put(FORCE_PRISM, forcePrism);
		bundle.put(COUNTS, cnt);
		putMap(bundle, STAR_N, STAR_V, stars);
		if (goal != null) bundle.put(GOAL, goal);
		bundle.put(MYSTERY, mystery);
		bundle.put(PREMIUM, premium);
		bundle.put(PRIMED, shardsPrimed);
		bundle.put(RECORDS, new int[]{floorDmg, floorProcs, floorKills, floorCrit, runDmg, runProcs, bestCrit, bestCritFloor, jackpots, bigMoments, stars3});
		bundle.put(LEGEND_LOG, legendLog.toArray(new String[0]));
		bundle.put(HL_SHOWN, highlightsShown);
		String[] gn = growBase.keySet().toArray(new String[0]);
		int[] gb = new int[gn.length], gs = new int[gn.length];
		for (int k = 0; k < gn.length; k++){
			gb[k] = growBase.get(gn[k]);
			Integer st = growStep.get(gn[k]);
			gs[k] = st == null ? 0 : st;
		}
		bundle.put(GROW_NAMES, gn);
		bundle.put(GROW_BASE, gb);
		bundle.put(GROW_STEP, gs);
		bundle.put(SHARDS, shards);
		bundle.put(SHARD_PICKS, shardPicks);
		bundle.put(PACT, pactFloor);
		bundle.put(SEED, seedFloor);
		bundle.put(STATIC, staticCharge);
		bundle.put(AUG_VERSION, VERSION);
		if (soulKey != null){ bundle.put(SOUL, soulKey); bundle.put(SOUL_NAME, soulName == null ? "" : soulName); }
		bundle.put(SOUL_FLOOR, soulFloor);
		bundle.put(BLESSING, blessing);
		bundle.put(BLESS_FLOOR, blessingFloor);
		if (devoured != null) bundle.put(DEVOURED, devoured);
		bundle.put(DEVOURED_ST, devouredStars);
		bundle.put(DEBT, debtTimer);
		putMap(bundle, Q_STAGE_N, Q_STAGE_V, questStage);
		putMap(bundle, Q_PROG_N, Q_PROG_V, questProg);
		bundle.put(Q_WANDS, questWands.toArray(new String[0]));
		bundle.put(GOALS_DONE, goalsDone);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		owned.clear();
		if (bundle.contains(OWNED)) owned.addAll(Arrays.asList(bundle.getStringArray(OWNED)));
		pending = bundle.getInt(PENDING);
		picks = bundle.getInt(PICKS);
		offer = bundle.contains(OFFER) ? bundle.getStringArray(OFFER) : null;
		offerTier = bundle.getInt(OFFER_TIER);
		if (bundle.contains(REROLLED)) rerolled = bundle.getBooleanArray(REROLLED);
		if (rerolled == null || rerolled.length != 4){
			boolean[] r = new boolean[4];
			if (rerolled != null) System.arraycopy(rerolled, 0, r, 0, Math.min(4, rerolled.length));
			rerolled = r;
		}
		cardTier = bundle.contains(CARD_TIER) ? bundle.getIntArray(CARD_TIER) : null;
		if (cardTier == null || cardTier.length != 4){
			cardTier = new int[4];
			java.util.Arrays.fill(cardTier, offerTier);
		}
		lucky = bundle.getInt(LUCKY);
		forcePrism = bundle.getInt(FORCE_PRISM);
		shownThisPick.clear();
		if (bundle.contains(SHOWN)) shownThisPick.addAll(Arrays.asList(bundle.getStringArray(SHOWN)));
		reviveUsed = bundle.getBoolean(REVIVED);
		lastStandFloor = bundle.contains(LAST_STAND) ? bundle.getInt(LAST_STAND) : Integer.MIN_VALUE;
		blinkFloor = bundle.contains(BLINK) ? bundle.getInt(BLINK) : Integer.MIN_VALUE;
		lastFloor = bundle.contains(LAST_FLOOR) ? bundle.getInt(LAST_FLOOR) : Integer.MIN_VALUE;
		visitedFloors.clear();
		if (bundle.contains(VISITED)) for (int f : bundle.getIntArray(VISITED)) visitedFloors.add(f);
		orbTimer = bundle.getFloat(ORB_TIMER);
		getMap(bundle, STAR_N, STAR_V, stars);
		goal = bundle.contains(GOAL) ? bundle.getString(GOAL) : null;
		mystery = bundle.contains(MYSTERY) ? bundle.getInt(MYSTERY) : -1;
		premium = bundle.getInt(PREMIUM);
		shardsPrimed = bundle.getBoolean(PRIMED);
		if (bundle.contains(RECORDS)){
			int[] r = bundle.getIntArray(RECORDS);
			int[] dst = new int[11];
			System.arraycopy(r, 0, dst, 0, Math.min(r.length, dst.length));
			floorDmg = dst[0]; floorProcs = dst[1]; floorKills = dst[2]; floorCrit = dst[3];
			runDmg = dst[4]; runProcs = dst[5]; bestCrit = dst[6]; bestCritFloor = dst[7];
			jackpots = dst[8]; bigMoments = dst[9]; stars3 = dst[10];
		}
		legendLog.clear();
		if (bundle.contains(LEGEND_LOG)) legendLog.addAll(Arrays.asList(bundle.getStringArray(LEGEND_LOG)));
		highlightsShown = bundle.getBoolean(HL_SHOWN);
		if (bundle.contains(COUNTS)){
			int[] c = bundle.getIntArray(COUNTS);
			System.arraycopy(c, 0, cnt, 0, Math.min(c.length, cnt.length));
		}
		growBase.clear();
		growStep.clear();
		if (bundle.contains(GROW_NAMES)){
			String[] gn = bundle.getStringArray(GROW_NAMES);
			int[] gb = bundle.getIntArray(GROW_BASE);
			int[] gs = bundle.getIntArray(GROW_STEP);
			for (int k = 0; k < gn.length; k++){
				growBase.put(gn[k], gb[k]);
				growStep.put(gn[k], gs[k]);
			}
		}
		shards = bundle.getInt(SHARDS);
		shardPicks = bundle.getInt(SHARD_PICKS);
		pactFloor = bundle.contains(PACT) ? bundle.getInt(PACT) : Integer.MIN_VALUE;
		seedFloor = bundle.contains(SEED) ? bundle.getInt(SEED) : Integer.MIN_VALUE;
		staticCharge = bundle.getInt(STATIC);
		soulKey = bundle.contains(SOUL) ? bundle.getString(SOUL) : null;
		soulName = bundle.contains(SOUL_NAME) ? bundle.getString(SOUL_NAME) : null;
		soulFloor = bundle.contains(SOUL_FLOOR) ? bundle.getInt(SOUL_FLOOR) : Integer.MIN_VALUE;
		blessing = bundle.contains(BLESSING) ? bundle.getInt(BLESSING) : -1;
		blessingFloor = bundle.contains(BLESS_FLOOR) ? bundle.getInt(BLESS_FLOOR) : Integer.MIN_VALUE;
		devoured = bundle.contains(DEVOURED) ? bundle.getString(DEVOURED) : null;
		devouredStars = bundle.contains(DEVOURED_ST) ? bundle.getInt(DEVOURED_ST) : 1;
		debtTimer = bundle.getInt(DEBT);
		getMap(bundle, Q_STAGE_N, Q_STAGE_V, questStage);
		getMap(bundle, Q_PROG_N, Q_PROG_V, questProg);
		questWands.clear();
		if (bundle.contains(Q_WANDS)) questWands.addAll(Arrays.asList(bundle.getStringArray(Q_WANDS)));
		goalsDone = bundle.getInt(GOALS_DONE);
		upgradeSave(bundle.contains(AUG_VERSION) ? bundle.getInt(AUG_VERSION) : 1);
		recalc();
	}

	//older saves may hold augments that were renamed or taken out
	private void upgradeSave( int version ){
		if (version < 2){
			rename("그림자 분신", "환영 도주");
			rename("반격 자세", "방어 자세");
		}
		owned.removeIf(n -> Aug.get(n) == null);
		stars.keySet().removeIf(n -> Aug.get(n) == null);
		growBase.keySet().removeIf(n -> Aug.get(n) == null);
		growStep.keySet().removeIf(n -> Aug.get(n) == null);
		shownThisPick.removeIf(n -> Aug.get(n) == null);
		if (goal != null && Aug.get(goal) == null) goal = null;
		if (devoured != null && Aug.get(devoured) == null) devoured = null;
		if (offer != null){
			for (String n : offer){
				if (n == null || Aug.get(n) == null){
					offer = null;
					mystery = -1;
					break;
				}
			}
		}
	}

	private void rename( String from, String to ){
		for (int i = 0; i < owned.size(); i++) if (owned.get(i).equals(from)) owned.set(i, to);
		if (stars.containsKey(from)) stars.put(to, stars.remove(from));
		if (offer != null) for (int i = 0; i < offer.length; i++) if (from.equals(offer[i])) offer[i] = to;
		if (from.equals(goal)) goal = to;
	}
}
