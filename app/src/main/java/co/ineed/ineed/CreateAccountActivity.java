package co.ineed.ineed;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * Created by John on 11/07/2015.
 */
public class CreateAccountActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private Utils utils;
    private User user;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_account_activity);
        utils = new Utils();
        prefs = getSharedPreferences("user", 0);
        user = (User) getIntent().getSerializableExtra("user");
        getSupportActionBar().setTitle(getResources().getString(R.string.create_account));
        TextView signIn = (TextView) findViewById(R.id.signin);
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CreateAccountActivity.this, SignInActivity.class);
                startActivity(intent);
            }
        });
        if (user.email != null) {
            // Return from email already registered
            EditText txt = (EditText) findViewById(R.id.editEmail);
            txt.requestFocus();
            txt = (EditText) findViewById(R.id.editMobile);
            txt.setText(user.phone);
            txt = (EditText) findViewById(R.id.editPassword);
            txt.setText(user.password);
        }
        Button btn = (Button) findViewById(R.id.buttonNext);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText txt1 = (EditText) findViewById(R.id.editEmail);
                if (txt1.getText().length() == 0) {
                    new AlertDialog.Builder(CreateAccountActivity.this)
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
                    new AlertDialog.Builder(CreateAccountActivity.this)
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
                user.email = txt1.getText().toString();
                final EditText txt2 = (EditText) findViewById(R.id.editMobile);
                if (txt2.getText().length() == 0) {
                    new AlertDialog.Builder(CreateAccountActivity.this)
                            .setTitle(getResources().getString(R.string.create_account))
                            .setMessage(getResources().getString(R.string.error3))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    txt2.requestFocus();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return;
                }
                if (!android.util.Patterns.PHONE.matcher(txt2.getText()).matches()) {
                    new AlertDialog.Builder(CreateAccountActivity.this)
                            .setTitle(getResources().getString(R.string.create_account))
                            .setMessage(getResources().getString(R.string.error4))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    txt2.requestFocus();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return;
                }
                user.phone = txt2.getText().toString();
                final EditText txt3 = (EditText) findViewById(R.id.editPassword);
                if (txt3.getText().length() == 0) {
                    new AlertDialog.Builder(CreateAccountActivity.this)
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
                user.password = txt3.getText().toString();
                Gson gson = new Gson();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("user", gson.toJson(user));
                Intent intent = new Intent(CreateAccountActivity.this, CreateAccountNameActivity.class);
                intent.putExtra("user", user);
                startActivity(intent);
                finish();
                /*


                progressDialog = new ProgressDialog(CreateAccountActivity.this);
                progressDialog.setMessage(getResources().getString(R.string.validating_details));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.show();
                URL url;
                try {
                    url = new URL(getString(R.string.api) + "accounts");
                    new createAccount().execute(url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                */
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cancel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_cancel) {
            Boolean c = prefs.getBoolean("completed", false);
            if (prefs.getBoolean("completed", false)) {
                finish();
            }else{
                Intent intent = new Intent(CreateAccountActivity.this, FrontActivity.class);
                startActivity(intent);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private class createAccount extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            String result = "";
            String charset = "UTF-8";
            InputStream response = null;
            String line = "";
            EditText txt = (EditText) findViewById(R.id.editEmail);
            String email = txt.getText().toString();
            txt = (EditText) findViewById(R.id.editMobile);
            String mobile = txt.getText().toString();
            txt = (EditText) findViewById(R.id.editPassword);
            String password = txt.getText().toString();

            JSONObject jsonParam = new JSONObject();
            try {
                jsonParam.put("email", email);
                jsonParam.put("phone", mobile);
                jsonParam.put("password", password);
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
            connection.setRequestProperty("session_token", utils.getSessionToken(CreateAccountActivity.this));
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
                final EditText txtEmail = (EditText) findViewById(R.id.editEmail);
                new AlertDialog.Builder(CreateAccountActivity.this)
                    .setTitle(getResources().getString(R.string.create_account))
                    .setMessage(getResources().getString(R.string.error6))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            txtEmail.requestFocus();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            }else if (result.equals("401") || result.equals("403")) {
                utils.newSession(CreateAccountActivity.this);
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
                EditText txt = (EditText) findViewById(R.id.editEmail);
                String email = txt.getText().toString();
                txt = (EditText) findViewById(R.id.editMobile);
                String mobile = txt.getText().toString();
                txt = (EditText) findViewById(R.id.editPassword);
                String password = txt.getText().toString();
                Gson gson = new Gson();
                user = new User();
                user.email = email;
                user.phone = mobile;
                user.password = password;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("user", gson.toJson(user));
                Intent intent = new Intent(CreateAccountActivity.this, CreateAccountNameActivity.class);
                intent.putExtra("user", user);
                startActivity(intent);
            }
        }
    }
}