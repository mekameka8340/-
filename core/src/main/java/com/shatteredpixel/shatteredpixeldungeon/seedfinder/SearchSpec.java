/*
 * Seed finder - everything a search needs, in a form that can be sent to worker processes
 */

package com.shatteredpixel.shatteredpixeldungeon.seedfinder;

import com.badlogic.gdx.Preferences;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.noosa.Game;
import com.watabou.utils.GameSettings;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SearchSpec {

	public String heroClass;
	public int challenges;
	public int maxDepth;
	public boolean noCursed;
	public final ArrayList<String> targets = new ArrayList<>();
	public final ArrayList<String> enchants = new ArrayList<>(); //"" for any
	public final ArrayList<Integer> levels = new ArrayList<>();
	//journal progress and the intro setting, both change what level generation drops
	public String journal = "";
	public boolean intro = false;

	public static SearchSpec from( SeedFinder f ){
		SearchSpec s = new SearchSpec();
		HeroClass cls = GamesInProgress.selectedClass != null ? GamesInProgress.selectedClass : HeroClass.WARRIOR;
		s.heroClass = cls.name();
		s.challenges = SPDSettings.challenges();
		s.maxDepth = f.maxDepth;
		s.noCursed = f.noCursed;
		s.journal = journalState();
		s.intro = SPDSettings.intro();
		for (int i = 0; i < f.targets.size(); i++){
			s.targets.add(f.targets.get(i).getName());
			Class<?> e = i < f.enchants.size() ? f.enchants.get(i) : null;
			s.enchants.add(e == null ? "" : e.getName());
			s.levels.add(i < f.levels.size() ? f.levels.get(i) : 0);
		}
		return s;
	}

	//one line of text, so it can be passed through any channel
	public String encode(){
		StringBuilder sb = new StringBuilder();
		sb.append(heroClass).append(';').append(challenges).append(';').append(maxDepth).append(';').append(noCursed ? 1 : 0)
				.append(';').append(intro ? 1 : 0).append(';').append(journal);
		for (int i = 0; i < targets.size(); i++){
			sb.append(';').append(targets.get(i)).append(',').append(enchants.get(i)).append(',').append(levels.get(i));
		}
		return sb.toString();
	}

	public static SearchSpec decode( String text ){
		String[] parts = text.split(";", -1);
		SearchSpec s = new SearchSpec();
		s.heroClass = parts[0];
		s.challenges = Integer.parseInt(parts[1]);
		s.maxDepth = Integer.parseInt(parts[2]);
		s.noCursed = parts[3].equals("1");
		s.intro = parts[4].equals("1");
		s.journal = parts[5];
		for (int i = 6; i < parts.length; i++){
			String[] t = parts[i].split(",", -1);
			s.targets.add(t[0]);
			s.enchants.add(t[1]);
			s.levels.add(Integer.parseInt(t[2]));
		}
		return s;
	}

	//sets up a seed finder for this spec, in the current process
	@SuppressWarnings("unchecked")
	public SeedFinder makeFinder(){
		SeedFinder f = new SeedFinder();
		f.maxDepth = maxDepth;
		f.noCursed = noCursed;
		for (int i = 0; i < targets.size(); i++){
			Class<? extends Item> cls = (Class<? extends Item>) Reflection.forName(targets.get(i));
			Class<?> ench = enchants.get(i).isEmpty() ? null : Reflection.forName(enchants.get(i));
			if (cls != null) f.addTarget(cls, ench, levels.get(i));
		}
		return f;
	}

	//applies hero class and challenges, so level generation matches a real game with these settings
	public void applyGameSettings(){
		try {
			GamesInProgress.selectedClass = HeroClass.valueOf(heroClass);
		} catch (Exception e){
			GamesInProgress.selectedClass = HeroClass.WARRIOR;
		}
		GameSettings.put(SPDSettings.KEY_CHALLENGES, challenges);
		GameSettings.put(SPDSettings.KEY_INTRO, intro);
		applyJournalState(journal);
	}

	// ---------------------------------------------------------------- journal snapshot

	//every document page and its state (0 not found, 1 found, 2 read), as text
	public static String journalState(){
		StringBuilder sb = new StringBuilder();
		for (com.shatteredpixel.shatteredpixeldungeon.journal.Document doc : com.shatteredpixel.shatteredpixeldungeon.journal.Document.values()){
			for (String page : doc.pageNames()){
				int st = doc.isPageRead(page) ? 2 : (doc.isPageFound(page) ? 1 : 0);
				if (sb.length() > 0) sb.append('|');
				sb.append(doc.name()).append(':').append(page).append(':').append(st);
			}
		}
		return sb.toString();
	}

	public static void applyJournalState( String state ){
		if (state == null || state.isEmpty()) return;
		for (String entry : state.split("\\|")){
			String[] p = entry.split(":");
			if (p.length != 3) continue;
			com.shatteredpixel.shatteredpixeldungeon.journal.Document doc;
			try {
				doc = com.shatteredpixel.shatteredpixeldungeon.journal.Document.valueOf(p[0]);
			} catch (Exception e){
				continue;
			}
			int st = Integer.parseInt(p[2]);
			doc.deletePage(p[1]);
			if (st >= 1) doc.findPage(p[1]);
			if (st >= 2) doc.readPage(p[1]);
		}
	}

	// ---------------------------------------------------------------- running without the game itself

	//prepares the minimum needed to generate levels in a process that has no game window
	public static void headlessSetup( String version, int versionCode ){
		Game.version = version;
		Game.versionCode = versionCode;
		GameSettings.set(new MemoryPreferences());
		com.shatteredpixel.shatteredpixeldungeon.Badges.setEmptyGlobal();
	}

	public static class MemoryPreferences implements Preferences {
		private final HashMap<String, Object> map = new HashMap<>();

		@Override public Preferences putBoolean(String k, boolean v){ map.put(k, v); return this; }
		@Override public Preferences putInteger(String k, int v){ map.put(k, v); return this; }
		@Override public Preferences putLong(String k, long v){ map.put(k, v); return this; }
		@Override public Preferences putFloat(String k, float v){ map.put(k, v); return this; }
		@Override public Preferences putString(String k, String v){ map.put(k, v); return this; }
		@Override public Preferences put(Map<String, ?> vals){ map.putAll(vals); return this; }
		@Override public boolean getBoolean(String k){ return getBoolean(k, false); }
		@Override public int getInteger(String k){ return getInteger(k, 0); }
		@Override public long getLong(String k){ return getLong(k, 0); }
		@Override public float getFloat(String k){ return getFloat(k, 0); }
		@Override public String getString(String k){ return getString(k, ""); }
		@Override public boolean getBoolean(String k, boolean d){ Object o = map.get(k); return o instanceof Boolean ? (Boolean)o : d; }
		@Override public int getInteger(String k, int d){ Object o = map.get(k); return o instanceof Number ? ((Number)o).intValue() : d; }
		@Override public long getLong(String k, long d){ Object o = map.get(k); return o instanceof Number ? ((Number)o).longValue() : d; }
		@Override public float getFloat(String k, float d){ Object o = map.get(k); return o instanceof Number ? ((Number)o).floatValue() : d; }
		@Override public String getString(String k, String d){ Object o = map.get(k); return o instanceof String ? (String)o : d; }
		@Override public Map<String, ?> get(){ return map; }
		@Override public boolean contains(String k){ return map.containsKey(k); }
		@Override public void clear(){ map.clear(); }
		@Override public void remove(String k){ map.remove(k); }
		@Override public void flush(){}
	}
}
