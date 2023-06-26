package com.ravenpos.ravendspreadpos.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class BaseData<T> {
    @SerializedName("message")
    @Expose
    private String message = "";

    @SerializedName("data")
    @Expose
    private T data = null;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public BaseData(T data) {
        setData(data);
        setMessage("");
    }

    public BaseData(String message) {
        setMessage(message);
        setData(null);
    }
}
