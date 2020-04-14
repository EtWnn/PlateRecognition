package com.sspr.Recognition;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.os.Message;

import com.sspr.Image.Blob;
import com.sspr.Image.ResizedBitmap;

import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.sspr.Image.ImageUtils.binarizeMat;
import static com.sspr.Image.ImageUtils.cropByAxis;
import static com.sspr.Image.ImageUtils.drawHeaderFrames;
import static com.sspr.Image.ImageUtils.equalizeHistogram;
import static com.sspr.Image.ImageUtils.frameBitmap;
import static com.sspr.Image.ImageUtils.getCharRect;
import static com.sspr.Image.ImageUtils.getRegion;
import static com.sspr.Image.ImageUtils.getResizedBitmap;
import static com.sspr.Image.ImageUtils.hasWhiteBackground;
import static com.sspr.Image.ImageUtils.reverseColor;
import static com.sspr.Image.ImageUtils.scaleDown;
import static com.sspr.Image.ImageUtils.stretchConstrast;
import static com.sspr.Image.ImageUtils.toGrayScale;
import static com.sspr.MainActivity.CHARACTER_IMAGE_PADDING;
import static com.sspr.MainActivity.CHARACTER_IMAGE_SIZE;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static org.opencv.android.Utils.bitmapToMat;
import static org.opencv.android.Utils.matToBitmap;


public class Task implements Runnable{

    static final int TASK_FAILED = -1;
    static final int TASK_RUNNING = 0;
    static final int TASK_COMPLETE = 1;


    //image received by an ImageReader
    Image mImage = null;

    private Detection mDetection;
    private ArrayList<Detection> mCharacterDetections;

    // The decoded image
    private Bitmap mBitmap = null;

    // The cropped region were something has been detected
    private Bitmap mDetectedBitmap = null;

    // concatenation of the bitmap above and the inputs of the charater model
    private Bitmap mInputsBitmap = null;

    // models
    private DetectionModel mDetectionModel;
    private ClassificationModel mClassificationModel;

    private int mInputSize;

    //detection results
    private String mResults = "";

    // Gets a handle to the object that creates the thread pools
    private TaskManager mTaskManager;

    public Task(TaskManager manager, Image image, DetectionModel detectionModel,
                ClassificationModel classificationModel){
        mImage = image;
        mTaskManager = manager;
        mDetectionModel = detectionModel;
        mClassificationModel = classificationModel;
    }

    public Task(TaskManager manager, Bitmap bitmap, DetectionModel detectionModel,
                ClassificationModel classificationModel){
        mBitmap = bitmap;
        mTaskManager = manager;
        mDetectionModel = detectionModel;
        mInputSize = mDetectionModel.getInputSize();
        mClassificationModel = classificationModel;
    }

    // Runs the code for this task
    public void run() {
        Message stateMessage;
        try{
            stateMessage = mTaskManager.constructMessage(TASK_RUNNING, this);
            stateMessage.sendToTarget();

            // Tries to decode the image buffer if an encoded image was passed
            if (mBitmap == null) {
                Image.Plane[] planes = mImage.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
            }

            //detection
            detection();

            //take the detected region
            mDetectedBitmap = getRegion(mBitmap, mDetection.getLocation());

            //apply transformation
            recognizeCharacters();

            stateMessage = mTaskManager.constructMessage(TASK_COMPLETE, this);
            stateMessage.sendToTarget();

        }catch (Exception e){
            stateMessage = mTaskManager.constructMessage(TASK_FAILED, this);
            stateMessage.sendToTarget();
        }

    }

    private void detection(){
        ResizedBitmap resizedBitMap = getResizedBitmap(mBitmap, mInputSize, mInputSize);
        final List<Detection> results = mDetectionModel.recognizeImage(resizedBitMap.bitmap);

        if (results.size() > 0){
            mDetection = results.get(0);
            mDetection.rescaleRect(resizedBitMap.scaleWidth, resizedBitMap.scaleHeight);
            mResults = mDetection.getTitle() + " " + mDetection.getConfidence();
        }

    }

    private void recognizeCharacters(){

        mDetectedBitmap = scaleDown(mDetectedBitmap, 300, 40);

        Mat mat = new Mat();
        bitmapToMat(mDetectedBitmap, mat);

        toGrayScale(mat);
        stretchConstrast(mat);

        Boolean whiteBackGround = hasWhiteBackground(mat);

        equalizeHistogram(mat);
        binarizeMat(mat, 41, 20, whiteBackGround);

        double meanMat = Core.mean(mat).val[0];
        if (meanMat > 128){
            reverseColor(mat);
        }

        cropByAxis(mat, 0, 1.9f);
        cropByAxis(mat, 1, 1.9f);
        //resize the bitmap according to the cropped Mat
        mDetectedBitmap = getResizedBitmap(mDetectedBitmap, mat.width(), mat.height()).bitmap;
        matToBitmap(mat, mDetectedBitmap);

        ArrayList<Blob> blobs = getCharRect(mat);
        ArrayList<Rect> charLocations = selectCharLocations(blobs);
        mCharacterDetections = new ArrayList<>();

        mResults = mDetection.getTitle() + " :";
        ArrayList<Bitmap> frames = new ArrayList<>();
        for(Rect charLocation : charLocations){
            Bitmap characterBitmap = frameBitmap(mDetectedBitmap, charLocation,
                    CHARACTER_IMAGE_SIZE, CHARACTER_IMAGE_PADDING);
            frames.add(characterBitmap);
            Detection detection = mClassificationModel.recognizeImage(characterBitmap);
            mCharacterDetections.add(detection);
            mResults = mResults + detection.getTitle();
        }

        drawCharLocations(charLocations);
        mInputsBitmap = drawHeaderFrames(mDetectedBitmap, frames);
        mat.release();
    }

    /**
     * draw the character location on detected image
     * @param charLocations List of the bounding box of the characters
     */
    private void drawCharLocations(ArrayList<Rect> charLocations){
        Paint paint = new Paint();
        paint.setColor(Color.argb(255, 255, 0 , 0));
        paint.setStrokeWidth(4);
        paint.setStyle(Paint.Style.STROKE);

        Canvas canvas = new Canvas(mDetectedBitmap);
        for(Rect rect : charLocations){
            canvas.drawRect(rect, paint);
        }

        canvas.drawBitmap(mDetectedBitmap, null,
                new Rect(0,0, mDetectedBitmap.getWidth(), mDetectedBitmap.getHeight()),
                paint);
    }

    /**
     * from a selection of blobs, select those who are supposed to be characters
     * @param blobs arraylist of blobs
     * @return arraylist of characters locations
     */
    private ArrayList<Rect> selectCharLocations(ArrayList<Blob> blobs){
        ArrayList<Rect> charLocations = new ArrayList<>();
        int sourceWidth = mDetectedBitmap.getWidth();
        int sourceHeight = mDetectedBitmap.getHeight();
        int iBlob = 0;
        int nBlobs = blobs.size();

        //discard those too small , too wide or too thin
        while(iBlob < nBlobs){
            Blob blob = blobs.get(iBlob);
            Rect location =blob.getLocation();
            if (location.height() > 0.3 * sourceHeight && location.width() > 0.005 * sourceWidth
                    && location.width() < 0.3 * sourceWidth && blob.getDensity() < 0.95){
                charLocations.add(location);
                iBlob++;
            }
            else{
                blobs.remove(iBlob);
                nBlobs --;
            }
        }

        //discard heights too far from the mean location heights
        double meanH = 0;
        double stdH = 0;
        int nLocations = charLocations.size();
        for(Rect rect : charLocations){
            int rectHeight = rect.height();
            meanH += rectHeight;
            stdH += rectHeight * rectHeight;
        }
        meanH /= nLocations;
        stdH = sqrt(stdH / nLocations - meanH * meanH);
        stdH = max(0.1 * meanH, stdH);

        int iLocations = 0;
        while (iLocations < nLocations){
            int locationHeight = charLocations.get(iLocations).height();
            if (abs(meanH - locationHeight) > stdH){
                nLocations--;
                charLocations.remove(iLocations);
            }
            else{
                iLocations++;
            }
        }


        return charLocations;
    }


    public Bitmap getDetectedImage(){
        return mDetectedBitmap;
    }

    public Bitmap getInputsBitmap(){
        return mInputsBitmap;
    }

    public String getResults() {return mResults;}

    public RectF getRectangle(){
        return mDetection.getLocation();
    }

    public float getConfidence(){
        return mDetection.getConfidence();
    }


}
