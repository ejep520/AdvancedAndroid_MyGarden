package com.example.android.mygarden;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.ui.MainActivity;
import com.example.android.mygarden.utils.PlantUtils;

import org.jetbrains.annotations.NotNull;

public class PlantWateringService extends IntentService {

    public static final String ACTION_WATER_PLANTS = "com.example.android.mygarden.action.water_plants";

    private static final int NOTIFICATION_ID = 1;
    private static final String LOG_TAG = PlantWateringService.class.getSimpleName();

    private NotificationManager mNotificationManager;

    public PlantWateringService() {
        super("DefaultWateringService");
    }

    public PlantWateringService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Context context = getApplicationContext();
            NotificationChannel channel = new NotificationChannel("mygarden",
                    "Watering Service",
                    NotificationManager.IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(channel);
            startForeground(NOTIFICATION_ID, buildNotification(context));
        }
    }

    @Override
    protected void onHandleIntent(@Nullable @org.jetbrains.annotations.Nullable Intent intent) {
        if (intent == null) { return; }
        final String action = intent.getAction();
        if (ACTION_WATER_PLANTS.equals(action)) {
            handleActionWaterPlants();
        }
    }

    public void startActionWaterPlants(@NonNull @NotNull Context context) {
        Intent intent = new Intent(context, PlantWateringService.class);
        intent.setAction(ACTION_WATER_PLANTS);
        context.startService(intent);
    }

    private void handleActionWaterPlants() {
        Log.i(LOG_TAG, "Handler called.");
        Uri PLANTS_URI = PlantContract.BASE_CONTENT_URI.buildUpon()
                .appendPath(PlantContract.PATH_PLANTS)
                .build();
        ContentValues values = new ContentValues();
        long timeNow = System.currentTimeMillis();
        values.put(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME, timeNow);
        int returnedNo = getContentResolver().update(
                PLANTS_URI,
                values,
                PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME + ">?",
                new String[] { String.valueOf(timeNow - PlantUtils.MAX_AGE_WITHOUT_WATER)});
        switch (returnedNo) {
            case 0:
                Log.i(LOG_TAG, "No records modified.");
                break;
            case 1:
                Log.i(LOG_TAG, "1 record modified.");
                break;
            default:
                Log.i(LOG_TAG, Integer.valueOf(returnedNo).toString() + " records modified.");
                break;
        }
        stopSelf();
    }

    private Notification buildNotification(Context context) {
        final int icon = R.drawable.grass;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                "mygarden");
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                new Intent(context, MainActivity.class),
                0);
        builder.setContentTitle("mygarden")
                .setContentText("Watering Service")
                .setContentIntent(pendingIntent)
                .setSmallIcon(icon)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        return builder.build();
    }
}