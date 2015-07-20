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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by John on 12/07/2015.
 */
public class SignInActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private Utils utils;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signin_activity);
        utils = new Utils();
        getSupportActionBar().setTitle(getResources().getString(R.string.existing_account));
        Button btn = (Button) findViewById(R.id.buttonNext);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText txt1 = (EditText) findViewById(R.id.editEmail);
                if (txt1.getText().length() == 0) {
                    new AlertDialog.Builder(SignInActivity.this)
                            .setTitle(getResources().getString(R.string.create_account))
                            .setMessage(getResources().getString(R.string.error1))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    txt1.requestFocus();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return;
                }
                if (!Utils.isValidEmail(txt1.getText())) {
                    new AlertDialog.Builder(SignInActivity.this)
                            .setTitle(getResources().getString(R.string.create_account))
                            .setMessage(getResources().getString(R.string.error2))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    txt1.requestFocus();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return;
                }
                final EditText txt3 = (EditText) findViewById(R.id.editPassword);
                if (txt3.getText().length() == 0) {
                    new AlertDialog.Builder(SignInActivity.this)
                            .setTitle(getResources().getString(R.string.create_account))
                            .setMessage(getResources().getString(R.string.error5))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    txt3.requestFocus();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return;
                }
                progressDialog = new ProgressDialog(SignInActivity.this);
                progressDialog.setMessage(getResources().getString(R.string.signing_in));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.show();
                URL url;
                try {
                    url = new URL(getString(R.string.api) + "login/pw");
                    new signIn().execute(url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    private class signIn extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            String result = "";
            String charset = "UTF-8";
            String query = "";
            InputStream response = null;
            String line = "";
            EditText txt = (EditText) findViewById(R.id.editEmail);
            String email = txt.getText().toString();
            txt = (EditText) findViewById(R.id.editPassword);
            String password = txt.getText().toString();
            JSONObject jsonParam = new JSONObject();
            try {
                jsonParam.put("email", email);
                jsonParam.put("password", password);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String json = jsonParam.toString();

            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) urls[0].openConnection();
                connection.setRequestMethod("POST");
            } catch (IOException e) {
                e.printStackTrace();
            }
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept-Charset", charset);
            connection.setRequestProperty("session_token", utils.getSessionToken(SignInActivity.this));
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
            progressDialog.hide();
            if (result.equals("401") || result.equals("403")) {
                final EditText txtEmail = (EditText) findViewById(R.id.editEmail);
                new AlertDialog.Builder(SignInActivity.this)
                    .setTitle(getResources().getString(R.string.create_account))
                    .setMessage(getResources().getString(R.string.error7))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            txtEmail.requestFocus();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            }else{
                try {
                    JSONObject json = new JSONObject(result);
                    SharedPreferences prefs = getSharedPreferences("user", 0);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("session", json.getString("sessionToken"));
                    editor.commit();
                    utils.getUser(SignInActivity.this);
                    Intent intent = new Intent(SignInActivity.this, LinkPaymentActivity.class);
                    startActivity(intent);

                    //finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}