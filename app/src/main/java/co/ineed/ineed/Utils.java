package co.ineed.ineed;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

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
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Created by John on 10/07/2015.
 */
public class Utils {
    private static Context context;

    public static void showErrorDialog(Context ctx, String message, String title, DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener cancelListener){
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setMessage(message).setTitle(title);
        if (okListener != null){
            builder.setPositiveButton(R.string.button_positive, okListener);
        }
        if (cancelListener != null){
            builder.setNegativeButton(R.string.button_negative, cancelListener);
        }
        builder.show();
    }

    public void newSession(Context ctx) {
        context = ctx;
        //SharedPreferences prefs = context.getSharedPreferences("user", 0);
        //SharedPreferences.Editor editor = prefs.edit();
        //editor.putString("session", "");
        //editor.commit();
        URL url;
        try {
            url = new URL(context.getString(R.string.api) + "login/anon");
            new getSession().execute(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private class getSession extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            String result = "";
            String charset = "UTF-8";
            String GUID = UUID.randomUUID().toString().toUpperCase();
            InputStream response = null;
            String line = "";
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) urls[0].openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            connection.setDoInput(true);
            connection.setDoOutput(true); // Triggers POST.
            connection.setRequestProperty("Accept-Charset", charset);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            JSONObject jsonParam = new JSONObject();
            try {
                jsonParam.put("TrackingId", GUID);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String json = jsonParam.toString();
            Integer responseCode = 0;
            try {
                OutputStream output = connection.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(output, charset);
                osw.write(json);
                osw.flush ();
                osw.close ();
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
                JSONObject json = new JSONObject(result);
                SharedPreferences prefs = context.getSharedPreferences("user", 0);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("session", json.getString("sessionToken"));
                editor.commit();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public final static String getSessionToken(Context ctx) {
        context = ctx;
        SharedPreferences prefs = context.getSharedPreferences("user", 0);
        return prefs.getString("session", "");
    }

    public void getUser(Context ctx) {
        context = ctx;
        URL url;
        try {
            url = new URL(context.getString(R.string.api) + "users/me?session_token=" + getSessionToken(context));
            new getUserTask().execute(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
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
                connection.setRequestProperty("session_token", getSessionToken(context));
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
                    User user = gson.fromJson(result, User.class);
                    SharedPreferences prefs = context.getSharedPreferences("user", 0);
                    SharedPreferences.Editor editor = prefs.edit();
                    String json = gson.toJson(user);
                    editor.putString("user", json);
                    editor.commit();
                }
            }catch (JsonParseException e) {
                e.printStackTrace();
            }
        }
    }

    public String formatDate(String date) {
        String formatted = "";
        String[] separated = date.split("T");
        SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy");
        SimpleDateFormat fmt1 = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date d1 = fmt1.parse(separated[0]);
            formatted = fmt.format(d1);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return formatted;
    }

    public String formatDateTime(String date) {
        String formatted = "";
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        SimpleDateFormat fmt1 = new SimpleDateFormat("dd MMM yyyy HH:mm");
        try {
            Date d1 = fmt.parse(date);
            formatted = fmt1.format(d1);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return formatted;
    }
}
