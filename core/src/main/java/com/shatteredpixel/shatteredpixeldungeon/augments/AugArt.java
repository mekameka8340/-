/*
 * Pixel art for the augment cards, drawn in code so it matches the game's own window frames:
 * bevelled metal frames with corner gems for each tier, and the augment emblem.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments;

import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;

public class AugArt {

	public static final int FRAME = 24;
	public static final int MARGIN = 6;

	//outline, dark, mid, light, panel top, panel bottom, gem
	private static final int[][] PALETTES = {
			{0xFF1C1E24, 0xFF545A66, 0xFF969EAC, 0xFFE6ECF4, 0xFF22242C, 0xFF16171C, 0xFFFFFFFF},
			{0xFF281806, 0xFF8A5A14, 0xFFD69A2E, 0xFFFFE89A, 0xFF261E12, 0xFF18130C, 0xFFFFF4C0},
			{0xFF1E1E22, 0xFF8A8A90, 0xFFC6C6CC, 0xFFFFFFFF, 0xFF221E2A, 0xFF15121A, 0xFFFFFFFF},
			//legend: smouldering crimson and gold
			{0xFF1E0806, 0xFF7A1C10, 0xFFC84A22, 0xFFFFC870, 0xFF2A1410, 0xFF1A0C0A, 0xFFFFE6A0},
	};

	private static void px( Pixmap p, int x, int y, int argb ){
		//pixmaps take RGBA
		p.drawPixel(x, y, (argb << 8) | ((argb >>> 24) & 0xFF));
	}

	public static NinePatch frame( int tier ){
		String key = "aug_frame_" + tier;
		if (!TextureCache.contains(key)){
			SmartTexture tx = TextureCache.create(key, FRAME, FRAME);
			Pixmap p = tx.bitmap;
			p.setBlending(Pixmap.Blending.None);
			int[] c = PALETTES[tier];
			int n = FRAME - 1;
			for (int y = 0; y <= n; y++){
				for (int x = 0; x <= n; x++){
					int ring = Math.min(Math.min(x, y), Math.min(n - x, n - y));
					int col;
					if (ring == 0){
						col = c[0];
					} else if (ring == 1){
						//light from the top left
						col = (x + y < n) ? c[3] : c[1];
					} else if (ring <= 3){
						col = c[2];
						if (ring == 3 && (x + y >= n)) col = c[1];
					} else if (ring == 4){
						col = c[1];
					} else if (ring == 5){
						col = c[0];
					} else {
						float f = (y - 6) / (float)(n - 12);
						col = f < 0.5f ? c[4] : c[5];
					}
					px(p, x, y, col);
				}
			}
			//corner gems
			int[][] corners = {{1, 1}, {n - 3, 1}, {1, n - 3}, {n - 3, n - 3}};
			for (int[] k : corners){
				for (int dy = 0; dy < 3; dy++){
					for (int dx = 0; dx < 3; dx++){
						boolean edge = (dx == 1 || dy == 1);
						if (!edge) continue;
						px(p, k[0] + dx, k[1] + dy, (dx == 1 && dy == 1) ? c[6] : c[3]);
					}
				}
			}
			//uploaded when first drawn, which is always on the render thread
			tx.filter(com.watabou.glwrap.Texture.NEAREST, com.watabou.glwrap.Texture.NEAREST);
		}
		return new NinePatch(key, 0, 0, FRAME, FRAME, MARGIN);
	}

	//small rounded badge behind the "augment went off" icon, ring in the given colour
	public static Image badge( int color ){
		String key = "aug_badge";
		if (!TextureCache.contains(key)){
			SmartTexture tx = TextureCache.create(key, 20, 20);
			Pixmap p = tx.bitmap;
			p.setBlending(Pixmap.Blending.None);
			p.setColor(0);
			p.fill();
			for (int y = 0; y < 20; y++){
				for (int x = 0; x < 20; x++){
					//rounded square: cut the corners
					int cx = Math.min(x, 19 - x), cy = Math.min(y, 19 - y);
					if (cx + cy < 3) continue;
					boolean ring = cx == 0 || cy == 0 || cx + cy == 3;
					boolean inner = !ring && (cx == 1 || cy == 1 || cx + cy == 4);
					int col;
					if (ring) col = 0xFFFFFFFF;
					else if (inner) col = (y < 10) ? 0xFF55505A : 0xFF2A2630;
					else col = (y < 10) ? 0xF0221E28 : 0xF0181520;
					px(p, x, y, col);
				}
			}
			//uploaded when first drawn, which is always on the render thread
			tx.filter(com.watabou.glwrap.Texture.NEAREST, com.watabou.glwrap.Texture.NEAREST);
		}
		Image i = new Image(key);
		//only the white ring takes the colour, the dark inside stays dark
		tint(i, color);
		return i;
	}

	//the augment emblem: a faceted diamond, white so it can be tinted
	public static Image emblem(){
		String key = "aug_emblem";
		if (!TextureCache.contains(key)){
			SmartTexture tx = TextureCache.create(key, 13, 13);
			Pixmap p = tx.bitmap;
			p.setBlending(Pixmap.Blending.None);
			p.setColor(0);
			p.fill();
			for (int y = 0; y < 13; y++){
				for (int x = 0; x < 13; x++){
					int d = Math.abs(x - 6) + Math.abs(y - 6);
					if (d > 6) continue;
					int col;
					if (d == 6) col = 0xFF3A3A44;
					else if (d >= 4) col = (x <= 6 && y <= 6) ? 0xFFFFFFFF : (x >= 6 && y >= 6 ? 0xFF9A9AA6 : 0xFFD0D0DA);
					else if (d >= 2) col = (y < 6) ? 0xFFE8E8F0 : 0xFFB4B4C0;
					else col = 0xFFFFFFFF;
					px(p, x, y, col);
				}
			}
			//small sparkle facet
			px(p, 5, 4, 0xFFFFFFFF);
			px(p, 4, 5, 0xFFFFFFFF);
			//uploaded when first drawn, which is always on the render thread
			tx.filter(com.watabou.glwrap.Texture.NEAREST, com.watabou.glwrap.Texture.NEAREST);
		}
		return new Image(key);
	}

	//multiplies colours, keeping the shading of the art (hardlight would flatten it)
	public static void tint( com.watabou.noosa.Visual v, int color ){
		v.rm = ((color >> 16) & 0xFF) / 255f;
		v.gm = ((color >> 8) & 0xFF) / 255f;
		v.bm = (color & 0xFF) / 255f;
		v.ra = v.ga = v.ba = 0;
	}

	//hue cycling colour for prism frames
	public static void prismTint( com.watabou.noosa.Visual v, float t, float strength ){
		float h = (t * 0.35f) % 1f;
		float r = clamp(Math.abs(h * 6f - 3f) - 1f);
		float g = clamp(2f - Math.abs(h * 6f - 2f));
		float b = clamp(2f - Math.abs(h * 6f - 4f));
		float k = 1f - strength;
		v.rm = k + strength * (0.55f + 0.45f * r);
		v.gm = k + strength * (0.55f + 0.45f * g);
		v.bm = k + strength * (0.55f + 0.45f * b);
	}

	public static int prismColor( float t ){
		float h = (t * 0.35f) % 1f;
		int r = (int)(255 * (0.55f + 0.45f * clamp(Math.abs(h * 6f - 3f) - 1f)));
		int g = (int)(255 * (0.55f + 0.45f * clamp(2f - Math.abs(h * 6f - 2f))));
		int b = (int)(255 * (0.55f + 0.45f * clamp(2f - Math.abs(h * 6f - 4f))));
		return (r << 16) | (g << 8) | b;
	}

	private static float clamp( float v ){
		return v < 0 ? 0 : (v > 1 ? 1 : v);
	}
}
