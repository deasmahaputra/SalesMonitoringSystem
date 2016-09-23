package com.buahbatu.toyotasalesman;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.buahbatu.toyotasalesman.network.NetHelper;
import com.buahbatu.toyotasalesman.network.ToyotaService;

import org.json.JSONException;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class Main2Activity extends AppCompatActivity {
    private final static String TAG = "Main2Activity";
    private ProgressDialog progressDialog;

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

        Log.i(TAG, "loginOnClick: ");
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(NetHelper.getDomainAddress(this))
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ToyotaService toyotaService = retrofit.create(ToyotaService.class);

        // caller
        final Call<ResponseBody> caller = toyotaService.login(AppConfig.getImeiNum(this),
                mUserText.getText().toString(),
                mPassText.getText().toString());
        // async task
        caller.enqueue(stringCallback);

        progressDialog.setTitle(getString(R.string.login_process));
        progressDialog.setIndeterminate(true);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                progressDialog.dismiss();
                caller.cancel();
            }
        });
        progressDialog.show();
    }

    Callback<ResponseBody> stringCallback = new Callback<ResponseBody>() {
        @Override
        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
            try {
                String responseString = response.body().string();
                Log.d(TAG, "onResponse: "+responseString);
                progressDialog.dismiss();
                String responseKey = NetHelper.getLoginResponse(responseString);
                AlertDialog alertDialog = new AlertDialog.Builder(Main2Activity.this).create();
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.error_solve), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                boolean isSucces = false;
                switch(responseKey){
                    case "success":
                        isSucces = true;
                        AppConfig.saveLoginStatus(Main2Activity.this, AppConfig.LOGIN);
                        AppConfig.storeAccount(Main2Activity.this, mUserText.getText().toString(),
                                mPassText.getText().toString());
                        moveToTrackingActivity();
                        break;
                    case "username":
                        alertDialog.setTitle(getString(R.string.error_username));
                        break;
                    case "password":
                        alertDialog.setTitle(getString(R.string.error_password));
                        break;
                    case "imei":
                        alertDialog.setTitle(getString(R.string.error_imei));
                        break;
                    case "time":
                        alertDialog.setTitle(getString(R.string.error_time));
                        break;
                    default:
                        alertDialog.setTitle(getString(R.string.error_time));break;
                }
                if (!isSucces)
                    alertDialog.show();
            }catch (IOException|JSONException e){
                Log.e(TAG, "onResponse: ", e);
            }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {
            Log.d(TAG, "onResponse: "+t.getMessage());
            progressDialog.dismiss();
            Toast.makeText(Main2Activity.this, getString(R.string.internet_sudden_error),
                    Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        if (AppConfig.getLoginStatus(this))
            moveToTrackingActivity();
        else
            ButterKnife.bind(this);

        if (!AppConfig.checkForPermission(this))
            AppConfig.askForPermission(this);

        progressDialog = new ProgressDialog(this);
    }

    private void moveToTrackingActivity(){
        startActivity(new Intent(this, TrackingActivity.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AppConfig.REQUEST_PHONE_PERMISSION){
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
