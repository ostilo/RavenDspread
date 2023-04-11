package com.ravenpos.ravendspreadpos.utils;

import com.ravenpos.ravendspreadpos.pos.TransactionResponse;

import java.io.Serializable;

public class RavenEmv implements Serializable {
    public TransactionResponse dataModel;
    public TransactionMessage nibbsEmv;

    public  RavenEmv(){

    }
}