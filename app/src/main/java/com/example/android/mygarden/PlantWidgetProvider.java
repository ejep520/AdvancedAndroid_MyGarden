package com.example.android.mygarden;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.ui.MainActivity;
import com.example.android.mygarden.ui.PlantDetailActivity;

import org.jetbrains.annotations.NotNull;

/**
 * Implementation of App Widget functionality.
 */
public class PlantWidgetProvider extends AppWidgetProvider {

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    static void updateAppWidget(@NotNull @NonNull Context context,
                                @NotNull @NonNull AppWidgetManager appWidgetManager,
                                @DrawableRes int imgRes,
                                int appWidgetId, long plantId, boolean showWater) {

        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);

        // Allocate memory for a RemoteViews object
        RemoteViews views;
        if (width < 300) {
            views = getSinglePlantRemoteViews(context, imgRes, plantId, showWater);
        } else {
            views = getGardenGridRemoteViews(context);
        }
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @NotNull
    private static RemoteViews getSinglePlantRemoteViews(@NotNull @NonNull Context context,
                                                           @DrawableRes int imgRes, long plantId,
                                                           boolean showWater) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.plant_widget_provider);

        // Create the intents, which will be wrapped in PendingIntents.
        Intent intent = new Intent(context, MainActivity.class);

        Intent wateringIntent;
        if (plantId == PlantContract.INVALID_PLANT_ID) {
            wateringIntent = (Intent) intent.clone();
        } else {
            wateringIntent = new Intent(context, PlantWateringService.class);
            wateringIntent.setAction(PlantWateringService.ACTION_WATER_PLANTS);
            wateringIntent.putExtra(PlantWateringService.EXTRA_PLANT_ID, plantId);
        }

        views.setViewVisibility(R.id.widget_water_button, showWater ? View.VISIBLE : View.INVISIBLE);

        // Create the first PendingIntent and set it to the plant image.
        views.setOnClickPendingIntent(R.id.widget_plant_image,
                PendingIntent.getActivity(context, 0, intent, 0));

        // Based on the OS running, make the PendingIntent for a foreground service or a service.
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pendingIntent = PendingIntent.getForegroundService(
                    context,
                    0,
                    wateringIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntent = PendingIntent.getService(
                    context,
                    0,
                    wateringIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        // Set the PendingIntent for the watering image.
        views.setOnClickPendingIntent(R.id.widget_water_button, pendingIntent);

        //Update the image in the widget to display the plant closest to death.
        views.setImageViewResource(R.id.widget_plant_image, imgRes);

        return views;
    }

    @Override
    public void onUpdate(@NonNull @NotNull Context context,
                         @NonNull @NotNull AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        PlantWateringService.startActionUpdatePlantWidgets(context);
    }

    public static void updatePlantWidgets(@NonNull @NotNull Context context,
                                          @NonNull @NotNull AppWidgetManager appWidgetManager,
                                          @DrawableRes int imgRes,
                                          @NotNull int[] appWidgetIds,
                                          long plantId, boolean showWater) {

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, imgRes, appWidgetId, plantId, showWater);
        }
    }

    @NonNull
    @NotNull
    private static RemoteViews getGardenGridRemoteViews(@NotNull @NonNull Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_grid_view);

        Intent intent = new Intent(context, GridWidgetService.class);
        views.setRemoteAdapter(R.id.widget_grid_view, intent);

        Intent appIntent = new Intent(context, PlantDetailActivity.class);
        views.setPendingIntentTemplate(R.id.widget_grid_view, PendingIntent.getActivity(context,
                0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        views.setEmptyView(R.id.widget_grid_view, R.id.empty_view);
        return views;
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        PlantWateringService.startActionUpdatePlantWidgets(context);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}