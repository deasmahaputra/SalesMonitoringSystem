package com.bionus.auto2000salesman;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.bionus.auto2000salesman.services.ReportingService3;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TrackingActivity extends AppCompatActivity {
    private final String TAG = "TrackingActivity";
    private BroadcastReceiver broadcastReceiver;

    @BindView(R.id.my_coordinate) TextView myCoordinate;
    @BindView(R.id.my_location) TextView myLocation;

    private GoogleMap mMap;
    private boolean myReceiverIsRegistered;

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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item){

        switch (item.getItemId()){
            case R.id.logoutbar:

                break;
        }
        return false;
    }
}
