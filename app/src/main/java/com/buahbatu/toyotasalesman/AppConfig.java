package com.buahbatu.toyotasalesman;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * This is for configuration in app
 */
public class AppConfig {
    public final static int NOTIFICATION = 11;
    public final static int REQUEST_PHONE_PERMISSION = 16;

    public static boolean LOGIN = true;
    public static boolean LOGOUT = false;

    private static String preference = "SMS_pref";
    private static String loginKey = "login";
    private static String onTrackedKey = "bind";
    public static String versionKey = "version";

    public static SharedPreferences getDefaultPreferences(Context context){
        return context.getSharedPreferences(preference, Context.MODE_PRIVATE);
    }

    public static void saveLoginStatus(Context context, boolean isLoggedIn){
        getDefaultPreferences(context).edit().putBoolean(loginKey, isLoggedIn).apply();
    }

    public static boolean getLoginStatus(Context context){
        return getDefaultPreferences(context).getBoolean(loginKey, false);
    }

    public static void saveOnTracked(Context context, boolean onTracked){
        getDefaultPreferences(context).edit().putBoolean(onTrackedKey, onTracked).apply();
    }

    public static boolean getOnTracked(Context context){
        return getDefaultPreferences(context).getBoolean(onTrackedKey, false);
    }

    public static String getImeiNum(Context context){
        TelephonyManager mngr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        return mngr.getDeviceId();
//        return "359734050001172";
    }

    public static void storeAccount(Context context, String username, String pass){
        SharedPreferences.Editor editor = getDefaultPreferences(context).edit();
        editor.putString(context.getString(R.string.api_username), username);
        editor.putString(context.getString(R.string.api_password), pass);
        editor.apply();
    }

    public static String getUserName(Context context){
        return getDefaultPreferences(context).getString(context.getString(R.string.api_username), "User");
    }

    public static boolean checkForPermission(Context context){
        return context.checkCallingOrSelfPermission("android.permission.READ_PHONE_STATE") == PackageManager.PERMISSION_GRANTED
                && context.checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED
                && context.checkCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_GRANTED
                && context.checkCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE") == PackageManager.PERMISSION_GRANTED
                && context.checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED
                && context.checkCallingOrSelfPermission("android.permission.INTERNET") == PackageManager.PERMISSION_GRANTED;
    }

    public static void askForPermission(Activity activity){
        ActivityCompat.requestPermissions(activity, new String[]{
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE
        }, REQUEST_PHONE_PERMISSION);
    }

    public static int getLocalAppVersion(Context context){
        return context.getResources().getInteger(R.integer.version);
    }
}
