/*
 * Atmosphere mod - full screen post processing
 *
 * The game world is rendered into an offscreen buffer (between PostFX.Begin and PostFX.End),
 * then composited to the screen with bloom, color grading, distortions and status effects.
 * The UI is drawn afterwards and is not affected.
 */

package com.shatteredpixel.shatteredpixeldungeon.atmosphere;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blizzard;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ConfusionGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Inferno;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SmokeScreen;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.StenchGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.glscripts.Script;
import com.watabou.glwrap.Blending;
import com.watabou.glwrap.Uniform;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.NoosaScriptNoLighting;
import com.watabou.utils.Point;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class PostFX {

	public static boolean enabled(){
		return Atmosphere.quality() >= 2;
	}

	// ---------------------------------------------------------------- frame buffers

	private static FrameBuffer sceneFB;
	private static FrameBuffer blurA, blurB;     // 1/4 resolution
	private static FrameBuffer wideA, wideB;     // 1/8 resolution
	private static int fbW, fbH;

	private static boolean capturing = false;

	private static FrameBuffer makeFB( int w, int h ){
		FrameBuffer fb = new FrameBuffer(Pixmap.Format.RGBA8888, Math.max(1, w), Math.max(1, h), false);
		fb.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		fb.getColorBufferTexture().setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
		return fb;
	}

	private static void ensureBuffers(){
		int w = Gdx.graphics.getBackBufferWidth();
		int h = Gdx.graphics.getBackBufferHeight();
		if (sceneFB != null && w == fbW && h == fbH) return;

		release();
		fbW = w;
		fbH = h;
		sceneFB = makeFB(w, h);
		blurA = makeFB(w / 4, h / 4);
		blurB = makeFB(w / 4, h / 4);
		wideA = makeFB(w / 8, h / 8);
		wideB = makeFB(w / 8, h / 8);
		com.watabou.glwrap.Texture.clear();
	}

	public static void release(){
		FrameBuffer[] all = {sceneFB, blurA, blurB, wideA, wideB};
		for (FrameBuffer fb : all){
			if (fb != null){
				try { fb.dispose(); } catch (Exception ignored){}
			}
		}
		sceneFB = blurA = blurB = wideA = wideB = null;
		fbW = fbH = 0;
		capturing = false;
	}

	private static void restoreNoosaState(){
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		com.watabou.glwrap.Texture.clear();
		Blending.useDefault();
		NoosaScript.get().resetCamera();
		NoosaScriptNoLighting.get().resetCamera();
	}

	// ---------------------------------------------------------------- begin / end markers in the scene

	public static class Begin extends Gizmo {
		@Override
		public void draw() {
			capturing = false;
			if (!enabled() || Dungeon.level == null) return;
			try {
				ensureBuffers();
				sceneFB.bind();
				Gdx.gl.glViewport(0, 0, fbW, fbH);
				Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
				Gdx.gl.glClearColor(0, 0, 0, 1);
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
				capturing = true;
			} catch (Exception e){
				//if anything goes wrong with offscreen rendering, fall back to drawing directly
				Game.reportException(e);
				FrameBuffer.unbind();
				Atmosphere.quality(1);
			}
			restoreNoosaState();
		}

		@Override
		public void destroy() {
			super.destroy();
			release();
		}
	}

	public static class End extends Gizmo {
		@Override
		public void update() {
			super.update();
			updateEffects();
		}

		@Override
		public void draw() {
			if (!capturing) return;
			capturing = false;
			composite();
			restoreNoosaState();
		}
	}

	// ---------------------------------------------------------------- full screen quad

	private static FloatBuffer quad;

	private static FloatBuffer quad(){
		if (quad == null){
			quad = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
			quad.put(new float[]{
					-1, -1, 0, 0,
					 1, -1, 1, 0,
					 1,  1, 1, 1,
					-1,  1, 0, 1 });
		}
		return quad;
	}

	private static void drawTo( FrameBuffer target, int w, int h ){
		Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
		if (target != null){
			target.bind();
		} else {
			FrameBuffer.unbind();
		}
		Gdx.gl.glViewport(0, 0, w, h);
	}

	private static void bindTex( int unit, FrameBuffer fb ){
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + unit);
		Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, fb.getColorBufferTexture().getTextureObjectHandle());
	}

	private static void blur( FrameBuffer src, FrameBuffer tmp, int w, int h, float spread ){
		BlurScript b = BlurScript.get();
		//horizontal
		drawTo(tmp, w, h);
		bindTex(0, src);
		Gdx.gl.glUniform1i(b.uTex.location(), 0);
		b.uDir.value2f(spread / w, 0);
		b.drawQuad(quad());
		//vertical
		drawTo(src, w, h);
		bindTex(0, tmp);
		b.uDir.value2f(0, spread / h);
		b.drawQuad(quad());
	}

	private static void composite(){
		Blending.disable();
		Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

		int q = Atmosphere.quality();
		int qw = Math.max(1, fbW / 4), qh = Math.max(1, fbH / 4);
		int ew = Math.max(1, fbW / 8), eh = Math.max(1, fbH / 8);

		//downsample the scene to 1/4 resolution (4x4 box filter)
		DownScript d = DownScript.get();
		drawTo(blurA, qw, qh);
		bindTex(0, sceneFB);
		Gdx.gl.glUniform1i(d.uTex.location(), 0);
		d.uTexel.value2f(1f / fbW, 1f / fbH);
		d.drawQuad(quad());

		blur(blurA, blurB, qw, qh, 1f);
		blur(blurA, blurB, qw, qh, 2f);

		//wider glow at 1/8 resolution
		boolean wide = q >= 3;
		if (wide){
			drawTo(wideA, ew, eh);
			bindTex(0, blurA);
			d.uTexel.value2f(0.5f / qw, 0.5f / qh);
			d.drawQuad(quad());
			blur(wideA, wideB, ew, eh, 1.5f);
			blur(wideA, wideB, ew, eh, 3f);
		}

		//final composite to the screen
		CompositeScript c = CompositeScript.get();
		drawTo(null, fbW, fbH);
		bindTex(2, wide ? wideA : blurA);
		bindTex(1, blurA);
		bindTex(0, sceneFB);
		Gdx.gl.glUniform1i(c.uTex.location(), 0);
		Gdx.gl.glUniform1i(c.uBlur.location(), 1);
		Gdx.gl.glUniform1i(c.uWide.location(), 2);
		c.uRes.value2f(fbW, fbH);
		c.uTime.value1f(time);

		c.uGrade.value4f(cur[SAT], cur[CONTRAST], cur[BRIGHT], cur[RED_KEEP]);
		c.uGain.value4f(cur[GAIN_R], cur[GAIN_G], cur[GAIN_B], cur[BLOOM]);
		c.uFx.value4f(cur[VIGNETTE], 0.005f, cur[CHROMA], cur[BLOOM_TH]);
		c.uWobble.value4f(cur[WOB_AMP], cur[WOB_FREQ], cur[WOB_SPEED], cur[BLUR_MIX]);
		c.uTint.value4f(cur[TINT_R], cur[TINT_G], cur[TINT_B], cur[TINT_A]);
		c.uEdge.value4f(cur[EDGE_R], cur[EDGE_G], cur[EDGE_B], edgeNow);
		c.uFlash.value4f(flashR, flashG, flashB, flashA);
		c.uDesat.value1f(cur[DESAT]);
		c.uZoom.value1f(zoomNow);

		//heat haze sources, in screen uv
		Uniform[] hu = {c.uHeat0, c.uHeat1, c.uHeat2, c.uHeat3};
		for (int i = 0; i < hu.length; i++){
			if (i < heatUV.length / 4 && heatUV[i*4+3] > 0){
				hu[i].value4f(heatUV[i*4], heatUV[i*4+1], heatUV[i*4+2], heatUV[i*4+3]);
			} else {
				hu[i].value4f(0, 0, 0.0001f, 0);
			}
		}

		c.drawQuad(quad());

		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
	}

	// ---------------------------------------------------------------- effect state

	private static final int SAT = 0, CONTRAST = 1, BRIGHT = 2, RED_KEEP = 3,
			GAIN_R = 4, GAIN_G = 5, GAIN_B = 6, BLOOM = 7,
			VIGNETTE = 8, CHROMA = 9, BLOOM_TH = 10,
			WOB_AMP = 11, WOB_FREQ = 12, WOB_SPEED = 13, BLUR_MIX = 14,
			TINT_R = 15, TINT_G = 16, TINT_B = 17, TINT_A = 18,
			EDGE_R = 19, EDGE_G = 20, EDGE_B = 21, EDGE_A = 22,
			DESAT = 23, COUNT = 24;

	private static final float[] cur = new float[COUNT];
	private static final float[] target = new float[COUNT];
	private static boolean initialized = false;

	private static float time = 0f;
	private static float edgeNow = 0f;
	private static float zoomNow = 1f;
	private static float breathe = 0f;
	private static float edgePulse = 0f;

	private static float flashR, flashG, flashB, flashA;

	private static final float[] heatUV = new float[Atmosphere.MAX_HEAT * 4];

	//a brief full screen flash, used for explosions, lightning and heavy hits
	public static void flash( int color, float amount ){
		if (!enabled()) return;
		float a = Math.min(0.6f, Math.max(flashA, amount));
		flashR = ((color >> 16) & 0xFF) / 255f;
		flashG = ((color >> 8) & 0xFF) / 255f;
		flashB = (color & 0xFF) / 255f;
		flashA = a;
	}

	private static void setGrade( String tiles ){
		float sat, con, bri, red, gr, gg, gb, bloom, th;
		switch (tiles == null ? "" : tiles){
			case Assets.Environment.TILES_SEWERS:
				sat = 0.90f; con = 1.08f; bri = 1.02f; red = 0f; gr = 0.97f; gg = 1.02f; gb = 1.02f; bloom = 0.55f; th = 0.62f; break;
			case Assets.Environment.TILES_PRISON:
				sat = 0.78f; con = 1.12f; bri = 1.02f; red = 0.3f; gr = 0.96f; gg = 0.99f; gb = 1.06f; bloom = 0.6f; th = 0.64f; break;
			case Assets.Environment.TILES_CAVES:
			case Assets.Environment.TILES_CAVES_GNOLL:
				sat = 0.95f; con = 1.12f; bri = 1.02f; red = 0f; gr = 1.05f; gg = 1.00f; gb = 0.92f; bloom = 0.6f; th = 0.62f; break;
			case Assets.Environment.TILES_CAVES_CRYSTAL:
				sat = 1.05f; con = 1.10f; bri = 1.03f; red = 0f; gr = 0.95f; gg = 1.02f; gb = 1.08f; bloom = 0.75f; th = 0.58f; break;
			case Assets.Environment.TILES_CITY:
				sat = 0.92f; con = 1.10f; bri = 1.03f; red = 0f; gr = 1.06f; gg = 1.01f; gb = 0.90f; bloom = 0.65f; th = 0.62f; break;
			case Assets.Environment.TILES_HALLS:
				sat = 0.75f; con = 1.12f; bri = 1.02f; red = 0.45f; gr = 1.04f; gg = 0.96f; gb = 0.96f; bloom = 0.75f; th = 0.58f; break;
			default:
				sat = 1.00f; con = 1.06f; bri = 1.02f; red = 0f; gr = 1f; gg = 1f; gb = 1f; bloom = 0.55f; th = 0.62f; break;
		}
		target[SAT] = sat; target[CONTRAST] = con; target[RED_KEEP] = red;
		target[BRIGHT] = bri;
		target[GAIN_R] = gr; target[GAIN_G] = gg; target[GAIN_B] = gb;
		target[BLOOM] = bloom; target[BLOOM_TH] = th;
	}

	private static void updateEffects(){
		float dt = Game.elapsed;
		time += dt;
		if (time > 3600f) time -= 3600f;

		flashA = Math.max(0f, flashA - dt * 2.2f);

		if (!enabled() || Dungeon.level == null || Dungeon.hero == null) return;

		Level level = Dungeon.level;
		Hero hero = Dungeon.hero;

		setGrade(level.tilesTex());
		target[VIGNETTE] = Atmosphere.currentVignette();
		if (Ambience.bossVisible){
			target[VIGNETTE] += 0.15f;
			target[CONTRAST] += 0.04f;
		}
		target[CHROMA] = 0f;
		target[WOB_AMP] = 0f; target[WOB_FREQ] = 20f; target[WOB_SPEED] = 2f;
		target[BLUR_MIX] = 0f;
		target[TINT_R] = target[TINT_G] = target[TINT_B] = 1f; target[TINT_A] = 0f;
		target[EDGE_R] = 1f; target[EDGE_G] = 0f; target[EDGE_B] = 0f; target[EDGE_A] = 0f;
		target[DESAT] = 0f;
		edgePulse = 0f;

		int pos = hero.pos;
		boolean inLevel = pos >= 0 && pos < level.length();

		//standing in water: gentle ripple
		if (inLevel && level.water[pos] && !hero.flying){
			target[WOB_AMP] = 0.00035f; target[WOB_FREQ] = 7f; target[WOB_SPEED] = 0.9f;
			tint(0.55f, 0.9f, 1f, 0.06f);
		}

		//gases at the hero's position
		if (inLevel && level.blobs != null){
			for (Blob b : level.blobs.values()){
				if (b == null || b.volume <= 0 || b.cur == null || b.cur[pos] <= 0) continue;
				if (b instanceof ToxicGas || b instanceof StenchGas){
					tint(0.55f, 1f, 0.35f, 0.16f); wobble(0.0008f, 8f, 1.0f);
				} else if (b instanceof CorrosiveGas){
					tint(1f, 0.6f, 0.25f, 0.16f); wobble(0.0008f, 8f, 1.0f);
				} else if (b instanceof ParalyticGas){
					tint(1f, 0.95f, 0.4f, 0.14f); wobble(0.0006f, 8f, 0.9f);
				} else if (b instanceof ConfusionGas){
					tint(0.8f, 0.6f, 1f, 0.12f); wobble(0.003f, 6f, 1.6f);
					target[CHROMA] = Math.max(target[CHROMA], 0.03f);
				} else if (b instanceof SmokeScreen){
					tint(0.6f, 0.6f, 0.62f, 0.2f); target[BLUR_MIX] = Math.max(target[BLUR_MIX], 0.45f);
				} else if (b instanceof Blizzard){
					tint(0.8f, 0.95f, 1f, 0.2f); target[DESAT] = Math.max(target[DESAT], 0.3f);
				} else if (b instanceof Inferno){
					tint(1f, 0.55f, 0.2f, 0.18f); wobble(0.0008f, 10f, 2f);
				}
			}
		}

		//hero status effects
		if (!hero.isAlive()){
			target[DESAT] = 1f;
			target[CONTRAST] *= 1.1f;
			target[VIGNETTE] = 0.9f;
		} else {
			if (hero.buff(Vertigo.class) != null){
				wobble(0.004f, 5f, 1.5f);
				target[CHROMA] = Math.max(target[CHROMA], 0.045f);
			}
			if (hero.buff(Blindness.class) != null){
				target[BLUR_MIX] = Math.max(target[BLUR_MIX], 0.85f);
				target[VIGNETTE] = Math.max(target[VIGNETTE], 0.95f);
				target[DESAT] = Math.max(target[DESAT], 0.3f);
			}
			if (hero.invisible > 0 || hero.buff(Invisibility.class) != null){
				target[DESAT] = Math.max(target[DESAT], 0.35f);
				tint(0.7f, 0.85f, 1f, 0.12f);
			}
			if (hero.buff(Paralysis.class) != null){
				target[DESAT] = Math.max(target[DESAT], 0.75f);
				edge(0.9f, 0.9f, 0.6f, 0.25f);
			}
			if (hero.buff(Frost.class) != null){
				edge(0.6f, 0.85f, 1f, 0.55f);
				target[DESAT] = Math.max(target[DESAT], 0.35f);
			} else if (hero.buff(Chill.class) != null){
				edge(0.6f, 0.85f, 1f, 0.3f);
				target[DESAT] = Math.max(target[DESAT], 0.15f);
			}
			if (hero.buff(Burning.class) != null){
				edge(1f, 0.45f, 0.1f, 0.45f);
				edgePulse = Math.max(edgePulse, 0.4f);
			}
			if (hero.buff(Poison.class) != null){
				edge(0.4f, 0.9f, 0.2f, 0.3f);
			}

			//low health
			float ratio = hero.HT > 0 ? hero.HP / (float)hero.HT : 1f;
			if (ratio < 0.35f){
				float k = (0.35f - ratio) / 0.35f;
				edge(0.85f, 0.05f, 0.05f, 0.25f + 0.45f * k);
				edgePulse = Math.max(edgePulse, 0.6f + 0.4f * k);
				target[DESAT] = Math.max(target[DESAT], 0.25f * k);
			}
		}

		//smoothly move towards the targets
		float rate = Math.min(1f, dt * 4f);
		if (!initialized){
			System.arraycopy(target, 0, cur, 0, COUNT);
			initialized = true;
		} else {
			for (int i = 0; i < COUNT; i++){
				cur[i] += (target[i] - cur[i]) * rate;
			}
		}

		//heartbeat style pulse for edge effects
		float beat = (float)Math.pow(Math.max(0, Math.sin(time * 5.2f)), 6) + 0.6f * (float)Math.pow(Math.max(0, Math.sin(time * 5.2f - 0.9f)), 8);
		edgeNow = cur[EDGE_A] * (1f - edgePulse + edgePulse * (0.45f + 0.9f * beat));

		//screen zoom: heartbeat at low health, slow breathing while standing still
		float ratioHP = hero.HT > 0 ? hero.HP / (float)hero.HT : 1f;
		float lowHP = hero.isAlive() && ratioHP < 0.35f ? (0.35f - ratioHP) / 0.35f : 0f;
		float breatheTarget = Ambience.idleTime > 3f ? Math.min(1f, (Ambience.idleTime - 3f) / 2f) : 0f;
		breathe += (breatheTarget - breathe) * Math.min(1f, dt * 1.5f);
		zoomNow = 1f + lowHP * 0.006f * beat
				+ breathe * 0.0035f * (0.5f - 0.5f * (float)Math.cos(time * 0.7f));

		//heat haze sources, converted to screen space
		Camera c = Camera.main;
		for (int i = 0; i < Atmosphere.MAX_HEAT; i++){
			if (i < Atmosphere.heatCount && c != null){
				float wx = Atmosphere.heat[i*4], wy = Atmosphere.heat[i*4+1];
				Point p = c.cameraToScreen(wx, wy);
				heatUV[i*4]   = p.x / (float)Game.width;
				heatUV[i*4+1] = 1f - p.y / (float)Game.height;
				heatUV[i*4+2] = Atmosphere.heat[i*4+2] * 16f * c.zoom / Game.height;
				heatUV[i*4+3] = Atmosphere.heat[i*4+3] * (Atmosphere.quality() >= 3 ? 1f : 0.6f);
			} else {
				heatUV[i*4+3] = 0;
			}
		}
	}

	private static void tint( float r, float g, float b, float a ){
		if (a > target[TINT_A]){
			target[TINT_R] = r; target[TINT_G] = g; target[TINT_B] = b; target[TINT_A] = a;
		}
	}

	private static void wobble( float amp, float freq, float speed ){
		if (amp > target[WOB_AMP]){
			target[WOB_AMP] = amp; target[WOB_FREQ] = freq; target[WOB_SPEED] = speed;
		}
	}

	private static void edge( float r, float g, float b, float a ){
		if (a > target[EDGE_A]){
			target[EDGE_R] = r; target[EDGE_G] = g; target[EDGE_B] = b; target[EDGE_A] = a;
		}
	}

	// ---------------------------------------------------------------- shaders

	private static final String VERTEX =
			"attribute vec4 aXYZW;\n" +
			"attribute vec2 aUV;\n" +
			"varying vec2 vUV;\n" +
			"void main() {\n" +
			"  gl_Position = vec4( aXYZW.xy, 0.0, 1.0 );\n" +
			"  vUV = aUV;\n" +
			"}\n" +
			"//\n";

	private static final String PRECISION =
			"#ifdef GL_ES\n" +
			"  #ifdef GL_FRAGMENT_PRECISION_HIGH\n" +
			"    precision highp float;\n" +
			"  #else\n" +
			"    precision mediump float;\n" +
			"  #endif\n" +
			"#endif\n";

	public static class DownScript extends NoosaScript {
		public Uniform uTexel;
		public DownScript(){
			super();
			uTexel = uniform("uTexel");
		}
		public static DownScript get(){ return Script.use(DownScript.class); }
		@Override public void lighting(float a, float b, float c, float d, float e, float f, float g, float h) {}
		@Override protected String shader() {
			return VERTEX + PRECISION +
					"varying vec2 vUV;\n" +
					"uniform sampler2D uTex;\n" +
					"uniform vec2 uTexel;\n" +
					"void main() {\n" +
					"  vec3 c = texture2D( uTex, vUV + uTexel * vec2(-1.0, -1.0) ).rgb;\n" +
					"  c += texture2D( uTex, vUV + uTexel * vec2( 1.0, -1.0) ).rgb;\n" +
					"  c += texture2D( uTex, vUV + uTexel * vec2(-1.0,  1.0) ).rgb;\n" +
					"  c += texture2D( uTex, vUV + uTexel * vec2( 1.0,  1.0) ).rgb;\n" +
					"  gl_FragColor = vec4( c * 0.25, 1.0 );\n" +
					"}\n";
		}
	}

	public static class BlurScript extends NoosaScript {
		public Uniform uDir;
		public BlurScript(){
			super();
			uDir = uniform("uDir");
		}
		public static BlurScript get(){ return Script.use(BlurScript.class); }
		@Override public void lighting(float a, float b, float c, float d, float e, float f, float g, float h) {}
		@Override protected String shader() {
			return VERTEX + PRECISION +
					"varying vec2 vUV;\n" +
					"uniform sampler2D uTex;\n" +
					"uniform vec2 uDir;\n" +
					"void main() {\n" +
					"  vec3 c = texture2D( uTex, vUV ).rgb * 0.2270270;\n" +
					"  c += texture2D( uTex, vUV + uDir * 1.3846154 ).rgb * 0.3162162;\n" +
					"  c += texture2D( uTex, vUV - uDir * 1.3846154 ).rgb * 0.3162162;\n" +
					"  c += texture2D( uTex, vUV + uDir * 3.2307692 ).rgb * 0.0702703;\n" +
					"  c += texture2D( uTex, vUV - uDir * 3.2307692 ).rgb * 0.0702703;\n" +
					"  gl_FragColor = vec4( c, 1.0 );\n" +
					"}\n";
		}
	}

	public static class CompositeScript extends NoosaScript {
		public Uniform uBlur, uWide, uRes, uTime, uGrade, uGain, uFx, uWobble, uTint, uEdge, uFlash, uDesat, uZoom;
		public Uniform uHeat0, uHeat1, uHeat2, uHeat3;

		public CompositeScript(){
			super();
			uBlur   = uniform("uBlur");
			uWide   = uniform("uWide");
			uRes    = uniform("uRes");
			uTime   = uniform("uTime");
			uGrade  = uniform("uGrade");
			uGain   = uniform("uGain");
			uFx     = uniform("uFx");
			uWobble = uniform("uWobble");
			uTint   = uniform("uTint");
			uEdge   = uniform("uEdge");
			uFlash  = uniform("uFlash");
			uDesat  = uniform("uDesat");
			uZoom   = uniform("uZoom");
			uHeat0  = uniform("uHeat0");
			uHeat1  = uniform("uHeat1");
			uHeat2  = uniform("uHeat2");
			uHeat3  = uniform("uHeat3");
		}
		public static CompositeScript get(){ return Script.use(CompositeScript.class); }
		@Override public void lighting(float a, float b, float c, float d, float e, float f, float g, float h) {}

		@Override protected String shader() {
			return VERTEX + PRECISION +
					"varying vec2 vUV;\n" +
					"uniform sampler2D uTex;\n" +
					"uniform sampler2D uBlur;\n" +
					"uniform sampler2D uWide;\n" +
					"uniform vec2 uRes;\n" +
					"uniform float uTime;\n" +
					"uniform vec4 uGrade;\n" +   // saturation, contrast, brightness, red keep
					"uniform vec4 uGain;\n" +    // rgb gain, bloom strength
					"uniform vec4 uFx;\n" +      // vignette, grain, chromatic aberration, bloom threshold
					"uniform vec4 uWobble;\n" +  // amplitude, frequency, speed, blur mix
					"uniform vec4 uTint;\n" +
					"uniform vec4 uEdge;\n" +
					"uniform vec4 uFlash;\n" +
					"uniform float uDesat;\n" +
					"uniform float uZoom;\n" +
					"uniform vec4 uHeat0;\n" +
					"uniform vec4 uHeat1;\n" +
					"uniform vec4 uHeat2;\n" +
					"uniform vec4 uHeat3;\n" +

					"vec2 haze( vec2 uv, vec4 h, float aspect ){\n" +
					"  if (h.w <= 0.0) return vec2(0.0);\n" +
					"  vec2 d = uv - h.xy; d.x *= aspect;\n" +
					"  float f = h.w * (1.0 - smoothstep( 0.0, h.z, length(d) ));\n" +
					//heat rises: stronger above the source
					"  f *= smoothstep( -h.z * 0.4, h.z * 0.6, uv.y - h.xy.y + h.z * 0.2 );\n" +
					"  return vec2( sin( uv.y * 90.0 - uTime * 5.0 ) * 0.0008, cos( uv.x * 70.0 + uTime * 3.5 ) * 0.0005 ) * f;\n" +
					"}\n" +

					"float hash( vec2 p ){\n" +
					"  p = fract( p * vec2( 123.34, 456.21 ) );\n" +
					"  p += dot( p, p + 45.32 );\n" +
					"  return fract( p.x * p.y );\n" +
					"}\n" +

					"void main() {\n" +
					"  vec2 uv = ( vUV - 0.5 ) / uZoom + 0.5;\n" +
					"  float aspect = uRes.x / uRes.y;\n" +

					//screen wobble (water, gas, vertigo)
					"  if (uWobble.x > 0.0001) {\n" +
					"    uv += vec2( sin( uv.y * uWobble.y + uTime * uWobble.z ),\n" +
					"                cos( uv.x * uWobble.y * 0.8 + uTime * uWobble.z * 1.3 ) ) * uWobble.x;\n" +
					"  }\n" +

					//heat haze
					"  uv += haze( uv, uHeat0, aspect ) + haze( uv, uHeat1, aspect ) + haze( uv, uHeat2, aspect ) + haze( uv, uHeat3, aspect );\n" +

					//chromatic aberration, stronger towards the edges
					"  vec2 cc = uv - 0.5;\n" +
					"  vec2 ca = cc * dot( cc, cc ) * uFx.z * 4.0;\n" +
					"  vec3 col;\n" +
					"  col.r = texture2D( uTex, uv + ca ).r;\n" +
					"  col.g = texture2D( uTex, uv ).g;\n" +
					"  col.b = texture2D( uTex, uv - ca ).b;\n" +

					//blur (blindness, smoke)
					"  vec3 blurred = texture2D( uBlur, uv ).rgb;\n" +
					//local contrast, brings out textures a little
					"  float lc = dot( col - blurred, vec3( 0.299, 0.587, 0.114 ) );\n" +
					"  col += lc * 0.18;\n" +
					"  col = mix( col, blurred, uWobble.w );\n" +

					//bloom
					"  vec3 wide = texture2D( uWide, uv ).rgb;\n" +
					"  vec3 bl = max( blurred - uFx.w, 0.0 ) + max( wide - uFx.w * 0.9, 0.0 ) * 0.8;\n" +
					"  bl = bl / ( 1.0 + bl );\n" +
					"  col += bl * uGain.a;\n" +

					//color grading
					"  float lum = dot( col, vec3( 0.299, 0.587, 0.114 ) );\n" +
					"  float redness = clamp( ( col.r - max( col.g, col.b ) ) * 4.0, 0.0, 1.0 );\n" +
					"  float sat = mix( uGrade.x, max( uGrade.x, 1.1 ), redness * uGrade.w );\n" +
					"  col = mix( vec3( lum ), col, sat );\n" +
					"  col = ( col - 0.5 ) * uGrade.y + 0.5;\n" +
					"  col *= uGain.rgb * uGrade.z;\n" +
					"  lum = dot( col, vec3( 0.299, 0.587, 0.114 ) );\n" +
					"  col = mix( col, vec3( lum ), uDesat );\n" +
					"  col = mix( col, uTint.rgb * ( lum * 1.15 + 0.05 ), uTint.a );\n" +

					//vignette
					"  vec2 vd = vUV - 0.5; vd.x *= aspect;\n" +
					"  float r = length( vd ) / length( vec2( 0.5 * aspect, 0.5 ) );\n" +
					"  col *= 1.0 - uFx.x * smoothstep( 0.35, 1.05, r );\n" +

					//edge glow for status effects
					"  float e = smoothstep( 0.45, 1.05, r ) * uEdge.a;\n" +
					"  col = mix( col, uEdge.rgb * 0.8, clamp( e, 0.0, 0.85 ) );\n" +

					//flashes
					"  col += uFlash.rgb * uFlash.a;\n" +

					//film grain
					"  float n = hash( floor( vUV * uRes ) ) - 0.5;\n" +
					"  col += n * uFx.y;\n" +

					"  gl_FragColor = vec4( clamp( col, 0.0, 1.0 ), 1.0 );\n" +
					"}\n";
		}
	}
}
