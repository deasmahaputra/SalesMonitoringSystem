package com.buahbatu.toyotasalesman;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.buahbatu.toyotasalesman.network.NetHelper;
import com.buahbatu.toyotasalesman.network.ToyotaService;
import com.buahbatu.toyotasalesman.services.ReportingService3;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class TrackingActivity extends AppCompatActivity {
    private final String TAG = "TrackingActivity";
    private BroadcastReceiver broadcastReceiver;

    @BindView(R.id.my_coordinate) TextView myCoordinate;
    @BindView(R.id.my_location) TextView myLocation;

    private GoogleMap mMap;
    private boolean myReceiverIsRegistered;

    @Override
    protected void onResume() {
        super.onResume();
        if (!myReceiverIsRegistered) {
            registerReceiver(broadcastReceiver, new IntentFilter(ReportingService3.ACTION_LOCATION_BROADCAST));
            myReceiverIsRegistered = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
        myReceiverIsRegistered = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        ButterKnife.bind(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(mapReadyCallback);

        Intent service = new Intent(this, ReportingService3.class);
        service.putExtra(getString(R.string.service_intent_switch), AppConfig.LOGIN);
        startService(service);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
//                String action = intent.getAction();
//                if(action.equalsIgnoreCase(ReportingService3.ACTION_LOCATION_BROADCAST)){
                    // send message to activity
                    Log.i(TAG, "onReceive: BROADCAST");
                    String address = intent.getStringExtra(ReportingService3.EXTRA_ADDRESS);
                    String coordinate = intent.getStringExtra(ReportingService3.EXTRA_COORDINATE);
                    myCoordinate.setText(coordinate);
                    myLocation.setText(address);
//                }
            }
        };
    }

    OnMapReadyCallback mapReadyCallback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            Log.i(TAG, "onMapReady: "+AppConfig.checkForPermission(TrackingActivity.this));
            if (AppConfig.checkForPermission(TrackingActivity.this))
                googleMap.setMyLocationEnabled(true);

            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    if (mMap.getMyLocation() != null){
                        LatLng latLng = new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
                    }else
                        Log.i(TAG, "onMyLocationButtonClick: null");
                    return true;
                }
            });
        }
    };

    private void doLogout(){
        Log.i(TAG, "loginOnClick: ");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(NetHelper.getDomainAddress(this))
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ToyotaService toyotaService = retrofit.create(ToyotaService.class);

        // caller
        Call<ResponseBody> caller = toyotaService.logout("0,0",
                AppConfig.getUserName(this),
                "kosong");
        // async task
        caller.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    Log.i(TAG, "onResponse: "+response.body().string());
                }catch (IOException e){}

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "onFailure: ", t);
            }
        });

        AppConfig.saveLoginStatus(this, AppConfig.LOGOUT);
        AppConfig.storeAccount(this, "", "");

        Intent service = new Intent(this, ReportingService3.class);
        service.putExtra(getString(R.string.service_intent_switch), AppConfig.LOGOUT);
        startService(service);

        Intent intent = new Intent(this, Main2Activity.class);
        startActivity(intent);
        finish();
    }

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
        }
        return false;
    }
}
