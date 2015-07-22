package co.ineed.ineed;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import org.json.JSONArray;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;

/**
 * Created by John on 13/07/2015.
 */
public class MakeRequestActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private Utils utils;
    private User user;
    private ProgressDialog progressDialog;
    private MenuItem menuItem;
    private Dialog dialog;
    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private Location location;
    private JSONObject request = new JSONObject();
    private String requestId;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.make_request_activity);
        utils = new Utils();
        menuItem = (MenuItem) getIntent().getSerializableExtra("menuItem");
        getSupportActionBar().setTitle(menuItem.name);
        prefs = getSharedPreferences("user", 0);
        Gson gson = new Gson();
        String json = prefs.getString("location", "");
        location = gson.fromJson(json, Location.class);
        int textCount = 0;
        for(int i = 0; i < menuItem.content.task.formElements.size(); i++) {
            final FormElements fe = menuItem.content.task.formElements.get(i);
            if (fe.type.equals("TextArea")) {
                EditText txt = (EditText) findViewById(R.id.moreDetails);
                txt.setVisibility(View.VISIBLE);
            }else{
                if (fe.type.equals("Text")) {
                    if (textCount == 0) {
                        EditText txt = (EditText) findViewById(R.id.editText1);
                        txt.setHint(fe.name);
                        txt.setVisibility(View.VISIBLE);
                        textCount++;
                    }else{
                        EditText txt = (EditText) findViewById(R.id.editText2);
                        txt.setHint(fe.name);
                        txt.setVisibility(View.VISIBLE);
                        textCount++;
                    }
                }else if (fe.type.equals("Time")) {
                    LinearLayout dt = (LinearLayout) findViewById(R.id.dateTime);
                    dt.setVisibility(View.VISIBLE);
                    dt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog = new Dialog(MakeRequestActivity.this);
                            dialog.setContentView(R.layout.date_time_dialog);
                            dialog.setTitle(fe.name);
                            dialog.show();
                            Button ok = (Button)dialog.findViewById(R.id.buttonSelectDateTime);
                            ok.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    DatePicker dp = (DatePicker)dialog.findViewById(R.id.datePicker);
                                    TextView txt = (TextView) findViewById(R.id.textDateTime);
                                    txt.setText(dp.getDayOfMonth() + " " + MONTHS[dp.getMonth()] + " " + dp.getYear());
                                    dialog.dismiss();
                                }
                            });
                        }
                    });
                }else if (fe.type.equals("Location")) {
                    TextView txt = (TextView) findViewById(R.id.textLocation);
                    txt.setText(location.fullAddress);
                    LinearLayout dt = (LinearLayout) findViewById(R.id.location);
                    dt.setVisibility(View.VISIBLE);
                    dt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(MakeRequestActivity.this, SelectLocationActivity.class);
                            startActivityForResult(intent, 0);
                        }
                    });
                }
            }
        }
        Button btn = (Button) findViewById(R.id.buttonNext);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONArray form = new JSONArray();
                try {
                    request.put("taskId", menuItem.content.task.id);
                    request.put("form", form);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                int textCount = 0;
                for(int i = 0; i < menuItem.content.task.formElements.size(); i++) {
                    final FormElements fe = menuItem.content.task.formElements.get(i);
                    if (fe.type.equals("TextArea")) {
                        EditText txt = (EditText) findViewById(R.id.moreDetails);
                        if (txt.getText().length() == 0 && fe.isMandatory) {
                            showErrorDialog(fe.name);
                            return;
                        }
                        JSONObject item = new JSONObject();
                        try {
                            item.put("name", fe.name);
                            item.put("type", fe.type);
                            item.put("value", txt.getText());
                            form.put(item);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (fe.type.equals("Text")) {
                            EditText txt;
                            if (textCount == 0) {
                                txt = (EditText) findViewById(R.id.editText1);
                            } else {
                                txt = (EditText) findViewById(R.id.editText2);
                            }
                            if (txt.getText().length() == 0 && fe.isMandatory) {
                                showErrorDialog(fe.name);
                                return;
                            }
                            JSONObject item = new JSONObject();
                            try {
                                item.put("name", fe.name);
                                item.put("type", fe.type);
                                item.put("value", txt.getText());
                                form.put(item);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            textCount++;
                        } else if (fe.type.equals("Time")) {
                            TextView txt = (TextView) findViewById(R.id.textDateTime);
                            if (txt.getText().length() == 0 && fe.isMandatory) {
                                showErrorDialog(fe.name);
                                return;
                            }
                            JSONObject item = new JSONObject();
                            SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy");
                            SimpleDateFormat fmtOut = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
                            try {
                                Date date = fmt.parse(txt.getText().toString());
                                String d = fmtOut.format(date);
                                d = d.replace(" ", "T");
                                item.put("value", d);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                item.put("name", fe.name);
                                item.put("type", fe.type);
                                form.put(item);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else if (fe.type.equals("Location")) {
                            TextView txt = (TextView) findViewById(R.id.textLocation);
                            if (txt.getText().length() == 0 && fe.isMandatory) {
                                showErrorDialog(fe.name);
                                return;
                            }
                            JSONObject item = new JSONObject();
                            JSONObject loc = new JSONObject();
                            try {
                                loc.put("lat", location.lat);
                                loc.put("lon", location.lon);
                                loc.put("fullAddress", location.fullAddress);
                                item.put("name", fe.name);
                                item.put("type", fe.type);
                                item.put("value", loc);
                                form.put(item);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                progressDialog = new ProgressDialog(MakeRequestActivity.this);
                progressDialog.setMessage(getResources().getString(R.string.sending_request));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.show();
                URL url;
                try {
                    url = new URL(getString(R.string.api) + "requests");
                    new postRequest().execute(url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private class postRequest extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            String result = "";
            String charset = "UTF-8";
            String line = "";
            InputStream response = null;
            HttpURLConnection connection = null;
            String session = prefs.getString("session", "");
            try {
                connection = (HttpURLConnection) urls[0].openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            connection.setDoInput(true);
            connection.setDoOutput(true); // Triggers POST.
            connection.setRequestProperty("Accept-Charset", charset);
            connection.setRequestProperty("session_token", session);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            String json = request.toString();
            Integer responseCode = 0;
            try {
                OutputStream output = connection.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(output, charset);
                osw.write(json);
                osw.flush();
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
            try {
                JSONObject JSONrequest = new JSONObject(result);
                requestId = JSONrequest.getString("id");
                //handler.postDelayed(runnable, 2000);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("requestId", requestId);
                editor.commit();
                progressDialog.hide();
                Intent intent = new Intent(MakeRequestActivity.this, NotificationRequest.class);
                intent.putExtra("requestId", requestId);
                startActivity(intent);
                finish();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            progressDialog.hide();
            Intent intent = new Intent(MakeRequestActivity.this, NotificationRequest.class);
            intent.putExtra("requestId", requestId);
            startActivity(intent);
            finish();
        }
    };
    private void showErrorDialog(String name) {
        String msg = String.format(getResources().getString(R.string.error8), name);
        new AlertDialog.Builder(MakeRequestActivity.this)
                .setTitle(menuItem.name)
                .setMessage(msg)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        return;
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            location = (Location) data.getSerializableExtra("location");
            TextView txt = (TextView) findViewById(R.id.textLocation);
            txt.setText(location.fullAddress);
        }
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
        finish();

        return super.onOptionsItemSelected(item);
    }
}