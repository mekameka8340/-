/*
 * The cards of the Augment II challenge and how a hand of three is dealt.
 *
 * A card is a skill, a passive, or an awakening. Its id is what gets saved:
 * "s:<skill>" for skills, "p:<passive>" for passives.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments2;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class Card2 {

	public static final int SILVER = 0, GOLD = 1, PRISM = 2;
	public static final int[] TIER_COLORS = {0xC8D0DC, 0xFFD45A, 0xE6D8FF};

	public final String id;
	public final Skill skill;

	private Card2( String id, Skill skill ){
		this.id = id;
		this.skill = skill;
	}

	public static Card2 get( String id ){
		if (id == null) return null;
		if (id.startsWith("s:")){
			Skill s = Skill.get(id.substring(2));
			return s == null ? null : new Card2(id, s);
		}
		return null;
	}

	public String name(){
		return skill.name;
	}

	public int tier(){
		return skill.ult ? GOLD : SILVER;
	}

	public int icon(){
		return skill.icon;
	}

	//the small line under the name
	public String kind( Tracker2 t ){
		String s = skill.ult ? "궁극기" : "기본 스킬";
		s += " · 재사용 " + skill.cd + "턴";
		if (skill.free) s += " · 즉시";
		return s;
	}

	public String desc( Tracker2 t ){
		return skill.desc(t, 1f);
	}

	//true when taking this card needs one of the current skills to go
	public boolean replaces( Tracker2 t ){
		if (skill == null) return false;
		return skill.ult ? t.slot[Tracker2.ULT] != null : t.freeBasic() < 0;
	}

	// ================================================================ dealing a hand

	private static Random rng( Tracker2 t ){
		return new Random(Dungeon.seed * 1000003L + t.picks * 7919L + t.rerolls * 104729L + 17L);
	}

	private static ArrayList<String> skillPool( Tracker2 t, boolean ults ){
		ArrayList<String> pool = new ArrayList<>();
		for (Skill s : Skill.all(ults)) if (t.slotOf(s.id) < 0) pool.add("s:" + s.id);
		return pool;
	}

	static void makeOffer( Tracker2 t ){
		Random r = rng(t);
		int level = t.rounds.isEmpty() ? 1 : t.rounds.get(0);
		ArrayList<String> hand = new ArrayList<>();

		ArrayList<String> basics = skillPool(t, false);
		ArrayList<String> ults = skillPool(t, true);
		Collections.shuffle(basics, r);
		Collections.shuffle(ults, r);

		if (level == 8 && ults.size() >= 3){
			hand.addAll(ults.subList(0, 3));
		} else {
			for (String s : basics){
				if (hand.size() < 3) hand.add(s);
			}
		}
		t.offer = hand.toArray(new String[0]);
	}

	//deals the whole hand again
	static boolean reroll( Tracker2 t ){
		if (t.rerolls <= 0) return false;
		t.rerolls--;
		makeOffer(t);
		return true;
	}

	//into is the slot to overwrite, only looked at when the card replaces a skill
	static void choose( Tracker2 t, Card2 c, int into ){
		Hero h = Dungeon.hero;
		if (c.skill != null){
			int slot = c.skill.ult ? Tracker2.ULT : t.freeBasic();
			if (slot < 0) slot = into;
			String old = t.slot[slot];
			t.put(slot, c.skill.id);
			if (old != null && Skill.get(old) != null){
				GLog.p("%s 대신 %s을(를) 익혔습니다.", Skill.get(old).name, c.skill.name);
			} else {
				GLog.p("%s을(를) 익혔습니다.", c.skill.name);
			}
		}
		if (!t.rounds.isEmpty()) t.rounds.remove(0);
		t.offer = null;
		t.picks++;
		Sample.INSTANCE.play(Assets.Sounds.LEVELUP, 0.7f, 1.2f);
		if (h.sprite != null) Fx2.learned(h.sprite, TIER_COLORS[c.tier()]);
	}

	static void upgrade( Tracker2 t, int slot ){
		if (t.slot[slot] == null || t.step[slot] >= Tracker2.MAX_STEP) return;
		t.step[slot]++;
		t.upgrades--;
		GLog.p("%s을(를) %d단계로 강화했습니다.", t.skill(slot).name, t.step[slot]);
		Sample.INSTANCE.play(Assets.Sounds.EVOKE, 0.8f, 1.1f);
		if (Dungeon.hero.sprite != null) Fx2.learned(Dungeon.hero.sprite, TIER_COLORS[GOLD]);
	}
}
