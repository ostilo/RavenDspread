package com.ravenpos.ravendspreadpos.pos;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.ravenpos.ravendspreadpos.utils.TransactionListener;
import com.ravenpos.ravendspreadpos.utils.TransactionType;

import java.util.List;

public class EmvTransactionHelper {
    private static MyPosListener myPosListener;

    public static void startTransaction(final Activity activity, final String bluetoothAddress, final Double amount, MutableLiveData<String> listenerMutable, final TransactionListener listener){
        if(myPosListener == null){
            listener.onProcessingError(new RuntimeException("Not yet initialized"), -100);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                TransactionType finalTransactionType = TransactionType.PURCHASE;
                myPosListener.startTransaction(bluetoothAddress, amount, finalTransactionType, null, listenerMutable,new TransactionListener() {
                    @Override
                    public void onProcessingError(final RuntimeException message, final int errorcode) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                close();
                                Log.d("TAG", "Version 1.0: " + message.getMessage());
                                listener.onProcessingError(message, errorcode);
                            }
                        });
                    }

                    @Override
                    public void onCompleteTransaction(final TransactionResponse response) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                close();
                                Log.d("TAG", "Version 1.0: " + response.responseDescription);
                                listener.onCompleteTransaction(response);
                            }
                        });
                    }
                });
            }
        }).start();
    }


    public static void initialize(Context context) {
        myPosListener = myPosListener != null ? myPosListener :  MyPosListener.initialize(context);
    }
    private static void close(){
        try {
            myPosListener.close();
            Thread.sleep(1000);
        }catch (Exception e){}
    }



}
