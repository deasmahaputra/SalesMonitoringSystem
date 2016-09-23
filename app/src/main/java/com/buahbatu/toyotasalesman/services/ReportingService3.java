package com.buahbatu.toyotasalesman.services;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.buahbatu.toyotasalesman.AppConfig;
import com.buahbatu.toyotasalesman.Main2Activity;
import com.buahbatu.toyotasalesman.R;
import com.buahbatu.toyotasalesman.network.NetHelper;
import com.buahbatu.toyotasalesman.network.ToyotaService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ReportingService3 extends Service {
    private final String TAG = "ReportingService3";
    private final static int NOTIFICATION_ID = 0;

    public static final String
            ACTION_LOCATION_BROADCAST = ReportingService3.class.getName(),
            EXTRA_COORDINATE = "extra_coordinate",
            EXTRA_ADDRESS = "extra_address";

    private Handler handler;
    private LocationRequest mLocationRequest;
    private Retrofit retrofit;
    private Geocoder geocoder;
    private GoogleApiClient mGoogleApiClient;

    Runnable location_updater = new Runnable() {
        @Override
        public void run() {
            if (AppConfig.checkForPermission(ReportingService3.this)) {
                Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                        mGoogleApiClient);
                Log.i(TAG, "startTracking "+mLastLocation.getLatitude());

                ToyotaService toyotaService = retrofit.create(ToyotaService.class);
                String street = "Unknown";
                try {
                    List<Address> addresses = geocoder.getFromLocation(mLastLocation.getLatitude(),
                            mLastLocation.getLongitude(), 1);
                    if (addresses != null) {
                        String address = addresses.get(0).getAddressLine(0);
                        String city = addresses.get(0).getLocality();
                        String state = addresses.get(0).getAdminArea();
                        String country = addresses.get(0).getCountryName();
                        String postalCode = addresses.get(0).getPostalCode();
                        String knowName = addresses.get(0).getFeatureName();
                        street = address + " " + city + " " + state + " " + country + " " + postalCode + " " + knowName;
                        Log.i(TAG, "street "+street);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String coordinate = mLastLocation.getLatitude()+","+mLastLocation.getLongitude();
                Call<ResponseBody> call = toyotaService.report(coordinate,
                        AppConfig.getUserName(ReportingService3.this), street);
                call.enqueue(bodyCallback);
                sendBroadcastMessage(coordinate, street);

            } else Log.i(TAG, "Permission needed");
        }
    };

    private void sendBroadcastMessage(String coordinate, String address) {
        Log.i(TAG, "send BROADCAST");
        Intent intent = new Intent(ACTION_LOCATION_BROADCAST);
        intent.putExtra(EXTRA_ADDRESS, address);
        intent.putExtra(EXTRA_COORDINATE, coordinate);
        sendBroadcast(intent);
    }

    Callback<ResponseBody> bodyCallback = new Callback<ResponseBody>() {
        @Override
        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
            try {
                Log.i(TAG, "onResponse is success: "+response.isSuccessful());
                int nextUpdate = 60;
                if (response.isSuccessful()) {
                    Log.i(TAG, "onResponse code: " + response.errorBody().string());
                    nextUpdate = NetHelper.getNextUpdateSchedule(response.body().string()); // in second
                    Log.i(TAG, "next is in " + nextUpdate + " seconds");
                    if (nextUpdate > 60) {
                        dismissNotification();
                    } else {
                        showNotification();
                    }
                }
                handler.postDelayed(location_updater, nextUpdate * 1000 /*millisecond*/);
            }catch (IOException|JSONException e){
                Log.i(TAG, "postEvent error update");
                e.printStackTrace();
                handler.postDelayed(location_updater, getResources().getInteger(R.integer.INTERVAL) * 1000 /*millisecond*/);
            }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {

        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: ");
        handler = new Handler();
        geocoder = new Geocoder(this, Locale.getDefault());
        mLocationRequest = createLocationRequest();
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            Log.i(TAG, "onCreate: create client");
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(connectionCallbacks)
                    .addOnConnectionFailedListener(failedListener)
                    .addApi(LocationServices.API)
                    .build();
        }
        retrofit = new Retrofit.Builder()
                .baseUrl(NetHelper.getDomainAddress(this))
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
    }

    GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.i(TAG, "onConnected: ");
            location_updater.run();
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.i(TAG, "onConnectionSuspended: ");
        }
    };

    GoogleApiClient.OnConnectionFailedListener failedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.i(TAG, "onConnectionFailed: ");
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean isStart = intent.getBooleanExtra(getString(R.string.service_intent_switch), false);
        if (isStart){
            Log.i(TAG, "onStartCommand: true");
            showNotification();
            startLocationUpdates();
        }else {
            Log.i(TAG, "onStartCommand: false");
            dismissNotification();
            stopLocationUpdates();
        }
        return START_STICKY;
    }



    public void startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates: check");
        if (mGoogleApiClient!=null) {
            mGoogleApiClient.connect();
            Log.i(TAG, "startLocationUpdates: after");

        }
    }

    public void stopLocationUpdates() {
        Log.i(TAG, "stopLocationUpdates: check");
        if (mGoogleApiClient!=null) {
            mGoogleApiClient.disconnect();
            Log.i(TAG, "stopLocationUpdates: after");
            handler.removeCallbacks(location_updater);
            location_updater.run();
        }
    }


    private void showNotification(){
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, Main2Activity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)   // the status icon
                .setTicker(this.getString(R.string.ticker))        // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(this.getString(R.string.app_name))  // the label of the entry
                .setContentText(this.getString(R.string.tracked))            // the contents of the entry
                .setContentIntent(contentIntent)      // The intent to send when the entry is clicked
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void dismissNotification(){
        // update UI
        try {
//            stopForeground(true);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private LocationRequest createLocationRequest(){
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(getResources().getInteger(R.integer.INTERVAL) /*second*/ * 1000 /*millis*/); // in millis
        mLocationRequest.setFastestInterval(getResources().getInteger(R.integer.fast_interval) /*second*/ * 1000 /*millis*/); // in millis
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }
}
