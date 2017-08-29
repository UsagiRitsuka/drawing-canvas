package wachi.drawingview.manager;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * Created by USER on 2015/3/3.
 */
public class ThreadManager {
    private static ThreadManager instance = null;
    private Handler mUI_Handler,mBackgroundHandler,mAPIQueueThread;
    private HandlerThread mThread,apiQueueThread;
    private ThreadManager(){
        mUI_Handler = new Handler(Looper.getMainLooper());
        mThread = new HandlerThread("backgroundThread");
        mThread.start();
        mBackgroundHandler = new Handler(mThread.getLooper());
        apiQueueThread = new HandlerThread("apiQueueThread");
        apiQueueThread.start();
        mAPIQueueThread = new Handler(apiQueueThread.getLooper());
    }

    public synchronized static ThreadManager getInstance() {
        if (instance == null) {
            instance = new ThreadManager();
        }
        return instance;
    }

    public void postToUIThread(Runnable runnable){
        mUI_Handler.post(runnable);
    }

    public void postToUIThread(Runnable runnable, long delayMillis){
        mUI_Handler.postDelayed(runnable, delayMillis);
    }

    public void removeCallbacks(Runnable runnable){
        mUI_Handler.removeCallbacks(runnable);
        mBackgroundHandler.removeCallbacks(runnable);
        mAPIQueueThread.removeCallbacks(runnable);
    }

    public void postToBackgroungThread(Runnable runnable){
        mBackgroundHandler.post(runnable);
    }

    public void postToBackgroungThread(Runnable runnable, long delayMillis){
        mBackgroundHandler.postDelayed(runnable, delayMillis);
    }

    public void postToAPIQueueThread(Runnable runnable){
        mAPIQueueThread.post(runnable);
    }

    public void postToAPIQueueThread(Runnable runnable, long delayMillis){
        mAPIQueueThread.postDelayed(runnable, delayMillis);
    }

    public void destroy(){
        mThread.quit();
        apiQueueThread.quit();
        instance = null;
    }
}
