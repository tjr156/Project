package com.gps.kms.safetyhome;

/**
 * Created by Administrator on 2017-12-04.
 */

public class MarkerItem {

    double lat;
    double lon;
    int title;

    public MarkerItem(double lat, double lon, int title) {
        this.lat = lat;
        this.lon = lon;
        this.title = title;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public int getTitle() {
        return title;
    }

    public void setTitle(int title) {
        this.title = title;
    }
}