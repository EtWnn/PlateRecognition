package com.sspr.Image;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;


import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static org.opencv.core.CvType.CV_32SC1;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY_INV;
import static org.opencv.imgproc.Imgproc.cvtColor;


/**
 * This class host static functions usefull for Mat and Bitmap manipulations
 */
public class ImageUtils {

    /**
     * create a Bitmap from a region of a bigger bitmap
     * @param bitmap source bitmap
     * @param rectangle region of the source to keep
     * @return bitmap corresponding to the rectangle region of the source
     */
    static public Bitmap getRegion(Bitmap bitmap, RectF rectangle){
        int left = max((int)rectangle.left, 0);
        int top = max((int)rectangle.top, 0);
        int bottom = min((int)rectangle.bottom, bitmap.getHeight());
        int right = min((int)rectangle.right, bitmap.getWidth());
        return Bitmap.createBitmap(bitmap, left, top, right - left,  bottom - top);
    }

    static public Bitmap getRegion(Bitmap bitmap, Rect rectangle){
        int width = min(rectangle.width(), bitmap.getWidth() - rectangle.left);
        int height = min(rectangle.height(), bitmap.getHeight() - rectangle.top);
        return Bitmap.createBitmap(bitmap, rectangle.left, rectangle.top, width,  height);
    }

    static public Bitmap scaleDown(Bitmap bitmap, int minWidth, int minHeight){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > minWidth && height > minHeight){
            float scale = max((float)minWidth / width, (float)minHeight / height);
            // CREATE A MATRIX FOR THE MANIPULATION
            Matrix matrix = new Matrix();
            // RESIZE THE BIT MAP
            matrix.postScale(scale, scale);

            // "RECREATE" THE NEW BITMAP
            return Bitmap.createBitmap(bitmap, 0, 0, width, height,
                    matrix, false);
        }

        return bitmap;

    }

    static public ResizedBitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        //bm.recycle();
        return new ResizedBitmap(resizedBitmap, scaleWidth, scaleHeight);
    }

    /**
     * given a CV_8UC1 mat, stretched the value histogram between 0 and 255.
     * @param mat
     */
    static public void stretchConstrast(Mat mat){
        Core.MinMaxLocResult minMaxLoc = Core.minMaxLoc(mat);
        double range = minMaxLoc.maxVal - minMaxLoc.minVal;
        int frameSize = mat.width() * mat.height();
        byte[] buffer = new byte[frameSize];
        mat.get(0, 0, buffer);

        for(int i = 0; i < frameSize; i++){
            byte temp = buffer[i];
            temp = (byte)(255 * (temp + 128 - minMaxLoc.minVal) / range - 128);
            buffer[i] = temp;
        }

        mat.put(0, 0, buffer);
    }

    static public Boolean hasWhiteBackground(Mat mat){
        Mat reduceMat = new Mat();
        Core.reduce(mat, reduceMat, 0, Core.REDUCE_SUM, CV_32SC1);
        int nElements = reduceMat.width();

        Mat derivative = new Mat(1, nElements-2, CV_32SC1);
        for(int i = 1; i < nElements - 1; i++){
            derivative.put(0,i, (mat.get(0,i + 1)[0] - mat.get(0,i-1)[0])/2);
        }
        int nDerivatives = nElements - 2;
        int firstSum = 0;
        for(int i = nDerivatives / 4; i < nDerivatives / 2; i++){
            firstSum += derivative.get(0,i)[0];
        }
        int secondSum = 0;
        for (int i = nDerivatives / 2; i < 3* nDerivatives / 4; i++){
            secondSum += derivative.get(0,i)[0];
        }

        return firstSum > 0 && secondSum < 0;
    }

    /**
     * crop an image along an axis according to a gaussian distribution af the value
     * @param mat
     * @param axis 0 -> crop the width of the matrix
     */
    static public void cropByAxis(Mat mat, int axis, float cropFactor){
        Mat reduceMat = new Mat();
        Core.reduce(mat, reduceMat, axis, Core.REDUCE_SUM, CV_32SC1);
        int nElements = reduceMat.width();
        if (axis == 1){
            nElements = reduceMat.height();
        }
        double mean = 0;
        double variance = 0;
        double sum = Core.sumElems(reduceMat).val[0];
        for (int k = 0; k < nElements; k++){
            int i = 0;
            int j = k;
            if (axis == 1){
                i = k;
                j = 0;
            }
            double temp = reduceMat.get(i,j)[0];
            mean += temp * k;
            variance += temp * k * k;
        }

        mean /= sum;
        variance /= sum;
        variance -= mean * mean;

        double stdev = sqrt(variance);

        int minIndex = (int) (mean - cropFactor * stdev);
        int maxIndex = (int) (mean + cropFactor * stdev);

        org.opencv.core.Rect croppedRect;
        if(axis == 0){
            minIndex = max(0, minIndex);
            maxIndex = min(maxIndex, mat.width());
            croppedRect = new org.opencv.core.Rect(minIndex, 0,
                    maxIndex - minIndex, mat.height());
        }
        else{
            minIndex = max(0, minIndex);
            maxIndex = min(maxIndex, mat.height());
            croppedRect = new org.opencv.core.Rect(0, minIndex,
                    mat.width(), maxIndex - minIndex);
        }

        Mat croppedMat = mat.submat(croppedRect);
        croppedMat.copyTo(mat);

    }

    static public void reverseColor(Mat mat){Core.bitwise_not(mat, mat);};

    /**
     * given a grayScaled mat, equalise the histogram
     * @param mat
     */
    static public void equalizeHistogram(Mat mat){
        Imgproc.equalizeHist(mat, mat);
    }

    /**
     * convert a CV mat to gray Scale
     * @param mat
     */
    static public void toGrayScale(Mat mat){
        cvtColor(mat, mat, COLOR_RGB2GRAY);
    }

    /**
     * given a CV_8UC1 Mat, use an adaptive threshold to binarise it
     * @param mat
     */
    static public void binarizeMat(Mat mat, int blockSize, int C, Boolean whiteBackGround){
        int thresh_type = THRESH_BINARY;
        if (whiteBackGround){
            thresh_type = THRESH_BINARY_INV;
        }

        Imgproc.adaptiveThreshold(mat, mat, 255, ADAPTIVE_THRESH_GAUSSIAN_C,
                thresh_type, blockSize, C);

    }

    /**
     * Calls the BlocSearch class to find the rectangle where the characters are
     * @param mat plate image
     * @return ArrayList of Blob instances
     */
    static public ArrayList<Blob> getCharRect(Mat mat){
        BlobSearch blobSearch = new BlobSearch(mat);
        blobSearch.search();
        return blobSearch.getBlobs();

    }


    /**
     * put a portion a source image in a framed bitmap (the added borders are black)
     * @param source bitmap source
     * @param box ROI on the source
     * @param size final size of the ROI
     * @param padding size of the added borders
     * @return
     */
    static public Bitmap frameBitmap(Bitmap source, android.graphics.Rect box,
                                     int size, int padding){
        int finalSize = size + 2*padding;
        Bitmap frame = Bitmap.createBitmap(finalSize, finalSize,
                source.getConfig());

        //draw the background
        Canvas canvas = new Canvas(frame);
        Paint paint = new Paint();
        paint.setColor(Color.argb(255, 0, 0 , 0));
        canvas.drawRect(0F, 0F, (float) finalSize, (float) finalSize, paint);

        //put a portion of the source image in the frame
        Bitmap portionBitmap = getRegion(source, box);
        portionBitmap = getResizedBitmap(portionBitmap, size, size).bitmap;
        canvas.drawBitmap(portionBitmap, padding, padding, null);

        return frame;

    }

    static public Bitmap drawHeaderFrames(Bitmap header, ArrayList<Bitmap> frames){
        int nFrames = frames.size();
        int headerHeight = header.getHeight();
        int headerWidth = header.getWidth();
        int inputSize = frames.get(0).getHeight();
        int numberPerRow = (int) headerWidth / inputSize;
        int numberPerColumn = (int)ceil((double)nFrames / numberPerRow);
        int headerOffset = 10;
        Bitmap resultBitmap = Bitmap.createBitmap(headerWidth,
                headerHeight + inputSize*numberPerColumn + headerOffset, header.getConfig());

        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(header, null, new Rect(0,0, headerWidth, headerHeight), null);

        int numFrame = 0;
        int i = 0;
        int j = 0;
        while (numFrame < nFrames){
            canvas.drawBitmap(frames.get(numFrame), null,
                    new Rect(j * inputSize, headerHeight + headerOffset + i*inputSize,
                            (j + 1) * inputSize, headerHeight + (i+1)*inputSize),
                    null);
            numFrame++;
            j++;
            if (j >= numberPerRow){
                j = 0;
                i += 1;
            }
        }

        return resultBitmap;
    }
}
