package co.ineed.ineed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.ismaeltoe.FlowLayout;
import com.loopj.android.image.SmartImageView;
import com.stripe.model.Customer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by John on 12/07/2015.
 */
public class FrontActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private SharedPreferences prefs;
    private Utils utils;
    private User user;
    private ProgressDialog progressDialog;
    private MenuItems menuItems;
    final Gson gson = new Gson();
    private Boolean generalShowing = true;
    private Searches searches;
    private Location location;
    private AutoCompleteTextView autoCompView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.front_activity);
        utils = new Utils();
        prefs = getSharedPreferences("user", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("completed", true);
        editor.commit();
        user = new User();
        String json = prefs.getString("user", "");
        user = gson.fromJson(json, User.class);
        location = new Location();
        json = prefs.getString("location", "");
        location = gson.fromJson(json, Location.class);

        URL url;
        try {
            url = new URL(getString(R.string.api) + "menu");
            new getMenuTask().execute(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        autoCompView = (AutoCompleteTextView) findViewById(R.id.autocomplete);
        autoCompView.setDropDownBackgroundResource(R.drawable.transparent);
        autoCompView.setAdapter(new searchAutoCompleteAdapter(this, R.layout.search_item));
        autoCompView.setOnItemClickListener(this);


        json = prefs.getString("menu", "");
        if (json.length() > 0) {
            menuItems = gson.fromJson(json, MenuItems.class);
            buildMenu();
        }
        LinearLayout requests = (LinearLayout) findViewById(R.id.toolbarRequests);
        requests.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FrontActivity.this, RequestsActivity.class);
                startActivity(intent);
            }
        });
        LinearLayout profile = (LinearLayout) findViewById(R.id.toolbarProfile);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FrontActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });
        Intent intent = new Intent(FrontActivity.this, RegisterNotifications.class);
        startActivity(intent);
    }

    private class getMenuTask extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            String result = "";
            String charset = "UTF-8";
            Integer responseCode = 0;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) urls[0].openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept-Charset", charset);
                connection.setRequestProperty("session_token", utils.getSessionToken(FrontActivity.this));
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
                    String original = prefs.getString("menu", "");
                    if (!original.equals(result)) {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("menu", result);
                        editor.commit();
                        Gson gson = new Gson();
                        menuItems = gson.fromJson(result, MenuItems.class);
                        buildMenu();
                    }
                }
            }catch (JsonParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void buildMenu() {
        FlowLayout menu = (FlowLayout) findViewById(R.id.scrollMenu);
        menu.removeAllViews();
        for(int i = 0; i < menuItems.items.size(); i++) {
            MenuItem menuItem = menuItems.items.get(i);
            LinearLayout rl = buildMenuItem(menuItem);
            rl.setOnClickListener(handleOnClick(menuItem));
            menu.addView(rl);
        }
        com.loopj.android.image.SmartImageView imageGeneral = (com.loopj.android.image.SmartImageView) findViewById(R.id.imageGeneral);
        imageGeneral.setImageUrl(getString(R.string.image_url) + menuItems.general.icon + "?width=400&height=120&mode=crop");
        LinearLayout general = (LinearLayout) findViewById(R.id.general);
        general.setOnClickListener(handleOnClick2(menuItems.general));
    }
    private LinearLayout buildMenuItem(MenuItem menuItem) {
        LinearLayout ll = new LinearLayout(this);

        RelativeLayout rl = new RelativeLayout(this);
        rl.setBackgroundColor(0xFF999999);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(160, 160);
        LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(156, 156);
        rl.setLayoutParams(params);
        FrameLayout fl = new FrameLayout(this);
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(156, 156);
        fl.setLayoutParams(flp);
        SmartImageView img = new SmartImageView(this);
        img.setLayoutParams(params2);
        img.setImageUrl(getString(R.string.image_url) + menuItem.icon + "?width=156&height=156&mode=crop");
        fl.addView(img);
        ImageView trans = new ImageView(this);
        trans.setLayoutParams(params2);
        trans.setImageResource(R.drawable.menu_transparency);
        fl.addView(trans);
        rl.addView(fl);

        TextView txt = new TextView(this);
        txt.setTextColor(0xFFFFFFFF);
        txt.setText(menuItem.name);
        txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        //txt.setTypeface(null, Typeface.BOLD);
        RelativeLayout.LayoutParams lp=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        txt.setLayoutParams(lp);
        txt.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        rl.addView(txt);
        ll.addView(rl);
        return ll;
    }
    View.OnClickListener handleOnClick(final MenuItem menuItem) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                final LinearLayout general = (LinearLayout) findViewById(R.id.general);
                general.animate()
                    .translationY(140)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            if (generalShowing) {
                                general.setVisibility(View.GONE);
                                generalShowing = false;
                            }else{
                                generalShowing = true;
                            }
                        }
                    });
                FlowLayout menu = (FlowLayout) findViewById(R.id.scrollMenu);
                menu.removeAllViews();
                menu.addView(menuBackButton());
                for(int i = 0; i < menuItem.children.size(); i++) {
                    MenuItem mi = menuItem.children.get(i);
                    LinearLayout rl = buildMenuItem(mi);
                    if (mi.children.size() > 0) {
                        rl.setOnClickListener(handleOnClick(mi));
                    }else {
                        rl.setOnClickListener(handleOnClick2(mi));
                    }
                    menu.addView(rl);
                }
            }
        };
    }

    View.OnClickListener handleOnClick2(final MenuItem menuItem) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                //if (user.id != null) {
                    Intent intent = new Intent(FrontActivity.this, MakeRequestActivity.class);
                    intent.putExtra("menuItem", menuItem);
                    startActivity(intent);
                /*
                }else{
                    Intent intent = new Intent(FrontActivity.this, CreateAccountActivity.class);
                    intent.putExtra("user", user);
                    startActivity(intent);
                }
                */
            }
        };
    }
    View.OnClickListener handleOnClick3() {
        return new View.OnClickListener() {
            public void onClick(View v) {
                buildMenu();
                LinearLayout general = (LinearLayout) findViewById(R.id.general);
                general.setVisibility(View.VISIBLE);
                general.animate().translationY(0);
            }
        };
    }
    private LinearLayout menuBackButton() {
        LinearLayout back = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(160, 160);
        back.setLayoutParams(params);
        back.setBackgroundResource(R.drawable.back);
        TextView txt = new TextView(this);
        txt.setText(getResources().getString(R.string.back));
        txt.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        txt.setTextSize(14);
        txt.setTextColor(0xFF000000);
        LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        params2.setMargins(0, 0, 0, 30);
        txt.setLayoutParams(params2);
        back.addView(txt);
        back.setOnClickListener(handleOnClick3());
        return back;
    }

    // ********************* Search

    private List<String> autocomplete(String input) {
        ArrayList<String> resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(getString(R.string.api) + "search");
            sb.append("?query=" + input);
            sb.append("&lat=" + location.lat);
            sb.append("&lon=" + location.lon);

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("session_token", utils.getSessionToken(FrontActivity.this));
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            return resultList;
        } catch (IOException e) {
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        Gson gson = new Gson();
        searches = gson.fromJson(jsonResults.toString(), Searches.class);
        resultList = new ArrayList<String>(searches.data.size());
        for(int i = 0; i < searches.data.size(); i++) {
            Search search = searches.data.get(i);
            if (search.type.equals("Task")) {
                resultList.add(i, search.task.name);
            }
        }
        return resultList;
    }

    private class searchAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
        private ArrayList<String> resultList;

        public searchAutoCompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = (ArrayList<String>) autocomplete(constraint.toString());

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    }
                    else {
                        notifyDataSetInvalidated();
                    }
                }};
            return filter;
        }
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Search search = searches.data.get(position);
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(autoCompView.getWindowToken(), 0);
        autoCompView.setText("");
        for(int i = 0; i < menuItems.items.size(); i++) {
            MenuItem mi = menuItems.items.get(i);
            for(int i2 = 0; i2 < mi.children.size(); i2++) {
                MenuItem mi2 = mi.children.get(i2);
                if (mi2.name.equals(search.task.name)) {
                    Intent intent = new Intent(FrontActivity.this, MakeRequestActivity.class);
                    intent.putExtra("menuItem", mi2);
                    startActivity(intent);
                    return;
                }
            }
        }
    }
}
