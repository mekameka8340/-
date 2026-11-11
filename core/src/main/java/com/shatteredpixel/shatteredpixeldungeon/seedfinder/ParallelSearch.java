/*
 * Seed finder - searching in several worker processes at once
 *
 * The game keeps its level generation state in static fields, so only one level can be
 * generated per process. Platforms that can start extra processes (Android) provide a backend,
 * each worker searches independently and reports back here.
 */

package com.shatteredpixel.shatteredpixeldungeon.seedfinder;

public abstract class ParallelSearch {

	//set by the platform launcher, null when parallel search isn't available
	public static ParallelSearch backend = null;

	public interface Listener {
		//total seeds checked so far by one worker
		void progress( int worker, int checked );
		void result( long seed, String[] where );
		void workerFailed( int worker );
	}

	public abstract int workerCount();

	public abstract void start( String spec, Listener listener );

	public abstract void stop();

	// ---------------------------------------------------------------- shared by worker processes

	//encodes a result for sending between processes
	public static String encodeResult( long seed, String[] where ){
		StringBuilder sb = new StringBuilder();
		sb.append(seed);
		for (String w : where) sb.append('\n').append(w);
		return sb.toString();
	}

	public static long decodeSeed( String text ){
		int i = text.indexOf('\n');
		return Long.parseLong(i < 0 ? text : text.substring(0, i));
	}

	public static String[] decodeWhere( String text ){
		String[] lines = text.split("\n", -1);
		String[] where = new String[lines.length - 1];
		System.arraycopy(lines, 1, where, 0, where.length);
		return where;
	}

	//the search loop a worker process runs. it calls back with progress and results until stopped
	public static class WorkerLoop {

		public interface Sink {
			void progress( int checked );
			void result( String encoded );
		}

		private volatile boolean stopped = false;

		public void stop(){
			stopped = true;
		}

		public void run( String specText, Sink sink ){
			SearchSpec spec = SearchSpec.decode(specText);
			spec.applyGameSettings();
			SeedFinder f = spec.makeFinder();
			f.resultLimit = Integer.MAX_VALUE;
			f.start();
			long lastProgress = 0;
			while (!stopped && f.running()){
				f.step(150);
				//results are sent as they come, the main process decides when enough are found
				for (SeedFinder.Result r : f.results){
					sink.result(encodeResult(r.seed, r.where));
				}
				f.results.clear();
				long now = System.currentTimeMillis();
				if (now - lastProgress > 250){
					lastProgress = now;
					sink.progress(f.seedsChecked);
				}
			}
			sink.progress(f.seedsChecked);
			f.stop();
		}
	}
}
