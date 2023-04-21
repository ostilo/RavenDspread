package com.ravenpos.ravendspreadpos.model;

import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Keep
public class ParamDownloadData {

    @SerializedName("encpinkey")
    @Expose
    private String encpinkey;
    @SerializedName("encsesskey")
    @Expose
    private String encsesskey;
    @SerializedName("clrpinkey")
    @Expose
    private String clrpinkey;
    @SerializedName("port")
    @Expose
    private String port;
    @SerializedName("ip")
    @Expose
    private String ip;
    @SerializedName("clrsesskey")
    @Expose
    private String clrsesskey;
    @SerializedName("encmasterkey")
    @Expose
    private String encmasterkey;
    @SerializedName("paramdownload")
    @Expose
    private String paramdownload;
    @SerializedName("tid")
    @Expose
    private String tid;
    @SerializedName("clrmasterkey")
    @Expose
    private String clrmasterkey;

    public String getEncpinkey() {
        return encpinkey;
    }

    public void setEncpinkey(String encpinkey) {
        this.encpinkey = encpinkey;
    }

    public String getEncsesskey() {
        return encsesskey;
    }

    public void setEncsesskey(String encsesskey) {
        this.encsesskey = encsesskey;
    }

    public String getClrpinkey() {
        return clrpinkey;
    }

    public void setClrpinkey(String clrpinkey) {
        this.clrpinkey = clrpinkey;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getClrsesskey() {
        return clrsesskey;
    }

    public void setClrsesskey(String clrsesskey) {
        this.clrsesskey = clrsesskey;
    }

    public String getEncmasterkey() {
        return encmasterkey;
    }

    public void setEncmasterkey(String encmasterkey) {
        this.encmasterkey = encmasterkey;
    }

    public String getParamdownload() {
        return paramdownload;
    }

    public void setParamdownload(String paramdownload) {
        this.paramdownload = paramdownload;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getClrmasterkey() {
        return clrmasterkey;
    }

    public void setClrmasterkey(String clrmasterkey) {
        this.clrmasterkey = clrmasterkey;
    }

}