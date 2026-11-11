/*
 * Augment visuals that live in the dungeon itself:
 * - the awakening aura of a completed family (6 augments)
 * - the little icon that pops above the hero when one of their augments goes off
 */

package com.shatteredpixel.shatteredpixeldungeon.augments;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.Atmosphere;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PoisonParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SacrificialParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShaftParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.WindParticle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Halo;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.EnumMap;

public class AugFx {

	// ================================================================ awakening aura

	private static Emitter.Factory factoryFor( AugSet set ){
		switch (set){
			case BLOOD:  return BloodParticle.FACTORY;
			case FLAME:  return FlameParticle.FACTORY;
			case FROST:  return SnowParticle.FACTORY;
			case STORM:  return SparkParticle.STATIC;
			case VENOM:  return PoisonParticle.SPLASH;
			case SHADOW: return ShadowParticle.UP;
			case GUARD:  return ShaftParticle.FACTORY;
			case ARCANE: return PurpleParticle.MISSILE;
			case HUNT:   return WindParticle.FACTORY;
			case GREED:  return Speck.factory(Speck.STAR);
			case WILD:   return LeafParticle.GENERAL;
			case FURY: default: return SacrificialParticle.FACTORY;
		}
	}

	//how often a particle appears, tuned per type so none of them overwhelms the hero sprite
	private static float intervalFor( AugSet set ){
		switch (set){
			case FLAME: case SHADOW: return 0.06f;
			case FROST: case ARCANE: case FURY: return 0.08f;
			case BLOOD: case STORM: case VENOM: return 0.12f;
			default: return 0.16f;
		}
	}

	//added to the game scene, keeps one emitter per awakened family around the hero
	public static class Aura extends Gizmo {

		private final EnumMap<AugSet, Emitter> emitters = new EnumMap<>(AugSet.class);
		//one glow under the hero, whatever the number of awakened families, so it never piles up
		private Glow glow;
		private float clock = 0;
		private float pulse = 0;

		public Aura(){
			//a fresh scene: forget pops that belonged to the old one
			pops.clear();
			lastShown.clear();
		}

		@Override
		public void update() {
			super.update();
			AugMoments.watchDeath();
			AugTracker t = Augs.tracker();
			Hero h = Dungeon.hero;
			if (t == null || h == null || h.sprite == null || h.sprite.parent == null) return;
			CharSprite sp = h.sprite;
			AugQuest.frame(h, t);
			boolean show = sp.visible && h.isAlive() && h.invisible <= 0
					&& com.shatteredpixel.shatteredpixeldungeon.atmosphere.Ambience.enabled();

			pulse -= Game.elapsed;
			clock += Game.elapsed;
			boolean doPulse = pulse <= 0;
			if (doPulse) pulse = 0.5f;

			for (AugSet set : AugSet.values()){
				boolean awake = t.count(set) >= AugSet.THRESHOLDS[AugSet.THRESHOLDS.length - 1];
				Emitter e = emitters.get(set);
				if (awake && e == null){
					e = new Emitter();
					e.pour(factoryFor(set), intervalFor(set));
					sp.parent.add(e);
					emitters.put(set, e);
				}
				if (e == null) continue;
				if (e.parent != sp.parent){
					//new sprite after a level change
					e.killAndErase();
					emitters.remove(set);
					continue;
				}
				e.on = awake && show;
				e.pos(sp.x + 2, sp.y + sp.height() * 0.25f, sp.width() - 4, sp.height() * 0.7f);
				if (awake && show && doPulse){
					//a faint glow of the family's colour around the hero
					PointF c = sp.center();
					Atmosphere.burst(c.x, c.y, 2.2f, 0.8f, set.color, 0.9f);
				}
			}

			//a soft glow on the floor beneath the hero, drifting through the awakened colours
			ArrayList<Integer> cols = new ArrayList<>();
			for (AugSet set : AugSet.values()){
				if (t.count(set) >= AugSet.THRESHOLDS[AugSet.THRESHOLDS.length - 1]) cols.add(set.color);
			}
			if (glow != null && glow.parent != sp.parent){
				glow.killAndErase();
				glow = null;
			}
			if (cols.isEmpty()){
				if (glow != null) glow.visible = false;
				return;
			}
			if (glow == null){
				glow = new Glow(0xFFFFFF);
				sp.parent.addToBack(glow);
			}
			float f = (clock * 0.25f) % cols.size();
			int a = cols.get((int) f), b = cols.get(((int) f + 1) % cols.size());
			float k = f - (int) f;
			glow.hardlight(lerp(a, b, k));
			glow.visible = show;
			PointF c = sp.center();
			glow.point(c.x, sp.y + sp.height() - 2);
			glow.alpha(0.32f + 0.1f * (float)Math.sin(clock * 2.2f));
		}

		private static int lerp( int a, int b, float k ){
			int r = (int)(((a >> 16) & 0xFF) * (1 - k) + ((b >> 16) & 0xFF) * k);
			int g = (int)(((a >> 8) & 0xFF) * (1 - k) + ((b >> 8) & 0xFF) * k);
			int bl = (int)((a & 0xFF) * (1 - k) + (b & 0xFF) * k);
			return (r << 16) | (g << 8) | bl;
		}

		@Override
		public void destroy() {
			super.destroy();
			for (Emitter e : emitters.values()) e.killAndErase();
			emitters.clear();
			if (glow != null) glow.killAndErase();
			glow = null;
		}
	}

	//flattened, additive halo
	private static class Glow extends Halo {
		Glow( int color ){
			super(14, color, 1f);
			scale.y *= 0.55f;
		}

		@Override
		public void draw() {
			com.watabou.glwrap.Blending.setLightMode();
			super.draw();
			com.watabou.glwrap.Blending.setNormalMode();
		}
	}

	// ================================================================ "this augment just went off"

	private static final EnumMap<St, Long> lastShown = new EnumMap<>(St.class);
	private static final ArrayList<Pop> pops = new ArrayList<>();
	private static long lastAny = 0;

	//effects that go off all the time stay quiet: an icon for everything would make every icon mean nothing
	private static final java.util.EnumSet<St> FREQUENT = java.util.EnumSet.of(
			St.U_STATIC, St.U_CHARGE, St.U_BETRAY, St.U_EMBERARMOR, St.U_FROSTARMOR, St.U_GOLDSHOT,
			St.U_MANACYCLE, St.U_ANTIDOTE, St.U_THRILL, St.U_COLDHEAL, St.U_STEADFAST, St.U_DOORHASTE,
			St.U_WALLBACK, St.U_GUST,
			//the newer ones that go off with nearly every blow or step
			St.U_FLURRY, St.U_GALE, St.U_FINISHER, St.U_SOULCHAIN, St.U_BOOMERANG, St.U_CALM, St.U_SPLIT,
			St.L_TWIN, St.L_MOUNTAIN, St.L_ICEDANCER, St.L_GARDEN);

	static boolean quiet( St s, AugTracker t ){
		if (FREQUENT.contains(s)) return true;
		String n = s.name();
		if (n.startsWith("AURA_")) return true;
		//a chance effect that triggers often enough is part of the rhythm, not news
		if ((n.startsWith("P_") || n.startsWith("K_") || n.startsWith("H_")) && t.total[s.ordinal()] >= 0.10f) return true;
		return false;
	}

	//a flare on a sprite; nothing if the sprite has already left the scene (a dying enemy's can, a moment early)
	public static void flare( com.watabou.noosa.Visual sprite, int rays, float radius, int color, float duration ){
		if (sprite == null || sprite.parent == null) return;
		new com.shatteredpixel.shatteredpixeldungeon.effects.Flare(rays, radius).color(color, true).show(sprite, duration);
	}

	public static void notify( St s ){
		AugMoments.proc();
		if (!com.shatteredpixel.shatteredpixeldungeon.SPDSettings.augmentPops()) return;
		Hero h = Dungeon.hero;
		AugTracker t = Augs.tracker();
		if (t != null && quiet(s, t)) return;
		if (t == null || h == null || h.sprite == null || h.sprite.parent == null || !h.sprite.visible) return;
		long now = System.currentTimeMillis();
		Long last = lastShown.get(s);
		//keep it to a gentle trickle: one icon per effect every few seconds, at most two at once
		if (last != null && now - last < 2500) return;
		if (pops.size() >= 2 || now - lastAny < 400) return;
		lastShown.put(s, now);
		lastAny = now;

		Aug src = sourceOf(t, s);
		Image icon;
		int glow;
		if (src != null){
			icon = new ItemSprite(src.icon);
			glow = src.tier == Aug.LEGEND ? 0xFF8A50 : (src.tier == Aug.PRISM ? 0xE8C8FF : (src.tier == Aug.GOLD ? 0xFFD870 : 0xD8E0EA));
		} else {
			AugSet set = setSourceOf(t, s);
			icon = AugArt.emblem();
			glow = set != null ? set.color : 0xFFD45A;
			AugArt.tint(icon, glow);
		}
		Pop p = new Pop(h.sprite, icon, glow, pops.size());
		pops.add(p);
	}

	//shows an augment's icon regardless of the usual throttling, for rare big moments
	public static void force( Aug a, int glow ){
		Hero h = Dungeon.hero;
		if (a == null || h == null || h.sprite == null || h.sprite.parent == null || !h.sprite.visible) return;
		lastAny = System.currentTimeMillis();
		Pop p = new Pop(h.sprite, new ItemSprite(a.icon), glow, pops.isEmpty() ? 0 : 1);
		pops.add(p);
	}

	//a growing augment reached a milestone
	public static void milestone( Aug a, String text ){
		Hero h = Dungeon.hero;
		if (a == null || h == null) return;
		com.shatteredpixel.shatteredpixeldungeon.utils.GLog.p(a.name + " 성장: " + text);
		if (h.sprite == null || h.sprite.parent == null) return;
		int col = a.set != null ? a.set.color : 0xFFD45A;
		force(a, col);
		h.sprite.showStatus(0xFFD870, a.name + " 성장!");
		new com.shatteredpixel.shatteredpixeldungeon.effects.Flare(6, 24).color(col, true).show(h.sprite, 1.2f);
		PointF c = h.sprite.center();
		Atmosphere.burst(c.x, c.y, 4f, 0.85f, col, 1.1f);
		h.sprite.emitter().burst(Speck.factory(Speck.STAR), 6);
		com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.CHARGEUP, 0.7f, 1.1f);
	}

	//a build just came together: legend taken or a family awakened
	public static void buildComplete( int color ){
		Hero h = Dungeon.hero;
		if (h == null || h.sprite == null || h.sprite.parent == null) return;
		com.shatteredpixel.shatteredpixeldungeon.atmosphere.Ambience.slowMotion(1.1f);
		com.shatteredpixel.shatteredpixeldungeon.atmosphere.PostFX.flash(color, 0.28f);
		new com.shatteredpixel.shatteredpixeldungeon.effects.Flare(12, 64).color(color, true).show(h.sprite, 2.6f);
		new com.shatteredpixel.shatteredpixeldungeon.effects.Flare(6, 40).color(0xFFFFFF, true).show(h.sprite, 1.4f);
		PointF c = h.sprite.center();
		Atmosphere.burst(c.x, c.y, 8f, 1f, color, 2.8f);
		h.sprite.emitter().burst(Speck.factory(Speck.STAR), 16);
		GameScene.shake(1.5f, 0.4f);
	}

	//the strongest held augment carrying an effect
	private static Aug sourceOf( AugTracker t, St s ){
		Aug best = null;
		for (String n : t.owned){
			Aug a = Aug.get(n);
			if (a == null || !a.stats.containsKey(s) || a.stats.get(s) <= 0) continue;
			if (best == null || a.tier > best.tier) best = a;
		}
		return best;
	}

	private static AugSet setSourceOf( AugTracker t, St s ){
		for (AugSet set : AugSet.values()){
			int r = AugSet.reached(t.count(set));
			for (int i = 0; i < r; i++) if (set.bonus(i).containsKey(s)) return set;
		}
		return null;
	}

	private static class Pop extends Gizmo {
		private final CharSprite target;
		private final Image icon, badge;
		private final Halo halo;
		private final float offset;
		private float t = 0;

		Pop( CharSprite target, Image icon, int glow, int slot ){
			this.target = target;
			this.icon = icon;
			offset = slot == 0 ? 0 : 22f;
			halo = new Halo(13, glow, 0.5f);
			badge = AugArt.badge(glow);
			GameScene.effectOverFog(halo);
			GameScene.effectOverFog(badge);
			GameScene.effectOverFog(icon);
			target.parent.add(this);
			place();
		}

		private void place(){
			float rise = Math.min(1f, t / 0.25f);
			rise = rise * (2f - rise);
			float cx = target.x + target.width() / 2f + offset;
			//above the hero's head, clear of the tile row right above them
			float cy = target.y - 14 - 10 * rise - t * 3f;
			badge.x = cx - badge.width() / 2f;
			badge.y = cy - badge.height() / 2f;
			icon.x = cx - icon.width() / 2f;
			icon.y = cy - icon.height() / 2f;
			com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.align(badge);
			com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.align(icon);
			halo.point(cx, cy);
			float a;
			if (t < 0.12f) a = t / 0.12f;
			else if (t > 0.95f) a = Math.max(0f, 1f - (t - 0.95f) / 0.35f);
			else a = 1f;
			icon.alpha(a);
			badge.alpha(a);
			halo.alpha(0.4f * a);
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			if (t >= 1.3f || target.parent == null){
				icon.killAndErase();
				badge.killAndErase();
				halo.killAndErase();
				pops.remove(this);
				killAndErase();
				return;
			}
			place();
		}
	}
}
