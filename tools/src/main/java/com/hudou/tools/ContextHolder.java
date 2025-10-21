package com.hudou.tools;

import android.content.Context;

public class ContextHolder {
    private static volatile Context appContext;

    private ContextHolder() {
    }

    public static void init(Context context) {
        if (appContext == null && context != null) {
            synchronized (ContextHolder.class) {
                if (appContext == null) {
                    appContext = context.getApplicationContext();
                }
            }
        }
    }

    public static Context getContext() {
        if (appContext == null) {
            throw new IllegalStateException("ContextHolder is not initialized. ");
        }
        return appContext;
    }
}
