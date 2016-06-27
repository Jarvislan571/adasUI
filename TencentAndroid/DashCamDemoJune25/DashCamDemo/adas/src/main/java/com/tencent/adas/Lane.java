package com.tencent.adas;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by cejachen on 16/5/22.
 */
public class Lane implements Parcelable {
    public int x1;
    public int y1;
    public int x2;
    public int y2;
    public int departure;

    //  0 keep the line
    //  1 on left line
    //  2 on rigth line
    public static final int KEEP_THE_LINE = 0;
    public static final int ON_LEFT_LINE = 1;
    public static final int ON_RIGHT_LINE = 2;

    public Lane() {

    }

    public Lane(int x1, int y1,
                int x2, int y2,
                int departure) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;

        this.departure = departure;
    }

    public Lane(Parcel parcel) {
        x1 = parcel.readInt();
        y1 = parcel.readInt();
        x2 = parcel.readInt();
        y2 = parcel.readInt();
        departure = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        // TODO Auto-generated method stub
        parcel.writeInt(this.x1);
        parcel.writeInt(this.y1);
        parcel.writeInt(this.x2);
        parcel.writeInt(this.y2);
        parcel.writeInt(this.departure);
    }

    public static final Parcelable.Creator<Lane> CREATOR = new Parcelable.Creator<Lane>() {

        @Override
        public Lane createFromParcel(Parcel parcel) {
            // TODO Auto-generated method stub
            return new Lane(parcel);
        }

        @Override
        public Lane[] newArray(int size) {
            // TODO Auto-generated method stub
            return new Lane[size];
        }

    };

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("x1: ").append(this.x1).append(" y1: ").append(this.y1)
                .append(" x2: ").append(this.x2)
                .append(" y2: ").append(this.y2)
                .append(" departure: ").append(this.departure);
        return sb.toString();
    }
}
