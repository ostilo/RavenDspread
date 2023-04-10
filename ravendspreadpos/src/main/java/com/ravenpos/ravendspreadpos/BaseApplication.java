package com.ravenpos.ravendspreadpos;

import android.app.Application;
import android.content.Context;

import xcrash.XCrash;

public class BaseApplication extends Application {

    private static final String TAG = "MyApplication";
    private static BaseApplication INSTANCE;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //  Default init
        XCrash.init(this);
       // MultiDex.install(this);
    }

    public static BaseApplication getINSTANCE(){
        return INSTANCE;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
    }
}
