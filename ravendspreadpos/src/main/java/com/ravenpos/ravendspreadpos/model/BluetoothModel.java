package com.ravenpos.ravendspreadpos.model;

public class BluetoothModel {
    public String address;
    public String title;
    public int icon;

    public String serialNo;

    public  BluetoothModel(String address, String title, int icon,String serialNo){
        this.address = address;
        this.title = title; this.icon = icon;
        this.serialNo = serialNo;
    }
}
