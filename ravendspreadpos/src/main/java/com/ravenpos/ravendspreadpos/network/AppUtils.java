package com.ravenpos.ravendspreadpos.network;

public class AppUtils {
    static final String BASE_URL = "https://posapi.getravenbank.com/v1/";
    public static Baas getAPIService() {
        return RetrofitClient.getClient().create(Baas.class);
    }
}