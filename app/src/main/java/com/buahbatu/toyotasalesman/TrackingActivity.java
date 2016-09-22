package com.buahbatu.toyotasalesman;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.buahbatu.toyotasalesman.network.NetHelper;
import com.buahbatu.toyotasalesman.network.ToyotaService;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

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

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        ButterKnife.bind(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(mapReadyCallback);
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
                    if (mMap.getMyLocation()!=null){
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
