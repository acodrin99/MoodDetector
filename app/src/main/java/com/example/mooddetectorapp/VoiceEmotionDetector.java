package com.example.mooddetectorapp;

import android.content.Context;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class VoiceEmotionDetector {
    private Interpreter interpreter;
    private static final int INPUT_SIZE = 128; // Example input size

    public VoiceEmotionDetector(Context context) throws IOException {
        interpreter = new Interpreter(loadModelFile(context));
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        FileInputStream fileInputStream = new FileInputStream("assets/Emotion_Voice_Detection_Model.tflite");
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = 0;
        long declaredLength = fileChannel.size();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public String detectEmotion(float[] audioData) {
        // Assuming the model input is a 2D array of shape [1, INPUT_SIZE]
        float[][] input = new float[1][INPUT_SIZE];
        for (int i = 0; i < INPUT_SIZE; i++) {
            input[0][i] = audioData[i];
        }

        // Output array to store the result
        float[][] output = new float[1][8]; // 8 emotions

        // Run the model
        interpreter.run(input, output);

        // Get the index of the highest value in the output array
        int emotionIndex = argMax(output[0]);

        // Return the detected emotion as a string
        return getEmotionLabel(emotionIndex);
    }

    private int argMax(float[] array) {
        int maxIndex = 0;
        float maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private String getEmotionLabel(int index) {
        switch (index) {
            case 0: return "Neutral";
            case 1: return "Calm";
            case 2: return "Happy";
            case 3: return "Sad";
            case 4: return "Angry";
            case 5: return "Fearful";
            case 6: return "Disgust";
            case 7: return "Surprised";
            default: return "Unknown";
        }
    }
}
