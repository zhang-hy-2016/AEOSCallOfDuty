package info.zha.aeos;

import static info.zha.aeos.DutyPlanInspectorJob.LAST_ACTION_NUMBER;
import static info.zha.aeos.DutyPlanInspectorJob.LAST_ACTION_PERSON;
import static info.zha.aeos.DutyPlanInspectorJob.LAST_ACTION_TS;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
    Context appContext;
    AppUtil appUtil;
    Properties appProperties;

    Button start_button;
    Button status_button;
    Button stop_button;
    Button force_on_button;

    TextView msgView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        appContext = this.getApplicationContext();
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

        // Manually setup call forwarding to duty person of current week
        force_on_button = (Button) findViewById(R.id.bnt_force_on);
        force_on_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Map<String, String> dutyPlan =
                        appUtil.buildDutyPlan(appProperties.getProperty("duty.plan.csv"));
                String manOnDuty = appUtil.getDutyPerson(dutyPlan);
                String manOnDutyPhone = appProperties.getProperty("phone."+manOnDuty);
                setForwardingOverAlert(manOnDuty, manOnDutyPhone);
            }
        });

        msgView =  (TextView) findViewById(R.id.txt_MultiLine);
        msgView.setText((CharSequence) "Click Status button to refresh.");
    }

    /**
     * Manually setup call forwarding.
     * @param manOnDuty
     * @param manOnDutyPhone
     */
    private void setForwardingOverAlert(String manOnDuty, String manOnDutyPhone){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        // Setting Alert Dialog Title
        alertDialogBuilder.setTitle("Confirm Manually operation");
        // Setting Alert Dialog Message
        alertDialogBuilder.setMessage("Are you sure of enable the call forwarding to " +
                manOnDuty + ":" + manOnDutyPhone);
        alertDialogBuilder.setCancelable(false);

        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                Log.i(TAG, "Manually setup call forwarding to " + manOnDutyPhone);
                appUtil.commitAppLog("Manually setup call forwarding to " + manOnDutyPhone);
                setCallForwarding(manOnDutyPhone);
                StringBuffer info = new StringBuffer("Enable call forwarding to ")
                        .append(manOnDuty).append(":").append(manOnDutyPhone);
                msgView.setText((CharSequence) info);
            }
        });

        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "NO");
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void setupToolbox() throws IOException {
        appUtil = new AppUtil();
        appUtil.commitAppLog("-------");
        appProperties = appUtil.getAppProperties(this);
        Log.i(TAG, "Successful load application properties, app.version="
                + appProperties.getProperty("app.version"));

    }


    private void createTimeWatchJob(){
        ComponentName componentName = new ComponentName(this, DutyPlanInspectorJob.class);
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
                    .setPeriodic(intervalMillis)
                    .setPersisted(true)
                    .build();

                  // use this code to start this job immediately (for testing)
//                long k = 30 * 1000L;
//                jobInfo =  new JobInfo.Builder(jobId ,componentName)
//                    .setMinimumLatency(k)
//                    .setOverrideDeadline(k)
//                    .build();

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

    /**
     * Setup Call Forwarding to duty person
     */
    private void setCallForwarding(String phoneNumber){
        String mmi_code = appProperties.getProperty("call.forwarding.auto.vodafone")
                .replaceAll("Zielrufnummer", phoneNumber);
        appUtil.commitAppLog("Manually setup call forwarding to " + phoneNumber);
        appUtil.callNumber(this, mmi_code);
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


