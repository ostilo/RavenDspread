package com.ravenpos.ravendspreadpos.network;

import com.ravenpos.ravendspreadpos.model.BaseData;
import com.ravenpos.ravendspreadpos.model.BluetoothResponse;
import com.ravenpos.ravendspreadpos.utils.BluetoothSearch;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface Baas {

    @POST("pdon/get_business_serial")
    Call<BaseData<BluetoothResponse>> performBluSearch(@Body BluetoothSearch transactionMessage);

}
