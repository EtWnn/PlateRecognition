package com.sspr.Image;

import android.graphics.Rect;

/**
 * class to represent a group of adjacent non null pixels on an image
 */
public class Blob {
    private Rect mLocation;
    private int mCount;


    public Blob(int i, int j){
        mLocation = new Rect(j, i, j+1, i+1);
        mCount = 1;
    }

    public  void updateCount(){
        mCount ++;
    }

    public void updateLocation(int i, int j){
        if (j < mLocation.left) {mLocation.left = j;}
        else if (j >= mLocation.right) {mLocation.right = j + 1;}
        if (i < mLocation.top) {mLocation.top = i;}
        else if (i >= mLocation.bottom) {mLocation.bottom = i + 1;}
    }

    public double getDensity(){
        return (double) mCount / (mLocation.height() * mLocation.width());
    }

    public  Rect getLocation(){
        return mLocation;
    }
}
