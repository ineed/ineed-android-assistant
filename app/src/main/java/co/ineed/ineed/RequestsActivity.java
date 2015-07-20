package co.ineed.ineed;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.loopj.android.image.SmartImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by John on 10/07/2015.
 */
public class RequestsActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private Utils utils;
    private User user;
    private ProgressDialog progressDialog;
    private Boolean hasRequest = false;
    Boolean editing = false;
    private Requests requests;
    private ListAdapter customAdapter;
    private Integer mPosition;
    private String mTitle;
    private String mID;
    private Handler mHandler;
    private int mInterval = 5000;
    Menu myMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.requests_activity);
        utils = new Utils();
        prefs = getSharedPreferences("user", 0);
        getSupportActionBar().setTitle(getResources().getString(R.string.requests));

        LinearLayout menu = (LinearLayout) findViewById(R.id.toolbarMenu);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RequestsActivity.this, FrontActivity.class);
                startActivity(intent);
            }
        });
        LinearLayout profile = (LinearLayout) findViewById(R.id.toolbarProfile);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RequestsActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });
        Button btn = (Button) findViewById(R.id.buttonNext);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RequestsActivity.this, FrontActivity.class);
                startActivity(intent);
            }
        });

        String json = prefs.getString("requests", "");
        if (json.length() > 15) {
            ListView lv = (ListView) findViewById(R.id.listContent);
            lv.setVisibility(View.VISIBLE);
            Gson gson = new Gson();
            requests = gson.fromJson(json, Requests.class);
            buildRequests(json);
        }else {
            progressDialog = new ProgressDialog(RequestsActivity.this);
            progressDialog.setMessage(getResources().getString(R.string.fetching_requests));
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.show();
        }
        URL url;
        try {
            url = new URL(getString(R.string.api) + "requests");
            new getRequestsTask().execute(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    Runnable mRequestsChecker = new Runnable() {
        @Override
        public void run() {
            URL url;
            try {
                url = new URL(getString(R.string.api) + "requests");
                new getRequestsTask().execute(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    };

    void startRequestTask() {
        mHandler.postDelayed(mRequestsChecker, 5000);
    }

    void stopRequestTask() {
        mHandler.removeCallbacks(mRequestsChecker);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        myMenu = menu;
        getMenuInflater().inflate(R.menu.menu_requests, menu);
        int positionOfMenuItem = 0; // or whatever...
        MenuItem item = menu.getItem(positionOfMenuItem);
        if (requests == null || requests.data.size() == 0) {
            item.setVisible(false);
        }
        SpannableString s = new SpannableString(getResources().getString(R.string.edit));
        s.setSpan(new ForegroundColorSpan(Color.BLUE), 0, s.length(), 0);
        item.setTitle(s);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (!editing) {
            item.setTitle(getResources().getString(R.string.done));
            editing = true;
        }else{
            item.setTitle(getResources().getString(R.string.edit));
            editing = false;
        }
        customAdapter.notifyDataSetChanged();
        return super.onOptionsItemSelected(item);
    }

    private class getRequestsTask extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            String result = "";
            String charset = "UTF-8";
            Integer responseCode = 0;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) urls[0].openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept-Charset", charset);
                connection.setRequestProperty("session_token", utils.getSessionToken(RequestsActivity.this));
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
            if (progressDialog != null) {
                progressDialog.hide();
            }
            try {
                if (!result.equals("")) {
                    String original = prefs.getString("requests", "");
                    if (result.length() < 15 || !original.equals(result)) {
                        MenuItem item = myMenu.getItem(0);
                        Gson gson = new Gson();
                        requests = gson.fromJson(result, Requests.class);
                        if (requests.data.size() == 0) {
                            item.setVisible(false);
                            ScrollView sv = (ScrollView) findViewById(R.id.scrollNoContent);
                            sv.setVisibility(View.VISIBLE);
                        }else {
                            item.setVisible(true);
                            ListView lv = (ListView) findViewById(R.id.listContent);
                            lv.setVisibility(View.VISIBLE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("requests", result);
                            editor.putBoolean("hasRequest", true);
                            editor.commit();
                            buildRequests(result);
                        }
                    }
                }
                //startRequestTask();
            }catch (JsonParseException e) {
                e.printStackTrace();
            }
        }
    }

    private class deleteRequestTask extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            String result = "";
            String charset = "UTF-8";
            Integer responseCode = 0;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) urls[0].openConnection();
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("Accept-Charset", charset);
                connection.setRequestProperty("session_token", utils.getSessionToken(RequestsActivity.this));
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
            if (progressDialog != null) {
                progressDialog.hide();
            }
            try {
                int a=1;
            }catch (JsonParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void buildRequests(String result) {
        try {
            String value;
            JSONObject data = new JSONObject(result);
            JSONArray JSONrequests = (JSONArray) data.getJSONArray("data");
            for (int i = 0; i < JSONrequests.length(); i++) {
                JSONObject JSONrequest  = (JSONObject) JSONrequests.get(i);
                JSONArray JSONforms = (JSONArray) JSONrequest.getJSONArray("form");
                for (int i2 = 0; i2 < JSONforms.length(); i2++) {
                    JSONObject JSONform  = (JSONObject) JSONforms.get(i2);
                    try {
                        value = JSONform.getString("value");
                        if (value.contains("fullAddress")) {
                            JSONObject location = new JSONObject(value);
                            value = location.getString("fullAddress");
                        }
                        Request r = requests.data.get(i);
                        FormElements f = r.form.get(i2);
                        f.form_value = value;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ListView lc = (ListView) findViewById(R.id.listContent);
        customAdapter = new ListAdapter(this, R.layout.request_list, (ArrayList<Request>) requests.data);
        lc.setAdapter(customAdapter);
        lc.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                if (editing) {
                    editing = false;
                    invalidateOptionsMenu();
                    customAdapter.notifyDataSetChanged();
                }
                Request request = requests.data.get(position);
                Intent intent = new Intent(RequestsActivity.this, RequestActivity.class);
                intent.putExtra("request", request);
                startActivity(intent);
            }
        });
    }
    public class ListAdapter extends ArrayAdapter<Request> {

        public ListAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        public ListAdapter(Context context, int resource, List<Request> items) {
            super(context, resource, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v = convertView;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.request_list, null);
            }

            Request r = getItem(position);

            if (r != null) {
                SmartImageView img = (SmartImageView) v.findViewById(R.id.imageUser);
                Message m = r.messages.get(r.messages.size()-1);
                if (m.profileImage != null) {
                    img.setImageUrl(getString(R.string.image_url) + m.profileImage + "?width=44&height=44&mode=crop");
                }else{
                    img.setImageResource(R.drawable.no_profile_picture);
                }
                TextView txt = (TextView) v.findViewById(R.id.textTitle);
                txt.setText(r.name);
                RelativeTimeTextView rt = (RelativeTimeTextView) v.findViewById(R.id.textDate);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                try {
                    Date date = format.parse(r.createdTime);
                    rt.setReferenceTime(date.getTime());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Message msg = r.messages.get(r.messages.size()-1);
                txt = (TextView) v.findViewById(R.id.textMessage);
                txt.setText(msg.text);
                txt = (TextView) v.findViewById(R.id.textStatus);
                if (r.status.equals("Active") || r.status.equals("Open") || r.status.equals("New")) {
                    txt.setBackgroundResource(R.drawable.green_request_button);
                    txt.setTextColor(0xFF74C391);
                }else{
                    txt.setBackgroundResource(R.drawable.red_request_button);
                    txt.setTextColor(0xFFFF4444);
                }
                txt.setText(r.displayStatus);
                ImageView delete = (ImageView) v.findViewById(R.id.imageDelete);
                if (editing) {
                    delete.setVisibility(View.VISIBLE);
                }else{
                    delete.setVisibility(View.GONE);
                }
                View.OnClickListener ocl = new deleteOnClickListener(position, r.name, r.id);
                delete.setOnClickListener(ocl);
            }

            return v;
        }

    }

    public class deleteOnClickListener implements View.OnClickListener  {
        int position;
        String title;
        String id;
        public deleteOnClickListener(int position, String title, String id) {
            this.position = position;
            this.title = title;
            this.id = id;
            mPosition = position;
            mTitle = title;
            mID = id;
        }

        @Override
        public void onClick(View v)
        {
            new AlertDialog.Builder(RequestsActivity.this)
                .setTitle(getResources().getString(R.string.requests))
                .setMessage(String.format(getResources().getString(R.string.delete_request_message), this.title))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        requests.data.remove(position);
                        Gson gson = new Gson();
                        String json = gson.toJson(requests);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("requests", json);
                        editor.commit();
                        customAdapter.notifyDataSetChanged();
                        URL url;
                        try {
                            url = new URL(getString(R.string.api) + "requests/" + mID);
                            new deleteRequestTask().execute(url);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }

                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        }

    };
}
