package com.ravenpos.ravendspreadpos.utils.utils;

import android.app.Activity;
import android.widget.Button;

import com.binioter.guideview.Guide;
import com.binioter.guideview.GuideBuilder;
import com.ravenpos.ravendspreadpos.widget.SimpleComponent;

/**
 * Created by Qianmeng on 2020/3/10
 * Edited by Qianmeng on 2020/3/10
 */
public class ShowGuideView {
    public void show(final Button button, final Activity context,String msg){
        GuideBuilder builder = new GuideBuilder();
        builder.setTargetView(button)
                .setAlpha(150)
                .setHighTargetCorner(20)
                .setHighTargetPadding(8);
        builder.setOnVisibilityChangedListener(new GuideBuilder.OnVisibilityChangedListener() {
            @Override
            public void onShown() {
            }

            @Override
            public void onDismiss() {
                listener.onGuideListener(button);
            }
        });
        SimpleComponent simpleComponent = new SimpleComponent();
        builder.addComponent(simpleComponent);
        Guide guide = builder.createGuide();
        guide.show(context);
        simpleComponent.setText(msg);
    }


    /**
     * define an interface
     */
    public interface  onGuideViewListener{
        void onGuideListener(Button btn);
    }
    /**
     *define a veriable
     */
    private onGuideViewListener listener;
    /**
     *provide a public method and init interface type data
     */
    public void setListener(onGuideViewListener listener){
        this.listener = listener;
    }

}
