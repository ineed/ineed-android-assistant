package co.ineed.ineed;

import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private Utils utils;
    private User user;
    final Gson gson = new Gson();
    private List<Address> addresses;
    private co.ineed.ineed.Location location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utils = new Utils();
        prefs = getSharedPreferences("user", 0);
        if (utils.getSessionToken(MainActivity.this).equals("")) {
            utils.newSession(this);
        }
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String locationProvider = LocationManager.NETWORK_PROVIDER;
        Location lastKnownLocation = lm.getLastKnownLocation(locationProvider);
        Double latitude = 52.0354041;
        Double longitude = 0.724502;
        if (lastKnownLocation != null) {
            latitude = lastKnownLocation.getLatitude();
            longitude = lastKnownLocation.getLongitude();
        }
        location = new co.ineed.ineed.Location();
        location.lat = latitude;
        location.lon = longitude;
        URL url;
        try {
            url = new URL("https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude.toString() + "," + longitude.toString() + "&key=AIzaSyBsDmZKYh9g8mL22KXrfKlq_uiObUeY4-Y");
            new reverseGeoLookup().execute(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }





        user = new User();
        String json = prefs.getString("user", "");
        if (json.length() == 0) {
            // New user
            setContentView(R.layout.activity_main);
            Button btn = (Button) findViewById(R.id.buttonNext);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, Onramp1Activity.class);
                    startActivity(intent);
                }
            });
        }else{
            // Existing user;
            user = gson.fromJson(json, User.class);
            if (user.isPaymentEnabled == null ||user.isPaymentEnabled == false ) {
                Intent intent = new Intent(MainActivity.this, LinkPaymentActivity.class);
                startActivity(intent);
            }else{
                Intent intent = new Intent(MainActivity.this, FrontActivity.class);
                startActivity(intent);
            }
        }
    }






    private class doSendRegIdTask extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            String result = "";
            try {
                URLConnection conn = urls[0].openConnection();
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = "";
                while ((line = rd.readLine()) != null) {
                    result+=line;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }
        protected void onProgressUpdate(Integer... progress) {

        }
        protected void onPostExecute(String result) {

        }
    }

    private class reverseGeoLookup extends AsyncTask<URL, Integer, String> {

        protected String doInBackground(URL... urls) {
            String result = "";
            try {
                URLConnection conn = urls[0].openConnection();
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = "";
                while ((line = rd.readLine()) != null) {
                    result+=line;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }
        protected void onProgressUpdate(Integer... progress) {

        }
        protected void onPostExecute(String result) {
            try {
                JSONObject json = new JSONObject(result);
                String address = ((JSONArray)json.get("results")).getJSONObject(0).getString("formatted_address");
                if (!address.equals("")) {
                    if("OK".equalsIgnoreCase(json.getString("status"))){
                        Log.d("status", json.getString("status"));
                        Address addr1 = new Address(Locale.getDefault());
                        for(int i=1;i<((JSONArray)json.get("results")).length()-2;i++){
                            JSONArray addrComp = ((JSONArray)json.get("results")).getJSONObject(i).getJSONArray("address_components");
                            for(int j=0;j<addrComp.length();j++){
                                String neighborhood = ((JSONArray)((JSONObject)addrComp.get(j)).get("types")).getString(0);
                                if (neighborhood.compareTo("neighborhood") == 0) {
                                    String neighborhood1 = ((JSONObject)addrComp.get(j)).getString("long_name");
                                    addr1.setSubThoroughfare(neighborhood1);
                                }
                                String locality = ((JSONArray)((JSONObject)addrComp.get(j)).get("types")).getString(0);
                                if (locality.compareTo("locality") == 0) {
                                    String locality1 = ((JSONObject)addrComp.get(0)).getString("long_name");
                                    location.fullAddress = locality1;
                                    SharedPreferences.Editor editor = prefs.edit();
                                    Gson gson = new Gson();
                                    editor.putString("location", gson.toJson(location));
                                    editor.commit();
                                    return;
                                    //addr1.setLocality(locality1);
                                }

                                String subadminArea = ((JSONArray)((JSONObject)addrComp.get(j)).get("types")).getString(0);
                                if (locality.compareTo("administrative_area_level_2") == 0) {
                                    String subadminArea1 = ((JSONObject)addrComp.get(j)).getString("long_name");
                                    addr1.setSubAdminArea(subadminArea1);
                                }
                                String adminArea = ((JSONArray)((JSONObject)addrComp.get(j)).get("types")).getString(0);
                                if (adminArea.compareTo("administrative_area_level_1") == 0) {
                                    String adminArea1 = ((JSONObject)addrComp.get(j)).getString("long_name");
                                    addr1.setAdminArea(adminArea1);
                                }

                                String postalcode = ((JSONArray)((JSONObject)addrComp.get(j)).get("types")).getString(0);
                                if (postalcode.compareTo("postal_code") == 0) {
                                    String postalcode1 = ((JSONObject)addrComp.get(j)).getString("long_name");
                                    addr1.setPostalCode(postalcode1);
                                }
                                String sublocality = ((JSONArray)((JSONObject)addrComp.get(j)).get("types")).getString(0);
                                if (sublocality.compareTo("sublocality") == 0) {
                                    String sublocality1 = ((JSONObject)addrComp.get(j)).getString("long_name");
                                    addr1.setSubLocality(sublocality1);
                                }
                                String countr = ((JSONArray)((JSONObject)addrComp.get(j)).get("types")).getString(0);
                                if (countr.compareTo("country") == 0) {
                                    String countr1 = ((JSONObject)addrComp.get(j)).getString("long_name");

                                    addr1.setCountryName(countr1);
                                }

                            }
                        }

                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


}