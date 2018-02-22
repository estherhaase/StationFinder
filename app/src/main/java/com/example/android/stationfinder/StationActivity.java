package com.example.android.stationfinder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

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

public class StationActivity extends Activity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    GoogleMap mGoogleMap;
    protected GoogleApiClient mGoogleApiClient;
    LocationManager mLocationManager;
    private Location stationLocation;
    private TransportUnit unit;
    private ArrayList<TransportUnit> transportUnitArrayList, transportUnitArrayList2;
    @BindView(R.id.btn_refresh)
    Button btn_refresh;
    ArrayList<Integer> rbls;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station);
        ButterKnife.bind(this);
        transportUnitArrayList2 = new ArrayList<>();
/*
        String[] nums = getIntent().getStringArrayExtra("RBL_ARRAY");
        new GetRealtimeTask().execute(WienerLinenApi.buildWienerLinienMonitorUrl(nums));*/
        initializeMap();
        buildGoogleApiClient();

        final Intent intent = getIntent();

        if(intent.hasExtra("RequestedTransportUnits")){

            try{
                transportUnitArrayList = intent.getExtras().getParcelableArrayList("RequestedTransportUnits");
              //  unit = (TransportUnit) intent.getExtras().getParcelableArrayList("RequestedTransportUnits").get(0);
            }catch (NullPointerException e){
                e.printStackTrace();
            }


        }

        if(intent.hasExtra("rbls")){
            try{
                rbls = intent.getIntegerArrayListExtra("rbls");
                int i = rbls.size();
            }catch (NullPointerException e){
                e.printStackTrace();
            }

        }




    }

    protected void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private void initializeMap(){
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.fragment_map);
        mapFragment.getMapAsync(this);
    }

    public void onConnected(@Nullable Bundle bundle) {

        checkPermission();
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(2000);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void goToLocation(double lat, double lng,  float zoom) {
        LatLng ll = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, zoom);
        mGoogleMap.moveCamera(update);

    }

    @Override
    public void onLocationChanged(Location location) {

       /* goToLocation(location.getLatitude(), location.getLongitude());
        mGoogleMap.clear();
        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())));*/
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if(mGoogleMap == null) {

            mGoogleMap = googleMap;
            goToLocation(transportUnitArrayList.get(0).getLat(), transportUnitArrayList.get(0).getLon(), 18);
            mGoogleMap.clear();
            for (int i = 0; i < transportUnitArrayList.size(); i++) {
                TransportUnit temp = transportUnitArrayList.get(i);
                mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(temp.getLat(), temp.getLon())).title(temp.getLineName() + " " + temp.getLineDirection()).snippet("Next Departures: " + temp.getDepTime1()[0] + " | " + temp.getDepTime1()[1]));

            }

        }

    }

    public void checkPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            System.out.println("Location Manger Problem");
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            System.out.println("Location Manager started!");
            mLocationManager = (LocationManager)
                    getSystemService(Context.LOCATION_SERVICE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @OnClick(R.id.btn_refresh)
    public void refresh(){

        new GetRealtimeTask(StationActivity.this).execute(WienerLinenApi.buildWienerLinienMonitorUrl(rbls));

    }

    static class GetRealtimeTask extends AsyncTask<URL, Void, String[]> {

        private final WeakReference<StationActivity> stationActivityWeakReference;

        GetRealtimeTask(StationActivity context){
            stationActivityWeakReference = new WeakReference<StationActivity>(context);
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
                stationActivityWeakReference.get().transportUnitArrayList2.clear();




                for (int i = 0; i < monitors.length(); i++) {

                    // rbls = transportUnitArrayList.get(i).getRbls();
                    String[] depTime = new String[2];
                    boolean barrierFree = false;
                    JSONObject currentMonitor = monitors.getJSONObject(i);
                    JSONArray coords = currentMonitor.getJSONObject("locationStop").getJSONObject("geometry").getJSONArray("coordinates");
                    Double lat = coords.getDouble(1);
                    Double lon = coords.getDouble(0);
                    String lineName = currentMonitor.getJSONArray("lines").getJSONObject(0).getString("name");
                    String lineDirection = currentMonitor.getJSONArray("lines").getJSONObject(0).getString("towards");

                    for(int j=0; j < currentMonitor.getJSONArray("lines").getJSONObject(0).getJSONObject("departures").getJSONArray("departure").length() && j < 2; j++){

                        depTime[j] = currentMonitor.getJSONArray("lines").getJSONObject(0).getJSONObject("departures").getJSONArray("departure").getJSONObject(j).getJSONObject("departureTime").getString("countdown");

                    }


                    TransportUnit temp = new TransportUnit(lineName, lineDirection, depTime, lat, lon, barrierFree);
                    stationActivityWeakReference.get().transportUnitArrayList2.add(temp);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }catch (NullPointerException e){
                e.printStackTrace();
            }
            return new String[0];
        }

        @Override
        protected void onPostExecute(String[] strings) {

            stationActivityWeakReference.get().onResume();

            //super.onPostExecute(strings);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mGoogleMap!= null){
            mGoogleMap.clear();
            for (int i= 0; i < transportUnitArrayList2.size();i++){
                TransportUnit temp = transportUnitArrayList2.get(i);
                mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(temp.getLat(), temp.getLon())).title(temp.getLineName() + " " + temp.getLineDirection()).snippet("Next Departures: " + temp.getDepTime1()[0] + " | " + temp.getDepTime1()[1]));

            }
        }
    }

    @Override
    protected void onStop(){
        mGoogleApiClient.disconnect();
        super.onStop();
    }


}
