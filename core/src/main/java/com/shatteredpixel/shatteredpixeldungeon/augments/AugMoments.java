/*
 * The moments around the augments rather than the augments themselves:
 * - records kept through the run (augment damage, triggers, best critical hit, big moments)
 * - a short summary banner when the hero leaves a floor
 * - the highlights of the run, shown when it ends
 * - little sequences of sounds, so rare things sound rarer
 */

package com.shatteredpixel.shatteredpixeldungeon.augments;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.Ambience;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.Atmosphere;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.PostFX;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Callback;

public class AugMoments {

	// ================================================================ records

	//damage an enemy is taking right now
	public static void dealt( Char ch, int dmg, Object src, boolean crit ){
		AugTracker t = Augs.tracker();
		if (t == null || dmg <= 0 || ch.alignment != Char.Alignment.ENEMY) return;
		AugY.shared(ch, dmg, src);
		if (src instanceof Augs.Src){
			t.floorDmg += dmg;
			t.runDmg += dmg;
		}
		if (crit){
			t.floorCrit = Math.max(t.floorCrit, dmg);
			if (dmg > t.bestCrit){
				t.bestCrit = dmg;
				t.bestCritFloor = Dungeon.depth;
			}
		}
	}

	static void proc(){
		AugTracker t = Augs.tracker();
		if (t == null) return;
		t.floorProcs++;
		t.runProcs++;
	}

	static void kill(){
		AugTracker t = Augs.tracker();
		if (t != null) t.floorKills++;
	}

	static void bigMoment(){
		AugTracker t = Augs.tracker();
		if (t != null) t.bigMoments++;
	}

	// ================================================================ floor summary

	static void floorSummary( AugTracker t, int prevKey ){
		final int dmg = t.floorDmg, procs = t.floorProcs, kills = t.floorKills, crit = t.floorCrit;
		t.floorDmg = t.floorProcs = t.floorKills = t.floorCrit = 0;
		if (dmg + procs + kills == 0) return;
		final int depth = prevKey / 100;
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (!(Game.scene() instanceof GameScene)) return;
				Game.scene().addToFront(new Banner(depth, dmg, procs, kills, crit));
			}
		});
	}

	//a framed strip near the top of the screen that fades in, stays a moment and fades out
	public static class Banner extends Component {

		private NinePatch bg;
		private RenderedTextBlock head, body;
		private float t = 0;

		Banner( int depth, int dmg, int procs, int kills, int crit ){
			super();
			camera = PixelScene.uiCamera;
			head.text(depth + "층 결산");
			StringBuilder sb = new StringBuilder();
			sb.append("증강 추가 피해 _").append(dmg).append("_");
			sb.append("   발동 _").append(procs).append("_회");
			sb.append("\n처치 _").append(kills).append("_");
			if (crit > 0) sb.append("   최대 치명타 _").append(crit).append("_");
			body.text(sb.toString());
			float w = Math.max(head.width(), body.width()) + 16;
			float h = head.height() + body.height() + 13;
			setRect((PixelScene.uiCamera.width - w) / 2f, 34, w, h);
			alpha(0);
		}

		@Override
		protected void createChildren() {
			bg = Chrome.get(Chrome.Type.TOAST_TR);
			add(bg);
			head = PixelScene.renderTextBlock(8);
			head.hardlight(0xFFD45A);
			add(head);
			body = PixelScene.renderTextBlock(7);
			body.align(RenderedTextBlock.CENTER_ALIGN);
			add(body);
		}

		@Override
		protected void layout() {
			bg.x = x;
			bg.y = y;
			bg.size(width, height);
			head.setPos(x + (width - head.width()) / 2f, y + 5);
			body.setPos(x + (width - body.width()) / 2f, head.bottom() + 3);
			PixelScene.align(head);
			PixelScene.align(body);
		}

		private void alpha( float a ){
			bg.alpha(a * 0.9f);
			head.alpha(a);
			body.alpha(a);
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			float a;
			if (t < 0.35f) a = t / 0.35f;
			else if (t < 4f) a = 1f;
			else a = Math.max(0f, 1f - (t - 4f) / 0.7f);
			alpha(a);
			if (t > 4.8f) killAndErase();
		}
	}

	// ================================================================ a legend aimed for and reached

	static void goalComplete( final Aug a ){
		final Hero h = Dungeon.hero;
		GLog.h("목표 달성: " + a.name + "!");
		seq(Assets.Sounds.AUG_LEGEND, 1f, 1f, 0f,
				Assets.Sounds.MASTERY, 0.8f, 1f, 0.45f,
				Assets.Sounds.AUG_STAR, 1f, 1.12f, 0.9f,
				Assets.Sounds.AUG_STAR, 1f, 1.33f, 1.15f,
				Assets.Sounds.LEVELUP, 0.8f, 1.2f, 1.4f);
		PostFX.flash(0xFFB060, 0.3f);
		Ambience.slowMotion(1f);
		if (h != null && h.sprite != null){
			AugFx.flare(h.sprite, 12,60, 0xFFB050, 3f);
			h.sprite.emitter().burst(Speck.factory(Speck.STAR), 30);
			com.watabou.utils.PointF c = h.sprite.center();
			Atmosphere.burst(c.x, c.y, 9f, 1f, 0xFFB060, 3f);
		}
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (!(Game.scene() instanceof GameScene)) return;
				Game.scene().addToFront(new GoalBanner(a.name));
			}
		});
	}

	//a large strip in the middle of the screen
	public static class GoalBanner extends Component {
		private NinePatch bg;
		private RenderedTextBlock head, body;
		private float t = 0;

		GoalBanner( String name ){
			super();
			camera = PixelScene.uiCamera;
			body.text(name);
			float w = Math.max(head.width(), body.width()) + 24;
			float hh = head.height() + body.height() + 16;
			setRect((PixelScene.uiCamera.width - w) / 2f, PixelScene.uiCamera.height * 0.3f, w, hh);
			alpha(0);
		}

		@Override
		protected void createChildren() {
			bg = Chrome.get(Chrome.Type.TOAST_TR);
			add(bg);
			head = PixelScene.renderTextBlock("목표 달성!", 12);
			head.hardlight(0xFFB050);
			add(head);
			body = PixelScene.renderTextBlock(9);
			body.hardlight(0xFFE0B0);
			add(body);
		}

		@Override
		protected void layout() {
			bg.x = x; bg.y = y;
			bg.size(width, height);
			head.setPos(x + (width - head.width()) / 2f, y + 6);
			body.setPos(x + (width - body.width()) / 2f, head.bottom() + 4);
			PixelScene.align(head);
			PixelScene.align(body);
		}

		private void alpha( float a ){
			bg.alpha(a * 0.92f);
			head.alpha(a);
			body.alpha(a);
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			float a;
			if (t < 0.4f) a = t / 0.4f;
			else if (t < 3.6f) a = 1f;
			else a = Math.max(0f, 1f - (t - 3.6f) / 0.8f);
			alpha(a);
			if (t > 4.4f) killAndErase();
		}
	}

	// ================================================================ the end of the run

	//called every frame from the aura gizmo
	private static float deadTime = 0;
	//the hero's buffs are gone once they die, so the record is kept from while they lived
	private static AugTracker last = null;

	static void watchDeath(){
		if (Dungeon.hero == null) return;
		if (Dungeon.hero.isAlive()){
			deadTime = 0;
			AugTracker t = Augs.tracker();
			if (t != null) last = t;
			return;
		}
		AugTracker t = last;
		if (t == null || t.highlightsShown) return;
		deadTime += Game.elapsed;
		if (deadTime > 2.6f && !GameScene.showingWindow()){
			t.highlightsShown = true;
			GameScene.show(new WndHighlights(t));
		}
	}

	public static class WndHighlights extends Window {
		public WndHighlights(){
			this(Augs.tracker());
		}

		public WndHighlights( AugTracker t ){
			int w = Math.min(140, (int) PixelScene.uiCamera.width - 16);
			RenderedTextBlock title = PixelScene.renderTextBlock("이번 판의 증강", 9);
			title.hardlight(TITLE_COLOR);
			add(title);
			title.setPos((w - title.width()) / 2f, 0);

			StringBuilder sb = new StringBuilder();
			if (t != null){
				sb.append("증강 _").append(t.owned.size()).append("_개");
				if (t.stars3 > 0) sb.append(",  3성 _").append(t.stars3).append("_개");
				int awake = 0;
				for (AugSet s : AugSet.values()) if (t.count(s) >= AugSet.THRESHOLDS[AugSet.THRESHOLDS.length - 1]) awake++;
				if (awake > 0) sb.append(",  각성 _").append(awake).append("_계열");
				for (String l : t.legendLog) sb.append("\n전설: _").append(l).append("_");
				if (t.goalsDone > 0) sb.append("\n목표 달성 _").append(t.goalsDone).append("_번");
				if (t.bestCrit > 0) sb.append("\n최대 치명타 _").append(t.bestCrit).append("_ (").append(t.bestCritFloor).append("층)");
				sb.append("\n증강이 더한 피해 _").append(t.runDmg).append("_");
				sb.append("\n증강 발동 _").append(t.runProcs).append("_회");
				sb.append("\n처치 _").append(t.cnt[AugGrow.KILLS]).append("_");
				if (t.bigMoments > 0) sb.append("\n잭팟·대폭발 같은 큰 순간 _").append(t.bigMoments).append("_번");
				String grow = AugGrow.biggest(t);
				if (grow != null) sb.append("\n가장 크게 자란 증강: ").append(grow);
				//the strongest family
				AugSet best = null;
				for (AugSet s : AugSet.values()) if (best == null || t.count(s) > t.count(best)) best = s;
				if (best != null && t.count(best) > 0) sb.append("\n중심 계열: _").append(best.title).append("_ (").append(t.count(best)).append(")");
			}
			RenderedTextBlock body = PixelScene.renderTextBlock(sb.toString(), 7);
			body.maxWidth(w);
			body.setPos(0, title.bottom() + 6);
			add(body);

			if (Dungeon.hero != null && Dungeon.hero.isAlive()){
				RedButton list = new RedButton("증강 목록"){
					@Override
					protected void onClick() {
						GameScene.show(new WndAugment.WndOwned());
					}
				};
				list.setRect(0, body.bottom() + 6, w, 16);
				add(list);
				resize(w, (int) list.bottom());
			} else {
				resize(w, (int) body.bottom() + 2);
			}
		}
	}

	// ================================================================ sound

	//plays a few sounds one after another
	static void seq( final Object... notes ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (Game.scene() == null) return;
				Game.scene().add(new Seq(notes));
			}
		});
	}

	private static class Seq extends Gizmo {
		private final Object[] notes; //asset, volume, pitch, time
		private float t = 0;
		private int next = 0;

		Seq( Object[] notes ){
			this.notes = notes;
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			while (next < notes.length && t >= ((Number) notes[next + 3]).floatValue()){
				Sample.INSTANCE.play((String) notes[next], ((Number) notes[next + 1]).floatValue(), ((Number) notes[next + 2]).floatValue());
				next += 4;
			}
			if (next >= notes.length) killAndErase();
		}
	}

	//the sound of a card of a given tier turning over (the tier sound files are made for this mod)
	static void reveal( int tier ){
		switch (tier){
			case Aug.SILVER:
				Sample.INSTANCE.play(Assets.Sounds.AUG_SILVER, 0.7f);
				break;
			case Aug.GOLD:
				Sample.INSTANCE.play(Assets.Sounds.AUG_GOLD, 0.8f);
				break;
			case Aug.PRISM:
				Sample.INSTANCE.play(Assets.Sounds.AUG_PRISM, 0.85f);
				break;
			default:
				Sample.INSTANCE.play(Assets.Sounds.AUG_LEGEND, 1f);
				break;
		}
	}
}
