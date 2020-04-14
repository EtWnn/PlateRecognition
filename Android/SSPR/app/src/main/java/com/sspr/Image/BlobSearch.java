package com.sspr.Image;

import android.graphics.Rect;
import android.util.Pair;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This class is made to search all white pixel groups (Blob) within a Mat
 */
public class BlobSearch {

    private ArrayList<Blob>  mBlobs;
    private Mat mMat;
    private int mWidth;
    private int mHeight;

    static private int[] xDelta = {-1, 0, 1, 1, 1, 0, -1, -1};
    static private int[] yDelta = {-1, -1, -1, 0, 1, 1, 1, 0};


    public BlobSearch(Mat mat){
        mMat = new Mat();
        mat.copyTo(mMat);

        mWidth = mMat.width();
        mHeight = mMat.height();

        mBlobs = new ArrayList<>();
    }

    /**
     * perform the blob search
     */
    public void search(){
        for (int j = 0; j < mWidth; j++){
            for (int i = 0; i < mHeight; i++){
                if (mMat.get(i, j)[0] > 0){
                    Blob blob = new Blob(i,j);
                    BFS(i, j, blob);
                    mBlobs.add(blob);
                }
            }
        }
    }

    /**
     * breadth first search algorithm that compute the dimensions of a blob by going through
     * all its pixels. Its also modify the attribute mMat.
     * @param i starting position on the first dimension
     * @param j starting position on the second dimension
     * @param blob instance of the Blob class
     */
    private void BFS(int i, int j, Blob blob){
        mMat.put(i, j, 0);
        Queue<Pair<Integer, Integer>> toVisit = new LinkedList<>();
        toVisit.add(new Pair<>(i,j));

        Iterator iterator = toVisit.iterator();
        while (iterator.hasNext()){
            Pair<Integer, Integer> pixel = toVisit.remove();

            for(int k = 0; k < 8; k++){
                int new_i = pixel.first + xDelta[k];
                int new_j = pixel.second + yDelta[k];

                if( new_i >= 0 && new_j >= 0 && new_i < mHeight && new_j < mWidth
                        && mMat.get(new_i, new_j)[0] > 0){
                    mMat.put(new_i, new_j, 0);
                    blob.updateCount();
                    blob.updateLocation(new_i, new_j);
                    toVisit.add(new Pair<>(new_i, new_j));
                }
            }
        }
    }

    /**
     * reject a Blob if its dimensions are not expected compare to the dimensions of the original
     * image
     * @param blobLocation bounding box of the blob
     * @return if the blob is to be kept
     */
    private Boolean isBlobLegit(Rect blobLocation){
        return blobLocation.width() > 0.005 * mWidth && blobLocation.width() < 0.5 * mWidth
                && blobLocation.height() > 0.3 * mHeight;
    }

    public ArrayList<Blob> getBlobs(){
        return mBlobs;
    }
}
