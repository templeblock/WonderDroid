
package uk.org.cardboardbox.wonderdroid.utils;

import uk.org.cardboardbox.wonderdroid.Button;
import android.graphics.Canvas;
import android.os.SystemClock;

import android.view.SurfaceHolder;

public class EmuThread extends Thread {

	@SuppressWarnings("unused")
	private static final String TAG = EmuThread.class.getSimpleName();

	public static interface Renderer {
		public void start ();

		public void setButtons (Button[] buttons);

		public void showButtons (boolean show);

		public void update (boolean skip);

		public void render (Canvas c);
	}

	private Renderer renderer;

	private static final int TARGETFRAMETIME = (int)Math.round(1000 / 75.47);

	private boolean mIsRunning = false;
	private boolean isPaused = false;

	private SurfaceHolder mSurfaceHolder;

	private Canvas c;

	private int frame;
	private long frameStart;
	private long frameEnd;
	private int realRuntime;
	private int emulatedRuntime;
	private int frametime;

	boolean skip = false;
	boolean behind = false;

	public EmuThread (Renderer renderer) {
		this.renderer = renderer;
	}

	public void setSurfaceHolder (SurfaceHolder sh) {
		mSurfaceHolder = sh;
	}

	public void pause () {
		isPaused = true;
	}

	public void unpause () {
		isPaused = false;
		// if (WonderSwan.audio.getState() == AudioTrack.PLAYSTATE_PAUSED) {
		// WonderSwan.audio.play();
		// }
	}

	@Override
	public void run () {

		while (mSurfaceHolder == null) {
			SystemClock.sleep(20);
		}

		while (mIsRunning) {

			if (isPaused) {
				SystemClock.sleep(TARGETFRAMETIME);
			} else {

				skip = behind || frame % 3 == 0;

				frameStart = System.currentTimeMillis();
				renderer.update(skip);

				if (!skip) {
					c = null;
					try {
						c = mSurfaceHolder.lockCanvas();
						synchronized (mSurfaceHolder) {
							renderer.render(c);
						}
					} finally {
						if (c != null) {
							mSurfaceHolder.unlockCanvasAndPost(c);
						}
					}
				}

				frameEnd = System.currentTimeMillis();
				frametime = (int)(frameEnd - frameStart);
				realRuntime += frametime;
				emulatedRuntime += TARGETFRAMETIME;

				if (realRuntime <= emulatedRuntime) {
					behind = false;
				} else {
					// behind = true;
				}

				frame++;
			}

		}

		synchronized (this) {
			notifyAll();
		}

	}

	public boolean isRunning () {
		return mIsRunning;
	}

	public void setRunning () {
		mIsRunning = true;
		renderer.start();
	}

	public void clearRunning () {
		mIsRunning = false;
	}

}
