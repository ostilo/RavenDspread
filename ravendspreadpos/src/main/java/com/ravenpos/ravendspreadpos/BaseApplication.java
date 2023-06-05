package com.ravenpos.ravendspreadpos;

import android.app.Application;
import android.content.Context;

import com.ravenpos.ravendspreadpos.utils.AppLog;
public class BaseApplication extends Application {

    private static Context getApplicationInstance;
    public static void setContext(Context cntxt) {
        getApplicationInstance = cntxt;
    }

//    private static BaseApplication INSTANCE;
//    public static Context getApplicationInstance;
//    private static String mPosID;

//    public static String getmPosID() {
//        return mPosID;
//    }

//    public static void setmPosID(String mPosID) {
//        BaseApplication.mPosID = mPosID;
//    }

    @Override
    public void onCreate() {
        super.onCreate();
       /// INSTANCE = this;
        AppLog.debug(true);
        getApplicationInstance = this;
       // CrashHandler.getInstance().init(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //  Default init
       // XCrash.init(this);
//        OkGo.getInstance().init(this);
//        initXHttp();
//
//        initOKHttpUtils();
//        initAppUpDate();
    }
//    public static BaseApplication getINSTANCE(){
//        return INSTANCE;
//    }

    public static Context getInstance() {
        return getApplicationInstance;
    }


}


