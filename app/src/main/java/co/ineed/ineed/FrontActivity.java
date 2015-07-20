package co.ineed.ineed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.ismaeltoe.FlowLayout;
import com.loopj.android.image.SmartImageView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by John on 12/07/2015.
 */
public class FrontActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private Utils utils;
    private User user;
    private ProgressDialog progressDialog;
    private MenuItems menuItems;
    final Gson gson = new Gson();
    private Boolean generalShowing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.front_activity);
        utils = new Utils();
        prefs = getSharedPreferences("user", 0);
        URL url;
        try {
            url = new URL(getString(R.string.api) + "menu?session_token=" + utils.getSessionToken(FrontActivity.this));
            new getMenuTask().execute(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        String json = prefs.getString("menu", "");
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
        ll.setBackgroundColor(0x000);
        RelativeLayout rl = new RelativeLayout(this);
        rl.setBackgroundColor(0xFF999999);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(160, 160);
        rl.setLayoutParams(params);
        SmartImageView img = new SmartImageView(this);
        LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(156, 156);
        img.setLayoutParams(params2);
        img.setImageUrl(getString(R.string.image_url) + menuItem.icon + "?width=156&height=156&mode=crop");
        img.setAlpha((float) 0.6);
        rl.addView(img);
        TextView txt = new TextView(this);
        txt.setTextColor(0xFFFFFFFF);
        txt.setText(menuItem.name);
        txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        txt.setTypeface(null, Typeface.BOLD);
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
                Intent intent = new Intent(FrontActivity.this, MakeRequestActivity.class);
                intent.putExtra("menuItem", menuItem);
                startActivity(intent);
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
    private ImageView menuBackButton() {
        ImageView img = new ImageView(FrontActivity.this);
        LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(160, 160);
        img.setLayoutParams(params2);
        img.setImageResource(R.drawable.back);
        img.setScaleType(ImageView.ScaleType.CENTER);
        img.setOnClickListener(handleOnClick3());
        return img;
    }
}
