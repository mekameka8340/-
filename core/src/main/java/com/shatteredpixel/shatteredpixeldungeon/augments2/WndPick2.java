/*
 * The windows of the Augment II challenge: choosing one of three cards, choosing which skill a
 * new one replaces, strengthening a skill after a boss, and the description of a skill.
 */

package com.shatteredpixel.shatteredpixeldungeon.augments2;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.augments.AugArt;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.audio.Sample;

public class WndPick2 extends Window {

	private static final int GAP = 2;
	private static final int BTN = 16;

	private final Tracker2 t;
	private final CardView[] cards;
	private final RedButton take;
	private int selected = -1;
	private float time = 0;

	public WndPick2( Tracker2 tracker ){
		super();
		this.t = tracker;

		//three cards side by side only when the screen is really wide
		boolean wide = Game.width > Game.height && PixelScene.uiCamera.width >= 280;
		int width = wide ? Math.min(330, (int)PixelScene.uiCamera.width - 16) : Math.min(170, (int)PixelScene.uiCamera.width - 12);

		int level = t.rounds.isEmpty() ? 1 : t.rounds.get(0);
		RenderedTextBlock title = PixelScene.renderTextBlock(level <= 1 ? "첫 스킬 선택" : "레벨 " + level + " 선택", 10);
		title.hardlight(TITLE_COLOR);
		title.setPos((width - title.width()) / 2f, 2);
		PixelScene.align(title);
		add(title);

		float top = title.bottom() + 4;

		cards = new CardView[t.offer.length];
		float pos = top;
		if (wide){
			float cw = (width - GAP * (cards.length - 1)) / (float)cards.length;
			float tallest = 0;
			for (int i = 0; i < cards.length; i++){
				cards[i] = new CardView(i, Card2.get(t.offer[i]));
				add(cards[i]);
				tallest = Math.max(tallest, cards[i].wanted(cw));
			}
			for (int i = 0; i < cards.length; i++){
				cards[i].setRect(i * (cw + GAP), top, cw, tallest);
			}
			pos = top + tallest;
		} else {
			for (int i = 0; i < cards.length; i++){
				cards[i] = new CardView(i, Card2.get(t.offer[i]));
				add(cards[i]);
				cards[i].setRect(0, pos, width, cards[i].wanted(width));
				pos = cards[i].bottom() + GAP;
			}
			pos -= GAP;
		}

		pos += 4;
		RedButton reroll = new RedButton("다시 뽑기 (" + t.rerolls + ")"){
			@Override
			protected void onClick() {
				if (Card2.reroll(t)){
					Sample.INSTANCE.play(Assets.Sounds.READ, 0.6f, 1.3f);
					//closing lets the game scene open the new hand
					hide();
				}
			}
		};
		reroll.enable(t.rerolls > 0);
		reroll.setRect(0, pos, (width - GAP) / 2f, BTN);
		add(reroll);

		take = new RedButton("선택"){
			@Override
			protected void onClick() {
				confirm();
			}
		};
		take.enable(false);
		take.setRect(reroll.right() + GAP, pos, width - reroll.right() - GAP, BTN);
		add(take);

		resize(width, (int)(pos + BTN));
	}

	private void select( int i ){
		selected = i;
		take.enable(true);
		Sample.INSTANCE.play(Assets.Sounds.CLICK);
		for (CardView c : cards) c.mark(c.index == i);
	}

	private void confirm(){
		if (selected < 0) return;
		final Card2 c = cards[selected].card;
		if (c.replaces(t)){
			GameScene.show(new Replace(t, c, this));
		} else {
			Card2.choose(t, c, -1);
			hide();
		}
	}

	@Override
	public void update() {
		super.update();
		time += Game.elapsed;
		for (CardView c : cards) c.deal(time);
	}

	@Override
	public void onBackPressed() {
		//a choice must be made
	}

	@Override
	public void hide() {
		super.hide();
		Aug2.windowClosed();
	}

	// ================================================================ one card

	private class CardView extends Button {

		final int index;
		final Card2 card;
		private final NinePatch back;
		private final Image socket, icon;
		private final RenderedTextBlock name, kind, desc;
		private boolean marked = false;
		private float shownAlpha = -1;

		private static final int M = AugArt.MARGIN + 1;

		CardView( int index, Card2 card ){
			super();
			this.index = index;
			this.card = card;

			back = AugArt.frame(card.tier());
			add(back);

			socket = Fx2.slotFrame(card.tier() >= Card2.GOLD);
			add(socket);
			icon = Fx2.icon(card.icon());
			add(icon);

			name = PixelScene.renderTextBlock(card.name(), 9);
			name.hardlight(Card2.TIER_COLORS[card.tier()]);
			add(name);

			kind = PixelScene.renderTextBlock(card.kind(t), 6);
			kind.hardlight(0x9A9AA8);
			add(kind);

			desc = PixelScene.renderTextBlock(6);
			add(desc);
		}

		float wanted( float w ){
			desc.text(card.desc(t), (int)(w - M * 2));
			return M + Fx2.SLOT + 3 + desc.height() + M;
		}

		@Override
		protected void layout() {
			super.layout();
			back.x = x;
			back.y = y;
			back.size(width, height);

			socket.x = x + M;
			socket.y = y + M;
			icon.x = socket.x + (Fx2.SLOT - icon.width()) / 2f;
			icon.y = socket.y + (Fx2.SLOT - icon.height()) / 2f;
			PixelScene.align(icon);

			float tx = socket.x + Fx2.SLOT + 4;
			name.maxWidth((int)(x + width - M - tx));
			float block = name.height() + 2 + kind.height();
			name.setPos(tx, socket.y + Math.max(0, (Fx2.SLOT - block) / 2f));
			PixelScene.align(name);
			kind.setPos(tx, name.bottom() + 2);
			PixelScene.align(kind);

			desc.maxWidth((int)(width - M * 2));
			desc.setPos(x + M, socket.y + Fx2.SLOT + 3);
			PixelScene.align(desc);
		}

		void mark( boolean on ){
			marked = on;
		}

		//cards appear one after another, and those not picked step back
		void deal( float time ){
			float k = Math.max(0f, Math.min(1f, (time - index * 0.09f) / 0.18f));
			float a = k * (selected >= 0 && !marked ? 0.5f : 1f);
			if (a != shownAlpha){
				shownAlpha = a;
				back.alpha(a);
				socket.alpha(a);
				icon.alpha(a);
				name.alpha(a);
				kind.alpha(a);
				desc.alpha(a);
			}
			float glow = marked ? 0.10f + 0.04f * (float)Math.sin(time * 5f) : 0f;
			back.ra = back.ga = back.ba = glow;
		}

		@Override
		protected void onClick() {
			if (selected == index) confirm();
			else select(index);
		}
	}

	// ================================================================ replacing a skill

	private static class Replace extends Window {

		Replace( final Tracker2 t, final Card2 c, final WndPick2 owner ){
			super();
			int width = Math.min(150, (int)PixelScene.uiCamera.width - 16);

			IconTitle title = new IconTitle(Fx2.icon(c.icon()), c.name());
			title.setRect(0, 0, width, 0);
			add(title);

			RenderedTextBlock msg = PixelScene.renderTextBlock(
					c.skill.ult ? "궁극기는 하나만 지닐 수 있습니다. 지금 궁극기를 버리고 바꿉니다."
							: "스킬 칸이 가득 찼습니다. 버릴 스킬을 고르세요. 강화 단계는 이어지지 않습니다.", 6);
			msg.maxWidth(width);
			msg.setPos(0, title.bottom() + 3);
			add(msg);

			float pos = msg.bottom() + 4;
			int from = c.skill.ult ? Tracker2.ULT : 0, to = c.skill.ult ? Tracker2.SLOTS : Tracker2.ULT;
			for (int i = from; i < to; i++){
				final Skill s = t.skill(i);
				if (s == null) continue;
				final int slot = i;
				RedButton b = new RedButton(s.name + (t.step[i] > 0 ? "  (" + t.step[i] + "단계)" : "")){
					@Override
					protected void onClick() {
						Card2.choose(t, c, slot);
						hide();
						owner.hide();
					}
				};
				b.icon(Fx2.icon(s.icon));
				b.setRect(0, pos, width, 18);
				add(b);
				pos = b.bottom() + GAP;
			}

			RedButton back = new RedButton("돌아가기"){
				@Override
				protected void onClick() {
					hide();
				}
			};
			back.setRect(0, pos + 2, width, BTN);
			add(back);

			resize(width, (int)back.bottom());
		}
	}

	// ================================================================ strengthening a skill

	public static class Upgrade extends Window {

		public Upgrade( final Tracker2 t ){
			super();
			int width = Math.min(150, (int)PixelScene.uiCamera.width - 16);

			RenderedTextBlock title = PixelScene.renderTextBlock("스킬 강화", 10);
			title.hardlight(TITLE_COLOR);
			title.setPos((width - title.width()) / 2f, 2);
			PixelScene.align(title);
			add(title);

			RenderedTextBlock msg = PixelScene.renderTextBlock(
					"보스를 쓰러뜨렸습니다. 강화할 스킬을 고르세요.\n단계마다 피해 _+25%_, 재사용 대기 _-15%_. 다시 뽑기도 1번 늘었습니다.", 6);
			msg.maxWidth(width);
			msg.setPos(0, title.bottom() + 4);
			add(msg);

			float pos = msg.bottom() + 4;
			for (int i = 0; i < Tracker2.SLOTS; i++){
				final Skill s = t.skill(i);
				if (s == null || t.step[i] >= Tracker2.MAX_STEP) continue;
				final int slot = i;
				RedButton b = new RedButton(s.name + "  " + t.step[i] + " → " + (t.step[i] + 1) + "단계"){
					@Override
					protected void onClick() {
						Card2.upgrade(t, slot);
						hide();
					}
				};
				b.icon(Fx2.icon(s.icon));
				b.setRect(0, pos, width, 18);
				add(b);
				pos = b.bottom() + GAP;
			}

			resize(width, (int)(pos - GAP));
		}

		@Override
		public void onBackPressed() {
			//a choice must be made
		}

		@Override
		public void hide() {
			super.hide();
			Aug2.windowClosed();
		}
	}

	// ================================================================ what a skill does

	public static class Info extends Window {

		public Info( Tracker2 t, int slot ){
			super();
			Skill s = t.skill(slot);
			int width = Math.min(150, (int)PixelScene.uiCamera.width - 16);

			IconTitle title = new IconTitle(Fx2.icon(s.icon), s.name);
			title.color(Card2.TIER_COLORS[s.ult ? Card2.GOLD : Card2.SILVER]);
			title.setRect(0, 0, width, 0);
			add(title);

			String text = s.desc(t, t.power(slot));
			text += "\n\n재사용 대기: _" + t.cooldown(slot) + "턴_";
			if (t.cd[slot] > 0) text += " (지금 " + (int)Math.ceil(t.cd[slot]) + "턴 남음)";
			if (s.free) text += "\n턴을 쓰지 않습니다.";
			text += "\n강화: " + t.step[slot] + " / " + Tracker2.MAX_STEP + "단계";
			if (s.aim != Skill.SELF) text += "\n\n버튼을 누른 뒤 대상을 고릅니다. 버튼을 한 번 더 누르면 가장 가까운 적을 겨눕니다.";

			RenderedTextBlock msg = PixelScene.renderTextBlock(text, 6);
			msg.maxWidth(width);
			msg.setPos(0, title.bottom() + 3);
			add(msg);

			resize(width, (int)msg.bottom());
		}
	}
}
