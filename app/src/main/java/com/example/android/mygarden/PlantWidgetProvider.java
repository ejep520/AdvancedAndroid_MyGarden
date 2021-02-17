package com.example.android.mygarden;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.example.android.mygarden.ui.MainActivity;

import org.jetbrains.annotations.NotNull;

/**
 * Implementation of App Widget functionality.
 */
public class PlantWidgetProvider extends AppWidgetProvider {


    static void updateAppWidget(@NotNull @NonNull Context context,
                                @NotNull @NonNull AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.plant_widget_provider);

        Intent intent = new Intent(context, MainActivity.class);
        Intent wateringIntent = new Intent(context, PlantWateringService.class);
        wateringIntent.setAction(PlantWateringService.ACTION_WATER_PLANTS);

        views.setOnClickPendingIntent(R.id.widget_plant_image,
                PendingIntent.getActivity(context, 0, intent, 0));
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pendingIntent = PendingIntent.getForegroundService(
                    context,
                    0,
                    wateringIntent,
                    0);
        } else {
            pendingIntent = PendingIntent.getService(
                    context,
                    0,
                    wateringIntent,
                    0);
        }
        views.setOnClickPendingIntent(R.id.widget_water_button, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         @NonNull @NotNull int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}