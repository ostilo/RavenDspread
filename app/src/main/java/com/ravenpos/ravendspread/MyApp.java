package com.ravenpos.ravendspread;

import android.app.Application;

import com.ravenpos.ravendspreadpos.BaseApplication;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BaseApplication.setContext(this);
    }
}
