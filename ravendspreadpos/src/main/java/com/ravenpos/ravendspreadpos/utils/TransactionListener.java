package com.ravenpos.ravendspreadpos.utils;

import com.ravenpos.ravendspreadpos.pos.TransactionResponse;
/**
 * Created by AYODEJI on 05/15/2023.
 */
public interface TransactionListener {
    public void onProcessingError(RuntimeException message, int errorcode);
    public void onCompleteTransaction(TransactionResponse response);
}