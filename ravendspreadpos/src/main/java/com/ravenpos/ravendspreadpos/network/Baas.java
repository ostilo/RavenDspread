package com.ravenpos.ravendspreadpos.network;

import com.ravenpos.ravendspreadpos.model.ParamDownloadData;
import com.ravenpos.ravendspreadpos.utils.TransactionMessage;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface Baas {

    @GET("card/processing")
    Call<ParamDownloadData> paramDownloadAsync(@Header("x-serial-no") String serialNo, @Header("x-brand") String brand, @Header("x-device-model") String model, @Header("x-app-version") String version);

    @POST("card/processing")
    Call<Object> performTransaction(@Header("x-serial-no") String serialNo, @Header("x-brand") String brand, @Header("x-device-model") String model, @Header("x-app-version") String version, @Body TransactionMessage transactionMessage);

}
