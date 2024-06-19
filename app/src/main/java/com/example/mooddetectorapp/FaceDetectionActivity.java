package com.example.mooddetectorapp;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutionException;

public class FaceDetectionActivity extends AppCompatActivity {

    private ImageCapture imageCapture;
    private Interpreter tflite;
    private Handler handler;

    private static final String TAG = "FaceDetectionActivity";
    private static final int MODEL_INPUT_SIZE = 48;
    private static final int OUTPUT_CLASSES = 8;
    private static final long DETECTION_DELAY = 5000; // 5 seconds delay

    private boolean isProcessing = false;
    private boolean fromRewardTask = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detection);

        handler = new Handler();

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        fromRewardTask = getIntent().getBooleanExtra("fromRewardTask", false);

        // Load TensorFlow Lite model
        loadModel();

        // Start camera
        startCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }

    @Override
    public void onBackPressed() {
        // Call finish to close the activity
        finish();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                PreviewView previewView = findViewById(R.id.viewFinder);
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
                    if (!isProcessing) {
                        isProcessing = true;
                        processImageProxy(image);
                    } else {
                        image.close(); // Close image if currently processing
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImageProxy(ImageProxy image) {
        Image mediaImage = image.getImage();
        if (mediaImage != null) {
            Log.d(TAG, "Processing image with dimensions: " + mediaImage.getWidth() + "x" + mediaImage.getHeight() + " and format: " + mediaImage.getFormat());

            InputImage inputImage = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());

            Bitmap originalBitmap = mediaImageToBitmap(mediaImage);
            if (originalBitmap != null) {
                Log.d(TAG, "Successfully converted mediaImage to Bitmap");
                Bitmap rotatedBitmap = rotateBitmap(originalBitmap, 270); // Rotate image by 90 degrees to correct orientation
                detectFaces(image, inputImage, rotatedBitmap);
            } else {
                Log.e(TAG, "Failed to convert mediaImage to Bitmap");
                image.close();
                resetProcessing();
            }
        } else {
            Log.e(TAG, "mediaImage is null");
            image.close();
            resetProcessing();
        }
    }

    private void resetProcessing() {
        handler.postDelayed(() -> isProcessing = false, DETECTION_DELAY);
    }

    private Bitmap mediaImageToBitmap(Image mediaImage) {
        ByteBuffer yBuffer = mediaImage.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = mediaImage.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = mediaImage.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, mediaImage.getWidth(), mediaImage.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, mediaImage.getWidth(), mediaImage.getHeight()), 100, out);
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        if (bitmap == null) {
            Log.e(TAG, "rotateBitmap: Bitmap is null");
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void detectFaces(ImageProxy imageProxy, InputImage image, Bitmap originalBitmap) {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    imageProxy.close(); // Close the image proxy on success
                    if (faces.isEmpty()) {
                        Log.d(TAG, "No faces detected");
                    } else {
                        Log.d(TAG, "Faces detected: " + faces.size());
                        for (Face face : faces) {
                            detectEmotion(face, originalBitmap);
                        }
                    }
                    resetProcessing();
                })
                .addOnFailureListener(e -> {
                    imageProxy.close(); // Close the image proxy on failure
                    Log.e(TAG, "Face detection failed: " + e.getMessage(), e);
                    Toast.makeText(FaceDetectionActivity.this, "Face detection failed", Toast.LENGTH_SHORT).show();
                    resetProcessing();
                });
    }

    private float[] softmax(float[] logits) {
        float[] expValues = new float[logits.length];
        float sum = 0f;

        for (int i = 0; i < logits.length; i++) {
            expValues[i] = (float) Math.exp(logits[i]);
            sum += expValues[i];
        }

        for (int i = 0; i < logits.length; i++) {
            expValues[i] /= sum;
        }

        return expValues;
    }

    private void detectEmotion(Face face, Bitmap originalBitmap) {
        if (tflite == null) {
            Log.e(TAG, "TensorFlow Lite model not loaded.");
            return;
        }
        float[][][][] input = preprocessFaceImage(face, originalBitmap);
        if (input == null) {
            Log.e(TAG, "Preprocessed input is null.");
            return;
        }
        float[][] output = new float[1][OUTPUT_CLASSES]; // Adjust to match the model's output shape

        tflite.run(input, output);

        float[] probabilities = softmax(output[0]);

        for (int i = 0; i < probabilities.length; i++) {
            Log.d(TAG, "Probability for index " + i + ": " + probabilities[i]);
        }

        String emotion = mapOutputToEmotionLabel(probabilities);
        Log.d(TAG, "Detected emotion: " + emotion);

        if (fromRewardTask) {
            if (emotion.equals("happy")) {
                Intent resultIntent = new Intent();
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                showEncouragementDialog(emotion);
            }
        } else {
            showEmotionDialog(emotion);
        }
    }

    private void showEmotionDialog(String emotion) {
        new AlertDialog.Builder(this)
                .setTitle("Detected Emotion")
                .setMessage("The detected emotion is: " + emotion)
                .show();
    }

    private void showEncouragementDialog(String emotion) {
        new AlertDialog.Builder(this)
                .setTitle("Keep Trying")
                .setMessage("The detected emotion is: " + emotion + ". Try to smile!")
                .setPositiveButton("Okay", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("fer_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void loadModel() {
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float[][][][] preprocessFaceImage(Face face, Bitmap originalBitmap) {
        Rect boundingBox = face.getBoundingBox();
        int left = Math.max(boundingBox.left, 0);
        int top = Math.max(boundingBox.top, 0);
        int right = Math.min(boundingBox.right, originalBitmap.getWidth());
        int bottom = Math.min(boundingBox.bottom, originalBitmap.getHeight());

        int width = right - left;
        int height = bottom - top;

        if (width <= 0 || height <= 0) {
            Log.e(TAG, "Invalid face bounding box dimensions");
            return null;
        }

        Bitmap faceBitmap = Bitmap.createBitmap(originalBitmap, left, top, width, height);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true);

        ByteBuffer grayscaleBuffer = toGrayscaleByteBuffer(resizedBitmap);

        float[][][][] input = new float[1][MODEL_INPUT_SIZE][MODEL_INPUT_SIZE][1];
        grayscaleBuffer.rewind();
        for (int i = 0; i < MODEL_INPUT_SIZE; i++) {
            for (int j = 0; j < MODEL_INPUT_SIZE; j++) {
                input[0][i][j][0] = (grayscaleBuffer.get() & 0xFF) / 255.0f;
            }
        }

        return input;
    }

    private ByteBuffer toGrayscaleByteBuffer(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE);
        buffer.order(ByteOrder.nativeOrder());
        int[] pixels = new int[MODEL_INPUT_SIZE * MODEL_INPUT_SIZE];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int pixel : pixels) {
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            int gray = (r + g + b) / 3;
            buffer.put((byte) gray);
        }

        return buffer;
    }

    private String mapOutputToEmotionLabel(float[] probabilities) {
        int maxIndex = 0;
        float maxProbability = probabilities[0];

        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > maxProbability) {
                maxProbability = probabilities[i];
                maxIndex = i;
            }
        }

        String[] emotionLabels = {"neutral", "happy", "surprised", "sad", "anger", "disgust", "fear", "contempt"};

        return emotionLabels[maxIndex];
    }
}
