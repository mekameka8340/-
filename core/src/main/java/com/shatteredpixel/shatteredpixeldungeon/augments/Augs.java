/*
 * Augment challenge - runtime. The game calls into here at its hook points; everything the
 * augments do happens in this class.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Freezing;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MindVision;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.Atmosphere;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.PostFX;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.Wound;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

public class Augs {

	//damage sources, so kills and immunities can tell augment damage apart
	public static class Src {
		public final String name;
		Src( String name ){ this.name = name; }
	}
	public static final Src ECHO      = new Src("echo");
	public static final Src CLEAVE    = new Src("cleave");
	public static final Src THORNS    = new Src("thorns");
	public static final Src LIGHTNING = new Src("lightning");
	public static final Src BLAST     = new Src("blast");
	public static final Src FIRE      = new Src("fire");
	public static final Src FROST     = new Src("frost");
	public static final Src ORB       = new Src("orb");

	// ================================================================ state access

	public static boolean active(){
		return Dungeon.hero != null && Dungeon.isChallenged(Challenges.AUGMENTS);
	}

	public static AugTracker tracker(){
		Hero h = Dungeon.hero;
		if (h == null || !Dungeon.isChallenged(Challenges.AUGMENTS)) return null;
		return h.buff(AugTracker.class);
	}

	public static float st( St s ){
		AugTracker t = tracker();
		if (t == null) return 0f;
		float v = t.total[s.ordinal()];
		//war avatar: everything but health and on/off effects is 1.5x while a boss is in sight
		if (t.avatar && v != 0 && !s.flag() && !s.once() && s != St.HP && s != St.HP_PCT) v *= 1.5f;
		return v;
	}

	public static boolean has( St s ){
		return st(s) > 0f;
	}

	private static boolean roll( St s ){
		float c = st(s);
		return c > 0 && Random.Float() < c;
	}

	//a chance-based effect that shows its augment's icon when it goes off
	private static boolean proc( St s ){
		if (roll(s)){
			AugFx.notify(s);
			return true;
		}
		return false;
	}

	private static int depthScale(){
		return Math.max(1, Dungeon.depth);
	}

	// ================================================================ simple stat hooks

	public static int maxHP( int base ){
		return Math.max(1, Math.round((base + st(St.HP)) * Math.max(0.1f, 1f + st(St.HP_PCT))));
	}

	public static float accuracy( float acc ){
		return (acc + st(St.ACC)) * Math.max(0.1f, 1f + st(St.ACC_PCT));
	}

	public static float evasion( Hero h, float eva ){
		eva = (eva + st(St.EVA)) * Math.max(0.1f, 1f + st(St.EVA_PCT));
		if (has(St.GRASS_EVA) && Dungeon.level != null && h.pos >= 0 && h.pos < Dungeon.level.length()){
			int t = Dungeon.level.map[h.pos];
			if (t == Terrain.GRASS || t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS){
				eva *= 1f + st(St.GRASS_EVA);
			}
		}
		if (has(St.U_ICEDANCE) && Dungeon.level != null){
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar(h.pos + n);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY && AugX.chilled(ch)){
					eva *= 1.3f;
					break;
				}
			}
		}
		if (has(St.U_GASCLOAK) && inGas(h)) eva *= 1.4f;
		if (has(St.L_GARDEN) && AugY.nearFlower(h)) eva *= 1.3f;
		return eva;
	}

	public static int armorBonus(){
		int dr = 0;
		int a = Math.round(st(St.ARMOR));
		if (a > 0) dr += Random.NormalIntRange((a + 1) / 2, a);
		int gold = Math.round(st(St.GOLD_ARMOR));
		if (gold > 0) dr += Math.min(gold, Dungeon.gold / 300);
		return dr;
	}

	public static float attackDelay( float delay ){
		float bonus = Dungeon.hero != null ? AugX.speedBonus(Dungeon.hero) : 0f;
		return delay / Math.max(0.3f, 1f + st(St.ASPD) + bonus);
	}

	public static float moveSpeed( float speed ){
		return speed * Math.max(0.3f, 1f + st(St.MSPD)) * AugY.blessingMult(4);
	}

	public static int scaleExp( int exp ){
		float e = exp * (1f + st(St.EXP));
		int whole = (int)e;
		if (Random.Float() < e - whole) whole++;
		return whole;
	}

	public static int scaleGold( int gold ){
		return Math.round(gold * (1f + st(St.GOLD)) * AugY.blessingMult(2));
	}

	public static float lootMultiplier(){
		return 1f + st(St.LOOT);
	}

	public static int shopPrice( int price ){
		return Math.max(1, Math.round(price * (1f - Math.min(0.6f, st(St.DISCOUNT)))));
	}

	public static float hungerRate(){
		return Math.max(0.2f, 1f - st(St.HUNGER));
	}

	public static float regenMultiplier( Hero h ){
		float m = Math.max(0.1f, 1f + st(St.REGEN));
		if (has(St.WATER_REGEN) && Dungeon.level != null && h.pos >= 0 && h.pos < Dungeon.level.length()
				&& Dungeon.level.water[h.pos]){
			m *= 3f;
		}
		if (has(St.U_FULLBELLY) && AugX.notHungry(h)) m *= 1.5f;
		if (has(St.U_HIBERNATE) && h.resting) m *= 4f;
		return m;
	}

	public static int healAmount( Char target, int amount ){
		if (target != Dungeon.hero) return amount;
		return Math.round(amount * Math.max(0f, 1f + st(St.HEALAMP)));
	}

	public static void onEat( Hero h ){
		float p = st(St.EAT_HEAL);
		if (p > 0) heal(h, Math.round(h.HT * p));
	}

	public static float durability(){
		return 1f + st(St.THROW_DUR);
	}

	public static float wandChargeMultiplier( Char target ){
		return target == Dungeon.hero ? 1f + st(St.WAND_CHARGE) : 1f;
	}

	public static float artifactChargeMultiplier( Char target ){
		return target == Dungeon.hero ? 1f + st(St.ARTI_CHARGE) : 1f;
	}

	public static boolean freeWandCharge(){
		if (proc(St.WAND_FREE)){
			Hero h = Dungeon.hero;
			if (h.sprite != null) h.sprite.showStatus(0xE070E8, "마력 보존");
			return true;
		}
		return false;
	}

	public static int wandDamage( Char target, int dmg ){
		if (target == Dungeon.hero || target.alignment == Char.Alignment.ALLY) return dmg;
		return AugX.wandHit(target, Math.round(dmg * (1f + st(St.WAND_PCT))));
	}

	public static int vision(){
		return Math.round(st(St.VISION));
	}

	public static float stealth(){
		float s = st(St.STEALTH);
		Hero h = Dungeon.hero;
		if (h != null && Dungeon.level != null && h.pos >= 0 && h.pos < Dungeon.level.length()){
			if (has(St.U_WALLSHADOW) && AugX.nearWall(h.pos)) s += 2;
			if (has(St.U_GASCLOAK) && inGas(h)) s += 2;
		}
		return s;
	}

	static boolean inGas( Hero h ){
		Blob gas = Dungeon.level.blobs.get(ToxicGas.class);
		return gas != null && gas.volume > 0 && h.pos >= 0 && h.pos < gas.cur.length && gas.cur[h.pos] > 0;
	}

	public static HashSet<Class> immunities(){
		HashSet<Class> out = new HashSet<>();
		if (has(St.IMM_FIRE)){ out.add(Burning.class); out.add(Fire.class); }
		if (has(St.IMM_POISON)){ out.add(Poison.class); out.add(ToxicGas.class); out.add(Corrosion.class); out.add(CorrosiveGas.class); }
		if (has(St.IMM_COLD)){ out.add(Chill.class); out.add(Frost.class); out.add(Freezing.class); }
		if (has(St.IMM_ELEC)){ out.add(Electricity.class); }
		if (has(St.IMM_PARA)){ out.add(Paralysis.class); out.add(ParalyticGas.class); }
		return out;
	}

	public static boolean dodge(){
		if (proc(St.DODGE)){
			return true;
		}
		return false;
	}

	// ================================================================ attacking

	//all damage multipliers and the critical hit for one hero attack, before armor
	public static float modifyAttack( Hero h, Char enemy, float dmg ){
		AugTracker t = tracker();
		if (t == null) return dmg;

		float mult = 1f + st(St.DMG_PCT);
		boolean thrown = h.belongings.attackingWeapon() instanceof MissileWeapon;
		mult += thrown ? st(St.THROWN_PCT) : st(St.MELEE_PCT);

		if (enemy.HT > 0){
			if (enemy.HP <= enemy.HT * 0.3f) mult += st(St.EXECUTE);
			if (enemy.HP >= enemy.HT)        mult += st(St.OPENER);
		}
		boolean surprise = enemy instanceof Mob && ((Mob) enemy).surprisedBy(h, false);
		if (surprise){
			mult += st(St.SURPRISE);
			t.surpriseId = enemy.id();
		}
		if (Char.hasProp(enemy, Char.Property.BOSS) || Char.hasProp(enemy, Char.Property.MINIBOSS)) mult += st(St.BOSS);
		if (h.HP <= h.HT * 0.4f) mult += st(St.LOW_DMG);
		if (h.HP >= h.HT)        mult += st(St.FULL_DMG);
		if (enemy.buff(Burning.class) != null) mult += st(St.BURNING_DMG);
		if (enemy.buff(Chill.class) != null || enemy.buff(Frost.class) != null) mult += st(St.CHILLED_DMG);
		if (enemy.buff(Poison.class) != null) mult += st(St.POISONED_DMG);
		if (has(St.DEBUFF_DMG)){
			for (Buff b : enemy.buffs()){
				if (b.type == Buff.buffType.NEGATIVE){
					mult += st(St.DEBUFF_DMG);
					break;
				}
			}
		}
		if (has(St.BLOOD_RAGE) && h.HT > 0){
			mult += st(St.BLOOD_RAGE) * Math.max(0f, 1f - h.HP / (float) h.HT);
		}
		mult += st(St.RAGE) * t.rage;
		if (has(St.GOLD_DMG)) mult += Math.min(st(St.GOLD_DMG), Dungeon.gold / 10000f);

		mult += AugX.attackBonus(h, enemy, t, surprise, thrown);

		dmg = (dmg + st(St.DMG_FLAT)) * mult * AugX.attackMult(h, enemy);

		boolean sureCrit = (has(St.EXECUTE_CRIT) && enemy.HT > 0 && enemy.HP <= enemy.HT * 0.3f) || AugX.sureCrit(h) || AugY.sureCrit();
		float critChance = st(St.CRIT) + AugX.critBonus(h, enemy);
		if (sureCrit || (critChance > 0 && Random.Float() < critChance)){
			dmg *= 1.5f + st(St.CRITD);
			critEffect(enemy);
			critTarget = enemy;
		}
		return dmg;
	}

	private static Char critTarget = null;

	//whether the damage a character is taking right now comes from an augment critical hit
	public static boolean peekCrit( Char ch ){
		return critTarget != null && critTarget == ch;
	}

	public static boolean consumeCrit( Char ch ){
		if (critTarget == null) return false;
		boolean hit = critTarget == ch;
		critTarget = null;
		return hit;
	}

	static void critEffect( Char enemy ){
		if (enemy.sprite == null || !enemy.sprite.visible) return;
		if (!com.shatteredpixel.shatteredpixeldungeon.SPDSettings.damageNumbers()) enemy.sprite.showStatus(0xFFB040, "치명타!");
		enemy.sprite.centerEmitter().burst(SparkParticle.FACTORY, 6);
		Atmosphere.burstCell(enemy.pos, 1.8f, 0.6f, 0xFFD890, 0.3f);
		Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 0.8f, 1.15f);
	}

	//after a hero attack lands. damage is what the enemy is about to take
	public static void onHit( final Hero h, final Char enemy, final int damage ){
		final AugTracker t = tracker();
		if (t == null || enemy == null || enemy == h || enemy.alignment == h.alignment) return;

		if (st(St.RAGE) > 0){
			t.rage = Math.min(5, t.rage + 1);
			t.lastHit = Actor.now();
		}

		float ls = st(St.LIFESTEAL);
		//the extra hits of a split attack were already counted with the first one
		if (ls > 0 && damage > 0 && !AugY.inFlurry){
			float amt = damage * ls;
			int whole = (int) amt;
			if (Random.Float() < amt - whole) whole++;
			if (whole > 0){
				int room = h.HT - h.HP;
				heal(h, whole);
				//blood armor: what doesn't fit becomes a shield
				int over = whole - Math.max(0, room);
				if (over > 0 && has(St.U_BLOODARMOR)){
					Barrier b = h.buff(Barrier.class);
					int cap = Math.max(1, Math.round(h.HT * 0.2f));
					int cur = b == null ? 0 : b.shielding();
					int add = Math.min(over, cap - cur);
					if (add > 0){
						shield(h, add);
						AugFx.notify(St.U_BLOODARMOR);
					}
				}
			}
		}

		if (!enemy.isAlive()) return;

		final boolean melee = !(h.belongings.attackingWeapon() instanceof MissileWeapon)
				&& Dungeon.level.adjacent(h.pos, enemy.pos);

		if (proc(St.P_BURN) && !enemy.isImmune(Burning.class))   Buff.affect(enemy, Burning.class).reignite(enemy);
		if (proc(St.P_POISON) && !enemy.isImmune(Poison.class))  Buff.affect(enemy, Poison.class).set(3 + depthScale() / 2f);
		if (proc(St.P_CHILL) && !enemy.isImmune(Chill.class)){
			Buff.affect(enemy, Chill.class, 4f);
			AugX.coldApplied();
		}
		if (proc(St.P_FREEZE) && !enemy.isImmune(Frost.class)){
			Buff.affect(enemy, Frost.class, 2f);
			AugX.coldApplied();
		}
		if (proc(St.P_PARA) && !enemy.isImmune(Paralysis.class)) Buff.prolong(enemy, Paralysis.class, 2f);
		if (proc(St.P_BLEED))   Buff.affect(enemy, Bleeding.class).set(Math.max(2f, damage * 0.5f));
		if (proc(St.P_CRIPPLE)) Buff.prolong(enemy, Cripple.class, 4f);
		if (proc(St.P_WEAK))    Buff.prolong(enemy, Weakness.class, 5f);
		if (proc(St.P_VULN))    Buff.prolong(enemy, Vulnerable.class, 5f);
		if (proc(St.P_BLIND))   Buff.prolong(enemy, Blindness.class, 4f);
		if (proc(St.P_SLOW))    Buff.prolong(enemy, Slow.class, 4f);
		if (proc(St.P_HEX))     Buff.prolong(enemy, Hex.class, 5f);
		if (proc(St.P_TERROR) && !Char.hasProp(enemy, Char.Property.BOSS)){
			Buff.affect(enemy, Terror.class, 5f).object = h.id();
		}

		AugX.onHit(h, enemy, damage, !melee && h.belongings.attackingWeapon() instanceof MissileWeapon);
		AugY.onHit(h, enemy, damage, !melee && h.belongings.attackingWeapon() instanceof MissileWeapon, melee);
		if (!enemy.isAlive()) return;

		//effects that deal extra damage happen right after the hit itself
		final boolean echo  = proc(St.P_ECHO);
		final boolean arc   = proc(St.P_LIGHTNING);
		final boolean blast = proc(St.P_BLAST);
		final boolean knock = proc(St.P_KNOCK);
		final float cleave  = melee ? st(St.CLEAVE) : 0f;
		if (!echo && !arc && !blast && !knock && cleave <= 0) return;

		Actor.add(new Actor() {
			{ actPriority = VFX_PRIO; }

			@Override
			protected boolean act() {
				Actor.remove(this);
				if (cleave > 0 && damage > 0){
					boolean cleaved = false;
					for (int n : PathFinder.NEIGHBOURS8){
						Char ch = Actor.findChar(h.pos + n);
						if (ch != null && ch != enemy && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
							Wound.hit(ch);
							cleaved = true;
							int cd = Math.max(1, Math.round(damage * cleave));
							ch.damage(cd, CLEAVE);
							if (has(St.CLEAVE_STEAL)){
								int back = Math.round(cd * Math.max(0.1f, st(St.LIFESTEAL)));
								if (back > 0) heal(h, back);
							}
						}
					}
					if (cleaved) AugFx.notify(St.CLEAVE);
				}
				if (!enemy.isAlive()) return true;
				if (echo && damage > 0){
					echoStrike(enemy, Math.max(1, Math.round(damage * 0.6f)));
				}
				if (arc && enemy.isAlive()){
					chainLightning(h, enemy, Math.max(1, Math.round(damage * 0.5f)));
				}
				if (blast && enemy.isAlive()){
					explosion(enemy.pos, Math.max(1, Math.round(damage * 0.6f)), false, enemy);
				}
				if (knock && enemy.isAlive() && !Char.hasProp(enemy, Char.Property.IMMOVABLE)){
					Ballistica traj = new Ballistica(h.pos, enemy.pos, Ballistica.STOP_TARGET);
					traj = new Ballistica(traj.collisionPos, traj.path.get(Math.min(traj.dist + 2, traj.path.size() - 1)), Ballistica.PROJECTILE);
					WandOfBlastWave.throwChar(enemy, traj, 2, false, true, h);
				}
				return true;
			}
		});
	}

	static void echoStrike( Char enemy, int dmg ){
		if (enemy.sprite != null && enemy.sprite.visible){
			Wound.hit(enemy);
			enemy.sprite.centerEmitter().burst(ShadowParticle.UP, 4);
			enemy.sprite.showStatus(0xC8B0FF, "메아리");
			Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 0.6f, 1.25f);
		}
		enemy.damage(dmg, ECHO);
	}

	static void chainLightning( Hero h, Char enemy, int dmg ){
		ArrayList<Char> affected = new ArrayList<>();
		ArrayList<Lightning.Arc> arcs = new ArrayList<>();
		affected.add(enemy);
		arcs.add(new Lightning.Arc(h.sprite.center(), enemy.sprite.center()));
		Char from = enemy;
		for (int jump = 0; jump < 3; jump++){
			Char next = null;
			for (int n : PathFinder.NEIGHBOURS8){
				for (int r = 1; r <= 2 && next == null; r++){
					int c = from.pos + n * r;
					if (c < 0 || c >= Dungeon.level.length()) continue;
					Char ch = Actor.findChar(c);
					if (ch != null && ch.alignment == Char.Alignment.ENEMY && !affected.contains(ch)) next = ch;
				}
				if (next != null) break;
			}
			if (next == null) break;
			arcs.add(new Lightning.Arc(from.sprite.center(), next.sprite.center()));
			affected.add(next);
			from = next;
		}
		for (Char ch : affected){
			if (ch.isAlive() && !ch.isImmune(Electricity.class)){
				ch.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
				ch.sprite.flash();
				ch.damage(dmg, LIGHTNING);
				AugX.counted(AugGrow.LIGHTNING, 1);
				if (ch.isAlive() && has(St.ARC_FREEZE) && !ch.isImmune(Frost.class)){
					Buff.affect(ch, Frost.class, 1.5f);
					AugX.coldApplied();
					AugX.checkSteam(ch);
				}
			}
		}
		if (h.sprite != null && h.sprite.parent != null){
			h.sprite.parent.addToFront(new Lightning(arcs, null));
		}
		for (Char ch : affected) Atmosphere.burstCell(ch.pos, 2.2f, 0.7f, 0xC8E0FF, 0.25f);
		Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 0.7f);
	}

	//damages enemies around a cell. center: whether the center cell is hit too
	static void explosion( int cell, int dmg, boolean center, Char skip ){
		if (Dungeon.level.heroFOV[cell]){
			CellEmitter.center(cell).burst(BlastParticle.FACTORY, 20);
			CellEmitter.get(cell).burst(SmokeParticle.FACTORY, 4);
			Atmosphere.burstCell(cell, 3f, 0.9f, 0xFFC070, 0.45f);
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.6f, 1.15f);
		}
		for (int n : PathFinder.NEIGHBOURS9){
			if (n == 0 && !center) continue;
			int c = cell + n;
			if (c < 0 || c >= Dungeon.level.length()) continue;
			Char ch = Actor.findChar(c);
			if (ch != null && ch != skip && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
				ch.damage(dmg, BLAST);
			}
		}
	}

	static void fireBurst( int cell, Char skip ){
		int dmg = 2 + depthScale() / 2;
		if (Dungeon.level.heroFOV[cell]){
			for (int n : PathFinder.NEIGHBOURS9){
				int c = cell + n;
				if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c]){
					CellEmitter.get(c).burst(FlameParticle.FACTORY, 5);
				}
			}
			Atmosphere.burstCell(cell, 3.2f, 0.9f, 0xFF9A40, 0.6f);
			Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.8f);
		}
		for (int n : PathFinder.NEIGHBOURS8){
			int c = cell + n;
			if (c < 0 || c >= Dungeon.level.length()) continue;
			Char ch = Actor.findChar(c);
			if (ch != null && ch != skip && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
				ch.damage(dmg, FIRE);
				if (ch.isAlive() && !ch.isImmune(Burning.class)){
					Buff.affect(ch, Burning.class).reignite(ch);
					AugX.checkSteam(ch);
				}
			}
		}
	}

	static void frostBurst( int cell, Char skip ){
		int dmg = 1 + depthScale() / 3;
		if (Dungeon.level.heroFOV[cell]){
			for (int n : PathFinder.NEIGHBOURS9){
				int c = cell + n;
				if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c]){
					CellEmitter.get(c).burst(SnowParticle.FACTORY, 6);
				}
			}
			Atmosphere.burstCell(cell, 3.2f, 0.85f, 0xA8DCFF, 0.6f);
			Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.6f, 1.2f);
		}
		for (int n : PathFinder.NEIGHBOURS8){
			int c = cell + n;
			if (c < 0 || c >= Dungeon.level.length()) continue;
			Char ch = Actor.findChar(c);
			if (ch != null && ch != skip && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
				ch.damage(dmg, FROST);
				if (ch.isAlive() && !ch.isImmune(Chill.class)){
					Buff.affect(ch, Chill.class, 4f);
					AugX.coldApplied();
					AugX.checkSteam(ch);
				}
			}
		}
	}

	// ================================================================ being attacked

	public static int reduceDamage( Hero h, int dmg, Object src ){
		if (dmg <= 0) return dmg;
		float red = st(St.TAKEN);
		if (h.HP <= h.HT * 0.4f) red += st(St.LOW_TAKEN);
		red += AugX.damageReduction(h, src);
		red = Math.max(-2f, Math.min(0.6f, red));
		//negative reduction (from gambles) means taking more damage
		if (red != 0) dmg = Math.max(1, (int) Math.ceil(dmg * (1f - red)));
		//the roulette's blessing halves what is left
		float bless = AugY.blessingMult(1);
		if (bless != 1f) dmg = Math.max(1, (int) Math.ceil(dmg * bless));
		return AugX.capDamage(h, dmg);
	}

	//lethal damage can be survived once per floor
	public static int lastStand( Hero h, int dmg ){
		AugTracker t = tracker();
		if (t == null || !has(St.LAST_STAND)) return dmg;
		if (dmg < h.HP + h.shielding()) return dmg;
		int key = AugTracker.floorKey();
		if (t.lastStandFloor == key) return dmg;
		t.lastStandFloor = key;
		lastStandTriggered = true;
		AugFx.notify(St.LAST_STAND);
		if (h.sprite != null){
			h.sprite.showStatus(0xFFE080, "최후의 저항!");
			AugFx.flare(h.sprite, 6,28, 0xFFE080, 1.6f);
			Atmosphere.burstCell(h.pos, 4f, 1f, 0xFFE8A0, 1.2f);
			PostFX.flash(0xFFE8A0, 0.15f);
		}
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 0.8f, 0.8f);
		GLog.p("최후의 저항으로 쓰러지지 않았습니다!");
		return Math.max(0, h.HP + h.shielding() - 1);
	}

	private static boolean lastStandTriggered = false;

	//called once the damage of a last stand went through, so the hero is left at 1 HP first
	public static void afterLastStand( Hero h ){
		if (!lastStandTriggered) return;
		lastStandTriggered = false;
		if (h.isAlive()){
			Buff.affect(h, Invulnerability.class, Invulnerability.DURATION);
			if (has(St.LAST_STAND_HEAL)) heal(h, h.HT / 2);
		}
	}

	//once per run, the hero comes back from death
	public static boolean tryRevive( Hero h ){
		AugTracker t = tracker();
		if (t == null || t.reviveUsed || !has(St.REVIVE)) return false;
		t.reviveUsed = true;
		h.HP = Math.max(1, h.HT / 2);
		AugFx.notify(St.REVIVE);
		Buff.affect(h, Invulnerability.class, Invulnerability.DURATION);
		if (h.sprite != null){
			h.sprite.showStatus(0xFFC860, "부활!");
			AugFx.flare(h.sprite, 8,40, 0xFFB040, 2.5f);
			h.sprite.emitter().burst(FlameParticle.FACTORY, 20);
			h.sprite.emitter().burst(Speck.factory(Speck.HEALING), 6);
		}
		Atmosphere.burstCell(h.pos, 6f, 1f, 0xFFB050, 2.2f);
		PostFX.flash(0xFFC870, 0.3f);
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 1f, 0.7f);
		GLog.p("증강의 힘으로 죽음에서 되살아났습니다!");
		return true;
	}

	//after damage was taken
	public static void afterDamage( Hero h, int taken ){
		AugTracker t = tracker();
		if (t == null || !h.isAlive()) return;
		AugX.afterDamage(h, taken);
		AugQuest.afterDamage(h, taken, h.HP + taken);
		if (!has(St.BLINK_LOW)) return;
		if (h.HP > h.HT * 0.25f) return;
		int key = AugTracker.floorKey();
		if (t.blinkFloor == key) return;
		t.blinkFloor = key;
		AugFx.notify(St.BLINK_LOW);
		if (h.sprite != null) CellEmitter.get(h.pos).burst(ShadowParticle.UP, 10);
		if (ScrollOfTeleportation.teleportChar(h)){
			GLog.p("환영이 당신을 빼돌렸습니다!");
		}
	}

	public static void onDefense( Hero h, Char enemy, int damage ){
		if (tracker() == null || enemy == null || enemy == h) return;
		boolean adjacent = Dungeon.level.adjacent(h.pos, enemy.pos);

		float thorns = st(St.THORNS);
		if (thorns > 0 && damage > 0 && adjacent && enemy.isAlive()){
			int back = Math.max(1, Math.round(damage * thorns));
			enemy.damage(back, THORNS);
			AugFx.notify(St.THORNS);
			if (enemy.sprite != null) enemy.sprite.flash();
		}
		if (proc(St.H_SHIELD)) shield(h, Math.max(1, Math.round(h.HT * 0.1f)));
		if (proc(St.H_FROST)) frostBurst(h.pos, null);
		if (proc(St.H_HASTE)) Buff.prolong(h, Haste.class, 2f);
		AugX.onDefense(h, enemy, damage, adjacent);
		AugY.onDefense(h, enemy, damage, adjacent);
		if (adjacent && enemy.isAlive()){
			if (proc(St.H_SHOCK) && !enemy.isImmune(Electricity.class)){
				ArrayList<Lightning.Arc> arcs = new ArrayList<>();
				arcs.add(new Lightning.Arc(h.sprite.center(), enemy.sprite.center()));
				h.sprite.parent.addToFront(new Lightning(arcs, null));
				enemy.damage(2 + depthScale() / 2, LIGHTNING);
				Atmosphere.burstCell(enemy.pos, 2f, 0.6f, 0xC8E0FF, 0.25f);
				Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 0.5f);
			}
			if (enemy.isAlive() && proc(St.H_BURN) && !enemy.isImmune(Burning.class)){
				Buff.affect(enemy, Burning.class).reignite(enemy);
			}
			if (enemy.isAlive() && proc(St.H_POISON) && !enemy.isImmune(Poison.class)){
				Buff.affect(enemy, Poison.class).set(3 + depthScale() / 2f);
			}
		}
	}

	// ================================================================ killing

	public static boolean heroCaused( Object cause ){
		return cause == Dungeon.hero || cause instanceof Src || cause instanceof Wand
				|| cause instanceof Buff.DOTbuff || cause instanceof Blob
				|| cause instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon
				|| cause instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment;
	}

	public static void onKill( Mob m ){
		Hero h = Dungeon.hero;
		if (tracker() == null || h == null || !h.isAlive()) return;
		int pos = m.pos;

		int heal = Math.round(st(St.K_HEAL));
		if (heal > 0) heal(h, heal);
		int sh = Math.round(st(St.K_SHIELD));
		if (sh > 0) shield(h, sh);
		int haste = Math.round(st(St.K_HASTE));
		if (haste > 0) Buff.prolong(h, Haste.class, haste);
		int bless = Math.round(st(St.K_BLESS));
		if (bless > 0) Buff.prolong(h, Bless.class, bless);
		if (proc(St.K_INVIS)) Buff.affect(h, Invisibility.class, 2f);

		if (proc(St.K_GOLD)){
			Gold g = new Gold(Random.IntRange(4 + depthScale() * 2, 10 + depthScale() * 4));
			Dungeon.level.drop(g, pos).sprite.drop();
		}
		if (proc(St.K_BLAST)){
			explosion(pos, Math.max(2, Math.round(m.HT * 0.3f) + depthScale()), true, null);
		}
		if (proc(St.K_FIRE)) fireBurst(pos, null);
		if (proc(St.K_FROST)) frostBurst(pos, null);
		if (proc(St.K_GAS)){
			GameScene.add(Blob.seed(pos, 120, ToxicGas.class));
		}
		if (proc(St.K_RECHARGE)){
			for (Item i : h.belongings){
				if (i instanceof Wand && ((Wand) i).curCharges < ((Wand) i).maxCharges){
					((Wand) i).curCharges++;
					i.updateQuickslot();
					ScrollOfRecharging.charge(h);
					break;
				}
			}
		}
		if (has(St.K_WRAITH) && !(m instanceof Wraith) && AugX.wraithRoom() && proc(St.K_WRAITH)){
			Wraith w = Wraith.spawnAt(pos, Wraith.class);
			if (w != null){
				Buff.affect(w, Corruption.class);
				if (Dungeon.level.heroFOV[pos]){
					CellEmitter.get(pos).burst(ShadowParticle.CURSE, 6);
					Sample.INSTANCE.play(Assets.Sounds.CURSED, 0.7f);
				}
			}
		}
		AugX.onKill(h, m, tracker());
	}

	// ================================================================ periodic and floor effects

	public static void onNewFloor( Hero h ){
		for (St s : new St[]{St.F_SHIELD, St.F_HEAL, St.F_MIND, St.F_RECHARGE, St.F_GOLD}){
			if (has(s)) AugFx.notify(s);
		}
		float sh = st(St.F_SHIELD);
		if (sh > 0) shield(h, Math.max(1, Math.round(h.HT * sh)));
		float hl = st(St.F_HEAL);
		if (hl > 0) heal(h, Math.round(h.HT * hl));
		int mind = Math.round(st(St.F_MIND));
		if (mind > 0) Buff.affect(h, MindVision.class, mind);
		int rech = Math.round(st(St.F_RECHARGE));
		if (rech > 0) Buff.affect(h, Recharging.class, rech);
		int gold = Math.round(st(St.F_GOLD));
		if (gold > 0){
			int amt = gold * Math.max(1, Dungeon.depth);
			Dungeon.gold += amt;
			if (h.sprite != null) h.sprite.showStatusWithIcon(CharSprite.NEUTRAL, Integer.toString(amt), FloatingText.GOLD);
		}
		if (proc(St.F_MAP)) revealMap();
		AugX.onNewFloor(h);
	}

	private static void revealMap(){
		int length = Dungeon.level.length();
		for (int i = 0; i < length; i++){
			int terr = Dungeon.level.map[i];
			if (Dungeon.level.discoverable[i]){
				Dungeon.level.mapped[i] = true;
				if ((Terrain.flags[terr] & Terrain.SECRET) != 0){
					Dungeon.level.discover(i);
				}
			}
		}
		GameScene.updateFog();
		Dungeon.observe();
		GLog.p("증강의 힘으로 이 층의 지도가 드러났습니다.");
		Sample.INSTANCE.play(Assets.Sounds.READ, 0.6f);
	}

	public static void everyTurn( Hero h, AugTracker t ){
		float fire = t.total[St.AURA_FIRE.ordinal()];
		float frost = t.total[St.AURA_FROST.ordinal()];
		float shock = t.total[St.AURA_SHOCK.ordinal()];
		if (fire > 0 || frost > 0 || shock > 0){
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar(h.pos + n);
				if (ch == null || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()) continue;
				if (fire > 0 && Random.Float() < fire && !ch.isImmune(Burning.class)){
					Buff.affect(ch, Burning.class).reignite(ch);
					AugFx.notify(St.AURA_FIRE);
					AugX.checkSteam(ch);
				}
				if (frost > 0 && Random.Float() < frost && !ch.isImmune(Chill.class)){
					Buff.affect(ch, Chill.class, 3f);
					if (ch.sprite != null && ch.sprite.visible) ch.sprite.emitter().burst(SnowParticle.FACTORY, 3);
					AugFx.notify(St.AURA_FROST);
					AugX.coldApplied();
					AugX.checkSteam(ch);
				}
				if (shock > 0 && Random.Float() < shock && !ch.isImmune(Electricity.class) && h.sprite != null && ch.sprite != null){
					ArrayList<Lightning.Arc> arcs = new ArrayList<>();
					arcs.add(new Lightning.Arc(h.sprite.center(), ch.sprite.center()));
					h.sprite.parent.addToFront(new Lightning(arcs, null));
					ch.damage(1 + depthScale() / 3, LIGHTNING);
					Atmosphere.burstCell(ch.pos, 1.8f, 0.5f, 0xC8E0FF, 0.2f);
					AugFx.notify(St.AURA_SHOCK);
					Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 0.35f, 1.3f);
				}
			}
		}

		if (has(St.TRAP_SENSE)){
			int w = Dungeon.level.width();
			for (int dy = -2; dy <= 2; dy++){
				for (int dx = -2; dx <= 2; dx++){
					int c = h.pos + dx + dy * w;
					if (c < 0 || c >= Dungeon.level.length()) continue;
					if (Math.abs((c % w) - (h.pos % w)) > 2) continue;
					int terr = Dungeon.level.map[c];
					if ((Terrain.flags[terr] & Terrain.SECRET) != 0 && Dungeon.level.heroFOV[c]){
						Dungeon.level.discover(c);
						GameScene.discoverTile(c, terr);
						ScrollOfMagicMapping.discover(c);
						GLog.w("함정 감지: 숨겨진 것을 발견했습니다.");
					}
				}
			}
		}
	}

	//arcane orb: a bolt at the nearest visible enemy. returns false if there was nothing to shoot
	//the orb's damage lands when the bolt arrives, so the caller waits for it (waiter.next() is called then):
	//damage arriving in the middle of another character's animation would leave that animation unfinished
	public static boolean fireOrb( final Hero h, float power, final Actor waiter ){
		if (h.sprite == null || h.sprite.parent == null) return false;
		Mob best = null;
		int bestDist = Integer.MAX_VALUE;
		for (Mob m : Dungeon.level.mobs){
			if (m.alignment != Char.Alignment.ENEMY || !m.isAlive() || !Dungeon.level.heroFOV[m.pos]) continue;
			int d = Dungeon.level.distance(h.pos, m.pos);
			if (d > 7 || d >= bestDist) continue;
			Ballistica b = new Ballistica(h.pos, m.pos, Ballistica.MAGIC_BOLT);
			if (b.collisionPos != m.pos) continue;
			best = m;
			bestDist = d;
		}
		if (best == null) return false;
		final Mob target = best;
		final int dmg = Math.max(1, Math.round(Random.NormalIntRange(3 + depthScale() / 2, 6 + depthScale()) * power));
		MagicMissile.boltFromChar(h.sprite.parent, MagicMissile.MAGIC_MISSILE, h.sprite, target.pos, new Callback() {
			@Override
			public void call() {
				if (target.isAlive()){
					target.damage(dmg, ORB);
					if (target.sprite != null){
						target.sprite.burst(0xFFC890FF, 5);
						Atmosphere.burstCell(target.pos, 2f, 0.7f, 0xD8A0FF, 0.3f);
					}
				}
				if (waiter != null) waiter.next();
			}
		});
		Sample.INSTANCE.play(Assets.Sounds.ZAP, 0.6f, 1.2f);
		AugFx.notify(St.ORB);
		return true;
	}

	// ================================================================ helpers

	public static void heal( Hero h, int amount ){
		int real = Math.min(amount, h.HT - h.HP);
		if (real <= 0) return;
		h.HP += real;
		if (h.sprite != null) h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(real), FloatingText.HEALING);
	}

	public static void shield( Hero h, int amount ){
		if (amount <= 0) return;
		Buff.affect(h, Barrier.class).incShield(amount);
		if (h.sprite != null) h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.SHIELDING);
	}

	// ================================================================ choosing augments

	//the hero reached a new level
	public static void onLevelUp( Hero h ){
		AugTracker t = tracker();
		if (t == null) return;
		t.pending++;
		//every fifth level: a stronger round
		if (h.lvl % 5 == 0) t.premium++;
	}

	private static boolean picking = false;

	//called every frame from the game scene: opens the choice when one is waiting
	public static void checkPending(){
		AugTracker t = tracker();
		if (t == null || t.pending <= 0 || picking) return;
		Hero h = Dungeon.hero;
		if (!h.isAlive() || !h.ready || GameScene.showingWindow()) return;
		if (t.offer == null) makeOffer(t);
		picking = true;
		GameScene.show(new WndAugment(t));
	}

	static void pickWindowClosed(){
		picking = false;
	}

	private static java.util.Random rng( AugTracker t, int salt ){
		return new java.util.Random(Dungeon.seed * 1000003L + t.picks * 7919L + salt * 104729L + 17L);
	}

	//tier chances shift towards the rarer tiers as the hero levels up
	private static int rollTier( java.util.Random r, int level ){
		float prism = 0.03f + 0.006f * level;
		float gold = 0.22f + 0.008f * level;
		float f = r.nextFloat();
		if (f < prism) return Aug.PRISM;
		if (f < prism + gold) return Aug.GOLD;
		return Aug.SILVER;
	}

	static void makeOffer( AugTracker t ){
		java.util.Random r = rng(t, 0);
		int tier;
		boolean boss = t.forcePrism > 0;
		boolean premium = !boss && t.premium > 0;
		boolean golden = false;
		if (boss){
			//a boss reward is gold or prism, even odds
			tier = r.nextBoolean() ? Aug.PRISM : Aug.GOLD;
			t.forcePrism--;
		} else {
			tier = rollTier(r, Dungeon.hero.lvl);
			if (premium){
				//the round promised every fifth level: at least gold
				tier = Math.max(tier, Aug.GOLD);
				t.premium--;
			} else if (r.nextFloat() < 0.03f){
				//very rarely, out of nowhere: a golden round, all prism
				tier = Aug.PRISM;
				golden = true;
			}
		}
		float luck = r.nextFloat();
		int count = premium ? 4 : ((!boss && !golden && luck < 0.08f) ? 4 : 3);

		t.offerTier = tier;
		t.lucky = boss ? 3 : (golden ? 4 : (premium ? 5 : (count == 4 ? 1 : 0)));
		t.mystery = -1;
		t.offer = new String[count];
		t.rerolled = new boolean[4];
		t.cardTier = new int[4];
		java.util.Arrays.fill(t.cardTier, tier);
		t.shownThisPick.clear();
		for (int i = 0; i < count; i++){
			Aug a = drawCard(t, tier, r);
			t.offer[i] = a.name;
			t.shownThisPick.add(a.name);
		}

		//lucky moment: one card is raised a tier
		if (t.lucky == 0 && count == 3 && tier < Aug.PRISM && luck < 0.18f){
			int i = r.nextInt(count);
			Aug a = drawCard(t, tier + 1, r);
			t.offer[i] = a.name;
			t.cardTier[i] = a.tier;
			t.shownThisPick.add(a.name);
			t.lucky = 2;
		}

		//behind on basic stats: make sure one card helps with them
		if (tier == Aug.SILVER){
			Aug core = neededCore(t);
			if (core != null){
				boolean present = false;
				for (String n : t.offer) if (Aug.get(n).set == null) present = true;
				int slot = count - 1;
				if (t.cardTier[slot] != tier) slot = 0;
				if (!present){
					t.offer[slot] = core.name;
					t.shownThisPick.add(core.name);
				}
			}
		}

		//a legendary combination that has been unlocked can turn up in place of a card,
		//and the one the player is aiming for always does
		ArrayList<Aug> legends = unlockedLegends(t);
		Aug goal = t.goal == null ? null : Aug.get(t.goal);
		boolean goalReady = goal != null && legends.contains(goal);
		if (!legends.isEmpty() && (goalReady || r.nextFloat() < 0.4f)){
			Aug l = goalReady ? goal : legends.get(r.nextInt(legends.size()));
			int i = r.nextInt(count);
			if (t.cardTier[i] != tier) i = (i + 1) % count;
			t.offer[i] = l.name;
			t.cardTier[i] = Aug.LEGEND;
			t.shownThisPick.add(l.name);
		}

		//the mystery card: face down until chosen, anything from silver to legend
		if (!golden && r.nextFloat() < ((boss || premium) ? 0.25f : 0.12f)){
			int i = -1;
			for (int k = count - 1; k >= 0; k--) if (t.cardTier[k] == tier){ i = k; break; }
			if (i >= 0){
				float f = r.nextFloat();
				int mt = f < 0.25f ? Aug.SILVER : (f < 0.60f ? Aug.GOLD : (f < 0.90f ? Aug.PRISM : Aug.LEGEND));
				Aug m = null;
				if (mt == Aug.LEGEND){
					ArrayList<Aug> open = unlockedLegends(t);
					open.removeIf(x -> t.shownThisPick.contains(x.name));
					if (!open.isEmpty()) m = open.get(r.nextInt(open.size()));
					else mt = Aug.PRISM;
				}
				if (m == null) m = drawCard(t, mt, r);
				t.offer[i] = m.name;
				t.cardTier[i] = m.tier;
				t.shownThisPick.add(m.name);
				t.mystery = i;
			}
		}
	}

	static ArrayList<Aug> unlockedLegends( AugTracker t ){
		ArrayList<Aug> out = new ArrayList<>();
		for (Aug a : Aug.all()){
			if (a.tier == Aug.LEGEND && t.timesOwned(a) == 0 && a.unlocked(t.owned)) out.add(a);
		}
		return out;
	}

	static void reroll( AugTracker t, int index ){
		if (t.offer == null || index >= t.offer.length || t.rerolled[index]) return;
		java.util.Random r = rng(t, 100 + index);
		int tier = Math.min(Aug.PRISM, t.cardTier[index]);
		if (t.cardTier[index] == Aug.LEGEND) tier = t.offerTier;
		//rerolling never makes a card worse than the round it belongs to
		tier = Math.max(tier, Math.min(Aug.PRISM, t.offerTier));
		Aug a = drawCard(t, tier, r);
		t.offer[index] = a.name;
		t.cardTier[index] = a.tier;
		t.shownThisPick.add(a.name);
		t.rerolled[index] = true;
	}

	//a boss fell: the next choice is a gold or prism one
	public static void onBossKill(){
		AugTracker t = tracker();
		if (t == null) return;
		t.forcePrism++;
		t.pending++;
		AugQuest.onBossKill();
		GLog.p("보스 처치! 골드 또는 프리즘 증강을 고를 수 있습니다.");
	}

	private static Aug neededCore( AugTracker t ){
		Hero h = Dungeon.hero;
		int lvl = h.lvl;
		float expectHP = 20 + 5 * (lvl - 1);
		float expectStat = lvl - 1;
		//accuracy and evasion grow with levels, so only health can fall behind
		if (h.HT < expectHP * 0.75f) return Aug.get("체력 단련");
		return null;
	}

	private static Aug drawCard( AugTracker t, int tier, java.util.Random r ){
		ArrayList<Aug> pool = new ArrayList<>();
		ArrayList<Float> weights = new ArrayList<>();
		Aug goal = t.goal == null ? null : Aug.get(t.goal);
		for (int pass = tier; pass >= 0 && pool.isEmpty(); pass--){
			for (Aug a : Aug.all()){
				if (a.tier != pass) continue;
				if (t.shownThisPick.contains(a.name)) continue;
				if (a.name.equals(t.devoured)) continue;
				boolean star = t.canStar(a);
				if (!a.repeatable && t.timesOwned(a) > 0 && !star) continue;
				float w = 1f;
				if (a.set != null && t.count(a.set) > 0) w = 1.6f;
				if (a.set == null) w = 0.7f;
				if (a.gamble) w *= 0.8f;
				//an augment already held comes back as a star upgrade, a bit less often than new ones
				if (star) w = 0.55f;
				//materials of the legend being aimed for turn up more
				if (goal != null && !star && goal.requires != null && java.util.Arrays.asList(goal.requires).contains(a.name)) w *= 3f;
				pool.add(a);
				weights.add(w);
			}
		}
		if (pool.isEmpty()){
			//everything has been seen this pick, allow repeats of the shown ones
			return Aug.get("체력 단련");
		}
		float sum = 0;
		for (float w : weights) sum += w;
		float f = r.nextFloat() * sum;
		for (int i = 0; i < pool.size(); i++){
			f -= weights.get(i);
			if (f <= 0) return pool.get(i);
		}
		return pool.get(pool.size() - 1);
	}

	//the player took an augment
	static void choose( AugTracker t, Aug a ){
		Hero h = Dungeon.hero;
		int[] before = t.setCount.clone();
		ArrayList<Aug> legendsBefore = unlockedLegends(t);
		boolean starUp = !a.repeatable && t.owned.contains(a.name);
		boolean goalDone = !starUp && a.name.equals(t.goal);
		int newStars = 1;
		if (starUp){
			newStars = Math.min(AugTracker.MAX_STARS, t.stars(a.name) + 1);
			t.stars.put(a.name, newStars);
			if (newStars == AugTracker.MAX_STARS) t.stars3++;
		} else {
			t.owned.add(a.name);
			if (a.tier == Aug.LEGEND) t.legendLog.add(a.name + " (" + Dungeon.depth + "층" + (goalDone ? ", 목표 달성" : "") + ")");
			if (goalDone) t.goalsDone++;
			if (a.name.equals(t.goal)) t.goal = null;
		}
		t.mystery = -1;
		t.pending = Math.max(0, t.pending - 1);
		t.picks++;
		t.offer = null;
		t.rerolled = new boolean[4];
		t.lucky = 0;
		t.shownThisPick.clear();
		if (!starUp){
			AugGrow.taken(t, a);
			AugQuest.taken(t, a);
		}
		t.recalc();
		h.updateHT(true);
		com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.refreshHero();

		if (starUp){
			starEffect(h, a, newStars);
			return;
		}

		//one-off rewards
		for (java.util.Map.Entry<St, Float> e : a.stats.entrySet()){
			int n = Math.round(e.getValue());
			switch (e.getKey()){
				case G_GOLD:
					Dungeon.gold += n;
					if (h.sprite != null) h.sprite.showStatusWithIcon(CharSprite.NEUTRAL, Integer.toString(n), FloatingText.GOLD);
					break;
				case G_UPGRADE:   give(h, new ScrollOfUpgrade(), n); break;
				case G_STRENGTH:  give(h, new PotionOfStrength(), n); break;
				case G_HEALING:   give(h, new PotionOfHealing(), n); break;
				case G_EXPPOT:    give(h, new PotionOfExperience(), n); break;
				default: break;
			}
		}

		Augs.takenEffect(h, a);

		//announce newly reached set bonuses
		if (a.set != null){
			int i = a.set.ordinal();
			int was = AugSet.reached(before[i]), now = AugSet.reached(t.setCount[i]);
			if (now > was){
				if (now == AugSet.THRESHOLDS.length){
					//the whole family: awakening
					GLog.h("각성: " + a.set.title + "의 기운이 깨어났습니다!");
					if (h.sprite != null){
						AugFx.flare(h.sprite, 10,48, a.set.color, 2.2f);
						Atmosphere.burst(h.sprite.center().x, h.sprite.center().y, 7f, 1f, a.set.color, 2.4f);
						PostFX.flash(a.set.color, 0.14f);
					}
					AugFx.buildComplete(a.set.color);
					Sample.INSTANCE.play(Assets.Sounds.MASTERY, 0.8f);
				} else {
					GLog.p("세트 효과 발동: " + a.set.title + " (" + AugSet.THRESHOLDS[now - 1] + ")");
					if (h.sprite != null){
						AugFx.flare(h.sprite, 7,32, a.set.color, 1.4f);
						Atmosphere.burst(h.sprite.center().x, h.sprite.center().y, 5f, 0.85f, a.set.color, 1.4f);
					}
					Sample.INSTANCE.play(Assets.Sounds.BADGE, 0.7f);
				}
			}
		}
		GLog.p("증강 획득: " + a.name);
		if (a.tier == Aug.LEGEND){
			GLog.h("전설 완성: " + a.name + "!");
			AugFx.buildComplete(0xFF8A50);
			Sample.INSTANCE.play(Assets.Sounds.MASTERY, 0.9f, 0.9f);
			//three materials: a bigger moment
			if (a.requires != null && a.requires.length >= 3){
				GLog.h("삼중 조합 완성!");
				PostFX.flash(0xFFB070, 0.3f);
				com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.shake(2.5f, 0.4f);
				com.shatteredpixel.shatteredpixeldungeon.atmosphere.Ambience.slowMotion(0.7f);
				if (h.sprite != null){
					AugFx.flare(h.sprite, 12,56, 0xFF9A50, 2.6f);
					h.sprite.emitter().burst(Speck.factory(Speck.STAR), 30);
				}
			}
			if (goalDone) AugMoments.goalComplete(a);
			if (a.stats.containsKey(St.L_DEVOUR)){
				com.watabou.noosa.Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						AugTracker tt = tracker();
						if (tt == null || tt.devoured != null) return;
						boolean any = false;
						for (String n : tt.owned) if (AugY.canDevour(Aug.get(n))){ any = true; break; }
						if (any) GameScene.show(new AugY.WndDevour());
					}
				});
			}
		}

		//newly unlocked hidden combinations
		for (Aug l : unlockedLegends(t)){
			if (!legendsBefore.contains(l)){
				GLog.h("숨겨진 조합 발견: " + l.name + " (" + l.requiresText() + ")");
				if (h.sprite != null){
					AugFx.flare(h.sprite, 8,36, 0xFF8A50, 1.8f);
				}
				Sample.INSTANCE.play(Assets.Sounds.SECRET, 0.8f, 0.9f);
			}
		}
	}

	//an augment taken again: it grows a star
	private static void starEffect( Hero h, Aug a, int stars ){
		String st = Aug.stars(stars);
		int col = a.set != null ? a.set.color : 0xFFD45A;
		if (stars >= AugTracker.MAX_STARS){
			GLog.h(a.name + " " + st + " 완성! 모든 효과 " + AugTracker.STAR_POWER[stars] + "배");
			AugFx.buildComplete(col);
		} else {
			GLog.p(a.name + " " + st + " 강화! 모든 효과 " + AugTracker.STAR_POWER[stars] + "배");
		}
		Sample.INSTANCE.play(Assets.Sounds.AUG_STAR, 1f, stars >= AugTracker.MAX_STARS ? 0.9f : 1f);
		if (h.sprite != null){
			h.sprite.showStatus(0xFFE070, st);
			AugFx.flare(h.sprite, 7,30, col, 1.6f);
			h.sprite.emitter().burst(Speck.factory(Speck.STAR), 8 + 6 * stars);
			PointF c = h.sprite.center();
			Atmosphere.burst(c.x, c.y, 5f + stars, 0.9f, col, 1.6f);
		}
	}

	private static void give( Hero h, Item item, int qty ){
		if (qty <= 0) return;
		item.quantity(qty);
		item.identify();
		if (!item.collect()){
			Dungeon.level.drop(item, h.pos).sprite.drop();
		}
	}

	//light and specks around the hero when an augment is taken, coloured by tier
	private static void takenEffect( Hero h, Aug a ){
		if (h.sprite == null) return;
		PointF c = h.sprite.center();
		int col = a.tier == Aug.LEGEND ? 0xFF9A50 : (a.tier == Aug.PRISM ? 0xF0C8FF : (a.tier == Aug.GOLD ? 0xFFD870 : 0xE0E8F4));
		Atmosphere.burst(c.x, c.y, 4.5f + a.tier, 0.8f, col, 1.2f);
		h.sprite.emitter().burst(Speck.factory(Speck.STAR), 4 + 3 * a.tier);
	}
}
