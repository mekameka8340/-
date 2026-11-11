/*
 * Atmosphere mod - per pixel water mask
 *
 * Reflections are only drawn where there is water, and shadows only where there isn't,
 * so both blend correctly as characters step in and out of water.
 */

package com.shatteredpixel.shatteredpixeldungeon.atmosphere;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glscripts.Script;
import com.watabou.glwrap.Texture;
import com.watabou.glwrap.Uniform;
import com.watabou.noosa.Image;
import com.watabou.noosa.NoosaScript;

import java.nio.ByteBuffer;

public class WaterMask {

	//texels per tile, the outer texel of water tiles next to land is left dry
	// because the land tiles' ragged edges are drawn over it
	private static final int TPT = 4;

	private static SmartTexture tex;
	private static String key;
	private static int keyCounter = 0;
	private static int hash = 0;
	private static int w2, h2;

	public static void update( Level level ){
		int h = 7;
		for (int i = 0; i < level.length(); i++){
			h = h * 31 + (level.water[i] ? 1 : 0);
		}
		h = h * 31 + level.width();
		if (tex != null && h == hash) return;
		hash = h;
		build(level);
	}

	private static void build( Level level ){
		release();
		int mw = level.width(), mh = level.height();
		int tw = mw * TPT, th = mh * TPT;
		w2 = 1; while (w2 < tw) w2 <<= 1;
		h2 = 1; while (h2 < th) h2 <<= 1;

		key = "WaterMask_" + (keyCounter++);
		tex = TextureCache.create(key, w2, h2);
		//smooth filtering lets reflections fade out softly at the water's edge
		tex.filter(Texture.LINEAR, Texture.LINEAR);
		ByteBuffer px = tex.bitmap.getPixels();
		for (int i = 0; i < w2 * h2 * 4; i++) px.put(i, (byte)0);

		for (int cy = 0; cy < mh; cy++){
			for (int cx = 0; cx < mw; cx++){
				int cell = cy * mw + cx;
				if (!level.water[cell]) continue;
				boolean l = cx > 0 && level.water[cell - 1];
				boolean r = cx < mw - 1 && level.water[cell + 1];
				boolean u = cy > 0 && level.water[cell - mw];
				boolean d = cy < mh - 1 && level.water[cell + mw];
				for (int ty = 0; ty < TPT; ty++){
					for (int tx = 0; tx < TPT; tx++){
						if (tx == 0 && !l) continue;
						if (tx == TPT - 1 && !r) continue;
						if (ty == 0 && !u) continue;
						if (ty == TPT - 1 && !d) continue;
						int x = cx * TPT + tx, y = cy * TPT + ty;
						px.put((y * w2 + x) * 4 + 3, (byte)0xFF);
					}
				}
			}
		}
		tex.bitmap(tex.bitmap);
		Texture.clear();
	}

	public static void release(){
		if (key != null){
			TextureCache.remove(key);
			key = null;
			tex = null;
		}
	}

	static boolean ready(){
		return tex != null;
	}

	//binds the mask on texture unit 1
	static void bind(){
		if (tex.id == -1){
			tex.bind();
		}
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE1);
		Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, tex.id);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		Texture.clear();
	}

	static float scaleX(){ return 1f / ((DungeonTilemap.SIZE / (float)TPT) * w2); }
	static float scaleY(){ return 1f / ((DungeonTilemap.SIZE / (float)TPT) * h2); }

	// ---------------------------------------------------------------- image drawn through the mask

	public static class MaskedImage extends Image {

		public boolean invert = false;  //true: draw only where there is NO water
		public boolean masked = true;   //false: ignore the mask, something else already limits it
		public float fadeFromY = 0;     //world y where a reflection starts
		public float fadeLength = 0;    //0 = no fade
		public float wave = 0;          //horizontal ripple, in texels
		public float waveTime = 0;

		public MaskedImage(){
			super();
		}

		public MaskedImage( Object tx ){
			super(tx);
		}

		@Override
		protected NoosaScript script() {
			return MaskScript.get();
		}

		@Override
		public void draw() {
			if (!ready() || texture == null) return;
			MaskScript s = MaskScript.get();
			//the camera moves every frame, make sure its matrix is re-uploaded
			s.resetCamera();
			bind();
			Gdx.gl.glUniform1i(s.uMask.location(), 1);
			s.uMaskScale.value2f(scaleX(), scaleY());
			s.uParams.value4f(fadeFromY, fadeLength, wave, waveTime);
			s.uInvert.value1f(!masked ? 2f : invert ? 1f : 0f);
			s.uFrame.value4f(frame.left, frame.top, frame.right, frame.bottom);
			s.uTexel.value1f(texture.width > 0 ? 1f / texture.width : 0f);
			super.draw();
		}
	}

	public static class MaskScript extends NoosaScript {
		public Uniform uMask, uMaskScale, uParams, uInvert, uFrame, uTexel;

		public MaskScript(){
			super();
			uMask      = uniform("uMask");
			uMaskScale = uniform("uMaskScale");
			uParams    = uniform("uParams");
			uInvert    = uniform("uInvert");
			uFrame     = uniform("uFrame");
			uTexel     = uniform("uTexel");
		}

		public static MaskScript get(){
			return Script.use(MaskScript.class);
		}

		@Override
		protected String shader() {
			return
				"uniform mat4 uCamera;\n" +
				"uniform mat4 uModel;\n" +
				"attribute vec4 aXYZW;\n" +
				"attribute vec2 aUV;\n" +
				"varying vec2 vUV;\n" +
				"varying vec2 vWorld;\n" +
				"void main() {\n" +
				"  vec4 world = uModel * aXYZW;\n" +
				"  vWorld = world.xy;\n" +
				"  gl_Position = uCamera * world;\n" +
				"  vUV = aUV;\n" +
				"}\n" +
				"//\n" +
				"#ifdef GL_ES\n" +
				"  #ifdef GL_FRAGMENT_PRECISION_HIGH\n" +
				"    precision highp float;\n" +
				"  #else\n" +
				"    precision mediump float;\n" +
				"  #endif\n" +
				"#endif\n" +
				"varying vec2 vUV;\n" +
				"varying vec2 vWorld;\n" +
				"uniform sampler2D uTex;\n" +
				"uniform sampler2D uMask;\n" +
				"uniform vec2 uMaskScale;\n" +
				"uniform vec4 uParams;\n" +   // fade start y, fade length, wave, time
				"uniform float uInvert;\n" +
				"uniform vec4 uFrame;\n" +
				"uniform float uTexel;\n" +
				"uniform vec4 uColorM;\n" +
				"uniform vec4 uColorA;\n" +
				"void main() {\n" +
				"  float m = texture2D( uMask, vWorld * uMaskScale ).a;\n" +
				"  if (uInvert > 1.5) m = 1.0;\n" +
				"  else if (uInvert > 0.5) m = 1.0 - m;\n" +
				"  float edge = smoothstep( 0.1, 0.9, m );\n" +
				"  if (edge <= 0.0) discard;\n" +
				"  vec2 uv = vUV;\n" +
				"  float fade = 1.0;\n" +
				"  if (uParams.y > 0.0) {\n" +
				"    float d = clamp( ( vWorld.y - uParams.x ) / uParams.y, 0.0, 1.0 );\n" +
				"    fade = 1.0 - d * 0.85;\n" +
				//ripples get stronger further from the feet, like a real reflection
				"    float w = uParams.z * ( 0.4 + d );\n" +
				//gentle bands of two rows, whole pixels only, so the pixel art stays clean
				"    float band = floor( vWorld.y * 0.5 );\n" +
				"    uv.x += floor( sin( band * 0.8 - uParams.w * 2.0 ) * w + 0.5 ) * uTexel;\n" +
				"    uv.x = clamp( uv.x, min( uFrame.x, uFrame.z ), max( uFrame.x, uFrame.z ) );\n" +
				"  }\n" +
				"  vec4 c = texture2D( uTex, uv ) * uColorM + uColorA;\n" +
				"  gl_FragColor = vec4( c.rgb, c.a * fade * edge );\n" +
				"}\n";
		}
	}
}
