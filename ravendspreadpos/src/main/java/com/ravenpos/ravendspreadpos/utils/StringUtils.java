package com.ravenpos.ravendspreadpos.utils;

import androidx.annotation.Keep;

import com.ravenpos.ravendspreadpos.pos.TransactionResponse;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by AYODEJI on 05/19/2022.
 */
@Keep
public class StringUtils {
    public static String getNairaUnitFormat(String amount) {
        try {
            String amounts = amount + "D";
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.CANADA);
            String currency = format.format(Double.parseDouble(amounts));
            return currency.replace("$", "");
        }catch (Exception e){
            return amount;
        }
    }

    public static TransactionResponse getTransactionTesponse(String message, int code) {
        TransactionResponse response = new TransactionResponse();
        response.responseCode = String.valueOf(code);
        response.responseDescription = message;
        return  response;
    }

}
