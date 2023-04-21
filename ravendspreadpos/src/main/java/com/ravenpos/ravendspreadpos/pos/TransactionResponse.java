package com.ravenpos.ravendspreadpos.pos;

import java.io.Serializable;
/**
 * Created by AYODEJI on 05/15/2023.
 */
public class TransactionResponse implements Serializable {
    public String TransactionType;
    public String AID ;
    public String ExpireDate ;
    public String CardHolderName ;
    public String CardType;
    public String AccountType ;
    public String responseCode ;
    public String responseDescription ;
    public String PAN;
    public String amount ;
    public String TLV;
    public String CardNo;
    public String CardOrg;
    public String CardSequenceNumber;
    public  String CardIssuerCountryCode;
    public String CardServiceCode;
    public String Track2;
    public String MacAddress;
    public String PinBlock;
    public String totalAmoount;
    public String TerminaID;
    public  String IccData;
    public  String RRN;
}