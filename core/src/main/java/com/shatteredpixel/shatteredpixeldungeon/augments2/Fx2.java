/*
 * Visuals for the Augment II challenge: skill effects, the pixel art for the skill buttons,
 * mines lying on the floor and the kill streak banner.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments2;

import com.badlogic.gdx.graphics.Pixmap;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.atmosphere.Atmosphere;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.Wound;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.glwrap.Blending;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Fx2 {

	private static void px( Pixmap p, int x, int y, int argb ){
		//pixmaps take RGBA
		p.drawPixel(x, y, (argb << 8) | ((argb >>> 24) & 0xFF));
	}

	public static Callback idle( final CharSprite s ){
		return new Callback() {
			@Override public void call() {
				s.idle();
			}
		};
	}

	// ================================================================ skill button art

	public static final int SLOT = 22;

	//outline, light edge, dark edge, panel top, panel bottom
	private static final int[][] SLOT_PAL = {
			{0xFF14151A, 0xFF8A919E, 0xFF4A4F5A, 0xFF2C2E37, 0xFF1D1E25},
			{0xFF1E1204, 0xFFE2AA3A, 0xFF86560F, 0xFF30260F, 0xFF1E1709},
	};

	//a bevelled socket the skill icon sits in; the ultimate's is gold
	public static Image slotFrame( boolean ult ){
		String key = "aug2_slot_" + (ult ? 1 : 0);
		if (!TextureCache.contains(key)){
			SmartTexture tx = TextureCache.create(key, SLOT, SLOT);
			Pixmap p = tx.bitmap;
			p.setBlending(Pixmap.Blending.None);
			p.setColor(0);
			p.fill();
			int[] c = SLOT_PAL[ult ? 1 : 0];
			int n = SLOT - 1;
			for (int y = 0; y <= n; y++){
				for (int x = 0; x <= n; x++){
					int cx = Math.min(x, n - x), cy = Math.min(y, n - y);
					//clipped corners
					if (cx + cy < 2) continue;
					int ring = Math.min(cx, cy);
					if (cx + cy == 2) ring = 0;
					else if (cx + cy == 3) ring = Math.min(ring, 1);
					int col;
					if (ring == 0) col = c[0];
					else if (ring == 1) col = (x + y <= n) ? c[1] : c[2];
					else if (ring == 2) col = c[0];
					else col = (y < SLOT / 2) ? c[3] : c[4];
					px(p, x, y, col);
				}
			}
			tx.filter(com.watabou.glwrap.Texture.NEAREST, com.watabou.glwrap.Texture.NEAREST);
		}
		return new Image(key);
	}

	public static Image icon( int image ){
		return new ItemSprite(image);
	}

	// ================================================================ skill effects

	public static void slash( CharSprite target ){
		if (target == null || target.ch == null || !target.visible) return;
		Wound.hit(target.ch);
	}

	public static void mark( CharSprite target ){
		if (target == null || target.parent == null) return;
		new Flare(4, 18).color(0xFF5A4A, true).show(target, 0.7f);
	}

	public static void markBreak( CharSprite target ){
		if (target == null || target.parent == null) return;
		new Flare(8, 26).color(0xFF5A4A, true).show(target, 0.5f);
	}

	public static void shield( CharSprite s ){
		if (s == null || s.parent == null) return;
		new Flare(6, 22).color(0x9FC8FF, true).show(s, 0.9f);
		s.emitter().burst(Speck.factory(Speck.LIGHT), 4);
	}

	public static void shieldHit( CharSprite s ){
		if (s == null || s.parent == null) return;
		new Flare(4, 14).color(0x9FC8FF, true).show(s, 0.3f);
	}

	public static void drain( CharSprite from, CharSprite to ){
		if (from == null || to == null || to.parent == null) return;
		to.parent.add(new Beam.HealthRay(from.center(), to.center()));
	}

	public static void learned( CharSprite s, int color ){
		if (s == null || s.parent == null) return;
		new Flare(8, 32).color(color, true).show(s, 1.4f);
		s.emitter().burst(Speck.factory(Speck.STAR), 6);
	}

	public static void blastLight( int cell ){
		Atmosphere.burstCell(cell, 2.6f, 0.9f, 0xFFD8A0, 0.3f);
	}

	//the arc of a wide swing, turning once around the hero
	public static void sweep( CharSprite s, int radius ){
		if (s == null || s.parent == null) return;
		Sweep w = new Sweep(s.center(), radius * DungeonTilemap.SIZE + 5, s.flipHorizontal);
		s.parent.add(w);
	}

	private static final int ARC = 64;

	private static String arcTex(){
		String key = "aug2_arc";
		if (!TextureCache.contains(key)){
			SmartTexture tx = TextureCache.create(key, ARC, ARC);
			Pixmap p = tx.bitmap;
			p.setBlending(Pixmap.Blending.None);
			p.setColor(0);
			p.fill();
			float c = (ARC - 1) / 2f;
			for (int y = 0; y < ARC; y++){
				for (int x = 0; x < ARC; x++){
					float dx = x - c, dy = y - c;
					float d = (float)Math.sqrt(dx * dx + dy * dy);
					//the blade's path: thick at the leading edge, thinning to nothing behind it
					float a = (float)Math.atan2(dy, dx);
					float t = (a + (float)Math.PI) / (2f * (float)Math.PI);   // 0..1 around, leading edge at 1
					float tail = Math.max(0f, (t - 0.35f) / 0.65f);
					if (tail <= 0) continue;
					float outer = 30f, width = 1.5f + 6f * tail * tail;
					if (d > outer || d < outer - width) continue;
					float k = (d - (outer - width)) / width;      // 0 inner .. 1 outer edge
					float v = tail * (0.35f + 0.65f * k);
					int alpha = v > 0.72f ? 0xFF : v > 0.45f ? 0xB0 : v > 0.2f ? 0x60 : 0x28;
					px(p, x, y, (alpha << 24) | 0xFFFFFF);
				}
			}
			tx.filter(com.watabou.glwrap.Texture.NEAREST, com.watabou.glwrap.Texture.NEAREST);
		}
		return key;
	}

	private static class Sweep extends Image {
		private static final float TIME = 0.26f;
		private float left = TIME;
		private final float dir;

		Sweep( PointF centre, float radius, boolean flipped ){
			super(arcTex());
			origin.set(ARC / 2f, ARC / 2f);
			scale.set(radius / 30f);
			x = centre.x - ARC / 2f;
			y = centre.y - ARC / 2f;
			dir = flipped ? -1 : 1;
			if (flipped) scale.x = -scale.x;
			angle = -120 * dir;
			hardlight(0xFFF4DC);
		}

		@Override
		public void update() {
			super.update();
			left -= Game.elapsed;
			if (left <= 0){
				killAndErase();
				return;
			}
			float k = 1f - left / TIME;
			//fast at first, easing out
			float e = 1f - (1f - k) * (1f - k);
			angle = (-120 + 400 * e) * dir;
			alpha(k < 0.6f ? 1f : (1f - k) / 0.4f);
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	// ================================================================ mines on the floor

	private static final HashMap<Integer, Image> mineSprites = new HashMap<>();

	//a new scene has none of the old sprites
	static void sceneReset(){
		mineSprites.clear();
	}

	static void syncMines( final Tracker2 t ){
		Game.runOnRenderThread(new Callback() {
			@Override public void call() {
				Iterator<Map.Entry<Integer, Image>> it = mineSprites.entrySet().iterator();
				while (it.hasNext()){
					Map.Entry<Integer, Image> e = it.next();
					if (!t.mines.contains(e.getKey()) || t.mineFloor != Tracker2.floorKey()){
						e.getValue().killAndErase();
						it.remove();
					}
				}
				if (t.mineFloor != Tracker2.floorKey() || Dungeon.level == null) return;
				for (int cell : t.mines.toArray(new Integer[0])){
					if (mineSprites.containsKey(cell)) continue;
					MineSprite m = new MineSprite(cell);
					mineSprites.put(cell, m);
					GameScene.prop(m);
				}
			}
		});
	}

	//half buried: smaller than a dropped bomb, with a slow warning blink
	private static class MineSprite extends ItemSprite {
		private final int cell;
		private float time = 0;

		MineSprite( int cell ){
			super(ItemSpriteSheet.BOMB);
			this.cell = cell;
			scale.set(0.75f);
			PointF p = DungeonTilemap.tileCenterToWorld(cell);
			x = p.x - width() / 2f;
			y = p.y - height() / 2f + 2;
			PixelScene.align(this);
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			visible = Dungeon.level != null && cell < Dungeon.level.length() && Dungeon.level.heroFOV[cell];
			float blink = (time % 1.6f) < 0.18f ? 1f : 0f;
			rm = gm = bm = 0.75f;
			ra = 0.45f * blink;
			ga = ba = 0f;
		}
	}

	// ================================================================ kill streak banner

	private static final String[] STREAK = {"", "", "더블 킬", "트리플 킬", "쿼드라 킬", "펜타 킬"};
	private static final int[] STREAK_COLOR = {0, 0, 0xFFE9A8, 0xFFC860, 0xFF9340, 0xFF5038};

	private static Banner banner = null;

	static void streak( final int n ){
		final int i = Math.min(n, 5);
		Game.runOnRenderThread(new Callback() {
			@Override public void call() {
				if (banner != null && banner.parent != null) banner.killAndErase();
				banner = new Banner(STREAK[i], STREAK_COLOR[i], i);
				GameScene.overlay(banner);
				Sample.INSTANCE.play(Assets.Sounds.BADGE, 0.5f, 0.9f + 0.12f * i);
				if (i >= 5) GameScene.flash(0x40FF5038);
			}
		});
	}

	private static class Banner extends Group {
		private final RenderedTextBlock text;
		private final float baseY;
		private float time = 0;
		private static final float IN = 0.12f, HOLD = 1.1f, OUT = 0.35f;

		Banner( String label, int color, int level ){
			text = PixelScene.renderTextBlock(label, 9 + level);
			text.hardlight(color);
			add(text);
			baseY = Math.round(PixelScene.uiCamera.height * 0.24f);
			place(0);
		}

		private void place( float dy ){
			text.setPos((PixelScene.uiCamera.width - text.width()) / 2f, baseY + dy);
			PixelScene.align(text);
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			if (time < IN){
				float k = time / IN;
				text.alpha(k);
				place(6 * (1 - k));
			} else if (time < IN + HOLD){
				text.alpha(1f);
				place(0);
			} else if (time < IN + HOLD + OUT){
				float k = (time - IN - HOLD) / OUT;
				text.alpha(1 - k);
				place(-4 * k);
			} else {
				killAndErase();
			}
		}
	}
}
