/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.VialOfBlood;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

public class Dewdrop extends Item {
	
	{
		image = ItemSpriteSheet.DEWDROP;
		
		stackable = true;
		dropsDownHeap = true;
	}
	
	@Override
	public boolean doPickUp(Hero hero, int pos) {
		
		Waterskin flask = hero.belongings.getItem( Waterskin.class );
		Catalog.setSeen(getClass());
		Statistics.itemTypesDiscovered.add(getClass());
		
		if (flask != null && !flask.isFull()){

			flask.collectDew( this );
			GameScene.pickUp( this, pos );

		} else {

			int terr = Dungeon.level.map[pos];
			if (!consumeDew(1, hero, terr == Terrain.ENTRANCE || terr == Terrain.ENTRANCE_SP
					|| terr == Terrain.EXIT || terr == Terrain.UNLOCKED_EXIT)){
				return false;
			} else {
				Catalog.countUse(getClass());
			}
			
		}
		
		Sample.INSTANCE.play( Assets.Sounds.DEWDROP );
		hero.spendAndNext( pickupDelay() );
		
		return true;
	}

	// ---------------------------------------------------------------- collecting dew on its own (player setting)

	//the cell the hero is stepping onto by walking, on their own turn (-1 otherwise). Only such a step collects
	// on its own: being pushed, pulled, teleported, leaping or falling onto dew or into grass takes nothing,
	// so no turn is ever added to an ability, a knockback or another character's turn
	public static int heroStep = -1;

	public static boolean steppingOnto( int pos ){
		return heroStep != -1 && heroStep == pos && Dungeon.hero != null && Dungeon.hero.pos == pos;
	}

	//whether dew is collected on its own right now: off, unless an enemy is hunting the hero, or unless one is in sight
	public static boolean autoCollectNow(){
		Hero hero = Dungeon.hero;
		if (hero == null || Dungeon.level == null) return false;
		switch (com.shatteredpixel.shatteredpixeldungeon.SPDSettings.autoDewMode()){
			default: case com.shatteredpixel.shatteredpixeldungeon.SPDSettings.DEW_UNLESS_SEEN:
				return hero.visibleEnemies() == 0;
			case com.shatteredpixel.shatteredpixeldungeon.SPDSettings.DEW_UNLESS_HUNTED:
				//any enemy on the floor that is hunting the hero, seen or not
				for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m : Dungeon.level.mobs.toArray(new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob[0])){
					if (m.isAlive() && m.alignment == com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY
							&& m.state == m.HUNTING && m.isTargeting(hero)){
						return false;
					}
				}
				return true;
		}
	}

	//a seed the hero comes across goes into the bag, by the same rule as dew and at the same cost of a turn.
	//Returns whether it was taken (the caller removes it from where it was)
	public static boolean takeSeed( Hero hero, Item seed, int pos ){
		if (!com.shatteredpixel.shatteredpixeldungeon.SPDSettings.autoSeeds() || !autoCollectNow()) return false;
		if (com.shatteredpixel.shatteredpixeldungeon.Challenges.isItemBlocked(seed)) return false;
		if (!seed.collect(hero.belongings.backpack)) return false;
		GameScene.pickUp(seed, pos);
		Sample.INSTANCE.play( Assets.Sounds.ITEM );
		GLog.i( Messages.capitalize(Messages.get(Hero.class, "you_now_have", seed.name())) );
		hero.spend(1f);
		return true;
	}

	//the hero walked onto a cell: dew (or a seed) already lying there is taken along the way, if the setting allows.
	//Like picking it up by hand it takes a turn, added to the step
	public static void walkedOver( Hero hero, int pos ){
		if (!steppingOnto(pos)) return;
		//only once per step
		heroStep = -1;
		if (!com.shatteredpixel.shatteredpixeldungeon.SPDSettings.autoDewFloor() || !autoCollectNow()) return;
		//walking there to pick it up anyway
		if (hero.curAction instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction.PickUp
				&& hero.curAction.dst == pos) return;
		com.shatteredpixel.shatteredpixeldungeon.items.Heap heap = Dungeon.level.heaps.get(pos);
		if (heap == null || heap.type != com.shatteredpixel.shatteredpixeldungeon.items.Heap.Type.HEAP || heap.items.isEmpty()) return;
		if (heap.peek() instanceof com.shatteredpixel.shatteredpixeldungeon.plants.Plant.Seed){
			Item seed = heap.peek();
			if (takeSeed(hero, seed, pos)) heap.pickUp();
			return;
		}
		if (!(heap.peek() instanceof Dewdrop) || !com.shatteredpixel.shatteredpixeldungeon.SPDSettings.autoDew()) return;
		Waterskin flask = hero.belongings.getItem( Waterskin.class );
		if (flask == null || flask.isFull()) return;
		Dewdrop dew = (Dewdrop) heap.peek();
		if (com.shatteredpixel.shatteredpixeldungeon.Challenges.isItemBlocked(dew)) return;

		Catalog.setSeen(Dewdrop.class);
		Statistics.itemTypesDiscovered.add(Dewdrop.class);
		heap.pickUp();
		flask.collectDew(dew);
		GameScene.pickUp(dew, pos);
		Sample.INSTANCE.play( Assets.Sounds.DEWDROP );
		hero.spend(1f);
	}

	public static boolean consumeDew(int quantity, Hero hero, boolean force){
		//20 drops for a full heal
		int effect = Math.round( hero.HT * 0.05f * quantity );

		int heal = Math.min( hero.HT - hero.HP, effect );

		int shield = 0;
		if (hero.hasTalent(Talent.SHIELDING_DEW)){

			//When vial is present, this allocates exactly as much of the effect as is needed
			// to get to 100% HP, and the rest is then given as shielding (without the vial boost)
			if (quantity > 1 && heal < effect && VialOfBlood.delayBurstHealing()){
				heal = Math.round(heal/VialOfBlood.totalHealMultiplier());
			}

			shield = effect - heal;

			int maxShield = Math.round(hero.HT *0.2f*hero.pointsInTalent(Talent.SHIELDING_DEW));
			int curShield = 0;
			if (hero.buff(Barrier.class) != null) curShield = hero.buff(Barrier.class).shielding();
			shield = Math.min(shield, maxShield-curShield);
		}

		if (heal > 0 || shield > 0) {

			if (heal > 0 && quantity > 1 && VialOfBlood.delayBurstHealing()){
				Healing healing = Buff.affect(hero, Healing.class);
				healing.setHeal(heal, 0, VialOfBlood.maxHealPerTurn(), true);
			} else {
				hero.HP += heal;
				if (heal > 0){
					hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
				}
			}

			if (shield > 0) {
				Buff.affect(hero, Barrier.class).incShield(shield);
				hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING );
			}

		} else if (!force) {
			GLog.i( Messages.get(Dewdrop.class, "already_full") );
			return false;
		}

		return true;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	//max of one dew in a stack

	@Override
	public Item merge( Item other ){
		if (isSimilar( other )){
			quantity = 1;
			other.quantity = 0;
		}
		return this;
	}

	@Override
	public Item quantity(int value) {
		quantity = Math.min( value, 1);
		return this;
	}

}
