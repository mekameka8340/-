/*
 * Augment II challenge: skills the player uses, and passives that tie them together.
 *
 * Everything the rest of the game calls is here. Each call checks that the challenge is on,
 * so the hooks cost nothing in a normal run.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments2;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Aug2 {

	public static final int[] PICK_LEVELS = {1, 4, 8, 12, 16, 20, 24, 28};

	// ================================================================ state access

	public static boolean active(){
		return Dungeon.hero != null && Dungeon.isChallenged(Challenges.AUGMENTS_2);
	}

	public static Tracker2 tracker(){
		Hero h = Dungeon.hero;
		if (h == null || !Dungeon.isChallenged(Challenges.AUGMENTS_2)) return null;
		return h.buff(Tracker2.class);
	}

	// ================================================================ finding targets

	public static boolean foe( Char c ){
		return c != null && c != Dungeon.hero && c.isAlive() && c.alignment == Char.Alignment.ENEMY;
	}

	public static ArrayList<Char> foesAround( int pos, int radius ){
		ArrayList<Char> list = new ArrayList<>();
		for (Char c : Actor.chars()){
			if (foe(c) && c.pos != pos && Dungeon.level.distance(pos, c.pos) <= radius) list.add(c);
		}
		return list;
	}

	public static ArrayList<Char> nearestFoes( final int pos, int range, int count ){
		ArrayList<Char> list = new ArrayList<>();
		for (Char c : Actor.chars()){
			if (foe(c) && Dungeon.level.heroFOV[c.pos] && Dungeon.level.distance(pos, c.pos) <= range) list.add(c);
		}
		Collections.sort(list, new Comparator<Char>() {
			@Override public int compare( Char a, Char b ) {
				return Float.compare(Dungeon.level.trueDistance(pos, a.pos), Dungeon.level.trueDistance(pos, b.pos));
			}
		});
		while (list.size() > count) list.remove(list.size() - 1);
		return list;
	}

	// ================================================================ using a skill

	//true while a skill is dealing its damage, so hits and kills can be told from basic attacks
	static boolean casting = false;

	//a hit worth the given share of weapon damage; returns what the enemy actually lost
	public static int strike( Hero h, Char enemy, float mult ){
		if (h == null || !h.isAlive() || enemy == null || !enemy.isAlive()) return 0;
		int before = enemy.HP + enemy.shielding();
		boolean was = casting;
		casting = true;
		try {
			h.attack(enemy, mult, 0, Char.INFINITE_ACCURACY);
		} finally {
			casting = was;
		}
		int after = enemy.isAlive() ? enemy.HP + enemy.shielding() : 0;
		return Math.max(0, before - after);
	}

	//the selection now waiting for a target, so a second press of the same button can aim by itself
	private static CellSelector.Listener aiming = null;
	private static int aimingSlot = -1;

	public static int aimingSlot(){
		return aiming != null ? aimingSlot : -1;
	}

	//the button for a slot was pressed
	public static void press( final int slot ){
		final Hero h = Dungeon.hero;
		final Tracker2 t = tracker();
		if (t == null || h == null || !h.isAlive()) return;
		final Skill s = t.skill(slot);
		if (s == null){
			GLog.i(slot == Tracker2.ULT ? "궁극기 칸이 비어 있습니다." : "스킬 칸이 비어 있습니다.");
			return;
		}

		//a second press while aiming: pick the nearest target
		if (aiming != null && aimingSlot == slot){
			ArrayList<Char> near = nearestFoes(h.pos, Math.max(1, s.range(t)), 1);
			boolean wantsFoe = s.aim == Skill.ENEMY || s == Skill.HOOK || s == Skill.PIERCE || s == Skill.DASH;
			//cancelling tells the listener, which clears the aiming state
			GameScene.cancelCellSelector();
			aiming = null;
			if (wantsFoe && !near.isEmpty()) cast(slot, near.get(0).pos);
			return;
		}

		if (!h.ready) return;
		if (t.cd[slot] > 0){
			GLog.w(s.name + ": " + (int)Math.ceil(t.cd[slot]) + "턴 뒤에 다시 쓸 수 있습니다.");
			return;
		}

		if (s.aim == Skill.SELF){
			cast(slot, h.pos);
			return;
		}

		aimingSlot = slot;
		aiming = new CellSelector.Listener() {
			@Override public void onSelect( Integer cell ) {
				if (aiming == this) aiming = null;
				if (cell != null) cast(slot, cell);
			}
			@Override public String prompt() {
				return s.name + ": " + (s.aim == Skill.ENEMY ? "대상을 고르세요" : "위치를 고르세요");
			}
		};
		GameScene.selectCell(aiming);
	}

	static void cast( int slot, int cell ){
		Hero h = Dungeon.hero;
		Tracker2 t = tracker();
		if (t == null || !h.isAlive() || !h.ready || !t.ready(slot)) return;
		Skill s = t.skill(slot);
		Skill.Ctx c = new Skill.Ctx(h, t, slot, cell, t.power(slot));
		s.cast(c);
	}

	static void finishCast( Skill.Ctx c ){
		if (c.echo) return;
		c.t.cd[c.slot] = c.t.cooldown(c.slot);
		if (c.skill.is(Skill.DAMAGE)) Invisibility.dispel();
		c.h.spendAndNext(c.skill.free ? 0f : 1f);
	}

	// ================================================================ hooks from the game

	//a basic attack or a skill hit is about to land
	public static float modifyAttack( Hero h, Char enemy, float dmg ){
		Tracker2 t = tracker();
		if (t == null) return dmg;
		if (t.stored > 0){
			dmg += t.stored;
			if (h.sprite != null) h.sprite.showStatus(0xFFD060, "+" + t.stored);
			t.stored = 0;
		}
		return dmg;
	}

	//damage about to be taken by the hero
	public static int reduceDamage( Hero h, int dmg, Object src ){
		Tracker2 t = tracker();
		if (t == null || dmg <= 0 || src instanceof Hunger) return dmg;
		if (h.buff(Guarding.class) != null){
			int blocked = Math.round(dmg * 0.8f);
			if (blocked > 0){
				t.stored += blocked;
				dmg -= blocked;
				if (h.sprite != null){
					h.sprite.showStatus(0x9FC8FF, "막음 " + blocked);
					Fx2.shieldHit(h.sprite);
				}
				Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 0.8f, 1.1f);
			}
		}
		return dmg;
	}

	//damage about to be taken by anything else
	public static int onDamaged( Char ch, int dmg, Object src ){
		if (dmg <= 0 || ch == Dungeon.hero) return dmg;
		Marked m = ch.buff(Marked.class);
		if (m != null){
			m.detach();
			dmg *= 2;
			if (ch.sprite != null){
				ch.sprite.showStatus(0xFF5A4A, "표식!");
				Fx2.markBreak(ch.sprite);
			}
		}
		return dmg;
	}

	public static int armor( Hero h, int dr ){
		if (tracker() != null && h.buff(Braced.class) != null) dr *= 2;
		return dr;
	}

	public static void onLevelUp( Hero h ){
		Tracker2 t = tracker();
		if (t == null) return;
		for (int lvl : PICK_LEVELS) if (lvl == h.lvl) t.rounds.add(lvl);
	}

	public static boolean pickLevel( int lvl ){
		for (int l : PICK_LEVELS) if (l == lvl) return true;
		return false;
	}

	//the level of the next choice, 0 when there are none left
	public static int nextPickLevel( int lvl ){
		for (int l : PICK_LEVELS) if (l > lvl) return l;
		return 0;
	}

	public static void onKill( Mob m, Object cause ){
		Tracker2 t = tracker();
		if (t == null || m.alignment != Char.Alignment.ENEMY) return;
		if (!casting && !com.shatteredpixel.shatteredpixeldungeon.augments.Augs.heroCaused(cause) && !(cause instanceof Mine)) return;
		float now = Actor.now();
		t.streak = (now - t.lastKillAt <= 3f) ? t.streak + 1 : 1;
		t.lastKillAt = now;
		if (t.streak >= 2) Fx2.streak(t.streak);
	}

	public static void onBossKill(){
		Tracker2 t = tracker();
		if (t == null) return;
		t.upgrades++;
		t.rerolls++;
	}

	//every creature that ends up on a cell passes through here
	public static void onOccupy( Char ch ){
		Tracker2 t = tracker();
		if (t == null || t.mines.isEmpty() || !foe(ch)) return;
		if (t.mineFloor != Tracker2.floorKey() || !t.mines.contains(ch.pos)) return;
		explode(t, ch.pos);
	}

	//the game scene was (re)built: a new floor, or a loaded game
	public static void onScene(){
		Tracker2 t = tracker();
		Fx2.sceneReset();
		if (t == null) return;
		if (t.mineFloor != Tracker2.floorKey()) t.mines.clear();
		Fx2.syncMines(t);
	}

	//each of the hero's turns
	static void tick( Tracker2 t ){
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.buff(Taunted.class) != null && m.isAlive() && Dungeon.hero.isAlive()) m.aggro(Dungeon.hero);
		}
	}

	// ================================================================ mines

	//marks damage that came from a mine going off
	public static class Mine {}
	private static final Mine MINE = new Mine();

	static void placeMine( Tracker2 t, int cell, float power ){
		if (t.mineFloor != Tracker2.floorKey()){
			t.mines.clear();
			t.mineFloor = Tracker2.floorKey();
		}
		t.mines.add(cell);
		while (t.mines.size() > 3) t.mines.remove(0);
		Fx2.syncMines(t);
	}

	static void explode( Tracker2 t, int cell ){
		t.mines.remove((Integer)cell);
		Fx2.syncMines(t);
		Hero h = Dungeon.hero;
		int slot = t.slotOf("mine");
		float power = slot >= 0 ? t.power(slot) : 1f;
		int r = 1 + t.reach();

		if (Dungeon.level.heroFOV[cell]){
			Sample.INSTANCE.play(Assets.Sounds.BLAST);
			CellEmitter.center(cell).burst(BlastParticle.FACTORY, 24);
			for (int i = 0; i < Dungeon.level.length(); i++){
				if (Dungeon.level.distance(cell, i) <= r && !Dungeon.level.solid[i]){
					CellEmitter.get(i).burst(SmokeParticle.FACTORY, 3);
				}
			}
			Fx2.blastLight(cell);
		}
		ArrayList<Char> hit = foesAround(cell, r);
		Char on = Actor.findChar(cell);
		if (foe(on)) hit.add(0, on);
		for (Char e : hit){
			strike(h, e, 1.6f * power);
			if (e.isAlive()) Buff.prolong(e, Roots.class, 2f);
		}
	}

	// ================================================================ choices

	private static boolean picking = false;

	//called every frame from the game scene: opens a choice when one is waiting
	public static void checkPending(){
		Tracker2 t = tracker();
		if (t == null || picking) return;
		if (t.rounds.isEmpty() && t.upgrades <= 0) return;
		Hero h = Dungeon.hero;
		if (!h.isAlive() || !h.ready || GameScene.showingWindow()) return;
		if (!t.rounds.isEmpty()){
			if (t.offer == null) Card2.makeOffer(t);
			picking = true;
			GameScene.show(new WndPick2(t));
		} else {
			//an upgrade with nothing left to upgrade is simply lost
			boolean any = false;
			for (int i = 0; i < Tracker2.SLOTS; i++) if (t.slot[i] != null && t.step[i] < Tracker2.MAX_STEP) any = true;
			if (!any){
				t.upgrades = 0;
				return;
			}
			picking = true;
			GameScene.show(new WndPick2.Upgrade(t));
		}
	}

	static void windowClosed(){
		picking = false;
	}

	// ================================================================ timed states

	//shared by the short states below: named in Korean right here, and shown on the creature
	public static abstract class State extends FlavourBuff {
		{ announced = false; }
		abstract String label();
		abstract String about();
		int tint(){ return 0xFFFFFF; }
		@Override public String name(){ return label(); }
		@Override public String desc(){ return about() + "\n\n남은 턴: " + (int)Math.ceil(visualcooldown()); }
		@Override public void tintIcon( Image icon ){ icon.hardlight(tint()); }
	}

	public static class Marked extends State {
		{ type = buffType.NEGATIVE; }
		@Override public int icon(){ return BuffIndicator.MARK; }
		@Override int tint(){ return 0xFF5A4A; }
		@Override String label(){ return "표식"; }
		@Override String about(){ return "다음에 받는 피해가 2배가 됩니다."; }
	}

	public static class Guarding extends State {
		{ type = buffType.POSITIVE; }
		@Override public int icon(){ return BuffIndicator.ARMOR; }
		@Override int tint(){ return 0x9FC8FF; }
		@Override String label(){ return "방패 올리기"; }
		@Override String about(){ return "받는 피해가 80% 줄어듭니다. 막아낸 피해는 다음 공격에 얹힙니다."; }
	}

	public static class Braced extends State {
		{ type = buffType.POSITIVE; }
		@Override public int icon(){ return BuffIndicator.ARMOR; }
		@Override int tint(){ return 0xFFC060; }
		@Override String label(){ return "도발의 함성"; }
		@Override String about(){ return "방어력이 2배가 됩니다."; }
	}

	public static class Taunted extends State {
		{ type = buffType.NEGATIVE; }
		@Override public int icon(){ return BuffIndicator.RAGE; }
		@Override String label(){ return "도발당함"; }
		@Override String about(){ return "영웅만 노립니다."; }
	}
}
