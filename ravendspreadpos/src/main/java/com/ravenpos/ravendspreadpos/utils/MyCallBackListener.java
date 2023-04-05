package com.ravenpos.ravendspreadpos.utils;

public interface MyCallBackListener {
    void onProcessComplete(String result, int callCode);
    void onErrorOccur(String error, int callCode);
}
