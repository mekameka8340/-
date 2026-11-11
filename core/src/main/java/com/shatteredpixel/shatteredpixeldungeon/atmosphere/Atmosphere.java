/*
 * Atmosphere mod - dynamic lighting, region color grading and vignette
 */

package com.shatteredpixel.shatteredpixeldungeon.atmosphere;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Inferno;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SacrificialFire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalWisp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Eye;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Pylon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.TormentedSpirit;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NoosaScript;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class Atmosphere extends Image {

	//a creature that is hiding unseen (an ebony mimic before it reveals itself) must not give itself away
	//through a reflection, shadow or ripple
	//the same goes for a ward of the wand of warding while it is still a magic circle on the floor (tiers 1-3):
	// it is drawn flat on the ground, so it has nothing to reflect and no height to cast a shadow
	public static boolean concealed( com.shatteredpixel.shatteredpixeldungeon.actors.Char c ){
		if (c instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding.Ward
				&& ((com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding.Ward) c).tier <= 3){
			return true;
		}
		return c instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EbonyMimic
				&& c.alignment == com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.NEUTRAL;
	}

	//the level's monsters, read from the drawing side. The game's own thread may be adding or removing one
	//at the same moment, which can break the copy; then this frame simply draws without them
	public static Mob[] mobs( com.shatteredpixel.shatteredpixeldungeon.levels.Level level ){
		for (int tries = 0; tries < 3; tries++){
			try {
				return level.mobs.toArray(new Mob[0]);
			} catch (RuntimeException e){
				//changed while being copied, try again
			}
		}
		return new Mob[0];
	}


	// ---------------------------------------------------------------- settings

	//0 = off, 1 = low, 2 = medium, 3 = high
	private static int quality = -1;

	public static int quality(){
		if (quality == -1) quality = SPDSettings.atmosphere();
		return quality;
	}

	public static void quality( int value ){
		quality = value;
	}

	private static int texelsPerTile( int q ){
		return q <= 1 ? 2 : 4;
	}

	private static float updateInterval( int q ){
		switch (q){
			case 1:  return 1/15f;
			case 2:  return 0f;
			default: return 0f;
		}
	}

	private static Atmosphere instance;

	// ---------------------------------------------------------------- region mood

	private float tintR = 1f, tintG = 1f, tintB = 1f;
	private float ambient = 0.75f;
	private float vignette = 0.4f;

	private void setupMood( Level level ){
		String tiles = level.tilesTex();
		if (tiles == null) tiles = "";

		switch (tiles){
			case Assets.Environment.TILES_SEWERS:
				mood(0.92f, 1.00f, 1.00f, 0.55f, 0.55f); break;
			case Assets.Environment.TILES_PRISON:
				mood(0.90f, 0.94f, 1.04f, 0.45f, 0.60f); break;
			case Assets.Environment.TILES_CAVES:
				mood(1.05f, 0.97f, 0.86f, 0.45f, 0.60f); break;
			case Assets.Environment.TILES_CAVES_CRYSTAL:
				mood(0.92f, 0.98f, 1.08f, 0.42f, 0.60f); break;
			case Assets.Environment.TILES_CAVES_GNOLL:
				mood(1.05f, 0.95f, 0.84f, 0.45f, 0.60f); break;
			case Assets.Environment.TILES_CITY:
				mood(1.06f, 0.99f, 0.86f, 0.48f, 0.60f); break;
			case Assets.Environment.TILES_HALLS:
				mood(1.08f, 0.88f, 0.86f, 0.44f, 0.65f); break;
			default:
				mood(1.00f, 1.00f, 1.00f, 0.60f, 0.50f); break;
		}

		if (level.feeling == Level.Feeling.DARK){
			//truly dark: only what is lit can be seen
			ambient *= 0.5f;
		}
		if (Dungeon.isChallenged(Challenges.DARKNESS)){
			ambient *= 0.7f;
		}
	}

	private void mood( float r, float g, float b, float amb, float vig ){
		tintR = r; tintG = g; tintB = b;
		ambient = amb;
		vignette = vig;
	}

	public static float currentVignette(){
		return instance != null ? instance.vignette : 0.5f;
	}

	private float currentAmbient(){
		return Math.max(0.2f, ambient + 0.12f * SPDSettings.brightness());
	}

	// ---------------------------------------------------------------- light sources

	private static class StaticLight {
		int cell;
		float x, y;            //in tiles
		float radius, intensity;
		float r, g, b;
		float flickerAmp, flickerSpeed, phase;
		boolean hot;           //produces heat haze

		//occlusion box, centered on the origin cell. null means no occlusion
		boolean[] vis;
		int ox, oy, boxR;
	}

	private final ArrayList<StaticLight> staticLights = new ArrayList<>();
	private int terrainHash = 0;
	private boolean[] fovTmp;

	//distance from each water cell to the shore, deeper water is darker
	private byte[] waterDepth;

	private void computeWaterDepth( Level level ){
		int len = level.length(), w = level.width();
		waterDepth = new byte[len];
		for (int i = 0; i < len; i++) waterDepth[i] = (byte)(level.water[i] ? 3 : 0);
		for (int pass = 1; pass <= 2; pass++){
			byte[] next = waterDepth.clone();
			for (int i = 0; i < len; i++){
				if (!level.water[i]) continue;
				int x = i % w, y = i / w;
				boolean shore = false;
				for (int dy = -1; dy <= 1 && !shore; dy++){
					for (int dx = -1; dx <= 1; dx++){
						int x2 = x + dx, y2 = y + dy;
						if (x2 < 0 || y2 < 0 || x2 >= w || y2 >= level.height()) continue;
						if (waterDepth[y2*w + x2] < pass){ shore = true; break; }
					}
				}
				if (shore && waterDepth[i] > pass) next[i] = (byte)pass;
			}
			waterDepth = next;
		}
	}

	private void rebuildStaticLights( Level level ){
		staticLights.clear();
		computeWaterDepth(level);

		String tiles = level.tilesTex();
		if (tiles == null) tiles = "";
		boolean prison = tiles.equals(Assets.Environment.TILES_PRISON);
		boolean caves  = tiles.startsWith("environment/tiles_caves");
		boolean city   = tiles.equals(Assets.Environment.TILES_CITY);
		boolean halls  = tiles.equals(Assets.Environment.TILES_HALLS);

		int w = level.width();
		for (int i = 0; i < level.length(); i++){
			int t = level.map[i];
			int cx = i % w, cy = i / w;

			if (prison && t == Terrain.WALL_DECO){
				//wall torch, shines out of the wall
				addStatic(level, i, cx + 0.5f, cy + 1.0f, belowIfOpen(level, i),
						4.5f, 0.80f, 1.00f, 0.68f, 0.34f, 0.12f, 9f).hot = true;

			} else if (caves && t == Terrain.WALL_DECO){
				//glowing crystal vein
				addStatic(level, i, cx + 0.5f, cy + 0.9f, belowIfOpen(level, i),
						2.6f, 0.35f, 0.45f, 0.75f, 1.00f, 0.25f, 1.5f);

			} else if (city && (t == Terrain.REGION_DECO || t == Terrain.REGION_DECO_ALT)){
				//green brazier flame
				addStatic(level, i, cx + 0.5f, cy + 0.3f, i,
						3.4f, 0.60f, 0.40f, 1.00f, 0.45f, 0.15f, 9f).hot = true;

			} else if (halls && t == Terrain.WATER){
				//streams of fire
				addStatic(level, i, cx + 0.5f, cy + 0.5f, -1,
						1.9f, 0.20f, 1.00f, 0.38f, 0.15f, 0.12f, 3f).hot = true;

			} else if (t == Terrain.EMBERS){
				addStatic(level, i, cx + 0.5f, cy + 0.5f, -1,
						1.4f, 0.20f, 1.00f, 0.45f, 0.15f, 0.15f, 6f);

			} else if (t == Terrain.WELL){
				addStatic(level, i, cx + 0.5f, cy + 0.5f, i,
						2.2f, 0.30f, 0.35f, 0.65f, 1.00f, 0.10f, 1.2f);

			} else if (t == Terrain.ALCHEMY){
				addStatic(level, i, cx + 0.5f, cy + 0.5f, i,
						2.2f, 0.35f, 0.75f, 0.45f, 1.00f, 0.12f, 4f);

			} else if (t == Terrain.MINE_CRYSTAL){
				addStatic(level, i, cx + 0.5f, cy + 0.5f, i,
						2.4f, 0.40f, 0.50f, 0.85f, 1.00f, 0.25f, 1.3f);

			} else if (t == Terrain.PEDESTAL){
				addStatic(level, i, cx + 0.5f, cy + 0.4f, i,
						2.4f, 0.35f, 1.00f, 0.88f, 0.55f, 0.08f, 1.5f);

			} else if (t == Terrain.ENTRANCE || t == Terrain.ENTRANCE_SP){
				//light from the floor above
				addStatic(level, i, cx + 0.5f, cy + 0.5f, i,
						3.0f, 0.35f, 1.00f, 0.95f, 0.80f, 0f, 0f);
			}
		}
	}

	private static int belowIfOpen( Level level, int cell ){
		int below = cell + level.width();
		if (below < level.length() && !level.solid[below]){
			return below;
		}
		return cell;
	}

	private StaticLight addStatic( Level level, int cell, float x, float y, int fovOrigin,
							float radius, float intensity, float r, float g, float b,
							float flickerAmp, float flickerSpeed ){
		StaticLight l = new StaticLight();
		l.cell = cell;
		l.x = x; l.y = y;
		l.radius = radius; l.intensity = intensity;
		l.r = r; l.g = g; l.b = b;
		l.flickerAmp = flickerAmp; l.flickerSpeed = flickerSpeed;
		l.phase = (cell * 7919 % 1000) / 100f;

		if (fovOrigin >= 0){
			int w = level.width();
			int ox = fovOrigin % w, oy = fovOrigin / w;
			int boxR = (int)Math.ceil(radius) + 1;

			if (fovTmp == null || fovTmp.length != level.length()){
				fovTmp = new boolean[level.length()];
			}
			ShadowCaster.castShadow(ox, oy, w, fovTmp, level.losBlocking, boxR);

			int size = boxR*2 + 1;
			l.vis = new boolean[size*size];
			for (int dy = -boxR; dy <= boxR; dy++){
				for (int dx = -boxR; dx <= boxR; dx++){
					int x2 = ox + dx, y2 = oy + dy;
					if (x2 < 0 || y2 < 0 || x2 >= w || y2 >= level.height()) continue;
					l.vis[(dy + boxR)*size + (dx + boxR)] = fovTmp[y2*w + x2];
				}
			}
			l.ox = ox; l.oy = oy; l.boxR = boxR;
		}

		staticLights.add(l);
		return l;
	}

	private static int computeTerrainHash( Level level ){
		int h = 17;
		int[] map = level.map;
		boolean[] los = level.losBlocking;
		for (int i = 0; i < map.length; i++){
			h = h*31 + map[i];
			if (los[i]) h += 0x5bd1e995;
		}
		return h;
	}

	// ---------------------------------------------------------------- transient lights (flashes, bursts, missiles)

	private static class Burst {
		float x, y, radius, intensity, r, g, b, life, maxLife;
	}

	private static final ArrayList<Burst> bursts = new ArrayList<>();

	//x, y in world pixels, radius in tiles, color as 0xRRGGBB
	public static void burst( float x, float y, float radius, float intensity, int color, float duration ){
		if (quality() <= 0) return;
		Burst b = new Burst();
		b.x = x / DungeonTilemap.SIZE;
		b.y = y / DungeonTilemap.SIZE;
		b.radius = radius;
		b.intensity = intensity;
		b.r = ((color >> 16) & 0xFF) / 255f;
		b.g = ((color >> 8) & 0xFF) / 255f;
		b.b = (color & 0xFF) / 255f;
		b.life = b.maxLife = duration;
		synchronized (bursts) {
			bursts.add(b);
		}
	}

	public static void burstCell( int cell, float radius, float intensity, int color, float duration ){
		if (Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length()) return;
		int w = Dungeon.level.width();
		burst((cell % w + 0.5f) * DungeonTilemap.SIZE, (cell / w + 0.5f) * DungeonTilemap.SIZE,
				radius, intensity, color, duration);
	}

	private static class TrackedMissile {
		float lastTrail = 0;
		MagicMissile missile;
		float r, g, b, radius, intensity;
	}

	private static final ArrayList<TrackedMissile> missiles = new ArrayList<>();

	public static void trackMissile( MagicMissile m, int type ){
		if (quality() <= 0) return;
		TrackedMissile t = new TrackedMissile();
		t.missile = m;
		t.radius = 2.6f;
		t.intensity = 0.8f;
		int color;
		switch (type){
			case MagicMissile.FIRE: case MagicMissile.FIRE_CONE:
			case MagicMissile.SHAMAN_RED: case MagicMissile.ELMO:
				color = 0xFF8833; t.radius = 3f; t.intensity = 1f; break;
			case MagicMissile.FROST: case MagicMissile.FROST_CONE:
				color = 0x88DDFF; break;
			case MagicMissile.CORROSION: case MagicMissile.CORROSION_CONE:
				color = 0xFFAA44; break;
			case MagicMissile.FOLIAGE: case MagicMissile.FOLIAGE_CONE:
			case MagicMissile.POISON: case MagicMissile.POISON_CONE:
				color = 0x77FF66; break;
			case MagicMissile.SHADOW: case MagicMissile.SHADOW_CONE:
			case MagicMissile.SHAMAN_PURPLE: case MagicMissile.PURPLE_CONE:
				color = 0xAA66FF; t.intensity = 0.55f; break;
			case MagicMissile.SHAMAN_BLUE:
				color = 0x6688FF; break;
			case MagicMissile.EARTH: case MagicMissile.EARTH_CONE:
				color = 0xCC9966; t.intensity = 0.4f; break;
			case MagicMissile.SPARK_CONE:
				color = 0xFFEE77; break;
			case MagicMissile.BLOOD_CONE:
				color = 0xFF3344; break;
			case MagicMissile.RAINBOW: case MagicMissile.RAINBOW_CONE:
				color = -1; break;
			default:
				if (type >= MagicMissile.SPECK) return;
				color = 0xCCDDFF; break;
		}
		if (color == -1){
			t.r = -1;
		} else {
			t.r = ((color >> 16) & 0xFF) / 255f;
			t.g = ((color >> 8) & 0xFF) / 255f;
			t.b = (color & 0xFF) / 255f;
		}
		synchronized (missiles) {
			missiles.add(t);
		}
		Ambience.trackBolt(m, t.r, t.g, t.b);
	}

	// ---------------------------------------------------------------- heat sources, read by the post processor

	public static final int MAX_HEAT = 4;
	//world pixel x, y, radius (tiles), strength
	static final float[] heat = new float[MAX_HEAT * 4];
	static int heatCount = 0;

	private void offerHeat( float x, float y, float radius, float strength ){
		Hero hero = Dungeon.hero;
		if (hero == null || hero.sprite == null) return;
		float hx = (hero.sprite.x + hero.sprite.width()/2f) / DungeonTilemap.SIZE;
		float hy = (hero.sprite.y + hero.sprite.height()/2f) / DungeonTilemap.SIZE;
		float score = strength / (1f + (x-hx)*(x-hx) + (y-hy)*(y-hy));

		//replace the weakest entry if this one is better
		int slot = -1;
		if (heatCount < MAX_HEAT){
			slot = heatCount++;
		} else {
			float worst = Float.MAX_VALUE;
			for (int i = 0; i < MAX_HEAT; i++){
				float s = heatScore[i];
				if (s < worst){
					worst = s;
					slot = i;
				}
			}
			if (worst >= score) return;
		}
		heatScore[slot] = score;
		heat[slot*4]   = x * DungeonTilemap.SIZE;
		heat[slot*4+1] = y * DungeonTilemap.SIZE;
		heat[slot*4+2] = radius;
		heat[slot*4+3] = strength;
	}
	private final float[] heatScore = new float[MAX_HEAT];

	// ---------------------------------------------------------------- light map texture

	private int mapW, mapH;
	private int texelsPerTile;
	private int texW, texH;      //area of the texture that covers the level
	private int width2, height2; //texture size, power of two
	private String texKey;
	private static int keyCounter = 0;
	private SmartTexture lightTex;
	private float[] accR, accG, accB;
	//what is currently shown, eased towards the new lighting so changes never pop
	private float[] outR, outG, outB;
	private boolean outValid = false;
	private float sinceWrite = 0f;

	private float time = 0f;
	private float timer = 0f;

	public Atmosphere( int mapWidth, int mapHeight ){
		super();
		mapW = mapWidth;
		mapH = mapHeight;
		instance = this;

		synchronized (bursts) { bursts.clear(); }
		synchronized (missiles) { missiles.clear(); }
		heatCount = 0;

		if (Dungeon.level != null) {
			setupMood(Dungeon.level);
		}

		visible = quality() > 0;
		if (visible){
			setupTexture(texelsPerTile(quality()));
		}
	}

	private void setupTexture( int tpt ){
		releaseTexture();

		texelsPerTile = tpt;
		texW = mapW * tpt;
		texH = mapH * tpt;

		width2 = 1;
		while (width2 < texW) width2 <<= 1;
		height2 = 1;
		while (height2 < texH) height2 <<= 1;

		texKey = "Atmosphere_" + (keyCounter++) + "_" + width2 + "x" + height2;
		lightTex = TextureCache.create(texKey, width2, height2);
		texture(lightTex);

		accR = new float[texW*texH];
		accG = new float[texW*texH];
		accB = new float[texW*texH];
		outR = new float[texW*texH];
		outG = new float[texW*texH];
		outB = new float[texW*texH];
		outValid = false;

		//fill everything with plain ambient light
		byte amb = toByte(currentAmbient() * 0.5f);
		ByteBuffer px = lightTex.bitmap.getPixels();
		for (int i = 0; i < width2*height2; i++){
			px.put(i*4,   amb);
			px.put(i*4+1, amb);
			px.put(i*4+2, amb);
			px.put(i*4+3, (byte)0xFF);
		}
		lightTex.bitmap(lightTex.bitmap);

		float size = DungeonTilemap.SIZE / (float)tpt;
		scale.set(size, size);

		timer = 0f;
	}

	private void releaseTexture(){
		if (texKey != null){
			TextureCache.remove(texKey);
			texKey = null;
			lightTex = null;
			texture = null;
		}
	}

	private static byte toByte( float v ){
		int i = (int)(v * 255f + 0.5f);
		if (i < 0) i = 0;
		if (i > 255) i = 255;
		return (byte)i;
	}

	@Override
	public void update() {
		super.update();

		time += Game.elapsed;
		sinceWrite += Game.elapsed;

		synchronized (bursts) {
			Iterator<Burst> it = bursts.iterator();
			while (it.hasNext()) {
				Burst b = it.next();
				b.life -= Game.elapsed;
				if (b.life <= 0) it.remove();
			}
		}

		int q = quality();
		if (q <= 0 || Dungeon.level == null || Dungeon.hero == null){
			visible = false;
			heatCount = 0;
			return;
		}
		visible = true;

		if (lightTex == null || texelsPerTile != texelsPerTile(q)){
			setupTexture(texelsPerTile(q));
		}

		timer -= Game.elapsed;
		if (timer <= 0f){
			timer = updateInterval(q);
			recompute(Dungeon.level);
		}
	}

	// ---------------------------------------------------------------- light map calculation

	private int viewL, viewT, viewR, viewB; //visible area in tiles, with margin

	private void recompute( Level level ){

		int hash = computeTerrainHash(level);
		if (hash != terrainHash){
			terrainHash = hash;
			rebuildStaticLights(level);
		}

		//visible part of the level, so that offscreen lights can be skipped
		Camera c = Camera.main;
		float ts = DungeonTilemap.SIZE;
		viewL = (int)Math.floor(c.scroll.x / ts) - 5;
		viewT = (int)Math.floor(c.scroll.y / ts) - 5;
		viewR = (int)Math.ceil((c.scroll.x + c.width) / ts) + 5;
		viewB = (int)Math.ceil((c.scroll.y + c.height) / ts) + 5;

		Arrays.fill(accR, 0f);
		Arrays.fill(accG, 0f);
		Arrays.fill(accB, 0f);
		heatCount = 0;

		addHeroLight(level);
		addStaticLights(level);
		addBlobLights(level);
		addMobLights(level);
		addTransientLights(level);
		addValuableLights(level);

		writeTexture(level);
	}

	private boolean onScreen( float x, float y, float radius ){
		return x + radius >= viewL && x - radius <= viewR
				&& y + radius >= viewT && y - radius <= viewB;
	}

	//gentle, slow variation only: fast flicker reads as the screen blinking
	private float flicker( float amp, float speed, float phase ){
		if (amp <= 0f) return 1f;
		amp *= 0.3f;
		speed = Math.min(speed, 3.5f);
		float t = time * speed + phase;
		float n = 0.6f * (float)Math.sin(t)
				+ 0.4f * (float)Math.sin(t * 0.61f + phase * 2.1f);
		return 1f + amp * n;
	}


	private void addHeroLight( Level level ){
		Hero hero = Dungeon.hero;
		if (hero.sprite == null || !hero.isAlive()) return;

		float radius = 5.5f;
		float intensity = 0.75f;
		if (hero.buff(Light.class) != null){
			radius = 7.5f;
			intensity = 0.85f;
		}
		if (Dungeon.isChallenged(Challenges.DARKNESS)){
			radius = Math.min(radius, 3f);
		}
		if (level.feeling == Level.Feeling.DARK){
			radius *= 0.8f;
		}

		float f = 1f;
		CharSprite s = hero.sprite;
		float x = (s.x + s.width()/2f) / DungeonTilemap.SIZE;
		float y = (s.y + s.height()/2f) / DungeonTilemap.SIZE;

		addLight(x, y, radius, intensity * f, 1.00f, 0.88f, 0.70f, level.heroFOV, null, 0, 0, 0);
	}

	private void addStaticLights( Level level ){
		boolean[] visited = level.visited;
		for (StaticLight l : staticLights){
			if (!visited[l.cell] && !level.heroFOV[l.cell]) continue;
			if (!onScreen(l.x, l.y, l.radius)) continue;

			float f = flicker(l.flickerAmp, l.flickerSpeed, l.phase);
			addLight(l.x, l.y, l.radius * (0.97f + 0.03f*f), l.intensity * f,
					l.r, l.g, l.b, null, l.vis, l.ox, l.oy, l.boxR);

			if (l.hot && level.heroFOV[l.cell]){
				offerHeat(l.x, l.y - 0.4f, 1.1f, 0.5f);
			}
		}
	}

	private void addBlobLights( Level level ){
		if (level.blobs == null) return;
		for (Blob blob : level.blobs.values()){
			if (blob == null || blob.volume <= 0 || blob.cur == null) continue;

			float r, g, b, radius, intensity, amp, speed;
			boolean hot = false;
			if (blob instanceof Fire || blob instanceof Inferno){
				r = 1.00f; g = 0.55f; b = 0.20f; radius = 2.3f; intensity = 0.45f; amp = 0.22f; speed = 11f;
				hot = true;
			} else if (blob instanceof Electricity){
				r = 0.60f; g = 0.75f; b = 1.00f; radius = 2.0f; intensity = 0.40f; amp = 0.35f; speed = 23f;
			} else if (blob instanceof SacrificialFire){
				r = 0.55f; g = 0.75f; b = 1.00f; radius = 2.6f; intensity = 0.40f; amp = 0.15f; speed = 7f;
			} else {
				continue;
			}

			int w = level.width();
			int top = Math.max(0, blob.area.top), bottom = Math.min(level.height(), blob.area.bottom);
			int left = Math.max(0, blob.area.left), right = Math.min(w, blob.area.right);
			for (int y = top; y < bottom; y++){
				for (int x = left; x < right; x++){
					int cell = y*w + x;
					if (blob.cur[cell] <= 0 || !level.heroFOV[cell]) continue;
					if (!onScreen(x, y, radius)) continue;
					float f = flicker(amp, speed, cell * 0.37f);
					addLight(x + 0.5f, y + 0.5f, radius, intensity * f, r, g, b, null, null, 0, 0, 0);
					if (hot) offerHeat(x + 0.5f, y + 0.3f, 1.6f, 1f);
				}
			}
		}
	}

	private void addMobLights( Level level ){
		for (Mob mob : mobs(level)){
			if (mob.sprite == null || !mob.sprite.visible || !mob.isAlive()) continue;
			if (mob.pos < 0 || mob.pos >= level.length() || !level.heroFOV[mob.pos]) continue;

			float r, g, b, radius, intensity, amp = 0.08f, speed = 6f;
			boolean hot = false;
			if (mob instanceof Elemental.FireElemental || mob instanceof YogFist.BurningFist){
				r = 1.00f; g = 0.55f; b = 0.20f; radius = 2.8f; intensity = 0.65f; amp = 0.2f; speed = 11f;
				hot = true;
			} else if (mob instanceof Elemental.FrostElemental){
				r = 0.50f; g = 0.80f; b = 1.00f; radius = 2.5f; intensity = 0.45f;
			} else if (mob instanceof Elemental.ShockElemental){
				r = 0.60f; g = 0.75f; b = 1.00f; radius = 2.8f; intensity = 0.55f; amp = 0.35f; speed = 23f;
			} else if (mob instanceof Elemental.ChaosElemental){
				r = 0.90f; g = 0.50f; b = 1.00f; radius = 2.8f; intensity = 0.55f; amp = 0.2f;
			} else if (mob instanceof YogFist.BrightFist){
				r = 1.00f; g = 1.00f; b = 0.90f; radius = 3.5f; intensity = 0.70f;
			} else if (mob instanceof YogDzewa){
				r = 1.00f; g = 0.30f; b = 0.30f; radius = 4.0f; intensity = 0.50f; amp = 0.1f; speed = 2f;
			} else if (mob instanceof Eye){
				r = 1.00f; g = 0.35f; b = 0.50f; radius = 2.2f; intensity = 0.40f;
			} else if (mob instanceof CrystalWisp){
				r = 0.50f; g = 0.90f; b = 1.00f; radius = 2.5f; intensity = 0.50f; amp = 0.15f; speed = 4f;
			} else if (mob instanceof Wraith || mob instanceof TormentedSpirit){
				r = 0.75f; g = 0.80f; b = 1.00f; radius = 2.0f; intensity = 0.35f; amp = 0.15f; speed = 3f;
			} else if (mob instanceof Pylon && mob.alignment != Char.Alignment.NEUTRAL){
				r = 0.60f; g = 0.80f; b = 1.00f; radius = 3.0f; intensity = 0.60f; amp = 0.3f; speed = 17f;
			} else if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper){
				//the shopkeeper's lantern
				r = 1.00f; g = 0.80f; b = 0.50f; radius = 3.4f; intensity = 0.60f; amp = 0.06f; speed = 4f;
			} else if (Char.hasProp(mob, Char.Property.BOSS)){
				//bosses are put in the spotlight
				r = 1.00f; g = 0.92f; b = 0.85f; radius = 3.6f; intensity = 0.45f; amp = 0.05f; speed = 2f;
			} else {
				continue;
			}

			CharSprite s = mob.sprite;
			float x = (s.x + s.width()/2f) / DungeonTilemap.SIZE;
			float y = (s.y + s.height()/2f) / DungeonTilemap.SIZE;
			if (!onScreen(x, y, radius)) continue;

			float f = flicker(amp, speed, mob.id() * 0.61f);
			addLight(x, y, radius, intensity * f, r, g, b, null, null, 0, 0, 0);
			if (hot) offerHeat(x, y - 0.3f, 1.4f, 0.9f);
		}
	}

	private void addTransientLights( Level level ){
		synchronized (bursts) {
			for (Burst b : bursts) {
				float k = b.life / b.maxLife;
				k = k * k * (3f - 2f * k); //smooth fade out
				if (!onScreen(b.x, b.y, b.radius)) continue;
				addLight(b.x, b.y, b.radius * (0.6f + 0.4f * k), b.intensity * k, b.r, b.g, b.b, null, null, 0, 0, 0);
			}
		}

		synchronized (missiles) {
			Iterator<TrackedMissile> it = missiles.iterator();
			while (it.hasNext()) {
				TrackedMissile t = it.next();
				MagicMissile m = t.missile;
				if (m == null || !m.on || !m.alive || m.parent == null) {
					it.remove();
					continue;
				}
				float x = (m.x + m.width / 2f) / DungeonTilemap.SIZE;
				float y = (m.y + m.height / 2f) / DungeonTilemap.SIZE;
				if (!onScreen(x, y, t.radius)) continue;

				float r = t.r, g = t.g, b = t.b;
				if (r < 0) {
					//rainbow, cycles hue
					float h = (time * 1.5f) % 1f;
					r = clamp01(Math.abs(h * 6f - 3f) - 1f);
					g = clamp01(2f - Math.abs(h * 6f - 2f));
					b = clamp01(2f - Math.abs(h * 6f - 4f));
				}
				addLight(x, y, t.radius, t.intensity * flicker(0.15f, 13f, x), r, g, b, null, null, 0, 0, 0);

				//glowing trail left behind
				if (time - t.lastTrail > 0.04f){
					t.lastTrail = time;
					Burst trail = new Burst();
					trail.x = x; trail.y = y;
					trail.radius = 1.6f; trail.intensity = 0.4f;
					trail.r = r; trail.g = g; trail.b = b;
					trail.life = trail.maxLife = 0.3f;
					synchronized (bursts) { bursts.add(trail); }
				}
			}
		}
	}

	//valuable items give off a soft, slowly wavering light
	private void addValuableLights( Level level ){
		for (com.shatteredpixel.shatteredpixeldungeon.items.Heap heap : level.heaps.valueList()){
			int col = Ambience.valuableColor(heap, level);
			if (col == 0) continue;
			int w = level.width();
			float x = heap.pos % w + 0.5f, y = heap.pos / w + 0.5f;
			if (!onScreen(x, y, 2f)) continue;
			float ph = heap.pos * 0.73f;
			float wave = (float)Math.sin(time * 2.1f + ph) * 0.6f + (float)Math.sin(time * 3.7f + ph * 1.7f) * 0.4f;
			addLight(x, y, 2.0f + 0.25f * wave, 0.32f + 0.08f * wave,
					((col >> 16) & 0xFF) / 255f, ((col >> 8) & 0xFF) / 255f, (col & 0xFF) / 255f,
					null, null, 0, 0, 0);
		}
	}

	private static float clamp01( float v ){
		return v < 0 ? 0 : (v > 1 ? 1 : v);
	}

	//visibility of a cell for occlusion, false outside of the occlusion area
	private boolean cellVis( int cx, int cy, boolean[] fullVis, boolean[] boxVis, int ox, int oy, int boxR, int boxSize ){
		if (cx < 0 || cy < 0 || cx >= mapW || cy >= mapH) return false;
		if (fullVis != null){
			return fullVis[cy*mapW + cx];
		}
		int bx = cx - ox + boxR, by = cy - oy + boxR;
		if (bx < 0 || by < 0 || bx >= boxSize || by >= boxSize) return false;
		return boxVis[by*boxSize + bx];
	}

	//adds a light, centered at x,y (in tiles)
	//occlusion is either by a full-level visibility map, or by a box around an origin cell
	//shadow edges are softened by interpolating visibility between cell centers
	private void addLight( float x, float y, float radius, float intensity,
						   float r, float g, float b,
						   boolean[] fullVis, boolean[] boxVis, int ox, int oy, int boxR ){

		if (intensity <= 0f) return;

		int tpt = texelsPerTile;
		int x0 = Math.max(0, (int)Math.floor((x - radius) * tpt));
		int x1 = Math.min(texW - 1, (int)Math.ceil((x + radius) * tpt));
		int y0 = Math.max(0, (int)Math.floor((y - radius) * tpt));
		int y1 = Math.min(texH - 1, (int)Math.ceil((y + radius) * tpt));

		float r2 = radius * radius;
		float inv = 1f / tpt;
		int boxSize = boxR*2 + 1;
		boolean occluded = fullVis != null || boxVis != null;

		for (int ty = y0; ty <= y1; ty++){
			float tyc = (ty + 0.5f) * inv;
			float dy = tyc - y;
			float dy2 = dy*dy;
			if (dy2 >= r2) continue;
			int row = ty * texW;

			//vertical interpolation between cell centers
			float cyf = tyc - 0.5f;
			int cy0 = (int)Math.floor(cyf);
			float fy = cyf - cy0;

			for (int tx = x0; tx <= x1; tx++){
				float txc = (tx + 0.5f) * inv;
				float dx = txc - x;
				float d2 = dx*dx + dy2;
				if (d2 >= r2) continue;

				float v = 1f;
				if (occluded){
					//the texel's own cell must be visible for any light at all
					int ownX = tx / tpt, ownY = ty / tpt;
					if (!cellVis(ownX, ownY, fullVis, boxVis, ox, oy, boxR, boxSize)) continue;

					float cxf = txc - 0.5f;
					int cx0 = (int)Math.floor(cxf);
					float fx = cxf - cx0;

					float v00 = cellVis(cx0,   cy0,   fullVis, boxVis, ox, oy, boxR, boxSize) ? 1f : 0f;
					float v10 = cellVis(cx0+1, cy0,   fullVis, boxVis, ox, oy, boxR, boxSize) ? 1f : 0f;
					float v01 = cellVis(cx0,   cy0+1, fullVis, boxVis, ox, oy, boxR, boxSize) ? 1f : 0f;
					float v11 = cellVis(cx0+1, cy0+1, fullVis, boxVis, ox, oy, boxR, boxSize) ? 1f : 0f;
					v = (v00*(1-fx) + v10*fx)*(1-fy) + (v01*(1-fx) + v11*fx)*fy;
					//keep a bit of light so shadow edges stay soft rather than vanishing
					v = 0.5f + 0.5f*v;
				}

				float f = 1f - d2 / r2;
				f = f * f * intensity * v;

				//overlapping lights don't add up: each spot takes the strongest light reaching it,
				// so a torch next to the hero is no brighter than the torch alone
				int i = row + tx;
				float cr = f * r, cg = f * g, cb = f * b;
				if (cr > accR[i]) accR[i] = cr;
				if (cg > accG[i]) accG[i] = cg;
				if (cb > accB[i]) accB[i] = cb;
			}
		}
	}

	private void writeTexture( Level level ){
		float amb = currentAmbient();
		ByteBuffer px = lightTex.bitmap.getPixels();
		boolean[] water = level.water;
		int tpt = texelsPerTile;
		float t = time;

		float rememberLift = Math.max(0f, 0.15f * SPDSettings.atmoBright());
		boolean[] fov = level.heroFOV, visited = level.visited, mapped = level.mapped;

		//ease towards the new values over roughly a fifth of a second
		float ease = outValid ? 1f - (float)Math.exp(-sinceWrite * 9f) : 1f;
		sinceWrite = 0f;
		outValid = true;

		for (int ty = 0; ty < texH; ty++){
			int src = ty * texW;
			int dst = ty * width2 * 4;
			int cellRow = (ty / tpt) * mapW;
			for (int tx = 0; tx < texW; tx++){
				int i = src + tx;
				float lr = accR[i], lg = accG[i], lb = accB[i];

				//lit water shimmers
				if (water != null && water[cellRow + tx / tpt] && (lr + lg + lb) > 0.05f){
					float s = (float)(Math.sin(tx * 1.7f + ty * 0.9f + t * 0.9f) * Math.sin(tx * 0.6f - ty * 1.3f + t * 0.7f));
					if (s > 0){
						float k = 1f + 0.22f * s * s;
						lr *= k; lg *= k; lb *= k;
					}
				}

				float depthK = 0.5f;
				if (waterDepth != null){
					int d = waterDepth[cellRow + tx / tpt];
					if (d >= 2) depthK = d >= 3 ? 0.40f : 0.45f;
				}
				float vr = (amb + lr) * depthK, vg = (amb + lg) * depthK, vb = (amb + lb) * depthK;
				//areas seen before but out of view are lifted towards normal brightness
				int cellIdx = cellRow + tx / tpt;
				if (rememberLift > 0 && fov != null && visited != null && mapped != null && !fov[cellIdx] && (visited[cellIdx] || mapped[cellIdx])){
					vr += (0.5f - vr) * rememberLift;
					vg += (0.5f - vg) * rememberLift;
					vb += (0.5f - vb) * rememberLift;
				}
				vr = outR[i] += (vr - outR[i]) * ease;
				vg = outG[i] += (vg - outG[i]) * ease;
				vb = outB[i] += (vb - outB[i]) * ease;
				px.put(dst,     toByte(vr));
				px.put(dst + 1, toByte(vg));
				px.put(dst + 2, toByte(vb));
				dst += 4;
			}
		}

		//update the existing texture in place, rather than re-creating it every frame
		lightTex.bind();
		((java.nio.Buffer)px).position(0);
		Gdx.gl.glTexSubImage2D(Gdx.gl.GL_TEXTURE_2D, 0, 0, 0, width2, texH,
				Gdx.gl.GL_RGBA, Gdx.gl.GL_UNSIGNED_BYTE, px);
	}

	// ---------------------------------------------------------------- drawing

	@Override
	protected NoosaScript script() {
		return AtmosphereScript.get();
	}

	@Override
	public void draw() {
		if (lightTex == null) return;

		AtmosphereScript s = AtmosphereScript.get();
		s.resetCamera();
		s.uTint.value4f(tintR, tintG, tintB, 1f);
		s.uScreen.value2f(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
		//when post processing is active, it handles the vignette instead
		s.uVignette.value1f(PostFX.enabled() ? 0f : currentVignette());

		Gdx.gl.glBlendFunc(Gdx.gl.GL_DST_COLOR, Gdx.gl.GL_SRC_COLOR);
		super.draw();
		Blending.setNormalMode();
	}

	@Override
	public void destroy() {
		super.destroy();
		releaseTexture();
		if (instance == this) instance = null;
	}
}
