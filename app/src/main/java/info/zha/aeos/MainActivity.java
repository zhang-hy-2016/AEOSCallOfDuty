package info.zha.aeos;

import static info.zha.aeos.TimeWatchJob.LAST_ACTION_NUMBER;
import static info.zha.aeos.TimeWatchJob.LAST_ACTION_PERSON;
import static info.zha.aeos.TimeWatchJob.LAST_ACTION_TS;

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    AppUtil appUtil;
    Properties appProperties;

    Button start_button;
    Button status_button;
    Button stop_button;

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
                Log.d(TAG, "Start button is fired");
                createTimeWatchJob();
                msgView.setText((CharSequence) getStatusInfo());
            }
        });

        status_button = (Button) findViewById(R.id.bnt_status);
        status_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                msgView.setText((CharSequence) getStatusInfo());
            }
        });

        stop_button = (Button) findViewById(R.id.bnt_stop);
        stop_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stop_all_jobs();
                msgView.setText((CharSequence) getStatusInfo());
            }
        });

        msgView =  (TextView) findViewById(R.id.txt_MultiLine);
        msgView.setText((CharSequence) "Click Status button to refresh.");
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

    private StringBuffer getStatusInfo(){
        String job_status = isJobExist(5001)?
                "Main Thread is running":
                "Main Thread is stopped, you need to click the start button.";
        String currentWeek = appUtil.getWeekIndex();

        StringBuffer planFileStatus =
                appUtil.checkDutyPlan(appProperties.getProperty("duty.plan.csv"));

        Map<String, String> dutyPlan =
                appUtil.buildDutyPlan(appProperties.getProperty("duty.plan.csv"));
        String manOnDuty = appUtil.getDutyPerson(dutyPlan);
        String manOnDutyPhone = appProperties.getProperty("phone."+manOnDuty);

        Properties runtimeProperties = appUtil.readRuntimeProperties();
        long lastActionTS = runtimeProperties.getProperty(LAST_ACTION_TS) == null?
                0 : Long.parseLong(runtimeProperties.getProperty(LAST_ACTION_TS));
        String lastActionNum = runtimeProperties.getProperty(LAST_ACTION_NUMBER) == null?
                "" : runtimeProperties.getProperty(LAST_ACTION_NUMBER);
        String lastActionPerson = runtimeProperties.getProperty(LAST_ACTION_PERSON) == null?
                "" : runtimeProperties.getProperty(LAST_ACTION_PERSON);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String actionTime = lastActionTS==0?"N.A.": df.format(new Date(lastActionTS));

        StringBuffer status = new StringBuffer();
        status.append("Status: \n");
        status.append(job_status).append("\n");
        status.append("-------------------").append("\n");
        status.append(planFileStatus).append("\n");
        status.append("-------------------").append("\n");
        status.append("Current Week:").append(currentWeek).append("\n");
        status.append("Current Man on Duty:").append(manOnDuty).append(("\n"));
        status.append("Current number:").append(manOnDutyPhone).append(("\n"));
        status.append("-------------------").append("\n");
        status.append("Last Action Time: ").append(actionTime).append("\n");
        status.append("Last Action Person: ").append(lastActionPerson).append("\n");
        status.append("Last Action Number: ").append(lastActionNum).append("\n");
        return status;
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

}


