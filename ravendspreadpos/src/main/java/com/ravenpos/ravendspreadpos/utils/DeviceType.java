package com.ravenpos.ravendspreadpos.utils;

public enum DeviceType {
    BLUETOOTH("00"), OTG_CORD("01");
    private String code;

    DeviceType(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }
}
