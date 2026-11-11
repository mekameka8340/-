/*
 * The augment tab of the hero window: set progress at the top, then every augment taken.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;
import com.watabou.noosa.audio.Sample;
import com.shatteredpixel.shatteredpixeldungeon.Assets;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class AugTab extends Component {

	private ScrollPane pane;
	private final ArrayList<Row> rows = new ArrayList<>();

	@Override
	protected void createChildren() {
		super.createChildren();
		pane = new ScrollPane(new Component()){
			@Override
			public void onClick( float x, float y ) {
				for (Row r : rows){
					if (r.inside(x, y)){
						r.onClick();
						break;
					}
				}
			}
		};
		add(pane);
	}

	@Override
	protected void layout() {
		super.layout();
		pane.setRect(x, y, width, height);
		fill();
	}

	private void fill(){
		Component content = pane.content();
		content.clear();
		rows.clear();

		AugTracker t = Augs.tracker();
		float pos = 0;
		if (t == null){
			pane.content().setSize(width, 0);
			return;
		}

		RenderedTextBlock head = PixelScene.renderTextBlock("증강 " + t.owned.size() + "개", 9);
		head.hardlight(Window.TITLE_COLOR);
		head.setPos(0, pos);
		content.add(head);

		RedButton totals = new RedButton("효과 합계", 7){
			@Override
			protected void onClick() {
				GameScene.show(new WndTotals());
			}
		};
		totals.setRect(width - 46, pos - 1, 46, 13);
		content.add(totals);
		pos += 15;

		//the legend being aimed for, and the run so far
		RedButton goalBtn = new RedButton(goalText(t), 7){
			@Override
			protected void onClick() {
				GameScene.show(new WndGoal());
			}
		};
		goalBtn.setRect(0, pos, width - 48, 13);
		content.add(goalBtn);
		RedButton rec = new RedButton("기록", 7){
			@Override
			protected void onClick() {
				GameScene.show(new AugMoments.WndHighlights());
			}
		};
		rec.setRect(width - 46, pos, 46, 13);
		content.add(rec);
		pos += 16;

		//family progress
		boolean anySet = false;
		for (AugSet set : AugSet.values()){
			int n = t.count(set);
			if (n <= 0) continue;
			anySet = true;
			Row r = new SetRow(set, n);
			r.setRect(0, pos, width, 11);
			content.add(r);
			rows.add(r);
			pos += 12;
		}
		if (anySet) pos += 3;

		ColorBlock line = new ColorBlock(width, 1, 0xFF3A3A3A);
		line.y = pos;
		content.add(line);
		pos += 4;

		if (t.owned.isEmpty()){
			RenderedTextBlock none = PixelScene.renderTextBlock("아직 증강이 없습니다.", 7);
			none.hardlight(0x999999);
			none.setPos(0, pos);
			content.add(none);
			pos += 12;
		}

		//augments, repeated basic ones grouped with a count
		LinkedHashMap<String, Integer> counted = new LinkedHashMap<>();
		for (String s : t.owned) counted.put(s, counted.containsKey(s) ? counted.get(s) + 1 : 1);
		//the devoured one stays on the list, it still works at twice the strength
		if (t.devoured != null && !counted.containsKey(t.devoured)) counted.put(t.devoured, 1);
		for (Map.Entry<String, Integer> e : counted.entrySet()){
			Aug a = Aug.get(e.getKey());
			if (a == null) continue;
			Row r = new AugRow(a, e.getValue(), a.repeatable ? 1 : (a.name.equals(t.devoured) ? t.devouredStars : t.stars(a.name)));
			r.setRect(0, pos, width, 16);
			content.add(r);
			rows.add(r);
			pos += 17;
		}

		content.setSize(width, pos);
		pane.scrollTo(0, 0);
	}

	private abstract static class Row extends Component {
		abstract void onClick();
	}

	private static class SetRow extends Row {
		final AugSet set;
		RenderedTextBlock label, next;
		final ColorBlock[] pips = new ColorBlock[6];

		SetRow( AugSet set, int n ){
			this.set = set;
			label = PixelScene.renderTextBlock(set.title + "  " + n, 7);
			label.hardlight(set.color);
			add(label);
			//one pip per threshold: outer square is the frame, inner one lights up when reached
			for (int i = 0; i < 3; i++){
				boolean on = n >= AugSet.THRESHOLDS[i];
				pips[i*2] = new ColorBlock(5, 5, on ? 0xFF000000 | set.color : 0xFF555555);
				pips[i*2+1] = new ColorBlock(3, 3, on ? 0xFFFFFFFF : 0xFF222222);
				if (on) pips[i*2+1].alpha(0.55f);
				add(pips[i*2]);
				add(pips[i*2+1]);
			}
			int nx = -1;
			for (int th : AugSet.THRESHOLDS) if (n < th){ nx = th; break; }
			next = PixelScene.renderTextBlock(nx > 0 ? "다음 " + nx + "개" : "완성", 6);
			next.hardlight(nx > 0 ? 0x999999 : set.color);
			add(next);
		}

		@Override
		protected void layout() {
			label.setPos(x + 2, y + (height - label.height()) / 2f);
			PixelScene.align(label);
			float px = x + width * 0.42f;
			float py = y + (height - 5) / 2f;
			for (int i = 0; i < 3; i++){
				pips[i*2].x = px + i * 8;
				pips[i*2].y = py;
				pips[i*2+1].x = px + i * 8 + 1;
				pips[i*2+1].y = py + 1;
				PixelScene.align(pips[i*2]);
				PixelScene.align(pips[i*2+1]);
			}
			next.setPos(px + 28, y + (height - next.height()) / 2f);
			PixelScene.align(next);
		}

		@Override
		void onClick() {
			GameScene.show(new WndSet(set));
		}
	}

	private static class AugRow extends Row {
		final Aug aug;
		Image icon;
		RenderedTextBlock label, fam;

		AugRow( Aug a, int count, int stars ){
			aug = a;
			icon = new ItemSprite(a.icon);
			add(icon);
			label = PixelScene.renderTextBlock(a.name.replace(" ", "  ") + (count > 1 ? "  x" + count : "")
					+ (stars > 1 ? "  _" + Aug.stars(stars) + "_" : "")
					+ (Augs.tracker() != null && a.name.equals(Augs.tracker().devoured) ? "  _흡수 2배_" : ""), 7);
			label.hardlight(a.tier == Aug.PRISM ? 0xF0D0FF : Aug.TIER_COLORS[a.tier]);
			add(label);
			fam = PixelScene.renderTextBlock(a.set == null ? "기본" : a.set.title, 6);
			fam.hardlight(a.set == null ? 0x999999 : a.set.color);
			add(fam);
		}

		@Override
		protected void layout() {
			icon.x = x;
			icon.y = y + (height - icon.height()) / 2f;
			label.setPos(x + 19, y + (height - label.height()) / 2f);
			fam.setPos(x + width - fam.width() - 1, y + (height - fam.height()) / 2f);
			PixelScene.align(icon);
			PixelScene.align(label);
			PixelScene.align(fam);
		}

		@Override
		void onClick() {
			GameScene.show(new WndInfo(aug));
		}
	}

	// ================================================================ windows

	//details of a single augment
	public static class WndInfo extends Window {
		public WndInfo( Aug a ){
			int w = 120;
			Image icon = new ItemSprite(a.icon);
			add(icon);
			RenderedTextBlock title = PixelScene.renderTextBlock(a.name.replace(" ", "  "), 9);
			title.hardlight(a.tier == Aug.PRISM ? 0xF0D0FF : Aug.TIER_COLORS[a.tier]);
			title.maxWidth(w - 20);
			title.setPos(19, 0);
			add(title);
			String info = Aug.TIER_NAMES[a.tier] + (a.gamble ? " · 도박" : "") + (a.jackpot() ? " · 잭팟" : "")
					+ (AugGrow.grows(a) ? " · 성장" : "") + " · " + (a.set == null ? "기본 수련" : a.set.title);
			if (a.requires != null) info += "\n조합: " + a.requiresText();
			RenderedTextBlock tier = PixelScene.renderTextBlock(info, 6);
			tier.maxWidth(w - 20);
			tier.hardlight(a.set == null ? 0x999999 : a.set.color);
			tier.setPos(19, title.bottom() + 2);
			add(tier);
			icon.y = Math.max(0, (tier.bottom() - icon.height()) / 2f);
			AugTracker t = Augs.tracker();
			String body = a.desc();
			if (t != null && t.owned.contains(a.name) && !a.repeatable){
				int st = t.stars(a.name);
				if (st > 1) body = "_" + Aug.stars(st) + "_: 모든 효과 " + AugTracker.STAR_POWER[st] + "배\n" + body;
			}
			if (t != null && AugGrow.grows(a) && t.growBase.containsKey(a.name)){
				AugGrow.Rule r = AugGrow.rule(a.name);
				int st = AugGrow.steps(t, r);
				body += "\n\n_지금까지 성장:_ " + (st == 0 ? "아직 없음" : AugGrow.describe(r, st));
			}
			if (AugQuest.isQuest(a)) body += "\n\n" + AugQuest.overview(t, a.name);
			if (t != null && a.stats().containsKey(St.U_SOULSTEAL) && t.soulKey != null && t.soulFloor == AugTracker.floorKey()){
				body += "\n\n_지금 빌린 영혼:_ " + t.soulName + " (" + AugY.soulTitle(t.soulKey) + ")";
			}
			if (t != null && a.name.equals(t.devoured)) body = "_삼켜서 2배로 흡수한 증강_\n" + body;
			RenderedTextBlock desc = PixelScene.renderTextBlock(body, 7);
			desc.maxWidth(w);
			desc.setPos(0, Math.max(icon.y + icon.height(), tier.bottom()) + 5);
			add(desc);
			resize(w, (int)desc.bottom() + 1);
		}
	}

	//bonuses of one family
	public static class WndSet extends Window {
		public WndSet( AugSet set ){
			int w = 120;
			AugTracker t = Augs.tracker();
			int n = t == null ? 0 : t.count(set);
			RenderedTextBlock title = PixelScene.renderTextBlock(set.title + " 세트  (" + n + ")", 9);
			title.hardlight(set.color);
			title.setPos(0, 0);
			add(title);
			float pos = title.bottom() + 5;
			for (int i = 0; i < 3; i++){
				boolean on = n >= AugSet.THRESHOLDS[i];
				StringBuilder sb = new StringBuilder();
				sb.append(AugSet.THRESHOLDS[i]).append("개: ");
				boolean first = true;
				for (Map.Entry<St, Float> e : set.bonus(i).entrySet()){
					if (!first) sb.append(", ");
					sb.append(e.getKey().describe(e.getValue()));
					first = false;
				}
				RenderedTextBlock line = PixelScene.renderTextBlock(sb.toString(), 7);
				line.maxWidth(w);
				line.hardlight(on ? set.color : 0x808080);
				line.setPos(0, pos);
				add(line);
				pos = line.bottom() + 4;
			}
			resize(w, (int)pos);
		}
	}

	//everything added together
	public static class WndTotals extends Window {
		public WndTotals(){
			int w = 124;
			RenderedTextBlock title = PixelScene.renderTextBlock("증강 효과 합계", 9);
			title.hardlight(TITLE_COLOR);
			title.setPos(0, 0);
			add(title);
			StringBuilder sb = new StringBuilder();
			AugTracker t = Augs.tracker();
			if (t != null){
				for (St s : St.values()){
					if (s.once()) continue;
					float v = t.total[s.ordinal()];
					if (v == 0) continue;
					if (sb.length() > 0) sb.append('\n');
					sb.append("- ").append(s.describe(v));
				}
			}
			if (sb.length() == 0) sb.append("아직 효과가 없습니다.");
			RenderedTextBlock body = PixelScene.renderTextBlock(sb.toString(), 7);
			body.maxWidth(w - 4);
			float top = title.bottom() + 4;
			float maxH = Math.min(body.height(), PixelScene.uiCamera.height * 0.65f - top);

			//the scroll pane's camera follows the window, so it is placed after the window has its size
			resize(w, (int)Math.ceil(top + maxH));

			ScrollPane sp = new ScrollPane(new Component());
			add(sp);
			sp.content().add(body);
			body.setPos(0, 0);
			sp.content().setSize(w, body.height());
			sp.setRect(0, top, w, maxH);
		}
	}

	static String goalText( AugTracker t ){
		if (t == null || t.goal == null) return "목표 전설 정하기";
		Aug g = Aug.get(t.goal);
		if (g == null) return "목표 전설 정하기";
		int have = 0;
		for (String r : g.requires) if (t.owned.contains(r)) have++;
		return "목표: " + g.name + " (" + have + "/" + g.requires.length + ")";
	}

	//choose the legend to aim for: its materials turn up three times as often, and once unlocked it is offered for sure
	public static class WndGoal extends Window {
		private final ArrayList<Component> rows = new ArrayList<>();

		public WndGoal(){
			final int w = Math.min(150, (int) PixelScene.uiCamera.width - 16);
			final int h = Math.min(200, (int) (PixelScene.uiCamera.height * 0.75f));
			final AugTracker t = Augs.tracker();
			RenderedTextBlock title = PixelScene.renderTextBlock("목표 전설", 9);
			title.hardlight(TITLE_COLOR);
			title.setPos((w - title.width()) / 2f, 0);
			add(title);
			RenderedTextBlock help = PixelScene.renderTextBlock("고른 전설의 재료가 3배 자주 나오고, 재료를 다 모으면 다음 선택에 반드시 등장합니다.", 6);
			help.maxWidth(w);
			help.hardlight(0xB0B0B0);
			help.setPos(0, title.bottom() + 3);
			add(help);
			float top = help.bottom() + 4;
			resize(w, h);

			ScrollPane sp = new ScrollPane(new Component()){
				@Override
				public void onClick( float x, float y ) {
					for (Component c : rows){
						if (c.inside(x, y)){
							String name = (String) ((GoalRow) c).aug.name;
							if (t != null) t.goal = name.equals(t.goal) ? null : name;
							Sample.INSTANCE.play(Assets.Sounds.CLICK);
							hide();
							GameScene.show(new WndGoal());
							return;
						}
					}
				}
			};
			add(sp);
			float pos = 0;
			Component content = sp.content();
			for (Aug a : Aug.all()){
				if (a.tier != Aug.LEGEND || a.requires == null) continue;
				GoalRow r = new GoalRow(a, t, w);
				r.setRect(0, pos, w, r.height());
				content.add(r);
				rows.add(r);
				pos += r.height() + 3;
			}
			content.setSize(w, pos);
			sp.setRect(0, top, w, h - top);
		}
	}

	private static class GoalRow extends Component {
		static final int OWNED = 0x6FDC6F;
		final Aug aug;
		ColorBlock bg;
		RenderedTextBlock name, mats;

		GoalRow( Aug a, AugTracker t, int w ){
			aug = a;
			boolean owned = t != null && t.owned.contains(a.name);
			boolean goal = t != null && a.name.equals(t.goal);
			//owned legends stand out in green, so picking the next goal is a glance
			bg = new ColorBlock(1, 1, goal ? 0x66FF8A40 : owned ? 0x3348C048 : 0x22FFFFFF);
			add(bg);
			name = PixelScene.renderTextBlock((goal ? "목표  " : "") + a.name + (owned ? "  (보유)" : ""), 7);
			name.hardlight(owned ? OWNED : 0xFF9A60);
			name.maxWidth(w - 4);
			add(name);
			StringBuilder sb = new StringBuilder();
			for (String r : a.requires){
				if (sb.length() > 0) sb.append("  +  ");
				boolean have = t != null && t.owned.contains(r);
				sb.append(have ? "_" + r + "_" : r);
			}
			mats = PixelScene.renderTextBlock(sb.toString(), 6);
			mats.maxWidth(w - 4);
			mats.hardlight(0xC8C8C8);
			//materials already held are drawn in the same green
			mats.setHightlighting(true, OWNED);
			add(mats);
			height = name.height() + mats.height() + 6;
		}

		@Override
		protected void layout() {
			bg.x = x; bg.y = y;
			bg.size(width, height);
			name.setPos(x + 2, y + 2);
			mats.setPos(x + 2, name.bottom() + 2);
			PixelScene.align(name);
			PixelScene.align(mats);
		}
	}

	//tab icon
	public static Image tabIcon(){
		Image i = AugArt.emblem();
		AugArt.tint(i, 0xFFD45A);
		return i;
	}

	static boolean visible(){
		return Dungeon.hero != null && Augs.tracker() != null;
	}
}
