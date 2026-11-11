/*
 * Unique augment effects: the ones that change how a fight plays out rather than adding numbers.
 * Augs calls in here from its hooks, and a few game hooks (moving, potions, scrolls, traps,
 * plants, grass, gold, chests) come here directly.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.Ambience;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.Atmosphere;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.PostFX;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.effects.Wound;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ElmoParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PoisonParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public class AugX {

	public static final Augs.Src STEAM   = new Augs.Src("steam");
	public static final Augs.Src STATIC  = new Augs.Src("static");
	public static final Augs.Src COUNTER = new Augs.Src("counter");
	public static final Augs.Src SPELL   = new Augs.Src("spell");
	public static final Augs.Src CHAIN   = new Augs.Src("chain");
	public static final Augs.Src SKY     = new Augs.Src("sky");
	public static final Augs.Src DRAIN   = new Augs.Src("drain");
	public static final Augs.Src BLADE   = new Augs.Src("blade");

	private static boolean has( St s ){
		return Augs.has(s);
	}

	private static AugTracker t(){
		return Augs.tracker();
	}

	//how much stronger a unique effect is from the stars of the augment carrying it
	static float pw( St s ){
		AugTracker t = t();
		if (t == null) return 1f;
		float best = 1f;
		for (String n : t.owned){
			Aug a = Aug.get(n);
			if (a != null && a.stats.containsKey(s)) best = Math.max(best, t.power(a));
		}
		return best;
	}

	private static int depth(){
		return Math.max(1, Dungeon.depth);
	}

	private static boolean seen( int cell ){
		return Dungeon.level != null && cell >= 0 && cell < Dungeon.level.length() && Dungeon.level.heroFOV[cell];
	}

	static boolean chilled( Char ch ){
		return ch.buff(Chill.class) != null || ch.buff(Frost.class) != null;
	}

	static boolean nearWall( int pos ){
		for (int n : PathFinder.NEIGHBOURS4){
			int c = pos + n;
			if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.solid[c] && !Dungeon.level.passable[c]) return true;
		}
		return false;
	}

	private static boolean boss( Char ch ){
		return Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS);
	}

	private static ArrayList<Char> enemiesNear( int cell, int range ){
		ArrayList<Char> out = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && Dungeon.level.distance(cell, m.pos) <= range) out.add(m);
		}
		return out;
	}

	//runs right after the current action, so extra damage never lands before the hit that caused it
	private static void later( final Runnable r ){
		Actor.add(new Actor() {
			{ actPriority = VFX_PRIO; }
			@Override
			protected boolean act() {
				Actor.remove(this);
				r.run();
				return true;
			}
		});
	}

	//a bolt whose hit is dealt when it arrives. Its own actor waits for it, so nothing else moves meanwhile:
	//damage landing in the middle of another character's animation would leave that animation unfinished
	private static void bolt( final Char from, final Char target, final int type, final Runnable onHit ){
		Actor.add(new Actor() {
			{ actPriority = VFX_PRIO; }
			@Override
			protected boolean act() {
				Actor.remove(this);
				if (from.sprite == null || from.sprite.parent == null || target.sprite == null){
					onHit.run();
					return true;
				}
				final Actor self = this;
				MagicMissile.boltFromChar(from.sprite.parent, type, from.sprite, target.pos, new Callback() {
					@Override
					public void call() {
						onHit.run();
						self.next();
					}
				});
				return false;
			}
		});
	}

	// ================================================================ timed states with a visible icon

	public static class Thrill extends FlavourBuff {
		{ type = buffType.POSITIVE; announced = true; }
		@Override public int icon(){ return BuffIndicator.HASTE; }
		@Override public void tintIcon( Image i ){ i.hardlight(1f, 0.35f, 0.3f); }
		@Override public float iconFadePercent(){ return Math.max(0, (3f - visualcooldown()) / 3f); }
		@Override public String name(){ return "사냥의 흥분"; }
		@Override public String desc(){ return "적을 쓰러뜨린 흥분으로 공격 속도가 40% 빨라졌습니다.\n\n남은 시간: " + dispTurns(); }
	}

	public static class Frenzy extends FlavourBuff {
		{ type = buffType.POSITIVE; announced = true; }
		@Override public int icon(){ return BuffIndicator.BERSERK; }
		@Override public void tintIcon( Image i ){ i.hardlight(1f, 0.25f, 0.2f); }
		@Override public float iconFadePercent(){ return Math.max(0, (5f - visualcooldown()) / 5f); }
		@Override public String name(){ return "광란"; }
		@Override public String desc(){ return "연이은 처치로 광란에 빠졌습니다. 공격 속도 +50%, 공격 피해 +25%.\n\n남은 시간: " + dispTurns(); }
		@Override public void fx( boolean on ){
			if (target.sprite == null) return;
			if (on) target.sprite.aura(0xFF3020, 5); else target.sprite.clearAura();
		}
	}

	public static class Avatar extends Buff {
		{ type = buffType.POSITIVE; }
		@Override public int icon(){ return BuffIndicator.FURY; }
		@Override public void tintIcon( Image i ){ i.hardlight(1f, 0.5f, 0.15f); }
		@Override public String name(){ return "전쟁의 화신"; }
		@Override public String desc(){ return "강적을 마주해 모든 증강 효과가 1.5배가 되었습니다."; }
		@Override public boolean act(){ spend(TICK); return true; }
	}

	static float speedBonus( Hero h ){
		float b = 0;
		if (h.buff(Thrill.class) != null) b += 0.4f * pw(St.U_THRILL);
		if (h.buff(Frenzy.class) != null) b += 0.5f * pw(St.U_FRENZY);
		return b;
	}

	// ================================================================ attacking

	//additive damage bonuses of unique effects, called while an attack is being worked out
	static float attackBonus( Hero h, Char enemy, AugTracker t, boolean surprise, boolean thrown ){
		float b = 0;
		if (surprise && has(St.U_WALLSHADOW) && nearWall(h.pos)) b += 0.25f * pw(St.U_WALLSHADOW);
		if (has(St.U_BETRAY) && betrayable(enemy)){
			b += 0.5f * pw(St.U_BETRAY);
			AugFx.notify(St.U_BETRAY);
		}
		if (has(St.U_MARK) && enemy.id() == t.markId) b += 0.05f * t.markStacks * pw(St.U_MARK);
		if (thrown && has(St.U_SNIPE)){
			b += Math.min(0.6f, 0.08f * Dungeon.level.distance(h.pos, enemy.pos)) * pw(St.U_SNIPE);
		}
		if (thrown && has(St.U_GOLDSHOT)){
			int cost = 2 * depth();
			if (Dungeon.gold >= cost){
				Dungeon.gold -= cost;
				b += 0.5f * pw(St.U_GOLDSHOT);
				if (h.sprite != null) h.sprite.showStatusWithIcon(0xFFD040, "-" + cost, FloatingText.GOLD);
				if (enemy.sprite != null && enemy.sprite.visible) enemy.sprite.centerEmitter().burst(Speck.factory(Speck.COIN), 4);
				AugFx.notify(St.U_GOLDSHOT);
			}
		}
		if (has(St.U_CHARGE) && Actor.now() - t.lastMoveAt <= 1.05f){
			b += 0.25f * pw(St.U_CHARGE);
			AugFx.notify(St.U_CHARGE);
		}
		if (has(St.U_GUST) && t.gustReady > 0) b += 0.6f * pw(St.U_GUST);
		if (h.buff(Frenzy.class) != null) b += 0.25f * pw(St.U_FRENZY);
		return b;
	}

	static boolean betrayable( Char enemy ){
		return enemy.buff(Terror.class) != null || enemy.buff(Blindness.class) != null || enemy.buff(Paralysis.class) != null;
	}

	//multiplying effects, applied after the additive ones
	static float attackMult( Hero h, Char enemy ){
		float m = 1f;
		if (has(St.U_SHATTER) && enemy.buff(Frost.class) != null){
			m *= 2f;
			shatterFx(enemy);
		}
		if (has(St.L_TIME) && h.buff(Swiftthistle.TimeBubble.class) != null) m *= 2f;
		m *= AugY.attackMult(h, enemy);
		return m;
	}

	static float critBonus( Hero h, Char enemy ){
		return has(St.U_BETRAY) && betrayable(enemy) ? 0.2f * pw(St.U_BETRAY) : 0f;
	}

	static boolean sureCrit( Hero h ){
		AugTracker t = t();
		return t != null && has(St.L_SLAYER) && Actor.now() <= t.stepUntil;
	}

	private static void shatterFx( Char enemy ){
		AugFx.notify(St.U_SHATTER);
		if (enemy.sprite == null || !enemy.sprite.visible){
			if (has(St.L_ICEDANCER)) AugY.iceShards(enemy.pos, 0);
			return;
		}
		enemy.sprite.centerEmitter().burst(SnowParticle.FACTORY, 10);
		CellEmitter.get(enemy.pos).burst(Speck.factory(Speck.STEAM), 2);
		Splash.at(enemy.sprite.center(), 0xFFC8ECFF, 8);
		Atmosphere.burstCell(enemy.pos, 2.6f, 0.85f, 0xC8ECFF, 0.4f);
		Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.9f, 1.1f);
		enemy.sprite.showStatus(0xC8ECFF, "산산조각!");
		if (has(St.L_ICEDANCER)) AugY.iceShards(enemy.pos, 0);
	}

	//guillotine: an instant kill
	static boolean guillotine( final Hero h, final Char enemy ){
		if (!has(St.U_GUILLOTINE) || Random.Float() >= 0.01f * pw(St.U_GUILLOTINE)) return false;
		final boolean big = boss(enemy);
		later(new Runnable() {
			@Override
			public void run() {
				if (!enemy.isAlive()) return;
				AugFx.notify(St.U_GUILLOTINE);
				AugMoments.bigMoment();
				if (enemy.sprite != null){
					enemy.sprite.showStatus(0xFF4030, "단두대!");
					enemy.sprite.centerEmitter().burst(BloodParticle.BURST, 16);
					AugFx.flare(enemy.sprite, 8,40, 0xFF3020, 1.6f);
				}
				Atmosphere.burstCell(enemy.pos, 5f, 1f, 0xFF4030, 1.2f);
				PostFX.flash(0xFF2010, 0.22f);
				Ambience.slowMotion(0.7f);
				GameScene.shake(2f, 0.3f);
				Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.6f);
				Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.6f, 0.8f);
				if (big){
					enemy.damage(Math.max(1, enemy.HT / 10), BLADE);
				} else {
					enemy.damage(enemy.HP + enemy.shielding() + 1, BLADE);
				}
				GLog.h("단두대가 떨어졌습니다!");
			}
		});
		return true;
	}

	//after a hero attack lands, before its damage is dealt
	static void onHit( final Hero h, final Char enemy, final int damage, final boolean thrown ){
		final AugTracker t = t();
		if (t == null) return;

		//focus fire marks
		if (has(St.U_MARK)){
			if (enemy.id() == t.markId){
				int was = t.markStacks;
				t.markStacks = Math.min(8, t.markStacks + 1);
				if (t.markStacks == 8 && was < 8){
					AugFx.notify(St.U_MARK);
					if (enemy.sprite != null) enemy.sprite.showStatus(0xFFB060, "표식 완성");
				}
			} else {
				t.markId = enemy.id();
				t.markStacks = 1;
			}
		}

		if (has(St.U_DEEPEN) && enemy.buff(Poison.class) != null){
			enemy.buff(Poison.class).extend(2f);
			if (enemy.sprite != null && enemy.sprite.visible) enemy.sprite.centerEmitter().burst(PoisonParticle.SPLASH, 3);
		}

		guillotine(h, enemy);

		final boolean reopen = has(St.U_REOPEN) && enemy.buff(Bleeding.class) != null;
		final int charge = has(St.U_STATIC) ? t.staticCharge : 0;
		if (charge > 0) t.staticCharge = 0;
		final boolean conduct = has(St.U_CONDUCT) && Dungeon.level.water[enemy.pos];
		final boolean gust = has(St.U_GUST) && t.gustReady > 0;
		if (gust){
			t.gustReady = 0;
			t.moveStreak = 0;
		}
		final boolean ricochet = thrown && has(St.U_RICOCHET);
		boolean warp = false;
		//the extra hits of a split attack don't count as attacks of their own here
		if (has(St.U_TIMEWARP) && !AugY.inFlurry){
			t.attacks++;
			if (t.attacks % Math.max(3, Math.round(5 / pw(St.U_TIMEWARP))) == 0) warp = true;
		}
		final boolean timeStop = warp;

		later(new Runnable() {
			@Override
			public void run() {
				if (reopen && enemy.isAlive() && enemy.buff(Bleeding.class) != null){
					Bleeding b = enemy.buff(Bleeding.class);
					int burst = Math.max(1, Math.round(b.level() * 2.5f));
					b.detach();
					if (enemy.sprite != null && enemy.sprite.visible){
						enemy.sprite.centerEmitter().burst(BloodParticle.BURST, 12);
						Splash.at(enemy.sprite.center(), 0xFFB00010, 10);
						enemy.sprite.showStatus(0xFF4040, "상처 폭발");
						Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 0.8f, 0.8f);
					}
					AugFx.notify(St.U_REOPEN);
					enemy.damage(burst, BLADE);
				}
				if (charge > 0 && enemy.isAlive() && h.sprite != null && enemy.sprite != null){
					int dmg = Math.max(1, Math.round(charge * (1f + depth() / 8f) * pw(St.U_STATIC)));
					ArrayList<Lightning.Arc> arcs = new ArrayList<>();
					arcs.add(new Lightning.Arc(h.sprite.center(), enemy.sprite.center()));
					h.sprite.parent.addToFront(new Lightning(arcs, null));
					enemy.sprite.centerEmitter().burst(SparkParticle.FACTORY, 2 + charge);
					Atmosphere.burstCell(enemy.pos, 1.5f + charge * 0.15f, 0.7f, 0xC8E0FF, 0.25f);
					Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 0.3f + charge * 0.04f, 1.4f);
					if (charge >= 6) AugFx.notify(St.U_STATIC);
					enemy.damage(dmg, STATIC);
				}
				if (conduct && enemy.isAlive()) conduct(h, enemy, damage);
				if (gust && enemy.isAlive() && has(St.L_DASH)){
					AugY.dash(h, enemy, charge);
				} else if (gust && enemy.isAlive()){
					AugFx.notify(St.U_GUST);
					if (enemy.sprite != null && enemy.sprite.visible){
						enemy.sprite.centerEmitter().burst(Speck.factory(Speck.JET), 4);
						enemy.sprite.showStatus(0xD8F0FF, "질풍!");
					}
					if (!Char.hasProp(enemy, Char.Property.IMMOVABLE)){
						Ballistica traj = new Ballistica(h.pos, enemy.pos, Ballistica.STOP_TARGET);
						traj = new Ballistica(traj.collisionPos, traj.path.get(Math.min(traj.dist + 3, traj.path.size() - 1)), Ballistica.PROJECTILE);
						WandOfBlastWave.throwChar(enemy, traj, 3, false, true, h);
					}
				}
				if (ricochet && damage > 0) ricochet(enemy, Math.max(1, Math.round(damage * 0.6f * pw(St.U_RICOCHET))));
				if (timeStop) stopTime(h, 2);
			}
		});

		checkSteam(enemy);
	}

	//lightning running through the water to every enemy standing in it close by
	private static void conduct( Hero h, Char enemy, int damage ){
		ArrayList<Lightning.Arc> arcs = new ArrayList<>();
		ArrayList<Char> hit = new ArrayList<>();
		for (Char ch : enemiesNear(enemy.pos, 3)){
			if (ch == enemy || !Dungeon.level.water[ch.pos] || ch.sprite == null) continue;
			arcs.add(new Lightning.Arc(enemy.sprite.center(), ch.sprite.center()));
			hit.add(ch);
		}
		if (hit.isEmpty()) return;
		AugFx.notify(St.U_CONDUCT);
		if (h.sprite != null && h.sprite.parent != null) h.sprite.parent.addToFront(new Lightning(arcs, null));
		Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 0.6f, 0.9f);
		int dmg = Math.max(1, Math.round(damage * 0.4f * pw(St.U_CONDUCT)));
		for (Char ch : hit){
			ch.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
			CellEmitter.get(ch.pos).burst(Speck.factory(Speck.STEAM), 1);
			Atmosphere.burstCell(ch.pos, 2f, 0.7f, 0xB8D8FF, 0.3f);
			ch.damage(dmg, Augs.LIGHTNING);
			AugX.counted(AugGrow.LIGHTNING, 1);
		}
	}

	private static void ricochet( final Char from, final int dmg ){
		Char best = null;
		int bd = Integer.MAX_VALUE;
		for (Char ch : enemiesNear(from.pos, 4)){
			if (ch == from || !seen(ch.pos)) continue;
			int d = Dungeon.level.distance(from.pos, ch.pos);
			if (d < bd && new Ballistica(from.pos, ch.pos, Ballistica.PROJECTILE).collisionPos == ch.pos){
				bd = d;
				best = ch;
			}
		}
		if (best == null || from.sprite == null || from.sprite.parent == null) return;
		final Char target = best;
		AugFx.notify(St.U_RICOCHET);
		bolt(from, target, MagicMissile.FORCE, new Runnable() {
			@Override
			public void run() {
				if (!target.isAlive()) return;
				Wound.hit(target);
				Sample.INSTANCE.play(Assets.Sounds.HIT, 0.7f, 1.3f);
				target.damage(dmg, BLADE);
			}
		});
	}

	//the bubble itself starts on the hero's next turn: started in the middle of someone else's turn,
	//it would pause their sprite mid-animation and the game would wait on it forever
	static void stopTime( Hero h, int turns ){
		AugTracker t = t();
		if (t != null) t.pendingStop = Math.max(t.pendingStop, turns);
		if (h.sprite != null){
			h.sprite.showStatus(0xFFF080, "시간 정지");
			AugFx.flare(h.sprite, 6,30, 0xFFF0A0, 1.2f);
			PointF c = h.sprite.center();
			Atmosphere.burst(c.x, c.y, 6f, 0.9f, 0xFFF0B0, 1.2f);
		}
		PostFX.flash(0xFFF4C0, 0.12f);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 0.6f, 1.4f);
	}

	//burning and frozen at once: the fire bursts into steam
	static void checkSteam( final Char ch ){
		if (!has(St.U_STEAM) || ch == null || !ch.isAlive() || ch.alignment != Char.Alignment.ENEMY) return;
		if (ch.buff(Burning.class) == null || !chilled(ch)) return;
		Buff.detach(ch, Burning.class);
		Buff.detach(ch, Chill.class);
		final int cell = ch.pos;
		later(new Runnable() {
			@Override
			public void run() {
				steamBlast(cell);
			}
		});
	}

	private static void steamBlast( int cell ){
		boolean hell = has(St.L_STEAM);
		int range = hell ? 2 : 1;
		int dmg = Math.round((5 + depth() * 1.5f) * (hell ? 2f : 1f) * pw(St.U_STEAM));
		AugFx.notify(St.U_STEAM);
		if (seen(cell)){
			for (int n : PathFinder.NEIGHBOURS9){
				int c = cell + n;
				if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c]) CellEmitter.get(c).burst(Speck.factory(Speck.STEAM), hell ? 6 : 4);
			}
			Atmosphere.burstCell(cell, hell ? 4.5f : 3.2f, 0.95f, 0xF0F4FF, 0.7f);
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.5f, 1.4f);
			Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.6f, 1.6f);
			if (hell) GameScene.shake(1f, 0.2f);
		}
		for (Char ch : enemiesNear(cell, range)){
			ch.damage(dmg, STEAM);
			if (hell && ch.isAlive()) Buff.prolong(ch, Blindness.class, 3f);
		}
	}

	// ================================================================ defending

	static float damageReduction( Hero h, Object src ){
		float r = 0;
		Char attacker = src instanceof Char ? (Char) src : null;
		if (attacker != null && has(St.U_EMBERARMOR) && attacker.buff(Burning.class) != null){
			r += 0.3f * pw(St.U_EMBERARMOR);
			AugFx.notify(St.U_EMBERARMOR);
		}
		if (attacker != null && has(St.U_FROSTARMOR) && chilled(attacker)){
			r += 0.4f * pw(St.U_FROSTARMOR);
			AugFx.notify(St.U_FROSTARMOR);
		}
		if (has(St.U_WALLBACK) && nearWall(h.pos)) r += 0.15f * pw(St.U_WALLBACK);
		if (has(St.U_DIGNITY)) r += Math.min(0.15f, Dungeon.gold / 1000 * 0.03f) * pw(St.U_DIGNITY);
		if (has(St.U_FULLBELLY) && notHungry(h)) r += 0.10f * pw(St.U_FULLBELLY);
		return r;
	}

	static boolean notHungry( Hero h ){
		Hunger hu = h.buff(Hunger.class);
		return hu == null || hu.hunger() < Hunger.HUNGRY;
	}

	static int capDamage( Hero h, int dmg ){
		if (!has(St.U_UNBREAK)) return dmg;
		int cap = Math.max(1, Math.round(h.HT * 0.25f / pw(St.U_UNBREAK)));
		if (dmg > cap){
			AugFx.notify(St.U_UNBREAK);
			if (h.sprite != null) h.sprite.showStatus(0xE8E0C0, "불굴");
			return cap;
		}
		return dmg;
	}

	//poison feeds the hero instead of hurting them
	public static boolean absorbPoison( Hero h, int dmg, Object src ){
		if (!has(St.U_ANTIDOTE) || dmg <= 0) return false;
		if (!(src instanceof Poison || src instanceof ToxicGas)) return false;
		AugFx.notify(St.U_ANTIDOTE);
		if (h.sprite != null && h.sprite.visible) h.sprite.emitter().burst(Speck.factory(Speck.HEALING), 1);
		Augs.heal(h, dmg);
		return true;
	}

	static void afterDamage( Hero h, int taken ){
		AugTracker t = t();
		if (t == null || !h.isAlive()) return;
		if (taken > 0) counted(AugGrow.TAKEN, taken);
		int key = AugTracker.floorKey();

		if (has(St.U_BLOODPACT) && h.HP <= h.HT / 2 && t.pactFloor != key){
			ArrayList<Char> victims = new ArrayList<>();
			for (Mob m : Dungeon.level.mobs){
				if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && seen(m.pos)) victims.add(m);
			}
			if (!victims.isEmpty()){
				t.pactFloor = key;
				bloodPact(h, victims);
			}
		}
		if (has(St.U_SEED) && h.HP <= h.HT * 0.3f && t.seedFloor != key){
			t.seedFloor = key;
			lifeSeed(h);
		}
	}

	private static void bloodPact( final Hero h, final ArrayList<Char> victims ){
		AugFx.notify(St.U_BLOODPACT);
		GLog.p("피의 계약: 주변의 생명력을 빨아들입니다!");
		later(new Runnable() {
			@Override
			public void run() {
				int total = 0;
				for (Char ch : victims){
					if (!ch.isAlive()) continue;
					int d = Math.max(1, Math.round(ch.HT * (boss(ch) ? 0.05f : 0.15f) * pw(St.U_BLOODPACT)));
					total += Math.min(d, ch.HP);
					if (ch.sprite != null && h.sprite != null){
						ch.sprite.centerEmitter().burst(BloodParticle.BURST, 8);
						MagicMissile.boltFromChar(h.sprite.parent, MagicMissile.SHAMAN_RED, ch.sprite, h.pos, null);
					}
					ch.damage(d, DRAIN);
				}
				if (h.sprite != null){
					AugFx.flare(h.sprite, 7,32, 0xC01010, 1.6f);
					PointF c = h.sprite.center();
					Atmosphere.burst(c.x, c.y, 5f, 0.9f, 0xD02020, 1.4f);
				}
				PostFX.flash(0xA01010, 0.16f);
				Sample.INSTANCE.play(Assets.Sounds.DRINK, 1f, 0.7f);
				Sample.INSTANCE.play(Assets.Sounds.CURSED, 0.5f, 1.2f);
				Augs.heal(h, total);
			}
		});
	}

	private static void lifeSeed( Hero h ){
		AugFx.notify(St.U_SEED);
		GLog.p("생명의 씨앗이 터졌습니다!");
		Augs.heal(h, Math.round(h.HT * Math.min(1f, 0.4f * pw(St.U_SEED))));
		for (Char ch : enemiesNear(h.pos, 2)){
			if (!Char.hasProp(ch, Char.Property.IMMOVABLE)) Buff.prolong(ch, Roots.class, 3f);
			if (ch.sprite != null && ch.sprite.visible) CellEmitter.get(ch.pos).burst(LeafParticle.GENERAL, 8);
		}
		if (h.sprite != null){
			for (int n : PathFinder.NEIGHBOURS9){
				int c = h.pos + n;
				if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c]) CellEmitter.get(c).burst(LeafParticle.GENERAL, 6);
			}
			AugFx.flare(h.sprite, 7,34, 0x70E060, 1.8f);
			PointF c = h.sprite.center();
			Atmosphere.burst(c.x, c.y, 5.5f, 0.9f, 0x80F070, 1.6f);
		}
		PostFX.flash(0x80E070, 0.14f);
		Sample.INSTANCE.play(Assets.Sounds.PLANT, 1f, 0.9f);
	}

	static void onDefense( Hero h, Char enemy, int damage, boolean adjacent ){
		if (adjacent && enemy.isAlive() && has(St.U_SHOVE) && Random.Float() < 0.2f * pw(St.U_SHOVE) && !Char.hasProp(enemy, Char.Property.IMMOVABLE)){
			AugFx.notify(St.U_SHOVE);
			final Char e = enemy;
			final Hero hero = h;
			later(new Runnable() {
				@Override
				public void run() {
					if (!e.isAlive()) return;
					Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 0.8f, 0.9f);
					if (e.sprite != null) e.sprite.showStatus(0xE0E0E0, "밀치기");
					Ballistica traj = new Ballistica(hero.pos, e.pos, Ballistica.STOP_TARGET);
					traj = new Ballistica(traj.collisionPos, traj.path.get(Math.min(traj.dist + 2, traj.path.size() - 1)), Ballistica.PROJECTILE);
					WandOfBlastWave.throwChar(e, traj, 2, false, true, hero);
					if (e.isAlive()) Buff.prolong(e, Paralysis.class, 1f);
				}
			});
		}
	}

	//an enemy's attack on the hero missed
	public static void onDodged( final Char attacker ){
		final Hero h = Dungeon.hero;
		if (!Augs.active() || !has(St.U_COUNTER) || h == null || !h.isAlive() || attacker == null) return;
		if (!attacker.isAlive() || !Dungeon.level.adjacent(h.pos, attacker.pos)) return;
		later(new Runnable() {
			@Override
			public void run() {
				if (!attacker.isAlive() || !h.isAlive()) return;
				AugFx.notify(St.U_COUNTER);
				int dmg = Math.max(1, Math.round(h.damageRoll() * 0.6f * pw(St.U_COUNTER)));
				if (attacker.sprite != null){
					Wound.hit(attacker);
					attacker.sprite.showStatus(0xF0E0B0, "반격");
				}
				Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1f, 1.1f);
				attacker.damage(dmg, COUNTER);
			}
		});
	}

	// ================================================================ killing

	//what a dying enemy was suffering from, taken before its buffs go away
	public static class Snap {
		boolean burning, chilled, poisoned, surprise, byPoison;
		float poison;
		Object cause;
	}

	private static Snap snap = null;

	public static void preKill( Mob m, Object cause ){
		AugTracker t = t();
		if (t == null) return;
		Snap s = new Snap();
		s.burning = m.buff(Burning.class) != null;
		s.chilled = chilled(m);
		Poison p = m.buff(Poison.class);
		s.poisoned = p != null;
		s.poison = 4 + depth() / 2f;
		s.surprise = m.id() == t.surpriseId;
		s.byPoison = cause instanceof Poison || cause instanceof ToxicGas;
		s.cause = cause;
		snap = s;
	}

	static void onKill( final Hero h, final Mob m, final AugTracker t ){
		Snap s = snap;
		snap = null;
		if (s == null) s = new Snap();
		final int pos = m.pos;

		//growth, shards and records
		AugMoments.kill();
		counted(AugGrow.KILLS, has(St.GR_DOUBLE) ? 2 : 1);
		if (s.burning) counted(AugGrow.BURN_KILLS, 1);
		addShards(h, t, boss(m) ? 15 : 1);

		if (has(St.U_THRILL)){
			Buff.prolong(h, Thrill.class, 3f);
			AugFx.notify(St.U_THRILL);
		}
		if (s.burning && has(St.U_SPREAD)){
			boolean any = false;
			for (Char ch : enemiesNear(pos, 1)){
				if (ch.isImmune(Burning.class)) continue;
				Buff.affect(ch, Burning.class).reignite(ch);
				if (ch.sprite != null && ch.sprite.visible) ch.sprite.centerEmitter().burst(FlameParticle.FACTORY, 6);
				any = true;
				checkSteam(ch);
			}
			if (any){
				AugFx.notify(St.U_SPREAD);
				if (seen(pos)){
					CellEmitter.get(pos).burst(ElmoParticle.FACTORY, 10);
					Atmosphere.burstCell(pos, 2.8f, 0.85f, 0xFF9040, 0.5f);
					Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.6f, 1.2f);
				}
			}
		}
		if (s.chilled && has(St.U_COLDHEAL)){
			AugFx.notify(St.U_COLDHEAL);
			Augs.heal(h, Math.round((3 + depth() / 2) * pw(St.U_COLDHEAL)));
			if (h.sprite != null) h.sprite.emitter().burst(SnowParticle.FACTORY, 4);
		}
		if (s.poisoned && has(St.U_PLAGUE)){
			boolean any = false;
			for (Char ch : enemiesNear(pos, 2)){
				if (ch.isImmune(Poison.class)) continue;
				Buff.affect(ch, Poison.class).set(Math.max(3f, s.poison * 0.6f));
				if (ch.sprite != null && ch.sprite.visible) ch.sprite.centerEmitter().burst(PoisonParticle.SPLASH, 5);
				any = true;
			}
			if (any){
				AugFx.notify(St.U_PLAGUE);
				if (seen(pos)) CellEmitter.center(pos).burst(PoisonParticle.SPLASH, 12);
			}
		}
		if (s.byPoison && has(St.U_ROT)){
			AugFx.notify(St.U_ROT);
			GameScene.add(Blob.seed(pos, 80, ToxicGas.class));
		}
		if (s.surprise && has(St.U_THIEF) && Random.Float() < 0.3f * pw(St.U_THIEF)){
			AugFx.notify(St.U_THIEF);
			steal(h, pos);
		}
		if (has(St.U_INTIMIDATE)){
			boolean any = false;
			for (Char ch : enemiesNear(pos, 2)){
				if (boss(ch)) continue;
				Buff.affect(ch, Terror.class, 3f).object = h.id();
				if (ch.sprite != null && ch.sprite.visible) ch.sprite.showStatus(0xFFB0B0, "!");
				any = true;
			}
			if (any) AugFx.notify(St.U_INTIMIDATE);
		}
		if (has(St.U_FRENZY)){
			float now = Actor.now();
			t.killTimes[0] = t.killTimes[1];
			t.killTimes[1] = t.killTimes[2];
			t.killTimes[2] = now;
			if (now - t.killTimes[0] <= 10f && h.buff(Frenzy.class) == null){
				Buff.affect(h, Frenzy.class, 5f);
				AugFx.notify(St.U_FRENZY);
				if (h.sprite != null){
					h.sprite.showStatus(0xFF4030, "광란!");
					AugFx.flare(h.sprite, 6,26, 0xFF3020, 1.2f);
				}
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.7f, 1.2f);
				java.util.Arrays.fill(t.killTimes, -100);
			}
		}
		if (has(St.U_EXECSWING) && Actor.now() - t.execSwingAt >= 8f / pw(St.U_EXECSWING) && h.buff(Swiftthistle.TimeBubble.class) == null){
			t.execSwingAt = Actor.now();
			AugFx.notify(St.U_EXECSWING);
			stopTime(h, 1);
		}
		if (has(St.U_DEVOUR)){
			Hunger hu = h.buff(Hunger.class);
			if (hu != null) hu.satisfy(Math.min(60, 10 + m.HT / 2));
		}
		if (has(St.U_SHADOWSTEP) && (has(St.L_SLAYER) || Actor.now() - t.shadowStepAt >= 5f / pw(St.U_SHADOWSTEP))){
			later(new Runnable() {
				@Override
				public void run() {
					shadowStep(h, t, m);
				}
			});
		}
		if (has(St.U_CHAINBLAST)){
			float chance = (s.cause == CHAIN ? 0.3f : 0.05f) * pw(St.U_CHAINBLAST);
			if (Random.Float() < chance){
				final int hp = m.HT;
				later(new Runnable() {
					@Override
					public void run() {
						chainBlast(pos, hp);
					}
				});
			}
		}
		if (has(St.U_JACKPOT) && Random.Float() < 0.01f * pw(St.U_JACKPOT) * (has(St.L_ROULETTE) ? 2f : 1f)){
			jackpot(h, pos);
		}
		AugY.onKill(h, m, t, s);
	}

	private static void steal( Hero h, int pos ){
		Item loot;
		switch (Random.Int(4)){
			case 0: case 1:
				loot = new Gold(Random.IntRange(5 + depth() * 3, 12 + depth() * 6));
				break;
			case 2:
				loot = Reflection.newInstance(Random.oneOf(
						com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed.class,
						com.shatteredpixel.shatteredpixeldungeon.plants.Fadeleaf.Seed.class,
						com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle.Seed.class,
						com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot.Seed.class,
						com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom.Seed.class,
						com.shatteredpixel.shatteredpixeldungeon.plants.Icecap.Seed.class));
				break;
			default:
				loot = Reflection.newInstance(Random.oneOf(
						com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink.class,
						com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfShock.class,
						com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfDetectMagic.class,
						com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfFear.class));
				break;
		}
		if (loot == null) return;
		Dungeon.level.drop(loot, pos).sprite.drop();
		if (seen(pos)){
			CellEmitter.get(pos).burst(ShadowParticle.UP, 6);
			Sample.INSTANCE.play(Assets.Sounds.ITEM, 0.8f, 1.2f);
		}
		if (h.sprite != null) h.sprite.showStatus(0xC8B0FF, "훔침!");
	}

	private static void shadowStep( Hero h, AugTracker t, Mob dead ){
		if (!h.isAlive() || h.sprite == null) return;
		int best = -1;
		int bestD = Integer.MAX_VALUE;
		for (Mob m : Dungeon.level.mobs){
			if (m == dead || m.alignment != Char.Alignment.ENEMY || !m.isAlive() || !seen(m.pos)) continue;
			int d = Dungeon.level.distance(h.pos, m.pos);
			if (d <= 1 || d > 6 || d >= bestD) continue;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = m.pos + n;
				if (c < 0 || c >= Dungeon.level.length()) continue;
				if (Dungeon.level.passable[c] && !Dungeon.level.avoid[c] && Actor.findChar(c) == null && seen(c)){
					best = c;
					bestD = d;
					break;
				}
			}
		}
		if (best < 0) return;
		t.shadowStepAt = Actor.now();
		t.stepUntil = Actor.now() + 2f;
		AugFx.notify(St.U_SHADOWSTEP);
		CellEmitter.get(h.pos).burst(ShadowParticle.UP, 10);
		ScrollOfTeleportation.appear(h, best);
		CellEmitter.get(best).burst(ShadowParticle.UP, 10);
		Atmosphere.burstCell(best, 2.5f, 0.6f, 0x9070C0, 0.5f);
		Sample.INSTANCE.play(Assets.Sounds.PUFF, 0.8f, 0.8f);
		Dungeon.observe();
		GameScene.updateFog();
	}

	private static void chainBlast( int cell, int hp ){
		AugFx.notify(St.U_CHAINBLAST);
		AugMoments.bigMoment();
		int dmg = Math.round(Math.max(4, Math.round(hp * 0.5f) + depth() * 2) * pw(St.U_CHAINBLAST));
		if (seen(cell)){
			for (int n : PathFinder.NEIGHBOURS9){
				int c = cell + n;
				if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c]){
					CellEmitter.center(c).burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle.FACTORY, 12);
				}
			}
			CellEmitter.get(cell).burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle.FACTORY, 8);
			Atmosphere.burstCell(cell, 5f, 1f, 0xFFB050, 0.8f);
			PostFX.flash(0xFFC070, 0.12f);
			GameScene.shake(2f, 0.3f);
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.9f);
		}
		for (Char ch : enemiesNear(cell, 2)){
			ch.damage(dmg, CHAIN);
		}
	}

	private static void jackpot( Hero h, int pos ){
		AugFx.notify(St.U_JACKPOT);
		AugMoments.bigMoment();
		Augs.tracker().jackpots++;
		GLog.h("잭팟! 골드가 쏟아집니다!");
		int total = Math.round(depth() * 40 * pw(St.U_JACKPOT));
		int piles = 6;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS9){
			int c = pos + n;
			if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c]) cells.add(c);
		}
		if (cells.isEmpty()) cells.add(pos);
		for (int i = 0; i < piles; i++){
			Dungeon.level.drop(new Gold(Math.max(1, total / piles)), Random.element(cells)).sprite.drop();
		}
		if (seen(pos)){
			CellEmitter.center(pos).burst(Speck.factory(Speck.COIN), 24);
			Atmosphere.burstCell(pos, 5f, 1f, 0xFFD860, 1.6f);
		}
		if (h.sprite != null){
			AugFx.flare(h.sprite, 10,48, 0xFFD040, 2f);
			h.sprite.showStatus(0xFFD040, "잭팟!");
		}
		PostFX.flash(0xFFD860, 0.2f);
		Ambience.slowMotion(0.8f);
		Sample.INSTANCE.play(Assets.Sounds.GOLD, 1f, 0.8f);
		Sample.INSTANCE.play(Assets.Sounds.LEVELUP, 0.8f, 1.2f);
	}

	// ================================================================ growth counters and shards

	static void counted( int counter, int amount ){
		AugTracker t = t();
		if (t == null || amount <= 0) return;
		t.cnt[counter] += amount;
		if (t.hasGrowth()) AugGrow.changed(t);
	}

	static void addShards( Hero h, AugTracker t, int n ){
		if (!t.shardsActive()) return;
		//each cycle starts a fifth of the way full: a head start makes the goal feel closer
		if (!t.shardsPrimed){
			t.shardsPrimed = true;
			t.shards += t.shardsNeeded() / 5;
		}
		t.shards += n;
		BuffIndicator.refreshHero();
		if (t.shards < t.shardsNeeded()) return;
		t.shards -= t.shardsNeeded();
		t.shardPicks++;
		t.shards += t.shardsNeeded() / 5;
		t.pending++;
		GLog.h("증강 파편이 모였습니다! 증강을 하나 더 고를 수 있습니다.");
		if (h.sprite != null){
			AugFx.flare(h.sprite, 8,40, 0xFFD45A, 2f);
			PointF c = h.sprite.center();
			Atmosphere.burst(c.x, c.y, 6f, 0.95f, 0xFFD870, 1.8f);
			h.sprite.emitter().burst(Speck.factory(Speck.STAR), 12);
			h.sprite.showStatus(0xFFD45A, "증강 파편!");
		}
		PostFX.flash(0xFFE090, 0.14f);
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 0.9f);
	}

	//storm's eye: a huge bolt from the sky on every enemy around
	static void megaStrike(){
		final Hero h = Dungeon.hero;
		if (h == null || !h.isAlive()) return;
		later(new Runnable() {
			@Override
			public void run() {
				ArrayList<Char> hit = new ArrayList<>();
				for (Char ch : enemiesNear(h.pos, 5)) if (seen(ch.pos)) hit.add(ch);
				GLog.h("폭풍의 눈: 거대한 낙뢰가 떨어집니다!");
				AugMoments.bigMoment();
				PostFX.flash(0xD8E8FF, 0.3f);
				Ambience.slowMotion(0.6f);
				GameScene.shake(3f, 0.4f);
				Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 1f, 0.7f);
				Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.7f, 0.8f);
				Aug a = Aug.get("폭풍의 눈");
				if (a != null) AugFx.force(a, 0xC8E0FF);
				int dmg = 10 + depth() * 3;
				for (Char ch : hit){
					if (ch.sprite != null && h.sprite != null && h.sprite.parent != null){
						PointF to = ch.sprite.center();
						ArrayList<Lightning.Arc> arcs = new ArrayList<>();
						arcs.add(new Lightning.Arc(new PointF(to.x + Random.Float(-4, 4), to.y - 60), to));
						h.sprite.parent.addToFront(new Lightning(arcs, null));
						ch.sprite.centerEmitter().burst(SparkParticle.FACTORY, 10);
						Atmosphere.burstCell(ch.pos, 4f, 1f, 0xC8E0FF, 0.6f);
					}
					ch.damage(dmg, SKY);
				}
			}
		});
	}

	// ================================================================ floors and turns

	static void onNewFloor( Hero h ){
		AugTracker t = t();
		if (t == null) return;
		if (t.hasGrowth()) AugGrow.changed(t);
		AugAllies.summonForFloor(h);
		AugY.onNewFloor(h, t);

		if (has(St.U_INTEREST) && Dungeon.gold > 0){
			int amt = Math.round(Math.min(depth() * 25, Math.max(1, Dungeon.gold / 20)) * pw(St.U_INTEREST));
			Dungeon.gold += amt;
			counted(AugGrow.GOLD, amt);
			AugFx.notify(St.U_INTEREST);
			if (h.sprite != null) h.sprite.showStatusWithIcon(0xFFD040, "+" + amt, FloatingText.GOLD);
			GLog.p("이자로 골드 " + amt + "을 받았습니다.");
		}
		if (has(St.U_APPRAISE)){
			int n = 0;
			for (Heap heap : Dungeon.level.heaps.valueList()){
				if (heap.type == Heap.Type.FOR_SALE) continue;
				Dungeon.level.mapped[heap.pos] = true;
				n++;
			}
			if (n > 0){
				GameScene.updateFog();
				AugFx.notify(St.U_APPRAISE);
				GLog.p("보물 감별사: 이 층의 보물 " + n + "곳이 드러났습니다.");
			}
		}
	}

	static void everyTurn( Hero h, AugTracker t ){
		//war avatar: the strongest foes bring out the best
		if (has(St.U_AVATAR)){
			boolean bossNear = false;
			for (Mob m : Dungeon.level.mobs){
				if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && boss(m) && seen(m.pos)){
					bossNear = true;
					break;
				}
			}
			if (bossNear != t.avatar){
				t.avatar = bossNear;
				if (bossNear){
					Buff.affect(h, Avatar.class);
					AugFx.notify(St.U_AVATAR);
					if (h.sprite != null){
						h.sprite.showStatus(0xFF8030, "전쟁의 화신!");
						AugFx.flare(h.sprite, 8,36, 0xFF6020, 1.6f);
					}
					Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.9f);
				} else {
					Buff.detach(h, Avatar.class);
				}
				t.recalc();
			}
		} else if (t.avatar){
			t.avatar = false;
			Buff.detach(h, Avatar.class);
		}

		//standing your ground
		if (h.pos == t.stillPos){
			if (has(St.U_STEADFAST)){
				int cap = Math.max(1, Math.round(h.HT * 0.15f * pw(St.U_STEADFAST)));
				Barrier b = h.buff(Barrier.class);
				int cur = b == null ? 0 : b.shielding();
				if (cur < cap){
					Buff.affect(h, Barrier.class).incShield(1);
					if (cur + 1 == cap && h.sprite != null){
						h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(cap), FloatingText.SHIELDING);
						AugFx.notify(St.U_STEADFAST);
					}
				}
			}
			if (Actor.now() - t.lastMoveAt > 2f){
				t.moveStreak = 0;
				t.gustReady = 0;
			}
		}
		t.stillPos = h.pos;
		AugY.everyTurn(h, t);
	}

	public static void onHeroMove( int from, int to ){
		AugTracker t = t();
		Hero h = Dungeon.hero;
		if (t == null || h == null || from == to || from < 0) return;
		t.lastMoveAt = Actor.now();

		if (has(St.U_STATIC) && t.staticCharge < 10){
			t.staticCharge++;
			if (h.sprite != null && h.sprite.visible && t.staticCharge % 2 == 0) h.sprite.centerEmitter().burst(SparkParticle.STATIC, 1);
			if (t.staticCharge == 10) AugFx.notify(St.U_STATIC);
		}

		int dir = to - from;
		if (dir == t.moveDir) t.moveStreak++;
		else {
			t.moveDir = dir;
			t.moveStreak = 1;
		}
		if (has(St.U_GUST) && t.moveStreak >= 3 && t.gustReady == 0){
			t.gustReady = 1;
			if (h.sprite != null && h.sprite.visible){
				h.sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.WindParticle.FACTORY, 4);
			}
		}

		int terr = Dungeon.level.map[to];
		if (terr == Terrain.DOOR || terr == Terrain.OPEN_DOOR){
			if (has(St.U_FIREDASH) && Actor.now() - t.fireDashAt >= 5f / pw(St.U_FIREDASH)){
				boolean any = false;
				for (Char ch : enemiesNear(to, 2)){
					if (ch.isImmune(Burning.class)) continue;
					Buff.affect(ch, Burning.class).reignite(ch);
					any = true;
				}
				if (any){
					t.fireDashAt = Actor.now();
					AugFx.notify(St.U_FIREDASH);
					for (int n : PathFinder.NEIGHBOURS9){
						int c = to + n;
						if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c]) CellEmitter.get(c).burst(FlameParticle.FACTORY, 4);
					}
					Atmosphere.burstCell(to, 3.5f, 0.9f, 0xFF9040, 0.7f);
					Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.8f);
				}
			}
			if (has(St.U_DOORHASTE) && Actor.now() - t.doorHasteAt >= 10f / pw(St.U_DOORHASTE)){
				t.doorHasteAt = Actor.now();
				Buff.prolong(h, Haste.class, 3f);
				AugFx.notify(St.U_DOORHASTE);
				if (h.sprite != null) h.sprite.emitter().burst(SparkParticle.STATIC, 4);
			}
		}
		AugY.onHeroMove(from, to);
	}

	//start of the hero's turn
	public static void heroTurn( Hero h ){
		AugTracker t = t();
		if (t == null) return;
		AugY.heroTurn(h, t);
		if (t.pendingStop <= 0) return;
		int turns = t.pendingStop;
		t.pendingStop = 0;
		if (h.buff(Swiftthistle.TimeBubble.class) == null) Buff.affect(h, Swiftthistle.TimeBubble.class).reset(turns);
	}

	// ================================================================ items, terrain and gold

	public static void onWandUsed(){
		Hero h = Dungeon.hero;
		if (!Augs.active() || h == null) return;
		AugQuest.onWandUsed();
		if (!has(St.U_MANACYCLE)) return;
		Augs.heal(h, Math.round((2 + depth() / 3) * pw(St.U_MANACYCLE)));
		AugFx.notify(St.U_MANACYCLE);
	}

	//a wand is about to hurt an enemy
	static int wandHit( final Char target, int dmg ){
		AugTracker t = t();
		if (t == null) return dmg;
		if (has(St.U_DICE)){
			//one roll per zap: everything a single zap hits shares it
			float now = Actor.now();
			if (now != t.diceAt){
				t.diceAt = now;
				t.diceMult = Random.Float() < 0.05f * pw(St.U_DICE) ? 5f : 1f;
				if (t.diceMult > 1f){
					AugFx.notify(St.U_DICE);
					AugMoments.bigMoment();
					GLog.h("운명의 주사위: 위력 5배!");
					PostFX.flash(0xE0A0FF, 0.18f);
					Ambience.slowMotion(0.5f);
					Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 1.3f);
				}
			}
			if (t.diceMult > 1f){
				dmg = Math.round(dmg * t.diceMult);
				if (target.sprite != null){
					AugFx.flare(target.sprite, 6,28, 0xE0A0FF, 1f);
					target.sprite.showStatus(0xE0A0FF, "x5");
				}
			}
		}
		final int base = dmg;
		if (has(St.U_CHAINSPELL) && Random.Float() < 0.25f * pw(St.U_CHAINSPELL)){
			later(new Runnable() {
				@Override
				public void run() {
					chainSpell(target, Math.max(1, base / 2));
				}
			});
		}
		if (has(St.U_ELESTORM) && Random.Float() < 0.3f * pw(St.U_ELESTORM)){
			final int cell = target.pos;
			later(new Runnable() {
				@Override
				public void run() {
					AugFx.notify(St.U_ELESTORM);
					switch (Random.Int(3)){
						case 0: Augs.fireBurst(cell, null); break;
						case 1: Augs.frostBurst(cell, null); coldApplied(); break;
						default:
							Char c = Actor.findChar(cell);
							if (c != null && c.isAlive() && Dungeon.hero.sprite != null && c.sprite != null){
								Augs.chainLightning(Dungeon.hero, c, Math.max(1, base / 2));
							} else {
								Augs.explosion(cell, Math.max(1, base / 2), true, null);
							}
							break;
					}
				}
			});
		}
		return dmg;
	}

	private static void chainSpell( Char from, final int dmg ){
		Char best = null;
		int bd = Integer.MAX_VALUE;
		for (Char ch : enemiesNear(from.pos, 4)){
			if (ch == from || !seen(ch.pos)) continue;
			int d = Dungeon.level.distance(from.pos, ch.pos);
			if (d < bd){ bd = d; best = ch; }
		}
		if (best == null || from.sprite == null || from.sprite.parent == null) return;
		final Char target = best;
		AugFx.notify(St.U_CHAINSPELL);
		bolt(from, target, MagicMissile.MAGIC_MISSILE, new Runnable() {
			@Override
			public void run() {
				if (!target.isAlive()) return;
				if (target.sprite != null) target.sprite.burst(0xFFC890FF, 4);
				Sample.INSTANCE.play(Assets.Sounds.ZAP, 0.6f, 1.3f);
				target.damage(dmg, SPELL);
			}
		});
	}

	//enemies take poison damage; returns the adjusted amount
	public static int poisonDamage( Char target, int dmg ){
		if (target.alignment != Char.Alignment.ENEMY) return dmg;
		AugTracker t = t();
		if (t == null) return dmg;
		counted(AugGrow.POISON, 1);
		dmg += Math.round(Augs.st(St.POISON_PLUS));
		if (has(St.U_ROT)) dmg *= 2;
		return dmg;
	}

	public static void onPotion( Hero h ){
		if (!Augs.active() || !has(St.U_POTIONWARD)) return;
		Buff.affect(h, Invulnerability.class, 2f);
		AugFx.notify(St.U_POTIONWARD);
		if (h.sprite != null){
			AugFx.flare(h.sprite, 5,22, 0xE8E0FF, 1f);
			h.sprite.showStatus(0xE8E0FF, "주문 보호");
		}
	}

	public static void onScrollRead(){
		Hero h = Dungeon.hero;
		if (!Augs.active() || h == null) return;
		AugQuest.onScrollRead();
		if (!has(St.U_SCROLLRES)) return;
		boolean any = false;
		for (Item i : h.belongings){
			if (i instanceof Wand && ((Wand) i).curCharges < ((Wand) i).maxCharges){
				((Wand) i).curCharges++;
				i.updateQuickslot();
				any = true;
			}
		}
		if (any){
			AugFx.notify(St.U_SCROLLRES);
			ScrollOfRecharging.charge(h);
		}
	}

	public static void onTrap( int cell ){
		if (!Augs.active() || !has(St.U_TRAPPER)) return;
		final Char ch = Actor.findChar(cell);
		if (ch == null || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()) return;
		AugFx.notify(St.U_TRAPPER);
		if (!Char.hasProp(ch, Char.Property.IMMOVABLE)) Buff.prolong(ch, Roots.class, 4f);
		if (ch.sprite != null && ch.sprite.visible){
			CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 4);
			ch.sprite.showStatus(0xE0C090, "덫!");
		}
		final int dmg = Math.round((3 + depth() * 2) * pw(St.U_TRAPPER));
		later(new Runnable() {
			@Override
			public void run() {
				if (ch.isAlive()) ch.damage(dmg, BLADE);
			}
		});
	}

	public static void onPlant( int cell ){
		Hero h = Dungeon.hero;
		if (!Augs.active() || h == null || h.pos != cell || !has(St.U_HERBALIST)) return;
		AugFx.notify(St.U_HERBALIST);
		Augs.heal(h, Math.round(h.HT * 0.15f * pw(St.U_HERBALIST)));
		if (h.sprite != null) h.sprite.emitter().burst(LeafParticle.GENERAL, 8);
	}

	public static void onTrample( int cell ){
		Hero h = Dungeon.hero;
		if (!Augs.active() || h == null || h.pos != cell || !has(St.U_GRASSCOIN)) return;
		if (Random.Float() >= 0.1f * pw(St.U_GRASSCOIN)) return;
		int amt = Augs.scaleGold(Random.IntRange(depth() * 2, depth() * 4 + 5));
		Dungeon.gold += amt;
		counted(AugGrow.GOLD, amt);
		AugFx.notify(St.U_GRASSCOIN);
		if (h.sprite != null){
			h.sprite.showStatusWithIcon(0xFFD040, "+" + amt, FloatingText.GOLD);
			CellEmitter.get(cell).burst(Speck.factory(Speck.COIN), 3);
		}
		Sample.INSTANCE.play(Assets.Sounds.GOLD, 0.6f, 1.2f);
	}

	public static void onGold( int amount ){
		if (Augs.active()) counted(AugGrow.GOLD, amount);
	}

	public static void onHeapOpened( Heap heap, Heap.Type type ){
		if (!Augs.active() || !has(St.U_APPRAISE)) return;
		if (type != Heap.Type.CHEST && type != Heap.Type.LOCKED_CHEST && type != Heap.Type.CRYSTAL_CHEST
				&& type != Heap.Type.SKELETON && type != Heap.Type.REMAINS && type != Heap.Type.TOMB) return;
		if (Random.Float() >= Math.min(1f, 0.5f * pw(St.U_APPRAISE))) return;
		AugFx.notify(St.U_APPRAISE);
		Gold g = new Gold(Random.IntRange(depth() * 5, depth() * 10 + 10));
		Dungeon.level.drop(g, heap.pos).sprite.drop();
		CellEmitter.center(heap.pos).burst(Speck.factory(Speck.COIN), 8);
	}

	//an augment just chilled or froze something
	static void coldApplied(){
		counted(AugGrow.COLD, 1);
	}

	//wraith servants: at most two at once
	static boolean wraithRoom(){
		int n = 0;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith && m.alignment == Char.Alignment.ALLY && m.isAlive()) n++;
		}
		return n < 2;
	}
}
