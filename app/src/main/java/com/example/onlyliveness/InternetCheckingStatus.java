package com.example.onlyliveness;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class InternetCheckingStatus {

    private static String TAG = "InternetCheckingStatus Activity";
    private Context mContext;
    // 0 = no internet connection, 1 = WiFi, 2 = Mobile connection
    private int internetStatus;


    public InternetCheckingStatus(Context mContext) {
        this.mContext = mContext;
        internetStatus = checkInternetConnection();
    }

    private int checkInternetConnection() {
        ConnectivityManager connectivitymanager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo[] networkInfo = connectivitymanager.getAllNetworkInfo();

        for (NetworkInfo netInfo : networkInfo) {
            Log.d(TAG, "For loop");
            if (netInfo.getTypeName().equalsIgnoreCase("WIFI")) {

                if (netInfo.isConnected()) {
                    /*Toast.makeText(getApplicationContext(), "Connected to WiFi",
                            Toast.LENGTH_SHORT).show();*/
                    //internetStatusImage.setImageResource(R.drawable.wifi_icon18);
                    //internetStatusImage.setVisibility(View.VISIBLE);
                    return 1;
                }
            }


            if (netInfo.getTypeName().equalsIgnoreCase("MOBILE")) {
                if (netInfo.isConnected()) {
                    /*Toast.makeText(getApplicationContext(), "Connected to mobile data",
                            Toast.LENGTH_SHORT).show();*/
                    //internetStatusImage.setImageResource(R.drawable.cellular_icon18);
                    //internetStatusImage.setVisibility(View.VISIBLE);
                    return 2;
                }
            }
        }
        return 0;
    }

    public int getInternetStatus() {
        return internetStatus;
    }
}
