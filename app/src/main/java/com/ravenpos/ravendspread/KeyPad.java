package com.ravenpos.ravendspread;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ravenpos.ravendspreadpos.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;


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
       // findViewById(R.id.btn_dot).setOnClickListener(this);

        ArrayList<Integer> listRand = new ArrayList<Integer>(Arrays.asList(0,1,2,3,4,5,6,7,8,9));
        Collections.shuffle(listRand);
        // findViewById(R.id.btn_r).setOnClickListener(this);

        TextView textView =  (TextView)((FrameLayout)findViewById(R.id.btn_0)).getChildAt(0);
        String realResult = listRand.get(0).toString();
        textView.setText(realResult);

        TextView textView1 =  (TextView)((FrameLayout)findViewById(R.id.btn_1)).getChildAt(0);
        String realResult1 = listRand.get(1).toString();
        textView1.setText(realResult1);

        TextView textView2 =  (TextView)((FrameLayout)findViewById(R.id.btn_2)).getChildAt(0);
        String realResult2 = listRand.get(2).toString();
        textView2.setText(realResult2);

        TextView textView3 =  (TextView)((FrameLayout)findViewById(R.id.btn_3)).getChildAt(0);
        String realResult3 = listRand.get(3).toString();
        textView3.setText(realResult3);

        TextView textView4 =  (TextView)((FrameLayout)findViewById(R.id.btn_4)).getChildAt(0);
        String realResult4 = listRand.get(4).toString();
        textView4.setText(realResult4);

        TextView textView5 =  (TextView)((FrameLayout)findViewById(R.id.btn_5)).getChildAt(0);
        String realResult5 = listRand.get(5).toString();
        textView5.setText(realResult5);

        TextView textView6 =  (TextView)((FrameLayout)findViewById(R.id.btn_6)).getChildAt(0);
        String realResult6 = listRand.get(6).toString();
        textView6.setText(realResult6);

        TextView textView7 =  (TextView)((FrameLayout)findViewById(R.id.btn_7)).getChildAt(0);
        String realResult7 = listRand.get(7).toString();
        textView7.setText(realResult7);

        TextView textView8 =  (TextView)((FrameLayout)findViewById(R.id.btn_8)).getChildAt(0);
        String realResult8 = listRand.get(8).toString();
        textView8.setText(realResult8);

        TextView textView9 =  (TextView)((FrameLayout)findViewById(R.id.btn_9)).getChildAt(0);
        String realResult9 = listRand.get(9).toString();
        textView9.setText(realResult9);



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
//            if(id == R.id.btn_Del)
//                processDelete();
//            else if(id == R.id.btn_dot){
//                if(displayView.getText().toString().contains("."))
//                    return;
//                else
//                    processInput(".");
//            }
//            else{
//                TextView textView =  (TextView)((FrameLayout)view).getChildAt(0);
//                processInput(textView.getText().toString());
//            }

    }
}
