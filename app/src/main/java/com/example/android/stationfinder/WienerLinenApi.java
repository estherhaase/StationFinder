package com.example.android.stationfinder;

import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import static com.example.android.stationfinder.BuildConfig.API_KEY;

public class WienerLinenApi {

    private final static String WL_AUTHORITY = "www.wienerlinien.at";
    private final static String REALTIME_PATH = "ogd_realtime";
    private final static String MONITOR_PATH = "monitor";
    private final static String RBL_PARAM = "rbl";
    private final static String TRAFFIC_INFO_PARAM = "activateTrafficInfo";
    private final static String TRAFFIC_INFO_PATH = "stoerungkurz";
    private final static String KEY_PARAM = "sender";
    private final static String IESLAMP_AUTHORITY = "ieslamp.technikum-wien.at";
    private final static String DB_PATH = "sys_bvu4_17_j";
    private final static String PROJECT_PATH = "Stationfinder";
    private final static String FILE_PATH = "QueryData.php";


    public static URL buildWienerLinienMonitorUrl(ArrayList<Integer> rblNum){

        Uri monitorUri;
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .encodedAuthority(WL_AUTHORITY)
                .appendEncodedPath(REALTIME_PATH)
                .appendEncodedPath(MONITOR_PATH)
                .appendQueryParameter(TRAFFIC_INFO_PARAM, TRAFFIC_INFO_PATH)
                .appendQueryParameter(KEY_PARAM, API_KEY);


        for(int i= 0; i < rblNum.size(); i++){
            builder.appendQueryParameter(RBL_PARAM, rblNum.get(i).toString());
        }

        monitorUri = builder.build();


        URL url = null;
        try {
            url = new URL(monitorUri.toString());
        }catch (MalformedURLException e){
            e.printStackTrace();
        }

        return url;
    }

    public static URL buildDatabaseRequestUrl(){

        Uri dbUri;
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .encodedAuthority(IESLAMP_AUTHORITY)
                .appendEncodedPath(DB_PATH)
                .appendEncodedPath(PROJECT_PATH)
                .appendEncodedPath(FILE_PATH);

        dbUri = builder.build();
        URL url = null;
        try {
            url = new URL(dbUri.toString());
        }catch (MalformedURLException e){
            e.printStackTrace();
        }

        return url;

    }

    public static String getHttpResponse(URL url) throws IOException {

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if(hasInput){
                return scanner.next();
            }else {
                return null;
            }

    }finally {
            urlConnection.disconnect();
        }
        }
}
