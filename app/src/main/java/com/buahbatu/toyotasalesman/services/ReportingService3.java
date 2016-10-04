package com.buahbatu.toyotasalesman.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.buahbatu.toyotasalesman.AppConfig;
import com.buahbatu.toyotasalesman.Main2Activity;
import com.buahbatu.toyotasalesman.R;
import com.buahbatu.toyotasalesman.network.NetHelper;
import com.buahbatu.toyotasalesman.network.PostWebTask;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ReportingService3 extends Service
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private final static int NOTIFICATION_ID = 2;
    public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 5;

    private final static String TAG = "ReportingService3";
    private final static boolean VISIBLE = true;
    private final static boolean INVISIBLE = false;

    private static GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Handler handler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (mGoogleApiClient == null)
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .build();
        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        handler = new Handler();
        mLocationRequest = createLocationRequest();
    }

    private LocationRequest createLocationRequest(){
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(getResources().getInteger(R.integer.INTERVAL) /*second*/ * 1000 /*millis*/); // in millis
        mLocationRequest.setFastestInterval(getResources().getInteger(R.integer.fast_interval) /*second*/ * 1000 /*millis*/); // in millis
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);


//        if (!mGoogleApiClient.isConnected())
//            mGoogleApiClient.connect();
        if (intent!=null){
            if (intent.getBooleanExtra(getString(R.string.service_intent_switch), AppConfig.LOGOUT)){
                Log.i(TAG, "start Tracking");
                AppConfig.saveOnTracked(this, AppConfig.LOGIN);
                setNotification(VISIBLE);
                startLocationUpdates();
            } else {
                Log.i(TAG, "stop Tracking");
                AppConfig.saveOnTracked(this, AppConfig.LOGOUT);
                setNotification(INVISIBLE);
                stopLocationUpdates();
            }
        }
        return START_STICKY;
    }

    Runnable location_updater = new Runnable() {
        @Override
        public void run() {
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
                if (AppConfig.checkForPermission(getApplicationContext())) {
                    Log.i(TAG, "onTracking");
//                    Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
//                            mGoogleApiClient);
//                    sendUpdateLocation(mLastLocation);
                    handler.postDelayed(location_updater, 10 /*seconds*/ * 1000 /*milliseconds*/);
                } else Log.i(TAG, "Permission needed");
            else{
                // wait a minute
                Log.i(TAG, "run: google not connected wait 10 second");
                handler.postDelayed(location_updater, 10 /*seconds*/ * 1000 /*milliseconds*/);
            }
        }
    };

    public void startLocationUpdates() {
        location_updater.run();
    }

    public void stopLocationUpdates() {
        handler.removeCallbacks(location_updater);
    }

    private void setNotification(boolean isVisible){
        if (isVisible) {
            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(this, Main2Activity.class);

            PendingIntent resultPendingIntent = PendingIntent
                    .getActivity(this, AppConfig.NOTIFICATION,
                            resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
            mBuilder.setSmallIcon(R.mipmap.ic_track)
                    // the label of the entry
                    .setContentTitle(this.getString(R.string.app_name))
                    // the contents of the entry
                    .setContentText(this.getString(R.string.tracked))
                    .setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
//            startForeground(NOTIFICATION_ID, mBuilder.build());
        }else {
            stopForeground(true);
        }

        Log.i(TAG, "setNotification: " + (isVisible?"VISIBLE":"INVISIBLE"));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected: ");
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
        LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                // final LocationSettingsStates locationSettingsStates = locationSettingsResult.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        // ...
                        Log.i(TAG, "google connect onResult: SUCCESS");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        Log.i(TAG, "google connect onResult: RESOLUTION_REQUIRED");
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        // ...
                        Log.i(TAG, "google connect onResult: SETTINGS_CHANGE_UNAVAILABLE");
                        break;
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended: ");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed: ");
        // * Google Play services can resolve some errors it detects.
        // * If the error has a resolution, try sending an Intent to
        // * start a Google Play services activity that can resolve
        //* error.

        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(null, CONNECTION_FAILURE_RESOLUTION_REQUEST);

                // * Thrown if Google Play services canceled the original
                // * PendingIntent

            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {

            //* If no resolution is available, display a dialog to the
            // * user with the error.

            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    private void sendUpdateLocation(Location location) {
        if (location!=null) {
            Log.i(TAG, "onLocationChanged " + location.getLongitude());

            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(this, Locale.getDefault());
            String street = "unknown";
            try {
                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null) {
                    String address = addresses.get(0).getAddressLine(0);
                    String city = addresses.get(0).getLocality();
                    String state = addresses.get(0).getAdminArea();
                    String country = addresses.get(0).getCountryName();
                    String postalCode = addresses.get(0).getPostalCode();
                    String knowName = addresses.get(0).getFeatureName();
                    street = address + " " + city + " " + state + " " + country + " " + postalCode + " " + knowName;
                    Log.i(TAG, "street " + street);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtra(getString(R.string.api_coordinate),
                    location.getLatitude() + "," + location.getLongitude());
            broadcastIntent.putExtra(getString(R.string.api_alamat), street);
            sendBroadcast(broadcastIntent);

            NetHelper.report(this, AppConfig.getUserName(this), location.getLatitude(),
                    location.getLongitude(), street, new PostWebTask.HttpConnectionEvent() {
                        @Override
                        public void preEvent() {

                        }

                        @Override
                        public void postEvent(String... result) {
                            try {
                                int nextUpdate = NetHelper.getNextUpdateSchedule(result[0]); // in second
                                Log.i(TAG, "next is in " + nextUpdate + " seconds");
                                handler.postDelayed(location_updater, nextUpdate * 1000 /*millisecond*/);
                            } catch (JSONException e) {
                                Log.i(TAG, "postEvent error update");
                                e.printStackTrace();
                                handler.postDelayed(location_updater, getResources().getInteger(R.integer.INTERVAL) * 1000 /*millisecond*/);
                            }
                        }
                    });
        }
    }
}
