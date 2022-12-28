package info.zha.aeos;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Provide utility function
 */
public class AppUtil {
    private static final String TAG = AppUtil.class.getName();

    // place to save the application files
    private File appFilesFolder;
    private File appLogFile;

    public AppUtil() {
        setupAppLogs();
    }

    private void setupAppLogs() {
        // create application folder under "/storage/emulated/0/Documents"
        File sysDocumentsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        appFilesFolder =  new File(sysDocumentsFolder, "aeos_call");

        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            Log.w(TAG, "Storage is not available");
            return;
        } else {
            Log.i(TAG, "Create new application folder under " + appFilesFolder.getAbsolutePath());
            if (!appFilesFolder.exists() && !appFilesFolder.mkdirs()) {
                Log.w(TAG, "Unable to create application folder at: " + appFilesFolder.getAbsolutePath());
            }
        }

        // I can put execution result at this file, this is not the Logcat content.
        appLogFile = new File(appFilesFolder, "app.log");
        Log.i(TAG, "Create new application log file under " + appLogFile.getAbsolutePath());
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }
    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    /**
     * Append a new message into application log file.
     * @param msg
     */
    public void commitAppLog(String msg){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String msg_entry = String.format("%s - %s \n",df.format(new Date()), msg);
        try {
            FileOutputStream fOut = new FileOutputStream(appLogFile, true );
            OutputStreamWriter osw = new OutputStreamWriter(fOut);
            osw.write(msg_entry);
            osw.flush();
            osw.close();
        } catch (IOException e){
            Log.w(TAG, "Can't append log message",e );
        }
    }

}
