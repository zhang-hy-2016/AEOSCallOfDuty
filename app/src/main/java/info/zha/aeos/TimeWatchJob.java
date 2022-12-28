package info.zha.aeos;
import android.app.job.JobParameters;
import android.app.job.JobService;

import android.content.Intent;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;

import android.util.Log;

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
 *
 */
public class TimeWatchJob extends JobService {
    private static final String TAG = TimeWatchJob.class.getName();


    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "onStartJob....");
        doMyJob();

        // false - job is finished
        // true - job is still running (in a separated thread).
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "onStopJob.....");
        AppUtil appUtil = new AppUtil();
        appUtil.commitAppLog("TimeWatchJob: Stop the job ");

        // this job
        return false;
    }


    private void doMyJob(){
        // create a new appUtil object
        AppUtil appUtil = new AppUtil();
        appUtil.commitAppLog("TimeWatchJob: Start to working");

        String number = "11111";
        try {
            appUtil.callNumber(this, number);
        } catch (Exception e){
            Log.e(TAG, "Can not call to " + number, e);
        }
    }
}
