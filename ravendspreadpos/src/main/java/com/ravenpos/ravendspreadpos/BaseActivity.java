package com.ravenpos.ravendspreadpos;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

/**
 * Created by AYODEJI on 05/15/2023.
 */
public class BaseActivity extends AppCompatActivity {

    private BottomSheetDialog cancelDialog;
    private MaterialButton btnContinue;
    private final String LOG_TAG = BaseActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeSheet();
    }
    private void initializeSheet(){
        cancelDialog = new BottomSheetDialog(this);
        cancelDialog.setContentView(R.layout.not_supported);
        btnContinue = cancelDialog.findViewById(R.id.btnDonCancel);
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelDialog.dismiss();
            }
        });

    }

    void toastPendingSheet() {
        cancelDialog.show();
    }

    protected void showResult(final TextView textView, final String text) {
        Log.d(LOG_TAG,text);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(text);
            }
        });
    }
}