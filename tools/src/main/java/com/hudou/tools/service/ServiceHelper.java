package com.hudou.tools.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.hudou.tools.BuildConfig;
import com.hudou.tools.ContextHolder;
import com.hudou.tools.IToolsService;
import com.hudou.tools.ToolsServiceInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ServiceHelper {
    private static final String TAG = "ServiceHelper";
    private static final long TIMEOUT_MS = 3000;
    private static final int SERVICE_CONNECTED = 0;
    private static final int SERVICE_DISCONNECTED = 1;
    private static final long RETRY_INTERVAL_MS = 3000;
    private static final AtomicReference<IToolsService> toolsServiceRef = new AtomicReference<>();
    private static final AtomicBoolean isBinding = new AtomicBoolean(false);

    private static final Set<ServiceConnectionListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public static void init(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        context.registerReceiver(packageReceiver, filter);
    }

    private static final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBinding.set(false);
            toolsServiceRef.set(IToolsService.Stub.asInterface(service));

            executor.execute(() -> {
                notifyListeners(SERVICE_CONNECTED);
            });
            Log.d(TAG, "[Tools] Service connected.");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            toolsServiceRef.set(null);
            isBinding.set(false);
            executor.execute(() -> {
                notifyListeners(SERVICE_DISCONNECTED);
                bindService(ContextHolder.getContext());
            });
            Log.w(TAG, "[Tools] Service disconnected...");
        }
    };

    public static void registerListener(ServiceConnectionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public static void unregisterListener(ServiceConnectionListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private static void notifyListeners(int type) {
        for (ServiceConnectionListener listener : new HashSet<>(listeners)) {
            try {
                if (type == SERVICE_CONNECTED) {
                    listener.onServiceConnected();
                } else if (type == SERVICE_DISCONNECTED) {
                    listener.onServiceDisconnected();
                }
            } catch (Exception e) {
                Log.e(TAG, "[Tools] Service notification failed for listener", e);
            }
        }
    }

    public static void bindService(Context context) {
        if (!isServiceAvailable(context)) {
            isBinding.set(false);
            scheduleRetry(context);
            Log.e(TAG, "[Tools] Service is not available now, will try later.");
            return;
        }

        if (toolsServiceRef.get() != null) {
            Log.d(TAG, "[Tools] Service already bound.");
            return;
        }
        if (!isBinding.compareAndSet(false, true)) {
            Log.d(TAG, "[Tools] Already binding, skipping...");
            return;
        }

        Intent intent = new Intent(ToolsServiceInfo.SERVICE_ACTION);
        intent.setPackage(BuildConfig.SERVICE_PACKAGE);
        Log.d(TAG, "[Tools] Start to bind HAL service: " + BuildConfig.SERVICE_PACKAGE);
        try {
            boolean success;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                // 设置 executor 让服务回调在另外的线程触发
                success = context.bindService(
                        intent,
                        Context.BIND_AUTO_CREATE,
                        executor,
                        serviceConnection
                );
            } else {
                success = context.bindService(
                        intent,
                        serviceConnection,
                        Context.BIND_AUTO_CREATE
                );
            }

            Log.d(TAG, "[Tools] Binding to HAL service ret: " + success);
            if (!success) {
                Log.w(TAG, "[Tools] Initial bind failed, scheduling retry...");
                isBinding.set(false);
                scheduleRetry(context);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "[Tools] SecurityException when binding, scheduling retry...");
            isBinding.set(false);
            scheduleRetry(context);
        }
    }

    private static void scheduleRetry(Context context) {
        executor.schedule(() -> {
            bindService(context);
        }, RETRY_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public static IToolsService checkAndGetService()  {
        if (!isServiceAvailable(ContextHolder.getContext())) {
            Log.d(TAG, "[Tools] Service not available");
        }

        if (toolsServiceRef.get() != null) {
            return toolsServiceRef.get();
        }

        if (!isBinding.get()) {
            Log.d(TAG, "[Tools] Service not connected, start to connect...");
            bindService(ContextHolder.getContext());
        }

        // 如果是安卓 10 以下在主线程中调用，就不用阻塞等待了，直接返回服务未连接。安卓 10 以上的服务回调可以在子线程触发，所以即便主线程阻塞也没关系
        if (Looper.getMainLooper().isCurrentThread() && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.d(TAG, "[Tools] Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ");
        }
        waitForServiceConnected();
        return toolsServiceRef.get();
    }

    private static void waitForServiceConnected() {
        long start = SystemClock.uptimeMillis();
        final long interval = 10; // 每 10ms 轮询一次
        while (toolsServiceRef.get() == null && (SystemClock.uptimeMillis() - start) < TIMEOUT_MS) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public static IToolsService getHalService(){
        return toolsServiceRef.get();
    }

    public static void unbindService(Context context) {
        Log.d(TAG, "[Tools] Unbind service...");
        try {
            context.unbindService(serviceConnection);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "[Tools] Service not registered", e);
        }
        toolsServiceRef.set(null);
        isBinding.set(false);
    }

    public static boolean isServiceAvailable(Context context) {
        Intent intent = new Intent(ToolsServiceInfo.SERVICE_ACTION);
        intent.setPackage(BuildConfig.SERVICE_PACKAGE);
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentServices(intent, PackageManager.MATCH_ALL);
        return !resolveInfos.isEmpty();
    }

    private static final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String packageName = Objects.requireNonNull(intent.getData()).getSchemeSpecificPart();
            if (!BuildConfig.SERVICE_PACKAGE.equals(packageName)) {
                return;
            }
            String action = intent.getAction();
            if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                Log.d(TAG, "[Tools] Service package removed");
                toolsServiceRef.set(null);
            } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                Log.d(TAG, "[Tools] Service package added");
                if (toolsServiceRef.get() != null) {
                    Log.d(TAG, "[Tools] Service already bound, no need to rebind");
                    return;
                }
                boolean isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
                if (isReplacing) {
                    // 是覆盖安装，忽略（因为还会触发 REPLACED）
                    Log.d(TAG, "[Tools] Wait for REPLACED to rebind");
                    return;
                }
                // 是全新安装，执行绑定
                forceRebindService(context);
            } else if (Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
                Log.d(TAG, "[Tools] Service package replaced");
                if (toolsServiceRef.get() != null) {
                    Log.d(TAG, "[Tools] Service already bound, no need to rebind");
                    return;
                }

                forceRebindService(context);
            }
        }
    };

    public static void forceRebindService(Context context) {
        if (isBinding.get()) {
            unbindService(context);
        }

        Log.d(TAG, "[Tools] Force rebind service...");
        bindService(context);
    }
}