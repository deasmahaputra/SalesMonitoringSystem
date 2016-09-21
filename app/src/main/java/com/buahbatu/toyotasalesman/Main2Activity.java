package com.buahbatu.toyotasalesman;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.buahbatu.toyotasalesman.network.NetHelper;
import com.buahbatu.toyotasalesman.network.ToyotaService;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class Main2Activity extends AppCompatActivity {
    private final static int requestPhonePermission = 0;
    private final static String TAG = "MainActivity";

    @BindView(R.id.username_text) TextView mUserText;
    @BindView(R.id.pass_text) TextView mPassText;

    @OnClick(R.id.logo_view) void logoOnClick(){
        new AlertDialog.Builder(this).setTitle("Phone IMEI")
                .setMessage("Your imei " + AppConfig.getImeiNum(this))
                .setPositiveButton("Ok got it!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @OnClick(R.id.login_button) void loginOnClick(){
        if (TextUtils.isEmpty(mUserText.getText())){
            mUserText.setError(getString(R.string.error_field));
            return;
        }
        if (TextUtils.isEmpty(mPassText.getText())){
            mPassText.setError(getString(R.string.error_field));
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(NetHelper.getDomainAddress(this))
                .build();

        ToyotaService toyotaService = retrofit.create(ToyotaService.class);
        toyotaService.login(RequestBody.create(MediaType.parse("text/plain"), AppConfig.getImeiNum(this)),
                RequestBody.create(MediaType.parse("text/plain"), mUserText.getText().toString()),
                RequestBody.create(MediaType.parse("text/plain"), mPassText.getText().toString()))
                .enqueue(stringCallback);
    }

    Callback<ResponseBody> stringCallback = new Callback<ResponseBody>() {
        @Override
        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
            try {
                Log.d(TAG, "onResponse: "+response.body().string());
            }catch (IOException e){};

        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        if (AppConfig.getLoginStatus(this)) {
            startActivity(new Intent(this, TrackingActivity.class));
            finish();
        }else
            ButterKnife.bind(this);
        if (!checkForPermission())
            askForPermission();
    }

    private boolean checkForPermission(){
        return checkCallingOrSelfPermission("android.permission.READ_PHONE_STATE") == PackageManager.PERMISSION_GRANTED
                && checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED
                && checkCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE") == PackageManager.PERMISSION_GRANTED
                && checkCallingOrSelfPermission("android.permission.INTERNET") == PackageManager.PERMISSION_GRANTED;
    }
    private void askForPermission(){
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE
        }, requestPhonePermission);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == requestPhonePermission){
            for (int result:grantResults) {
                if (result==PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, getString(R.string.service_not_enabled), Toast.LENGTH_SHORT)
                            .show();
                    break;
                }
            }
        }
    }
}
