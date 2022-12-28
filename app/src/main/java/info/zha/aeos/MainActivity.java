package info.zha.aeos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.os.Environment;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // place to save the application files
    private File appFilesFolder;
    private File appLogFile;

    Button start_button;
    Button stop_button;
    Button save_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            setupLayoutComponents();
            setupStorage();

        } catch (Exception e){
            Log.e(TAG, "Unable to initialize due to:", e);
        }

    }

    private void setupLayoutComponents() {
        Button start_button = (Button) findViewById(R.id.bnt_start);
        start_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "Start button is fired");
            }
        });

        Button stop_button = (Button) findViewById(R.id.bnt_stop);
        stop_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "Stop button is fired");
            }
        });

        Button save_button = (Button) findViewById(R.id.bnt_save);
        save_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "Save button is fired");
                commitAppLog("test test test ");
            }
        });
    }

    private void setupStorage() throws IOException {
        // create application folder under "/storage/emulated/0/Documents"
        File sysDocumentsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        appFilesFolder =  new File(sysDocumentsFolder, "aeos_call");

        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            Log.w(TAG, "Storage is not available");
            return;
        } else {
            Log.i(TAG, "Create new application folder under " + appFilesFolder.getAbsolutePath());
            if (!appFilesFolder.exists() && !appFilesFolder.mkdirs()) {
                throw new IOException("Unable to create application folder at:  " + appFilesFolder.getAbsolutePath());
            }
        }

        // I can put execution result at this file, this is not the Logcat content.
        appLogFile = new File(appFilesFolder, "app.log");
        Log.i(TAG, "Create new application log file under " + appLogFile.getAbsolutePath());
        commitAppLog("-------");

    }

    private void commitAppLog(String msg){
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

    private void readLocalFile(){
        Context context= getApplicationContext();

        InputStream inputStream = context.getResources().openRawResource(R.raw.info);
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        Log.d(TAG,  "Res = " + result);

    }

    private void writeFileTest(){
        String filename = "SampleFile.txt";
        File myExternalFile;

        File sysDocumentsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);


        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            Log.w(TAG, "Storage is not available");
            return;
        } else {
            Log.w(TAG, "Create new file under " + sysDocumentsFolder);
            myExternalFile = new File(sysDocumentsFolder, filename);
        }
        String msg = "hello zha";
        try {
            FileOutputStream fos = new FileOutputStream(myExternalFile);

            fos.write(msg.getBytes());
            fos.close();
        } catch (IOException e) {
            Log.w(TAG, e);
        }
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


}


