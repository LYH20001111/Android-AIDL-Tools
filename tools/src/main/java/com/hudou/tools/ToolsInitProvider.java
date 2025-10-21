package com.hudou.tools;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hudou.tools.service.ServiceHelper;

public class ToolsInitProvider extends ContentProvider {
    private static final String TAG = "ToolsInitProvider";

    @Override
    public boolean onCreate() {
        Log.d(TAG, "[Tools] ToolsInitProvider onCreate start.");
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "[Tools] ToolsInitProvider context is null.");
            return false;
        }

        ContextHolder.init(context.getApplicationContext());
        ServiceHelper.init(context);
        ServiceHelper.bindService(context);

        Log.d(TAG, "[Tools] ToolsInitProvider onCreate end.");
        return true;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "";
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        return null;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] strings, @Nullable String s, @Nullable String[] strings1, @Nullable String s1) {
        return null;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }
}
