package com.example.android.mygarden;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.ui.MainActivity;
import com.example.android.mygarden.utils.PlantUtils;

import org.jetbrains.annotations.NotNull;

public class PlantWateringService extends JobIntentService {

    public static final String ACTION_WATER_PLANTS = "com.example.android.mygarden.action.water_plants";
    public static final String ACTION_UPDATE_PLANT_WIDGET = "com.example.android.mygarden.action.update_plant_widget";
    public static final String EXTRA_PLANT_ID = "com.example.android.mygarden.extra.PLANT_ID";

    private static final int NOTIFICATION_ID = 1;
    private static final String LOG_TAG = PlantWateringService.class.getSimpleName();

    private NotificationManager mNotificationManager;

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

    private void onHandleIntent(@Nullable @org.jetbrains.annotations.Nullable Intent intent) {
        Log.i(LOG_TAG, "onHandleIntent started...");
        if (intent == null) { return; }
        final String action = intent.getAction();
        if (ACTION_WATER_PLANTS.equals(action)) {
            final long plantId = intent.getLongExtra(EXTRA_PLANT_ID,
                    PlantContract.INVALID_PLANT_ID);
            handleActionWaterPlants(plantId);
        } else if (ACTION_UPDATE_PLANT_WIDGET.equals(action)) {
            handleActionUpdatePlantWidget();
        }
    }

    public static void startActionWaterPlants(@NonNull @NotNull Context context, long plantId) {
        Intent intent = new Intent(context, PlantWateringService.class);
        intent.setAction(ACTION_WATER_PLANTS);
        intent.putExtra(EXTRA_PLANT_ID, plantId);
        enqueueWork(context, PlantWateringService.class, 1, intent);
/*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        } */
    }

    public static void startActionUpdatePlantWidgets(@NotNull @NonNull Context context) {

        Log.i(LOG_TAG, "StartActionUpdatePlantWidgets started...");
        Intent intent = new Intent(context, PlantWateringService.class);
        intent.setAction(ACTION_UPDATE_PLANT_WIDGET);
        enqueueWork(context, PlantWateringService.class, 1, intent);
/*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
*/
    }

    private void handleActionWaterPlants(long plantId) {
        Log.i(LOG_TAG, "Watering handler called.");
        if (plantId < 0) {
            Log.w(LOG_TAG, "PlantId was less than zero. Abend.");
            return;
        }
        Uri SINGLE_PLANT_URI = ContentUris.withAppendedId(
                PlantContract
                        .BASE_CONTENT_URI
                        .buildUpon()
                        .appendPath(PlantContract.PATH_PLANTS)
                        .build(),
                plantId);
        ContentValues values = new ContentValues();
        long timeNow = System.currentTimeMillis();
        values.put(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME, timeNow);
        int returnedNo = getContentResolver().update(
                SINGLE_PLANT_URI,
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
                Log.w(LOG_TAG, "Too many records modified.");
                break;
        }
        startActionUpdatePlantWidgets(this);
        stopSelf();
    }

    private void handleActionUpdatePlantWidget() {
        Log.i(LOG_TAG, "Update Plant Widgets handler called.");
        Uri PLANT_URI = PlantContract.PlantEntry.CONTENT_URI;
        boolean canWater = false;
        Cursor plantCursor = getContentResolver().query(
                PLANT_URI,
                null,
                null,
                null,
                PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME);
        @DrawableRes int imageRes = R.drawable.grass;
        long plantId = PlantContract.INVALID_PLANT_ID;
        if ((plantCursor != null) && plantCursor.moveToFirst()) {
            final int createTimeIndex = plantCursor.getColumnIndex(PlantContract
                    .PlantEntry.COLUMN_CREATION_TIME);
            final int waterTimeIndex = plantCursor.getColumnIndex(PlantContract
                    .PlantEntry.COLUMN_LAST_WATERED_TIME);
            final int plantTypeIndex = plantCursor.getColumnIndex(PlantContract
                    .PlantEntry.COLUMN_PLANT_TYPE);
            final int plantIdIndex = plantCursor.getColumnIndex(PlantContract
                    .PlantEntry._ID);
            final long timeNow = System.currentTimeMillis();
            final long wateredAt = plantCursor.getLong(waterTimeIndex);
            final long createdAt = plantCursor.getLong(createTimeIndex);
            final int plantType = plantCursor.getInt(plantTypeIndex);
            plantId = plantCursor.getLong(plantIdIndex);
            plantCursor.close();
            imageRes = PlantUtils.getPlantImageRes(
                    this,
                    timeNow - createdAt,
                    timeNow - wateredAt,
                    plantType);
            canWater = (((timeNow - wateredAt) > PlantUtils.MIN_AGE_BETWEEN_WATER) && ((timeNow - wateredAt) < PlantUtils.MAX_AGE_WITHOUT_WATER));
        } else if (plantCursor != null) {
            plantCursor.close();
        }
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        final int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(this, PlantWidgetProvider.class));

        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_grid_view);

        PlantWidgetProvider.updatePlantWidgets(
                this,
                appWidgetManager,
                imageRes,
                appWidgetIds,
                plantId,
                canWater);
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

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }

    @Override
    protected void onHandleWork(@NonNull @NotNull Intent intent) {
        Log.i(LOG_TAG, "onHandleWork started.");
        onHandleIntent(intent);
    }
}