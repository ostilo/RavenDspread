package com.ravenpos.ravendspreadpos;

import android.app.Application;
import android.content.Context;

import xcrash.XCrash;

public class BaseApplication extends Application {

    private static BaseApplication INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        //  Default init
        XCrash.init(this);
    }
    public static BaseApplication getINSTANCE(){
        return INSTANCE;
    }



}
