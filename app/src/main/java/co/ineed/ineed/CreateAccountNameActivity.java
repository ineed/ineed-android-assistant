package co.ineed.ineed;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by John on 21/07/2015.
 */
public class CreateAccountNameActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private Utils utils;
    private User user;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_account_name_activity);
        utils = new Utils();
        prefs = getSharedPreferences("user", 0);
        user = (User) getIntent().getSerializableExtra("user");
        getSupportActionBar().setTitle(getResources().getString(R.string.create_account));

        Button btn = (Button) findViewById(R.id.buttonNext);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText txt1 = (EditText) findViewById(R.id.editFirstName);
                if (txt1.getText().length() == 0) {
                    new AlertDialog.Builder(CreateAccountNameActivity.this)
                            .setTitle(getResources().getString(R.string.create_account))
                            .setMessage(getResources().getString(R.string.error9))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    txt1.requestFocus();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return;
                }
                final EditText txt2 = (EditText) findViewById(R.id.editSurname);
                if (txt2.getText().length() == 0) {
                    new AlertDialog.Builder(CreateAccountNameActivity.this)
                            .setTitle(getResources().getString(R.string.create_account))
                            .setMessage(getResources().getString(R.string.error10))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    txt2.requestFocus();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return;
                }
                progressDialog = new ProgressDialog(CreateAccountNameActivity.this);
                progressDialog.setMessage(getResources().getString(R.string.creating_account));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.show();
                URL url;
                try {
                    url = new URL(getString(R.string.api) + "accounts");
                    new createAccount().execute(url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        });
        TextView ll = (TextView) findViewById(R.id.textTerms);
        ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CreateAccountNameActivity.this, WebActivity.class);
                intent.putExtra("url", "http://www.ineed.co/terms");
                intent.putExtra("title",getResources().getString(R.string.terms_conditions));
                startActivity(intent);
            }
        });
        ll = (TextView) findViewById(R.id.textPrivacy);
        ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CreateAccountNameActivity.this, WebActivity.class);
                intent.putExtra("url", "http://www.ineed.co/privacy");
                intent.putExtra("title", getResources().getString(R.string.privacy_policy));
                startActivity(intent);
            }
        });
    }

    private class createAccount extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            String result = "";
            String charset = "UTF-8";
            InputStream response = null;
            String line = "";
            EditText txt = (EditText) findViewById(R.id.editFirstName);
            String firstName = txt.getText().toString();
            txt = (EditText) findViewById(R.id.editSurname);
            String surname = txt.getText().toString();

            JSONObject jsonParam = new JSONObject();
            try {
                jsonParam.put("email", user.email);
                jsonParam.put("phone", user.phone);
                jsonParam.put("password", user.password);
                jsonParam.put("firstName", firstName);
                jsonParam.put("surname", surname);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String json = jsonParam.toString();

            HttpURLConnection connection = null;
            URL url;
            try {
                url = new URL("https://data-test.ineedapp.com/assistant/v1/accounts");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept-Charset", charset);
            connection.setRequestProperty("session_token", utils.getSessionToken(CreateAccountNameActivity.this));
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            Integer responseCode = 0;
            try {
                OutputStream output = connection.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(output, charset);
                osw.write(json);
                osw.flush ();
                osw.close();
                response = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(response));
                while ((line = rd.readLine()) != null) {
                    result += line;
                }
            } catch (IOException e) {
                try {
                    responseCode = connection.getResponseCode();
                    result = responseCode.toString();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
            return result;
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPostExecute(String result) {
            progressDialog.hide();;
            if (result.equals("409")) {
                new AlertDialog.Builder(CreateAccountNameActivity.this)
                        .setTitle(getResources().getString(R.string.create_account))
                        .setMessage(getResources().getString(R.string.error6))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(CreateAccountNameActivity.this, CreateAccountActivity.class);
                                intent.putExtra("user", user);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }else if (result.equals("401") || result.equals("403")) {
                utils.newSession(CreateAccountNameActivity.this);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        URL url;
                        try {
                            url = new URL(getString(R.string.api) + "accounts");
                            new createAccount().execute(url);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    }
                }, 1000);
            }else{
                EditText txt = (EditText) findViewById(R.id.editFirstName);
                user.firstName = txt.getText().toString();
                txt = (EditText) findViewById(R.id.editSurname);
                user.surname = txt.getText().toString();
                Gson gson = new Gson();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("user", gson.toJson(user));
                Intent intent = new Intent(CreateAccountNameActivity.this, LinkPaymentActivity.class);
                intent.putExtra("user", user);
                startActivity(intent);
            }
        }
    }
}
