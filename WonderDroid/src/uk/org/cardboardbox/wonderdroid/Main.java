
package uk.org.cardboardbox.wonderdroid;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.io.File;
import java.io.IOException;
import java.util.GregorianCalendar;

import uk.org.cardboardbox.wonderdroid.utils.RomAdapter.Rom;
import uk.org.cardboardbox.wonderdroid.views.EmuView;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.ProgressBar;

public class Main extends BaseActivity {

    public static final String ROM = "rom";

    public static final String ROMHEADER = "romheader";

    private Context mContext;

    private ProgressBar mPB;

    private EmuView view;

    private Rom mRom;

    private WonderSwanHeader mRomHeader;

    private File mCartMem;

    private boolean mControlsVisible = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRom = (Rom)this.getIntent().getExtras().getSerializable(ROM);
        mRomHeader = (WonderSwanHeader)this.getIntent().getExtras().getSerializable(ROMHEADER);

        if (mRom == null || mRomHeader == null) {
            throw new IllegalArgumentException();
        }

        view = new EmuView(this);
        setContentView(view);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);

        mContext = this.getBaseContext();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        parseEmuOptions(prefs);
        parseKeys(prefs);

        mPB = (ProgressBar)this.findViewById(R.id.romloadprogressbar);

        AsyncTask<Void, Void, Void> loader = new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
            }

            @Override
            protected Void doInBackground(Void... params) {
                mCartMem = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/wonderdroid/cartmem/" + mRomHeader.internalname + ".mem");
                try {
                    mCartMem.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Main.this);
                String name = prefs.getString("ws_name", "");
                String sex = prefs.getString("ws_sex", "1");
                String blood = prefs.getString("ws_blood", "1");
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTimeInMillis(prefs.getLong("ws_birthday", 0));

                WonderSwan.load(Rom.getRomFile(mContext, mRom).getAbsolutePath(),
                        mRomHeader.isColor, name, cal.get(GregorianCalendar.YEAR),
                        cal.get(GregorianCalendar.MONTH), cal.get(GregorianCalendar.DAY_OF_MONTH),
                        Integer.parseInt(blood), Integer.parseInt(sex));
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (mPB != null) {
                    mPB.setVisibility(ProgressBar.GONE);
                }

                WonderSwan.reset();
                if (mCartMem.isFile() && (mCartMem.length() > 0)) {
                    WonderSwan.loadbackupdata(mCartMem.getAbsolutePath());
                }
                view.start();
            }
        };

        loader.execute((Void[])null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.main_exitmi:
                this.finish();
                return true;

            case R.id.main_pausemi:
                view.togglepause();
                return true;

            case R.id.main_resetmi:
                WonderSwan.reset();
                return true;

            case R.id.main_prefsmi:
                Intent intent = new Intent(this, Prefs.class);
                startActivity(intent);
                return true;

            case R.id.main_togcntrlmi:
                toggleControls();
                return true;
                // case R.id.quit:
                // quit();
                // return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        return true;
    }

    private void toggleControls() {
        mControlsVisible = !mControlsVisible;
        view.showButtons(mControlsVisible);
    }

    private void parseEmuOptions(SharedPreferences prefs) {
        WonderSwan.audioEnabled = prefs.getBoolean("emusound", true);
    }

    private void parseKeys(SharedPreferences prefs) {

        view.setKeyCodes(prefs.getInt("hwcontrolStart", 0), prefs.getInt("hwcontrolA", 0),
                prefs.getInt("hwcontrolB", 0), prefs.getInt("hwcontrolX1", 0),
                prefs.getInt("hwcontrolX2", 0), prefs.getInt("hwcontrolX3", 0),
                prefs.getInt("hwcontrolX4", 0), prefs.getInt("hwcontrolY1", 0),
                prefs.getInt("hwcontrolY2", 0), prefs.getInt("hwcontrolY3", 0),
                prefs.getInt("hwcontrolY4", 0));

    }

    @Override
    public void onRestart() {
        super.onRestart();
        view.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        view.stop();
        WonderSwan.storebackupdata(mCartMem.getAbsolutePath());
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        parseEmuOptions(prefs);
        parseKeys(prefs);
    }

}
