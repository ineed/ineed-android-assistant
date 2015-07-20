package co.ineed.ineed;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by John on 17/07/2015.
 */
public class NotificationRequest  extends AppCompatActivity {
    private SharedPreferences prefs;
    private Utils utils;
    private String requestId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.request_activity);
        utils = new Utils();
        prefs = getSharedPreferences("user", 0);
        requestId = prefs.getString("requestId", "");
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("requestId", "");
        editor.commit();
        URL url;
        try {
            url = new URL(getString(R.string.api) + "requests/" + requestId);
            new getRequestTask().execute(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private class getRequestTask extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            String result = "";
            String charset = "UTF-8";
            Integer responseCode = 0;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) urls[0].openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept-Charset", charset);
                connection.setRequestProperty("session_token", utils.getSessionToken(NotificationRequest.this));
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = "";
                while ((line = rd.readLine()) != null) {
                    result+=line;
                }
            } catch (IOException e) {
                try {
                    responseCode = connection.getResponseCode();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
            return result;
        }
        protected void onProgressUpdate(Integer... progress) {

        }
        protected void onPostExecute(String result) throws JsonParseException {
            try {
                if (!result.equals("")) {
                    Gson gson = new Gson();
                    Request request = gson.fromJson(result, Request.class);
                    try {
                        JSONObject JSONrequest = new JSONObject(result);
                        JSONArray JSONforms = (JSONArray) JSONrequest.getJSONArray("form");
                        for (int i2 = 0; i2 < JSONforms.length(); i2++) {
                            JSONObject JSONform  = (JSONObject) JSONforms.get(i2);
                            try {
                                String value = JSONform.getString("value");
                                if (value.contains("fullAddress")) {
                                    JSONObject location = new JSONObject(value);
                                    value = location.getString("fullAddress");
                                }
                                FormElements f = request.form.get(i2);
                                f.form_value = value;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Intent intent = new Intent(NotificationRequest.this, RequestActivity.class);
                    intent.putExtra("request", request);
                    startActivity(intent);
                    finish();
                }
            }catch (JsonParseException e) {
                e.printStackTrace();
            }
        }
    }
}
