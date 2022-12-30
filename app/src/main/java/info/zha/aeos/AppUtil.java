package info.zha.aeos;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Environment;
import android.telephony.SmsManager;
import android.util.Log;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

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

    /**
     * get duty plan status information
     * @param dutyplan_csv
     * @return
     */
    public StringBuffer checkDutyPlan(String dutyplan_csv){
        StringBuffer info = new StringBuffer();
        File aeos_plan = new File(appFilesFolder, dutyplan_csv);
        if (!aeos_plan.exists()){
            info.append("Can't find duty plan at ").append(aeos_plan.getAbsolutePath()).append("\n");
            info.append("Please ensure this file is existed or the app has permission on it. ");
            return info;
        }
        try {
            Reader in = new FileReader(aeos_plan);
            CSVFormat csvFormat =  CSVFormat.Builder.create(CSVFormat.EXCEL)
                    .setDelimiter(';')
                    .build();
            Iterable<CSVRecord> records = csvFormat.parse(in);
            info.append("Duty plan is OK ");
        } catch (IOException e){
            info.append("Can not read duty plan at ").append(aeos_plan.getAbsolutePath()).append("\n");
            info.append("Please ensure this file is existed or the app has permission on it. ");

        }
        return info;
    }

    /**
     * Build DutyPlan from csv file
     * @return An empty map when csv file is missing or other exception
     */
    public Map<String,String> buildDutyPlan(String dutyplan_csv){
        File aeos_plan = new File(appFilesFolder, dutyplan_csv);
        if (!aeos_plan.exists()) {
            Log.e(TAG, "Can not find csv file at " + aeos_plan.getAbsolutePath());
            commitAppLog("Can not find csv file at " + aeos_plan.getAbsolutePath());
            return new HashMap<String, String>();
        }


        /*
          dutyPlan example:
          kw_1 - user1
          kw_2 - user3
          ...
          kw_52 - userx
         */
        Map<String, String> dutyPlan = new HashMap<>();
        try {
            Reader in = new FileReader(aeos_plan);
            CSVFormat csvFormat =  CSVFormat.Builder.create(CSVFormat.EXCEL)
                    .setDelimiter(';')
                    .build();
            Iterable<CSVRecord> records = csvFormat.parse(in);
            boolean head_line = true;
            for (CSVRecord record : records) {
                if (head_line){
                    for (int i = 1; i < record.size(); i++){
                        // create week index
                        dutyPlan.put(record.get(i), "");
                    }
                    head_line = false;
                } else {
                    String user = record.get(0);
                    for (int i = 1; i < record.size(); i++ ) {
                        String v = record.get(i);
                        String week = "kw" + i;
                        if (v.contains("b")) {
                            // add user into duty plan
                            dutyPlan.put(week,user);
                        }
                    }
                }
            }
        } catch (IOException e){
            Log.e(TAG, "Can not build duty plan",e );
            commitAppLog("Can not build duty plan du to exception:" + e.toString());
            return new HashMap<String, String>();
        }
        return dutyPlan;
    }

    public void viewDutyPlan(Map<String,String> dutyPlan){
        SortedSet<String> keys = new TreeSet<String>(dutyPlan.keySet());

        for (String week: keys) {
            Log.d(TAG, week + ":" +  dutyPlan.get(week));
        }

        String dutyPerson = getDutyPerson(dutyPlan);
        Log.d(TAG, "Man on Duty today = " + dutyPerson);
    }

    /**
     * get current week index
     * @return
     */
    public String getWeekIndex(){
        // Setup Calender, kw1 = the first full week
        Calendar now = GregorianCalendar.getInstance(Locale.GERMANY);
        now.setFirstDayOfWeek(Calendar.MONDAY);
        now.setMinimalDaysInFirstWeek(4); // 4 is ISO 8601 standard compatible setting
        String weekNum = "kw"+now.get(Calendar.WEEK_OF_YEAR);
        return weekNum;
    }

    /**
     * get man on duty from current week .
     * @param dutyPlan
     * @return An empty string for not matched search.
     */
    public String getDutyPerson(Map<String,String> dutyPlan){
        String weekNum = getWeekIndex();
        return dutyPlan.containsKey(weekNum)?dutyPlan.get(weekNum): "";
    }

    /**
     * Get application properties
     * @param context
     * @return  an empty properties on exception
     */
    public Properties getAppProperties(Context context){
        Properties appProperties = new Properties();
        try {
            // Load application properties file
            String propertiesFile = "app.properties";
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open(propertiesFile);
            appProperties.load(inputStream);
        } catch (IOException e) {
            Log.e(TAG, "Can not load app.properties file." ,e);
        }
        return appProperties;
    }

    /**
     * Read runtime properties from file "app_runtime.properties"
     * Different to "application properties" which is a configuration file, the "runtime properties"
     * will be updated during the runtime.
     * @return an empty properties when file is not existed or it is not a valid java properties file.
     */
    public Properties readRuntimeProperties(){
        File runtimeFile = new File(appFilesFolder, "app_runtime.properties");
        Properties runtimeProperties = new Properties();

        if (!runtimeFile.exists()) {
            return new Properties();
        }

        try {
            runtimeProperties.load(new FileInputStream(runtimeFile));
        } catch (IOException e) {
            Log.e(TAG, runtimeFile.getAbsolutePath() +  " is not a valid properties file");
        }
        return runtimeProperties;
    }

    /**
     * Write given runtime properties into file.
     * Be aware, this is not a thread safe method!
     * @param runtimeProperties
     * @return False on IOException
     */
    public boolean persistentRuntimeProperties(Properties runtimeProperties){
        File runtimeFile = new File(appFilesFolder, "app_runtime.properties");

        try {
            FileOutputStream fr = new FileOutputStream(runtimeFile);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
            runtimeProperties.store(fr, "Updated at " +  simpleDateFormat.format(new Date()));
            fr.close();
            return  true;
        } catch (IOException e){
            Log.w(TAG,"Can't write into runtime properties file: " +  runtimeFile.getAbsolutePath(), e);
            return false;
        }
    }


    public void sendSMS(Context context, String phoneNo, String msg) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            Log.i(TAG, "Send sms to " + phoneNo + ":" + msg);
            commitAppLog("Send sms to " + phoneNo + ":" + msg);
        } catch (Exception ex) {
            Log.w(TAG, "Can't send sms to " + phoneNo, ex);
        }
    }
}
