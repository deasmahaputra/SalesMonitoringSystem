package com.buahbatu.toyotasalesman.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;


public interface ToyotaService {
    @Multipart
    @POST("login_mobile")
    Call<ResponseBody> login(@Part("imei") String imei,
                             @Part("usr") String username,
                             @Part("pass") String password);

    @Multipart
    @POST("logout")
    Call<ResponseBody> logout(@Part("coor") String coor,
                             @Part("usr") String username,
                             @Part("alamat") String alamat);

    @Multipart
    @POST("inputKoordinate")
    Call<ResponseBody> report(@Part("coor") String coor,
                              @Part("usr") String username,
                              @Part("alamat") String alamat);
}
