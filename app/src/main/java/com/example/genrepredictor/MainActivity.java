package com.example.genrepredictor;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.example.genrepredictor.ml.CNN;

public class MainActivity extends AppCompatActivity {

    TextView result, confidence;
    ImageView imageView;
    Button picture;
    int imageSize = 224;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = findViewById(R.id.result);
        confidence = findViewById(R.id.confidence);
        imageView = findViewById(R.id.imageView);
        picture = findViewById(R.id.button);

        picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch camera if we have permission
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 1);
                } else {
                    //Request camera permission if we don't have it.
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });
    }

    public void classifyImage(Bitmap image){
        try {
            CNN model = CNN.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            // get 1D array of 224 * 224 pixels in image
            int [] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

            // iterate over pixels and extract R, G, and B values. Add to bytebuffer.
            int pixel = 0;
            for(int i = 0; i < imageSize; i++){
                for(int j = 0; j < imageSize; j++){
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            CNN.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            int midPos = 0;
            int minPos = 0;
            float midConfidence = 0;
            float minConfidence = 0;
            float maxConfidence = 0;
            // Find first
            // largest element
            float first = confidences[0];
            for (int i = 1;
                 i < confidences.length ; i++) {
                if (confidences[i] > first) {
                    first = confidences[i];
                }
            }

            // Find second
            // largest element
            float second = Integer.MIN_VALUE;
            for (int i = 0;
                 i < confidences.length ; i++) {
                if (confidences[i] > second &&
                        confidences[i] < first) {
                    second = confidences[i];
                }
            }

            // Find third
            // largest element
            float third = Integer.MIN_VALUE;
            for (int i = 0;
                 i < confidences.length ; i++) {
                if (confidences[i] > third &&
                        confidences[i] < second) {
                    third = confidences[i];
                }
            }
            for(int i = 0; i < confidences.length; i++){
                if(confidences[i] ==  first){
                    //minConfidence = midConfidence;
                    //midConfidence = maxConfidence;
                    //maxConfidence = confidences[i];
                    maxPos = i;
                }
                else if (confidences[i] == second)
                {
                    //minConfidence = midConfidence;
                    //midConfidence = confidences[i];
                    midPos = i;
                }

                else if (confidences[i] == third) {
                    //minConfidence = confidences[i];
                    minPos = i;
                }
            }
            String[] classes = {"Action", "Adventure", "Animation", "Biography", "Comedy", "Crime", "Documentary", "Drama", "Family", "Fantasy", "History", "Horror", "Music", "Musical", "Mystery", "N/A", "News", "Reality-TV", "Romance", "Sci-Fi", "Short", "Sport", "Thriller", "War", "Western"};
            String g = "";
            g += String.format("%s  ", classes[maxPos]);
            g += String.format("%s  ", classes[midPos]);
            g += String.format("%s  ", classes[minPos]);
            result.setText(g);

            //result.setText(classes[maxPos]);
            //result.setText(classes[midPos]);
            //result.setText(classes[minPos]);

            String s = "";
            for(int i = 0; i < classes.length; i++){
                s += String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100);
            }
            confidence.setText(s);
            Intent resultIntent = new Intent(MainActivity.this, ResultActivity.class);
            resultIntent.putExtra("ResultData", g);
            resultIntent.putExtra("ResultConfidence", s);
            startActivity(resultIntent);


            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bitmap image = (Bitmap) data.getExtras().get("data");
            int dimension = Math.min(image.getWidth(), image.getHeight());
            image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
            imageView.setImageBitmap(image);

            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
            classifyImage(image);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}