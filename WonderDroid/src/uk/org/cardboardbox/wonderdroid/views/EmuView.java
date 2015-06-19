
package uk.org.cardboardbox.wonderdroid.views;

import uk.org.cardboardbox.wonderdroid.Button;
import uk.org.cardboardbox.wonderdroid.R;
import uk.org.cardboardbox.wonderdroid.TouchInputHandler;
import uk.org.cardboardbox.wonderdroid.TouchInputHandler.Pointer;
import uk.org.cardboardbox.wonderdroid.WonderSwan;
import uk.org.cardboardbox.wonderdroid.WonderSwan.WonderSwanButton;
import uk.org.cardboardbox.wonderdroid.WonderSwanRenderer;
import uk.org.cardboardbox.wonderdroid.utils.EmuThread;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;

import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class EmuView extends SurfaceView implements SurfaceHolder.Callback {

	private final static String TAG = EmuView.class.getSimpleName();
	@SuppressWarnings("unused")
	private final static boolean debug = true;
	private boolean mPaused = false;

	private EmuThread mThread;
	private final WonderSwanRenderer renderer;
	private boolean controlsVisible = false;
	private final GradientDrawable[] buttons;
	private final TouchInputHandler inputHandler;

	public void setKeyCodes (int start, int a, int b, int x1, int x2, int x3, int x4, int y1, int y2, int y3, int y4) {
		WonderSwanButton.START.keyCode = start;
		WonderSwanButton.A.keyCode = a;
		WonderSwanButton.B.keyCode = b;
		WonderSwanButton.X1.keyCode = x1;
		WonderSwanButton.X2.keyCode = x2;
		WonderSwanButton.X3.keyCode = x3;
		WonderSwanButton.X4.keyCode = x4;
		WonderSwanButton.Y1.keyCode = y1;
		WonderSwanButton.Y2.keyCode = y2;
		WonderSwanButton.Y3.keyCode = y3;
		WonderSwanButton.Y4.keyCode = y4;
	}

	public EmuView (Context context) {
		this(context, null);
	}

	public EmuView (Context context, AttributeSet attrs) {
		super(context, attrs);

		inputHandler = new TouchInputHandler(context);

		buttons = new GradientDrawable[WonderSwanButton.values().length];

		for (int i = 0; i < buttons.length; i++) {
			buttons[i] = (GradientDrawable)getResources().getDrawable(R.drawable.button);
		}

		setZOrderOnTop(true); // FIXME any advantage to this?

		SurfaceHolder holder = this.getHolder();
		holder.addCallback(this);

		renderer = new WonderSwanRenderer();
		mThread = new EmuThread(renderer);

	}

	@Override
	public void surfaceChanged (SurfaceHolder holder, int format, int width, int height) {
		int spacing = height / 50;
		int buttonsize = (int)(height / 6.7);
		for (int i = 0; i < buttons.length; i++) {
			buttons[i].setSize(buttonsize, buttonsize);

			int updownleft = buttonsize / 2 + (spacing / 2);
			int updownright = buttonsize + (buttonsize / 2) + (spacing / 2);
			int bottomrowtop = height - buttonsize;

			switch (i) {
			// Y
			case 0:
				buttons[i].setBounds(updownleft, 0, updownright, buttonsize);
				break;
			case 1:
				buttons[i].setBounds(0, buttonsize + spacing, buttonsize, (buttonsize * 2) + spacing);
				break;
			case 2:
				buttons[i].setBounds(buttonsize + spacing, buttonsize + spacing, (buttonsize * 2) + spacing, (buttonsize * 2)
					+ spacing);
				break;
			case 3:
				buttons[i].setBounds(updownleft, (buttonsize * 2) + (spacing * 2), updownright, (buttonsize * 3) + (spacing * 2));
				break;
			// X
			case 4:
				buttons[i].setBounds(updownleft, height - buttonsize, updownright, height);
				break;
			case 5:
				buttons[i].setBounds(0, height - (buttonsize * 2) - spacing, buttonsize, height - buttonsize - spacing);
				break;
			case 6:
				buttons[i].setBounds(buttonsize + spacing, height - (buttonsize * 2) - spacing, (buttonsize * 2) + spacing, height
					- buttonsize - spacing);
				break;
			case 7:
				buttons[i].setBounds(updownleft, (height - (buttonsize * 3)) - (2 * spacing), updownright,
					(height - (buttonsize * 2)) - (2 * spacing));
				break;
			// A,B
			case 8:
				buttons[i].setBounds(width - (buttonsize * 2) - spacing, bottomrowtop, (width - buttonsize) - spacing, height);
				break;
			case 9:
				buttons[i].setBounds(width - buttonsize, bottomrowtop, width, height);
				break;
			// Start
			case 10:
				buttons[i].setSize(buttonsize * 2, buttonsize);
				buttons[i].setBounds((width / 2) - buttonsize, bottomrowtop, (width / 2) + buttonsize, height);
				break;
			}
		}

		Button[] buts = new Button[buttons.length];

		if (buttons != null) {
			Paint textPaint = new Paint();
			textPaint.setColor(0xFFFFFFFF);
			textPaint.setTextSize(height / 30);
			textPaint.setShadowLayer(3, 1, 1, 0x99000000);
			textPaint.setAntiAlias(true);

			for (int i = 0; i < buttons.length; i++) {
				buts[i] = new Button(buttons[i], textPaint, WonderSwanButton.values()[i].name());
			}
		}

		float postscale = (float)width / (float)WonderSwan.SCREEN_WIDTH;

		if (WonderSwan.SCREEN_HEIGHT * postscale > height) {
			postscale = (float)height / (float)WonderSwan.SCREEN_HEIGHT;

		}

		Matrix scale = renderer.getMatrix();

		renderer.setButtons(buts);

		scale.reset();
		scale.postScale(postscale, postscale);
		scale.postTranslate((width - (WonderSwan.SCREEN_WIDTH * postscale)) / 2, 0);

	}

	@Override
	public void surfaceCreated (SurfaceHolder holder) {
		holder.setFormat(PixelFormat.RGB_565);
		mThread.setSurfaceHolder(holder);
	}

	@Override
	public void surfaceDestroyed (SurfaceHolder holder) {
		mThread.clearRunning();
	}

	public void start () {
		Log.d(TAG, "emulation started");
		mThread.setRunning();
		mThread.start();
	}

	public void togglepause () {
		if (mPaused) {
			mPaused = false;
			mThread.unpause();
		} else {
			mPaused = true;
			mThread.pause();
		}
	}

	public void onResume () {
		mThread = new EmuThread(renderer);
		start();
	}

	public void stop () {

		if (mThread.isRunning()) {
			Log.d(TAG, "shutting down emulation");

			mThread.clearRunning();

			synchronized (mThread) {
				try {
					mThread.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
			}

		}
	}

	public static void changeButton (WonderSwanButton which, boolean newstate) {
		which.down = newstate;
		WonderSwan.buttonsDirty = true;

	}

	public EmuThread getThread () {
		return mThread;
	}

	public EmuThread.Renderer getRenderer () {
		return renderer;
	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {

		if (!controlsVisible) {
			return false;
		}

		inputHandler.onTouchEvent(event);
		resetButtons();

		for (Pointer pointer : inputHandler.pointers) {
			if (pointer.down) {
				checkButtons(pointer.x, pointer.y, true);
			}
		}
		return true;

	}

	private boolean checkButtons (float x, float y, boolean pressed) {
		for (int i = 0; i < buttons.length; i++) {
			if (buttons[i].getBounds().contains((int)x, (int)y)) {
				changeButton(WonderSwanButton.values()[i], pressed);
				return true;
			}
		}
		return false;
	}

	private final WonderSwanButton[] buttonVals = WonderSwanButton.values();

	private void resetButtons () {
		for (WonderSwanButton button : buttonVals) {
			changeButton(button, false | button.hardwareKeyDown);
		}
	}

	private boolean decodeKey (int keycode, boolean down) {

		for (WonderSwanButton button : WonderSwanButton.values()) {
			if (button.keyCode == keycode) {
				Log.d(TAG, "here");
				button.hardwareKeyDown = down;

				changeButton(button, down);
				return true;
			}
		}

		return false;

	}

	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event) {

		// Log.d(TAG, "key down");

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// disable back key
			return true;
		}

		if (keyCode == KeyEvent.KEYCODE_MENU) {
			// if (!mRomLoaded) {
			// return true;
			// }
			return false;
		}

		return decodeKey(keyCode, true);
	}

	@Override
	public boolean onKeyUp (int keyCode, KeyEvent event) {
		return decodeKey(keyCode, false);
	}

	public void showButtons (boolean show) {
		controlsVisible = show;
		renderer.showButtons(show);
	}

}
