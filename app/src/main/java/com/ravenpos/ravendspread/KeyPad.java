package com.ravenpos.ravendspread;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.ravenpos.ravendspreadpos.R;


public class KeyPad extends FrameLayout implements View.OnClickListener {
    private TextView displayView;

    public KeyPad(Context context){
        super(context);
        init();

    }
    public KeyPad(Context context, AttributeSet attrs){
        super(context, attrs);
        init();
    }
    public KeyPad(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        init();
    }




    private void init(){
        View view =  LayoutInflater.from(getContext()).inflate(R.layout.keypad, null);
        addView(view);
        findViewById(R.id.btn_0).setOnClickListener(this);
        findViewById(R.id.btn_1).setOnClickListener(this);
        findViewById(R.id.btn_2).setOnClickListener(this);
        findViewById(R.id.btn_3).setOnClickListener(this);
        findViewById(R.id.btn_4).setOnClickListener(this);
        findViewById(R.id.btn_5).setOnClickListener(this);
        findViewById(R.id.btn_6).setOnClickListener(this);
        findViewById(R.id.btn_7).setOnClickListener(this);
        findViewById(R.id.btn_8).setOnClickListener(this);
        findViewById(R.id.btn_9).setOnClickListener(this);
        findViewById(R.id.btn_Del).setOnClickListener(this);
        findViewById(R.id.btn_dot).setOnClickListener(this);
    }

    public void registerDisplayDelegate(TextView displayTextView){
        this.displayView = displayTextView;
    }





    private void processInput(String input){

        if (!input.isEmpty()) {
            String currentString =  displayView.getText().toString().replace("\\,", "");

            if(TextUtils.isEmpty(currentString)){ //Empty assign and return
                displayView.setText(input);
                return;
            }

            if(currentString.contains(".")){ //Ensure only 2 decimal places
                if((currentString.substring(currentString.indexOf('.'), currentString.length())).length() > 2) return;
            }

            currentString += input;  //concatenate
            displayView.setText(currentString);
        }

    }

    private void processDelete() {
        String currentString = displayView.getText().toString().trim();

        if (!TextUtils.isEmpty(currentString)) {
            currentString = currentString.substring(0, currentString.length() -1);
        }

        displayView.setText(currentString);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
            if(id == R.id.btn_Del)
                processDelete();
            else if(id == R.id.btn_dot){
                if(displayView.getText().toString().contains("."))
                    return;
                else
                    processInput(".");
            }
            else{
                TextView textView =  (TextView)((FrameLayout)view).getChildAt(0);
                processInput(textView.getText().toString());
            }

    }
}
