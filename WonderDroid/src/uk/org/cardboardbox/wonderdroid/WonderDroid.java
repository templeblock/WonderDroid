
package uk.org.cardboardbox.wonderdroid;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import uk.org.cardboardbox.wonderdroid.utils.ZipCache;

import android.app.Application;
import android.os.Environment;

import java.io.File;

@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=30ee9348", formKey = "")
public class WonderDroid extends Application {

    public static final String DIRECTORY = "/wonderdroid/";

    public static final String CARTMEMDIRECTORY = DIRECTORY + "cartmem/";

    public static final String SAVESTATEDIRECTORY = DIRECTORY + "savestates/";

    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);
        WonderSwan.outputDebugShizzle();
        ZipCache.dumpInfo(this.getBaseContext());
        ZipCache.clean(this.getBaseContext());
    }

    public File getRomDir() {
        if (Environment.getExternalStorageState().compareTo(Environment.MEDIA_MOUNTED) != 0)
            return null;

        return Environment.getExternalStorageDirectory();
    }

}
