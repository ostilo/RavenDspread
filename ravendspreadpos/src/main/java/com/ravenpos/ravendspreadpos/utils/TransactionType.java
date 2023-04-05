package com.ravenpos.ravendspreadpos.utils;

import java.io.Serializable;

/**
 * Created by AYODEJI on 4/3/2023.
 */

public enum TransactionType implements Serializable {

    PURCHASE("00"), CASH_ADVANCE("01"), CASH_BACK("09"), BALANCE_ENQUIRY("31"), REFUND("20"), REVERSAL("32"), FUND_WALLET("50"), PAY_BILL("60");

    private String code;


    TransactionType(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }
    public String getTransactionTypeTitle(){
        switch (code){
            case "00":
                return "POS Transaction";
            case "01":
                return "Cash Advance";
            case "09":
                return "Cash Back";
            case "31":
                return "Balance Enquiry";
            case "20":
                return "Refund Transaction";
            case "32":
                return "Reversal Transaction";
            case "50":
                return "Fund Wallet";
            case "60":
                return "Pay Bill";
            default:
                return "Unknown Transaction";
        }
    }
}
