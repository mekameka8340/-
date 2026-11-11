/*
 * The augment choice: three cards dealt face down that flip over one after another.
 * Tap a card to select it, then confirm. Every card can be rerolled once.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.RainbowParticle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Halo;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;

public class WndAugment extends Window {

	private static final int GAP = 3;

	private final AugTracker tracker;
	private final Card[] cards;
	private RedButton confirm;
	private RedButton owned, goalBtn;
	private RenderedTextBlock title, sub;
	private int selected = -1;
	private float time = 0;
	private final int width;
	private final boolean wide;

	//the boss reward roulette: card backs flicker between gold and prism before settling
	private float roulette = 0;
	private boolean rouletteAlt = false;
	private float rouletteTick = 0;
	//when the last card has turned over and choices can be made
	private float dealEnd = 0;
	//taking a card is delayed while the mystery card shows what it was
	private int taking = -1;
	private float takeAt = -1;
	//the goal can be changed from this window: the cards then show which ones are its materials
	private String shownGoal;

	static WndAugment current;

	public WndAugment( AugTracker tracker ){
		super();
		this.tracker = tracker;
		current = this;
		//three cards side by side only when the screen is really wide
		wide = Game.width > Game.height && PixelScene.uiCamera.width >= 280;
		width = wide ? Math.min(330, (int)PixelScene.uiCamera.width - 16) : Math.min(170, (int)PixelScene.uiCamera.width - 12);

		title = PixelScene.renderTextBlock("증강 선택", 10);
		title.hardlight(TITLE_COLOR);
		add(title);

		sub = PixelScene.renderTextBlock("", 7);
		add(sub);

		cards = new Card[tracker.offer.length];
		for (int i = 0; i < cards.length; i++){
			cards[i] = new Card(i);
			add(cards[i]);
		}

		confirm = new RedButton("획득"){
			@Override
			protected void onClick() {
				if (selected >= 0) take(selected);
			}
		};
		confirm.enable(false);
		add(confirm);

		//what the hero already has, to plan which families to build
		owned = new RedButton("내 증강 " + tracker.owned.size(), 7){
			@Override
			protected void onClick() {
				GameScene.show(new WndOwned());
			}
		};
		owned.enable(!tracker.owned.isEmpty());
		add(owned);

		goalBtn = new RedButton("목표", 7){
			@Override
			protected void onClick() {
				GameScene.show(new AugTab.WndGoal());
			}
		};
		if (tracker.goal != null) goalBtn.textColor(0xFF9A60);
		add(goalBtn);

		if (tracker.lucky == 3) roulette = 1.5f;
		shownGoal = tracker.goal;
		build(true);
		schedule();

		Sample.INSTANCE.play(Assets.Sounds.READ, 0.6f);
		if (tracker.lucky == 1 || tracker.lucky == 2 || tracker.lucky == 5){
			//a lucky or premium round gets its own little fanfare
			Sample.INSTANCE.play(Assets.Sounds.SECRET, 0.7f, 1.2f);
			new Flare(6, 26).color(0xFFD860, true).show(this, new com.watabou.utils.PointF(width / 2f, sub.top() + sub.height() / 2f), 1.4f);
		} else if (tracker.lucky == 4){
			//the golden round: out of nowhere, everything shines
			AugMoments.seq(Assets.Sounds.SECRET, 0.8f, 1f, 0f, Assets.Sounds.AUG_PRISM, 0.9f, 1f, 0.25f);
			new Flare(10, 46).color(0xFFE070, true).show(this, new com.watabou.utils.PointF(width / 2f, sub.top() + sub.height() / 2f), 2.2f);
		}
	}

	//when each card turns over: lowest tier first, the best one last, with a pause before a surprise
	private void schedule(){
		int n = cards.length;
		Integer[] order = new Integer[n];
		for (int i = 0; i < n; i++) order[i] = i;
		java.util.Arrays.sort(order, (a, b) -> {
			int ta = tracker.cardTier[a], tb = tracker.cardTier[b];
			return ta != tb ? Integer.compare(ta, tb) : Integer.compare(a, b);
		});
		float t = 0.3f + roulette;
		for (int k = 0; k < n; k++){
			Card c = cards[order[k]];
			if (c.mystery){
				c.delay = -1;
				continue;
			}
			if (c.surprise()) t += 0.75f;
			c.delay = t;
			t += 0.42f;
		}
		dealEnd = t;
	}

	//lays everything out. deal: whether the cards start face down
	private void build( boolean deal ){
		sub.text(subText());
		sub.hardlight(subColor());

		title.setPos((width - title.width()) / 2f, 2);
		sub.setPos((width - sub.width()) / 2f, title.bottom() + 2);
		float top = sub.bottom() + 5;

		int n = cards.length;
		for (int i = 0; i < n; i++){
			Aug a = Aug.get(tracker.offer[i]);
			cards[i].set(a, deal);
		}

		float bottom;
		if (wide){
			float cw = (width - GAP * (n - 1)) / (float)n;
			float h = 0;
			for (Card c : cards) h = Math.max(h, c.measure(cw));
			for (int i = 0; i < n; i++){
				cards[i].setRect(i * (cw + GAP), top, cw, h);
			}
			bottom = top + h;
		} else {
			float y = top;
			for (Card c : cards){
				float h = c.measure(width);
				c.setRect(0, y, width, h);
				y += h + GAP;
			}
			bottom = y - GAP;
		}

		float bw = (width - GAP * 2) / 3f;
		owned.setRect(0, bottom + 5, bw, 18);
		goalBtn.setRect(bw + GAP, bottom + 5, bw, 18);
		confirm.setRect((bw + GAP) * 2, bottom + 5, bw, 18);
		resize(width, (int)confirm.bottom() + 1);
	}

	private String subText(){
		if (roulette > 0) return "보스 처치 보상   ·   " + (rouletteAlt ? "프리즘?" : "골드?");
		int tier = tracker.offerTier;
		String tierText = Aug.TIER_NAMES[tier] + " 등급";
		if (tracker.lucky == 1) tierText += "   ·   행운! 카드 4장";
		else if (tracker.lucky == 2) tierText += "   ·   행운! 등급 상승";
		else if (tracker.lucky == 3) tierText = "보스 처치 보상   ·   " + tierText;
		else if (tracker.lucky == 4) tierText = "황금 라운드!   ·   전부 프리즘";
		else if (tracker.lucky == 5) tierText = "상급 라운드   ·   " + tierText + " · 카드 4장";
		if (tracker.pending > 1) tierText += "   ·   남은 선택 " + tracker.pending;
		return tierText;
	}

	private int subColor(){
		if (roulette > 0) return rouletteAlt ? AugArt.prismColor(time) : Aug.TIER_COLORS[Aug.GOLD];
		if (tracker.lucky == 4) return 0xFFE070;
		int tier = tracker.offerTier;
		return tier == Aug.PRISM ? AugArt.prismColor(time) : Aug.TIER_COLORS[tier];
	}

	boolean dealt(){
		return time >= dealEnd + 0.35f;
	}

	void select( int i ){
		if (taking >= 0) return;
		selected = i;
		confirm.enable(true);
		for (int k = 0; k < cards.length; k++) cards[k].selected(k == i);
		Sample.INSTANCE.play(Assets.Sounds.CLICK, 0.8f, 1.1f);
	}

	private void reroll( int i ){
		if (tracker.rerolled[i] || taking >= 0) return;
		Augs.reroll(tracker, i);
		if (selected == i){
			selected = -1;
			confirm.enable(false);
			cards[i].selected(false);
		}
		build(false);
		cards[i].flipFrom(0f);
		Sample.INSTANCE.play(Assets.Sounds.READ, 0.6f, 1.2f);
	}

	void take( int i ){
		if (taking >= 0) return;
		int m = tracker.mystery;
		if (m >= 0 && m < cards.length && cards[m].mystery){
			//the mystery card shows itself first, chosen or not
			taking = i;
			confirm.enable(false);
			owned.enable(false);
			goalBtn.enable(false);
			boolean chosen = i == m;
			cards[m].reveal(chosen);
			takeAt = time + (chosen ? 0.9f : 0.35f) + 0.4f + (chosen ? 1.1f : 1.5f);
			Aug a = Aug.get(tracker.offer[m]);
			if (chosen) GLog.p("미지의 카드: " + a.name + " (" + Aug.TIER_NAMES[a.tier] + ")!");
			else GLog.i("미지의 카드는 " + a.name + " (" + Aug.TIER_NAMES[a.tier] + ")" + (a.tier >= Aug.PRISM ? "였습니다... 아깝다!" : "였습니다."));
			return;
		}
		finishTake(i);
	}

	private void finishTake( int i ){
		Aug a = Aug.get(tracker.offer[i]);
		hide();
		Augs.choose(tracker, a);
	}

	@Override
	public void update() {
		super.update();
		time += Game.elapsed;

		if (roulette > 0){
			//flicks between gold and prism, slowing down, then settles
			float left = roulette - time;
			if (left <= 0){
				roulette = 0;
				AugMoments.reveal(tracker.offerTier);
				new Flare(7, 30).color(tracker.offerTier == Aug.PRISM ? 0xF0D0FF : 0xFFD060, true)
						.show(this, new com.watabou.utils.PointF(width / 2f, sub.top() + sub.height() / 2f), 1.4f);
				sub.text(subText());
				sub.setPos((width - sub.width()) / 2f, sub.top());
			} else {
				rouletteTick -= Game.elapsed;
				if (rouletteTick <= 0){
					float k = 1f - left / 1.5f;
					rouletteTick = 0.05f + 0.28f * k * k;
					rouletteAlt = !rouletteAlt;
					Sample.INSTANCE.play(Assets.Sounds.AUG_TICK, 0.6f, rouletteAlt ? 1.15f : 1f);
					sub.text(subText());
					sub.setPos((width - sub.width()) / 2f, sub.top());
				}
			}
		}
		sub.hardlight(subColor());

		if (!java.util.Objects.equals(shownGoal, tracker.goal) && taking < 0){
			shownGoal = tracker.goal;
			goalBtn.textColor(tracker.goal != null ? 0xFF9A60 : 0xFFFFFF);
			build(false);
		}

		if (taking >= 0 && time >= takeAt){
			int i = taking;
			taking = -1;
			finishTake(i);
		}
	}

	@Override
	public void onBackPressed() {
		//a choice must be made
	}

	@Override
	public void destroy() {
		super.destroy();
		if (current == this) current = null;
		Augs.pickWindowClosed();
	}

	// ================================================================ owned augments, viewable while choosing

	public static class WndOwned extends Window {
		public WndOwned(){
			int w = Math.min(140, (int)PixelScene.uiCamera.width - 16);
			int h = Math.min(190, (int)(PixelScene.uiCamera.height * 0.7f));
			resize(w, h);
			AugTab tab = new AugTab();
			add(tab);
			tab.setRect(0, 0, w, h);
		}
	}

	// ================================================================ twinkling specks on rare cards

	public static class Twinkle extends com.watabou.noosa.particles.PixelParticle {

		private static float clock = 0;

		//color -1 cycles through the rainbow
		public static Emitter.Factory factory( final int color ){
			return new Emitter.Factory() {
				@Override
				public void emit( Emitter emitter, int index, float x, float y ) {
					int c;
					if (color == -1)      c = AugArt.prismColor(clock += 0.37f);
					else if (color == -2) c = com.watabou.utils.Random.oneOf(0xFF7A30, 0xFFB050, 0xFFE090, 0xFF5030);
					else                  c = color;
					((Twinkle) emitter.recycle(Twinkle.class)).reset(x, y, c);
				}

				@Override
				public boolean lightMode() {
					return true;
				}
			};
		}

		public void reset( float x, float y, int color ){
			super.reset(x, y, 0xFF000000 | color, 1, com.watabou.utils.Random.Float(0.7f, 1.2f));
			speed.set(0, -com.watabou.utils.Random.Float(2f, 6f));
		}

		@Override
		public void update() {
			super.update();
			float t = 1f - left / lifespan;
			size(t > 0.35f && t < 0.6f ? 2f : 1f);
			alpha((float) Math.sin(t * Math.PI));
		}
	}

	// ================================================================ one card

	private class Card extends Button {

		private final int index;
		private Aug aug;
		private float flip = 1f;   //<0.5 shows the back, reaches 1 when fully turned
		private boolean announced = false;
		//seconds after the window opened when this card turns over; -1 waits (the mystery card)
		float delay = 0;
		private boolean waiting = false;
		boolean mystery = false;
		private boolean teaseSounded = false;
		private float revealAt = -1;
		private boolean missed = false;

		private NinePatch back, backAlt, front;
		private Image emblem;
		private RenderedTextBlock mark, markLabel, missedText;
		private ItemSprite icon;
		private RenderedTextBlock name, family, desc;
		private com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton rerollBtn;
		private ColorBlock glint;
		private ColorBlock[] ring;
		private Emitter sparkles;
		private Halo tease;
		private boolean isSelected = false;
		private boolean upgraded = false;
		private float t = 0;
		private float baseX;

		Card( int index ){
			super();
			this.index = index;
		}

		@Override
		protected void createChildren() {
			super.createChildren();
			ring = new ColorBlock[4];
			for (int i = 0; i < 4; i++){
				ring[i] = new ColorBlock(1, 1, 0xFFFFFFFF);
				ring[i].visible = false;
				add(ring[i]);
			}
		}

		//whether this card holds more than its face down back promises
		boolean surprise(){
			int real = tracker.cardTier[index];
			return real > tracker.offerTier || real == Aug.LEGEND;
		}

		void set( Aug a, boolean deal ){
			aug = a;
			int tier = a.tier;
			mystery = index == tracker.mystery && flip < 1f || (deal && index == tracker.mystery);
			drop(back); drop(backAlt); drop(front); drop(emblem); drop(icon);
			drop(name); drop(family); drop(desc); drop(rerollBtn); drop(glint);
			drop(mark); drop(markLabel); drop(tease); drop(missedText);
			if (sparkles != null) sparkles.on = false;
			drop(sparkles);

			//a soft light that seeps out from behind a card about to surprise
			tease = new Halo(Math.max(20, (int)(width * 0.5f)), 0xFFFFFF, 1f){
				@Override
				public void draw() {
					com.watabou.glwrap.Blending.setLightMode();
					super.draw();
					com.watabou.glwrap.Blending.setNormalMode();
				}
			};
			tease.alpha(0);
			add(tease);

			//the back always shows the round's tier, so a lucky or legendary card is a surprise
			int backTier = mystery ? Aug.LEGEND : tracker.offerTier;
			back = AugArt.frame(backTier);
			add(back);
			if (tracker.lucky == 3 && roulette > 0){
				backAlt = AugArt.frame(tracker.offerTier == Aug.PRISM ? Aug.GOLD : Aug.PRISM);
				add(backAlt);
			}
			emblem = AugArt.emblem();
			AugArt.tint(emblem, backTier == Aug.PRISM ? 0xFFFFFF : Aug.TIER_COLORS[Math.min(backTier, Aug.TIER_COLORS.length - 1)]);
			add(emblem);
			if (mystery){
				emblem.visible = false;
				mark = PixelScene.renderTextBlock("?", 16);
				add(mark);
				markLabel = PixelScene.renderTextBlock("미지의 카드  ·  골라야 정체가 드러납니다", 6);
				markLabel.hardlight(0xE0C8FF);
				add(markLabel);
			}

			front = AugArt.frame(tier);
			add(front);

			icon = new ItemSprite(a.icon);
			add(icon);

			boolean star = tracker.canStar(a);
			String nm = a.name.replace(" ", "  ");
			if (star) nm += "  " + Aug.stars(tracker.stars(a.name) + 1);
			name = PixelScene.renderTextBlock(nm, 8);
			name.hardlight(tier == Aug.PRISM ? 0xF4E0FF : Aug.TIER_COLORS[tier]);
			upgraded = tier > tracker.offerTier;
			add(name);

			String fam;
			if (a.set == null){
				fam = a.repeatable ? "기본 수련 · 중복 가능" : "기본 수련";
			} else if (star){
				fam = a.set.title;
			} else {
				int have = tracker.count(a.set);
				int next = -1;
				for (int th : AugSet.THRESHOLDS) if (have + 1 <= th){ next = th; break; }
				fam = a.set.title + " " + (have + 1) + (next > 0 ? "/" + next : "");
				//taking this card reaches a set bonus
				for (int th : AugSet.THRESHOLDS){
					if (have + 1 == th){
						fam += "  세트 " + th + " 발동!";
						break;
					}
				}
			}
			int famColor = a.set == null ? 0xB0B0B0 : a.set.color;
			//taking this card completes a hidden combination
			String unlocks = null;
			for (Aug l : Aug.all()){
				if (l.tier != Aug.LEGEND || l.requires == null || tracker.timesOwned(l) > 0) continue;
				boolean part = false, rest = true;
				for (String r : l.requires){
					if (r.equals(a.name)) part = true;
					else if (!tracker.owned.contains(r)) rest = false;
				}
				if (part && rest){ unlocks = l.name; break; }
			}
			boolean goalPart = false;
			Aug goal = tracker.goal == null ? null : Aug.get(tracker.goal);
			if (!star && goal != null && goal.requires != null && java.util.Arrays.asList(goal.requires).contains(a.name)) goalPart = true;
			if (a.tier != Aug.LEGEND && !star && unlocks != null) fam += "  전설 해금!";
			if (a.tier == Aug.LEGEND){
				fam = "전설 조합 · " + a.requiresText();
				famColor = 0xFF9A60;
			} else if (star){
				fam = "강화 · " + fam;
				famColor = 0xFFE070;
			} else if (a.gamble){
				fam = "도박 · " + fam;
				famColor = 0xFF6A5A;
			} else if (goalPart){
				fam = "목표 재료! " + fam;
				famColor = 0xFF9A60;
			} else if (a.jackpot()){
				fam = "잭팟 · " + fam;
			} else if (AugGrow.grows(a)){
				fam = "성장 · " + fam;
			} else if (tier > tracker.offerTier){
				fam = "행운! " + fam;
			}
			family = PixelScene.renderTextBlock(fam, 6);
			family.hardlight(famColor);
			add(family);

			desc = PixelScene.renderTextBlock(a.desc(tracker), 6);
			add(desc);

			rerollBtn = new com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton(com.shatteredpixel.shatteredpixeldungeon.Chrome.Type.GREY_BUTTON_TR, ""){
				@Override
				protected void onClick() {
					reroll(index);
				}

				@Override
				protected String hoverText() {
					return "다시 뽑기";
				}
			};
			rerollBtn.icon(Icons.get(Icons.SHUFFLE));
			rerollBtn.visible = rerollBtn.active = !tracker.rerolled[index] && !mystery;
			add(rerollBtn);

			//a soft additive flash once the card has fully turned
			glint = new ColorBlock(1, 1, 0xFFFFFFFF){
				@Override
				public void draw() {
					com.watabou.glwrap.Blending.setLightMode();
					super.draw();
					com.watabou.glwrap.Blending.setNormalMode();
				}
			};
			glint.alpha(0);
			add(glint);

			missedText = PixelScene.renderTextBlock("놓친 카드", 7);
			missedText.hardlight(0xFF8080);
			missedText.visible = false;
			add(missedText);

			if (tier > Aug.SILVER || mystery){
				sparkles = new Emitter();
				add(sparkles);
				if (mystery) sparkles.pour(Twinkle.factory(-1), 0.25f);
			}

			if (deal){
				flip = 0f;
				waiting = true;
				announced = false;
			}
			if (isSelected) selected(true);
		}

		private void drop( com.watabou.noosa.Gizmo g ){
			if (g == null) return;
			g.killAndErase();
			g.destroy();
		}

		void flipFrom( float f ){
			flip = f;
			waiting = false;
			announced = false;
		}

		//the mystery card turns over, after a build up when it was the one chosen
		void reveal( boolean chosen ){
			revealAt = time + (chosen ? 0.9f : 0.35f);
			missed = !chosen;
			Sample.INSTANCE.play(Assets.Sounds.AUG_TEASE, chosen ? 0.9f : 0.5f, chosen ? 1f : 1.3f);
		}

		void selected( boolean value ){
			isSelected = value;
			for (ColorBlock c : ring) c.visible = value;
		}

		//height needed for a given width
		float measure( float w ){
			float inner = w - AugArt.MARGIN * 2 - 2;
			name.maxWidth((int)(inner - 36));
			family.maxWidth((int)(inner - 36));
			desc.maxWidth((int)inner);
			if (markLabel != null) markLabel.maxWidth((int)inner);
			float head = Math.max(18, name.height() + family.height() + 3);
			float h = AugArt.MARGIN * 2 + 2 + head + 3 + desc.height();
			if (mystery) h = Math.max(h, 44);
			return h;
		}

		@Override
		protected void layout() {
			super.layout();
			baseX = x;
			float m = AugArt.MARGIN + 1;

			back.size(width, height);
			back.x = x; back.y = y;
			if (backAlt != null){
				backAlt.size(width, height);
				backAlt.x = x; backAlt.y = y;
			}
			front.size(width, height);
			front.x = x; front.y = y;

			emblem.scale.set(Math.min(2f, (height - 10) / 13f));
			emblem.x = x + (width - emblem.width()) / 2f;
			emblem.y = y + (height - emblem.height()) / 2f;
			PixelScene.align(emblem);
			if (mark != null){
				mark.setPos(x + (width - mark.width()) / 2f, y + (height - mark.height()) / 2f - 4);
				markLabel.setPos(x + (width - markLabel.width()) / 2f, mark.bottom() + 2);
				PixelScene.align(mark);
				PixelScene.align(markLabel);
			}

			icon.x = x + m;
			icon.y = y + m;
			name.setPos(icon.x + 18, y + m);
			family.setPos(icon.x + 18, name.bottom() + 2);
			float head = Math.max(18, name.height() + family.height() + 3);
			desc.setPos(x + m, y + m + head + 2);
			PixelScene.align(name);
			PixelScene.align(family);
			PixelScene.align(desc);
			missedText.setPos(x + width - missedText.width() - m - 2, y + height - missedText.height() - m + 1);
			PixelScene.align(missedText);

			rerollBtn.setRect(x + width - m - 15, y + m - 1, 16, 15);

			glint.x = x + 3; glint.y = y + 3;
			glint.size(width - 6, height - 6);

			tease.point(x + width / 2f, y + height / 2f);

			ring[0].x = x - 2; ring[0].y = y - 2; ring[0].size(width + 4, 2);
			ring[1].x = x - 2; ring[1].y = y + height; ring[1].size(width + 4, 2);
			ring[2].x = x - 2; ring[2].y = y; ring[2].size(2, height);
			ring[3].x = x + width; ring[3].y = y; ring[3].size(2, height);

			if (sparkles != null){
				sparkles.pos(x + 2, y + 2, width - 4, height - 4);
			}
			applyFlip();
		}

		private void applyFlip(){
			float f = Math.max(0f, flip);
			boolean showFront = f >= 0.5f;
			float k = showFront ? (f - 0.5f) * 2f : 1f - f * 2f;
			k = Math.max(0.02f, Math.min(1f, k));

			NinePatch backShown = (backAlt != null && roulette > 0 && rouletteAlt) ? backAlt : back;
			NinePatch shown = showFront ? front : backShown;
			front.visible = back.visible = false;
			if (backAlt != null) backAlt.visible = false;
			shown.visible = true;
			float w = Math.max(AugArt.MARGIN * 2, width * k);
			shown.size(w, height);
			shown.x = x + (width - w) / 2f;

			if (!mystery){
				emblem.visible = !showFront;
				emblem.scale.x = Math.min(2f, (height - 10) / 13f) * (showFront ? 0 : k);
				emblem.x = x + (width - emblem.width()) / 2f;
			} else {
				mark.visible = markLabel.visible = !showFront && k > 0.6f;
			}

			float contentAlpha = showFront ? Math.min(1f, Math.max(0f, (f - 0.8f) / 0.2f)) : 0f;
			if (f >= 1f) contentAlpha = 1f;
			icon.visible = name.visible = family.visible = desc.visible = contentAlpha > 0;
			icon.alpha(contentAlpha);
			name.alpha(contentAlpha);
			family.alpha(contentAlpha);
			desc.alpha(contentAlpha);
			rerollBtn.visible = contentAlpha >= 0.99f && !tracker.rerolled[index] && !mystery && taking < 0;
			rerollBtn.active = rerollBtn.visible;
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;

			//waiting to turn: a surprise card trembles and leaks light just before
			float shake = 0;
			if (waiting && delay >= 0){
				float until = delay - time;
				if (surprise() && until < 0.9f && until > 0){
					if (!teaseSounded){
						teaseSounded = true;
						Sample.INSTANCE.play(Assets.Sounds.AUG_TEASE, 0.8f);
					}
					float p = 1f - until / 0.9f;
					shake = (float)Math.sin(t * 60f) * (0.6f + 1.6f * p);
					tease.hardlight(glowColor());
					tease.alpha(0.55f * p);
				}
				if (until <= 0){
					waiting = false;
					flip = 0f;
				}
			}
			//the mystery card builds up to its reveal
			if (revealAt > 0){
				float until = revealAt - time;
				if (until > 0){
					float p = 1f - until / 0.9f;
					shake = (float)Math.sin(t * 60f) * (0.8f + 1.8f * Math.max(0, p));
					tease.hardlight(glowColor());
					tease.alpha(0.6f * Math.max(0, p));
				} else {
					revealAt = -1;
					waiting = false;
					flip = 0f;
					mystery = false;
					if (mark != null){ mark.visible = false; markLabel.visible = false; }
				}
			}
			if (shake != 0 || x != baseX){
				float bx = baseX;
				x = bx + shake;
				layout();
				baseX = bx;
			}
			if (!waiting && revealAt < 0 && tease.alpha() > 0) tease.alpha(Math.max(0, tease.alpha() - Game.elapsed * 1.5f));

			if (!waiting && revealAt < 0 && flip < 1f && !mystery){
				float before = flip;
				flip = Math.min(1f, flip + Game.elapsed * 2.6f);
				if (before < 0.5f && flip >= 0.5f) onTurned();
				if (flip >= 1f){
					//flash only when the face is fully shown, so it never covers a half turned card
					glint.alpha(tier() == Aug.SILVER ? 0.16f : (tier() == Aug.LEGEND ? 0.45f : 0.28f));
					if (missed && tier() >= Aug.PRISM) missedText.visible = true;
				}
				applyFlip();
			} else if (roulette > 0){
				applyFlip();
			}

			//the glint fades after turning over
			if (glint.alpha() > 0) glint.alpha(Math.max(0f, glint.alpha() - Game.elapsed * 1.2f));

			int tier = aug.tier;
			boolean backSide = flip < 0.5f;
			float dim = (selected >= 0 && !isSelected) ? 0.6f : 1f;
			if (backSide){
				if (mystery){
					//a dark card with a slow shifting rainbow
					AugArt.prismTint(back, t * 0.6f + index, 0.45f);
					back.rm *= 0.75f; back.gm *= 0.6f; back.bm *= 0.9f;
					if (mark != null) mark.hardlight(AugArt.prismColor(t * 0.8f));
				} else {
					//the back uses the round's look
					int bt = tracker.offerTier;
					if (bt == Aug.PRISM){
						AugArt.prismTint(back, t + index * 0.4f, 0.8f);
						AugArt.tint(emblem, AugArt.prismColor(t + index * 0.4f));
					} else {
						back.rm = back.gm = back.bm = bt == Aug.GOLD ? 1f + 0.12f * (float)Math.sin(t * 3f + index) : 1f;
					}
					if (backAlt != null){
						AugArt.prismTint(backAlt, t, 0.8f);
					}
				}
			} else if (tier == Aug.LEGEND){
				//embers: a slow warm pulse with a flicker
				float p = 1f + 0.18f * (float)Math.sin(t * 2.4f + index) + 0.05f * (float)Math.sin(t * 13f);
				front.rm = p * dim;
				front.gm = (0.92f + 0.08f * (float)Math.sin(t * 1.7f)) * dim;
				front.bm = 0.85f * dim;
			} else if (tier == Aug.PRISM){
				AugArt.prismTint(front, t + index * 0.4f, 0.8f);
				front.rm *= dim; front.gm *= dim; front.bm *= dim;
			} else if (tier == Aug.GOLD){
				float s = (1f + 0.12f * (float)Math.sin(t * 3f + index)) * dim;
				front.rm = front.gm = front.bm = s;
			} else {
				front.rm = front.gm = front.bm = dim;
			}
			float ta = (selected >= 0 && !isSelected) ? 0.55f : 1f;
			if (flip >= 1f){
				icon.alpha(ta); name.alpha(ta); family.alpha(ta); desc.alpha(ta);
			}

			if (isSelected){
				float a = 0.75f + 0.25f * (float)Math.sin(t * 6f);
				int col = mystery ? AugArt.prismColor(t) : (tier == Aug.LEGEND ? 0xFF8A50 : (tier == Aug.PRISM ? AugArt.prismColor(t) : (aug.set != null ? aug.set.color : Aug.TIER_COLORS[tier])));
				for (ColorBlock c : ring){
					c.hardlight(col);
					c.alpha(a);
				}
			}
		}

		private int glowColor(){
			int real = aug.tier;
			if (real == Aug.LEGEND) return 0xFF8A40;
			if (real == Aug.PRISM) return AugArt.prismColor(t);
			if (real == Aug.GOLD) return 0xFFD060;
			return 0xD8E0EA;
		}

		private void onTurned(){
			if (!announced){
				announced = true;
				AugMoments.reveal(tier());
				if (tier() == Aug.LEGEND){
					new Flare(9, 40).color(0xFF8A40, true).show(this.parent, center(), 1.8f);
				} else if (tier() == Aug.PRISM){
					new Flare(7, 30).color(AugArt.prismColor(t), true).show(this.parent, center(), 1.2f);
				} else if (tier() == Aug.GOLD){
					new Flare(5, 22).color(0xFFD060, true).show(this.parent, center(), 0.9f);
				}
				if (tracker.canStar(aug)){
					Sample.INSTANCE.play(Assets.Sounds.AUG_STAR, 0.5f, 1.1f);
				}
			}
			if (sparkles != null){
				sparkles.on = false;
				if (tier() == Aug.LEGEND)     sparkles.pour(Twinkle.factory(-2), 0.08f);
				else if (tier() == Aug.PRISM) sparkles.pour(Twinkle.factory(-1), 0.12f);
				else if (tier() == Aug.GOLD)  sparkles.pour(Twinkle.factory(0xFFE6A0), 0.2f);
			}
		}

		private int tier(){
			return aug.tier;
		}

		private com.watabou.utils.PointF center(){
			return new com.watabou.utils.PointF(x + width / 2f, y + height / 2f);
		}

		@Override
		protected void onClick() {
			if (taking >= 0) return;
			if (flip >= 1f || (mystery && dealt())) select(index);
		}
	}
}
