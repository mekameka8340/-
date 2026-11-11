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

package com.watabou.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.watabou.noosa.Game;

public class GameSettings {
	
	public static final String DEFAULT_PREFS_FILE = "settings.xml";
	
	private static Preferences prefs;
	
	private static Preferences get() {
		if (prefs == null) {
			prefs = Gdx.app.getPreferences( DEFAULT_PREFS_FILE );
		}
		return prefs;
	}
	
	//allows setting up of preferences directly during game initialization
	public static void set( Preferences prefs ){
		GameSettings.prefs = prefs;
	}
	
	public static boolean contains( String key ){
		return get().contains( key );
	}
	
	public static int getInt( String key, int defValue ) {
		return getInt(key, defValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}
	
	public static int getInt( String key, int defValue, int min, int max ) {
		try {
			int i = get().getInteger( key, defValue );
			if (i < min || i > max){
				int val = (int)GameMath.gate(min, i, max);
				put(key, val);
				return val;
			} else {
				return i;
			}
		} catch (Exception e) {
			Game.reportException(e);
			put(key, defValue);
			return defValue;
		}
	}

	public static long getLong( String key, long defValue ) {
		return getLong(key, defValue, Long.MIN_VALUE, Long.MAX_VALUE);
	}

	public static long getLong( String key, long defValue, long min, long max ) {
		try {
			long i = get().getLong( key, defValue );
			if (i < min || i > max){
				long val = (long)GameMath.gate(min, i, max);
				put(key, val);
				return val;
			} else {
				return i;
			}
		} catch (Exception e) {
			Game.reportException(e);
			put(key, defValue);
			return defValue;
		}
	}
	
	public static boolean getBoolean( String key, boolean defValue ) {
		try {
			return get().getBoolean(key, defValue);
		} catch (Exception e) {
			Game.reportException(e);
			return defValue;
		}
	}
	
	public static String getString( String key, String defValue ) {
		return getString(key, defValue, Integer.MAX_VALUE);
	}
	
	public static String getString( String key, String defValue, int maxLength ) {
		try {
			String s = get().getString( key, defValue );
			if (s != null && s.length() > maxLength) {
				put(key, defValue);
				return defValue;
			} else {
				return s;
			}
		} catch (Exception e) {
			Game.reportException(e);
			put(key, defValue);
			return defValue;
		}
	}
	
	public static void put( String key, int value ) {
		get().putInteger(key, value);
		get().flush();
	}

	public static void put( String key, long value ) {
		get().putLong(key, value);
		get().flush();
	}
	
	public static void put( String key, boolean value ) {
		get().putBoolean(key, value);
		get().flush();
	}
	
	public static void put( String key, String value ) {
		get().putString(key, value);
		get().flush();
	}
	
	//animation speeds, x1 to x10: actions (attacks, casting...), walking, and projectiles.
	//Read every frame, so kept in memory and only written to the settings file when changed
	public static final String KEY_FRAME_SPEED = "frame_speed";
	public static final String KEY_MOVE_SPEED = "move_anim_speed";
	public static final String KEY_MISSILE_SPEED = "missile_speed";
	private static int frameSpeed = -1, moveSpeed = -1, missileSpeed = -1;

	public static int frameSpeed(){
		if (frameSpeed < 0) frameSpeed = getInt(KEY_FRAME_SPEED, 1, 1, 10);
		return frameSpeed;
	}

	public static void frameSpeed( int value ){
		frameSpeed = (int)GameMath.gate(1, value, 10);
		put(KEY_FRAME_SPEED, frameSpeed);
	}

	public static int moveSpeed(){
		if (moveSpeed < 0) moveSpeed = getInt(KEY_MOVE_SPEED, 1, 1, 10);
		return moveSpeed;
	}

	public static void moveSpeed( int value ){
		moveSpeed = (int)GameMath.gate(1, value, 10);
		put(KEY_MOVE_SPEED, moveSpeed);
	}

	//whether the standing (idle) animation follows the action speed too
	public static final String KEY_IDLE_FAST = "idle_anim_fast";
	private static int idleFast = -1;

	public static boolean idleFast(){
		if (idleFast < 0) idleFast = getBoolean(KEY_IDLE_FAST, false) ? 1 : 0;
		return idleFast == 1;
	}

	public static void idleFast( boolean value ){
		idleFast = value ? 1 : 0;
		put(KEY_IDLE_FAST, value);
	}

	public static int missileSpeed(){
		if (missileSpeed < 0) missileSpeed = getInt(KEY_MISSILE_SPEED, frameSpeed(), 1, 10);
		return missileSpeed;
	}

	public static void missileSpeed( int value ){
		missileSpeed = (int)GameMath.gate(1, value, 10);
		put(KEY_MISSILE_SPEED, missileSpeed);
	}

}
