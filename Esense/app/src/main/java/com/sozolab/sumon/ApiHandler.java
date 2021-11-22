package com.sozolab.sumon;

import android.os.AsyncTask;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

public class ApiHandler  {
    private String hostURL;

    public ApiHandler(String hostIPString, String port) {
        this.hostURL = hostIPString + port;
    }

    public void getAction(String path) {
        OutputStream out = null;
        String urlString = this.hostURL + path;
        System.out.print("getAction on - " + urlString + path);
        HttpURLConnection urlConnection = null;

        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            // get response
            InputStream in = urlConnection.getInputStream();
            InputStreamReader isw = new InputStreamReader(in);
            int data = isw.read();
            while (data != -1) {
                char current = (char) data;
                data = isw.read();
                System.out.print(current);
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

}
