package com.example.mooddetectorapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.camera.view.PreviewView;

import com.google.mlkit.vision.face.Face;

import java.util.List;

public class FaceOverlayView extends View {
    private List<Face> faces;
    private List<String> emotions;
    private List<float[]> probabilities;
    private Paint paint;
    private PreviewView previewView;

    private static final String TAG = "FaceOverlayView";

    public FaceOverlayView(Context context) {
        super(context);
        init();
    }

    public FaceOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FaceOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setTextSize(40f);
    }

    public void setFaces(List<Face> faces, List<String> emotions, List<float[]> probabilities, PreviewView previewView) {
        this.faces = faces;
        this.emotions = emotions;
        this.probabilities = probabilities;
        this.previewView = previewView;
        invalidate(); // Request a redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (faces == null || emotions == null || probabilities == null || previewView == null) {
            return;
        }

        // Get scaling factors to map face coordinates to the overlay view's coordinates
        float widthScaleFactor = getWidth() / (float) previewView.getWidth() * 2;
        float heightScaleFactor = getHeight() / (float) previewView.getHeight() * 2;

        Log.d(TAG, "Width scale factor: " + widthScaleFactor + ", Height scale factor: " + heightScaleFactor);

        for (int i = 0; i < faces.size(); i++) {
            Face face = faces.get(i);
            String emotion = emotions.get(i);
            float[] probs = probabilities.get(i);

            Rect boundingBox = face.getBoundingBox();

            // Scale bounding box coordinates
            @SuppressLint("DrawAllocation") Rect rect = new Rect(
                    (int) (boundingBox.left * widthScaleFactor),
                    (int) (boundingBox.top * heightScaleFactor * 1.5),
                    (int) (boundingBox.right * widthScaleFactor * 1.25),
                    (int) (boundingBox.bottom * heightScaleFactor * 1.6)
            );

            Log.d(TAG, "Original bounding box: " + boundingBox);
            Log.d(TAG, "Scaled bounding box: " + rect);

            // Draw bounding box
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(rect, paint);

            // Draw emotion label and probabilities
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);

            int x = rect.left;
            int y = rect.top - 10; // Adjust position to be above the box

            canvas.drawText("Detected emotion:"+emotion, x, y, paint);

            String[] emotionLabels = {"neutral", "happy", "surprised", "sad", "anger", "disgust", "fear", "contempt"};
            for (int j = 0; j < probs.length; j++) {
                y += paint.getTextSize();
                canvas.drawText(emotionLabels[j] + ": " + String.format("%.2f", probs[j]), x, y, paint);
            }
        }
    }
}
