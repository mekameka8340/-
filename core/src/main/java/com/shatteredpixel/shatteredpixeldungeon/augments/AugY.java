/*
 * The newer augments: the ones that change how an attack works (split hits, wind slashes, brands,
 * chains of souls, returning throws...), the newer gambles and legends, and the effects they need
 * from the game's hooks. AugX holds the older unique effects.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.Ambience;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.Atmosphere;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.PostFX;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.effects.Wound;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PoisonParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.WindParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.DamageWand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.EnumMap;

public class AugY {

	public static final Augs.Src FLURRY   = new Augs.Src("flurry");
	public static final Augs.Src GALE     = new Augs.Src("gale");
	public static final Augs.Src BRAND    = new Augs.Src("brand");
	public static final Augs.Src CHAIN    = new Augs.Src("soulchain");
	public static final Augs.Src SPLIT    = new Augs.Src("split");
	public static final Augs.Src MELODY   = new Augs.Src("melody");
	public static final Augs.Src REFLECT  = new Augs.Src("reflect");
	public static final Augs.Src RITUAL   = new Augs.Src("ritual");
	public static final Augs.Src TWIN     = new Augs.Src("twin");
	public static final Augs.Src REPLAY   = new Augs.Src("replay");
	public static final Augs.Src DASH     = new Augs.Src("dash");
	public static final Augs.Src MOUNTAIN = new Augs.Src("mountain");
	public static final Augs.Src SHARD    = new Augs.Src("shard");

	private static boolean has( St s ){
		return Augs.has(s);
	}

	private static AugTracker t(){
		return Augs.tracker();
	}

	private static float pw( St s ){
		return AugX.pw(s);
	}

	private static int depth(){
		return Math.max(1, Dungeon.depth);
	}

	private static boolean seen( int cell ){
		return Dungeon.level != null && cell >= 0 && cell < Dungeon.level.length() && Dungeon.level.heroFOV[cell];
	}

	static boolean boss( Char ch ){
		return Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS);
	}

	private static ArrayList<Char> enemiesNear( int cell, int range ){
		ArrayList<Char> out = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && Dungeon.level.distance(cell, m.pos) <= range) out.add(m);
		}
		return out;
	}

	private static int visibleEnemies(){
		int n = 0;
		for (Mob m : Dungeon.level.mobs){
			if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && seen(m.pos)) n++;
		}
		return n;
	}

	//runs right after the current action, so extra damage never lands before the hit that caused it
	static void later( final Runnable r ){
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

	//one step of a paced sequence; returns false to stop early
	interface Step { boolean run( int i ); }

	//a few hits one after another, a moment apart. Its actor waits until the last one, so nothing else moves meanwhile
	static void paced( final int count, final float interval, final Step step ){
		if (count <= 0) return;
		Actor.add(new Actor() {
			{ actPriority = VFX_PRIO; }
			@Override
			protected boolean act() {
				Actor.remove(this);
				final Actor self = this;
				Hero h = Dungeon.hero;
				if (h == null || h.sprite == null || h.sprite.parent == null){
					for (int i = 0; i < count; i++) if (!step.run(i)) break;
					return true;
				}
				h.sprite.parent.add(new Gizmo(){
					float time = interval;
					int i = 0;
					boolean done = false;
					@Override
					public void update() {
						super.update();
						time += Game.elapsed;
						while (!done && time >= interval && i < count){
							time -= interval;
							boolean go;
							try {
								go = step.run(i);
							} catch (Exception e){
								ShatteredPixelDungeon.reportException(e);
								go = false;
							}
							i++;
							if (!go) i = count;
						}
						if (!done && i >= count) finish();
					}
					private void finish(){
						done = true;
						killAndErase();
						self.next();
					}
					@Override
					public void destroy() {
						super.destroy();
						//the scene went away halfway: never leave the game waiting
						if (!done){
							done = true;
							self.next();
						}
					}
				});
				return false;
			}
		});
	}

	//a thrown copy flying to a target, dealt when it arrives
	private static void missile( final Item item, final ArrayList<Char> targets, final int dmg ){
		Actor.add(new Actor() {
			{ actPriority = VFX_PRIO; }
			@Override
			protected boolean act() {
				Actor.remove(this);
				final Hero h = Dungeon.hero;
				if (h == null || h.sprite == null || h.sprite.parent == null){
					for (Char c : targets) if (c.isAlive()) c.damage(dmg, SPLIT);
					return true;
				}
				final Actor self = this;
				final int[] left = {targets.size()};
				for (final Char c : targets){
					((MissileSprite) h.sprite.parent.recycle(MissileSprite.class)).reset(h.sprite, c.pos, item, new Callback() {
						@Override
						public void call() {
							if (c.isAlive()){
								Wound.hit(c);
								Sample.INSTANCE.play(Assets.Sounds.HIT, 0.7f, 1.2f);
								c.damage(dmg, SPLIT);
							}
							if (--left[0] <= 0) self.next();
						}
					});
				}
				return false;
			}
		});
	}

	// ================================================================ attacking

	//true while the extra hits of a split attack are being dealt
	static boolean inFlurry = false;

	//split attack: the damage that got past armor is dealt in four hits, the first one right away
	public static int flurry( final Hero h, final Char enemy, int dmg ){
		if (!has(St.U_FLURRY) || dmg <= 1 || inFlurry) return dmg;
		if (h.belongings.attackingWeapon() instanceof MissileWeapon) return dmg;
		final int hits = Math.min(4, dmg);
		final int q = dmg / hits;
		int first = dmg - q * (hits - 1);
		AugFx.notify(St.U_FLURRY);
		paced(hits - 1, 0.09f, new Step() {
			@Override
			public boolean run( int i ) {
				if (!enemy.isAlive() || !h.isAlive()) return false;
				int d = q;
				float critChance = Augs.st(St.CRIT) + AugX.critBonus(h, enemy);
				if (sureCrit() || (critChance > 0 && Random.Float() < critChance)){
					d = Math.round(d * (1.5f + Augs.st(St.CRITD)));
					Augs.critEffect(enemy);
				}
				inFlurry = true;
				try {
					Augs.onHit(h, enemy, d);
				} finally {
					inFlurry = false;
				}
				if (!enemy.isAlive()) return false;
				if (enemy.sprite != null && enemy.sprite.visible){
					Wound.hit(enemy);
					enemy.sprite.flash();
				}
				Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 0.6f, 1.1f + i * 0.12f);
				enemy.damage(d, FLURRY);
				return enemy.isAlive();
			}
		});
		return first;
	}

	static boolean sureCrit(){
		if (has(St.SURE_CRIT)) return true;
		AugTracker t = t();
		return t != null && blessingNow(t) == 0;
	}

	//multiplying effects of the newer augments, applied once the attack is known to land
	static float attackMult( Hero h, Char enemy ){
		AugTracker t = t();
		if (t == null) return 1f;
		float m = 1f;
		if (has(St.U_FINISHER) && enemy.alignment == Char.Alignment.ENEMY && visibleEnemies() <= 1){
			m *= 1f + 2f * pw(St.U_FINISHER);
			t.finisherId = enemy.id();
			AugFx.notify(St.U_FINISHER);
		}
		if (t.calm && has(St.U_CALM)){
			t.calm = false;
			m *= 1f + 1.5f * pw(St.U_CALM);
			AugFx.notify(St.U_CALM);
			if (h.sprite != null) h.sprite.showStatus(0xC8E0FF, "폭풍!");
			if (enemy.sprite != null && h.sprite != null && h.sprite.parent != null){
				ArrayList<Lightning.Arc> arcs = new ArrayList<>();
				arcs.add(new Lightning.Arc(new PointF(enemy.sprite.center().x, enemy.sprite.center().y - 40), enemy.sprite.center()));
				h.sprite.parent.addToFront(new Lightning(arcs, null));
			}
			Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 0.8f, 0.8f);
		}
		return m;
	}

	//after a hero attack lands, before its damage is dealt (also for each extra hit of a split attack)
	static void onHit( final Hero h, final Char enemy, final int damage, final boolean thrown, final boolean melee ){
		final AugTracker t = t();
		if (t == null) return;
		boolean primary = !inFlurry;

		//brand: every third hit on the same enemy bursts
		if (has(St.U_BRAND)){
			if (enemy.id() == t.brandId) t.brandHits++;
			else {
				t.brandId = enemy.id();
				t.brandHits = 1;
			}
			if (t.brandHits >= 3){
				t.brandHits = 0;
				final int burst = Math.max(1, Math.round(damage * 1.5f * pw(St.U_BRAND)));
				later(new Runnable() {
					@Override
					public void run() {
						if (!enemy.isAlive()) return;
						AugFx.notify(St.U_BRAND);
						if (enemy.sprite != null && enemy.sprite.visible){
							enemy.sprite.centerEmitter().burst(ShadowParticle.CURSE, 12);
							AugFx.flare(enemy.sprite, 5,20, 0xB040FF, 0.8f);
							enemy.sprite.showStatus(0xD080FF, "낙인 폭발");
							Atmosphere.burstCell(enemy.pos, 2.4f, 0.8f, 0xB060FF, 0.4f);
						}
						Sample.INSTANCE.play(Assets.Sounds.CURSED, 0.6f, 1.4f);
						Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 0.7f, 1.2f);
						enemy.damage(burst, BRAND);
					}
				});
			} else if (enemy.sprite != null && enemy.sprite.visible){
				enemy.sprite.centerEmitter().burst(ShadowParticle.CURSE, 3 * t.brandHits);
			}
		}

		//a chain of souls between this enemy and the closest other one
		if (has(St.U_SOULCHAIN)){
			Char a = chained(t, 0), b = chained(t, 1);
			boolean linked = (a == enemy || b == enemy) && a != null && b != null && a.isAlive() && b.isAlive();
			if (!linked) linkSouls(h, t, enemy);
		}

		if (has(St.U_RITUAL) && side(enemy.pos, h.pos) != 0){
			//attacking from beside an enemy starts its circle too
			if (enemy.id() != t.ritualId){
				Actor cur = t.ritualId < 0 ? null : Actor.findById(t.ritualId);
				if (!(cur instanceof Char) || !((Char) cur).isAlive() || !Dungeon.level.adjacent(h.pos, ((Char) cur).pos)){
					t.ritualId = enemy.id();
					t.ritualMask = 0;
				}
			}
			if (enemy.id() == t.ritualId) ritualSide(h, t, enemy, h.pos);
		}

		if (!primary) return;

		//wind slash down the line of the attack
		if (melee && has(St.U_GALE) && damage > 0){
			final int dmg = Math.max(1, Math.round(damage * 0.5f * pw(St.U_GALE)));
			later(new Runnable() {
				@Override
				public void run() {
					gale(h, enemy, dmg);
				}
			});
		}

		//thrown copies to two more enemies
		if (thrown && has(St.U_SPLIT) && damage > 0){
			Item item = h.belongings.attackingWeapon();
			ArrayList<Char> targets = new ArrayList<>();
			for (Char c : enemiesNear(enemy.pos, 3)){
				if (c == enemy || !seen(c.pos)) continue;
				if (new Ballistica(h.pos, c.pos, Ballistica.PROJECTILE).collisionPos != c.pos) continue;
				targets.add(c);
				if (targets.size() >= 2) break;
			}
			if (!targets.isEmpty() && item != null){
				AugFx.notify(St.U_SPLIT);
				missile(item, targets, Math.max(1, Math.round(damage * 0.5f * pw(St.U_SPLIT))));
			}
		}

		//shadow twin: the same blow again
		if (has(St.L_TWIN) && damage > 0){
			final int dmg = damage;
			later(new Runnable() {
				@Override
				public void run() {
					if (!enemy.isAlive()) return;
					AugFx.notify(St.L_TWIN);
					if (enemy.sprite != null && enemy.sprite.visible){
						enemy.sprite.centerEmitter().burst(ShadowParticle.UP, 8);
						Wound.hit(enemy);
						enemy.sprite.showStatus(0x9080C0, "그림자");
					}
					Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 0.6f, 0.75f);
					enemy.damage(dmg, TWIN);
				}
			});
		}

		//ghost replay remembers the blow
		if (has(St.L_REPLAY) && damage > 0){
			t.replay.add(new float[]{enemy.id(), damage, Actor.now()});
			while (t.replay.size() > 12) t.replay.remove(0);
		}

		//the giant's blows shake the ground
		if (melee && AugQuest.stage(t, "거인화") >= AugQuest.GIANT_DONE) GameScene.shake(0.8f, 0.12f);
	}

	private static void gale( Hero h, Char enemy, int dmg ){
		if (h.sprite == null) return;
		Ballistica line = new Ballistica(h.pos, enemy.pos, Ballistica.STOP_SOLID);
		int start = line.path.indexOf(enemy.pos);
		if (start < 0) return;
		boolean any = false;
		for (int i = start + 1; i < line.path.size() && i <= start + 4; i++){
			int c = line.path.get(i);
			if (Dungeon.level.solid[c]) break;
			if (seen(c)) CellEmitter.get(c).burst(WindParticle.FACTORY, 3);
			Char ch = Actor.findChar(c);
			if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
				any = true;
				if (ch.sprite != null && ch.sprite.visible) Wound.hit(ch);
				ch.damage(dmg, GALE);
			}
		}
		if (seen(enemy.pos)){
			CellEmitter.get(enemy.pos).burst(WindParticle.FACTORY, 4);
			Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 0.5f, 1.5f);
		}
		if (any) AugFx.notify(St.U_GALE);
	}

	// ---------------------------------------------------------------- chains of souls

	private static Char chained( AugTracker t, int i ){
		int id = i == 0 ? t.chainA : t.chainB;
		if (id < 0) return null;
		Actor a = Actor.findById(id);
		return a instanceof Char ? (Char) a : null;
	}

	private static void linkSouls( Hero h, AugTracker t, Char enemy ){
		Char best = null;
		int bd = Integer.MAX_VALUE;
		for (Char c : enemiesNear(enemy.pos, 5)){
			if (c == enemy || !seen(c.pos)) continue;
			int d = Dungeon.level.distance(enemy.pos, c.pos);
			if (d < bd){ bd = d; best = c; }
		}
		if (best == null) return;
		t.chainA = enemy.id();
		t.chainB = best.id();
		AugFx.notify(St.U_SOULCHAIN);
		chainFx(enemy, best);
		if (best.sprite != null && best.sprite.visible) best.sprite.showStatus(0xC0A0FF, "사슬");
	}

	private static void chainFx( Char a, Char b ){
		Hero h = Dungeon.hero;
		if (a.sprite == null || b.sprite == null || h == null || h.sprite == null || h.sprite.parent == null) return;
		if (!a.sprite.visible && !b.sprite.visible) return;
		//a ghostly chain runs from one to the other
		h.sprite.parent.add(new com.shatteredpixel.shatteredpixeldungeon.effects.Chains(a.sprite.center(), b.sprite.center(),
				com.shatteredpixel.shatteredpixeldungeon.effects.Effects.Type.ETHEREAL_CHAIN, null));
		a.sprite.centerEmitter().burst(ShadowParticle.UP, 4);
		b.sprite.centerEmitter().burst(ShadowParticle.UP, 4);
		Sample.INSTANCE.play(Assets.Sounds.CHARMS, 0.4f, 0.8f);
	}

	//an enemy is taking damage: half of it goes down the chain of souls
	public static void shared( final Char ch, int dmg, Object src ){
		if (src == CHAIN || dmg <= 0) return;
		AugTracker t = t();
		if (t == null || !has(St.U_SOULCHAIN) || (t.chainA < 0 && t.chainB < 0)) return;
		int other;
		if (ch.id() == t.chainA) other = t.chainB;
		else if (ch.id() == t.chainB) other = t.chainA;
		else return;
		Actor a = Actor.findById(other);
		if (!(a instanceof Char)) return;
		final Char partner = (Char) a;
		if (!partner.isAlive() || partner == ch) return;
		final int half = Math.max(1, Math.round(dmg * 0.5f * pw(St.U_SOULCHAIN)));
		later(new Runnable() {
			@Override
			public void run() {
				if (!partner.isAlive()) return;
				chainFx(ch, partner);
				partner.damage(half, CHAIN);
			}
		});
	}

	// ---------------------------------------------------------------- the ritual circle

	//which side of the enemy a cell is on: 1 above, 2 below, 4 left, 8 right, 0 if not straight beside it
	private static int side( int enemyPos, int cell ){
		int w = Dungeon.level.width();
		int d = cell - enemyPos;
		if (d == -w) return 1;
		if (d == w) return 2;
		if (d == -1) return 4;
		if (d == 1) return 8;
		return 0;
	}

	private static void ritualSide( Hero h, AugTracker t, Char enemy, int cell ){
		int bit = side(enemy.pos, cell);
		if (bit == 0 || (t.ritualMask & bit) != 0) return;
		t.ritualMask |= bit;
		int n = Integer.bitCount(t.ritualMask);
		if (enemy.sprite != null && enemy.sprite.visible){
			enemy.sprite.showStatus(0xB080FF, "의식 " + n + "/4");
			CellEmitter.get(cell).burst(Speck.factory(Speck.LIGHT), 3);
		}
		Sample.INSTANCE.play(Assets.Sounds.CHARMS, 0.5f, 0.8f + n * 0.12f);
		if (t.ritualMask == 15){
			t.ritualMask = 0;
			t.ritualId = -1;
			completeRitual(h, enemy);
		}
	}

	private static void completeRitual( final Hero h, final Char enemy ){
		later(new Runnable() {
			@Override
			public void run() {
				if (!enemy.isAlive()) return;
				AugFx.notify(St.U_RITUAL);
				AugMoments.bigMoment();
				int w = Dungeon.level.width();
				for (int n : new int[]{-w, w, -1, 1}){
					int c = enemy.pos + n;
					if (c >= 0 && c < Dungeon.level.length() && seen(c)){
						CellEmitter.get(c).burst(ShadowParticle.CURSE, 8);
						CellEmitter.get(c).burst(Speck.factory(Speck.LIGHT), 4);
					}
				}
				if (enemy.sprite != null){
					AugFx.flare(enemy.sprite, 8,36, 0x9040FF, 1.4f);
					enemy.sprite.showStatus(0xC080FF, "의식 완성");
				}
				Atmosphere.burstCell(enemy.pos, 5f, 1f, 0x9050FF, 1f);
				PostFX.flash(0x8040E0, 0.18f);
				GameScene.shake(1.5f, 0.25f);
				Sample.INSTANCE.play(Assets.Sounds.CURSED, 1f, 0.7f);
				Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.6f, 0.7f);
				if (boss(enemy)){
					enemy.damage(Math.max(1, Math.round(enemy.HT * 0.2f * pw(St.U_RITUAL))), RITUAL);
				} else {
					enemy.damage(enemy.HP + enemy.shielding() + 1, RITUAL);
				}
				GLog.h("의식이 완성되었습니다!");
			}
		});
	}

	// ================================================================ defending

	public static void onDefense( final Hero h, final Char enemy, int damage, boolean adjacent ){
		AugTracker t = t();
		if (t == null || !adjacent || !enemy.isAlive()) return;
		//the mountain answers every blow while it stands still
		if (has(St.L_MOUNTAIN) && standingStill(t)){
			later(new Runnable() {
				@Override
				public void run() {
					if (!enemy.isAlive() || !h.isAlive()) return;
					AugFx.notify(St.L_MOUNTAIN);
					int dmg = Math.max(1, h.damageRoll());
					if (enemy.sprite != null){
						Wound.hit(enemy);
						enemy.sprite.showStatus(0xE8D8A0, "반격");
					}
					Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1f, 0.9f);
					enemy.damage(dmg, MOUNTAIN);
				}
			});
		}
	}

	static boolean standingStill( AugTracker t ){
		//a full turn spent without moving: right after a step the hero isn't standing still yet
		return Actor.now() - t.lastMoveAt > 1.01f;
	}

	//knockback can't move a mountain
	public static boolean immovable( Char ch ){
		AugTracker t = t();
		return ch == Dungeon.hero && t != null && has(St.L_MOUNTAIN) && standingStill(t);
	}

	private static void shockwave( final Hero h ){
		final ArrayList<Char> near = enemiesNear(h.pos, 1);
		if (near.isEmpty()) return;
		AugFx.notify(St.L_MOUNTAIN);
		later(new Runnable() {
			@Override
			public void run() {
				if (h.sprite != null){
					AugFx.flare(h.sprite, 7,30, 0xE8D8A0, 1f);
					for (int n : PathFinder.NEIGHBOURS9){
						int c = h.pos + n;
						if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c]) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 3);
					}
				}
				GameScene.shake(2f, 0.3f);
				Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.8f, 0.6f);
				for (Char ch : near){
					if (!ch.isAlive()) continue;
					if (!Char.hasProp(ch, Char.Property.IMMOVABLE)){
						Ballistica traj = new Ballistica(h.pos, ch.pos, Ballistica.STOP_TARGET);
						traj = new Ballistica(traj.collisionPos, traj.path.get(Math.min(traj.dist + 2, traj.path.size() - 1)), Ballistica.PROJECTILE);
						WandOfBlastWave.throwChar(ch, traj, 2, false, true, h);
					}
					if (ch.isAlive()) Buff.prolong(ch, Paralysis.class, 1f);
				}
			}
		});
	}

	// ================================================================ moving and resting

	public static void onHeroMove( int from, int to ){
		AugTracker t = t();
		Hero h = Dungeon.hero;
		if (t == null || h == null) return;
		t.stillTurns = 0;

		//the ritual: going round the enemy
		if (has(St.U_RITUAL)){
			Actor a = t.ritualId < 0 ? null : Actor.findById(t.ritualId);
			Char cur = a instanceof Char && ((Char) a).isAlive() ? (Char) a : null;
			if (cur != null && Dungeon.level.adjacent(to, cur.pos)){
				ritualSide(h, t, cur, to);
			} else {
				t.ritualId = -1;
				t.ritualMask = 0;
				for (int n : PathFinder.NEIGHBOURS4){
					Char c = Actor.findChar(to + n);
					if (c != null && c.alignment == Char.Alignment.ENEMY && c.isAlive()){
						t.ritualId = c.id();
						//the side just left counts too
						if (side(c.pos, from) != 0) t.ritualMask |= side(c.pos, from);
						ritualSide(h, t, c, to);
						break;
					}
				}
			}
		}

		//the ice dancer freezes what it passes
		if (has(St.L_ICEDANCER)){
			for (int n : PathFinder.NEIGHBOURS8){
				Char c = Actor.findChar(to + n);
				if (c == null || c.alignment != Char.Alignment.ENEMY || !c.isAlive()) continue;
				if (c.buff(Chill.class) != null && c.buff(Frost.class) == null && !c.isImmune(Frost.class)){
					Buff.affect(c, Frost.class, 2f);
					AugX.coldApplied();
					AugFx.notify(St.L_ICEDANCER);
					if (c.sprite != null && c.sprite.visible) c.sprite.centerEmitter().burst(SnowParticle.FACTORY, 6);
				}
			}
		}

		AugQuest.onHeroMove(h, t, to);
	}

	public static void onRest( Hero h ){
		AugTracker t = t();
		if (t == null || !has(St.U_CALM) || t.calm) return;
		t.calm = true;
		AugFx.notify(St.U_CALM);
		if (h.sprite != null){
			h.sprite.showStatus(0xC8E0FF, "폭풍 전야");
			h.sprite.centerEmitter().burst(SparkParticle.STATIC, 6);
		}
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.4f, 1.4f);
	}

	// ================================================================ killing

	static void onKill( final Hero h, final Mob m, final AugTracker t, AugX.Snap s ){
		final int pos = m.pos;

		if (has(St.U_FINISHER) && m.id() == t.finisherId){
			//the last one standing falls: a short pause on the blow
			t.finisherId = -1;
			Ambience.slowMotion(0.6f);
			PostFX.flash(0xFFF0D0, 0.12f);
			if (m.sprite != null && m.sprite.visible){
				m.sprite.showStatus(0xFFE0A0, "최후의 일격");
				AugFx.flare(m.sprite, 6,28, 0xFFE0A0, 0.8f);
			}
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 0.9f, 0.7f);
		}

		//borrowed soul
		if (has(St.U_SOULSTEAL)){
			String key = soulKey(m);
			if (!key.equals(t.soulKey) || t.soulFloor != AugTracker.floorKey()){
				t.soulKey = key;
				t.soulName = m.name();
				t.soulFloor = AugTracker.floorKey();
				t.recalc();
				h.updateHT(false);
				AugFx.notify(St.U_SOULSTEAL);
				if (h.sprite != null){
					h.sprite.showStatus(0xE070A0, "영혼: " + m.name());
					h.sprite.emitter().burst(ShadowParticle.UP, 8);
				}
				if (m.sprite != null) m.sprite.centerEmitter().burst(ShadowParticle.CURSE, 6);
				GLog.p("영혼 착취: " + m.name() + "의 힘 (" + soulTitle(key) + ")");
				Buff.affect(h, Soul.class);
			}
		}

		//the melody of the dead
		if (has(St.U_MELODY)){
			String kind = m.getClass().getSimpleName();
			if (t.melody.contains(kind)) t.melody.clear();
			t.melody.add(kind);
			int n = t.melody.size();
			float[] pitch = {1f, 1.26f, 1.5f};
			Sample.INSTANCE.play(Assets.Sounds.AUG_SILVER, 0.6f, pitch[Math.min(2, n - 1)]);
			if (h.sprite != null) h.sprite.emitter().burst(Speck.factory(Speck.NOTE), n * 2);
			if (n >= 3){
				t.melody.clear();
				later(new Runnable() {
					@Override
					public void run() {
						melody(h);
					}
				});
			}
		}

		//a poison flower where it fell
		if (has(St.L_GARDEN) && (s.byPoison || s.poisoned) && Dungeon.level.passable[pos]){
			plantFlower(t, pos);
		}

		AugQuest.onKill(h, t, m, s);
	}

	private static void melody( Hero h ){
		AugFx.notify(St.U_MELODY);
		AugMoments.bigMoment();
		Sample.INSTANCE.play(Assets.Sounds.AUG_SILVER, 0.8f, 1f);
		Sample.INSTANCE.play(Assets.Sounds.AUG_SILVER, 0.8f, 1.26f);
		Sample.INSTANCE.play(Assets.Sounds.AUG_SILVER, 0.8f, 1.5f);
		Sample.INSTANCE.play(Assets.Sounds.AUG_GOLD, 0.7f, 0.9f);
		int dmg = Math.round((8 + depth() * 3) * pw(St.U_MELODY));
		if (h.sprite != null){
			h.sprite.showStatus(0xF0D0FF, "죽음의 선율");
			AugFx.flare(h.sprite, 8,40, 0xD0A0FF, 1.4f);
			h.sprite.emitter().burst(Speck.factory(Speck.NOTE), 16);
			PointF c = h.sprite.center();
			Atmosphere.burst(c.x, c.y, 6f, 0.9f, 0xD0A0FF, 1.2f);
		}
		PostFX.flash(0xD0B0FF, 0.12f);
		for (Char ch : enemiesNear(h.pos, 3)){
			if (!seen(ch.pos)) continue;
			if (ch.sprite != null) ch.sprite.centerEmitter().burst(Speck.factory(Speck.NOTE), 3);
			ch.damage(dmg, MELODY);
		}
	}

	// ---------------------------------------------------------------- borrowed souls

	static String soulKey( Mob m ){
		if (m instanceof Albino) return "albino";
		if (m instanceof Rat) return "rat";
		if (m instanceof Snake) return "snake";
		if (m instanceof Gnoll) return "gnoll";
		if (m instanceof Crab) return "crab";
		if (m instanceof Swarm) return "swarm";
		if (m instanceof Slime) return "slime";
		if (m instanceof Skeleton) return "skeleton";
		if (m instanceof Thief) return "thief";
		if (m instanceof DM100) return "dm100";
		if (m instanceof Guard) return "guard";
		if (m instanceof Necromancer) return "necro";
		if (m instanceof Bat) return "bat";
		if (m instanceof Brute) return "brute";
		if (m instanceof Shaman) return "shaman";
		if (m instanceof Spinner) return "spinner";
		if (m instanceof DM200) return "dm200";
		if (m instanceof Ghoul) return "ghoul";
		if (m instanceof Elemental) return "elemental";
		if (m instanceof Warlock) return "warlock";
		if (m instanceof Monk) return "monk";
		if (m instanceof Golem) return "golem";
		if (m instanceof Succubus) return "succubus";
		if (m instanceof Eye) return "eye";
		if (m instanceof Scorpio) return "scorpio";
		if (m instanceof RipperDemon) return "ripper";
		return "other";
	}

	static EnumMap<St, Float> soulStats( String key ){
		EnumMap<St, Float> s = new EnumMap<>(St.class);
		switch (key){
			case "albino":    s.put(St.P_BLEED, 0.30f); break;
			case "rat":       s.put(St.MSPD, 0.20f); s.put(St.DMG_PCT, 0.05f); break;
			case "snake":     s.put(St.EVA_PCT, 0.30f); break;
			case "gnoll":     s.put(St.ACC_PCT, 0.25f); break;
			case "crab":      s.put(St.ARMOR, 4f); break;
			case "swarm":     s.put(St.K_HEAL, 3f); break;
			case "slime":     s.put(St.TAKEN, 0.15f); break;
			case "skeleton":  s.put(St.K_BLAST, 0.25f); break;
			case "thief":     s.put(St.GOLD, 0.60f); s.put(St.K_GOLD, 0.15f); break;
			case "dm100":     s.put(St.P_LIGHTNING, 0.20f); break;
			case "guard":     s.put(St.P_KNOCK, 0.25f); s.put(St.ARMOR, 2f); break;
			case "necro":     s.put(St.DEBUFF_DMG, 0.25f); break;
			case "bat":       s.put(St.LIFESTEAL, 0.12f); break;
			case "brute":     s.put(St.LOW_DMG, 0.40f); break;
			case "shaman":    s.put(St.P_LIGHTNING, 0.15f); s.put(St.WAND_PCT, 0.15f); break;
			case "spinner":   s.put(St.P_POISON, 0.30f); s.put(St.P_SLOW, 0.15f); break;
			case "dm200":     s.put(St.ARMOR, 6f); break;
			case "ghoul":     s.put(St.REGEN, 1.0f); break;
			case "elemental": s.put(St.P_BURN, 0.25f); break;
			case "warlock":   s.put(St.P_WEAK, 0.30f); break;
			case "monk":      s.put(St.ASPD, 0.25f); break;
			case "golem":     s.put(St.TAKEN, 0.20f); s.put(St.HP, 20f); break;
			case "succubus":  s.put(St.LIFESTEAL, 0.10f); s.put(St.P_TERROR, 0.10f); break;
			case "eye":       s.put(St.CRIT, 0.15f); break;
			case "scorpio":   s.put(St.P_CRIPPLE, 0.30f); s.put(St.THROWN_PCT, 0.30f); break;
			case "ripper":    s.put(St.MSPD, 0.30f); s.put(St.DMG_PCT, 0.15f); break;
			default:          s.put(St.DMG_PCT, 0.10f); break;
		}
		return s;
	}

	static String soulTitle( String key ){
		StringBuilder sb = new StringBuilder();
		float p = pw(St.U_SOULSTEAL);
		for (java.util.Map.Entry<St, Float> e : soulStats(key).entrySet()){
			if (sb.length() > 0) sb.append(", ");
			sb.append(e.getKey().describe(e.getValue() * p));
		}
		return sb.toString();
	}

	//the borrowed soul, shown with the other buffs
	public static class Soul extends Buff {
		{ type = buffType.POSITIVE; }
		@Override
		public boolean act() {
			AugTracker t = Augs.tracker();
			if (t == null || t.soulKey == null || t.soulFloor != AugTracker.floorKey() || !Augs.has(St.U_SOULSTEAL)){
				detach();
				if (t != null){
					t.recalc();
					if (target instanceof Hero) ((Hero) target).updateHT(false);
				}
				return true;
			}
			spend(TICK);
			return true;
		}
		@Override public int icon(){ return BuffIndicator.CORRUPT; }
		@Override public void tintIcon( Image i ){ i.hardlight(0.9f, 0.4f, 0.65f); }
		@Override public String name(){ return "영혼 착취"; }
		@Override public String desc(){
			AugTracker t = Augs.tracker();
			if (t == null || t.soulKey == null) return "";
			return "쓰러뜨린 " + t.soulName + "의 힘을 이 층이 끝날 때까지 빌려 쓰고 있습니다.\n\n" + soulTitle(t.soulKey);
		}
	}

	// ---------------------------------------------------------------- the garden of poison

	static class Flower {
		int cell;
		float until;
		Image image;
		Emitter fumes;
	}

	private static void plantFlower( AugTracker t, final int cell ){
		for (Flower f : t.flowers) if (f.cell == cell){ f.until = Actor.now() + 20f; return; }
		while (t.flowers.size() >= 6) removeFlower(t, t.flowers.get(0));
		final Flower f = new Flower();
		f.cell = cell;
		f.until = Actor.now() + 20f;
		t.flowers.add(f);
		AugFx.notify(St.L_GARDEN);
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				Hero h = Dungeon.hero;
				if (h == null || h.sprite == null || h.sprite.parent == null) return;
				//the poison flower: the sorrowmoss plant, sprouting up
				Image img = com.shatteredpixel.shatteredpixeldungeon.tiles.TerrainFeaturesTilemap.getPlantVisual(new com.shatteredpixel.shatteredpixeldungeon.plants.Sorrowmoss());
				if (img == null) return;
				img.origin.set(8, 12);
				img.point(com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.tileToWorld(cell));
				img.scale.set(0);
				img.hardlight(0.75f, 1f, 0.55f);
				h.sprite.parent.addToBack(img);
				h.sprite.parent.add(new com.watabou.noosa.tweeners.ScaleTweener(img, new PointF(1.15f, 1.15f), 0.35f));
				f.image = img;
				Emitter e = CellEmitter.get(cell);
				e.pour(PoisonParticle.MISSILE, 0.35f);
				f.fumes = e;
				CellEmitter.get(cell).burst(PoisonParticle.SPLASH, 10);
			}
		});
		Sample.INSTANCE.play(Assets.Sounds.PLANT, 0.7f, 1.1f);
	}

	private static void removeFlower( AugTracker t, final Flower f ){
		t.flowers.remove(f);
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (f.image != null) f.image.killAndErase();
				if (f.fumes != null) f.fumes.on = false;
			}
		});
	}

	static void clearFlowers( AugTracker t ){
		for (Flower f : t.flowers.toArray(new Flower[0])) removeFlower(t, f);
	}

	static boolean nearFlower( Hero h ){
		AugTracker t = t();
		if (t == null || t.flowers.isEmpty()) return false;
		for (Flower f : t.flowers) if (Dungeon.level.distance(f.cell, h.pos) <= 1) return true;
		return false;
	}

	// ================================================================ every turn

	static void everyTurn( Hero h, AugTracker t ){
		//the mountain: a shockwave every third turn standing still
		if (has(St.L_MOUNTAIN) && standingStill(t)){
			t.stillTurns++;
			if (t.stillTurns % 3 == 0) shockwave(h);
		}

		//time debt
		if (has(St.TIME_DEBT)){
			t.debtTimer++;
			if (t.debtTimer >= 20){
				t.debtTimer = 0;
				t.pendingFreeze = 3;
			}
		}

		//the trial: every enemy is a champion
		if (has(St.CHAMPIONS)){
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (m.alignment != Char.Alignment.ENEMY || !m.isAlive() || boss(m)) continue;
				//buff() matches the exact class only, the champion kinds are subclasses
				if (Char.hasProp(m, Char.Property.IMMOVABLE) || !m.buffs(ChampionEnemy.class).isEmpty()) continue;
				Class<? extends ChampionEnemy> cls;
				switch (Random.Int(6)){
					case 0: default: cls = ChampionEnemy.Blazing.class; break;
					case 1: cls = ChampionEnemy.Projecting.class; break;
					case 2: cls = ChampionEnemy.AntiMagic.class; break;
					case 3: cls = ChampionEnemy.Giant.class; break;
					case 4: cls = ChampionEnemy.Blessed.class; break;
					case 5: cls = ChampionEnemy.Growing.class; break;
				}
				Buff.affect(m, cls);
			}
		}

		//the garden
		if (!t.flowers.isEmpty()){
			for (Flower f : t.flowers.toArray(new Flower[0])){
				if (Actor.now() >= f.until){
					removeFlower(t, f);
					continue;
				}
				if (Random.Int(2) == 0){
					ArrayList<Char> near = enemiesNear(f.cell, 2);
					if (!near.isEmpty()){
						Char c = Random.element(near);
						if (!c.isImmune(Poison.class)){
							Buff.affect(c, Poison.class).set(2 + depth() / 3f);
							if (seen(c.pos)) CellEmitter.center(c.pos).burst(PoisonParticle.SPLASH, 6);
						}
					}
				}
			}
			if (nearFlower(h) && h.HP < h.HT){
				Augs.heal(h, 1 + depth() / 6);
			}
		}

		//ghost replay: the last five turns, once more
		if (has(St.L_REPLAY)){
			t.replayTimer++;
			if (t.replayTimer >= 5){
				t.replayTimer = 0;
				replay(h, t);
			}
		}

		AugQuest.everyTurn(h, t);
	}

	private static void replay( final Hero h, AugTracker t ){
		final ArrayList<float[]> todo = new ArrayList<>();
		float now = Actor.now();
		for (float[] r : t.replay) if (now - r[2] <= 5.5f) todo.add(r);
		t.replay.clear();
		if (todo.isEmpty()) return;
		AugFx.notify(St.L_REPLAY);
		paced(todo.size(), 0.12f, new Step() {
			@Override
			public boolean run( int i ) {
				float[] r = todo.get(i);
				Actor a = Actor.findById((int) r[0]);
				if (!(a instanceof Char)) return true;
				Char c = (Char) a;
				if (!c.isAlive() || !seen(c.pos)) return true;
				int dmg = Math.max(1, Math.round(r[1] * 0.7f));
				if (c.sprite != null){
					c.sprite.centerEmitter().burst(Speck.factory(Speck.LIGHT), 4);
					c.sprite.centerEmitter().burst(ShadowParticle.UP, 4);
					c.sprite.showStatus(0xA0D0FF, "재생");
					Wound.hit(c);
				}
				Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 0.5f, 0.7f);
				c.damage(dmg, REPLAY);
				return true;
			}
		});
	}

	//start of the hero's turn: timed states that must not begin in the middle of someone else's turn
	static void heroTurn( Hero h, AugTracker t ){
		if (t.pendingFreeze > 0){
			int n = t.pendingFreeze;
			t.pendingFreeze = 0;
			Buff.affect(h, TimeFrozen.class, n);
			GLog.w("빚진 시간을 갚습니다. 몸이 굳었습니다.");
			if (h.sprite != null){
				h.sprite.showStatus(0xA0A0C0, "시간 청산");
				AugFx.flare(h.sprite, 5,22, 0x8080A0, 1f);
			}
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 0.6f, 0.6f);
		}
	}

	//the hero can't act while paying back borrowed time. Unlike paralysis it doesn't stop the sprite,
	//which could leave an animation unfinished
	public static class TimeFrozen extends FlavourBuff {
		{ type = buffType.NEGATIVE; announced = true; }
		@Override
		public boolean attachTo( Char target ) {
			if (super.attachTo(target)){
				target.paralysed++;
				if (target.sprite != null) target.sprite.tint(0.6f, 0.6f, 0.8f, 0.45f);
				return true;
			}
			return false;
		}
		@Override
		public void detach() {
			super.detach();
			if (target.paralysed > 0) target.paralysed--;
			if (target.sprite != null) target.sprite.resetColor();
		}
		@Override public int icon(){ return BuffIndicator.TIME; }
		@Override public void tintIcon( Image i ){ i.hardlight(0.6f, 0.6f, 0.9f); }
		@Override public String name(){ return "빚진 시간"; }
		@Override public String desc(){ return "빌린 시간을 갚는 중이라 움직일 수 없습니다.\n\n남은 시간: " + dispTurns(); }
	}

	// ================================================================ floors

	//any change of floor, also going back to one already seen
	static void onFloorChange( AugTracker t ){
		clearFlowers(t);
		t.chainA = t.chainB = -1;
		t.ritualId = -1;
		t.ritualMask = 0;
		t.brandId = -1;
		t.replay.clear();
		t.melody.clear();
	}

	//a floor reached for the first time
	static void onNewFloor( Hero h, AugTracker t ){
		if (has(St.L_ROULETTE)) roulette(h, t);
	}

	//the blessing of this floor, -1 if none
	static int blessingNow( AugTracker t ){
		return onThisFloor(t.blessingFloor) ? t.blessing : -1;
	}

	//whether something tied to a floor still applies. While a save is loading the floor isn't known yet:
	//the hero was saved on the floor they are on, so it is taken to match (it is checked again on the first turn)
	static boolean onThisFloor( int floor ){
		return Dungeon.depth <= 0 || floor == AugTracker.floorKey();
	}

	//the roulette's multiplying blessings: 1 halves damage taken, 2 triples gold, 4 doubles speed
	static float blessingMult( int which ){
		AugTracker t = t();
		if (t == null || blessingNow(t) != which) return 1f;
		switch (which){
			case 1: return 0.5f;
			case 2: return 3f;
			case 4: return 2f;
			default: return 1f;
		}
	}

	static final String[] BLESSINGS = {"모든 공격 치명타", "받는 피해 절반", "골드 3배", "처치마다 폭발", "이동 속도 2배", "증강 하나 더"};

	private static void roulette( Hero h, AugTracker t ){
		final int result = Random.Int(BLESSINGS.length);
		t.blessing = result;
		t.blessingFloor = AugTracker.floorKey();
		t.recalc();
		AugFx.notify(St.L_ROULETTE);
		if (result == 5) t.pending++;
		GLog.h("운명의 룰렛: " + BLESSINGS[result] + "!");
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (!(Game.scene() instanceof GameScene)) return;
				Game.scene().addToFront(new Roulette(result));
			}
		});
	}

	//a strip that spins through the blessings and stops on the floor's one
	public static class Roulette extends Component {
		private com.watabou.noosa.NinePatch bg;
		private RenderedTextBlock head, body;
		private final int result;
		private float time = 0, tick = 0;
		private int shown = 0;
		private boolean landed = false;

		Roulette( int result ){
			super();
			this.result = result;
			camera = PixelScene.uiCamera;
			show(0);
			float w = 120;
			setRect((PixelScene.uiCamera.width - w) / 2f, 34, w, head.height() + 9 + 13);
		}

		@Override
		protected void createChildren() {
			bg = com.shatteredpixel.shatteredpixeldungeon.Chrome.get(com.shatteredpixel.shatteredpixeldungeon.Chrome.Type.TOAST_TR);
			add(bg);
			head = PixelScene.renderTextBlock("운명의 룰렛", 8);
			head.hardlight(0xFFD45A);
			add(head);
			body = PixelScene.renderTextBlock(9);
			add(body);
		}

		private void show( int i ){
			shown = i;
			body.text(BLESSINGS[i]);
			body.hardlight(0xFFFFFF);
			layout();
		}

		@Override
		protected void layout() {
			if (bg == null) return;
			bg.x = x; bg.y = y;
			bg.size(width, height);
			head.setPos(x + (width - head.width()) / 2f, y + 4);
			body.setPos(x + (width - body.width()) / 2f, head.bottom() + 3);
			PixelScene.align(head);
			PixelScene.align(body);
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			if (!landed){
				tick += Game.elapsed;
				//slows down as it goes
				float step = 0.06f + time * 0.08f;
				if (tick >= step){
					tick = 0;
					show((shown + 1) % BLESSINGS.length);
					Sample.INSTANCE.play(Assets.Sounds.AUG_TICK, 0.5f, 1f + 0.05f * shown);
				}
				if (time >= 1.7f){
					landed = true;
					show(result);
					body.hardlight(0xFFD45A);
					Sample.INSTANCE.play(Assets.Sounds.AUG_GOLD, 0.9f);
				}
			}
			float a = time < 4.3f ? 1f : Math.max(0f, 1f - (time - 4.3f) / 0.6f);
			bg.alpha(a * 0.9f);
			head.alpha(a);
			body.alpha(a);
			if (time > 5f) killAndErase();
		}
	}

	// ================================================================ wands

	//a wand bolt has been aimed: the reflecting missile when it ended on a wall
	public static void onWandZap( Wand w, Ballistica shot ){
		final Hero h = Dungeon.hero;
		if (!Augs.active() || h == null || !has(St.U_REFLECT) || shot == null || shot.path == null || shot.dist <= 0) return;
		if (Actor.findChar(shot.collisionPos) != null) return;
		int from = shot.collisionPos;
		if (Dungeon.level.solid[from]) from = shot.path.get(Math.max(0, shot.dist - 1));
		if (Dungeon.level.solid[from]) return;
		Char best = null;
		int bd = Integer.MAX_VALUE;
		for (Char c : enemiesNear(from, 6)){
			if (!seen(c.pos)) continue;
			if (new Ballistica(from, c.pos, Ballistica.MAGIC_BOLT).collisionPos != c.pos) continue;
			int d = Dungeon.level.distance(from, c.pos);
			if (d < bd){ bd = d; best = c; }
		}
		if (best == null) return;
		final Char target = best;
		final int start = from;
		int base = w instanceof DamageWand ? ((DamageWand) w).damageRoll() : 2 + w.buffedLvl() * 2 + depth() / 3;
		final int dmg = Math.max(1, Math.round(base * 1.3f * pw(St.U_REFLECT)));
		Actor.add(new Actor() {
			{ actPriority = VFX_PRIO; }
			@Override
			protected boolean act() {
				Actor.remove(this);
				if (h.sprite == null || h.sprite.parent == null){
					if (target.isAlive()) target.damage(dmg, REFLECT);
					return true;
				}
				final Actor self = this;
				AugFx.notify(St.U_REFLECT);
				if (seen(start)) CellEmitter.center(start).burst(SparkParticle.FACTORY, 6);
				Sample.INSTANCE.play(Assets.Sounds.ZAP, 0.6f, 1.4f);
				((MagicMissile) h.sprite.parent.recycle(MagicMissile.class)).reset(MagicMissile.MAGIC_MISSILE, start, target.pos, new Callback() {
					@Override
					public void call() {
						if (target.isAlive()){
							if (target.sprite != null) target.sprite.burst(0xFFC890FF, 6);
							target.damage(dmg, REFLECT);
						}
						self.next();
					}
				});
				return false;
			}
		});
	}

	//the vow of silence
	public static boolean wandsForbidden(){
		if (!Augs.active() || !has(St.NO_WAND)) return false;
		GLog.w("침묵의 서약: 마법 막대를 쓸 수 없습니다.");
		return true;
	}

	// ================================================================ thrown weapons

	//a thrown weapon about to land or drop: it comes back to the hand instead
	public static boolean boomerang( final MissileWeapon w, final int cell ){
		final Hero h = Dungeon.hero;
		if (!Augs.active() || h == null || !h.isAlive() || w.spawnedForEffect || !has(St.U_BOOMERANG)) return false;
		if (!w.collect()){
			Dungeon.level.drop(w, h.pos).sprite.drop();
		}
		if (h.sprite != null && h.sprite.parent != null){
			((MissileSprite) h.sprite.parent.recycle(MissileSprite.class)).reset(cell, h.sprite, w, null);
		}
		Sample.INSTANCE.play(Assets.Sounds.ITEM, 0.5f, 1.4f);
		AugFx.notify(St.U_BOOMERANG);
		return true;
	}

	// ================================================================ legends

	//lightning dash after a run of three
	static void dash( final Hero h, final Char enemy, final int charge ){
		later(new Runnable() {
			@Override
			public void run() {
				if (!h.isAlive() || h.sprite == null) return;
				Ballistica line = new Ballistica(h.pos, enemy.pos, Ballistica.STOP_SOLID);
				ArrayList<Char> hit = new ArrayList<>();
				int land = -1;
				for (int i = 1; i < line.path.size() && i <= 6; i++){
					int c = line.path.get(i);
					if (Dungeon.level.solid[c]) break;
					Char ch = Actor.findChar(c);
					if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()) hit.add(ch);
					else if (ch == null && Dungeon.level.passable[c] && !Dungeon.level.avoid[c]) land = c;
				}
				AugFx.notify(St.L_DASH);
				AugMoments.bigMoment();
				int dmg = Math.round(6 + depth() * 1.5f + charge * 3);
				ArrayList<Lightning.Arc> arcs = new ArrayList<>();
				PointF last = h.sprite.center();
				for (Char ch : hit){
					if (ch.sprite == null) continue;
					arcs.add(new Lightning.Arc(last, ch.sprite.center()));
					last = ch.sprite.center();
				}
				int start = h.pos;
				if (land >= 0){
					com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation.appear(h, land);
					arcs.add(new Lightning.Arc(last, h.sprite.center()));
				}
				if (!arcs.isEmpty() && h.sprite.parent != null) h.sprite.parent.addToFront(new Lightning(arcs, null));
				Atmosphere.burstCell(start, 3f, 0.9f, 0xC8E0FF, 0.6f);
				PostFX.flash(0xD8E8FF, 0.12f);
				Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 1f, 1.2f);
				boolean killed = false;
				for (Char ch : hit){
					if (!ch.isAlive()) continue;
					if (ch.sprite != null) ch.sprite.centerEmitter().burst(SparkParticle.FACTORY, 8);
					ch.damage(dmg, DASH);
					if (!ch.isAlive()) killed = true;
				}
				AugTracker t = t();
				if (killed && t != null){
					t.gustReady = 1;
					t.moveStreak = 3;
					if (h.sprite != null) h.sprite.showStatus(0xC8E0FF, "다시 돌파!");
				}
				Dungeon.observe();
				GameScene.updateFog();
			}
		});
	}

	//the ice dancer: shards of a shattered enemy freeze the ones around, and frozen ones shatter in turn
	static void iceShards( final int cell, final int gen ){
		if (!has(St.L_ICEDANCER) || gen > 2) return;
		later(new Runnable() {
			@Override
			public void run() {
				Hero h = Dungeon.hero;
				int dmg = 2 + depth() / 2;
				if (seen(cell)){
					CellEmitter.center(cell).burst(SnowParticle.FACTORY, 12);
					Splash.at(cell, 0xFFD8F0FF, 10);
				}
				for (Char c : enemiesNear(cell, 2)){
					if (c.pos == cell) continue;
					if (c.buff(Frost.class) != null){
						if (c.sprite != null && c.sprite.visible) c.sprite.showStatus(0xC8ECFF, "파쇄!");
						Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.6f, 1.3f);
						c.damage(dmg * 2, SHARD);
						iceShards(c.pos, gen + 1);
					} else if (!c.isImmune(Frost.class)){
						Buff.affect(c, Frost.class, 2f);
						AugX.coldApplied();
						c.damage(dmg, SHARD);
					}
				}
				if (h != null && h.isAlive()) Augs.heal(h, 2 + depth() / 3);
			}
		});
	}

	// ---------------------------------------------------------------- the devourer

	public static void devour( AugTracker t, String name ){
		Aug d = Aug.get(name);
		Hero h = Dungeon.hero;
		if (d == null || h == null) return;
		t.devouredStars = Math.max(1, t.stars(name));
		t.devoured = name;
		while (t.owned.remove(name));
		t.stars.remove(name);
		t.recalc();
		h.updateHT(false);
		GLog.h("포식자: " + name + "을(를) 삼켰습니다. 효과가 2배가 됩니다!");
		if (h.sprite != null){
			h.sprite.showStatus(0xFF7050, "포식!");
			AugFx.flare(h.sprite, 8,36, 0xD03020, 1.6f);
			h.sprite.emitter().burst(ShadowParticle.CURSE, 14);
			PointF c = h.sprite.center();
			Atmosphere.burst(c.x, c.y, 6f, 0.9f, 0xE04030, 1.6f);
		}
		PostFX.flash(0xC03020, 0.16f);
		Sample.INSTANCE.play(Assets.Sounds.EAT, 1f, 0.6f);
		Sample.INSTANCE.play(Assets.Sounds.CURSED, 0.6f, 0.8f);
		com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.refreshHero();
	}

	static boolean canDevour( Aug a ){
		return a != null && a.tier != Aug.LEGEND && !a.repeatable;
	}

	//choose the augment to devour. It can't be closed without choosing
	public static class WndDevour extends Window {
		private final ArrayList<Component> rows = new ArrayList<>();
		private final ArrayList<String> names = new ArrayList<>();

		public WndDevour(){
			final int w = Math.min(140, (int) PixelScene.uiCamera.width - 16);
			final int maxH = (int) (PixelScene.uiCamera.height * 0.7f);
			final AugTracker t = Augs.tracker();
			RenderedTextBlock title = PixelScene.renderTextBlock("포식자", 9);
			title.hardlight(0xFF8050);
			title.setPos((w - title.width()) / 2f, 0);
			add(title);
			RenderedTextBlock help = PixelScene.renderTextBlock("삼킬 증강을 고르세요. 그 증강은 사라지고, 효과가 2배로 흡수됩니다.", 6);
			help.maxWidth(w);
			help.hardlight(0xB0B0B0);
			help.setPos(0, title.bottom() + 3);
			add(help);
			float top = help.bottom() + 4;

			Component content = new Component();
			float pos = 0;
			if (t != null){
				java.util.LinkedHashSet<String> seenNames = new java.util.LinkedHashSet<>(t.owned);
				for (String n : seenNames){
					final Aug a = Aug.get(n);
					if (!canDevour(a)) continue;
					Component row = new Component();
					ColorBlock bg = new ColorBlock(1, 1, 0x22FFFFFF);
					row.add(bg);
					int st = t.stars(n);
					RenderedTextBlock label = PixelScene.renderTextBlock(n + (st > 1 ? "  " + Aug.stars(st) : ""), 7);
					label.hardlight(a.nameColor());
					label.maxWidth(w - 4);
					row.add(label);
					RenderedTextBlock fam = PixelScene.renderTextBlock(Aug.TIER_NAMES[a.tier] + (a.set == null ? "" : " · " + a.set.title), 6);
					fam.hardlight(a.set == null ? 0x999999 : a.set.color);
					row.add(fam);
					float rh = label.height() + 5;
					row.setRect(0, pos, w, rh);
					bg.x = 0; bg.y = pos; bg.size(w, rh);
					label.setPos(2, pos + 2.5f);
					fam.setPos(w - fam.width() - 2, pos + 3.5f);
					PixelScene.align(label);
					PixelScene.align(fam);
					content.add(row);
					rows.add(row);
					names.add(n);
					pos += rh + 2;
				}
			}
			content.setSize(w, pos);
			float listH = Math.min(pos, maxH - top);
			resize(w, (int) Math.ceil(top + listH));

			//the scroll pane is placed once the window has its size, as its camera follows the window
			ScrollPane sp = new ScrollPane(content){
				@Override
				public void onClick( float x, float y ) {
					for (int i = 0; i < rows.size(); i++){
						if (rows.get(i).inside(x, y)){
							if (t != null) devour(t, names.get(i));
							hide();
							return;
						}
					}
				}
			};
			add(sp);
			sp.setRect(0, top, w, listH);
		}

		@Override
		public void onBackPressed() {
			//a choice has to be made
		}
	}

	// ================================================================ stats added through the tracker

	static void addStats( AugTracker t, EnumMap<St, Float> into ){
		//borrowed soul
		if (t.soulKey != null && onThisFloor(t.soulFloor) && t.total[St.U_SOULSTEAL.ordinal()] > 0){
			float p = 1f;
			Aug a = Aug.get("영혼 착취");
			if (a != null) p = t.power(a);
			for (java.util.Map.Entry<St, Float> e : soulStats(t.soulKey).entrySet()) put(into, e.getKey(), e.getValue() * p);
		}
		//the floor's blessing
		//halved damage, triple gold and double speed are applied on top of everything else, see blessingMult
		if (blessingNow(t) == 3) put(into, St.K_BLAST, 1f);
		AugQuest.addStats(t, into);
	}

	private static void put( EnumMap<St, Float> into, St s, float v ){
		Float old = into.get(s);
		into.put(s, (old == null ? 0f : old) + v);
	}
}
