package com.ravenpos.ravendspreadpos.utils;

import android.util.Log;

import androidx.annotation.Keep;

/**
 * Created by AYODEJI on 10/10/2020.
 *
 */
@Keep
public class AppLog {
    /**
     * 控制android.util.Log.v是否输出
     */
    private static boolean DEBUG_V = false;
    /**
     * 控制android.util.Log.d是否输出
     */
    private static boolean DEBUG_D = false;
    /**
     * 控制android.util.Log.i是否输出
     */
    private static boolean DEBUG_I = false;
    /**
     * 控制android.util.Log.w是否输出
     */
    private static boolean DEBUG_W = false;
    /**
     * 控制android.util.Log.e是否输出
     */
    private static boolean DEBUG_E = false;

    public enum EDebugLevel {
        DEBUG_V,
        DEBUG_D,
        DEBUG_I,
        DEBUG_W,
        DEBUG_E,
    }

    /**
     * 同时控制V/D/I/W/E 5种输出开关
     * @param debugFlag 开关, true打开, false关闭
     */
    public static void debug(boolean debugFlag) {
        DEBUG_V = debugFlag;
        DEBUG_D = debugFlag;
        DEBUG_I = debugFlag;
        DEBUG_W = debugFlag;
        DEBUG_E = debugFlag;
    }

    /**
     * 分别控制V/D/I/W/E 5种输出开关
     * @param debugFlag 开关, true打开, false关闭
     */
    public static void debug(EDebugLevel debugLevel, boolean debugFlag) {
        if (debugLevel == EDebugLevel.DEBUG_V) {
            DEBUG_V = debugFlag;
        } else if (debugLevel == EDebugLevel.DEBUG_D) {
            DEBUG_D = debugFlag;
        } else if (debugLevel == EDebugLevel.DEBUG_I) {
            DEBUG_I = debugFlag;
        } else if (debugLevel == EDebugLevel.DEBUG_W) {
            DEBUG_W = debugFlag;
        } else if (debugLevel == EDebugLevel.DEBUG_E) {
            DEBUG_E = debugFlag;
        }
    }

    /**
     * 输出v级别log, 内部根据设置的开关决定是否真正输出log
     * @param tag   同系统android.util.log的tag定义
     * @param msg   待输出的信息
     */
    public static void v(String tag, String msg) {
        if (DEBUG_V) {
            Log.v(tag, msg);
        }
    }

    /**
     * 输出d级别log, 内部根据设置的开关决定是否真正输出log
     * @param tag   同系统android.util.log的tag定义
     * @param msg   待输出的信息
     */
    public static void d(String tag, String msg) {
        if (DEBUG_D) {
            Log.d(tag, msg);
        }
    }

    /**
     * 输出i级别log, 内部根据设置的开关决定是否真正输出log
     * @param tag   同系统android.util.log的tag定义
     * @param msg   待输出的信息
     */
    public static void i(String tag, String msg) {
        if (DEBUG_I) {
            Log.i(tag, msg);
        }
    }

    /**
     * 输出w级别log, 内部根据设置的开关决定是否真正输出log
     * @param tag   同系统android.util.log的tag定义
     * @param msg   待输出的信息
     */
    public static void w(String tag, String msg) {
        if (DEBUG_W) {
            Log.w(tag, msg);
        }
    }

    /**
     * 输出e级别log, 内部根据设置的开关决定是否真正输出log
     * @param tag   同系统android.util.log的tag定义
     * @param msg   待输出的信息
     */
    public static void e(String tag, String msg) {
        if (DEBUG_E) {
            Log.e(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable e) {
        if (DEBUG_E) {
            Log.e(tag, msg, e);
        }
    }

}
