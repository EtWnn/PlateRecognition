package com.sspr.Recognition;

import android.graphics.RectF;

/**
 * Class to represent the detection of an object on an image (ex a plate)
 */
public class Detection {

    /**
     * title of the detection
     */
    private String title;

    /**
     * score given by the model
     */
    private float confidence;

    /**
     * Rectangle box around the object on the image
     */
    private RectF location;

    public Detection(String title, float confidence, RectF location){
        this.title = title;
        this.confidence = confidence;
        this.location = location;
    }

    public Detection(String title, float confidence){
        this.title = title;
        this.confidence = confidence;
    }

    public String getTitle() {
        return title;
    }

    public Float getConfidence() {
        return confidence;
    }

    public RectF getLocation() {
        return new RectF(location);
    }

    /**
     * rescale the rectangle coordinates if the model and the image have not the same scale
     * @param scaleWidth
     * @param scaleHeight
     */
    public void rescaleRect(final float scaleWidth, final float scaleHeight){
        if (scaleHeight * scaleWidth > 0){
            location.top /= scaleHeight;
            location.bottom /= scaleHeight;
            location.left /= scaleWidth;
            location.right /= scaleWidth;
        }
    }
}
