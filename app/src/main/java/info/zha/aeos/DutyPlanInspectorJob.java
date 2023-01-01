package info.zha.aeos;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/*
 * onStartJob和onStopJob方法是执行在主线程中的，我们不可以在其中做耗时操作，否则可能导致ANR。
 * onStartJob方法在系统判定达到约束条件时被调用，我们在此处执行我们的业务逻辑。
 * onStopJob方法在系统判定你需要停止你的任务时被调用，可能在你调用jobFinish停止任务之前，那么什么时候会发生该情况呢？
 * 一般为约束条件不满足的时候，例如我们设置约束条件为充电中，则我们的任务会在充电中开始执行，如果在执行过程中，我们拔下了充电线，
 * 则系统判定我们的约束条件失效了，就会回调onStopJob方法，通知我们停止任务，我们应该在此时立即停止当前正在执行的业务逻辑。
 * onStartJob方法的返回false，代表我们的工作已经处理完了，系统会自动结束该任务，适用于任务在主线程中执行的情况。
 * 返回true，代表我们在子线程中执行任务，在任务执行完成后，我们需要手动调用jobFinish方法，通知系统任务已经执行完成。
 * onStopJob方法返回false，代表我们直接丢弃该任务。返回true则代表，如果我们设置了重试策略，该任务将按照重试策略进行调度。
 * ————————————————
 * 版权声明：本文为CSDN博主「珍心」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
 * 原文链接：https://blog.csdn.net/yuzhangzhen/article/details/108568484
 */

/**
 * JobService to setup call forwarding based of duty plan.
 */
public class DutyPlanInspectorJob extends JobService {
    private static final String TAG = DutyPlanInspectorJob.class.getName();

    // ths object will be updated in every onStartJob call
    private Properties appProperties;

    static String LAST_ACTION_TS="last.action.ts";          // timestamp of last action
    static String LAST_ACTION_PERSON="last.action.person";
    static String LAST_ACTION_NUMBER="last.action.number";  // Call-out numbers of last action

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "onStartJob....");
        doMyJob();
        // return false to tell scheduleManager job is finished.
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        // this method should be never fired.
        Log.d(TAG, "onStopJob.....");
        return false;
    }


    private void doMyJob(){
        // create a new appUtil object
        AppUtil appUtil = new AppUtil();
        appUtil.commitAppLog("TimeWatchJob: Start to working");
        appProperties = appUtil.getAppProperties(this);

        if (! isMondayMorning() ) {
            // switching should be only happened at every Monday morning.
            return;
        }

        Map<String, String> dutyPlan = appUtil.buildDutyPlan(appProperties.getProperty("duty.plan.csv"));
        appUtil.viewDutyPlan(dutyPlan); // for debug only

        String manOnDuty = appUtil.getDutyPerson(dutyPlan);
        Log.i(TAG,"Find Man on duty is " +  manOnDuty);
        String manOnDutyPhone = appProperties.getProperty("phone."+manOnDuty);

        if ( manOnDuty == null || manOnDutyPhone == null ) {
            if (turnOffCallForwarding(appUtil)) {
                Log.w(TAG,"There is on person for  " + appUtil.getWeekIndex() +
                        " or there is no phone number for " + manOnDuty);
                Log.w(TAG,"Turn off call forwarding.");
                appUtil.commitAppLog("There is on person for  " + appUtil.getWeekIndex() +
                        " or there is no phone number for " + manOnDuty);
                appUtil.commitAppLog("Turn off call forwarding");
            }
        } else {
            // Set call forwarding to duty person
            if (turnOnCallForwarding(appUtil, manOnDuty, manOnDutyPhone)) {
                Log.i(TAG,"Turn on call forwarding to " + manOnDuty + ":" + manOnDutyPhone);
                appUtil.commitAppLog("Turn on call forwarding to " + manOnDuty + ":" + manOnDutyPhone);
            }
        }
    }

    private void wakeUpAndCall(AppUtil appUtil, String number){
        try {
            // We must Wakeup Phone -> Open Main windows -> make call
            Log.d(TAG, "WakeUP Phone");
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "aeos::TimeWatchTag");
            wakeLock.acquire();

            // switch to main window
            Intent main_intent = new Intent(this.getApplicationContext(), MainActivity.class);
            main_intent.setAction(Intent.ACTION_VIEW);
            main_intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            main_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.getApplicationContext().startActivity(main_intent);

            // Make a phone call
            appUtil.callNumber(this.getApplicationContext(), number);

            wakeLock.release();
        } catch (Exception e){
            Log.e(TAG, "Can not wake up and call " + number, e);
        }
    }

    private boolean isMondayMorning(){
        // Setup Calender, kw1 = the first full week
        Calendar now = GregorianCalendar.getInstance(Locale.GERMANY);
        now.setFirstDayOfWeek(Calendar.MONDAY);
        now.setMinimalDaysInFirstWeek(4); // 4 is ISO 8601 standard compatible setting

        int min_hour = appProperties.getProperty("monday.min.hour") == null ?
                7 : Integer.parseInt(appProperties.getProperty("monday.min.hour"));
        int max_hour = appProperties.getProperty("monday.max.hour") == null ?
                8 : Integer.parseInt(appProperties.getProperty("monday.max.hour"));

        if (now.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY &&
            now.get(Calendar.HOUR_OF_DAY) >= min_hour && now.get(Calendar.HOUR_OF_DAY) <= max_hour){
            return true;
        }
        return false;
    }


    private List<String> getAEOSAdminsPhones(){
        List<String> admins = new ArrayList<>();
        for (String key: appProperties.stringPropertyNames()) {
            if (key.contains("phone.")) {
                admins.add(appProperties.getProperty(key));
            }
        }
        return admins;
    }

    /**
     * @return true when action has been made.
     */
    private boolean turnOffCallForwarding(AppUtil appUtil){
        String number = appProperties.getProperty("call.forwarding.stop.vodafone");

        Properties runtimeProperties = appUtil.readRuntimeProperties();
        long lastActionTS = runtimeProperties.getProperty(LAST_ACTION_TS) == null?
                0 : Long.parseLong(runtimeProperties.getProperty(LAST_ACTION_TS));

        Date now = new Date();
        long one_week = (7 * 24 * 3600 * 1000L) - (2 * 3600 * 1000L);

        // avoid duplicated actions in same week
        if ((now.getTime() - lastActionTS) > one_week) {
            // turn off call forwarding
            wakeUpAndCall(appUtil, number);

            // send sms to AEOS admins
            String sms_message = appProperties.getProperty("sms.alarm.no.person")
                                    .replaceAll("_week_",appUtil.getWeekIndex());
            for (String num: getAEOSAdminsPhones()) {
                appUtil.sendSMS(this, num, sms_message);
            }

            // update runtime properties
            runtimeProperties.put(LAST_ACTION_TS, Long.toString(now.getTime()));
            runtimeProperties.put(LAST_ACTION_NUMBER, number);
            runtimeProperties.put(LAST_ACTION_PERSON, "");
            appUtil.persistentRuntimeProperties(runtimeProperties);
            return true;
        }
        return false;
    }

    /**
     * @return true when action has been made.
     */
    private boolean turnOnCallForwarding(AppUtil appUtil, String manOnDuty, String manOnDutyPhone){
        String number = appProperties.getProperty("call.forwarding.auto.vodafone")
                .replaceAll("Zielrufnummer", manOnDutyPhone);

        Properties runtimeProperties = appUtil.readRuntimeProperties();
        long lastActionTS = runtimeProperties.getProperty(LAST_ACTION_TS) == null?
                0 : Long.parseLong(runtimeProperties.getProperty(LAST_ACTION_TS));
        String lastActionNum = runtimeProperties.getProperty(LAST_ACTION_NUMBER) == null?
                "" : runtimeProperties.getProperty(LAST_ACTION_NUMBER);
        String lastActionPerson = runtimeProperties.getProperty(LAST_ACTION_PERSON) == null?
                "" : runtimeProperties.getProperty(LAST_ACTION_PERSON);

        Date now = new Date();
        long one_week = (7 * 24 * 3600 * 1000L) - (2 * 3600 * 1000L);

        // avoid duplicated actions in same week
        if ((now.getTime() - lastActionTS) > one_week &&
            !number.equalsIgnoreCase(lastActionNum) ) {
            // turn on call forwarding to person
            wakeUpAndCall(appUtil, number);

            // send sms to new person
            String sms_message = appProperties.getProperty("sms.duty.on")
                    .replaceAll("_name_",manOnDuty)
                    .replaceAll("_number_",manOnDutyPhone);

            appUtil.sendSMS(this, manOnDutyPhone, sms_message);

            // send sms to last person
            if (! lastActionPerson.isEmpty()) {
                String phone = appProperties.getProperty("phone."+lastActionPerson);
                sms_message = appProperties.getProperty("sms.duty.off")
                        .replaceAll("_name_",lastActionPerson);
                appUtil.sendSMS(this, phone, sms_message);
            }

            // update runtime properties
            runtimeProperties.put(LAST_ACTION_TS, Long.toString(now.getTime()));
            runtimeProperties.put(LAST_ACTION_NUMBER, number);
            runtimeProperties.put(LAST_ACTION_PERSON, manOnDuty);

            appUtil.persistentRuntimeProperties(runtimeProperties);
            return true;
        }
        return false;
    }

}
