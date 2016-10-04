package com.buahbatu.toyotasalesman;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.buahbatu.toyotasalesman.network.NetHelper;
import com.buahbatu.toyotasalesman.network.PostWebTask;
import com.buahbatu.toyotasalesman.services.ReportingService;

import org.json.JSONException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "Main2Activity";
    private ReportingService reportingService;
    private Context context;
    public final static int requestPhonePermission = 12;

    @BindView(R.id.username_text)EditText userText;
    @BindView(R.id.pass_text)EditText passText;
//    @BindView(R.id.textInfo)TextView info;

    @BindView(R.id.my_location) TextView mLocationText;
    @BindView(R.id.my_coordinate) TextView mCoorText;

    @BindView(R.id.info_box) View mInfoBox;
    @BindView(R.id.login_box) View mLoginBox;

    @BindView(R.id.login_button) Button loginButton;
    @BindView(R.id.logout_button) Button logoutButton;

    @OnClick(R.id.logout_button)void onLogoutClick(Button button){
        doLogout();
    }
    @OnClick(R.id.login_button)void onLoginClick(Button button){
        if (checkForPermission()) {
            switch (button.getText().toString()) {
                case "Login":
                    doLogin();
                    break;
                case "Logout":
                    doLogout();
                    break;
            }
        } else askForPermission();

    }
    @OnClick(R.id.logo_view)void onImageClick(){
        new AlertDialog.Builder(context).setTitle("Phone IMEI")
                .setMessage("Your imei " + AppConfig.getImeiNum(context))
                .setPositiveButton("Ok got it!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    LocalBroadcastManager broadcastManager;
    String street;
    double lat, longi;
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive: BROADCAST");
            street = intent.getStringExtra(getString(R.string.api_alamat));
            lat = intent.getDoubleExtra("lat",0);
            longi = intent.getDoubleExtra("long",0);
            mCoorText.setText(lat+","+longi);
            mLocationText.setText(street);
        }
    };
    IntentFilter intentFilter = new IntentFilter("com.buahbatu.toyotasalesman.MainActivity");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        ButterKnife.bind(this);

        context = MainActivity.this;

        // enable auto login
        setUI(AppConfig.getLoginStatus(context));
        startTrack(AppConfig.getLoginStatus(context));

        //isnetworkAvailable();
//        updateWithNewLocation();

        loginButton.setActivated(false);
        logoutButton.setActivated(true);

        broadcastManager = LocalBroadcastManager.getInstance(this);
    }

//    private boolean isnetworkAvailable(){
//        String permission = "ACCESS_FINE_LOCATION";
//        int res = context.checkCallingOrSelfPermission(permission);
//        if(res == PackageManager.PERMISSION_GRANTED){
//            LocationManager locationManager;
//            locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
//            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//            updateWithNewLocation(location);
//        }
//        return false;
   // }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item){

        switch (item.getItemId()){
            case R.id.logoutbar:
                doLogout();
                break;
            default:

                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("failed logout")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }

                        }).show();


        }
        return false;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            Log.i(TAG, "onServiceConnected ");
            ReportingService.LocalBinder b = (ReportingService.LocalBinder) binder;
            reportingService = b.getService();
            reportingService.setUpGoogleClient(MainActivity.this);
        }

        public void onServiceDisconnected(ComponentName className) {
            reportingService = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent= new Intent(this, ReportingService.class);
        bindService(intent, mConnection,
                Context.BIND_AUTO_CREATE);
        broadcastManager.registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
        broadcastManager.unregisterReceiver(broadcastReceiver);
    }

    boolean checkForPermission(){
        return checkCallingOrSelfPermission("android.permission.READ_PHONE_STATE") == PackageManager.PERMISSION_GRANTED
                && checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED
                && checkCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE") == PackageManager.PERMISSION_GRANTED
                && checkCallingOrSelfPermission("android.permission.INTERNET") == PackageManager.PERMISSION_GRANTED;
    }

    void askForPermission(){
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE
        }, requestPhonePermission);
    }

    void doLogout(){
        AppConfig.saveLoginStatus(this, false);
        AppConfig.storeAccount(this, "", "");

        // update UI
        setUI(false);

        // stop tracking
        startTrack(false);

        NetHelper.logout(this, AppConfig.getUserName(this), lat,
                longi, street, new PostWebTask.HttpConnectionEvent() {
                    @Override
                    public void preEvent() {

                    }

                    @Override
                    public void postEvent(String... result) {
                        Log.i(TAG, "postEvent logout " + result[0]);
                    }
                });
    }

    void doLogin(){
        if (!userText.getText().toString().isEmpty()){
            if (!userText.getText().toString().isEmpty()){
                if (!reportingService.mGoogleApiClient.isConnected())
                    reportingService.mGoogleApiClient.connect();
                else
                    NetHelper.login(this, userText.getText().toString(), passText.getText().toString(), loginEvent);
            }else Toast.makeText(this, "Please fill your password", Toast.LENGTH_SHORT).show();
        }else Toast.makeText(this, "Please fill your username", Toast.LENGTH_SHORT).show();
    }

    ProgressDialog progressDialog;
    PostWebTask.HttpConnectionEvent loginEvent = new PostWebTask.HttpConnectionEvent() {
        @Override
        public void preEvent() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Logging In");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        public void postEvent(String... result) { // check login state
            progressDialog.dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            // handle if login fail and success
            try {
                String response = NetHelper.getLoginResponse(result[0]);
//                String response = "imei";
                switch (response){
                    case "success":
                        AppConfig.saveLoginStatus(context, true);
                        AppConfig.storeAccount(context, userText.getText().toString(),
                                passText.getText().toString());

                        // update UI
                        setUI(true);

                        // start tracking
                        Intent mServiceIntent = new Intent(context, ReportingService.class);
                        mServiceIntent.putExtra("run", true);
                        startService(mServiceIntent);
                        break;
                    case "username":
                        builder.setMessage("Username is not registered")
                                .setPositiveButton("Ok got it!", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                        break;
                    case "password":
                        builder.setMessage("Your password is not match")
                                .setPositiveButton("Ok got it!", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                        break;
                    case "imei":
                        builder.setMessage("Your phone is not registered")
                                .setPositiveButton("Ok got it!", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                        break;
                    case "time":
                        builder.setMessage("It is out of working hours")
                                .setPositiveButton("Ok got it!", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                        break;
                    default:
                        builder.setMessage("Something wrong occurred, please contact your administrator")
                                .setPositiveButton("Ok got it!", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                        break;

                }
            }catch (JSONException e){
                new AlertDialog.Builder(context)
                        .setMessage("Something wrong occurred, please contact your administrator")
                        .setPositiveButton("Ok got it!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
            }
        }
    };

    // update UI
    void setUI(boolean isLoogedIn){
        if (isLoogedIn){
            mLoginBox.setVisibility(View.GONE);
            mInfoBox.setVisibility(View.VISIBLE);
        }else {
            mLoginBox.setVisibility(View.VISIBLE);
            mInfoBox.setVisibility(View.GONE);
        }
    }

    void startTrack(boolean shouldStart){
        // start tracking
        Intent mServiceIntent = new Intent(context, ReportingService.class);
        mServiceIntent.putExtra(shouldStart ? "run":"stop", true);
        startService(mServiceIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case requestPhonePermission:
                boolean granted = true;
                for (int result:grantResults) {
                    if (result==PackageManager.PERMISSION_DENIED) {
                        granted = false;
                        break;
                    }
                }
                if (granted) doLogin(); break;
            default: Toast.makeText(this, "Please give permissions", Toast.LENGTH_SHORT).show(); break;
        }
    }
}
