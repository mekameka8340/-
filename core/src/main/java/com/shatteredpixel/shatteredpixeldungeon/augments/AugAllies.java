/*
 * Allies summoned by augments. They stay near the hero, follow them down the stairs when close,
 * and grow with the depth. At most three of them fight beside the hero at once.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.MirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.Atmosphere;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ElmoParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class AugAllies {

	public static final int MAX = 3;

	//marks a character as summoned by an augment
	public static class Bond extends AllyBuff {
		@Override
		public void fx( boolean on ) {
			if (target.sprite == null) return;
			if (on) target.sprite.add(CharSprite.State.HEARTS);
			else    target.sprite.remove(CharSprite.State.HEARTS);
		}
	}

	public static int count(){
		int n = 0;
		if (Dungeon.level == null) return 0;
		for (Mob m : Dungeon.level.mobs){
			if (m.isAlive() && m.buff(Bond.class) != null) n++;
		}
		return n;
	}

	//region scale like the summoned elementals use: 2,2,3,4,5
	static int region(){
		return Math.max(2, 1 + Dungeon.scalingDepth() / 5);
	}

	//a free cell next to the hero, or -1
	static int freeCell( Hero h ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			int c = h.pos + n;
			if (c < 0 || c >= Dungeon.level.length()) continue;
			if (Dungeon.level.passable[c] && !Dungeon.level.avoid[c] && Actor.findChar(c) == null) cells.add(c);
		}
		return cells.isEmpty() ? -1 : Random.element(cells);
	}

	//called on arriving at a new floor
	static void summonForFloor( Hero h ){
		boolean lord = Augs.has(St.L_ELEMENT);
		//one of each kind: a summon that survived the last floor is not doubled
		if (Augs.has(St.U_FIREELE) && !present(FireSpirit.class)) spawn(h, new FireSpirit().lord(lord));
		if (Augs.has(St.U_FROSTELE) && !present(FrostSpirit.class)) spawn(h, new FrostSpirit().lord(lord));
		if (lord && !present(ShockSpirit.class)) spawn(h, new ShockSpirit().lord(true));
		if (Augs.has(St.U_HAWK) && !present(Hawk.class)) spawn(h, new Hawk());
		if (Augs.has(St.U_MIRROR)){
			int images = 1 + Math.round(AugX.pw(St.U_MIRROR));
			for (int i = 0; i < images; i++){
				int cell = freeCell(h);
				if (cell < 0) break;
				MirrorImage img = new MirrorImage();
				img.duplicate(h);
				GameScene.add(img);
				ScrollOfTeleportation.appear(img, cell);
				CellEmitter.get(cell).burst(ShadowParticle.UP, 8);
			}
			AugFx.notify(St.U_MIRROR);
		}
	}

	private static boolean present( Class<? extends Mob> kind ){
		for (Mob m : Dungeon.level.mobs) if (m.getClass() == kind && m.isAlive() && m.buff(Bond.class) != null) return true;
		return false;
	}

	static boolean spawn( Hero h, Mob m ){
		if (count() >= MAX) return false;
		int cell = freeCell(h);
		if (cell < 0) return false;
		//stars on the summoning augment make the summon tougher
		float pw = m instanceof FireSpirit ? AugX.pw(St.U_FIREELE) : (m instanceof FrostSpirit ? AugX.pw(St.U_FROSTELE)
				: (m instanceof Hawk ? AugX.pw(St.U_HAWK) : 1f));
		if (pw > 1f){
			m.HT = Math.round(m.HT * pw);
			m.HP = m.HT;
		}
		GameScene.add(m);
		Buff.affect(m, Bond.class);
		m.state = m.WANDERING;
		ScrollOfTeleportation.appear(m, cell);
		if (m instanceof FireSpirit){
			CellEmitter.get(cell).burst(ElmoParticle.FACTORY, 14);
			Atmosphere.burstCell(cell, 3f, 0.9f, 0xFF9A40, 0.8f);
			Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.8f);
			AugFx.notify(St.U_FIREELE);
		} else if (m instanceof FrostSpirit){
			CellEmitter.get(cell).burst(SnowParticle.FACTORY, 14);
			Atmosphere.burstCell(cell, 3f, 0.85f, 0xA8DCFF, 0.8f);
			Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.6f, 1.3f);
			AugFx.notify(St.U_FROSTELE);
		} else if (m instanceof ShockSpirit){
			CellEmitter.get(cell).burst(SparkParticle.FACTORY, 14);
			Atmosphere.burstCell(cell, 3f, 0.85f, 0xC8E0FF, 0.8f);
			Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 0.6f);
		} else {
			CellEmitter.get(cell).burst(Speck.factory(Speck.LIGHT), 6);
			Atmosphere.burstCell(cell, 2.5f, 0.8f, 0xE0F0FF, 0.6f);
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.5f, 1.4f);
			AugFx.notify(St.U_HAWK);
		}
		return true;
	}

	// ================================================================ staying with the hero

	//free cells near the hero, closest first, optionally only inside an area
	private static ArrayList<Integer> cellsNearHero( com.shatteredpixel.shatteredpixeldungeon.levels.Level level, com.watabou.utils.Rect area ){
		ArrayList<Integer> out = new ArrayList<>();
		Hero h = Dungeon.hero;
		if (h == null) return out;
		for (int r = 1; r <= 3 && out.isEmpty(); r++){
			for (int c = 0; c < level.length(); c++){
				if (level.distance(c, h.pos) != r) continue;
				if (!level.passable[c] || level.avoid[c] || Actor.findChar(c) != null) continue;
				if (area != null && !area.inside(level.cellToPoint(c))) continue;
				out.add(c);
			}
		}
		return out;
	}

	private static void moveTo( Mob m, int cell ){
		CellEmitter.get(m.pos).burst(Speck.factory(Speck.LIGHT), 4);
		m.pos = cell;
		if (m.sprite != null){
			m.sprite.interruptMotion();
			m.sprite.place(cell);
			m.sprite.visible = Dungeon.level.heroFOV[cell];
		}
		CellEmitter.get(cell).burst(Speck.factory(Speck.LIGHT), 4);
		m.state = m.WANDERING;
	}

	//the floor was sealed for a boss: every summon steps in beside the hero
	public static void gather( com.shatteredpixel.shatteredpixeldungeon.levels.Level level ){
		Hero h = Dungeon.hero;
		if (h == null || !Augs.active()) return;
		for (Mob m : level.mobs.toArray(new Mob[0])){
			if (m.buff(Bond.class) == null || !m.isAlive()) continue;
			if (level.distance(m.pos, h.pos) <= 1) continue;
			ArrayList<Integer> cells = cellsNearHero(level, null);
			if (cells.isEmpty()) continue;
			moveTo(m, cells.get(0));
		}
	}

	//a boss fight is clearing the map: a summon is moved next to the hero instead. false if it can't be
	public static boolean bring( Mob m, com.watabou.utils.Rect safeArea, com.shatteredpixel.shatteredpixeldungeon.levels.Level level ){
		if (m.buff(Bond.class) == null || !m.isAlive() || Dungeon.hero == null) return false;
		com.watabou.utils.Rect area = null;
		if (safeArea != null){
			//rects in the prison level are inclusive of their right and bottom edges for this check
			area = new com.watabou.utils.Rect(safeArea.left, safeArea.top, safeArea.right, safeArea.bottom);
		}
		ArrayList<Integer> cells = cellsNearHero(level, area);
		if (cells.isEmpty()) return false;
		moveTo(m, cells.get(0));
		return true;
	}

	// ================================================================ how summons behave

	//the closest awake enemy it can see, not too far from the hero; keeps its current target while that stays valid
	static Char pickEnemy( Mob self, Char current, boolean[] fov ){
		Hero h = Dungeon.hero;
		if (fov == null) return null;
		if (current != null && current.isAlive() && current.alignment == Char.Alignment.ENEMY
				&& current.pos >= 0 && current.pos < fov.length && fov[current.pos] && current.invisible <= 0
				&& (h == null || Dungeon.level.distance(h.pos, current.pos) <= 7)){
			return current;
		}
		Char best = null;
		int bestD = Integer.MAX_VALUE;
		for (Mob m : Dungeon.level.mobs){
			if (m == self || m.alignment != Char.Alignment.ENEMY || !m.isAlive() || m.invisible > 0) continue;
			if (m.pos < 0 || m.pos >= fov.length || !fov[m.pos]) continue;
			//sleeping and harmless things are left alone, so the summons never start fights the hero did not want
			if (m.state == m.SLEEPING || m.state == m.PASSIVE || m.isInvulnerable(self.getClass())) continue;
			if (h != null && Dungeon.level.distance(h.pos, m.pos) > 7) continue;
			int d = Dungeon.level.distance(self.pos, m.pos);
			if (d < bestD){
				bestD = d;
				best = m;
			}
		}
		return best;
	}

	//too far from the hero to keep fighting
	static boolean strayed( Mob self ){
		Hero h = Dungeon.hero;
		return h != null && h.isAlive() && Dungeon.level.distance(self.pos, h.pos) > 7;
	}

	// ================================================================ elementals

	//keeps an elemental's random walk close to the hero
	static int nearHero( Mob m ){
		Hero h = Dungeon.hero;
		if (h == null) return Dungeon.level.randomDestination(m);
		for (int tries = 0; tries < 10; tries++){
			int c = h.pos + Random.IntRange(-3, 3) + Random.IntRange(-3, 3) * Dungeon.level.width();
			if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Dungeon.level.distance(c, h.pos) <= 3) return c;
		}
		return h.pos;
	}

	public static class FireSpirit extends Elemental.FireElemental {
		{ EXP = 0; lootChance = 0; intelligentAlly = true; WANDERING = new Follow(); HUNTING = new Fight(); }
		boolean lord = false;
		FireSpirit lord( boolean l ){ lord = l; scale(); return this; }
		public FireSpirit(){ super(); scale(); }
		private void scale(){
			setSummonedALly();
			if (lord) HT *= 2;
			HP = HT;
		}
		@Override public int damageRoll(){ return lord ? super.damageRoll() * 2 : super.damageRoll(); }
		class Follow extends Mob.Wandering {
			@Override
			public boolean act( boolean enemyInFOV, boolean justAlerted ){
				//a summoned ally does not need to stumble upon enemies: it sees one, it goes for it
				if (enemyInFOV){
					enemySeen = true;
					state = HUNTING;
					target = enemy.pos;
					return true;
				}
				enemySeen = false;
				//otherwise it stays at the hero's side
				Hero h = Dungeon.hero;
				int oldPos = pos;
				if (h != null && h.isAlive() && Dungeon.level.distance(pos, h.pos) > 1 && getCloser(h.pos)){
					spend(1 / speed());
					return moveSprite(oldPos, pos);
				}
				spend(TICK);
				return true;
			}
		}
		class Fight extends Mob.Hunting {
			@Override
			public boolean act( boolean enemyInFOV, boolean justAlerted ){
				//never chases so far that the hero is left behind
				if (strayed(FireSpirit.this)){
					FireSpirit.this.enemy = null;
					state = WANDERING;
					spend(TICK);
					return true;
				}
				return super.act(enemyInFOV, justAlerted);
			}
		}
		@Override
		protected Char chooseEnemy(){
			return pickEnemy(this, enemy, fieldOfView);
		}
		@Override public void storeInBundle( Bundle b ){ super.storeInBundle(b); b.put("aug_lord", lord); }
		@Override public void restoreFromBundle( Bundle b ){ super.restoreFromBundle(b); lord = b.getBoolean("aug_lord"); if (lord) HT *= 2; }
	}

	public static class FrostSpirit extends Elemental.FrostElemental {
		{ EXP = 0; lootChance = 0; intelligentAlly = true; WANDERING = new Follow(); HUNTING = new Fight(); }
		boolean lord = false;
		FrostSpirit lord( boolean l ){ lord = l; scale(); return this; }
		public FrostSpirit(){ super(); scale(); }
		private void scale(){
			setSummonedALly();
			if (lord) HT *= 2;
			HP = HT;
		}
		@Override public int damageRoll(){ return lord ? super.damageRoll() * 2 : super.damageRoll(); }
		class Follow extends Mob.Wandering {
			@Override
			public boolean act( boolean enemyInFOV, boolean justAlerted ){
				//a summoned ally does not need to stumble upon enemies: it sees one, it goes for it
				if (enemyInFOV){
					enemySeen = true;
					state = HUNTING;
					target = enemy.pos;
					return true;
				}
				enemySeen = false;
				//otherwise it stays at the hero's side
				Hero h = Dungeon.hero;
				int oldPos = pos;
				if (h != null && h.isAlive() && Dungeon.level.distance(pos, h.pos) > 1 && getCloser(h.pos)){
					spend(1 / speed());
					return moveSprite(oldPos, pos);
				}
				spend(TICK);
				return true;
			}
		}
		class Fight extends Mob.Hunting {
			@Override
			public boolean act( boolean enemyInFOV, boolean justAlerted ){
				//never chases so far that the hero is left behind
				if (strayed(FrostSpirit.this)){
					FrostSpirit.this.enemy = null;
					state = WANDERING;
					spend(TICK);
					return true;
				}
				return super.act(enemyInFOV, justAlerted);
			}
		}
		@Override
		protected Char chooseEnemy(){
			return pickEnemy(this, enemy, fieldOfView);
		}
		@Override public void storeInBundle( Bundle b ){ super.storeInBundle(b); b.put("aug_lord", lord); }
		@Override public void restoreFromBundle( Bundle b ){ super.restoreFromBundle(b); lord = b.getBoolean("aug_lord"); if (lord) HT *= 2; }
	}

	public static class ShockSpirit extends Elemental.ShockElemental {
		{ EXP = 0; lootChance = 0; intelligentAlly = true; WANDERING = new Follow(); HUNTING = new Fight(); }
		boolean lord = false;
		ShockSpirit lord( boolean l ){ lord = l; scale(); return this; }
		public ShockSpirit(){ super(); scale(); }
		private void scale(){
			setSummonedALly();
			if (lord) HT *= 2;
			HP = HT;
		}
		@Override public int damageRoll(){ return lord ? super.damageRoll() * 2 : super.damageRoll(); }
		class Follow extends Mob.Wandering {
			@Override
			public boolean act( boolean enemyInFOV, boolean justAlerted ){
				//a summoned ally does not need to stumble upon enemies: it sees one, it goes for it
				if (enemyInFOV){
					enemySeen = true;
					state = HUNTING;
					target = enemy.pos;
					return true;
				}
				enemySeen = false;
				//otherwise it stays at the hero's side
				Hero h = Dungeon.hero;
				int oldPos = pos;
				if (h != null && h.isAlive() && Dungeon.level.distance(pos, h.pos) > 1 && getCloser(h.pos)){
					spend(1 / speed());
					return moveSprite(oldPos, pos);
				}
				spend(TICK);
				return true;
			}
		}
		class Fight extends Mob.Hunting {
			@Override
			public boolean act( boolean enemyInFOV, boolean justAlerted ){
				//never chases so far that the hero is left behind
				if (strayed(ShockSpirit.this)){
					ShockSpirit.this.enemy = null;
					state = WANDERING;
					spend(TICK);
					return true;
				}
				return super.act(enemyInFOV, justAlerted);
			}
		}
		@Override
		protected Char chooseEnemy(){
			return pickEnemy(this, enemy, fieldOfView);
		}
		@Override public void storeInBundle( Bundle b ){ super.storeInBundle(b); b.put("aug_lord", lord); }
		@Override public void restoreFromBundle( Bundle b ){ super.restoreFromBundle(b); lord = b.getBoolean("aug_lord"); if (lord) HT *= 2; }
	}

	// ================================================================ spirit hawk

	public static class Hawk extends Mob {
		{
			spriteClass = com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk.HawkSprite.class;
			flying = true;
			baseSpeed = 2f;
			EXP = 0;
			lootChance = 0;
			intelligentAlly = true;
			viewDistance = 7;
			WANDERING = new Follow();
			HUNTING = new Fight();
		}

		public Hawk(){
			super();
			int d = Math.max(1, Dungeon.scalingDepth());
			HP = HT = 8 + d * 2;
			defenseSkill = 10 + d * 2;
		}

		@Override
		public int attackSkill( Char target ) {
			return 15 + Dungeon.scalingDepth() * 2;
		}

		@Override
		public int damageRoll() {
			int d = Math.max(1, Dungeon.scalingDepth());
			return Random.NormalIntRange(2 + d / 2, 4 + d);
		}

		@Override
		public int attackProc( Char enemy, int damage ) {
			damage = super.attackProc(enemy, damage);
			//goes for the eyes
			if (Random.Int(4) == 0) Buff.prolong(enemy, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness.class, 2f);
			return damage;
		}

		@Override
		public String name() {
			return "영혼 매";
		}

		@Override
		public String description() {
			return "증강의 힘으로 불려 나온 영혼 매입니다. 주인 곁을 맴돌며 적의 눈을 노립니다.";
		}

		class Follow extends Mob.Wandering {
			@Override
			public boolean act( boolean enemyInFOV, boolean justAlerted ){
				//a summoned ally does not need to stumble upon enemies: it sees one, it goes for it
				if (enemyInFOV){
					enemySeen = true;
					state = HUNTING;
					target = enemy.pos;
					return true;
				}
				enemySeen = false;
				//otherwise it stays at the hero's side
				Hero h = Dungeon.hero;
				int oldPos = pos;
				if (h != null && h.isAlive() && Dungeon.level.distance(pos, h.pos) > 1 && getCloser(h.pos)){
					spend(1 / speed());
					return moveSprite(oldPos, pos);
				}
				spend(TICK);
				return true;
			}
		}
		class Fight extends Mob.Hunting {
			@Override
			public boolean act( boolean enemyInFOV, boolean justAlerted ){
				//never chases so far that the hero is left behind
				if (strayed(Hawk.this)){
					Hawk.this.enemy = null;
					state = WANDERING;
					spend(TICK);
					return true;
				}
				return super.act(enemyInFOV, justAlerted);
			}
		}
		@Override
		protected Char chooseEnemy(){
			return pickEnemy(this, enemy, fieldOfView);
		}
	}
}
