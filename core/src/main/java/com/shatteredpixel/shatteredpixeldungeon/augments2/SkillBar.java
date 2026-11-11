/*
 * The four skill buttons of the Augment II challenge: three basic skills and the ultimate.
 * They sit in a row right above the toolbar.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments2;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;

public class SkillBar extends Component {

	public static final int SIZE = Fx2.SLOT;
	private static final int GAP = 1;
	//the ultimate stands a little apart
	private static final int ULT_GAP = 3;

	private Slot[] slots;

	public SkillBar(){
		super();
		width = Tracker2.SLOTS * SIZE + (Tracker2.SLOTS - 2) * GAP + ULT_GAP;
		height = SIZE;
	}

	@Override
	protected void createChildren() {
		slots = new Slot[Tracker2.SLOTS];
		for (int i = 0; i < slots.length; i++){
			add(slots[i] = new Slot(i));
		}
	}

	@Override
	protected void layout() {
		float px = x;
		for (int i = 0; i < slots.length; i++){
			if (i == Tracker2.ULT) px += ULT_GAP - GAP;
			slots[i].setRect(px, y, SIZE, SIZE);
			px += SIZE + GAP;
		}
	}

	@Override
	public void update() {
		//follows the toolbar, which is hidden during the opening and the tutorial
		boolean show = GameScene.toolbarShown();
		if (show != visible){
			visible = active = show;
		}
		super.update();
	}

	private static class Slot extends Button {

		private final int index;
		private Image frame;
		private Image icon;
		private ColorBlock shade;
		private BitmapText count;
		private ColorBlock[] pips;

		private String shown = null;
		private boolean wasReady = true;
		private float flash = 0;
		private boolean pressed = false;

		Slot( int index ){
			super();
			this.index = index;
			//built here rather than in createChildren, which runs before the index is known
			frame = Fx2.slotFrame(index == Tracker2.ULT);
			add(frame);

			shade = new ColorBlock(1, 1, 0x8C000000);
			add(shade);

			count = new BitmapText(PixelScene.pixelFont);
			add(count);

			pips = new ColorBlock[Tracker2.MAX_STEP];
			for (int i = 0; i < pips.length; i++){
				add(pips[i] = new ColorBlock(2, 2, 0xFFFFD45A));
			}
		}

		@Override
		protected void layout() {
			super.layout();
			frame.x = x;
			frame.y = y;
			if (icon != null){
				icon.x = x + (width - icon.width()) / 2f;
				icon.y = y + (height - icon.height()) / 2f;
				PixelScene.align(icon);
			}
			for (int i = 0; i < pips.length; i++){
				pips[i].x = x + 3 + i * 3;
				pips[i].y = y + height - 5;
			}
		}

		@Override
		public void update() {
			super.update();
			Tracker2 t = Aug2.tracker();
			Skill s = t == null ? null : t.skill(index);
			String id = s == null ? null : s.id;

			if (id == null ? shown != null : !id.equals(shown)){
				shown = id;
				if (icon != null){
					icon.killAndErase();
					icon = null;
				}
				if (s != null){
					icon = Fx2.icon(s.icon);
					addToBack(icon);
					sendToBack(frame);
					layout();
				}
				wasReady = true;
			}

			if (s == null){
				frame.alpha(0.45f);
				shade.visible = count.visible = false;
				for (ColorBlock p : pips) p.visible = false;
				return;
			}
			frame.alpha(1f);

			float cd = t.cd[index];
			float max = Math.max(1, t.cooldown(index));
			boolean ready = cd <= 0;
			boolean usable = ready && Dungeon.hero.isAlive();

			if (ready && !wasReady) flash = 1f;
			wasReady = ready;

			//the inside of the socket darkens from the top while the skill recovers
			float inner = SIZE - 6;
			if (!ready){
				float k = Math.min(1f, cd / max);
				shade.visible = true;
				shade.size(inner, Math.max(1, Math.round(inner * k)));
				shade.x = x + 3;
				shade.y = y + 3;
				count.visible = true;
				String num = Integer.toString((int)Math.ceil(cd));
				if (!num.equals(count.text())){
					count.text(num);
					count.measure();
				}
				count.x = x + (width - count.width()) / 2f;
				count.y = y + (height - count.baseLine()) / 2f - 1;
				PixelScene.align(count);
			} else {
				shade.visible = count.visible = false;
			}

			for (int i = 0; i < pips.length; i++) pips[i].visible = i < t.step[index];

			boolean aiming = Aug2.aimingSlot() == index;
			float glow = 0;
			if (flash > 0){
				flash -= Game.elapsed * 2f;
				glow = Math.max(0, flash);
			}
			if (aiming) glow = Math.max(glow, 0.35f + 0.2f * (float)Math.sin(Game.timeTotal * 8f));
			if (pressed) glow = Math.max(glow, 0.3f);
			frame.resetColor();
			frame.ra = frame.ga = frame.ba = glow * 0.6f;
			if (icon != null){
				icon.resetColor();
				if (!usable) icon.rm = icon.gm = icon.bm = 0.75f;
				icon.ra = icon.ga = icon.ba = glow * 0.5f;
			}
		}

		@Override
		protected void onPointerDown() {
			pressed = true;
		}

		@Override
		protected void onPointerUp() {
			pressed = false;
		}

		@Override
		protected void onClick() {
			Aug2.press(index);
		}

		@Override
		protected boolean onLongClick() {
			Tracker2 t = Aug2.tracker();
			if (t == null || t.skill(index) == null) return false;
			GameScene.show(new WndPick2.Info(t, index));
			return true;
		}

		@Override
		protected String hoverText() {
			Tracker2 t = Aug2.tracker();
			Skill s = t == null ? null : t.skill(index);
			return s == null ? null : s.name;
		}
	}
}
