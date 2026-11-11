/*
 * Seed finder - searches random dungeon seeds for ones containing chosen items.
 *
 * Levels are generated exactly as the game would generate them (sequentially from floor 1),
 * using the currently selected hero class and challenges. The search is time sliced so it
 * can run on the main thread without freezing the UI.
 */

package com.shatteredpixel.shatteredpixeldungeon.seedfinder;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import com.watabou.noosa.Game;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collection;

public class SeedFinder {

	public static class Result {
		public long seed;
		public String code;
		public String[] where; //per target
	}

	public static final int MAX_RESULTS = 10;
	public static final int MAX_TARGETS = 20;

	public final ArrayList<Class<? extends Item>> targets = new ArrayList<>();
	//required enchantment / glyph (or curse) for each target, null means any
	public final ArrayList<Class<?>> enchants = new ArrayList<>();
	//minimum upgrade level for each target
	public final ArrayList<Integer> levels = new ArrayList<>();

	public void addTarget( Class<? extends Item> cls, Class<?> ench, int lvl ){
		if (targets.size() >= MAX_TARGETS) return;
		targets.add(cls);
		enchants.add(ench);
		levels.add(lvl);
	}

	public void removeTarget( int index ){
		if (index < 0 || index >= targets.size()) return;
		targets.remove(index);
		enchants.remove(index);
		levels.remove(index);
	}

	public static boolean levelled( Class<?> cls ){
		return Weapon.class.isAssignableFrom(cls) || Armor.class.isAssignableFrom(cls)
				|| Wand.class.isAssignableFrom(cls) || Ring.class.isAssignableFrom(cls);
	}

	public static boolean isCurse( Class<?> ench ){
		if (ench == null) return false;
		for (Class<?> c : Weapon.Enchantment.curses) if (c == ench) return true;
		for (Class<?> c : Armor.Glyph.curses) if (c == ench) return true;
		return false;
	}
	public int maxDepth = 4;
	public boolean noCursed = true;

	public final ArrayList<Result> results = new ArrayList<>();
	public int seedsChecked = 0;
	//worker processes keep going until the main process stops them
	public int resultLimit = MAX_RESULTS;

	private boolean running = false;

	//state of the seed currently being examined
	private long curSeed;
	private int curDepth;
	private String[] curWhere;
	private boolean ghostDone, wandDone, impDone, smithDone;

	private long forcedSeed = -1;
	private ArrayList<String> dumpLog;

	//checks one specific seed against the current targets.
	// returns where each target was found, or null if the seed doesn't satisfy all of them
	public String[] evaluate( long seed ){
		boolean wasRunning = running;
		int wasChecked = seedsChecked;
		ArrayList<Result> savedResults = new ArrayList<>(results);
		results.clear();
		running = false;
		forcedSeed = seed;
		try {
			rejected = false;
			beginSeed();
			if (rejected) return null;
			while (curWhere != null){
				if (curWhere.length > 0 && allFound()){
					String[] w = curWhere.clone();
					finishSeed();
					return w;
				}
				int before = results.size();
				generateNextLevel();
				if (results.size() > before){
					return results.remove(results.size() - 1).where;
				}
			}
			return null;
		} finally {
			forcedSeed = -1;
			curWhere = null;
			cleanup();
			running = wasRunning;
			seedsChecked = wasChecked;
			results.clear();
			results.addAll(savedResults);
		}
	}

	//generates one seed and lists every item found on each floor, used for verifying consistency
	public ArrayList<String> dump( long seed, int depth ){
		ArrayList<Class<? extends Item>> savedTargets = new ArrayList<>(targets);
		ArrayList<Class<?>> savedEnchants = new ArrayList<>(enchants);
		ArrayList<Integer> savedLevels = new ArrayList<>(levels);
		int savedDepth = maxDepth;
		targets.clear();
		enchants.clear();
		levels.clear();
		maxDepth = depth;
		forcedSeed = seed;
		dumpLog = new ArrayList<>();
		try {
			beginSeed();
			while (curWhere != null){
				generateNextLevel();
			}
			return dumpLog;
		} finally {
			dumpLog = null;
			forcedSeed = -1;
			targets.addAll(savedTargets);
			enchants.addAll(savedEnchants);
			levels.addAll(savedLevels);
			maxDepth = savedDepth;
			cleanup();
		}
	}

	public boolean running(){
		return running;
	}

	public void start(){
		if (targets.isEmpty()) return;
		results.clear();
		seedsChecked = 0;
		running = true;
		curWhere = null;
	}

	public void stop(){
		if (!running) return;
		running = false;
		curWhere = null;
		cleanup();
	}

	//the player's journal, generation can change it (e.g. removing the intro page)
	private String journalSnapshot = null;

	private void protectState(){
		//every seed starts from the same journal, as a fresh game would
		if (journalSnapshot == null) journalSnapshot = SearchSpec.journalState();
		else SearchSpec.applyJournalState(journalSnapshot);
		com.shatteredpixel.shatteredpixeldungeon.Bones.skipForFinder = true;
	}

	private void cleanup(){
		com.shatteredpixel.shatteredpixeldungeon.Bones.skipForFinder = false;
		if (journalSnapshot != null){
			SearchSpec.applyJournalState(journalSnapshot);
			journalSnapshot = null;
		}
		try {
			Actor.clear();
		} catch (Exception ignored){}
		Dungeon.level = null;
		Dungeon.hero = null;
		Random.resetGenerators();
	}

	//runs the search for roughly the given number of milliseconds
	public void step( long budgetMs ){
		if (!running) return;

		long end = System.currentTimeMillis() + budgetMs;
		while (running && System.currentTimeMillis() < end){
			try {
				if (curWhere == null){
					rejected = false;
					beginSeed();
					if (rejected){
						finishSeed();
						continue;
					}
				}
				generateNextLevel();
			} catch (Throwable t){
				//a broken seed shouldn't stop the whole search
				Game.reportException(t);
				curWhere = null;
				seedsChecked++;
			}

			if (results.size() >= resultLimit){
				stop();
			}
		}
	}

	private void beginSeed(){
		protectState();
		if (GamesInProgress.selectedClass == null){
			GamesInProgress.selectedClass = HeroClass.WARRIOR;
		}
		curSeed = forcedSeed >= 0 ? forcedSeed : DungeonSeed.randomSeed();
		Dungeon.seed = curSeed;
		Dungeon.daily = Dungeon.dailyReplay = false;
		Dungeon.customSeedText = "";
		Dungeon.init();
		//not cleared by the game's own quest reset, would otherwise leak between seeds
		Imp.Quest.rewardOptions.clear();

		curDepth = 1;
		curWhere = new String[targets.size()];
		ghostDone = wandDone = impDone = smithDone = false;
		rollCatalystTrinkets();

		//trinkets only come from the catalyst, which is decided before any floor exists.
		// if a wanted trinket isn't among its options, no floor needs to be generated at all
		for (int i = 0; i < targets.size(); i++){
			if (curWhere[i] == null && com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket.class.isAssignableFrom(targets.get(i))){
				rejected = true;
				return;
			}
		}
	}

	private boolean rejected = false;

	private void generateNextLevel(){
		Dungeon.depth = curDepth;
		Dungeon.branch = 0;
		Level level = Dungeon.newLevel();

		scanLevel(level, curDepth);

		if (curWhere.length > 0 && allFound()){
			Result r = new Result();
			r.seed = curSeed;
			r.code = DungeonSeed.convertToCode(curSeed);
			r.where = curWhere.clone();
			results.add(r);
			finishSeed();
			return;
		}

		curDepth++;
		if (curDepth > maxDepth){
			finishSeed();
		}
	}

	private void finishSeed(){
		seedsChecked++;
		curWhere = null;
		Actor.clear();
		Dungeon.level = null;
	}

	//the trinket catalyst (always found on floors 1-3) offers 4 trinkets when used.
	// they come from the trinket deck, which has its own seed and isn't touched by level
	// generation, so the first 4 draws are exactly what the catalyst will offer.
	private void rollCatalystTrinkets(){
		for (int k = 0; k < 4; k++){
			Item t = com.shatteredpixel.shatteredpixeldungeon.items.Generator.random(
					com.shatteredpixel.shatteredpixeldungeon.items.Generator.Category.TRINKET);
			check(t, 0, "catalyst");
		}
	}

	private boolean allFound(){
		for (String s : curWhere){
			if (s == null) return false;
		}
		return true;
	}

	private void scanLevel( Level level, int depth ){
		for (Heap heap : level.heaps.valueList()){
			String src;
			switch (heap.type){
				case FOR_SALE:      src = "shop"; break;
				case CHEST:         src = "chest"; break;
				case LOCKED_CHEST:  src = "locked"; break;
				case CRYSTAL_CHEST: src = "crystal"; break;
				case TOMB:          src = "tomb"; break;
				case SKELETON: case REMAINS: src = "remains"; break;
				default:            src = "floor"; break;
			}
			for (Item item : heap.items){
				check(item, depth, src);
			}
		}

		for (Mob mob : level.mobs){
			if (mob instanceof Mimic && ((Mimic) mob).items != null){
				for (Item item : ((Mimic) mob).items){
					check(item, depth, "mimic");
				}
			} else if (mob instanceof ArmoredStatue){
				check(((ArmoredStatue) mob).armor(), depth, "statue");
				check(((Statue) mob).weapon(), depth, "statue");
			} else if (mob instanceof Statue){
				check(((Statue) mob).weapon(), depth, "statue");
			} else if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RotHeart){
				//the rot garden: its heart always drops a rotberry seed when killed
				check(new com.shatteredpixel.shatteredpixeldungeon.plants.Rotberry.Seed(), depth, "rotheart");
			}
		}

		//quest rewards become known when the quest giver spawns
		if (!ghostDone && (Ghost.Quest.weapon != null || Ghost.Quest.armor != null)){
			ghostDone = true;
			check(Ghost.Quest.weapon, depth, "ghost");
			check(Ghost.Quest.armor, depth, "ghost");
		}
		if (!wandDone && (Wandmaker.Quest.wand1 != null || Wandmaker.Quest.wand2 != null)){
			wandDone = true;
			check(Wandmaker.Quest.wand1, depth, "wandmaker");
			check(Wandmaker.Quest.wand2, depth, "wandmaker");
		}
		if (!impDone && Imp.Quest.rewardOptions != null && !Imp.Quest.rewardOptions.isEmpty()){
			impDone = true;
			checkAll(Imp.Quest.rewardOptions, depth, "imp");
		}
		if (!impDone && Imp.Quest.reward != null){
			impDone = true;
			check(Imp.Quest.reward, depth, "imp");
		}
		if (!smithDone && Blacksmith.Quest.smithRewards != null && !Blacksmith.Quest.smithRewards.isEmpty()){
			smithDone = true;
			checkAll(Blacksmith.Quest.smithRewards, depth, "smith");
		}
	}

	//quest rewards get their enchantment when handed over, it is rolled in advance though
	private static Class<?> enchantOf( Item item, String src ){
		if (item instanceof Weapon){
			Weapon.Enchantment e = ((Weapon) item).enchantment;
			if (src.equals("ghost") && Ghost.Quest.enchant != null) e = Ghost.Quest.enchant;
			if (src.equals("smith") && Blacksmith.Quest.smithEnchant != null) e = Blacksmith.Quest.smithEnchant;
			return e == null ? null : e.getClass();
		}
		if (item instanceof Armor){
			Armor.Glyph g = ((Armor) item).glyph;
			if (src.equals("ghost") && Ghost.Quest.glyph != null) g = Ghost.Quest.glyph;
			if (src.equals("smith") && Blacksmith.Quest.smithGlyph != null) g = Blacksmith.Quest.smithGlyph;
			return g == null ? null : g.getClass();
		}
		return null;
	}

	private void checkAll( Collection<Item> items, int depth, String src ){
		for (Item i : items) check(i, depth, src);
	}

	private void check( Item item, int depth, String src ){
		if (item == null) return;
		if (dumpLog != null){
			dumpLog.add(depth + ":" + src + ":" + item.getClass().getSimpleName() + ":" + item.level() + (item.cursed ? ":c" : ""));
		}
		boolean levelled = item instanceof Weapon || item instanceof Armor
				|| item instanceof Wand || item instanceof Ring;
		Class<?> ench = enchantOf(item, src);

		//a stack can satisfy several targets of the same kind (e.g. two potions of strength)
		int remaining = Math.max(1, item.quantity());
		for (int i = 0; i < targets.size() && remaining > 0; i++){
			if (curWhere[i] != null) continue;
			if (item.getClass() != targets.get(i)) continue;

			Class<?> want = i < enchants.size() ? enchants.get(i) : null;
			int minLvl = i < levels.size() ? levels.get(i) : 0;
			if (levelled && item.level() < minLvl) continue;
			if (want != null && ench != want) continue;
			//a curse that was asked for is fine, any other cursed item is skipped if requested
			if (noCursed && item.cursed && !isCurse(want)) continue;

			String lvl = levelled && item.level() > 0 ? " +" + item.level() : "";
			curWhere[i] = depth + "|" + src + "|" + lvl;
			remaining--;
		}
	}
}
