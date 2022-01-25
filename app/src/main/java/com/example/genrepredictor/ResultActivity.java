package com.example.genrepredictor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {

    TextView result1, confidence1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        result1 = findViewById(R.id.result1);
        confidence1 = findViewById(R.id.confidence1);

        Intent getResult = getIntent();

        String Result = getResult.getStringExtra("ResultData");
        String ResultConfidence = getResult.getStringExtra("ResultConfidence");
        result1.setText(Result);
        confidence1.setText(ResultConfidence);

    }
}