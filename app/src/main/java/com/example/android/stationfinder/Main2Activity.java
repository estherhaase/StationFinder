package com.example.android.stationfinder;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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


/**
 * In der MainActivity passiert fast genau das gleiche wie in der Main2Activity, außer das hier zu Übungszwecken eine Station auch anhand ihrer RBL Nummer gesucht werden kann
 * das macht einerseits die http Anfrage einfacher, da diese mit der RBL Nummer gestellt wird
 * andererseits muss hier mehr auf die Eingabe des Users geachtet werden, da keine Vorschläge generiert werden wie bei der Suche mittels Namen
 */

public class Main2Activity extends Activity {

    @BindView(R.id.btn_go)
    Button btn_go;

    @BindView(R.id.et_rbl)
    EditText et_rbl;

    SQLiteDatabase db;
    ArrayList<Integer> rbls;
    ArrayList<TransportUnit> transportUnits;
    WienerLinienDBHelper wienerLinienDBHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        switch (getResources().getConfiguration().orientation) {

            case Configuration.ORIENTATION_PORTRAIT:
                setContentView(R.layout.activity_main2);
                break;

            case Configuration.ORIENTATION_LANDSCAPE:
                setContentView(R.layout.activity_main2_landscape);
                break;
        }

        ButterKnife.bind(this);
        wienerLinienDBHelper = WienerLinienDBHelper.getsInstance(getApplicationContext());
        db = wienerLinienDBHelper.getWritableDatabase();


    }

    static class GetRealtimeTask extends AsyncTask<URL, Void, String[]> {

        private final WeakReference<Main2Activity> mainActivityWeakReference;

        GetRealtimeTask(Main2Activity context){
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


    /**
     * Die RBL Nummern müssen in einem ganz bestimmten Format eingegeben werden, damit das Programm korrekt funktioniert
     * als guter Entwickler muss man jedoch alle Fälle von fehlerhafter Eingabe abdecken um einen Programmabsturz zu verhindern
     * evtl. Übung:
     * welche Arten von fehlerhafter Eingabe fallen euch ein, die einem User passieren könnten?
     */


    @OnClick(R.id.btn_go)
    public void submit() {

        String userInput = et_rbl.getText().toString();
        String[] nums = userInput.split(",");
        String empty = "";
        if (nums.length == 1) {
            if (nums[0].trim().equals(empty)) {

                Toast.makeText(getApplicationContext(), "Please enter number!", Toast.LENGTH_SHORT).show();

                et_rbl.getText().clear();

            } else if (!wienerLinienDBHelper.rblExists(nums[0])) {

                Toast.makeText(getApplicationContext(), "Faulty RBL!", Toast.LENGTH_SHORT).show();
                et_rbl.getText().clear();

            } else {
                rbls = new ArrayList<>();
                rbls.add(Integer.parseInt(nums[0].trim()));
                new GetRealtimeTask(Main2Activity.this).execute(WienerLinenApi.buildWienerLinienMonitorUrl(rbls));

            }

        } else if (nums.length > 1) {
            rbls = new ArrayList<>();
            Toast faultyToast = null;
            for (String num : nums) {
                if (num.trim().equals(empty) || !wienerLinienDBHelper.rblExists(num)) {

                    faultyToast = Toast.makeText(getApplicationContext(), "Faulty input, showing existing stations!", Toast.LENGTH_LONG);

                    et_rbl.getText().clear();

                } else {

                    rbls.add(Integer.parseInt(num.trim()));

                }
            }
            if (rbls.size() > 0) {
                new GetRealtimeTask(Main2Activity.this).execute(WienerLinenApi.buildWienerLinienMonitorUrl(rbls));
                if (faultyToast != null) faultyToast.show();
            }

        }else {
            Toast.makeText(getApplicationContext(), "Please enter number!", Toast.LENGTH_SHORT).show();

        }

        // id = Integer.parseInt(userInput);
        //tv_test.setText(wienerLinienDBHelper.getStationName(id));

       /* String userInput = actv_search.getText().toString();

        new GetRealtimeTask(MainActivity.this).execute(WienerLinenApi.buildWienerLinienMonitorUrl(rbls));*/

    }

}
