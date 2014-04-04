package edu.rit.csh.photohunt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Semaphore;


public class MainActivity extends Activity {
    TextView startTime, startDate, endTime, endDate, timeRemaining, 
         teamName, picturesUploaded, invalidText, remainingTextView;
    Button uploadPictureButton, openCameraButton, instructionsButton;
    ProgressDialog pd = null;
    ProgressDialog pdupload = null;

    String startTimeText = "";
    String startDateText = "";
    String endTimeText = "";
    String endDateText = "";
    String numPics = "";

    SimpleDateFormat dateformat
            = new SimpleDateFormat("MM/dd/yyyy HH:mm");
    Date start;
    Date end;

    KillableThread timesThread = null;
    KillableThread numPicsThread = null;

    Semaphore changingSomething = new Semaphore(1, true);
    String key = null;
    public static String url = "photohunt-server.csh.rit.edu:3912";
    String team = "";

    String tellTheUser = null;

    boolean keyIsValid = false;
    boolean showpd = false;
    boolean showpdupload = false;

    SharedPreferences pm = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startTime = (TextView) findViewById(R.id.startTime);
        startDate = (TextView) findViewById(R.id.startDate);
        endTime = (TextView) findViewById(R.id.endTime);
        endDate = (TextView) findViewById(R.id.endDate);
        timeRemaining = (TextView) findViewById(R.id.timeRemaining);
        remainingTextView = 
                     (TextView) findViewById(R.id.remainingTextView);
        teamName = (TextView) findViewById(R.id.teamName);
        picturesUploaded = 
                      (TextView) findViewById(R.id.picturesUploaded);
        invalidText = (TextView) findViewById(R.id.invalidText);
        uploadPictureButton = 
                    (Button) findViewById(R.id.uploadPicturesButton);
        openCameraButton =
                        (Button) findViewById(R.id.openCameraButton);
        instructionsButton =
                      (Button) findViewById(R.id.instructionsButton);

        lock();
        if(pm == null)
        {
            pm = PreferenceManager.getDefaultSharedPreferences(this);
        }

        if(key == null)
        {
            key = pm.getString("key", "");
        }
        Log.d("Photohunt", "key = " + key);
        unlock();

        MyCount counter = new MyCount(1000000000, 1000);
        counter.start();

        uploadPictureButton.setOnClickListener(
                new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), 1337);
            }
        });

        openCameraButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
                        startActivity(intent);
                    }
                }
        );

        instructionsButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Instructions")
                                .setMessage(
                                        "This is the app for Photohunt. In order to talk to " +
                                        "the server, you'll need a key. This should be given " +
                                        "to you by one of the judges before Photohunt begins. " +
                                        "Enter your key in to this app's settings. Once the " +
                                        "\"Invalid API Key\" warning goes away, you can start " +
                                        "uploading pictures. Take pictures normally on your phone, " +
                                        "with the camera app. There's a button here to open it for " +
                                        "convenience. Once you have a picture you like, upload it!\n" +
                                        "Protip: the server won't let you upload the same picture " +
                                        "twice, so if you're ever not sure if you uploaded something " +
                                        "just upload it again."
                                )
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // continue with delete
                                    }
                                })
                                .show();
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        lock();
        if(pd != null) pd.dismiss();
        if(!showpd) {
            pd = ProgressDialog.show(this, "", "Loading...");
            showpd = true;
        }
        key = pm.getString("key", "");
        unlock();
        verifyKey();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1337 
                && resultCode == RESULT_OK 
                && null != data) {
            //Uri selectedImage = data.getData();
            //String[] filePathColumn = {MediaStore.Images.Media.DATA};

            //Cursor cursor = getContentResolver().query(selectedImage,
            //        filePathColumn, null, null, null);
            //cursor.moveToFirst();

            //int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            //String picturePath = cursor.getString(columnIndex);
            //cursor.close();

            // String picturePath contains the path of selected Image

            Uri selectedImageUri = data.getData();
            Log.d("Photohunt", "Returned URI: " + selectedImageUri.toString());
            String picturePath = getPath(selectedImageUri);
            Log.d("Photohunt", "Picture selected: " + picturePath);

            lock();
            pdupload = ProgressDialog.show(this, "", "Uploading...");
            showpdupload = true;
            unlock();

            uploadImage(picturePath);
        }
    }



    /**
     * helper to retrieve the path of an image URI
     */
    public String getPath(Uri uri) {
        // just some safety built in
        if( uri == null ) {
            // TODO perform some logging or show user feedback
            return null;
        }
        // try to retrieve the image from the media store first
        // this will only work for images selected from gallery
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String returnValue = cursor.getString(column_index);
            if(returnValue != null) return returnValue;
        }

        if(Build.VERSION.SDK_INT < 19) {
            // this is our fallback here
            Log.e("Photohunt", "Error reading and below api level 19. Getting the uri's path.");
            String returnValue = uri.getPath();
            return returnValue;
        }
        else {
            Log.e("Photohunt", "Error reading, trying method 2");

            // Will return "image:x*"
            String wholeID = DocumentsContract.getDocumentId(uri);

            // Split at colon, use second item in the array
            String id = wholeID.split(":")[1];

            String[] column = {MediaStore.Images.Media.DATA};

            // where id is equal to
            String sel = MediaStore.Images.Media._ID + "=?";

            cursor = getContentResolver().
                    query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            column, sel, new String[]{id}, null);

            String filePath = null;

            int columnIndex = cursor.getColumnIndex(column[0]);

            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }

            cursor.close();
            return filePath;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu 
        // this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }

    public void lock() {
        try {
            changingSomething.acquire();
        } catch (InterruptedException e) {
            Log.e("Photohunt", "Error acquiring lock");
        }
    }

    public void unlock() {
        changingSomething.release();
    }

    public void verifyKey() {
        new Thread() {
            public void run() {
                if(setKey(key))
                {
                    lock();
                    keyIsValid = true;
                    showpd = false;
                    unlock();

                    updateTimes();
                    fetchAndSaveTeamName();
                    fetchAndSaveNumPics();
                }
                else
                {
                    lock();
                    keyIsValid = false;
                    showpd = false;
                    unlock();
                }
            }
        }.start();
    }

    public void updateTimes() {
        if(timesThread != null) {
            timesThread.kill();
            timesThread = null;
        }
        timesThread = new KillableThread() {
            public void run() {
                while(!kill) {
                    lock();
                    if(keyIsValid) {
                        unlock();
                        String times = 
                            NetworkStuff.makeGetRequest("times", key);
                        if (!"".equals(times)) {
                            lock();
                            String[] times1 = times.split(" until ");
                            String[] times2 = times1[0].split(" at ");
                            startTimeText = times2[1];
                            startDateText = times2[0];
                            String[] times3 = times1[1].split(" at ");
                            endTimeText = times3[1];
                            endDateText = times3[0];

                            try {
                                start = dateformat.parse(
                                        times2[0] + " " + times2[1]);

                                end = dateformat.parse(
                                        times3[0] + " " + times3[1]);
                            } catch (ParseException e) {
                                Log.e("Photohunt", 
                                            "Error parsing dates");
                            }
                            Log.d("Photohunt", 
                                        "Start: " + start.toString());

                            unlock();
                        }
                    }
                    else
                        unlock();
                    try {
                        Thread.sleep(300000); //Sleep for 5 minutes
                    } catch (InterruptedException e) {
                        Log.e("Photohunt", "Error sleeping in thread1 for updateTimes");
                    }
                }
            }
        };
        timesThread.start();
    }

    private void fetchAndSaveTeamName() {
        new Thread() {
            public void run() {
                String temp = NetworkStuff.makeGetRequest("team", key);
                lock();
                team = temp;
                unlock();
            }
        }.start();
    }

    private void fetchAndSaveNumPics() {
        if(numPicsThread != null) {
            numPicsThread.kill();
            numPicsThread = null;
        }
        numPicsThread = new KillableThread() {
            public void run() {
                while(true) {
                    if(keyIsValid) {
                        String temp = 
                           NetworkStuff.makeGetRequest("numpics", key);
                        lock();
                        numPics = temp;
                        unlock();
                    }
                    try {
                        Thread.sleep(300000); //Sleep for 5 minutes
                    } catch (InterruptedException e) {
                        Log.e("Photohunt", 
                              "Error sleeping in fetchAndSameNumPics");
                    }
                }
            }
        };
        numPicsThread.start();
    }

    public void uploadImage(final String picturePath) {
        new Thread() {
            public void run() {
                Log.e("Photohunt", "Uploading image...");
                //Convert image to a byte[]
                Bitmap bm = BitmapFactory.decodeFile(picturePath);
                if(bm == null) {
                    lock();
                    tellTheUser = "Error decoding image";
                    showpdupload = false;
                    unlock();
                    return;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                //bm is the bitmap object
                bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] byteArrayImage = baos.toByteArray();

                //Encode it in base64
                String encodedImage =
                        Base64.encodeToString(byteArrayImage, Base64.DEFAULT);

                MessageDigest digest = null;
                try {
                    digest = MessageDigest.getInstance("SHA-256");
                    digest.reset();
                } catch (NoSuchAlgorithmException e) {
                    Log.e("Photohunt", "SHA-256 isn't available");
                }

                byte[] imageHash = digest.digest(byteArrayImage);
                String encodedImageHash =
                        Base64.encodeToString(imageHash, Base64.URL_SAFE);

                Log.d("Photohunt", "Hash length: " + encodedImageHash.length());
                Log.d("Photohunt", "Image length: " + encodedImage.length());

                ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("key", key));
                params.add(new BasicNameValuePair("hash", encodedImageHash));
                params.add(new BasicNameValuePair("fileextension", "jpg"));

                String message = NetworkStuff.makePostRequest("upload", params, encodedImage, key);
                lock();
                if(message == null) {
                    tellTheUser = "Error uploading image";
                } else if(!message.equals("File received")) {
                    tellTheUser = message;
                }
                fetchAndSaveNumPics();

                showpdupload = false;
                unlock();
            }
        }.start();
    }

    private boolean setKey(String tempkey) {
        ArrayList<NameValuePair> params 
                = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("key", tempkey));
        HttpClient client = NetworkStuff.getNewHttpClient();
        HttpResponse response = null;
        try {
            URI target = URIUtils.createURI("https", url, -1, "/times",
                    URLEncodedUtils.format(params, "UTF-8"), null);
            HttpGet request = new HttpGet(target);
            request.addHeader("Accept", "application/json");
            response = client.execute(request);

            if(response.getStatusLine().getStatusCode() == 200) {
                Log.d("Photohunt", "Key is valid");
                key = tempkey;
                return true;
            }
            Log.d("Photohunt", "Key is invalid");
            return false;
        } catch (ClientProtocolException e) {
            Log.e("photohunt", "ClientProtocolException for setting key: " + e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("photohunt", "IOException for setting key: " + e.toString());
            e.printStackTrace();
        } catch (URISyntaxException e) {
            Log.e("photohunt", "URISyntaxException for setting key: " + e.toString());
            e.printStackTrace();
        }
        return false;
    }
    // countdowntimer is an abstract class, 
    // so extend it and fill in methods
    public class MyCount extends CountDownTimer {

        public MyCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
        }

        @Override
        public void onTick(long millisUntilFinished) {
            lock();
            if(end != null) {
                Date now = new Date();
                if(now.getTime() < start.getTime()) {
                    long diff = start.getTime() - now.getTime();
                    timeRemaining.setText(
                            +diff / 1000 / 60 / 60 + "h "
                                    + (diff / 1000 / 60) % 60 + "m "
                                    + (diff / 1000) % 60 + "s"
                    );
                    remainingTextView.setText("Until the start");
                } else if(now.getTime() < end.getTime()) {
                    long diff = end.getTime() - now.getTime();
                    timeRemaining.setText(
                            +diff / 1000 / 60 / 60 + "h "
                                    + (diff / 1000 / 60) % 60 + "m "
                                    + (diff / 1000) % 60 + "s"
                    );
                    remainingTextView.setText("Remaining");
                }
                else {
                    long diff = now.getTime() - end.getTime();
                    timeRemaining.setText(
                            +diff / 1000 / 60 / 60 + "h "
                                    + (diff / 1000 / 60) % 60 + "m "
                                    + (diff / 1000) % 60 + "s"
                    );
                    remainingTextView.setText("Since the end");

                }
            }
            if(keyIsValid) {
                invalidText.setVisibility(View.GONE);
            } else {
                invalidText.setVisibility(View.VISIBLE);
            }
            if(!showpd && pd != null) {
                pd.dismiss();
                pd = null;
            }
            if(!showpdupload && pdupload != null) {
                pdupload.dismiss();
                pdupload = null;
            }
            startTime.setText(startTimeText);
            startDate.setText(startDateText);
            endTime.setText(endTimeText);
            endDate.setText(endDateText);
            teamName.setText("Team: " + team);
            picturesUploaded.setText(numPics + " pictures uploaded");

            if(tellTheUser != null) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error")
                        .setMessage(tellTheUser)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue with delete
                            }
                        })
                        .show();
                tellTheUser = null;
            }
            unlock();
        }
    }

    public abstract class KillableThread extends Thread {
        boolean kill = false;

        public void kill() {
            kill = true;
        }
    }
}
