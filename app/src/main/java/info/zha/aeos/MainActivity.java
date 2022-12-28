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

    AppUtil appUtil;

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
                appUtil.commitAppLog("test test test ");
            }
        });
    }

    private void setupStorage() throws IOException {
        appUtil = new AppUtil();
        appUtil.commitAppLog("-------");
    }


    private void readLocalFile(){
        Context context= getApplicationContext();

        InputStream inputStream = context.getResources().openRawResource(R.raw.info);
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        Log.d(TAG,  "Res = " + result);

    }

}


