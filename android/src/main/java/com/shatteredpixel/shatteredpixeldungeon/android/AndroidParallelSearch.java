/*
 * Starts seed finder worker processes and relays what they find
 */

package com.shatteredpixel.shatteredpixeldungeon.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.shatteredpixel.shatteredpixeldungeon.seedfinder.ParallelSearch;
import com.watabou.noosa.Game;

import java.util.ArrayList;

public class AndroidParallelSearch extends ParallelSearch {

	private final Context context;
	private final int count;

	private final ArrayList<Connection> connections = new ArrayList<>();
	private int generation = 0;

	public AndroidParallelSearch( Context context ){
		this.context = context.getApplicationContext();
		//leave a couple of cores for the game itself
		int cores = Runtime.getRuntime().availableProcessors();
		count = Math.max(1, Math.min(SeedFinderService.WORKERS.length, cores - 2));
	}

	@Override
	public int workerCount() {
		return count;
	}

	@Override
	public synchronized void start( final String spec, final Listener listener ) {
		stop();
		final int gen = ++generation;

		for (int i = 0; i < count; i++){
			final int index = i;
			Connection c = new Connection(index, gen, spec, listener);
			connections.add(c);
			Intent intent = new Intent(context, SeedFinderService.WORKERS[i]);
			boolean ok = false;
			try {
				ok = context.bindService(intent, c, Context.BIND_AUTO_CREATE);
			} catch (Exception ignored){}
			if (!ok) listener.workerFailed(index);
		}
	}

	@Override
	public synchronized void stop() {
		generation++;
		for (Connection c : connections){
			c.stopAndUnbind();
		}
		connections.clear();
	}

	private class Connection implements ServiceConnection {
		final int index, gen;
		final String spec;
		final Listener listener;
		Messenger service;
		boolean bound = true;

		final Messenger reply = new Messenger(new Handler(Looper.getMainLooper()){
			@Override
			public void handleMessage(Message msg) {
				//ignore anything from an older search
				if (gen != generation) return;
				switch (msg.what){
					case SeedFinderService.MSG_PROGRESS:
						listener.progress(index, msg.arg1);
						break;
					case SeedFinderService.MSG_RESULT:
						String data = msg.getData().getString(SeedFinderService.KEY_DATA);
						if (data != null){
							listener.result(decodeSeed(data), decodeWhere(data));
						}
						break;
					case SeedFinderService.MSG_FAILED:
						listener.workerFailed(index);
						break;
				}
			}
		});

		Connection( int index, int gen, String spec, Listener listener ){
			this.index = index;
			this.gen = gen;
			this.spec = spec;
			this.listener = listener;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			service = new Messenger(binder);
			Message m = Message.obtain(null, SeedFinderService.MSG_START);
			Bundle b = new Bundle();
			b.putString(SeedFinderService.KEY_SPEC, spec);
			b.putString(SeedFinderService.KEY_VERSION, Game.version);
			b.putInt(SeedFinderService.KEY_VCODE, Game.versionCode);
			m.setData(b);
			m.replyTo = reply;
			try {
				service.send(m);
			} catch (RemoteException e){
				listener.workerFailed(index);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			//the worker process died
			service = null;
			if (gen == generation) listener.workerFailed(index);
		}

		void stopAndUnbind(){
			if (service != null){
				try {
					service.send(Message.obtain(null, SeedFinderService.MSG_STOP));
				} catch (RemoteException ignored){}
			}
			if (bound){
				bound = false;
				try {
					context.unbindService(this);
				} catch (Exception ignored){}
			}
		}
	}
}
