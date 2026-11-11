/*
 * Augment II challenge state, kept on the hero as a hidden buff so it is saved with the game.
 * Holds the four skill slots and their cooldowns, the passives, and the choices still waiting.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments2;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class Tracker2 extends Buff {

	{
		revivePersists = true;
		type = buffType.NEUTRAL;
	}

	public static final int SLOTS = 4;
	//the last slot only takes an ultimate
	public static final int ULT = 3;
	public static final int MAX_STEP = 3;

	public final String[] slot = new String[SLOTS];
	public final float[] cd = new float[SLOTS];
	public final int[] step = new int[SLOTS];

	public final ArrayList<String> passives = new ArrayList<>();
	public boolean awakened = false;

	//levels whose choice has not been made yet, oldest first
	public final ArrayList<Integer> rounds = new ArrayList<>();
	//skill upgrades earned from bosses and not spent yet
	public int upgrades = 0;
	//the cards on offer, kept so reopening the game shows the same hand
	public String[] offer = null;
	public int rerolls = 2;
	public int picks = 0;

	//mines lying on the current floor
	public final ArrayList<Integer> mines = new ArrayList<>();
	public int mineFloor = Integer.MIN_VALUE;

	//damage soaked up by a raised shield, spent on the next attack
	public int stored = 0;

	//transient
	float lastKillAt = -100f;
	int streak = 0;

	public void startNew(){
		rounds.add(1);
	}

	public static int floorKey(){
		return Dungeon.depth * 100 + Dungeon.branch;
	}

	public Skill skill( int i ){
		return slot[i] == null ? null : Skill.get(slot[i]);
	}

	public int slotOf( String id ){
		for (int i = 0; i < SLOTS; i++) if (id.equals(slot[i])) return i;
		return -1;
	}

	public boolean has( String passive ){
		return passives.contains(passive);
	}

	public int skillCount(){
		int n = 0;
		for (int i = 0; i < SLOTS; i++) if (slot[i] != null) n++;
		return n;
	}

	//first empty basic slot, -1 when all three are taken
	public int freeBasic(){
		for (int i = 0; i < ULT; i++) if (slot[i] == null) return i;
		return -1;
	}

	//extra reach and area from passives
	public int reach(){
		return has("reach") ? 1 : 0;
	}

	public float power( int i ){
		return 1f + 0.25f * step[i];
	}

	public int cooldown( int i ){
		Skill s = skill(i);
		if (s == null) return 0;
		float c = s.cd * (1f - 0.15f * step[i]);
		return Math.max(1, Math.round(c));
	}

	public boolean ready( int i ){
		return slot[i] != null && cd[i] <= 0;
	}

	public void put( int i, String id ){
		slot[i] = id;
		cd[i] = 0;
		step[i] = 0;
	}

	@Override
	public boolean act() {
		for (int i = 0; i < SLOTS; i++){
			if (cd[i] > 0) cd[i] = Math.max(0, cd[i] - 1);
		}
		Aug2.tick(this);
		spend(TICK);
		return true;
	}

	// ---------------------------------------------------------------- saving

	private static final String SLOT = "slot", CD = "cd", STEP = "step", PASSIVES = "passives", AWAKENED = "awakened",
			ROUNDS = "rounds", UPGRADES = "upgrades", OFFER = "offer", REROLLS = "rerolls", PICKS = "picks",
			MINES = "mines", MINE_FLOOR = "mine_floor", STORED = "stored";

	private static int[] ints( ArrayList<Integer> list ){
		int[] a = new int[list.size()];
		for (int i = 0; i < a.length; i++) a[i] = list.get(i);
		return a;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		String[] s = new String[SLOTS];
		for (int i = 0; i < SLOTS; i++) s[i] = slot[i] == null ? "" : slot[i];
		bundle.put(SLOT, s);
		bundle.put(CD, cd);
		bundle.put(STEP, step);
		bundle.put(PASSIVES, passives.toArray(new String[0]));
		bundle.put(AWAKENED, awakened);
		bundle.put(ROUNDS, ints(rounds));
		bundle.put(UPGRADES, upgrades);
		if (offer != null) bundle.put(OFFER, offer);
		bundle.put(REROLLS, rerolls);
		bundle.put(PICKS, picks);
		bundle.put(MINES, ints(mines));
		bundle.put(MINE_FLOOR, mineFloor);
		bundle.put(STORED, stored);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		String[] s = bundle.getStringArray(SLOT);
		float[] c = bundle.getFloatArray(CD);
		int[] st = bundle.getIntArray(STEP);
		for (int i = 0; i < SLOTS; i++){
			slot[i] = (s == null || i >= s.length || s[i].isEmpty() || Skill.get(s[i]) == null) ? null : s[i];
			cd[i] = (c == null || i >= c.length) ? 0 : c[i];
			step[i] = (st == null || i >= st.length) ? 0 : st[i];
		}
		passives.clear();
		String[] p = bundle.getStringArray(PASSIVES);
		if (p != null) for (String id : p) passives.add(id);
		awakened = bundle.getBoolean(AWAKENED);
		rounds.clear();
		int[] r = bundle.getIntArray(ROUNDS);
		if (r != null) for (int v : r) rounds.add(v);
		upgrades = bundle.getInt(UPGRADES);
		offer = bundle.contains(OFFER) ? bundle.getStringArray(OFFER) : null;
		rerolls = bundle.getInt(REROLLS);
		picks = bundle.getInt(PICKS);
		mines.clear();
		int[] m = bundle.getIntArray(MINES);
		if (m != null) for (int v : m) mines.add(v);
		mineFloor = bundle.getInt(MINE_FLOOR);
		stored = bundle.getInt(STORED);
	}
}
