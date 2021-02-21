package com.example.android.mygarden;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.ui.PlantDetailActivity;
import com.example.android.mygarden.utils.PlantUtils;

public class GridWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new GridRemoteViewsFactory(this.getApplicationContext());
    }
}

class GridRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context mContext;
    private Cursor mCursor;
    private static final String LOG_TAG = GridRemoteViewsFactory.class.getSimpleName();

    public GridRemoteViewsFactory(Context applicationContext) {
        mContext = applicationContext;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        Log.i(LOG_TAG, "onDataSetChanged called.");
        Uri PLANT_URI = PlantContract.BASE_CONTENT_URI.buildUpon().appendPath(
                PlantContract.PATH_PLANTS).build();
        if (mCursor != null) mCursor.close();
        mCursor = mContext.getContentResolver().query(
                PLANT_URI,
                null,
                null,
                null,
                PlantContract.PlantEntry.COLUMN_CREATION_TIME);
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    @Override
    public int getCount() {
        if (mCursor == null) return 0;
        return mCursor.getCount();
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public RemoteViews getViewAt(int position) {
        if ((mCursor == null) || (position < 0) || !mCursor.moveToPosition(position)) return null;
        final int idIndex = mCursor.getColumnIndex(PlantContract.PlantEntry._ID);
        final int createTimeIndex = mCursor.getColumnIndex(PlantContract
                .PlantEntry.COLUMN_CREATION_TIME);
        final int waterTimeIndex = mCursor.getColumnIndex(PlantContract
                .PlantEntry.COLUMN_LAST_WATERED_TIME);
        final int plantTypeIndex = mCursor.getColumnIndex(PlantContract
                .PlantEntry.COLUMN_PLANT_TYPE);

        final long plantId = mCursor.getLong(idIndex);
        final int plantType = mCursor.getInt(plantTypeIndex);
        final long createdAt = mCursor.getLong(createTimeIndex);
        final long wateredAt = mCursor.getLong(waterTimeIndex);
        final long timeNow = System.currentTimeMillis();

        RemoteViews views = new RemoteViews(mContext.getPackageName(),
                R.layout.plant_widget_provider);

        @DrawableRes int imgRes = PlantUtils.getPlantImageRes(
                mContext,
                timeNow - createdAt,
                timeNow - wateredAt,
                plantType);
        views.setImageViewResource(R.id.widget_plant_image, imgRes);
        views.setTextViewText(R.id.widget_plant_name, Long.valueOf(plantId).toString());
        views.setViewVisibility(R.id.widget_water_button, View.GONE);

        Bundle extras = new Bundle();
        extras.putLong(PlantDetailActivity.EXTRA_PLANT_ID, plantId);
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        views.setOnClickFillInIntent(R.id.widget_plant_image, fillInIntent);
        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
