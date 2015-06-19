
package uk.org.cardboardbox.wonderdroid;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.AudioTrack;

import uk.org.cardboardbox.wonderdroid.utils.EmuThread;

@SuppressLint("NewApi")
public class WonderSwanRenderer implements EmuThread.Renderer {

    private AudioTrack audio = new AudioTrack(AudioManager.STREAM_MUSIC, WonderSwan.audiofreq,
            WonderSwan.channelconf, WonderSwan.encoding, AudioTrack.getMinBufferSize(
                    WonderSwan.audiofreq, WonderSwan.channelconf, WonderSwan.encoding) * 4,
            AudioTrack.MODE_STREAM);

    private Button[] buttons;

    private boolean showButtons = false;

    private final ShortBuffer frameone;

    private final Bitmap framebuffer;

    private final Matrix scale = new Matrix();

    private final Paint paint = new Paint();

    private final Paint textPaint = new Paint();

    public WonderSwanRenderer() {

        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(35);
        textPaint.setShadowLayer(3, 1, 1, 0x99000000);
        textPaint.setAntiAlias(true);

        frameone = ByteBuffer.allocateDirect(WonderSwan.FRAMEBUFFERSIZE).asShortBuffer();
        framebuffer = Bitmap.createBitmap(WonderSwan.SCREEN_WIDTH, WonderSwan.SCREEN_HEIGHT,
                Bitmap.Config.RGB_565);
    }

    @Override
    public void render(Canvas c) {

        // c.drawARGB(0xff, 0, 0, 0);
        c.drawBitmap(framebuffer, scale, paint);
        // c.drawBitmap(framebuffer, 0, 0, null);

        if (showButtons && buttons != null) {
            for (Button button : buttons) {
                c.drawBitmap(button.normal, button.drawrect, button.rect, null);
            }
        }

    }

    public Matrix getMatrix() {
        return scale;
    }

    public Paint getPaint() {
        return paint;
    }

    @Override
    public void start() {
        audio.play();
    }

    @Override
    public void update(boolean skip) {
        WonderSwan.execute_frame(frameone, skip);
        audio.write(WonderSwan.audiobuffer, 0, WonderSwan.samples * 2);

        if (!skip) {
            frameone.rewind();
            framebuffer.copyPixelsFromBuffer(frameone);
        }
    }

    @Override
    public void setButtons(Button[] buttons) {
        this.buttons = buttons;
    }

    @Override
    public void showButtons(boolean show) {
        this.showButtons = show;
    }
}
