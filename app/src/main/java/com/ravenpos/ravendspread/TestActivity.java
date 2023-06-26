package com.ravenpos.ravendspread;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.airbnb.lottie.LottieAnimationView;
import com.ravenpos.ravendspreadpos.utils.RavenExtensions;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lottie_test);

        int originalColour  = getResources().getColor(R.color.primary_color);

        int generatedColor1 = RavenExtensions.INSTANCE.generateTransparentColor(originalColour, 0.7);
        int generatedColor2 = RavenExtensions.INSTANCE.generateTransparentColor(originalColour, 0.4);
        int generatedColor3 = RavenExtensions.INSTANCE.generateTransparentColor(originalColour, 0.2);

        LottieAnimationView lottieAnimationView = findViewById(R.id.mainSpace);
//Bluetooth middle layer shape group
        //Bluetooth last layer
        RavenExtensions.INSTANCE.resetAnimationView(lottieAnimationView);
        //RavenExtensions.INSTANCE.addAnimationView(lottieAnimationView,getResources().getColor(R.color.primary_color),"Bluetooth layer");
        RavenExtensions.INSTANCE.addAnimationView(lottieAnimationView,getResources().getColor(R.color.insufficient_balance),"Bluetooth middle layer");
        RavenExtensions.INSTANCE.addAnimationView(lottieAnimationView,getResources().getColor(R.color.wallet_deep_green),"Bluetooth last layer");
        RavenExtensions.INSTANCE.addAnimationView(lottieAnimationView,getResources().getColor(R.color.purple_700),"Bluetooth middle layer shape group");

        //Bluetooth layer shape group

    }

}