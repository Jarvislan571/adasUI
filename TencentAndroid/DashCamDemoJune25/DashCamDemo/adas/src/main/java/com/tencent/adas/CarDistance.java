package com.tencent.adas;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by cejachen on 16/5/22.
 */
public class CarDistance implements Parcelable {
    public int x;
    public int y;
    public int width;
    public int height;
    public int distance;
    public float timeToCrash;

    public CarDistance() {

    }

    public CarDistance(int x, int y,
                       int width, int height,
                       int distance, float timeToCrash) {
        this.x  = x;
        this.y  = y;
        this.width  = width;
        this.height = height;
        this.distance   = distance;
        this.timeToCrash    = timeToCrash;
    }

    public CarDistance(Parcel parcel) {
        x = parcel.readInt();
        y = parcel.readInt();
        width = parcel.readInt();
        height = parcel.readInt();

        distance = parcel.readInt();
        timeToCrash = parcel.readFloat();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        // TODO Auto-generated method stub
        parcel.writeInt(this.x);
        parcel.writeInt(this.y);
        parcel.writeInt(this.width);
        parcel.writeInt(this.height);

        parcel.writeInt(this.distance);
        parcel.writeFloat(timeToCrash);
    }

    public static final Parcelable.Creator<CarDistance> CREATOR = new Parcelable.Creator<CarDistance>() {

        @Override
        public CarDistance createFromParcel(Parcel parcel) {
            // TODO Auto-generated method stub
            return new CarDistance(parcel);
        }

        @Override
        public CarDistance[] newArray(int size) {
            // TODO Auto-generated method stub
            return new CarDistance[size];
        }

    };

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("x: ").append(this.x).append(" y: ").append(this.y)
                .append(" width: ").append(this.width)
                .append(" height: ").append(this.height)
                .append(" distance: ").append(this.distance)
                .append(" timeToCrash: ").append(timeToCrash);
        return sb.toString();
    }
}
