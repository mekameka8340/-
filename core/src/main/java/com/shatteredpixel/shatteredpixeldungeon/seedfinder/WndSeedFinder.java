/*
 * Seed finder window
 */

package com.shatteredpixel.shatteredpixeldungeon.seedfinder;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingGridPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public class WndSeedFinder extends Window {

	//kept between openings, so results aren't lost when the window is closed
	private static SeedFinder finder;

	private static final int GAP = 2;
	private static final int BTN_H = 16;

	private final int width;

	private Component content;
	private ScrollPane scroll;
	private RenderedTextBlock status;
	private RedButton btnSearch;
	private RedButton btnAdd;

	private int lastResultCount = -1;
	private boolean lastRunning;
	private float statusTimer;

	public WndSeedFinder(){
		super();
		if (finder == null){
			finder = new SeedFinder();
		}
		width = PixelScene.landscape() ? 200 : 135;
		rebuild();
	}

	public static String sourceName( String src ){
		switch (src){
			case "shop":      return "상점";
			case "chest":     return "상자";
			case "locked":    return "잠긴 상자";
			case "crystal":   return "수정 상자";
			case "tomb":      return "무덤";
			case "remains":   return "유해";
			case "mimic":     return "미믹";
			case "statue":    return "석상";
			case "ghost":     return "유령 보상(택1)";
			case "wandmaker": return "마법사 보상(택1)";
			case "imp":       return "임프 보상(택1)";
			case "smith":     return "대장장이 보상(택1)";
			case "catalyst":  return "마법의 촉매제 선택지(택1)";
			case "rotheart":  return "썩은 정원 (썩은 심장 처치)";
			default:          return "바닥";
		}
	}

	public static String itemName( Class<? extends Item> cls ){
		return Messages.get(cls, "name");
	}

	public static ItemSprite itemIcon( Class<? extends Item> cls ){
		//these look different in every run, so show a generic icon
		if (Ring.class.isAssignableFrom(cls)){
			return new ItemSprite(ItemSpriteSheet.RING_HOLDER);
		}
		if (com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion.class.isAssignableFrom(cls)){
			return new ItemSprite(ItemSpriteSheet.POTION_HOLDER);
		}
		if (com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.class.isAssignableFrom(cls)){
			return new ItemSprite(ItemSpriteSheet.SCROLL_HOLDER);
		}
		try {
			Item i = Reflection.newInstance(cls);
			if (i != null) return new ItemSprite(i.image(), null);
		} catch (Exception ignored){}
		return new ItemSprite(ItemSpriteSheet.SOMETHING);
	}

	//the small type mark the game shows on identified potions, scrolls and rings
	public static com.watabou.noosa.Image typeMark( Class<? extends Item> cls ){
		try {
			Item i = Reflection.newInstance(cls);
			if (i == null || i.icon == -1) return null;
			com.watabou.noosa.Image img = new com.watabou.noosa.Image(com.shatteredpixel.shatteredpixeldungeon.Assets.Sprites.ITEM_ICONS);
			img.frame(ItemSpriteSheet.Icons.film.get(i.icon));
			return img;
		} catch (Exception e){
			return null;
		}
	}

	private void rebuild(){
		float keepScroll = 0;
		if (scroll != null){
			keepScroll = scroll.content().camera.scroll.y;
			scroll.destroy();
			remove(scroll);
		}

		//the scroll pane is created first so that the buttons inside it,
		// created afterwards, receive touches before the pane's own drag handler
		scroll = new ScrollPane(new Component());
		add(scroll);
		content = scroll.content();
		float y = 0;

		RenderedTextBlock title = PixelScene.renderTextBlock("시드 파인더", 9);
		title.hardlight(TITLE_COLOR);
		title.setPos((width - title.width())/2f, y + 2);
		content.add(title);
		y = title.bottom() + 4;

		String cls = GamesInProgress.selectedClass != null ? Messages.titleCase(GamesInProgress.selectedClass.title()) : "";
		int chal = Challenges.activeChallenges();
		RenderedTextBlock info = PixelScene.renderTextBlock(
				"원하는 아이템이 나오는 시드를 찾습니다. 현재 선택한 직업(" + cls + ")"
				+ (chal > 0 ? "과 도전 " + chal + "개" : "") + " 기준으로 검색합니다."
				+ " 5층까지는 정확하며, 이후 층은 몬스터 드랍과 상점 가방에 따라 일부 달라질 수 있습니다.", 6);
		info.maxWidth(width);
		info.setPos(0, y);
		content.add(info);
		y = info.bottom() + 4;

		//targets
		RenderedTextBlock tHead = PixelScene.renderTextBlock("찾을 아이템 (" + finder.targets.size() + "/" + SeedFinder.MAX_TARGETS + ")", 7);
		tHead.hardlight(TITLE_COLOR);
		tHead.setPos(0, y);
		content.add(tHead);
		y = tHead.bottom() + 2;

		for (int i = 0; i < finder.targets.size(); i++){
			final int index = i;
			final Class<? extends Item> t = finder.targets.get(i);
			ColorBlock bg = new ColorBlock(width, 16, 0x33000000);
			bg.y = y;
			content.add(bg);

			ItemSprite icon = itemIcon(t);
			icon.x = 1 + (16 - icon.width())/2f;
			icon.y = y + (16 - icon.height())/2f;
			content.add(icon);
			com.watabou.noosa.Image mark = typeMark(t);
			if (mark != null){
				mark.x = 17 - mark.width();
				mark.y = y;
				content.add(mark);
			}

			RenderedTextBlock name = PixelScene.renderTextBlock(Messages.titleCase(targetName(index)), 7);
			name.setPos(19, y + (16 - name.height())/2f);
			content.add(name);

			IconButton rem = new IconButton(Icons.get(Icons.CLOSE)){
				@Override
				protected void onClick() {
					if (searching()) return;
					finder.removeTarget(index);
					rebuild();
				}
			};
			rem.setRect(width - 16, y, 16, 16);
			rem.enable(!searching());
			content.add(rem);
			y += 17;
		}

		btnAdd = new RedButton("+ 아이템 추가", 7){
			@Override
			protected void onClick() {
				ShatteredPixelDungeon.scene().addToFront(new WndPick(WndSeedFinder.this));
			}
		};
		btnAdd.enable(!searching() && finder.targets.size() < SeedFinder.MAX_TARGETS);
		btnAdd.setRect(0, y, width, BTN_H);
		content.add(btnAdd);
		y = btnAdd.bottom() + GAP;

		final RenderedTextBlock depthInfo = PixelScene.renderTextBlock(depthInfo(finder.maxDepth), 6);
		OptionSlider depth = new OptionSlider(depthTitle(finder.maxDepth), "1", "24", 1, 24){
			@Override
			protected void onChange() {
				finder.maxDepth = getSelectedValue();
				title(depthTitle(finder.maxDepth));
				depthInfo.text(depthInfo(finder.maxDepth));
			}
			@Override
			protected void onDragValue( int val ) {
				val = Math.max(1, Math.min(24, val));
				title(depthTitle(val));
				depthInfo.text(depthInfo(val));
			}
		};
		depth.setSelectedValue(finder.maxDepth);
		depth.setRect(0, y, width, 25);
		//boss floors stand out on the slider
		for (int b = 5; b <= 20; b += 5) depth.markTick(b, 0xFFE040);
		depth.active = !searching();
		content.add(depth);
		y = depth.bottom() + 1;
		//floor numbers under the boss floor marks
		float tickDist = (width - 5) / 23f;
		for (int b = 5; b <= 20; b += 5){
			RenderedTextBlock n = PixelScene.renderTextBlock(Integer.toString(b), 6);
			n.hardlight(0xFFE040);
			n.setPos(2 + tickDist * (b - 1) - n.width() / 2f, y);
			PixelScene.align(n);
			content.add(n);
		}
		y += 8;
		depthInfo.maxWidth(width - 2);
		depthInfo.hardlight(0xCCCCCC);
		depthInfo.setPos(1, y);
		content.add(depthInfo);
		//the text changes with the slider, so reserve room for its longest version
		float infoH = 0;
		for (int d = 1; d <= 24; d++){
			RenderedTextBlock probe = PixelScene.renderTextBlock(depthInfo(d), 6);
			probe.maxWidth(width - 2);
			infoH = Math.max(infoH, probe.height());
			probe.destroy();
		}
		y = y + infoH + GAP + 1;


		CheckBox curse = new CheckBox("저주받은 아이템 제외"){
			@Override
			protected void onClick() {
				super.onClick();
				finder.noCursed = checked();
			}
		};
		curse.checked(finder.noCursed);
		curse.active = !searching();
		curse.setRect(0, y, width, BTN_H);
		content.add(curse);
		y = curse.bottom() + GAP;

		btnSearch = new RedButton(searching() ? "검색 중지" : "검색 시작", 8){
			@Override
			protected void onClick() {
				if (searching()){
					stopSearch();
				} else if (finder.targets.isEmpty()){
					ShatteredPixelDungeon.scene().addToFront(new WndMessage("먼저 찾을 아이템을 추가하세요."));
					return;
				} else {
					startSearch();
				}
				rebuild();
			}
		};
		btnSearch.setRect(0, y, width, BTN_H + 2);
		if (searching()) btnSearch.textColor(0xFF8888);
		content.add(btnSearch);
		y = btnSearch.bottom() + GAP;

		status = PixelScene.renderTextBlock(statusText(), 6);
		status.maxWidth(width);
		status.setPos(0, y);
		content.add(status);
		y = status.bottom() + 3;

		//results
		for (final SeedFinder.Result r : finder.results){
			RedButton b = new RedButton(r.code + "  (적용)", 7){
				@Override
				protected void onClick() {
					SPDSettings.customSeed(r.code);
					ShatteredPixelDungeon.scene().addToFront(new WndMessage(
							"시드 " + r.code + " 를 적용했습니다.\n\n시작 버튼을 누르면 이 시드로 게임이 시작됩니다. (같은 직업과 도전 설정을 유지하세요)"));
				}
			};
			b.setRect(0, y, width, BTN_H);
			content.add(b);
			y = b.bottom() + 1;

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < r.where.length && i < finder.targets.size(); i++){
				String[] w = r.where[i].split("\\|", -1);
				if (sb.length() > 0) sb.append("\n");
				sb.append("- ").append(Messages.titleCase(targetName(i)))
						.append(w.length > 2 ? w[2] : "")
						.append(": ");
				if (w.length > 1 && w[1].equals("catalyst")){
					sb.append(sourceName(w[1]));
				} else {
					sb.append(w[0]).append("층 ").append(sourceName(w.length > 1 ? w[1] : ""));
				}
			}
			RenderedTextBlock d = PixelScene.renderTextBlock(sb.toString(), 6);
			d.maxWidth(width);
			d.setPos(2, y);
			content.add(d);
			y = d.bottom() + 4;
		}

		content.setSize(width, y);

		//fit inside the screen, scrolling if needed
		float maxH = PixelScene.uiCamera.height * 0.85f;
		float h = Math.min(y, maxH);

		resize(width, (int)Math.ceil(h));
		scroll.setRect(0, 0, width, (int)Math.ceil(h));
		scroll.scrollTo(0, keepScroll);

		lastResultCount = finder.results.size();
		lastRunning = searching();
	}

	// ---------------------------------------------------------------- parallel search

	private static boolean parallel = false;
	private static int[] workerChecked;
	private static boolean[] workerDead;
	private static final java.util.concurrent.ConcurrentLinkedQueue<long[]> pendingSeeds = new java.util.concurrent.ConcurrentLinkedQueue<>();
	private static final java.util.HashSet<Long> seenSeeds = new java.util.HashSet<>();
	private static int rejectedResults = 0;
	private static long searchStart;

	private static boolean searching(){
		return parallel || finder.running();
	}

	private static void startSearch(){
		finder.results.clear();
		finder.seedsChecked = 0;
		pendingSeeds.clear();
		seenSeeds.clear();
		rejectedResults = 0;
		searchStart = System.currentTimeMillis();

		final ParallelSearch backend = ParallelSearch.backend;
		if (backend != null && backend.workerCount() > 1){
			int n = backend.workerCount();
			workerChecked = new int[n];
			workerDead = new boolean[n];
			parallel = true;
			try {
				backend.start(SearchSpec.from(finder).encode(), new ParallelSearch.Listener() {
					@Override
					public void progress(int worker, int checked) {
						if (worker < workerChecked.length) workerChecked[worker] = checked;
					}
					@Override
					public void result(long seed, String[] where) {
						//verified on the game thread before being shown
						pendingSeeds.add(new long[]{seed});
					}
					@Override
					public void workerFailed(int worker) {
						if (worker < workerDead.length) workerDead[worker] = true;
					}
				});
			} catch (Exception e){
				Game.reportException(e);
				parallel = false;
			}
		}
		if (!parallel){
			finder.start();
		}
	}

	private static void stopSearch(){
		if (parallel){
			parallel = false;
			try {
				ParallelSearch.backend.stop();
			} catch (Exception e){
				Game.reportException(e);
			}
			pendingSeeds.clear();
		}
		finder.stop();
	}

	//called every frame while searching in parallel
	private static void updateParallel(){
		int total = 0;
		for (int c : workerChecked) total += c;
		finder.seedsChecked = total;

		//every seed a worker finds is generated again here, so only seeds that really match are shown
		long budgetEnd = System.currentTimeMillis() + 40;
		long[] next;
		while (finder.results.size() < SeedFinder.MAX_RESULTS
				&& System.currentTimeMillis() < budgetEnd
				&& (next = pendingSeeds.poll()) != null){
			long seed = next[0];
			if (!seenSeeds.add(seed)) continue;
			String[] where = finder.evaluate(seed);
			if (where == null){
				rejectedResults++;
				continue;
			}
			SeedFinder.Result r = new SeedFinder.Result();
			r.seed = seed;
			r.code = com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed.convertToCode(seed);
			r.where = where;
			finder.results.add(r);
		}

		if (finder.results.size() >= SeedFinder.MAX_RESULTS){
			stopSearch();
			return;
		}

		//if every worker failed, carry on in this process instead
		boolean allDead = true;
		for (boolean d : workerDead) if (!d) allDead = false;
		if (allDead){
			parallel = false;
			try { ParallelSearch.backend.stop(); } catch (Exception ignored){}
			int checked = finder.seedsChecked;
			java.util.ArrayList<SeedFinder.Result> keep = new java.util.ArrayList<>(finder.results);
			finder.start();
			finder.results.addAll(keep);
			finder.seedsChecked = checked;
		}
	}

	private String statusText(){
		if (searching()){
			float secs = Math.max(0.1f, (System.currentTimeMillis() - searchStart) / 1000f);
			String mode = parallel ? "병렬 " + workerChecked.length + "개, " : "";
			return "검색 중... (" + mode + "초당 " + Math.round(finder.seedsChecked / secs) + "개) 확인한 시드 "
					+ finder.seedsChecked + "개, 찾은 시드 " + finder.results.size() + "개";
		} else if (finder.seedsChecked > 0){
			return "검색 완료: 확인한 시드 " + finder.seedsChecked + "개, 찾은 시드 " + finder.results.size() + "개"
					+ (finder.results.isEmpty() ? "\n조건을 완화하거나 층 범위를 늘려보세요." : "")
					+ (rejectedResults > 0 ? "\n(재확인에서 맞지 않아 제외한 결과 " + rejectedResults + "개)" : "");
		} else {
			return "최대 " + SeedFinder.MAX_RESULTS + "개까지 찾으면 자동으로 멈춥니다.";
		}
	}

	@Override
	public void update() {
		super.update();

		if (parallel){
			updateParallel();
		} else if (finder.running()){
			//most of each frame goes to the search, the window only needs a few frames per second
			finder.step(60);
		}

		if (finder.results.size() != lastResultCount || searching() != lastRunning){
			rebuild();
			return;
		}

		statusTimer -= Game.elapsed;
		if (statusTimer <= 0 && status != null){
			statusTimer = 0.25f;
			status.text(statusText());
		}
	}

	@Override
	public void onBackPressed() {
		//keep searching? no, a closed window stops the search to restore normal state
		stopSearch();
		super.onBackPressed();
	}

	@Override
	public void destroy() {
		if (finder != null) stopSearch();
		super.destroy();
	}

	private static String depthTitle( int d ){
		return "검색할 층: 1 ~ " + d + "층";
	}

	//what the chosen depth covers, so it's clear without counting ticks
	private static String depthInfo( int d ){
		String region;
		if (d <= 4) region = "하수도";
		else if (d == 5) region = "하수도 보스(5층)까지";
		else if (d <= 9) region = "감옥";
		else if (d == 10) region = "감옥 보스(10층)까지";
		else if (d <= 14) region = "동굴";
		else if (d == 15) region = "동굴 보스(15층)까지";
		else if (d <= 19) region = "드워프 도시";
		else if (d == 20) region = "도시 보스(20층)까지";
		else region = "악마의 전당";
		StringBuilder sb = new StringBuilder(region);
		if (d >= 7) sb.append(" · 마법사 퀘스트(7~9층)");
		if (d >= 12) sb.append(" · 대장장이(12~14층)");
		if (d >= 17) sb.append(" · 임프(17층~)");
		sb.append(" 포함");
		if (d >= 9 && d < 14) sb.append("\n대장장이 보상까지 보려면 14층으로 설정하세요");
		return sb.toString();
	}

	void addTarget( Class<? extends Item> cls, Class<?> ench, int lvl ){
		finder.addTarget(cls, ench, lvl);
		rebuild();
	}

	//item name with its required enchantment, e.g. "불타는 롱소드"
	private static String targetName( int i ){
		Class<? extends Item> cls = finder.targets.get(i);
		Class<?> ench = i < finder.enchants.size() ? finder.enchants.get(i) : null;
		int lvl = i < finder.levels.size() ? finder.levels.get(i) : 0;
		return withEnchant(cls, ench) + levelText(lvl);
	}

	static String levelText( int lvl ){
		return lvl > 0 ? " (+" + lvl + " 이상)" : "";
	}

	static String withEnchant( Class<? extends Item> cls, Class<?> ench ){
		if (ench == null) return itemName(cls);
		return Messages.get(ench, "name", itemName(cls));
	}

	static boolean enchantable( Class<?> cls ){
		return com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.class.isAssignableFrom(cls)
				|| com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor.class.isAssignableFrom(cls);
	}

	// ---------------------------------------------------------------- item picker

	public static class WndPick extends Window {

		private Class<? extends Item> selected;
		private Class<?> selectedEnch;
		private RedButton btnEnch;
		private int selectedLvl = 0;
		private RedButton btnLvl;

		private void updateLvlText(){
			btnLvl.text(selected == null || !SeedFinder.levelled(selected) ? "강화 수치 없음"
					: selectedLvl == 0 ? "강화: 상관없음" : "강화: +" + selectedLvl + " 이상");
		}

		void setEnchant( Class<?> ench ){
			selectedEnch = ench;
			btnEnch.text(ench == null ? "접두사: 상관없음" : "접두사: " + withEnchant(selected, ench));
			selName.text(Messages.titleCase(withEnchant(selected, ench)));
			selName.setPos((width - selName.width())/2f, selName.top());
		}
		private RenderedTextBlock selName;
		private RedButton btnOk;

		public WndPick( final WndSeedFinder owner ){
			super();
			int w = PixelScene.landscape() ? 200 : 135;
			float paneH = Math.min(150, PixelScene.uiCamera.height * 0.6f);

			RenderedTextBlock title = PixelScene.renderTextBlock("아이템 선택", 9);
			title.hardlight(TITLE_COLOR);
			title.setPos((w - title.width())/2f, 2);
			add(title);

			ScrollingGridPane pane = new ScrollingGridPane();
			add(pane);

			addCategory(pane, "근접 무기", Generator.Category.WEP_T1, Generator.Category.WEP_T2,
					Generator.Category.WEP_T3, Generator.Category.WEP_T4, Generator.Category.WEP_T5);
			addCategory(pane, "방어구", Generator.Category.ARMOR);
			addCategory(pane, "장신구 (마법의 촉매제 선택지)", true, Generator.Category.TRINKET);
			addExtra(pane, "연금술 재료",
					com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst.class,
					com.shatteredpixel.shatteredpixeldungeon.items.EnergyCrystal.class);
			addCategory(pane, "마법 막대", Generator.Category.WAND);
			addCategory(pane, "반지", Generator.Category.RING);
			addCategory(pane, "유물", Generator.Category.ARTIFACT);
			addCategory(pane, "투척 무기", Generator.Category.MIS_T1, Generator.Category.MIS_T2,
					Generator.Category.MIS_T3, Generator.Category.MIS_T4, Generator.Category.MIS_T5);
			//alchemy: potions of strength and scrolls of upgrade are placed by special rules,
			// so every potion and scroll type is listed
			addCategory(pane, "물약", true, Generator.Category.POTION);
			addCategory(pane, "주문서", true, Generator.Category.SCROLL);
			addCategory(pane, "룬석", true, Generator.Category.STONE);
			addCategory(pane, "씨앗", true, Generator.Category.SEED);

			float y = title.bottom() + 4;
			float paneY = y;
			y += paneH + 3;

			selName = PixelScene.renderTextBlock("아이템을 눌러 선택하세요", 7);
			selName.setPos((w - selName.width())/2f, y);
			add(selName);
			y = selName.bottom() + 3;

			//enchantment (prefix) for weapons and armor
			btnEnch = new RedButton("접두사: 상관없음", 7){
				@Override
				protected void onClick() {
					if (selected != null && enchantable(selected)){
						ShatteredPixelDungeon.scene().addToFront(new WndEnchant(WndPick.this, selected));
					}
				}
			};
			btnEnch.enable(false);
			btnEnch.setRect(0, y, w, BTN_H);
			add(btnEnch);
			y = btnEnch.bottom() + 2;

			//minimum upgrade level, cycles through +0 to +3 (game generates up to +3)
			btnLvl = new RedButton("강화: 상관없음", 7){
				@Override
				protected void onClick() {
					if (selected != null && SeedFinder.levelled(selected)){
						selectedLvl = (selectedLvl + 1) % 4;
						updateLvlText();
					}
				}
			};
			btnLvl.enable(false);
			btnLvl.setRect(0, y, w, BTN_H);
			add(btnLvl);
			y = btnLvl.bottom() + 2;

			btnOk = new RedButton("추가", 8){
				@Override
				protected void onClick() {
					if (selected != null){
						hide();
						owner.addTarget(selected, selectedEnch, selectedLvl);
					}
				}
			};
			btnOk.enable(false);
			btnOk.setRect(0, y, w, BTN_H);
			add(btnOk);
			y = btnOk.bottom();

			resize(w, (int)Math.ceil(y));
			//the pane's camera depends on the window position, so it's placed after resizing
			pane.setRect(0, paneY, w, paneH);
		}

		@SuppressWarnings("unchecked")
		private void addCategory( ScrollingGridPane pane, String header, Generator.Category... cats ){
			addCategory(pane, header, false, cats);
		}

		@SafeVarargs
		private final void addExtra( ScrollingGridPane pane, String header, Class<? extends Item>... classes ){
			pane.addHeader(header);
			for (Class<? extends Item> c : classes) addGridItem(pane, c);
		}

		@SuppressWarnings("unchecked")
		private void addCategory( ScrollingGridPane pane, String header, boolean includeAll, Generator.Category... cats ){
			pane.addHeader(header);
			ArrayList<Class<? extends Item>> list = new ArrayList<>();
			for (Generator.Category c : cats){
				if (c.classes == null) continue;
				float[] probs = c.defaultProbs != null ? c.defaultProbs : c.probs;
				for (int i = 0; i < c.classes.length; i++){
					Class<?> cls = c.classes[i];
					//skip items that never generate naturally
					if (!includeAll && probs != null && i < probs.length && probs[i] <= 0) continue;
					if (!list.contains(cls)) list.add((Class<? extends Item>) cls);
				}
			}
			for (final Class<? extends Item> cls : list){
				addGridItem(pane, cls);
			}
		}

		private void addGridItem( ScrollingGridPane pane, final Class<? extends Item> cls ){
			{
				ScrollingGridPane.GridItem gi = new ScrollingGridPane.GridItem(itemIcon(cls)){
					@Override
					public boolean onClick(float x, float y) {
						if (inside(x, y)){
							select(cls);
							return true;
						}
						return false;
					}
				};
				com.watabou.noosa.Image mark = typeMark(cls);
				if (mark != null) gi.addSecondIcon(mark);
				pane.addItem(gi);
			}
		}

		private void select( Class<? extends Item> cls ){
			selected = cls;
			selName.text(Messages.titleCase(itemName(cls)));
			selectedEnch = null;
			btnEnch.enable(enchantable(cls));
			btnEnch.text(enchantable(cls) ? "접두사: 상관없음" : "접두사 없음");
			selectedLvl = 0;
			btnLvl.enable(SeedFinder.levelled(cls));
			updateLvlText();
			selName.setPos((width - selName.width())/2f, selName.top());
			btnOk.enable(true);
		}
	}

	// ---------------------------------------------------------------- enchantment picker

	public static class WndEnchant extends Window {

		public WndEnchant( final WndPick owner, final Class<? extends Item> item ){
			super();
			int w = PixelScene.landscape() ? 200 : 135;
			boolean weapon = com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.class.isAssignableFrom(item);

			RenderedTextBlock title = PixelScene.renderTextBlock("접두사 선택", 9);
			title.hardlight(TITLE_COLOR);
			title.setPos((w - title.width())/2f, 2);
			add(title);

			com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane list =
					new com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane();
			add(list);

			addOption(list, owner, item, null, "상관없음");
			list.addTitle("부여 마법");
			Class<?>[][] good = weapon
					? new Class<?>[][]{ com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment.common,
							com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment.uncommon,
							com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment.rare }
					: new Class<?>[][]{ com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor.Glyph.common,
							com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor.Glyph.uncommon,
							com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor.Glyph.rare };
			for (Class<?>[] group : good){
				for (Class<?> e : group){
					addOption(list, owner, item, e, Messages.titleCase(withEnchant(item, e)));
				}
			}
			list.addTitle("저주");
			Class<?>[] curses = weapon
					? com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment.curses
					: com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor.Glyph.curses;
			for (Class<?> e : curses){
				addOption(list, owner, item, e, Messages.titleCase(withEnchant(item, e)));
			}

			float h = Math.min(200, PixelScene.uiCamera.height * 0.7f);
			resize(w, (int)(title.bottom() + 4 + h));
			list.setRect(0, title.bottom() + 4, w, h);
		}

		private void addOption( com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane list,
								final WndPick owner, Class<? extends Item> item, final Class<?> ench, String label ){
			com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane.ListItem li =
					new com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane.ListItem(itemIcon(item), label){
						@Override
						public boolean onClick(float x, float y) {
							if (inside(x, y)){
								owner.setEnchant(ench);
								hide();
								return true;
							}
							return false;
						}
					};
			if (SeedFinder.isCurse(ench)) li.hardlight(0xFF8080);
			list.addItem(li);
		}
	}
}
