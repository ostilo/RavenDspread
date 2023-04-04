package com.ravenpos.ravendspreadpos;

import android.app.Application;
import android.content.Context;

import xcrash.XCrash;

public class BaseApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //  Default init
        XCrash.init(this);
    }

}
