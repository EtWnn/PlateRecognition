package com.sspr.Recognition;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.ImageView;

import com.sspr.Camera.AutoFitTextureView;
import com.sspr.Camera.Results;

public class TaskManager {

    /*
     * Status indicators
     */
    static final int WAITING_FOR_TASK = 0;
    static final int TASK_LAUNCHED = 1;
    static final int TASK_RUNNING = 2;

    public int callCount = 0;

    private int mState = WAITING_FOR_TASK;

    private int mFailCount = 0;

    //the imageView to which is attached the result of the decoding
    private ImageView mImageView;

    // the results instance to which pass the result of the detection
    private Results mResults;

    public AutoFitTextureView mAutoFitTextureView;

    private DetectionModel mDetectionModel;

    private ClassificationModel mClassificationModel;

    // Defines a Handler object that's attached to the UI thread
    private Handler handler = new Handler(Looper.getMainLooper()) {
        /*
         * handleMessage() defines the operations to perform when
         * the Handler receives a new Message to process.
         */
        @Override
        public void handleMessage(Message inputMessage) {
            // Gets the image task from the incoming Message object.
            Task task = (Task) inputMessage.obj;
            switch (inputMessage.what) {
                case Task.TASK_RUNNING:
                    mState = TASK_RUNNING;
                    break;
                case Task.TASK_FAILED:
                    mFailCount ++;
                    if (mFailCount > 5){
                        mImageView.setImageBitmap(null);
                        mResults.reset();
                        mResults.pushChange();
                    }
                    mState = WAITING_FOR_TASK;
                    mResults.reset();
                    relaunchTask();
                    break;
                case Task.TASK_COMPLETE:
                    if ( task.getConfidence() > 0.3){
                        mFailCount = 0;
                        mImageView.setImageBitmap(task.getInputsBitmap());
                        mResults.setRectangle(task.getRectangle());
                        mResults.setResult(task.getResults());
                        mResults.pushChange();
                    }
                    mState = WAITING_FOR_TASK;
                    relaunchTask();
                    break;
                default:
                    //Pass along other messages from the UI
                    super.handleMessage(inputMessage);
            }

        }

    };

    public Message constructMessage(int what, Object obj){
        return handler.obtainMessage(what, obj);
    }

    public TaskManager(Results results, AutoFitTextureView view,
                       AssetManager assets, String detectionModelFile, String detectionLabelFile,
                       int detectionImageSize, Boolean isDetectionModelQuantized,
                       String characterModelFile, String characterLabelFile,
                       int characterImageSize, int characterImagePadding,
                       Boolean isCharacterModelQuantised){
        mResults = results;
        mAutoFitTextureView = view;

        try {
            mDetectionModel = DetectionModel.create(assets, detectionModelFile, detectionLabelFile,
                    detectionImageSize, isDetectionModelQuantized);
            mClassificationModel = ClassificationModel.create(assets, characterModelFile,
                    characterLabelFile, characterImageSize + 2*characterImagePadding,
                    isCharacterModelQuantised);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void relaunchTask(){
        setTask(mAutoFitTextureView.getBitmap());
    }

    public void setTask(Image image){
        if (image != null){
            switch (mState){
                case WAITING_FOR_TASK:
                    mState = TASK_LAUNCHED;
                    Task task = new Task(this, image, mDetectionModel,
                            mClassificationModel);
                    new Thread(task).start();
                    callCount ++;
                    break;
                default:
                    break;
            }
        }
    }

    public void setPreview(ImageView rectangleView ){
        mImageView = rectangleView;
    }


    public void setTask(Bitmap image){
        switch (mState){
            case WAITING_FOR_TASK:
                mState = TASK_LAUNCHED;
                Task task = new Task(this, image, mDetectionModel,
                        mClassificationModel);
                new Thread(task).start();
                callCount++;
                break;
            default:
                break;
        }
    }



}
