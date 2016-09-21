package com.buahbatu.toyotasalesman.network;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Created by maaakbar on 9/21/16.
 */
public interface ToyotaService {
    @Multipart
    @POST("login_mobile")
    Call<ResponseBody> loginStandar(@Part("imei") RequestBody imei,
                             @Part("usr") RequestBody username,
                             @Part("pass") RequestBody password);

    @Multipart
    @POST("login_mobile")
    Call<ResponseBody> loginStandar2(@Part("imei") RequestBody imei,
                                    @Part("usr") RequestBody username,
                                    @Part("pass") RequestBody password);
}
