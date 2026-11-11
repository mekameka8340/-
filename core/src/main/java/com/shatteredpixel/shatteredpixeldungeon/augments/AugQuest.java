/*
 * Late bloomers: prism augments that grow by completing stages. While a stage is being worked on
 * nothing changes; finishing it brings a clear jump in power. Stars on the augment lower the
 * requirements instead of raising the effects.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.Ambience;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.Atmosphere;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.PostFX;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.EnumMap;

public class AugQuest {

	public static final String GIANT = "거인화", STARS = "별의 궤도";
	public static final int GIANT_DONE = 4, STARS_DONE = 4;

	//what a stage asks for
	static final int TAKEN = 0, BOSS = 1, LOW = 2, KILL = 3, WAND = 4, WAND_KILL = 5, SCROLL = 6;

	static final int[][] GIANT_REQ = {{TAKEN, 300}, {BOSS, 1}, {LOW, 3}, {KILL, 200}};
	static final String[] GIANT_TASK = {"피해를 누적 {} 받고 살아남기", "보스 {}체 처치", "체력 30% 이하에서 {}번 살아남기", "적 {}마리 처치"};
	static final String[] GIANT_REWARD = {
			"몸집 1.1배, 최대 체력 +15%",
			"몸집 1.2배, 공격 피해 +20%, 공격이 적을 밀쳐냄",
			"걸을 때 땅울림, 곁의 적이 20% 확률로 기절",
			"거인: 몸집 1.4배, 최대 체력·공격 피해 +50%, 받는 피해 -25%, 모든 근접 공격이 주변 적을 휩쓸고 날려버림"};

	static final int[][] STARS_REQ = {{WAND, 30}, {WAND_KILL, 3}, {SCROLL, 15}, {BOSS, 2}};
	static final String[] STARS_TASK = {"마법 막대 {}번 사용", "서로 다른 막대 {}종으로 적 처치", "주문서 {}장 읽기", "보스 {}체 처치"};
	static final String[] STARS_REWARD = {
			"별 3개",
			"별 5개, 별이 적을 맞히면 마법 막대가 충전됨",
			"별 6개",
			"별의 군주: 별 8개, 별의 피해가 크게 오르고 방어력을 무시, 5턴마다 유성우"};
	static final int[] STAR_COUNT = {1, 3, 5, 6, 8};

	static final float[] STAR_REQ_FACTOR = {1f, 1f, 0.7f, 0.5f};

	static boolean holds( AugTracker t, String name ){
		return t.owned.contains(name) || name.equals(t.devoured);
	}

	public static int stage( AugTracker t, String name ){
		if (t == null || !holds(t, name)) return -1;
		Integer s = t.questStage.get(name);
		return s == null ? 0 : s;
	}

	static int progress( AugTracker t, String name ){
		Integer p = t.questProg.get(name);
		return p == null ? 0 : p;
	}

	static int[][] reqs( String name ){
		return GIANT.equals(name) ? GIANT_REQ : STARS_REQ;
	}

	static int done( String name ){
		return GIANT.equals(name) ? GIANT_DONE : STARS_DONE;
	}

	//what the current stage needs, lowered by stars
	static int needed( AugTracker t, String name, int stage ){
		int base = reqs(name)[stage][1];
		int st = Math.max(1, Math.min(3, name.equals(t.devoured) ? t.devouredStars : t.stars(name)));
		return Math.max(1, (int) Math.ceil(base * STAR_REQ_FACTOR[st]));
	}

	static void taken( AugTracker t, Aug a ){
		if (a == null || (!GIANT.equals(a.name) && !STARS.equals(a.name))) return;
		if (!t.questStage.containsKey(a.name)){
			t.questStage.put(a.name, 0);
			t.questProg.put(a.name, 0);
		}
	}

	//something happened that a stage may be waiting for
	static void event( int type, int amount, String extra ){
		AugTracker t = Augs.tracker();
		if (t == null || amount <= 0) return;
		for (String name : new String[]{GIANT, STARS}){
			int s = stage(t, name);
			if (s < 0 || s >= done(name)) continue;
			int[] req = reqs(name)[s];
			if (req[0] != type) continue;
			int p;
			if (type == WAND_KILL){
				t.questWands.add(extra);
				p = t.questWands.size();
			} else {
				p = progress(t, name) + amount;
			}
			t.questProg.put(name, p);
			if (p >= needed(t, name, s)) complete(t, name, s);
		}
	}

	private static void complete( AugTracker t, String name, int s ){
		int next = s + 1;
		t.questStage.put(name, next);
		t.questProg.put(name, 0);
		t.questWands.clear();
		t.recalc();
		Hero h = Dungeon.hero;
		if (h != null) h.updateHT(false);
		boolean last = next >= done(name);
		String reward = (GIANT.equals(name) ? GIANT_REWARD : STARS_REWARD)[s];
		Aug a = Aug.get(name);
		int col = GIANT.equals(name) ? 0xE8C070 : 0xF0D8FF;
		if (last){
			GLog.h(name + " 완성! " + reward);
			if (a != null) AugFx.force(a, col);
			AugFx.buildComplete(col);
			AugMoments.bigMoment();
			Ambience.slowMotion(0.8f);
			PostFX.flash(col, 0.25f);
			GameScene.shake(2.5f, 0.4f);
			Sample.INSTANCE.play(Assets.Sounds.MASTERY, 1f, 0.9f);
			Sample.INSTANCE.play(Assets.Sounds.AUG_LEGEND, 0.9f, 1f);
		} else {
			GLog.p(name + " " + next + "단계 완성: " + reward);
			if (a != null) AugFx.force(a, col);
			PostFX.flash(col, 0.12f);
			Sample.INSTANCE.play(Assets.Sounds.AUG_STAR, 1f, 0.9f + 0.05f * next);
		}
		if (h != null && h.sprite != null){
			h.sprite.showStatus(col, last ? (GIANT.equals(name) ? "거인!" : "별의 군주!") : name + " " + next + "단계");
			AugFx.flare(h.sprite, 8,last?48:32, col, last ? 2.4f : 1.4f);
			h.sprite.emitter().burst(Speck.factory(Speck.STAR), last ? 24 : 10);
			PointF c = h.sprite.center();
			Atmosphere.burst(c.x, c.y, last ? 8f : 5f, 1f, col, last ? 2.4f : 1.2f);
		}
	}

	// ================================================================ events from the game

	static void afterDamage( Hero h, int taken, int before ){
		if (taken > 0) event(TAKEN, taken, null);
		if (h.isAlive() && before > h.HT * 0.3f && h.HP <= h.HT * 0.3f) event(LOW, 1, null);
	}

	static void onBossKill(){
		event(BOSS, 1, null);
	}

	static void onKill( Hero h, AugTracker t, Mob m, AugX.Snap s ){
		event(KILL, 1, null);
		if (s != null && s.cause instanceof Wand) event(WAND_KILL, 1, s.cause.getClass().getSimpleName());
	}

	static void onWandUsed(){
		event(WAND, 1, null);
	}

	static void onScrollRead(){
		event(SCROLL, 1, null);
	}

	// ================================================================ effects

	static void addStats( AugTracker t, EnumMap<St, Float> into ){
		int g = stage(t, GIANT);
		switch (g){
			case 0: put(into, St.HP_PCT, 0.05f); break;
			case 1: put(into, St.HP_PCT, 0.15f); break;
			case 2: case 3:
				put(into, St.HP_PCT, 0.15f); put(into, St.DMG_PCT, 0.20f); put(into, St.P_KNOCK, 0.35f);
				break;
			case 4:
				put(into, St.HP_PCT, 0.50f); put(into, St.DMG_PCT, 0.50f); put(into, St.TAKEN, 0.25f);
				put(into, St.CLEAVE, 1f); put(into, St.P_KNOCK, 1f);
				break;
			default: break;
		}
	}

	private static void put( EnumMap<St, Float> into, St s, float v ){
		Float old = into.get(s);
		into.put(s, (old == null ? 0f : old) + v);
	}

	static float giantScale( int stage ){
		switch (stage){
			case 1: return 1.1f;
			case 2: case 3: return 1.2f;
			case 4: return 1.4f;
			default: return 1f;
		}
	}

	static int starCount( AugTracker t ){
		int s = stage(t, STARS);
		return s < 0 ? 0 : STAR_COUNT[Math.min(s, STAR_COUNT.length - 1)];
	}

	private static int starDamage( AugTracker t ){
		Hero h = Dungeon.hero;
		int lvl = h == null ? 1 : h.lvl;
		if (stage(t, STARS) >= STARS_DONE) return Math.round(2 + lvl * 0.6f);
		return 1 + lvl / 3;
	}

	static void onHeroMove( Hero h, AugTracker t, int to ){
		if (stage(t, GIANT) < 3) return;
		if (Dungeon.level.heroFOV[to]) CellEmitter.get(to).burst(Speck.factory(Speck.DUST), 2);
		for (int n : PathFinder.NEIGHBOURS8){
			final Char c = Actor.findChar(to + n);
			if (c == null || c.alignment != Char.Alignment.ENEMY || !c.isAlive() || AugY.boss(c)) continue;
			if (Random.Float() < 0.2f){
				AugY.later(new Runnable() {
					@Override
					public void run() {
						if (!c.isAlive()) return;
						Buff.prolong(c, Paralysis.class, 1f);
						if (c.sprite != null && c.sprite.visible) c.sprite.showStatus(0xE8C070, "땅울림");
					}
				});
			}
		}
		if (stage(t, GIANT) >= GIANT_DONE) GameScene.shake(0.4f, 0.1f);
	}

	private static int volleyTimer = 0;

	static void everyTurn( Hero h, AugTracker t ){
		int n = starCount(t);
		if (n <= 0) return;
		ArrayList<Char> near = new ArrayList<>();
		for (int d : PathFinder.NEIGHBOURS8){
			Char c = Actor.findChar(h.pos + d);
			if (c != null && c.alignment == Char.Alignment.ENEMY && c.isAlive()) near.add(c);
		}
		int dmg = starDamage(t);
		boolean charge = stage(t, STARS) >= 2;
		if (!near.isEmpty()){
			for (int i = 0; i < n; i++){
				Char c = near.get(i % near.size());
				if (!c.isAlive()) continue;
				strike(i, c);
				if (c.sprite != null && c.sprite.visible) c.sprite.centerEmitter().burst(Speck.factory(Speck.STAR), 2);
				c.damage(dmg, STAR_SRC);
				if (charge) chargeWand(h);
			}
			Sample.INSTANCE.play(Assets.Sounds.ZAP, 0.35f, 1.6f);
		}

		//the lord of stars: a meteor shower every fifth turn
		if (stage(t, STARS) >= STARS_DONE){
			volleyTimer++;
			if (volleyTimer >= 5){
				volleyTimer = 0;
				volley(h, n, dmg);
			}
		}
	}

	public static final Augs.Src STAR_SRC = new Augs.Src("stars");

	private static void chargeWand( Hero h ){
		for (Item i : h.belongings){
			if (i instanceof Wand){
				Wand w = (Wand) i;
				if (w.curCharges < w.maxCharges){
					w.partialCharge += 0.05f;
					if (w.partialCharge >= 1f){
						w.partialCharge -= 1f;
						w.curCharges++;
						w.updateQuickslot();
					}
					return;
				}
			}
		}
	}

	private static void volley( final Hero h, final int n, final int dmg ){
		final ArrayList<Char> targets = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs){
			if (m.alignment != Char.Alignment.ENEMY || !m.isAlive() || !Dungeon.level.heroFOV[m.pos]) continue;
			if (Dungeon.level.distance(h.pos, m.pos) > 7) continue;
			if (new Ballistica(h.pos, m.pos, Ballistica.MAGIC_BOLT).collisionPos != m.pos) continue;
			targets.add(m);
		}
		if (targets.isEmpty()) return;
		AugFx.notify(St.Q_STARS);
		Actor.add(new Actor() {
			{ actPriority = VFX_PRIO; }
			@Override
			protected boolean act() {
				Actor.remove(this);
				if (h.sprite == null || h.sprite.parent == null){
					for (int i = 0; i < n; i++){
						Char c = targets.get(i % targets.size());
						if (c.isAlive()) c.damage(dmg, STAR_SRC);
					}
					return true;
				}
				final Actor self = this;
				final int[] left = {n};
				Sample.INSTANCE.play(Assets.Sounds.ZAP, 0.8f, 1.3f);
				PostFX.flash(0xF0E0FF, 0.08f);
				for (int i = 0; i < n; i++){
					final Char c = targets.get(i % targets.size());
					((MagicMissile) h.sprite.parent.recycle(MagicMissile.class)).reset(MagicMissile.LIGHT_MISSILE, h.sprite, c.pos, new Callback() {
						@Override
						public void call() {
							if (c.isAlive()){
								if (c.sprite != null) c.sprite.centerEmitter().burst(Speck.factory(Speck.STAR), 4);
								c.damage(dmg, STAR_SRC);
							}
							if (--left[0] <= 0) self.next();
						}
					});
				}
				return false;
			}
		});
	}

	// ================================================================ what is drawn every frame

	//a star dashing at its target: when, and where to
	private static final float[] strikeT = new float[8];
	private static final PointF[] strikeTo = new PointF[8];

	private static void strike( int i, Char c ){
		if (i < 0 || i >= strikeT.length || c.sprite == null) return;
		strikeTo[i] = c.sprite.center();
		strikeT[i] = 0.28f;
	}

	private static Orbit orbit = null;
	private static boolean scaled = false;
	private static float clock = 0;

	static void frame( Hero h, AugTracker t ){
		CharSprite sp = h.sprite;
		clock += Game.elapsed;

		//the giant
		int g = stage(t, GIANT);
		float want = giantScale(g);
		if (want != 1f || scaled){
			sp.origin.set(sp.width / 2f, sp.height);
			if (sp.scale.y != want){
				//grows smoothly instead of popping
				float s = sp.scale.y + Math.signum(want - sp.scale.y) * Math.min(Math.abs(want - sp.scale.y), Game.elapsed * 0.5f);
				sp.scale.set(s);
			}
			scaled = want != 1f || sp.scale.y != 1f;
		}

		//the stars
		int n = starCount(t);
		boolean show = n > 0 && sp.visible && h.isAlive() && h.invisible <= 0;
		if (!show){
			if (orbit != null){
				orbit.killAndErase();
				orbit = null;
			}
			return;
		}
		if (orbit == null || orbit.parent != sp.parent || orbit.count != n){
			if (orbit != null) orbit.killAndErase();
			orbit = new Orbit(n);
			sp.parent.add(orbit);
		}
		PointF c = sp.center();
		for (int i = 0; i < n; i++){
			Image img = orbit.stars[i];
			float ang = clock * 1.7f + i * (float) (Math.PI * 2 / n);
			float r = 10f * sp.scale.y;
			float x = c.x + (float) Math.cos(ang) * r;
			float y = c.y + (float) Math.sin(ang) * r * 0.55f - 2;
			if (strikeT[i] > 0 && strikeTo[i] != null){
				strikeT[i] -= Game.elapsed;
				float k = (float) Math.sin(Math.max(0, strikeT[i]) / 0.28f * Math.PI);
				x += (strikeTo[i].x - x) * k;
				y += (strikeTo[i].y - y) * k;
			}
			img.x = x - img.width() / 2f;
			img.y = y - img.height() / 2f;
			float pulse = 0.85f + 0.15f * (float) Math.sin(clock * 5 + i);
			img.scale.set(pulse);
			img.alpha(0.75f + 0.25f * pulse);
		}
	}

	private static class Orbit extends Group {
		final int count;
		final Image[] stars;

		Orbit( int n ){
			count = n;
			stars = new Image[n];
			TextureFilm film = new TextureFilm(Assets.Effects.SPECKS, 7, 7);
			for (int i = 0; i < n; i++){
				Image img = new Image(Assets.Effects.SPECKS);
				img.frame(film.get(Speck.STAR));
				img.origin.set(3.5f, 3.5f);
				img.hardlight(1f, 0.92f, 0.65f);
				stars[i] = img;
				add(img);
			}
		}
	}

	// ================================================================ text

	static boolean isQuest( Aug a ){
		return a != null && (GIANT.equals(a.name) || STARS.equals(a.name));
	}

	//the payoff, short enough for a card
	static String cardLine( String name ){
		return GIANT.equals(name)
				? "완성 시: 몸집 1.4배, 체력·피해 +50%, 받는 피해 -25%, 근접 공격이 주변을 휩씀"
				: "완성 시: 별 8개, 별 피해 크게 증가, 5턴마다 유성우";
	}

	//the stages and what they give, for the info window
	static String overview( AugTracker t, String name ){
		int[][] req = reqs(name);
		String[] task = GIANT.equals(name) ? GIANT_TASK : STARS_TASK;
		String[] reward = GIANT.equals(name) ? GIANT_REWARD : STARS_REWARD;
		int s = t == null ? -1 : stage(t, name);
		StringBuilder sb = new StringBuilder();
		sb.append(GIANT.equals(name) ? "얻는 즉시: 최대 체력 +5%" : "얻는 즉시: 별 1개");
		for (int i = 0; i < req.length; i++){
			int need = t == null ? req[i][1] : needed(t, name, i);
			sb.append("\n\n");
			boolean cur = i == s, past = s > i;
			String head = (i == req.length - 1 ? "완성" : (i + 1) + "단계") + ": " + task[i].replace("{}", Integer.toString(need));
			if (cur) head += " (" + Math.min(need, t.questProg.get(name) == null ? 0 : t.questProg.get(name)) + "/" + need + ")";
			if (past) head += " (완료)";
			sb.append(cur ? "_" + head + "_" : head);
			sb.append("\n  ").append(reward[i]);
		}
		return sb.toString();
	}

	//one line per held late bloomer, for the status buff
	static String status( AugTracker t ){
		StringBuilder sb = new StringBuilder();
		for (String name : new String[]{GIANT, STARS}){
			int s = stage(t, name);
			if (s < 0) continue;
			if (sb.length() > 0) sb.append('\n');
			if (s >= done(name)){
				sb.append("- ").append(name).append(": 완성");
			} else {
				String[] task = GIANT.equals(name) ? GIANT_TASK : STARS_TASK;
				int need = needed(t, name, s);
				sb.append("- ").append(name).append(" ").append(s + 1).append("단계: ")
						.append(task[s].replace("{}", Integer.toString(need)))
						.append(" (").append(Math.min(need, progress(t, name))).append("/").append(need).append(")");
			}
		}
		return sb.toString();
	}
}
