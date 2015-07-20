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
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.gson.Gson;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.exception.AuthenticationException;

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

import io.card.payment.CardIOActivity;
import io.card.payment.CreditCard;

/**
 * Created by John on 12/07/2015.
 */
public class LinkPaymentActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private Utils utils;
    private User user;
    private static final int  MY_SCAN_REQUEST_CODE = 1;
    private com.stripe.android.Stripe stripe;
    private ProgressDialog progressDialog;
    private String cardToken;
    private Boolean has_close = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.linkpayment_activity);
        utils = new Utils();
        prefs = getSharedPreferences("user", 0);
        getSupportActionBar().setTitle(getResources().getString(R.string.link_payment));
        if (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey("has_close")) {
            has_close = true;
        }
        LinearLayout scanCard = (LinearLayout) findViewById(R.id.scan_card);
        scanCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent scanIntent = new Intent(LinkPaymentActivity.this, CardIOActivity.class);

                // customize these values to suit your needs.
                scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, true); // default: false
                scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_CVV, true); // default: false
                scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, false); // default: false

                // MY_SCAN_REQUEST_CODE is arbitrary and is only used within this activity.
                startActivityForResult(scanIntent, MY_SCAN_REQUEST_CODE);
            }
        });
        Button btn = (Button) findViewById(R.id.buttonNext);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText txt1 = (EditText) findViewById(R.id.editCardNumber);
                if (txt1.getText().length() == 0) {
                    new AlertDialog.Builder(LinkPaymentActivity.this)
                            .setTitle(getResources().getString(R.string.link_payment))
                            .setMessage(getResources().getString(R.string.enter_card_number))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    txt1.requestFocus();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return;
                }
                final EditText txt2 = (EditText) findViewById(R.id.editCardExpiry);
                if (txt2.getText().length() == 0) {
                    new AlertDialog.Builder(LinkPaymentActivity.this)
                            .setTitle(getResources().getString(R.string.link_payment))
                            .setMessage(getResources().getString(R.string.enter_card_expiry))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    txt2.requestFocus();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return;
                }
                final EditText txt3 = (EditText) findViewById(R.id.editCardCVV);
                if (txt3.getText().length() == 0) {
                    new AlertDialog.Builder(LinkPaymentActivity.this)
                            .setTitle(getResources().getString(R.string.link_payment))
                            .setMessage(getResources().getString(R.string.enter_card_cvv))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    txt3.requestFocus();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return;
                }
                progressDialog = new ProgressDialog(LinkPaymentActivity.this);
                progressDialog.setMessage(getResources().getString(R.string.validating_card));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.show();
                try {
                    stripe = new com.stripe.android.Stripe(getString(R.string.stripe_key));
                    Card card = new Card("4242424242424242", 12, 2018, "123");
                    stripe.createToken(
                            card,
                            new TokenCallback() {
                                public void onSuccess(Token token) {
                                    cardToken = token.getId();
                                    URL url;
                                    try {
                                        url = new URL(getString(R.string.api) + "cards");
                                        new saveCardToken().execute(url);
                                    } catch (MalformedURLException e) {
                                        e.printStackTrace();
                                    }
                                }

                                public void onError(Exception error) {
                                    // Show localized error message
                                    progressDialog.hide();
                                    int a = 1;
                                }
                            }
                    );
                } catch (AuthenticationException e) {
                    e.printStackTrace();
                }

            }
        });

        Intent intent = new Intent(LinkPaymentActivity.this, RegisterNotifications.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (has_close) {
            getMenuInflater().inflate(R.menu.menu_cancel, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_cancel) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MY_SCAN_REQUEST_CODE) {
            String resultDisplayStr;
            if (data != null && data.hasExtra(CardIOActivity.EXTRA_SCAN_RESULT)) {
                CreditCard scanResult = data.getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT);
                final EditText txt1 = (EditText) findViewById(R.id.editCardNumber);
                txt1.setText(scanResult.getFormattedCardNumber());
                final EditText txt2 = (EditText) findViewById(R.id.editCardExpiry);
                txt2.setText(scanResult.expiryMonth + "/" + scanResult.expiryYear);
                final EditText txt3 = (EditText) findViewById(R.id.editCardCVV);
                txt3.setText(scanResult.cvv);
            }
            else {
                resultDisplayStr = "Scan was canceled.";
            }
            // do something with resultDisplayStr, maybe display it in a textView
            // resultTextView.setText(resultStr);
        }
        // else handle other activity results
    }

    private class saveCardToken extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            String result = "";
            String charset = "UTF-8";
            String query = "";
            InputStream response = null;
            String line = "";

            JSONObject jsonParam = new JSONObject();
            try {
                jsonParam.put("data", cardToken);
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
            connection.setRequestProperty("session_token", utils.getSessionToken(LinkPaymentActivity.this));
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
            if (result.equals("")) {
                Gson gson = new Gson();
                user = new User();
                String json = prefs.getString("user", "");
                user = gson.fromJson(json, User.class);
                user.isPaymentEnabled = true;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("user", gson.toJson(user));
                editor.commit();
            }
            if (progressDialog != null) {
                progressDialog.hide();
            }
            Intent intent = new Intent(LinkPaymentActivity.this, FrontActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
