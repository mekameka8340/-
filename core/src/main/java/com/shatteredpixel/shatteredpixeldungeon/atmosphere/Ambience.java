/*
 * Atmosphere mod - environmental details
 *
 * Shadows, water reflections, decals (blood, scorch marks, frost), ambient particles,
 * footstep effects, ghost afterimages, light shafts, sparkles, sounds and the floor banner.
 */

package com.shatteredpixel.shatteredpixeldungeon.atmosphere;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.TormentedSpirit;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.PseudoPixel;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Ambience {

	public static boolean enabled(){
		return Atmosphere.quality() >= 1;
	}

	private static FloorLayer floor;
	private static AirLayer air;
	private static GlowLayer glow;

	//read by the other systems
	static boolean bossVisible = false;
	static float idleTime = 0f;

	private static final float TS = DungeonTilemap.SIZE;

	private static String region(){
		if (Dungeon.level == null) return "";
		String t = Dungeon.level.tilesTex();
		return t == null ? "" : t;
	}

	// ================================================================ things in flight, for their reflections

	private static final ArrayList<MissileSprite> thrown = new ArrayList<>();
	private static final ArrayList<float[]> boltColors = new ArrayList<>();
	private static final ArrayList<MagicMissile> bolts = new ArrayList<>();

	//thrown weapons, arrows and the like
	public static void trackThrown( MissileSprite s ){
		synchronized (thrown){
			if (!thrown.contains(s)) thrown.add(s);
		}
	}

	//magic bolts, with their colour (r < 0: rainbow)
	public static void trackBolt( MagicMissile m, float r, float g, float b ){
		synchronized (bolts){
			bolts.add(m);
			boltColors.add(new float[]{r, g, b});
		}
	}

	// ================================================================ textures

	//soft round light, for glows on the water
	private static SmartTexture glowTex(){
		String key = "amb_glow";
		if (!TextureCache.contains(key)){
			SmartTexture tx = TextureCache.create(key, 16, 16);
			Pixmap p = tx.bitmap;
			p.setBlending(Pixmap.Blending.None);
			for (int y = 0; y < 16; y++){
				for (int x = 0; x < 16; x++){
					float dx = (x + 0.5f - 8f) / 8f, dy = (y + 0.5f - 8f) / 8f;
					float d = (float)Math.sqrt(dx*dx + dy*dy);
					float a = d >= 1f ? 0f : (1f - d) * (1f - d);
					p.drawPixel(x, y, 0xFFFFFF00 | (int)(a * 255));
				}
			}
		}
		return TextureCache.get(key);
	}

	private static SmartTexture shadowTex(){
		String key = "amb_shadow";
		if (!TextureCache.contains(key)){
			SmartTexture tx = TextureCache.create(key, 16, 8);
			Pixmap p = tx.bitmap;
			p.setBlending(Pixmap.Blending.None);
			for (int y = 0; y < 8; y++){
				for (int x = 0; x < 16; x++){
					float dx = (x + 0.5f - 8f) / 8f, dy = (y + 0.5f - 4f) / 4f;
					float d = (float)Math.sqrt(dx*dx + dy*dy);
					float a = d >= 1f ? 0f : (1f - d) * (1f - d) * 1.6f;
					if (a > 1f) a = 1f;
					p.drawPixel(x, y, (0x000000 << 8) | (int)(a * 255));
				}
			}
			//uploaded when first drawn, which is always on the render thread
		}
		return TextureCache.get(key);
	}

	private static SmartTexture shaftTex(){
		String key = "amb_shaft";
		if (!TextureCache.contains(key)){
			SmartTexture tx = TextureCache.create(key, 16, 32);
			Pixmap p = tx.bitmap;
			p.setBlending(Pixmap.Blending.None);
			for (int y = 0; y < 32; y++){
				for (int x = 0; x < 16; x++){
					float dx = Math.abs(x + 0.5f - 8f) / 8f;
					float fy = (y + 0.5f) / 32f;
					//beam widens and fades towards the bottom
					float w = 0.45f + 0.55f * fy;
					float a = Math.max(0f, 1f - dx / w);
					a = a * a * (0.25f + 0.75f * fy) * (1f - fy * fy * 0.6f);
					p.drawPixel(x, y, 0xFFFFFF00 | (int)(Math.min(1f, a) * 255));
				}
			}
			//uploaded when first drawn, which is always on the render thread
		}
		return TextureCache.get(key);
	}

	// ================================================================ floor layer: shadows, reflections, decals, light shafts

	public static class FloorLayer extends Group {

		private final HashMap<Char, Image> shadows = new HashMap<>();
		private final HashMap<Char, Image> reflections = new HashMap<>();
		private final Group decals = new Group();
		private final Group shafts = new Group();
		private final Group moments = new Group();
		private final Group shadowGroup = new Group();
		private final Group reflGroup = new Group();
		private int shaftHash = 0;
		private float time = 0;

		//reflections are drawn between the water and the floor tiles: the floor sits above the water,
		// so it hides whatever part of a reflection isn't over water, just like a real bank
		public Group reflections(){
			return reflGroup;
		}

		public FloorLayer(){
			floor = this;
			add(shafts);
			add(moments);
			add(decals);
			add(shadowGroup);
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			Level level = Dungeon.level;
			if (level == null || !enabled()){
				visible = false;
				reflGroup.visible = false;
				return;
			}
			visible = true;
			reflGroup.visible = true;

			WaterMask.update(level);

			ArrayList<Char> chars = new ArrayList<>();
			if (Dungeon.hero != null) chars.add(Dungeon.hero);
			java.util.Collections.addAll(chars, Atmosphere.mobs(level));

			//drop entries for characters that are gone
			Iterator<Char> it = shadows.keySet().iterator();
			while (it.hasNext()){
				Char c = it.next();
				if (!chars.contains(c) || c.sprite == null || c.sprite.parent == null){
					Image s = shadows.get(c);
					s.killAndErase();
					it.remove();
				}
			}
			it = reflections.keySet().iterator();
			while (it.hasNext()){
				Char c = it.next();
				if (!chars.contains(c) || c.sprite == null || c.sprite.parent == null){
					Image r = reflections.get(c);
					r.killAndErase();
					it.remove();
				}
			}

			for (Char c : chars){
				updateShadow(c, level);
				updateReflection(c, level);
			}
			updateObjectReflections(level);

			updateShafts(level);
		}

		private final HashMap<Char, Float> waveEnergy = new HashMap<>();
		private final HashMap<Char, Float> rippleTimers = new HashMap<>();

		private void updateShadow( Char c, Level level ){
			CharSprite s = c.sprite;
			WaterMask.MaskedImage sh = (WaterMask.MaskedImage) shadows.get(c);
			boolean show = s != null && s.visible && s.parent != null && c.isAlive()
					&& c.pos >= 0 && c.pos < level.length() && !level.pit[c.pos] && !Atmosphere.concealed(c);
			if (!show){
				if (sh != null) sh.visible = false;
				return;
			}
			if (sh == null){
				sh = new WaterMask.MaskedImage(shadowTex());
				//shadows are drawn only on dry ground, so they slide off naturally into water
				sh.invert = true;
				shadowGroup.add(sh);
				shadows.put(c, sh);
			}
			sh.visible = true;

			float fx = s.x + s.width() / 2f;
			float fy = s.y + s.height() - 1f;
			float w = Math.max(8f, s.width() * 0.75f);
			float alpha = c.flying ? 0.22f : 0.38f;
			float stretch = 1f;
			float angle = 0f;

			//shadows are cast away from the hero, who carries the light
			Hero h = Dungeon.hero;
			if (c != h && h != null && h.sprite != null){
				float hx = h.sprite.x + h.sprite.width()/2f, hy = h.sprite.y + h.sprite.height() - 1f;
				float dx = fx - hx, dy = fy - hy;
				float dist = (float)Math.sqrt(dx*dx + dy*dy);
				if (dist > 2f && dist < TS * 6f){
					stretch = 1f + 0.9f * (1f - dist / (TS * 6f));
					angle = (float)Math.toDegrees(Math.atan2(dy, dx));
					alpha *= 0.85f;
				}
			}

			sh.origin.set(8f, 4f);
			sh.scale.set(w / 16f * stretch, (c.flying ? 0.5f : 0.7f));
			sh.angle = angle;
			//offset the shadow along its direction so it starts at the feet
			float off = (stretch - 1f) * w * 0.5f;
			sh.x = fx - 8f + (float)Math.cos(Math.toRadians(angle)) * off;
			sh.y = fy - 4f + (c.flying ? 4f : 0f) + (float)Math.sin(Math.toRadians(angle)) * off * 0.5f;
			//fade out while standing in water, the reflection takes over there
			int fc = DungeonTilemap.worldToTile(fx, fy - 1f, level.width());
			boolean wet = fc >= 0 && fc < level.length() && level.water[fc] && !c.flying;
			Float k = shadowFade.get(c);
			float kv = k == null ? (wet ? 0f : 1f) : k;
			kv += ((wet ? 0f : 1f) - kv) * Math.min(1f, Game.elapsed * 10f);
			shadowFade.put(c, kv);
			sh.alpha(alpha * kv);
			sh.visible = kv > 0.02f;
		}

		private final HashMap<Char, Float> shadowFade = new HashMap<>();

		private void updateReflection( Char c, Level level ){
			CharSprite s = c.sprite;
			WaterMask.MaskedImage r = (WaterMask.MaskedImage) reflections.get(c);

			boolean show = s != null && s.visible && s.parent != null && c.isAlive() && s.texture != null && !Atmosphere.concealed(c);
			int feetCell = -1;
			if (show){
				//use the sprite's actual position, so the reflection follows smoothly while moving
				float fx = s.x + s.width() / 2f;
				float fy = s.y + s.height() - 2f;
				feetCell = DungeonTilemap.worldToTile(fx, fy, level.width());
				if (feetCell < 0 || feetCell >= level.length()) feetCell = -1;
				//always drawn: it lies under the floor tiles, so it only shows where there is water,
				// whether the creature stands in it or on the bank just above
			}

			if (!show){
				if (r != null) r.visible = false;
				return;
			}
			if (r == null){
				r = new WaterMask.MaskedImage();
				//the floor tiles above do the masking, so the reflection meets the bank's ragged edge exactly
				r.masked = false;
				reflGroup.add(r);
				reflections.put(c, r);
			}
			r.visible = true;

			//mirror the sprite's current frame, full height like a real reflection
			if (r.texture != s.texture) r.texture = s.texture;
			r.frame(s.frame());
			r.flipHorizontal = s.flipHorizontal;

			float h = s.height;
			r.origin.set(s.width / 2f, h);
			r.scale.set(s.scale.x, -s.scale.y);
			r.x = s.x;
			//flying creatures hover above their reflection
			r.y = s.y + (c.flying ? 5f : 0f);
			r.angle = 0;

			//movement stirs the water: the reflection ripples more, then settles
			Float e = waveEnergy.get(c);
			float energy = e == null ? 0f : e;
			if (s.isMoving && feetCell >= 0 && level.water[feetCell]) energy = 1f;
			energy = Math.max(0f, energy - Game.elapsed * 0.8f);
			waveEnergy.put(c, energy);

			r.fadeFromY = s.y + h - 1f + (c.flying ? 5f : 0f);
			//long enough that the whole body, face included, shows in the water, even from the bank
			// where the floor's edge hides the part nearest the feet
			r.fadeLength = h * 1.6f;
			r.wave = 0.35f + 1.1f * energy;
			r.waveTime = time + c.id() * 0.37f;

			//darker and tinted towards the water, rather than a pale copy
			r.resetColor();
			r.rm = 0.80f; r.gm = 0.92f; r.bm = 0.96f;
			r.alpha(0.62f);

			//piranhas swim under the surface: they stir the water but cast no reflection
			if (c instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Piranha) r.visible = false;

			//standing in water now and then sends out a ripple
			if (feetCell >= 0 && level.water[feetCell] && !c.flying){
				Float rt = rippleTimers.get(c);
				float t = (rt == null ? 0.5f : rt) - Game.elapsed;
				if (t <= 0){
					t = s.isMoving ? 0.35f : Random.Float(1.6f, 2.6f);
					GameScene.ripple(feetCell);
				}
				rippleTimers.put(c, t);
			}
		}

		// ------------------------------------------------ reflections of things that aren't creatures

		private final HashMap<Object, WaterMask.MaskedImage> objRefl = new HashMap<>();
		private final java.util.HashSet<Object> objSeen = new java.util.HashSet<>();

		//light added onto the water rather than blended over it
		public static class WaterGlow extends WaterMask.MaskedImage {
			public WaterGlow(){
				super(glowTex());
				masked = false;
			}
			@Override
			public void draw() {
				Blending.setLightMode();
				super.draw();
				Blending.setNormalMode();
			}
		}

		//the bolt's trail on the water: small patches of its light, fading out behind it
		public static class TrailGlow extends WaterGlow {
			float life, maxLife, peak;
			public TrailGlow(){
				super();
			}
			void reset( float x, float y, float r, float g, float b ){
				revive();
				visible = true;
				origin.set(8f, 8f);
				scale.set(Random.Float(1.2f, 1.5f), Random.Float(0.55f, 0.7f));
				this.x = x - 8f + Random.Float(-1.5f, 1.5f);
				this.y = y - 8f + Random.Float(-1f, 1f);
				resetColor();
				rm = r; gm = g; bm = b;
				maxLife = life = Random.Float(0.35f, 0.45f);
				peak = 0.32f;
				alpha(peak);
			}
			@Override
			public void update() {
				super.update();
				life -= Game.elapsed;
				if (life <= 0){
					kill();
					return;
				}
				float k = life / maxLife;
				alpha(peak * k);
				//spreads a little as it fades, like light settling on the surface
				scale.x += Game.elapsed * 0.8f;
			}
		}

		private final HashMap<MagicMissile, Float> trailTimers = new HashMap<>();

		private boolean inView( Level level, float wx, float wy ){
			int cell = DungeonTilemap.worldToTile(wx, wy, level.width());
			return cell >= 0 && cell < level.length() && level.heroFOV[cell];
		}

		//a reflection of the image s, mirrored about the water line at world height mirrorY.
		// Like the creatures' it lies under the floor tiles, so it only shows where there is water
		private void mirror( Object key, Image s, float mirrorY, float alpha ){
			objSeen.add(key);
			WaterMask.MaskedImage r = objRefl.get(key);
			if (r == null){
				r = new WaterMask.MaskedImage();
				r.masked = false;
				reflGroup.add(r);
				objRefl.put(key, r);
			}
			r.visible = true;
			if (r.texture != s.texture) r.texture = s.texture;
			r.frame(s.frame());
			r.flipHorizontal = s.flipHorizontal;
			r.origin.set(s.origin.x, s.origin.y);
			r.scale.set(s.scale.x, -s.scale.y);
			r.angle = -s.angle;
			r.x = s.x;
			//the pivot is mirrored about the water line, the flipped scale does the rest
			r.y = 2f * mirrorY - (s.y + s.origin.y) - s.origin.y;
			r.fadeFromY = mirrorY;
			r.fadeLength = Math.max(8f, s.height * 1.6f);
			r.wave = 0.35f;
			r.waveTime = time + (key.hashCode() % 97) * 0.37f;
			r.resetColor();
			r.rm = 0.80f; r.gm = 0.92f; r.bm = 0.96f;
			r.alpha(alpha * s.alpha());
		}

		private void updateObjectReflections( Level level ){
			objSeen.clear();

			//items lying on the floor are not reflected: they lie flat, so a mirrored copy only made them
			// look stood up on the water

			//thrown weapons fly about half a tile above the ground: their reflection shows that gap
			synchronized (thrown){
				Iterator<MissileSprite> it = thrown.iterator();
				while (it.hasNext()){
					MissileSprite s = it.next();
					if (!s.alive || s.parent == null){ it.remove(); continue; }
					if (!s.visible || s.texture == null) continue;
					float cy = s.y + s.height / 2f;
					float ground = cy + TS * 0.45f;
					if (!inView(level, s.x + s.width / 2f, ground)) continue;
					mirror(s, s, ground, 0.6f);
				}
			}

			//magic bolts light up the water they pass over
			synchronized (bolts){
				for (int i = bolts.size() - 1; i >= 0; i--){
					MagicMissile m = bolts.get(i);
					if (m == null || !m.on || !m.alive || m.parent == null){
						bolts.remove(i); boltColors.remove(i);
						trailTimers.remove(m);
						continue;
					}
					float cx = m.x + m.width / 2f, cy = m.y + m.height / 2f;
					float ground = cy + TS * 0.45f;
					if (!inView(level, cx, ground)) continue;
					float[] c = boltColors.get(i);
					float rr = c[0], gg = c[1], bb = c[2];
					if (rr < 0){
						float hh = (time * 1.5f) % 1f;
						rr = Math.max(0f, Math.min(1f, Math.abs(hh * 6f - 3f) - 1f));
						gg = Math.max(0f, Math.min(1f, 2f - Math.abs(hh * 6f - 2f)));
						bb = Math.max(0f, Math.min(1f, 2f - Math.abs(hh * 6f - 4f)));
					}
					objSeen.add(m);
					WaterMask.MaskedImage g = objRefl.get(m);
					if (g == null){
						g = new WaterGlow();
						reflGroup.add(g);
						objRefl.put(m, g);
					}
					g.visible = true;
					//a wide, flattened pool of light where the bolt's reflection would be
					float flick = 0.85f + 0.15f * (float)Math.sin(time * 23f + i);
					g.origin.set(8f, 8f);
					g.scale.set(2.4f * flick, 1.1f * flick);
					g.x = cx - 8f;
					g.y = ground + (ground - cy) - 8f;
					g.resetColor();
					g.rm = rr; g.gm = gg; g.bm = bb;
					g.alpha(0.32f);

					//and leaves a fading trail of light behind on the water
					Float tt = trailTimers.get(m);
					float tv = (tt == null ? 0f : tt) - Game.elapsed;
					if (tv <= 0){
						tv = 0.015f;
						TrailGlow tg = (TrailGlow) reflGroup.recycle(TrailGlow.class);
						tg.reset(cx, ground + (ground - cy), rr, gg, bb);
					}
					trailTimers.put(m, tv);
				}
			}

			//drop the ones no longer needed
			Iterator<Object> it = objRefl.keySet().iterator();
			while (it.hasNext()){
				Object k = it.next();
				if (!objSeen.contains(k)){
					objRefl.get(k).killAndErase();
					it.remove();
				}
			}
		}

		private void updateShafts( Level level ){
			int h = level.length() * 31 + level.width();
			if (h == shaftHash && shafts.length > 0) return;
			shaftHash = h;
			shafts.clear();
			for (int i = 0; i < level.length(); i++){
				int t = level.map[i];
				if (t == Terrain.ENTRANCE || t == Terrain.ENTRANCE_SP){
					shafts.add(new Shaft(i, 0xFFF0C8, 0.35f));
				}
			}
		}

		private final ArrayList<Decal> decalList = new ArrayList<>();

		//reuses dead decals, or the oldest one once there are too many
		Decal recycleDecal(){
			Decal d = (Decal) decals.getFirstAvailable(Decal.class);
			if (d == null){
				if (decalList.size() >= 220){
					d = decalList.remove(0);
				} else {
					d = new Decal();
					decals.add(d);
				}
			} else {
				decalList.remove(d);
			}
			decalList.add(d);
			return d;
		}

		@Override
		public void destroy() {
			super.destroy();
			WaterMask.release();
			if (floor == this) floor = null;
		}
	}

	//a beam of light coming down from above
	//tall beam of light for rare drops, brightest at its foot
	private static SmartTexture pillarTex(){
		String key = "amb_pillar";
		if (!TextureCache.contains(key)){
			SmartTexture tx = TextureCache.create(key, 16, 48);
			Pixmap p = tx.bitmap;
			p.setBlending(Pixmap.Blending.None);
			for (int y = 0; y < 48; y++){
				for (int x = 0; x < 16; x++){
					float dx = Math.abs(x + 0.5f - 8f) / 8f;
					float fy = (y + 0.5f) / 48f;
					float w = 0.30f + 0.30f * fy;
					float a = Math.max(0f, 1f - dx / w);
					a = a * a * (float)Math.pow(fy, 1.3f);
					//soft foot instead of a hard edge
					if (fy > 0.9f) a *= 1f - (fy - 0.9f) / 0.1f * 0.7f;
					p.drawPixel(x, y, 0xFFFFFF00 | (int)(Math.min(1f, a * 1.2f) * 255));
				}
			}
			//uploaded when first drawn, which is always on the render thread
		}
		return TextureCache.get(key);
	}

	//flattened ring of light spreading over the floor
	private static SmartTexture ringTex(){
		String key = "amb_ring";
		if (!TextureCache.contains(key)){
			SmartTexture tx = TextureCache.create(key, 64, 32);
			Pixmap p = tx.bitmap;
			p.setBlending(Pixmap.Blending.None);
			for (int y = 0; y < 32; y++){
				for (int x = 0; x < 64; x++){
					float dx = (x + 0.5f - 32f) / 32f, dy = (y + 0.5f - 16f) / 16f;
					float d = (float)Math.sqrt(dx*dx + dy*dy);
					float a = Math.max(0f, 1f - Math.abs(d - 0.84f) / 0.14f);
					a = a * a;
					if (d < 0.84f) a = Math.max(a, 0.14f * (d / 0.84f) * (d / 0.84f));
					p.drawPixel(x, y, 0xFFFFFF00 | (int)(Math.min(1f, a) * 255));
				}
			}
			//uploaded when first drawn, which is always on the render thread
		}
		return TextureCache.get(key);
	}

	private static class Shaft extends Image {
		private final int cell;
		private float t = Random.Float(10f);
		private final float baseAlpha;

		Shaft( int cell, int color, float alpha ){
			super(shaftTex());
			this.cell = cell;
			this.baseAlpha = alpha;
			hardlight(color);
			PointF p = DungeonTilemap.tileCenterToWorld(cell);
			scale.set(1.4f, 1.3f);
			x = p.x - 8f * 1.4f;
			y = p.y - 32f * 1.3f + 8f;
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			Level level = Dungeon.level;
			visible = level != null && cell < level.length() && (level.heroFOV[cell] || level.visited[cell]);
			alpha(baseAlpha * (0.8f + 0.2f * (float)Math.sin(t * 0.9f)));
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	//pixels left on the floor (blood, scorch marks, frost), slowly fading away
	public static class Decal extends PseudoPixel {
		float life, maxLife;

		public void reset( float x, float y, int color, float size, float life ){
			revive();
			this.x = x;
			this.y = y;
			color(color);
			size(size);
			this.life = this.maxLife = life;
			alpha(1f);
		}

		@Override
		public void update() {
			super.update();
			life -= Game.elapsed;
			if (life <= 0){
				kill();
				return;
			}
			float k = life / maxLife;
			alpha(Math.min(1f, k * 3f) * 0.85f);
		}
	}

	private static void decalSplat( float x, float y, int color, int n, float spread, float life ){
		if (floor == null || !enabled()) return;
		for (int i = 0; i < n; i++){
			Decal d = floor.recycleDecal();
			float r = Random.Float(spread);
			float a = Random.Float(6.2832f);
			d.reset((float)Math.floor(x + Math.cos(a) * r), (float)Math.floor(y + Math.sin(a) * r * 0.6f),
					color, Random.Int(3) == 0 ? 2 : 1, life * Random.Float(0.7f, 1.2f));
		}
	}

	// ================================================================ hooks called from game code

	public static void onBlood( CharSprite s, int color, int damage ){
		if (s == null || s.ch == null || !s.visible || !enabled()) return;
		Char c = s.ch;
		if (Dungeon.level == null || c.pos < 0 || c.pos >= Dungeon.level.length()) return;
		if (Dungeon.level.water[c.pos] || Dungeon.level.pit[c.pos]) return;
		float fx = s.x + s.width() / 2f;
		float fy = s.y + s.height() - 2f;
		int n = (int)Math.min(9, 2 + 8 * Math.sqrt(damage / (float)Math.max(1, c.HT)));
		int col = (color & 0xFFFFFF) | 0xFF000000;
		decalSplat(fx, fy, darken(col, 0.75f), n, 5f, 25f);
	}

	public static void onExplosion( int cell ){
		if (Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length() || !enabled()) return;
		PointF p = DungeonTilemap.tileCenterToWorld(cell);
		decalSplat(p.x, p.y + 3, 0xFF1A120C, 26, 10f, 45f);
		decalSplat(p.x, p.y + 3, 0xFF3A2A1A, 12, 14f, 35f);
		if (glow != null){
			//lingering smoke
			for (int i = 0; i < 10; i++){
				Mote m = glow.recycleMote(false);
				m.smoke(p.x + Random.Float(-8, 8), p.y + Random.Float(-6, 6));
			}
		}
	}

	public static void onMove( CharSprite s, int from, int to ){
		if (s == null || s.ch == null || !s.visible || Dungeon.level == null || !enabled()) return;
		Level level = Dungeon.level;
		if (to < 0 || to >= level.length() || from < 0 || from >= level.length()) return;
		Char c = s.ch;

		PointF dest = DungeonTilemap.tileCenterToWorld(to);
		float fx = dest.x, fy = dest.y + 5f;

		//elemental trails
		if (c instanceof Elemental.FireElemental || c instanceof YogFist.BurningFist){
			PointF p = DungeonTilemap.tileCenterToWorld(from);
			if (!level.water[from]) decalSplat(p.x, p.y + 4, 0xFF221510, 6, 5f, 20f);
		} else if (c instanceof Elemental.FrostElemental){
			PointF p = DungeonTilemap.tileCenterToWorld(from);
			if (!level.water[from]) decalSplat(p.x, p.y + 4, 0xFFCCE8FF, 7, 6f, 12f);
		}

		if (c.flying || air == null) return;

		int t = level.map[to];
		if (level.water[to]){
			for (int i = 0; i < 5; i++){
				air.recycleMote().droplet(fx + Random.Float(-3, 3), fy - 1);
			}
		} else if (t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS || t == Terrain.GRASS){
			int n = t == Terrain.GRASS ? 2 : 4;
			for (int i = 0; i < n; i++){
				air.recycleMote().leaf(fx + Random.Float(-4, 4), fy - Random.Float(2, 7));
			}
		} else if (!level.pit[to]){
			for (int i = 0; i < 3; i++){
				air.recycleMote().dust(fx + Random.Float(-3, 3), fy);
			}
		}
	}

	public static void onHeroDeath(){
		if (!enabled()) return;
		slowmo = 1.6f;
	}

	//a short moment of slow motion for big moments (seconds of real time)
	public static void slowMotion( float seconds ){
		if (!enabled()) return;
		slowmo = Math.max(slowmo, seconds);
	}

	private static float slowmo = 0f;

	private static int darken( int argb, float k ){
		int r = (int)(((argb >> 16) & 0xFF) * k);
		int g = (int)(((argb >> 8) & 0xFF) * k);
		int b = (int)((argb & 0xFF) * k);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	// ================================================================ particles

	public static class Mote extends PixelParticle {
		private int kind;
		private float phase;
		private float baseAlpha;
		private boolean light;

		static final int DUST = 0, DROPLET = 1, LEAF = 2, MOTE = 3, EMBER = 4, DRIP = 5, BUBBLE = 6, SPARKLE = 7, SMOKE = 8, FALL = 9, RISE = 10;

		void dust( float x, float y ){
			reset(x, y, 0xFF8A7F70, 1, Random.Float(0.35f, 0.6f));
			kind = DUST; baseAlpha = 0.55f;
			speed.set(Random.Float(-8, 8), Random.Float(-10, -4));
			acc.set(0, 12);
		}

		void droplet( float x, float y ){
			reset(x, y, 0xFFB8DCE8, 1, Random.Float(0.3f, 0.5f));
			kind = DROPLET; baseAlpha = 0.8f;
			speed.set(Random.Float(-12, 12), Random.Float(-28, -14));
			acc.set(0, 110);
		}

		void leaf( float x, float y ){
			reset(x, y, Random.Int(2) == 0 ? 0xFF5E9E3A : 0xFF7DBA4C, 1, Random.Float(0.6f, 1.0f));
			kind = LEAF; baseAlpha = 0.9f; phase = Random.Float(6.28f);
			speed.set(Random.Float(-10, 10), Random.Float(-16, -8));
			acc.set(0, 18);
		}

		void mote( float x, float y, int color, float life ){
			reset(x, y, color, 1, life);
			kind = MOTE; baseAlpha = 0.5f; phase = Random.Float(6.28f);
			speed.set(Random.Float(-2, 2), Random.Float(-3, 1));
			acc.set(0, 0);
		}

		void ember( float x, float y ){
			reset(x, y, Random.Int(3) == 0 ? 0xFFFFD060 : 0xFFFF7A20, 1, Random.Float(1.2f, 2.4f));
			kind = EMBER; baseAlpha = 1f; phase = Random.Float(6.28f);
			speed.set(Random.Float(-3, 3), Random.Float(-14, -7));
			acc.set(0, -2);
		}

		void drip( float x, float y, float fall ){
			reset(x, y - fall, 0xFFA8D0DC, 1, fall / 70f);
			kind = DRIP; baseAlpha = 0.85f;
			speed.set(0, 40);
			acc.set(0, 60);
		}

		void bubble( float x, float y, int color ){
			reset(x, y, color, 1, Random.Float(0.6f, 1.1f));
			kind = BUBBLE; baseAlpha = 0.8f;
			speed.set(0, -3);
			acc.set(0, 0);
		}

		void sparkle( float x, float y, int color ){
			reset(x, y, color, 1, Random.Float(0.4f, 0.8f));
			kind = SPARKLE; baseAlpha = 1f;
			speed.set(0, -4);
			acc.set(0, 0);
		}

		//a speck of light drifting upwards, used by the reward moments
		void rise( float x, float y, int color, float life, float up ){
			reset(x, y, color, 1, life);
			kind = RISE; baseAlpha = 1f; phase = Random.Float(6.28f);
			speed.set(Random.Float(-2, 2), -up);
			acc.set(0, up * 0.5f);
		}

		void smoke( float x, float y ){
			reset(x, y, 0xFF3A3632, 2, Random.Float(1.5f, 2.6f));
			kind = SMOKE; baseAlpha = 0.45f;
			speed.set(Random.Float(-4, 4), Random.Float(-8, -4));
			acc.set(0, -1);
		}

		void fall( float x, float y, int color ){
			reset(x, y, color, 1, Random.Float(0.6f, 1.1f));
			kind = FALL; baseAlpha = 0.6f;
			speed.set(Random.Float(-2, 2), Random.Float(10, 20));
			acc.set(0, 25);
		}

		@Override
		public void update() {
			super.update();
			float k = left / lifespan;
			float t = 1f - k;
			switch (kind){
				case LEAF:
					speed.x = (float)Math.sin((t * 9f) + phase) * 12f;
					alpha(baseAlpha * Math.min(1f, k * 3f));
					break;
				case MOTE:
					x += (float)Math.sin(t * 5f + phase) * 0.06f;
					//fade in and out
					alpha(baseAlpha * Math.min(1f, Math.min(t * 4f, k * 4f)));
					break;
				case EMBER:
					x += (float)Math.sin(t * 11f + phase) * 0.15f;
					alpha(baseAlpha * Math.min(1f, k * 2.5f));
					break;
				case BUBBLE:
					size(1f + (t > 0.8f ? 1f : 0f));
					alpha(baseAlpha * (t > 0.9f ? 0.3f : 1f));
					break;
				case SPARKLE:
					size(t < 0.5f ? 1f : (t < 0.7f ? 2f : 1f));
					alpha(baseAlpha * (float)Math.sin(t * Math.PI));
					break;
				case RISE:
					x += (float)Math.sin(t * 7f + phase) * 0.08f;
					size(t < 0.2f ? 2f : 1f);
					alpha(baseAlpha * Math.min(1f, Math.min(t * 8f, k * 1.6f)));
					break;
				case SMOKE:
					size(2f + t * 3f);
					alpha(baseAlpha * Math.min(1f, Math.min(t * 5f, k * 1.5f)));
					break;
				default:
					alpha(baseAlpha * Math.min(1f, k * 2f));
					break;
			}
		}

		@Override
		public void kill() {
			super.kill();
			if (kind == DRIP && Dungeon.level != null){
				//splash where the drip lands
				int cell = DungeonTilemap.worldToTile(x, y, Dungeon.level.width());
				if (cell >= 0 && cell < Dungeon.level.length() && Dungeon.level.heroFOV[cell]){
					if (Dungeon.level.water[cell]){
						GameScene.ripple(cell);
					} else if (air != null){
						for (int i = 0; i < 2; i++) air.recycleMote().droplet(x, y);
					}
				}
			}
		}
	}

	//dust, leaves, droplets: lit by the dungeon lighting
	public static class AirLayer extends Group {
		private float timer = 0;
		private float dripTimer = 2f, fallTimer = 3f, rippleTimer = 1.5f, soundTimer = 15f;
		private float time = 0;

		private final HashMap<Char, Float> ghostTimers = new HashMap<>();

		public AirLayer(){
			air = this;
		}

		Mote recycleMote(){
			Mote m = (Mote) recycle(Mote.class);
			return m;
		}

		@Override
		public void update() {
			super.update();
			if (!enabled() || Dungeon.level == null || Dungeon.hero == null || Dungeon.hero.sprite == null){
				return;
			}
			float dt = Game.elapsed;
			time += dt;
			Level level = Dungeon.level;
			String reg = region();

			timer -= dt;
			if (timer <= 0){
				timer = 0.12f;
				//floating dust in the light
				int color;
				switch (reg){
					case Assets.Environment.TILES_CITY: color = 0xFFE8C878; break;
					case Assets.Environment.TILES_CAVES_CRYSTAL: color = 0xFF9FE6FF; break;
					case Assets.Environment.TILES_HALLS: color = 0xFFB07060; break;
					default: color = 0xFFCFC6B0; break;
				}
				int cell = randomVisibleCell(level, 5);
				if (cell != -1){
					PointF p = DungeonTilemap.tileCenterToWorld(cell);
					recycleMote().mote(p.x + Random.Float(-8, 8), p.y + Random.Float(-10, 6), color, Random.Float(2.5f, 4.5f));
				}
			}

			//sewers: drips from the ceiling
			if (reg.equals(Assets.Environment.TILES_SEWERS) || reg.startsWith("environment/tiles_caves")){
				dripTimer -= dt;
				if (dripTimer <= 0){
					dripTimer = Random.Float(0.8f, 2.2f);
					int cell = randomVisibleCell(level, 6);
					if (cell != -1 && !level.solid[cell]){
						PointF p = DungeonTilemap.tileCenterToWorld(cell);
						recycleMote().drip(p.x + Random.Float(-5, 5), p.y + Random.Float(-3, 5), Random.Float(30, 48));
					}
				}
			}

			//prison and caves: dust and grit falling from above
			if (reg.equals(Assets.Environment.TILES_PRISON) || reg.startsWith("environment/tiles_caves")){
				fallTimer -= dt;
				if (fallTimer <= 0){
					fallTimer = Random.Float(1.5f, 4f);
					int cell = randomVisibleCell(level, 6);
					if (cell != -1){
						PointF p = DungeonTilemap.tileCenterToWorld(cell);
						int n = Random.IntRange(3, 6);
						int col = reg.equals(Assets.Environment.TILES_PRISON) ? 0xFF9A948A : 0xFF8E7A62;
						for (int i = 0; i < n; i++){
							recycleMote().fall(p.x + Random.Float(-3, 3), p.y - 14 + Random.Float(-3, 3), col);
						}
					}
				}
			}

			//idle ripples on water
			rippleTimer -= dt;
			if (rippleTimer <= 0){
				rippleTimer = Random.Float(0.7f, 1.8f);
				int cell = randomVisibleCell(level, 6);
				if (cell != -1 && level.water[cell] && !reg.equals(Assets.Environment.TILES_HALLS)){
					GameScene.ripple(cell);
				}
			}

			updateGhosts(level, dt);
			updateSounds(level, reg, dt);
		}

		private void updateGhosts( Level level, float dt ){
			for (Mob m : Atmosphere.mobs(level)){
				if (!(m instanceof Wraith || m instanceof TormentedSpirit)) continue;
				CharSprite s = m.sprite;
				if (s == null || !s.visible || !m.isAlive()) continue;
				//translucent, gently wavering
				s.alpha(0.72f + 0.12f * (float)Math.sin(time * 3f + m.id()));
				Float t = ghostTimers.get(m);
				t = (t == null ? 0f : t) - dt;
				if (t <= 0 && s.isMoving){
					t = 0.09f;
					Afterimage a = (Afterimage) recycle(Afterimage.class);
					a.reset(s);
				}
				ghostTimers.put(m, t);
			}
		}

		private void updateSounds( Level level, String reg, float dt ){
			soundTimer -= dt;
			if (soundTimer > 0) return;
			Hero h = Dungeon.hero;
			int hp = h.pos;
			int w = level.width();

			//proximity: fire and lava
			float nearest = 99f;
			for (int i = 0; i < level.length(); i++){
				boolean hot = (level.map[i] == Terrain.WALL_DECO && reg.equals(Assets.Environment.TILES_PRISON))
						|| (level.map[i] == Terrain.WATER && reg.equals(Assets.Environment.TILES_HALLS));
				if (!hot || !level.heroFOV[i]) continue;
				int dx = i % w - hp % w, dy = i / w - hp / w;
				float d = (float)Math.sqrt(dx*dx + dy*dy);
				if (d < nearest) nearest = d;
			}
			if (nearest < 5f){
				Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.06f + 0.12f * (1f - nearest / 5f));
				soundTimer = Random.Float(2.5f, 4f);
				return;
			}

			switch (reg){
				case Assets.Environment.TILES_SEWERS:
					Sample.INSTANCE.play(Assets.Sounds.WATER, 0.08f, Random.Float(1.2f, 1.6f));
					soundTimer = Random.Float(6f, 12f);
					break;
				case Assets.Environment.TILES_PRISON:
					Sample.INSTANCE.play(Assets.Sounds.CHAINS, 0.06f, Random.Float(0.8f, 1.0f));
					soundTimer = Random.Float(18f, 35f);
					break;
				case Assets.Environment.TILES_CAVES:
				case Assets.Environment.TILES_CAVES_CRYSTAL:
				case Assets.Environment.TILES_CAVES_GNOLL:
					Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.05f, Random.Float(0.9f, 1.1f));
					soundTimer = Random.Float(20f, 40f);
					break;
				default:
					soundTimer = 20f;
					break;
			}
		}

		@Override
		public void destroy() {
			super.destroy();
			if (air == this) air = null;
		}
	}

	public static class Afterimage extends Image {
		private float life;

		public void reset( CharSprite s ){
			revive();
			texture = s.texture;
			frame(s.frame());
			flipHorizontal = s.flipHorizontal;
			x = s.x; y = s.y;
			origin.set(s.origin);
			scale.set(s.scale);
			tint(0.7f, 0.8f, 1f, 0.4f);
			life = 0.35f;
			alpha(0.35f);
		}

		@Override
		public void update() {
			super.update();
			life -= Game.elapsed;
			if (life <= 0){
				kill();
			} else {
				alpha(0.35f * life / 0.35f);
			}
		}
	}

	//embers, sparkles, smoke: drawn after the lighting so they glow in the dark
	public static class GlowLayer extends Group {
		private float timer = 0, sparkleTimer = 0, bubbleTimer = 0;
		private final Group lit = new Group();
		private final Group unlit = new Group();

		public GlowLayer(){
			glow = this;
			add(unlit);
			add(lit);
		}

		private final HashMap<Heap, com.watabou.noosa.Halo> halos = new HashMap<>();
		private float haloTime = 0;

		//a soft, wavering glow around valuable items in view
		private void updateHalos( Level level ){
			haloTime += Game.elapsed;
			java.util.HashSet<Heap> seen = new java.util.HashSet<>();
			for (Heap heap : level.heaps.valueList()){
				int col = valuableColor(heap, level);
				if (col == 0) continue;
				seen.add(heap);
				com.watabou.noosa.Halo h = halos.get(heap);
				if (h == null){
					h = new com.watabou.noosa.Halo(10, col, 0.3f);
					lit.add(h);
					halos.put(heap, h);
				}
				float ph = heap.pos * 0.73f;
				float wave = (float)Math.sin(haloTime * 2.1f + ph) * 0.6f + (float)Math.sin(haloTime * 3.7f + ph * 1.7f) * 0.4f;
				h.radius(11f + 2.2f * wave);
				h.alpha(0.34f + 0.13f * wave);
				PointF p = DungeonTilemap.tileCenterToWorld(heap.pos);
				h.point(p.x, p.y + 1);
				h.visible = true;
			}
			Iterator<Heap> it = halos.keySet().iterator();
			while (it.hasNext()){
				Heap heap = it.next();
				if (!seen.contains(heap)){
					halos.get(heap).killAndErase();
					it.remove();
				}
			}
		}

		Mote recycleMote( boolean light ){
			return (Mote) (light ? lit : unlit).recycle(Mote.class);
		}

		@Override
		public void update() {
			super.update();
			if (!enabled() || Dungeon.level == null || Dungeon.hero == null) return;
			float dt = Game.elapsed;
			Level level = Dungeon.level;
			String reg = region();

			updateHalos(level);

			//halls: embers rising from the fire streams and floor cracks
			if (reg.equals(Assets.Environment.TILES_HALLS)){
				timer -= dt;
				if (timer <= 0){
					timer = 0.07f;
					int cell = randomVisibleCell(level, 7);
					if (cell != -1 && (level.water[cell] || Random.Int(4) == 0)){
						PointF p = DungeonTilemap.tileCenterToWorld(cell);
						recycleMote(true).ember(p.x + Random.Float(-7, 7), p.y + Random.Float(-4, 6));
					}
				}
			}

			//bubbles in water
			bubbleTimer -= dt;
			if (bubbleTimer <= 0){
				bubbleTimer = Random.Float(0.4f, 1.0f);
				int cell = randomVisibleCell(level, 6);
				if (cell != -1 && level.water[cell]){
					PointF p = DungeonTilemap.tileCenterToWorld(cell);
					int col = reg.equals(Assets.Environment.TILES_HALLS) ? 0xFFFF9040 : 0xFFA8D8E0;
					recycleMote(reg.equals(Assets.Environment.TILES_HALLS)).bubble(p.x + Random.Float(-6, 6), p.y + Random.Float(-5, 5), col);
				}
			}

			//sparkles on rare items and special places
			sparkleTimer -= dt;
			if (sparkleTimer <= 0){
				sparkleTimer = 0.25f;
				for (Heap heap : level.heaps.valueList()){
					int col = valuableColor(heap, level);
					if (col != 0 && Random.Int(3) == 0){
						PointF p = DungeonTilemap.tileCenterToWorld(heap.pos);
						recycleMote(true).sparkle(p.x + Random.Float(-5, 5), p.y + Random.Float(-7, 2), 0xFF000000 | col);
					}
				}
				for (int i = 0; i < 4; i++){
					int cell = randomVisibleCell(level, 7);
					if (cell == -1) continue;
					int t = level.map[cell];
					int col = 0;
					if (t == Terrain.PEDESTAL) col = 0xFFFFE8A0;
					else if (t == Terrain.WELL) col = 0xFF9FD8FF;
					else if (t == Terrain.ALCHEMY) col = 0xFFD0A0FF;
					else if (t == Terrain.WALL_DECO && reg.startsWith("environment/tiles_caves")) col = 0xFF9FE6FF;
					if (col != 0){
						PointF p = DungeonTilemap.tileCenterToWorld(cell);
						recycleMote(true).sparkle(p.x + Random.Float(-6, 6), p.y + Random.Float(-7, 4), col);
					}
				}
			}
		}

		@Override
		public void draw() {
			unlit.draw();
			Blending.setLightMode();
			lit.draw();
			Blending.setNormalMode();
		}

		@Override
		public void destroy() {
			super.destroy();
			if (glow == this) glow = null;
		}
	}

	//glow color for a valuable item lying in view, 0 for anything else.
	// only items whose kind is obvious from their look, so nothing is given away
	static int valuableColor( Heap heap, Level level ){
		if (heap == null || heap.pos < 0 || heap.pos >= level.length() || !level.heroFOV[heap.pos]) return 0;
		if (heap.type != Heap.Type.HEAP && heap.type != Heap.Type.FOR_SALE) return 0;
		return valuableItemColor(heap.peek());
	}

	static int valuableItemColor( Item it ){
		if (it == null) return 0;
		if (it instanceof Artifact) return 0xFFD878;
		if (it instanceof Wand) return 0x9FD4FF;
		if (it instanceof Ring) return 0xD8A8FF;
		if (it instanceof com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst) return 0xFFE070;
		if (it instanceof com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket) return 0xFFC8E0;
		if (it.visiblyUpgraded() > 0) return 0xFFF0C8;
		return 0;
	}

	// ================================================================ reward moments: rare drops and level ups

	//how grand the moment is: a little more for artifacts, catalysts and well upgraded gear
	private static float rarity( Item it ){
		float s = 1f;
		if (it instanceof Artifact || it instanceof com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst) s = 1.2f;
		int lvl = it.visiblyUpgraded();
		if (lvl > 0) s += Math.min(0.3f, 0.1f * lvl);
		return s;
	}

	//called whenever an item lands on the floor. items the hero has carried are never celebrated
	public static void itemDropped( Item item, int cell ){
		if (item == null || item.atmoKnown) return;
		item.atmoKnown = true;
		celebrate(item, cell);
	}

	//a chest, tomb or skeleton was opened
	public static void heapOpened( Heap heap ){
		if (heap == null) return;
		Item best = null;
		for (Item it : heap.items){
			if (it.atmoKnown) continue;
			it.atmoKnown = true;
			if (valuableItemColor(it) != 0 && (best == null || rarity(it) > rarity(best))) best = it;
		}
		if (best != null) celebrate(best, heap.pos);
	}

	private static void celebrate( Item item, int cell ){
		if (!enabled() || glow == null) return;
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length() || !level.heroFOV[cell]) return;
		int col = valuableItemColor(item);
		if (col == 0) return;
		float s = rarity(item);
		glow.lit.add(new Pillar(cell, col, s));
		Atmosphere.burstCell(cell, 2.6f + 0.8f * s, 0.7f, col, 2.0f);
		Sample.INSTANCE.play(Assets.Sounds.SECRET, 0.6f, 1.1f);
	}

	//beam of light rising from a freshly dropped rare item, with specks drifting up along it
	private static class Pillar extends Group {
		private final int cell;
		private final int col;
		private final float s, px, py;
		private final Image beam;
		private final com.watabou.noosa.Halo flare;
		private float t = 0, speckTimer = 0;

		Pillar( int cell, int col, float s ){
			this.cell = cell;
			this.col = col;
			this.s = s;
			PointF p = DungeonTilemap.tileCenterToWorld(cell);
			px = p.x;
			py = p.y;
			flare = new com.watabou.noosa.Halo(18, col, 0f);
			add(flare);
			beam = new Image(pillarTex());
			beam.hardlight(col);
			beam.alpha(0f);
			add(beam);
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			if (t > 2.4f){
				killAndErase();
				return;
			}
			Level level = Dungeon.level;
			visible = level != null && cell < level.length() && level.heroFOV[cell];

			float in = Math.min(1f, t / 0.2f);
			in = in * (2f - in);
			float out = t < 0.4f ? 1f : Math.max(0f, 1f - (t - 0.4f) / 2.0f);
			out = out * out * (3f - 2f * out);
			float shimmer = 1f + 0.06f * (float)Math.sin(t * 9f);

			float sx = (0.5f + 0.5f * in) * (0.7f + 0.3f * out) * 1.15f;
			float sy = (1.25f + 0.25f * s) * (0.85f + 0.15f * in);
			beam.scale.set(sx, sy);
			beam.x = px - 8f * sx;
			beam.y = py + 5f - 48f * sy;
			beam.alpha(Math.min(0.7f, 0.5f * s) * in * out * shimmer);

			//bright flash at the foot that settles into the item's own glow
			float f = Math.max(0f, 1f - t / 0.9f);
			flare.radius(10f + 10f * f);
			flare.alpha(0.3f * f * in);
			flare.point(px, py + 1);

			if (visible && t < 1.4f){
				speckTimer -= Game.elapsed;
				while (speckTimer <= 0){
					speckTimer += 0.05f;
					glow.recycleMote(true).rise(px + Random.Float(-4, 4), py + Random.Float(-2, 4),
							0xFF000000 | col, Random.Float(0.7f, 1.2f), Random.Float(16, 28));
				}
			}
		}
	}

	//the hero grew stronger: a warm swell of light, a ring spreading over the floor, specks rising
	public static void levelUp( Hero hero ){
		if (!enabled() || hero == null || hero.sprite == null || floor == null || glow == null) return;
		CharSprite sp = hero.sprite;
		float cx = sp.x + sp.width() / 2f;
		float feet = sp.y + sp.height() - 2f;
		floor.moments.add(new LevelRing(cx, feet, 0f));
		floor.moments.add(new LevelRing(cx, feet, 0.22f));
		glow.lit.add(new LevelSpecks(cx, feet));
		Atmosphere.burst(cx, sp.y + sp.height() / 2f, 6.5f, 0.95f, 0xFFD890, 1.8f);
		PostFX.flash(0xFFE8B0, 0.07f);
	}

	private static class LevelRing extends Image {
		private final float cx, cy;
		private float t;

		LevelRing( float cx, float cy, float delay ){
			super(ringTex());
			this.cx = cx;
			this.cy = cy;
			this.t = -delay;
			hardlight(0xFFE6A0);
			alpha(0f);
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			if (t < 0) return;
			float p = Math.min(1f, t / 1.0f);
			if (p >= 1f){
				killAndErase();
				return;
			}
			float e = 1f - (1f - p) * (1f - p) * (1f - p);
			float sc = 0.12f + 1.2f * e;
			scale.set(sc);
			x = cx - 32f * sc;
			y = cy - 16f * sc;
			float a = (1f - p);
			alpha(0.65f * a * a * Math.min(1f, t / 0.08f));
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	private static class LevelSpecks extends com.watabou.noosa.Gizmo {
		private final float cx, cy;
		private float t = 0, timer = 0;

		LevelSpecks( float cx, float cy ){
			this.cx = cx;
			this.cy = cy;
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			if (t > 0.9f || glow == null){
				killAndErase();
				return;
			}
			timer -= Game.elapsed;
			while (timer <= 0){
				timer += 0.03f;
				float ang = Random.Float(6.2832f);
				float r = Random.Float(3f, 10f);
				int c = Random.Int(3) == 0 ? 0xFFFFF4D0 : (Random.Int(2) == 0 ? 0xFFFFE6A0 : 0xFFFFC860);
				glow.recycleMote(true).rise(cx + (float)Math.cos(ang) * r, cy + (float)Math.sin(ang) * r * 0.5f - 1f,
						c, Random.Float(0.7f, 1.3f), Random.Float(14, 30));
			}
		}
	}

	//random cell near the hero that is currently visible
	private static int randomVisibleCell( Level level, int radius ){
		Hero h = Dungeon.hero;
		if (h == null) return -1;
		int w = level.width();
		int hx = h.pos % w, hy = h.pos / w;
		for (int tries = 0; tries < 6; tries++){
			int x = hx + Random.IntRange(-radius, radius);
			int y = hy + Random.IntRange(-radius, radius);
			if (x < 0 || y < 0 || x >= w || y >= level.height()) continue;
			int c = y * w + x;
			if (level.heroFOV[c]) return c;
		}
		return -1;
	}

	// ================================================================ per-frame state: boss focus, idle time, slow motion

	public static class Watcher extends com.watabou.noosa.Gizmo {
		@Override
		public void update() {
			super.update();

			//slow motion on death, timed in real time
			if (slowmo > 0){
				slowmo -= Math.min(0.2f, Gdx.graphics.getDeltaTime());
				Game.timeScale = slowmo > 0 ? 0.35f : 1f;
			}

			bossVisible = false;
			Level level = Dungeon.level;
			Hero h = Dungeon.hero;
			if (level == null || h == null) return;
			for (Mob m : Atmosphere.mobs(level)){
				if (Char.hasProp(m, Char.Property.BOSS) && m.isAlive() && m.sprite != null && m.sprite.visible
						&& m.pos >= 0 && m.pos < level.length() && level.heroFOV[m.pos]){
					bossVisible = true;
					break;
				}
			}

			if (h.sprite != null && !h.sprite.isMoving && h.curAction == null && h.isAlive()){
				idleTime += Game.elapsed;
			} else {
				idleTime = 0;
			}

		}

		@Override
		public void destroy() {
			super.destroy();
			if (slowmo > 0){
				slowmo = 0;
				Game.timeScale = 1f;
			}
		}
	}

	// ================================================================ floor banner

	public static class Banner extends Component {
		private RenderedTextBlock title, sub;
		private float t = 0;

		public Banner(){
			super();
			camera = PixelScene.uiCamera;
			String reg = regionName();
			title = PixelScene.renderTextBlock(Dungeon.depth + "층", 12);
			title.hardlight(0xFFE8C8);
			add(title);
			sub = PixelScene.renderTextBlock(reg, 8);
			sub.hardlight(0xBFB3A0);
			add(sub);
			setAlpha(0);
			layoutBanner();
		}

		private void layoutBanner(){
			Camera c = PixelScene.uiCamera;
			float top = c.height * 0.18f;
			title.setPos((c.width - title.width()) / 2f, top);
			sub.setPos((c.width - sub.width()) / 2f, title.bottom() + 3);
			PixelScene.align(title);
			PixelScene.align(sub);
		}

		private void setAlpha( float a ){
			title.alpha(a);
			sub.alpha(a);
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			float a;
			if (t < 0.6f) a = 0;
			else if (t < 1.5f) a = (t - 0.6f) / 0.9f;
			else if (t < 3.8f) a = 1f;
			else if (t < 5.0f) a = 1f - (t - 3.8f) / 1.2f;
			else { killAndErase(); return; }
			setAlpha(a * a);
			//slow drift upwards for a cinematic feel
			float top = PixelScene.uiCamera.height * 0.18f - t * 1.2f;
			title.setPos(title.left(), top);
			sub.setPos(sub.left(), title.bottom() + 3);
		}

		private static String regionName(){
			int d = Dungeon.depth;
			if (Dungeon.branch == 1){
				if (d >= 11 && d <= 14) return "버려진 광산";
				if (d >= 16 && d <= 19) return "금고";
			}
			if (d <= 4) return "하수도";
			if (d == 5) return "하수도 · 보스의 소굴";
			if (d <= 9) return "감옥";
			if (d == 10) return "감옥 · 교도소장의 방";
			if (d <= 14) return "동굴";
			if (d == 15) return "동굴 · 거대 기계의 둥지";
			if (d <= 19) return "드워프 도시";
			if (d == 20) return "드워프 도시 · 왕좌";
			if (d <= 24) return "악마의 전당";
			if (d == 25) return "악마의 전당 · 심연의 문";
			return "던전의 끝";
		}
	}

	public static void showBanner( Group scene ){
		if (!enabled() || Dungeon.level == null) return;
		scene.add(new Banner());
	}
}
