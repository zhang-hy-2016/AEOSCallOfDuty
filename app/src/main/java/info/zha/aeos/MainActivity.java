package info.zha.aeos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.widget.TextView;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    AppUtil appUtil;
    Properties appProperties;


    Button start_button;
    Button stop_button;
    Button test_button;
    TextView msgView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            setupToolbox();
            setupLayoutComponents();
        } catch (Exception e){
            Log.e(TAG, "Unable to initialize due to:", e);
        }

    }

    private void setupLayoutComponents() {
        start_button = (Button) findViewById(R.id.bnt_start);
        start_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "Start button is fired");
                createTimeWatchJob();
            }
        });

        stop_button = (Button) findViewById(R.id.bnt_stop);
        stop_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "Stop button is fired");
                stop_all_jobs();
            }
        });

        test_button = (Button) findViewById(R.id.bnt_test);
        test_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "test button is fired");
                Map<String, String> dutyPlan = appUtil.buildDutyPlan();
                appUtil.viewDutyPlan(dutyPlan);
                //appUtil.getDutyPerson(dutyPlan);
                msgView.setText((CharSequence) appProperties.getProperty("monday.min.hour"));


            }
        });

        msgView =  (TextView) findViewById(R.id.txt_msg_displayer);
        msgView.setText((CharSequence) "init. ");
    }

    private void setupToolbox() throws IOException {
        appUtil = new AppUtil();
        appUtil.commitAppLog("-------");
        appProperties = appUtil.getAppProperties(this);
        Log.i(TAG, "Successful load application properties, app.version="
                + appProperties.getProperty("app.version"));

    }


    private void createTimeWatchJob(){
        ComponentName componentName = new ComponentName(this, TimeWatchJob.class);
        int jobId = 5001;


        if (isJobExist(jobId)){
            Log.i(TAG, String.format("Job [%s] already exists!",jobId));
            return;
        }

        Log.i(TAG, "Create TimeWatch Job ...");
        appUtil.commitAppLog("Create TimeWatch Job ...");
        JobInfo jobInfo;

        int inv_minute = appProperties.getProperty("watch.job.interval_minute") == null ?
                120 : Integer.parseInt(appProperties.getProperty("watch.job.interval_minute"));
        // see more at https://stackoverflow.com/questions/51304185/jobscheduler-executes-after-every-15-min
        long intervalMillis = inv_minute * 60 * 1000; // every x minutes
        long flexMillis = 5 * 60 * 1000;      // wait 5 minute, then start the job

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            jobInfo = new JobInfo.Builder(jobId, componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)   // don't need any network connection
                    .setPeriodic(intervalMillis, flexMillis)
                    .setPersisted(true)
                    .build();
            JobScheduler jobScheduler = (JobScheduler) getSystemService(
                    Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.schedule(jobInfo);
            Log.i(TAG, "TimeWatch job is created ! ");
        }
    }

    private boolean isJobExist(int jobId){
        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        List<JobInfo> allPendingJobs = scheduler.getAllPendingJobs();
        for (JobInfo info : allPendingJobs) {
            if (info.getId() == jobId ) {
                return  true;
            }
        }
        return false;
    }

    private void stop_all_jobs() {
        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        List<JobInfo> allPendingJobs = scheduler.getAllPendingJobs();
        for (JobInfo info : allPendingJobs) {
            int id = info.getId();
            scheduler.cancel(id);
            Log.i(TAG, "Cancel job with id = " + id );
        }

    }

    private void readLocalFile(){
        Context context= getApplicationContext();

        InputStream inputStream = context.getResources().openRawResource(R.raw.info);
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        Log.d(TAG,  "Res = " + result);
    }


}


