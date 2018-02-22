package com.example.android.stationfinder;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/*** In dieser Activity sind ein Textfeld (actv_search) und ein Button (btn_go) zu finden
 *  actv steht für AutoCompleteTextView. Gibt man hier einen Stationsnamen ein, werden Vorschläge gemacht für die Eingabe.
 *  Diese Vorschläge stammen aus dem internen Speicher des Android Gerätes, welcher zu Beginn einmal beschrieben wird.
 *  Die Stationsinfos stammen alle von OpenData Quellen der Wiener Linien
 *
 *  wird ein name ausgewählt und der btn_go gedrückt finden im Hintergrund mehrere Prozesse statt
 *  Es wird aus dem internen Speicher mittels Stationsnamen die Stations-ID herausgesucht
 *  Mittels Stations-ID werden nun alle Steignummern (RBL-Nummern) gesucht und in einer Liste gespeichert
 *  Eine Station besteht oftmals aus mehreren Steignummern, da für jede Linie und jede Richtung ein eigener Steig existiert
 *
 *  Die RBL-Nummern braucht man um eine Anfrage an die Schnittstelle der Wiener Linien für Echtzeitinformationen zu erhalten
 *  Das geschieht mittels der sogenannten RESTful Webservices
 *  Hier wird eine Ressource, welche sich auf einem Webserver befindet über einen eindeutig identifizierbaren Namen angesprochen
 *
 *  Somit haben wir nun alle Linien, welche die ausgewählte Station passieren und deren Echtzeit Abfahrtsdaten
 *  Diese schicken wir an die StationActivity ---->
 *  */


public class MainActivity extends Activity {

    //@BindView(R.id.et_rbl)
    // EditText et_rbl;
    @BindView(R.id.btn_go)
    Button btn_go;
    @BindView(R.id.tv_test)
    TextView tv_test;
    @BindView(R.id.actv_search)
    AutoCompleteTextView actv_search;
    ArrayList<TransportUnit> transportUnits;
    ArrayList<String> stationNames;
    private WienerLinienDBHelper wienerLinienDBHelper;
    SQLiteDatabase db;
    private int id;
    ArrayList<Integer> rbls;

    /*** In onCreate wird die Activity erstellt, das layout festgelegt, die diversen Eingabefelder und Buttons aktiviert und die benötigten Variablen instanziiert **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        wienerLinienDBHelper = WienerLinienDBHelper.getsInstance(getApplicationContext());
        db = wienerLinienDBHelper.getWritableDatabase();

        stationNames = wienerLinienDBHelper.getAllStationNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, stationNames);
        actv_search.setAdapter(adapter);

        if(savedInstanceState != null){
            actv_search.setText(savedInstanceState.getString("searchterm"));
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("searchterm", actv_search.getText().toString());
        super.onSaveInstanceState(outState);
    }

    /*** Der AsyncTask macht die Anfragen im Hintergrund und speichert die Ergebnisse
     * Hier passiert etwas wirklich Interessantes, die Informationen werden von einem Webserver geholt, ausgelesen und gespeichert
     * Dabei wird das Format JSON verwendet
     *
     * JSON wird zum Datenaustausch verwendet
     * Es ist leicht lesbar für Menschen und leicht von Maschinen zu erstellen bzw. umwandeln
     *
     * Die Aufgabe ist es nun sich die Struktur der JSON Daten anzusehen (das machen wir gemeinsam) und zu überlegen, wie wir eine weitere Information (barrierFree) herauslesen und im Code verwenden können
     * Ziel ist es in der StationActivity die Eigenschaft barrierFree unter den Abfahrtszeiten anzuzeigen**/

    static class GetRealtimeTask extends AsyncTask<URL, Void, String[]> {

        private final WeakReference<MainActivity> mainActivityWeakReference;
        GetRealtimeTask(MainActivity context){
            mainActivityWeakReference = new WeakReference<>(context);
        }

        @Override
        protected String[] doInBackground(URL... urls) {
            String response;


            try {
                response = WienerLinenApi.getHttpResponse(urls[0]);

            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            try {

                JSONObject realtimeJSON = new JSONObject(response);
                JSONObject monitorData = realtimeJSON.getJSONObject("data");
                JSONArray monitors = monitorData.getJSONArray("monitors");
                //Testing variable
                int test = monitors.length();



                mainActivityWeakReference.get().transportUnits = new ArrayList<>();

                for (int i = 0; i < test; i++) {

                    String[] depTime = new String[2];
                    boolean barrierFree = false;
                    JSONObject currentMonitor = monitors.getJSONObject(i);

                    JSONArray coords = currentMonitor.getJSONObject("locationStop").getJSONObject("geometry").getJSONArray("coordinates");
                    Double lat = coords.getDouble(1);
                    Double lon = coords.getDouble(0);
                    String lineName = currentMonitor.getJSONArray("lines").getJSONObject(0).getString("name");
                    String lineDirection = currentMonitor.getJSONArray("lines").getJSONObject(0).getString("towards");

                    for(int j = 0; j < currentMonitor.getJSONArray("lines").getJSONObject(0).getJSONObject("departures").getJSONArray("departure").length() && j < 2; j++){

                        depTime[j] = currentMonitor.getJSONArray("lines").getJSONObject(0).getJSONObject("departures").getJSONArray("departure").getJSONObject(j).getJSONObject("departureTime").getString("countdown");

                    }


                    TransportUnit temp = new TransportUnit(lineName, lineDirection, depTime, lat, lon, barrierFree);

                    mainActivityWeakReference.get().transportUnits.add(temp);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return new String[0];
        }

        @Override
        protected void onPostExecute(String[] strings) {

            // stationLocation = new Location("");
            Intent intent = new Intent( mainActivityWeakReference.get() , StationActivity.class);
            // intent.putExtra("RBL_ARRAY", nums);

            intent.putParcelableArrayListExtra("RequestedTransportUnits", mainActivityWeakReference.get().transportUnits);
            intent.putIntegerArrayListExtra("rbls", mainActivityWeakReference.get().rbls);
            mainActivityWeakReference.get().startActivity(intent);
            //super.onPostExecute(strings);
        }
    }


    @OnClick(R.id.btn_go)
    public void submit() {

        /*String userInput = et_rbl.getText().toString();
        String[] nums = userInput.split(",");
       // new GetRealtimeTask().execute(WienerLinenApi.buildWienerLinienMonitorUrl(nums));
        id = Integer.parseInt(userInput);
        tv_test.setText(wienerLinienDBHelper.getStationName(id));*/

        String userInput = actv_search.getText().toString();
        rbls = wienerLinienDBHelper.getRBLs(wienerLinienDBHelper.getStationId(userInput));
        new GetRealtimeTask(MainActivity.this).execute(WienerLinenApi.buildWienerLinienMonitorUrl(rbls));

    }

}

