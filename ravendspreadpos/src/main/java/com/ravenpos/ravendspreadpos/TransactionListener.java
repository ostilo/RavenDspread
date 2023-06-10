package com.ravenpos.ravendspreadpos;

public interface TransactionListener {
    void onProcessingError(RuntimeException var1, int var2);

    void onCompleteTransaction(String var1);
}
