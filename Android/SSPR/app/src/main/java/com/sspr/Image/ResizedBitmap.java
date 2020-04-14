package com.sspr.Image;

import android.graphics.Bitmap;

/**
 * a data container for resized image
 */
public class ResizedBitmap{
    public final Bitmap bitmap;
    public final float scaleWidth;
    public final float scaleHeight;

    public ResizedBitmap(Bitmap bitmap, float scaleWidth, float scaleHeight){
        this.bitmap = bitmap;
        this.scaleHeight = scaleHeight;
        this.scaleWidth = scaleWidth;
    }
}
