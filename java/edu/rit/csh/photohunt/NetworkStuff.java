package edu.rit.csh.photohunt;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.ArrayList;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

public class NetworkStuff {

    //Returns whether or not the phone has network access
    public static boolean isNetworkAvailable(Activity act) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) act.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    //Makes a get request to page with no additional parameters
    public static String makeGetRequest(String page, String key)
    {
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        return makeGetRequest(page, params, key);
    }

    //Makes a get request to page with the specified additional parameters
    public static String makeGetRequest(String page, ArrayList<NameValuePair> params, String key)
    {
        params.add(new BasicNameValuePair("key", key));

        HttpClient client = getNewHttpClient();
        HttpResponse response = null;
        try {

            URI target = URIUtils.createURI("https", MainActivity.url, -1, "/" + page,
                    URLEncodedUtils.format(params, "UTF-8"), null);
            HttpGet request = new HttpGet(target);
            request.addHeader("Accept", "application/json");
            response = client.execute(request);

            if(response.getStatusLine().getStatusCode() != 200)
                Log.d("Hi", "Status: " + response.getStatusLine().getStatusCode());

            // Get the response
            BufferedReader rd = new BufferedReader
                    (new InputStreamReader(response.getEntity().getContent()));

            StringBuilder sb = new StringBuilder();
            String line = "";
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();
            return sb.toString();
        } catch (IOException e) {
            if(response != null && response.getStatusLine() != null)
            Log.e("Hi", "IOException on the get request to " + page);
        } catch (URISyntaxException e) {
            if(response != null && response.getStatusLine() != null)
            Log.e("Hi", "URISyntaxException on the get request to " + page);
            return null;
        }
        return null;
    }

    //Makes a get request to page with no additional parameters
    public static String makePostRequest(String page, String body, String key)
    {
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        return makePostRequest(page, params, body, key);
    }

    //Makes a get request to page with the specified additional parameters
    public static String makePostRequest(String page, ArrayList<NameValuePair> params, String body, String key)
    {
        Log.e("Photohunt", "Making post request to " + page);
        params.add(new BasicNameValuePair("key", key));

        HttpClient client = getNewHttpClient();

        HttpResponse response = null;
        try {

            URI target = URIUtils.createURI("https", MainActivity.url, -1, "/" + page,
                    URLEncodedUtils.format(params, "UTF-8"), null);
            HttpPost request = new HttpPost(target);
            request.addHeader("Accept", "application/json");
            request.setEntity(new StringEntity(body, "UTF8"));
            response = client.execute(request);

            if(response.getStatusLine().getStatusCode() != 200)
                Log.d("Hi", "Status: " + response.getStatusLine().getStatusCode());

            // Get the response
            BufferedReader rd = new BufferedReader
                    (new InputStreamReader(response.getEntity().getContent()));

            StringBuilder sb = new StringBuilder();
            String line = "";
            Log.e("Photohunt", "Reading message...");
            while ((line = rd.readLine()) != null) {
                Log.e("Photohunt", line);
                sb.append(line);
            }
            rd.close();
            return sb.toString();
        } catch (IOException e) {
            Log.e("Photohunt", "IOException on the get request to " + page);
            Log.e("Photohunt", e.toString());
        } catch (URISyntaxException e) {
            Log.e("Photohunt", "URISyntaxException on the get request to " + page);
            Log.e("Photohunt", e.toString());
        }
        return null;
    }

    public static HttpClient getNewHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }

}
