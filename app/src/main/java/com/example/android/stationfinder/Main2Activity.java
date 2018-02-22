package com.example.android.stationfinder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

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
        setContentView(R.layout.activity_main2);

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



    @OnClick(R.id.btn_go)
    public void submit() {

        String userInput = et_rbl.getText().toString();
        String[] nums = userInput.split(",");
        String empty = "";
        if(nums.length != 0){
            rbls = new ArrayList<>();
            for (String num : nums) {
                if (num.trim().equals(empty)) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(Main2Activity.this);
                    builder.setMessage("Please enter a number!")
                            .setNeutralButton("OK", null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    et_rbl.getText().clear();

                }
                else if(!wienerLinienDBHelper.rblExists(num)){
                    AlertDialog.Builder builder = new AlertDialog.Builder(Main2Activity.this);
                    builder.setMessage("No such RBL!")
                            .setNeutralButton("OK", null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

                 else {

                    rbls.add(Integer.parseInt(num.trim()));
                    new GetRealtimeTask(Main2Activity.this).execute(WienerLinenApi.buildWienerLinienMonitorUrl(rbls));
                }
            }

        }else {
            AlertDialog.Builder builder = new AlertDialog.Builder(Main2Activity.this);
            builder.setMessage("Please enter a number!")
                    .setNeutralButton("OK", null);
            AlertDialog dialog = builder.create();
            dialog.show();
        }

       // id = Integer.parseInt(userInput);
        //tv_test.setText(wienerLinienDBHelper.getStationName(id));

       /* String userInput = actv_search.getText().toString();

        new GetRealtimeTask(MainActivity.this).execute(WienerLinenApi.buildWienerLinienMonitorUrl(rbls));*/

    }

}
