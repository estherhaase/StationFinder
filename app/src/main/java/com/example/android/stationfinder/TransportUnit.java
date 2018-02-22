package com.example.android.stationfinder;


import android.os.Parcel;
import android.os.Parcelable;


public class TransportUnit implements Parcelable {

    private final String lineName;
    private final String lineDirection;
    private final String[] depTime1;
    private final double lat;
    private final double lon;
    private final boolean barrierFree;

    TransportUnit(String lineName, String lineDirection, String[] depTime1, double lat, double lon, boolean barrierFree) {
        this.lineName = lineName;
        this.lineDirection = lineDirection;
        this.depTime1 = depTime1;
        this.lat = lat;
        this.lon = lon;
        this.barrierFree = barrierFree;

    }

    private TransportUnit(Parcel in){
        this.lineName = in.readString();
        this.lineDirection = in.readString();
        this.depTime1 = in.createStringArray();
        this.lat = in.readDouble();
        this.lon = in.readDouble();
        this.barrierFree = in.readByte() !=0;

    }

    String getLineName() {
        return lineName;
    }

    String getLineDirection() {
        return lineDirection;
    }



   double getLat() {
        return lat;
    }

    String[] getDepTime1() {
        return depTime1;
    }

    double getLon() {
        return lon;
    }

    public static final Creator<TransportUnit> CREATOR = new Creator<TransportUnit>() {
        @Override
        public TransportUnit createFromParcel(Parcel in) {
            return new TransportUnit(in);
        }

        @Override
        public TransportUnit[] newArray(int size) {
            return new TransportUnit[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(lineName);
        dest.writeString(lineDirection);
        dest.writeStringArray(depTime1);
        dest.writeDouble(lat);
        dest.writeDouble(lon);
        dest.writeByte((byte) (barrierFree ? 1 : 0));

    }
}
