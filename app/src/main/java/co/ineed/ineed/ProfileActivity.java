package co.ineed.ineed;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by John on 12/07/2015.
 */
public class ProfileActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private Utils utils;
    private User user;
    private ProgressDialog progressDialog;
    final Gson gson = new Gson();
    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utils = new Utils();
        prefs = getSharedPreferences("user", 0);
        setContentView(R.layout.profile_activity);
        getSupportActionBar().setTitle(getResources().getString(R.string.profile));

        LinearLayout requests = (LinearLayout) findViewById(R.id.toolbarRequests);
        requests.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, RequestsActivity.class);
                startActivity(intent);
            }
        });

        LinearLayout menu = (LinearLayout) findViewById(R.id.toolbarMenu);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, FrontActivity.class);
                startActivity(intent);
            }
        });
        user = new User();
        String json = prefs.getString("user", "");
        user = gson.fromJson(json, User.class);
        buildProfile();
        URL url;
        try {
            url = new URL(getString(R.string.api) + "users/me");
            new getUserTask().execute(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        LinearLayout ll = (LinearLayout) findViewById(R.id.layoutCard);
        ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, LinkPaymentActivity.class);
                intent.putExtra("has_close", 1);
                startActivity(intent);
            }
        });
        ll = (LinearLayout) findViewById(R.id.layoutTerms);
        ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, WebActivity.class);
                intent.putExtra("url", "http://www.ineed.co/terms");
                intent.putExtra("title",getResources().getString(R.string.terms_conditions));
                startActivity(intent);
            }
        });
        ll = (LinearLayout) findViewById(R.id.layoutPrivacy);
        ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, WebActivity.class);
                intent.putExtra("url", "http://www.ineed.co/privacy");
                intent.putExtra("title",getResources().getString(R.string.privacy_policy));
                startActivity(intent);
            }
        });
        ll = (LinearLayout) findViewById(R.id.layoutSignOut);
        ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("user", "");
                editor.commit();
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private class getUserTask extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            String result = "";
            String charset = "UTF-8";
            Integer responseCode = 0;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) urls[0].openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept-Charset", charset);
                connection.setRequestProperty("session_token", utils.getSessionToken(ProfileActivity.this));
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
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("user", result);
                    editor.commit();
                    user = gson.fromJson(result, User.class);
                    buildProfile();
                }
            }catch (JsonParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void buildProfile() {
        TextView tc = (TextView) findViewById(R.id.textCredit);
        TextView te = (TextView) findViewById(R.id.textExpires);
        if (user.credit != null) {
            tc.setText(String.format(getResources().getString(R.string.credit), (user.credit / 100)));
            te.setText(String.format(getResources().getString(R.string.credit_expires), utils.formatDate(user.creditExpiryDate)));
        } else {
            tc.setText("");
            te.setText("");
        }
        TextView txt = (TextView) findViewById(R.id.textEmail);
        txt.setText(user.email);
        txt = (TextView) findViewById(R.id.textCard);
        if (user.card.last4 != null) {
            txt.setText(String.format(getResources().getString(R.string.card_last), user.card.last4));
        }else{
            txt.setText(String.format(getResources().getString(R.string.card_last), "1234"));
        }
    }
}
