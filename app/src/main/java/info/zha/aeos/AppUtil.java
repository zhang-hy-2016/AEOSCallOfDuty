package info.zha.aeos;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
        }

        Log.d(TAG, "The path of application folder is: " + appFilesFolder.getAbsolutePath());
        if (!appFilesFolder.exists() && !appFilesFolder.mkdirs()) {
            Log.w(TAG, "Unable to create application folder at: " + appFilesFolder.getAbsolutePath());
        }

        appLogFile = new File(appFilesFolder, "app.log");
        Log.d(TAG, "The path to application log file is: " + appLogFile.getAbsolutePath());
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

    /**
     * Make a call of given number
     * @param context
     * @param number
     */
    public void callNumber(Context context, String number) {
        // Getting instance of Intent with action as ACTION_CALL
        Intent phone_intent = new Intent(Intent.ACTION_CALL);
        // Set data of Intent through Uri by parsing phone number
        phone_intent.setData(Uri.parse("tel:" + number));
        Log.i(TAG, "Dial Number " +  number);
        commitAppLog("Dial Number " +  number);
        context.startActivity(phone_intent);
    }


}
