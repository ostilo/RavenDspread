package com.ravenpos.ravendspreadpos.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.ravenpos.ravendspreadpos.R;

public class MyHandler extends Handler {
    public MyHandler(Context context, boolean dismissable){
        this.dismissable = dismissable;
        this.context = context;
        progressDialog = new ProgressDialog(context);
        builder = new AlertDialog.Builder(context);
    }
    ProgressDialog progressDialog;
    private Context context;
    private boolean dismissable;
    private AlertDialog.Builder builder;

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what){
            case 0:
                progressDialog.setMessage(msg.obj == null ? context.getString(R.string.defaultloading) : msg.obj.toString());
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setCancelable(dismissable);
                progressDialog.show();
                break;
            case 1:
                if(progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
                if(msg.obj != null)
                    Toast.makeText(context, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                break;
            case 2:
                if(progressDialog != null && progressDialog.isShowing())
                    progressDialog.setMessage(msg.obj == null ? context.getString(R.string.defaultloading) : msg.obj.toString() );
                break;
            case 3:
                if(progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
                builder.setTitle("Message");
                builder.setMessage(msg.obj == null ? context.getString(R.string.defaultloading): msg.obj.toString());
                if(msg.arg2 == 1)
                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    });
                else
                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                builder.show();
                break;
        }
    }
}