/*
 * The skills of the Augment II challenge.
 *
 * A skill checks its target, plays out, and calls Ctx.finish exactly once when it is over.
 * Damage is always a share of what the hero's weapon would deal, through the normal attack
 * code, so armour, enchantments and everything else apply as they do to a regular hit.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments2;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Chains;
import com.shatteredpixel.shatteredpixeldungeon.effects.Effects;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Javelin;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Shuriken;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Door;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public abstract class Skill {

	//how a skill is aimed
	public static final int SELF = 0, ENEMY = 1, CELL = 2;
	//what a skill does, passives look at these
	public static final int AREA = 1, CONTROL = 2, MOVE = 4, DAMAGE = 8;

	public final String id, name;
	public final int cd, icon, aim, tags;
	public final boolean ult;
	//a free skill costs no turn
	public boolean free = false;

	private static final LinkedHashMap<String, Skill> ALL = new LinkedHashMap<>();

	Skill( String id, String name, int cd, int icon, int aim, int tags, boolean ult ){
		this.id = id; this.name = name; this.cd = cd; this.icon = icon; this.aim = aim; this.tags = tags; this.ult = ult;
		ALL.put(id, this);
	}

	public static Skill get( String id ){
		return ALL.get(id);
	}

	public static ArrayList<Skill> all( boolean ults ){
		ArrayList<Skill> list = new ArrayList<>();
		for (Skill s : ALL.values()) if (s.ult == ults) list.add(s);
		return list;
	}

	public boolean is( int tag ){
		return (tags & tag) != 0;
	}

	//how far it can be aimed, 0 for skills centred on the hero
	public int range( Tracker2 t ){
		return 0;
	}

	//what the card and the info window say; k scales the damage figures
	public abstract String desc( Tracker2 t, float k );

	//returns false when there is nothing to do, in which case no cooldown starts
	public abstract boolean cast( Ctx c );

	static String pct( int base, float k ){
		return "_" + Math.round(base * k) + "%_";
	}

	static boolean fail( String why ){
		GLog.w(why);
		return false;
	}

	// ================================================================ one use of a skill

	public static class Ctx {
		public final Hero h;
		public final Tracker2 t;
		public final int slot;
		public final Skill skill;
		public int cell;
		//damage scale: upgrades, passives, and repeats at reduced strength
		public float power;
		//a repeat started by a passive: starts no cooldown and takes no time
		public boolean echo = false;

		public int dealt = 0, kills = 0;
		public final ArrayList<Integer> struck = new ArrayList<>();

		Ctx( Hero h, Tracker2 t, int slot, int cell, float power ){
			this.h = h; this.t = t; this.slot = slot; this.skill = t.skill(slot); this.cell = cell; this.power = power;
		}

		public void begin(){
			if (!echo) h.busy();
		}

		public int hit( Char e, int pct ){
			if (e == null || !e.isAlive()) return 0;
			int d = Aug2.strike(h, e, pct / 100f * power);
			dealt += d;
			struck.add(e.pos);
			if (!e.isAlive()) kills++;
			return d;
		}

		public void finish(){
			Aug2.finishCast(this);
		}

		//for animations that only have to end before the skill does
		public Callback done(){
			return new Callback() {
				@Override public void call() {
					h.sprite.idle();
					finish();
				}
			};
		}
	}

	// ================================================================ basic skills

	static final Skill HOOK = new Skill("hook", "갈고리", 12, ItemSpriteSheet.ARTIFACT_CHAINS, CELL, CONTROL | DAMAGE, false){
		@Override public int range( Tracker2 t ){ return 6 + t.reach(); }
		@Override public String desc( Tracker2 t, float k ){
			return "직선 " + range(t) + "칸 안의 첫 적을 눈앞으로 끌어와 무기 피해의 " + pct(80, k) + "를 주고 _1턴 기절_시킵니다.";
		}
		@Override public boolean cast( final Ctx c ){
			final Hero h = c.h;
			final Ballistica b = new Ballistica(h.pos, c.cell, Ballistica.PROJECTILE);
			final Char e = Actor.findChar(b.collisionPos);
			if (!Aug2.foe(e) || b.dist > range(c.t)) return fail("갈고리가 닿는 곳에 적이 없습니다.");
			c.begin();
			//the cell right in front of the hero, if the enemy can be dragged there
			int to = e.pos;
			if (b.dist > 1 && !Char.hasProp(e, Char.Property.IMMOVABLE)){
				int front = b.path.get(1);
				if (Dungeon.level.passable[front] && Actor.findChar(front) == null) to = front;
			}
			final int dest = to;
			h.sprite.zap(e.pos, Fx2.idle(h.sprite));
			Sample.INSTANCE.play(Assets.Sounds.CHAINS);
			h.sprite.parent.add(new Chains(h.sprite.center(), e.sprite.center(), Effects.Type.CHAIN, new Callback() {
				@Override public void call() {
					final Callback land = new Callback() {
						@Override public void call() {
							if (dest != e.pos){
								e.pos = dest;
								Dungeon.level.occupyCell(e);
								Dungeon.observe();
								GameScene.updateFog();
							}
							c.hit(e, 80);
							if (e.isAlive()) Buff.prolong(e, Paralysis.class, 1f);
							c.finish();
						}
					};
					if (dest != e.pos){
						Actor.add(new Pushing(e, e.pos, dest, land));
						h.next();
					} else {
						land.call();
					}
				}
			}));
			return true;
		}
	};

	static final Skill DASH = new Skill("dash", "돌진 베기", 10, ItemSpriteSheet.RAPIER, CELL, MOVE | AREA | DAMAGE, false){
		@Override public int range( Tracker2 t ){ return 4 + t.reach(); }
		@Override public String desc( Tracker2 t, float k ){
			return "가리킨 쪽으로 " + range(t) + "칸까지 내달리며, 지나친 모든 적에게 무기 피해의 " + pct(120, k) + "를 줍니다.";
		}
		@Override public boolean cast( final Ctx c ){
			final Hero h = c.h;
			if (c.cell == h.pos) return fail("나아갈 방향을 고르세요.");
			Ballistica b = new Ballistica(h.pos, c.cell, Ballistica.STOP_SOLID);
			int len = Math.min(b.dist, range(c.t));
			final ArrayList<Char> targets = new ArrayList<>();
			int stop = 0;
			for (int i = 1; i <= len; i++){
				int p = b.path.get(i);
				Char ch = Actor.findChar(p);
				if (Aug2.foe(ch)) targets.add(ch);
				if (ch == null && Dungeon.level.passable[p] && !Dungeon.level.pit[p]) stop = i;
			}
			if (stop == 0 && targets.isEmpty()) return fail("그쪽으로는 나아갈 수 없습니다.");
			final int dest = b.path.get(stop);
			c.begin();
			final Callback land = new Callback() {
				@Override public void call() {
					if (dest != h.pos){
						if (Dungeon.level.map[h.pos] == Terrain.OPEN_DOOR) Door.leave(h.pos);
						h.pos = dest;
						Dungeon.level.occupyCell(h);
						Dungeon.observe();
						GameScene.updateFog();
					}
					Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 1f, 1.2f);
					for (Char e : targets){
						Fx2.slash(e.sprite);
						c.hit(e, 120);
					}
					c.finish();
				}
			};
			if (dest != h.pos){
				Sample.INSTANCE.play(Assets.Sounds.MISS, 1f, 0.8f);
				h.sprite.emitter().start(Speck.factory(Speck.JET), 0.01f, 4 + 2 * stop);
				h.sprite.jump(h.pos, dest, 0, 0.05f + 0.04f * stop, land);
			} else {
				h.sprite.attack(targets.get(0).pos, land);
			}
			return true;
		}
	};

	static final Skill MARK = new Skill("mark", "표식", 8, ItemSpriteSheet.ARTIFACT_TALISMAN, ENEMY, CONTROL, false){
		{ free = true; }
		@Override public int range( Tracker2 t ){ return 8 + t.reach(); }
		@Override public String desc( Tracker2 t, float k ){
			return "턴을 쓰지 않습니다. 적 하나에 표식을 새겨, 6턴 안에 받는 다음 피해를 _2배_로 만듭니다.";
		}
		@Override public boolean cast( Ctx c ){
			Char e = Actor.findChar(c.cell);
			if (!Aug2.foe(e) || !Dungeon.level.heroFOV[c.cell]) return fail("표식을 새길 적을 고르세요.");
			c.begin();
			Buff.prolong(e, Aug2.Marked.class, 6f);
			Sample.INSTANCE.play(Assets.Sounds.READ, 0.7f, 1.4f);
			Fx2.mark(e.sprite);
			c.finish();
			return true;
		}
	};

	static final Skill SPIN = new Skill("spin", "회전 베기", 9, ItemSpriteSheet.WAR_SCYTHE, SELF, AREA | DAMAGE, false){
		@Override public String desc( Tracker2 t, float k ){
			return "크게 휘둘러 주위 " + (t.reach() > 0 ? "2칸" : "8칸") + "의 모든 적에게 무기 피해의 " + pct(140, k) + "를 줍니다.";
		}
		@Override public boolean cast( final Ctx c ){
			final Hero h = c.h;
			c.begin();
			final int r = 1 + c.t.reach();
			final Callback strike = new Callback() {
				@Override public void call() {
					Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 1f, 0.85f);
					Fx2.sweep(h.sprite, r);
					for (Char e : Aug2.foesAround(h.pos, r)){
						c.hit(e, 140);
					}
					c.finish();
				}
			};
			//swing towards a neighbour if there is one, so the hero does not always face the same way
			ArrayList<Char> near = Aug2.foesAround(h.pos, r);
			h.sprite.attack(near.isEmpty() ? h.pos + (h.sprite.flipHorizontal ? -1 : 1) : near.get(0).pos, strike);
			return true;
		}
	};

	static final Skill BASH = new Skill("bash", "밀어치기", 11, ItemSpriteSheet.WAR_HAMMER, ENEMY, CONTROL | DAMAGE, false){
		@Override public int range( Tracker2 t ){ return 1; }
		@Override public String desc( Tracker2 t, float k ){
			return "곁의 적에게 무기 피해의 " + pct(150, k) + "를 주고 " + (3 + t.reach()) + "칸 밀쳐냅니다. 벽이나 다른 적에 부딪히면 부딪힌 쪽 모두 _2턴 기절_합니다.";
		}
		@Override public boolean cast( final Ctx c ){
			final Hero h = c.h;
			final Char e = Actor.findChar(c.cell);
			if (!Aug2.foe(e) || !Dungeon.level.adjacent(h.pos, e.pos)) return fail("바로 곁의 적을 고르세요.");
			c.begin();
			h.sprite.attack(e.pos, new Callback() {
				@Override public void call() {
					Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
					c.hit(e, 150);
					if (e.isAlive() && !Char.hasProp(e, Char.Property.IMMOVABLE)){
						int power = 3 + c.t.reach();
						Ballistica aim = new Ballistica(h.pos, e.pos, Ballistica.STOP_TARGET);
						Ballistica traj = new Ballistica(e.pos, aim.path.get(aim.path.size() - 1), Ballistica.PROJECTILE);
						Char behind = Actor.findChar(traj.collisionPos);
						boolean crash = traj.dist < power || (behind != null && behind != e);
						if (crash){
							Buff.prolong(e, Paralysis.class, 2f);
							if (behind != null && behind != e && behind != h) Buff.prolong(behind, Paralysis.class, 2f);
						}
						WandOfBlastWave.BlastWave.blast(e.pos);
						WandOfBlastWave.throwChar(e, traj, power, false, false, h);
					}
					c.finish();
				}
			});
			return true;
		}
	};

	static final Skill GUARD = new Skill("guard", "방패 올리기", 14, ItemSpriteSheet.GREATSHIELD, SELF, 0, false){
		@Override public String desc( Tracker2 t, float k ){
			return "2턴 동안 받는 피해가 _80%_ 줄어듭니다. 막아낸 피해는 모아 두었다가 다음 공격에 그대로 얹습니다.";
		}
		@Override public boolean cast( Ctx c ){
			c.begin();
			Buff.prolong(c.h, Aug2.Guarding.class, 2f);
			Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1f, 0.8f);
			Fx2.shield(c.h.sprite);
			c.h.sprite.operate(c.h.pos, c.done());
			return true;
		}
	};

	static final Skill BLINK = new Skill("blink", "점멸", 16, ItemSpriteSheet.STONE_BLINK, CELL, MOVE, false){
		{ free = true; }
		@Override public int range( Tracker2 t ){ return 5 + t.reach(); }
		@Override public String desc( Tracker2 t, float k ){
			return "턴을 쓰지 않습니다. 보이는 " + range(t) + "칸 안의 빈자리로 곧바로 옮겨 갑니다.";
		}
		@Override public boolean cast( Ctx c ){
			Hero h = c.h;
			int p = c.cell;
			if (p == h.pos || !Dungeon.level.heroFOV[p] || !Dungeon.level.passable[p] || Dungeon.level.pit[p]
					|| Actor.findChar(p) != null || Dungeon.level.distance(h.pos, p) > range(c.t)){
				return fail("그곳으로는 옮겨 갈 수 없습니다.");
			}
			c.begin();
			CellEmitter.get(h.pos).start(Speck.factory(Speck.LIGHT), 0.2f, 3);
			ScrollOfTeleportation.appear(h, p);
			Dungeon.observe();
			GameScene.updateFog();
			c.finish();
			return true;
		}
	};

	static final Skill PIERCE = new Skill("pierce", "관통 창", 9, ItemSpriteSheet.JAVELIN, CELL, AREA | CONTROL | DAMAGE, false){
		@Override public int range( Tracker2 t ){ return 7 + t.reach(); }
		@Override public String desc( Tracker2 t, float k ){
			return "직선 " + range(t) + "칸을 꿰뚫는 창을 던져, 맞은 모든 적에게 무기 피해의 " + pct(110, k) + "를 주고 _2턴 둔화_시킵니다.";
		}
		@Override public boolean cast( final Ctx c ){
			final Hero h = c.h;
			if (c.cell == h.pos) return fail("창을 던질 방향을 고르세요.");
			final Ballistica b = new Ballistica(h.pos, c.cell, Ballistica.STOP_SOLID);
			final int len = Math.min(b.dist, range(c.t));
			if (len < 1) return fail("그쪽으로는 던질 수 없습니다.");
			c.begin();
			final int end = b.path.get(len);
			h.sprite.zap(end, new Callback() {
				@Override public void call() {
					Sample.INSTANCE.play(Assets.Sounds.ATK_SPIRITBOW, 1f, 0.8f);
					((MissileSprite)h.sprite.parent.recycle(MissileSprite.class)).reset(h.sprite, end, new Javelin(), new Callback() {
						@Override public void call() {
							for (int i = 1; i <= len; i++){
								Char e = Actor.findChar(b.path.get(i));
								if (!Aug2.foe(e)) continue;
								c.hit(e, 110);
								if (e.isAlive()) Buff.prolong(e, Slow.class, 2f);
							}
							c.finish();
						}
					});
				}
			});
			return true;
		}
	};

	static final Skill MINE = new Skill("mine", "지뢰", 7, ItemSpriteSheet.BOMB, CELL, AREA | CONTROL | DAMAGE, false){
		@Override public int range( Tracker2 t ){ return 1 + t.reach(); }
		@Override public String desc( Tracker2 t, float k ){
			return "곁의 빈자리에 지뢰를 묻습니다(층마다 3개까지). 적이 밟으면 주위 " + (t.reach() > 0 ? "2칸" : "1칸") + "에 무기 피해의 " + pct(160, k) + "를 주고 _2턴 속박_합니다.";
		}
		@Override public boolean cast( final Ctx c ){
			final Hero h = c.h;
			final int p = c.cell;
			if (p == h.pos || Dungeon.level.distance(h.pos, p) > range(c.t) || !Dungeon.level.passable[p]
					|| Dungeon.level.pit[p] || Actor.findChar(p) != null || !Dungeon.level.heroFOV[p]){
				return fail("곁의 빈자리를 고르세요.");
			}
			if (c.t.mineFloor == Tracker2.floorKey() && c.t.mines.contains(p)) return fail("이미 지뢰가 묻혀 있습니다.");
			c.begin();
			h.sprite.operate(p, new Callback() {
				@Override public void call() {
					Aug2.placeMine(c.t, p, c.power);
					Sample.INSTANCE.play(Assets.Sounds.TRAP, 0.7f, 0.8f);
					h.sprite.idle();
					c.finish();
				}
			});
			return true;
		}
	};

	static final Skill TAUNT = new Skill("taunt", "도발의 함성", 13, ItemSpriteSheet.ARTIFACT_HORN1, SELF, CONTROL, false){
		@Override public String desc( Tracker2 t, float k ){
			return "보이는 모든 적이 3턴 동안 영웅만 노립니다. 그동안 방어력이 _2배_가 됩니다.";
		}
		@Override public boolean cast( Ctx c ){
			Hero h = c.h;
			c.begin();
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (!Aug2.foe(m) || !Dungeon.level.heroFOV[m.pos]) continue;
				m.aggro(h);
				Buff.prolong(m, Aug2.Taunted.class, 3f);
				if (m.sprite != null) m.sprite.showStatus(CharSprite.WARNING, "!");
			}
			Buff.prolong(h, Aug2.Braced.class, 3f);
			Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
			h.sprite.centerEmitter().start(Speck.factory(Speck.SCREAM), 0.3f, 3);
			h.sprite.operate(h.pos, c.done());
			return true;
		}
	};

	static final Skill DRAIN = new Skill("drain", "흡수 일격", 10, ItemSpriteSheet.ARTIFACT_CHALICE2, ENEMY, DAMAGE, false){
		@Override public int range( Tracker2 t ){ return 1; }
		@Override public String desc( Tracker2 t, float k ){
			return "곁의 적에게 무기 피해의 " + pct(180, k) + "를 주고, 준 피해의 _절반_만큼 체력을 되찾습니다.";
		}
		@Override public boolean cast( final Ctx c ){
			final Hero h = c.h;
			final Char e = Actor.findChar(c.cell);
			if (!Aug2.foe(e) || !Dungeon.level.adjacent(h.pos, e.pos)) return fail("바로 곁의 적을 고르세요.");
			c.begin();
			h.sprite.attack(e.pos, new Callback() {
				@Override public void call() {
					Sample.INSTANCE.play(Assets.Sounds.HIT_STAB, 1f, 0.8f);
					int d = c.hit(e, 180);
					int heal = Math.min(h.HT - h.HP, d / 2);
					if (heal > 0 && h.isAlive()){
						h.HP += heal;
						h.sprite.emitter().burst(Speck.factory(Speck.HEALING), 3);
						h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
					}
					Fx2.drain(e.sprite, h.sprite);
					c.finish();
				}
			});
			return true;
		}
	};

	static final Skill STARS = new Skill("stars", "그림자 표창", 6, ItemSpriteSheet.SHURIKEN, SELF, AREA | DAMAGE, false){
		@Override public int range( Tracker2 t ){ return 6 + t.reach(); }
		@Override public String desc( Tracker2 t, float k ){
			return "가장 가까운 적 셋에게 표창을 던져 각각 무기 피해의 " + pct(70, k) + "를 줍니다.";
		}
		@Override public boolean cast( final Ctx c ){
			final Hero h = c.h;
			final ArrayList<Char> targets = Aug2.nearestFoes(h.pos, range(c.t), 3);
			if (targets.isEmpty()) return fail("표창이 닿는 곳에 적이 없습니다.");
			c.begin();
			final int[] left = { targets.size() };
			h.sprite.zap(targets.get(0).pos, new Callback() {
				@Override public void call() {
					Sample.INSTANCE.play(Assets.Sounds.MISS, 1f, 1.3f);
					for (final Char e : targets){
						((MissileSprite)h.sprite.parent.recycle(MissileSprite.class)).reset(h.sprite, e.pos, new Shuriken(), new Callback() {
							@Override public void call() {
								c.hit(e, 70);
								if (--left[0] == 0) c.finish();
							}
						});
					}
				}
			});
			return true;
		}
	};

}
