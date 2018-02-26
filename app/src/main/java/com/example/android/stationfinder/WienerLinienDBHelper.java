package com.example.android.stationfinder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;


public class WienerLinienDBHelper extends SQLiteOpenHelper {

    private static WienerLinienDBHelper sInstance;
    private static final String DATABASE_NAME = "wienerlinien.db";
    private static final int DATABASE_VERSION = 15;
    private static final String TABLE_STEIGE = "Steige";
    private static final String COL_RBL = "RBL";
    private static final String COL_LAT = "Latitude";
    private static final String COL_LON = "Longitude";
    private static final String COL_HALTESTELLEN_ID = "Haltestellen_ID";
    private static final String TABLE_HALTESTELLEN = "Haltestellen";
    private static final String COL_NAME = "Name";
    private final String CREATE_STEIGE_TABLE = "CREATE TABLE " + TABLE_STEIGE
            + " (" + COL_RBL + " INTEGER, " + COL_LAT + " DOUBLE NOT NULL, "
            + COL_LON + " DOUBLE NOT NULL, "
            + COL_HALTESTELLEN_ID + " INTEGER NOT NULL, FOREIGN KEY (" + COL_HALTESTELLEN_ID + ") REFERENCES "
            + TABLE_HALTESTELLEN + " (" + COL_HALTESTELLEN_ID +")  )";
    private final String CREATE_HALTESTELLEN_TABLE = "CREATE TABLE " + TABLE_HALTESTELLEN
            + " (" + COL_HALTESTELLEN_ID + " INTEGER PRIMARY KEY NOT NULL, "
            + COL_NAME + " TEXT)";

    static synchronized WienerLinienDBHelper getsInstance(Context context){
        if(sInstance == null){
            sInstance = new WienerLinienDBHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private WienerLinienDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_HALTESTELLEN_TABLE);
        db.execSQL(CREATE_STEIGE_TABLE);

    new GetStationInformationTask(WienerLinienDBHelper.this).execute(WienerLinenApi.buildDatabaseRequestUrl());

    }

    static class GetStationInformationTask extends AsyncTask<URL, Void, String[]> {

        WeakReference<WienerLinienDBHelper> wienerLinienDBHelperWeakReference;

        GetStationInformationTask(WienerLinienDBHelper context){
            wienerLinienDBHelperWeakReference = new WeakReference<>(context);
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
                JSONObject stationJSON = new JSONObject(response);
                JSONArray rbls = stationJSON.getJSONArray("rbls");
                JSONArray stations = stationJSON.getJSONArray("stations");

                for (int i = 0; i < stations.length(); i++) {
                    int tempId = Integer.parseInt(stations.getJSONObject(i).getString("HALTESTELLEN_ID"));
                    String tempName = stations.getJSONObject(i).getString("NAME");
                   wienerLinienDBHelperWeakReference.get().addStation(tempId, tempName);
                }

                for (int j = 0; j < rbls.length(); j++) {
                    int rbl = Integer.parseInt(rbls.getJSONObject(j).getString("RBL_NUMMER"));
                    double lat = Double.parseDouble(rbls.getJSONObject(j).getString("STEIG_WGS84_LAT"));
                    double lon = Double.parseDouble(rbls.getJSONObject(j).getString("STEIG_WGS84_LON"));
                    int tempId = Integer.parseInt(rbls.getJSONObject(j).getString("FK_HALTESTELLEN_ID"));

                   wienerLinienDBHelperWeakReference.get().addRbl(rbl, lat, lon, tempId);
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }

            return new String[0];
        }

    }
    private void addRbl(int rbl, double lat, double lon, int stationId){

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try{
            ContentValues vals = new ContentValues();
            vals.put(COL_RBL, rbl);
            vals.put(COL_LAT, lat);
            vals.put(COL_LON, lon);
            vals.put(COL_HALTESTELLEN_ID, stationId);


            db.insert(TABLE_STEIGE  , null, vals);
            db.setTransactionSuccessful();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            db.endTransaction();
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion != newVersion){
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_HALTESTELLEN);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_STEIGE);

            reCreateTables(db);
        }


    }

    private void reCreateTables(SQLiteDatabase sqLiteDatabase){

        sqLiteDatabase.execSQL(CREATE_HALTESTELLEN_TABLE);
        sqLiteDatabase.execSQL(CREATE_STEIGE_TABLE);
        new GetStationInformationTask(WienerLinienDBHelper.this).execute(WienerLinenApi.buildDatabaseRequestUrl());


    }

    private void addStation(int stationId, String name){

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try{
            ContentValues vals = new ContentValues();
            vals.put(COL_HALTESTELLEN_ID, stationId);
            vals.put(COL_NAME, name);

            db.insert(TABLE_HALTESTELLEN, null, vals);
            db.setTransactionSuccessful();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            db.endTransaction();
        }

    }

   /* String getStationName(int id){

        SQLiteDatabase db = getReadableDatabase();
        String stringId = Integer.toString(id);
        String SQL = "SELECT " + COL_NAME + " FROM " + TABLE_HALTESTELLEN + " WHERE " + COL_HALTESTELLEN_ID + " = ? ";
        Cursor res = db.rawQuery(SQL, new String[]{stringId});

        if(res.getCount() == 0){
            return null;
        }else {
            res.moveToFirst();


        }

        return res.getString(0);
     }*/

    ArrayList<String> getAllStationNames(){
        SQLiteDatabase db = getReadableDatabase();
        ArrayList<String> list = new ArrayList<>();
        Cursor res = db.rawQuery("SELECT " + COL_NAME + " FROM " + TABLE_HALTESTELLEN,new String[]{});
        if(res.getCount() == 0){
            res.close();
            return null;

        }else{

                if(res.moveToFirst()) {
                    do {

                        list.add(res.getString(0));

                    }while (res.moveToNext());
                }
            res.close();

        }return list;
     }

    ArrayList<Integer> getRBLs(int id) {
        SQLiteDatabase db = getReadableDatabase();
        String stringId = Integer.toString(id);
        ArrayList<Integer> rbls = new ArrayList<>();
        String sql = "SELECT " + COL_RBL + " FROM "  + TABLE_STEIGE + " WHERE " + COL_HALTESTELLEN_ID + "  = ?";
        Cursor res = db.rawQuery(sql, new String[]{stringId});

       if(res.getCount() == 0){
           res.close();
           return null;
       }else {
           if(res.moveToFirst()){
               do {
                   rbls.add(res.getInt(0));
               }while (res.moveToNext());
           }
           res.close();
       }return rbls;



    }

    int getStationId(String name){

        SQLiteDatabase db = getReadableDatabase();
        String SQL = "SELECT " + COL_HALTESTELLEN_ID + " FROM " + TABLE_HALTESTELLEN + " WHERE " + COL_NAME + " = ?";
        int r;
        Cursor res = db.rawQuery(SQL, new String[]{name});
        if (res.getCount() == 0) {
            r = 0;
            res.close();
            return 0;
        }
        else {
            res.moveToFirst();
            r = res.getInt(0);
            res.close();
        }
        return r;

    }

    boolean rblExists(String rbl){

        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT " + COL_HALTESTELLEN_ID + " FROM " + TABLE_STEIGE + " WHERE " + COL_RBL + " = ?";
        Cursor res = db.rawQuery(sql, new String[]{ rbl});
        boolean rBool = res.moveToFirst();
        res.close();
        return rBool;

    }
}




