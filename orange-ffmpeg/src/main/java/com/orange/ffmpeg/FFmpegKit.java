package com.orange.ffmpeg;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

public final class FFmpegKit {

    private static final String TAG = "FFmpegKit";

    public static final int RESULT_OK = 0;
    public static final int RESULT_NOT_INITIALIZED = -1;
    public static final int RESULT_LIBRARY_LOAD_FAILED = -2;
    public static final int RESULT_EXECUTE_FAILED = -3;
    public static final int RESULT_CANCELLED = -4;

    private static final AtomicBoolean sLibraryLoaded = new AtomicBoolean(false);
    private static final AtomicBoolean sInitialized = new AtomicBoolean(false);
    private static final AtomicBoolean sCancelled = new AtomicBoolean(false);
    private static final AtomicBoolean sStubMode = new AtomicBoolean(false);


    private FFmpegKit() {
    }

    public static synchronized int init() {
        if (sInitialized.get()) {
            return RESULT_OK;
        }
        boolean loaded = ensureLibraryLoaded();
        if (!loaded) {
            Log.w(TAG, "native library not found, switch to stub mode");
            sStubMode.set(true);
            sInitialized.set(true);
            sCancelled.set(false);
            return RESULT_OK;
        }
        try {
            int ret = nativeInit();
            if (ret == 0) {
                sInitialized.set(true);
                sCancelled.set(false);
                sStubMode.set(false);
                return RESULT_OK;
            }
            return ret;
        } catch (Throwable t) {
            Log.e(TAG, "init failed", t);
            return RESULT_EXECUTE_FAILED;
        }
    }


    public static int execute(String[] args) {
        if (!sInitialized.get()) {
            return RESULT_NOT_INITIALIZED;
        }
        if (sCancelled.get()) {
            return RESULT_CANCELLED;
        }
        if (sStubMode.get()) {
            return executeInStubMode(args);
        }
        try {
            return nativeExecute(args);
        } catch (Throwable t) {
            Log.e(TAG, "execute failed", t);
            return RESULT_EXECUTE_FAILED;
        }
    }


    public static void executeAsync(final String[] args, final FFmpegCallback callback) {
        new Thread(() -> {
            int code = execute(args);
            if (callback != null) {
                callback.onComplete(code, code == RESULT_OK ? "success" : "failed:" + code);
            }
        }, "orange-ffmpeg-exec").start();
    }

    public static void cancel() {
        sCancelled.set(true);
        if (!sInitialized.get()) {
            return;
        }
        try {
            nativeCancel();
        } catch (Throwable t) {
            Log.w(TAG, "cancel ignored", t);
        }
    }

    public static String getVersion() {
        if (!sInitialized.get()) {
            return "not_initialized";
        }
        if (sStubMode.get()) {
            return "stub-0.1";
        }
        try {
            String version = nativeGetVersion();
            return version == null ? "unknown" : version;
        } catch (Throwable t) {
            Log.w(TAG, "getVersion failed", t);
            return "unknown";
        }
    }


    public static boolean isInitialized() {
        return sInitialized.get();
    }

    public static boolean isStubMode() {
        return sStubMode.get();
    }

    private static int executeInStubMode(String[] args) {
        if (args == null || args.length == 0) {
            return RESULT_EXECUTE_FAILED;
        }
        for (String arg : args) {
            if ("-version".equals(arg)) {
                return RESULT_OK;
            }
        }
        return RESULT_EXECUTE_FAILED;
    }

    private static boolean ensureLibraryLoaded() {

        if (sLibraryLoaded.get()) {
            return true;
        }
        try {
            System.loadLibrary("orangeffmpegkit");
            sLibraryLoaded.set(true);
            return true;
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "load library failed", e);
            return false;
        }
    }

    private static native int nativeInit();

    private static native int nativeExecute(String[] args);

    private static native void nativeCancel();

    private static native String nativeGetVersion();
}
