package co.ineed.ineed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.loopj.android.image.SmartImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by John on 15/07/2015.
 */
public class RequestActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private Utils utils;
    private User user;
    private ProgressDialog progressDialog;
    private Request request;
    private ListAdapter customAdapter;
    private JSONObject jsonMessage = new JSONObject();
    private Boolean requestShowing = false;
    static final int REQUEST_CAMERA = 1;
    static final int SELECT_FILE = 2;
    static final int RECORD_AUDIO = 3;
    private Bitmap thumbnail;
    private String audioFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.request_activity);
        utils = new Utils();
        prefs = getSharedPreferences("user", 0);
        request = (Request) getIntent().getSerializableExtra("request");
        getSupportActionBar().setTitle(request.name);
        ListView lc = (ListView) findViewById(R.id.listMessages);
        customAdapter = new ListAdapter(this, R.layout.message_list, (ArrayList<Message>) request.messages);
        lc.setAdapter(customAdapter);
        lc.setSelection(customAdapter.getCount() - 1);
        final TextView send = (TextView) findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView message = (TextView) findViewById(R.id.message);
                if (message.getText().length() > 0) {
                    try {
                        jsonMessage.put("text", message.getText());
                        jsonMessage.put("type", "Text");
                        jsonMessage.put("link", "");
                        jsonMessage.put("image", "");
                        URL url;
                        try {
                            url = new URL(getString(R.string.api) + "requests/" + request.id + "/messages");
                            new newMessageTask().execute(url);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        message.setText("");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        ImageView paperclip = (ImageView) findViewById(R.id.paperclip);
        paperclip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectAttachment();
            }
        });
        final EditText message = (EditText) findViewById(R.id.message);
        message.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if(message.getText().length() == 0) {
                    send.setTextColor(0xFFADADAD);
                }else{
                    send.setTextColor(0xFF0f87ff);
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        for(int i=0; i < request.form.size();i++) {
            FormElements f = request.form.get(i);
            if (f.type.equals("Text")) {
                LinearLayout text = (LinearLayout) findViewById(R.id.layoutWhat);
                text.setVisibility(View.VISIBLE);
                TextView txt = (TextView) findViewById(R.id.textWhat);
                txt.setText(f.form_value);
            }else if (f.type.equals("Location")) {
                LinearLayout location = (LinearLayout) findViewById(R.id.layoutWhere);
                location.setVisibility(View.VISIBLE);
                TextView txt = (TextView) findViewById(R.id.textWhere);
                txt.setText(f.form_value);
            }else if (f.type.equals("Time")) {
                LinearLayout time = (LinearLayout) findViewById(R.id.layoutWhen);
                time.setVisibility(View.VISIBLE);
                TextView txt = (TextView) findViewById(R.id.textWhen);
                txt.setText(utils.formatDateTime(f.form_value));
            }else if (f.type.equals("TextArea")) {
                LinearLayout textArea = (LinearLayout) findViewById(R.id.layoutMore);
                textArea.setVisibility(View.VISIBLE);
                TextView txt = (TextView) findViewById(R.id.textMore);
                txt.setText(f.form_value);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_request, menu);
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
        }else if (id == R.id.action_request) {
            final LinearLayout layoutRequest = (LinearLayout) findViewById(R.id.layoutRequest);
            layoutRequest.setVisibility(View.VISIBLE);
            if (requestShowing) {
                Animation slide_down = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down);
                slide_down.setAnimationListener(new Animation.AnimationListener(){
                    @Override
                    public void onAnimationStart(Animation arg0) {
                    }
                    @Override
                    public void onAnimationRepeat(Animation arg0) {
                    }
                    @Override
                    public void onAnimationEnd(Animation arg0) {
                        layoutRequest.setVisibility(View.GONE);
                    }
                });
                LinearLayout ll = (LinearLayout) findViewById(R.id.layoutMessages);
                layoutRequest.startAnimation(slide_down);
                ll.startAnimation(slide_down);
                item.setIcon(R.drawable.dots);
                requestShowing = false;
            }else{
                Animation slide_up = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
                layoutRequest.startAnimation(slide_up);
                LinearLayout ll = (LinearLayout) findViewById(R.id.layoutMessages);
                ll.startAnimation(slide_up);
                item.setIcon(R.drawable.dots_vertical);
                requestShowing = true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public class ListAdapter extends ArrayAdapter<Message> {

        public ListAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        public ListAdapter(Context context, int resource, List<Message> items) {
            super(context, resource, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v = convertView;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.message_list, null);
            }

            final Message m = getItem(position);

            if (m != null) {
                LinearLayout admin = (LinearLayout) v.findViewById(R.id.admin);
                LinearLayout user = (LinearLayout) v.findViewById(R.id.user);
                LinearLayout photo = (LinearLayout) v.findViewById(R.id.adminImage);
                LinearLayout userPhoto = (LinearLayout) v.findViewById(R.id.userImage);
                LinearLayout button = (LinearLayout) v.findViewById(R.id.adminButton);
                photo.setVisibility(View.GONE);
                if (m.isFromUser) {
                    if (m.type.equals("Text")) {
                        admin.setVisibility(View.GONE);
                        user.setVisibility(View.VISIBLE);
                        button.setVisibility(View.GONE);
                        userPhoto.setVisibility(View.GONE);
                        TextView txt = (TextView) v.findViewById(R.id.textUserDate);
                        txt.setText(utils.formatDateTime(m.createdTime));
                        txt = (TextView) v.findViewById(R.id.textUserName);
                        txt.setText(m.name);
                        RoundedImageView riv = (RoundedImageView) v.findViewById(R.id.imageUser);
                        if (m.profileImage != null) {
                            riv.setImageUrl(getString(R.string.image_url) + m.profileImage + "?width=22&height=22&mode=crop");
                        } else {
                            riv.setImageResource(R.drawable.no_profile_picture);
                        }
                        txt = (TextView) v.findViewById(R.id.textUserMessage);
                        txt.setText(m.text);
                    }else if (m.type.equals("Image")) {
                        admin.setVisibility(View.GONE);
                        user.setVisibility(View.GONE);
                        button.setVisibility(View.GONE);
                        userPhoto.setVisibility(View.VISIBLE);
                        TextView txt = (TextView) v.findViewById(R.id.textUserDateImage);
                        txt.setText(utils.formatDateTime(m.createdTime));
                        txt = (TextView) v.findViewById(R.id.textUserNameImage);
                        txt.setText(m.name);
                        RoundedImageView riv = (RoundedImageView) v.findViewById(R.id.imageUserImage);
                        if (m.profileImage != null) {
                            riv.setImageUrl(getString(R.string.image_url) + m.profileImage + "?width=22&height=22&mode=crop");
                        }else{
                            riv.setImageResource(R.drawable.assistant_profile);
                        }
                        SmartImageView p = (SmartImageView) v.findViewById(R.id.imageUserPhoto);
                        p.setImageUrl(getString(R.string.image_url) + m.image + "?width=300&height=200&mode=crop");
                    }
                }else{
                    if (m.type.equals("Image")) {
                        admin.setVisibility(View.GONE);
                        user.setVisibility(View.GONE);
                        button.setVisibility(View.GONE);
                        photo.setVisibility(View.VISIBLE);
                        TextView txt = (TextView) v.findViewById(R.id.textAdminDateImage);
                        txt.setText(utils.formatDateTime(m.createdTime));
                        txt = (TextView) v.findViewById(R.id.textAdminNameImage);
                        txt.setText(m.name);
                        RoundedImageView riv = (RoundedImageView) v.findViewById(R.id.imageAdminImage);
                        if (m.profileImage != null) {
                            riv.setImageUrl(getString(R.string.image_url) + m.profileImage + "?width=22&height=22&mode=crop");
                        }else{
                            riv.setImageResource(R.drawable.assistant_profile);
                        }
                        SmartImageView p = (SmartImageView) v.findViewById(R.id.imageAdminPhoto);
                        p.setImageUrl(getString(R.string.image_url) + m.image + "?width=300&height=200&mode=crop");
                    }else if (m.type.equals("Text")) {
                        admin.setVisibility(View.VISIBLE);
                        user.setVisibility(View.GONE);
                        button.setVisibility(View.GONE);
                        TextView txt = (TextView) v.findViewById(R.id.textAdminDate);
                        txt.setText(utils.formatDateTime(m.createdTime));
                        txt = (TextView) v.findViewById(R.id.textAdminName);
                        txt.setText(m.name);
                        RoundedImageView riv = (RoundedImageView) v.findViewById(R.id.imageAdmin);
                        if (m.profileImage != null) {
                            riv.setImageUrl(getString(R.string.image_url) + m.profileImage + "?width=22&height=22&mode=crop");
                        }else{
                            riv.setImageResource(R.drawable.assistant_profile);
                        }
                        txt = (TextView) v.findViewById(R.id.textAdminMessage);
                        if (m.link != null) {
                            SpannableString content = new SpannableString(m.text);
                            content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                            txt.setText(content);
                            txt.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(RequestActivity.this, WebActivity.class);
                                    intent.putExtra("url", m.link);
                                    startActivity(intent);
                                }
                            });
                        }else{
                            txt.setText(m.text);
                        }
                    }else if (m.type.equals("Button")) {
                        admin.setVisibility(View.GONE);
                        user.setVisibility(View.GONE);
                        button.setVisibility(View.VISIBLE);
                        TextView txt = (TextView) v.findViewById(R.id.textAdminDateButton);
                        txt.setText(utils.formatDateTime(m.createdTime));
                        txt = (TextView) v.findViewById(R.id.textAdminName);
                        txt.setText(m.name);
                        RoundedImageView riv = (RoundedImageView) v.findViewById(R.id.imageAdminButton);
                        if (m.profileImage != null) {
                            riv.setImageUrl(getString(R.string.image_url) + m.profileImage + "?width=22&height=22&mode=crop");
                        }else{
                            riv.setImageResource(R.drawable.assistant_profile);
                        }
                        TextView btn = (TextView) v.findViewById(R.id.buttonAdminButton);
                        btn.setText(m.text);
                        btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(RequestActivity.this, LinkPaymentActivity.class);
                                intent.putExtra("has_close", 1);
                                startActivity(intent);
                            }
                        });
                    }
                }
            }

            return v;
        }

    }

    private class newMessageTask extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            String result = "";
            String charset = "UTF-8";
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
            connection.setRequestProperty("session_token", utils.getSessionToken(RequestActivity.this));
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            String json = jsonMessage.toString();
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
                if (result.equals("")) {
                    View view = RequestActivity.this.getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager)getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    URL url;
                    try {
                        url = new URL(getString(R.string.api) + "requests/" + request.id);
                        new getRequestTask().execute(url);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
        }
    }

    private class getRequestTask extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            String result = "";
            String charset = "UTF-8";
            Integer responseCode = 0;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) urls[0].openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept-Charset", charset);
                connection.setRequestProperty("session_token", utils.getSessionToken(RequestActivity.this));
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
                    request = gson.fromJson(result, Request.class);
                    ListView lc = (ListView) findViewById(R.id.listMessages);
                    customAdapter = new ListAdapter(RequestActivity.this, R.layout.message_list, (ArrayList<Message>) request.messages);
                    lc.setAdapter(customAdapter);
                    lc.setSelection(customAdapter.getCount() - 1);
                }
            }catch (JsonParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void selectAttachment() {
        final CharSequence[] items = { getResources().getString(R.string.take_picture), getResources().getString(R.string.choose_library), getResources().getString(R.string.record_request), getResources().getString(R.string.record_video), getResources().getString(R.string.cancel) };
        AlertDialog.Builder builder = new AlertDialog.Builder(RequestActivity.this);
        builder.setTitle(getResources().getString(R.string.send_picture));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals(getResources().getString(R.string.take_picture))) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, REQUEST_CAMERA);
                } else if (items[item].equals(getResources().getString(R.string.choose_library))) {
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE);
                } else if (items[item].equals(getResources().getString(R.string.record_request))) {
                    Intent intent = new Intent(RequestActivity.this, RecordAudio.class);
                    intent.setType("image/*");
                    startActivityForResult(Intent.createChooser(intent, "Record Audio"), RECORD_AUDIO);
                } else if (items[item].equals(getResources().getString(R.string.cancel))) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                thumbnail = (Bitmap) data.getExtras().get("data");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                //thumbnail.compress(Bitmap.CompressFormat.JPEG, 70, bytes);
                File destination = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".jpg");
                FileOutputStream fo;
                try {
                    destination.createNewFile();
                    fo = new FileOutputStream(destination);
                    fo.write(bytes.toByteArray());
                    fo.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                thumbnail = getResizedBitmap(thumbnail, 300, 200);
                //ivImage.setImageBitmap(thumbnail);
            } else if (requestCode == SELECT_FILE) {
                Uri selectedImageUri = data.getData();
                String[] projection = {MediaStore.MediaColumns.DATA};
                Cursor cursor = managedQuery(selectedImageUri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                cursor.moveToFirst();
                String selectedImagePath = cursor.getString(column_index);
                Bitmap bm;
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(selectedImagePath, options);
                final int REQUIRED_SIZE = 300;
                int scale = 1;
                while (options.outWidth / scale / 2 >= REQUIRED_SIZE && options.outHeight / scale / 2 >= REQUIRED_SIZE) scale *= 2;
                options.inSampleSize = scale;
                options.inJustDecodeBounds = false;
                thumbnail = BitmapFactory.decodeFile(selectedImagePath, options);

                //ivImage.setImageBitmap(bm);
            } else if (requestCode == RECORD_AUDIO) {
                audioFile = data.getExtras().getString("filename");
                URL url;
                try {
                    url = new URL(getString(R.string.api_image) + "?category=Item");
                    new postAudio().execute(url);
                    return;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            URL url;
            try {
                url = new URL(getString(R.string.api_image) + "?category=Item");
                new postThumbnail().execute(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }




    private class postThumbnail extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            String result = "";
            String charset = "UTF-8";
            String line = "";
            InputStream response = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) urls[0].openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            connection.setDoInput(true);
            connection.setDoOutput(true); // Triggers POST.
            connection.setRequestProperty("Accept-Charset", charset);
            connection.setRequestProperty("session_token", utils.getSessionToken(RequestActivity.this));
            connection.setRequestProperty("Content-Type", "image/jpeg");
            connection.setRequestProperty("Accept", "application/json");
            Integer responseCode = 0;
            try {
                OutputStream output = connection.getOutputStream();
                // compress and write the image to the output stream
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 70, output);
                output.close();
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
                String id = json.getString("id");
                TextView message = (TextView) findViewById(R.id.message);
                jsonMessage.put("text", message.getText());
                jsonMessage.put("type", "Image");
                jsonMessage.put("link", "");
                jsonMessage.put("image", id);
                URL url;
                try {
                    url = new URL(getString(R.string.api) + "requests/" + request.id + "/messages");
                    new newMessageTask().execute(url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    private class postAudio extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            String result = "";
            String charset = "UTF-8";
            String line = "";
            InputStream response = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) urls[0].openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }

            File file = new File(audioFile);
            byte fileContent[] = new byte[(int)file.length()];
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                fileInputStream.read(fileContent);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            connection.setDoInput(true);
            connection.setDoOutput(true); // Triggers POST.
            connection.setRequestProperty("Accept-Charset", charset);
            connection.setRequestProperty("session_token", utils.getSessionToken(RequestActivity.this));
            connection.setRequestProperty("Content-Type", "audio/3gpp");
            connection.setRequestProperty("Accept", "application/json");
            Integer responseCode = 0;
            try {
                OutputStream output = connection.getOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(output);
                oos.writeObject(fileContent);
                oos.close();
                output.close();
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
                String id = json.getString("id");
                TextView message = (TextView) findViewById(R.id.message);
                jsonMessage.put("text", message.getText());
                jsonMessage.put("type", "Image");
                jsonMessage.put("link", "");
                jsonMessage.put("image", id);
                URL url;
                try {
                    url = new URL(getString(R.string.api) + "requests/" + request.id + "/messages");
                    new newMessageTask().execute(url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }



    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }


}
