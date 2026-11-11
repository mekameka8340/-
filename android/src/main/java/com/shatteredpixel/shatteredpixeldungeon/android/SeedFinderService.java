/*
 * Seed finder worker. Each subclass is declared in its own process in the manifest,
 * so every worker has its own copy of the game's level generation state.
 */

package com.shatteredpixel.shatteredpixeldungeon.android;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.DefaultAndroidFiles;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.shatteredpixel.shatteredpixeldungeon.seedfinder.ParallelSearch;
import com.shatteredpixel.shatteredpixeldungeon.seedfinder.SearchSpec;

public class SeedFinderService extends Service {

	public static final int MSG_START    = 1;
	public static final int MSG_STOP     = 2;
	public static final int MSG_PROGRESS = 3;
	public static final int MSG_RESULT   = 4;
	public static final int MSG_FAILED   = 5;

	public static final String KEY_SPEC    = "spec";
	public static final String KEY_VERSION = "version";
	public static final String KEY_VCODE   = "vcode";
	public static final String KEY_DATA    = "data";

	public static final Class<?>[] WORKERS = {
			Worker1.class, Worker2.class, Worker3.class, Worker4.class, Worker5.class, Worker6.class
	};

	public static class Worker1 extends SeedFinderService {}
	public static class Worker2 extends SeedFinderService {}
	public static class Worker3 extends SeedFinderService {}
	public static class Worker4 extends SeedFinderService {}
	public static class Worker5 extends SeedFinderService {}
	public static class Worker6 extends SeedFinderService {}

	private static boolean initialized = false;

	private HandlerThread searchThread;
	private Handler searchHandler;
	private ParallelSearch.WorkerLoop loop;
	private Messenger messenger;

	@Override
	public void onCreate() {
		super.onCreate();
		searchThread = new HandlerThread("seedfinder");
		searchThread.start();
		searchHandler = new Handler(searchThread.getLooper());

		messenger = new Messenger(new Handler(Looper.getMainLooper()){
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what){
					case MSG_START:
						startSearch(msg.getData(), msg.replyTo);
						break;
					case MSG_STOP:
						if (loop != null) loop.stop();
						break;
				}
			}
		});
	}

	private void startSearch( Bundle data, final Messenger reply ){
		if (loop != null) loop.stop();
		final String spec = data.getString(KEY_SPEC);
		final String version = data.getString(KEY_VERSION, "???");
		final int vcode = data.getInt(KEY_VCODE, 0);
		final ParallelSearch.WorkerLoop myLoop = new ParallelSearch.WorkerLoop();
		loop = myLoop;

		searchHandler.post(new Runnable() {
			@Override
			public void run() {
				try {
					if (!initialized){
						GdxNativesLoader.load();
						Gdx.files = new DefaultAndroidFiles(getAssets(), SeedFinderService.this, true);
						SearchSpec.headlessSetup(version, vcode);
						initialized = true;
					}
					myLoop.run(spec, new ParallelSearch.WorkerLoop.Sink() {
						@Override
						public void progress(int checked) {
							Message m = Message.obtain(null, MSG_PROGRESS, checked, 0);
							send(reply, m);
						}

						@Override
						public void result(String encoded) {
							Message m = Message.obtain(null, MSG_RESULT);
							Bundle b = new Bundle();
							b.putString(KEY_DATA, encoded);
							m.setData(b);
							send(reply, m);
						}
					});
				} catch (Throwable t){
					t.printStackTrace();
					send(reply, Message.obtain(null, MSG_FAILED));
				}
			}
		});
	}

	private static void send( Messenger to, Message m ){
		if (to == null) return;
		try {
			to.send(m);
		} catch (RemoteException ignored){}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return messenger.getBinder();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		if (loop != null) loop.stop();
		return false;
	}

	@Override
	public void onDestroy() {
		if (loop != null) loop.stop();
		searchThread.quitSafely();
		super.onDestroy();
	}
}
