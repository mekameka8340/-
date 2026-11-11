/*
 * Growing augments: their strength depends on a counter that keeps going up through the run
 * (kills, floors, levels, damage taken...). Only what happened after the augment was taken counts.
 * Every few steps of growth a milestone is celebrated.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;

import java.util.EnumMap;

public class AugGrow {

	//counters kept by the tracker
	public static final int KILLS = 0, FLOORS = 1, LEVELS = 2, TAKEN = 3, GOLD = 4,
			BURN_KILLS = 5, COLD = 6, POISON = 7, LIGHTNING = 8;
	public static final int COUNTERS = 9;

	static class Rule {
		final String aug;
		final int counter;
		final float per;         //counter needed for one step
		final St[] stats;
		final float[] each;      //added per step
		final int maxSteps;      //0 = endless
		final int milestone;     //steps between celebrations
		final String unit;       //what one step gives, for the status window

		Rule( String aug, int counter, float per, int maxSteps, int milestone, String unit, Object... statPairs ){
			this.aug = aug;
			this.counter = counter;
			this.per = per;
			this.maxSteps = maxSteps;
			this.milestone = milestone;
			this.unit = unit;
			stats = new St[statPairs.length / 2];
			each = new float[statPairs.length / 2];
			for (int i = 0; i < stats.length; i++){
				stats[i] = (St) statPairs[i * 2];
				each[i] = ((Number) statPairs[i * 2 + 1]).floatValue();
			}
		}
	}

	static final Rule[] RULES = {
			new Rule("피의 수집가",   KILLS,      5,   0, 10, "최대 체력 +1",          St.HP, 1),
			new Rule("학살의 기억",   KILLS,      10,  0, 10, "공격 피해 +1%",          St.DMG_PCT, 0.01f),
			new Rule("영혼 수확",     KILLS,      12,  30, 5, "치명타 +1%",             St.CRIT, 0.01f),
			new Rule("단련된 강철",   TAKEN,      100, 0, 5,  "방어력 +1",              St.ARMOR, 1),
			new Rule("끈질긴 여정",   FLOORS,     1,   0, 5,  "최대 체력 +3, 회복 +3%", St.HP, 3, St.REGEN, 0.03f),
			new Rule("심연의 지식",   LEVELS,     1,   0, 5,  "막대 피해 +4%",          St.WAND_PCT, 0.04f),
			new Rule("노련한 사냥꾼", LEVELS,     1,   0, 5,  "명중·투척 +2%",          St.ACC_PCT, 0.02f, St.THROWN_PCT, 0.02f),
			new Rule("꺼지지 않는 불",BURN_KILLS, 1,   0, 25, "불타는 적 피해 +1%",     St.BURNING_DMG, 0.01f),
			new Rule("복리",          GOLD,       300, 0, 10, "골드 +2%, 피해 +1%",     St.GOLD, 0.02f, St.DMG_PCT, 0.01f),
			new Rule("맹독 연구",     POISON,     20,  0, 10, "독 피해 +1",             St.POISON_PLUS, 1),
			new Rule("빙하기",        COLD,       2,   0, 20, "냉기 적 피해 +1%",       St.CHILLED_DMG, 0.01f),
			new Rule("폭풍의 눈",     LIGHTNING,  60,  0, 1,  "거대 낙뢰",              new Object[0]),
			new Rule("끝없는 학살",   KILLS,      100, 6, 1,  "공격 속도 +5%",          St.ASPD, 0.05f),
			new Rule("불멸의 행군",   FLOORS,     1,   0, 5,  "받는 피해 -2%, 체력 +3", St.TAKEN, 0.02f, St.HP, 3),
	};

	static Rule rule( String aug ){
		for (Rule r : RULES) if (r.aug.equals(aug)) return r;
		return null;
	}

	public static boolean grows( Aug a ){
		return a != null && rule(a.name) != null;
	}

	//current value of a counter
	static int counter( AugTracker t, int c ){
		switch (c){
			case FLOORS: return t.floorsVisited();
			case LEVELS: return Dungeon.hero == null ? 0 : Dungeon.hero.lvl;
			default:     return t.cnt[c];
		}
	}

	//how many steps an owned growing augment has taken so far
	static int steps( AugTracker t, Rule r ){
		Integer base = t.growBase.get(r.aug);
		if (base == null) return 0;
		int done = Math.max(0, counter(t, r.counter) - base);
		int s = (int) (done / r.per);
		if (r.maxSteps > 0) s = Math.min(r.maxSteps, s);
		return s;
	}

	//progress towards the next step, 0..1
	static float progress( AugTracker t, Rule r ){
		Integer base = t.growBase.get(r.aug);
		if (base == null) return 0;
		int done = Math.max(0, counter(t, r.counter) - base);
		if (r.maxSteps > 0 && done / r.per >= r.maxSteps) return 1f;
		return (done % r.per) / r.per;
	}

	//the augment was just taken: remember where its counter started
	static void taken( AugTracker t, Aug a ){
		Rule r = rule(a.name);
		if (r == null || t.growBase.containsKey(a.name)) return;
		int now = counter(t, r.counter);
		//the veteran hunter counts half of the levels gained before it
		if (a.name.equals("노련한 사냥꾼")) now = now - (now - 1) / 2;
		t.growBase.put(a.name, now);
		t.growStep.put(a.name, steps(t, r));
	}

	//adds the growth of every owned growing augment
	static void addGrowth( AugTracker t, EnumMap<St, Float> into ){
		for (Rule r : RULES){
			if (!t.growBase.containsKey(r.aug)) continue;
			int s = steps(t, r);
			//stars make it grow faster
			Aug a = Aug.get(r.aug);
			float pw = a == null ? 1f : t.power(a);
			for (int i = 0; i < r.stats.length; i++){
				Float old = into.get(r.stats[i]);
				into.put(r.stats[i], (old == null ? 0f : old) + s * r.each[i] * pw);
			}
		}
	}

	//a counter moved: refresh totals and celebrate milestones
	static void changed( AugTracker t ){
		boolean any = false;
		for (Rule r : RULES){
			if (!t.growBase.containsKey(r.aug)) continue;
			int s = steps(t, r);
			Integer was = t.growStep.get(r.aug);
			if (was == null) was = 0;
			if (s != was){
				any = true;
				t.growStep.put(r.aug, s);
				if (s > was){
					if (r.counter == LIGHTNING){
						AugX.megaStrike();
					} else if (s / r.milestone > was / r.milestone){
						AugFx.milestone(Aug.get(r.aug), describe(r, s));
					}
				}
			}
		}
		if (any){
			t.recalc();
			if (Dungeon.hero != null) Dungeon.hero.updateHT(false);
		}
	}

	//"+12 최대 체력" style summary of a growth
	static String describe( Rule r, int s ){
		if (r.stats.length == 0) return r.aug;
		AugTracker t = Augs.tracker();
		Aug a = Aug.get(r.aug);
		float pw = (t == null || a == null) ? 1f : t.power(a);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < r.stats.length; i++){
			if (sb.length() > 0) sb.append(", ");
			sb.append(r.stats[i].describe(s * r.each[i] * pw));
		}
		return sb.toString();
	}

	//the growing augment that has come furthest, for the end of run highlights
	static String biggest( AugTracker t ){
		Rule best = null;
		int bs = 0;
		for (Rule r : RULES){
			if (!t.growBase.containsKey(r.aug) || r.stats.length == 0) continue;
			int s = steps(t, r);
			if (s > bs){ bs = s; best = r; }
		}
		if (best == null) return null;
		return "_" + best.aug + "_ (" + describe(best, bs) + ")";
	}

	//lines for the status window
	static String status( AugTracker t ){
		StringBuilder sb = new StringBuilder();
		for (Rule r : RULES){
			if (!t.growBase.containsKey(r.aug)) continue;
			int s = steps(t, r);
			if (sb.length() > 0) sb.append("\n");
			sb.append("- ").append(r.aug).append(": ");
			if (r.counter == LIGHTNING){
				Integer base = t.growBase.get(r.aug);
				int done = Math.max(0, counter(t, r.counter) - base);
				sb.append("낙뢰 ").append(s).append("회, 다음까지 ").append((int) r.per - done % (int) r.per);
			} else {
				sb.append(s == 0 ? "아직 성장 전" : describe(r, s));
				if (r.maxSteps > 0 && s >= r.maxSteps) sb.append(" (최대)");
				else sb.append(" (다음 ").append(Math.round(progress(t, r) * 100)).append("%)");
			}
		}
		return sb.toString();
	}
}
