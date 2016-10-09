package com.bionus.auto2000salesman;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bionus.auto2000salesman.network.NetHelper;
import com.bionus.auto2000salesman.network.PostWebTask;
import com.bionus.auto2000salesman.services.ReportingService3;

import org.json.JSONException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;

public class Main2Activity extends AppCompatActivity {
    private final static String TAG = "Main2Activity";
    private ProgressDialog progressDialog;
    private Realm realm;
    private BroadcastReceiver broadcastReceiver;

    @BindView(R.id.info_box) View mInfoBox;
    @BindView(R.id.login_box) View mLoginBox;


    @BindView(R.id.my_location) TextView mLocationText;
    @BindView(R.id.my_coordinate) TextView mCoorText;

    @BindView(R.id.username_text) TextView mUserText;
    @BindView(R.id.pass_text) TextView mPassText;

    @BindView(R.id.login_button) Button loginButton;
    @BindView(R.id.logout_button) Button logoutButton;

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

    @OnClick(R.id.logout_button) void logoutOnClick(){
        doLogout();
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

        doLogin();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        ButterKnife.bind(this);

        if (AppConfig.getLoginStatus(this))
            autoStartService();

        setUI(AppConfig.getLoginStatus(this));

        if (!AppConfig.checkForPermission(this))
            AppConfig.askForPermission(this);

        // Open the Realm
        realm = Realm.getDefaultInstance();

        loginButton.setActivated(false);
        logoutButton.setActivated(true);

        progressDialog = new ProgressDialog(this);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
//                String action = intent.getAction();
//                if(action.equalsIgnoreCase(ReportingService3.ACTION_LOCATION_BROADCAST)){
                // send message to activity
                Log.i(TAG, "onReceive: BROADCAST");
                String address = intent.getStringExtra(getString(R.string.api_alamat));
                String coordinate = intent.getStringExtra(getString(R.string.api_coordinate));
                mCoorText.setText(coordinate);
                mLocationText.setText(address);
//                }
            }
        };
    }

    private void doLogin(){
        if (!AppConfig.getLoginStatus(this)) {

            NetHelper.login(this, mUserText.getText().toString(), mPassText.getText().toString(),
                    new PostWebTask.HttpConnectionEvent() {
                @Override
                public void preEvent() {
                    progressDialog.setMessage(getString(R.string.login_process));
                    progressDialog.setIndeterminate(true);
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }

                // set UI and start tracking if success
                @Override
                public void postEvent(String... result) {
                    try {
                        progressDialog.dismiss();
                        String responseKey = NetHelper.getLoginResponse(result[0]);
                        Log.d(TAG, "onResponse: "+responseKey);
                        AlertDialog alertDialog = new AlertDialog.Builder(Main2Activity.this).create();
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.error_solve), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        boolean isSuccess = false;
                        switch(responseKey){
                            case "success":
                                isSuccess = true;
                                AppConfig.saveLoginStatus(Main2Activity.this, AppConfig.LOGIN);
                                AppConfig.storeAccount(Main2Activity.this, mUserText.getText().toString(),
                                        mPassText.getText().toString());

                                // update UI
                                setUI(AppConfig.LOGIN);
                                // start tracking
                                setTrackingService(AppConfig.LOGIN);

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
                        if (!isSuccess)
                            alertDialog.show();
                    }catch (JSONException e){
                        Log.e(TAG, "onResponse: ", e);
                    }
                }
            });
        }
    }

    private void doLogout(){
        if (AppConfig.getLoginStatus(this)) {

            NetHelper.logout(this, AppConfig.getUserName(this), -12.3, 123.33, "jalan kampus",
                    new PostWebTask.HttpConnectionEvent() {
                @Override
                public void preEvent() {
                    progressDialog.setMessage(getString(R.string.logout_process));
                    progressDialog.setIndeterminate(true);
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }

                @Override
                public void postEvent(String... result) {
                    progressDialog.dismiss();

                    String responseKey = result[0];
                    Log.d(TAG, "onResponse: "+responseKey);

                    // login context update
                    AppConfig.saveLoginStatus(getApplicationContext(), false);
                    AppConfig.storeAccount(getApplicationContext(), "", "");

                    // update UI
                    setUI(AppConfig.LOGOUT);

                    // stop tracking
                    setTrackingService(AppConfig.LOGOUT);
                }
            });
        }
    }

    private void setUI(boolean isLogin){
        if (isLogin){
            mLoginBox.setVisibility(View.GONE);
            mInfoBox.setVisibility(View.VISIBLE);
        }else {
            mLoginBox.setVisibility(View.VISIBLE);
            mInfoBox.setVisibility(View.GONE);
        }
    }

    private void autoStartService(){
        setTrackingService(AppConfig.LOGIN);
    }

    private void setTrackingService(boolean isLogin){
        Intent intent = new Intent(this, ReportingService3.class);
        intent.putExtra(getString(R.string.service_intent_switch), isLogin);
        startService(intent);
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
        }else if (requestCode == ReportingService3.CONNECTION_FAILURE_RESOLUTION_REQUEST){
            Log.i(TAG, "onRequestPermissionsResult: need resolve");
        }
    }
}
