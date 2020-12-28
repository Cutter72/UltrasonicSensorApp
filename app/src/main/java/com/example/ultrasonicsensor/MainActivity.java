package com.example.ultrasonicsensor;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName() + ": ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClickDoSomething(View view) {
        System.out.println(TAG + "onClickDoSomething");
        Toast.makeText(this, "onClickDoSomething", Toast.LENGTH_SHORT).show();
    }
}