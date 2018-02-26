package com.example.android.stationfinder;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
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
    //@BindView(R.id.tv_test)
   // TextView tv_test;
    @BindView(R.id.actv_search)
    AutoCompleteTextView actv_search;
    ArrayList<TransportUnit> transportUnits;
    ArrayList<String> stationNames;
    private WienerLinienDBHelper wienerLinienDBHelper;
    SQLiteDatabase db;
   // private int id;
    ArrayList<Integer> rbls;

    /*** In onCreate wird die Activity erstellt, das layout festgelegt, die diversen Eingabefelder und Buttons aktiviert und die benötigten Variablen instanziiert **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        switch (getResources().getConfiguration().orientation) {

            case Configuration.ORIENTATION_PORTRAIT:
                setContentView(R.layout.activity_main);
                break;

            case Configuration.ORIENTATION_LANDSCAPE:
                setContentView(R.layout.activity_main_landscape);
                break;
        }
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
            /**In der Variable "response" wird das Ergebnis gespeichert, welches als Antwort auf die Anfrage an die Wiener Linien API gesendet wird */
            String response;


            try {
                response = WienerLinenApi.getHttpResponse(urls[0]);

            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            try {

                /** Hier wird der String response in ein JSONObject umgewandelt
                 * das ist notwendig um zielgerichtet die benötigten Elemente aus der response herauszulesen
                 * wir werden uns die originale Antwort im Detail gemeinsam ansehen
                 * damit ist dann auch klar, dass in der Antwort viel mehr Informationen enthalten sind als wir brauchen*/

                JSONObject realtimeJSON = new JSONObject(response);
                JSONObject monitorData = realtimeJSON.getJSONObject("data");
                JSONArray monitors = monitorData.getJSONArray("monitors");
                //Testing variable
                int test = monitors.length();


                mainActivityWeakReference.get().transportUnits = new ArrayList<>();


                /** In der Schleife werden die einzelnen Monitore durchgegangen, vorübergehend in Objekte gespeichert und jeweils die wesentlichen Informationen herausgeholt
                 * diese Informationen werden in einem selbst erstellten Objekt (TransportUnit) gespeichert
                 * die TransportUnits werden in einer Liste gespeichert, welche dann der nächsten Activity übergeben werden um die Haltestellen auf einer Karte anzuzeigen*/

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

        /**
         * Die Methode onPostExecute() wird erst ausgeführt, wenn der AsyncTask fertig ist, sprich wenn die Antwort auf die Http-Anfrage fertig geladen hat
         * erst hier wird die nächste Activiy gestartet
         * das ist wichtig, denn sonst wären die angeforderten Informationen unvollständig und das Programm würde abstürzen, da der Prozess einfach unterbrochen wird
         * wie wir bereits in der MenuActivity gesehen haben, wird eine neue Activity mittels eines Intent gestartet
         * ein Intent ist ein Objekt zur Nachrichtenübermittlung
         * ihm wird die Liste mit den Stationsinformationen mitgegeben
         */

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

    /**
     * Diese Funktion wird aufgerufen sobald der Button btn_go gedrückt wird
     * es wird die Eingabe des Users aus dem Edit Textfeld herausgeholt und behandelt
     * mittels Datenbank Funktionen wird überprüft, ob zu diesem Namen eine RBL Nummer existiert
     * wenn nicht, dann wird ein Dialog erstellt, welcher den User darauf hinweist, dass die Daten unvollständig sind
     * ansonsten wird der AsyncTask gestartet, welcher die Informationen von der Wiener Linien API anfordert
     */

    @OnClick(R.id.btn_go)
    public void submit() {

        /*String userInput = et_rbl.getText().toString();
        String[] nums = userInput.split(",");
       // new GetRealtimeTask().execute(WienerLinenApi.buildWienerLinienMonitorUrl(nums));
        id = Integer.parseInt(userInput);
        tv_test.setText(wienerLinienDBHelper.getStationName(id));*/

        String userInput = actv_search.getText().toString();

        rbls = wienerLinienDBHelper.getRBLs(wienerLinienDBHelper.getStationId(userInput));
        int id = wienerLinienDBHelper.getStationId(userInput);
        if (id == 0) {

            Toast.makeText(getApplicationContext(), "Station does not exist!", Toast.LENGTH_SHORT).show();

            actv_search.getText().clear();
        } else if (rbls != null) {
            new GetRealtimeTask(MainActivity.this).execute(WienerLinenApi.buildWienerLinienMonitorUrl(rbls));
        } else {

            Toast.makeText(getApplicationContext(), "Data is incomplete for this station", Toast.LENGTH_SHORT).show();
            actv_search.getText().clear();
        }

    }

}

