/*
 * Atmosphere mod - the water's surface and the mist over the ground
 *
 * Water: wavering nets of light under the surface, and the glitter that torches, braziers and crystals
 * throw across it.
 * Mist: thin fog drifting low over the floor, coloured by the region and lit by whatever light it passes.
 *
 * Both are worked out on the game's own pixel grid, so they stay in step with the pixel art.
 */

package com.shatteredpixel.shatteredpixeldungeon.atmosphere;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glscripts.Script;
import com.watabou.glwrap.Blending;
import com.watabou.glwrap.Texture;
import com.watabou.glwrap.Uniform;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NoosaScript;

import java.nio.ByteBuffer;

public class Surface {

	private static final float TS = DungeonTilemap.SIZE;

	//these are whole-screen shader passes, so they follow the same setting as the post processing
	public static boolean enabled(){
		return Atmosphere.quality() >= 2;
	}

	private static String region( Level level ){
		String t = level.tilesTex();
		return t == null ? "" : t;
	}

	// ================================================================ drifting noise, shared by both

	//64x64, repeating. Red and green hold two unrelated cloud patterns
	private static SmartTexture noiseTex(){
		String key = "surf_noise";
		if (!TextureCache.contains(key)){
			int n = 64;
			SmartTexture tx = TextureCache.create(key, n, n);
			tx.filter(Texture.LINEAR, Texture.LINEAR);
			tx.wrap(Texture.REPEAT, Texture.REPEAT);
			Pixmap p = tx.bitmap;
			p.setBlending(Pixmap.Blending.None);
			for (int y = 0; y < n; y++){
				for (int x = 0; x < n; x++){
					float r = cloud(x, y, n, 11);
					float g = cloud(x, y, n, 57);
					p.drawPixel(x, y, ((int)(r * 255) << 24) | ((int)(g * 255) << 16) | 0xFF);
				}
			}
			//uploaded when first drawn, which is always on the render thread
		}
		return TextureCache.get(key);
	}

	private static float lattice( int x, int y, int period, int seed ){
		x = ((x % period) + period) % period;
		y = ((y % period) + period) % period;
		int h = x * 374761393 + y * 668265263 + seed * 1274126177 + period * 362437;
		h = (h ^ (h >>> 13)) * 1274126177;
		h ^= h >>> 16;
		return (h & 0xFFFF) / 65535f;
	}

	//value noise that wraps around after the given number of cells
	private static float wrapNoise( float x, float y, int period, int seed ){
		int x0 = (int)Math.floor(x), y0 = (int)Math.floor(y);
		float fx = x - x0, fy = y - y0;
		fx = fx * fx * (3f - 2f * fx);
		fy = fy * fy * (3f - 2f * fy);
		float a = lattice(x0, y0, period, seed), b = lattice(x0 + 1, y0, period, seed);
		float c = lattice(x0, y0 + 1, period, seed), d = lattice(x0 + 1, y0 + 1, period, seed);
		return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy;
	}

	private static float cloud( int x, int y, int size, int seed ){
		float u = x / (float)size, v = y / (float)size;
		float n = 0.55f * wrapNoise(u * 4, v * 4, 4, seed)
				+ 0.30f * wrapNoise(u * 8, v * 8, 8, seed + 1)
				+ 0.15f * wrapNoise(u * 16, v * 16, 16, seed + 2);
		return Math.max(0f, Math.min(1f, n));
	}

	//the part after the decimal point: the clouds repeat every whole unit, so only that matters
	private static float wrap( double v ){
		return (float)(v - Math.floor(v));
	}

	private static void bindOn( int unit, SmartTexture tx ){
		if (tx.id == -1){
			tx.bind();
		}
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + unit);
		Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, tx.id);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		Texture.clear();
	}

	// ================================================================ where the ground is open, and where the water is

	//one texel per tile, smoothed: red is open ground, green is water, blue is grass
	// (a third for short grass, two thirds for trampled grass, full for tall grass),
	// alpha is full where the tile uses the second of the two grass drawings and half elsewhere
	private static class GroundMask {
		private static SmartTexture tex;
		private static String key;
		private static int keyCounter = 0;
		private static int hash = 0;
		private static int w2, h2;

		static void update( Level level ){
			int h = 7;
			boolean[] solid = level.solid, water = level.water;
			for (int i = 0; i < level.length(); i++){
				h = h * 31 + (solid[i] ? 1 : 0) + (water[i] ? 2 : 0) + grass(level, i) * 4;
			}
			h = h * 31 + level.width();
			if (tex != null && h == hash) return;
			hash = h;
			build(level);
		}

		private static int grass( Level level, int cell ){
			//a plant growing there keeps its own colours
			if (level.plants.get(cell) != null) return 0;
			switch (level.map[cell]){
				case Terrain.HIGH_GRASS:     return 3;
				case Terrain.FURROWED_GRASS: return 2;
				case Terrain.GRASS:          return 1;
				default:                     return 0;
			}
		}

		private static void build( Level level ){
			int mw = level.width(), mh = level.height();
			int nw = 1; while (nw < mw) nw <<= 1;
			int nh = 1; while (nh < mh) nh <<= 1;
			//grass is trampled all the time: the same texture is written over rather than made anew
			if (tex == null || nw != w2 || nh != h2){
				release();
				w2 = nw; h2 = nh;
				key = "GroundMask_" + (keyCounter++);
				tex = TextureCache.create(key, w2, h2);
				tex.filter(Texture.LINEAR, Texture.LINEAR);
			}
			ByteBuffer px = tex.bitmap.getPixels();
			for (int i = 0; i < w2 * h2 * 4; i++) px.put(i, (byte)0);

			for (int cy = 0; cy < mh; cy++){
				for (int cx = 0; cx < mw; cx++){
					int cell = cy * mw + cx;
					int t = level.map[cell];
					//doors count as closed ground: mist doesn't pour through a doorway's frame
					boolean open = !level.solid[cell] && t != Terrain.DOOR && t != Terrain.OPEN_DOOR
							&& t != Terrain.LOCKED_DOOR && t != Terrain.CRYSTAL_DOOR;
					int o = (cy * w2 + cx) * 4;
					px.put(o,     (byte)(open ? 0xFF : 0));
					px.put(o + 1, (byte)(level.water[cell] ? 0xFF : 0));
					px.put(o + 2, (byte)(grass(level, cell) * 85));
					boolean second = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet.getVisualWithAlts(
							com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet.RAISED_HIGH_GRASS, cell)
							== com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet.RAISED_HIGH_GRASS_ALT;
					px.put(o + 3, (byte)(second ? 0xFF : 0x80));
				}
			}
			tex.bitmap(tex.bitmap);
			Texture.clear();
		}

		static void release(){
			if (key != null){
				TextureCache.remove(key);
				key = null;
				tex = null;
			}
		}

		static boolean ready(){ return tex != null; }
		static float scaleX(){ return 1f / (TS * w2); }
		static float scaleY(){ return 1f / (TS * h2); }
	}

	//called once a frame, before anything here is drawn
	public static void update( Level level ){
		clock += Game.elapsed;
		if (enabled()){
			GroundMask.update(level);
			updateStir(level);
			updateWind();
		}
	}

	private static double clock = 0;

	public static void release(){
		GroundMask.release();
	}

	// ================================================================ lights that shine on the water

	//filled in by the lighting each frame: position x, y in world pixels, how far down the water its
	// glitter reaches, its width; then its colour and strength
	static final int MAX_GLINTS = 4;
	static final float[] glintPos = new float[MAX_GLINTS * 4];
	static final float[] glintCol = new float[MAX_GLINTS * 4];
	static int glintCount = 0;

	// ================================================================ the water's surface

	//laid over the water and under the floor tiles, like the reflections: the banks cover its edges
	public static class Water extends Image {

		private double time = 0;
		private float cr = 1, cg = 1, cb = 1, cStrength = 0;
		private float flow = 1f, glintK = 1f;

		public Water(){
			super(TextureCache.createSolid(0xFFFFFFFF));
		}

		private void setup( Level level ){
			scale.set(level.width() * TS, level.height() * TS);
			x = y = 0;

			flow = 1f; glintK = 1f;
			switch (region(level)){
				case Assets.Environment.TILES_SEWERS:
					caustic(0.70f, 1.00f, 0.86f, 0.42f); break;
				case Assets.Environment.TILES_PRISON:
					caustic(0.70f, 0.86f, 1.00f, 0.30f); break;
				case Assets.Environment.TILES_CAVES:
				case Assets.Environment.TILES_CAVES_GNOLL:
					caustic(0.72f, 1.00f, 0.92f, 0.34f); break;
				case Assets.Environment.TILES_CAVES_CRYSTAL:
					caustic(0.66f, 0.90f, 1.00f, 0.40f); break;
				case Assets.Environment.TILES_CITY:
					caustic(0.80f, 0.95f, 1.00f, 0.30f); break;
				case Assets.Environment.TILES_HALLS:
					//streams of fire: slow veins of brighter heat, and nothing mirrored in them
					caustic(1.00f, 0.62f, 0.20f, 0.45f);
					flow = 0.45f; glintK = 0f;
					break;
				default:
					caustic(0.75f, 0.95f, 1.00f, 0.32f); break;
			}
		}

		private void caustic( float r, float g, float b, float s ){ cr = r; cg = g; cb = b; cStrength = s; }

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			Level level = Dungeon.level;
			visible = level != null && enabled();
			if (visible) setup(level);
		}

		@Override
		protected NoosaScript script() {
			return WaterScript.get();
		}

		@Override
		public void draw() {
			if (!WaterMask.ready() || texture == null) return;
			WaterScript s = WaterScript.get();
			s.resetCamera();
			WaterMask.bind();
			bindOn(2, noiseTex());
			Gdx.gl.glUniform1i(s.uMask.location(), 1);
			Gdx.gl.glUniform1i(s.uNoise.location(), 2);
			s.uMaskScale.value2f(WaterMask.scaleX(), WaterMask.scaleY());
			s.uTime.value1f((float)(time % 600.0));
			s.uCaustic.value4f(cr, cg, cb, cStrength);
			//how far each of the three cloud layers has slid by now
			double t = time * flow;
			s.uSlideA.value4f(wrap(t * 0.030), wrap(t * 0.016), wrap(-t * 0.025), wrap(t * 0.019));
			s.uSlideB.value2f(wrap(-t * 0.012), wrap(t * 0.004));

			Uniform[] lp = {s.uL0, s.uL1, s.uL2, s.uL3};
			Uniform[] lc = {s.uC0, s.uC1, s.uC2, s.uC3};
			for (int i = 0; i < MAX_GLINTS; i++){
				if (i < glintCount && glintK > 0){
					lp[i].value4f(glintPos[i*4], glintPos[i*4+1], glintPos[i*4+2], glintPos[i*4+3]);
					lc[i].value4f(glintCol[i*4], glintCol[i*4+1], glintCol[i*4+2], glintCol[i*4+3] * glintK);
				} else {
					lp[i].value4f(0, 0, 1, 1);
					lc[i].value4f(0, 0, 0, 0);
				}
			}

			//light, added onto the water
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	private static final String VERTEX =
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
			"uniform sampler2D uTex;\n";

	public static class WaterScript extends NoosaScript {
		public Uniform uMask, uNoise, uMaskScale, uTime, uCaustic, uSlideA, uSlideB;
		public Uniform uL0, uL1, uL2, uL3, uC0, uC1, uC2, uC3;

		public WaterScript(){
			super();
			uMask      = uniform("uMask");
			uNoise     = uniform("uNoise");
			uMaskScale = uniform("uMaskScale");
			uTime      = uniform("uTime");
			uCaustic   = uniform("uCaustic");
			uSlideA    = uniform("uSlideA");
			uSlideB    = uniform("uSlideB");
			uL0 = uniform("uL0"); uL1 = uniform("uL1"); uL2 = uniform("uL2"); uL3 = uniform("uL3");
			uC0 = uniform("uC0"); uC1 = uniform("uC1"); uC2 = uniform("uC2"); uC3 = uniform("uC3");
		}

		public static WaterScript get(){
			return Script.use(WaterScript.class);
		}

		@Override
		public void lighting(float a, float b, float c, float d, float e, float f, float g, float h) {}

		@Override
		protected String shader() {
			return VERTEX +
				"uniform sampler2D uMask;\n" +
				"uniform sampler2D uNoise;\n" +
				"uniform vec2 uMaskScale;\n" +
				"uniform float uTime;\n" +
				"uniform vec4 uSlideA;\n" +
				"uniform vec2 uSlideB;\n" +
				"uniform vec4 uCaustic;\n" +
				"uniform vec4 uL0;\n" + "uniform vec4 uL1;\n" + "uniform vec4 uL2;\n" + "uniform vec4 uL3;\n" +
				"uniform vec4 uC0;\n" + "uniform vec4 uC1;\n" + "uniform vec4 uC2;\n" + "uniform vec4 uC3;\n" +

				//a light's glitter on the water: a broken streak running from the light towards the viewer
				"vec3 glint( vec2 p, vec4 l, vec4 c ){\n" +
				"  vec2 d = p - l.xy;\n" +
				"  if (c.a <= 0.0 || d.y <= 0.0 || d.y >= l.z) return vec3( 0.0 );\n" +
				"  float k = d.y / l.z;\n" +
				"  float row = floor( p.y );\n" +
				//every row of pixels is its own dash: its own width, its own small shift, its own slow blinking
				"  float rr = mod( row, 61.0 ) + mod( l.x, 37.0 ) * 0.731;\n" +
				"  float r1 = fract( sin( rr * 12.9898 ) * 43758.5453 );\n" +
				"  float r2 = fract( sin( rr * 78.233 + 1.7 ) * 24634.6345 );\n" +
				"  float w = l.w * ( 0.5 + 1.9 * k ) * ( 0.35 + 0.95 * r2 );\n" +
				"  float slide = ( r1 - 0.5 ) * ( 1.0 + 4.0 * k ) + sin( uTime * 1.4 + r1 * 6.2831 ) * ( 0.3 + 1.2 * k );\n" +
				"  float s = clamp( ( w - abs( d.x + slide ) ) + 0.5, 0.0, 1.0 );\n" +
				"  float blink = 0.5 + 0.5 * sin( uTime * ( 1.3 + 2.2 * r2 ) + r1 * 6.2831 );\n" +
				"  blink = smoothstep( 0.25 + 0.35 * k, 0.9, blink );\n" +
				"  float fall = ( 1.0 - k ) * ( 1.0 - 0.6 * k );\n" +
				//under the dashes, a faint wide sheen
				"  float sheen = max( 0.0, 1.0 - abs( d.x ) / ( l.w * ( 2.0 + 5.0 * k ) ) );\n" +
				"  return c.rgb * ( c.a * fall * ( s * ( 0.2 + 0.8 * blink ) + sheen * sheen * 0.22 ) );\n" +
				"}\n" +

				"void main() {\n" +
				//everything is worked out per game pixel
				"  vec2 p = floor( vWorld ) + 0.5;\n" +
				"  float m = texture2D( uMask, p * uMaskScale ).a;\n" +
				"  float base = texture2D( uTex, vUV ).a;\n" +
				"  vec2 q = p / 16.0;\n" +
				//nets of light: two cloud patterns slide across each other, and it is bright along the lines
				// where they meet. The lines wander and re-knit all the time, the way light does under ripples
				"  float c1 = texture2D( uNoise, q * 0.300 + uSlideA.xy ).r;\n" +
				"  float c2 = texture2D( uNoise, q * 0.380 + uSlideA.zw + vec2( 0.31, 0.77 ) ).g;\n" +
				"  float c3 = texture2D( uNoise, q * 0.150 + uSlideB + vec2( 0.50, 0.20 ) ).r;\n" +
				"  float c = clamp( 1.0 - abs( c1 - c2 ) * 7.0, 0.0, 1.0 );\n" +
				//brighter in some stretches than others, and a touch stronger out in the deeper water
				"  c = c * c * ( 0.35 + 1.1 * clamp( c3 * 1.6 - 0.4, 0.0, 1.0 ) ) * ( 0.7 + 0.3 * m );\n" +
				"  vec3 add = uCaustic.rgb * ( c * uCaustic.a );\n" +

				"  add += glint( p, uL0, uC0 ) + glint( p, uL1, uC1 ) + glint( p, uL2, uC2 ) + glint( p, uL3, uC3 );\n" +

				"  gl_FragColor = vec4( add * base, 1.0 );\n" +
				"}\n";
		}
	}

	// ================================================================ mist over the ground

	public static class Mist extends Image {

		private double time = 0;
		private float r = 1, g = 1, b = 1, density = 0;
		private float lo = 0.40f, hi = 0.70f, overWater = 0f;
		private float windX = 1f, windY = 0.3f;
		private float heroX, heroY;
		//how much of the mist is left right around the hero, so it never hides what is next to them
		private float nearHero = 0.4f;
		private float shown = 0f;

		public Mist(){
			super(TextureCache.createSolid(0xFFFFFFFF));
		}

		private void setup( Level level ){
			scale.set(level.width() * TS, level.height() * TS);
			x = y = 0;

			overWater = 0f; lo = 0.40f; hi = 0.70f; windX = 1f; windY = 0.3f;
			switch (region(level)){
				case Assets.Environment.TILES_SEWERS:
					//damp, greenish, thickest where it rises off the water
					color(0.80f, 0.88f, 0.84f, 0.55f); overWater = 0.35f; break;
				case Assets.Environment.TILES_PRISON:
					//cold and thin
					color(0.83f, 0.86f, 0.93f, 0.40f); overWater = 0.2f; break;
				case Assets.Environment.TILES_CAVES:
				case Assets.Environment.TILES_CAVES_GNOLL:
					//dust hanging in still air
					color(0.89f, 0.84f, 0.77f, 0.40f); overWater = 0.15f; windX = 0.5f; windY = 0.15f; break;
				case Assets.Environment.TILES_CAVES_CRYSTAL:
					color(0.79f, 0.89f, 0.98f, 0.42f); overWater = 0.2f; windX = 0.5f; windY = 0.15f; break;
				case Assets.Environment.TILES_CITY:
					//smoke from the braziers
					color(0.90f, 0.87f, 0.79f, 0.36f); break;
				case Assets.Environment.TILES_HALLS:
					//heavy red haze, rising off the fire
					color(0.85f, 0.45f, 0.35f, 0.40f); overWater = 0.25f; windX = 0.4f; windY = -0.8f; break;
				default:
					color(0.86f, 0.87f, 0.89f, 0.32f); break;
			}

			if (level.feeling == Level.Feeling.WATER) density *= 1.25f;
			if (level.feeling == Level.Feeling.DARK)  density *= 1.15f;
			//a boss fight needs to stay easy to read
			if (Ambience.bossVisible || Dungeon.bossLevel()) density *= 0.6f;
		}

		private void color( float r, float g, float b, float density ){
			this.r = r; this.g = g; this.b = b; this.density = density;
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			Level level = Dungeon.level;
			visible = level != null && enabled();
			if (!visible) return;

			setup(level);
			//fades in after arriving on a floor rather than being there at once
			shown = Math.min(1f, shown + Game.elapsed * 0.5f);

			if (Dungeon.hero != null && Dungeon.hero.sprite != null){
				CharSprite s = Dungeon.hero.sprite;
				heroX = s.x + s.width() / 2f;
				heroY = s.y + s.height() - 3f;
			}
		}

		@Override
		protected NoosaScript script() {
			return MistScript.get();
		}

		@Override
		public void draw() {
			if (!GroundMask.ready() || texture == null || density <= 0f) return;
			MistScript s = MistScript.get();
			s.resetCamera();
			bindOn(1, GroundMask.tex);
			bindOn(2, noiseTex());
			Gdx.gl.glUniform1i(s.uMask.location(), 1);
			Gdx.gl.glUniform1i(s.uNoise.location(), 2);
			s.uMaskScale.value2f(GroundMask.scaleX(), GroundMask.scaleY());
			s.uColor.value4f(r, g, b, density * shown);
			s.uShape.value4f(lo, hi, overWater, nearHero);
			//the two cloud layers drift with the wind, the second across the first
			s.uWind.value4f(wrap(time * 0.0090 * windX), wrap(time * 0.0090 * windY),
					wrap(-time * 0.0140 * windY), wrap(time * 0.0140 * windX));
			s.uHero.value2f(heroX, heroY);
			Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
			super.draw();
			Blending.setNormalMode();
		}

		@Override
		public void destroy() {
			super.destroy();
			GroundMask.release();
		}
	}

	public static class MistScript extends NoosaScript {
		public Uniform uMask, uNoise, uMaskScale, uColor, uShape, uWind, uHero;

		public MistScript(){
			super();
			uMask      = uniform("uMask");
			uNoise     = uniform("uNoise");
			uMaskScale = uniform("uMaskScale");
			uColor     = uniform("uColor");
			uShape     = uniform("uShape");
			uWind      = uniform("uWind");
			uHero      = uniform("uHero");
		}

		public static MistScript get(){
			return Script.use(MistScript.class);
		}

		@Override
		public void lighting(float a, float b, float c, float d, float e, float f, float g, float h) {}

		@Override
		protected String shader() {
			return VERTEX +
				"uniform sampler2D uMask;\n" +
				"uniform sampler2D uNoise;\n" +
				"uniform vec2 uMaskScale;\n" +
				"uniform vec4 uColor;\n" +   // rgb, density
				"uniform vec4 uShape;\n" +   // noise low, noise high, extra over water, share left near the hero
				"uniform vec4 uWind;\n" +
				"uniform vec2 uHero;\n" +
				"void main() {\n" +
				"  vec2 p = floor( vWorld ) + 0.5;\n" +
				"  vec4 mk = texture2D( uMask, p * uMaskScale );\n" +
				//thins out towards the walls
				"  float open = smoothstep( 0.30, 0.85, mk.r );\n" +
				"  if (open <= 0.0) discard;\n" +
				"  vec2 q = p / 16.0;\n" +
				//two layers of cloud sliding past each other, so the shapes keep changing
				"  float n1 = texture2D( uNoise, q * 0.105 + uWind.xy ).r;\n" +
				"  float n2 = texture2D( uNoise, q * 0.230 + uWind.zw + 0.37 ).g;\n" +
				"  float d = smoothstep( uShape.x, uShape.y, n1 * 0.62 + n2 * 0.38 );\n" +
				"  float dens = uColor.a * ( 1.0 + uShape.z * mk.g );\n" +
				"  float hd = distance( p, uHero ) / 16.0;\n" +
				"  dens *= mix( uShape.w, 1.0, smoothstep( 0.6, 3.0, hd ) );\n" +
				"  float a = d * dens * open * texture2D( uTex, vUV ).a;\n" +
				//tall grass shades the ground at its foot: a soft dark fringe just below and beside it
				"  float ownGrass = texture2D( uMask, ( floor( p / 16.0 ) + 0.5 ) * 16.0 * uMaskScale ).b;\n" +
				"  float nearGrass = texture2D( uMask, ( p - vec2( 1.5, 3.0 ) ) * uMaskScale ).b;\n" +
				"  float shade = smoothstep( 0.35, 0.95, nearGrass ) * step( ownGrass, 0.9 ) * 0.30 * open;\n" +
				//the mist is laid over, the shade darkens what is under both
				"  gl_FragColor = vec4( uColor.rgb * a, a + shade * ( 1.0 - a ) );\n" +
				"}\n";
		}
	}

	// ================================================================ grass

	//The tile layers that hold grass are drawn through this in place of the plain shader.
	//
	//Movement: every blade is rooted and bends from there, the tip most. At rest the tips barely stir.
	// Now and then a gust crosses the view - when, how hard, how fast and from which side all vary -
	// and the grass leans over as its front passes, flutters while held down, and swings back.
	// Tall grass leans right out over the tile beside it, so a lone tuft bends as freely as a field.
	// Creatures part the grass they stand in and set it rustling as they push through.
	//Colour: gentle lighter, darker and drier patches, tall grass shaded from its roots up, the bent
	// tips catching the light in a gust, and now and then a blade glinting.
	//
	//All of it moves whole pixels of the original art, so it stays pixel art.
	//rowsBelow: the layer drawing the tips that hang over the tile above looks one tile down for its grass
	//tallTile, tallTileAlt: where on this layer's sheet the two drawings of tall grass are
	public static NoosaScript grass( int rowsBelow, SmartTexture sheet, int tallTile, int tallTileAlt ){
		if (!enabled() || !GroundMask.ready() || sheet == null){
			return com.watabou.noosa.NoosaScriptNoLighting.get();
		}
		GrassScript s = GrassScript.get();
		s.resetCamera();
		bindOn(1, GroundMask.tex);
		bindOn(2, noiseTex());
		Gdx.gl.glUniform1i(s.uMask.location(), 1);
		Gdx.gl.glUniform1i(s.uNoise.location(), 2);
		s.uMaskScale.value2f(GroundMask.scaleX(), GroundMask.scaleY());
		s.uSheet.value2f(sheet.width, sheet.height);
		int cols = Math.max(1, sheet.width / 16);
		s.uTall.value4f((tallTile % cols) * 16, (tallTile / cols) * 16, (tallTileAlt % cols) * 16, (tallTileAlt / cols) * 16);
		s.uBelow.value1f(rowsBelow);
		s.uTime.value1f((float)(clock % 600.0));
		s.uWind.value4f(gustFront, gustOn ? gustStrength : 0f, gustDir, gustWidth);
		Uniform[] ch = {s.uCh0, s.uCh1, s.uCh2, s.uCh3};
		for (int i = 0; i < MAX_STIR; i++){
			if (i < stirCount) ch[i].value4f(stir[i*3], stir[i*3+1], stir[i*3+2], 1f);
			else               ch[i].value4f(0, 0, 0, 0);
		}
		return s;
	}

	//The floor layer under the grass has the same blades painted into its tiles. Left alone they would
	// stand still under the moving ones and show through as a second, frozen set. So where the floor
	// tile has a blade it is shown as the dark ground between the blades instead.
	public static NoosaScript ground(){
		if (!enabled() || !GroundMask.ready()){
			return com.watabou.noosa.NoosaScriptNoLighting.get();
		}
		SmartTexture blades = TextureCache.get(Assets.Environment.TERRAIN_FEATURES);
		GroundScript s = GroundScript.get();
		s.resetCamera();
		bindOn(1, GroundMask.tex);
		bindOn(3, blades);
		Gdx.gl.glUniform1i(s.uMask.location(), 1);
		Gdx.gl.glUniform1i(s.uBlades.location(), 3);
		s.uMaskScale.value2f(GroundMask.scaleX(), GroundMask.scaleY());
		//the grass drawings of this region sit side by side in one row of the sheet:
		// tall, tall again, trampled, trampled again
		int stage = (Dungeon.depth - 1) / 5;
		if (Dungeon.depth == 21 && Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.LastShopLevel) stage--;
		stage = Math.min(stage, 4);
		s.uBladeRow.value4f((8 + stage) * 16, blades.width, blades.height, 0f);
		return s;
	}

	public static class GroundScript extends NoosaScript {
		public Uniform uMask, uBlades, uMaskScale, uBladeRow;

		public GroundScript(){
			super();
			uMask      = uniform("uMask");
			uBlades    = uniform("uBlades");
			uMaskScale = uniform("uMaskScale");
			uBladeRow  = uniform("uBladeRow");
		}

		public static GroundScript get(){
			return Script.use(GroundScript.class);
		}

		@Override
		public void lighting(float a, float b, float c, float d, float e, float f, float g, float h) {}

		@Override
		protected String shader() {
			return VERTEX +
				"uniform sampler2D uMask;\n" +
				"uniform sampler2D uBlades;\n" +
				"uniform vec2 uMaskScale;\n" +
				"uniform vec4 uBladeRow;\n" +   // y of the row of grass drawings on their sheet, the sheet's width and height
				"void main() {\n" +
				"  vec4 base = texture2D( uTex, vUV );\n" +
				"  vec2 p = floor( vWorld ) + 0.5;\n" +
				"  if (texture2D( uMask, p * uMaskScale ).b > 0.0) {\n" +
				"    vec4 own = texture2D( uMask, ( floor( p / 16.0 ) + 0.5 ) * 16.0 * uMaskScale );\n" +
				//tall or trampled grass. Short grass doesn't move, so its floor stays as painted
				"    if (own.b > 0.5) {\n" +
				"      float which = ( own.b > 0.9 ? 0.0 : 2.0 ) + ( own.a > 0.75 ? 1.0 : 0.0 );\n" +
				"      vec2 inTile = floor( fract( vWorld / 16.0 ) * 16.0 );\n" +
				"      float blade = texture2D( uBlades, ( vec2( which * 16.0, uBladeRow.x ) + inTile + 0.5 ) / uBladeRow.yz ).a;\n" +
				"      if (blade > 0.5) base.rgb *= 0.34;\n" +
				"    }\n" +
				"  }\n" +
				"  gl_FragColor = base;\n" +
				"}\n";
		}
	}

	//whether the grass may lean out of its own tile: the tile layers then give the empty tiles beside
	// tall grass something (blank) to draw, so the blades have somewhere to lean into
	public static boolean grassLeans(){
		return enabled();
	}

	//one gust at a time crosses the view, then a calm of uneven length
	private static boolean gustOn = false;
	private static float gustWait = 3f;
	private static float gustFront, gustEnd, gustSpeed, gustStrength, gustDir = 1f, gustWidth = 120f;

	//a gust's front is a slanted line; this is how far along its path a point lies
	private static float along( float x, float y, float dir ){
		return x * 0.93f * dir + y * 0.36f;
	}

	private static void updateWind(){
		com.watabou.noosa.Camera c = com.watabou.noosa.Camera.main;
		if (c == null) return;
		if (!gustOn){
			gustWait -= Game.elapsed;
			if (gustWait > 0) return;
			gustOn = true;
			//mostly from one side, now and then from the other
			gustDir = com.watabou.utils.Random.Int(5) == 0 ? -1f : 1f;
			//most are light, a few lay the grass flat
			float r = com.watabou.utils.Random.Float();
			gustStrength = 0.35f + 0.85f * r * r;
			gustSpeed = com.watabou.utils.Random.Float(55f, 100f);
			gustWidth = com.watabou.utils.Random.Float(85f, 140f);
			float x0 = c.scroll.x, x1 = c.scroll.x + c.width, y0 = c.scroll.y, y1 = c.scroll.y + c.height;
			float a = Math.min(Math.min(along(x0, y0, gustDir), along(x1, y0, gustDir)), Math.min(along(x0, y1, gustDir), along(x1, y1, gustDir)));
			float b = Math.max(Math.max(along(x0, y0, gustDir), along(x1, y0, gustDir)), Math.max(along(x0, y1, gustDir), along(x1, y1, gustDir)));
			//starts just out of sight and runs until all of it has passed out the far side
			gustFront = a - 45f;
			gustEnd = b + gustWidth * 2.1f + 45f;
		} else {
			gustFront += gustSpeed * Game.elapsed;
			if (gustFront > gustEnd){
				gustOn = false;
				gustWait = com.watabou.utils.Random.Float(4.5f, 16f);
			}
		}
	}

	//creatures on the ground near the hero: x, the y of their feet, and how much they are stirring the grass
	private static final int MAX_STIR = 4;
	private static final float[] stir = new float[MAX_STIR * 3];
	private static int stirCount = 0;
	private static final java.util.HashMap<com.shatteredpixel.shatteredpixeldungeon.actors.Char, Float> stirEnergy = new java.util.HashMap<>();

	private static void updateStir( Level level ){
		stirCount = 0;
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero = Dungeon.hero;
		if (hero == null || hero.sprite == null) return;

		java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.actors.Char> all = new java.util.ArrayList<>();
		all.add(hero);
		java.util.Collections.addAll(all, Atmosphere.mobs(level));

		float hx = hero.sprite.x, hy = hero.sprite.y;
		float[] dist = new float[MAX_STIR];
		for (com.shatteredpixel.shatteredpixeldungeon.actors.Char c : all){
			CharSprite sp = c.sprite;
			if (sp == null || !sp.visible || sp.parent == null || !c.isAlive() || c.flying) continue;
			if (c.pos < 0 || c.pos >= level.length() || !level.heroFOV[c.pos]) continue;

			Float e = stirEnergy.get(c);
			float energy = e == null ? 0f : e;
			if (sp.isMoving) energy = 1f;
			else energy = Math.max(0f, energy - Game.elapsed * 1.8f);
			stirEnergy.put(c, energy);

			float d = (sp.x - hx) * (sp.x - hx) + (sp.y - hy) * (sp.y - hy);
			int slot;
			if (stirCount < MAX_STIR){
				slot = stirCount++;
			} else {
				slot = 0;
				for (int i = 1; i < MAX_STIR; i++) if (dist[i] > dist[slot]) slot = i;
				if (dist[slot] <= d) continue;
			}
			dist[slot] = d;
			stir[slot*3]   = sp.x + sp.width() / 2f;
			stir[slot*3+1] = sp.y + sp.height();
			stir[slot*3+2] = energy;
		}
		if (stirEnergy.size() > 64) stirEnergy.clear();
	}

	public static class GrassScript extends NoosaScript {
		public Uniform uMask, uNoise, uMaskScale, uSheet, uTall, uBelow, uTime, uWind, uCh0, uCh1, uCh2, uCh3;

		public GrassScript(){
			super();
			uMask      = uniform("uMask");
			uNoise     = uniform("uNoise");
			uMaskScale = uniform("uMaskScale");
			uSheet     = uniform("uSheet");
			uTall      = uniform("uTall");
			uBelow     = uniform("uBelow");
			uTime      = uniform("uTime");
			uWind      = uniform("uWind");
			uCh0 = uniform("uCh0"); uCh1 = uniform("uCh1"); uCh2 = uniform("uCh2"); uCh3 = uniform("uCh3");
		}

		public static GrassScript get(){
			return Script.use(GrassScript.class);
		}

		@Override
		public void lighting(float a, float b, float c, float d, float e, float f, float g, float h) {}

		@Override
		protected String shader() {
			return VERTEX +
				"uniform sampler2D uMask;\n" +
				"uniform sampler2D uNoise;\n" +
				"uniform vec2 uMaskScale;\n" +
				"uniform vec2 uSheet;\n" +
				"uniform vec4 uTall;\n" +    // where the two drawings of tall grass are on the sheet, in pixels
				"uniform float uBelow;\n" +
				"uniform float uTime;\n" +
				"uniform vec4 uWind;\n" +    // the gust: how far its front has come, strength, side it comes from, width
				"uniform vec4 uCh0;\n" + "uniform vec4 uCh1;\n" + "uniform vec4 uCh2;\n" + "uniform vec4 uCh3;\n" +

				"float hash( vec2 p ){\n" +
				"  p = fract( p * vec2( 0.1031, 0.1030 ) );\n" +
				"  p += dot( p, p.yx + 33.33 );\n" +
				"  return fract( ( p.x + p.y ) * p.y );\n" +
				"}\n" +

				//a creature standing in the grass: the blades around it lean away to either side,
				// and shake while it is moving
				"float part( vec2 p, float rootY, vec4 ch ){\n" +
				"  if (ch.w <= 0.0) return 0.0;\n" +
				"  vec2 d = vec2( p.x - ch.x, ( rootY - ch.y ) * 1.3 );\n" +
				"  float near = max( 0.0, 1.0 - length( d ) / 15.0 );\n" +
				"  float side = d.x < 0.0 ? -1.0 : 1.0;\n" +
				"  return near * ( side * 2.6 + ch.z * 2.2 * sin( uTime * 17.0 + p.x * 0.06 + rootY * 0.4 ) );\n" +
				"}\n" +

				"void main() {\n" +
				"  vec4 base = texture2D( uTex, vUV );\n" +
				"  vec2 p = floor( vWorld ) + 0.5;\n" +
				//smoothed, this is above zero anywhere within reach of grass, and nowhere else:
				// everything that isn't near grass is done here
				"  if (texture2D( uMask, ( p + vec2( 0.0, 16.0 * uBelow ) ) * uMaskScale ).b <= 0.0) {\n" +
				"    gl_FragColor = base;\n" +
				"    return;\n" +
				"  }\n" +

				//the tile whose grass shows here: this one, or for the overhanging tips the one below
				"  vec2 gcell = floor( p / 16.0 ) + vec2( 0.0, uBelow );\n" +
				"  vec4 own = texture2D( uMask, ( gcell + 0.5 ) * 16.0 * uMaskScale );\n" +
				"  float kind = own.b;\n" +
				//short grass has no tips reaching the tile above
				"  if (uBelow > 0.5 && kind < 0.5) kind = 0.0;\n" +
				//where the blades here are rooted, and how high up them this pixel is
				"  float rootY = ( gcell.y + 1.0 ) * 16.0;\n" +
				"  float high = rootY - p.y;\n" +
				"  float hw = pow( clamp( high / 20.0, 0.0, 1.25 ), 1.4 );\n" +

				"  vec2 q = p / 16.0;\n" +
				"  float n1 = texture2D( uNoise, q * 0.090 + vec2( 0.13, 0.57 ) ).r;\n" +
				"  float n2 = texture2D( uNoise, q * 0.210 + vec2( 0.71, 0.29 ) ).g;\n" +
				//each stretch of a row moves in its own time, changing smoothly along the row
				"  float ph = texture2D( uNoise, vec2( q.x * 0.17, gcell.y * 0.37 + 0.11 ) ).g * 12.566;\n" +

				//at rest: hardly more than a stir at the tips
				"  float bend = sin( uTime * 1.7 + ph ) * 0.60 + sin( uTime * 2.9 + ph * 1.7 + p.x * 0.11 ) * 0.30;\n" +
				//the gust: its ragged front sweeps across; the grass goes over, flutters, and swings back
				"  float f = ( uWind.x - ( p.x * 0.93 * uWind.z + rootY * 0.36 ) + ( n1 - 0.5 ) * 50.0 ) / uWind.w;\n" +
				"  float gust = smoothstep( 0.0, 0.18, f ) * ( 1.0 - smoothstep( 0.24, 1.2, f ) ) * uWind.y;\n" +
				"  float back = smoothstep( 1.2, 1.45, f ) * ( 1.0 - smoothstep( 1.45, 2.0, f ) ) * uWind.y;\n" +
				//(the flutter changes only slowly from one blade to the next: neighbours torn different ways
				// would smear the drawing)
				"  bend += uWind.z * ( gust * ( 3.4 + sin( uTime * 13.0 + ph * 1.5 + p.x * 0.05 ) * 0.9 ) - back );\n" +
				"  bend += part( p, rootY, uCh0 ) + part( p, rootY, uCh1 ) + part( p, rootY, uCh2 ) + part( p, rootY, uCh3 );\n" +

				"  vec2 inTile = floor( fract( vWorld / 16.0 ) * 16.0 );\n" +
				"  vec4 c = vec4( 0.0 );\n" +
				"  float tall = 0.0;\n" +

				//tall grass: the blade seen here is rooted this many pixels to the side, which may be in this
				// tile or in the one next to it - that is how it leans out over its neighbour
				"  float shift = clamp( floor( bend * hw + 0.5 ), -7.0, 7.0 );\n" +
				"  float sx = inTile.x - shift;\n" +
				"  float side = floor( sx / 16.0 );\n" +
				"  vec4 src = own;\n" +
				"  if (abs( side ) > 0.5) src = texture2D( uMask, ( gcell + vec2( side, 0.0 ) + 0.5 ) * 16.0 * uMaskScale );\n" +
				"  if (src.b > 0.9) {\n" +
				"    vec2 org = src.a > 0.75 ? uTall.zw : uTall.xy;\n" +
				"    c = texture2D( uTex, ( org + vec2( sx - side * 16.0, inTile.y ) + 0.5 ) / uSheet );\n" +
				"    tall = 1.0;\n" +
				//whatever this tile draws of its own stands in front of a blade leaning in
				"    if (kind < 0.2 && base.a > 0.0) c = vec4( 0.0 );\n" +
				"  }\n" +

				"  if (c.a <= 0.0) {\n" +
				"    tall = 0.0;\n" +
				//tall grass that has leaned away leaves a gap
				"    if (kind > 0.9) discard;\n" +
				"    if (kind < 0.2) {\n" +
				"      gl_FragColor = base;\n" +
				"      return;\n" +
				"    }\n" +
				//trampled grass moves little and stays within its own tile: what leaves one side comes in
				// at the other. Short grass is only specks on the floor and doesn't move
				"    float low = kind > 0.5 ? floor( bend * hw * 0.55 + 0.5 ) : 0.0;\n" +
				"    vec2 origin = floor( ( vUV * uSheet - fract( vWorld / 16.0 ) * 16.0 ) / 16.0 + 0.5 ) * 16.0;\n" +
				"    c = texture2D( uTex, ( origin + vec2( mod( inTile.x - low, 16.0 ), inTile.y ) + 0.5 ) / uSheet );\n" +
				"    if (c.a <= 0.0) discard;\n" +
				"  }\n" +

				//patches: faint ones for light and dark, and where the grass has gone a little dry or lush
				"  c.rgb *= 0.91 + 0.18 * n1;\n" +
				"  float dry = smoothstep( 0.52, 0.70, n2 );\n" +
				"  c.rgb = mix( c.rgb, c.rgb * vec3( 1.14, 1.02, 0.72 ), dry * 0.28 );\n" +
				"  float lush = smoothstep( 0.55, 0.75, 1.0 - n2 );\n" +
				"  c.rgb = mix( c.rgb, c.rgb * vec3( 0.86, 1.04, 1.02 ), lush * 0.25 );\n" +

				//tall grass stands in its own shade: dark at the roots, light at the tips
				"  float up = clamp( high / 24.0, 0.0, 1.0 );\n" +
				"  c.rgb *= mix( 1.0, mix( 0.88, 1.07, up ), tall );\n" +
				//bent over in a gust, the tips turn their pale side to the light
				"  c.rgb *= 1.0 + gust * 0.08 * up * ( 0.5 + 0.5 * tall );\n" +

				//a blade catching the light for a moment
				"  float h = hash( p );\n" +
				"  float tw = sin( uTime * ( 0.7 + 1.6 * h ) + h * 40.0 );\n" +
				"  float glint = step( 0.965, h ) * smoothstep( 0.86, 1.0, tw ) * ( 0.5 + 0.5 * tall );\n" +
				"  c.rgb += vec3( 0.30, 0.34, 0.22 ) * glint * step( 0.25, c.g );\n" +
				"  gl_FragColor = c;\n" +
				"}\n";
		}
	}

	// ================================================================ what a blow leaves on the floor

	//What a creature leaves behind when it is struck depends on what it is made of.
	// Liquid (blood, ichor, sap, oil) is thrown away from the blow as a spray of drops, thick where it
	//  starts and thinning out, with a small ragged splash under a heavy hit. It shines while wet and
	//  darkens as it dries.
	// Fragments (bone, stone, crystal, wood, embers, frost) scatter as chips and splinters, dry from the start.
	// Slime drops in a few thick glossy gobbets close by, and never dries.
	public static final int LIQUID = 0, FRAGMENTS = 1, GOBBETS = 2;

	public static class Stain extends Image {

		public float cx, cy, age, life;
		private int form;
		private float reach, core, amount, dirX, dirY;
		private float r, g, b, dr, dg, db;
		private float drySecs, wetSecs;
		private boolean glints;
		private float seedX, seedY;

		public Stain(){
			super(TextureCache.createSolid(0xFFFFFFFF));
		}

		//reach: how far the furthest of it lands. core: the size of the splash in the middle, 0 for none.
		// dirX, dirY: which way it was thrown, 0 0 for all round. amount: how much of it, around 1.
		// color: fresh. dried: what it turns to over drySecs. wetSecs: how long it shines
		public void reset( float x, float y, int form, int color, int dried, float reach, float core,
		                   float dirX, float dirY, float amount, float life, float drySecs, float wetSecs, boolean glints ){
			revive();
			visible = true;
			cx = (float)Math.floor(x) + 0.5f;
			cy = (float)Math.floor(y) + 0.5f;
			this.form = form;
			r = ((color >> 16) & 0xFF) / 255f; g = ((color >> 8) & 0xFF) / 255f; b = (color & 0xFF) / 255f;
			dr = ((dried >> 16) & 0xFF) / 255f; dg = ((dried >> 8) & 0xFF) / 255f; db = (dried & 0xFF) / 255f;
			this.reach = reach;
			this.core = core;
			this.dirX = dirX;
			this.dirY = dirY;
			this.amount = amount;
			this.life = life;
			this.drySecs = drySecs;
			this.wetSecs = wetSecs;
			this.glints = glints;
			age = 0;
			seedX = com.watabou.utils.Random.Float();
			seedY = com.watabou.utils.Random.Float();
			float w = reach + 3f;
			scale.set(w * 2f, w * 2f);
			this.x = cx - w;
			this.y = cy - w;
		}

		@Override
		public void update() {
			super.update();
			age += Game.elapsed;
			if (age >= life) kill();
		}

		@Override
		protected NoosaScript script() {
			return StainScript.get();
		}

		@Override
		public void draw() {
			if (!WaterMask.ready() || texture == null) return;
			StainScript s = StainScript.get();
			s.resetCamera();
			WaterMask.bind();
			bindOn(2, noiseTex());
			Gdx.gl.glUniform1i(s.uMask.location(), 1);
			Gdx.gl.glUniform1i(s.uNoise.location(), 2);
			s.uMaskScale.value2f(WaterMask.scaleX(), WaterMask.scaleY());

			//it lands over the first moments: the nearest drops first, the furthest last
			float landed = Math.min(1f, age / 0.22f);
			s.uPool.value4f(cx, cy, reach, core);
			s.uThrow.value4f(dirX, dirY, form, amount);
			s.uSeed.value2f(seedX, seedY);

			float dry = drySecs <= 0 ? 0f : Math.min(1f, age / drySecs);
			float fade = age > life * 0.75f ? (life - age) / (life * 0.25f) : 1f;
			s.uCol.value4f(r + (dr - r) * dry, g + (dg - g) * dry, b + (db - b) * dry, 0.94f * fade);
			float wet = glints ? 1f : (wetSecs <= 0 ? 0f : Math.max(0f, 1f - age / wetSecs));
			s.uState.value4f(wet, (float)(clock % 600.0), landed, Math.max(0f, 1f - age / 2.5f));
			super.draw();
		}
	}

	public static class StainScript extends NoosaScript {
		public Uniform uMask, uNoise, uMaskScale, uPool, uThrow, uSeed, uCol, uState;

		public StainScript(){
			super();
			uMask      = uniform("uMask");
			uNoise     = uniform("uNoise");
			uMaskScale = uniform("uMaskScale");
			uPool      = uniform("uPool");
			uThrow     = uniform("uThrow");
			uSeed      = uniform("uSeed");
			uCol       = uniform("uCol");
			uState     = uniform("uState");
		}

		public static StainScript get(){
			return Script.use(StainScript.class);
		}

		@Override
		public void lighting(float a, float b, float c, float d, float e, float f, float g, float h) {}

		@Override
		protected String shader() {
			return VERTEX +
				"uniform sampler2D uMask;\n" +
				"uniform sampler2D uNoise;\n" +
				"uniform vec2 uMaskScale;\n" +
				"uniform vec4 uPool;\n" +    // centre x, y, how far it reaches, size of the splash in the middle
				"uniform vec4 uThrow;\n" +   // direction x, y, form, amount
				"uniform vec2 uSeed;\n" +
				"uniform vec4 uCol;\n" +
				"uniform vec4 uState;\n" +   // wetness, time, how much has landed yet, dust still in the air

				"float hash( vec2 p ){\n" +
				"  p = fract( p * vec2( 0.1031, 0.1030 ) );\n" +
				"  p += dot( p, p.yx + 33.33 );\n" +
				"  return fract( ( p.x + p.y ) * p.y );\n" +
				"}\n" +

				"void main() {\n" +
				"  vec2 p = floor( vWorld ) + 0.5;\n" +
				//not on water
				"  float land = 1.0 - smoothstep( 0.1, 0.9, texture2D( uMask, p * uMaskScale ).a );\n" +
				"  vec2 d = p - uPool.xy;\n" +
				//the floor is seen at a slant, so everything lies wider than it is deep
				"  d.y *= 1.35;\n" +
				"  float dist = length( d );\n" +
				"  float form = uThrow.z;\n" +
				"  float much = uThrow.w;\n" +

				//where things land: thrown one way they fall in a fan that opens out from the blow,
				// with nothing thrown they fall evenly all round. t runs from 0 where it starts to 1 at its furthest
				"  float thrown = step( 0.5, dot( uThrow.xy, uThrow.xy ) );\n" +
				"  float along = dot( d, uThrow.xy );\n" +
				"  float across = dot( d, vec2( -uThrow.y, uThrow.x ) );\n" +
				"  float t = mix( dist / uPool.z, ( along + 0.2 * uPool.z ) / ( 1.2 * uPool.z ), thrown );\n" +
				"  float spread = mix( 0.0, abs( across ) / ( 1.3 + 0.55 * max( along, 0.0 ) ), thrown );\n" +
				"  float fall = step( 0.0, t ) * pow( clamp( 1.0 - t, 0.0, 1.0 ), 1.1 ) * exp( -spread * spread * 0.9 );\n" +
				//only what has had time to get there
				"  fall *= step( t, uState.z + 0.15 );\n" +

				"  float n1 = texture2D( uNoise, p * 0.021 + uSeed ).r;\n" +
				"  float n2 = texture2D( uNoise, p * 0.060 + uSeed.yx ).g;\n" +
				"  float h1 = hash( p + uSeed * 97.0 );\n" +
				"  float h2 = hash( floor( p / 2.0 ) + uSeed * 53.0 );\n" +

				"  float a = 0.0;\n" +
				"  vec3 col = uCol.rgb;\n" +

				"  if (form < 0.5) {\n" +
				//liquid: single drops, some twice the size nearer the start, and under a heavy blow a ragged splash
				"    float small = step( 1.0 - fall * 0.85 * much, h1 );\n" +
				"    float big = step( 1.0 - fall * 0.55 * much, h2 ) * step( t, 0.70 );\n" +
				"    vec2 cd = vec2( ( along - 0.25 * uPool.w ) / 1.5, across );\n" +
				"    float edge = mix( dist, length( cd ), thrown ) / max( uPool.w, 0.01 ) + ( n1 - 0.5 ) * 1.2 + ( n2 - 0.5 ) * 0.6;\n" +
				"    float splash = step( edge, 0.85 ) * step( 0.5, uPool.w );\n" +
				"    a = max( splash, max( big, small * 0.92 ) );\n" +
				//it gathers darker along the splash's rim
				"    col *= 1.0 - 0.30 * splash * smoothstep( 0.40, 0.85, edge );\n" +
				//the smallest drops are thin and a little paler
				"    col *= 1.0 + 0.10 * ( 1.0 - max( splash, big ) );\n" +
				//while wet, the thicker parts shine here and there
				"    float sh = max( splash, big ) * step( 0.88, hash( p * 1.31 + uSeed.yx * 57.0 ) );\n" +
				"    sh *= uState.x * ( 0.55 + 0.45 * sin( uState.y * 2.6 + h1 * 6.2831 ) );\n" +
				"    col += vec3( 0.46, 0.38, 0.38 ) * sh;\n" +

				"  } else if (form < 1.5) {\n" +
				//fragments: specks, and splinters two pixels long lying this way or that
				"    float speck = step( 1.0 - fall * 0.50 * much, h1 );\n" +
				//not named "flat": that word is reserved in the phone's shader language and stops the shader compiling
				"    float lying = step( 1.0 - fall * 0.40 * much, hash( floor( p / vec2( 2.0, 1.0 ) ) + uSeed * 31.0 ) );\n" +
				"    float upright = step( 1.0 - fall * 0.30 * much, hash( floor( p / vec2( 1.0, 2.0 ) ) + uSeed * 71.0 ) );\n" +
				"    a = max( speck, max( lying, upright ) );\n" +
				//no two pieces quite the same shade
				"    col *= 0.78 + 0.34 * h2;\n" +
				//crystal and frost catch the light
				"    float tw = sin( uState.y * ( 1.5 + 2.5 * h1 ) + h1 * 40.0 );\n" +
				"    col += vec3( 0.45 ) * a * uState.x * step( 0.72, h1 ) * smoothstep( 0.6, 1.0, tw );\n" +
				//and for a moment there is dust hanging where it broke
				"    float dust = uState.w * smoothstep( 0.45, 0.75, n2 ) * clamp( 1.0 - dist / ( uPool.z * 0.7 ), 0.0, 1.0 ) * 0.30;\n" +
				"    col = mix( uCol.rgb, col, step( 0.5, a ) );\n" +
				"    a = max( a, dust );\n" +

				"  } else {\n" +
				//slime: a thick lobed gobbet, and a few smaller ones dropped close around it
				"    vec2 cd = vec2( ( along - 0.6 * uPool.w ) / 1.3, across );\n" +
				"    float edge = mix( dist, length( cd ), thrown ) / max( uPool.w, 0.01 ) + ( n1 - 0.5 ) * 1.5 + ( n2 - 0.5 ) * 0.4;\n" +
				"    float blob = step( edge, 0.9 );\n" +
				"    float bits = step( 1.0 - fall * 0.80 * much, h2 );\n" +
				"    a = max( blob, bits );\n" +
				"    col *= 1.0 - 0.28 * blob * smoothstep( 0.45, 0.9, edge );\n" +
				//it stays wet: a steady highlight on each gobbet's upper left
				"    float hi = a * step( 0.80, hash( floor( p / 2.0 ) * 1.7 + uSeed.yx * 23.0 ) ) * step( fract( p.x / 2.0 ), 0.5 ) * step( fract( p.y / 2.0 ), 0.5 );\n" +
				"    col += vec3( 0.34, 0.38, 0.34 ) * max( hi, blob * step( 0.93, h1 ) * step( edge, 0.5 ) );\n" +
				"  }\n" +

				"  a *= land * texture2D( uTex, vUV ).a;\n" +
				"  if (a <= 0.0) discard;\n" +
				"  gl_FragColor = vec4( col, a * uCol.a );\n" +
				"}\n";
		}
	}
}
