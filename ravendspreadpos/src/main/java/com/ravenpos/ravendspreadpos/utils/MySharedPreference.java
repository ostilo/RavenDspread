package com.ravenpos.ravendspreadpos.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.ravenpos.ravendspreadpos.R;

public class MySharedPreference {
    private Context context;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private String name  = "";
    public MySharedPreference(Context context){
        this.context = context;
        pref =  context.getSharedPreferences(name, Context.MODE_PRIVATE); //PreferenceManager.getDefaultSharedPreferences(context);
        editor = pref.edit();
    }

    public void setLastMacAddress(String macAddress){
        editor.putString(context.getString(R.string.mposmacaddress), macAddress);
        editor.commit();
    }

    public String getLastMacAddress(){
        return pref.getString(context.getString(R.string.mposmacaddress), null);
    }

    public void signOut() {
        editor.clear().commit();

    }
}
