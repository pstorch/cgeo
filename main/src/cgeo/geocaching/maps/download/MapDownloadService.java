package cgeo.geocaching.maps.download;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to download maps and themes
 */
public class MapDownloadService extends Service {

    @Nullable
    private static WeakReference<Activity> startingActivity;
    private static final Object startingActivityLock = new Object();
    private List<String> downloads;

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent != null) {
            downloads = intent.getStringArrayListExtra(Intents.EXTRA_MAP_DOWNLOADS);
        }
        return START_NOT_STICKY; // service can be stopped by system, if under memory pressure
    }

    public static void startService(final Activity activity, final ArrayList<String> downloads) {
        synchronized (startingActivityLock) {
            startingActivity = new WeakReference<>(activity);
        }
        final Intent service = new Intent(activity, MapDownloadService.class);
        service.putStringArrayListExtra(Intents.EXTRA_MAP_DOWNLOADS, downloads);
        activity.startService(service);
    }

    public static void stopService(final Activity activity) {
        synchronized (startingActivityLock) {
            if (activity.stopService(new Intent(activity, MapDownloadService.class))) {
                ActivityMixin.showShortToast(activity, activity.getString(R.string.map_download_stopped));
            }
            startingActivity = null;
        }
    }

    public static boolean isRunning() {
        return startingActivity != null && startingActivity.get() != null;
    }

}
