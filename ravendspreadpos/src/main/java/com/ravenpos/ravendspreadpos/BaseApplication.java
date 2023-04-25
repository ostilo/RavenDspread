package com.ravenpos.ravendspreadpos;

import android.app.Application;
import android.content.Context;

import com.ravenpos.ravendspreadpos.utils.AppLog;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import xcrash.XCrash;

public class BaseApplication extends Application {

    private static BaseApplication INSTANCE;
    public static Context getApplicationInstance;
    private static String mPosID;

    public static String getmPosID() {
        return mPosID;
    }

    public static void setmPosID(String mPosID) {
        BaseApplication.mPosID = mPosID;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        AppLog.debug(true);
        getApplicationInstance = this;
       // CrashHandler.getInstance().init(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //  Default init
        XCrash.init(this);
//        OkGo.getInstance().init(this);
//        initXHttp();
//
//        initOKHttpUtils();
//        initAppUpDate();
    }
    public static BaseApplication getINSTANCE(){
        return INSTANCE;
    }


    private void initOKHttpUtils() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(20000L, TimeUnit.MILLISECONDS)
                .readTimeout(20000L, TimeUnit.MILLISECONDS)
                .build();
        //OkHttpUtils.initClient(okHttpClient);
    }

    private void initXHttp() {
        //初始化网络请求框架，必须首先执行
//        XHttpSDK.init(this);
//        //需要调试的时候执行
//        XHttpSDK.debug("XHttp");
//        XHttp.getInstance().setTimeout(20000);
    }

    private void initAppUpDate() {

//        XUpdate.get()
//                .debug(true)
//                .isWifiOnly(true)                                               // By default, only version updates are checked under WiFi
//                .isGet(true)                                                    // The default setting uses Get request to check versions
//                .isAutoMode(false)                                              // The default setting is non automatic mode
//                .param("versionCode", UpdateUtils.getVersionCode(this))         // Set default public request parameters
//                .param("appKey", getPackageName())
//                .setOnUpdateFailureListener(new OnUpdateFailureListener() {     // Set listening for version update errors
//                    @Override
//                    public void onFailure(UpdateError error) {
//                        if (error.getCode() != CHECK_NO_NEW_VERSION) {          // Handling different errors
//                            ToastUtils.toast(error.toString());
//                        }
//                    }
//                })
//                .supportSilentInstall(true)                                     // Set whether silent installation is supported. The default is true
//                .setIUpdateHttpService(new OKHttpUpdateHttpService())           // This must be set! Realize the network request function.
//                .init(this);
    }



}


